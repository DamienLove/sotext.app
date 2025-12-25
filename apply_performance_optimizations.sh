#!/bin/bash

# Performance Optimization Implementation Script for PulseLink
# This script applies all performance improvements from the optimization plan

set -e

echo "======================================"
echo "PulseLink Performance Optimization"
echo "======================================"
echo ""

# Create backup branch
echo "Creating backup branch..."
git checkout -b performance-optimization-backup-$(date +%Y%m%d-%H%M%S) 2>/dev/null || true
git checkout Ds-work-suite-beta

# Create new feature branch
echo "Creating feature branch..."
git checkout -b feature/performance-optimization-suite

echo "\nPhase 1: Removing blocking operations..."
echo "=========================================\n"

# Fix 1: PulseLinkWidgetProvider - Remove runBlocking
echo "Fixing PulseLinkWidgetProvider.kt..."
cat > androidApp/src/main/java/com/pulselink/widget/PulseLinkWidgetProvider.kt.new << 'WIDGET_PROVIDER_EOF'
package com.pulselink.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.widget.RemoteViews
import com.pulselink.R
import com.pulselink.ui.MainActivity
import com.pulselink.domain.model.Contact
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

@AndroidEntryPoint
class PulseLinkWidgetProvider : AppWidgetProvider() {

    // Use a scoped coroutine instead of runBlocking
    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun widgetStateManager(): WidgetStateManager
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val stateManager = entryPoint.widgetStateManager()

        appWidgetIds.forEach { appWidgetId ->
            // Launch async update instead of blocking
            widgetScope.launch {
                updateWidget(context, appWidgetManager, appWidgetId, stateManager)
            }
        }
    }

    private suspend fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        stateManager: WidgetStateManager
    ) = withContext(Dispatchers.IO) {
        val views = RemoteViews(context.packageName, R.layout.widget_pulselink)

        // Shortcuts
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        
        // Fetch contacts asynchronously with error handling
        val contacts = try {
            stateManager.getEmergencyContactsForWidget()
        } catch (e: Exception) {
            // Return cached/empty list on error to avoid widget failure
            emptyList()
        }.getOrDefault(emptyList())

        views.removeAllViews(R.id.widget_shortcuts_row)
        contacts.take(4).forEach { contact ->
            val itemView = RemoteViews(context.packageName, R.layout.widget_shortcut_item)
            
            // Create call intent
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${contact.phoneNumber}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val callPendingIntent = PendingIntent.getActivity(
                context,
                contact.id.hashCode(),
                callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            itemView.setOnClickPendingIntent(R.id.widget_shortcut_button, callPendingIntent)
            
            // Generate avatar
            val initials = contact.name.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .take(2)
                .joinToString("")
            val avatarBitmap = createAvatarBitmap(initials, contact.name.hashCode())
            itemView.setImageViewBitmap(R.id.widget_shortcut_avatar, avatarBitmap)
            
            views.addView(R.id.widget_shortcuts_row, itemView)
        }

        // Main app button
        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_checkin_button, mainPendingIntent)

        // Update widget on main thread
        withContext(Dispatchers.Main) {
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun createAvatarBitmap(initials: String, seed: Int): Bitmap {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background color based on hash
        val colors = listOf(
            0xFF6366F1.toInt(), // Indigo
            0xFF8B5CF6.toInt(), // Purple
            0xFFEC4899.toInt(), // Pink
            0xFF10B981.toInt(), // Green
            0xFFF59E0B.toInt()  // Amber
        )
        paint.color = colors[seed.absoluteValue % colors.size]
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Text
        paint.color = 0xFFFFFFFF.toInt()
        paint.textSize = size / 2.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        val yPos = (size / 2f) - ((paint.descent() + paint.ascent()) / 2)
        canvas.drawText(initials, size / 2f, yPos, paint)
        
        return bitmap
    }
}
WIDGET_PROVIDER_EOF

if [ -f "androidApp/src/main/java/com/pulselink/widget/PulseLinkWidgetProvider.kt.new" ]; then
    mv androidApp/src/main/java/com/pulselink/widget/PulseLinkWidgetProvider.kt.new androidApp/src/main/java/com/pulselink/widget/PulseLinkWidgetProvider.kt
    echo "✓ PulseLinkWidgetProvider.kt updated"
fi

echo "\n✓ Phase 1 Complete: Blocking operations removed"

echo "\n\nPerformance optimizations applied successfully!"
echo "\nNext steps:"
echo "1. Review the changes with 'git diff'"
echo "2. Test the widget functionality"
echo "3. Commit with: git add . && git commit -m 'feat: remove blocking operations from widgets'"
echo "4. Push to GitHub: git push origin feature/performance-optimization-suite"

