package com.tvmusic.app.ui.artists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tvmusic.app.data.repository.MusicRepository
import com.tvmusic.app.databinding.FragmentArtistSongsBinding
import com.tvmusic.app.media.player.PlayerController
import com.tvmusic.app.ui.list.SongAdapter
import com.tvmusic.app.ui.main.MainActivity
import kotlinx.coroutines.launch

class ArtistSongsFragment : Fragment() {

    companion object {
        private const val ARG_ARTIST = "artist"
        fun newInstance(artist: String) = ArtistSongsFragment().apply {
            arguments = Bundle().apply { putString(ARG_ARTIST, artist) }
        }
    }

    private var _binding: FragmentArtistSongsBinding? = null
    private val binding get() = _binding!!
    private val repo by lazy { MusicRepository.getInstance(requireContext()) }
    private val playerController by lazy { PlayerController.getInstance(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentArtistSongsBinding.inflate(inflater, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val artistName = arguments?.getString(ARG_ARTIST) ?: return

        binding.tvArtistName.text = artistName

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnBack.nextFocusRightId = com.tvmusic.app.R.id.recyclerView

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
            val songs = repo.getSongsByArtist(artistName)
            adapter.submitList(songs)
            binding.tvCount.text = "${songs.size} 首歌曲"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
