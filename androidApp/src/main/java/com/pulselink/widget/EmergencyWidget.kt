package com.pulselink.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pulselink.R
import com.pulselink.domain.model.EscalationTier
import androidx.glance.LocalContext

class EmergencyWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }

    @Composable
    fun WidgetContent() {
        val context = LocalContext.current
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier.padding(bottom = 12.dp)
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_logo),
                    contentDescription = null,
                    modifier = GlanceModifier.size(24.dp)
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = context.getString(R.string.app_name),
                    style = TextStyle(
                        color = GlanceTheme.colors.onBackground,
                        fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
                    )
                )
            }

            // Buttons
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    text = context.getString(R.string.widget_button_emergency),
                    onClick = actionRunCallback<EmergencyWidgetActionWorker>(
                        actionParametersOf(
                            EmergencyWidgetActionWorker.ACTION_KEY to EmergencyWidgetActionWorker.ACTION_EMERGENCY
                        )
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                Button(
                    text = context.getString(R.string.widget_button_check_in),
                    onClick = actionRunCallback<EmergencyWidgetActionWorker>(
                        actionParametersOf(
                            EmergencyWidgetActionWorker.ACTION_KEY to EmergencyWidgetActionWorker.ACTION_CHECK_IN
                        )
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }
}
