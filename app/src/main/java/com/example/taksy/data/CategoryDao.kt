package com.example.taksy.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones de categorías
 */
@Dao
interface CategoryDao {
    
    /**
     * Obtiene todas las categorías ordenadas por el campo orden
     */
    @Query("SELECT * FROM categories ORDER BY orden ASC, nombre ASC")
    fun getAllCategories(): Flow<List<Category>>
    
    /**
     * Obtiene todas las categorías de forma síncrona (para verificar si existen)
     */
    @Query("SELECT * FROM categories ORDER BY orden ASC, nombre ASC")
    suspend fun getAllCategoriesSync(): List<Category>
    
    /**
     * Obtiene una categoría por ID
     */
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?
    
    /**
     * Inserta una nueva categoría
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long
    
    /**
     * Actualiza una categoría existente
     */
    @Update
    suspend fun updateCategory(category: Category)
    
    /**
     * Elimina una categoría
     */
    @Delete
    suspend fun deleteCategory(category: Category)
    
    /**
     * Cuenta el total de categorías existentes
     */
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    /**
     * Elimina todas las categorías
     */
    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
    
    /**
     * Inserta múltiples categorías (para inicialización)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)
    
    /**
     * Actualiza el orden de una categoría
     */
    @Query("UPDATE categories SET orden = :newOrder WHERE id = :categoryId")
    suspend fun updateCategoryOrder(categoryId: Long, newOrder: Int)
    
    /**
     * Actualiza múltiples categorías (para reordenamiento)
     */
    @Update
    suspend fun updateCategories(categories: List<Category>)
}
