package com.example.taksy.repository

import com.example.taksy.data.Category
import com.example.taksy.data.CategoryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryRepositoryTest {

    private class FakeCategoryDao(private var count: Int = 0) : CategoryDao {
        val insertedBatches = mutableListOf<List<Category>>()
        val categories = mutableListOf<Category>()

        override suspend fun getCategoryCount(): Int = count
        override suspend fun insertCategories(categories: List<Category>) {
            insertedBatches.add(categories)
            this.categories.addAll(categories)
            count += categories.size
        }
        override fun getAllCategories(): Flow<List<Category>> = flowOf(categories.toList())
        override suspend fun getAllCategoriesSync(): List<Category> = categories.toList()
        override suspend fun getCategoryById(id: Long): Category? = categories.find { it.id == id }
        override suspend fun insertCategory(category: Category): Long {
            categories.add(category); count++; return categories.size.toLong()
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
            count--
        }
        override suspend fun deleteAllCategories() { categories.clear(); count = 0 }
        override suspend fun updateCategoryOrder(categoryId: Long, newOrder: Int) {
            val idx = categories.indexOfFirst { it.id == categoryId }
            if (idx >= 0) categories[idx] = categories[idx].copy(orden = newOrder)
        }
    }

    private fun makeRepo(initialCount: Int = 0): Pair<FakeCategoryDao, CategoryRepository> {
        val dao = FakeCategoryDao(count = initialCount)
        return dao to CategoryRepository(dao)
    }

    // ── initializeDefaultCategories ───────────────────────────────────────

    @Test
    fun `initializeDefaultCategories inserts when empty`() = runTest {
        val (dao, repo) = makeRepo(0)
        repo.initializeDefaultCategories()
        assertEquals(1, dao.insertedBatches.size)
    }

    @Test
    fun `initializeDefaultCategories skips when categories exist`() = runTest {
        val (dao, repo) = makeRepo(3)
        repo.initializeDefaultCategories()
        assertEquals(0, dao.insertedBatches.size)
    }

    @Test
    fun `initializeDefaultCategories idempotent on repeated calls`() = runTest {
        val (dao, repo) = makeRepo(0)
        repo.initializeDefaultCategories()
        repo.initializeDefaultCategories()
        assertEquals(1, dao.insertedBatches.size)
    }

    @Test
    fun `initializeDefaultCategories inserts 8 default categories`() = runTest {
        val (dao, repo) = makeRepo(0)
        repo.initializeDefaultCategories()
        assertEquals(8, dao.insertedBatches[0].size)
    }

    // ── CRUD ──────────────────────────────────────────────────────────────

    @Test
    fun `insertCategory stores category`() = runTest {
        val (dao, repo) = makeRepo()
        val cat = Category(id = 1, nombre = "Test", color = "#FFF")
        repo.insertCategory(cat)
        assertEquals(1, dao.categories.size)
        assertEquals("Test", dao.categories[0].nombre)
    }

    @Test
    fun `getCategoryById returns correct category`() = runTest {
        val (dao, repo) = makeRepo()
        dao.categories.add(Category(id = 5, nombre = "Find me", color = "#000"))
        dao.categories.add(Category(id = 6, nombre = "Not me", color = "#111"))

        val result = repo.getCategoryById(5)
        assertEquals("Find me", result?.nombre)
    }

    @Test
    fun `getCategoryById returns null for missing id`() = runTest {
        val (_, repo) = makeRepo()
        assertNull(repo.getCategoryById(999))
    }

    @Test
    fun `updateCategory modifies existing`() = runTest {
        val (dao, repo) = makeRepo()
        val cat = Category(id = 1, nombre = "Old", color = "#FFF")
        dao.categories.add(cat)

        repo.updateCategory(cat.copy(nombre = "New"))

        assertEquals("New", dao.categories[0].nombre)
    }

    @Test
    fun `deleteCategory removes specific category`() = runTest {
        val (dao, repo) = makeRepo()
        val cat1 = Category(id = 1, nombre = "Keep", color = "#FFF")
        val cat2 = Category(id = 2, nombre = "Delete", color = "#000")
        dao.categories.addAll(listOf(cat1, cat2))

        repo.deleteCategory(cat2)

        assertEquals(1, dao.categories.size)
        assertEquals("Keep", dao.categories[0].nombre)
    }

    @Test
    fun `getAllCategories returns all stored`() = runTest {
        val (dao, repo) = makeRepo()
        dao.categories.add(Category(id = 1, nombre = "A", color = "#FFF"))
        dao.categories.add(Category(id = 2, nombre = "B", color = "#000"))

        val result = repo.getAllCategories().first()
        assertEquals(2, result.size)
    }

    // ── Reorder ───────────────────────────────────────────────────────────

    @Test
    fun `reorderCategories updates categories`() = runTest {
        val (dao, repo) = makeRepo()
        val cat1 = Category(id = 1, nombre = "A", color = "#FFF", orden = 0)
        val cat2 = Category(id = 2, nombre = "B", color = "#000", orden = 1)
        dao.categories.addAll(listOf(cat1, cat2))

        repo.reorderCategories(listOf(
            cat1.copy(orden = 1),
            cat2.copy(orden = 0)
        ))

        assertEquals(1, dao.categories.find { it.id == 1L }?.orden)
        assertEquals(0, dao.categories.find { it.id == 2L }?.orden)
    }
}
