package com.tvmusic.app.ui.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tvmusic.app.R
import com.tvmusic.app.data.model.Song
import com.tvmusic.app.data.model.SortOrder
import com.tvmusic.app.databinding.FragmentSongListBinding
import com.tvmusic.app.media.player.PlayerController
import com.tvmusic.app.ui.main.MainActivity
import kotlinx.coroutines.launch

class SongListFragment : Fragment() {

    private var _binding: FragmentSongListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SongListViewModel by viewModels()
    private lateinit var adapter: SongAdapter
    private val playerController by lazy { PlayerController.getInstance(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentSongListBinding.inflate(inflater, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSortMenu()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = SongAdapter(
            onSongClick = { song, allSongs -> playSong(song, allSongs) },
            onSongLongClick = { song -> showSongOptions(song) }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SongListFragment.adapter
            setHasFixedSize(true)
            // TV: preserve focus
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }
    }

    private fun setupSortMenu() {
        val sortItems = listOf(
            R.id.sort_title_asc to SortOrder.TITLE_ASC,
            R.id.sort_title_desc to SortOrder.TITLE_DESC,
            R.id.sort_artist to SortOrder.ARTIST_ASC,
            R.id.sort_album to SortOrder.ALBUM_ASC,
            R.id.sort_date_new to SortOrder.DATE_ADDED_DESC,
            R.id.sort_date_old to SortOrder.DATE_ADDED_ASC,
            R.id.sort_duration_long to SortOrder.DURATION_DESC,
            R.id.sort_size to SortOrder.FILE_SIZE_DESC
        )

        binding.btnSort.setOnClickListener {
            val popup = androidx.appcompat.widget.PopupMenu(requireContext(), binding.btnSort)
            popup.menuInflater.inflate(R.menu.menu_sort, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                sortItems.firstOrNull { it.first == item.itemId }?.let { (_, order) ->
                    viewModel.setSortOrder(order)
                    true
                } ?: false
            }
            popup.show()
        }

        binding.btnSort.nextFocusDownId = R.id.recyclerView
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.songs.collect { songs ->
                adapter.submitList(songs)
                binding.tvEmpty.visibility = if (songs.isEmpty()) View.VISIBLE else View.GONE
                binding.tvCount.text = "${songs.size} 首歌曲"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sortOrder.collect { order ->
                binding.tvSortLabel.text = order.displayName()
            }
        }
    }

    private fun playSong(song: Song, allSongs: List<Song>) {
        playerController.playSong(song, allSongs)
        (activity as? MainActivity)?.openPlayer()
    }

    private fun showSongOptions(song: Song) {
        SongOptionsDialog.show(
            fragmentManager = childFragmentManager,
            song = song,
            onAddToQueue = { playerController.addToQueue(it) },
            onToggleFavorite = {
                lifecycleScope.launch { viewModel.toggleFavorite(it) }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun SortOrder.displayName() = when (this) {
        SortOrder.TITLE_ASC -> "标题 A→Z"
        SortOrder.TITLE_DESC -> "标题 Z→A"
        SortOrder.ARTIST_ASC -> "艺术家"
        SortOrder.ARTIST_DESC -> "艺术家 Z→A"
        SortOrder.ALBUM_ASC -> "专辑"
        SortOrder.DATE_ADDED_DESC -> "最新添加"
        SortOrder.DATE_ADDED_ASC -> "最早添加"
        SortOrder.DURATION_DESC -> "时长最长"
        SortOrder.DURATION_ASC -> "时长最短"
        SortOrder.FILE_SIZE_DESC -> "文件大小"
    }
}
