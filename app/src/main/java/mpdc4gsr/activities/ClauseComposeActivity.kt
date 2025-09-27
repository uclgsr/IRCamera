package mpdc4gsr.activities

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.BaseApplication
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.dialog.TipDialog
import com.mpdc4gsr.libunified.app.dialog.TipProgressDialog
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import com.mpdc4gsr.libunified.app.lms.utils.NetworkUtil
import com.mpdc4gsr.libunified.app.lms.weiget.TToast
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.libunified.app.utils.CommUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.App
import mpdc4gsr.ui.theme.IRCameraTheme
import mpdc4gsr.utils.VersionUtils
import java.util.Calendar
import com.mpdc4gsr.libunified.R as LibCoreR

/**
 * Compose version of ClauseActivity demonstrating agreement/terms acceptance UI.
 * Shows how to handle user agreements and app initialization in Compose.
 */
class ClauseComposeActivity : BaseComposeActivity() {

    private var dialog: TipProgressDialog? = null

    @Composable
    override fun Content() {
        IRCameraTheme {
            ClauseScreen(
                onAgree = { confirmInitApp() },
                onDisagree = { showDisagreeDialog() }
            )
        }
    }

    @Composable
    private fun ClauseScreen(
        onAgree: () -> Unit,
        onDisagree: () -> Unit
    ) {
        val scrollState = rememberScrollState()
        val year = Calendar.getInstance().get(Calendar.YEAR)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF16131E))
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // App Logo/Title
            Text(
                text = CommUtils.getAppName(),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Terms and Privacy Policy Text
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2A2A2A)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.user_services_agreement),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = stringResource(R.string.privacy_policy_content),
                        color = Color(0xCCFFFFFF),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.terms_of_service_content),
                        color = Color(0xCCFFFFFF),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Copyright year
            Text(
                text = stringResource(R.string.version_year, "2023-$year"),
                color = Color(0x80FFFFFF),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Agree button
                Button(
                    onClick = onAgree,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.privacy_agree),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Disagree button
                OutlinedButton(
                    onClick = onDisagree,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = stringResource(R.string.privacy_disagree),
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initDialog()
    }

    private fun initDialog() {
        dialog = TipProgressDialog.Builder(this)
            .setMessage(LibCoreR.string.tip_loading)
            .setCanceleable(false)
            .create()
    }

    private fun showDisagreeDialog() {
        TipDialog.Builder(this)
            .setMessage(getString(R.string.privacy_tips))
            .setPositiveListener(R.string.privacy_confirm) {
                confirmInitApp()
            }
            .setCancelListener(R.string.privacy_cancel) {
                finish()
            }
            .setCanceled(true)
            .create().show()
    }

    private fun confirmInitApp() {
        dialog?.show()

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val networkCheck = async(Dispatchers.IO) {
                    NetworkUtil.checkNetworkConnectivity()
                }

                val appInit = async(Dispatchers.IO) {
                    delay(1000) // Simulate initialization time
                    App.initializeApp(this@ClauseComposeActivity)
                }

                // Wait for both operations
                val isNetworkAvailable = networkCheck.await()
                appInit.await()

                // Store agreement acceptance
                SharedManager.setUserAgreementAccepted(true)

                dialog?.dismiss()

                // Navigate to main screen
                NavigationManager.getInstance()
                    .build(RouterConfig.MAIN)
                    .navigation(this@ClauseComposeActivity)
                finish()

            } catch (e: Exception) {
                dialog?.dismiss()
                TToast.showToast(
                    this@ClauseComposeActivity,
                    "Initialization failed: ${e.message}"
                )
            }
        }
    }
}