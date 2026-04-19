package com.tvmusic.app.ui.list

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tvmusic.app.data.model.Song

class SongOptionsDialog : DialogFragment() {

    companion object {
        private const val TAG = "SongOptionsDialog"
        private const val ARG_TITLE = "title"
        private const val ARG_ARTIST = "artist"
        private const val ARG_SONG_ID = "songId"

        private var onAddToQueue: ((Song) -> Unit)? = null
        private var onToggleFavorite: ((Song) -> Unit)? = null
        private var targetSong: Song? = null

        fun show(
            fragmentManager: FragmentManager,
            song: Song,
            onAddToQueue: (Song) -> Unit,
            onToggleFavorite: (Song) -> Unit
        ) {
            this.onAddToQueue = onAddToQueue
            this.onToggleFavorite = onToggleFavorite
            targetSong = song

            SongOptionsDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, song.title)
                    putString(ARG_ARTIST, song.artist)
                }
            }.show(fragmentManager, TAG)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = arguments?.getString(ARG_TITLE) ?: ""
        val artist = arguments?.getString(ARG_ARTIST) ?: ""
        val song = targetSong ?: return super.onCreateDialog(savedInstanceState)

        val items = arrayOf(
            "添加到队列",
            if (song.isFavorite) "取消收藏" else "收藏",
            "取消"
        )

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("$title\n$artist")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> onAddToQueue?.invoke(song)
                    1 -> onToggleFavorite?.invoke(song)
                    2 -> dismiss()
                }
            }
            .create()
    }
}
