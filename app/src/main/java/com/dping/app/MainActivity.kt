package com.dping.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dping.app.ui.components.DPingBottomNavigationBar
import com.dping.app.ui.components.DPingTopBar
import com.dping.app.ui.components.MainTab
import com.dping.app.ui.components.SegmentedTabBar
import com.dping.app.ui.i18n.AppStringsProvider
import com.dping.app.ui.i18n.LocalAppStrings
import com.dping.app.ui.screens.*
import com.dping.app.ui.theme.*
import com.dping.app.ui.viewmodels.*
import com.dping.app.utils.LocaleHelper
import kotlinx.coroutines.launch

import androidx.activity.compose.BackHandler

enum class AppDestination {
    HOME,
    PING,
    TRACEROUTE,
    SPEED_TEST,
    LAN_DISCOVERY,
    IP_RANGE,
    MY_IP,
    TOOLS,
    DNS_LOOKUP,
    NETWORK_INTERFACES,
    HISTORY,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        val app = application as DPingApp

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel {
                SettingsViewModel(app.preferences)
            }
            val settingsState by settingsViewModel.uiState.collectAsState()

            val currentStrings = remember(settingsState.selectedLanguage) {
                AppStringsProvider.getStrings(settingsState.selectedLanguage)
            }

            LaunchedEffect(settingsState.selectedLanguage) {
                LocaleHelper.applyLocale(this@MainActivity, settingsState.selectedLanguage)
            }

