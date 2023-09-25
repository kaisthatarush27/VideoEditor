package com.example

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.videoeditor.R
import com.example.videoeditor.databinding.ActivityEmojiBinding
import com.vanniktech.emoji.EmojiPopup
import java.util.regex.Pattern

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
        binding.etEmoji.filters = arrayOf(EmojiInputFilter())
        binding.etEmoji.inputType = InputType.TYPE_NULL
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

class EmojiInputFilter : InputFilter {

    // Define a regular expression pattern for emoji characters
    private val emojiPattern = Pattern.compile(
        "[\\x{1F600}-\\x{1F64F}" +  // Emoticons
                "\\x{1F300}-\\x{1F5FF}" +  // Misc Symbols and Pictographs
                "\\x{1F680}-\\x{1F6FF}" +  // Transport and Map Symbols
                "\\x{1F700}-\\x{1F77F}" +  // Alchemical Symbols
                "\\x{1F780}-\\x{1F7FF}" +  // Geometric Shapes Extended
                "\\x{1F800}-\\x{1F8FF}" +  // Supplemental Arrows-C
                "\\x{1F900}-\\x{1F9FF}" +  // Supplemental Symbols and Pictographs
                "\\x{1FA00}-\\x{1FA6F}" +  // Chess Symbols
                "\\x{1FA70}-\\x{1FAFF}" +  // Symbols and Pictographs Extended-A
                "\\x{1F004}-\\x{1F0CF}" +  // Playing Cards
                "\\x{1F170}-\\x{1F251}" +  // Enclosed Ideographic Supplement
                "]"
    )

    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        // Check if the entered text contains only emoji characters
        if (source != null && !emojiPattern.matcher(source).matches()) {
            return ""
        }
        return null // Allow the input
    }
}