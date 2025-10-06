package com.mpdc4gsr.libunified.app.compose.components
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
fun MenuEditView(
    @DrawableRes menu1Icon: Int,
    @DrawableRes menu2Icon: Int,
    @DrawableRes menu3Icon: Int,
    @DrawableRes menu4Icon: Int,
    menu1Label: String = "Menu 1",
    menu2Label: String = "Menu 2",
    menu3Label: String = "Menu 3",
    menu4Label: String = "Bar",
    selectedPosition: Int = -1,
    isBarSelected: Boolean = false,
    onMenuItemClick: (Int) -> Unit = {},
    onBarClick: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF2C2F33))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Menu 1
        MenuEditItem(
            iconRes = menu1Icon,
            label = menu1Label,
            isSelected = selectedPosition == 0,
            onClick = { onMenuItemClick(0) }
        )
        // Menu 2
        MenuEditItem(
            iconRes = menu2Icon,
            label = menu2Label,
            isSelected = selectedPosition == 1,
            onClick = { onMenuItemClick(1) }
        )
        // Menu 3
        MenuEditItem(
            iconRes = menu3Icon,
            label = menu3Label,
            isSelected = selectedPosition == 2,
            onClick = { onMenuItemClick(2) }
        )
        // Menu 4 (Bar Toggle)
        MenuEditItem(
            iconRes = menu4Icon,
            label = menu4Label,
            isSelected = isBarSelected,
            onClick = { onBarClick(!isBarSelected) }
        )
    }
}
@Composable
private fun MenuEditItem(
    @DrawableRes iconRes: Int? = null,
    icon: ImageVector? = null,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                icon != null -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(32.dp),
                        tint = if (isSelected) Color.White else Color.Gray
                    )
                }
                iconRes != null -> {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = label,
                        modifier = Modifier.size(32.dp),
                        colorFilter = ColorFilter.tint(
                            if (isSelected) Color.White else Color.Gray
                        )
                    )
                }
            }
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) Color.White else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
@Composable
fun CameraMenuView(
    @DrawableRes actionIcon: Int? = null,
    actionIconVector: ImageVector? = null,
    @DrawableRes galleryIcon: Int? = null,
    galleryIconVector: ImageVector? = null,
    @DrawableRes moreIcon: Int? = null,
    moreIconVector: ImageVector? = null,
    isVideoMode: Boolean = false,
    canSwitchMode: Boolean = true,
    onPhotoClick: () -> Unit = {},
    onVideoToggle: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onModeSwitch: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1C1E))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mode selector (Photo/Video)
        if (canSwitchMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Photo",
                    fontSize = 16.sp,
                    color = if (!isVideoMode) Color.White else Color.Gray,
                    fontWeight = if (!isVideoMode) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clickable { onModeSwitch(false) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(20.dp)
                        .background(Color.Gray)
                )
                Text(
                    text = "Video",
                    fontSize = 16.sp,
                    color = if (isVideoMode) Color.White else Color.Gray,
                    fontWeight = if (isVideoMode) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clickable { onModeSwitch(true) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        // Camera controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onGalleryClick)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    galleryIconVector != null -> {
                        Icon(
                            imageVector = galleryIconVector,
                            contentDescription = "Gallery",
                            tint = Color.White,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    galleryIcon != null -> {
                        Image(
                            painter = painterResource(id = galleryIcon),
                            contentDescription = "Gallery",
                            modifier = Modifier.fillMaxSize(),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }
                }
            }
            // Main action button (Photo or Video)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        if (isVideoMode) Color.Red else Color.White
                    )
                    .clickable {
                        if (isVideoMode) {
                            onVideoToggle()
                        } else {
                            onPhotoClick()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isVideoMode) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.White, RoundedCornerShape(4.dp))
                    )
                }
            }
            // More button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onMoreClick)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    moreIconVector != null -> {
                        Icon(
                            imageVector = moreIconVector,
                            contentDescription = "More",
                            tint = Color.White,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    moreIcon != null -> {
                        Image(
                            painter = painterResource(id = moreIcon),
                            contentDescription = "More",
                            modifier = Modifier.fillMaxSize(),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }
                }
            }
        }
    }
}
