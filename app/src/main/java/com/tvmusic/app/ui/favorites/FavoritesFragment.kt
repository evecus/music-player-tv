package com.tvmusic.app.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tvmusic.app.databinding.FragmentFavoritesBinding
import com.tvmusic.app.media.player.PlayerController
import com.tvmusic.app.ui.list.SongAdapter
import com.tvmusic.app.ui.main.MainActivity
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FavoritesViewModel by viewModels()
    private val playerController by lazy { PlayerController.getInstance(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = SongAdapter(
            onSongClick = { song, all ->
                playerController.playSong(song, all)
                (activity as? MainActivity)?.openPlayer()
            },
            onSongLongClick = { }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.favorites.collect { songs ->
                adapter.submitList(songs)
                binding.tvCount.text = "${songs.size} 首收藏"
                binding.tvEmpty.visibility = if (songs.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
