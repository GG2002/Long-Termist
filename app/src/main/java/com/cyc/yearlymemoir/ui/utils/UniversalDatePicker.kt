package com.cyc.yearlymemoir.ui.utils

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.cyc.yearlymemoir.MainActivity
import com.cyc.yearlymemoir.domain.model.UniversalDate
import com.cyc.yearlymemoir.domain.model.UniversalMDDateType
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt


@Composable
fun UnifiedDateInput(
    value: Int,
    onValueChange: (Int) -> Unit,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    width: Int = 40,
    prefixStr: String? = null,
    suffixStr: String? = null,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // 内部状态
    var inputState by remember {
        mutableStateOf(
            TextFieldValue(
                text = if (prefixStr == null) value.toString()
                    .padStart(2, '0') else value.toString(),
                selection = TextRange(value.toString().length)
            )
        )
    }

    // 状态同步
    LaunchedEffect(value) {
        if (inputState.text.toIntOrNull() != value) {
            val newText = value.toString()
            inputState = TextFieldValue(
                text = newText,
                selection = TextRange(newText.length)
            )
        }
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.6f,
        label = "alpha"
    )

    // 根容器
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // --- 1. 底层视觉与输入 (傀儡层) ---
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (prefixStr != null) {
                Text(
                    text = prefixStr,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            BasicTextField(
                value = inputState,
                onValueChange = { newValue ->
                    inputState = newValue
                    val newInt = newValue.text.toIntOrNull()
                    if (newInt != null) {
                        onValueChange(newInt)
                    }
                },
                modifier = Modifier
                    .width(width.dp)
                    .focusRequester(focusRequester)
                    // 核心技巧：虽然我们需要它能输入，但我们不想让它处理任何触摸
                    // 这里不需要做 pointerInput 拦截，因为上面的 Spacer 会物理隔绝它
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            // 兜底：只要获得焦点，永远锁死光标在最后
                            inputState =
                                inputState.copy(selection = TextRange(inputState.text.length))
                        }
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    fontSize = 28.sp,
                    textAlign = TextAlign.Right,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha)
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
            )

            if (suffixStr != null) {
                Text(
                    text = suffixStr,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        // --- 2. 顶层交互 (控制层) ---
        // 这是一个“隐形按钮”，因为写在 Row 后面，所以它在 Z 轴的最上层。
        // 它完全挡住了下面 Row 里的输入框和文字，所有的点击都由它接管。
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Transparent) // 必须设置背景 (即使透明) 以响应点击
                .clickable(
                    // 使用 clickable 而不是 detectTapGestures
                    // clickable 对“轻微手指抖动”有容错处理，点击成功率远高于手势检测
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null // 无水波纹，如需反馈可去掉这行
                ) {
                    // 1. 强制夺取焦点
                    focusRequester.requestFocus()

                    // 2. 强制修正光标位置（即使原本已经是焦点状态）
                    inputState = inputState.copy(selection = TextRange(inputState.text.length))

                    // 3. 强制弹出键盘
                    keyboardController?.show()
                }
        )
    }
}


/**
 * Main Composable for the Universal Date Picker
 */
