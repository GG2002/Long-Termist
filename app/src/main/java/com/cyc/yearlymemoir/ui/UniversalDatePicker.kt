package com.cyc.yearlymemoir.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.cyc.yearlymemoir.MainActivity
import com.cyc.yearlymemoir.utils.UniversalDate
import com.cyc.yearlymemoir.utils.UniversalMDDateType
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
private fun StatefulDateInputTextField(
    value: Int, // The "true", canonical integer value from the UniversalDate state
    onValueChange: (Int) -> Unit, // Callback to update the UniversalDate when the text is a valid Int
    modifier: Modifier = Modifier,
    width: Int = 40,
    isFocused: Boolean,
) {
    // This is the local, temporary UI state. It can be any string.
    var text by remember { mutableStateOf(value.toString()) }

    // This effect ensures that if the date is changed by *another* input field,
    // our text field updates to reflect the new canonical value.
    LaunchedEffect(value) {
        if (text.toIntOrNull() != value) {
            text = value.toString()
        }
    }

    val textColor by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.6f,
        label = "textColor"
    )

    BasicTextField(
        value = text, // Bind to the local string state
        onValueChange = { newText ->
            // 1. Update the local state immediately. The user sees what they type.
            text = newText
            // 2. Try to parse it.
            val newInt = newText.toIntOrNull()
            if (newInt != null) {
                // 3. If parsing is successful, call the main callback to update the global state.
                onValueChange(newInt)
            }
        },
        enabled = isFocused,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.width(width.dp),
        textStyle = TextStyle(
            fontSize = 28.sp,
            textAlign = TextAlign.Right,
            color = colorScheme.primary.copy(alpha = textColor)
        ),
        singleLine = true,
        cursorBrush = SolidColor(colorScheme.primary)
    )
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


/**
 * Main Composable for the Universal Date Picker
 */
