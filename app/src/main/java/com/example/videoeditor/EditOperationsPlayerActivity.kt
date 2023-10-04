package com.example.videoeditor

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.TextureOverlay
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.util.EventLogger
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
    private var scaledBitmap: Bitmap? = null
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
    private var videoWidth = 0
    private var videoHeight = 0
    private var exoPlayerPrepared = false
    private var scaledEmojiBitmap: Bitmap? = null
    private var scaledImageBitmap: Bitmap? = null
    private var scaledWidth: Int = 0
    private var scaledHeight: Int = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditOperationsPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        exoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoPlayer
        exoPlayer.addListener(playbackStateListener())
        exoPlayer.addAnalyticsListener(EventLogger())

        fetchVideoString = intent.getStringExtra("inputVideoUri")
        if (fetchVideoString != null) {
            Log.d("fetchVideoUri", "onCreate:$fetchVideoString")

            mediaItem = MediaItem.fromUri(fetchVideoString!!.toUri())
            exoPlayer.setMediaItem(mediaItem!!)
            exoPlayer.addListener(playbackStateListener())
            exoPlayer.prepare()
        }
        binding.insertTextLl.setOnClickListener {
            this.IMAGE_OR_GIF = 2
            startActivityForResult(Intent(this, EditTextPopupActivity::class.java), 1)
        }

        mViews = ArrayList()

//        binding.captureLayout.setOnTouchListener(@SuppressLint("ClickableViewAccessibility")
//        object : OnTouchListener{
//            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
//                if(event!!.action == MotionEvent.ACTION_UP){
//                    Log.d("eopa", "onTouchx: ${event.x} onTouchy: ${event.y}")
//                    Log.d("eopa", "collage onTouchx: ${binding.collageView.left} onTouchy: ${binding.collageView.right}")
//                    val location = IntArray(2)
//                    binding.collageView.getLocationOnScreen(location)
//                    val x = location[0]
//                    val y = location[1]
//                    Log.d("eopa", "collage location X axis is : $x Y axis is: $y")
//
//
//                }
//                return true
//            }
//
//        })

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

    fun getLocationOnScreen(view: View): Point {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return Point(location[0], location[1])
    }

    private fun ffmpegGifTransformation() {
        val gifPath = saveGif(gifData!!)
        Log.d("eopa", "gifFilePath:$gifPath")
        val gifFileUri = Uri.fromFile(File(gifPath!!))
        Log.d("eopa", "gifFileUri:$gifFileUri")
        outputFilePath = getOutputFilePath()

        input_video_uri_ffmpeg =
            FFmpegKitConfig.getSafParameterForRead(this, Uri.parse(fetchVideoString!!))
        Log.d("eopa", "input_video_uri_ffmpeg:$input_video_uri_ffmpeg")

        val location = IntArray(2)
        binding.collageView.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1]
        Log.d("eopa", "collage after saved x value:$x y value: $y")
        val command =
            "-y -i $input_video_uri_ffmpeg -stream_loop -1 -i $gifFileUri -filter_complex [0]overlay=x=${x}:y=${y}:shortest=1[out] -map [out] -map 0:a? $outputFilePath"
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
//                val nh = (bitmap.height * (512.0 / bitmap.width)).toInt()
//                Log.d("eopa", "nh: $nh")
//                  video 480x 864
//                bitma 3072x4096
//                480x648
//                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 512, nh, true)
//                Log.d("eopa", "scaledBitmap: $scaledBitmap")
//                val newMutableBitmap = convertToMutable(scaledBitmap)
//                Log.d("eopa", "newMutableBitmap:$newMutableBitmap ")
//                var ww = bitmap.width
//                if (ww < 550) {
//                    ww = 550
//                }
//                var s = ww - bitmap.width
//                if (s > 2) {
//                    s /= 2
//                }
//                val dstBmp = Bitmap.createBitmap(ww, bitmap.height + 30, Bitmap.Config.ARGB_8888)
//                val bmOverlay = Bitmap.createBitmap(
//                    dstBmp.width, dstBmp.height, dstBmp.config
//                )
//
//                val canvas = Canvas(bmOverlay)
//                Log.d("eoa", "hwa:${canvas.isHardwareAccelerated}")
//                canvas.drawBitmap(dstBmp, Matrix(), null)
//                canvas.drawBitmap(bitmap, s.toFloat(), 15f, null)
//                addStickerView(newMutableBitmap)
//                val bitmapAspectRatio = bitmap.width / bitmap.height
//                1080x2160
//                1060x500
//                1080x509
                val ar = calculateAspectRatio(bitmap)
                Log.d("eopa", "aspectRatio:$ar")

