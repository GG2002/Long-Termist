package com.cyc.yearlymemoir.ui.personalbanlance

import android.app.Application
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyc.yearlymemoir.MainActivity.Companion.navController
import com.cyc.yearlymemoir.MainApplication
import com.cyc.yearlymemoir.domain.model.BalanceRecord
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.extensions.format
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

/**
 * 聚合资产概况所需的指标：总余额、本月支出、本月收入、现金资产走势图。
 * 独立于其他页面的 ViewModel，直接使用仓库数据计算。
 */
class AssetSummaryViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = MainApplication.repository
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _totalBalance = MutableStateFlow(0.0)
    val totalBalance: StateFlow<Double> = _totalBalance

    private val _monthExpense = MutableStateFlow(0.0)
    val monthExpense: StateFlow<Double> = _monthExpense

    private val _monthIncome = MutableStateFlow(0.0)
    val monthIncome: StateFlow<Double> = _monthIncome

    private val _balanceChart = MutableStateFlow<List<Pair<String, Double>>>(emptyList())
    val balanceChart: StateFlow<List<Pair<String, Double>>> = _balanceChart

    fun loadAll() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                computeTodayTotalBalance()
                computeMonthIncomeExpenseWithAutoAdjustment()
                computeRecentDailyBalanceChart()
            }
        }
    }

    private suspend fun computeTodayTotalBalance() {
        val today = LocalDate.now().format(dateFormatter)
        val balances = runCatching { repo.getBalancesByDate(today) }.getOrElse { emptyList() }
        _totalBalance.value = balances.sumOf { it.balance }
    }

    /**
     * 本月收入/支出：先计算本月余额基线与今日余额的差额，再与已记录的收入/支出求差，得到自动补差项计入收入或支出。
     */
    private suspend fun computeMonthIncomeExpenseWithAutoAdjustment() {
        val month = YearMonth.now()
        val today = LocalDate.now()

        // 本月基线余额
        val monthBaselineBalanceSum = findMonthBaselineBalanceSum(month, today)

        // 今日余额总和
        val todayBalances = runCatching { repo.getBalancesByDate(today.format(dateFormatter)) }.getOrElse { emptyList() }
        val todayBalanceSum = todayBalances.sumOf { it.balance }

        // 本月余额变动
        val balanceDelta = if (monthBaselineBalanceSum != null) {
            todayBalanceSum - monthBaselineBalanceSum
        } else 0.0

        // 当月交易记录
        val monthTransactions = runCatching { repo.getAllTransactionsDesc() }.getOrElse { emptyList() }
            .filter { isSameMonth(it.recordDate, month) }

        val incomeSum = monthTransactions.filter { it.amount > 0 }.sumOf { it.amount }
        val expenseSum = monthTransactions.filter { it.amount <= 0 }.sumOf { it.amount } // 负数或 0

        // 自动补差，确保 收入 + 支出 + 自动项 = 余额变动
        val recordedSum = incomeSum + expenseSum
        val autoAmount = balanceDelta - recordedSum

        var finalIncome = incomeSum
        var finalExpense = expenseSum
        if (autoAmount != 0.0) {
            if (autoAmount > 0) finalIncome += autoAmount else finalExpense += autoAmount
        }

        _monthIncome.value = finalIncome
        _monthExpense.value = kotlin.math.abs(finalExpense)
    }

    /**
     * 现金资产走势图：最近 30 天每日总余额（MM-dd）。
     */
    private suspend fun computeRecentDailyBalanceChart() {
        val allBalances = runCatching { repo.getAllBalances() }.getOrElse { emptyList() }
        val dailyTotals = aggregateDailyTotals(allBalances)
            .sortedBy { it.first }
            .takeLast(30)
        _balanceChart.value = dailyTotals.map { (date, sum) ->
            String.format("%02d-%02d", date.monthValue, date.dayOfMonth) to sum
        }
    }

    private fun aggregateDailyTotals(allBalances: List<BalanceRecord>): List<Pair<LocalDate, Double>> {
        return allBalances
            .groupBy { it.recordDate }
            .map { (date, records) -> Pair(LocalDate.parse(date), records.sumOf { it.balance }) }
    }

    private suspend fun findMonthBaselineBalanceSum(month: YearMonth, today: LocalDate): Double? {
        var cursor = today
        var firstInMonth: Double? = null

        repeat(366) {
            val balances = runCatching { repo.getBalancesByDate(cursor.format(dateFormatter)) }.getOrElse { emptyList() }
            val hasData = balances.isNotEmpty()
            val isInMonth = YearMonth.from(cursor) == month

            if (isInMonth) {
                if (hasData) {
                    // 倒序回溯时，离开本月之前最新一次更新到的 firstInMonth 即为本月最早记录的余额和
                    firstInMonth = balances.sumOf { it.balance }
                }
            } else {
                if (firstInMonth != null) return firstInMonth
                if (hasData) return balances.sumOf { it.balance }
            }
            cursor = cursor.minusDays(1)
        }
        return firstInMonth
    }

    private fun isSameMonth(recordDate: String, target: YearMonth): Boolean {
        return runCatching { LocalDate.parse(recordDate) }
            .map { YearMonth.from(it) == target }
            .getOrDefault(false)
    }
}

