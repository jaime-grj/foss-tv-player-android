package com.gaarx.iptvplayer.ui.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.annotation.OptIn
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.gaarx.iptvplayer.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PlayerFragment())
                .commit()
        }
    }

    @SuppressLint("RestrictedApi")
    @OptIn(UnstableApi::class)
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is PlayerFragment) {
            if (fragment.handleKeyEvent(event)) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    fun showPipOverlay(player: ExoPlayer) {
        val pipView = findViewById<PlayerView>(R.id.pip_player_view)
        pipView.player = player
        pipView.useController = false
        pipView.visibility = View.VISIBLE
    }

    fun hidePipOverlay() {
        val pipView = findViewById<PlayerView>(R.id.pip_player_view)
        pipView.player?.release()
        pipView.visibility = View.GONE
    }
}
