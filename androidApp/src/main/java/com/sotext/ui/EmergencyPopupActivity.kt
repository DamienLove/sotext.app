package com.sotext.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.sotext.R
import com.sotext.domain.model.EscalationTier
import com.sotext.service.AlertRouter
import com.sotext.ui.theme.PulseLinkTheme
import com.sotext.widget.WidgetStateManager
import com.sotext.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

@AndroidEntryPoint
@android.annotation.SuppressLint("MissingPermission")
class EmergencyPopupActivity : AppCompatActivity() {

    @Inject
    lateinit var widgetStateManager: WidgetStateManager
    
    @Inject
    lateinit var alertRouter: AlertRouter

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Trigger Activation Haptic
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
             @Suppress("DEPRECATION")
             vibrator.vibrate(500)
        }

        // Set window flags to show on top of lock screen
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: "Unknown"
        val tier = intent.getStringExtra(EXTRA_ESCALATION_TIER) ?: "Emergency"
        val triggerSource = intent.getStringExtra("EXTRA_TRIGGER_SOURCE") ?: "Widget emergency"

        setContent {
            PulseLinkTheme {
                EmergencyPopupScreen(
                    contactName = contactName,
                    tier = tier,
                    onCancel = { authenticateAndCancel() },
                    onTimeout = { 
                        // Dispatch Emergency Alert
                        lifecycleScope.launch {
                            alertRouter.dispatchManual(EscalationTier.EMERGENCY, triggerSource)
                        }
                    }
                )
            }
        }
    }

    private fun authenticateAndCancel() {
        val biometricResult = BiometricManager.from(this)
            .canAuthenticate(CANCEL_EMERGENCY_AUTHENTICATORS)
        if (biometricResult != BiometricManager.BIOMETRIC_SUCCESS) {
            handleBiometricUnavailable(biometricResult)
            return
        }
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    cancelEmergency()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        return
                    }
                    Log.w(TAG, "Biometric auth error $errorCode: $errString")
                    val message = errString.takeIf { it.isNotBlank() }?.toString()
                        ?: getString(R.string.cancel_emergency_failure)
                    Toast.makeText(this@EmergencyPopupActivity, message, Toast.LENGTH_LONG).show()
                }

                override fun onAuthenticationFailed() {
                    Toast.makeText(
                        this@EmergencyPopupActivity,
                        getString(R.string.cancel_emergency_failure),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.cancel_emergency_prompt_title))
            .setSubtitle(getString(R.string.cancel_emergency_prompt_subtitle))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfoBuilder.setAllowedAuthenticators(CANCEL_EMERGENCY_AUTHENTICATORS)
        } else {
            promptInfoBuilder.setNegativeButtonText(getString(android.R.string.cancel))
        }
        val promptInfo = promptInfoBuilder.build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun handleBiometricUnavailable(biometricResult: Int) {
        Toast.makeText(
            this,
            getString(R.string.cancel_emergency_biometric_unavailable),
            Toast.LENGTH_LONG
        ).show()
        if (
            biometricResult == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ||
            biometricResult == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
        ) {
            showCancelEmergencyFallbackDialog()
        } else {
            Log.w(TAG, "Biometric unavailable: $biometricResult")
        }
    }

    private fun showCancelEmergencyFallbackDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.cancel_emergency_prompt_title))
            .setMessage(getString(R.string.cancel_emergency_fallback_message))
            .setPositiveButton(getString(R.string.cancel_emergency_button)) { _, _ ->
                cancelEmergency()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun cancelEmergency() {
        // Haptic Feedback for Cancellation
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1))
        } else {
             @Suppress("DEPRECATION")
             vibrator.vibrate(300)
        }
        
        // Reset Widget State
        lifecycleScope.launch {
            settingsRepository.setEmergencyActive(false)
            widgetStateManager.requestWidgetUpdate()
        }
        finish()
    }

    companion object {
        private const val EXTRA_CONTACT_NAME = "extra_contact_name"
        private const val EXTRA_ESCALATION_TIER = "extra_escalation_tier"
        private const val TAG = "EmergencyPopupActivity"
        private const val CANCEL_EMERGENCY_AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        fun newIntent(context: Context, contactName: String, tier: String): Intent {
            return Intent(context, EmergencyPopupActivity::class.java).apply {
                putExtra(EXTRA_CONTACT_NAME, contactName)
                putExtra(EXTRA_ESCALATION_TIER, tier)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        }
    }
}

@Composable
fun EmergencyPopupScreen(
    contactName: String, 
    tier: String, 
    onCancel: () -> Unit,
    onTimeout: () -> Unit
) {
    var timeLeft by remember { mutableStateOf(5) } // 5s flashing/grace
    var isFlashing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Flashing Timer
        launch {
            while (timeLeft > 0) {
                isFlashing = !isFlashing
                delay(500) // 1Hz (500ms toggle)
            }
        }
        // Countdown
        launch {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            onTimeout()
        }
    }

    val backgroundColor = if (isFlashing) Color.White else Color.Red
    val contentColor = if (isFlashing) Color.Red else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (contactName == "Crash Detected") "CRASH DETECTED" else "EMERGENCY ACTIVE",
            color = contentColor,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Dispatching in $timeLeft s",
            color = contentColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(containerColor = contentColor)
        ) {
            Text(
                "Cancel Alert", 
                color = backgroundColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Requires Authentication",
            color = contentColor,
            fontSize = 14.sp
        )
    }
}
