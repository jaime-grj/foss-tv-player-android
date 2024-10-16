package com.gaarj.iptvplayer.ui.adapters

import android.graphics.Color
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.gaarj.iptvplayer.R
import com.gaarj.iptvplayer.databinding.ItemChannelListBinding
import com.gaarj.iptvplayer.domain.model.ChannelItem

class ChannelListViewHolder(view: View) : RecyclerView.ViewHolder(view){

    private val binding = ItemChannelListBinding.bind(view)

    fun render(channel: ChannelItem, onChannelSelected: (ChannelItem) -> Unit){
        binding.tvChannelListNumber.text = channel.indexFavourite.toString()
        binding.tvChannelListName.text = channel.name

        binding.tvChannelListName.post{
            binding.tvChannelListName.requestLayout()
        }

        itemView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                itemView.setBackgroundResource(R.drawable.bg_rounded_item_menu) // Highlight the focused item
                binding.tvChannelListName.setTextColor(Color.BLACK)
                binding.tvChannelListNumber.setTextColor(Color.BLACK)
                binding.tvChannelListName.post{
                    binding.tvChannelListName.requestLayout()
                }
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT) // Reset when not focused
                binding.tvChannelListName.setTextColor(Color.WHITE)
                binding.tvChannelListNumber.setTextColor(Color.WHITE)
            }
        }

        itemView.setOnClickListener {
            onChannelSelected(channel)
        }
    }
}