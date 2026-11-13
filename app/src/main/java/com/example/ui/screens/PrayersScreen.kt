package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.DailyLog
import com.example.data.network.PrayerTimeSource
import com.example.data.network.PrayerTimesService
import com.example.data.preferences.LocalPrayerPreferences
import com.example.domain.CalculationMethod
import com.example.domain.CityCoordinate
import com.example.domain.HijriDateInfo
import com.example.domain.HijriMonthDay
import com.example.domain.IslamicCities
import com.example.domain.PrayerTimeEntry
import com.example.domain.PrayerType
import com.example.notifications.PrayerNotificationManager
import com.example.ui.theme.MedinaGold
import com.example.ui.theme.MedinaGoldLight
import com.example.ui.theme.SuccessJade
import com.example.ui.viewmodel.NextPrayerCountdown
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayersScreen(
    prayers: List<PrayerTimeEntry>,
    prayerSource: PrayerTimeSource,
    isFetchingPrayers: Boolean,
    countdown: NextPrayerCountdown?,
    todayLog: DailyLog,
    hijriData: Pair<HijriDateInfo, List<HijriMonthDay>>,
    prefs: LocalPrayerPreferences,
    onTogglePrayer: (PrayerType) -> Unit,
    onSelectCity: (CityCoordinate) -> Unit,
    onGpsLocated: (Double, Double, String) -> Unit,
    onRefreshPrayers: () -> Unit,
    onSelectMethod: (CalculationMethod, Boolean) -> Unit,
    onToggleAlert: (String, Boolean) -> Unit,
    onSetNotifyAtPrayerStart: (Boolean) -> Unit,
    onSetLeadMinutes: (Int) -> Unit,
    onSetHijriAdjustment: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showLocationSheet by remember { mutableStateOf(false) }
    var showAlertsSheet by remember { mutableStateOf(false) }
    var showHijriSheet by remember { mutableStateOf(false) }
    var locationNotice by remember { mutableStateOf<String?>(null) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            fetchCurrentLocation(context, scope, onGpsLocated) { msg -> locationNotice = msg }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            fetchCurrentLocation(context, scope, onGpsLocated) { msg -> locationNotice = msg }
        } else {
            locationNotice = "يمكنك اختيار مدينتك يدوياً من القائمة أدناه"
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (granted) {
            onRefreshPrayers()
            PrayerNotificationManager.showNotificationNow(
                context,
                "صلاتي • تم تفعيل إشعارات الصلاة",
                "سيصلك تنبيه محلي فور دخول وقت كل صلاة حسب موقعك"
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Countdown & Location Card
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(235.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_prayer_hero),
                        contentDescription = "خلفية مواقيت الصلاة",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x88041C16),
                                        Color(0xDD062E24)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .clickable { showLocationSheet = true }
                                    .testTag("city_selector_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = "الموقع",
                                        tint = MedinaGoldLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = prefs.cityName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.16f),
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clickable { showHijriSheet = true }
                                        .testTag("hijri_calendar_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.CalendarMonth,
                                            contentDescription = "التقويم الهجري",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.16f),
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clickable { showAlertsSheet = true }
                                        .testTag("alerts_settings_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.NotificationsActive,
                                            contentDescription = "تنبيهات الصلاة",
                                            tint = MedinaGoldLight,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = countdown?.let { "الصلاة القادمة: ${it.nextPrayer.type.arabicName}" }
                                    ?: "مواقيت الصلاة اليوم",
                                style = MaterialTheme.typography.titleMedium,
                                color = MedinaGoldLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = countdown?.let {
                                    "%02d:%02d:%02d".format(
                                        it.remainingHours,
                                        it.remainingMinutes,
                                        it.remainingSeconds
                                    )
                                } ?: "--:--:--",
                                style = MaterialTheme.typography.displayLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = countdown?.let { "الموعد عند ${it.nextPrayer.formattedTime}" } ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Black.copy(alpha = 0.28f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showHijriSheet = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${hijriData.first.dayOfWeekArabic}، ${hijriData.first.formattedArabic}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = hijriData.first.eventTitle ?: "عرض التقويم الهجري",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MedinaGoldLight
                                )
                            }
                        }
                    }
                }
            }
        }

        // Location & Notification Quick Permission Cards
        if (!hasLocationPermission || !hasNotificationPermission) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!hasLocationPermission) {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("تحديد الموقع التلقائي", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "فعّل الموقع لجلب مواقيت الصلاة الدقيقة لمدينتك الحالية",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                                Button(
                                    onClick = {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                                Manifest.permission.ACCESS_FINE_LOCATION
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("grant_location_button")
                                ) {
                                    Text("تفعيل")
                                }
                            }
                        }
                    }
                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("تنبيهات دخول وقت الصلاة", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "اسمح بالإشعارات لتنبيهك فور حلول الفجر والظهر والعصر والمغرب والعشاء",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                                Button(
                                    onClick = {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("grant_notifications_button")
                                ) {
                                    Text("السماح")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daily Prayer Check-In Summary & Live Sync Status
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "متابعة صلوات اليوم",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "أنجزت ${todayLog.completedPrayersCount} من ٥ فروض اليوم",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CloudDone,
                                    contentDescription = null,
                                    tint = if (prayerSource == PrayerTimeSource.LIVE_API) SuccessJade else MedinaGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${prayerSource.arabicLabel} • ${prefs.cityName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = onRefreshPrayers,
                                modifier = Modifier.testTag("refresh_prayers_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "تحديث مواقيت الصلاة",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "${(todayLog.completedPrayersCount * 20)}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    if (isFetchingPrayers) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        // Prayer Time Cards
        items(prayers, key = { it.type.key }) { entry ->
            val isNext = countdown?.nextPrayer?.type == entry.type
            val isDone = when (entry.type) {
                PrayerType.FAJR -> todayLog.fajrDone
                PrayerType.DHUHR -> todayLog.dhuhrDone
                PrayerType.ASR -> todayLog.asrDone
                PrayerType.MAGHRIB -> todayLog.maghribDone
                PrayerType.ISHA -> todayLog.ishaDone
                PrayerType.SUNRISE -> false
            }
            val alertEnabled = entry.type != PrayerType.SUNRISE && prefs.isEnabledFor(entry.type.key)

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNext) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isNext) 3.dp else 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = entry.type.firestoreField != null) {
                        onTogglePrayer(entry.type)
                    }
                    .testTag("prayer_card_${entry.type.key}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (entry.type.firestoreField != null) {
                            IconButton(
                                onClick = { onTogglePrayer(entry.type) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("toggle_prayer_${entry.type.key}")
                            ) {
                                Icon(
                                    imageVector = if (isDone) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                    contentDescription = if (isDone) "تمت الصلاة" else "تحديد كـ مصلاة",
                                    tint = if (isDone) SuccessJade else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WbSunny,
                                    contentDescription = "الشروق",
                                    tint = MedinaGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = entry.type.arabicName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isNext) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "القادمة",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            if (entry.type != PrayerType.SUNRISE) {
                                val alertSummary = when {
                                    !alertEnabled -> "التنبيه صامت"
                                    prefs.notifyAtPrayerStart && prefs.leadMinutes > 0 ->
                                        "تنبيه عند الأذان وقبلها بـ ${prefs.leadMinutes} د"
                                    prefs.notifyAtPrayerStart -> "تنبيه عند دخول الوقت"
                                    prefs.leadMinutes > 0 -> "تنبيه قبل الأذان بـ ${prefs.leadMinutes} د"
                                    else -> "التنبيه صامت"
                                }
                                Text(
                                    text = alertSummary,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = entry.formattedTime,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        if (entry.type != PrayerType.SUNRISE) {
                            IconButton(
                                onClick = { onToggleAlert(entry.type.key, !alertEnabled) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (alertEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                                    contentDescription = "تبديل تنبيه ${entry.type.arabicName}",
                                    tint = if (alertEnabled) MedinaGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Offline Astronomical Night Prayer Guide (Islamic Midnight & Last Third of the Night)
        val maghribTime = prayers.firstOrNull { it.type == PrayerType.MAGHRIB }
        val fajrTime = prayers.firstOrNull { it.type == PrayerType.FAJR }
        if (maghribTime != null && fajrTime != null) {
            item {
                val maghribMs = maghribTime.timestampMillis
                val nextFajrMs = if (fajrTime.timestampMillis <= maghribMs) {
                    fajrTime.timestampMillis + 86_400_000L
                } else {
                    fajrTime.timestampMillis
                }
                val nightDurationMs = (nextFajrMs - maghribMs).coerceAtLeast(3_600_000L)
                val midnightMs = maghribMs + nightDurationMs / 2L
                val lastThirdMs = nextFajrMs - nightDurationMs / 3L
                val formatter = remember { SimpleDateFormat("hh:mm a", Locale("ar")) }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("night_prayer_guide_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NightsStay,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("أوقات القيام والتهجد (حساب فلكي محلي)", style = MaterialTheme.typography.titleMedium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("منتصف الليل الشرعي", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatter.format(Date(midnightMs)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("بداية الثلث الأخير", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatter.format(Date(lastThirdMs)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Location & Calculation Method Sheet
    if (showLocationSheet) {
        ModalBottomSheet(onDismissRequest = { showLocationSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "الموقع وطريقة الحساب",
                    style = MaterialTheme.typography.headlineSmall
                )
                Button(
                    onClick = {
                        val fineGranted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val coarseGranted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (fineGranted || coarseGranted) {
                            fetchCurrentLocation(context, scope, onGpsLocated) { msg -> locationNotice = msg }
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("تحديد موقعي تلقائياً (GPS)")
                }

                AnimatedVisibility(visible = locationNotice != null) {
                    Text(
                        text = locationNotice.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text("أو اختر مدينة:", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(IslamicCities.presets) { city ->
                        FilterChip(
                            selected = prefs.cityName == city.name,
                            onClick = { onSelectCity(city) },
                            label = { Text(city.name) }
                        )
                    }
                }

                HorizontalDivider()
                Text("طريقة حساب المواقيت:", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CalculationMethod.entries) { method ->
                        FilterChip(
                            selected = prefs.calculationMethod == method.id,
                            onClick = { onSelectMethod(method, prefs.hanafiAsr) },
                            label = { Text(method.arabicName) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المذهب الحنفي لصلاة العصر", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = prefs.hanafiAsr,
                        onCheckedChange = {
                            onSelectMethod(CalculationMethod.fromId(prefs.calculationMethod), it)
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Custom Notifications Sheet
    if (showAlertsSheet) {
        ModalBottomSheet(onDismissRequest = { showAlertsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("تنبيهات الصلاة المخصصة", style = MaterialTheme.typography.headlineSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("إشعار عند دخول وقت الصلاة", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "إرسال إشعار محلي تلقائي فور حلول كل صلاة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = prefs.notifyAtPrayerStart,
                        onCheckedChange = onSetNotifyAtPrayerStart
                    )
                }
                Text(
                    text = "تذكير مسبق قبل الأذان (اختياري):",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 5, 10, 15).forEach { mins ->
                        FilterChip(
                            selected = prefs.leadMinutes == mins,
                            onClick = { onSetLeadMinutes(mins) },
                            label = { Text(if (mins == 0) "بدون" else "قبل $mins د") }
                        )
                    }
                }

                listOf(
                    PrayerType.FAJR,
                    PrayerType.DHUHR,
                    PrayerType.ASR,
                    PrayerType.MAGHRIB,
                    PrayerType.ISHA
                ).forEach { prayer ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تنبيه صلاة ${prayer.arabicName}", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = prefs.isEnabledFor(prayer.key),
                            onCheckedChange = { onToggleAlert(prayer.key, it) }
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            PrayerNotificationManager.showNotificationNow(
                                context,
                                "صلاتي • تنبيه تجريبي",
                                "تعمل تنبيهات الصلاة بنجاح! الموعد القادم: ${countdown?.nextPrayer?.type?.arabicName ?: "الفجر"}"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("إرسال تنبيه تجريبي الآن")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Hijri Calendar Sheet
    if (showHijriSheet) {
        ModalBottomSheet(onDismissRequest = { showHijriSheet = false }) {
            val (todayInfo, days) = hijriData
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "التقويم الهجري • ${todayInfo.monthName} ${todayInfo.year} هـ",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "اليوم: ${todayInfo.formattedArabic}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onSetHijriAdjustment(prefs.hijriAdjustment - 1) }) {
                            Icon(Icons.Filled.Remove, contentDescription = "إنقاص يوم")
                        }
                        Text(
                            text = "${if (prefs.hijriAdjustment >= 0) "+" else ""}${prefs.hijriAdjustment}",
                            style = MaterialTheme.typography.labelLarge
                        )
                        IconButton(onClick = { onSetHijriAdjustment(prefs.hijriAdjustment + 1) }) {
                            Icon(Icons.Filled.Add, contentDescription = "زيادة يوم")
                        }
                    }
                }

                // Grid of Hijri Month Days (6 cols x 5 rows)
                val rows = days.chunked(6)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rows.forEach { rowDays ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowDays.forEach { dayItem ->
                                val bg = when {
                                    dayItem.isToday -> MaterialTheme.colorScheme.primary
                                    dayItem.isWhiteDay -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                }
                                val fg = if (dayItem.isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = bg,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "${dayItem.hijriDay}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = fg,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${dayItem.gregorianDay}/${dayItem.gregorianMonth}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = fg.copy(alpha = 0.72f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            repeat(6 - rowDays.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(16.dp)
                    ) {}
                    Text(
                        text = "الأيام البيض (١٣-١٥) • يسن صيامها",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                val monthEvents = days.mapNotNull { d -> d.eventTitle?.let { "${d.hijriDay} ${todayInfo.monthName}: $it" } }.distinct()
                if (monthEvents.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.NightsStay, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text("مناسبات الشهر المبارك", style = MaterialTheme.typography.titleMedium)
                            }
                            monthEvents.take(4).forEach { ev ->
                                Text("• $ev", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

private fun fetchCurrentLocation(
    context: android.content.Context,
    scope: CoroutineScope,
    onLocated: (Double, Double, String) -> Unit,
    onMessage: (String) -> Unit
) {
    try {
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    scope.launch {
                        val cityLabel = PrayerTimesService.resolveCityNameFromCoordinates(
                            context,
                            loc.latitude,
                            loc.longitude
                        )
                        onLocated(loc.latitude, loc.longitude, cityLabel)
                        onMessage("تم تحديث المواقيت حسب موقعك: $cityLabel")
                    }
                } else {
                    onMessage("تعذر التقاط إشارة الموقع حالياً، يمكنك اختيار المدينة يدوياً")
                }
            }
            .addOnFailureListener {
                onMessage("تعذر الوصول لخدمة الموقع")
            }
    } catch (_: SecurityException) {
        onMessage("يرجى منح صلاحية الموقع أولاً")
    }
}
