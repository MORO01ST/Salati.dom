package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.network.PrayerTimesService
import com.example.domain.CityCoordinate
import com.example.domain.IslamicCities
import com.example.sensors.CompassReading
import com.example.sensors.CompassSensorManager
import com.example.ui.theme.MedinaGold
import com.example.ui.theme.SanctuaryEmerald
import com.example.ui.theme.SuccessJade
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun QiblaScreen(
    latitude: Double,
    longitude: Double,
    cityName: String,
    qiblaBearing: Double,
    distanceKm: Int,
    onSelectCity: (CityCoordinate) -> Unit,
    onGpsLocated: (Double, Double, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()

    val sensorManager = remember(context) { CompassSensorManager(context) }
    val reading by sensorManager.observeAzimuth(latitude, longitude)
        .collectAsStateWithLifecycle(initialValue = CompassReading())

    var showCalibrationDialog by remember { mutableStateOf(false) }
    var locationNotice by remember { mutableStateOf<String?>(null) }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            locateUserForQibla(context, scope, onGpsLocated) { msg -> locationNotice = msg }
        } else {
            locationNotice = "اختر أقرب مدينة أدناه لحساب زاوية القبلة بدقة"
        }
    }

    val rawDiff = ((qiblaBearing.toFloat() - reading.azimuthDegrees + 540f) % 360f) - 180f
    val isAligned = reading.hasSensors && abs(rawDiff) <= 5f
    var wasAligned by remember { mutableStateOf(false) }

    LaunchedEffect(isAligned) {
        if (isAligned && !wasAligned) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        wasAligned = isAligned
    }

    val tiltMagnitudeDeg = hypot(reading.pitchDegrees.toDouble(), reading.rollDegrees.toDouble()).toFloat()
    val isPhoneLevel = tiltMagnitudeDeg <= 22f
    val hasMagneticInterference = reading.magneticFieldMicroTesla > 85f || reading.magneticFieldMicroTesla < 15f

    val animatedDialRotation by animateFloatAsState(
        targetValue = -reading.azimuthDegrees,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
        label = "dialRotation"
    )
    val animatedNeedleRotation by animateFloatAsState(
        targetValue = qiblaBearing.toFloat() - reading.azimuthDegrees,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 220f),
        label = "needleRotation"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MedinaGold
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val activeAlignedColor = SuccessJade
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "بوصلة اتجاه القبلة",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "تعمل بدقة كاملة دون الحاجة للإنترنت",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            OutlinedButton(
                onClick = { showCalibrationDialog = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("qibla_calibration_button")
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("المعايرة")
            }
        }

        // Location Badge + GPS Trigger
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "المدينة",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "$cityName • المسافة للكعبة $distanceKm كم",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = String.format(Locale.US, "الإحداثيات: %.3f° N, %.3f° E", latitude, longitude),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                            )
                        }
                    }
                    Button(
                        onClick = {
                            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (fine || coarse) {
                                locateUserForQibla(context, scope, onGpsLocated) { msg -> locationNotice = msg }
                            } else {
                                locationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("qibla_gps_button")
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("موقعي")
                    }
                }
                if (locationNotice != null) {
                    Text(
                        text = locationNotice!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(IslamicCities.presets.take(8)) { city ->
                        FilterChip(
                            selected = cityName == city.name,
                            onClick = { onSelectCity(city) },
                            label = { Text(city.name) }
                        )
                    }
                }
            }
        }

        // Compass Dial Canvas with Cardinal Letters & Tilt Bubble
        Box(
            modifier = Modifier
                .widthIn(max = 330.dp)
                .fillMaxWidth(0.86f)
                .aspectRatio(1f)
                .testTag("qibla_compass_dial"),
            contentAlignment = Alignment.Center
        ) {
            val cardinalStyle = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = onSurfaceColor
            )
            val northStyle = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFD32F2F)
            )
            val northText = remember(textMeasurer, northStyle) { textMeasurer.measure("ش", northStyle) }
            val eastText = remember(textMeasurer, cardinalStyle) { textMeasurer.measure("ق", cardinalStyle) }
            val southText = remember(textMeasurer, cardinalStyle) { textMeasurer.measure("ج", cardinalStyle) }
            val westText = remember(textMeasurer, cardinalStyle) { textMeasurer.measure("غ", cardinalStyle) }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f * 0.88f

                // Outer Aura Ring
                drawCircle(
                    color = if (isAligned) activeAlignedColor.copy(alpha = 0.16f) else surfaceVariantColor.copy(alpha = 0.45f),
                    radius = radius * 1.07f,
                    center = center
                )

                // Compass Rim
                drawCircle(
                    color = if (isAligned) activeAlignedColor else primaryColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 8f)
                )

                // Fixed Phone Heading Marker at Top
                val topMarker = Path().apply {
                    moveTo(center.x, center.y - radius - 14f)
                    lineTo(center.x - 10f, center.y - radius - 28f)
                    lineTo(center.x + 10f, center.y - radius - 28f)
                    close()
                }
                drawPath(path = topMarker, color = if (isAligned) activeAlignedColor else MedinaGold)

                // Rotating Dial Ticks + Arabic Cardinal Points
                rotate(degrees = animatedDialRotation, pivot = center) {
                    for (deg in 0 until 360 step 10) {
                        val isCardinal = deg % 90 == 0
                        val tickLen = if (isCardinal) 22f else if (deg % 30 == 0) 14f else 8f
                        val rad = Math.toRadians((deg - 90).toDouble())
                        val start = Offset(
                            center.x + (radius - tickLen) * cos(rad).toFloat(),
                            center.y + (radius - tickLen) * sin(rad).toFloat()
                        )
                        val end = Offset(
                            center.x + radius * cos(rad).toFloat(),
                            center.y + radius * sin(rad).toFloat()
                        )
                        drawLine(
                            color = if (deg == 0) Color(0xFFD32F2F) else primaryColor.copy(alpha = 0.65f),
                            start = start,
                            end = end,
                            strokeWidth = if (isCardinal) 5f else 2.5f,
                            cap = StrokeCap.Round
                        )
                    }

                    val labelRadius = radius - 42f
                    val cardinals = listOf(
                        0 to northText,
                        90 to eastText,
                        180 to southText,
                        270 to westText
                    )
                    cardinals.forEach { (deg, layout) ->
                        val rad = Math.toRadians((deg - 90).toDouble())
                        val lx = center.x + labelRadius * cos(rad).toFloat() - layout.size.width / 2f
                        val ly = center.y + labelRadius * sin(rad).toFloat() - layout.size.height / 2f
                        drawText(layout, topLeft = Offset(lx, ly))
                    }
                }

                // Inner Level Ring & Tilt Bubble
                val levelRadius = radius * 0.24f
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.35f),
                    radius = radius * 0.65f,
                    center = center,
                    style = Stroke(width = 3f)
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.25f),
                    radius = levelRadius,
                    center = center,
                    style = Stroke(width = 2.5f)
                )
                val bubbleOffsetX = (reading.rollDegrees / 45f).coerceIn(-1f, 1f) * (levelRadius * 0.75f)
                val bubbleOffsetY = (-reading.pitchDegrees / 45f).coerceIn(-1f, 1f) * (levelRadius * 0.75f)
                drawCircle(
                    color = if (isPhoneLevel) activeAlignedColor.copy(alpha = 0.55f) else secondaryColor.copy(alpha = 0.75f),
                    radius = 10f,
                    center = Offset(center.x + bubbleOffsetX, center.y + bubbleOffsetY)
                )

                // Qibla Pointer Needle
                rotate(degrees = animatedNeedleRotation, pivot = center) {
                    val tip = Offset(center.x, center.y - radius * 0.78f)
                    val leftBase = Offset(center.x - 18f, center.y)
                    val rightBase = Offset(center.x + 18f, center.y)
                    val needlePath = Path().apply {
                        moveTo(tip.x, tip.y)
                        lineTo(leftBase.x, leftBase.y)
                        lineTo(rightBase.x, rightBase.y)
                        close()
                    }
                    drawPath(
                        path = needlePath,
                        color = if (isAligned) activeAlignedColor else secondaryColor
                    )

                    // Kaaba Emblem at tip
                    drawCircle(
                        color = if (isAligned) activeAlignedColor else SanctuaryEmerald,
                        radius = 18f,
                        center = Offset(center.x, center.y - radius * 0.80f)
                    )

                    // Tail
                    drawLine(
                        color = primaryColor.copy(alpha = 0.4f),
                        start = center,
                        end = Offset(center.x, center.y + radius * 0.44f),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }

                // Center Pivot
                drawCircle(color = primaryColor, radius = 12f, center = center)
                drawCircle(color = Color.White, radius = 5f, center = center)
            }
        }

        // Alignment & Sensor Telemetry Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isAligned) {
                    SuccessJade.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when {
                            !reading.hasSensors -> Icons.Filled.WarningAmber
                            isAligned -> Icons.Filled.CheckCircle
                            else -> Icons.Filled.Explore
                        },
                        contentDescription = null,
                        tint = if (isAligned) SuccessJade else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = when {
                            !reading.hasSensors -> "مستشعر البوصلة غير متوفر على هذا الجهاز"
                            isAligned -> "أنت الآن متجه نحو الكعبة المشرفة"
                            rawDiff > 0 -> "أدر الهاتف لليمين بمقدار ${abs(rawDiff).roundToInt()}°"
                            else -> "أدر الهاتف لليسار بمقدار ${abs(rawDiff).roundToInt()}°"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isAligned) SuccessJade else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("زاوية القبلة", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${qiblaBearing.roundToInt()}° (${arabicCardinalDirection(qiblaBearing)})",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("اتجاه الهاتف", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${reading.azimuthDegrees.roundToInt()}°",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Sensor source, level status & magnetic field health
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Sensors, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "المصدر: ${reading.sensorModeLabel}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Text(
                            text = if (isPhoneLevel) {
                                "ميلان الهاتف: مستوٍ (${tiltMagnitudeDeg.roundToInt()}°) • قراءة مستقرة"
                            } else {
                                "ميلان الهاتف: ${tiltMagnitudeDeg.roundToInt()}° — يُفضل وضع الهاتف أفقياً لدقة أعلى"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPhoneLevel) SuccessJade else MedinaGold
                        )
                        Text(
                            text = if (hasMagneticInterference) {
                                "المجال المغناطيسي: ${reading.magneticFieldMicroTesla.roundToInt()} µT (تشويش قريب — ابتعد عن المعادن)"
                            } else {
                                "المجال المغناطيسي: ${reading.magneticFieldMicroTesla.roundToInt()} µT (طبيعي)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasMagneticInterference) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (reading.accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                    Text(
                        text = "دقة المستشعر منخفضة — حرّك الهاتف على شكل رقم 8 في الهواء لإعادة المعايرة.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showCalibrationDialog) {
        AlertDialog(
            onDismissRequest = { showCalibrationDialog = false },
            title = { Text("معايرة مستشعر البوصلة") },
            text = {
                Text(
                    "للحصول على أدق اتجاه نحو الكعبة المشرفة:\n\n" +
                        "١. حرّك الهاتف في الهواء على شكل الرقم (8) عدة مرات.\n" +
                        "٢. ضع الهاتف بشكل مسطح وأفقي بعيداً عن الأغلفة المغناطيسية أو الأجسام المعدنية.\n" +
                        "٣. تأكد من تحديد مدينتك أو تفعيل الموقع (GPS) لحساب زاوية القبلة والانحراف المغناطيسي بدقة.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showCalibrationDialog = false }) {
                    Text("حسناً")
                }
            }
        )
    }
}

private fun arabicCardinalDirection(bearingDeg: Double): String {
    val norm = ((bearingDeg % 360.0) + 360.0) % 360.0
    return when {
        norm < 22.5 || norm >= 337.5 -> "شمال"
        norm < 67.5 -> "شمال شرق"
        norm < 112.5 -> "شرق"
        norm < 157.5 -> "جنوب شرق"
        norm < 202.5 -> "جنوب"
        norm < 247.5 -> "جنوب غرب"
        norm < 292.5 -> "غرب"
        else -> "شمال غرب"
    }
}

private fun locateUserForQibla(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onGpsLocated: (Double, Double, String) -> Unit,
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
                        onGpsLocated(loc.latitude, loc.longitude, cityLabel)
                        onMessage("تم تحديد موقعك: $cityLabel")
                    }
                } else {
                    onMessage("تعذر التقاط الموقع حالياً، يمكنك اختيار المدينة أدناه")
                }
            }
            .addOnFailureListener {
                onMessage("تعذر الوصول لخدمة الموقع")
            }
    } catch (_: SecurityException) {
        onMessage("يرجى منح صلاحية الموقع أولاً")
    }
}
