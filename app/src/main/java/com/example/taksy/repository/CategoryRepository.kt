package com.example.taksy.repository

import com.example.taksy.data.Category
import com.example.taksy.data.CategoryDao
import com.example.taksy.data.DefaultCategories
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {

    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun getCategoryById(id: Long): Category? = categoryDao.getCategoryById(id)

    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)

    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    suspend fun reorderCategories(categories: List<Category>) =
        categoryDao.updateCategories(categories)

    /**
     * Inserta las categorías predefinidas solo en el primer inicio (cuando no hay ninguna).
     * Nunca borra categorías existentes.
     */
    suspend fun initializeDefaultCategories() {
        if (categoryDao.getCategoryCount() == 0) {
            categoryDao.insertCategories(DefaultCategories.getAll())
        }
    }
}
