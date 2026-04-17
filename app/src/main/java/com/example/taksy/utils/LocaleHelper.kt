package com.example.taksy.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import java.util.*

object LocaleHelper {
    private const val PREF_NAME = "language_pref"
    private const val SELECTED_LANGUAGE = "selected_language"
    
    fun setLocale(context: Context, language: String): Context {
        // Guardar el idioma seleccionado
        saveLanguage(context, language)
        
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
    
    fun applyLocale(context: Context, language: String) {
        Log.d("LocaleHelper", "applyLocale llamado con: $language")
        
        // Guardar el idioma seleccionado
        saveLanguage(context, language)
        Log.d("LocaleHelper", "Idioma guardado: $language")
        
        val locale = Locale(language)
        Locale.setDefault(locale)
        Log.d("LocaleHelper", "Locale por defecto establecido: $locale")
        
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            Log.d("LocaleHelper", "Configuración establecida para API >= N: $locale")
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            Log.d("LocaleHelper", "Configuración establecida para API < N: $locale")
        }
        
        // Aplicar la configuración
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Log.d("LocaleHelper", "API >= N_MR1, contexto se recrea automáticamente")
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            Log.d("LocaleHelper", "Configuración aplicada para API < N_MR1")
        }
        
        // Forzar actualización del contexto
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        Log.d("LocaleHelper", "Configuración forzada aplicada")
    }
    
    fun getCurrentLanguage(context: Context): String {
        val language = getSavedLanguage(context)
        Log.d("LocaleHelper", "getCurrentLanguage devuelve: $language")
        return language
    }
    
    fun saveLanguage(context: Context, language: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        Log.d("LocaleHelper", "Guardando idioma con PREF_NAME: $PREF_NAME, SELECTED_LANGUAGE: $SELECTED_LANGUAGE, language: $language")
        val result = prefs.edit().putString(SELECTED_LANGUAGE, language).apply()
        Log.d("LocaleHelper", "saveLanguage guardado: $language, result: $result")
        
        // Verificar que se guardó correctamente
        val savedValue = prefs.getString(SELECTED_LANGUAGE, "NO_ENCONTRADO")
        Log.d("LocaleHelper", "Verificación - valor guardado: $savedValue")
    }
    
    private fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        Log.d("LocaleHelper", "Leyendo preferencias con PREF_NAME: $PREF_NAME, SELECTED_LANGUAGE: $SELECTED_LANGUAGE")
        val language = prefs.getString(SELECTED_LANGUAGE, "es") ?: "es"
        Log.d("LocaleHelper", "getSavedLanguage leído: $language")
        Log.d("LocaleHelper", "Valor por defecto usado: ${prefs.getString(SELECTED_LANGUAGE, "es") == "es"}")
        return language
    }
    
    /**
     * Función para forzar la recreación de la actividad cuando cambie el idioma
     */
    fun recreateActivity(context: Context) {
        if (context is android.app.Activity) {
            context.recreate()
        }
    }
}
