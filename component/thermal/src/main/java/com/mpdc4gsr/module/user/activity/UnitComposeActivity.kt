package com.mpdc4gsr.module.user.activity

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.component.shared.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.module.user.viewmodel.UnitViewModel
import com.mpdc4gsr.component.shared.R as RCore

class UnitComposeActivity : BaseComposeActivity<UnitViewModel>() {
    override fun createViewModel(): UnitViewModel = viewModels<UnitViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: UnitViewModel) {
        val selectedUnit by viewModel.selectedUnit.collectAsState()
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(RCore.string.setting_unit),
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                viewModel.saveTemperatureUnit()
                                finish()
                            },
                        ) {
                            Text(
                                text = stringResource(RCore.string.person_save),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Celsius Option
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedUnit == UnitViewModel.CELSIUS,
                                onClick = { viewModel.selectUnit(UnitViewModel.CELSIUS) },
                            ),
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                if (selectedUnit == UnitViewModel.CELSIUS) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "℃",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                            color =
                                if (selectedUnit == UnitViewModel.CELSIUS) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                        )
                        if (selectedUnit == UnitViewModel.CELSIUS) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                // Fahrenheit Option
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedUnit == UnitViewModel.FAHRENHEIT,
                                onClick = { viewModel.selectUnit(UnitViewModel.FAHRENHEIT) },
                            ),
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                if (selectedUnit == UnitViewModel.FAHRENHEIT) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "℉",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                            color =
                                if (selectedUnit == UnitViewModel.FAHRENHEIT) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                        )
                        if (selectedUnit == UnitViewModel.FAHRENHEIT) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}


