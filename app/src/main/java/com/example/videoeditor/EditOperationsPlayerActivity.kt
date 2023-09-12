package com.example.videoeditor

import android.os.Bundle
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.videoeditor.databinding.ActivityEditOperationsPlayerBinding

class EditOperationsPlayerActivity : BaseActivity() {
    private lateinit var binding: ActivityEditOperationsPlayerBinding
    private lateinit var exoPlayer: ExoPlayer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditOperationsPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        exoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoPlayer

        val fetchVideoString = intent.getStringExtra("inputVideoUri")
        Log.d("fetchVideoUri", "onCreate:$fetchVideoString")

        val mediaItem = MediaItem.fromUri(fetchVideoString!!.toUri())
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

    }
}