@Composable
fun UniversalDatePicker(
    initialDate: UniversalDate,
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

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            // 1. Animate scroll to snap to the center
            listState.animateScrollToItem(centeredItemIndex)

            // 2. Based on the final centered item, update the UniversalDate object
            //    to reflect the selected *type*.
            if (centeredItemIndex != initialDate.getMDDateType()) {
                val (newYear, newMdDate) = when (centeredItemIndex) {
                    0 -> Pair(initialDate.getSolarYear(), initialDate.asMonthDay())
                    1 -> Pair(initialDate.getLunarYear(), initialDate.asLunarDate())
                    2 -> Pair(initialDate.getSolarYear(), initialDate.asMonthWeekday())
                    else -> Pair(
                        initialDate.getSolarYear(),
                        initialDate.asMonthDay()
                    ) // Safe default
                }
                onDateChanged(UniversalDate(newYear, newMdDate))
            }
        }
    }

    val itemHeight = 30.dp
    val containerHeight = itemHeight * 3

    LaunchedEffect(Unit) {
        val initialIndex = initialDate.getMDDateType()
        // Use scrollToItem for an immediate jump without animation on init.
        listState.scrollToItem(initialIndex)
    }

    Spacer(modifier = Modifier.height(16.dp))

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Master Year Input
        Row(
            modifier = Modifier
                .padding(top = 100.dp)
                .height(containerHeight + 40.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier
                    .width(100.dp)
                    .padding(bottom = itemHeight),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val (历法, year) = when (initialDate.getMDDateType()) {
                    0, 2 -> {
                        Pair("公历", initialDate.getSolarYear())
                    }

                    1 -> {
                        Pair("农历", initialDate.getLunarYear())
                    }

                    else -> {
                        Pair("公历", initialDate.getSolarYear())
                    }
                }
                Text(
                    历法,
                    style = typography.titleLarge.copy(
                        fontSize = 29.sp,
                        color = colorScheme.onSurface
                    )
                )
                Row(modifier = Modifier.height(itemHeight), verticalAlignment = Alignment.Bottom) {
                    StatefulDateInputTextField(
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
                        isFocused = true
                    )
                    DateLabel("年", true)
                }
            }
            // The picker
            LazyColumn(
                state = listState,
                modifier = Modifier.height(containerHeight),
                contentPadding = PaddingValues(vertical = itemHeight) // This padding centers the first and last items
            ) {
                items(3) { index ->
                    val isFocused = (index == centeredItemIndex)
                    val scale by animateFloatAsState(if (isFocused) 1.0f else 0.8f)
                    val alpha by animateFloatAsState(if (isFocused) 1.0f else 0.5f)

                    Box(
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth()
                            .scale(scale)
                            .alpha(alpha),
                        contentAlignment = Alignment.Center
                    ) {
                        when (index) {
                            0 -> MonthDayRow(
                                date = initialDate,
                                onDateChanged = onDateChanged,
                                isFocused = isFocused
                            )

                            1 -> LunarDateRow(
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


// Row 1: M-D (e.g., 6月13日)
@Composable
fun MonthDayRow(
    date: UniversalDate,
    onDateChanged: (UniversalDate) -> Unit,
    isFocused: Boolean
) {
    val mdDate = date.asMonthDay()

    // This logic now only deals with valid integers.
    val onValueChange = { newMonth: Int, newDay: Int ->
        try {
            if (newMonth < 0 || newDay < 0) {
                throw Exception("not allowed Month or Day integer")
            }
            LocalDate.of(date.getSolarYear(), newMonth, newDay)
            val newMdDate = UniversalMDDateType.MonthDay(newMonth, newDay)
            onDateChanged(UniversalDate(date.getSolarYear(), newMdDate))
        } catch (e: Exception) {
            // Invalid date (e.g., Feb 30), do nothing
        }
    }

    Row(verticalAlignment = Alignment.Bottom) {
        // Use the new Stateful component
        StatefulDateInputTextField(
            value = mdDate.month, // Pass the Int value
            onValueChange = { newMonth -> // Receive a valid Int
                onValueChange(newMonth, mdDate.day)
            },
            isFocused = isFocused,
        )
        DateLabel("月", isFocused)
        StatefulDateInputTextField(
            value = mdDate.day, // Pass the Int value
            onValueChange = { newDay -> // Receive a valid Int
                onValueChange(mdDate.month, newDay)
            },
            isFocused = isFocused
        )
        DateLabel("日", isFocused)
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


// Row 2: M-W-D (e.g., 5月第2个周日)
@Composable
fun MonthWeekdayRow(
    date: UniversalDate,
    onDateChanged: (UniversalDate) -> Unit,
    isFocused: Boolean
) {
    val mwDate = date.asMonthWeekday()
    println("mwDate $mwDate")

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
            UniversalDate(date.getSolarYear(), newMdDate).asMonthDay()
            onDateChanged(UniversalDate(date.getSolarYear(), newMdDate))
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
        StatefulDateInputTextField(
            value = mwDate.month,
            onValueChange = { newMonth ->
                onValueChange(newMonth, mwDate.weekOrder, mwDate.weekday)
            },
            isFocused = isFocused
        )
        DateLabel("月 第", isFocused)
        StatefulDateInputTextField(
            value = mwDate.weekOrder,
            onValueChange = { newWeekOrder ->
                onValueChange(mwDate.month, newWeekOrder, mwDate.weekday)
            },
            width = 23,
            isFocused = isFocused
        )
        DateLabel("个", isFocused)

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
                        .height(WEEKDAY_PICKER_TOTAL_HEIGHT_DP.dp)
                        .background(colorScheme.background, RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            colorScheme.outline.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Box(modifier = Modifier.offset {
                        IntOffset(0, pickerOffsetY.roundToInt())
                    }) {
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


// Row 3: Lunar (e.g., 四月初五)
@Composable
fun LunarDateRow(
    date: UniversalDate,
    onDateChanged: (UniversalDate) -> Unit,
    isFocused: Boolean
) {
    val lDate = date.asLunarDate()

    val onValueChange = { newMonth: Int, newDay: Int, newIsLeap: Boolean ->
        try {
            if (newMonth < 0 || newDay < 0) {
                throw Exception("not allowed Month or Day integer")
            }
            val newMdDate = UniversalMDDateType.LunarDate(newMonth, newDay, newIsLeap)
            UniversalDate(date.getLunarYear(), newMdDate).asMonthDay()
            onDateChanged(UniversalDate(date.getLunarYear(), newMdDate))
        } catch (e: Exception) {
            // Invalid lunar date
        }
    }

    Row(verticalAlignment = Alignment.Bottom) {
        DateLabel("闰月", isFocused)
        Switch(
            checked = lDate.isLeap,
            onCheckedChange = { newIsLeap ->
                onValueChange(lDate.month, lDate.day, newIsLeap)
            },
            enabled = isFocused,
            modifier = Modifier.scale(0.7f)
        )
        StatefulDateInputTextField(
            value = lDate.month,
            onValueChange = { newMonth ->
                onValueChange(newMonth, lDate.day, lDate.isLeap)
            },
            isFocused = isFocused
        )
        DateLabel("月", isFocused)
        StatefulDateInputTextField(
            value = lDate.day,
            onValueChange = { newDay ->
                onValueChange(lDate.month, newDay, lDate.isLeap)
            },
            isFocused = isFocused
        )
        DateLabel("日", isFocused)
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
                    UniversalMDDateType.MonthDay(12, 22)
                )
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UniversalDatePicker(
                initialDate = date,
                onDateChanged = { newDate -> date = newDate }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            Text("当前选中日期:", style = typography.titleLarge)
            Text(date.toChineseString(), fontSize = 20.sp)
        }
    }
}