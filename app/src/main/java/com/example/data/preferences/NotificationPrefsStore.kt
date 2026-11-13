package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.salatiDataStore by preferencesDataStore(name = "salati_prefs")

data class LocalPrayerPreferences(
    val fajrAlert: Boolean = true,
    val dhuhrAlert: Boolean = true,
    val asrAlert: Boolean = true,
    val maghribAlert: Boolean = true,
    val ishaAlert: Boolean = true,
    val notifyAtPrayerStart: Boolean = true,
    val leadMinutes: Int = 0,
    val hijriAdjustment: Int = 0,
    val latitude: Double = 21.4225,
    val longitude: Double = 39.8262,
    val cityName: String = "مكة المكرمة",
    val calculationMethod: String = "UMM_AL_QURA",
    val hanafiAsr: Boolean = false
) {
    fun isEnabledFor(key: String): Boolean = when (key) {
        "fajr" -> fajrAlert
        "dhuhr" -> dhuhrAlert
        "asr" -> asrAlert
        "maghrib" -> maghribAlert
        "isha" -> ishaAlert
        else -> false
    }
}

class NotificationPrefsStore(private val context: Context) {

    private object Keys {
        val FAJR = booleanPreferencesKey("alert_fajr")
        val DHUHR = booleanPreferencesKey("alert_dhuhr")
        val ASR = booleanPreferencesKey("alert_asr")
        val MAGHRIB = booleanPreferencesKey("alert_maghrib")
        val ISHA = booleanPreferencesKey("alert_isha")
        val NOTIFY_AT_START = booleanPreferencesKey("notify_at_prayer_start")
        val LEAD_MIN = intPreferencesKey("alert_lead_minutes")
        val HIJRI_ADJ = intPreferencesKey("hijri_adjustment")
        val LAT = doublePreferencesKey("city_lat")
        val LON = doublePreferencesKey("city_lon")
        val CITY = stringPreferencesKey("city_name")
        val METHOD = stringPreferencesKey("calc_method")
        val HANAFI = booleanPreferencesKey("hanafi_asr")
    }

    val preferences: Flow<LocalPrayerPreferences> = context.salatiDataStore.data.map { prefs ->
        LocalPrayerPreferences(
            fajrAlert = prefs[Keys.FAJR] ?: true,
            dhuhrAlert = prefs[Keys.DHUHR] ?: true,
            asrAlert = prefs[Keys.ASR] ?: true,
            maghribAlert = prefs[Keys.MAGHRIB] ?: true,
            ishaAlert = prefs[Keys.ISHA] ?: true,
            notifyAtPrayerStart = prefs[Keys.NOTIFY_AT_START] ?: true,
            leadMinutes = prefs[Keys.LEAD_MIN] ?: 0,
            hijriAdjustment = prefs[Keys.HIJRI_ADJ] ?: 0,
            latitude = prefs[Keys.LAT] ?: 21.4225,
            longitude = prefs[Keys.LON] ?: 39.8262,
            cityName = prefs[Keys.CITY] ?: "مكة المكرمة",
            calculationMethod = prefs[Keys.METHOD] ?: "UMM_AL_QURA",
            hanafiAsr = prefs[Keys.HANAFI] ?: false
        )
    }

    suspend fun setPrayerAlert(key: String, enabled: Boolean) {
        context.salatiDataStore.edit { prefs ->
            when (key) {
                "fajr" -> prefs[Keys.FAJR] = enabled
                "dhuhr" -> prefs[Keys.DHUHR] = enabled
                "asr" -> prefs[Keys.ASR] = enabled
                "maghrib" -> prefs[Keys.MAGHRIB] = enabled
                "isha" -> prefs[Keys.ISHA] = enabled
            }
        }
    }

    suspend fun setNotifyAtPrayerStart(enabled: Boolean) {
        context.salatiDataStore.edit { it[Keys.NOTIFY_AT_START] = enabled }
    }

    suspend fun setLeadMinutes(minutes: Int) {
        context.salatiDataStore.edit { it[Keys.LEAD_MIN] = minutes.coerceIn(0, 30) }
    }

    suspend fun setHijriAdjustment(days: Int) {
        context.salatiDataStore.edit { it[Keys.HIJRI_ADJ] = days.coerceIn(-2, 2) }
    }

    suspend fun setLocation(cityName: String, latitude: Double, longitude: Double) {
        context.salatiDataStore.edit {
            it[Keys.CITY] = cityName
            it[Keys.LAT] = latitude
            it[Keys.LON] = longitude
        }
    }

    suspend fun setCalculationMethod(methodId: String, hanafiAsr: Boolean) {
        context.salatiDataStore.edit {
            it[Keys.METHOD] = methodId
            it[Keys.HANAFI] = hanafiAsr
        }
    }
}
