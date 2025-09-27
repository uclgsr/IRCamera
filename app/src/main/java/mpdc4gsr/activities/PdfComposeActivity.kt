package mpdc4gsr.activities

import android.os.Bundle
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import mpdc4gsr.ui.components.CommonComponents
import mpdc4gsr.ui.theme.IRCameraTheme
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Compose version of PdfActivity demonstrating PDF viewer functionality.
 * Shows how to handle file operations and display placeholder content in Compose.
 */
class PdfComposeActivity : BaseComposeActivity() {

    private var isTS001: Boolean = false

    @Composable
    override fun Content() {
        LaunchedEffect(Unit) {
            isTS001 = intent.getBooleanExtra("isTS001", false)
            initializeFiles()
        }

        IRCameraTheme {
            PdfScreen(
                isTS001 = isTS001,
                onBackPressed = { finish() }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PdfScreen(
        isTS001: Boolean,
        onBackPressed: () -> Unit
    ) {
        val pdfFileName = if (isTS001) "TC001.pdf" else "TS004.pdf"
        val scrollState = rememberScrollState()

        Scaffold(
            topBar = {
                CommonComponents.IRCameraTopAppBar(
                    title = pdfFileName,
                    onNavigationClick = onBackPressed
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // PDF icon
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = "PDF Document",
                    modifier = Modifier.size(120.dp),
                    tint = Color(0xFF6B35FF)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // PDF title
                Text(
                    text = pdfFileName,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status message
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2A2A2A)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PDF Viewer",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            text = "PDF functionality temporarily unavailable.\n$pdfFileName will be displayed here when PDF library is available.",
                            color = Color(0xCCFFFFFF),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Document info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            InfoItem(
                                label = "Document Type",
                                value = if (isTS001) "TC001 Manual" else "TS004 Manual"
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            InfoItem(
                                label = "Format", 
                                value = "PDF"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons placeholder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = { /* TODO: Implement share functionality */ },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Share")
                    }

                    OutlinedButton(
                        onClick = { /* TODO: Implement print functionality */ },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Print")
                    }
                }
            }
        }
    }

    @Composable
    private fun InfoItem(
        label: String,
        value: String
    ) {
        Column {
            Text(
                text = label,
                color = Color(0x80FFFFFF),
                fontSize = 12.sp
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun initializeFiles() {
        // Initialize PDF files in background
        val tc001File = File(getExternalFilesDir("pdf")!!, "TC001.pdf")
        if (!tc001File.exists()) {
            copyBigDataToSD("TC001.pdf", tc001File)
        }

        val tc004File = File(getExternalFilesDir("pdf")!!, "TS004.pdf")
        if (!tc004File.exists()) {
            copyBigDataToSD("TS004.pdf", tc004File)
        }
    }

    @Throws(IOException::class)
    private fun copyBigDataToSD(
        assetsName: String,
        targetFile: File,
    ) {
        try {
            val inputStream = assets.open(assetsName)
            val outputStream: FileOutputStream = FileOutputStream(targetFile)
            val buffer = ByteArray(1024)
            var byteCount: Int
            while (inputStream.read(buffer).also { byteCount = it } != -1) {
                outputStream.write(buffer, 0, byteCount)
            }
            outputStream.flush()
            inputStream.close()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            // Handle gracefully - files may not exist in assets
        }
    }
}