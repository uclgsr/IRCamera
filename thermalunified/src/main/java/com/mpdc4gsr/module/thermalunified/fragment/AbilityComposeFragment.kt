package com.mpdc4gsr.module.thermalunified.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.activity.IRThermalPlusComposeActivity
import com.mpdc4gsr.module.thermalunified.activity.MonitoryHomeComposeActivity
import com.mpdc4gsr.module.thermalunified.activity.ThermalIrNightComposeActivity

class AbilityComposeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                Content()
            }
        }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content() {
        val context = LocalContext.current
        var isTC007 by remember { mutableStateOf(false) }
        // Get TC007 status from arguments
        LaunchedEffect(Unit) {
            isTC007 = arguments?.getBoolean("IS_TC007", false) ?: false
        }
        LibUnifiedTheme {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
            ) {
                // Header
                Text(
                    text = "Thermal Imaging Abilities",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Text(
                    text = "Explore advanced thermal imaging capabilities and specialized modes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
                // Abilities grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(getAbilityItems(isTC007)) { ability ->
                        AbilityCard(
                            ability = ability,
                            onClick = {
                                handleAbilityClick(context, ability, isTC007)
                            },
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun AbilityCard(
        ability: AbilityItem,
        onClick: () -> Unit,
    ) {
        Card(
            onClick = onClick,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            colors =
                CardDefaults.cardColors(
                    containerColor = ability.containerColor,
                ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (ability.iconRes != null) {
                    Icon(
                        painter = painterResource(id = ability.iconRes),
                        contentDescription = ability.title,
                        modifier = Modifier.size(48.dp),
                        tint = ability.iconTint,
                    )
                } else {
                    Icon(
                        imageVector = ability.icon ?: Icons.Default.Settings,
                        contentDescription = ability.title,
                        modifier = Modifier.size(48.dp),
                        tint = ability.iconTint,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = ability.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ability.textColor,
                )
                if (ability.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ability.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = ability.textColor.copy(alpha = 0.8f),
                    )
                }
                if (ability.badge.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = ability.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }

    private fun getAbilityItems(isTC007: Boolean): List<AbilityItem> =
        listOf(
            AbilityItem(
                id = "winter",
                title = "Winter Mode",
                description = "Enhanced cold weather detection",
                iconRes = R.drawable.ic_ir_winter_bg,
                containerColor =
                    androidx.compose.ui.graphics
                        .Color(0xFF2196F3),
                iconTint = androidx.compose.ui.graphics.Color.White,
                textColor = androidx.compose.ui.graphics.Color.White,
                badge = "SPECIALTY",
            ),
            AbilityItem(
                id = "monitoring",
                title = "Monitoring",
                description = "Advanced thermal monitoring",
                icon = Icons.Default.Monitor,
                containerColor =
                    androidx.compose.ui.graphics
                        .Color(0xFF4CAF50),
                iconTint = androidx.compose.ui.graphics.Color.White,
                textColor = androidx.compose.ui.graphics.Color.White,
                badge = "CORE",
            ),
            AbilityItem(
                id = "residential",
                title = "Residential",
                description = "Home energy audit mode",
                icon = Icons.Default.Home,
                containerColor =
                    androidx.compose.ui.graphics
                        .Color(0xFFFF9800),
                iconTint = androidx.compose.ui.graphics.Color.White,
                textColor = androidx.compose.ui.graphics.Color.White,
                badge = "AUDIT",
            ),
            AbilityItem(
                id = "automotive",
                title = "Automotive",
                description = "Vehicle thermal analysis",
                icon = Icons.Default.DirectionsCar,
                containerColor =
                    androidx.compose.ui.graphics
                        .Color(0xFFF44336),
                iconTint = androidx.compose.ui.graphics.Color.White,
                textColor = androidx.compose.ui.graphics.Color.White,
                badge = "AUTO",
            ),
            AbilityItem(
                id = "night_vision",
                title = "Night Vision",
                description = "Enhanced night thermal",
                icon = Icons.Default.NightsStay,
                containerColor =
                    androidx.compose.ui.graphics
                        .Color(0xFF9C27B0),
                iconTint = androidx.compose.ui.graphics.Color.White,
                textColor = androidx.compose.ui.graphics.Color.White,
                badge = if (isTC007) "TC007" else "ENHANCED",
            ),
            AbilityItem(
                id = "thermal_plus",
                title = "Thermal Plus",
                description = "Advanced thermal features",
                icon = Icons.Default.Add,
                containerColor =
                    androidx.compose.ui.graphics
                        .Color(0xFF607D8B),
                iconTint = androidx.compose.ui.graphics.Color.White,
                textColor = androidx.compose.ui.graphics.Color.White,
                badge = "PLUS",
            ),
        )

    private fun handleAbilityClick(
        context: android.content.Context,
        ability: AbilityItem,
        isTC007: Boolean,
    ) {
        when (ability.id) {
            "winter" -> {
                // Handle winter mode - winter mode tracking
            }

            "monitoring" -> {
                // Navigate to monitoring home
                val intent = Intent(context, MonitoryHomeComposeActivity::class.java)
                context.startActivity(intent)
            }

            "residential" -> {
                // Navigate to residential thermal analysis
                NavigationManager
                    .getInstance()
                    .build("IR_RESIDENTIAL")
                    .navigation(context)
            }

            "automotive" -> {
                // Navigate to automotive thermal analysis
                NavigationManager
                    .getInstance()
                    .build("IR_AUTOMOTIVE")
                    .navigation(context)
            }

            "night_vision" -> {
                // Navigate to night vision thermal mode
                val intent = Intent(context, ThermalIrNightComposeActivity::class.java)
                context.startActivity(intent)
            }

            "thermal_plus" -> {
                // Navigate to thermal plus features
                val intent = Intent(context, IRThermalPlusComposeActivity::class.java)
                context.startActivity(intent)
            }
        }
    }

    data class AbilityItem(
        val id: String,
        val title: String,
        val description: String,
        val icon: ImageVector? = null,
        val iconRes: Int? = null,
        val containerColor: androidx.compose.ui.graphics.Color,
        val iconTint: androidx.compose.ui.graphics.Color,
        val textColor: androidx.compose.ui.graphics.Color,
        val badge: String = "",
    )
}
