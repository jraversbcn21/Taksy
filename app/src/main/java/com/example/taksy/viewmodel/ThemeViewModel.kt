package com.example.taksy.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import com.example.taksy.data.preferences.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val preferences: PreferencesRepository
) : ViewModel() {

    private val _isDarkMode = MutableStateFlow(preferences.isDarkMode())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _currentLanguage = MutableStateFlow(preferences.getLanguage())
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun setDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
        preferences.setDarkMode(isDark)
    }

    fun setLanguage(language: String) {
        if (_currentLanguage.value == language) return
        preferences.setLanguage(language)
        _currentLanguage.value = language
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
    }
}
