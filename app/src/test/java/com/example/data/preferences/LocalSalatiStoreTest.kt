package com.example.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.content.AyahTafsir
import com.example.data.content.SurahTafsirBundle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalSalatiStoreTest {

    @Test
    fun guestMode_togglesAndPersists() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = LocalSalatiStore(context)

        assertFalse(store.isGuestMode.first())
        store.setGuestMode(true)
        assertTrue(store.isGuestMode.first())
        store.setGuestMode(false)
        assertFalse(store.isGuestMode.first())
    }

    @Test
    fun dailyLogMutations_persistLocallyWithoutFirebase() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = LocalSalatiStore(context)
        val dateKey = "2026-11-13"

        store.ensureDailyLog(dateKey)
        store.togglePrayer(dateKey, "fajrDone", true)
        store.togglePrayer(dateKey, "asrDone", true)
        store.updateWird(dateKey, 3L, 5L)
        store.incrementDhikr(dateKey, 33L)
        store.toggleAdhkarPeriod(dateKey, isMorning = true, completed = true)

        val log = store.observeDailyLogs("guest_local_user").first().first { it.dateKey == dateKey }
        assertTrue(log.fajrDone)
        assertTrue(log.asrDone)
        assertFalse(log.dhuhrDone)
        assertEquals(3L, log.wirdPagesDone)
        assertEquals(5L, log.wirdTargetPages)
        assertEquals(33L, log.dhikrCount)
        assertTrue(log.morningAdhkarDone)
        assertFalse(log.eveningAdhkarDone)
    }

    @Test
    fun customSupplicationsAndBookmarks_addAndRemoveLocally() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = LocalSalatiStore(context)

        val id = store.addCustomSupplication(
            supplicationType = "DUA",
            title = "دعاء التوفيق",
            arabicText = "اللهم وفقني لما تحب وترضى",
            occasionNote = "عام",
            targetCount = 3L
        )
        store.saveBookmark("dua_1", "DUA", "دعاء السفر", "سفر", "1")

        val customList = store.observeCustomSupplications("guest_local_user").first()
        val bookmarkList = store.observeBookmarks("guest_local_user").first()
        assertTrue(customList.any { it.id == id && it.title == "دعاء التوفيق" })
        assertTrue(bookmarkList.any { it.id == "dua_1" })

        store.deleteCustomSupplication(id)
        store.removeBookmark("dua_1")
        assertTrue(store.observeCustomSupplications("guest_local_user").first().none { it.id == id })
        assertTrue(store.observeBookmarks("guest_local_user").first().none { it.id == "dua_1" })
    }

    @Test
    fun surahCache_roundTripsForOfflineReading() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = LocalSalatiStore(context)
        val surah = SurahTafsirBundle(
            surahNumber = 108,
            nameArabic = "سورة الكوثر",
            revelationType = "مكية",
            totalAyahs = 3,
            themeSummary = "امتنّ الله على نبيه بالخير الكثير",
            ayahs = listOf(
                AyahTafsir(1, "إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ", "إنا أعطيناك أيها النبي الخير الكثير في الدنيا والآخرة")
            )
        )

        store.cacheSurah(surah)
        val restored = store.getCachedSurah(108)
        assertNotNull(restored)
        assertEquals("سورة الكوثر", restored?.nameArabic)
        assertEquals(1, restored?.ayahs?.size)
        assertNull(store.getCachedSurah(999))
    }
}
