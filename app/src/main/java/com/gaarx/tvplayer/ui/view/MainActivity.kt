package com.gaarx.tvplayer.ui.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.annotation.OptIn
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.gaarx.tvplayer.R
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.view.isVisible

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
        pipView.player?.playWhenReady = false
        pipView.player?.release()
        pipView.visibility = View.GONE
    }

    override fun onPause() {
        val pipView = findViewById<PlayerView>(R.id.pip_player_view)
        if (pipView.isVisible) {
            pipView.player?.playWhenReady = false
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        val pipView = findViewById<PlayerView>(R.id.pip_player_view)
        if (pipView.isVisible) {
            pipView.player?.playWhenReady = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val pipView = findViewById<PlayerView>(R.id.pip_player_view)
        pipView.player?.release()
        pipView.player = null
    }

    override fun onStop() {
        val pipView = findViewById<PlayerView>(R.id.pip_player_view)
        pipView.player?.release()
        pipView.player = null
        pipView.visibility = View.GONE
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
    }
}
