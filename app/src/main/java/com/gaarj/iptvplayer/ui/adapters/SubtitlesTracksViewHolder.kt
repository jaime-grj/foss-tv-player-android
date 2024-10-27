package com.gaarj.iptvplayer.ui.adapters

import android.graphics.Color
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.gaarj.iptvplayer.R
import com.gaarj.iptvplayer.databinding.ItemChannelTrackSettingsBinding
import com.gaarj.iptvplayer.domain.model.SubtitlesTrack

class SubtitlesTracksViewHolder(view: View) : RecyclerView.ViewHolder(view){

    private val binding = ItemChannelTrackSettingsBinding.bind(view)

    fun render(subtitlesTrack: SubtitlesTrack, onItemSelected: (SubtitlesTrack) -> Unit){
        if (subtitlesTrack.isSelected) {
            binding.rbChannelSettingsTrack.isChecked = true
        }
        if (subtitlesTrack.language == "") {
            binding.tvChannelSettingsTrackName.text = subtitlesTrack.id
            binding.tvChannelSettingsTrackSubtitle.text = subtitlesTrack.codec
        }
        else if (subtitlesTrack.id == "-1") {
            binding.tvChannelSettingsTrackName.text = subtitlesTrack.language
            binding.tvChannelSettingsTrackSubtitle.visibility = View.GONE
        }
        else{
            binding.tvChannelSettingsTrackName.text = subtitlesTrack.language
            binding.tvChannelSettingsTrackSubtitle.text = subtitlesTrack.id + " - " + subtitlesTrack.codec
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
            onItemSelected(subtitlesTrack)
        }
    }
}