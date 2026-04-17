package com.example.taksy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taksy.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HamburgerMenu(
    isDarkMode: Boolean,
    currentLanguage: String,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Botón hamburguesa
    IconButton(
        onClick = onSettingsClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = stringResource(R.string.settings),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

