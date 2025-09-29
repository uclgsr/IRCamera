package com.mpdc4gsr.module.user.fragment

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.user.viewmodel.MoreFragmentComposeViewModel

/**
 * Compose migration of MoreFragment - Minimal working version
 */
class MoreFragmentCompose : BaseComposeFragment<MoreFragmentComposeViewModel>() {

    override fun createViewModel(): MoreFragmentComposeViewModel {
        return viewModels<MoreFragmentComposeViewModel>().value
    }

    @Composable
    override fun Content(viewModel: MoreFragmentComposeViewModel) {
        LibUnifiedTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "More Options",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Additional Features",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Explore more tools and settings",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
