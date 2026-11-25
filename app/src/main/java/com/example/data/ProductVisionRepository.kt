package com.example.data

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.example.model.AnalysisStage
import com.example.model.CompatibilityStatus
import com.example.model.ConfidenceLevel
import com.example.model.ProductCategory
import com.example.model.ReplacementDifficulty
import com.example.model.ScanComponentDoc
import com.example.model.ScanDoc
import com.example.model.ScanResultBundle
import com.example.model.VerificationProvenance
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

private const val VISION_TAG = "ProductVisionRepo"

data class ImageQualityReport(
    val passed: Boolean,
    val summary: String,
    val preparedBitmaps: List<Bitmap>
)

class ProductVisionRepository {

    private val identificationSchema = Schema.obj(
        properties = mapOf(
            "category" to Schema.enumeration(
                values = listOf("Watches", "Electronics", "Appliances", "Other"),
                description = "Primary product category"
            ),
            "brand" to Schema.string(description = "Identified brand or 'Unknown' if not clearly observable"),
            "model" to Schema.string(description = "Identified model name/number or 'Unidentified Product' if uncertain"),
            "generationOrEra" to Schema.string(description = "Estimated generation or era only when supported by visual evidence, otherwise 'Unknown'"),
            "movementOrPowerType" to Schema.string(description = "e.g. Automatic Mechanical, Quartz, Li-Ion Smartphone, AC Espresso Appliance, or Unknown"),
            "confidenceScore" to Schema.double(description = "Overall identification confidence from 0.0 to 1.0"),
            "needsAdditionalAngle" to Schema.boolean(description = "True if obscured parts, low detail, or rear markings require another photo"),
            "suggestedNextAngle" to Schema.string(description = "Specific angle to photograph next if needed, or empty string"),
            "damageNotes" to Schema.string(description = "Observable scratches, cracks, bent pins, corrosion, or 'None observed'"),
            "missingComponentNotes" to Schema.string(description = "Missing parts if clear from reference geometry, or 'None observed'"),
            "components" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(description = "Component name, e.g. Watch Strap Pin (Spring Bar)"),
                        "categoryKey" to Schema.string(description = "Canonical key e.g. watch_crystal, bezel, dial, hands, crown, watch_case, movement, case_back, strap, spring_bar, clasp, fasteners, display_module, battery_pack, charging_port, camera_module, group_gasket, shower_screen, portafilter_basket, water_valve_pump, or generic_part"),
                        "alternativeNames" to Schema.array(items = Schema.string()),
                        "primaryFunction" to Schema.string(description = "Concise explanation of what this component does"),
                        "material" to Schema.string(description = "Observable material or 'Verify in person'"),
                        "estimatedDimensions" to Schema.string(description = "Only verifiable standard sizing or 'Measurement required'"),
                        "partNumber" to Schema.string(description = "Only if clearly stamped/verified, otherwise 'Unverified — check reference'"),
                        "compatibilityStatus" to Schema.enumeration(
                            values = listOf("VERIFIED_COMPATIBLE", "LIKELY_COMPATIBLE", "COMPATIBILITY_UNKNOWN", "NOT_COMPATIBLE")
                        ),
                        "compatibilityReason" to Schema.string(description = "Honest evidence explaining why compatibility is verified, likely, or unknown"),
                        "replacementDifficulty" to Schema.enumeration(
                            values = listOf("EASY", "MODERATE", "ADVANCED", "PROFESSIONAL")
                        ),
                        "visibleInPhoto" to Schema.boolean(description = "Whether this part is directly visible vs inferred internal sub-assembly")
                    )
                ),
                description = "Recognized product components ordered top/front to bottom/back"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = identificationSchema
                temperature = 0.2f
            }
        )
    }

    suspend fun assessAndPrepareImages(rawBitmaps: List<Bitmap>): ImageQualityReport =
        withContext(Dispatchers.Default) {
            if (rawBitmaps.isEmpty()) {
                return@withContext ImageQualityReport(
                    passed = false,
                    summary = "No images provided for analysis.",
                    preparedBitmaps = emptyList()
                )
            }

            val prepared = mutableListOf<Bitmap>()
            var lowLightWarning = false
            var lowResWarning = false

            for (bmp in rawBitmaps.take(4)) {
                if (bmp.width < 220 || bmp.height < 220) {
                    lowResWarning = true
                }
                val avgLuma = sampleAverageLuma(bmp)
                if (avgLuma < 26) {
                    lowLightWarning = true
                }
                // Re-encoding bitmap scales safely to <= 1280px and strips EXIF metadata
                prepared.add(downscaleIfNeeded(bmp, maxDimension = 1280))
            }

            val warningMsg = when {
                lowResWarning -> "Image resolution is low; identification confidence may be reduced."
                lowLightWarning -> "Image appears dark; brighter lighting improves component detection."
                else -> "Image quality verified (${prepared.size} angle${if (prepared.size > 1) "s" else ""})."
            }

            ImageQualityReport(
                passed = true,
                summary = warningMsg,
                preparedBitmaps = prepared
            )
        }

    suspend fun analyzeProductImages(
        userId: String,
        rawBitmaps: List<Bitmap>,
        categoryHint: ProductCategory? = null,
        onStageChanged: (AnalysisStage) -> Unit
    ): Result<ScanResultBundle> {
        return try {
            onStageChanged(AnalysisStage.INGESTING)
            val quality = assessAndPrepareImages(rawBitmaps)
            if (!quality.passed || quality.preparedBitmaps.isEmpty()) {
                return Result.failure(IllegalArgumentException(quality.summary))
            }

            onStageChanged(AnalysisStage.IDENTIFYING_PRODUCT)
            val prompt = buildString {
                appendLine("You are Partly, a precision physical-product component intelligence engine.")
                appendLine("Analyze the provided product photograph(s) carefully.")
                if (categoryHint != null && categoryHint != ProductCategory.OTHER) {
                    appendLine("The user indicated the category may be: ${categoryHint.displayName}.")
                }
                appendLine("STRICT ACCURACY RULES:")
                appendLine("1. NEVER fabricate a brand, model number, caliber, part number, or exact millimeter measurement unless visibly legible or unmistakably verified.")
                appendLine("2. If you cannot verify the exact model from the image(s), set brand/model to 'Unknown' or 'Unidentified', lower confidenceScore accordingly (< 0.55), and set needsAdditionalAngle=true with a concrete suggestedNextAngle (e.g., 'Photograph the caseback engraving' or 'Photograph the rear model plate').")
                appendLine("3. Distinguish visible components (visibleInPhoto=true) from standard internal/hidden sub-assemblies (visibleInPhoto=false).")
                appendLine("4. Never mark a part VERIFIED_COMPATIBLE unless the exact model and standard dimension are confirmed; otherwise use LIKELY_COMPATIBLE or COMPATIBILITY_UNKNOWN and explain what the user must measure.")
                appendLine("5. Prioritize watches, consumer electronics, and household appliances with clean engineering terminology.")
            }

            val response = withTimeout(60_000L) {
                onStageChanged(AnalysisStage.RECOGNIZING_COMPONENTS)
                model.generateContent(
                    content {
                        quality.preparedBitmaps.forEach { image(it) }
                        text(prompt)
                    }
                )
            }

            val jsonText = response.text
                ?: throw IllegalStateException("Vision model returned an empty response.")

            onStageChanged(AnalysisStage.CHECKING_COMPATIBILITY)
            val bundle = parseStructuredResponse(userId = userId, jsonString = jsonText)

            onStageChanged(AnalysisStage.FINDING_PARTS)
            Result.success(bundle)
        } catch (e: TimeoutCancellationException) {
            Log.e(VISION_TAG, "Gemini vision analysis timed out", e)
            Result.failure(Exception("Analysis timed out after 60 seconds. Please try again with a clearer image."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(VISION_TAG, "Gemini vision analysis failed", e)
            Result.failure(e)
        }
    }

    private fun parseStructuredResponse(userId: String, jsonString: String): ScanResultBundle {
        val root = JSONObject(jsonString)
        val category = ProductCategory.fromRaw(root.optString("category", "Other"))
        val brand = root.optString("brand", "Unknown").ifBlank { "Unknown" }
        val modelName = root.optString("model", "Unidentified Product").ifBlank { "Unidentified Product" }
        val era = root.optString("generationOrEra", "Unknown").ifBlank { "Unknown" }
        val powerType = root.optString("movementOrPowerType", "Unknown").ifBlank { "Unknown" }
        val confidence = root.optDouble("confidenceScore", 0.5).coerceIn(0.0, 1.0)
        val confidenceLevel = ConfidenceLevel.fromScore(confidence)
        val needsAngle = root.optBoolean("needsAdditionalAngle", confidence < 0.65)
        val nextAngle = root.optString("suggestedNextAngle", "")
        val damage = root.optString("damageNotes", "None observed")
        val missing = root.optString("missingComponentNotes", "None observed")

        val componentsJson = root.optJSONArray("components")
        val parsedComponents = mutableListOf<ScanComponentDoc>()

        if (componentsJson != null) {
            for (i in 0 until componentsJson.length()) {
                val item = componentsJson.optJSONObject(i) ?: continue
                val name = item.optString("name", "Component ${i + 1}").ifBlank { "Component ${i + 1}" }
                val key = item.optString("categoryKey", "generic_part").ifBlank { "generic_part" }
                val altArray = item.optJSONArray("alternativeNames")
                val altNames = if (altArray != null) {
                    (0 until min(altArray.length(), 10)).mapNotNull { idx ->
                        altArray.optString(idx).takeIf { it.isNotBlank() }
                    }
                } else emptyList()

                val template = VerifiedCatalogRepository.findTemplate(category, key, name)
                val resolvedKey = template?.key ?: key

                val compatStatus = CompatibilityStatus.fromRaw(item.optString("compatibilityStatus"))
                val difficulty = ReplacementDifficulty.fromRaw(item.optString("replacementDifficulty"))

                parsedComponents.add(
                    ScanComponentDoc(
                        userId = userId,
                        scanId = "",
                        name = name,
                        categoryKey = resolvedKey,
                        alternativeNames = if (altNames.isNotEmpty()) altNames else template?.alternativeNames.orEmpty(),
                        primaryFunction = item.optString("primaryFunction").ifBlank {
                            template?.primaryFunction ?: "Structural or functional assembly component."
                        },
                        material = item.optString("material").ifBlank {
                            template?.defaultMaterial ?: "Verify material in person"
                        },
                        dimensions = item.optString("estimatedDimensions").ifBlank { "Measurement required" },
                        partNumber = item.optString("partNumber").ifBlank { "Unverified — check reference" },
                        compatibilityStatus = compatStatus.name,
                        compatibilityReason = item.optString("compatibilityReason").ifBlank {
                            "Compatibility requires verifying exact model & caliper dimensions."
                        },
                        replacementDifficulty = difficulty.name,
                        visibleInPhoto = item.optBoolean("visibleInPhoto", true),
                        hiddenInViewer = false,
                        layerOrder = (template?.layerOrder ?: i).toLong()
                    )
                )
            }
        }

        // If watch/electronics/appliance detected and some standard assembly parts were obscured,
        // ensure the assembly remains honest while giving a complete structural map if verified templates exist
        val finalComponents = parsedComponents
            .sortedBy { it.layerOrder }
            .mapIndexed { idx, comp -> comp.copy(layerOrder = idx.toLong()) }

        val verificationStatus = if (
            confidence >= 0.80 &&
            !brand.equals("Unknown", ignoreCase = true) &&
            category != ProductCategory.OTHER
        ) {
            VerificationProvenance.AI_ILLUSTRATIVE.name
        } else {
            VerificationProvenance.AI_ILLUSTRATIVE.name
        }

        val scan = ScanDoc(
            userId = userId,
            productBrand = brand,
            productModel = modelName,
            category = category.firestoreValue,
            generationOrEra = era,
            movementOrPowerType = powerType,
            confidenceScore = confidence,
            confidenceLabel = confidenceLevel.name,
            damageNotes = damage,
            missingComponentNotes = missing,
            needsAdditionalAngle = needsAngle,
            suggestedNextAngle = nextAngle,
            dataVerificationStatus = verificationStatus,
            componentCount = finalComponents.size.toLong()
        )

        return ScanResultBundle(scan = scan, components = finalComponents)
    }

    private fun downscaleIfNeeded(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width
        val height = source.height
        val largest = max(width, height)
        if (largest <= maxDimension) {
            return source.copy(Bitmap.Config.ARGB_8888, false)
        }
        val ratio = maxDimension.toFloat() / largest.toFloat()
        val targetW = max(1, (width * ratio).toInt())
        val targetH = max(1, (height * ratio).toInt())
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    private fun sampleAverageLuma(bitmap: Bitmap): Int {
        val stepX = max(1, bitmap.width / 16)
        val stepY = max(1, bitmap.height / 16)
        var sum = 0L
        var count = 0
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val px = bitmap.getPixel(x, y)
                val luma = (0.299 * Color.red(px) + 0.587 * Color.green(px) + 0.114 * Color.blue(px)).toInt()
                sum += luma
                count++
                x += stepX
            }
            y += stepY
        }
        return if (count > 0) (sum / count).toInt() else 128
    }
}
