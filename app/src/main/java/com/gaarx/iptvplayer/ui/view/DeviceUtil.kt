package com.gaarx.iptvplayer.ui.view

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration

object DeviceUtil {
    fun isAndroidTV(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
}