package com.tvmusic.app.ui.list

import android.content.ContentUris
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.tvmusic.app.R
import com.tvmusic.app.data.model.Song
import com.tvmusic.app.databinding.ItemSongBinding

class SongAdapter(
    private val onSongClick: (Song, List<Song>) -> Unit,
    private val onSongLongClick: (Song) -> Unit
) : ListAdapter<Song, SongAdapter.SongViewHolder>(SongDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(getItem(position), currentList)
    }

    inner class SongViewHolder(private val b: ItemSongBinding) :
        RecyclerView.ViewHolder(b.root) {

        init {
            b.root.isFocusable = true
            b.root.isFocusableInTouchMode = false

            b.root.setOnFocusChangeListener { _, hasFocus ->
                b.root.isActivated = hasFocus
                b.root.animate()
                    .scaleX(if (hasFocus) 1.04f else 1f)
                    .scaleY(if (hasFocus) 1.04f else 1f)
                    .setDuration(120)
                    .start()
            }
        }

        fun bind(song: Song, allSongs: List<Song>) {
            b.tvTitle.text = song.title
            b.tvArtist.text = song.artist
            b.tvDuration.text = song.durationFormatted

            // Quality badge
            b.tvQuality.text = song.qualityLabel
            b.tvQuality.setBackgroundColor(song.qualityColor)

            // Favorite icon
            b.ivFavorite.visibility =
                if (song.isFavorite) android.view.View.VISIBLE else android.view.View.GONE

            // Cover art
            loadCover(song)

            b.root.setOnClickListener { onSongClick(song, allSongs) }
            b.root.setOnLongClickListener { onSongLongClick(song); true }
        }

        private fun loadCover(song: Song) {
            if (!song.hasCover) {
                b.ivCover.setImageResource(R.drawable.ic_default_cover)
                return
            }

            // Try embedded art via MediaStore album art URI
            val albumArtUri = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                song.albumId
            )

            Glide.with(b.ivCover.context)
                .load(albumArtUri)
                .placeholder(R.drawable.ic_default_cover)
                .error(R.drawable.ic_default_cover)
                .transition(DrawableTransitionOptions.withCrossFade(150))
                .centerCrop()
                .into(b.ivCover)
        }
    }

    object SongDiff : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(a: Song, b: Song) = a.id == b.id
        override fun areContentsTheSame(a: Song, b: Song) = a == b
    }
}
