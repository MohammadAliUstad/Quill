package com.yugentech.quill.ui.about.moreApps.components

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri

// Launcher background colours, so each icon sits on the same tile as on the home screen.
val MyntIconBackground = Color(0xFFFBF6E4)
val SessionsIconBackground = Color(0xFFFFE6C7)
val RyoriIconBackground = Color(0xFFFDDCB5)
val HajimeIconBackground = Color(0xFFF5F0E8)

const val SESSIONS_PACKAGE = "com.yugentech.sessions"

/** Opens the Play Store app if installed, otherwise the store's web page. */
fun openPlayStore(context: Context, packageName: String) {
    val market = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
    val web = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri())
    runCatching { context.startActivity(market) }
        .onFailure { runCatching { context.startActivity(web) } }
}
