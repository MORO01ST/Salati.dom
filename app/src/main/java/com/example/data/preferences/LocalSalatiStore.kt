package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.content.AyahTafsir
import com.example.data.content.SurahTafsirBundle
import com.example.data.model.Bookmark
import com.example.data.model.CustomSupplication
import com.example.data.model.DailyLog
import com.google.firebase.Timestamp
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.UUID

private val Context.localSalatiDataStore by preferencesDataStore(name = "salati_local_store")

@JsonClass(generateAdapter = true)
data class LocalDailyLogJson(
    val dateKey: String = "",
    val fajrDone: Boolean = false,
    val dhuhrDone: Boolean = false,
    val asrDone: Boolean = false,
    val maghribDone: Boolean = false,
    val ishaDone: Boolean = false,
    val wirdPagesDone: Long = 0L,
    val wirdTargetPages: Long = 4L,
    val dhikrCount: Long = 0L,
    val morningAdhkarDone: Boolean = false,
    val eveningAdhkarDone: Boolean = false
)

@JsonClass(generateAdapter = true)
data class LocalBookmarkJson(
    val id: String = "",
    val itemType: String = "DUA",
    val title: String = "",
    val subtitle: String = "",
    val referenceKey: String = ""
)

@JsonClass(generateAdapter = true)
data class LocalCustomSupplicationJson(
    val id: String = "",
    val supplicationType: String = "DUA",
    val title: String = "",
    val arabicText: String = "",
    val occasionNote: String = "",
    val targetCount: Long = 1L,
    val createdAtSeconds: Long = 0L
)

