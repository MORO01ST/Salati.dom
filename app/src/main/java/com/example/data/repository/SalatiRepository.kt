package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AchievementPost
import com.example.data.model.Bookmark
import com.example.data.model.CircleMember
import com.example.data.model.CustomSupplication
import com.example.data.model.DailyLog
import com.example.data.model.MemorizationCircle
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SalatiRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        FirebaseAuth.getInstance()
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.logFirestoreErrors(
        operationType: OperationType,
        path: String,
        expectedUid: String? = null
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && (expectedUid == null || auth.currentUser?.uid == expectedUid)) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    // --- User Profile ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .logFirestoreErrors(OperationType.GET, path, userId)
    }

    suspend fun ensureUserProfile(
        displayName: String,
        defaultCity: String = "مكة المكرمة"
    ): Result<UserProfile> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                Result.success(existing ?: UserProfile(id = uid, userId = uid, displayName = displayName))
            } else {
                val safeName = displayName.trim().ifBlank { "مستخدم صلاتي" }.take(60)
                val code = "SL${uid.take(4).uppercase()}${(10..99).random()}"
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "friendCode" to code,
                    "totalPrayersLogged" to 0L,
                    "totalTasbihCount" to 0L,
                    "totalWirdPages" to 0L,
                    "currentStreak" to 0L,
                    "bestStreak" to 0L,
                    "calculationMethod" to "UMM_AL_QURA",
                    "cityName" to defaultCity.take(80),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                val created = docRef.get().await().toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                Result.success(created ?: UserProfile(id = uid, userId = uid, displayName = safeName, friendCode = code))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProfilePreferences(
        calculationMethod: String,
        cityName: String
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            docRef.update(
                mapOf(
                    "calculationMethod" to calculationMethod,
                    "cityName" to cityName.take(80),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Daily Logs (Prayers, Wird, Dhikr) ---

    fun observeDailyLogs(userId: String): Flow<List<DailyLog>> {
        val path = "users/$userId/dailyLogs"
        return db.collection("users").document(userId).collection("dailyLogs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DailyLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.dateKey }
            }
            .logFirestoreErrors(OperationType.LIST, path, userId)
    }

    suspend fun getOrCreateDailyLog(dateKey: String, defaultWirdTarget: Long = 4L): Result<DailyLog> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(DailyLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                Result.success(existing ?: DailyLog(id = dateKey, userId = uid, dateKey = dateKey))
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "dateKey" to dateKey,
                    "fajrDone" to false,
                    "dhuhrDone" to false,
                    "asrDone" to false,
                    "maghribDone" to false,
                    "ishaDone" to false,
                    "wirdPagesDone" to 0L,
                    "wirdTargetPages" to defaultWirdTarget.coerceIn(1L, 604L),
                    "dhikrCount" to 0L,
                    "morningAdhkarDone" to false,
                    "eveningAdhkarDone" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                val created = docRef.get().await().toObject(
                    DailyLog::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                Result.success(created ?: DailyLog(id = dateKey, userId = uid, dateKey = dateKey))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun togglePrayerStatus(dateKey: String, prayerField: String, newValue: Boolean): Result<Unit> {
        val uid = requireUserId()
        require(prayerField in setOf("fajrDone", "dhuhrDone", "asrDone", "maghribDone", "ishaDone")) {
            "Invalid prayer field: $prayerField"
        }
        val docRef = db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
        val userRef = db.collection("users").document(uid)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                getOrCreateDailyLog(dateKey).getOrThrow()
            }
            val delta = if (newValue) 1L else -1L
            val batch = db.batch()
            batch.update(
                docRef,
                mapOf(
                    prayerField to newValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                userRef,
                mapOf(
                    "totalPrayersLogged" to FieldValue.increment(delta),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateWirdProgress(dateKey: String, pagesDone: Long, targetPages: Long, previousPagesDone: Long): Result<Unit> {
        val uid = requireUserId()
        val safeDone = pagesDone.coerceIn(0L, 604L)
        val safeTarget = targetPages.coerceIn(1L, 604L)
        val docRef = db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
        val userRef = db.collection("users").document(uid)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                getOrCreateDailyLog(dateKey, safeTarget).getOrThrow()
            }
            val delta = safeDone - previousPagesDone
            val batch = db.batch()
            batch.update(
                docRef,
                mapOf(
                    "wirdPagesDone" to safeDone,
                    "wirdTargetPages" to safeTarget,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (delta != 0L) {
                batch.update(
                    userRef,
                    mapOf(
                        "totalWirdPages" to FieldValue.increment(delta),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun incrementDhikr(dateKey: String, delta: Long = 1L): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
        val userRef = db.collection("users").document(uid)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                getOrCreateDailyLog(dateKey).getOrThrow()
            }
            val batch = db.batch()
            batch.update(
                docRef,
                mapOf(
                    "dhikrCount" to FieldValue.increment(delta),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                userRef,
                mapOf(
                    "totalTasbihCount" to FieldValue.increment(delta),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleAdhkarPeriod(dateKey: String, isMorning: Boolean, completed: Boolean): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
        val field = if (isMorning) "morningAdhkarDone" else "eveningAdhkarDone"
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                getOrCreateDailyLog(dateKey).getOrThrow()
            }
            docRef.update(
                mapOf(
                    field to completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateStreakMetrics(currentStreak: Long, bestStreak: Long): Result<Unit> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        return try {
            userRef.update(
                mapOf(
                    "currentStreak" to currentStreak.coerceAtLeast(0L),
                    "bestStreak" to bestStreak.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            Result.failure(e)
        }
    }

    // --- Bookmarks (Duas & Tafsir) ---

    fun observeBookmarks(userId: String): Flow<List<Bookmark>> {
        val path = "users/$userId/bookmarks"
        return db.collection("users").document(userId).collection("bookmarks")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Bookmark::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .logFirestoreErrors(OperationType.LIST, path, userId)
    }

    suspend fun saveBookmark(
        bookmarkId: String,
        itemType: String,
        title: String,
        subtitle: String,
        referenceKey: String
    ): Result<Unit> {
        val uid = requireUserId()
        val safeId = bookmarkId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val docRef = db.collection("users").document(uid).collection("bookmarks").document(safeId)
        return try {
            val payload = mapOf(
                "userId" to uid,
                "itemType" to itemType,
                "title" to title.take(120),
                "subtitle" to subtitle.take(200),
                "referenceKey" to referenceKey.take(100),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun removeBookmark(bookmarkId: String): Result<Unit> {
        val uid = requireUserId()
        val safeId = bookmarkId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val docRef = db.collection("users").document(uid).collection("bookmarks").document(safeId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Group Memorization Circles (Halaqat) ---

    fun observePublicCircles(): Flow<List<MemorizationCircle>> {
        val path = "circles"
        return db.collection(path)
            .whereEqualTo("isPublic", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(MemorizationCircle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.memberCount }
            }
            .logFirestoreErrors(OperationType.LIST, path)
    }

    fun observeUserCircles(userId: String): Flow<List<MemorizationCircle>> {
        val path = "circles"
        return db.collection(path)
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(MemorizationCircle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt }
            }
            .logFirestoreErrors(OperationType.LIST, path, userId)
    }

    fun observeCircleMembers(circle: MemorizationCircle, currentUserId: String): Flow<List<CircleMember>> {
        val path = "circles/${circle.id}/members"
        val query = when {
            circle.isPublic -> db.collection("circles").document(circle.id).collection("members")
                .whereEqualTo("isPublic", true)
            circle.ownerId == currentUserId -> db.collection("circles").document(circle.id).collection("members")
                .whereEqualTo("circleOwnerId", currentUserId)
            else -> db.collection("circles").document(circle.id).collection("members")
                .whereEqualTo("userId", currentUserId)
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(CircleMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.memorizedVerses }
            }
            .logFirestoreErrors(OperationType.LIST, path, currentUserId)
    }

    suspend fun createCircle(
        title: String,
        surahName: String,
        targetVerses: Long,
        displayName: String,
        isPublic: Boolean = true
    ): Result<String> {
        val uid = requireUserId()
        val circleId = "circle_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val inviteCode = "Q${UUID.randomUUID().toString().replace("-", "").take(5).uppercase()}"
        val circleRef = db.collection("circles").document(circleId)
        val memberRef = circleRef.collection("members").document(uid)
        return try {
            val circlePayload = mapOf(
                "ownerId" to uid,
                "title" to title.trim().take(80),
                "surahName" to surahName.trim().take(60),
                "targetVerses" to targetVerses.coerceIn(1L, 6236L),
                "inviteCode" to inviteCode,
                "isPublic" to isPublic,
                "memberCount" to 1L,
                "totalMemorizedVerses" to 0L,
                "memberUids" to listOf(uid),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val memberPayload = mapOf(
                "userId" to uid,
                "circleId" to circleId,
                "circleOwnerId" to uid,
                "isPublic" to isPublic,
                "displayName" to displayName.trim().ifBlank { "حافظ القرآن" }.take(60),
                "memorizedVerses" to 0L,
                "lastSurahNote" to "بداية الحفظ في $surahName".take(120),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            // Create circle first so exists() in member create rule evaluates cleanly
            circleRef.set(circlePayload).await()
            memberRef.set(memberPayload).await()
            Result.success(circleId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, circleRef.path)
            Result.failure(e)
        }
    }

    suspend fun joinCircle(circle: MemorizationCircle, displayName: String): Result<Unit> {
        val uid = requireUserId()
        val circleRef = db.collection("circles").document(circle.id)
        val memberRef = circleRef.collection("members").document(uid)
        return try {
            if (!circle.memberUids.contains(uid)) {
                circleRef.update(
                    mapOf(
                        "memberUids" to FieldValue.arrayUnion(uid),
                        "memberCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            val memberSnap = memberRef.get().await()
            if (!memberSnap.exists()) {
                val memberPayload = mapOf(
                    "userId" to uid,
                    "circleId" to circle.id,
                    "circleOwnerId" to circle.ownerId,
                    "isPublic" to circle.isPublic,
                    "displayName" to displayName.trim().ifBlank { "عضو الحلقة" }.take(60),
                    "memorizedVerses" to 0L,
                    "lastSurahNote" to "انضم إلى ${circle.surahName}".take(120),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                memberRef.set(memberPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, circleRef.path)
            Result.failure(e)
        }
    }

    suspend fun logCircleMemorization(
        circle: MemorizationCircle,
        newMemorizedVerses: Long,
        addedDelta: Long,
        note: String
    ): Result<Unit> {
        val uid = requireUserId()
        val circleRef = db.collection("circles").document(circle.id)
        val memberRef = circleRef.collection("members").document(uid)
        return try {
            val batch = db.batch()
            batch.update(
                memberRef,
                mapOf(
                    "memorizedVerses" to newMemorizedVerses.coerceIn(0L, 6236L),
                    "lastSurahNote" to note.trim().take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (addedDelta > 0L) {
                batch.update(
                    circleRef,
                    mapOf(
                        "totalMemorizedVerses" to FieldValue.increment(addedDelta),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, memberRef.path)
            Result.failure(e)
        }
    }

    // --- Social Achievements Feed ---

    fun observePublicAchievements(): Flow<List<AchievementPost>> {
        val path = "achievements"
        return db.collection(path)
            .whereEqualTo("isPublic", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AchievementPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .logFirestoreErrors(OperationType.LIST, path)
    }

    suspend fun shareAchievement(
        authorName: String,
        badgeTitle: String,
        description: String,
        category: String,
        streakDays: Long
    ): Result<String> {
        val uid = requireUserId()
        val achievementId = "ach_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("achievements").document(achievementId)
        return try {
            val payload = mapOf(
                "userId" to uid,
                "authorName" to authorName.trim().ifBlank { "مستخدم صلاتي" }.take(60),
                "badgeTitle" to badgeTitle.trim().take(80),
                "description" to description.trim().take(240),
                "category" to category,
                "streakDays" to streakDays.coerceIn(0L, 36500L),
                "ameenCount" to 0L,
                "isPublic" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(achievementId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendAmeen(achievementId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("achievements").document(achievementId)
        return try {
            docRef.update(
                mapOf(
                    "ameenCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Custom User-Authored Adhkar & Duas ---

    fun observeCustomSupplications(userId: String): Flow<List<CustomSupplication>> {
        val path = "users/$userId/customSupplications"
        return db.collection("users").document(userId).collection("customSupplications")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CustomSupplication::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .logFirestoreErrors(OperationType.LIST, path, userId)
    }

    suspend fun addCustomSupplication(
        supplicationType: String,
        title: String,
        arabicText: String,
        occasionNote: String,
        targetCount: Long
    ): Result<String> {
        val uid = requireUserId()
        val id = "sup_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("users").document(uid)
            .collection("customSupplications").document(id)
        return try {
            val payload = mapOf(
                "userId" to uid,
                "supplicationType" to supplicationType,
                "title" to title.trim().take(120),
                "arabicText" to arabicText.trim().take(1000),
                "occasionNote" to occasionNote.trim().take(160),
                "targetCount" to targetCount.coerceIn(1L, 1000L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomSupplication(supplicationId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("customSupplications").document(supplicationId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
