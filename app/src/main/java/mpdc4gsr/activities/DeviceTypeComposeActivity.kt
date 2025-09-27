package mpdc4gsr.activities

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.libunified.app.tools.DeviceTools
import mpdc4gsr.ui.theme.IRCameraTheme
import mpdc4gsr.ui.components.CommonComponents

/**
 * Compose version of DeviceTypeActivity demonstrating list UI and device selection.
 * Shows how to handle complex lists, device types, and navigation in Compose.
 */
class DeviceTypeComposeActivity : BaseComposeActivity() {

    private var selectedDeviceType: IRDeviceType? = null

    @Composable
    override fun Content() {
        IRCameraTheme {
            DeviceTypeScreen(
                onBackPressed = { finish() },
                onDeviceSelected = { deviceType ->
                    selectedDeviceType = deviceType
                    handleDeviceSelection(deviceType)
                }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DeviceTypeScreen(
        onBackPressed: () -> Unit,
        onDeviceSelected: (IRDeviceType) -> Unit
    ) {
        Scaffold(
            topBar = {
                CommonComponents.IRCameraTopAppBar(
                    title = stringResource(R.string.device_type_title),
                    onNavigationClick = onBackPressed
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(getDeviceItems()) { item ->
                    DeviceGroupItem(
                        item = item,
                        onDeviceClick = onDeviceSelected
                    )
                }
            }
        }
    }

    @Composable
    private fun DeviceGroupItem(
        item: DeviceItem,
        onDeviceClick: (IRDeviceType) -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Group title
            if (item.isTitle) {
                CommonComponents.SectionHeader(
                    text = getConnectionTypeTitle(item.firstType),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Device cards
            CommonComponents.IRCameraCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // First device
                    DeviceCard(
                        deviceType = item.firstType,
                        onClick = { onDeviceClick(item.firstType) },
                        modifier = Modifier.weight(1f)
                    )

                    // Second device (if exists)
                    item.secondType?.let { secondType ->
                        Spacer(modifier = Modifier.width(16.dp))
                        DeviceCard(
                            deviceType = secondType,
                            onClick = { onDeviceClick(secondType) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun DeviceCard(
        deviceType: IRDeviceType,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier
                .clickable { onClick() }
                .clip(RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF3A3A3A)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth() 
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Device image
                Image(
                    painter = painterResource(id = getDeviceIcon(deviceType)),
                    contentDescription = deviceType.getDeviceName(),
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Device name
                Text(
                    text = deviceType.getDeviceName(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    private fun getDeviceItems(): List<DeviceItem> = listOf(
        DeviceItem(true, IRDeviceType.TS001, IRDeviceType.TC001),
        DeviceItem(false, IRDeviceType.TC001_PLUS, IRDeviceType.TC002C_DUO),
        DeviceItem(true, IRDeviceType.TS004, null),
        DeviceItem(true, IRDeviceType.SHIMMER3_GSR, null),
        DeviceItem(true, IRDeviceType.PC_CONTROLLER, null)
    )

    private fun getConnectionTypeTitle(deviceType: IRDeviceType): String = 
        getString(
            when (deviceType) {
                IRDeviceType.SHIMMER3_GSR -> R.string.tc_connect_bluetooth
                IRDeviceType.PC_CONTROLLER -> R.string.tc_connect_wifi
                else -> if (deviceType.isLine()) R.string.tc_connect_line else R.string.tc_connect_wifi
            }
        )

    private fun getDeviceIcon(deviceType: IRDeviceType): Int = when (deviceType) {
        IRDeviceType.TC001 -> R.drawable.ic_device_type_tc001
        IRDeviceType.TC001_PLUS -> R.drawable.ic_device_type_tc001_plus
        IRDeviceType.TC002C_DUO -> R.drawable.ic_device_type_tc001_plus
        IRDeviceType.TC007 -> R.drawable.ic_device_type_tc007
        IRDeviceType.TS001 -> R.drawable.ic_device_type_ts001
        IRDeviceType.TS004 -> R.drawable.ic_device_type_ts004
        IRDeviceType.SHIMMER3_GSR -> R.drawable.ic_device_type_shimmer_gsr
        IRDeviceType.PC_CONTROLLER -> R.drawable.ic_device_type_pc
    }

    private fun handleDeviceSelection(deviceType: IRDeviceType) {
        when (deviceType) {
            IRDeviceType.TS004 -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.IR_DEVICE_ADD)
                    .withBoolean("isTS004", true)
                    .navigation(this)
            }

            IRDeviceType.TC007 -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.IR_DEVICE_ADD)
                    .withBoolean("isTS004", false)
                    .navigation(this)
            }

            IRDeviceType.SHIMMER3_GSR -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.GSR_MULTI_MODAL)
                    .navigation(this)
                finish()
            }

            IRDeviceType.PC_CONTROLLER -> {
                mpdc4gsr.network.DevicePairingActivity.start(this)
            }

            else -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.IR_MAIN)
                    .withBoolean(ExtraKeyConfig.IS_TC007, false)
                    .navigation(this)
                if (DeviceTools.isConnect()) {
                    finish()
                }
            }
        }
    }

    override fun connected() {
        if (selectedDeviceType?.isLine() == true) {
            finish()
        }
    }

    override fun onSocketConnected(isTS004: Boolean) {
        if (isTS004) {
            if (selectedDeviceType == IRDeviceType.TS004) {
                finish()
            }
        } else {
            if (selectedDeviceType == IRDeviceType.TC007) {
                finish()
            }
        }
    }

    private data class DeviceItem(
        val isTitle: Boolean,
        val firstType: IRDeviceType,
        val secondType: IRDeviceType?
    )

    enum class IRDeviceType {
        TC001 {
            override fun isLine(): Boolean = true
            override fun getDeviceName(): String = "TC001"
        },
        TC001_PLUS {
            override fun isLine(): Boolean = true
            override fun getDeviceName(): String = "TC001 Plus"
        },
        TC002C_DUO {
            override fun isLine(): Boolean = true
            override fun getDeviceName(): String = "TC002C Duo"
        },
        TC007 {
            override fun isLine(): Boolean = false
            override fun getDeviceName(): String = "TC007"
        },
        TS001 {
            override fun isLine(): Boolean = true
            override fun getDeviceName(): String = "TS001"
        },
        TS004 {
            override fun isLine(): Boolean = false
            override fun getDeviceName(): String = "TS004"
        },
        SHIMMER3_GSR {
            override fun isLine(): Boolean = false
            override fun getDeviceName(): String = "Shimmer3 GSR"
        },
        PC_CONTROLLER {
            override fun isLine(): Boolean = false
            override fun getDeviceName(): String = "PC Controller"
        };

        abstract fun isLine(): Boolean
        abstract fun getDeviceName(): String
    }
}