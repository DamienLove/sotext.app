package com.pulselink.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pulselink.ui.screens.LoginScreen
import com.pulselink.ui.state.LoginUiState
import com.pulselink.ui.theme.PulseLinkTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun googleSignInButtonIsVisible() {
        composeRule.setContent {
            PulseLinkTheme {
                LoginScreen(
                    state = LoginUiState(),
                    onEmailChange = {},
                    onPasswordChange = {},
                    onConfirmPasswordChange = {},
                    onSubmit = {},
                    onToggleMode = {},
                    onForgotPassword = {},
                    onSmsOnlyClick = {},
                    onGoogleClick = {},
                    onMessageConsumed = {}
                )
            }
        }

        composeRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
    }
}
