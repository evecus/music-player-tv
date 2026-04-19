package com.tvmusic.app.ui.artists

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tvmusic.app.data.model.Artist
import com.tvmusic.app.databinding.ItemArtistBinding

class ArtistAdapter(
    private val onClick: (Artist) -> Unit
) : ListAdapter<Artist, ArtistAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemArtistBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemArtistBinding) : RecyclerView.ViewHolder(b.root) {
        init {
            b.root.isFocusable = true
            b.root.setOnFocusChangeListener { _, hasFocus ->
                b.root.isActivated = hasFocus
                b.root.animate()
                    .scaleX(if (hasFocus) 1.08f else 1f)
                    .scaleY(if (hasFocus) 1.08f else 1f)
                    .setDuration(130)
                    .start()
            }
        }

        fun bind(artist: Artist) {
            b.tvArtistName.text = artist.name
            b.tvSongCount.text = "${artist.songCount} 首歌曲"
            b.tvAlbumCount.text = "${artist.albumCount} 张专辑"
            // First letter avatar
            b.tvAvatar.text = artist.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            b.root.setOnClickListener { onClick(artist) }
        }
    }

    object Diff : DiffUtil.ItemCallback<Artist>() {
        override fun areItemsTheSame(a: Artist, b: Artist) = a.name == b.name
        override fun areContentsTheSame(a: Artist, b: Artist) = a == b
    }
}
