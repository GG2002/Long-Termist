package com.cyc.yearlymemoir

import com.cyc.yearlymemoir.data.tidbcloud.db.DbClient
import com.cyc.yearlymemoir.data.tidbcloud.dto.DetailRequest
import com.cyc.yearlymemoir.domain.model.UniversalDate
import com.cyc.yearlymemoir.domain.model.UniversalMDDateType
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun testTidbRemote() {
        val dbClient = DbClient()
        dbClient.detailDao.upsertDetail(
            DetailRequest(
                detail_id = 0,
                year = 2025,
                md_date = "MD-07-01",
                yearly = "0",
                field_id = 4,
                detail = "0.00"
            )
        )
    }

    @Test
    fun testLastUpdate() {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStartMillis = todayStart.timeInMillis

        val currentTimeSeconds = System.currentTimeMillis().toDouble()
//        val lastUpdate = 1753899384
        val lastUpdate = currentTimeSeconds * 1000
        val formattedTime =
            SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
                .format(Date(lastUpdate.toLong()))
        val forma1 =
            SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
                .format(Date(todayStartMillis))
        println(todayStartMillis.toDouble())
        println(forma1)
        println(lastUpdate)
        println(formattedTime)
    }

    @Test
    fun testUniversalDate() {
        var uniDate = UniversalDate(
            2023,
            UniversalMDDateType.LunarDate(12, 1, false)
        )
        println("$uniDate：\t${uniDate.asMonthDay()}\t${uniDate.asMonthWeekday()}\t${uniDate.asLunarDate()}")
        println(
            "${uniDate.toChineseString()}："
        )
        println(uniDate.asMonthDayInYear(2002).toChineseString())
        println(uniDate.asMonthDayInYear(2003).toChineseString())
        println(uniDate.asMonthDayInYear(2004).toChineseString())
        println(uniDate.asMonthDayInYear(2005).toChineseString())
        println(uniDate.asMonthDayInYear(2006).toChineseString())
        println(uniDate.asMonthDayInYear(2007).toChineseString())
        println(uniDate.asMonthDayInYear(2008).toChineseString())
        println(uniDate.asMonthDayInYear(2009).toChineseString())
        println(uniDate.asMonthDayInYear(2010).toChineseString())
        println(uniDate.asMonthDayInYear(2011).toChineseString())
        println(uniDate.asMonthDayInYear(2025).toChineseString())
    }
}
