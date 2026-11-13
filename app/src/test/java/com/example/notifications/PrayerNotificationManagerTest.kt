package com.example.notifications

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.LocalPrayerPreferences
import com.example.domain.PrayerTimeEntry
import com.example.domain.PrayerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PrayerNotificationManagerTest {

    @Test
    fun scheduleUpcomingPrayers_schedulesExactStartAlarmsForEnabledPrayers() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)

        val now = 1_794_600_000_000L
        val hourMs = 3_600_000L
        val todayPrayers = listOf(
            PrayerTimeEntry(PrayerType.FAJR, 5, 0, now + 1 * hourMs),
            PrayerTimeEntry(PrayerType.SUNRISE, 6, 20, now + 2 * hourMs),
            PrayerTimeEntry(PrayerType.DHUHR, 12, 10, now + 3 * hourMs),
            PrayerTimeEntry(PrayerType.ASR, 15, 30, now + 4 * hourMs),
            PrayerTimeEntry(PrayerType.MAGHRIB, 18, 0, now + 5 * hourMs),
            PrayerTimeEntry(PrayerType.ISHA, 19, 30, now + 6 * hourMs)
        )
        val tomorrowPrayers = listOf(
            PrayerTimeEntry(PrayerType.FAJR, 5, 1, now + 25 * hourMs),
            PrayerTimeEntry(PrayerType.SUNRISE, 6, 21, now + 26 * hourMs),
            PrayerTimeEntry(PrayerType.DHUHR, 12, 10, now + 27 * hourMs),
            PrayerTimeEntry(PrayerType.ASR, 15, 30, now + 28 * hourMs),
            PrayerTimeEntry(PrayerType.MAGHRIB, 18, 0, now + 29 * hourMs),
            PrayerTimeEntry(PrayerType.ISHA, 19, 30, now + 30 * hourMs)
        )

        val prefs = LocalPrayerPreferences(
            fajrAlert = true,
            dhuhrAlert = false,
            asrAlert = true,
            maghribAlert = true,
            ishaAlert = true,
            notifyAtPrayerStart = true,
            leadMinutes = 10,
            cityName = "الرياض"
        )

        PrayerNotificationManager.scheduleUpcomingPrayers(
            context = context,
            todayPrayers = todayPrayers,
            tomorrowPrayers = tomorrowPrayers,
            prefs = prefs,
            nowMillis = now
        )

        val scheduled = shadowAlarmManager.scheduledAlarms
        // 4 enabled prayers * 2 days * 2 alerts (start + 10m pre-reminder) = 16 alarms
        assertEquals(16, scheduled.size)
        val triggerTimes = scheduled.map { it.triggerAtMs }.toSet()
        // Exact start times for today's enabled prayers (Fajr, Asr, Maghrib, Isha)
        assertTrue(triggerTimes.contains(now + 1 * hourMs))
        assertTrue(triggerTimes.contains(now + 4 * hourMs))
        assertTrue(triggerTimes.contains(now + 5 * hourMs))
        assertTrue(triggerTimes.contains(now + 6 * hourMs))
        // Disabled Dhuhr must not be scheduled
        assertTrue(!triggerTimes.contains(now + 3 * hourMs))
        // 10-min pre-reminder for Fajr
        assertTrue(triggerTimes.contains(now + 1 * hourMs - 10 * 60_000L))
    }
}
