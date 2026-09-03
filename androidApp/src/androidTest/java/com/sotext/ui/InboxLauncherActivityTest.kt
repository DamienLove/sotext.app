package com.sotext.ui

import android.content.Intent
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InboxLauncherActivityTest {

    @get:Rule
    val scenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun launchingInboxShortcutSendsNewIntentToMainActivity() {
        scenarioRule.scenario.onActivity { activity ->
            val launcherIntent = Intent(activity, InboxLauncherActivity::class.java)
            activity.startActivity(launcherIntent)
        }

        Intents.intended(
            allOf(
                hasComponent(MainActivity::class.java.name),
                hasExtra("open_sms_inbox", true)
            )
        )

        scenarioRule.scenario.onActivity { activity ->
            assertTrue(activity.intent?.getBooleanExtra("open_sms_inbox", false) == true)
        }
    }
}
