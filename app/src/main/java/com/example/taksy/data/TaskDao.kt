package com.example.taksy.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * DAO (Data Access Object) para operaciones de base de datos de tareas
 */
@Dao
interface TaskDao {
    
    /**
     * Obtiene todas las tareas ordenadas: pendientes primero, completadas al final
     */
    @Query("SELECT * FROM tasks ORDER BY CASE WHEN estado = 'PENDIENTE' THEN 0 ELSE 1 END, CASE prioridad WHEN 'ALTA' THEN 0 WHEN 'MEDIA' THEN 1 WHEN 'BAJA' THEN 2 ELSE 3 END, fechaCreacion DESC")
    fun getAllTasks(): Flow<List<Task>>
    
    /**
     * Obtiene una tarea por su ID
     */
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?
    
    /**
     * Inserta una nueva tarea
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long
    
    /**
     * Actualiza una tarea existente
     */
    @Update
    suspend fun updateTask(task: Task)
    
    /**
     * Elimina una tarea
     */
    @Delete
    suspend fun deleteTask(task: Task)
    
    /**
     * Marca una tarea como completada solo si sigue pendiente (compare-and-swap).
     * Devuelve el número de filas afectadas: 1 si esta llamada ganó la carrera,
     * 0 si otro caller ya la completó.
     */
    @Query("UPDATE tasks SET estado = 'COMPLETADA', fechaCompletada = :fechaCompletada WHERE id = :id AND estado = 'PENDIENTE'")
    suspend fun completeTaskIfPending(id: Long, fechaCompletada: Date): Int

    /**
     * Marca una tarea como pendiente
     */
    @Query("UPDATE tasks SET estado = 'PENDIENTE', fechaCompletada = NULL WHERE id = :id")
    suspend fun markTaskAsPending(id: Long)
    
    
    /**
     * Obtiene tareas por categoría.
     * Oculta las ocurrencias ya completadas de tareas recurrentes: al completarlas se crea
     * la siguiente ocurrencia, así que solo esa es relevante en la lista. Siguen en la base
     * de datos (estadísticas, backup).
     */
    @Query("SELECT * FROM tasks WHERE categoriaId = :categoryId AND NOT (estado = 'COMPLETADA' AND recurrencia != 'NINGUNA') ORDER BY CASE WHEN estado = 'PENDIENTE' THEN 0 ELSE 1 END, CASE prioridad WHEN 'ALTA' THEN 0 WHEN 'MEDIA' THEN 1 WHEN 'BAJA' THEN 2 ELSE 3 END, fechaVencimiento IS NULL, fechaVencimiento ASC, fechaCreacion DESC")
    fun getTasksByCategory(categoryId: Long): Flow<List<Task>>

    /** Todas las tareas de una categoría, incluidas las completadas (para borrarla). */
    @Query("SELECT * FROM tasks WHERE categoriaId = :categoryId")
    suspend fun getAllTasksByCategorySync(categoryId: Long): List<Task>

    @Query("SELECT * FROM tasks ORDER BY id ASC")
    suspend fun getAllTasksSync(): List<Task>

    @Query("SELECT * FROM tasks WHERE estado = 'PENDIENTE' ORDER BY CASE prioridad WHEN 'ALTA' THEN 0 WHEN 'MEDIA' THEN 1 WHEN 'BAJA' THEN 2 ELSE 3 END, fechaVencimiento IS NULL, fechaVencimiento ASC, fechaCreacion DESC")
    fun getPendingTasksSync(): List<Task>

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
    
    /**
     * Busca tareas por título dentro de una categoría
     */
    @Query("SELECT * FROM tasks WHERE categoriaId = :categoryId AND NOT (estado = 'COMPLETADA' AND recurrencia != 'NINGUNA') AND titulo LIKE '%' || :query || '%' ORDER BY CASE WHEN estado = 'PENDIENTE' THEN 0 ELSE 1 END, CASE prioridad WHEN 'ALTA' THEN 0 WHEN 'MEDIA' THEN 1 WHEN 'BAJA' THEN 2 ELSE 3 END, fechaCreacion DESC")
    fun searchTasksByCategory(categoryId: Long, query: String): Flow<List<Task>>

    /**
     * Busca tareas por título en todas las categorías. Como en la lista de una categoría, las
     * ocurrencias completadas de tareas recurrentes no salen (parecerían duplicados).
     */
    @Query("SELECT * FROM tasks WHERE NOT (estado = 'COMPLETADA' AND recurrencia != 'NINGUNA') AND titulo LIKE '%' || :query || '%' ORDER BY CASE WHEN estado = 'PENDIENTE' THEN 0 ELSE 1 END, CASE prioridad WHEN 'ALTA' THEN 0 WHEN 'MEDIA' THEN 1 WHEN 'BAJA' THEN 2 ELSE 3 END, fechaCreacion DESC")
    fun searchAllTasks(query: String): Flow<List<Task>>
    
    /**
     * Obtiene tareas que están realmente pendientes (no completamente terminadas)
     */
    @Query("""
        SELECT DISTINCT t.* FROM tasks t
        WHERE t.id NOT IN (
            SELECT s.taskId FROM subtasks s
            WHERE s.taskId = t.id
            GROUP BY s.taskId
            HAVING COUNT(*) = (
                SELECT COUNT(*) FROM subtasks s2 
                WHERE s2.taskId = s.taskId AND s2.estado = 'COMPLETADA'
            )
        )
        AND NOT (t.id NOT IN (SELECT DISTINCT taskId FROM subtasks) AND t.estado = 'COMPLETADA')
        ORDER BY t.fechaCreacion DESC
    """)
    fun getReallyPendingTasks(): Flow<List<Task>>
}
