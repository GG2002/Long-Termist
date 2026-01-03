package com.cyc.yearlymemoir.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cyc.yearlymemoir.data.local.dao.DetailDao
import com.cyc.yearlymemoir.data.local.dao.FieldDao
import com.cyc.yearlymemoir.data.local.dao.GroupDao
import com.cyc.yearlymemoir.data.local.entity.DetailEntity
import com.cyc.yearlymemoir.data.local.entity.FieldEntity
import com.cyc.yearlymemoir.data.local.entity.GroupEntity
import com.cyc.yearlymemoir.domain.model.FIELD_TYPE_NUM
import com.cyc.yearlymemoir.domain.model.FIELD_TYPE_STR
import com.cyc.yearlymemoir.domain.model.FIELD_TYPE_TEXT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 应用程序的 Room 数据库主类。
 *
 * @Database 注解标记了这个类作为 Room 数据库。
 * - entities: 列出所有需要被数据库管理的实体类。
 * - version: 数据库的版本号。每次修改表结构（增删改字段、表等），都必须增加版本号。
 * - exportSchema: 是否导出数据库结构到 JSON 文件。建议设为 false，除非你需要用于复杂的迁移分析。
 */
@Database(
    entities = [
        DetailEntity::class,
        FieldEntity::class,
        GroupEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // 为每个 DAO 提供一个抽象的 "getter" 方法。
    // Room 会在后台自动实现这些方法。
    abstract fun detailDao(): DetailDao
    abstract fun fieldDao(): FieldDao
    abstract fun groupDao(): GroupDao

    // 1. 定义一个数据库回调
    private class AppDatabaseCallback(
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

        // 2. 定义一个挂起函数来执行插入操作
        suspend fun populateDatabase(details: DetailDao, fields: FieldDao, groups: GroupDao) {
            // 清理旧数据（可选）
            details.deleteAll()
            fields.deleteAll()
            groups.deleteAll()

            // 插入你的预设数据
            val groupEntities = listOf(
                GroupEntity(groupId = 1, groupName = "default", groupParentId = 0),
                GroupEntity(groupId = 2, groupName = "资产余额", groupParentId = 0),
                GroupEntity(groupId = 3, groupName = "动物朋友", groupParentId = 0),
                GroupEntity(groupId = 4, groupName = "龟龟指标", groupParentId = 3)
            )
            groupEntities.forEach { groups.insertGroup(it) }

            val fieldsEntities = listOf(
                FieldEntity(
                    fieldId = 1,
                    fieldName = "支付宝余额",
                    fieldType = FIELD_TYPE_NUM,
                    groupId = 2
                ),
                FieldEntity(
                    fieldId = 2,
                    fieldName = "微信余额",
                    fieldType = FIELD_TYPE_NUM,
                    groupId = 2
                ),
                FieldEntity(
                    fieldId = 3,
                    fieldName = "云闪付余额",
                    fieldType = FIELD_TYPE_NUM,
                    groupId = 2
                ),
                FieldEntity(
                    fieldId = 4,
                    fieldName = "总余额",
                    fieldType = FIELD_TYPE_NUM,
                    groupId = 2
                ),
                FieldEntity(
                    fieldId = 5,
                    fieldName = "年事",
                    fieldType = FIELD_TYPE_TEXT,
                    groupId = 0
                ),
                FieldEntity(
                    fieldId = 6,
                    fieldName = "生日",
                    fieldType = FIELD_TYPE_STR,
                    groupId = 0
                ),
                FieldEntity(
                    fieldId = 7,
                    fieldName = "提醒我",
                    fieldType = FIELD_TYPE_TEXT,
                    groupId = 0
                ),
            )
            fieldsEntities.forEach { fields.insertField(it) }
        }
    }


    /**
     * 使用伴生对象 (Companion Object) 来实现单例模式。
     * 这可以防止在同一时间打开多个数据库实例，避免资源浪费和潜在的并发问题。
     */
    companion object {
        // @Volatile 注解确保 INSTANCE 变量的写入对所有线程立即可见。
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 获取数据库的单例实例。
         *
         * @param context 应用程序上下文，用于创建数据库。
         * @return AppDatabase 的单例实例。
         */
        fun getInstance(context: Context, scope: CoroutineScope): AppDatabase {
            // 使用 synchronized 块确保线程安全。
            // 如果 INSTANCE 不为 null，则直接返回它。
            // 如果为 null，则创建一个新的数据库实例。
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yearlmemoir.db" // 这是数据库在设备上存储的文件名
                )
                    .addCallback(AppDatabaseCallback(scope))
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