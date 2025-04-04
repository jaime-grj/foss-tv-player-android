package com.gaarx.iptvplayer.ui.viewholders

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.gaarx.iptvplayer.R
import com.gaarx.iptvplayer.databinding.ItemChannelTrackSettingsBinding
import com.gaarx.iptvplayer.domain.model.AudioTrack

class AudioTracksViewHolder(view: View) : RecyclerView.ViewHolder(view){

    private val binding = ItemChannelTrackSettingsBinding.bind(view)

    @SuppressLint("SetTextI18n")
    fun render(audioTrack: AudioTrack, onItemSelected: (AudioTrack) -> Unit){
        if (audioTrack.isSelected) {
            binding.rbChannelSettingsTrack.isChecked = true
        }
        if (audioTrack.language == "") {
            binding.tvChannelSettingsTrackName.text = audioTrack.id
            binding.tvChannelSettingsTrackSubtitle.text = audioTrack.codec +
                    if (audioTrack.bitrate > 0){
                        " - " + (audioTrack.bitrate / 1000).toString() + " kbps"
                    } else {
                        ""
                    } +
                    if (audioTrack.channelCount > 0){
                        " - " + audioTrack.channelCount + " ch"
                    } else{
                        ""
                    }
        }
        else{
            binding.tvChannelSettingsTrackName.text = audioTrack.language
            binding.tvChannelSettingsTrackSubtitle.text =
                audioTrack.id + " - " + audioTrack.codec +
                        if (audioTrack.bitrate > 0){
                            " - " + (audioTrack.bitrate / 1000).toString() + " kbps"
                        } else {
                            ""
                        } + " - " + audioTrack.channelCount + " ch"
        }

        binding.tvChannelSettingsTrackName.post{
            binding.tvChannelSettingsTrackName.requestLayout()
        }

        itemView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                itemView.setBackgroundResource(R.drawable.bg_rounded_item_menu) // Highlight the focused item
                binding.tvChannelSettingsTrackName.setTextColor(Color.BLACK)
                binding.tvChannelSettingsTrackSubtitle.setTextColor(Color.BLACK)
                binding.tvChannelSettingsTrackName.post{
                    binding.tvChannelSettingsTrackName.requestLayout()
                }
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT) // Reset when not focused
                binding.tvChannelSettingsTrackName.setTextColor(Color.WHITE)
                binding.tvChannelSettingsTrackSubtitle.setTextColor(Color.WHITE)
            }
        }

        itemView.setOnClickListener {
            onItemSelected(audioTrack)
        }
    }
}