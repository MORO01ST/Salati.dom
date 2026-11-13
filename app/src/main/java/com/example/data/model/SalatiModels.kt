package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val friendCode: String = "",
    val totalPrayersLogged: Long = 0L,
    val totalTasbihCount: Long = 0L,
    val totalWirdPages: Long = 0L,
    val currentStreak: Long = 0L,
    val bestStreak: Long = 0L,
    val calculationMethod: String = "UMM_AL_QURA",
    val cityName: String = "مكة المكرمة",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DailyLog(
    @DocumentId val id: String = "",
    val userId: String = "",
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
    val eveningAdhkarDone: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val completedPrayersCount: Int
        get() = listOf(fajrDone, dhuhrDone, asrDone, maghribDone, ishaDone).count { it }
}

@IgnoreExtraProperties
data class Bookmark(
    @DocumentId val id: String = "",
    val userId: String = "",
    val itemType: String = "DUA",
    val title: String = "",
    val subtitle: String = "",
    val referenceKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MemorizationCircle(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val surahName: String = "",
    val targetVerses: Long = 30L,
    val inviteCode: String = "",
    val isPublic: Boolean = true,
    val memberCount: Long = 1L,
    val totalMemorizedVerses: Long = 0L,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CircleMember(
    @DocumentId val id: String = "",
    val userId: String = "",
    val circleId: String = "",
    val circleOwnerId: String = "",
    val isPublic: Boolean = true,
    val displayName: String = "",
    val memorizedVerses: Long = 0L,
    val lastSurahNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AchievementPost(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val badgeTitle: String = "",
    val description: String = "",
    val category: String = "PRAYER",
    val streakDays: Long = 0L,
    val ameenCount: Long = 0L,
    val isPublic: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CustomSupplication(
    @DocumentId val id: String = "",
    val userId: String = "",
    val supplicationType: String = "DUA",
    val title: String = "",
    val arabicText: String = "",
    val occasionNote: String = "",
    val targetCount: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
