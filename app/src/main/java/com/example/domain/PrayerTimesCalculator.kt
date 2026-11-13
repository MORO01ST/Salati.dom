package com.example.domain

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

enum class CalculationMethod(
    val id: String,
    val arabicName: String,
    val fajrAngle: Double,
    val ishaAngle: Double?,
    val ishaIntervalMinutes: Int? = null
) {
    UMM_AL_QURA("UMM_AL_QURA", "أم القرى (مكة المكرمة)", 18.5, null, 90),
    MWL("MWL", "رابطة العالم الإسلامي", 18.0, 17.0),
    EGYPTIAN("EGYPTIAN", "الهيئة المصرية العامة للمساحة", 19.5, 17.5),
    ISNA("ISNA", "أمريكا الشمالية (ISNA)", 15.0, 15.0),
    KARACHI("KARACHI", "جامعة العلوم الإسلامية بكراتشي", 18.0, 18.0);

    companion object {
        fun fromId(id: String): CalculationMethod =
            entries.firstOrNull { it.id == id } ?: UMM_AL_QURA
    }
}

enum class PrayerType(val key: String, val arabicName: String, val firestoreField: String?) {
    FAJR("fajr", "الفجر", "fajrDone"),
    SUNRISE("sunrise", "الشروق", null),
    DHUHR("dhuhr", "الظهر", "dhuhrDone"),
    ASR("asr", "العصر", "asrDone"),
    MAGHRIB("maghrib", "المغرب", "maghribDone"),
    ISHA("isha", "العشاء", "ishaDone")
}

data class PrayerTimeEntry(
    val type: PrayerType,
    val hour: Int,
    val minute: Int,
    val timestampMillis: Long
) {
    val formattedTime: String
        get() {
            val period = if (hour < 12) "ص" else "م"
            val h12 = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            return "%d:%02d %s".format(h12, minute, period)
        }
}

data class CityCoordinate(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double
)

object IslamicCities {
    val presets = listOf(
        CityCoordinate("مكة المكرمة", "السعودية", 21.4225, 39.8262),
        CityCoordinate("المدينة المنورة", "السعودية", 24.4672, 39.6111),
        CityCoordinate("الرياض", "السعودية", 24.7136, 46.6753),
        CityCoordinate("جدة", "السعودية", 21.5433, 39.1728),
        CityCoordinate("القاهرة", "مصر", 30.0444, 31.2357),
        CityCoordinate("دبي", "الإمارات", 25.2048, 55.2708),
        CityCoordinate("الدوحة", "قطر", 25.2854, 51.5310),
        CityCoordinate("الكويت", "الكويت", 29.3759, 47.9774),
        CityCoordinate("عمّان", "الأردن", 31.9454, 35.9284),
        CityCoordinate("القدس", "فلسطين", 31.7683, 35.2137),
        CityCoordinate("الرباط", "المغرب", 34.0209, -6.8416),
        CityCoordinate("الجزائر", "الجزائر", 36.7538, 3.0588),
        CityCoordinate("تونس", "تونس", 36.8065, 10.1815),
        CityCoordinate("إسطنبول", "تركيا", 41.0082, 28.9784),
        CityCoordinate("لندن", "المملكة المتحدة", 51.5072, -0.1276)
    )
}

object PrayerTimesCalculator {

    private const val KAABA_LAT = 21.422487
    private const val KAABA_LON = 39.826206

