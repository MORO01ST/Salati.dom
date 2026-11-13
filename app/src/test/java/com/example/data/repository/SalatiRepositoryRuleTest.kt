package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class SalatiRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_and_dailyLog_workForAuthenticatedOwner() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = SalatiRepository(firestore, auth)

        val profileRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile("أحمد", "مكة المكرمة")
        }
        assertTrue(profileRes.isSuccess)
        assertEquals(uid, profileRes.getOrThrow().userId)

        val dateKey = "2026-11-13"
        val logRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.getOrCreateDailyLog(dateKey, 4L)
        }
        assertTrue(logRes.isSuccess)

        val toggleRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.togglePrayerStatus(dateKey, "fajrDone", true)
        }
        assertTrue(toggleRes.isSuccess)

        val logs = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeDailyLogs(uid).first { it.any { log -> log.dateKey == dateKey && log.fajrDone } }
        }
        assertTrue(logs.any { it.dateKey == dateKey && it.fajrDone })
    }

    @Test
    fun circles_and_achievements_supportCreateJoinAndAmeen() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = SalatiRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.ensureUserProfile("أحمد") }

        val circleTitle = "حلقة_${UUID.randomUUID().toString().take(6)}"
        val createCircleRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createCircle(circleTitle, "سورة الملك", 30L, "أحمد", isPublic = true)
        }
        assertTrue(createCircleRes.isSuccess)
        val circleId = createCircleRes.getOrThrow()

        val publicCircles = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observePublicCircles().first { list -> list.any { it.id == circleId } }
        }
        val createdCircle = publicCircles.first { it.id == circleId }
        assertTrue(createdCircle.memberUids.contains(aliceUid))

        val achRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.shareAchievement("أحمد", "ختم الورد", "أتممت وردي اليومي", "QURAN", 3L)
        }
        assertTrue(achRes.isSuccess)
        val achId = achRes.getOrThrow()

        // Switch to Bob and join circle + send Ameen
        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = SalatiRepository(firestore, auth)
        val joinRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.joinCircle(createdCircle, "بكر")
        }
        assertTrue(joinRes.isSuccess)

        val ameenRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.sendAmeen(achId)
        }
        assertTrue(ameenRes.isSuccess)
        assertTrue(bobUid != aliceUid)
    }

    @Test
    fun observeDailyLogs_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = SalatiRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeDailyLogs("some_uid").first()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
        }
    }

    @Test
    fun customSupplications_createObserveAndDelete_enforcesOwnership() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = SalatiRepository(firestore, auth)

        val addRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addCustomSupplication(
                supplicationType = "DUA",
                title = "دعاء السفر الخاص",
                arabicText = "اللهم هوّن علينا سفرنا هذا واطو عنّا بعده",
                occasionNote = "السفر",
                targetCount = 3L
            )
        }
        assertTrue(addRes.isSuccess)
        val supId = addRes.getOrThrow()

        val list = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeCustomSupplications(aliceUid).first { items -> items.any { it.id == supId } }
        }
        assertTrue(list.any { it.id == supId && it.title == "دعاء السفر الخاص" })

        val bookmarkRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveBookmark("dhikr_morning_1", "DHIKR", "أذكار الصباح", "أصبحنا وأصبح الملك لله", "morning_1")
        }
        assertTrue(bookmarkRes.isSuccess)

        // Bob cannot observe Alice's customSupplications
        signInTestUser(BOB_EMAIL)
        val bobRepo = SalatiRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeCustomSupplications(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED for cross-user customSupplications read")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
        }

        // Alice can delete her custom supplication
        signInTestUser(ALICE_EMAIL)
        val delRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.deleteCustomSupplication(supId)
        }
        assertTrue(delRes.isSuccess)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_salati@test.com"
        const val BOB_EMAIL = "bob_salati@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
