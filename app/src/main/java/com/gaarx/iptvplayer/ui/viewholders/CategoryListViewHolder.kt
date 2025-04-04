package com.gaarx.iptvplayer.ui.viewholders

import android.graphics.Color
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.gaarx.iptvplayer.R
import com.gaarx.iptvplayer.databinding.ItemCategoryListBinding
import com.gaarx.iptvplayer.domain.model.CategoryItem

class CategoryListViewHolder(view: View) : RecyclerView.ViewHolder(view){

    private val binding = ItemCategoryListBinding.bind(view)

    fun render(category: CategoryItem, onCategorySelected: (CategoryItem) -> Unit){
        binding.tvCategoryListName.text = category.name
        if (category.description.isNullOrEmpty()) {
            binding.tvCategoryListSubtitle.visibility = View.GONE
        } else {
            binding.tvCategoryListSubtitle.visibility = View.VISIBLE
            binding.tvCategoryListSubtitle.text = category.description
        }


        binding.tvCategoryListName.post{
            binding.tvCategoryListName.requestLayout()
        }

        itemView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                itemView.setBackgroundResource(R.drawable.bg_rounded_item_menu) // Highlight the focused item
                binding.tvCategoryListName.setTextColor(Color.BLACK)
                binding.tvCategoryListSubtitle.setTextColor(Color.BLACK)
                binding.tvCategoryListName.post{
                    binding.tvCategoryListName.requestLayout()
                }
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT) // Reset when not focused
                binding.tvCategoryListName.setTextColor(Color.WHITE)
                binding.tvCategoryListSubtitle.setTextColor(Color.WHITE)
            }
        }

        itemView.setOnClickListener {
            onCategorySelected(category)
        }
    }
}