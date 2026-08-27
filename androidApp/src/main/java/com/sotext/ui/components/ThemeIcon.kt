package com.sotext.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sotext.domain.model.ThemePreferences

object ThemeIconKey {
    const val BACK = "icon.back"
    const val SETTINGS = "icon.settings"
    const val LOCK = "icon.lock"
    const val SEARCH = "icon.search"
    const val CLOSE = "icon.close"
    const val INBOX = "icon.inbox"
    const val ARCHIVE = "icon.archive"
    const val UNARCHIVE = "icon.unarchive"
    const val DELETE = "icon.delete"
    const val FAVORITE = "icon.favorite"
    const val EDIT = "icon.edit"
    const val REFRESH = "icon.refresh"
    const val NOTIFICATIONS = "icon.notifications"
    const val PALETTE = "icon.palette"
    const val CALL = "icon.call"
    const val SEND = "icon.send"
    const val AI = "icon.ai"
    const val ARROW_DOWN = "icon.arrow_down"
    const val ATTACH = "icon.attach"
    const val CONTEXT_EVENT = "icon.context_event"
    const val CONTEXT_PLACE = "icon.context_place"
    const val CONTEXT_PHONE = "icon.context_phone"
    const val CONTEXT_LINK = "icon.context_link"
    const val CONTEXT_TRACKING = "icon.context_tracking"
    const val CONTEXT_CODE = "icon.context_code"
}

@Composable
fun ThemeIcon(
    iconKey: String,
    theme: ThemePreferences,
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    val overrideUrl = theme.iconOverrides[iconKey]?.trim().orEmpty()
    if (overrideUrl.isBlank()) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(overrideUrl)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier
        )
    }
}
