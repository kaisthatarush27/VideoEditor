package com.example

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.videoeditor.R
import com.example.videoeditor.databinding.ActivityEmojiBinding
import com.vanniktech.emoji.EmojiPopup

class EmojiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmojiBinding

    companion object {
        var emojiTextBitmap: Bitmap? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmojiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val emojiPopup =
            EmojiPopup.Builder.fromRootView(binding.insertEmojiRl).build(binding.etEmoji)
        binding.btnEmojis.setOnClickListener {
            emojiPopup.toggle()
        }

        binding.etEmoji.findFocus()
        binding.etEmoji.isDrawingCacheEnabled = true

        binding.cancel.setOnClickListener {
            finish()
        }

        binding.etEmoji.buildDrawingCache()
        binding.apply.setOnClickListener {
            if (!binding.etEmoji.text!!.toString().isEmpty()) {
                binding.etEmoji.background =
                    ContextCompat.getDrawable(this, R.drawable.edit_text_bg_none)
                binding.etEmoji.isCursorVisible = false
                emojiTextBitmap =
                    Bitmap.createBitmap(binding.etEmoji.drawingCache)
                Log.d("ea", "onCreate:$emojiTextBitmap")
            } else {
                Toast.makeText(this, "Please insert emojis", Toast.LENGTH_SHORT).show()
            }

            val intent = Intent()
            setResult(RESULT_OK, intent)
            finish()
        }
    }
}