package com.gaarx.iptvplayer.ui.viewholders

import android.graphics.Color
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.gaarx.iptvplayer.R
import com.gaarx.iptvplayer.core.MediaUtils
import com.gaarx.iptvplayer.domain.model.VideoTrack
import com.gaarx.iptvplayer.databinding.ItemChannelTrackSettingsBinding

class VideoTracksViewHolder(view: View) : RecyclerView.ViewHolder(view){

    private val binding = ItemChannelTrackSettingsBinding.bind(view)

    fun render(isQualityForced: Boolean, videoTrack: VideoTrack, onItemSelected: (VideoTrack) -> Unit){
        if (videoTrack.id == "-1") {
            binding.rbChannelSettingsTrack.isChecked = isQualityForced == false

            binding.tvChannelSettingsTrackName.text = videoTrack.name
            binding.tvChannelSettingsTrackSubtitle.visibility = View.GONE
        }
        else{
            if (isQualityForced){
                binding.rbChannelSettingsTrack.isChecked = videoTrack.isSelected
            }
            else{
                binding.rbChannelSettingsTrack.isChecked = false
            }
            binding.tvChannelSettingsTrackName.text = MediaUtils.calculateVideoQuality(videoTrack.width, videoTrack.height) + " " + videoTrack.width.toString() + "x" + videoTrack.height.toString()
            binding.tvChannelSettingsTrackSubtitle.text = "ID " + videoTrack.id + " - " + videoTrack.codec
            binding.tvChannelSettingsTrackSubtitle.visibility = View.VISIBLE
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
            onItemSelected(videoTrack)
        }
    }
}