//                val scaledWidth: Int
//                val scaledHeight: Int

                if (exoPlayerPrepared) {
                    if (ar > 1) {
                        // Landscape-oriented bitmap
                        scaledWidth = videoWidth
                        scaledHeight = (videoWidth / ar).toInt()
                        scaledImageBitmap =
                            Bitmap.createScaledBitmap(
                                bitmap,
                                scaledWidth,
                                scaledHeight,
                                false
                            )

                        Log.d("eopa", "scaledImageBitmapL:$scaledImageBitmap")
                        addStickerView(scaledImageBitmap!!)

                    } else {
                        // Portrait-oriented bitmap or square 1080x283 2160 3840
                        scaledHeight = videoHeight
                        scaledWidth = (videoHeight * ar).toInt()
                        scaledImageBitmap =
                            Bitmap.createScaledBitmap(
                                bitmap,
                                scaledWidth,
                                scaledHeight,
                                false
                            )

                        Log.d("eopa", "scaledImageBitmapP:$scaledImageBitmap")
                        addStickerView(scaledImageBitmap!!)
                    }
                }

//                if (exoPlayerPrepared) {
//                    Log.d("eopa", "onActivityResult: newVideoWidth: $videoWidth")
//
//                    scaledImageBitmap =
//                        Bitmap.createScaledBitmap(
//                            bitmap,
//                            videoWidth,
//                            videoWidth / bitmapAspectRatio,
//                            false
//                        )
//                    Log.d("eopa", "scaledImageBitmap:$scaledImageBitmap")
//                    addStickerView(scaledImageBitmap!!)
//                }
            }
        }

    private fun calculateAspectRatio(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height

        // Calculate the aspect ratio (width / height)
        return if (height != 0) {
            width.toFloat() / height.toFloat()
        } else {
            0f // Handle the case where height is 0 (to avoid division by zero)
        }
    }

    private fun convertToMutable(bitmap: Bitmap): Bitmap {
        // Get the width and height of the original bitmap
        val width = bitmap.width
        val height = bitmap.height
        Log.d("eopa", "convertToMutable: width : $width height : $height")
        // Create a mutable Bitmap with the same width and height
        val mutableBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        Log.d("eopa", "mutableBitmap: $mutableBitmap")

        // Create a Canvas object and associate it with the mutable Bitmap
        val canvas = Canvas(mutableBitmap)

        // Draw the original Bitmap onto the mutable Bitmap using the Canvas
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        // Return the mutable Bitmap
        return mutableBitmap
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

                        if (fetchVideoString != null) {
                            if (this.IMAGE_OR_GIF == 2) {
                                insertImage()
                            } else {
                                insertGif()
                            }
                        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                val textBitmap = Bitmap.createBitmap(
                    EditTextPopupActivity.textBitmap!!
                )
                Log.d("eopa", "onActivityResult:$textBitmap")
//                val trimmedBitmap = createTrimmedBitmap(textBitmap)!!
//                val aspectRatio = trimmedBitmap.width / trimmedBitmap.height
//                val ar = textBitmap.width / textBitmap.height
                val ar = calculateAspectRatio(textBitmap)
//                initExoPlayer()
//                val scaledWidth: Int
//                val scaledHeight: Int
                if (exoPlayerPrepared) {
//                    Log.d("eopa", "onActivityResult: newVideoWidth: $videoWidth")
//                    if(ar > 1)
//                        scaledWidth = videoWidth
//                        scaledHeight = (videoWidth / ar).toInt()
//                        scaledBitmap =
//                            Bitmap.createScaledBitmap(
//                                textBitmap,
//                                scaledWidth,
//                                scaledHeight,
//                                false
//                            )
//                        Log.d("eopa", "scaledBitmap:$scaledBitmap")
//                        addStickerView(scaledBitmap!!)
//                    }
//                    else{
//                        scaledHeight = videoHeight
//                        scaledWidth = (videoHeight * ar).toInt()
//                        scaledBitmap =
//                            Bitmap.createScaledBitmap(
//                                textBitmap,
//                                scaledWidth,
//                                scaledHeight,
//                                false
//                            )
//                        Log.d("eopa", "scaledBitmap:$scaledBitmap")
//                        addStickerView(scaledBitmap!!)
//                    }

                    /*scaledWidth = videoWidth*/
                    /*scaledHeight = (videoWidth / ar).toInt()*/
                    scaledBitmap =
                        Bitmap.createScaledBitmap(
                            textBitmap,
                            videoWidth,
                            (videoWidth / ar).toInt(),
                            false
                        )
                    Log.d("eopa", "scaledBitmap:$scaledBitmap")
                    addStickerView(scaledBitmap!!)

//                    scaledBitmap =
//                        Bitmap.createScaledBitmap(
//                            textBitmap,
//                            videoWidth,
//                            (videoWidth / ar).toInt(),
//                            false
//                        )
//                    Log.d("eopa", "scaledBitmap:$scaledBitmap")
//                    addStickerView(scaledBitmap!!)
                }
//                scaledBitmap =
//                    Bitmap.createScaledBitmap(
//                        trimmedBitmap,
//                        videoWidth,
//                        videoWidth / aspectRatio,
//                        false
//                    )
//                Log.d("eopa", "scaledBitmap:$scaledBitmap")
//                addStickerView(scaledBitmap!!)
            }
        } else if (requestCode == 2) {
            if (resultCode == RESULT_OK) {
                val emojiTextBitmap = Bitmap.createBitmap(EmojiActivity.emojiTextBitmap!!)
                Log.d("eopa", "emojiTextBitmap:$emojiTextBitmap")
//                val conf = Bitmap.Config.ARGB_4444
//
//                var ww = emojiTextBitmap.width
//                if (ww < 550) {
//                    ww = 550
//                }
//                var s = ww - emojiTextBitmap.width
//                if (s > 2) {
//                    s /= 2
//                }
//                val dstBmp = Bitmap.createBitmap(ww, emojiTextBitmap.height + 30, conf)
//
//                val bmOverlay = Bitmap.createBitmap(dstBmp.width, dstBmp.height, dstBmp.config)
//                val canvas = Canvas(bmOverlay)
//                canvas.drawBitmap(dstBmp, Matrix(), null)
//                canvas.drawBitmap(emojiTextBitmap, s.toFloat(), 15f, null)
                val ar = emojiTextBitmap.width / emojiTextBitmap.height
//                val ar = calculateAspectRatio(emojiTextBitmap)
                if (exoPlayerPrepared) {
                    /*scaledWidth = videoWidth*/
                    /*scaledHeight = videoWidth / ar*/
                    scaledEmojiBitmap = Bitmap.createScaledBitmap(
                        emojiTextBitmap,
                        videoWidth,
                        videoHeight / ar,
                        false
                    )
                    Log.d("eopa", "scaledEmojiBitmap:$scaledEmojiBitmap")
                    addStickerView(scaledEmojiBitmap!!)
                }
            }
        } else if (requestCode == 3) {
            if (resultCode == RESULT_OK) {
                gifUri = data!!.data  // gets the uri to that file which contains the data
                Log.d("gifuri", "gifUri: $gifUri")
                try {
                    contentResolver.openInputStream(gifUri!!).use {
                        gifData = it!!.readBytes()
                        Glide.with(this).load(gifData).into(binding.collageView)
                        val locations = IntArray(2)
                        binding.collageView.getLocationOnScreen(locations)
                        val x1 = locations[0]
                        val y1 = locations[1]
                        Log.d("eopa", "collage before drag x value:$x1 y value: $y1")
                        binding.collageView.setOnTouchListener(MultiTouchListener())
                        val location = IntArray(2)
                        binding.collageView.getLocationOnScreen(location)
                        val x = location[0]
                        val y = location[1]
                        Log.d("eopa", "collage after drag x value:$x y value: $y")
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
        mCurrentView!!.isDrawingCacheEnabled = true
        mCurrentView!!.buildDrawingCache()
        val finalBitmap: Bitmap = Bitmap.createScaledBitmap(
            mCurrentView!!.drawingCache,
            /*scaledWidth*/ videoWidth,
            /*scaledHeight*/ videoHeight,
            false
        )
        val overLaysBuilder: ImmutableList.Builder<TextureOverlay> = ImmutableList.builder()
        val overlaySettings = OverlaySettings.Builder().build()
//        finalBitmap = Utils.createSquaredBitmap(finalBitmap)
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
        val lp = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT
        )
        lp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        binding.captureLayout.addView(stickerView, lp)
        mViews!!.add(stickerView)
        stickerView.bitmap = bitmap

        setCurrentEdit(stickerView)
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

            // 757
            override fun onTop(stickerView: StickerView?) {
                Handler(Looper.getMainLooper()).post {
                    addStickerView(
                        stickerView!!.bitmap
                    )
                }
            }

        })
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
//        exoPlayerPrepared = false
        exoPlayer.stop()
        exoPlayer.release()
        initExoPlayer()
    }

    override fun onPause() {
        super.onPause()
        exoPlayer.stop()
        exoPlayer.release()
        exoPlayer.removeListener(playbackStateListener())
    }

    override fun onStop() {
        super.onStop()
        exoPlayer.stop()
        exoPlayer.release()
        exoPlayer.removeListener(playbackStateListener())
    }


    private fun initExoPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoPlayer
        mediaItem = MediaItem.fromUri(fetchVideoString!!.toUri())
        exoPlayer.setMediaItem(mediaItem!!)
        exoPlayer.addListener(playbackStateListener())
        exoPlayer.addAnalyticsListener(EventLogger())
        exoPlayer.prepare()
    }

    private fun playbackStateListener() = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            // super.onPlaybackStateChanged(playbackState)

            val stateString: String = when (playbackState) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE"
                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING"
                ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY"
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED"
                else -> "Unknown state"
            }
            Log.d("eopa", "onPlaybackStateChanged: changed state to $stateString")
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // super.onIsPlayingChanged(isPlaying)
            val playingString = if (isPlaying) "PLAYING" else "NOT PLAYING"
            Log.d("eopa", "onIsPlayingChanged: Player is currently $playingString")
        }

        override fun onTracksChanged(tracks: Tracks) {
//            super.onTracksChanged(tracks)
            for (trackGroup in tracks.groups) {
                for (i in 0 until trackGroup.length) {
                    val trackFormat = trackGroup.getTrackFormat(i)
                    if (trackFormat.sampleMimeType!!.startsWith("video/")) {
                        Log.d("eopa", "trackFormat:$trackFormat")
                        videoWidth = trackFormat.width
                        videoHeight = trackFormat.height
                        Log.d("eopa", "videoWidth:$videoWidth videoHeight: $videoHeight")
                        exoPlayerPrepared = true
                    }
                }
            }
        }
    }
}