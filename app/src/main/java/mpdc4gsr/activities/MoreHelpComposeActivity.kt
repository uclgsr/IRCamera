package mpdc4gsr.activities

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import com.mpdc4gsr.libunified.app.utils.Constants
import mpdc4gsr.ui.theme.IRCameraTheme

/**
 * Compose version of MoreHelpActivity demonstrating help/guide UI patterns.
 * Shows how to handle conditional UI display and system integrations in Compose.
 */
class MoreHelpComposeActivity : BaseComposeActivity() {

    private var connectionType: Int = 0
    private lateinit var wifiManager: WifiManager

    @Composable
    override fun Content() {
        val context = LocalContext.current
        
        LaunchedEffect(Unit) {
            connectionType = intent.getIntExtra(Constants.SETTING_CONNECTION_TYPE, 0)
            wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        }

        IRCameraTheme {
            MoreHelpScreen(
                connectionType = connectionType,
                onBackPressed = { finish() },
                onWifiSettingsClick = { startWifiList() }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MoreHelpScreen(
        connectionType: Int,
        onBackPressed: () -> Unit,
        onWifiSettingsClick: () -> Unit
    ) {
        val scrollState = rememberScrollState()
        val isConnectionType = connectionType == Constants.SETTING_CONNECTION

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = if (isConnectionType) {
                                stringResource(R.string.ts004_guide_text6)
                            } else {
                                stringResource(R.string.app_help)
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF16131E),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 31.dp)
            ) {
                // Title text
                Text(
                    text = if (isConnectionType) {
                        stringResource(R.string.ts004_guide_text8)
                    } else {
                        stringResource(R.string.ts004_disconnect_tips1)
                    },
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Connection guide tips
                if (isConnectionType) {
                    ConnectionGuideTips()
                } else {
                    DisconnectionTips(onWifiSettingsClick = onWifiSettingsClick)
                }
            }
        }
    }

    @Composable
    private fun ConnectionGuideTips() {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Guide tip 1
            GuideItem(
                iconRes = R.drawable.ic_connection_tip4,
                textRes = R.string.ts004_guide_text9
            )

            // Guide tip 2
            GuideItem(
                iconRes = R.drawable.ic_connection_tip5,
                textRes = R.string.ts004_guide_text10
            )

            // Guide tip 4
            GuideItem(
                iconRes = R.drawable.ic_connection_tip7,
                textRes = R.string.ts004_guide_text12
            )
        }
    }

    @Composable
    private fun DisconnectionTips(onWifiSettingsClick: () -> Unit) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Disconnect tip 1
            GuideItem(
                iconRes = R.drawable.ic_connection_tip1,
                textRes = R.string.ts004_disconnect_tips2
            )

            // Disconnect tip 2
            GuideItem(
                iconRes = R.drawable.ic_connection_tip2,
                textRes = R.string.ts004_disconnect_tips3
            )

            // WiFi settings link
            val underlinedText = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = Color.White,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(stringResource(R.string.ts004_disconnect_tips4))
                }
            }

            Text(
                text = underlinedText,
                modifier = Modifier
                    .clickable { onWifiSettingsClick() }
                    .padding(16.dp),
                fontSize = 14.sp
            )
        }
    }

    @Composable
    private fun GuideItem(iconRes: Int, textRes: Int) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A2A)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon placeholder - in real implementation, load from iconRes
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF444444)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔧",
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Guide text
                Text(
                    text = stringResource(textRes),
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    private fun startWifiList() {
        if (wifiManager.isWifiEnabled) {
            openWifiSettings()
        } else {
            showWifiEnableDialog()
        }
    }

    private fun openWifiSettings() {
        if (Build.VERSION.SDK_INT < 29) {
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = true
        } else {
            var wifiIntent = Intent(Settings.Panel.ACTION_WIFI)
            if (wifiIntent.resolveActivity(packageManager) == null) {
                wifiIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
                if (wifiIntent.resolveActivity(packageManager) != null) {
                    startActivity(wifiIntent)
                }
            } else {
                startActivity(wifiIntent)
            }
        }
    }

    private fun showWifiEnableDialog() {
        // Note: In a full implementation, you'd want to create a Compose dialog
        // For now, maintaining compatibility with existing TipDialog
        // This could be migrated to a Compose AlertDialog in the future
        com.mpdc4gsr.libunified.app.dialog.TipDialog.Builder(this)
            .setTitleMessage(getString(R.string.app_tip))
            .setMessage(R.string.ts004_wlan_tips)
            .setPositiveListener(R.string.app_open) {
                openWifiSettings()
            }
            .setCancelListener(R.string.app_cancel) {
                // Do nothing
            }
            .setCanceled(true)
            .create().show()
    }
}