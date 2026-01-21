package com.cyc.yearlymemoir.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ven.assists.AssistsCore

/**
 * 定义一个数据类来封装每项权限的信息
 * @param title 权限标题
 * @param description 权限用途的详细描述
 * @param isGranted 一个返回当前权限是否被授予的函数
 * @param onRequest 一个当用户点击申请时执行的函数
 */
private data class PermissionInfo(
    val title: String,
    val description: String,
    val isGranted: (Context) -> Boolean,
    val onRequest: () -> Unit
)

@SuppressLint("ServiceCast", "BatteryLife")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 使用 rememberLauncher 来处理 Activity 的返回结果，这里我们只关心返回后刷新状态
    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // 当从设置页面返回时，所有状态都会在 ON_RESUME 中刷新，这里无需做任何事
    }

    // 专门为运行时权限设计的 launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        // 当从权限请求对话框返回时，所有状态都会在 ON_RESUME 中刷新
    }

    // 1. 定义所有需要检查和申请的权限
    val permissionActions = remember {
        listOf(
            PermissionInfo(
                title = "通知权限",
                description = "用于在通知栏提醒用户日程安排。",
                isGranted = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val b = ContextCompat.checkSelfPermission(
                            it,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        b
                    } else {
                        true // Android 13 以下版本无需申请
                    }
                },
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            ),
            PermissionInfo(
                title = "悬浮窗权限",
                description = "用于在其他应用上层显示悬浮窗口，例如实时显示重要信息或提供快捷操作。",
                isGranted = { Settings.canDrawOverlays(it) },
                onRequest = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    settingsLauncher.launch(intent)
                }
            ),
            PermissionInfo(
                title = "无障碍服务",
                description = "核心功能依赖，用于自动化操作、监听特定事件，为您提供便利。",
                isGranted = { AssistsCore.isAccessibilityServiceEnabled() },
                onRequest = { AssistsCore.openAccessibilitySetting() }
            ),
            PermissionInfo(
                title = "电池优化白名单",
                description = "将应用加入电池优化白名单，以确保应用在后台也能稳定运行，避免被系统强制关闭。",
                isGranted = {
                    val powerManager = it.getSystemService(Context.POWER_SERVICE) as PowerManager
                    powerManager.isIgnoringBatteryOptimizations(it.packageName)
                },
                onRequest = {
                    val intent =
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    settingsLauncher.launch(intent)
                }
            ),
            PermissionInfo(
                title = "文件管理权限",
                description = "允许应用访问和管理设备上的所有文件，用于数据备份、恢复和管理。",
                isGranted = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        // Android 11+ 使用新 API
                        Environment.isExternalStorageManager()
                    } else
                    // Android 6.0-10 检查读写权限
                        checkReadWritePermissions(context)
                },
                onRequest = {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:${context.applicationContext.packageName}")
                        settingsLauncher.launch(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        settingsLauncher.launch(intent)
                    }
                }
            )
        )
    }

    // 2. 为每个权限创建一个状态，用于驱动UI更新
    val permissionStates = permissionActions.associate {
        it.title to remember { mutableStateOf(it.isGranted(context)) }
    }

    // 3. 使用 LifecycleEventObserver 在界面回到前台时刷新所有权限状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionActions.forEach { action ->
                    permissionStates[action.title]?.value = action.isGranted(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 4. UI 布局
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置中心") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.primaryContainer,
                    titleContentColor = colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
        ) {
            Text(
                "为了保证应用的正常运行，请授予以下必要权限：",
                style = typography.bodyLarge
            )
            Spacer(Modifier.height(6.dp))
            Column(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
            ) {
                permissionActions.forEach { permission ->
                    val isGranted = permissionStates[permission.title]?.value ?: false
                    PermissionItem(
                        title = permission.title,
                        description = permission.description,
                        isGranted = isGranted,
                        onRequest = permission.onRequest
                    )
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = typography.titleSmall
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val labelColor = if (isGranted) Color(0xFF4CAF50) else Color(0xFFF44336)
                    Row(
                        Modifier
                            .height(16.dp)
                            .clip(shape = RoundedCornerShape(8.dp))
                            .background(color = labelColor),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = "Status Icon",
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = if (isGranted) "已授予" else "未授予",
                            color = colorScheme.onSurface,
                            style = typography.labelSmall.copy(
                                fontSize = 8.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, style = typography.bodySmall)
            }
            if (!isGranted) {
                Spacer(modifier = Modifier.width(6.dp))
                TextButton(
                    border = BorderStroke(1.dp, color = colorScheme.primary.copy(0.5f)),
                    onClick = onRequest
                ) {
                    Text("授予", style = typography.bodySmall)
                }
            }
        }
    }
}

private fun checkReadWritePermissions(context: Context): Boolean {
    val writePermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    val readPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    return writePermission == PackageManager.PERMISSION_GRANTED &&
            readPermission == PackageManager.PERMISSION_GRANTED
}

@Preview(showBackground = true)
@Composable
fun PermissionScreenPreview() {
    MaterialTheme {
        // 这是一个纯UI预览，不包含真实逻辑
        // 你可以在这里模拟各种权限状态来测试UI
        PermissionScreen()
    }
}