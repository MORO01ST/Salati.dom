package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class PrayerTimesCalculatorTest {

    @Test
    fun calculateForDate_riyadh_producesOrderedPrayerTimes() {
        val prayers = PrayerTimesCalculator.calculateForDate(
            latitude = 24.7136,
            longitude = 46.6753,
            year = 2026,
            month = 11,
            day = 13,
            method = CalculationMethod.UMM_AL_QURA,
            timeZone = TimeZone.getTimeZone("Asia/Riyadh")
        )
        assertEquals(6, prayers.size)
        for (i in 0 until prayers.size - 1) {
            assertTrue(
                "Expected ${prayers[i].type} before ${prayers[i + 1].type}",
                prayers[i].timestampMillis < prayers[i + 1].timestampMillis
            )
        }
        // Fajr in Riyadh in November is around 4:30 - 5:15 AM
        val fajr = prayers.first { it.type == PrayerType.FAJR }
        assertTrue(fajr.hour in 4..5)
    }

    @Test
    fun calculateQiblaBearing_riyadh_isWestSouthwest() {
        val bearing = PrayerTimesCalculator.calculateQiblaBearing(24.7136, 46.6753)
        // Riyadh to Makkah is ~244 degrees
        assertTrue("Expected bearing ~244, got $bearing", bearing in 235.0..255.0)
        val distance = PrayerTimesCalculator.calculateDistanceKmToKaaba(24.7136, 46.6753)
        assertTrue("Expected distance ~780-810 km, got $distance", distance in 750..850)
    }

    @Test
    fun hijriCalendar_convertsValidDate() {
        val hijri = HijriCalendarHelper.fromGregorian(2026, 11, 13, 0)
        assertTrue(hijri.year >= 1448)
        assertTrue(hijri.month in 1..12)
        assertTrue(hijri.day in 1..30)
    }
}
