package com.tvmusic.app.ui.artists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.tvmusic.app.data.model.Artist
import com.tvmusic.app.databinding.FragmentArtistsBinding
import com.tvmusic.app.ui.list.SongListFragment
import kotlinx.coroutines.launch

class ArtistsFragment : Fragment() {

    private var _binding: FragmentArtistsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ArtistsViewModel by viewModels()
    private lateinit var adapter: ArtistAdapter

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentArtistsBinding.inflate(inflater, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ArtistAdapter { artist -> openArtistSongs(artist) }

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 4)
            adapter = this@ArtistsFragment.adapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.artists.collect { artists ->
                adapter.submitList(artists)
                binding.tvCount.text = "${artists.size} 位艺术家"
            }
        }
    }

    private fun openArtistSongs(artist: Artist) {
        parentFragmentManager.beginTransaction()
            .replace(com.tvmusic.app.R.id.contentFragment, ArtistSongsFragment.newInstance(artist.name))
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
