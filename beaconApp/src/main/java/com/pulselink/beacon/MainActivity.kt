package com.pulselink.beacon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.google.android.gms.ads.MobileAds
import com.pulselink.beacon.ui.ads.BannerAd
import com.pulselink.beacon.ui.InboxScreen
import com.pulselink.beacon.ui.SmsViewModel
import com.pulselink.beacon.ui.ThreadScreen
import com.pulselink.beacon.ui.ThemeViewModel
import com.pulselink.beacon.data.ThemeState
import com.pulselink.beacon.ui.customize.CustomizationScreen
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)
        setContent {
            val vm: SmsViewModel = viewModel(factory = SmsViewModel.factory(application))
            val themeVm: ThemeViewModel = viewModel(factory = ThemeViewModel.factory(application))
            BeaconTheme(theme = themeVm.themeState.global) {
                BeaconNav(vm, themeVm, themeVm.themeState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BeaconNav(vm: SmsViewModel, themeVm: ThemeViewModel, themeState: ThemeState) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDefaultSms by remember {
        mutableStateOf(isDefaultSmsRoleHeld(context))
    }
    var isCheckingDefaultSms by remember { mutableStateOf(false) }
    var missingPerms by remember { mutableStateOf(requiredPermissions(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        missingPerms = requiredPermissions(context)
    }

    LaunchedEffect(Unit) {
        isDefaultSms = isDefaultSmsRoleHeld(context)
        missingPerms = requiredPermissions(context)
    }

    Scaffold(
        bottomBar = { BannerAd(modifier = Modifier.navigationBarsPadding()) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "inbox",
            modifier = Modifier
                .imePadding()
                .padding(padding)
        ) {
            composable("inbox") {
                InboxScreen(
                    threads = vm.threads,
                    theme = themeState.global,
                    searchState = vm.searchState,
                    isDefaultSms = isDefaultSms,
                    isCheckingDefaultSms = isCheckingDefaultSms,
                    missingPermissions = missingPerms,
                    onRequestPermissions = {
                        permissionLauncher.launch(missingPerms.toTypedArray())
                    },
                    onRequestDefault = {
                        requestDefaultSms(context)
                        scope.launch {
                             isCheckingDefaultSms = true
                             isDefaultSms = checkDefaultSmsWithRetry(context)
                             isCheckingDefaultSms = false
                             missingPerms = requiredPermissions(context)
                        }
                    },
                    onOpenThread = { id, address ->
                        vm.openThread(id, address)
                        navController.navigate("thread/$id/${Uri.encode(address)}")
                    },
                    onDeleteThread = { vm.deleteThread(it) },
                    onRefresh = { vm.refreshThreads() },
                    onSearch = { vm.search(it) },
                    onClearSearch = { vm.clearSearch() },
                    onCustomize = { navController.navigate("customize?address=") }
                )
            }
            composable(
                route = "thread/{threadId}/{address}",
                arguments = listOf(
                    navArgument("threadId") { type = NavType.LongType },
                    navArgument("address") { type = NavType.StringType })
            ) { backStackEntry ->
                val threadId = backStackEntry.arguments?.getLong("threadId") ?: 0L
                val address = backStackEntry.arguments?.getString("address")?.let { Uri.decode(it) } ?: ""
                LaunchedEffect(threadId) {
                    vm.openThread(threadId, address)
                }
                val contactTheme = themeState.forAddress(address)
                BeaconTheme(theme = contactTheme) {
                    ThreadScreen(
                        address = address.ifBlank { "Unknown" },
                        messages = vm.messages,
                        theme = contactTheme,
                        onBack = { navController.popBackStack() },
                        onSend = { vm.sendMessage(it) },
                        onDeleteThread = {
                            vm.deleteThread(threadId)
                            navController.popBackStack()
                        },
                        onCustomize = { navController.navigate("customize?address=${Uri.encode(address)}") }
                    )
                }
            }
            composable(
                route = "customize?address={address}",
                arguments = listOf(
                    navArgument("address") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    }
                )
            ) { backStackEntry ->
                val addressArg = backStackEntry.arguments?.getString("address")?.takeIf { it.isNotBlank() }
                    ?.let { Uri.decode(it) }
                CustomizationScreen(
                    address = addressArg,
                    themeState = themeVm.themeState,
                    onBack = { navController.popBackStack() },
                    onColorChange = { target, color -> themeVm.applyColor(target, color, addressArg) },
                    onFontChange = { themeVm.applyFont(it, addressArg) },
                    onRadiusChange = { themeVm.applyRadius(it, addressArg) },
                    onPreset = { preset -> themeVm.applyPreset(preset, addressArg) },
                    onResetContact = { addressArg?.let { themeVm.resetContact(it) } },
                    onIconVariant = { variant -> themeVm.updateIconVariant(variant) }
                )
            }
        }
    }
}

private fun requestDefaultSms(context: android.content.Context) {
    val packageName = context.packageName
    if (isDefaultSmsRoleHeld(context)) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } else {
        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
            putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

private suspend fun checkDefaultSmsWithRetry(context: android.content.Context, maxAttempts: Int = 5, initialDelayMs: Long = 300): Boolean {
    var currentDelay = initialDelayMs
    repeat(maxAttempts) { attempt ->
        if (isDefaultSmsRoleHeld(context)) {
            return true
        }
        android.util.Log.d("DefaultSmsHelper", "Default SMS check attempt ${attempt + 1} failed. Retrying in ${currentDelay}ms...")
        delay(currentDelay)
        currentDelay *= 2
    }
    return isDefaultSmsRoleHeld(context)
}

private fun isDefaultSmsRoleHeld(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true) {
            return true
        }
    }
    return Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
}

private fun requiredPermissions(context: android.content.Context): List<String> {
    val basePerms = listOf(
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.RECEIVE_SMS,
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.RECEIVE_MMS,
        android.Manifest.permission.RECEIVE_WAP_PUSH
    )
    val notif =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) listOf(android.Manifest.permission.POST_NOTIFICATIONS) else emptyList()
    val all = basePerms + notif
    return all.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }
}
