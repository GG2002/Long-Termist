package com.cyc.yearlymemoir.ui.yearlycalendar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.cyc.yearlymemoir.MainActivity
import com.cyc.yearlymemoir.MainApplication
import com.cyc.yearlymemoir.domain.model.FIELD_TYPE_NUM
import com.cyc.yearlymemoir.domain.model.FIELD_TYPE_STR
import com.cyc.yearlymemoir.domain.model.FIELD_TYPE_TEXT
import com.cyc.yearlymemoir.domain.model.Field
import com.cyc.yearlymemoir.domain.model.UniversalDate
import com.cyc.yearlymemoir.domain.model.YearlyDetail
import com.cyc.yearlymemoir.ui.UniversalDatePicker
import com.cyc.yearlymemoir.utils.formatDateComponents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// 主界面入口
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EveryDayScreen() {
    val navController = MainActivity.navController
    val sheetState: SheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded, skipHiddenState = true // 通常我们不希望卡片能被完全隐藏
    )
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )

    // 表单
    var formState by remember { mutableStateOf(ReminderFormState()) }

    val today = LocalDate.now()
    val (solar, weekday, lunar) = remember { formatDateComponents(today) }

    BottomSheetScaffold(modifier = Modifier,
        scaffoldState = scaffoldState,
        sheetPeekHeight = 150.dp,
        topBar = {
            TopAppBar(
                title = {
                    DateHeader(solar = solar, weekday = weekday, lunar = lunar)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.primaryContainer,
                    titleContentColor = colorScheme.onPrimaryContainer,
                ), // ✨ 设置背景色
                actions = {
                    IconButton(onClick = { navController.navigate("PermissionScreen") }) {
                        Icon(Icons.Default.Settings, contentDescription = "菜单")
                    }
                })
        },
        sheetDragHandle = {
            Spacer(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        color = BottomSheetDefaults.ScrimColor, shape = CircleShape
                    )
            )
        },
        sheetContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                val isExpanded = sheetState.targetValue == SheetValue.Expanded
                val iconRotation by animateFloatAsState(
                    targetValue = if (isExpanded) 45f else 0f, label = "IconRotation"
                )
                CustomInputTable(
                    reminderFormState = formState,
                    onFormStateChange = { newState -> formState = newState },
                    showed = isExpanded
                )

                UpcomingEventSection(!isExpanded)

                // + 按钮，放到后面才能让其浮在上面两个组件的上面
                if (formState.temporaryInputValue.isNotBlank()) {
                    var isLoading by remember { mutableStateOf(false) }
                    Button(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(horizontal = 16.dp),
                        colors = ButtonColors(
                            containerColor = colorScheme.secondaryContainer,
                            contentColor = colorScheme.onSecondaryContainer,
                            disabledContainerColor = Color.LightGray,
                            disabledContentColor = Color.Gray,
                        ),
                        onClick = {
                            if (isLoading) return@Button // 防止重复点击

                            scope.launch {
                                isLoading = true
                                try {
                                    withContext(Dispatchers.IO) {
                                        if (formState.selectedField.fieldName == "年事" || formState.selectedField.fieldName == "生日") {
                                            formState.isRepeatAnnuallyChecked = true
                                        }
                                        if (formState.isRepeatAnnuallyChecked) {
                                            // yearly: 写入 details_yearly
                                            MainApplication.repository.upsertYearlyDetail(
                                                YearlyDetail(
                                                    mdDate = formState.universalDate,
                                                    fieldId = formState.selectedField.fieldId,
                                                    detail = formState.inputValue
                                                )
                                            )
                                        } else {
                                            // 非 yearly: 写入 details（包含具体年份）
                                            MainApplication.repository.insertOrUpdateDetail(
                                                formState.selectedField.fieldName,
                                                formState.inputValue,
                                                formState.universalDate
                                            )
                                        }
                                    }
                                } finally {
                                    isLoading = false
                                    // 统一通过收缩来触发重置
                                    sheetState.partialExpand()
                                }
                            }
                        }) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("记录")
                    }
                } else {
                    IconButton(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(horizontal = 16.dp),
                        colors = IconButtonColors(
                            containerColor = colorScheme.secondaryContainer,
                            contentColor = colorScheme.onSecondaryContainer,
                            disabledContainerColor = Color.LightGray,
                            disabledContentColor = Color.Gray,
                        ),
                        onClick = {
                            scope.launch {
                                if (isExpanded) {
                                    sheetState.partialExpand()
                                } else {
                                    sheetState.expand()
                                }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (isExpanded) "收起" else "展开",
                            modifier = Modifier.rotate(iconRotation)
                        )
                    }
                }
            }
        }) {

        // 指标图表区域
        val metrics by remember { mutableStateOf(getFavoriteMetrics()) }
        val scrollState = rememberScrollState()

        val content = remember(metrics) {
            movableContentOf {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = colorScheme.surface)
                        .padding(horizontal = 18.dp)
                        .verticalScroll(scrollState),
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Spacer(modifier = Modifier.height(8.dp))
                    MetricsChartSection(
                        metrics = metrics,
                        navController = navController
                    )
                }
            }
        }
        content()
        Spacer(modifier = Modifier.height(24.dp))

        if (sheetState.targetValue == SheetValue.Expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)) // 使用动画化的颜色
                    .clickable(
                        enabled = true, // 只有在展开时才可点击
                        onClick = {
                            // 点击蒙版后，收起 BottomSheet
                            scope.launch {
                                scaffoldState.bottomSheetState.partialExpand() // 收回到 peekHeight
                            }
                        },
                        // 为了无障碍，可以添加 interactionSource 和 indication
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null // 我们不希望点击时有水波纹效果
                    )
            )
        }

        // 监听收缩状态以统一重置输入变量
        LaunchedEffect(sheetState.targetValue) {
            if (sheetState.targetValue == SheetValue.PartiallyExpanded) {
                // 收缩时清空/重置所有输入变量
                formState = ReminderFormState()
            }
        }
    }
}

