package com.cyc.yearlymemoir.utils

import com.nlf.calendar.Lunar
import com.nlf.calendar.Solar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

private val CHINESE_NUMBERS =
    arrayOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "十一", "十二")

private fun lunarDayToChinese(day: Int): String {
    return when {
        day <= 10 -> "初${CHINESE_NUMBERS[day - 1]}"
        day < 20 -> "十${CHINESE_NUMBERS[day - 11]}"
        day == 20 -> "二十"
        day < 30 -> "廿${CHINESE_NUMBERS[day - 21]}"
        day == 30 -> "三十"
        else -> ""
    }
}

private fun lunarMonthToChinese(month: Int, isLeap: Boolean): String {
    val prefix = if (isLeap) "闰" else ""
    val monthStr = when (month) {
        1 -> "正"
        12 -> "腊"
        else -> CHINESE_NUMBERS[abs(month) - 1]
    }
    return "$prefix${monthStr}月"
}

private fun dayOfWeekToChinese(weekday: DayOfWeek): String {
    return when (weekday) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }
}

private fun Int.toZeroPaddedString(): String = this.toString().padStart(2, '0')

object UniversalDateType {
    const val MonthDay = "MD"
    const val MonthWeekday = "MW"
    const val LunarDate = "LD"
}

sealed class UniversalMDDateType {
    /**
     * 将日期转换为人类可读的中文格式。
     */
    fun toChineseString(): String = when (this) {
        is MonthDay -> "${this.month}月${this.day}日"
        is MonthWeekday -> {
            "${CHINESE_NUMBERS[this.month - 1]}月第${CHINESE_NUMBERS[this.weekOrder - 1]}个${
                dayOfWeekToChinese(
                    this.weekday
                )
            }"
        }

        is LunarDate -> "${lunarMonthToChinese(this.month, this.isLeap)}${
            lunarDayToChinese(
                this.day
            )
        }"
    }

    // 类型1：公历某月某日（如：6月13日）
    data class MonthDay(val month: Int, val day: Int) : UniversalMDDateType() {
        override fun toString(): String =
            "MD-${month.toZeroPaddedString()}-${day.toZeroPaddedString()}"
    }

    // 类型2：某月第几个周几（如：5月第2个周日）
    data class MonthWeekday(
        val month: Int,
        val weekOrder: Int,
        val weekday: DayOfWeek // 1=周一 ... 7=周日（或者用 java.time.DayOfWeek）
    ) : UniversalMDDateType() {
        override fun toString(): String =
            "MW-${month.toZeroPaddedString()}-$weekOrder${weekday.value}"
    }

    // 类型3：农历日期（如：四月初五）
    data class LunarDate(
        val month: Int, // 1~12
        val day: Int,    // 1~29/30
        val isLeap: Boolean
    ) : UniversalMDDateType() {
        override fun toString(): String =
            "LD-${if (isLeap) "L" else ""}${month.toZeroPaddedString()}-${day.toZeroPaddedString()}"
    }
}

