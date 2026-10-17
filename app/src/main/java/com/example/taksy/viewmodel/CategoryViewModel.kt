package com.example.taksy.viewmodel

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taksy.R
import com.example.taksy.data.Category
import com.example.taksy.domain.DeleteCategoryUseCase
import com.example.taksy.repository.CategoryRepository
import com.example.taksy.widget.TaskWidgetProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: CategoryRepository,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _errors = Channel<Int>(Channel.BUFFERED)

    /** Errores de operaciones, como id de string. Evento de un solo uso: lo muestra MainActivity. */
    val errors: Flow<Int> = _errors.receiveAsFlow()

    init {
        launchSafe(R.string.error_generic) {
            repository.initializeDefaultCategories(context)
        }
    }

    fun getAllCategories(): Flow<List<Category>> = repository.getAllCategories()

    suspend fun getCategoryById(id: Long): Category? = repository.getCategoryById(id)

    fun addCategory(nombre: String, color: String, icono: String) {
        launchSafe(R.string.error_create_category) {
            repository.insertCategory(Category(nombre = nombre, color = color, icono = icono))
        }
    }

    fun updateCategory(category: Category) {
        launchSafe(R.string.error_update_category) { repository.updateCategory(category) }
    }

    fun deleteCategory(category: Category) {
        launchSafe(R.string.error_delete_category) {
            deleteCategoryUseCase.execute(category)
            TaskWidgetProvider.refreshAll(context)
        }
    }

    fun reorderCategories(categories: List<Category>) {
        launchSafe(R.string.error_reorder_categories) { repository.reorderCategories(categories) }
    }

    private fun launchSafe(@StringRes errorMessage: Int, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Category operation failed", e)
                _errors.trySend(errorMessage)
            }
        }
    }

    private companion object {
        const val TAG = "CategoryViewModel"
    }
}
