package com.cyc.yearlymemoir.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cyc.yearlymemoir.data.local.entity.FieldEntity
import kotlinx.coroutines.flow.Flow

/**
 * Field 表的数据访问对象 (DAO)。
 * 定义了所有与 'fields' 表相关的数据库操作。
 */
@Dao
interface FieldDao {

    /**
     * 插入一个新的 field。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertField(field: FieldEntity)

    /**
     * 根据字段名称获取 field。
     */
    @Query("SELECT * FROM fields WHERE field_name = :name LIMIT 1")
    suspend fun getFieldByName(name: String): FieldEntity?

    /**
     * 获取指定 group ID 下的所有 fields。
     * 返回一个 Flow 以便数据变更时自动更新。
     */
    @Query("SELECT * FROM fields WHERE group_id = :groupId ORDER BY field_name ASC")
    fun getFieldsByGroupId(groupId: Int): Flow<List<FieldEntity>>

    /**
     * 获取所有的 fields。
     */
    @Query("SELECT * FROM fields")
    suspend fun getAllFields(): List<FieldEntity>


    @Query("DELETE FROM fields")
    suspend fun deleteAll()
}