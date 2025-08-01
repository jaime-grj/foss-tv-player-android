package com.gaarx.iptvplayer.ui.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.leanback.widget.Presenter

import com.gaarx.iptvplayer.R
import com.gaarx.iptvplayer.ui.viewmodel.ChannelViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainFragment : BrowseSupportFragment(), OnItemViewClickedListener {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title = getString(R.string.browse)

        val rowTitleTv = "TV"
        val rowTitleSearch = "Buscar canales"
        val rowTitleConfig = "Configuración"

        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        val listRowAdapterTv = ArrayObjectAdapter(CardPresenter())
        val listRowAdapterSearch = ArrayObjectAdapter(CardPresenter())
        val listRowAdapterConfig = ArrayObjectAdapter(CardPresenter())

        val headerTv = HeaderItem(0, rowTitleTv)

        listRowAdapterTv.add("TV en directo")
        listRowAdapterTv.add("Lista de canales")
        rowsAdapter.add(ListRow(headerTv, listRowAdapterTv))

        //val headerSearch = HeaderItem(1, rowTitleSearch)
        //listRowAdapterSearch.add("YouTube")
        //listRowAdapterSearch.add("Twitch")
        //rowsAdapter.add(ListRow(headerSearch, listRowAdapterSearch))


        val headerConfig = HeaderItem(2, rowTitleConfig)
        listRowAdapterConfig.add("Configuración")
        listRowAdapterConfig.add("Sincronizar con el servidor")
        rowsAdapter.add(ListRow(headerConfig, listRowAdapterConfig))

        adapter = rowsAdapter
        onItemViewClickedListener = this


    }

    override fun onItemClicked(
        itemViewHolder: Presenter.ViewHolder?,
        item: Any?,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Row?
    ) {
        if (item is String) {
            // Here we handle item clicks and navigate to another screen
            when (item) {
                "TV en directo" -> {
                    val intent = Intent(activity, MainActivity::class.java)
                    startActivity(intent)
                }
                "Sincronizar con el servidor" -> {
                    val channelViewModel: ChannelViewModel by viewModels()
                    channelViewModel.isLoadingChannelList.observe(this) { isLoading ->
                        if (isLoading == false) {
                            Toast.makeText(this.context, "Canal sincronizado", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

}

