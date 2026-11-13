package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.data.network.NetworkStatusMonitor
import com.example.data.preferences.LocalSalatiStore
import com.example.data.preferences.NotificationPrefsStore
import com.example.data.repository.SalatiRepository
import com.example.notifications.PrayerNotificationManager
import com.example.ui.SalatiApp
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.SalatiTheme
import com.example.ui.viewmodel.SalatiViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PrayerNotificationManager.ensureChannel(this)

        setContent {
            SalatiTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localStore = remember(context) { LocalSalatiStore(context.applicationContext) }
    val isGuestMode by localStore.isGuestMode.collectAsStateWithLifecycle(initialValue = false)

    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null && !isGuestMode) {
        AuthScreen(
            onAuthSuccess = {
                scope.launch { localStore.setGuestMode(false) }
            },
            onSkipSignIn = {
                scope.launch { localStore.setGuestMode(true) }
            }
        )
    } else {
        val isGuest = user == null
        val uid = user?.uid ?: SalatiViewModel.GUEST_USER_ID
        val displayName = user?.displayName ?: "زائر صلاتي"
        key(uid, isGuest) {
            val viewModel: SalatiViewModel = viewModel(
                key = "${uid}_$isGuest",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val repository = if (isGuest) {
                            null
                        } else {
                            val databaseId = app.getString(R.string.firestore_database_id)
                            val db = FirebaseFirestore.getInstance(databaseId)
                            SalatiRepository(db, Firebase.auth)
                        }
                        SalatiViewModel(
                            repository = repository,
                            localStore = LocalSalatiStore(app),
                            prefsStore = NotificationPrefsStore(app),
                            networkMonitor = NetworkStatusMonitor(app),
                            isGuestMode = isGuest,
                            currentUserId = uid,
                            initialDisplayName = displayName,
                            onScheduleAlarms = { todayPrayers, tomorrowPrayers, prefs ->
                                PrayerNotificationManager.scheduleUpcomingPrayers(
                                    context = app,
                                    todayPrayers = todayPrayers,
                                    tomorrowPrayers = tomorrowPrayers,
                                    prefs = prefs
                                )
                            }
                        )
                    }
                }
            )
            SalatiApp(viewModel = viewModel)
        }
    }
}
