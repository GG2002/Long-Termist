package com.cyc.yearlymemoir.ui.yearlycalendar

data class MemoryData(
    val summaryList: List<String> = listOf(),
    // show...Day flags are ignored based on new requirements
    val showSolarDay: Boolean = true,
    val showWeekDay: Boolean = false,
    val showLunarDay: Boolean = false,
)

// 新的数据类，用于存储每个日期单元格的预计算显示信息
data class DayDisplayData(
    val dateKey: String, // "MM-dd"
    val isToday: Boolean,
    val solarDayText: String,
    val weekDayText: String?, // Nullable if not shown
    val lunarDayText: String?, // Nullable if not shown
    val summaryList: List<String>,
    // 这些标志结合了 MemoryData 的设置和 'today' 的特殊处理
    val showSolarDay: Boolean,
    val showWeekDay: Boolean,
    val showLunarDay: Boolean
)

// 预计算结果的数据结构
data class PrecomputedData(
    val groupedItems: List<List<DayDisplayData>>, // 122行 x 3列
    val rowHeights: List<Float>,          // 122个行高 (px)
    val targetItemIndex: Int
)