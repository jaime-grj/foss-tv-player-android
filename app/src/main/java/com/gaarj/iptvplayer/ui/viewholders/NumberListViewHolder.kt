package com.gaarj.iptvplayer.ui.viewholders

import android.graphics.Color
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.gaarj.iptvplayer.R
import com.gaarj.iptvplayer.databinding.ItemNumberListBinding

class NumberListViewHolder (view: View) : RecyclerView.ViewHolder(view){
    private val binding = ItemNumberListBinding.bind(view)

    fun render(number: Int, onItemSelected: (Int) -> Unit){
        binding.tvNumber.text = number.toString()

        binding.tvNumber.post{
            binding.tvNumber.requestLayout()
        }

        itemView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                itemView.setBackgroundResource(R.drawable.bg_rounded_item_menu) // Highlight the focused item
                binding.tvNumber.setTextColor(Color.BLACK)
                binding.tvNumber.post{
                    binding.tvNumber.requestLayout()
                }
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT) // Reset when not focused
                binding.tvNumber.setTextColor(Color.WHITE)
            }
        }

        itemView.setOnClickListener {
            onItemSelected(number)
        }
    }
}