@Composable
fun UniversalDatePicker(
    initialDate: UniversalDate = UniversalDate.today(),
    onDateChanged: (UniversalDate) -> Unit
) {
    val listState = rememberLazyListState()

    // This derived state correctly identifies the centered item index
    val centeredItemIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                listState.firstVisibleItemIndex
            } else {
                val firstVisibleItem = visibleItemsInfo.first()
                val centerThreshold = firstVisibleItem.size / 2
                listState.firstVisibleItemIndex + if (listState.firstVisibleItemScrollOffset > centerThreshold) 1 else 0
            }
        }
    }

    // 起始阶段先 scroll 至传入的初始日期类型
    LaunchedEffect(Unit) {
        val initialIndex = initialDate.getMDDateType()
        listState.scrollToItem(initialIndex)
    }
    // 随滚动触发
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            listState.animateScrollToItem(centeredItemIndex)

            if (centeredItemIndex != initialDate.getMDDateType()) {
                val (newYear, newMdDate) = when (centeredItemIndex) {
                    0 -> initialDate.asLunarDate()
                    1 -> initialDate.asMonthDay()
                    2 -> initialDate.asMonthWeekday()
                    else -> initialDate.asMonthDay()
                }
                onDateChanged(UniversalDate(newYear, newMdDate))
            }
        }
    }

    val itemHeight = 30.dp
    val containerHeight = itemHeight * 3

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Master Year Input
        Row(
            modifier = Modifier
                .height(containerHeight),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .width(100.dp)
                    .padding(top = itemHeight),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val (历法, year) = when (initialDate.getMDDateType()) {
                    0 -> {
                        Pair("农历", initialDate.getLunarYear())
                    }

                    1, 2 -> {
                        Pair("公历", initialDate.getSolarYear())
                    }

                    else -> {
                        Pair("公历", initialDate.getSolarYear())
                    }
                }
                Row(modifier = Modifier.height(itemHeight), verticalAlignment = Alignment.Bottom) {
                    UnifiedDateInput(
                        value = year,
                        onValueChange = { newYear ->
                            onDateChanged(
                                UniversalDate(
                                    newYear,
                                    initialDate.getRawMDDate()
                                )
                            )
                        },
                        width = 70,
                        isFocused = true,
                        suffixStr = "年",
                    )
                }
                Text(
                    历法,
                    style = typography.labelLarge.copy(
                        fontSize = 16.sp,
                        color = colorScheme.onSurface
                    )
                )
            }
            val consumeAllScrollEvents = object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset = available
            }
            // The picker
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .height(containerHeight)
                    .nestedScroll(consumeAllScrollEvents),
                contentPadding = PaddingValues(vertical = itemHeight) // This padding centers the first and last items
            ) {
                items(3) { index ->
                    val isFocused = (index == centeredItemIndex)
                    val scale by animateFloatAsState(if (isFocused) 1.0f else 0.8f)
                    val alpha by animateFloatAsState(if (isFocused) 1.0f else 0.5f)

                    Box(
                        modifier = Modifier
                            .height(itemHeight)
                            .width(200.dp)
                            .scale(scale)
                            .alpha(alpha),
                        contentAlignment = Alignment.Center
                    ) {
                        when (index) {
                            0 -> LunarDateRow(
                                date = initialDate,
                                onDateChanged = onDateChanged,
                                isFocused = isFocused
                            )

                            1 -> MonthDayRow(
                                date = initialDate,
                                onDateChanged = onDateChanged,
                                isFocused = isFocused
                            )

                            2 -> MonthWeekdayRow(
                                date = initialDate,
                                onDateChanged = onDateChanged,
                                isFocused = isFocused
                            )
                        }
                    }
                }
            }
        }

    }
}


// Row 1: M-D (e.g., 6 月 13 日)
@Composable
fun MonthDayRow(
    date: UniversalDate,
    onDateChanged: (UniversalDate) -> Unit,
    isFocused: Boolean
) {
    val (year, mdDate) = date.asMonthDay()

    // This logic now only deals with valid integers.
    val onValueChange = { newMonth: Int, newDay: Int ->
        try {
            if (newMonth < 0 || newDay < 0) {
                throw Exception("not allowed Month or Day integer")
            }
            LocalDate.of(year, newMonth, newDay)
            val newMdDate = UniversalMDDateType.MonthDay(newMonth, newDay)
            onDateChanged(UniversalDate(year, newMdDate))
        } catch (e: Exception) {
            // Invalid date (e.g., Feb 30), do nothing
        }
    }

    Row(verticalAlignment = Alignment.Bottom) {
        UnifiedDateInput(
            value = mdDate.month,
            onValueChange = { newMonth ->
                onValueChange(newMonth, mdDate.day)
            },
            isFocused = isFocused,
            suffixStr = "月"
        )
        UnifiedDateInput(
            value = mdDate.day,
            onValueChange = { newDay ->
                onValueChange(mdDate.month, newDay)
            },
            isFocused = isFocused,
            suffixStr = "日"
        )
    }
}

private val WEEKDAYS_CHINESE = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
private val WEEKDAYS_ENUM =
    DayOfWeek.entries.toTypedArray() // MONDAY is at index 0, SUNDAY is at index 6

private fun dayOfWeekToChinese(weekday: DayOfWeek): String {
    return when (weekday) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }
}


