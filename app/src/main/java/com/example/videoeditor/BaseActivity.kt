package com.example.videoeditor

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            window.statusBarColor = Color.parseColor("#ff2d9a59")
        }
    }
}