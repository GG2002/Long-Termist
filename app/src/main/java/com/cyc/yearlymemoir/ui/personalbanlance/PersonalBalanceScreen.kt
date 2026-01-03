package com.cyc.yearlymemoir.ui.personalbanlance

import AutoCollectBalanceCard
import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import com.cyc.yearlymemoir.MainActivity
import com.cyc.yearlymemoir.MainApplication
import com.cyc.yearlymemoir.WorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class PersonalBalanceViewModel(application: Application) : AndroidViewModel(application) {
    private val model = MainApplication.repository

    private val _allBalance = MutableStateFlow(0.0)
    val allBalance: StateFlow<Double> = _allBalance

    private val _lastUpdated = MutableStateFlow(0.0)
    val lastUpdated = _lastUpdated

    // 是否开启定时任务
    private val _isScheduled = MutableStateFlow(false)
    val isScheduled: StateFlow<Boolean> = _isScheduled

    @SuppressLint("RestrictedApi")
    suspend fun loadIsScheduled() {
        val workManager = WorkManager.getInstance(application.applicationContext)
        val workInfos =
            workManager.getWorkInfosForUniqueWork(WorkScheduler.UNIQUE_WORK_NAME).await()
        _isScheduled.value =
            workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
    }

    @SuppressLint("RestrictedApi")
    fun setDailyTaskEnabled(enabled: Boolean) {
        if (enabled) {
            WorkScheduler.enableGetPersonalBalance8amWork(application.applicationContext)
        } else {
            WorkScheduler.cancelGetPersonalBalance8amWork(application.applicationContext)
        }
        _isScheduled.value = enabled
    }

    fun computeAllBalance() {
        viewModelScope.launch {
            _allBalance.value = 114514.19
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun PersonalBalanceScreen(
    viewModel: PersonalBalanceViewModel = viewModel()
) {
    val allBalance by viewModel.allBalance.collectAsStateWithLifecycle()
    val lastUpdate by viewModel.lastUpdated.collectAsStateWithLifecycle()
    val isScheduled by viewModel.isScheduled.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadIsScheduled()
        viewModel.computeAllBalance()
    }

    Scaffold(
        modifier = Modifier,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.primaryContainer,
                    titleContentColor = colorScheme.onPrimaryContainer,
                ),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val formattedTime =
                            SimpleDateFormat("MM 月 dd 日 HH:mm", Locale.getDefault())
                                .format(Date(lastUpdate.toLong()))
                        Column {
                            Text(
                                "总余额：${allBalance}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            )
                            Text(
                                "最后更新：$formattedTime",
                                style = typography.labelSmall
                            )

                        }
                        Column(
                            modifier = Modifier.padding(end = 18.dp),
                            verticalArrangement = Arrangement.spacedBy((-8).dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "定时获取",
                                style = typography.labelSmall
                            )
                            Switch(
//                                modifier = Modifier.padding(bottom = 8.dp),
                                checked = isScheduled,
                                onCheckedChange = { enabled ->
                                    viewModel.setDailyTaskEnabled(enabled)
//                                        if (enabled) {
//                                            WorkScheduler.scheduleNowForTest(context)
//                                        }
                                }
                            )
                            Text(
                                "1440 min",
                                style = typography.labelSmall
                            )
                        }
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    start = 18.dp,
                    end = 18.dp
                )
        ) {
            Spacer(Modifier.height(10.dp))

            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                    BanlanceChartCard()
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            AutoCollectBalanceCard {
                viewModel.computeAllBalance()
            }
        }
    }
}

