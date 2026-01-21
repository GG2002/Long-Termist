import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyc.yearlymemoir.MainActivity
import com.cyc.yearlymemoir.MainApplication
import com.cyc.yearlymemoir.domain.model.TransactionRecord
import com.cyc.yearlymemoir.domain.repository.YearlyMemoirRepository
import ir.ehsannarmani.compose_charts.extensions.format
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs

data class MonthlyLedgerState(
    val monthLabel: String = "",
    val balanceDelta: Double = 0.0,
    val totalIncome: Double = 0.0,
    val topIncomes: List<TransactionView> = emptyList(),
    val totalExpense: Double = 0.0,
    val topExpenses: List<TransactionView> = emptyList()
)

class MonthlyLedgerViewModel() : ViewModel() {
    private val repo: YearlyMemoirRepository = MainApplication.repository

    private val _state = MutableStateFlow(MonthlyLedgerState())
    val state: StateFlow<MonthlyLedgerState> = _state

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun load() {
        viewModelScope.launch {
            val month = YearMonth.now()
            val today = LocalDate.now()
            // 1. 从当天开始往前查，优先取“本月的第一条记录”（最好是 1 号；没有则 2/3/4 号等），
            //    若本月没有任何记录，则继续往前找，取最靠近本月的上一条记录作为基准。
            val monthBaselineBalanceSum = findMonthBaselineBalanceSum(month, today)

            // 2. 查询当天的余额之和，查不到就算了
            val todayBalances =
                runCatching { repo.getBalancesByDate(today.format(dateFormatter)) }.getOrElse { emptyList() }
            val todayBalanceSum = todayBalances.sumOf { it.balance }

            // 3. 计算余额变动数字（两个都查到才有效，否则为 0）
            val balanceDelta = if (monthBaselineBalanceSum != null) {
                todayBalanceSum - monthBaselineBalanceSum
            } else 0.0

            // 4. 查询本月记录的所有 transaction，并按收入/支出和 tag 分组，包含未分类（tag 为空）
            val monthTransactions =
                runCatching { repo.getAllTransactionsDesc() }.getOrElse { emptyList() }
                    .filter { isSameMonth(it.recordDate, month) }

            val incomeGroups =
                monthTransactions.filter { it.amount > 0 }.groupBy { it.tag.ifBlank { "" } }

            val expenseGroups =
                monthTransactions.filter { it.amount <= 0 }.groupBy { it.tag.ifBlank { "" } }

            // 汇总收入/支出总额
            var totalIncome = incomeGroups.values.sumOf { group -> group.sumOf { it.amount } }
            var totalExpense =
                expenseGroups.values.sumOf { group -> group.sumOf { it.amount } } // 负数或 0

            // 5. 生成自动补差的临时项，确保 收入 + 支出 + 自动项 = 余额变动数字
            val recordedSum = totalIncome + totalExpense // 注意：expense 为负数
            val autoAmount = balanceDelta - recordedSum

            val incomeViews = toTransactionViewsFromRecords(incomeGroups)
            val expenseViews = toTransactionViewsFromRecords(expenseGroups)

            if (autoAmount != 0.0) {
                if (autoAmount > 0) {
                    totalIncome += autoAmount
                    incomeViews.add(
                        TransactionView(
                            amount = autoAmount,
                            description = "日常收入（自动）",
                            icon = Icons.Filled.Calculate
                        )
                    )
                } else {
                    totalExpense += autoAmount
                    expenseViews.add(
                        TransactionView(
                            amount = autoAmount,
                            description = "日常支出（自动）",
                            icon = Icons.Filled.Calculate
                        )
                    )
                }
            }

            // 6. 排序并取前 3
            val topIncome = incomeViews.sortedByDescending { it.amount }.take(3)
            val topExpense = expenseViews.sortedBy { it.amount }
                .take(3) // expense 通常为负数，按绝对值从大到小可改：sortedByDescending { kotlin.math.abs(it.amount) }

            _state.value = MonthlyLedgerState(
                monthLabel = month.toString(),
                balanceDelta = balanceDelta,
                totalIncome = totalIncome,
                topIncomes = topIncome,
                totalExpense = abs(totalExpense),
                topExpenses = topExpense.map { tran -> tran.copy(amount = abs(tran.amount)) },
            )
            println(_state.value)
        }
    }

