package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.configureAppCheck
import com.example.data.PartlyFirestoreRepository
import com.example.data.PreferencesRepository
import com.example.data.ProductVisionRepository
import com.example.data.attemptAutoSignIn
import com.example.data.authStateFlow
import com.example.data.signOut
import com.example.model.CompatibilityStatus
import com.example.model.ProductCategory
import com.example.model.SavedItemType
import com.example.model.ThemePreference
import com.example.ui.components.NoticeCallout
import com.example.ui.screens.AnalysisScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CameraCaptureScreen
import com.example.ui.screens.ComponentDetailScreen
import com.example.ui.screens.ExplorerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MarketplaceScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RepairGuideScreen
import com.example.ui.screens.SavedLibraryScreen
import com.example.ui.theme.PartlyTheme
import com.example.ui.viewmodel.PartlyMainViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private object PartlyRoutes {
    const val HOME = "home"
    const val EXPLORER = "explorer"
    const val LIBRARY = "library"
    const val PROFILE = "profile"
    const val CAMERA = "camera"
    const val ANALYSIS = "analysis"
    const val COMPONENT_DETAIL = "component_detail/{componentId}"
    const val MARKETPLACE = "marketplace/{componentId}"
    const val REPAIR_GUIDE = "repair_guide/{componentId}"

    fun componentDetail(componentId: String) = "component_detail/$componentId"
    fun marketplace(componentId: String) = "marketplace/$componentId"
    fun repairGuide(componentId: String) = "repair_guide/$componentId"
}

private data class TopLevelTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory Firebase App Check wiring before UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        // STEP 2: Edge-to-Edge layout
        enableEdgeToEdge()

        setContent {
            PartlyRootApp()
        }
    }
}

@Composable
fun PartlyRootApp(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        PartlyTheme {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        val uid = user.uid
        key(uid) {
            val viewModel: PartlyMainViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        PartlyMainViewModel(
                            firestoreRepo = PartlyFirestoreRepository(db, auth),
                            visionRepo = ProductVisionRepository(),
                            prefsRepo = PreferencesRepository(app),
                            currentUserId = uid,
                            userDisplayName = user.displayName ?: "Partly User",
                            userEmail = user.email ?: "",
                            userPhotoUrl = user.photoUrl?.toString()
                        )
                    }
                }
            )

            val themePref by viewModel.themePreference.collectAsStateWithLifecycle()
            val darkTheme = when (themePref) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }

            PartlyTheme(darkTheme = darkTheme) {
                PartlyAuthenticatedShell(
                    viewModel = viewModel,
                    onSignOut = {
                        signOut(credentialManager = credentialManager, scope = scope)
                    }
                )
            }
        }
    }
}

