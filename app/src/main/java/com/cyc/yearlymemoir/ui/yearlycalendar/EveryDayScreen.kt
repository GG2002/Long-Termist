package com.cyc.yearlymemoir.ui.yearlycalendar

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

// 主界面入口
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun EveryDayScreen(
    animatedVisibilityScope: AnimatedContentScope,
    sharedTransitionScope: SharedTransitionScope,
    navController: NavController = rememberNavController(),
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // 使用 colorScheme.background 作为屏幕背景色
        containerColor = colorScheme.background,
    ) { innerPadding ->
        EveryDayCard(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
            animatedVisibilityScope = animatedVisibilityScope,
            sharedTransitionScope = sharedTransitionScope,
            navController = navController
        )
    }
}