class UniversalDate(
    private val year: Int,
    private val mdDate: UniversalMDDateType
) {
    fun asMonthDay(): UniversalMDDateType.MonthDay {
        return when (mdDate) {
            // 1. 如果本来就是 MonthDay，直接返回
            is UniversalMDDateType.MonthDay -> mdDate

            // 2. 如果是 MonthWeekday，计算出具体的公历日期
            is UniversalMDDateType.MonthWeekday -> {
                // 获取该年该月的第一天
                val firstDayOfMonth = LocalDate.of(year, mdDate.month, 1)
                // 找到该月中第一个匹配的周几
                var targetDate =
                    firstDayOfMonth.with(TemporalAdjusters.firstInMonth(mdDate.weekday))
                // 根据 weekOrder（第几个）调整日期
                // weekOrder=1 就是第一个，weekOrder=2 就是再加7天，以此类推
                targetDate = targetDate.plusWeeks((mdDate.weekOrder - 1).toLong())

                UniversalMDDateType.MonthDay(targetDate.monthValue, targetDate.dayOfMonth)
            }

            // 3. 如果是 LunarDate，使用 lunar-java 库进行转换
            is UniversalMDDateType.LunarDate -> {
                // lunar-java 使用负数月份来表示闰月，非常方便
                val lunarMonth = if (mdDate.isLeap) -mdDate.month else mdDate.month

                // 从指定的农历年月日创建 Lunar 对象
                val lunar = Lunar.fromYmd(year, lunarMonth, mdDate.day)

                // 从 Lunar 对象获取对应的公历 (Solar) 对象
                val solar = lunar.solar

                UniversalMDDateType.MonthDay(solar.month, solar.day)
            }
        }
    }

    /**
     * 将任何类型的 UniversalDate 转换为【某月第几个周几】的形式。
     */
    fun asMonthWeekday(): UniversalMDDateType.MonthWeekday {
        return when (mdDate) {
            is UniversalMDDateType.MonthDay -> {
                val date = LocalDate.of(year, mdDate.month, mdDate.day)
                val weekOrder = (date.dayOfMonth - 1) / 7 + 1
                UniversalMDDateType.MonthWeekday(
                    month = date.monthValue,
                    weekOrder = weekOrder,
                    weekday = date.dayOfWeek
                )
            }

            is UniversalMDDateType.MonthWeekday -> mdDate

            is UniversalMDDateType.LunarDate -> {
                val lunarDate = Lunar.fromYmd(
                    year,
                    if (mdDate.isLeap) -mdDate.month else mdDate.month,
                    mdDate.day
                )
                val solar = lunarDate.solar
                val date = LocalDate.of(solar.year, solar.month, solar.day)
                val weekOrder = (date.dayOfMonth - 1) / 7 + 1
                UniversalMDDateType.MonthWeekday(
                    month = date.monthValue,
                    weekOrder = weekOrder,
                    weekday = date.dayOfWeek
                )
            }

        }
    }

    /**
     * 将任何类型的 UniversalDate 转换为【农历】形式。
     */
    fun asLunarDate(): UniversalMDDateType.LunarDate {
        return when (mdDate) {
            is UniversalMDDateType.MonthDay -> {
                val solar = Solar.fromYmd(year, mdDate.month, mdDate.day)
                val lunar = solar.lunar
                UniversalMDDateType.LunarDate(abs(lunar.month), lunar.day, lunar.month < 0)
            }

            is UniversalMDDateType.MonthWeekday -> {
                val monthDay = this.asMonthDay()
                val solar = Solar.fromYmd(year, monthDay.month, monthDay.day)
                val lunar = solar.lunar
                UniversalMDDateType.LunarDate(
                    month = abs(lunar.month),
                    day = lunar.day,
                    isLeap = lunar.month < 0
                )
            }

            is UniversalMDDateType.LunarDate -> mdDate
        }
    }

    fun getSolarYear(): Int {
        return when (mdDate) {
            is UniversalMDDateType.MonthDay -> {
                year
            }

            is UniversalMDDateType.MonthWeekday -> {
                year
            }

            is UniversalMDDateType.LunarDate -> {
                val lunar = Lunar.fromYmd(
                    year,
                    if (mdDate.isLeap) -mdDate.month else mdDate.month,
                    mdDate.day
                )
                lunar.solar.year
            }
        }
    }

    fun getLunarYear(): Int {
        return when (mdDate) {
            is UniversalMDDateType.MonthDay -> {
                val solar = Solar.fromYmd(year, mdDate.month, mdDate.day)
                solar.lunar.year
            }

            is UniversalMDDateType.MonthWeekday -> {
                val monthDay = this.asMonthDay()
                val solar = Solar.fromYmd(year, monthDay.month, monthDay.day)
                solar.lunar.year
            }

            is UniversalMDDateType.LunarDate -> {
                year
            }
        }
    }

    fun getMDDateType(): Int {
        return when (mdDate) {
            is UniversalMDDateType.MonthDay -> 0
            is UniversalMDDateType.LunarDate -> 1
            is UniversalMDDateType.MonthWeekday -> 2
        }
    }

    fun getRawMDDate(): UniversalMDDateType {
        return mdDate
    }

    override fun toString(): String {
        return "$year-${mdDate}"
    }

    fun toChineseString(): String {
        return "${year}年${mdDate.toChineseString()}"
    }
}