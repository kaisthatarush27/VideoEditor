package com.example.videoeditor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.videoeditor.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar


class SelectVideoActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private var input_video_uri: String? = null
    private var fadeIn: Animation? = null
    private var fadeOut: Animation? = null
    private val handler: Handler = Handler()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.selectVideoLl.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                selectVideo()
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), 1
                )
            }
        }

        fadeIn = AlphaAnimation(0.6f, 1f)
        (fadeIn as AlphaAnimation).duration = 1000


        fadeOut = AlphaAnimation(1f, 0.6f)
        (fadeOut as AlphaAnimation).duration = 1000

        startAnimation()

    }

    private fun startAnimation() {
        binding.welcomeTv.startAnimation(fadeIn)
        handler.postDelayed({
            binding.welcomeTv.startAnimation(fadeOut)
            handler.postDelayed({
                startAnimation()
            }, fadeOut!!.duration)
        }, fadeIn!!.duration)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty()) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        var isPermissionsGranted = false
                        if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                            isPermissionsGranted = true
                        }

                        if (isPermissionsGranted) {
                            selectVideo()
                            val permissionGrantedSnackBar =
                                Snackbar.make(
                                    binding.root,
                                    "Permission Granted",
                                    Snackbar.LENGTH_LONG
                                )
                            permissionGrantedSnackBar.show()
                        } else {
                            val permissionDeniedSnackBar =
                                Snackbar.make(
                                    binding.root,
                                    "Permission Denied",
                                    Snackbar.LENGTH_LONG
                                )
                            permissionDeniedSnackBar.show()
                        }
                    } else {
                        selectVideo()
                    }
                } else {
                    val permissionDeniedSnackBar =
                        Snackbar.make(
                            binding.root,
                            "Permission Denied",
                            Snackbar.LENGTH_LONG
                        )
                    permissionDeniedSnackBar.show()
                }
            }
        }
    }

    private val selectVideoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            it?.let {
                input_video_uri = it.toString()
                Log.d("mauri", "uri:$input_video_uri")

                val sendVideoIntentToEditVideoOperationsActivity =
                    Intent(this, EditOperationsPlayerActivity::class.java)
                sendVideoIntentToEditVideoOperationsActivity.putExtra(
                    "inputVideoUri",
                    input_video_uri
                )
                startActivity(sendVideoIntentToEditVideoOperationsActivity)
            }
        }

    private fun selectVideo() {
        selectVideoLauncher.launch("video/*")
    }
}