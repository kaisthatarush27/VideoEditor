package com.example

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.videoeditor.R
import com.example.videoeditor.databinding.ActivityEmojiBinding

class EmojiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmojiBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmojiBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}