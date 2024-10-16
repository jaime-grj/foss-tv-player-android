package com.gaarj.iptvplayer.ui.adapters

import android.graphics.Color
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.gaarj.iptvplayer.R
import com.gaarj.iptvplayer.domain.model.VideoTrack
import com.gaarj.iptvplayer.databinding.ItemChannelTrackSettingsBinding

class VideoTracksViewHolder(view: View) : RecyclerView.ViewHolder(view){

    private val binding = ItemChannelTrackSettingsBinding.bind(view)

    fun render(videoTrack: VideoTrack, onItemSelected: (VideoTrack) -> Unit){
        if (videoTrack.isSelected) {
            binding.tvChannelSettingsTrackName.isChecked = true
        }
        if (videoTrack.id != "-1") {
            binding.tvChannelSettingsTrackName.text =
                "ID " + videoTrack.id + " - " + videoTrack.width + "x" + videoTrack.height
        }
        else{
            binding.tvChannelSettingsTrackName.text = videoTrack.name
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
            onItemSelected(videoTrack)
        }
    }
}