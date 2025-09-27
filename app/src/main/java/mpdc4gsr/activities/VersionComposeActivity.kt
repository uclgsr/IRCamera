package mpdc4gsr.activities

import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.csl.irCamera.BuildConfig
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.libunified.app.utils.CommUtils
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.lms.UrlConstant
import com.mpdc4gsr.libunified.app.viewmodel.VersionViewModel
import com.mpdc4gsr.libunified.app.BaseApplication
import mpdc4gsr.ui.theme.IRCameraTheme
import mpdc4gsr.utils.AppVersionUtil
import mpdc4gsr.utils.VersionUtils
import java.util.*

/**
 * Compose version of VersionActivity demonstrating migration pattern.
 * This serves as an example of how to convert existing Activities to Compose.
 */
class VersionComposeActivity : BaseComposeActivity() {

    @Composable
    override fun Content() {
        IRCameraTheme {
            VersionScreen()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initData()
    }

    override fun onResume() {
        super.onResume()
        SharedManager.setBaseHost(UrlConstant.BASE_URL)
    }

    private fun initData() {
        if (BaseApplication.instance.isDomestic()) {
            checkAppVersion(false)
        }
    }

    private var appVersionUtil: AppVersionUtil? = null

    private fun checkAppVersion(isShow: Boolean) {
        if (appVersionUtil == null) {
            appVersionUtil = AppVersionUtil(
                this,
                object : AppVersionUtil.DotIsShowListener {
                    override fun isShow(show: Boolean) {
                    }

                    override fun version(version: String) {
                    }
                },
            )
        }
        appVersionUtil?.checkVersion(isShow)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun VersionScreen() {
        val context = LocalContext.current
        val scrollState = rememberScrollState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Version") },
                    navigationIcon = {
                        // Since there's no back button in original, we'll keep it simple
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // App Icon (placeholder - original has image)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clickable {
                            if (BuildConfig.DEBUG) {
                                // Handle debug click
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "IR",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // App Name
                Text(
                    text = CommUtils.getAppName(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Version
                Text(
                    text = "${stringResource(R.string.set_version)}V${VersionUtils.getCodeStr(context)}",
                    style = MaterialTheme.typography.bodyLarge
                )

                // Check Version Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            checkAppVersion(true)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Check for Updates",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Links
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.version_year, "2023-${Calendar.getInstance().get(Calendar.YEAR)}"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Privacy Policy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                NavigationManager.build(RouterConfig.POLICY)
                                    .withInt(PolicyComposeActivity.KEY_THEME_TYPE, 1)
                                    .navigation(context)
                            }
                        )

                        Text(
                            text = "Terms",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                NavigationManager.build(RouterConfig.POLICY)
                                    .withInt(PolicyActivity.KEY_THEME_TYPE, 2)
                                    .navigation(context)
                            }
                        )

                        Text(
                            text = "Copyright",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                NavigationManager.build(RouterConfig.POLICY)
                                    .withInt(PolicyActivity.KEY_THEME_TYPE, 3)
                                    .navigation(context)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}