// Row 2: M-W-D (e.g., 5 月第 2 个周日)
@Composable
fun MonthWeekdayRow(
    date: UniversalDate,
    onDateChanged: (UniversalDate) -> Unit,
    isFocused: Boolean
) {
    val (year, mwDate) = date.asMonthWeekday()

    var weekdayItemHightDP by remember { mutableIntStateOf(30) }
    val WEEKDAY_ITEM_HEIGHT_PIXEL =
        (weekdayItemHightDP * MainActivity.metrics.density).toInt()
    val WEEKDAY_PICKER_TOTAL_HEIGHT_DP =
        weekdayItemHightDP * WEEKDAYS_CHINESE.size

    val weekdayPickerOffsetY =
        ((WEEKDAY_PICKER_TOTAL_HEIGHT_DP - weekdayItemHightDP / 2) * MainActivity.metrics.density / 2).toInt()
    // --- State for the Drag-to-Select Interaction ---
    var isPickingWeekday by remember { mutableStateOf(false) }
    // The Y-offset of the picker list, controlled by drag
    var pickerOffsetY by remember { mutableStateOf(0f) }
    // The calculated index (0-6) based on the offset
    val selectedIndex by remember {
        derivedStateOf {
            // Calculate which item is at the center based on the offset
            // Clamp the value to ensure it's within the valid index range [0, 6]
            ((-(pickerOffsetY - weekdayPickerOffsetY) / WEEKDAY_ITEM_HEIGHT_PIXEL).roundToInt()).coerceIn(
                0,
                WEEKDAYS_CHINESE.size - 1
            )
        }
    }
    // The position on screen where the popup should appear
    var popupPosition by remember { mutableStateOf(IntOffset.Zero) }
    // A reference to the "周日" text to get its position
    val textRef = remember { Ref<LayoutCoordinates>() }


    val onValueChange = { newMonth: Int, newWeekOrder: Int, newWeekday: DayOfWeek ->
        try {
            if (newMonth < 0 || newWeekOrder < 0) {
                throw Exception("not allowed Month or WeekOrder integer")
            }
            val newMdDate = UniversalMDDateType.MonthWeekday(newMonth, newWeekOrder, newWeekday)
            UniversalDate(year, newMdDate).asMonthDay()
            onDateChanged(UniversalDate(year, newMdDate))
        } catch (e: Exception) {
            // Invalid combination
        }
    }

    val haptic = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        snapshotFlow { selectedIndex }
            .distinctUntilChanged() // Only react when the value actually changes
            .collect {
                // We don't want to vibrate on the very first composition, only on subsequent changes.
                if (isPickingWeekday) { // Only vibrate when the user is actively picking
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
    }

    Row(verticalAlignment = Alignment.Bottom) {
        UnifiedDateInput(
            value = mwDate.month,
            onValueChange = { newMonth ->
                onValueChange(newMonth, mwDate.weekOrder, mwDate.weekday)
            },
            isFocused = isFocused,
            suffixStr = "月 "
        )
        UnifiedDateInput(
            value = mwDate.weekOrder,
            onValueChange = { newWeekOrder ->
                onValueChange(mwDate.month, newWeekOrder, mwDate.weekday)
            },
            isFocused = isFocused,
            width = 23,
            prefixStr = "第",
            suffixStr = "个 "
        )

        Spacer(modifier = Modifier.width(4.dp))
        val textColor by animateFloatAsState(
            targetValue = if (isFocused) 1f else 0.6f,
            label = "weekday_text_color"
        )
        Text(
            text = dayOfWeekToChinese(mwDate.weekday),
            fontSize = 23.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.primary.copy(alpha = textColor),
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    // Store the layout coordinates of the text
                    textRef.value = coordinates
                }
                .pointerInput(mwDate) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            if (!isFocused) return@detectDragGesturesAfterLongPress

                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            val coordinates = textRef.value
                            if (coordinates != null) {
                                val textPositionInScreen = coordinates.positionInParent()
                                popupPosition = IntOffset(
                                    x = (textPositionInScreen.x).roundToInt(),
                                    y = (textPositionInScreen.y).roundToInt()
                                )
                            }

                            val itemHeightPx = weekdayItemHightDP.dp.toPx()
                            pickerOffsetY = -(mwDate.weekday.value - 1) * itemHeightPx +
                                    weekdayPickerOffsetY

                            isPickingWeekday = true
                        },
                        onDragEnd = {
                            isPickingWeekday = false
                            val finalWeekday = WEEKDAYS_ENUM[selectedIndex]
                            if (finalWeekday != mwDate.weekday) {
                                onValueChange(mwDate.month, mwDate.weekOrder, finalWeekday)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            pickerOffsetY += dragAmount.y
                        }
                    )
                }
        )

        // This is the Popup that appears when showWeekdayPicker is true
        if (isPickingWeekday) {
            Popup(
                // Use the calculated screen position, but adjust so the popup's center is at the coordinate
                offset = popupPosition - IntOffset(
                    x = 0,
                    y = weekdayPickerOffsetY
                ),
                onDismissRequest = { isPickingWeekday = false }
            ) {
                Box(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .height((WEEKDAY_PICKER_TOTAL_HEIGHT_DP + 10).dp)
                        .background(colorScheme.background, RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            colorScheme.outline.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Box(modifier = Modifier
                        .offset {
                            IntOffset(0, pickerOffsetY.roundToInt())
                        }
                        .padding(4.dp)) {
                        Column(
                            modifier = Modifier,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            WEEKDAYS_CHINESE.forEachIndexed { index, weekdayName ->
                                val isSelected = (index == selectedIndex)
                                Row(modifier = Modifier.height(weekdayItemHightDP.dp)) {
                                    Text(
                                        text = weekdayName,
                                        color = if (isSelected) colorScheme.primary else colorScheme.onSurface,
                                        fontSize = 23.sp
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        colorScheme.surfaceVariant,
                                        Color.Transparent,
                                        Color.Transparent,
                                        colorScheme.surfaceVariant
                                    ),
                                    startY = 0.0f,
                                    endY = WEEKDAY_PICKER_TOTAL_HEIGHT_DP * MainActivity.metrics.density
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                    )
                    HorizontalDivider(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}


// A helper for static text like "月", "日"
@Composable
private fun DateLabel(text: String, isFocused: Boolean) {
    val textColor by animateFloatAsState(targetValue = if (isFocused) 1f else 0.6f)
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = colorScheme.onSurface.copy(alpha = textColor),
        modifier = Modifier.padding(start = 4.dp),
    )
}

// Row 3: Lunar (e.g., 四月初五)
@Composable
fun LunarDateRow(
    date: UniversalDate,
    onDateChanged: (UniversalDate) -> Unit,
    isFocused: Boolean
) {
    val (year, lDate) = date.asLunarDate()

    val onValueChange = { newMonth: Int, newDay: Int, newIsLeap: Boolean ->
        try {
            if (newMonth < 0 || newDay < 0) {
                throw Exception("not allowed Month or Day integer")
            }
            val newMdDate = UniversalMDDateType.LunarDate(newMonth, newDay, newIsLeap)
            UniversalDate(year, newMdDate).asMonthDay()
            onDateChanged(UniversalDate(year, newMdDate))
        } catch (e: Exception) {
            // Invalid lunar date
        }
    }

    Row(verticalAlignment = Alignment.Bottom) {
        if (lDate.isLeap) {
            DateLabel("闰月", isFocused)
//            Switch(
//                checked = lDate.isLeap,
//                onCheckedChange = { newIsLeap ->
//                    onValueChange(lDate.month, lDate.day, newIsLeap)
//                },
//                enabled = isFocused,
//                modifier = Modifier.scale(0.7f)
//            )
        }
        UnifiedDateInput(
            value = lDate.month,
            onValueChange = { newMonth ->
                onValueChange(newMonth, lDate.day, lDate.isLeap)
            },
            isFocused = isFocused,
            suffixStr = "月"
        )
        UnifiedDateInput(
            value = lDate.day,
            onValueChange = { newDay ->
                onValueChange(lDate.month, newDay, lDate.isLeap)
            },
            isFocused = isFocused,
            suffixStr = "日"
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun UniversalDatePickerPreview() {
    MaterialTheme {
        var date by remember {
            mutableStateOf(
                UniversalDate(
                    2025,
                    UniversalMDDateType.MonthDay(12, 2)
                )
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            UniversalDatePicker(
                initialDate = date,
                onDateChanged = { newDate -> date = newDate }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            Text("当前选中日期：", style = typography.titleLarge)
            Text(date.toChineseString(), fontSize = 20.sp)
        }
    }
}