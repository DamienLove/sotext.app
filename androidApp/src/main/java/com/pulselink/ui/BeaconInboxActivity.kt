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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pulselink.BuildConfig
import com.pulselink.billing.SubscriptionManager
import com.pulselink.ui.ads.BannerAdSlot
import com.pulselink.ui.screens.BeaconNavBar
import com.pulselink.ui.screens.BeaconNavRoute
import com.pulselink.ui.screens.BeaconSettingsScreen
import com.pulselink.ui.screens.PrivatePinScreen
import com.pulselink.ui.screens.NewMessageScreen
import com.pulselink.ui.screens.ProfileSettingsScreen
import com.pulselink.ui.screens.SmsInboxScreen
import com.pulselink.ui.screens.SmsThreadScreen
import com.pulselink.ui.screens.VisualSettingsScreen
import com.pulselink.ui.state.MainViewModel
import com.pulselink.ui.state.SmsInboxViewModel
import com.pulselink.ui.state.SmsThreadViewModel
import com.pulselink.ui.theme.PulseLinkTheme
import com.pulselink.util.DefaultSmsHelper
import com.pulselink.util.formatTimestamp
import com.pulselink.util.hashPin
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BeaconInboxActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    @Inject lateinit var defaultSmsHelper: DefaultSmsHelper
    @Inject lateinit var subscriptionManager: SubscriptionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PulseLinkTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val subscriptionUiState by subscriptionManager.subscriptionState.collectAsStateWithLifecycle()
                var isDefaultSms by remember { mutableStateOf(defaultSmsHelper.isDefaultSms()) }
                val privateThreads = state.settings.privateThreadIds.toSet()
                var showPrivate by remember { mutableStateOf(false) }
                var showPinDialog by remember { mutableStateOf(false) }
                var pinInput by remember { mutableStateOf("") }
                val defaultSmsSupported =
                    defaultSmsHelper.buildRoleRequestIntent() != null || isDefaultSms
                val lifecycleOwner = LocalLifecycleOwner.current

                val defaultSmsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    isDefaultSms = defaultSmsHelper.isDefaultSms()
                }

                // Permission handling
                var hasSmsPermissions by remember {
                    mutableStateOf(checkSmsPermissions(context))
                }
                var requestedDefaultOnce by remember { mutableStateOf(false) }
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    hasSmsPermissions = checkSmsPermissions(context)
                }

                // Refresh default-SMS status whenever the activity resumes.
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            isDefaultSms = defaultSmsHelper.isDefaultSms()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                LaunchedEffect(isDefaultSms, hasSmsPermissions, requestedDefaultOnce) {
                    if (!isDefaultSms && !requestedDefaultOnce) {
                        // Attempt to request role once automatically
                        requestedDefaultOnce = true
                        defaultSmsHelper.buildRoleRequestIntent()?.let { intent ->
                            defaultSmsLauncher.launch(intent)
                        }
                    }
                    if (isDefaultSms && !hasSmsPermissions) {
                        permissionLauncher.launch(requiredSmsPermissions())
                    }
                }

                // Show UI even if not default, but wrap in a check or show a banner/blocking UI if critical?
                // The requirement is "should just confirm it is default sms, instead of always saying its not...".
                // If we are not default, we should probably show a UI asking to become default,
                // rather than returning early and showing nothing.

                if (!isDefaultSms) {
                    // Show "Make Default" UI
                    PulseLinkTheme {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Text(
                                    text = "Beacon Inbox requires being the default SMS app.",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                Button(onClick = {
                                    defaultSmsHelper.buildRoleRequestIntent()?.let { intent ->
                                        defaultSmsLauncher.launch(intent)
                                    }
                                }) {
                                    Text("Set as Default SMS App")
                                }
                                Button(
                                    onClick = { finish() },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Close")
                                }
                            }
                        }
                    }
                } else if (!hasSmsPermissions) {
                     // Fallback if permissions are denied but is default (unlikely but possible)
                     Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                         Text("SMS Permissions are required.")
                         Button(onClick = { permissionLauncher.launch(requiredSmsPermissions()) }) {
                             Text("Grant Permissions")
                         }
                     }
                } else {
                    val bannerHeight = 50.dp
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = if (state.showAds) bannerHeight else 0.dp)
                        ) {
                            NavHost(navController = navController, startDestination = "sms/inbox") {
                                composable("sms/inbox") {
                                    val smsInboxViewModel: SmsInboxViewModel = hiltViewModel()
                                    val threads by smsInboxViewModel.threads.collectAsStateWithLifecycle()
                                    val archivedThreads by smsInboxViewModel.archived.collectAsStateWithLifecycle()
                                    LaunchedEffect(Unit) { smsInboxViewModel.refresh() }

                                    var currentRoute by remember { mutableStateOf(BeaconNavRoute.Inbox) }

                                    val displayedThreads = when (currentRoute) {
                                        BeaconNavRoute.Inbox -> threads
                                        BeaconNavRoute.Trusted -> threads.filter { it.isTrusted }
                                        BeaconNavRoute.Favorites -> threads.filter { it.isFavorite }
                                        BeaconNavRoute.Private -> threads
                                    }

                                    val displayedArchived = when (currentRoute) {
                                        BeaconNavRoute.Inbox -> archivedThreads
                                        BeaconNavRoute.Trusted -> archivedThreads.filter { it.isTrusted }
                                        BeaconNavRoute.Favorites -> archivedThreads.filter { it.isFavorite }
                                        BeaconNavRoute.Private -> archivedThreads
                                    }

                                    SmsInboxScreen(
                                        threads = displayedThreads,
                                        archivedThreads = displayedArchived,
                                        onOpenThread = { thread ->
                                            navController.navigate("sms/thread/${thread.threadId}/${Uri.encode(thread.address)}")
                                        },
                                        onArchiveThread = { thread -> smsInboxViewModel.archive(thread.threadId) },
                                        onUnarchiveThread = { thread -> smsInboxViewModel.unarchive(thread.threadId) },
                                        onDeleteThread = { thread -> smsInboxViewModel.delete(thread.threadId) },
                                        onBack = { finish() },
                                        dateFormatter = { ts -> formatTimestamp(context, ts, state.settings.timeFormat) },
                                        isBeaconMode = true,
                                        onOpenSettings = { navController.navigate("beacon_settings") },
                                        onOpenPrivate = {},
                                        privateThreadIds = privateThreads,
                                        showPrivateOnly = currentRoute == BeaconNavRoute.Private,
                                        onTogglePrivate = { thread, makePrivate ->
                                            viewModel.setThreadPrivacy(thread.threadId, thread.address, makePrivate)
                                        },
                                        theme = state.settings.themePreferences,
                                        bottomBar = {
                                            BeaconNavBar(
                                                currentRoute = currentRoute,
                                                onNavigate = { route ->
                                                    if (route == BeaconNavRoute.Private && currentRoute != BeaconNavRoute.Private) {
                                                        if (state.settings.privatePinHash.isNullOrBlank()) {
                                                            navController.navigate("private_pin")
                                                        } else {
                                                            pinInput = ""
                                                            showPinDialog = true
                                                        }
                                                    } else {
                                                        currentRoute = route
                                                    }
                                                },
                                                theme = state.settings.themePreferences
                                            )
                                        }
                                    )

                                    if (showPrivate && currentRoute != BeaconNavRoute.Private) {
                                         currentRoute = BeaconNavRoute.Private
                                         showPrivate = false
                                    }
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
                                    val contact by threadViewModel.contact.collectAsStateWithLifecycle()
                                    LaunchedEffect(threadId) { threadViewModel.load(threadId) }
                                    SmsThreadScreen(
                                        address = Uri.decode(address),
                                        messages = messages,
                                        contact = contact,
                                        onBack = { navController.popBackStack() },
                                        dateFormatter = { ts -> formatTimestamp(context, ts, state.settings.timeFormat) },
                                        globalTheme = state.settings.themePreferences,
                                        onUpdateContactTheme = { theme -> threadViewModel.setContactTheme(theme) },
                                        onCustomizeTheme = {
                                            val contactId = contact?.id ?: -1L
                                            if (contactId != -1L) {
                                                navController.navigate("visual_settings?contactId=$contactId")
                                            }
                                        },
                                        onSendMessage = { body -> threadViewModel.sendMessage(address, body) }
                                    )
                                }
                                composable("beacon_settings") {
                                    BeaconSettingsScreen(
                                        settings = state.settings,
                                        onBack = { navController.popBackStack() },
                                        onTimeFormatChange = { viewModel.setTimeFormat(it) },
                                        onOpenVisualSettings = { navController.navigate("visual_settings") },
                                        onOpenProfileSettings = { navController.navigate("profile_settings") },
                                        isDefaultSmsApp = isDefaultSms,
                                        defaultSmsSupported = defaultSmsSupported,
                                        onRequestDefaultSms = {
                                            val intent = defaultSmsHelper.buildRoleRequestIntent()
                                            if (intent != null) {
                                                defaultSmsLauncher.launch(intent)
                                            }
                                        },
                                        remoteWebAccessEnabled = state.settings.remoteWebAccessEnabled,
                                        isPremiumActive = subscriptionUiState.isPremiumActive || state.settings.premiumUnlocked,
                                        onToggleRemoteWebAccess = { enabled -> viewModel.setRemoteWebAccess(enabled) },
                                        onSetPrivatePin = { navController.navigate("private_pin") },
                                        onPurchasePremium = { subscriptionManager.launchSubscribe(this@BeaconInboxActivity) },
                                        beaconLauncherEnabled = state.settings.beaconLauncherEnabled,
                                        onToggleBeaconLauncher = { enabled -> viewModel.setBeaconLauncherEnabled(enabled) }
                                    )
                                }
                                composable(
                                    route = "visual_settings?contactId={contactId}",
                                    arguments = listOf(navArgument("contactId") {
                                        type = NavType.LongType
                                        defaultValue = -1L
                                    })
                                ) { entry ->
                                    val contactId = entry.arguments?.getLong("contactId") ?: -1L
                                    if (contactId != -1L) {
                                        val contact = state.contacts.find { it.id == contactId }
                                        val theme = contact?.themeOverride ?: state.settings.themePreferences
                                        VisualSettingsScreen(
                                            theme = theme,
                                            onSelectTheme = { newTheme -> viewModel.updateContactTheme(contactId, newTheme) },
                                            onBack = { navController.popBackStack() },
                                            isGlobal = false
                                        )
                                    } else {
                                        VisualSettingsScreen(
                                            theme = state.settings.themePreferences,
                                            onSelectTheme = { newTheme -> viewModel.setThemePreferences(newTheme) },
                                            onBack = { navController.popBackStack() },
                                            isGlobal = true
                                        )
                                    }
                                }
                                composable("profile_settings") {
                                    ProfileSettingsScreen(
                                        settings = state.settings,
                                        onSaveName = { viewModel.setOwnerName(it) },
                                        onSaveAvatar = { viewModel.setOwnerAvatarUrl(it) },
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("private_pin") {
                                    PrivatePinScreen(
                                        hasPin = state.settings.privatePinHash != null,
                                        onSavePin = { newPin ->
                                            val hashed = newPin?.let { hashPin(it) }
                                            viewModel.setPrivatePinHash(hashed)
                                            navController.popBackStack()
                                        },
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }

                        BannerAdSlot(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .navigationBarsPadding(),
                            enabled = state.showAds
                        )
                    }
                    if (showPinDialog) {
                        AlertDialog(
                            onDismissRequest = { showPinDialog = false },
                            title = { Text("Enter private PIN") },
                            text = {
                                OutlinedTextField(
                                    value = pinInput,
                                    onValueChange = { pinInput = it },
                                    label = { Text("PIN") },
                                    visualTransformation = PasswordVisualTransformation()
                                )
                            },
                            confirmButton = {
                                Button(onClick = {
                                    val hashed = hashPin(pinInput)
                                    if (hashed == state.settings.privatePinHash) {
                                        showPinDialog = false
                                        showPrivate = true
                                    } else {
                                        pinInput = ""
                                    }
                                }) {
                                    Text("Unlock")
                                }
                            },
                            dismissButton = {
                                Button(onClick = { showPinDialog = false }) { Text("Cancel") }
                            }
                        )
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
        if (BuildConfig.PRO_FEATURES) {
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
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                emptyArray()
            }
        }
}
