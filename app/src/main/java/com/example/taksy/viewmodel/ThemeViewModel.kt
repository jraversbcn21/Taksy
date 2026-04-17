package com.example.taksy.viewmodel

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import com.example.taksy.utils.LocaleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _currentLanguage = MutableStateFlow("es")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    init {
        _isDarkMode.value = getSavedTheme()
        _currentLanguage.value = LocaleHelper.getCurrentLanguage(context)
    }

    fun setDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
        context.getSharedPreferences("theme_pref", Context.MODE_PRIVATE)
            .edit().putBoolean("is_dark_mode", isDark).apply()
    }

    /**
     * Cambia el idioma de la app.
     * AppCompatDelegate.setApplicationLocales() gestiona la recreación de la
     * Activity en todos los niveles de API sin necesitar una referencia a Activity.
     */
    fun setLanguage(language: String) {
        if (_currentLanguage.value == language) return
        LocaleHelper.saveLanguage(context, language)
        _currentLanguage.value = language
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
    }

    private fun getSavedTheme(): Boolean =
        context.getSharedPreferences("theme_pref", Context.MODE_PRIVATE)
            .getBoolean("is_dark_mode", false)
}
