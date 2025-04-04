package com.gaarx.iptvplayer.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaarx.iptvplayer.R
import com.gaarx.iptvplayer.domain.model.CategoryItem
import com.gaarx.iptvplayer.ui.viewholders.CategoryListViewHolder

class CategoryListAdapter(private val categoryList: List<CategoryItem>, private val onCategorySelected: (CategoryItem) -> Unit) : RecyclerView.Adapter<CategoryListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryListViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return CategoryListViewHolder(layoutInflater.inflate(R.layout.item_category_list, parent, false))
    }

    override fun onBindViewHolder(holder: CategoryListViewHolder, position: Int) {
        val category = categoryList[position]
        holder.render(category, onCategorySelected)
    }

    override fun getItemCount(): Int = categoryList.size

    fun getItemAtPosition(position: Int): CategoryItem? {
        return if (position >= 0 && position < categoryList.size) {
            categoryList[position]
        } else {
            null // Or handle this case as needed
        }
    }
}