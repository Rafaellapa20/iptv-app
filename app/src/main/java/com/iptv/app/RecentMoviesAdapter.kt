package com.iptv.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RecentMoviesAdapter(
    private val list: List<Stream>,
    private val onClick: (Stream) -> Unit
) : RecyclerView.Adapter<RecentMoviesAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val ivFavorite: ImageView = view.findViewById(R.id.ivFavorite)

        init {
            view.setOnClickListener { onClick(list[bindingAdapterPosition]) }
            
            ivFavorite.setOnClickListener {
                val stream = list[bindingAdapterPosition]
                val context = it.context
                val isNowFavorite = FavoritesManager.toggleFavorite(context, stream)
                updateFavoriteIcon(ivFavorite, isNowFavorite)
            }
        }
    }

    private fun updateFavoriteIcon(ivFavorite: ImageView, isFavorite: Boolean) {
        if (isFavorite) {
            ivFavorite.setImageResource(android.R.drawable.btn_star_big_on)
        } else {
            ivFavorite.setImageResource(android.R.drawable.btn_star_big_off)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val stream = list[position]
        holder.tvName.text = stream.name

        val isFav = FavoritesManager.isFavorite(holder.itemView.context, stream.stream_id)
        updateFavoriteIcon(holder.ivFavorite, isFav)

        Glide.with(holder.itemView.context)
            .load(stream.stream_icon)
            .placeholder(android.R.drawable.ic_media_play)
            .error(android.R.drawable.ic_media_play)
            .into(holder.ivIcon)
    }

    override fun getItemCount() = list.size
}
