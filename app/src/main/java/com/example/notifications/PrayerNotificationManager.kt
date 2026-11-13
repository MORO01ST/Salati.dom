package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.network.PrayerTimesService
import com.example.data.preferences.LocalPrayerPreferences
import com.example.data.preferences.NotificationPrefsStore
import com.example.domain.CalculationMethod
import com.example.domain.PrayerTimeEntry
import com.example.domain.PrayerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

object PrayerNotificationManager {
    const val CHANNEL_ID = "salati_prayer_alerts"
    const val EXTRA_TITLE = "extra_prayer_title"
    const val EXTRA_BODY = "extra_prayer_body"
    const val EXTRA_NOTIF_ID = "extra_notif_id"

    private const val TODAY_ADHAN_BASE_CODE = 4000
    private const val TOMORROW_ADHAN_BASE_CODE = 4100
    private const val TODAY_REMINDER_BASE_CODE = 4200
    private const val TOMORROW_REMINDER_BASE_CODE = 4300

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "تنبيهات مواقيت الصلاة",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "إشعارات محلية عند دخول وقت كل صلاة وتنبيهات التذكير المسبقة"
                    enableVibration(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun scheduleUpcomingPrayers(
        context: Context,
        todayPrayers: List<PrayerTimeEntry>,
        tomorrowPrayers: List<PrayerTimeEntry>,
        prefs: LocalPrayerPreferences,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        scheduleDayPrayers(
            context = context,
            alarmManager = alarmManager,
            prayers = todayPrayers,
            prefs = prefs,
            adhanBaseCode = TODAY_ADHAN_BASE_CODE,
            reminderBaseCode = TODAY_REMINDER_BASE_CODE,
            nowMillis = nowMillis
        )
        scheduleDayPrayers(
            context = context,
            alarmManager = alarmManager,
            prayers = tomorrowPrayers,
            prefs = prefs,
            adhanBaseCode = TOMORROW_ADHAN_BASE_CODE,
            reminderBaseCode = TOMORROW_REMINDER_BASE_CODE,
            nowMillis = nowMillis
        )
    }

    private fun scheduleDayPrayers(
        context: Context,
        alarmManager: AlarmManager,
        prayers: List<PrayerTimeEntry>,
        prefs: LocalPrayerPreferences,
        adhanBaseCode: Int,
        reminderBaseCode: Int,
        nowMillis: Long
    ) {
        val obligatoryPrayers = prayers.filter { it.type != PrayerType.SUNRISE }
        val leadMs = prefs.leadMinutes * 60_000L

        obligatoryPrayers.forEachIndexed { idx, entry ->
            val enabled = prefs.isEnabledFor(entry.type.key)

            // 1. Notification right when each prayer time begins
            val startReqCode = adhanBaseCode + idx
            val startPending = buildPendingIntent(
                context = context,
                requestCode = startReqCode,
                title = "صلاتي • حان وقت صلاة ${entry.type.arabicName}",
                body = "حان الآن موعد صلاة ${entry.type.arabicName} (${entry.formattedTime}) في ${prefs.cityName}"
            )
            if (enabled && prefs.notifyAtPrayerStart && entry.timestampMillis > nowMillis) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    entry.timestampMillis,
                    startPending
                )
            } else {
                alarmManager.cancel(startPending)
            }

            // 2. Optional advance reminder before prayer time
            val reminderReqCode = reminderBaseCode + idx
            val reminderPending = buildPendingIntent(
                context = context,
                requestCode = reminderReqCode,
                title = "صلاتي • اقترب موعد صلاة ${entry.type.arabicName}",
                body = "متبقي ${prefs.leadMinutes} دقيقة على أذان ${entry.type.arabicName} (${entry.formattedTime})"
            )
            val reminderTriggerAt = entry.timestampMillis - leadMs
            if (enabled && prefs.leadMinutes > 0 && reminderTriggerAt > nowMillis) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTriggerAt,
                    reminderPending
                )
            } else {
                alarmManager.cancel(reminderPending)
            }
        }
    }

    private fun buildPendingIntent(
        context: Context,
        requestCode: Int,
        title: String,
        body: String
    ): PendingIntent {
        val intent = Intent(context, PrayerNotificationReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_NOTIF_ID, requestCode)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showNotificationNow(context: Context, title: String, body: String, notifId: Int = 9901) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPending = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(tapPending)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notification)
    }
}

class PrayerNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(PrayerNotificationManager.EXTRA_TITLE)
            ?: "صلاتي • حان وقت الصلاة"
        val body = intent.getStringExtra(PrayerNotificationManager.EXTRA_BODY)
            ?: "حافظ على صلاتك في وقتها"
        val notifId = intent.getIntExtra(PrayerNotificationManager.EXTRA_NOTIF_ID, 4001)
        PrayerNotificationManager.showNotificationNow(context, title, body, notifId)
    }
}

class PrayerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_TIMEZONE_CHANGED &&
            action != Intent.ACTION_TIME_CHANGED
        ) {
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = NotificationPrefsStore(context.applicationContext).preferences.first()
                val method = CalculationMethod.fromId(prefs.calculationMethod)
                val todayCal = Calendar.getInstance()
                val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

                val todayRes = PrayerTimesService.fetchDailyPrayerTimes(
                    latitude = prefs.latitude,
                    longitude = prefs.longitude,
                    year = todayCal.get(Calendar.YEAR),
                    month = todayCal.get(Calendar.MONTH) + 1,
                    day = todayCal.get(Calendar.DAY_OF_MONTH),
                    method = method,
                    hanafiAsr = prefs.hanafiAsr
                )
                val tomorrowRes = PrayerTimesService.fetchDailyPrayerTimes(
                    latitude = prefs.latitude,
                    longitude = prefs.longitude,
                    year = tomorrowCal.get(Calendar.YEAR),
                    month = tomorrowCal.get(Calendar.MONTH) + 1,
                    day = tomorrowCal.get(Calendar.DAY_OF_MONTH),
                    method = method,
                    hanafiAsr = prefs.hanafiAsr
                )
                PrayerNotificationManager.scheduleUpcomingPrayers(
                    context = context.applicationContext,
                    todayPrayers = todayRes.prayers,
                    tomorrowPrayers = tomorrowRes.prayers,
                    prefs = prefs
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
