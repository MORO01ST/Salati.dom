package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.content.IslamicContentData
import com.example.data.content.SurahTafsirBundle
import com.example.data.model.AchievementPost
import com.example.data.model.Bookmark
import com.example.data.model.CircleMember
import com.example.data.model.CustomSupplication
import com.example.data.model.DailyLog
import com.example.data.model.MemorizationCircle
import com.example.data.model.UserProfile
import com.example.data.network.AlQuranSurahMeta
import com.example.data.network.NetworkStatusMonitor
import com.example.data.network.PrayerTimeSource
import com.example.data.network.PrayerTimesService
import com.example.data.network.QuranCloudRepository
import com.example.data.preferences.LocalPrayerPreferences
import com.example.data.preferences.LocalSalatiStore
import com.example.data.preferences.NotificationPrefsStore
import com.example.data.repository.SalatiRepository
import com.example.domain.CalculationMethod
import com.example.domain.CityCoordinate
import com.example.domain.HijriCalendarHelper
import com.example.domain.HijriDateInfo
import com.example.domain.HijriMonthDay
import com.example.domain.PrayerTimeEntry
import com.example.domain.PrayerTimesCalculator
import com.example.domain.PrayerType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class NextPrayerCountdown(
    val nextPrayer: PrayerTimeEntry,
    val remainingHours: Int,
    val remainingMinutes: Int,
    val remainingSeconds: Int
)

data class WeeklyBarPoint(
    val label: String,
    val prayersCompleted: Int,
    val wirdPages: Int
)

data class SpiritualAnalytics(
    val weeklyConsistencyPercent: Int,
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    val totalPrayers: Int,
    val totalWirdPages: Int,
    val totalTasbih: Int,
    val weeklyBars: List<WeeklyBarPoint>,
    val earnedBadges: List<String>
)

sealed interface UiStatus {
    data object Idle : UiStatus
    data class Message(val text: String, val isError: Boolean = false) : UiStatus
}