@Composable
fun AssetSummaryCard(
    viewModel: AssetSummaryViewModel = viewModel()
) {
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val monthExpense by viewModel.monthExpense.collectAsStateWithLifecycle()
    val monthIncome by viewModel.monthIncome.collectAsStateWithLifecycle()
    val balanceChartData by viewModel.balanceChart.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadAll() }
    Card(
        Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    navController.navigate("PersonalBalanceScreen")
                }
            )
        }) {
        Column(Modifier.padding(16.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
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
                    text = "资产概况",
                    style = typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "总余额：",
                        style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "￥", style = typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                            )
                        )
                        Text(
                            String.format(Locale.getDefault(), "%.2f", totalBalance),
                            style = typography.bodySmall.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
                VerticalDivider(
                    Modifier
                        .width(2.dp)
                        .height(30.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "本月支出",
                        style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "￥", style = typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                            )
                        )
                        Text(
                            monthExpense.format(2),
                            style = typography.titleLarge.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        )
                    }
                }
                VerticalDivider(
                    Modifier
                        .width(2.dp)
                        .height(30.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "本月收入", modifier = Modifier, // 标准内边距
                        style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant) // 使用主题字体
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "￥", style = typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                            )
                        )
                        Text(
                            monthIncome.format(2),
                            style = typography.titleLarge.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        )
                    }
                }


            }
            LineChart(
                modifier = Modifier
                    .width(500.dp)
                    .height(180.dp),
                data = listOf(
                    Line(
                        label = "现金资产变动",
                        values = balanceChartData.map { it.second },
                        // 折线颜色使用主强调色
                        color = SolidColor(colorScheme.primary),
                        curvedEdges = false,
                        drawStyle = DrawStyle.Stroke(width = 1.dp),
                        // 渐变填充的起始色使用带透明度的主强调色
                        firstGradientFillColor = colorScheme.primary.copy(alpha = .4f),
                        secondGradientFillColor = Color.Transparent,
                        strokeAnimationSpec = tween(
                            750,
                            easing = FastOutSlowInEasing
                        ),
                        gradientAnimationDelay = 100,
                    )
                ),
                indicatorProperties = HorizontalIndicatorProperties(
                    enabled = false,
                    padding = 6.dp,
                    textStyle = typography.bodySmall.copy(
                        fontSize = 9.sp,
                        color = colorScheme.onSurfaceVariant
                    ),
                    contentBuilder = { it.format(0) }
                ),
                labelProperties = LabelProperties(
                    enabled = false,
                    padding = 2.dp,
                    textStyle = typography.labelSmall.copy(
                        fontSize = 7.sp,
                        color = colorScheme.onSurfaceVariant
                    ),
                    labels = balanceChartData.map { it.first }.sampleUpToN(7)
                ),
                // 左上角的 legend 标，展示单个指标的时候当然要禁掉
                labelHelperProperties = LabelHelperProperties(
                    enabled = false,
                    textStyle = typography.bodyMedium.copy(
                        color = colorScheme.onSurface
                    ),
                ),
                minValue = max(
                    balanceChartData.minOfOrNull { it.second }?.minus(100)
                        ?: 0.0, 0.0
                ),
                maxValue = balanceChartData.maxOfOrNull { it.second }?.plus(100)
                    ?: 0.0,
            )
        }
    }
}

@Preview
@Composable
fun AssetSummaryCardPreview() {
    AssetSummaryCard()
}