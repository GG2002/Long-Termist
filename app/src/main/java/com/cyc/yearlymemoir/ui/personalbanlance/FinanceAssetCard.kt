package com.cyc.yearlymemoir.ui.personalbanlance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyc.yearlymemoir.MainActivity
import com.cyc.yearlymemoir.MainApplication
import com.cyc.yearlymemoir.data.local.dao.LatestAssetAmount
import com.cyc.yearlymemoir.data.local.db.TmpFinanceDataBase
import ir.ehsannarmani.compose_charts.extensions.format
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class FinanceAssetUiState(
    val totalAmount: Double = 0.0,
    val items: List<LatestAssetAmount> = emptyList()
)

class FinanceAssetCardViewModel : ViewModel() {
    private val dao = TmpFinanceDataBase.get(MainApplication.instance).financeAssetDao()

    private val _state = MutableStateFlow(FinanceAssetUiState())
    val state: StateFlow<FinanceAssetUiState> = _state

    fun load() {
        viewModelScope.launch {
            val items = runCatching { dao.getLatestPerAsset() }.getOrElse { emptyList() }
            val total = runCatching { dao.getLatestTotalAmount() }.getOrElse { 0.0 }
            _state.value = FinanceAssetUiState(totalAmount = total, items = items)
        }
    }
}

@Composable
fun FinanceAssetCard(
    refreshTick: Int,
    viewModel: FinanceAssetCardViewModel = viewModel()
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(refreshTick) { viewModel.load() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                MainActivity.navController.navigate("FinanceAssetList")
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("金融资产", style = typography.titleSmall)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "合计",
                        style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        ui.totalAmount.format(2),
                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            if (ui.items.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "暂无资产记录",
                        style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant)
                    )
                }
            } else {
                // Show all assets (latest amounts per asset)
                ui.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.asset_name, style = typography.bodyMedium)
                        Text(
                            item.asset_amount.format(2),
                            style = typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}