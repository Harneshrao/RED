package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PartlyFirestoreRepository
import com.example.data.PreferencesRepository
import com.example.data.ProductVisionRepository
import com.example.data.VerifiedCatalogRepository
import com.example.model.AnalysisStage
import com.example.model.CompatibilityStatus
import com.example.model.ProductCategory
import com.example.model.SavedItemDoc
import com.example.model.SavedItemType
import com.example.model.ScanComponentDoc
import com.example.model.ScanDoc
import com.example.model.ThemePreference
import com.example.model.UserProfileDoc
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val VM_TAG = "PartlyMainVM"

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data class Processing(
        val stage: AnalysisStage,
        val progress: Float
    ) : AnalysisUiState
    data class Completed(
        val scanId: String,
        val needsAdditionalAngle: Boolean,
        val suggestedNextAngle: String,
        val confidenceScore: Double
    ) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class PartlyMainViewModel(
    private val firestoreRepo: PartlyFirestoreRepository,
    private val visionRepo: ProductVisionRepository,
    private val prefsRepo: PreferencesRepository,
    val currentUserId: String,
    val userDisplayName: String,
    val userEmail: String,
    val userPhotoUrl: String?
) : androidx.lifecycle.ViewModel() {

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _capturedBitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val capturedBitmaps: StateFlow<List<Bitmap>> = _capturedBitmaps.asStateFlow()

    private val _categoryHint = MutableStateFlow<ProductCategory?>(null)
    val categoryHint: StateFlow<ProductCategory?> = _categoryHint.asStateFlow()

    private val _analysisState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()

    private val _sessionThumbnails = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val sessionThumbnails: StateFlow<Map<String, Bitmap>> = _sessionThumbnails.asStateFlow()

    private val _activeScanId = MutableStateFlow<String?>(null)
    val activeScanId: StateFlow<String?> = _activeScanId.asStateFlow()

    val onboardingCompleted: StateFlow<Boolean?> = prefsRepo.onboardingCompleted
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val themePreference: StateFlow<ThemePreference> = prefsRepo.themePreference
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ThemePreference.SYSTEM)

    val userProfile: StateFlow<UserProfileDoc?> = firestoreRepo.observeUserProfile(currentUserId)
        .catch { error ->
            if (isPureCancellation(error)) throw error
            Log.w(VM_TAG, "Failed to observe user profile", unwrapFirestoreError(error))
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val scansState: StateFlow<UiState<List<ScanDoc>>> = firestoreRepo.observeScans(currentUserId)
        .asUiState("Failed to load scan history")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val savedItemsState: StateFlow<UiState<List<SavedItemDoc>>> = firestoreRepo.observeSavedItems(currentUserId)
        .asUiState("Failed to load saved library")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val activeScanState: StateFlow<UiState<ScanDoc?>> = _activeScanId
        .flatMapLatest { scanId ->
            if (scanId.isNullOrBlank()) {
                flowOf(UiState.Success(null))
            } else {
                firestoreRepo.observeScan(currentUserId, scanId)
                    .asUiState("Failed to load product overview")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val activeComponentsState: StateFlow<UiState<List<ScanComponentDoc>>> = _activeScanId
        .flatMapLatest { scanId ->
            if (scanId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                firestoreRepo.observeScanComponents(currentUserId, scanId)
                    .asUiState("Failed to load detected components")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    init {
        viewModelScope.launch {
            firestoreRepo.ensureUserProfile(
                userId = currentUserId,
                displayName = userDisplayName,
                email = userEmail,
                photoUrl = userPhotoUrl
            )
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun setCategoryHint(category: ProductCategory?) {
        _categoryHint.value = category
    }

    fun addCapturedBitmap(bitmap: Bitmap) {
        _capturedBitmaps.update { current -> (current + bitmap).take(4) }
    }

    fun replaceCapturedBitmap(index: Int, bitmap: Bitmap) {
        _capturedBitmaps.update { current ->
            if (index in current.indices) {
                current.toMutableList().apply { set(index, bitmap) }
            } else current
        }
    }

    fun removeCapturedBitmap(index: Int) {
        _capturedBitmaps.update { current ->
            current.filterIndexed { i, _ -> i != index }
        }
    }

    fun clearCapturedBitmaps() {
        _capturedBitmaps.value = emptyList()
        _analysisState.value = AnalysisUiState.Idle
    }

    fun selectScan(scanId: String) {
        _activeScanId.value = scanId
    }

    fun runProductAnalysis(onSuccessNavigate: (String) -> Unit) {
        val images = _capturedBitmaps.value
        if (images.isEmpty()) {
            _analysisState.value = AnalysisUiState.Error("Capture or upload at least one photograph first.")
            return
        }

        viewModelScope.launch {
            _analysisState.value = AnalysisUiState.Processing(AnalysisStage.INGESTING, 0.12f)
            val result = visionRepo.analyzeProductImages(
                userId = currentUserId,
                rawBitmaps = images,
                categoryHint = _categoryHint.value,
                onStageChanged = { stage ->
                    val progress = (stage.stepIndex + 1) / AnalysisStage.entries.size.toFloat()
                    _analysisState.value = AnalysisUiState.Processing(stage, progress.coerceIn(0.15f, 0.95f))
                }
            )

            result.fold(
                onSuccess = { bundle ->
                    val saveResult = firestoreRepo.saveScanWithComponents(
                        userId = currentUserId,
                        scan = bundle.scan,
                        components = bundle.components
                    )
                    saveResult.fold(
                        onSuccess = { scanId ->
                            images.firstOrNull()?.let { thumb ->
                                _sessionThumbnails.update { it + (scanId to thumb) }
                            }
                            _activeScanId.value = scanId
                            _analysisState.value = AnalysisUiState.Completed(
                                scanId = scanId,
                                needsAdditionalAngle = bundle.scan.needsAdditionalAngle,
                                suggestedNextAngle = bundle.scan.suggestedNextAngle,
                                confidenceScore = bundle.scan.confidenceScore
                            )
                            if (!bundle.scan.needsAdditionalAngle && bundle.scan.confidenceScore >= 0.60) {
                                onSuccessNavigate(scanId)
                            }
                        },
                        onFailure = { err ->
                            _analysisState.value = AnalysisUiState.Error(
                                err.localizedMessage ?: "Failed to save scan results to your library."
                            )
                        }
                    )
                },
                onFailure = { err ->
                    _analysisState.value = AnalysisUiState.Error(
                        err.localizedMessage ?: "Product recognition failed. Please try another angle."
                    )
                }
            )
        }
    }

    fun loadVerifiedCategoryReference(
        category: ProductCategory,
        onSuccessNavigate: (String) -> Unit
    ) {
        viewModelScope.launch {
            val bundle = VerifiedCatalogRepository.createVerifiedCategoryBundle(currentUserId, category)
            val result = firestoreRepo.saveScanWithComponents(
                userId = currentUserId,
                scan = bundle.scan,
                components = bundle.components
            )
            result.fold(
                onSuccess = { scanId ->
                    _activeScanId.value = scanId
                    onSuccessNavigate(scanId)
                },
                onFailure = { err ->
                    _statusBanner.value = err.localizedMessage ?: "Could not load reference assembly."
                }
            )
        }
    }

    fun toggleComponentVisibility(scanId: String, componentId: String, hiddenInViewer: Boolean) {
        viewModelScope.launch {
            firestoreRepo.updateComponentVisibility(currentUserId, scanId, componentId, hiddenInViewer)
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to update layer visibility." }
        }
    }

    fun updateComponentMeasurement(
        scanId: String,
        componentId: String,
        dimensions: String,
        status: CompatibilityStatus,
        reason: String
    ) {
        viewModelScope.launch {
            firestoreRepo.updateComponentMeasurement(
                userId = currentUserId,
                scanId = scanId,
                componentId = componentId,
                dimensions = dimensions,
                compatibilityStatus = status,
                compatibilityReason = reason
            ).onSuccess {
                _statusBanner.value = "Measurement & compatibility updated."
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to save measurement."
            }
        }
    }

    fun saveBookmark(
        itemType: SavedItemType,
        referenceId: String,
        title: String,
        subtitle: String,
        category: String,
        compatibilityStatus: CompatibilityStatus,
        notes: String = ""
    ) {
        viewModelScope.launch {
            firestoreRepo.saveBookmark(
                userId = currentUserId,
                itemType = itemType,
                referenceId = referenceId,
                title = title,
                subtitle = subtitle,
                category = category,
                compatibilityStatus = compatibilityStatus,
                notes = notes
            ).onSuccess {
                _statusBanner.value = "Saved to library."
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to save item."
            }
        }
    }

    fun removeSavedItem(savedId: String) {
        viewModelScope.launch {
            firestoreRepo.removeSavedItem(currentUserId, savedId)
                .onSuccess { _statusBanner.value = "Removed from library." }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to remove item." }
        }
    }

    fun deleteScan(scanId: String) {
        viewModelScope.launch {
            firestoreRepo.deleteScan(currentUserId, scanId)
                .onSuccess {
                    if (_activeScanId.value == scanId) _activeScanId.value = null
                    _statusBanner.value = "Scan deleted."
                }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to delete scan." }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch { prefsRepo.setOnboardingCompleted(true) }
    }

    fun resetOnboarding() {
        viewModelScope.launch { prefsRepo.setOnboardingCompleted(false) }
    }

    fun setThemePreference(theme: ThemePreference) {
        viewModelScope.launch {
            prefsRepo.setThemePreference(theme)
            firestoreRepo.updateThemePreference(currentUserId, theme)
        }
    }

    fun deleteAllUserData() {
        viewModelScope.launch {
            firestoreRepo.deleteAllUserData(currentUserId)
                .onSuccess {
                    _activeScanId.value = null
                    _statusBanner.value = "All scans and saved items deleted."
                }
                .onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Failed to clear account data."
                }
        }
    }

    private fun <T> Flow<T>.asUiState(fallbackError: String): Flow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(VM_TAG, fallbackError, root)
                emit(UiState.Error(root.message ?: fallbackError))
            }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private fun isPureCancellation(error: Throwable): Boolean =
        unwrapFirestoreError(error) == null && error is CancellationException
}
