package com.cyc.yearlymemoir.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cyc.yearlymemoir.data.local.entity.DetailEntity

@Dao
interface DetailDao {

    // `upsert` 完美对应你的 insertOrUpdate
    @Upsert
    suspend fun upsertDetail(detail: DetailEntity)

    @Query("SELECT * FROM details")
    suspend fun getAllDetails(): List<DetailEntity>

    // 查询每天的数据
    @Query("SELECT * FROM details WHERE field_id = :fieldId ORDER BY year ASC, md_date ASC")
    suspend fun getDayFieldDataById(fieldId: Int): List<DetailEntity>

    // 获取每月的第一天的数据。
    @Query(
        """
        SELECT * FROM details
        WHERE field_id = :fieldId AND (year, md_date) IN (
            SELECT year, MIN(md_date)
            FROM details
            WHERE field_id = :fieldId
            AND md_date LIKE "MD-%"
            AND yearly = False
            GROUP BY year, SUBSTR(md_date, 4, 2)
        )
        ORDER BY year, md_date
    """
    )
    suspend fun getFirstDayOfMonthDataById(fieldId: Int): List<DetailEntity>

    // 获取每年的第一天的数据。
    @Query(
        """
        SELECT * FROM details
        WHERE field_id = :fieldId AND (year, md_date) IN (
            SELECT year, MIN(md_date)
            FROM details
            WHERE field_id = :fieldId
            GROUP BY year
        )
        ORDER BY year, md_date
    """
    )
    suspend fun getFirstDayOfYearDataById(fieldId: Int): List<DetailEntity>

    @Query(
        """    
        SELECT * FROM details
        WHERE field_id=:fieldId AND year = :year AND md_date = :date AND yearly = :yearly 
        """
    )
    suspend fun getDetailByFieldAndUniversalDateAndYearly(
        fieldId: Int,
        year: Int,
        date: String,
        yearly: Boolean
    ): DetailEntity?

    @Query("DELETE FROM details")
    suspend fun deleteAll()
}