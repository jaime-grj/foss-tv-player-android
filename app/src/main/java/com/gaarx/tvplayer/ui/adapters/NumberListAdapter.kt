package com.gaarx.tvplayer.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaarx.tvplayer.R
import com.gaarx.tvplayer.ui.viewholders.NumberListViewHolder

class NumberListAdapter(private val numberList: List<Int>, private val onItemSelected: (Int) -> Unit) : RecyclerView.Adapter<NumberListViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumberListViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return NumberListViewHolder(layoutInflater.inflate(R.layout.item_number_list, parent, false))
    }

    override fun onBindViewHolder(holder: NumberListViewHolder, position: Int) {
        val number = numberList[position]
        holder.render(number, onItemSelected)
    }

    override fun getItemCount(): Int = numberList.size

    fun getItemAtPosition(position: Int): Int? {
        return if (position >= 0 && position < numberList.size) {
            numberList[position]
        } else {
            null // Or handle this case as needed
        }
    }
}