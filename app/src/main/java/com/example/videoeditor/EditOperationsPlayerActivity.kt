package com.example.videoeditor

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.TextureOverlay
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.Transformer
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.bumptech.glide.Glide
import com.example.EmojiActivity
import com.example.videoeditor.collageView.MultiTouchListener
import com.example.videoeditor.databinding.ActivityEditOperationsPlayerBinding
import com.example.videoeditor.stickerView.StickerView
import com.example.videoeditor.stickerView.StickerView.OperationListener
import com.example.videoeditor.utils.Utils
import com.google.android.material.snackbar.Snackbar
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

@UnstableApi
class EditOperationsPlayerActivity : BaseActivity() {
    private lateinit var binding: ActivityEditOperationsPlayerBinding
    private lateinit var exoPlayer: ExoPlayer
    private var mViews: ArrayList<View>? = null
    private var mCurrentView: StickerView? = null
    private var mediaItem: MediaItem? = null
    private var imageUri: Uri? = null
    private var imagePath: String? = null
    private var fetchVideoString: String? = null
    private var gifUri: Uri? = null
    var filePath: String? = null
    private var IMAGE_OR_GIF = 1
    private var outputFilePath: String? = null
    private var gifData: ByteArray? = null
    private var input_video_uri_ffmpeg: String? = null
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
            this.IMAGE_OR_GIF = 2
            startActivityForResult(Intent(this, EditTextPopupActivity::class.java), 1)
        }

        val mParams: ViewGroup.LayoutParams = binding.captureLayout.layoutParams
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)


        mParams.height = metrics.widthPixels
        binding.captureLayout.layoutParams = mParams

        mViews = ArrayList()

        binding.saveButton.setOnClickListener {

            when (this.IMAGE_OR_GIF) {
                2 -> {
                    mediaThreeTransformations()
                }
                3 -> {
                    ffmpegGifTransformation()
                }
                else -> {
                    val editOperationsSnackBar =
                        Snackbar.make(
                            binding.root,
                            "Please select the edit operations",
                            Snackbar.LENGTH_LONG
                        )
                    editOperationsSnackBar.show()
                }
            }
        }

        binding.insertEmojiLl.setOnClickListener {
            this.IMAGE_OR_GIF = 2
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

    private fun ffmpegGifTransformation() {
//        if (gifData!!.isEmpty()) {
//            return
//        }
        val gifPath = saveGif(gifData!!)
        Log.d("eopa", "gifFilePath:$gifPath")
        val gifFileUri = Uri.fromFile(File(gifPath!!))
        Log.d("eopa", "gifFileUri:$gifFileUri")
        outputFilePath = getOutputFilePath()

        input_video_uri_ffmpeg =
            FFmpegKitConfig.getSafParameterForRead(this, Uri.parse(fetchVideoString!!))
        Log.d("eopa", "input_video_uri_ffmpeg:$input_video_uri_ffmpeg")


        Log.d("eopa", "collageview:${binding.collageView.x} ${binding.collageView.y}")
        val command =
            "-y -i $input_video_uri_ffmpeg -stream_loop -1 -i $gifFileUri -filter_complex [0]overlay=x=${binding.collageView.x}:y=${binding.collageView.y}:shortest=1[out] -map [out] -map 0:a? $outputFilePath"
        executeFfmpegCommand(command, outputFilePath!!)
    }

    private fun mediaThreeTransformations() {
        mediaItem = MediaItem.fromUri(fetchVideoString!!.toUri())
        createTransformation(mediaItem!!)
    }

    private fun insertGif() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        val mimetypes = arrayOf("image/gif")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
        startActivityForResult(intent, 3)
    }

    private fun insertImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
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

                Log.d("eopa", "bitmap: $bitmap")

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
                            } else {
                                insertGif()
                            }
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
                var textBitmap = Bitmap.createBitmap(EditTextPopupActivity.textBitmap!!)
                Log.d("eopa", "onActivityResult:$textBitmap")
                textBitmap = createTrimmedBitmap(textBitmap)


                val conf = Bitmap.Config.ARGB_4444

                var ww = textBitmap.width
                if (ww < 550) {
                    ww = 550
                }
                var s = ww - textBitmap.width
                if (s > 2) {
                    s /= 2
                }
                val dstBmp = Bitmap.createBitmap(ww, textBitmap.height + 30, conf)

                val bmOverlay = Bitmap.createBitmap(dstBmp.width, dstBmp.height, dstBmp.config)
                val canvas = Canvas(bmOverlay)
                canvas.drawBitmap(dstBmp, Matrix(), null)
                canvas.drawBitmap(textBitmap, s.toFloat(), 15f, null)

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
                        gifData = it!!.readBytes()
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

    private fun createTransformation(mediaItem: MediaItem) {
        val inputEditedMediaItem = EditedMediaItem.Builder(mediaItem).setEffects(
            Effects(listOf(), createVideoEffects())
        ).build()
        val transformer = transformerBuilder()
        filePath = getOutputFilePath()
        Log.d("eopa", "filePath:$filePath")
        transformer.start(inputEditedMediaItem, filePath!!)
    }

    private fun executeFfmpegCommand(exe: String, filePath: String) {

        val progressDialog = ProgressDialog(this@EditOperationsPlayerActivity)
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()
        FFmpegKit.executeAsync(exe, { session ->
            val returnCode = session.returnCode
            lifecycleScope.launch(Dispatchers.Main) {
                if (returnCode.isValueSuccess) {
                    Log.d("ffmpeg", "execFilePath: $filePath")
                    outputFilePath = filePath
                    Log.d("ffmpeg", "execInputVideoUri: $fetchVideoString")
                    progressDialog.dismiss()

                    val filterAppliedSnackbar =
                        Snackbar.make(binding.root, "Filter Applied", Snackbar.LENGTH_LONG)
                    filterAppliedSnackbar.show()
                } else {
                    progressDialog.dismiss()
                    Log.d("TAG", session.allLogsAsString)
                    val somethingWentWrongSnackbar =
                        Snackbar.make(binding.root, "Something Went Wrong!", Snackbar.LENGTH_LONG)
                    somethingWentWrongSnackbar.show()
                }
            }
        }, { log ->
            lifecycleScope.launch(Dispatchers.Main) {
                progressDialog.setMessage("Applying Filter..${log.message}")
            }
        }) { statistics -> Log.d("STATS", statistics.toString()) }
    }

    private fun getOutputFilePath(): String? {

        val currentTimeMillis = System.currentTimeMillis()
        val today = Date(currentTimeMillis)
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss")
        val fileName: String = "media3_" + dateFormat.format(today) + ".mp4"

        val documentsDirectory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absoluteFile
        Log.d("itadocdir", "getDocDirName:$documentsDirectory")
        val mediaThreeDirectory = File(documentsDirectory, "Media3")
        if (!mediaThreeDirectory.exists()) {
            mediaThreeDirectory.mkdir()
        }
        val file = File(mediaThreeDirectory, fileName)


        file.createNewFile()
        println("No file found file created ${file.absolutePath}")


        return file.absolutePath
    }

    private fun transformerBuilder(): Transformer {


        val progressDialog = ProgressDialog(this@EditOperationsPlayerActivity)
        progressDialog.setCancelable(false)
        progressDialog.setMessage("Applying Filter..")
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()
        val request = TransformationRequest.Builder().setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC).build()
        val transformerListener: Transformer.Listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, result: ExportResult) {
                Log.d("vcas", "success")

                progressDialog.dismiss()
                val filterAppliedSnackbar =
                    Snackbar.make(binding.root, "Filter Applied", Snackbar.LENGTH_LONG)
                filterAppliedSnackbar.show()
            }

            override fun onError(
                composition: Composition, result: ExportResult, exception: ExportException
            ) {
                Log.d("vcae", "fail")
                progressDialog.dismiss()
                val somethingWentWrongSnackbar =
                    Snackbar.make(binding.root, "Something Went Wrong!", Snackbar.LENGTH_LONG)
                somethingWentWrongSnackbar.show()
            }
        }
        return Transformer.Builder(this).setTransformationRequest(request)
            .addListener(transformerListener).build()
    }

    private fun createVideoEffects(): ImmutableList<Effect> {
        val effects = ImmutableList.Builder<Effect>()
        val overlayEffect: OverlayEffect = createOverlayEffect()!!
        effects.add(overlayEffect)
        return effects.build()
    }

    private fun saveGif(gifData: ByteArray): String? {
        val currentTimeMillis = System.currentTimeMillis()
        val today = Date(currentTimeMillis)
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss")
        val fileName: String = "bitmap_" + dateFormat.format(today) + ".gif"
        val gifDirectory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absoluteFile
        if (!gifDirectory.exists()) {
            gifDirectory.mkdir()
        }
        val file = File(gifDirectory, fileName)
        file.createNewFile()
        try {
            val fos = FileOutputStream(file)
            fos.write(gifData)
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }


        println("No gif file found gif file created ${file.absolutePath}")
        return file.absolutePath
    }

    private fun saveBitmap(bitmap: Bitmap): String? {
        val currentTimeMillis = System.currentTimeMillis()
        val today = Date(currentTimeMillis)
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss")
        val fileName: String = "bitmap_" + dateFormat.format(today) + ".png"
        val bitmapDirectory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absoluteFile
        if (!bitmapDirectory.exists()) {
            bitmapDirectory.mkdir()
        }
        val file = File(bitmapDirectory, fileName)
        file.createNewFile()
        println("No bitmap file found bitmap file created ${file.absolutePath}")

        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()

            val fileWithBitmapSavedSnackbar =
                Snackbar.make(binding.root, "file with bitmap saved", Snackbar.LENGTH_LONG)
            fileWithBitmapSavedSnackbar.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return file.absolutePath
    }

    private fun createOverlayEffect(): OverlayEffect? {
        if (mCurrentView != null) {
            mCurrentView!!.setInEdit(false)
        }
        binding.captureLayout.isDrawingCacheEnabled = true
        binding.captureLayout.buildDrawingCache()
        var finalBitmap: Bitmap = Bitmap.createBitmap(binding.captureLayout.drawingCache)
        val overLaysBuilder: ImmutableList.Builder<TextureOverlay> = ImmutableList.builder()
        val overlaySettings = OverlaySettings.Builder().build()
        finalBitmap = Utils.createSquaredBitmap(finalBitmap)
        Log.d("eopa", "finalBitmap:$finalBitmap")
        val finalBitmapPath = saveBitmap(finalBitmap)
        Log.d("eopa", "finalBitmapPath:$finalBitmapPath")
        val finalBitmapUri = Uri.fromFile(finalBitmapPath?.let { File(it) })
        Log.d("eopa", "finalBitmapUri:$finalBitmapUri")
        val imageOverlay =
            BitmapOverlay.createStaticBitmapOverlay(this, finalBitmapUri, overlaySettings)
        overLaysBuilder.add(imageOverlay)
        val overlays: ImmutableList<TextureOverlay> = overLaysBuilder.build()
        return if (overlays.isEmpty()) null else OverlayEffect(overlays)
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

    override fun onResume() {
        super.onResume()
        exoPlayer.stop()
        exoPlayer.release()
        initExoPlayer()
    }

    override fun onPause() {
        super.onPause()
        exoPlayer.stop()
        exoPlayer.release()
    }

    override fun onStop() {
        super.onStop()
        exoPlayer.stop()
        exoPlayer.release()
    }


    private fun initExoPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoPlayer
        mediaItem = MediaItem.fromUri(fetchVideoString!!.toUri())
        exoPlayer.setMediaItem(mediaItem!!)
        exoPlayer.prepare()
    }
}