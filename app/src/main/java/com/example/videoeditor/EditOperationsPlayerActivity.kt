package com.example.videoeditor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.EmojiActivity
import com.example.videoeditor.databinding.ActivityEditOperationsPlayerBinding
import com.example.videoeditor.stickerView.StickerView
import com.example.videoeditor.stickerView.StickerView.OperationListener
import kotlin.math.roundToInt


class EditOperationsPlayerActivity : BaseActivity() {
    private lateinit var binding: ActivityEditOperationsPlayerBinding
    private lateinit var exoPlayer: ExoPlayer
    private var mViews: ArrayList<View>? = null
    private var mCurrentView: StickerView? = null
    private var mediaItem: MediaItem? = null
    private var imageUri: Uri? = null
    private var fetchVideoString: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditOperationsPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        exoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoPlayer

        fetchVideoString = intent.getStringExtra("inputVideoUri")
        if (fetchVideoString != null) {
            Log.d("fetchVideoUri", "onCreate:$fetchVideoString")

            mediaItem = MediaItem.fromUri(fetchVideoString!!.toUri())
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

        binding.insertEmojiLl.setOnClickListener {
            startActivity(Intent(this, EmojiActivity::class.java))
        }

        binding.insertImageLl.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                insertImage()
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), 1
                )
            }
        }

    }

    private fun insertImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imageLauncher.launch(intent)
    }

    private var imageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode != RESULT_OK) {
                return@registerForActivityResult
            }
            if (result.resultCode == RESULT_OK) {
                imageUri = result.data!!.data
                Log.d("imageUri", "imageUri: $imageUri")
                val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= 29) {
                    // To handle deprecation use
                    ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(
                            contentResolver,
                            imageUri!!
                        )
                    )
                } else {
                    // Use older version
                    MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                }

                Toast.makeText(this, "bitmap: $bitmap", Toast.LENGTH_LONG).show()


                val conf = Bitmap.Config.ARGB_4444

                var ww = bitmap.width
                if (ww < 550) {
                    ww = 550
                }
                var s = ww - bitmap.width
                if (s > 2) {
                    s /= 2
                }
                val dstBmp = Bitmap.createBitmap(ww, bitmap.height + 30, conf)
                val scaledBitmap = scaleDownImage(dstBmp, 400f)
                val bmOverlay = Bitmap.createBitmap(
                    scaledBitmap.width,
                    scaledBitmap.height,
                    scaledBitmap.config
                )
                val canvas = Canvas(bmOverlay)
                canvas.drawBitmap(dstBmp, Matrix(), null)
                canvas.drawBitmap(bitmap, s.toFloat(), 15f, null)

                addStickerView(bmOverlay)
            }
        }

    private fun scaleDownImage(
        realImage: Bitmap, maxImageHeight: Float
    ): Bitmap {
        return if (realImage.height <= maxImageHeight) {
            realImage
        } else {

            val ratio =
                (maxImageHeight / realImage.width).coerceAtMost(maxImageHeight / realImage.height)
            val width = (ratio * realImage.width).roundToInt()
            val height = (ratio * realImage.height).roundToInt()

            Bitmap.createScaledBitmap(
                realImage, width,
                height, true
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty()) {
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                        var isPermissionsGranted = false
                        if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                            isPermissionsGranted = true
                        }

                        if (isPermissionsGranted) {

                            insertImage()
                            Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
                        }
                    } else {

                        if (fetchVideoString != null) {
                            insertImage()
                        } else {
                            Toast.makeText(
                                this@EditOperationsPlayerActivity,
                                "Please upload video",
                                Toast.LENGTH_LONG
                            ).show()
                            return
                        }
                    }
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
                }
            }
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
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT
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