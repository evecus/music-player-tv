package com.tvmusic.app.ui.player

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import com.bumptech.glide.Glide
import com.tvmusic.app.R
import com.tvmusic.app.databinding.FragmentPlayerBinding
import com.tvmusic.app.media.player.PlayerController
import kotlinx.coroutines.launch

class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlayerViewModel by viewModels()
    private val playerController by lazy { PlayerController.getInstance(requireContext()) }

    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            progressHandler.postDelayed(this, 500)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentPlayerBinding.inflate(inflater, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Enable marquee scrolling for song title (replaces android:selected in XML)
        binding.tvTitle.isSelected = true
        setupControls()
        observePlayback()
        observeLyrics()
    }

    private fun setupControls() {
        binding.btnPlayPause.setOnClickListener { playerController.playPause() }
        binding.btnNext.setOnClickListener { playerController.next() }
        binding.btnPrev.setOnClickListener { playerController.previous() }
        binding.btnShuffle.setOnClickListener { playerController.toggleShuffle() }
        binding.btnRepeat.setOnClickListener { playerController.toggleRepeat() }

        // D-Pad focus chain for controls
        binding.btnPrev.nextFocusRightId = R.id.btnPlayPause
        binding.btnPlayPause.nextFocusRightId = R.id.btnNext
        binding.btnPlayPause.nextFocusLeftId = R.id.btnPrev
        binding.btnNext.nextFocusLeftId = R.id.btnPlayPause
        binding.btnShuffle.nextFocusRightId = R.id.btnRepeat
        binding.btnRepeat.nextFocusLeftId = R.id.btnShuffle

        // Seek bar: left/right d-pad seeks ±10s
        binding.seekBar.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        playerController.seekTo(playerController.getCurrentPosition() + 10000)
                        true
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                        playerController.seekTo(
                            (playerController.getCurrentPosition() - 10000).coerceAtLeast(0)
                        )
                        true
                    }
                    else -> false
                }
            } else false
        }
    }

    private fun observePlayback() {
        viewLifecycleOwner.lifecycleScope.launch {
            playerController.state.collect { state ->
                val song = state.currentSong

                // Title / artist
                binding.tvTitle.text = song?.title ?: "未在播放"
                binding.tvArtist.text = song?.artist ?: ""
                binding.tvAlbum.text = song?.album ?: ""
                binding.tvQuality.text = song?.qualityLabel ?: ""

                // Play/pause icon
                binding.btnPlayPause.setImageResource(
                    if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                )

                // Shuffle / repeat icon tint
                binding.btnShuffle.alpha = if (state.shuffleEnabled) 1f else 0.4f
                binding.btnRepeat.setImageResource(
                    when (state.repeatMode) {
                        Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one
                        Player.REPEAT_MODE_ALL -> R.drawable.ic_repeat_all
                        else -> R.drawable.ic_repeat_off
                    }
                )
                binding.btnRepeat.alpha = if (state.repeatMode != Player.REPEAT_MODE_OFF) 1f else 0.4f

                // Cover art
                if (song != null) {
                    loadCover(song.albumId, song.path)
                    viewModel.loadLyrics(song.path)
                } else {
                    binding.ivCover.setImageResource(R.drawable.ic_default_cover_large)
                }
            }
        }
    }

    private fun observeLyrics() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.lyrics.collect { lines ->
                if (lines.isEmpty()) {
                    binding.lyricsView.visibility = View.GONE
                    binding.tvNoLyrics.visibility = View.VISIBLE
                } else {
                    binding.lyricsView.visibility = View.VISIBLE
                    binding.tvNoLyrics.visibility = View.GONE
                    binding.lyricsView.setLyrics(lines)
                }
            }
        }
    }

    private fun loadCover(albumId: Long, path: String) {
        val albumArtUri = ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"), albumId
        )
        Glide.with(this)
            .load(albumArtUri)
            .placeholder(R.drawable.ic_default_cover_large)
            .error(R.drawable.ic_default_cover_large)
            .centerCrop()
            .into(binding.ivCover)
    }

    private fun updateProgress() {
        val state = playerController.state.value
        val pos = playerController.getCurrentPosition()
        val dur = state.durationMs
        if (dur > 0) {
            binding.seekBar.progress = ((pos.toFloat() / dur) * 1000).toInt()
        }
        binding.tvPosition.text = formatTime(pos)
        binding.tvDuration.text = formatTime(dur)

        // Sync lyrics highlight
        binding.lyricsView.updatePosition(pos)
    }

    private fun formatTime(ms: Long): String {
        val s = ms / 1000
        return "%d:%02d".format(s / 60, s % 60)
    }

    override fun onResume() {
        super.onResume()
        progressHandler.post(progressRunnable)
    }

    override fun onPause() {
        super.onPause()
        progressHandler.removeCallbacks(progressRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
