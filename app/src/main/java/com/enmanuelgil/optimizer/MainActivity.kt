package com.enmanuelgil.optimizer

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.enmanuelgil.optimizer.service.ThermalMonitorService
import com.enmanuelgil.optimizer.ui.screens.*
import com.enmanuelgil.optimizer.ui.theme.*
import com.enmanuelgil.optimizer.viewmodel.MainViewModel

class OptimizerApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThermalMonitorService.start(this)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        setContent {
            PhoneOptimizerTheme {
                PhoneOptimizerApp(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) viewModel.checkPrivileges()
    }
}

@Composable
fun PhoneOptimizerApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val isOptimizing by viewModel.isOptimizing.collectAsStateWithLifecycle()
    val progress by viewModel.optimizationProgress.collectAsStateWithLifecycle()
    val lastResult by viewModel.lastResult.collectAsStateWithLifecycle()
    val selectedProfile by viewModel.selectedProfile.collectAsStateWithLifecycle()
    val privilegesStatus by viewModel.privilegesStatus.collectAsStateWithLifecycle()

    var currentTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        NavTab("Panel", Icons.Default.Dashboard),
        NavTab("Optimizar", Icons.Default.FlashOn),
        NavTab("Ajustes", Icons.Default.Settings)
    )

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = {
            NavigationBar(containerColor = SurfaceDark, tonalElevation = 0.dp) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = currentTab == index,
                        onClick = { currentTab = index },
                        icon = {
                            Icon(
                                tab.icon, contentDescription = tab.label,
                                tint = if (currentTab == index) PrimaryBlue else TextSecondary
                            )
                        },
                        label = {
                            Text(
                                tab.label,
                                color = if (currentTab == index) PrimaryBlue else TextSecondary,
                                fontWeight = if (currentTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = PrimaryBlue.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(paddingValues)
        ) {
            when (currentTab) {
                0 -> DashboardScreen(stats)
                1 -> OptimizeScreen(
                    selectedProfile = selectedProfile,
                    onProfileSelected = viewModel::setProfile,
                    isOptimizing = isOptimizing,
                    progress = progress,
                    lastResult = lastResult,
                    privilegesStatus = privilegesStatus,
                    onOptimize = viewModel::optimize
                )
                2 -> SettingsScreen(
                    privilegesStatus = privilegesStatus,
                    onStartMonitor = { ThermalMonitorService.start(context) },
                    onStopMonitor = { ThermalMonitorService.stop(context) }
                )
            }
        }
    }
}

data class NavTab(val label: String, val icon: ImageVector)
