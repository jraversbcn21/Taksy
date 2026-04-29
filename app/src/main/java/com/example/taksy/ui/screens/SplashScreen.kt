package com.example.taksy.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taksy.R
import com.example.taksy.ui.theme.SplashLightBlue
import com.example.taksy.ui.theme.SplashLightGreen
import com.example.taksy.ui.theme.SplashLightTeal
import com.example.taksy.ui.theme.SplashLightYellow
import kotlinx.coroutines.delay

/**
 * Pantalla de splash con animaciones para la aplicación Ticksy
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Animaciones
    val scaleAnimation = remember { Animatable(0.8f) } // Empezar más grande
    val alphaAnimation = remember { Animatable(0.3f) } // Empezar con algo de visibilidad
    val textAlphaAnimation = remember { Animatable(0f) }
    val pulseAnimation = remember { Animatable(1f) }
    
    // Iniciar animaciones
    LaunchedEffect(Unit) {
        // Primero hacer visible el icono
        alphaAnimation.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 500,
                easing = FastOutSlowInEasing
            )
        )
        
        // Luego animar la escala
        scaleAnimation.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 600,
                easing = FastOutSlowInEasing
            )
        )
        
        // Pequeña pausa
        delay(200)
        
        // Animación del texto
        textAlphaAnimation.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 400,
                easing = FastOutSlowInEasing
            )
        )
        
        // Pequeña pausa antes del pulso
        delay(300)
        
        // Animación de pulso sutil del icono
        pulseAnimation.animateTo(
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
        
        // Esperar antes de navegar (tiempo reducido para mejor UX)
        delay(3000)
        
        // Navegar a la pantalla principal
        onSplashFinished()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SplashLightYellow,  // Amarillo-verde suave (arriba)
                        SplashLightGreen,   // Verde suave
                        SplashLightTeal,    // Azul-verde suave
                        SplashLightBlue     // Azul suave (abajo)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icono de la aplicación con animaciones
            Image(
                painter = painterResource(id = R.drawable.ticksy_icon),
                contentDescription = "Ticksy Icon",
                modifier = Modifier
                    .size(80.dp)
                    .scale(scaleAnimation.value * pulseAnimation.value)
                    .alpha(alphaAnimation.value)
            )
            
            // Texto de la aplicación con animación
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp
                ),
                color = Color(0xFF2E7D32), // Verde oscuro para mejor contraste
                modifier = Modifier.alpha(textAlphaAnimation.value)
            )
            
            // Subtítulo con animación
            Text(
                text = stringResource(R.string.splash_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF388E3C), // Verde medio para mejor contraste
                modifier = Modifier.alpha(textAlphaAnimation.value)
            )
        }
    }
}
