package com.example.taksy.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.util.*

@Composable
fun LanguageAwareContent(
    language: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(language) {
        LocaleHelper.applyLocale(context, language)
    }

    val updatedContext = remember(language) {
        LocaleHelper.setLocale(context, language)
    }

    var recompositionKey by remember { mutableStateOf(0) }
    LaunchedEffect(language) {
        recompositionKey++
    }

    CompositionLocalProvider(
        LocalContext provides updatedContext
    ) {
        key(recompositionKey) {
            content()
        }
    }
}
