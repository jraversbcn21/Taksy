package com.example.taksy.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.util.*

/**
 * Composable que maneja el cambio de idioma dinámicamente
 * Aplica el idioma seleccionado a todo el contenido de la aplicación
 */
@Composable
fun LanguageAwareContent(
    language: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    Log.d("LanguageAwareContent", "LanguageAwareContent creado con idioma: $language")
    
    // Aplicar el idioma cuando cambie
    LaunchedEffect(language) {
        Log.d("LanguageAwareContent", "Aplicando idioma: $language")
        LocaleHelper.applyLocale(context, language)
    }
    
    // Recrear el contexto con el idioma correcto
    val updatedContext = remember(language) {
        Log.d("LanguageAwareContent", "Recreando contexto con idioma: $language")
        LocaleHelper.setLocale(context, language)
    }
    
    // Forzar recomposición cuando cambie el idioma
    var recompositionKey by remember { mutableStateOf(0) }
    LaunchedEffect(language) {
        Log.d("LanguageAwareContent", "Incrementando recompositionKey para idioma: $language")
        recompositionKey++
    }
    
    CompositionLocalProvider(
        LocalContext provides updatedContext
    ) {
        // Usar key para forzar recomposición
        key(recompositionKey) {
            Log.d("LanguageAwareContent", "Renderizando contenido con recompositionKey: $recompositionKey")
            content()
        }
    }
}