    /**
     * 从当天开始往前查，优先返回“本月的第一条记录”的余额之和；
     * 若本月没有任何记录，则返回“最靠近本月的上一条记录”的余额之和；
     * 若一年内都找不到，返回 null。
     */
    private suspend fun findMonthBaselineBalanceSum(month: YearMonth, today: LocalDate): Double? {
        var cursor = today
        var firstInMonth: Double? = null

        repeat(366) {
            val balances =
                runCatching { repo.getBalancesByDate(cursor.format(dateFormatter)) }.getOrElse { emptyList() }
            val hasData = balances.isNotEmpty()
            val isInMonth = YearMonth.from(cursor) == month
            println("333 $cursor $firstInMonth")

            if (isInMonth) {
                if (hasData) {
                    // 记录“本月最早的一条记录”。因为我们在倒序回溯，
                    // 当离开本月之前最后一次更新到的 firstInMonth 就是最早的一条。
                    firstInMonth = balances.sumOf { it.balance }
                }
            } else {
                // 已经离开本月
                if (firstInMonth != null) {
                    // 如果在离开本月前已经在本月内找到过数据，则返回该“本月最早记录”
                    return firstInMonth
                }
                if (hasData) {
                    // 本月内未找到任何数据，则返回最靠近本月的上一条记录
                    return balances.sumOf { it.balance }
                }
            }

            cursor = cursor.minusDays(1)
        }

        // 回溯一年后：如果本月内找到过，则返回；否则返回 null
        return firstInMonth
    }

    private fun isSameMonth(recordDate: String, target: YearMonth): Boolean {
        return runCatching {
            LocalDate.parse(
                recordDate,
                dateFormatter
            )
        }.map { YearMonth.from(it) == target }.getOrDefault(false)
    }

    private fun Map<String, List<TransactionRecord>>.toLabelSumPairs(): List<Pair<String, Double>> {
        return this.map { (tag, list) ->
            val label = if (tag.isBlank()) "未分类" else tag
            val sum = list.sumOf { it.amount }
            label to sum
        }
    }

    private fun toTransactionViewsFromRecords(groups: Map<String, List<TransactionRecord>>): MutableList<TransactionView> {
        return groups.toLabelSumPairs().map { (label, sum) ->
            TransactionView(
                amount = sum, description = label, icon = Icons.Filled.Abc
            )
        }.toMutableList()
    }
}


@Composable
fun LedgerChartCard(viewModel: MonthlyLedgerViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Card(modifier = Modifier
        .pointerInput(Unit) {
            detectTapGestures(onTap = { _ ->
                MainActivity.navController.navigate("TransactionList")
            })
        }) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp)
        ) {
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Wallet,
                    contentDescription = "账本",
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            Color(0xFFFFA500).copy(
                                alpha = 0.2f
                            ), shape = CircleShape
                        )
                        .padding(4.dp),
                    tint = Color(0xFFFFA500)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "账本",
                    style = typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                )
            }
            Spacer(Modifier.height(16.dp))

            // 支出（展示前 3 项）
            SpendIncomeSummaryCard(
                isSpend = true,
                title = "本月支出",
                amount = state.totalExpense,
                data = state.topExpenses
            )

            Spacer(modifier = Modifier.height(9.dp))
            HorizontalDivider(modifier = Modifier.height(2.dp))
            Spacer(modifier = Modifier.height(9.dp))

            // 收入（展示前 3 项）
            SpendIncomeSummaryCard(
                isSpend = false,
                title = "本月收入",
                amount = state.totalIncome,
                data = state.topIncomes
            )
            Spacer(modifier = Modifier.height(10.dp))

//            NotifyCard()
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun SpendIncomeSummaryCard(
    isSpend: Boolean, title: String, amount: Double, data: List<TransactionView>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title, modifier = Modifier, // 标准内边距
                style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant) // 使用主题字体
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "￥", style = typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                    )
                )
                Text(
                    amount.format(2), style = typography.titleLarge.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
    FilteredRoundedLine(rawData = data.map { it.amount },
        thresholdPercentage = 0.02f,
        startColor = if (isSpend) colorScheme.primary else colorScheme.tertiary,
        endColor = colorScheme.secondary,
        onClick = { index ->
            println("你点击了第 $index 段")
        })
    Spacer(modifier = Modifier.height(10.dp))
    Column(modifier = Modifier.fillMaxWidth()) {
        // 展示交易记录
        if (data.isEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                if (isSpend) {
                    Text(
                        "- 无支出项 -",
                        style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant)
                    )
                } else {
                    Text(
                        "- 无收入项 -",
                        style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant)
                    )
                }
            }
        } else {
            data.forEach { transaction ->
                TransactionViewItem(
                    transaction
                )
            }
        }
    }
}

data class SegmentData(
    val value: Double, val originalIndex: Int // 记录原始位置，用于点击回调或保持颜色逻辑
)

