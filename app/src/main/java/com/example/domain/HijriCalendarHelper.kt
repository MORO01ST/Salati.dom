package com.example.domain

import android.os.Build
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Calendar

data class HijriDateInfo(
    val day: Int,
    val month: Int, // 1..12
    val year: Int,
    val monthName: String,
    val dayOfWeekArabic: String,
    val eventTitle: String? = null
) {
    val formattedArabic: String
        get() = "$day $monthName $year هـ"
}

data class HijriMonthDay(
    val hijriDay: Int,
    val gregorianDay: Int,
    val gregorianMonth: Int,
    val gregorianYear: Int,
    val isToday: Boolean,
    val isWhiteDay: Boolean,
    val isSunnahFastingDay: Boolean,
    val eventTitle: String?
)

object HijriCalendarHelper {

    val hijriMonths = listOf(
        "محرم",
        "صفر",
        "ربيع الأول",
        "ربيع الآخر",
        "جمادى الأولى",
        "جمادى الآخرة",
        "رجب",
        "شعبان",
        "رمضان",
        "شوال",
        "ذو القعدة",
        "ذو الحجة"
    )

    private val arabicWeekDays = mapOf(
        Calendar.SATURDAY to "السبت",
        Calendar.SUNDAY to "الأحد",
        Calendar.MONDAY to "الإثنين",
        Calendar.TUESDAY to "الثلاثاء",
        Calendar.WEDNESDAY to "الأربعاء",
        Calendar.THURSDAY to "الخميس",
        Calendar.FRIDAY to "الجمعة"
    )

    fun getTodayKey(offsetDays: Int = 0): String {
        val cal = Calendar.getInstance()
        if (offsetDays != 0) cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        return "%04d-%02d-%02d".format(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun fromGregorian(year: Int, month: Int, day: Int, hijriAdjustment: Int = 0): HijriDateInfo {
        val cal = Calendar.getInstance().apply {
            set(year, month - 1, day)
        }
        val dowName = arabicWeekDays[cal.get(Calendar.DAY_OF_WEEK)] ?: ""

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val adjustedGregorian = LocalDate.of(year, month, day).plusDays(hijriAdjustment.toLong())
            val hijrahDate = HijrahChronology.INSTANCE.date(adjustedGregorian)
            val hDay = hijrahDate.get(ChronoField.DAY_OF_MONTH)
            val hMonth = hijrahDate.get(ChronoField.MONTH_OF_YEAR)
            val hYear = hijrahDate.get(ChronoField.YEAR)
            val mName = hijriMonths.getOrElse(hMonth - 1) { "" }
            return HijriDateInfo(
                day = hDay,
                month = hMonth,
                year = hYear,
                monthName = mName,
                dayOfWeekArabic = dowName,
                eventTitle = findEvent(hMonth, hDay)
            )
        }

        // Tabular fallback for API 24-25
        val adjCal = Calendar.getInstance().apply {
            set(year, month - 1, day)
            add(Calendar.DAY_OF_MONTH, hijriAdjustment)
        }
        val (hYear, hMonth, hDay) = tabularGregorianToHijri(
            adjCal.get(Calendar.YEAR),
            adjCal.get(Calendar.MONTH) + 1,
            adjCal.get(Calendar.DAY_OF_MONTH)
        )
        return HijriDateInfo(
            day = hDay,
            month = hMonth,
            year = hYear,
            monthName = hijriMonths.getOrElse(hMonth - 1) { "" },
            dayOfWeekArabic = dowName,
            eventTitle = findEvent(hMonth, hDay)
        )
    }

    fun getCurrentHijriMonthGrid(hijriAdjustment: Int = 0): Pair<HijriDateInfo, List<HijriMonthDay>> {
        val now = Calendar.getInstance()
        val todayInfo = fromGregorian(
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH) + 1,
            now.get(Calendar.DAY_OF_MONTH),
            hijriAdjustment
        )
        val list = mutableListOf<HijriMonthDay>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val todayGreg = LocalDate.now().plusDays(hijriAdjustment.toLong())
            val todayHijri = HijrahChronology.INSTANCE.date(todayGreg)
            val len = todayHijri.lengthOfMonth()
            for (d in 1..len) {
                val hDate = HijrahDate.of(todayInfo.year, todayInfo.month, d)
                val gDate = LocalDate.from(hDate).minusDays(hijriAdjustment.toLong())
                val dow = gDate.dayOfWeek.value // 1=Mon..7=Sun
                list.add(
                    HijriMonthDay(
                        hijriDay = d,
                        gregorianDay = gDate.dayOfMonth,
                        gregorianMonth = gDate.monthValue,
                        gregorianYear = gDate.year,
                        isToday = d == todayInfo.day,
                        isWhiteDay = d in 13..15,
                        isSunnahFastingDay = dow == 1 || dow == 4 || d in 13..15,
                        eventTitle = findEvent(todayInfo.month, d)
                    )
                )
            }
        } else {
            for (d in 1..30) {
                list.add(
                    HijriMonthDay(
                        hijriDay = d,
                        gregorianDay = d,
                        gregorianMonth = now.get(Calendar.MONTH) + 1,
                        gregorianYear = now.get(Calendar.YEAR),
                        isToday = d == todayInfo.day,
                        isWhiteDay = d in 13..15,
                        isSunnahFastingDay = d in 13..15,
                        eventTitle = findEvent(todayInfo.month, d)
                    )
                )
            }
        }
        return todayInfo to list
    }

    fun findEvent(hijriMonth: Int, hijriDay: Int): String? = when {
        hijriMonth == 1 && hijriDay == 1 -> "رأس السنة الهجرية"
        hijriMonth == 1 && hijriDay == 10 -> "يوم عاشوراء"
        hijriMonth == 3 && hijriDay == 12 -> "ذكرى المولد النبوي الشريف"
        hijriMonth == 7 && hijriDay == 27 -> "الإسراء والمعراج"
        hijriMonth == 8 && hijriDay == 15 -> "ليلة النصف من شعبان"
        hijriMonth == 9 && hijriDay == 1 -> "بداية شهر رمضان المبارك"
        hijriMonth == 9 && hijriDay in 21..30 && hijriDay % 2 == 1 -> "العشر الأواخر من رمضان"
        hijriMonth == 10 && hijriDay == 1 -> "عيد الفطر المبارك"
        hijriMonth == 12 && hijriDay == 9 -> "يوم عرفة"
        hijriMonth == 12 && hijriDay == 10 -> "عيد الأضحى المبارك"
        hijriDay in 13..15 -> "الأيام البيض"
        else -> null
    }

    private fun tabularGregorianToHijri(year: Int, month: Int, day: Int): Triple<Int, Int, Int> {
        val m = if (month > 2) month else month + 12
        val y = if (month > 2) year else year - 1
        val a = y / 100
        val b = 2 - a + (a / 4)
        val jd = ((365.25 * (y + 4716)).toInt() + (30.6001 * (m + 1)).toInt() + day + b - 1524)
        val l = jd - 1948440 + 10632
        val n = (l - 1) / 10631
        val l2 = l - 10631 * n + 354
        val j = ((10985 - l2) / 5316) * ((50 * l2) / 17719) + (l2 / 5670) * ((43 * l2) / 15238)
        val l3 = l2 - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val hMonth = (24 * l3) / 709
        val hDay = l3 - (709 * hMonth) / 24
        val hYear = 30 * n + j - 30
        return Triple(hYear, hMonth, hDay)
    }
}