@Composable
private fun PartlyAuthenticatedShell(
    viewModel: PartlyMainViewModel,
    onSignOut: () -> Unit
) {
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
    val scansState by viewModel.scansState.collectAsStateWithLifecycle()
    val savedItemsState by viewModel.savedItemsState.collectAsStateWithLifecycle()
    val activeScanState by viewModel.activeScanState.collectAsStateWithLifecycle()
    val activeComponentsState by viewModel.activeComponentsState.collectAsStateWithLifecycle()
    val capturedBitmaps by viewModel.capturedBitmaps.collectAsStateWithLifecycle()
    val categoryHint by viewModel.categoryHint.collectAsStateWithLifecycle()
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
    val sessionThumbnails by viewModel.sessionThumbnails.collectAsStateWithLifecycle()
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    if (onboardingCompleted == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (onboardingCompleted == false) {
        OnboardingScreen(onFinished = { viewModel.completeOnboarding() })
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    val tabs = remember {
        listOf(
            TopLevelTab(PartlyRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
            TopLevelTab(PartlyRoutes.EXPLORER, "Explore", Icons.Filled.Layers, Icons.Outlined.Layers),
            TopLevelTab(PartlyRoutes.LIBRARY, "Library", Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks),
            TopLevelTab(PartlyRoutes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
        )
    }

    val showTopLevelNav = currentRoute in setOf(
        PartlyRoutes.HOME,
        PartlyRoutes.EXPLORER,
        PartlyRoutes.LIBRARY,
        PartlyRoutes.PROFILE
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 760.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showTopLevelNav && !useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = PartlyRoutes.HOME,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(PartlyRoutes.HOME) {
                        HomeScreen(
                            scansState = scansState,
                            savedItemsState = savedItemsState,
                            onOpenCamera = { category ->
                                viewModel.clearCapturedBitmaps()
                                viewModel.setCategoryHint(category)
                                navController.navigate(PartlyRoutes.CAMERA)
                            },
                            onGalleryImagePicked = { bmp ->
                                viewModel.clearCapturedBitmaps()
                                viewModel.setCategoryHint(null)
                                viewModel.addCapturedBitmap(bmp)
                                navController.navigate(PartlyRoutes.CAMERA)
                            },
                            onOpenScan = { scanId ->
                                viewModel.selectScan(scanId)
                                navController.navigate(PartlyRoutes.EXPLORER)
                            },
                            onLoadReferenceAssembly = { category ->
                                viewModel.loadVerifiedCategoryReference(category) {
                                    navController.navigate(PartlyRoutes.EXPLORER)
                                }
                            },
                            onOpenLibrary = {
                                navController.navigate(PartlyRoutes.LIBRARY) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }

                    composable(PartlyRoutes.CAMERA) {
                        CameraCaptureScreen(
                            capturedBitmaps = capturedBitmaps,
                            categoryHint = categoryHint,
                            onAddBitmap = { viewModel.addCapturedBitmap(it) },
                            onReplaceBitmap = { idx, bmp -> viewModel.replaceCapturedBitmap(idx, bmp) },
                            onRemoveBitmap = { viewModel.removeCapturedBitmap(it) },
                            onAnalyzeClicked = {
                                navController.navigate(PartlyRoutes.ANALYSIS)
                                viewModel.runProductAnalysis { _ ->
                                    navController.navigate(PartlyRoutes.EXPLORER) {
                                        popUpTo(PartlyRoutes.HOME)
                                    }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(PartlyRoutes.ANALYSIS) {
                        AnalysisScreen(
                            analysisState = analysisState,
                            onRetry = {
                                viewModel.runProductAnalysis { _ ->
                                    navController.navigate(PartlyRoutes.EXPLORER) {
                                        popUpTo(PartlyRoutes.HOME)
                                    }
                                }
                            },
                            onCaptureAnotherAngle = { navController.popBackStack() },
                            onProceedToOverview = { scanId ->
                                viewModel.selectScan(scanId)
                                navController.navigate(PartlyRoutes.EXPLORER) {
                                    popUpTo(PartlyRoutes.HOME)
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(PartlyRoutes.EXPLORER) {
                        val activeScan = (activeScanState as? UiState.Success)?.data
                        val thumb = activeScan?.id?.let { sessionThumbnails[it] }
                        ExplorerScreen(
                            scanState = activeScanState,
                            componentsState = activeComponentsState,
                            thumbnailBitmap = thumb,
                            onToggleComponentVisibility = { scanId, compId, hidden ->
                                viewModel.toggleComponentVisibility(scanId, compId, hidden)
                            },
                            onOpenComponentDetails = { compId ->
                                navController.navigate(PartlyRoutes.componentDetail(compId))
                            },
                            onOpenMarketplace = { compId ->
                                navController.navigate(PartlyRoutes.marketplace(compId))
                            },
                            onOpenRepairGuide = { compId ->
                                navController.navigate(PartlyRoutes.repairGuide(compId))
                            },
                            onSaveProductBookmark = { scan ->
                                viewModel.saveBookmark(
                                    itemType = SavedItemType.PRODUCT,
                                    referenceId = scan.id,
                                    title = "${scan.productBrand} ${scan.productModel}".trim(),
                                    subtitle = "${scan.category} • ${scan.componentCount} components",
                                    category = scan.category,
                                    compatibilityStatus = CompatibilityStatus.VERIFIED_COMPATIBLE
                                )
                            },
                            onCaptureAnotherAngle = {
                                viewModel.clearCapturedBitmaps()
                                viewModel.setCategoryHint(activeScan?.let { ProductCategory.fromRaw(it.category) })
                                navController.navigate(PartlyRoutes.CAMERA)
                            },
                            onBack = {
                                navController.navigate(PartlyRoutes.HOME) {
                                    popUpTo(navController.graph.findStartDestination().id)
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(
                        route = PartlyRoutes.COMPONENT_DETAIL,
                        arguments = listOf(navArgument("componentId") { type = NavType.StringType })
                    ) { entry ->
                        val componentId = entry.arguments?.getString("componentId").orEmpty()
                        val scan = (activeScanState as? UiState.Success)?.data
                        val components = (activeComponentsState as? UiState.Success)?.data.orEmpty()
                        val component = components.firstOrNull { it.id == componentId }
                        if (scan != null && component != null) {
                            ComponentDetailScreen(
                                scan = scan,
                                component = component,
                                onSaveComponentBookmark = {
                                    viewModel.saveBookmark(
                                        itemType = SavedItemType.COMPONENT,
                                        referenceId = "${scan.id}_${component.id}",
                                        title = component.name,
                                        subtitle = "${scan.productBrand} ${scan.productModel} • ${component.dimensions}",
                                        category = scan.category,
                                        compatibilityStatus = CompatibilityStatus.fromRaw(component.compatibilityStatus)
                                    )
                                },
                                onVerifyMeasurement = { dims, status, reason ->
                                    viewModel.updateComponentMeasurement(scan.id, component.id, dims, status, reason)
                                },
                                onOpenMarketplace = {
                                    navController.navigate(PartlyRoutes.marketplace(component.id))
                                },
                                onOpenRepairGuide = {
                                    navController.navigate(PartlyRoutes.repairGuide(component.id))
                                },
                                onBack = { navController.popBackStack() }
                            )
                        } else {
                            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                                NoticeCallout("Component unavailable", "Return to the explorer to select a component.", isWarning = true)
                            }
                        }
                    }

                    composable(
                        route = PartlyRoutes.MARKETPLACE,
                        arguments = listOf(navArgument("componentId") { type = NavType.StringType })
                    ) { entry ->
                        val componentId = entry.arguments?.getString("componentId").orEmpty()
                        val scan = (activeScanState as? UiState.Success)?.data
                        val components = (activeComponentsState as? UiState.Success)?.data.orEmpty()
                        val component = components.firstOrNull { it.id == componentId }
                        if (scan != null && component != null) {
                            MarketplaceScreen(
                                scan = scan,
                                component = component,
                                onSavePartChannel = { channel ->
                                    viewModel.saveBookmark(
                                        itemType = SavedItemType.REPLACEMENT_PART,
                                        referenceId = "${component.categoryKey}_${channel.id}",
                                        title = "${component.name} (${channel.partnerName})",
                                        subtitle = "${channel.originalOrAftermarket} • ${channel.dimensionSpec}",
                                        category = scan.category,
                                        compatibilityStatus = channel.compatibilityStatus,
                                        notes = channel.searchUrl
                                    )
                                },
                                onBack = { navController.popBackStack() }
                            )
                        } else {
                            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                                NoticeCallout("Marketplace unavailable", "Select a component first.", isWarning = true)
                            }
                        }
                    }

                    composable(
                        route = PartlyRoutes.REPAIR_GUIDE,
                        arguments = listOf(navArgument("componentId") { type = NavType.StringType })
                    ) { entry ->
                        val componentId = entry.arguments?.getString("componentId").orEmpty()
                        val scan = (activeScanState as? UiState.Success)?.data
                        val components = (activeComponentsState as? UiState.Success)?.data.orEmpty()
                        val component = components.firstOrNull { it.id == componentId }
                        if (scan != null && component != null) {
                            RepairGuideScreen(
                                scan = scan,
                                component = component,
                                onSaveRepairProject = { guide ->
                                    viewModel.saveBookmark(
                                        itemType = SavedItemType.REPAIR_PROJECT,
                                        referenceId = "${scan.id}_${guide.componentKey}",
                                        title = "${guide.componentName} Repair",
                                        subtitle = "${scan.productBrand} ${scan.productModel} • ${guide.estimatedMinutesRange}",
                                        category = scan.category,
                                        compatibilityStatus = CompatibilityStatus.fromRaw(component.compatibilityStatus)
                                    )
                                },
                                onBack = { navController.popBackStack() }
                            )
                        } else {
                            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                                NoticeCallout("Repair guide unavailable", "Select a component first.", isWarning = true)
                            }
                        }
                    }

                    composable(PartlyRoutes.LIBRARY) {
                        SavedLibraryScreen(
                            scansState = scansState,
                            savedItemsState = savedItemsState,
                            onOpenScan = { scanId ->
                                viewModel.selectScan(scanId)
                                navController.navigate(PartlyRoutes.EXPLORER)
                            },
                            onDeleteScan = { viewModel.deleteScan(it) },
                            onRemoveSavedItem = { viewModel.removeSavedItem(it) }
                        )
                    }

                    composable(PartlyRoutes.PROFILE) {
                        val scansCount = (scansState as? UiState.Success)?.data?.size ?: 0
                        val savedCount = (savedItemsState as? UiState.Success)?.data?.size ?: 0
                        ProfileScreen(
                            displayName = viewModel.userDisplayName,
                            email = viewModel.userEmail,
                            photoUrl = viewModel.userPhotoUrl,
                            scansCount = scansCount,
                            savedCount = savedCount,
                            themePreference = themePreference,
                            onThemeChanged = { viewModel.setThemePreference(it) },
                            onReplayOnboarding = { viewModel.resetOnboarding() },
                            onDeleteAllData = { viewModel.deleteAllUserData() },
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }
    }
}
