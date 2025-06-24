package com.cyc.yearlymemoir.utils

import com.nlf.calendar.Lunar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

data class LunarDate(
    val lunarYear: Int,
    val lunarMonth: Int,
    val lunarDay: Int,
    val lunarMonthInChinese: String,
    val lunarDayInChinese: String
)

fun LunarDateFromLocalDate(date: LocalDate): LunarDate {
    val calendar = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
    val lunar = Lunar(calendar)

    return LunarDate(
        lunarYear = lunar.year,
        lunarMonth = lunar.month,
        lunarDay = lunar.day,
        lunarMonthInChinese = lunar.monthInChinese,
        lunarDayInChinese = lunar.dayInChinese
    )
}

fun formatDateComponents(date: LocalDate): Triple<String, String, String> {
    // 阳历：4月1日
    val solar = "${date.monthValue}月${date.dayOfMonth}日"

    // 星期几：周一、周二...
    val weekDay = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }

    // 农历：十二、廿二...
    val lunar = LunarDateFromLocalDate(date)
    val lunarDay = lunar.lunarDayInChinese

    return Triple(solar, weekDay, lunarDay)
}

fun LocalDateFromLunar(year: Int, lunarDateString: String): LocalDate? {
    val (monthStr, dayStr) = lunarDateString.split("-")
    val month = monthStr.toInt()
    val day = dayStr.toInt()

    val lunar = Lunar(year, month, day)
    val solarCalendar = lunar.solar

    return LocalDate.of(solarCalendar.year, solarCalendar.month, solarCalendar.day)
}