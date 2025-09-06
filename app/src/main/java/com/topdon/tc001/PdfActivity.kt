package com.topdon.tc001

import android.view.WindowManager
// import com.github.barteksc.pdfviewer.PDFView // TODO: Add PDF library dependency
import android.widget.TextView
import com.csl.irCamera.R

import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * create by fylder on 2018/8/9
 **/
// Legacy ARouter route annotation - now using NavigationManager
class PdfActivity : BaseActivity() {
    
    // findViewById declarations - TODO: Replace with PDFView when library is added
    private val pdfView: TextView by lazy { findViewById<TextView>(R.id.pdf_view) }

    override fun initContentView() = R.layout.activity_pdf

    override fun initView() {
        // TODO: Implement PDF functionality when library is available
        // pdfView.fromAsset(if (intent.getBooleanExtra("isTS001", false)) "TC001.pdf" else "TS004.pdf")
        pdfView.text = "PDF functionality temporarily unavailable - library dependency missing"
        // TODO: Restore PDF functionality with proper method calls
        /*
        .enableSwipe(true) // allows to block changing pages using swipe
        .swipeHorizontal(false)
        .enableDoubletap(true)
        .defaultPage(0)
        .enableAnnotationRendering(false) // render annotations (such as comments, colors or forms)
        .password(null)
        .scrollHandle(null)
        .enableAntialiasing(true) // improve rendering a little bit on low-res screens
        .spacing(0) // spacing between pages in dp. To define spacing color, set view background
        .load()
        */
    }

    override fun initData() {
        val tc001File = File(getExternalFilesDir("pdf")!!, "TC001.pdf")
        if (!tc001File.exists()) {
            copyBigDataToSD("TC001.pdf", tc001File)
        }

        val tc004File = File(getExternalFilesDir("pdf")!!, "TS004.pdf")
        if (!tc004File.exists()) {
            copyBigDataToSD("TS004.pdf", tc004File)
        }
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    //复制assets文件
    @Throws(IOException::class)
    private fun copyBigDataToSD(assetsName: String, targetFile: File) {
        val myOutput: OutputStream = FileOutputStream(targetFile)
        val myInput = assets.open(assetsName)
        val buffer = ByteArray(1024)
        var length: Int = myInput.read(buffer)
        while (length > 0) {
            myOutput.write(buffer, 0, length)
            length = myInput.read(buffer)
        }
        myOutput.flush()
        myInput.close()
        myOutput.close()
    }

}