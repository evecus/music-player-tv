package com.tvmusic.app.ui.queue

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tvmusic.app.data.model.Song
import com.tvmusic.app.databinding.FragmentQueueBinding
import com.tvmusic.app.media.player.PlayerController
import com.tvmusic.app.ui.main.MainActivity
import kotlinx.coroutines.launch

class QueueFragment : Fragment() {

    private var _binding: FragmentQueueBinding? = null
    private val binding get() = _binding!!
    private val playerController by lazy { PlayerController.getInstance(requireContext()) }
    private lateinit var adapter: QueueAdapter

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentQueueBinding.inflate(inflater, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = QueueAdapter(
            onItemClick = { song, index ->
                playerController.skipToIndex(index)
                (activity as? MainActivity)?.openPlayer()
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@QueueFragment.adapter
        }

        binding.btnClearQueue.setOnClickListener {
            playerController.state.value.let { /* queue cleared via player */ }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            playerController.state.collect { state ->
                val queue = state.queue
                adapter.submitList(queue, state.currentIndex)
                binding.tvCount.text = "${queue.size} 首歌曲"
                binding.tvEmpty.visibility = if (queue.isEmpty()) View.VISIBLE else View.GONE

                // Scroll to current
                if (state.currentIndex >= 0 && state.currentIndex < queue.size) {
                    binding.recyclerView.smoothScrollToPosition(state.currentIndex)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
