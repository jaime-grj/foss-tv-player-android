package com.gaarj.iptvplayer.ui.view

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.gaarj.iptvplayer.R
import com.gaarj.iptvplayer.ui.viewmodel.ChannelViewModel
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Loads [EpgFragment].
 */
@AndroidEntryPoint
class EPGActivity : FragmentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize the 310 backport lib. Usually you do this in your application object
        AndroidThreeTen.init(this)
        setContentView(R.layout.activity_epg)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, EpgFragment())
            .commit()
    }
}