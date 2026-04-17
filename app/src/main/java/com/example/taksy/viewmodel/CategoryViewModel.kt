package com.example.taksy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taksy.data.Category
import com.example.taksy.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: CategoryRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.initializeDefaultCategories()
        }
    }

    fun getAllCategories(): Flow<List<Category>> = repository.getAllCategories()

    suspend fun getCategoryById(id: Long): Category? = repository.getCategoryById(id)

    fun addCategory(nombre: String, color: String, icono: String) {
        viewModelScope.launch {
            repository.insertCategory(Category(nombre = nombre, color = color, icono = icono))
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch { repository.updateCategory(category) }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch { repository.deleteCategory(category) }
    }

    fun reorderCategories(categories: List<Category>) {
        viewModelScope.launch { repository.reorderCategories(categories) }
    }
}
