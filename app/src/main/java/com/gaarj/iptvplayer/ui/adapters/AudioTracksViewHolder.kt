package com.gaarj.iptvplayer.ui.adapters

import android.graphics.Color
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.gaarj.iptvplayer.R
import com.gaarj.iptvplayer.databinding.ItemChannelTrackSettingsBinding
import com.gaarj.iptvplayer.domain.model.AudioTrack

class AudioTracksViewHolder(view: View) : RecyclerView.ViewHolder(view){

    private val binding = ItemChannelTrackSettingsBinding.bind(view)

    fun render(audioTrack: AudioTrack, onItemSelected: (AudioTrack) -> Unit){
        if (audioTrack.isSelected) {
            binding.tvChannelSettingsTrackName.isChecked = true
        }
        if (audioTrack.language == "") {
            binding.tvChannelSettingsTrackName.text = audioTrack.id
        }
        else{
            binding.tvChannelSettingsTrackName.text = audioTrack.language
        }

        binding.tvChannelSettingsTrackName.post{
            binding.tvChannelSettingsTrackName.requestLayout()
        }

        itemView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                itemView.setBackgroundResource(R.drawable.bg_rounded_item_menu) // Highlight the focused item
                binding.tvChannelSettingsTrackName.setTextColor(Color.BLACK)
                binding.tvChannelSettingsTrackName.post{
                    binding.tvChannelSettingsTrackName.requestLayout()
                }
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT) // Reset when not focused
                binding.tvChannelSettingsTrackName.setTextColor(Color.WHITE)
            }
        }

        itemView.setOnClickListener {
            onItemSelected(audioTrack)
        }
    }
}