package com.example.data

import android.content.Context
import com.example.R
import com.example.model.CompatibilityStatus
import com.example.model.SavedItemDoc
import com.example.model.SavedItemType
import com.example.model.ScanComponentDoc
import com.example.model.ScanDoc
import com.example.model.ThemePreference
import com.example.model.UserProfileDoc
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PartlyFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun usersCollection() = db.collection("users")
    private fun scansCollection(uid: String) = usersCollection().document(uid).collection("scans")
    private fun componentsCollection(uid: String, scanId: String) =
        scansCollection(uid).document(scanId).collection("components")

    private fun savedItemsCollection(uid: String) =
        usersCollection().document(uid).collection("savedItems")

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String,
        photoUrl: String?
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val docRef = usersCollection().document(userId)
            val existing = docRef.get().await()
            val safeName = displayName.trim().ifBlank { "Partly User" }.take(100)
            val safeEmail = email.trim().take(200)
            val safePhoto = photoUrl?.trim()?.take(500)?.takeIf { it.isNotBlank() }

            if (!existing.exists()) {
                val payload = buildMap<String, Any> {
                    put("userId", uid)
                    put("displayName", safeName)
                    put("email", safeEmail)
                    if (safePhoto != null) put("photoUrl", safePhoto)
                    put("preferredTheme", ThemePreference.SYSTEM.name)
                    put("scansCount", 0L)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            } else {
                val updates = buildMap<String, Any> {
                    put("displayName", safeName)
                    put("email", safeEmail)
                    if (safePhoto != null) put("photoUrl", safePhoto)
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfileDoc?> =
        usersCollection().document(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    UserProfileDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun updateThemePreference(userId: String, theme: ThemePreference): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            usersCollection().document(userId).update(
                mapOf(
                    "preferredTheme" to theme.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeScans(userId: String): Flow<List<ScanDoc>> =
        scansCollection(userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ScanDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeScan(userId: String, scanId: String): Flow<ScanDoc?> =
        scansCollection(userId).document(scanId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    ScanDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeScanComponents(userId: String, scanId: String): Flow<List<ScanComponentDoc>> =
        componentsCollection(userId, scanId)
            .orderBy("layerOrder", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ScanComponentDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun saveScanWithComponents(
        userId: String,
        scan: ScanDoc,
        components: List<ScanComponentDoc>
    ): Result<String> {
        val basePath = "users/$userId/scans"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }

            val scanRef = scansCollection(uid).document()
            val scanId = scanRef.id
            val boundedComponents = components.take(40)

            val scanPayload = mapOf(
                "userId" to uid,
                "productBrand" to scan.productBrand.ifBlank { "Unknown" }.take(100),
                "productModel" to scan.productModel.ifBlank { "Unidentified Product" }.take(140),
                "category" to scan.category.ifBlank { "Other" },
                "generationOrEra" to scan.generationOrEra.take(100),
                "movementOrPowerType" to scan.movementOrPowerType.take(100),
                "confidenceScore" to scan.confidenceScore.coerceIn(0.0, 1.0),
                "confidenceLabel" to scan.confidenceLabel,
                "damageNotes" to scan.damageNotes.take(600),
                "missingComponentNotes" to scan.missingComponentNotes.take(600),
                "needsAdditionalAngle" to scan.needsAdditionalAngle,
                "suggestedNextAngle" to scan.suggestedNextAngle.take(240),
                "dataVerificationStatus" to scan.dataVerificationStatus,
                "componentCount" to boundedComponents.size.toLong(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(scanRef, scanPayload)

            boundedComponents.forEachIndexed { index, component ->
                val compRef = componentsCollection(uid, scanId).document()
                val compPayload = mapOf(
                    "userId" to uid,
                    "scanId" to scanId,
                    "name" to component.name.ifBlank { "Component ${index + 1}" }.take(120),
                    "categoryKey" to component.categoryKey.ifBlank { "generic_part" }.take(60),
                    "alternativeNames" to component.alternativeNames.take(10).map { it.take(80) },
                    "primaryFunction" to component.primaryFunction.ifBlank { "Structural or functional assembly element." }.take(400),
                    "material" to component.material.take(120),
                    "dimensions" to component.dimensions.take(120),
                    "partNumber" to component.partNumber.take(80),
                    "compatibilityStatus" to component.compatibilityStatus,
                    "compatibilityReason" to component.compatibilityReason.take(400),
                    "replacementDifficulty" to component.replacementDifficulty,
                    "visibleInPhoto" to component.visibleInPhoto,
                    "hiddenInViewer" to component.hiddenInViewer,
                    "layerOrder" to index.toLong(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(compRef, compPayload)
            }

            batch.commit().await()

            // Increment user scan counter best-effort
            runCatching {
                usersCollection().document(uid).update(
                    mapOf(
                        "scansCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(scanId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, basePath)
            Result.failure(e)
        }
    }

    suspend fun updateComponentVisibility(
        userId: String,
        scanId: String,
        componentId: String,
        hiddenInViewer: Boolean
    ): Result<Unit> {
        val path = "users/$userId/scans/$scanId/components/$componentId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(scanId.isNotBlank() && componentId.isNotBlank()) { "IDs must not be blank." }
            componentsCollection(uid, scanId).document(componentId).update(
                mapOf(
                    "hiddenInViewer" to hiddenInViewer,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateComponentMeasurement(
        userId: String,
        scanId: String,
        componentId: String,
        dimensions: String,
        compatibilityStatus: CompatibilityStatus,
        compatibilityReason: String
    ): Result<Unit> {
        val path = "users/$userId/scans/$scanId/components/$componentId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            componentsCollection(uid, scanId).document(componentId).update(
                mapOf(
                    "dimensions" to dimensions.trim().take(120),
                    "compatibilityStatus" to compatibilityStatus.name,
                    "compatibilityReason" to compatibilityReason.trim().take(400),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteScan(userId: String, scanId: String): Result<Unit> {
        val path = "users/$userId/scans/$scanId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val compSnapshots = componentsCollection(uid, scanId).get().await()
            val batch = db.batch()
            compSnapshots.documents.forEach { batch.delete(it.reference) }
            batch.delete(scansCollection(uid).document(scanId))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeSavedItems(userId: String): Flow<List<SavedItemDoc>> =
        savedItemsCollection(userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SavedItemDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun saveBookmark(
        userId: String,
        itemType: SavedItemType,
        referenceId: String,
        title: String,
        subtitle: String,
        category: String,
        compatibilityStatus: CompatibilityStatus,
        notes: String = ""
    ): Result<Unit> {
        val path = "users/$userId/savedItems"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val safeRef = referenceId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120).ifBlank { "ref" }
            val docId = "${itemType.name.lowercase()}_$safeRef".take(128)
            val docRef = savedItemsCollection(uid).document(docId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "title" to title.trim().ifBlank { "Saved Item" }.take(160),
                        "subtitle" to subtitle.trim().take(240),
                        "notes" to notes.trim().take(500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "itemType" to itemType.name,
                    "referenceId" to safeRef,
                    "title" to title.trim().ifBlank { "Saved Item" }.take(160),
                    "subtitle" to subtitle.trim().take(240),
                    "category" to category.ifBlank { "Other" }.take(60),
                    "compatibilityStatus" to compatibilityStatus.name,
                    "notes" to notes.trim().take(500),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun removeSavedItem(userId: String, savedId: String): Result<Unit> {
        val path = "users/$userId/savedItems/$savedId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            savedItemsCollection(uid).document(savedId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAllUserData(userId: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }

            val scans = scansCollection(uid).get().await()
            for (scanDoc in scans.documents) {
                val comps = componentsCollection(uid, scanDoc.id).get().await()
                val batch = db.batch()
                comps.documents.forEach { batch.delete(it.reference) }
                batch.delete(scanDoc.reference)
                batch.commit().await()
            }

            val saved = savedItemsCollection(uid).get().await()
            if (!saved.isEmpty) {
                val batch = db.batch()
                saved.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }

            usersCollection().document(uid).update(
                mapOf(
                    "scansCount" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
