package com.example.hydro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.hydro.ui.theme.HydroTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = com.example.hydro.data.AppDatabase.getInstance(applicationContext)
        val homeViewModel = ViewModelProvider(
            this,
            HomeViewModel.Factory(
                waterRecordDao = database.waterRecordDao(),
                settingsRepository = com.example.hydro.data.SettingsRepository(applicationContext)
            )
        )[HomeViewModel::class.java]
        setContent {
            HydroTheme {
                HydroApp(homeViewModel)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HydroApp(homeViewModel: HomeViewModel) {
    val currentMl by homeViewModel.todayWaterMl.collectAsStateWithLifecycle()
    val canUndoWater by homeViewModel.canUndoWater.collectAsStateWithLifecycle()
    val allWaterRecords by homeViewModel.allWaterRecords.collectAsStateWithLifecycle()
    val dailyGoalMl by homeViewModel.dailyGoalMl.collectAsStateWithLifecycle()
    val selectedDestination = rememberSaveable { mutableIntStateOf(0) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val isHistorySelected = selectedDestination.intValue == 1
    val isSettingsSelected = selectedDestination.intValue == 2
    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeViewModel.refreshTodayIfDateChanged()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (showAbout) {
                TopAppBar(
                    title = { Text(text = "关于 Hydro") },
                    navigationIcon = {
                        IconButton(onClick = { showAbout = false }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回设置"
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = when {
                                isHistorySelected -> "历史"
                                isSettingsSelected -> "设置"
                                else -> "Hydro"
                            }
                        )
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = !isHistorySelected && !isSettingsSelected,
                    onClick = {
                        selectedDestination.intValue = 0
                        showAbout = false
                    },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "首页") },
                    label = { Text("首页") }
                )
                NavigationBarItem(
                    selected = isHistorySelected,
                    onClick = {
                        selectedDestination.intValue = 1
                        showAbout = false
                    },
                    icon = { Icon(Icons.Filled.History, contentDescription = "历史") },
                    label = { Text("历史") }
                )
                NavigationBarItem(
                    selected = isSettingsSelected,
                    onClick = {
                        selectedDestination.intValue = 2
                        showAbout = false
                    },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "设置") },
                    label = { Text("设置") }
                )
            }
        }
    ) { innerPadding ->
        if (showAbout) {
            AboutScreen(
                onOpenGithub = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))
                    )
                },
                modifier = Modifier.padding(innerPadding)
            )
        } else if (isHistorySelected) {
            HistoryScreen(
                records = allWaterRecords,
                modifier = Modifier.padding(innerPadding)
            )
        } else if (isSettingsSelected) {
            SettingsScreen(
                dailyGoalMl = dailyGoalMl,
                onDailyGoalChanged = homeViewModel::updateDailyGoal,
                onAboutClick = { showAbout = true },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            HomeScreen(
                currentMl = currentMl,
                goalMl = dailyGoalMl,
                canUndoWater = canUndoWater,
                onAddWater = homeViewModel::addWater,
                onUndoLastWater = homeViewModel::undoLastWater,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun HomeScreen(
    currentMl: Int,
    goalMl: Int,
    canUndoWater: Boolean,
    onAddWater: (Int) -> Unit,
    onUndoLastWater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (currentMl.toFloat() / goalMl).coerceIn(0f, 1f)
    val percentage = (currentMl.toFloat() / goalMl * 100)
        .roundToInt()
        .coerceAtMost(100)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "今日饮水",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "保持规律饮水，照顾好自己。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "今日进度",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                    Text(
                        text = currentMl.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "/ $goalMl ml",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    trackColor = MaterialTheme.colorScheme.surface
                )
                Text(
                    text = "已完成 $percentage%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Button(
            onClick = { onAddWater(250) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text(text = "+ 250 ml", fontSize = 16.sp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "快速记录",
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("150 ml", "250 ml", "500 ml").forEach { amount ->
                    OutlinedButton(
                        onClick = {
                            onAddWater(amount.removeSuffix(" ml").toInt())
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(text = amount)
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onUndoLastWater,
            enabled = canUndoWater,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "撤回上一次", fontSize = 16.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HydroTheme {
        HomeScreen(
            currentMl = 0,
            goalMl = 2000,
            canUndoWater = false,
            onAddWater = {},
            onUndoLastWater = {}
        )
    }
}