@Composable
fun DateHeader(solar: String, weekday: String, lunar: String) {
    Row(
        modifier = Modifier.pointerInput(Unit) {
            // 双击进入后台任务管理；使用手势而非 clickable，避免任何点击水波纹等效果
            detectTapGestures(onDoubleTap = {
                MainActivity.navController.navigate("TaskControlPanel")
            })
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = solar, // 只显示“日”
            fontSize = 48.sp, fontWeight = FontWeight.Bold
        )
        Column {
            Text(
                text = weekday, style = typography.titleMedium
            )
            Text(
                text = "农历 $lunar", style = typography.bodySmall,
            )
        }
    }
}


@Composable
fun UpcomingEventSection(showed: Boolean) {
    val today = LocalDate.now()
    val nearestEvent = "西瓜变甜了"
    val daysUntilEvent = ChronoUnit.DAYS.between(today, LocalDate.of(2025, 7, 1))
    val navController = MainActivity.navController

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp)
            .alpha(if (showed) 1f else 0f)
            .then(
                if (showed) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                navController.navigate("YearlyCalendar")
                            })
                        }
                } else {
                    Modifier // 不可见时，不添加任何手势处理
                }
            )
    ) {
        Text(
            text = "下一个期待",
            style = typography.titleMedium,
            color = colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$daysUntilEvent", fontSize = 40.sp, fontWeight = FontWeight.Bold,
                // 倒计时天数是视觉焦点，使用主强调色 primary
                color = colorScheme.primary, lineHeight = 40.sp
            )
            Text(
                text = "天后",
                style = typography.headlineSmall,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Text(
            text = "是「${nearestEvent}」的日子", style = typography.bodyLarge,
            // 描述性文本，使用 onSurfaceVariant
            color = colorScheme.onSurfaceVariant
        )
    }

}

data class ReminderFormState(
    // 当前选中的类型，默认为提醒日程
    var selectedField: Field = Field(
        fieldId = 1,
        fieldName = "提醒我",
        fieldType = FIELD_TYPE_TEXT,
        groupId = 0
    ),
    // 输入框的内容
    var inputValue: String = "",
    // 临时保存的用户原始输入（用于在不同日期间流转）
    var temporaryInputValue: String = "",
    // "每年往复"复选框是否选中
    var isRepeatAnnuallyChecked: Boolean = false,
    // 日期
    var universalDate: UniversalDate = UniversalDate.today()
)

