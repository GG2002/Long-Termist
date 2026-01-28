package com.cyc.yearlymemoir.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cyc.yearlymemoir.data.local.dao.DetailDao
import com.cyc.yearlymemoir.data.local.dao.FieldDao
import com.cyc.yearlymemoir.data.local.dao.GroupDao
import com.cyc.yearlymemoir.data.local.dao.YearlyDetailDao
import com.cyc.yearlymemoir.data.local.entity.DetailEntity
import com.cyc.yearlymemoir.data.local.entity.FieldEntity
import com.cyc.yearlymemoir.data.local.entity.GroupEntity
import com.cyc.yearlymemoir.data.local.entity.YearlyDetailEntity
import com.cyc.yearlymemoir.domain.model.FIELD_TYPE_STR
import com.cyc.yearlymemoir.domain.model.FIELD_TYPE_TEXT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
    entities = [
        DetailEntity::class,
        YearlyDetailEntity::class,
        FieldEntity::class,
        GroupEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CoreDatabase : RoomDatabase() {
    abstract fun detailDao(): DetailDao
    abstract fun yearlyDetailDao(): YearlyDetailDao
    abstract fun fieldDao(): FieldDao
    abstract fun groupDao(): GroupDao

    private class CoreDataDatabaseCallback(
        private val scope: CoroutineScope // 接收一个协程作用域
    ) : Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // 当数据库第一次被创建时，会调用这个方法
            INSTANCE?.let { database ->
                scope.launch {
                    // 在这里启动一个后台协呈来填充数据
                    populateDatabase(database.detailDao(), database.fieldDao(), database.groupDao())
                }
            }
        }

        suspend fun populateDatabase(details: DetailDao, fields: FieldDao, groups: GroupDao) {
            // 清理旧数据（可选）
            details.deleteAll()
            fields.deleteAll()
            groups.deleteAll()

            // 插入预设数据
            val groupEntities = listOf(
                GroupEntity(groupId = 1, groupName = "default", groupParentId = 0),
                GroupEntity(groupId = 3, groupName = "动物朋友", groupParentId = 0),
                GroupEntity(groupId = 4, groupName = "龟龟指标", groupParentId = 3)
            )
            groupEntities.forEach { groups.insertGroup(it) }

            val fieldsEntities = listOf(
                FieldEntity(
                    fieldId = 1,
                    fieldName = "提醒我",
                    fieldType = FIELD_TYPE_TEXT,
                    groupId = 0
                ),
                FieldEntity(
                    fieldId = 2,
                    fieldName = "年事",
                    fieldType = FIELD_TYPE_TEXT,
                    groupId = 0
                ),
                FieldEntity(
                    fieldId = 3,
                    fieldName = "生日",
                    fieldType = FIELD_TYPE_STR,
                    groupId = 0
                ),
            )
            fieldsEntities.forEach { fields.insertField(it) }
        }
    }


    companion object {
        @Volatile
        private var INSTANCE: CoreDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): CoreDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CoreDatabase::class.java,
                    "yearlmemoir_core.db" // 核心数据数据库文件名
                )
                    .addCallback(CoreDataDatabaseCallback(scope))
                    // 如果需要处理数据库版本升级，可以在这里添加迁移策略。
                    // .addMigrations(MIGRATION_1_2, ...)
                    .build()

                INSTANCE = instance
                // 返回新创建的实例
                instance
            }
        }
    }
}