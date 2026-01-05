package com.cyc.yearlymemoir.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.cyc.yearlymemoir.data.local.entity.YearlyDetailEntity

@Dao
interface YearlyDetailDao {
    @Upsert
    suspend fun upsert(detail: YearlyDetailEntity)

    @Query("SELECT * FROM details_yearly")
    suspend fun getAll(): List<YearlyDetailEntity>

    @Query(
        "SELECT * FROM details_yearly WHERE field_id = :fieldId AND md_date = :mdDate"
    )
    suspend fun getByFieldAndMdDate(fieldId: Int, mdDate: String): YearlyDetailEntity?

    @Delete
    suspend fun delete(detail: YearlyDetailEntity)

    @Query("DELETE FROM details_yearly")
    suspend fun deleteAll()
}