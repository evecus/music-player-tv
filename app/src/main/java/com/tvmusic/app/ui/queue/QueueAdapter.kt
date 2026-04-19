package com.tvmusic.app.ui.queue

import android.content.ContentUris
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tvmusic.app.R
import com.tvmusic.app.data.model.Song
import com.tvmusic.app.databinding.ItemQueueBinding

class QueueAdapter(
    private val onItemClick: (Song, Int) -> Unit
) : ListAdapter<Song, QueueAdapter.VH>(SongDiff) {

    private var currentPlayingIndex: Int = -1

    fun submitList(songs: List<Song>, currentIndex: Int) {
        currentPlayingIndex = currentIndex
        submitList(songs)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemQueueBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position), position, position == currentPlayingIndex)

    inner class VH(private val b: ItemQueueBinding) : RecyclerView.ViewHolder(b.root) {
        init {
            b.root.isFocusable = true
            b.root.setOnFocusChangeListener { _, hasFocus ->
                b.root.isActivated = hasFocus
                b.root.animate()
                    .scaleX(if (hasFocus) 1.03f else 1f)
                    .scaleY(if (hasFocus) 1.03f else 1f)
                    .setDuration(120).start()
            }
        }

        fun bind(song: Song, position: Int, isPlaying: Boolean) {
            b.tvIndex.text = (position + 1).toString()
            b.tvTitle.text = song.title
            b.tvArtist.text = song.artist
            b.tvDuration.text = song.durationFormatted
            b.ivNowPlaying.visibility =
                if (isPlaying) android.view.View.VISIBLE else android.view.View.GONE

            val albumUri = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"), song.albumId
            )
            Glide.with(b.ivCover.context)
                .load(albumUri)
                .placeholder(R.drawable.ic_default_cover)
                .error(R.drawable.ic_default_cover)
                .centerCrop()
                .into(b.ivCover)

            b.root.isSelected = isPlaying
            b.root.setOnClickListener { onItemClick(song, position) }
        }
    }

    object SongDiff : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(a: Song, b: Song) = a.id == b.id
        override fun areContentsTheSame(a: Song, b: Song) = a == b
    }
}
