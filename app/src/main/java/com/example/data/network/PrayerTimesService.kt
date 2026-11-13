package com.example.data.network

import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.example.domain.CalculationMethod
import com.example.domain.PrayerTimeEntry
import com.example.domain.PrayerTimesCalculator
import com.example.domain.PrayerType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume

@JsonClass(generateAdapter = true)
data class AladhanTimingsResponse(
    val code: Int = 0,
    val status: String = "",
    val data: AladhanDataDto? = null
)

@JsonClass(generateAdapter = true)
data class AladhanDataDto(
    val timings: AladhanTimingsDto? = null,
    val meta: AladhanMetaDto? = null
)

@JsonClass(generateAdapter = true)
data class AladhanTimingsDto(
    @param:Json(name = "Fajr") val fajr: String = "",
    @param:Json(name = "Sunrise") val sunrise: String = "",
    @param:Json(name = "Dhuhr") val dhuhr: String = "",
    @param:Json(name = "Asr") val asr: String = "",
    @param:Json(name = "Maghrib") val maghrib: String = "",
    @param:Json(name = "Isha") val isha: String = ""
)

@JsonClass(generateAdapter = true)
data class AladhanMetaDto(
    val timezone: String = ""
)

interface AladhanApi {
    @GET("timings/{date}")
    suspend fun getTimingsForDate(
        @Path("date") dateDdMmYyyy: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int,
        @Query("school") school: Int,
        @Query("timezonestring") timezoneId: String
    ): AladhanTimingsResponse
}

enum class PrayerTimeSource(val arabicLabel: String) {
    LIVE_API("مواقيت متزامنة مباشرة"),
    OFFLINE_ASTRONOMICAL("حساب فلكي محلي")
}

data class PrayerTimesFetchResult(
    val prayers: List<PrayerTimeEntry>,
    val source: PrayerTimeSource,
    val timezoneId: String
)

object PrayerTimesService {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val api: AladhanApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.aladhan.com/v1/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AladhanApi::class.java)
    }

    fun aladhanMethodId(method: CalculationMethod): Int = when (method) {
        CalculationMethod.UMM_AL_QURA -> 4
        CalculationMethod.MWL -> 3
        CalculationMethod.EGYPTIAN -> 5
        CalculationMethod.ISNA -> 2
        CalculationMethod.KARACHI -> 1
    }

    suspend fun fetchDailyPrayerTimes(
        latitude: Double,
        longitude: Double,
        year: Int,
        month: Int,
        day: Int,
        method: CalculationMethod,
        hanafiAsr: Boolean,
        timeZone: TimeZone = TimeZone.getDefault()
    ): PrayerTimesFetchResult = withContext(Dispatchers.IO) {
        val dateStr = String.format(Locale.US, "%02d-%02d-%04d", day, month, year)
        val school = if (hanafiAsr) 1 else 0
        val methodCode = aladhanMethodId(method)

        val apiResult = runCatching {
            val response = api.getTimingsForDate(
                dateDdMmYyyy = dateStr,
                latitude = latitude,
                longitude = longitude,
                method = methodCode,
                school = school,
                timezoneId = timeZone.id
            )
            val data = response.data ?: error("Empty Aladhan response")
            val timings = data.timings ?: error("Missing timings")
            val tz = data.meta?.timezone
                ?.takeIf { it.isNotBlank() }
                ?.let { TimeZone.getTimeZone(it) }
                ?: timeZone
            parseTimingsDto(timings, year, month, day, tz)
        }

        val parsed = apiResult.getOrNull()
        if (parsed != null && parsed.size >= 6) {
            PrayerTimesFetchResult(
                prayers = parsed,
                source = PrayerTimeSource.LIVE_API,
                timezoneId = timeZone.id
            )
        } else {
            val fallback = PrayerTimesCalculator.calculateForDate(
                latitude = latitude,
                longitude = longitude,
                year = year,
                month = month,
                day = day,
                method = method,
                timeZone = timeZone,
                hanafiAsr = hanafiAsr
            )
            PrayerTimesFetchResult(
                prayers = fallback,
                source = PrayerTimeSource.OFFLINE_ASTRONOMICAL,
                timezoneId = timeZone.id
            )
        }
    }

    internal fun parseTimingsDto(
        dto: AladhanTimingsDto,
        year: Int,
        month: Int,
        day: Int,
        timeZone: TimeZone
    ): List<PrayerTimeEntry> = listOf(
        parseEntry(PrayerType.FAJR, dto.fajr, year, month, day, timeZone),
        parseEntry(PrayerType.SUNRISE, dto.sunrise, year, month, day, timeZone),
        parseEntry(PrayerType.DHUHR, dto.dhuhr, year, month, day, timeZone),
        parseEntry(PrayerType.ASR, dto.asr, year, month, day, timeZone),
        parseEntry(PrayerType.MAGHRIB, dto.maghrib, year, month, day, timeZone),
        parseEntry(PrayerType.ISHA, dto.isha, year, month, day, timeZone)
    )

    internal fun parseHourMinute(raw: String): Pair<Int, Int>? {
        val clean = raw.trim().substringBefore(" ")
        val parts = clean.split(":")
        if (parts.size < 2) return null
        val hour = parts[0].toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        val minute = parts[1].take(2).toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        return hour to minute
    }

    private fun parseEntry(
        type: PrayerType,
        rawTime: String,
        year: Int,
        month: Int,
        day: Int,
        timeZone: TimeZone
    ): PrayerTimeEntry {
        val (hour, minute) = parseHourMinute(rawTime)
            ?: error("Invalid time string '$rawTime' for ${type.name}")
        val cal = Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return PrayerTimeEntry(type = type, hour = hour, minute = minute, timestampMillis = cal.timeInMillis)
    }

    @Suppress("DEPRECATION")
    suspend fun resolveCityNameFromCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String = withContext(Dispatchers.IO) {
        val fallbackLabel = String.format(Locale.US, "موقعي (%.2f, %.2f)", latitude, longitude)
        if (!Geocoder.isPresent()) return@withContext fallbackLabel
        runCatching {
            val geocoder = Geocoder(context, Locale.forLanguageTag("ar"))
            val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1) { list ->
                        if (cont.isActive) cont.resume(list.firstOrNull())
                    }
                }
            } else {
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }
            val locality = address?.locality ?: address?.subAdminArea ?: address?.adminArea
            val country = address?.countryName
            when {
                !locality.isNullOrBlank() && !country.isNullOrBlank() -> "$locality، $country"
                !locality.isNullOrBlank() -> locality
                !country.isNullOrBlank() -> country
                else -> fallbackLabel
            }
        }.getOrDefault(fallbackLabel)
    }
}
