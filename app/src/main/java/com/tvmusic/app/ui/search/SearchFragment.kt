package com.tvmusic.app.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tvmusic.app.databinding.FragmentSearchBinding
import com.tvmusic.app.media.player.PlayerController
import com.tvmusic.app.ui.list.SongAdapter
import com.tvmusic.app.ui.main.MainActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private val playerController by lazy { PlayerController.getInstance(requireContext()) }
    private var debounceJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, c, false)
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

        // Search input - debounce 300ms
        binding.etSearch.doAfterTextChanged { text ->
            debounceJob?.cancel()
            debounceJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                viewModel.search(text?.toString() ?: "")
            }
        }

        // Focus: D-Pad down from search bar → results
        binding.etSearch.nextFocusDownId = com.tvmusic.app.R.id.recyclerView

        // Auto-focus search box
        binding.etSearch.requestFocus()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.results.collect { songs ->
                adapter.submitList(songs)
                binding.tvCount.text = if (songs.isEmpty()) "无结果" else "找到 ${songs.size} 首"
                binding.tvEmpty.visibility =
                    if (songs.isEmpty() && binding.etSearch.text.isNotEmpty())
                        View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
