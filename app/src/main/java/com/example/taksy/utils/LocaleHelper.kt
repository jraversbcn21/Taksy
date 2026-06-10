package com.example.taksy.utils

import android.content.Context
import android.content.res.Configuration
import com.example.taksy.data.preferences.PreferencesRepository
import java.util.Locale

object LocaleHelper {

    fun wrap(context: Context, language: String = getCurrentLanguage(context)): Context {
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun getCurrentLanguage(context: Context): String =
        PreferencesRepository(context.applicationContext).getLanguage()
}
