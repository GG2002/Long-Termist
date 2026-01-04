package com.cyc.yearlymemoir.data.repository


import android.util.Log
import com.cyc.yearlymemoir.domain.model.BalanceRecord
import com.cyc.yearlymemoir.domain.model.Detail
import com.cyc.yearlymemoir.domain.model.Field
import com.cyc.yearlymemoir.domain.model.Group
import com.cyc.yearlymemoir.domain.model.TransactionRecord
import com.cyc.yearlymemoir.domain.model.UniversalDate
import com.cyc.yearlymemoir.domain.model.UniversalMDDateType
import com.cyc.yearlymemoir.domain.repository.DatastoreInit
import com.cyc.yearlymemoir.domain.repository.TimePeriod
import com.cyc.yearlymemoir.domain.repository.YearlyMemoirRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * 一个混合数据仓库，协调本地和云端数据。
 * 它实现了 "Cache-Aside"（旁路缓存）模式。
 *
 * @param localRepository 本地数据源的实现，作为缓存和离线数据存储。
 * @param cloudRepository 云端数据源的实现，作为最终的、权威的数据源。
 */
class HybridYearlyMemoirRepository(
    private val localRepository: YearlyMemoirRepository, // 假设这是你的本地数据库实现
    private val cloudRepository: YearlyMemoirRepository,  // 假设这是你的云端数据库实现
    private val ds: DatastoreInit, // 用于存储应用配置信息
) : YearlyMemoirRepository {

    suspend fun initializeDatabaseIfEmpty() {
        var isLocalDbEmpty = true
        var retryNum = 3
        while (retryNum > 0) {
            try {
                isLocalDbEmpty = localRepository.getFirstDayFieldDataByName(
                    "总余额",
                    TimePeriod.DAY
                ).first.isEmpty()
            } catch (_: IOException) {
                retryNum--
                delay(5000)
                continue
            }
            break
        }

        if (isLocalDbEmpty) {
            Log.i("数据同步", "本地数据库为空，开始从云端进行全量同步...")
            try {
                // 将所有数据写入本地数据库
                // 注意：这是一个效率较低的实现，因为它会对每个 Field 都发起一次网络请求。
                // 一个更优的方案是让 cloudRepository 提供一个 `getAllDetails()` 的方法。
                coroutineScope {
                    // 1. 定义需要并行处理的字段名列表
                    val fieldNames = listOf("总余额", "微信余额", "支付宝余额", "云闪付余额")

                    // 2. 并行获取所有字段的详情数据，并存入本地
                    //    对列表中的每个字段名，都启动一个独立的协程任务
                    fieldNames.forEach { fieldNameToFetch ->
                        launch { // 在 coroutineScope 内，每个 launch 都是并行的
                            val detailsPair = cloudRepository.getFirstDayFieldDataByName(
                                name = fieldNameToFetch,
                                period = TimePeriod.DAY,
                                isAsc = true
                            )

                            detailsPair.first.forEach { detail ->
                                val mdDate = UniversalMDDateType.fromString(detail.mdDate)!!

                                localRepository.insertOrUpdateDetail(
                                    fieldName = fieldNameToFetch,
                                    detail = detail.detail,
                                    date = UniversalDate(detail.year, mdDate),
                                    yearly = detail.yearly
                                )
                            }
                        }
                    }
                    // coroutineScope 会等待上面所有的 launch 任务全部完成后，才会继续向下执行
                }
                Log.i("数据同步", "全量同步完成。")
            } catch (e: Exception) {
                // 处理初始化失败的情况，例如网络错误
                Log.e("数据同步", "错误：数据库初始化失败。${e.message}")
                // 这里可以向上抛出异常，让调用方决定如何处理（例如显示错误页面、提示重试）
                throw IOException("Failed to initialize database from cloud.", e)
            }
        } else {
            Log.i("数据同步", "本地数据库已存在数据，跳过初始化。")
        }
    }

    override suspend fun getAllDetails(): List<Detail> {
        return localRepository.getAllDetails()
    }

    override suspend fun getDetailByFieldAndUniversalDateAndYearly(
        fieldId: Int,
        universalDate: UniversalDate,
        yearly: Boolean
    ): Detail? {
        return localRepository.getDetailByFieldAndUniversalDateAndYearly(
            fieldId,
            universalDate,
            yearly
        )
    }

    /**
     * 获取数据：优先从本地查询。如果本地没有，则从云端获取，存入本地，然后返回。
     */
    override suspend fun getFirstDayFieldDataByName(
        name: String,
        period: TimePeriod,
        isAsc: Boolean
    ): Pair<List<Detail>, String> {
        // 尝试从本地仓库获取数据
        val localData = localRepository.getFirstDayFieldDataByName(name, period, isAsc)

        return localData
    }

    /**
     * 插入或更新数据：同时操作本地和云端。
     * 先操作本地，以便 UI 能即时响应。
     */
    override suspend fun insertOrUpdateDetail(
        fieldName: String,
        detail: String,
        date: UniversalDate,
        yearly: Boolean
    ) {
        // 1. 尝试在云端插入/更新
        cloudRepository.insertOrUpdateDetail(fieldName, detail, date, yearly)
        // 2. 然后在本地插入/更新
        localRepository.insertOrUpdateDetail(fieldName, detail, date, yearly)
    }

    /**
     * 删除数据：同时操作本地和云端。
     */
    override suspend fun deleteDetail(detail: Detail) {
        // 1. 从云端删除
        cloudRepository.deleteDetail(detail)
        // 2. 从本地删除
        localRepository.deleteDetail(detail)
    }

    override suspend fun insertOrUpdateField(field: Field) {
        cloudRepository.insertOrUpdateField(field)
        localRepository.insertOrUpdateField(field)
    }

    override suspend fun getAllFields(): List<Field> {
        return localRepository.getAllFields()
    }

    /**
     * 获取字段信息：优先本地，其次云端。
     */
    override suspend fun getFieldByName(name: String): Field? {
        return localRepository.getFieldByName(name)
    }

    /**
     * 获取所有分组和字段：优先本地，其次云端。
     */
    override suspend fun getAllGroupsWithFields(): Map<Group, List<Field>> {
        val localGroups = localRepository.getAllGroupsWithFields()
        return localGroups
    }

    // Balance APIs
    override suspend fun upsertBalance(balance: BalanceRecord){}
    override suspend fun getAllBalances(): List<BalanceRecord>{ return emptyList() }
    override suspend fun getBalancesByDate(date: String): List<BalanceRecord>{ return emptyList() }
    override suspend fun getBalancesBySource(sourceType: String): List<BalanceRecord>{ return emptyList() }

    // Transaction APIs (split income/expense)
    override suspend fun upsertTransaction(record: TransactionRecord){}
    override suspend fun getAllIncomes(): List<TransactionRecord>{ return emptyList() }
    override suspend fun getAllExpenses(): List<TransactionRecord>{ return emptyList() }
    override suspend fun getIncomesByDate(date: String): List<TransactionRecord>{ return emptyList() }
    override suspend fun getExpensesByDate(date: String): List<TransactionRecord>{ return emptyList() }
    override suspend fun getIncomesByTag(tag: String): List<TransactionRecord>{ return emptyList() }
    override suspend fun getExpensesByTag(tag: String): List<TransactionRecord>{ return emptyList() }
    override suspend fun getIncomeTags(): List<String>{ return emptyList() }
    override suspend fun getExpenseTags(): List<String>{ return emptyList() }

}