    fun calculateForDate(
        latitude: Double,
        longitude: Double,
        year: Int,
        month: Int, // 1-12
        day: Int,
        method: CalculationMethod,
        timeZone: TimeZone = TimeZone.getDefault(),
        hanafiAsr: Boolean = false
    ): List<PrayerTimeEntry> {
        val cal = Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val tzHours = timeZone.getOffset(cal.timeInMillis) / 3600000.0
        val jd = julianDate(year, month, day) - longitude / (15.0 * 24.0)

        val (declination, eqTime) = sunPosition(jd + 0.5)
        val transit = 12.0 + tzHours - (longitude / 15.0) - eqTime

        val fajrTime = transit - hourAngle(latitude, declination, -method.fajrAngle) / 15.0
        val sunriseTime = transit - hourAngle(latitude, declination, -0.833) / 15.0
        val dhuhrTime = transit + (2.0 / 60.0) // +2 min safety margin after zenith
        val asrFactor = if (hanafiAsr) 2.0 else 1.0
        val asrElevation = Math.toDegrees(kotlin.math.atan(1.0 / (asrFactor + tan(Math.toRadians(kotlin.math.abs(latitude - declination))))))
        val asrTime = transit + hourAngle(latitude, declination, asrElevation) / 15.0
        val maghribTime = transit + hourAngle(latitude, declination, -0.833) / 15.0
        val ishaTime = if (method.ishaIntervalMinutes != null) {
            maghribTime + (method.ishaIntervalMinutes / 60.0)
        } else {
            transit + hourAngle(latitude, declination, -(method.ishaAngle ?: 17.0)) / 15.0
        }

        return listOf(
            buildEntry(PrayerType.FAJR, fajrTime, year, month, day, timeZone),
            buildEntry(PrayerType.SUNRISE, sunriseTime, year, month, day, timeZone),
            buildEntry(PrayerType.DHUHR, dhuhrTime, year, month, day, timeZone),
            buildEntry(PrayerType.ASR, asrTime, year, month, day, timeZone),
            buildEntry(PrayerType.MAGHRIB, maghribTime, year, month, day, timeZone),
            buildEntry(PrayerType.ISHA, ishaTime, year, month, day, timeZone)
        )
    }

    private fun buildEntry(
        type: PrayerType,
        decimalHours: Double,
        year: Int,
        month: Int,
        day: Int,
        timeZone: TimeZone
    ): PrayerTimeEntry {
        val normalized = ((decimalHours % 24.0) + 24.0) % 24.0
        val totalMinutes = kotlin.math.round(normalized * 60.0).toInt()
        val hour = (totalMinutes / 60) % 24
        val minute = totalMinutes % 60
        val cal = Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return PrayerTimeEntry(type, hour, minute, cal.timeInMillis)
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2 * g)))
        val e = 23.439 - 0.00000036 * d

        val ra = Math.toDegrees(atan2(cos(Math.toRadians(e)) * sin(Math.toRadians(l)), cos(Math.toRadians(l)))) / 15.0
        val dec = Math.toDegrees(asin(sin(Math.toRadians(e)) * sin(Math.toRadians(l))))
        val eqTime = (q / 15.0) - fixHour(ra)
        return dec to eqTime
    }

    private fun hourAngle(lat: Double, dec: Double, elevationAngle: Double): Double {
        val latRad = Math.toRadians(lat)
        val decRad = Math.toRadians(dec)
        val elevRad = Math.toRadians(elevationAngle)
        val cosH = (sin(elevRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        val clamped = cosH.coerceIn(-0.9999, 0.9999)
        return Math.toDegrees(acos(clamped))
    }

    private fun fixAngle(a: Double): Double = ((a % 360.0) + 360.0) % 360.0
    private fun fixHour(h: Double): Double = ((h % 24.0) + 24.0) % 24.0

    fun calculateQiblaBearing(latitude: Double, longitude: Double): Double {
        val phi1 = Math.toRadians(latitude)
        val phi2 = Math.toRadians(KAABA_LAT)
        val deltaLambda = Math.toRadians(KAABA_LON - longitude)
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    fun calculateDistanceKmToKaaba(latitude: Double, longitude: Double): Int {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(KAABA_LAT - latitude)
        val dLon = Math.toRadians(KAABA_LON - longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(KAABA_LAT)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return kotlin.math.round(earthRadiusKm * c).toInt()
    }
}
