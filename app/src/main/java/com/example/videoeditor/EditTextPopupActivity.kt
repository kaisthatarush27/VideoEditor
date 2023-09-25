package com.example.videoeditor

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import com.example.videoeditor.databinding.EditTextPopupBinding


class EditTextPopupActivity : BaseActivity() {
    private lateinit var binding: EditTextPopupBinding

    companion object {
        var textBitmap: Bitmap? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EditTextPopupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.editTv.findFocus()
        binding.textViewOutline.isDrawingCacheEnabled = true
        binding.editTv.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                binding.textViewOutline.text = s
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })

        binding.cancel.setOnClickListener {
            finish()
        }

        binding.apply.setOnClickListener {
            if (!binding.textViewOutline.text.toString()
                    .isEmpty() && !binding.textViewOutline.text.toString().replace(" ", "")
                    .isEmpty()
            ) {
                binding.textViewOutline.setText(binding.textViewOutline.text.toString())
                binding.textViewOutline.buildDrawingCache()
                textBitmap = Bitmap.createBitmap(binding.textViewOutline.drawingCache)
                val intent = Intent()
                setResult(RESULT_OK, intent)
                finish()
            }
        }
    }

}