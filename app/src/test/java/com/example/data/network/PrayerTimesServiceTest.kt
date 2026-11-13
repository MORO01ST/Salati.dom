package com.example.data.network

import com.example.domain.CalculationMethod
import com.example.domain.PrayerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.TimeZone

class PrayerTimesServiceTest {

    @Test
    fun parseHourMinute_handlesStandardAndTimezoneSuffixFormats() {
        assertEquals(4 to 38, PrayerTimesService.parseHourMinute("04:38"))
        assertEquals(18 to 15, PrayerTimesService.parseHourMinute("18:15 (+03)"))
        assertEquals(12 to 2, PrayerTimesService.parseHourMinute("12:02 (AST)"))
        assertNull(PrayerTimesService.parseHourMinute("invalid"))
        assertNull(PrayerTimesService.parseHourMinute("25:10"))
    }

    @Test
    fun parseTimingsDto_buildsOrderedPrayerEntries() {
        val dto = AladhanTimingsDto(
            fajr = "04:45 (+03)",
            sunrise = "06:08 (+03)",
            dhuhr = "12:05 (+03)",
            asr = "15:28 (+03)",
            maghrib = "18:02 (+03)",
            isha = "19:32 (+03)"
        )
        val tz = TimeZone.getTimeZone("Asia/Riyadh")
        val entries = PrayerTimesService.parseTimingsDto(dto, 2026, 11, 13, tz)

        assertEquals(6, entries.size)
        assertEquals(PrayerType.FAJR, entries[0].type)
        assertEquals(4, entries[0].hour)
        assertEquals(45, entries[0].minute)
        assertEquals(PrayerType.DHUHR, entries[2].type)
        assertEquals(PrayerType.ASR, entries[3].type)
        assertEquals(PrayerType.MAGHRIB, entries[4].type)
        assertEquals(PrayerType.ISHA, entries[5].type)
        assertEquals(19, entries[5].hour)
        assertEquals(32, entries[5].minute)
    }

    @Test
    fun aladhanMethodId_mapsAllCalculationMethods() {
        assertEquals(4, PrayerTimesService.aladhanMethodId(CalculationMethod.UMM_AL_QURA))
        assertEquals(3, PrayerTimesService.aladhanMethodId(CalculationMethod.MWL))
        assertEquals(5, PrayerTimesService.aladhanMethodId(CalculationMethod.EGYPTIAN))
        assertEquals(2, PrayerTimesService.aladhanMethodId(CalculationMethod.ISNA))
        assertEquals(1, PrayerTimesService.aladhanMethodId(CalculationMethod.KARACHI))
    }
}