@OptIn(ExperimentalCoroutinesApi::class)
class SalatiViewModel(
    private val repository: SalatiRepository?,
    private val localStore: LocalSalatiStore,
    private val prefsStore: NotificationPrefsStore,
    private val networkMonitor: NetworkStatusMonitor,
    val isGuestMode: Boolean,
    val currentUserId: String,
    private val initialDisplayName: String,
    private val onScheduleAlarms: (List<PrayerTimeEntry>, List<PrayerTimeEntry>, LocalPrayerPreferences) -> Unit
) : ViewModel() {

    private val _status = MutableStateFlow<UiStatus>(UiStatus.Idle)
    val status: StateFlow<UiStatus> = _status.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), networkMonitor.isCurrentlyOnline())

    private val _selectedCircle = MutableStateFlow<MemorizationCircle?>(null)
    val selectedCircle: StateFlow<MemorizationCircle?> = _selectedCircle.asStateFlow()

    private val _selectedSurah = MutableStateFlow(IslamicContentData.bundledSurahs.first())
    val selectedSurah: StateFlow<SurahTafsirBundle> = _selectedSurah.asStateFlow()

    private val _remoteSurahDirectory = MutableStateFlow<List<AlQuranSurahMeta>>(emptyList())
    val remoteSurahDirectory: StateFlow<List<AlQuranSurahMeta>> = _remoteSurahDirectory.asStateFlow()

    private val _isLoadingSurah = MutableStateFlow(false)
    val isLoadingSurah: StateFlow<Boolean> = _isLoadingSurah.asStateFlow()

    private val _prayerSource = MutableStateFlow(PrayerTimeSource.OFFLINE_ASTRONOMICAL)
    val prayerSource: StateFlow<PrayerTimeSource> = _prayerSource.asStateFlow()

    private val _isFetchingPrayers = MutableStateFlow(false)
    val isFetchingPrayers: StateFlow<Boolean> = _isFetchingPrayers.asStateFlow()

    private val prayerRefreshTick = MutableStateFlow(0)
    private val _tomorrowPrayerTimes = MutableStateFlow<List<PrayerTimeEntry>>(emptyList())

    val localPrefs: StateFlow<LocalPrayerPreferences> = prefsStore.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalPrayerPreferences())

    private val useLocalOnly: Boolean
        get() = isGuestMode || repository == null

    val userProfile: StateFlow<UserProfile?> = if (useLocalOnly) {
        flowOf(
            UserProfile(
                id = GUEST_USER_ID,
                userId = GUEST_USER_ID,
                displayName = initialDisplayName,
                friendCode = "وضع محلي"
            )
        )
    } else {
        repository!!.observeUserProfile(currentUserId)
            .catch { error ->
                handleStreamError("userProfile", error)
                emit(null)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val dailyLogs: StateFlow<List<DailyLog>> = if (useLocalOnly) {
        localStore.observeDailyLogs(currentUserId)
    } else {
        repository!!.observeDailyLogs(currentUserId)
            .catch { error ->
                handleStreamError("dailyLogs", error)
                emit(emptyList())
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val bookmarks: StateFlow<List<Bookmark>> = if (useLocalOnly) {
        localStore.observeBookmarks(currentUserId)
    } else {
        repository!!.observeBookmarks(currentUserId)
            .catch { error ->
                handleStreamError("bookmarks", error)
                emit(emptyList())
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val customSupplications: StateFlow<List<CustomSupplication>> = if (useLocalOnly) {
        localStore.observeCustomSupplications(currentUserId)
    } else {
        repository!!.observeCustomSupplications(currentUserId)
            .catch { error ->
                handleStreamError("customSupplications", error)
                emit(emptyList())
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val publicCircles: StateFlow<List<MemorizationCircle>> = if (useLocalOnly) {
        flowOf(emptyList())
    } else {
        repository!!.observePublicCircles()
            .catch { error ->
                handleStreamError("publicCircles", error)
                emit(emptyList())
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myCircles: StateFlow<List<MemorizationCircle>> = if (useLocalOnly) {
        flowOf(emptyList())
    } else {
        repository!!.observeUserCircles(currentUserId)
            .catch { error ->
                handleStreamError("myCircles", error)
                emit(emptyList())
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val selectedCircleMembers: StateFlow<List<CircleMember>> = _selectedCircle
        .flatMapLatest { circle ->
            if (circle == null || useLocalOnly) {
                flowOf(emptyList())
            } else {
                repository!!.observeCircleMembers(circle, currentUserId)
                    .catch { error ->
                        handleStreamError("circleMembers", error)
                        emit(emptyList())
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val publicAchievements: StateFlow<List<AchievementPost>> = if (useLocalOnly) {
        flowOf(emptyList())
    } else {
        repository!!.observePublicAchievements()
            .catch { error ->
                handleStreamError("publicAchievements", error)
                emit(emptyList())
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val todayKey: String
        get() = HijriCalendarHelper.getTodayKey()

    val todayLog: StateFlow<DailyLog> = dailyLogs
        .map { logs ->
            logs.firstOrNull { it.dateKey == todayKey }
                ?: DailyLog(id = todayKey, userId = currentUserId, dateKey = todayKey)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            DailyLog(id = todayKey, userId = currentUserId, dateKey = todayKey)
        )

    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    val prayerTimes: StateFlow<List<PrayerTimeEntry>> = combine(localPrefs, prayerRefreshTick) { prefs, _ -> prefs }
        .flatMapLatest { prefs ->
            flow {
                val method = CalculationMethod.fromId(prefs.calculationMethod)
                val todayCal = Calendar.getInstance()
                val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

                val initialToday = PrayerTimesCalculator.calculateForDate(
                    latitude = prefs.latitude,
                    longitude = prefs.longitude,
                    year = todayCal.get(Calendar.YEAR),
                    month = todayCal.get(Calendar.MONTH) + 1,
                    day = todayCal.get(Calendar.DAY_OF_MONTH),
                    method = method,
                    hanafiAsr = prefs.hanafiAsr
                )
                val initialTomorrow = PrayerTimesCalculator.calculateForDate(
                    latitude = prefs.latitude,
                    longitude = prefs.longitude,
                    year = tomorrowCal.get(Calendar.YEAR),
                    month = tomorrowCal.get(Calendar.MONTH) + 1,
                    day = tomorrowCal.get(Calendar.DAY_OF_MONTH),
                    method = method,
                    hanafiAsr = prefs.hanafiAsr
                )
                _prayerSource.value = PrayerTimeSource.OFFLINE_ASTRONOMICAL
                _tomorrowPrayerTimes.value = initialTomorrow
                onScheduleAlarms(initialToday, initialTomorrow, prefs)
                emit(initialToday)

                if (!networkMonitor.isCurrentlyOnline()) {
                    return@flow
                }

                _isFetchingPrayers.value = true
                try {
                    val todayFetch = PrayerTimesService.fetchDailyPrayerTimes(
                        latitude = prefs.latitude,
                        longitude = prefs.longitude,
                        year = todayCal.get(Calendar.YEAR),
                        month = todayCal.get(Calendar.MONTH) + 1,
                        day = todayCal.get(Calendar.DAY_OF_MONTH),
                        method = method,
                        hanafiAsr = prefs.hanafiAsr
                    )
                    val tomorrowFetch = PrayerTimesService.fetchDailyPrayerTimes(
                        latitude = prefs.latitude,
                        longitude = prefs.longitude,
                        year = tomorrowCal.get(Calendar.YEAR),
                        month = tomorrowCal.get(Calendar.MONTH) + 1,
                        day = tomorrowCal.get(Calendar.DAY_OF_MONTH),
                        method = method,
                        hanafiAsr = prefs.hanafiAsr
                    )
                    _prayerSource.value = todayFetch.source
                    _tomorrowPrayerTimes.value = tomorrowFetch.prayers
                    onScheduleAlarms(todayFetch.prayers, tomorrowFetch.prayers, prefs)
                    emit(todayFetch.prayers)
                } finally {
                    _isFetchingPrayers.value = false
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val nextPrayerCountdown: StateFlow<NextPrayerCountdown?> = combine(
        prayerTimes,
        _tomorrowPrayerTimes,
        tickerFlow,
        localPrefs
    ) { prayers, tomorrowPrayers, nowMs, prefs ->
        if (prayers.isEmpty()) return@combine null
        val upcoming = prayers.firstOrNull { it.type != PrayerType.SUNRISE && it.timestampMillis > nowMs }
        val target = if (upcoming != null) {
            upcoming
        } else {
            val tomorrowList = tomorrowPrayers.ifEmpty {
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                PrayerTimesCalculator.calculateForDate(
                    latitude = prefs.latitude,
                    longitude = prefs.longitude,
                    year = cal.get(Calendar.YEAR),
                    month = cal.get(Calendar.MONTH) + 1,
                    day = cal.get(Calendar.DAY_OF_MONTH),
                    method = CalculationMethod.fromId(prefs.calculationMethod),
                    hanafiAsr = prefs.hanafiAsr
                )
            }
            tomorrowList.firstOrNull { it.type == PrayerType.FAJR } ?: prayers.first()
        }
        val diffSec = ((target.timestampMillis - nowMs) / 1000L).coerceAtLeast(0L)
        NextPrayerCountdown(
            nextPrayer = target,
            remainingHours = (diffSec / 3600L).toInt(),
            remainingMinutes = ((diffSec % 3600L) / 60L).toInt(),
            remainingSeconds = (diffSec % 60L).toInt()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val hijriMonthGrid: StateFlow<Pair<HijriDateInfo, List<HijriMonthDay>>> = localPrefs
        .map { prefs -> HijriCalendarHelper.getCurrentHijriMonthGrid(prefs.hijriAdjustment) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            HijriCalendarHelper.getCurrentHijriMonthGrid(0)
        )

    val qiblaBearing: StateFlow<Double> = localPrefs
        .map { PrayerTimesCalculator.calculateQiblaBearing(it.latitude, it.longitude) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val distanceToKaabaKm: StateFlow<Int> = localPrefs
        .map { PrayerTimesCalculator.calculateDistanceKmToKaaba(it.latitude, it.longitude) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val spiritualAnalytics: StateFlow<SpiritualAnalytics> = combine(dailyLogs, userProfile) { logs, profile ->
        val logsByDate = logs.associateBy { it.dateKey }
        val weeklyBars = (6 downTo 0).map { offset ->
            val key = HijriCalendarHelper.getTodayKey(-offset)
            val log = logsByDate[key]
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val shortLabel = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SATURDAY -> "سبت"
                Calendar.SUNDAY -> "أحد"
                Calendar.MONDAY -> "إثن"
                Calendar.TUESDAY -> "ثلا"
                Calendar.WEDNESDAY -> "أرب"
                Calendar.THURSDAY -> "خمي"
                else -> "جمع"
            }
            WeeklyBarPoint(
                label = shortLabel,
                prayersCompleted = log?.completedPrayersCount ?: 0,
                wirdPages = (log?.wirdPagesDone ?: 0L).toInt()
            )
        }

        val totalWeekPrayers = weeklyBars.sumOf { it.prayersCompleted }
        val consistency = ((totalWeekPrayers / 35.0) * 100.0).toInt().coerceIn(0, 100)

        var streak = 0
        for (offset in 0..60) {
            val key = HijriCalendarHelper.getTodayKey(-offset)
            val dayLog = logsByDate[key]
            val activeDay = dayLog != null && (dayLog.completedPrayersCount >= 3 || dayLog.wirdPagesDone > 0)
            if (activeDay) {
                streak++
            } else if (offset > 0) {
                break
            }
        }
        val best = maxOf(streak, (profile?.bestStreak ?: 0L).toInt())
        val totalPrayers = maxOf(logs.sumOf { it.completedPrayersCount }, (profile?.totalPrayersLogged ?: 0L).toInt())
        val totalWird = maxOf(logs.sumOf { it.wirdPagesDone.toInt() }, (profile?.totalWirdPages ?: 0L).toInt())
        val totalTasbih = maxOf(logs.sumOf { it.dhikrCount.toInt() }, (profile?.totalTasbihCount ?: 0L).toInt())

        val badges = buildList {
            if (totalPrayers >= 5) add("محافظ على الفرائض")
            if (streak >= 3) add("سلسلة التزام ٣ أيام")
            if (streak >= 7) add("وسام المثابرة الأسبوعية")
            if (totalWird >= 10) add("قارئ الورد القرآني")
            if (totalTasbih >= 100) add("الذاكرون الله كثيراً")
        }

        SpiritualAnalytics(
            weeklyConsistencyPercent = consistency,
            currentStreakDays = streak,
            bestStreakDays = best,
            totalPrayers = totalPrayers,
            totalWirdPages = totalWird,
            totalTasbih = totalTasbih,
            weeklyBars = weeklyBars,
            earnedBadges = badges
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        SpiritualAnalytics(0, 0, 0, 0, 0, 0, emptyList(), emptyList())
    )

    init {
        viewModelScope.launch {
            if (useLocalOnly) {
                localStore.ensureDailyLog(todayKey)
            } else if (networkMonitor.isCurrentlyOnline()) {
                repository!!.ensureUserProfile(initialDisplayName)
                repository.getOrCreateDailyLog(todayKey)
            }
        }
        viewModelScope.launch {
            if (networkMonitor.isCurrentlyOnline()) {
                QuranCloudRepository.fetchAllSurahSummaries().onSuccess { list ->
                    _remoteSurahDirectory.value = list
                }
            }
        }
    }

    fun clearStatus() {
        _status.value = UiStatus.Idle
    }

    fun exitGuestToSignIn() {
        viewModelScope.launch {
            localStore.setGuestMode(false)
        }
    }

    fun togglePrayer(type: PrayerType) {
        val field = type.firestoreField ?: return
        val current = todayLog.value
        val currentVal = when (type) {
            PrayerType.FAJR -> current.fajrDone
            PrayerType.DHUHR -> current.dhuhrDone
            PrayerType.ASR -> current.asrDone
            PrayerType.MAGHRIB -> current.maghribDone
            PrayerType.ISHA -> current.ishaDone
            PrayerType.SUNRISE -> false
        }
        viewModelScope.launch {
            if (useLocalOnly) {
                localStore.togglePrayer(todayKey, field, !currentVal)
            } else {
                repository!!.togglePrayerStatus(todayKey, field, !currentVal)
                    .onFailure { _status.value = UiStatus.Message("تعذر تحديث حالة الصلاة", true) }
            }
        }
    }

    fun updateWird(pagesDone: Long, targetPages: Long) {
        val prevDone = todayLog.value.wirdPagesDone
        viewModelScope.launch {
            if (useLocalOnly) {
                localStore.updateWird(todayKey, pagesDone, targetPages)
            } else {
                repository!!.updateWirdProgress(todayKey, pagesDone, targetPages, prevDone)
                    .onSuccess {
                        val analytics = spiritualAnalytics.value
                        repository.updateStreakMetrics(
                            analytics.currentStreakDays.toLong(),
                            analytics.bestStreakDays.toLong()
                        )
                    }
                    .onFailure { _status.value = UiStatus.Message("تعذر حفظ تقدم الورد", true) }
            }
        }
    }

    fun incrementTasbih(delta: Long = 1L) {
        viewModelScope.launch {
            if (useLocalOnly) {
                localStore.incrementDhikr(todayKey, delta)
            } else {
                repository!!.incrementDhikr(todayKey, delta)
                    .onFailure { _status.value = UiStatus.Message("تعذر حفظ التسبيح", true) }
            }
        }
    }

    fun toggleAdhkarPeriod(isMorning: Boolean) {
        val current = if (isMorning) todayLog.value.morningAdhkarDone else todayLog.value.eveningAdhkarDone
        viewModelScope.launch {
            if (useLocalOnly) {
                localStore.toggleAdhkarPeriod(todayKey, isMorning, !current)
            } else {
                repository!!.toggleAdhkarPeriod(todayKey, isMorning, !current)
            }
        }
    }

    fun toggleBookmark(itemType: String, title: String, subtitle: String, referenceKey: String) {
        val bookmarkId = "${itemType.lowercase()}_$referenceKey"
        val exists = bookmarks.value.any { it.referenceKey == referenceKey && it.itemType == itemType }
        viewModelScope.launch {
            if (useLocalOnly) {
                if (exists) {
                    localStore.removeBookmark(bookmarkId)
                    _status.value = UiStatus.Message("تمت الإزالة من المحفوظات المحلية")
                } else {
                    localStore.saveBookmark(bookmarkId, itemType, title, subtitle, referenceKey)
                    _status.value = UiStatus.Message("تم الحفظ محلياً في قائمتي المفضلة")
                }
            } else {
                if (exists) {
                    repository!!.removeBookmark(bookmarkId)
                    _status.value = UiStatus.Message("تمت الإزالة من المحفوظات")
                } else {
                    repository!!.saveBookmark(bookmarkId, itemType, title, subtitle, referenceKey)
                    _status.value = UiStatus.Message("تم الحفظ في قائمتي المفضلة")
                }
            }
        }
    }

    fun addCustomSupplication(
        supplicationType: String,
        title: String,
        arabicText: String,
        occasionNote: String,
        targetCount: Long
    ) {
        if (title.isBlank() || arabicText.isBlank()) {
            _status.value = UiStatus.Message("يرجى إدخال العنوان ونص الذكر أو الدعاء", true)
            return
        }
        viewModelScope.launch {
            if (useLocalOnly) {
                localStore.addCustomSupplication(
                    supplicationType = supplicationType,
                    title = title,
                    arabicText = arabicText,
                    occasionNote = occasionNote,
                    targetCount = targetCount
                )
                _status.value = UiStatus.Message("تم الحفظ محلياً في قائمتك المفضلة")
            } else {
                repository!!.addCustomSupplication(
                    supplicationType = supplicationType,
                    title = title,
                    arabicText = arabicText,
                    occasionNote = occasionNote,
                    targetCount = targetCount
                ).onSuccess {
                    _status.value = UiStatus.Message("تمت الإضافة إلى قائمتك المفضلة")
                }.onFailure {
                    _status.value = UiStatus.Message("تعذر حفظ الذكر أو الدعاء المخصص", true)
                }
            }
        }
    }

    fun deleteCustomSupplication(supplicationId: String) {
        viewModelScope.launch {
            if (useLocalOnly) {
                localStore.deleteCustomSupplication(supplicationId)
                _status.value = UiStatus.Message("تم الحذف من قائمتك المفضلة")
            } else {
                repository!!.deleteCustomSupplication(supplicationId)
                    .onSuccess { _status.value = UiStatus.Message("تم الحذف من قائمتك المفضلة") }
                    .onFailure { _status.value = UiStatus.Message("تعذر حذف العنصر", true) }
            }
        }
    }

    fun selectBundledSurah(surah: SurahTafsirBundle) {
        _selectedSurah.value = surah
    }

    fun loadSurahByNumber(surahNumber: Int) {
        val bundled = IslamicContentData.bundledSurahs.firstOrNull {
            it.surahNumber == surahNumber && it.totalAyahs == it.ayahs.size
        }
        if (bundled != null) {
            _selectedSurah.value = bundled
            return
        }
        viewModelScope.launch {
            _isLoadingSurah.value = true
            val cached = localStore.getCachedSurah(surahNumber)
            if (cached != null) {
                _selectedSurah.value = cached
                if (!networkMonitor.isCurrentlyOnline()) {
                    _isLoadingSurah.value = false
                    _status.value = UiStatus.Message("تم فتح السورة من الذاكرة المحلية (بدون إنترنت)")
                    return@launch
                }
            } else if (!networkMonitor.isCurrentlyOnline()) {
                _isLoadingSurah.value = false
                _status.value = UiStatus.Message(
                    "هذه السورة غير محفوظة محلياً. تتوفر السور المدمجة أو السور المحملة مسبقاً بدون إنترنت",
                    true
                )
                return@launch
            }

            QuranCloudRepository.fetchSurahWithTafsir(surahNumber)
                .onSuccess { surah ->
                    _selectedSurah.value = surah
                    localStore.cacheSurah(surah)
                }
                .onFailure {
                    if (cached == null) {
                        _status.value = UiStatus.Message("تعذر تحميل السورة من الإنترنت، تأكد من الاتصال", true)
                    }
                }
            _isLoadingSurah.value = false
        }
    }

    fun selectCity(city: CityCoordinate) {
        viewModelScope.launch {
            prefsStore.setLocation(city.name, city.latitude, city.longitude)
            if (!useLocalOnly && networkMonitor.isCurrentlyOnline()) {
                repository?.updateProfilePreferences(localPrefs.value.calculationMethod, city.name)
            }
            _status.value = UiStatus.Message("تم تحديث الموقع إلى ${city.name}")
        }
    }

    fun updateGpsCoordinates(
        latitude: Double,
        longitude: Double,
        label: String = "موقعي الحالي (GPS)",
        silent: Boolean = false
    ) {
        viewModelScope.launch {
            prefsStore.setLocation(label, latitude, longitude)
            if (!useLocalOnly && networkMonitor.isCurrentlyOnline()) {
                repository?.updateProfilePreferences(localPrefs.value.calculationMethod, label)
            }
            if (!silent) {
                _status.value = UiStatus.Message("تم تحديث المواقيت واتجاه القبلة حسب موقعك: $label")
            }
        }
    }

    fun refreshPrayerTimes() {
        prayerRefreshTick.value += 1
    }

    fun setCalculationMethod(method: CalculationMethod, hanafiAsr: Boolean) {
        viewModelScope.launch {
            prefsStore.setCalculationMethod(method.id, hanafiAsr)
            if (!useLocalOnly && networkMonitor.isCurrentlyOnline()) {
                repository?.updateProfilePreferences(method.id, localPrefs.value.cityName)
            }
            _status.value = UiStatus.Message("تم اعتماد طريقة ${method.arabicName}")
        }
    }

    fun togglePrayerAlert(prayerKey: String, enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.setPrayerAlert(prayerKey, enabled)
        }
    }

    fun setNotifyAtPrayerStart(enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.setNotifyAtPrayerStart(enabled)
        }
    }

    fun setAlertLeadMinutes(minutes: Int) {
        viewModelScope.launch {
            prefsStore.setLeadMinutes(minutes)
        }
    }

    fun setHijriAdjustment(days: Int) {
        viewModelScope.launch {
            prefsStore.setHijriAdjustment(days)
        }
    }

    fun selectCircle(circle: MemorizationCircle?) {
        _selectedCircle.value = circle
    }

    fun createCircle(title: String, surahName: String, targetVerses: Long) {
        if (useLocalOnly) {
            _status.value = UiStatus.Message("يرجى تسجيل الدخول بحساب Google لاستخدام حلقات التحفيظ الجماعي", true)
            return
        }
        val displayName = userProfile.value?.displayName ?: initialDisplayName
        viewModelScope.launch {
            repository!!.createCircle(title, surahName, targetVerses, displayName, isPublic = true)
                .onSuccess { _status.value = UiStatus.Message("تم إنشاء حلقة التحفيظ بنجاح") }
                .onFailure { _status.value = UiStatus.Message("تعذر إنشاء الحلقة", true) }
        }
    }

    fun joinCircle(circle: MemorizationCircle) {
        if (useLocalOnly) {
            _status.value = UiStatus.Message("يرجى تسجيل الدخول بحساب Google للانضمام إلى الحلقات", true)
            return
        }
        val displayName = userProfile.value?.displayName ?: initialDisplayName
        viewModelScope.launch {
            repository!!.joinCircle(circle, displayName)
                .onSuccess {
                    _selectedCircle.value = circle
                    _status.value = UiStatus.Message("تم الانضمام إلى ${circle.title}")
                }
                .onFailure { _status.value = UiStatus.Message("تعذر الانضمام إلى الحلقة", true) }
        }
    }

    fun logMyMemorizedVerses(circle: MemorizationCircle, addedVerses: Long, note: String) {
        if (useLocalOnly) {
            _status.value = UiStatus.Message("يرجى تسجيل الدخول بحساب Google لمزامنة الحفظ الجماعي", true)
            return
        }
        val currentMember = selectedCircleMembers.value.firstOrNull { it.userId == currentUserId }
        val prev = currentMember?.memorizedVerses ?: 0L
        val updated = (prev + addedVerses).coerceAtMost(6236L)
        viewModelScope.launch {
            repository!!.logCircleMemorization(circle, updated, addedVerses, note)
                .onSuccess { _status.value = UiStatus.Message("تم تسجيل حفظ $addedVerses آية") }
                .onFailure { _status.value = UiStatus.Message("تعذر تسجيل الحفظ", true) }
        }
    }

    fun shareAchievement(badgeTitle: String, description: String, category: String) {
        if (useLocalOnly) {
            _status.value = UiStatus.Message("سجّل دخولك بحساب Google للنشر في الساحة العامة أو استخدم زر المشاركة المحلية", true)
            return
        }
        val author = userProfile.value?.displayName ?: initialDisplayName
        val streak = spiritualAnalytics.value.currentStreakDays.toLong()
        viewModelScope.launch {
            repository!!.shareAchievement(author, badgeTitle, description, category, streak)
                .onSuccess { _status.value = UiStatus.Message("تمت مشاركة إنجازك مع الأصدقاء") }
                .onFailure { _status.value = UiStatus.Message("تعذر مشاركة الإنجاز", true) }
        }
    }

    fun sendAmeen(achievementId: String) {
        if (useLocalOnly) return
        viewModelScope.launch {
            repository!!.sendAmeen(achievementId)
        }
    }

    private fun handleStreamError(streamName: String, error: Throwable) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $streamName", rootError)
        if (networkMonitor.isCurrentlyOnline()) {
            _status.value = UiStatus.Message(rootError.localizedMessage ?: "تعذر المزامنة مع السحابة", true)
        }
    }

    companion object {
        private const val TAG = "SalatiVM"
        const val GUEST_USER_ID = "guest_local_user"
    }
}
