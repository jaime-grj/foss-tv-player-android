package com.gaarx.tvplayer.ui.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import com.gaarx.tvplayer.databinding.FragmentPlayerBinding

class LoadingAnimationHelper(private val binding: FragmentPlayerBinding) {
    private var animatorSet: AnimatorSet? = null

    companion object {
        var animationsEnabled = true
    }

    /**
     * Starts the loading dots animation and makes the container visible.
     */
    fun showLoadingDots() {
        binding.loadingDots.visibility = View.VISIBLE
        animatorSet?.cancel()
        animatorSet = createLoadingAnimation()
        animatorSet?.start()
    }

    /**
     * Stops the loading dots animation, hides the container, and resets dot properties.
     */
    fun hideLoadingDots() {
        binding.loadingDots.visibility = View.GONE
        animatorSet?.cancel()
        resetDotProperties()
    }

    private fun createLoadingAnimation(): AnimatorSet {
        val dot1Animation = createDotAnimation(binding.dot1, 0)
        val dot2Animation = createDotAnimation(binding.dot2, 150)
        val dot3Animation = createDotAnimation(binding.dot3, 300)

        return AnimatorSet().apply {
            playTogether(dot1Animation, dot2Animation, dot3Animation)
        }
    }

    private fun createDotAnimation(dot: View, startDelay: Long): AnimatorSet {
        val scaleX = ObjectAnimator.ofFloat(dot, "scaleX", 1f, 1.5f, 1f)
        val scaleY = ObjectAnimator.ofFloat(dot, "scaleY", 1f, 1.5f, 1f)
        val alpha = ObjectAnimator.ofFloat(dot, "alpha", 1f, 0.5f, 1f)

        listOf(scaleX, scaleY, alpha).forEach {
            it.duration = 500
            it.startDelay = startDelay
            it.repeatCount = if (animationsEnabled) ObjectAnimator.INFINITE else 0
        }

        return AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
        }
    }

    private fun resetDotProperties() {
        listOf(binding.dot1, binding.dot2, binding.dot3).forEach { dot ->
            dot.scaleX = 1f
            dot.scaleY = 1f
            dot.alpha = 1f
        }
    }
}
