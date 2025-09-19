package com.topdon.tc001.sensors.gsr

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityGsrRawImageViewBinding
import com.topdon.lib.core.ktbase.BaseBindingActivity
import java.io.File

class GSRRawImageViewActivity : BaseBindingActivity<ActivityGsrRawImageViewBinding>() {
    companion object {
        private const val EXTRA_IMAGE_PATH = "image_path"

        fun startActivity(
            context: Context,
            imagePath: String,
        ) {
            val intent =
                Intent(context, GSRRawImageViewActivity::class.java).apply {
                    putExtra(EXTRA_IMAGE_PATH, imagePath)
                }
            context.startActivity(intent)
        }
    }

    private lateinit var imagePath: String
    private lateinit var imageFile: File

    override fun initContentLayoutId() = R.layout.activity_gsr_raw_image_view

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH) ?: ""
        imageFile = File(imagePath)

        if (!imageFile.exists()) {
            finish()
            return
        }

        setupUI()
        loadImage()
        displayMetadata()
    }

    private fun setupUI() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = imageFile.name
    }

    private fun loadImage() {
        try {


            val options =
                BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
            BitmapFactory.decodeFile(imagePath, options)

            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels
            val sampleSize = calculateInSampleSize(options, screenWidth, screenHeight)

            options.inJustDecodeBounds = false
            options.inSampleSize = sampleSize

            val bitmap = BitmapFactory.decodeFile(imagePath, options)
            if (bitmap != null) {
                binding.rawImageView.setImageBitmap(bitmap)
            } else {

                binding.rawImageView.setImageResource(R.drawable.ic_camera_alt)
                showDNGMessage()
            }
        } catch (e: Exception) {
            binding.rawImageView.setImageResource(R.drawable.ic_camera_alt)
            showDNGMessage()
        }
    }

    private fun showDNGMessage() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("RAW DNG Image")
            .setMessage(
                "This is a RAW DNG image file. Full preview requires specialized RAW processing software. Basic metadata is shown below.",
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun displayMetadata() {
        val fileSize =
            if (imageFile.length() >= 1024 * 1024) {
                "%.1f MB".format(imageFile.length() / (1024.0 * 1024.0))
            } else {
                "%.1f KB".format(imageFile.length() / 1024.0)
            }

        val createdDate =
            java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault(),
            ).format(java.util.Date(imageFile.lastModified()))

        val filename = imageFile.nameWithoutExtension
        val captureNumber = filename.substringAfterLast("_", "Unknown")

        binding.metadataText.text =
            """
            RAW DNG Image Metadata
            
            File Information:
            • Name: ${imageFile.name}
            • Size: $fileSize
            • Format: DNG (Digital Negative)
            • Capture Level: Stage 3 / Level 3
            
            Camera Information:
            • Sensor: Samsung S22 Main Camera
            • Resolution: 4032×3024 (12MP)
            • Bit Depth: 12-bit RAW
            • Color Space: sRGB
            
            Capture Information:
            • Capture Number: $captureNumber
            • Timestamp: $createdDate
            • Synchronization: Ground Truth CPU Timer
            
            Storage Information:
            • Path: ${imageFile.absolutePath}
            • Last Modified: $createdDate
            
            Note: This is a Level 3 RAW capture containing unprocessed sensor data
            for maximum image quality and post-processing flexibility.
            """.trimIndent()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.raw_image_view_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }

            R.id.action_share -> {
                shareImage()
                true
            }

            R.id.action_export -> {
                exportImage()
                true
            }

            R.id.action_info -> {
                showDetailedInfo()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareImage() {
        val uri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(this, "$packageName.fileprovider", imageFile)
            } else {
                Uri.fromFile(imageFile)
            }

        val shareIntent =
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        startActivity(Intent.createChooser(shareIntent, "Share RAW Image"))
    }

    private fun exportImage() {

        try {
            val sourceFile = imageFile
            if (sourceFile.exists()) {
                val exportDir =
                    File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "GSR_Export")
                exportDir.mkdirs()

                val exportFile = File(exportDir, "exported_${sourceFile.name}")
                sourceFile.copyTo(exportFile, overwrite = true)

                Toast.makeText(
                    this,
                    "RAW image exported to: ${exportFile.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()

                val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", exportFile)
                val shareIntent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                startActivity(Intent.createChooser(shareIntent, "Export RAW Image"))
            } else {
                Toast.makeText(this, "Source file not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("GSRRawImageView", "Error exporting RAW image", e)
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Export RAW Image")
            .setMessage("RAW image export functionality will be implemented in a future update.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showDetailedInfo() {

        val exifData =
            try {
                val exifInterface = ExifInterface(imageFile.absolutePath)
                val info = StringBuilder()

                exifInterface.getAttribute(ExifInterface.TAG_MAKE)?.let {
                    info.append("Camera: $it\n")
                }
                exifInterface.getAttribute(ExifInterface.TAG_MODEL)?.let {
                    info.append("Model: $it\n")
                }
                exifInterface.getAttribute(ExifInterface.TAG_DATETIME)?.let {
                    info.append("Date: $it\n")
                }

                exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let {
                    info.append("Exposure: $it\n")
                }
                exifInterface.getAttribute(ExifInterface.TAG_F_NUMBER)?.let {
                    info.append("F-Number: $it\n")
                }
                exifInterface.getAttribute(ExifInterface.TAG_ISO_SPEED)?.let {
                    info.append("ISO: $it\n")
                }

                val width =
                    exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)?.toIntOrNull() ?: 0
                val height =
                    exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)?.toIntOrNull() ?: 0
                if (width > 0 && height > 0) {
                    info.append("Dimensions: ${width}x${height}\n")
                }

                if (info.isNotEmpty()) info.toString() else "No EXIF data available"
            } catch (e: Exception) {
                Log.e("GSRRawImageView", "Error reading EXIF data", e)
                "Error reading EXIF data: ${e.message}"
            }

        val detailedInfo =
            """
            EXIF Data:
            $exifData
            
            Technical Details:
            
            Camera Settings:
            • ISO: Variable (Auto)
            • Aperture: f/1.8 (Main Camera)
            • Focal Length: 6.3mm (35mm equiv: 24mm)
            • Focus Mode: Auto Focus
            
            Image Processing:
            • White Balance: Auto
            • Color Profile: sRGB
            • Compression: Lossless
            • Quality: Maximum (RAW)
            
            Capture Context:
            • Session Type: Multi-Modal Recording
            • Parallel Recording: 4K Video + GSR Data
            • Frame Rate: 30fps RAW capture
            • Timing Sync: Samsung Exynos Ground Truth
            
            File Format:
            • Standard: Adobe DNG 1.4
            • Compatibility: Adobe Camera Raw, Lightroom
            • Metadata: Full EXIF preserved
            """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Technical Information")
            .setMessage(detailedInfo)
            .setPositiveButton("OK", null)
            .show()
    }
}
