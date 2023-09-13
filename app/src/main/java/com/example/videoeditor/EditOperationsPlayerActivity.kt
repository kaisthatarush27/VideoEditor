package com.example.videoeditor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.videoeditor.databinding.ActivityEditOperationsPlayerBinding
import com.example.videoeditor.stickerView.StickerView
import com.example.videoeditor.stickerView.StickerView.OperationListener


class EditOperationsPlayerActivity : BaseActivity() {
    private lateinit var binding: ActivityEditOperationsPlayerBinding
    private lateinit var exoPlayer: ExoPlayer
    private var mViews: ArrayList<View>? = null
    private var mCurrentView: StickerView? = null
    private var mediaItem: MediaItem? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditOperationsPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        exoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoPlayer

        val fetchVideoString = intent.getStringExtra("inputVideoUri")
        if (fetchVideoString != null) {
            Log.d("fetchVideoUri", "onCreate:$fetchVideoString")

            mediaItem = MediaItem.fromUri(fetchVideoString.toUri())
            exoPlayer.setMediaItem(mediaItem!!)
            exoPlayer.prepare()
        }

        binding.insertTextLl.setOnClickListener {
            startActivityForResult(Intent(this, EditTextPopupActivity::class.java), 1)
        }

        val mParams: ViewGroup.LayoutParams = binding.captureLayout.layoutParams
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)


        mParams.height = metrics.widthPixels
        binding.captureLayout.layoutParams = mParams

        mViews = ArrayList()

        binding.saveButton.setOnClickListener {

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                var aab = Bitmap.createBitmap(EditTextPopupActivity.textBitmap!!)
                Log.d("eopa", "onActivityResult:$aab")
                aab = createTrimmedBitmap(aab)


                val conf = Bitmap.Config.ARGB_4444

                var ww = aab.width
                if (ww < 550) {
                    ww = 550
                }
                var s = ww - aab.width
                if (s > 2) {
                    s /= 2
                }
                val dstBmp = Bitmap.createBitmap(ww, aab.height + 30, conf)

                val bmOverlay = Bitmap.createBitmap(dstBmp.width, dstBmp.height, dstBmp.config)
                val canvas = Canvas(bmOverlay)
                canvas.drawBitmap(dstBmp, Matrix(), null)
                canvas.drawBitmap(aab, s.toFloat(), 15f, null)

                addStickerView(bmOverlay)
            }
        }
    }

    private fun addStickerView(bitmap: Bitmap) {
        val stickerView = StickerView(this)
        stickerView.bitmap = bitmap
        stickerView.setOperationListener(object : OperationListener {
            override fun onDeleteClick() {
                mViews!!.remove(stickerView)
                binding.captureLayout.removeView(stickerView)
            }

            override fun onEdit(stickerView: StickerView?) {

                //For coordinates location relative to the parent
                mCurrentView!!.setInEdit(false)
                mCurrentView = stickerView
                mCurrentView!!.setInEdit(true)
            }

            override fun onTop(stickerView: StickerView?) {
                Handler(Looper.getMainLooper()).post {
                    addStickerView(
                        stickerView!!.bitmap
                    )
                }
            }

        })

        val lp = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        binding.captureLayout.addView(stickerView, lp)
        mViews!!.add(stickerView)
        setCurrentEdit(stickerView)
    }

    private fun setCurrentEdit(stickerView: StickerView) {
        if (mCurrentView != null) {
            mCurrentView!!.setInEdit(false)
        }
        mCurrentView = stickerView
        stickerView.setInEdit(true)
    }

    private fun createTrimmedBitmap(bmp: Bitmap): Bitmap? {
        var bmp = bmp
        val imgHeight = bmp.height
        val imgWidth = bmp.width
        val smallX = 0
        val smallY = 0
        var left = imgWidth
        var right = imgWidth
        var top = imgHeight
        var bottom = imgHeight
        for (i in 0 until imgWidth) {
            for (j in 0 until imgHeight) {
                if (bmp.getPixel(i, j) != Color.TRANSPARENT) {
                    if (i - smallX < left) {
                        left = i - smallX
                    }
                    if (imgWidth - i < right) {
                        right = imgWidth - i
                    }
                    if (j - smallY < top) {
                        top = j - smallY
                    }
                    if (imgHeight - j < bottom) {
                        bottom = imgHeight - j
                    }
                }
            }
        }
        val iwx = imgWidth - left - right
        bmp = if (iwx > 0) {
            Bitmap.createBitmap(bmp, left, top, imgWidth - left - right, imgHeight - top - bottom)
        } else {
            val conf = Bitmap.Config.ARGB_4444
            Bitmap.createBitmap(512, 512, conf)
        }
        return bmp
    }
}