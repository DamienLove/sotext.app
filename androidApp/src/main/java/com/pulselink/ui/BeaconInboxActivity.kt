package com.pulselink.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pulselink.ui.screens.BeaconSettingsScreen
import com.pulselink.ui.screens.SmsInboxScreen
import com.pulselink.ui.screens.SmsThreadScreen
import com.pulselink.ui.screens.VisualSettingsScreen
import com.pulselink.ui.state.MainViewModel
import com.pulselink.ui.state.SmsInboxViewModel
import com.pulselink.ui.state.SmsThreadViewModel
import com.pulselink.ui.theme.PulseLinkTheme
import com.pulselink.util.formatTimestamp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BeaconInboxActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PulseLinkTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                // Permission handling
                var hasSmsPermissions by remember {
                    mutableStateOf(checkSmsPermissions(context))
                }
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    hasSmsPermissions = checkSmsPermissions(context)
                }

                LaunchedEffect(Unit) {
                    if (!hasSmsPermissions) {
                        permissionLauncher.launch(requiredSmsPermissions())
                    }
                }

                if (hasSmsPermissions) {
                    NavHost(navController = navController, startDestination = "sms/inbox") {
                        composable("sms/inbox") {
                            val smsInboxViewModel: SmsInboxViewModel = hiltViewModel()
                            val threads by smsInboxViewModel.threads.collectAsStateWithLifecycle()
                            val archivedThreads by smsInboxViewModel.archived.collectAsStateWithLifecycle()
                            LaunchedEffect(Unit) { smsInboxViewModel.refresh() }
                            SmsInboxScreen(
                                threads = threads,
                                archivedThreads = archivedThreads,
                                onOpenThread = { thread ->
                                    navController.navigate("sms/thread/${thread.threadId}/${Uri.encode(thread.address)}")
                                },
                                onArchiveThread = { thread -> smsInboxViewModel.archive(thread.threadId) },
                                onUnarchiveThread = { thread -> smsInboxViewModel.unarchive(thread.threadId) },
                                onDeleteThread = { thread -> smsInboxViewModel.delete(thread.threadId) },
                                onBack = { finish() },
                                dateFormatter = { ts -> formatTimestamp(context, ts, state.settings.timeFormat) },
                                isBeaconMode = true,
                                onOpenSettings = { navController.navigate("beacon_settings") }
                            )
                        }
                        composable(
                            route = "sms/thread/{threadId}/{address}",
                            arguments = listOf(
                                navArgument("threadId") { type = NavType.LongType },
                                navArgument("address") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val threadId = entry.arguments?.getLong("threadId") ?: return@composable
                            val address = entry.arguments?.getString("address") ?: ""
                            val threadViewModel: SmsThreadViewModel = hiltViewModel()
                            val messages by threadViewModel.messages.collectAsStateWithLifecycle()
                            LaunchedEffect(threadId) { threadViewModel.load(threadId) }
                            SmsThreadScreen(
                                address = Uri.decode(address),
                                messages = messages,
                                onBack = { navController.popBackStack() },
                                dateFormatter = { ts -> formatTimestamp(context, ts, state.settings.timeFormat) },
                                themePreferences = state.settings.themePreferences
                            )
                        }
                        composable("beacon_settings") {
                            BeaconSettingsScreen(
                                settings = state.settings,
                                onBack = { navController.popBackStack() },
                                onTimeFormatChange = { viewModel.setTimeFormat(it) },
                                onOpenVisualSettings = { navController.navigate("visual_settings") }
                            )
                        }
                        composable("visual_settings") {
                            VisualSettingsScreen(
                                theme = state.settings.themePreferences,
                                onSelectTheme = { newTheme -> viewModel.setThemePreferences(newTheme) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkSmsPermissions(context: android.content.Context): Boolean {
        return requiredSmsPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requiredSmsPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.RECEIVE_MMS,
                Manifest.permission.RECEIVE_WAP_PUSH,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.RECEIVE_MMS,
                Manifest.permission.RECEIVE_WAP_PUSH
            )
        }
}
