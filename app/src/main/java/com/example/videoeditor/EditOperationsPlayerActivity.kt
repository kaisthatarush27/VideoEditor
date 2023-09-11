package com.example.videoeditor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.videoeditor.databinding.ActivityEditOperationsPlayerBinding

class EditOperationsPlayerActivity : BaseActivity() {
    private lateinit var binding: ActivityEditOperationsPlayerBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditOperationsPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}