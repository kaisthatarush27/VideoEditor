package com.example.videoeditor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
import com.bumptech.glide.Glide
import com.example.EmojiActivity
import com.example.videoeditor.collageView.MultiTouchListener
import com.example.videoeditor.databinding.ActivityEditOperationsPlayerBinding
import com.example.videoeditor.stickerView.StickerView
import com.example.videoeditor.stickerView.StickerView.OperationListener
import java.io.FileNotFoundException


class EditOperationsPlayerActivity : BaseActivity() {
    private lateinit var binding: ActivityEditOperationsPlayerBinding
    private lateinit var exoPlayer: ExoPlayer
    private var mViews: ArrayList<View>? = null
    private var mCurrentView: StickerView? = null
    private var mediaItem: MediaItem? = null
    private var imageUri: Uri? = null
    private var fetchVideoString: String? = null
    private var gifUri: Uri? = null
    var gifFilePath: String? = null
    var filePath: String? = null
    private val REQUEST_CODE_GIF_PERMISSION = 2
    private val REQUEST_CODE_IMAGE_PERMISSION = 1
    private var selectedImageUri: Uri? = null
    private var pathString: String? = null
    private var isImage = false
    private var IMAGE_OR_GIF = 1
//    private var GIF = 2
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
            startActivityForResult(Intent(this, EmojiActivity::class.java), 2)
        }

        binding.insertImageLl.setOnClickListener {
            this.IMAGE_OR_GIF = 2
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

        binding.insertGifLl.setOnClickListener {
            this.IMAGE_OR_GIF = 3
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                insertGif()
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

    private fun insertGif() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        val mimetypes = arrayOf("image/gif", "image/webp")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
        startActivityForResult(intent, 3)
    }

    private fun insertImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "*/*"
        val mimetypes = arrayOf("image/jpeg", "image/jpg", "image/png")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
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
                val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)

                Toast.makeText(this, "bitmap: $bitmap", Toast.LENGTH_LONG).show()

                var ww = bitmap.width
                if (ww < 550) {
                    ww = 550
                }
                var s = ww - bitmap.width
                if (s > 2) {
                    s /= 2
                }
                val dstBmp = Bitmap.createBitmap(ww, bitmap.height + 30, Bitmap.Config.ARGB_8888)
                val bmOverlay = Bitmap.createBitmap(
                    dstBmp.width, dstBmp.height, dstBmp.config
                )
                val canvas = Canvas(bmOverlay)
                Log.d("eoa", "hwa:${canvas.isHardwareAccelerated}")
                canvas.drawBitmap(dstBmp, Matrix(), null)
                canvas.drawBitmap(bitmap, s.toFloat(), 15f, null)
                addStickerView(bmOverlay)
            }
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

                            if (this.IMAGE_OR_GIF == 2) {
                                insertImage()
                            } else {
                                insertGif()
                            }
                            Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
                        }
                    } else {

                        if (fetchVideoString != null) {
                            if (this.IMAGE_OR_GIF == 2) {
                                insertImage()
                            } else  {
                                insertGif()
                            }
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
        } else if (requestCode == 2) {
            if (resultCode == RESULT_OK) {
                var aab = Bitmap.createBitmap(EmojiActivity.emojiTextBitmap!!)
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
        } else if (requestCode == 3) {
            if (resultCode == RESULT_OK) {
                gifUri = data!!.data  // gets the uri to that file which contains the data
                Log.d("gifuri", "gifUri: $gifUri")
                try {
                    contentResolver.openInputStream(gifUri!!).use {
                        val gifData = it!!.readBytes()
                        Glide.with(this).load(gifData).into(binding.collageView)
                        binding.collageView.setOnTouchListener(MultiTouchListener())
                        it.close()
                    }

                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }
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