@Composable
fun FilteredRoundedLine(
    rawData: List<Double>, thresholdPercentage: Float = 0.02f, // 2% 阈值
    startColor: Color, endColor: Color, onClick: (Int) -> Unit
) {
    // 将原始 float 转为带索引的对象
    val indexedData = remember(rawData) {
        rawData.mapIndexed { index, value -> SegmentData(value, index) }
    }

    // 执行递归过滤
    val filteredData = remember(indexedData) {
        recursiveFilter(indexedData, thresholdPercentage)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(11.dp), //稍微加高一点以便看清圆角
        // 关键点：因为每个段都有圆角，加上间距会让视觉更清晰，不会挤在一起
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val totalCount = filteredData.size

        if (totalCount == 0) {
            // 如果没有数据，展示占位内容
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(startColor, endColor)
                        )
                    )
            )
        } else {
            filteredData.forEachIndexed { index, item ->
                // 基于当前过滤后的列表位置渐变
                val fraction = if (totalCount > 1) index.toFloat() / (totalCount - 1) else 0f
                val color = lerp(startColor, endColor, fraction)

                Box(modifier = Modifier
                    .weight(item.value.toFloat()) // 按数值比例分配宽度
                    .fillMaxHeight()
                    // 关键点：给每个线段单独切圆角
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null // 无水波纹
                    ) {
                        onClick(item.originalIndex) // 返回原始数据的索引
                    })
            }

        }
    }
}

/**
 * 递归过滤函数
 * 逻辑：计算总和 -> 过滤掉占比小于阈值的 -> 如果列表长度有变化 (说明有删除)，则用新列表再算一遍
 */
private tailrec fun recursiveFilter(
    currentList: List<SegmentData>, threshold: Float
): List<SegmentData> {
    // 1. 如果列表为空，直接返回
    if (currentList.isEmpty()) return emptyList()

    // 2. 计算当前总和
    val total = currentList.sumOf { it.value.toDouble() }.toFloat()

    // 防止除以 0
    if (total == 0f) return currentList

    // 3. 筛选符合条件的数据
    val nextList = currentList.filter { item ->
        (item.value / total) >= threshold
    }

    // 4. 递归终止条件：如果筛选后的数量和筛选前一样，说明没有元素被删除，已经稳定
    return if (nextList.size == currentList.size) {
        currentList
    } else {
        // 5. 否则，拿着剩下的数据继续递归（因为总和变小了，比例关系变了）
        recursiveFilter(nextList, threshold)
    }
}

data class TransactionView(
    val amount: Double, val description: String, val icon: ImageVector
)

@Composable
fun TransactionViewItem(transactionView: TransactionView) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { _ ->
                    // 单击打开历史 Sheet
//                                    scope.launch { showHistorySheet = true }
                }, onDoubleTap = { _ -> }, onLongPress = { _ -> })
            }, verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = transactionView.icon,
                    contentDescription = transactionView.description,
                    modifier = Modifier.size(18.dp),
                    tint = colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = transactionView.description,
                style = typography.bodySmall,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "¥ " + transactionView.amount.format(2),
                style = typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "更多",
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size((typography.bodySmall.fontSize.value + 6).dp)
                .padding(end = 8.dp)
        )
    }
}

@Composable
fun NotifyCard() {
    Card(
        modifier = Modifier, colors = CardDefaults.cardColors().copy(
            containerColor = Color(0xFF176548).copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 10.dp)
                .padding(start = 12.dp, end = 18.dp)
        ) {
            Column {
                Icon(
                    imageVector = Icons.Outlined.Verified,
                    contentDescription = "",
                    tint = Color(0xFF176548),
                    modifier = Modifier
                        .size(26.dp)
                        .padding(end = 10.dp)
                )
            }
            Column() {
                Text(
                    "日均消费￥82，攒下￥4901，当前储蓄为￥10000，按此趋势自，再工作 20 年就能退休啦🎉",
                    style = typography.bodySmall.copy(
                        color = Color(0xFF176548)
                    ),
                )
            }
        }
    }
}

@Composable
fun SummaryStatisticsCard() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "统计指标", style = typography.bodySmall.copy(
                color = colorScheme.onSurfaceVariant
            )
        )
        Row(
            modifier = Modifier.width(80.dp), horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "均", style = typography.bodySmall.copy(
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorScheme.tertiary
                    )
                )
                Text(
                    "43.1", style = typography.bodySmall.copy(
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorScheme.tertiary
                    )
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "中", style = typography.bodySmall.copy(
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary
                    )
                )
                Text(
                    "43.1", style = typography.bodySmall.copy(
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary
                    )
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "众", style = typography.bodySmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.secondary
                    )
                )
                Text(
                    "40+", style = typography.bodySmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.secondary
                    )
                )
            }
        }
    }
}

@Preview(
    showBackground = true,  // 显示背景，这样能看清卡片轮廓
    backgroundColor = 0xFFF0F0F0, // 稍微设一点灰，让卡片白色/浅色背景更明显
    name = "Light Mode"
)
@Composable
fun LedgerChartCardPreview() {
    // 假设你的主题叫 AppTheme，一定要包一层
    // AppTheme {
    LedgerChartCard()
    // }
}
