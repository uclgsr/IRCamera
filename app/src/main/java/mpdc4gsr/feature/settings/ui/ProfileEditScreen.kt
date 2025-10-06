package mpdc4gsr.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme

/**
 * Profile Edit Screen - Interface for editing user profile information
 * Allows users to update personal details, research information, and preferences
 */
@Composable
fun ProfileEditScreen(
    onBackClick: (() -> Unit)? = null,
    onSave: ((ProfileData) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var userName by remember { mutableStateOf("Research Participant") }
    var userId by remember { mutableStateOf("RP-2025-001") }
    var email by remember { mutableStateOf("participant@research.edu") }
    var institution by remember { mutableStateOf("University Research Lab") }
    var researchArea by remember { mutableStateOf("Physiological Computing") }
    var bio by remember { mutableStateOf("Conducting multi-modal sensor research") }
    var showSaveDialog by remember { mutableStateOf(false) }

    IRCameraTheme {
        Scaffold(
            topBar = {
                TitleBar(
                    title = "Edit Profile",
                    showBackButton = true,
                    onBackClick = onBackClick
                )
            },
            containerColor = Color(0xFF16131e)
        ) { paddingValues ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Picture Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { /* TODO: Photo picker */ }
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Change Photo")
                        }
                    }
                }

                // Basic Information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Basic Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = userName,
                            onValueChange = { userName = it },
                            label = { Text("Name") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = userId,
                            onValueChange = { userId = it },
                            label = { Text("User ID") },
                            leadingIcon = {
                                Icon(Icons.Default.Badge, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Research Information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Research Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = institution,
                            onValueChange = { institution = it },
                            label = { Text("Institution") },
                            leadingIcon = {
                                Icon(Icons.Default.School, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = researchArea,
                            onValueChange = { researchArea = it },
                            label = { Text("Research Area") },
                            leadingIcon = {
                                Icon(Icons.Default.Science, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Bio") },
                            leadingIcon = {
                                Icon(Icons.Default.Description, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                }

                // Privacy Settings
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Privacy Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Profile Visible to Researchers")
                            Switch(
                                checked = true,
                                onCheckedChange = { }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Share Anonymized Data")
                            Switch(
                                checked = false,
                                onCheckedChange = { }
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onBackClick?.invoke() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val profileData = ProfileData(
                                userName, userId, email, institution, researchArea, bio
                            )
                            onSave?.invoke(profileData)
                            showSaveDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            }
        }

        // Save Confirmation Dialog
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Profile Updated") },
                text = { Text("Your profile has been successfully updated.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSaveDialog = false
                            onBackClick?.invoke()
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

/**
 * Data class for profile information
 */
data class ProfileData(
    val userName: String,
    val userId: String,
    val email: String,
    val institution: String,
    val researchArea: String,
    val bio: String
)

@Preview(showBackground = true)
@Composable
private fun ProfileEditScreenPreview() {
    IRCameraTheme {
        ProfileEditScreen()
    }
}
