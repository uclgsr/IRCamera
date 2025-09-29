package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel

/**
 * Simplified thermal configuration interface for quick setup
 */
class SimpleThermalConfigComposeActivity : BaseComposeActivity<ThermalViewModel>() {

    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Simple Config",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF0D1117)
                        )
                    )
                }
            ) { paddingValues ->
                SimpleConfigContent(
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun SimpleConfigContent(
        modifier: Modifier = Modifier
    ) {
        var selectedProfile by remember { mutableStateOf("Balanced") }
        var autoAdjust by remember { mutableStateOf(true) }
        var simplifiedMode by remember { mutableStateOf(true) }

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0D1117))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Setup Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFF6B35).copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = "Quick Setup",
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            "Quick Thermal Setup",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "Simplified configuration for quick thermal imaging setup",
                        color = Color(0xFF7D8590),
                        fontSize = 14.sp
                    )
                }
            }
            
            // Profile Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Thermal Profile",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileOption(
                            "Indoor",
                            "Optimized for indoor thermal imaging",
                            Icons.Default.Home,
                            selectedProfile == "Indoor"
                        ) { selectedProfile = "Indoor" }
                        
                        ProfileOption(
                            "Outdoor", 
                            "Configured for outdoor thermal analysis",
                            Icons.Default.Landscape,
                            selectedProfile == "Outdoor"
                        ) { selectedProfile = "Outdoor" }
                        
                        ProfileOption(
                            "Balanced",
                            "General purpose thermal configuration",
                            Icons.Default.Balance,
                            selectedProfile == "Balanced"
                        ) { selectedProfile = "Balanced" }
                        
                        ProfileOption(
                            "High Precision",
                            "Maximum accuracy for detailed analysis",
                            Icons.Default.PrecisionManufacturing,
                            selectedProfile == "High Precision"
                        ) { selectedProfile = "High Precision" }
                    }
                }
            }
            
            // Auto Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Auto Configuration",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    SettingToggle(
                        "Auto Adjust",
                        "Automatically adjust settings based on conditions",
                        Icons.Default.AutoMode,
                        autoAdjust
                    ) { autoAdjust = it }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    SettingToggle(
                        "Simplified Mode",
                        "Hide advanced options for easier operation",
                        Icons.Default.Straighten,
                        simplifiedMode
                    ) { simplifiedMode = it }
                }
            }
            
            // Current Settings Preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Current Settings",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    when (selectedProfile) {
                        "Indoor" -> {
                            SettingItem("Temperature Range", "-5°C to 40°C")
                            SettingItem("Distance", "0.5m - 3m")
                            SettingItem("Emissivity", "0.95 (Human/Building)")
                        }
                        "Outdoor" -> {
                            SettingItem("Temperature Range", "-20°C to 60°C")
                            SettingItem("Distance", "1m - 10m")
                            SettingItem("Emissivity", "0.90 (Mixed materials)")
                        }
                        "Balanced" -> {
                            SettingItem("Temperature Range", "-10°C to 50°C")
                            SettingItem("Distance", "0.3m - 5m")
                            SettingItem("Emissivity", "0.95 (Auto-detect)")
                        }
                        "High Precision" -> {
                            SettingItem("Temperature Range", "0°C to 100°C")
                            SettingItem("Distance", "0.2m - 2m")
                            SettingItem("Emissivity", "0.98 (Calibrated)")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { /* Reset to defaults */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF7D8590)
                    )
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset")
                }
                
                Button(
                    onClick = { /* Apply settings */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Apply")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply Settings")
                }
            }
        }
    }

    @Composable
    private fun ProfileOption(
        title: String,
        description: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        selected: Boolean,
        onClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (selected) Color(0xFFFF6B35).copy(alpha = 0.2f) else Color(0xFF161B22)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = if (selected) Color(0xFFFF6B35) else Color(0xFF7D8590),
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        color = if (selected) Color.White else Color(0xFF7D8590),
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                    )
                    
                    Text(
                        description,
                        color = Color(0xFF7D8590),
                        fontSize = 12.sp
                    )
                }
                
                if (selected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = Color(0xFFFF6B35),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun SettingToggle(
        title: String,
        description: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = Color(0xFF7D8590),
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    description,
                    color = Color(0xFF7D8590),
                    fontSize = 12.sp
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFF6B35),
                    checkedTrackColor = Color(0xFFFF6B35).copy(alpha = 0.5f)
                )
            )
        }
    }

    @Composable
    private fun SettingItem(
        label: String,
        value: String
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                color = Color(0xFF7D8590),
                fontSize = 14.sp
            )
            
            Text(
                value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}