package com.example.taksy.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taksy.R
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int,
    val accentColor: Color
)

private val onboardingPages = listOf(
    OnboardingPage(Icons.AutoMirrored.Filled.List, R.string.onboarding_title_1, R.string.onboarding_desc_1, Color(0xFF1E88E5)),
    OnboardingPage(Icons.Default.CheckCircle, R.string.onboarding_title_2, R.string.onboarding_desc_2, Color(0xFF43A047)),
    OnboardingPage(Icons.Default.Notifications, R.string.onboarding_title_3, R.string.onboarding_desc_3, Color(0xFF7E57C2))
)

/**
 * Presentación del primer arranque: tres páginas deslizables; "Saltar" o "Comenzar" la cierran.
 * Mismo lenguaje visual que la pantalla de inicio (HomePalette + Plus Jakarta Sans); cada página
 * tiene su color de acento, que también tiñe el botón y el indicador.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    isDarkMode: Boolean = false
) {
    val palette = if (isDarkMode) HomePalette.Dark else HomePalette.Light
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex
    val accent by animateColorAsState(onboardingPages[pagerState.currentPage].accentColor, label = "accent")

    Scaffold(containerColor = palette.background) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ticksy_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                )
                Text(
                    text = stringResource(R.string.app_name),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = palette.ink,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .weight(1f)
                )
                if (!isLastPage) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = palette.muted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button, onClick = onFinish)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                OnboardingPageContent(onboardingPages[page], palette)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                onboardingPages.indices.forEach { index ->
                    val selected = pagerState.currentPage == index
                    val width by animateDpAsState(if (selected) 26.dp else 8.dp, label = "indicator")
                    Box(
                        Modifier
                            .size(width = width, height = 8.dp)
                            .clip(CircleShape)
                            .background(if (selected) accent else palette.track)
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(accent)
                    .clickable(role = Role.Button) {
                        if (isLastPage) {
                            onFinish()
                        } else {
                            coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    }
            ) {
                Text(
                    text = stringResource(if (isLastPage) R.string.onboarding_start else R.string.onboarding_next),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, palette: HomePalette) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(page.accentColor.copy(alpha = if (palette.isDark) 0.16f else 0.1f))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(page.accentColor)
            ) {
                Icon(page.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
            }
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text = stringResource(page.titleRes),
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 30.sp,
            lineHeight = 34.sp,
            letterSpacing = (-1).sp,
            textAlign = TextAlign.Center,
            color = palette.ink
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = stringResource(page.descriptionRes),
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            color = palette.muted
        )
    }
}