@Composable
fun CustomInputTable(
    reminderFormState: ReminderFormState,
    onFormStateChange: (ReminderFormState) -> Unit,
    showed: Boolean,
) {
    // ---- 状态管理 ----

    // 所有可用类型的列表
    val fields = remember { mutableListOf<Field>() }
    LaunchedEffect(Unit) {
        fields.addAll(MainApplication.repository.getAllFields())
    }

    val selectedField = reminderFormState.selectedField
    val inputValue = reminderFormState.inputValue
    val isRepeatAnnuallyChecked = reminderFormState.isRepeatAnnuallyChecked
    val universalDate = reminderFormState.universalDate

    // 下拉列表是否展开
    var isDropdownExpanded by remember { mutableStateOf(false) }
    // 切换类型时，重置两个输入变量
    LaunchedEffect(selectedField) {
        // 需求：仅清空临时输入，inputValue 按日期/每年往复逻辑重新计算
        onFormStateChange(
            reminderFormState.copy(
                temporaryInputValue = ""
            )
        )
    }

    // 切换日期或“每年往复”时，仅重算 inputValue，不改动 temporaryInputValue
    LaunchedEffect(universalDate, isRepeatAnnuallyChecked, selectedField) {
        val field = MainApplication.repository.getFieldByName(selectedField.fieldName)
        val detail = if (isRepeatAnnuallyChecked) {
            val mdStr = universalDate.getRawMDDate().toString()
            MainApplication.repository
                .getYearlyDetailByFieldAndMdDate(field!!.fieldId, mdStr)
                ?.detail ?: ""
        } else {
            MainApplication.repository
                .getDetailByFieldAndUniversalDate(field!!.fieldId, universalDate)
                ?.detail ?: ""
        }

        val temp = reminderFormState.temporaryInputValue
        val merged = when {
            detail.isNotEmpty() && temp.isNotEmpty() -> "$detail\n$temp"
            detail.isNotEmpty() -> detail
            else -> temp
        }

        onFormStateChange(reminderFormState.copy(inputValue = merged))
    }

    // ---- UI 布局 ----

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .alpha(if (showed) 1f else 0f)
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .heightIn(max = 600.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 左上角：低调的下拉列表
                Box {
                    // 使用 TextButton 来创建一个无边框、紧凑的点击区域
                    TextButton(
                        onClick = { isDropdownExpanded = true },
                        enabled = showed,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(selectedField.fieldName)
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "选择类型"
                        )
                    }

                    // 下拉菜单本身
                    DropdownMenu(expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }) {
                        fields.forEach { field ->
                            DropdownMenuItem(text = { Text(field.fieldName) }, onClick = {
                                onFormStateChange(
                                    reminderFormState.copy(
                                        selectedField = field,
                                        inputValue = "",
                                        temporaryInputValue = ""
                                    )
                                )
                                isDropdownExpanded = false
                            }, enabled = showed
                            )
                        }
                    }
                }
            }

            // --- 中间区域：根据类型变化的输入框 ---

            key(selectedField) {
                when (selectedField.fieldType) {
                    FIELD_TYPE_STR -> {
                        val focusRequester = FocusRequester()
                        OutlinedTextField(
                            value = inputValue,
                            onValueChange = {
                                onFormStateChange(
                                    reminderFormState.copy(
                                        inputValue = it,
                                        temporaryInputValue = it
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .focusable(showed),
                            enabled = showed,
                            placeholder = { Text("简短记录...") },
                            singleLine = true,
                        )
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    }

                    FIELD_TYPE_TEXT -> {
                        val focusRequester = FocusRequester()
                        OutlinedTextField(
                            value = inputValue,
                            onValueChange = {
                                onFormStateChange(
                                    reminderFormState.copy(
                                        inputValue = it,
                                        temporaryInputValue = it
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .focusRequester(focusRequester)
                                .focusable(showed),
                            enabled = showed,
                            placeholder = { Text("记录...") },
                        )
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    }

                    FIELD_TYPE_NUM -> {
                        val focusRequester = FocusRequester()
                        OutlinedTextField(
                            value = inputValue,
                            onValueChange = {
                                onFormStateChange(
                                    reminderFormState.copy(
                                        inputValue = it,
                                        temporaryInputValue = it
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .focusable(showed),
                            enabled = showed,
                            label = { Text("数值") },
                            placeholder = { Text("0.00") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    }

                    else -> {}
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- 底部区域："每年往复" 复选框 ---
            if (reminderFormState.selectedField.fieldName != "生日" && reminderFormState.selectedField.fieldName != "年事") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable(
                            enabled = showed,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            onFormStateChange(
                                reminderFormState.copy(
                                    isRepeatAnnuallyChecked = !isRepeatAnnuallyChecked
                                )
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp),
                        text = "在年事本上显示 * ",
                        style = typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                    )

                    Checkbox(
                        checked = isRepeatAnnuallyChecked, onCheckedChange = null, enabled = showed
                    )
                    Text(
                        text = "每年往复", modifier = Modifier.padding(start = 8.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }

            UniversalDatePicker(universalDate) {
                onFormStateChange(
                    reminderFormState.copy(
                        universalDate = it
                    )
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}


@Preview(showBackground = true)
@Composable
fun FullScreenPreview() {
    MaterialTheme {
        // 使用 Box 将主内容和 BottomSheet 放在一起
        Box(Modifier.fillMaxSize()) {
            // 你的主屏幕内容
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 将状态和作用域传入我们的组件
                // 注意：这里我们重新构造了 onClick 逻辑，使其能控制 showBottomSheet

                CustomInputTable(ReminderFormState(), {}, true)

                Text("屏幕上的其他内容...", modifier = Modifier.padding(top = 20.dp))
            }
        }
    }
}
