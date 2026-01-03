package com.cyc.yearlymemoir

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cyc.yearlymemoir.data.local.dao.DetailDao
import com.cyc.yearlymemoir.data.local.db.AppDatabase
import com.cyc.yearlymemoir.data.local.entity.DetailEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class) // 指定测试运行器
class DetailDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: DetailDao

    // 1. 在每个测试方法运行前执行
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // 使用内存数据库。这很重要，因为它允许在不实际接触设备磁盘的情况下进行测试。
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            // 允许在主线程上进行查询 (仅限测试时使用！)
            .allowMainThreadQueries()
            .build()
        dao = db.detailDao() // 假设你的 AppDatabase 有一个 detailDao() 方法
    }

    // 2. 在每个测试方法运行后执行
    @After
    fun closeDb() {
        db.close()
    }

    // 3. 编写第一个测试用例
    @Test
    fun insertDetailAndGetByFieldId_returnsCorrectDetails() = runBlocking {
        // 准备数据 (Arrange)
        val detail1 = DetailEntity(
            detail_id = 1,
            year = 2023,
            md_date = "MD-01-01",
            yearly = false,
            field_id = 10,
            detail = "Detail A"
        )
        val detail2 = DetailEntity(
            detail_id = 2,
            year = 2023,
            md_date = "MD-01-02",
            yearly = false,
            field_id = 10,
            detail = "Detail B"
        )
        val detail3 = DetailEntity(
            detail_id = 3,
            year = 2024,
            md_date = "MD-02-01",
            yearly = false,
            field_id = 20,
            detail = "Detail C"
        ) // 不同的fieldId

        dao.upsertDetail(detail1)
        dao.upsertDetail(detail2)
        dao.upsertDetail(detail3)

        // 执行操作 (Act)
        val detailsForField10 = dao.getDayFieldDataById(10)

        // 断言结果 (Assert)
        assertThat(detailsForField10).hasSize(2)
        assertThat(detailsForField10).containsExactly(detail1, detail2)
        assertThat(detailsForField10).doesNotContain(detail3)
    }

    @Test
    fun getSortedDailyDetailsByFieldId_returnsSortedAndFilteredDetails() = runBlocking {
        // 准备数据 (Arrange)
        // 注意顺序是乱的，并且包含 yearly=true 的数据
        val detail1 = DetailEntity(
            detail_id = 1,
            year = 2023,
            md_date = "MD-05-10",
            yearly = false,
            field_id = 15,
            detail = "Second"
        )
        val detail2 = DetailEntity(
            detail_id = 2,
            year = 2022,
            md_date = "MD-12-25",
            yearly = false,
            field_id = 15,
            detail = "First"
        )
        val detail3 = DetailEntity(
            detail_id = 3,
            year = 2023,
            md_date = "MD-11-01",
            yearly = false,
            field_id = 15,
            detail = "Third"
        )
        val yearlyDetail = DetailEntity(
            detail_id = 4,
            year = 2023,
            md_date = "YR",
            yearly = true,
            field_id = 15,
            detail = "Yearly Detail"
        )
        val otherFieldDetail = DetailEntity(
            detail_id = 5,
            year = 2022,
            md_date = "MD-01-01",
            yearly = false,
            field_id = 99,
            detail = "Other Field"
        )

        dao.upsertDetail(detail1)
        dao.upsertDetail(detail2)
        dao.upsertDetail(detail3)
        dao.upsertDetail(yearlyDetail)
        dao.upsertDetail(otherFieldDetail)

        // 执行操作 (Act)
        val result = dao.getDayFieldDataById(15)

        // 断言结果 (Assert)
        // 1. 验证大小：应该只有 3 条记录（排除了 yearly=true 和 field_id=99 的）
        assertThat(result).hasSize(3)

        // 2. 验证内容和顺序：应该按照 year ASC, md_date ASC 排序
        assertThat(result).containsExactly(
            detail2, // year 2022
            detail1, // year 2023, month 05
            detail3  // year 2023, month 11
        ).inOrder()
    }


    @Test
    fun getFirstDayTest() = runBlocking {
        // 准备数据 (Arrange)
        // 注意顺序是乱的，并且包含 yearly=true 的数据
        val details = listOf(
            DetailEntity(
                detail_id = 1,
                year = 2023,
                md_date = "MD-05-10",
                yearly = false,
                field_id = 15,
                detail = "Second"
            ), DetailEntity(
                detail_id = 2,
                year = 2022,
                md_date = "MD-12-25",
                yearly = false,
                field_id = 15,
                detail = "First"
            ), DetailEntity(
                detail_id = 3,
                year = 2023,
                md_date = "MD-11-01",
                yearly = false,
                field_id = 15,
                detail = "Third"
            ), DetailEntity(
                detail_id = 4,
                year = 2023,
                md_date = "YR",
                yearly = true,
                field_id = 15,
                detail = "Yearly Detail"
            ), DetailEntity(
                detail_id = 5,
                year = 2022,
                md_date = "MD-01-01",
                yearly = false,
                field_id = 99,
                detail = "Other Field"
            )
        )

        details.forEach { dao.upsertDetail(it) }

        // 执行操作 (Act)
        val result = dao.getDayFieldDataById(15)
        println(result)
        println(dao.getFirstDayOfMonthDataById(15))
        println(dao.getFirstDayOfYearDataById(15))
        // 断言结果 (Assert)
        // 1. 验证大小：应该只有 3 条记录（排除了 yearly=true 和 field_id=99 的）
//        assertThat(result).hasSize(3)
    }
}