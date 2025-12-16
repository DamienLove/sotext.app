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
import androidx.compose.ui.Modifier
import com.google.android.gms.ads.MobileAds
import com.pulselink.beacon.ui.ads.BannerAd
import com.pulselink.beacon.ui.InboxScreen
import com.pulselink.beacon.ui.SmsViewModel
import com.pulselink.beacon.ui.ThreadScreen
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)
        setContent {
            BeaconTheme {
                val vm: SmsViewModel = viewModel(factory = SmsViewModel.factory(application))
                BeaconNav(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BeaconNav(vm: SmsViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var isDefaultSms by remember {
        mutableStateOf(Telephony.Sms.getDefaultSmsPackage(context) == context.packageName)
    }
    var missingPerms by remember { mutableStateOf(requiredPermissions(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        missingPerms = requiredPermissions(context)
    }

    LaunchedEffect(Unit) {
        isDefaultSms = Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
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
                    isDefaultSms = isDefaultSms,
                    missingPermissions = missingPerms,
                    onRequestPermissions = {
                        permissionLauncher.launch(missingPerms.toTypedArray())
                    },
                    onRequestDefault = {
                        requestDefaultSms(context)
                        isDefaultSms = Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
                        missingPerms = requiredPermissions(context)
                    },
                    onOpenThread = { id, address ->
                        vm.openThread(id, address)
                        navController.navigate("thread/$id/${Uri.encode(address)}")
                    },
                    onDeleteThread = { vm.deleteThread(it) },
                    onRefresh = { vm.refreshThreads() }
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
                ThreadScreen(
                    address = address.ifBlank { "Unknown" },
                    messages = vm.messages,
                    onBack = { navController.popBackStack() },
                    onSend = { vm.sendMessage(it) },
                    onDeleteThread = {
                        vm.deleteThread(threadId)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

private fun requestDefaultSms(context: android.content.Context) {
    val packageName = context.packageName
    if (Telephony.Sms.getDefaultSmsPackage(context) == packageName) return

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
