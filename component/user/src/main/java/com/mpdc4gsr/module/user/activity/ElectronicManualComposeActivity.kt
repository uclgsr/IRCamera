package com.mpdc4gsr.module.user.activity

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.libunified.app.utils.Constants
import com.mpdc4gsr.module.user.viewmodel.ElectronicManualViewModel

class ElectronicManualComposeActivity : BaseComposeActivity<ElectronicManualViewModel>() {
    override fun createViewModel(): ElectronicManualViewModel {
        return viewModels<ElectronicManualViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ElectronicManualViewModel) {
        val title by viewModel.title.collectAsState()
        val options by viewModel.options.collectAsState()
        val productType by viewModel.productType.collectAsState()
        // Get product type from intent
        val intentProductType = intent.getIntExtra(Constants.SETTING_TYPE, 0)
        // Load options on start
        LaunchedEffect(Unit) {
            viewModel.loadManualOptions(intentProductType)
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(options) { option ->
                    ManualOptionItem(
                        option = option,
                        isBookMode = viewModel.isBookMode(),
                        onClick = {
                            handleOptionClick(option.isTS001, viewModel.isBookMode())
                        }
                    )
                }
            }
        }
    }

    private fun handleOptionClick(isTS001: Boolean, isBookMode: Boolean) {
        if (isTS001) {
            if (isBookMode) {
                // TS001 manual - no action in original implementation
            } else {
                // TS001 FAQ
                NavigationManager.getInstance()
                    .build(RouterConfig.QUESTION)
                    .withBoolean("isTS001", true)
                    .navigation(this)
            }
        } else {
            if (isBookMode) {
                // TS004 manual - PDF viewer
                NavigationManager.getInstance()
                    .build(RouterConfig.PDF)
                    .withBoolean("isTS001", false)
                    .navigation(this)
            } else {
                // TS004 FAQ
                NavigationManager.getInstance()
                    .build(RouterConfig.QUESTION)
                    .withBoolean("isTS001", false)
                    .navigation(this)
            }
        }
    }
}

@Composable
private fun ManualOptionItem(
    option: ElectronicManualViewModel.ManualOption,
    isBookMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isBookMode) "User Manual" else "Frequently Asked Questions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}