package com.gaarj.iptvplayer.ui.viewholders

import android.graphics.Color
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.gaarj.iptvplayer.R
import com.gaarj.iptvplayer.databinding.ItemChannelListBinding
import com.gaarj.iptvplayer.domain.model.ChannelItem

class ChannelListViewHolder(view: View) : RecyclerView.ViewHolder(view){

    private val binding = ItemChannelListBinding.bind(view)

    fun render(channel: ChannelItem, onChannelSelected: (ChannelItem) -> Unit){
        binding.tvChannelListName.text = channel.indexFavourite.toString() + "  " + channel.name
        binding.tvChannelListSubtitle.text = channel.currentProgram?.title.orEmpty()

        val currentTime = System.currentTimeMillis()
        val currentProgram = channel.epgPrograms.filter { it.startTime.time < currentTime && it.stopTime.time > currentTime }
        if (currentProgram.isNotEmpty()) {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvChannelListSubtitle.visibility = View.VISIBLE
            val currentProgramDuration = currentProgram[0].stopTime.time.minus(
                currentProgram[0].startTime.time
            )
            val progress =
                (currentTime - currentProgram[0].startTime.time) * 100 / currentProgramDuration
            binding.progressBar.progress = progress.toInt()
            binding.tvChannelListSubtitle.text = currentProgram[0].title
        }

        /*if (channel.currentProgram != null) {
            val currentProgramDuration = channel.currentProgram?.stopTime?.time?.minus(
                channel.currentProgram?.startTime?.time ?: 0
            ) ?: 0
            val currentTime = System.currentTimeMillis()
            val progress =
                (currentTime - channel.currentProgram?.startTime?.time!!) * 100 / currentProgramDuration
            binding.progressBar.progress = progress.toInt()
        }*/
        else {
            binding.progressBar.progress = 0
            binding.progressBar.visibility = View.GONE
            binding.tvChannelListSubtitle.visibility = View.GONE
        }

        binding.tvChannelListName.post{
            binding.tvChannelListName.requestLayout()
        }

        itemView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                itemView.setBackgroundResource(R.drawable.bg_rounded_item_menu) // Highlight the focused item
                binding.tvChannelListName.setTextColor(Color.BLACK)
                binding.tvChannelListSubtitle.setTextColor(Color.BLACK)
                binding.tvChannelListName.post{
                    binding.tvChannelListName.requestLayout()
                }
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT) // Reset when not focused
                binding.tvChannelListName.setTextColor(Color.WHITE)
                binding.tvChannelListSubtitle.setTextColor(Color.WHITE)
            }
        }

        itemView.setOnClickListener {
            onChannelSelected(channel)
        }
    }
}