            CompositionLocalProvider(LocalAppStrings provides currentStrings) {
                DPingTheme(themeMode = settingsState.themeMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        var showSplash by remember { mutableStateOf(true) }

                        val pingViewModel: PingViewModel = viewModel {
                            PingViewModel(app.repository)
                        }
                        val historyViewModel: HistoryViewModel = viewModel {
                            HistoryViewModel(app.repository)
                        }
                        val myIpViewModel: MyIpViewModel = viewModel {
                            MyIpViewModel(app.applicationContext)
                        }
                        val ipRangeViewModel: IpRangeViewModel = viewModel {
                            IpRangeViewModel(app.applicationContext)
                        }
                        val toolsViewModel: ToolsViewModel = viewModel {
                            ToolsViewModel(app.applicationContext)
                        }
                        val speedTestViewModel: SpeedTestViewModel = viewModel {
                            SpeedTestViewModel(app.repository)
                        }
                        val lanDiscoveryViewModel: LanDiscoveryViewModel = viewModel {
                            LanDiscoveryViewModel(app.applicationContext)
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            MainAppScaffold(
                                pingViewModel = pingViewModel,
                                historyViewModel = historyViewModel,
                                myIpViewModel = myIpViewModel,
                                ipRangeViewModel = ipRangeViewModel,
                                toolsViewModel = toolsViewModel,
                                speedTestViewModel = speedTestViewModel,
                                lanDiscoveryViewModel = lanDiscoveryViewModel,
                                settingsViewModel = settingsViewModel
                            )

                            AnimatedVisibility(
                                visible = showSplash,
                                enter = fadeIn(),
                                exit = fadeOut(animationSpec = tween(500))
                            ) {
                                SplashScreen(onTimeout = { showSplash = false })
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    pingViewModel: PingViewModel,
    historyViewModel: HistoryViewModel,
    myIpViewModel: MyIpViewModel,
    ipRangeViewModel: IpRangeViewModel,
    toolsViewModel: ToolsViewModel,
    speedTestViewModel: SpeedTestViewModel,
    lanDiscoveryViewModel: LanDiscoveryViewModel,
    settingsViewModel: SettingsViewModel
) {
    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }
    val strings = LocalAppStrings.current

    // Android Hardware / Gesture Back Button handling
    BackHandler(enabled = currentDestination != AppDestination.HOME) {
        currentDestination = AppDestination.HOME
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentDestination) {
            AppDestination.HOME -> {
                HomeScreen(
                    onNavigateToPing = { currentDestination = AppDestination.PING },
                    onNavigateToTraceroute = { currentDestination = AppDestination.TRACEROUTE },
                    onNavigateToSpeedTest = { currentDestination = AppDestination.SPEED_TEST },
                    onNavigateToLanDiscovery = { currentDestination = AppDestination.LAN_DISCOVERY },
                    onNavigateToIpRange = { currentDestination = AppDestination.IP_RANGE },
                    onNavigateToMyIp = { currentDestination = AppDestination.MY_IP },
                    onNavigateToTools = { currentDestination = AppDestination.TOOLS },
                    onNavigateToHistory = { currentDestination = AppDestination.HISTORY },
                    onNavigateToSettings = { currentDestination = AppDestination.SETTINGS },
                    onOpenDrawer = { currentDestination = AppDestination.SETTINGS }
                )
            }

            AppDestination.PING -> {
                Scaffold(
                    topBar = {
                        DPingTopBar(
                            title = "D Ping",
                            onMenuClick = { currentDestination = AppDestination.HOME },
                            onSettingsClick = { currentDestination = AppDestination.SETTINGS }
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    PingScreen(
                        viewModel = pingViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }

            AppDestination.TRACEROUTE -> {
                TracerouteScreen(
                    viewModel = pingViewModel,
                    onBackClick = { currentDestination = AppDestination.HOME },
                    onOpenDrawer = { currentDestination = AppDestination.HOME }
                )
            }

            AppDestination.SETTINGS -> {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBackClick = { currentDestination = AppDestination.HOME }
                )
            }

            AppDestination.SPEED_TEST -> {
                SpeedTestScreen(
                    viewModel = speedTestViewModel,
                    onBackClick = { currentDestination = AppDestination.HOME },
                    onOpenDrawer = { currentDestination = AppDestination.HOME }
                )
            }

            AppDestination.IP_RANGE -> {
                IpRangeScreen(
                    viewModel = ipRangeViewModel,
                    onBackClick = { currentDestination = AppDestination.HOME },
                    onOpenDrawer = { currentDestination = AppDestination.HOME },
                    onNavigateToPing = { targetIp ->
                        pingViewModel.onTargetChange(targetIp)
                        currentDestination = AppDestination.PING
                    }
                )
            }

            AppDestination.LAN_DISCOVERY -> {
                LanDiscoveryScreen(
                    viewModel = lanDiscoveryViewModel,
                    onBackClick = { currentDestination = AppDestination.HOME },
                    onOpenDrawer = { currentDestination = AppDestination.HOME },
                    onNavigateToPing = { targetIp ->
                        pingViewModel.onTargetChange(targetIp)
                        currentDestination = AppDestination.PING
                    }
                )
            }

            AppDestination.MY_IP -> {
                MyIpScreen(
                    viewModel = myIpViewModel,
                    onBackClick = { currentDestination = AppDestination.HOME },
                    onOpenDrawer = { currentDestination = AppDestination.HOME }
                )
            }

            AppDestination.TOOLS -> {
                ToolsScreen(
                    viewModel = toolsViewModel,
                    onBackClick = { currentDestination = AppDestination.HOME },
                    onOpenDrawer = { currentDestination = AppDestination.HOME },
                    onNavigateToIpRange = { currentDestination = AppDestination.IP_RANGE },
                    onNavigateToSpeedTest = { currentDestination = AppDestination.SPEED_TEST },
                    onNavigateToLanDiscovery = { currentDestination = AppDestination.LAN_DISCOVERY },
                    onNavigateToDnsLookup = { currentDestination = AppDestination.DNS_LOOKUP },
                    onNavigateToNetworkInterfaces = { currentDestination = AppDestination.NETWORK_INTERFACES }
                )
            }

            AppDestination.DNS_LOOKUP -> {
                DnsLookupScreen(
                    viewModel = toolsViewModel,
                    onBackClick = { currentDestination = AppDestination.TOOLS },
                    onOpenDrawer = { currentDestination = AppDestination.HOME }
                )
            }

            AppDestination.NETWORK_INTERFACES -> {
                NetworkInterfacesScreen(
                    viewModel = toolsViewModel,
                    onBackClick = { currentDestination = AppDestination.TOOLS },
                    onOpenDrawer = { currentDestination = AppDestination.HOME }
                )
            }

            AppDestination.HISTORY -> {
                HistoryScreen(
                    viewModel = historyViewModel,
                    onBackClick = { currentDestination = AppDestination.HOME },
                    onOpenDrawer = { currentDestination = AppDestination.HOME }
                )
            }
        }
    }
}
