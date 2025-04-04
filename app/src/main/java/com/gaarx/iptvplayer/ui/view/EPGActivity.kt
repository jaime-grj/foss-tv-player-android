package com.gaarx.iptvplayer.ui.view

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.gaarx.iptvplayer.R
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