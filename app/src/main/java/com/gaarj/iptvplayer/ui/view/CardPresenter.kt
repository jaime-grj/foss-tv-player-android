package com.gaarj.iptvplayer.ui.view

import android.graphics.Typeface
import android.view.ViewGroup
import androidx.leanback.widget.BaseCardView
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter


class CardPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context)
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        cardView.textAlignment = BaseCardView.TEXT_ALIGNMENT_CENTER

        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        val title = item as String
        val cardView = viewHolder!!.view as ImageCardView
        cardView.titleText = title
        cardView.setMainImageDimensions(250, 180)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {
    }
}