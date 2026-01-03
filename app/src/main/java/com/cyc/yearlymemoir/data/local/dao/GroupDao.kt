package com.cyc.yearlymemoir.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cyc.yearlymemoir.data.local.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

/**
 * Group 表的数据访问对象 (DAO)。
 * 定义了所有与 'groups' 表相关的数据库操作。
 */
@Dao
interface GroupDao {

    /**
     * 插入一个新的 group。如果已存在，则忽略。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroup(group: GroupEntity)

    /**
     * 更新一个已存在的 group。
     */
    @Update
    suspend fun updateGroup(group: GroupEntity)

    /**
     * 根据 ID 删除一个 group。
     */
    @Query("DELETE FROM groups WHERE group_id = :groupId")
    suspend fun deleteGroupById(groupId: Int)

    /**
     * 获取所有的 group。
     * 返回一个 Flow，当数据库中的 groups 表发生变化时，它会自动发出新的列表。
     * 这对于响应式UI非常有帮助。
     */
    @Query("SELECT * FROM groups ORDER BY group_name ASC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    /**
     * 根据 ID 获取单个 group。
     */
    @Query("SELECT * FROM groups WHERE group_id = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: Int): GroupEntity?

    @Query("DELETE FROM groups")
    suspend fun deleteAll()
}