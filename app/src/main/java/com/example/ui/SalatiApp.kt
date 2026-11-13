package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.signOut
import com.example.ui.screens.AdhkarQuranScreen
import com.example.ui.screens.HalaqatScreen
import com.example.ui.screens.PrayersScreen
import com.example.ui.screens.ProgressSocialScreen
import com.example.ui.screens.QiblaScreen
import com.example.ui.viewmodel.SalatiViewModel
import com.example.ui.viewmodel.UiStatus
import kotlinx.coroutines.launch

const val ROUTE_PRAYERS = "prayers"
const val ROUTE_QIBLA = "qibla"
const val ROUTE_ADHKAR_QURAN = "adhkar_quran"
const val ROUTE_HALAQAT = "halaqat"
const val ROUTE_PROGRESS = "progress"

private data class SalatiTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val salatiTabs = listOf(
    SalatiTab(ROUTE_PRAYERS, "الصلاة", Icons.Filled.Mosque, Icons.Outlined.Mosque),
    SalatiTab(ROUTE_QIBLA, "القبلة", Icons.Filled.Explore, Icons.Outlined.Explore),
    SalatiTab(ROUTE_ADHKAR_QURAN, "الأذكار والتفسير", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    SalatiTab(ROUTE_HALAQAT, "التحفيظ", Icons.Filled.Groups, Icons.Outlined.Groups),
    SalatiTab(ROUTE_PROGRESS, "إنجازاتي", Icons.Filled.AutoGraph, Icons.Outlined.AutoGraph)
)

@Composable
fun SalatiApp(viewModel: SalatiViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_PRAYERS

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val prayers by viewModel.prayerTimes.collectAsStateWithLifecycle()
    val prayerSource by viewModel.prayerSource.collectAsStateWithLifecycle()
    val isFetchingPrayers by viewModel.isFetchingPrayers.collectAsStateWithLifecycle()
    val countdown by viewModel.nextPrayerCountdown.collectAsStateWithLifecycle()
    val todayLog by viewModel.todayLog.collectAsStateWithLifecycle()
    val hijriData by viewModel.hijriMonthGrid.collectAsStateWithLifecycle()
    val prefs by viewModel.localPrefs.collectAsStateWithLifecycle()
    val qiblaBearing by viewModel.qiblaBearing.collectAsStateWithLifecycle()
    val distanceKm by viewModel.distanceToKaabaKm.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val customSupplications by viewModel.customSupplications.collectAsStateWithLifecycle()
    val selectedSurah by viewModel.selectedSurah.collectAsStateWithLifecycle()
    val remoteSurahs by viewModel.remoteSurahDirectory.collectAsStateWithLifecycle()
    val isLoadingSurah by viewModel.isLoadingSurah.collectAsStateWithLifecycle()
    val myCircles by viewModel.myCircles.collectAsStateWithLifecycle()
    val publicCircles by viewModel.publicCircles.collectAsStateWithLifecycle()
    val selectedCircle by viewModel.selectedCircle.collectAsStateWithLifecycle()
    val circleMembers by viewModel.selectedCircleMembers.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val analytics by viewModel.spiritualAnalytics.collectAsStateWithLifecycle()
    val achievements by viewModel.publicAchievements.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    LaunchedEffect(status) {
        if (status is UiStatus.Message) {
            snackbarHostState.showSnackbar((status as UiStatus.Message).text)
            viewModel.clearStatus()
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        salatiTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.testTag("nav_${tab.route}")
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
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        salatiTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("rail_${tab.route}")
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isOnline) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("offline_status_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "وضع عدم الاتصال • المواقيت والقبلة والأذكار والورد تعمل محلياً",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_PRAYERS,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .widthIn(max = 760.dp)
                    ) {
                        composable(ROUTE_PRAYERS) {
                            PrayersScreen(
                                prayers = prayers,
                                prayerSource = prayerSource,
                                isFetchingPrayers = isFetchingPrayers,
                                countdown = countdown,
                                todayLog = todayLog,
                                hijriData = hijriData,
                                prefs = prefs,
                                onTogglePrayer = viewModel::togglePrayer,
                                onSelectCity = viewModel::selectCity,
                                onGpsLocated = { lat, lon, cityLabel ->
                                    viewModel.updateGpsCoordinates(lat, lon, cityLabel)
                                },
                                onRefreshPrayers = viewModel::refreshPrayerTimes,
                                onSelectMethod = viewModel::setCalculationMethod,
                                onToggleAlert = viewModel::togglePrayerAlert,
                                onSetNotifyAtPrayerStart = viewModel::setNotifyAtPrayerStart,
                                onSetLeadMinutes = viewModel::setAlertLeadMinutes,
                                onSetHijriAdjustment = viewModel::setHijriAdjustment
                            )
                        }
                        composable(ROUTE_QIBLA) {
                            QiblaScreen(
                                latitude = prefs.latitude,
                                longitude = prefs.longitude,
                                cityName = prefs.cityName,
                                qiblaBearing = qiblaBearing,
                                distanceKm = distanceKm,
                                onSelectCity = viewModel::selectCity,
                                onGpsLocated = { lat, lon, cityLabel ->
                                    viewModel.updateGpsCoordinates(lat, lon, cityLabel)
                                }
                            )
                        }
                        composable(ROUTE_ADHKAR_QURAN) {
                            AdhkarQuranScreen(
                                todayLog = todayLog,
                                bookmarks = bookmarks,
                                customSupplications = customSupplications,
                                selectedSurah = selectedSurah,
                                remoteSurahs = remoteSurahs,
                                isLoadingSurah = isLoadingSurah,
                                onIncrementTasbih = viewModel::incrementTasbih,
                                onToggleAdhkarPeriod = viewModel::toggleAdhkarPeriod,
                                onToggleBookmark = viewModel::toggleBookmark,
                                onAddCustomSupplication = viewModel::addCustomSupplication,
                                onDeleteCustomSupplication = viewModel::deleteCustomSupplication,
                                onSelectBundledSurah = viewModel::selectBundledSurah,
                                onLoadRemoteSurah = viewModel::loadSurahByNumber,
                                onNotify = { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            )
                        }
                        composable(ROUTE_HALAQAT) {
                            HalaqatScreen(
                                currentUserId = viewModel.currentUserId,
                                isGuestMode = viewModel.isGuestMode,
                                todayLog = todayLog,
                                myCircles = myCircles,
                                publicCircles = publicCircles,
                                selectedCircle = selectedCircle,
                                circleMembers = circleMembers,
                                onUpdateWird = viewModel::updateWird,
                                onSelectCircle = viewModel::selectCircle,
                                onCreateCircle = viewModel::createCircle,
                                onJoinCircle = viewModel::joinCircle,
                                onLogMemorizedVerses = viewModel::logMyMemorizedVerses,
                                onShareWirdAchievement = { pages ->
                                    viewModel.shareAchievement(
                                        badgeTitle = "إتمام الورد اليومي",
                                        description = "بفضل الله أتممت وردي اليوم بقراءة $pages صفحات من القرآن الكريم.",
                                        category = "QURAN"
                                    )
                                },
                                onRequestGoogleSignIn = viewModel::exitGuestToSignIn
                            )
                        }
                        composable(ROUTE_PROGRESS) {
                            ProgressSocialScreen(
                                isGuestMode = viewModel.isGuestMode,
                                userProfile = userProfile,
                                analytics = analytics,
                                achievements = achievements,
                                onShareAchievement = viewModel::shareAchievement,
                                onSendAmeen = viewModel::sendAmeen,
                                onRequestGoogleSignIn = viewModel::exitGuestToSignIn,
                                onSignOut = {
                                    signOut(credentialManager = credentialManager, scope = scope)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
