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
import com.mpdc4gsr.module.user.viewmodel.MineFragmentViewModel

/**
 * Compose migration of MineFragment - Minimal working version
 */
class MineFragmentCompose : BaseComposeFragment<MineFragmentViewModel>() {

    override fun createViewModel(): MineFragmentViewModel {
        return viewModels<MineFragmentViewModel>().value
    }

    @Composable
    override fun Content(viewModel: MineFragmentViewModel) {
        val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

        LibUnifiedTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "User Profile",
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
                            text = userProfile.username,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (userProfile.isLoggedIn) "Logged In" else "Guest",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Button(
                    onClick = { viewModel.refreshUserProfile() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Refresh Profile")
                }
            }
        }
    }
}
