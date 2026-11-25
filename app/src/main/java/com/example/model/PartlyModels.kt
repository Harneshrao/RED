package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ThemePreference {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun fromRaw(raw: String?): ThemePreference =
            entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}

enum class ProductCategory(val displayName: String, val firestoreValue: String) {
    WATCHES("Watches", "Watches"),
    ELECTRONICS("Smartphones & Electronics", "Electronics"),
    APPLIANCES("Household Appliances", "Appliances"),
    OTHER("Other Products", "Other");

    companion object {
        fun fromRaw(raw: String?): ProductCategory =
            entries.firstOrNull { it.firestoreValue.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true) } ?: OTHER
    }
}

enum class ConfidenceLevel(val label: String) {
    HIGH("High Confidence"),
    MEDIUM("Moderate Confidence"),
    LOW("Low Confidence");

    companion object {
        fun fromScore(score: Double): ConfidenceLevel = when {
            score >= 0.78 -> HIGH
            score >= 0.50 -> MEDIUM
            else -> LOW
        }

        fun fromRaw(raw: String?): ConfidenceLevel =
            entries.firstOrNull { it.name == raw } ?: MEDIUM
    }
}

enum class CompatibilityStatus(val label: String, val shortLabel: String) {
    VERIFIED_COMPATIBLE("Verified compatible", "Verified"),
    LIKELY_COMPATIBLE("Likely compatible", "Likely"),
    COMPATIBILITY_UNKNOWN("Compatibility unknown", "Unknown"),
    NOT_COMPATIBLE("Not compatible", "Incompatible");

    companion object {
        fun fromRaw(raw: String?): CompatibilityStatus =
            entries.firstOrNull { it.name == raw } ?: COMPATIBILITY_UNKNOWN
    }
}

enum class ReplacementDifficulty(val label: String) {
    EASY("Easy • Tool-free or basic"),
    MODERATE("Moderate • Precision tools"),
    ADVANCED("Advanced • Calibration required"),
    PROFESSIONAL("Professional • Watchmaker / Technician");

    companion object {
        fun fromRaw(raw: String?): ReplacementDifficulty =
            entries.firstOrNull { it.name == raw } ?: MODERATE
    }
}

enum class VerificationProvenance(val label: String) {
    VERIFIED_DATASET("Verified Reference Assembly"),
    AI_ILLUSTRATIVE("AI Illustrative Visualization");

    companion object {
        fun fromRaw(raw: String?): VerificationProvenance =
            entries.firstOrNull { it.name == raw } ?: AI_ILLUSTRATIVE
    }
}

enum class SavedItemType(val label: String) {
    PRODUCT("Products"),
    COMPONENT("Components"),
    REPLACEMENT_PART("Parts"),
    REPAIR_PROJECT("Repairs");

    companion object {
        fun fromRaw(raw: String?): SavedItemType =
            entries.firstOrNull { it.name == raw } ?: COMPONENT
    }
}

// --- Firestore Serialized Models ---

@Keep
@IgnoreExtraProperties
data class UserProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val preferredTheme: String = ThemePreference.SYSTEM.name,
    val scansCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ScanDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productBrand: String = "",
    val productModel: String = "",
    val category: String = ProductCategory.OTHER.firestoreValue,
    val generationOrEra: String = "",
    val movementOrPowerType: String = "",
    val confidenceScore: Double = 0.0,
    val confidenceLabel: String = ConfidenceLevel.LOW.name,
    val damageNotes: String = "",
    val missingComponentNotes: String = "",
    val needsAdditionalAngle: Boolean = false,
    val suggestedNextAngle: String = "",
    val dataVerificationStatus: String = VerificationProvenance.AI_ILLUSTRATIVE.name,
    val componentCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ScanComponentDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val scanId: String = "",
    val name: String = "",
    val categoryKey: String = "",
    val alternativeNames: List<String> = emptyList(),
    val primaryFunction: String = "",
    val material: String = "",
    val dimensions: String = "",
    val partNumber: String = "",
    val compatibilityStatus: String = CompatibilityStatus.COMPATIBILITY_UNKNOWN.name,
    val compatibilityReason: String = "",
    val replacementDifficulty: String = ReplacementDifficulty.MODERATE.name,
    val visibleInPhoto: Boolean = true,
    val hiddenInViewer: Boolean = false,
    val layerOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedItemDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val itemType: String = SavedItemType.COMPONENT.name,
    val referenceId: String = "",
    val title: String = "",
    val subtitle: String = "",
    val category: String = ProductCategory.WATCHES.firestoreValue,
    val compatibilityStatus: String = CompatibilityStatus.COMPATIBILITY_UNKNOWN.name,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// --- Domain & Catalog Intelligence Models ---

data class RepairGuideInfo(
    val componentKey: String,
    val componentName: String,
    val problemIdentification: String,
    val requiredTools: List<String>,
    val steps: List<String>,
    val difficulty: ReplacementDifficulty,
    val estimatedMinutesRange: String,
    val safetyWarnings: List<String>,
    val sourceAttribution: String
)

data class MarketplaceSearchChannel(
    val id: String,
    val partnerName: String,
    val channelType: String,
    val searchQuery: String,
    val searchUrl: String,
    val specNotes: String,
    val originalOrAftermarket: String,
    val materialSpec: String,
    val dimensionSpec: String,
    val compatibilityStatus: CompatibilityStatus,
    val integrationConfigured: Boolean = false
)

data class MarketplaceSampleListing(
    val id: String,
    val title: String,
    val sellerName: String,
    val priceUsd: Double,
    val availabilityLabel: String,
    val deliveryEstimate: String,
    val material: String,
    val dimensions: String,
    val isOriginalOem: Boolean,
    val isRefurbished: Boolean,
    val compatibilityStatus: CompatibilityStatus,
    val sellerRating: Double
)

data class CatalogComponentTemplate(
    val key: String,
    val name: String,
    val alternativeNames: List<String>,
    val primaryFunction: String,
    val defaultMaterial: String,
    val measurementGuidance: String,
    val standardSizes: List<String>,
    val defaultDifficulty: ReplacementDifficulty,
    val layerOrder: Int,
    val repairGuide: RepairGuideInfo
)

enum class AnalysisStage(val title: String, val subtitle: String, val stepIndex: Int) {
    INGESTING("Ingesting & checking image", "Validating sharpness, framing, and stripping metadata", 0),
    IDENTIFYING_PRODUCT("Identifying product", "Detecting category, brand, model, and architecture", 1),
    RECOGNIZING_COMPONENTS("Recognizing components", "Mapping visible parts and inspecting condition", 2),
    CHECKING_COMPATIBILITY("Checking compatibility", "Cross-referencing verified dimensions and assemblies", 3),
    FINDING_PARTS("Preparing replacement search", "Linking compatible parts catalogs and repair guides", 4)
}

data class ScanResultBundle(
    val scan: ScanDoc,
    val components: List<ScanComponentDoc>
)
