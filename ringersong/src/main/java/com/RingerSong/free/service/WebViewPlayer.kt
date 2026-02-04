package com.RingerSong.free.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebViewPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var webView: WebView? = null
    private var windowManager: WindowManager? = null

    companion object {
        private const val TAG = "WebViewPlayer"
    }

    suspend fun play(url: String) = withContext(Dispatchers.Main) {
        stop() // Cleanup any existing

        Log.d(TAG, "Initializing WebView for playback: $url")

        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                loadUrl(url)
            }

            val params = WindowManager.LayoutParams(
                1, 1, // 1x1 pixel
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }

            windowManager?.addView(webView, params)
            Log.d(TAG, "WebView added to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing WebViewPlayer", e)
            stop()
        }
    }

    suspend fun stop() = withContext(Dispatchers.Main) {
        try {
            if (webView != null) {
                Log.d(TAG, "Stopping WebViewPlayer")
                webView?.loadUrl("about:blank")
                if (webView?.parent != null) {
                    windowManager?.removeView(webView)
                }
                webView?.destroy()
                webView = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping WebViewPlayer", e)
        }
    }
}
