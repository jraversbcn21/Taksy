package com.example.taksy.repository

import com.example.taksy.data.Category
import com.example.taksy.data.CategoryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryRepositoryTest {

    private class FakeCategoryDao(private var count: Int = 0) : CategoryDao {
        val insertedBatches = mutableListOf<List<Category>>()
        private val categories = mutableListOf<Category>()

        override suspend fun getCategoryCount(): Int = count
        override suspend fun insertCategories(categories: List<Category>) {
            insertedBatches.add(categories)
            count += categories.size
        }
        override fun getAllCategories(): Flow<List<Category>> = flowOf(categories)
        override suspend fun getAllCategoriesSync(): List<Category> = categories
        override suspend fun getCategoryById(id: Long): Category? = categories.find { it.id == id }
        override suspend fun insertCategory(category: Category): Long {
            categories.add(category); return categories.size.toLong()
        }
        override suspend fun updateCategory(category: Category) {
            val idx = categories.indexOfFirst { it.id == category.id }
            if (idx >= 0) categories[idx] = category
        }
        override suspend fun updateCategories(categories: List<Category>) {
            categories.forEach { updateCategory(it) }
        }
        override suspend fun deleteCategory(category: Category) {
            categories.removeIf { it.id == category.id }
        }
        override suspend fun deleteAllCategories() { categories.clear() }
        override suspend fun updateCategoryOrder(categoryId: Long, newOrder: Int) {}
    }

    @Test
    fun `initializeDefaultCategories inserts when empty`() = runTest {
        val dao = FakeCategoryDao(count = 0)
        val repo = CategoryRepository(dao)

        repo.initializeDefaultCategories()

        assertEquals(1, dao.insertedBatches.size)
    }

    @Test
    fun `initializeDefaultCategories skips when categories exist`() = runTest {
        val dao = FakeCategoryDao(count = 3)
        val repo = CategoryRepository(dao)

        repo.initializeDefaultCategories()

        assertEquals(0, dao.insertedBatches.size)
    }

    @Test
    fun `initializeDefaultCategories idempotent on repeated calls`() = runTest {
        val dao = FakeCategoryDao(count = 0)
        val repo = CategoryRepository(dao)

        repo.initializeDefaultCategories()
        repo.initializeDefaultCategories() // second call should be a no-op

        assertEquals(1, dao.insertedBatches.size)
    }
}
