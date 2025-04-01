package com.gaarj.iptvplayer.ui.view

import android.os.Bundle
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow



class SettingsFragment : RowsSupportFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

        val settingsItems = arrayOf(
            "Display Settings",
            "Sound Settings",
            "Network Settings",
            "About",
            "Exit"
        )

        val cardPresenter = SettingsCardPresenter()
        val listRowAdapter = ArrayObjectAdapter(cardPresenter)

        for (item in settingsItems) {
            listRowAdapter.add(item)
        }

        val headerItem = HeaderItem(0, "Settings")
        rowsAdapter.add(ListRow(headerItem, listRowAdapter))

        adapter = rowsAdapter

        setAlignment()  // Aligns the menu to the right
    }

    private fun setAlignment() {
        setSelectedPosition(0, true, object : Presenter.ViewHolderTask() {
            override fun run(holder: Presenter.ViewHolder) {
                holder.view.apply {
                    pivotX = width.toFloat()
                    translationX = 500f  // Adjust as needed for proper right alignment
                }
            }
        })
    }
}
