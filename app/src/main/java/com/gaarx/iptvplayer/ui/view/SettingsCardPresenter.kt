package com.gaarx.iptvplayer.ui.view

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.leanback.widget.Presenter

class SettingsCardPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val textView = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(300, 100)
            isFocusable = true
            isFocusableInTouchMode = true
            setTextColor(Color.WHITE)
            textSize = 18f
            setBackgroundColor(Color.DKGRAY)
            gravity = Gravity.CENTER
        }
        return ViewHolder(textView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        (viewHolder.view as TextView).text = item as String
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {}
}
