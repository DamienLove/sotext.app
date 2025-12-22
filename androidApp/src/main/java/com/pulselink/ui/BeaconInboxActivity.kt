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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.pulselink.util.parseColorOr
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
                val scope = rememberCoroutineScope()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val subscriptionUiState by subscriptionManager.subscriptionState.collectAsStateWithLifecycle()
                var isDefaultSms by remember { mutableStateOf(defaultSmsHelper.isDefaultSms()) }
                var isCheckingDefaultSms by remember { mutableStateOf(false) }
                val privateThreads = state.settings.privateThreadIds.toSet()
                var showPrivate by remember { mutableStateOf(false) }
                var showPinDialog by remember { mutableStateOf(false) }
                var pinInput by remember { mutableStateOf("") }
                val defaultSmsSupported =
                    defaultSmsHelper.buildRoleRequestIntent() != null || isDefaultSms
                val lifecycleOwner = LocalLifecycleOwner.current
                var defaultSmsCheckJob by remember { mutableStateOf<Job?>(null) }
                val launchDefaultSmsCheck: () -> Unit = launchDefaultSmsCheck@{
                    if (defaultSmsCheckJob?.isActive == true) return@launchDefaultSmsCheck
                    isCheckingDefaultSms = true
                    defaultSmsCheckJob = scope.launch {
                        try {
                            val latest = defaultSmsHelper.checkDefaultSmsWithRetry()
                            isDefaultSms = latest
                        } finally {
                            isCheckingDefaultSms = false
                            defaultSmsCheckJob = null
                        }
                    }
                }

                val defaultSmsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    launchDefaultSmsCheck()
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
                            launchDefaultSmsCheck()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                LaunchedEffect(isDefaultSms, hasSmsPermissions, requestedDefaultOnce) {
                    if (!isDefaultSms && !requestedDefaultOnce && !isCheckingDefaultSms) {
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

                Column(modifier = Modifier.fillMaxSize()) {
                    val bannerHeight = 50.dp
                    Box(modifier = Modifier.weight(1f)) {
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
                                    val searchState by smsInboxViewModel.searchState.collectAsStateWithLifecycle()
                                    LaunchedEffect(Unit) { smsInboxViewModel.refresh() }
                                    LaunchedEffect(isDefaultSms, hasSmsPermissions) {
                                        if (isDefaultSms || hasSmsPermissions) {
                                            smsInboxViewModel.refresh()
                                        }
                                    }

                                    var currentRoute by remember { mutableStateOf(BeaconNavRoute.Inbox) }

                                    val displayedThreads = when (currentRoute) {
                                        BeaconNavRoute.Inbox -> threads
                                        BeaconNavRoute.Otp -> threads.filter { it.isOtp }
                                        BeaconNavRoute.Trusted -> threads.filter { it.isTrusted }
                                        BeaconNavRoute.Favorites -> threads.filter { it.isFavorite }
                                        BeaconNavRoute.Private -> threads
                                    }

                                    val displayedArchived = when (currentRoute) {
                                        BeaconNavRoute.Inbox -> archivedThreads
                                        BeaconNavRoute.Otp -> archivedThreads.filter { it.isOtp }
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
                                        onOpenThreadById = { threadId, address ->
                                            navController.navigate("sms/thread/$threadId/${Uri.encode(address)}")
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
                                        sectionTitle = when (currentRoute) {
                                            BeaconNavRoute.Inbox -> "All messages"
                                            BeaconNavRoute.Otp -> "2-step codes"
                                            BeaconNavRoute.Trusted -> "Trusted contacts"
                                            BeaconNavRoute.Favorites -> "Favorites"
                                            BeaconNavRoute.Private -> "Private"
                                        },
                                        showFilterTabs = currentRoute == BeaconNavRoute.Inbox,
                                        showSearchBar = currentRoute == BeaconNavRoute.Inbox,
                                        searchState = searchState,
                                        onSearch = { smsInboxViewModel.search(it) },
                                        onClearSearch = { smsInboxViewModel.clearSearch() },
                                        banner = {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                if (!hasSmsPermissions) {
                                                    Surface(
                                                        tonalElevation = 2.dp,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(12.dp),
                                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(Icons.Filled.Settings, contentDescription = null)
                                                            Column(Modifier.weight(1f)) {
                                                                Text(
                                                                    text = "Permissions needed",
                                                                    style = MaterialTheme.typography.titleSmall
                                                                )
                                                                Text(
                                                                    text = "Grant SMS + notifications so Beacon can read and show messages.",
                                                                    style = MaterialTheme.typography.bodySmall
                                                                )
                                                            }
                                                            OutlinedButton(onClick = {
                                                                permissionLauncher.launch(requiredSmsPermissions())
                                                            }) {
                                                                Text("Grant")
                                                            }
                                                        }
                                                    }
                                                }
                                                if (!isDefaultSms || isCheckingDefaultSms) {
                                                    Surface(
                                                        tonalElevation = 2.dp,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(12.dp),
                                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(Icons.Filled.Sms, contentDescription = null)
                                                            Column(Modifier.weight(1f)) {
                                                                Text(
                                                                    text = if (isCheckingDefaultSms) {
                                                                        "Checking default SMS status..."
                                                                    } else {
                                                                        "Set as default SMS"
                                                                    },
                                                                    style = MaterialTheme.typography.titleSmall
                                                                )
                                                                Text(
                                                                    text = if (defaultSmsSupported) {
                                                                        "Required for receiving texts and showing notifications."
                                                                    } else {
                                                                        "Default SMS role unavailable on this device."
                                                                    },
                                                                    style = MaterialTheme.typography.bodySmall
                                                                )
                                                            }
                                                            if (isCheckingDefaultSms) {
                                                                CircularProgressIndicator(
                                                                    modifier = Modifier.padding(end = 8.dp),
                                                                    strokeWidth = 2.dp
                                                                )
                                                            } else if (defaultSmsSupported) {
                                                                OutlinedButton(onClick = {
                                                                    defaultSmsHelper.buildRoleRequestIntent()?.let { intent ->
                                                                        defaultSmsLauncher.launch(intent)
                                                                    }
                                                                }) {
                                                                    Text("Set")
                                                                }
                                                            }
                                                            OutlinedButton(
                                                                onClick = launchDefaultSmsCheck,
                                                                enabled = !isCheckingDefaultSms
                                                            ) {
                                                                Icon(Icons.Filled.Refresh, contentDescription = null)
                                                                Text("Refresh", modifier = Modifier.padding(start = 6.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        floatingActionButton = {
                                            if (currentRoute == BeaconNavRoute.Inbox) {
                                                FloatingActionButton(
                                                    onClick = { navController.navigate("sms/new") },
                                                    containerColor = parseColorOr(
                                                        MaterialTheme.colorScheme.primary,
                                                        state.settings.themePreferences.primaryColor
                                                    ),
                                                    contentColor = parseColorOr(
                                                        MaterialTheme.colorScheme.onPrimary,
                                                        state.settings.themePreferences.onBubbleOutgoing
                                                    )
                                                ) {
                                                    Icon(Icons.Filled.Edit, contentDescription = "New message")
                                                }
                                            }
                                        },
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
                                                        if (route != BeaconNavRoute.Inbox) {
                                                            smsInboxViewModel.clearSearch()
                                                        }
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
                                composable("sms/new") {
                                    NewMessageScreen(
                                        contacts = state.contacts,
                                        onBack = { navController.popBackStack() },
                                        onCreateConversation = { selected ->
                                            val numbers = selected.mapNotNull { it.phoneNumber }
                                                .map { it.trim() }
                                                .filter { it.isNotBlank() }
                                            if (numbers.isNotEmpty()) {
                                                val address = numbers.joinToString(";")
                                                navController.navigate("sms/thread/0/${Uri.encode(address)}")
                                            }
                                        },
                                        onManualInput = { raw ->
                                            val address = raw.trim()
                                            if (address.isNotBlank()) {
                                                navController.navigate("sms/thread/0/${Uri.encode(address)}")
                                            }
                                        },
                                        theme = state.settings.themePreferences
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
                                    val contact by threadViewModel.contact.collectAsStateWithLifecycle()
                                    val decodedAddress = Uri.decode(address)
                                    LaunchedEffect(threadId, decodedAddress) { threadViewModel.load(threadId, decodedAddress) }
                                    SmsThreadScreen(
                                        address = decodedAddress,
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
                                        onSendMessage = { body -> threadViewModel.sendMessage(decodedAddress, body) }
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
                                        otpCleanupEnabled = state.settings.otpCleanupEnabled,
                                        otpCleanupDays = state.settings.otpCleanupDays,
                                        onToggleOtpCleanup = { enabled -> viewModel.setOtpCleanupEnabled(enabled) },
                                        onChangeOtpCleanupDays = { days -> viewModel.setOtpCleanupDays(days) },
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
