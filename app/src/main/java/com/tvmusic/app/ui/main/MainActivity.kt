package com.tvmusic.app.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.tvmusic.app.R
import com.tvmusic.app.data.repository.MusicRepository
import com.tvmusic.app.databinding.ActivityMainBinding
import com.tvmusic.app.media.player.PlayerController
import com.tvmusic.app.ui.artists.ArtistsFragment
import com.tvmusic.app.ui.favorites.FavoritesFragment
import com.tvmusic.app.ui.list.SongListFragment
import com.tvmusic.app.ui.player.PlayerFragment
import com.tvmusic.app.ui.queue.QueueFragment
import com.tvmusic.app.ui.search.SearchFragment
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val repository by lazy { MusicRepository.getInstance(this) }
    private val playerController by lazy { PlayerController.getInstance(this) }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            startScan()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSidebar()
        checkPermissionsAndScan()

        if (savedInstanceState == null) {
            navigateTo(NavItem.SONGS)
        }
    }

    private fun setupSidebar() {
        // Set icons and labels
        configNavButton(binding.navSongs.root,     R.drawable.ic_music_note,      "歌曲")
        configNavButton(binding.navArtists.root,   R.drawable.ic_person,          "艺术家")
        configNavButton(binding.navFavorites.root, R.drawable.ic_favorite_filled, "收藏")
        configNavButton(binding.navQueue.root,     R.drawable.ic_queue_music,     "播放队列")
        configNavButton(binding.navSearch.root,    R.drawable.ic_search,          "搜索")

        binding.navSongs.root.setOnClickListener     { navigateTo(NavItem.SONGS) }
        binding.navArtists.root.setOnClickListener   { navigateTo(NavItem.ARTISTS) }
        binding.navFavorites.root.setOnClickListener { navigateTo(NavItem.FAVORITES) }
        binding.navQueue.root.setOnClickListener     { navigateTo(NavItem.QUEUE) }
        binding.navSearch.root.setOnClickListener    { navigateTo(NavItem.SEARCH) }

        // D-Pad vertical chain between nav items
        binding.navSongs.root.nextFocusDownId      = R.id.navArtists
        binding.navArtists.root.nextFocusUpId      = R.id.navSongs
        binding.navArtists.root.nextFocusDownId    = R.id.navFavorites
        binding.navFavorites.root.nextFocusUpId    = R.id.navArtists
        binding.navFavorites.root.nextFocusDownId  = R.id.navQueue
        binding.navQueue.root.nextFocusUpId        = R.id.navFavorites
        binding.navQueue.root.nextFocusDownId      = R.id.navSearch
        binding.navSearch.root.nextFocusUpId       = R.id.navQueue

        // D-Pad right → jump to content area
        binding.navSongs.root.nextFocusRightId     = R.id.contentFragment
        binding.navArtists.root.nextFocusRightId   = R.id.contentFragment
        binding.navFavorites.root.nextFocusRightId = R.id.contentFragment
        binding.navQueue.root.nextFocusRightId     = R.id.contentFragment
        binding.navSearch.root.nextFocusRightId    = R.id.contentFragment

        // Mini player updates
        lifecycleScope.launch {
            PlayerController.getInstance(this@MainActivity).state.collect { state ->
                binding.miniTitle.text  = state.currentSong?.title  ?: "未在播放"
                binding.miniArtist.text = state.currentSong?.artist ?: ""
            }
        }
    }

    private fun configNavButton(view: android.view.View, iconRes: Int, label: String) {
        view.findViewById<android.widget.ImageView>(R.id.navIcon)?.setImageResource(iconRes)
        view.findViewById<android.widget.TextView>(R.id.navLabel)?.text = label
    }

    fun navigateTo(item: NavItem) {
        updateSidebarSelection(item)
        val fragment: Fragment = when (item) {
            NavItem.SONGS -> SongListFragment()
            NavItem.ARTISTS -> ArtistsFragment()
            NavItem.FAVORITES -> FavoritesFragment()
            NavItem.QUEUE -> QueueFragment()
            NavItem.SEARCH -> SearchFragment()
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentFragment, fragment)
            .commit()
    }

    fun openPlayer(animate: Boolean = true) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentFragment, PlayerFragment())
            .addToBackStack("player")
            .commit()
    }

    private fun updateSidebarSelection(item: NavItem) {
        listOf(
            binding.navSongs.root, binding.navArtists.root,
            binding.navFavorites.root, binding.navQueue.root, binding.navSearch.root
        ).forEach { it.isSelected = false }

        when (item) {
            NavItem.SONGS -> binding.navSongs.root.isSelected = true
            NavItem.ARTISTS -> binding.navArtists.root.isSelected = true
            NavItem.FAVORITES -> binding.navFavorites.root.isSelected = true
            NavItem.QUEUE -> binding.navQueue.root.isSelected = true
            NavItem.SEARCH -> binding.navSearch.root.isSelected = true
        }
    }

    private fun checkPermissionsAndScan() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasPermission(Manifest.permission.READ_MEDIA_AUDIO))
                needed.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            if (!hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE))
                needed.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (needed.isEmpty()) startScan() else permissionLauncher.launch(needed.toTypedArray())
    }

    private fun hasPermission(perm: String) =
        ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED

    private fun startScan() {
        lifecycleScope.launch {
            repository.scanLibrary()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Global media key handling
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                playerController.playPause(); true
            }
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                playerController.next(); true
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                playerController.previous(); true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    enum class NavItem { SONGS, ARTISTS, FAVORITES, QUEUE, SEARCH }
}