@JsonClass(generateAdapter = true)
data class CachedSurahJson(
    val surahNumber: Int = 0,
    val nameArabic: String = "",
    val revelationType: String = "",
    val totalAyahs: Int = 0,
    val themeSummary: String = "",
    val ayahs: List<CachedAyahJson> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CachedAyahJson(
    val numberInSurah: Int = 0,
    val arabicText: String = "",
    val tafsirMuyassar: String = ""
)

class LocalSalatiStore(private val context: Context) {

    private object Keys {
        val GUEST_MODE = booleanPreferencesKey("guest_mode_enabled")
        val DAILY_LOGS_JSON = stringPreferencesKey("local_daily_logs_json")
        val BOOKMARKS_JSON = stringPreferencesKey("local_bookmarks_json")
        val CUSTOM_SUPS_JSON = stringPreferencesKey("local_custom_sups_json")
        val CACHED_SURAHS_JSON = stringPreferencesKey("local_cached_surahs_json")
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val logsAdapter = moshi.adapter<List<LocalDailyLogJson>>(
        Types.newParameterizedType(List::class.java, LocalDailyLogJson::class.java)
    )
    private val bookmarksAdapter = moshi.adapter<List<LocalBookmarkJson>>(
        Types.newParameterizedType(List::class.java, LocalBookmarkJson::class.java)
    )
    private val customSupsAdapter = moshi.adapter<List<LocalCustomSupplicationJson>>(
        Types.newParameterizedType(List::class.java, LocalCustomSupplicationJson::class.java)
    )
    private val cachedSurahsAdapter = moshi.adapter<List<CachedSurahJson>>(
        Types.newParameterizedType(List::class.java, CachedSurahJson::class.java)
    )

    val isGuestMode: Flow<Boolean> = context.localSalatiDataStore.data.map { prefs ->
        prefs[Keys.GUEST_MODE] ?: false
    }

    suspend fun setGuestMode(enabled: Boolean) {
        context.localSalatiDataStore.edit { it[Keys.GUEST_MODE] = enabled }
    }

    fun observeDailyLogs(userId: String): Flow<List<DailyLog>> =
        context.localSalatiDataStore.data.map { prefs ->
            decodeLogs(prefs[Keys.DAILY_LOGS_JSON])
                .map { dto ->
                    DailyLog(
                        id = dto.dateKey,
                        userId = userId,
                        dateKey = dto.dateKey,
                        fajrDone = dto.fajrDone,
                        dhuhrDone = dto.dhuhrDone,
                        asrDone = dto.asrDone,
                        maghribDone = dto.maghribDone,
                        ishaDone = dto.ishaDone,
                        wirdPagesDone = dto.wirdPagesDone,
                        wirdTargetPages = dto.wirdTargetPages,
                        dhikrCount = dto.dhikrCount,
                        morningAdhkarDone = dto.morningAdhkarDone,
                        eveningAdhkarDone = dto.eveningAdhkarDone
                    )
                }
                .sortedByDescending { it.dateKey }
        }

    suspend fun ensureDailyLog(dateKey: String, defaultWirdTarget: Long = 4L) {
        context.localSalatiDataStore.edit { prefs ->
            val list = decodeLogs(prefs[Keys.DAILY_LOGS_JSON]).toMutableList()
            if (list.none { it.dateKey == dateKey }) {
                list.add(LocalDailyLogJson(dateKey = dateKey, wirdTargetPages = defaultWirdTarget.coerceIn(1L, 604L)))
                prefs[Keys.DAILY_LOGS_JSON] = logsAdapter.toJson(list)
            }
        }
    }

    suspend fun togglePrayer(dateKey: String, field: String, newValue: Boolean) {
        context.localSalatiDataStore.edit { prefs ->
            val list = decodeLogs(prefs[Keys.DAILY_LOGS_JSON]).toMutableList()
            val idx = list.indexOfFirst { it.dateKey == dateKey }
            val current = if (idx >= 0) list[idx] else LocalDailyLogJson(dateKey = dateKey)
            val updated = when (field) {
                "fajrDone" -> current.copy(fajrDone = newValue)
                "dhuhrDone" -> current.copy(dhuhrDone = newValue)
                "asrDone" -> current.copy(asrDone = newValue)
                "maghribDone" -> current.copy(maghribDone = newValue)
                "ishaDone" -> current.copy(ishaDone = newValue)
                else -> current
            }
            if (idx >= 0) list[idx] = updated else list.add(updated)
            prefs[Keys.DAILY_LOGS_JSON] = logsAdapter.toJson(list)
        }
    }

    suspend fun updateWird(dateKey: String, pagesDone: Long, targetPages: Long) {
        context.localSalatiDataStore.edit { prefs ->
            val list = decodeLogs(prefs[Keys.DAILY_LOGS_JSON]).toMutableList()
            val idx = list.indexOfFirst { it.dateKey == dateKey }
            val current = if (idx >= 0) list[idx] else LocalDailyLogJson(dateKey = dateKey)
            val updated = current.copy(
                wirdPagesDone = pagesDone.coerceIn(0L, 604L),
                wirdTargetPages = targetPages.coerceIn(1L, 604L)
            )
            if (idx >= 0) list[idx] = updated else list.add(updated)
            prefs[Keys.DAILY_LOGS_JSON] = logsAdapter.toJson(list)
        }
    }

    suspend fun incrementDhikr(dateKey: String, delta: Long) {
        context.localSalatiDataStore.edit { prefs ->
            val list = decodeLogs(prefs[Keys.DAILY_LOGS_JSON]).toMutableList()
            val idx = list.indexOfFirst { it.dateKey == dateKey }
            val current = if (idx >= 0) list[idx] else LocalDailyLogJson(dateKey = dateKey)
            val updated = current.copy(dhikrCount = (current.dhikrCount + delta).coerceAtLeast(0L))
            if (idx >= 0) list[idx] = updated else list.add(updated)
            prefs[Keys.DAILY_LOGS_JSON] = logsAdapter.toJson(list)
        }
    }

    suspend fun toggleAdhkarPeriod(dateKey: String, isMorning: Boolean, completed: Boolean) {
        context.localSalatiDataStore.edit { prefs ->
            val list = decodeLogs(prefs[Keys.DAILY_LOGS_JSON]).toMutableList()
            val idx = list.indexOfFirst { it.dateKey == dateKey }
            val current = if (idx >= 0) list[idx] else LocalDailyLogJson(dateKey = dateKey)
            val updated = if (isMorning) {
                current.copy(morningAdhkarDone = completed)
            } else {
                current.copy(eveningAdhkarDone = completed)
            }
            if (idx >= 0) list[idx] = updated else list.add(updated)
            prefs[Keys.DAILY_LOGS_JSON] = logsAdapter.toJson(list)
        }
    }

    fun observeBookmarks(userId: String): Flow<List<Bookmark>> =
        context.localSalatiDataStore.data.map { prefs ->
            decodeBookmarks(prefs[Keys.BOOKMARKS_JSON]).map { dto ->
                Bookmark(
                    id = dto.id,
                    userId = userId,
                    itemType = dto.itemType,
                    title = dto.title,
                    subtitle = dto.subtitle,
                    referenceKey = dto.referenceKey
                )
            }
        }

    suspend fun saveBookmark(
        bookmarkId: String,
        itemType: String,
        title: String,
        subtitle: String,
        referenceKey: String
    ) {
        context.localSalatiDataStore.edit { prefs ->
            val list = decodeBookmarks(prefs[Keys.BOOKMARKS_JSON]).toMutableList()
            list.removeAll { it.id == bookmarkId }
            list.add(
                0,
                LocalBookmarkJson(
                    id = bookmarkId,
                    itemType = itemType,
                    title = title.take(120),
                    subtitle = subtitle.take(200),
                    referenceKey = referenceKey.take(100)
                )
            )
            prefs[Keys.BOOKMARKS_JSON] = bookmarksAdapter.toJson(list)
        }
    }

    suspend fun removeBookmark(bookmarkId: String) {
        context.localSalatiDataStore.edit { prefs ->
            val list = decodeBookmarks(prefs[Keys.BOOKMARKS_JSON]).toMutableList()
            list.removeAll { it.id == bookmarkId }
            prefs[Keys.BOOKMARKS_JSON] = bookmarksAdapter.toJson(list)
        }
    }

    fun observeCustomSupplications(userId: String): Flow<List<CustomSupplication>> =
        context.localSalatiDataStore.data.map { prefs ->
            decodeCustomSups(prefs[Keys.CUSTOM_SUPS_JSON])
                .sortedByDescending { it.createdAtSeconds }
                .map { dto ->
                    CustomSupplication(
                        id = dto.id,
                        userId = userId,
                        supplicationType = dto.supplicationType,
                        title = dto.title,
                        arabicText = dto.arabicText,
                        occasionNote = dto.occasionNote,
                        targetCount = dto.targetCount,
                        createdAt = Timestamp(Date(dto.createdAtSeconds * 1000L))
                    )
                }
        }

    suspend fun addCustomSupplication(
        supplicationType: String,
        title: String,
        arabicText: String,
        occasionNote: String,
        targetCount: Long
    ): String {
        val id = "local_sup_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        context.localSalatiDataStore.edit { prefs ->
            val list = decodeCustomSups(prefs[Keys.CUSTOM_SUPS_JSON]).toMutableList()
            list.add(
                0,
                LocalCustomSupplicationJson(
                    id = id,
                    supplicationType = supplicationType,
                    title = title.trim().take(120),
                    arabicText = arabicText.trim().take(1000),
                    occasionNote = occasionNote.trim().take(160),
                    targetCount = targetCount.coerceIn(1L, 1000L),
                    createdAtSeconds = System.currentTimeMillis() / 1000L
                )
            )
            prefs[Keys.CUSTOM_SUPS_JSON] = customSupsAdapter.toJson(list)
        }
        return id
    }

    suspend fun deleteCustomSupplication(supplicationId: String) {
        context.localSalatiDataStore.edit { prefs ->
            val list = decodeCustomSups(prefs[Keys.CUSTOM_SUPS_JSON]).toMutableList()
            list.removeAll { it.id == supplicationId }
            prefs[Keys.CUSTOM_SUPS_JSON] = customSupsAdapter.toJson(list)
        }
    }

    suspend fun cacheSurah(surah: SurahTafsirBundle) {
        context.localSalatiDataStore.edit { prefs ->
            val list = decodeCachedSurahs(prefs[Keys.CACHED_SURAHS_JSON]).toMutableList()
            list.removeAll { it.surahNumber == surah.surahNumber }
            list.add(
                CachedSurahJson(
                    surahNumber = surah.surahNumber,
                    nameArabic = surah.nameArabic,
                    revelationType = surah.revelationType,
                    totalAyahs = surah.totalAyahs,
                    themeSummary = surah.themeSummary,
                    ayahs = surah.ayahs.map { CachedAyahJson(it.numberInSurah, it.arabicText, it.tafsirMuyassar) }
                )
            )
            // Keep up to 15 most recently cached Surahs locally for offline reading
            val trimmed = list.takeLast(15)
            prefs[Keys.CACHED_SURAHS_JSON] = cachedSurahsAdapter.toJson(trimmed)
        }
    }

    suspend fun getCachedSurah(surahNumber: Int): SurahTafsirBundle? {
        val prefs = context.localSalatiDataStore.data.first()
        return decodeCachedSurahs(prefs[Keys.CACHED_SURAHS_JSON])
            .firstOrNull { it.surahNumber == surahNumber }
            ?.let { cached ->
                SurahTafsirBundle(
                    surahNumber = cached.surahNumber,
                    nameArabic = cached.nameArabic,
                    revelationType = cached.revelationType,
                    totalAyahs = cached.totalAyahs,
                    themeSummary = cached.themeSummary,
                    ayahs = cached.ayahs.map { AyahTafsir(it.numberInSurah, it.arabicText, it.tafsirMuyassar) }
                )
            }
    }

    private fun decodeLogs(json: String?): List<LocalDailyLogJson> =
        if (json.isNullOrBlank()) emptyList() else runCatching { logsAdapter.fromJson(json) }.getOrNull().orEmpty()

    private fun decodeBookmarks(json: String?): List<LocalBookmarkJson> =
        if (json.isNullOrBlank()) emptyList() else runCatching { bookmarksAdapter.fromJson(json) }.getOrNull().orEmpty()

    private fun decodeCustomSups(json: String?): List<LocalCustomSupplicationJson> =
        if (json.isNullOrBlank()) emptyList() else runCatching { customSupsAdapter.fromJson(json) }.getOrNull().orEmpty()

    private fun decodeCachedSurahs(json: String?): List<CachedSurahJson> =
        if (json.isNullOrBlank()) emptyList() else runCatching { cachedSurahsAdapter.fromJson(json) }.getOrNull().orEmpty()
}
