package com.cyc.yearlymemoir

import com.cyc.yearlymemoir.model.Model
import com.cyc.yearlymemoir.utils.UniversalDate
import com.cyc.yearlymemoir.utils.UniversalMDDateType
import org.junit.Test
import java.time.LocalDateTime

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun testUDSWithDate() {
        val mm = Model()
        val res = mm.test()
        println(res)
    }

    @Test
    fun testUniversalDate() {
        var uniDate = UniversalDate(2023, UniversalMDDateType.MonthDay(2, 1))
        println("$uniDate：\t${uniDate.asMonthDay()}\t${uniDate.asMonthWeekday()}\t${uniDate.asLunarDate()}")
        println(
            "${uniDate.toChineseString()}：\t${uniDate.asMonthDay().toChineseString()}\t${
                uniDate.asLunarDate().toChineseString()
            } ${
                uniDate.asMonthWeekday().toChineseString()
            }"
        )


        uniDate = UniversalDate(2023, UniversalMDDateType.LunarDate(2, 1, true))
        println("$uniDate：\t${uniDate.asMonthDay()}\t${uniDate.asMonthWeekday()}\t${uniDate.asLunarDate()}")
        println(
            "${uniDate.toChineseString()}：\t${uniDate.asMonthDay().toChineseString()}\t${
                uniDate.asLunarDate().toChineseString()
            }\t${
                uniDate.asMonthWeekday().toChineseString()
            }"
        )

        uniDate = UniversalDate(2023, UniversalMDDateType.LunarDate(2, 1, false))
        println("$uniDate：\t${uniDate.asMonthDay()}\t${uniDate.asMonthWeekday()}\t${uniDate.asLunarDate()}")
        println(
            "${uniDate.toChineseString()}：\t${uniDate.asMonthDay().toChineseString()}\t${
                uniDate.asLunarDate().toChineseString()
            } ${
                uniDate.asMonthWeekday().toChineseString()
            }"
        )
    }

    @Test
    fun testLocalTime() {
        println(LocalDateTime.now().toString())
    }
}
