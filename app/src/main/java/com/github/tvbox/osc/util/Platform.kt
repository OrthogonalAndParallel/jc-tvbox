package com.github.tvbox.osc.util

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration

object Platform {
    fun isTvDevice(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
    fun isLandscape(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }
    fun useTvUi(context: Context): Boolean {
        return isTvDevice(context) || isLandscape(context)
    }
}
