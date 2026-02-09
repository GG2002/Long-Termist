package com.cyc.yearlymemoir.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cyc.yearlymemoir.data.local.entity.FinanceAsset

@Dao
interface FinanceAssetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asset: FinanceAsset): Long

    @Update
    suspend fun update(asset: FinanceAsset)

    @Delete
    suspend fun delete(asset: FinanceAsset)

    /** 删除某个资产名下的所有历史记录 */
    @Query("DELETE FROM finance_asset WHERE asset_name = :assetName")
    suspend fun deleteByAssetName(assetName: String)

    /** 将历史记录中的资产名整体重命名为新的资产名 */
    @Query("UPDATE finance_asset SET asset_name = :newName WHERE asset_name = :oldName")
    suspend fun renameAsset(oldName: String, newName: String): Int

    @Query("SELECT * FROM finance_asset ORDER BY record_date DESC, asset_name ASC")
    suspend fun getAll(): List<FinanceAsset>

    @Query("SELECT * FROM finance_asset WHERE id=:id")
    suspend fun getById(id: Int): FinanceAsset?

    @Query(
        """
        SELECT fa.asset_name, fa.asset_amount
        FROM finance_asset fa
        JOIN (
          SELECT asset_name, MAX(record_date) AS max_date
          FROM finance_asset
          GROUP BY asset_name
        ) t ON fa.asset_name = t.asset_name AND fa.record_date = t.max_date
        ORDER BY fa.asset_amount DESC
        """
    )
    suspend fun getLatestPerAsset(): List<LatestAssetAmount>

    @Query(
        """
        SELECT IFNULL(SUM(fa.asset_amount), 0)
        FROM finance_asset fa
        JOIN (
          SELECT asset_name, MAX(record_date) AS max_date
          FROM finance_asset
          GROUP BY asset_name
        ) t ON fa.asset_name = t.asset_name AND fa.record_date = t.max_date
        """
    )
    suspend fun getLatestTotalAmount(): Double

    /**
     * 按指定日期（含当日）汇总 FinanceAsset：
     * 对每个资产名取该日期之前（含当日）的最新一条记录，再求和。
     */
    @Query(
        """
        SELECT IFNULL(SUM(fa.asset_amount), 0)
        FROM finance_asset fa
        JOIN (
          SELECT asset_name, MAX(record_date) AS max_date
          FROM finance_asset
          WHERE record_date <= :date
          GROUP BY asset_name
        ) t ON fa.asset_name = t.asset_name AND fa.record_date = t.max_date
        """
    )
    suspend fun getLatestTotalAmountByDate(date: String): Double
}

data class LatestAssetAmount(
    val asset_name: String,
    val asset_amount: Double
)