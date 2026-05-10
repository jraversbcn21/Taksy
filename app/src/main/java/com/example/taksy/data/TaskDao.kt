package com.example.taksy.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para operaciones de base de datos de tareas
 */
@Dao
interface TaskDao {
    
    /**
     * Obtiene todas las tareas ordenadas: pendientes primero, completadas al final
     */
    @Query("SELECT * FROM tasks WHERE archivada = 0 ORDER BY CASE WHEN estado = 'PENDIENTE' THEN 0 ELSE 1 END, orden ASC, CASE prioridad WHEN 'ALTA' THEN 0 WHEN 'MEDIA' THEN 1 WHEN 'BAJA' THEN 2 ELSE 3 END, fechaCreacion DESC")
    fun getAllTasks(): Flow<List<Task>>
    
    /**
     * Obtiene todas las tareas pendientes
     */
    @Query("SELECT * FROM tasks WHERE estado = 'PENDIENTE' AND archivada = 0 ORDER BY orden ASC, CASE prioridad WHEN 'ALTA' THEN 0 WHEN 'MEDIA' THEN 1 WHEN 'BAJA' THEN 2 ELSE 3 END, fechaCreacion DESC")
    fun getPendingTasks(): Flow<List<Task>>
    
    /**
     * Obtiene todas las tareas completadas
     */
    @Query("SELECT * FROM tasks WHERE estado = 'COMPLETADA' AND archivada = 0 ORDER BY fechaCreacion DESC")
    fun getCompletedTasks(): Flow<List<Task>>
    
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
     * Marca una tarea como completada
     */
    @Query("UPDATE tasks SET estado = 'COMPLETADA' WHERE id = :id")
    suspend fun markTaskAsCompleted(id: Long)
    
    /**
     * Marca una tarea como pendiente
     */
    @Query("UPDATE tasks SET estado = 'PENDIENTE' WHERE id = :id")
    suspend fun markTaskAsPending(id: Long)
    
    /**
     * Elimina todas las tareas completadas
     */
    @Query("DELETE FROM tasks WHERE estado = 'COMPLETADA'")
    suspend fun deleteCompletedTasks()
    
    /**
     * Obtiene tareas por categoría
     */
    @Query("SELECT * FROM tasks WHERE categoriaId = :categoryId AND archivada = 0 ORDER BY CASE WHEN estado = 'PENDIENTE' THEN 0 ELSE 1 END, orden ASC, CASE prioridad WHEN 'ALTA' THEN 0 WHEN 'MEDIA' THEN 1 WHEN 'BAJA' THEN 2 ELSE 3 END, fechaCreacion DESC")
    fun getTasksByCategory(categoryId: Long): Flow<List<Task>>
    
    /**
     * Obtiene tareas sin categoría
     */
    @Query("SELECT * FROM tasks WHERE categoriaId IS NULL AND archivada = 0 ORDER BY fechaCreacion DESC")
    fun getTasksWithoutCategory(): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY id ASC")
    suspend fun getAllTasksSync(): List<Task>

    @Query("SELECT * FROM tasks WHERE estado = 'PENDIENTE' AND archivada = 0 ORDER BY orden ASC, CASE prioridad WHEN 'ALTA' THEN 0 WHEN 'MEDIA' THEN 1 WHEN 'BAJA' THEN 2 ELSE 3 END, fechaVencimiento ASC, fechaCreacion DESC")
    fun getPendingTasksSync(): List<Task>

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
    
    /**
     * Obtiene tareas que vencen hoy
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE fechaVencimiento IS NOT NULL 
        AND fechaVencimiento >= :startOfDay 
        AND fechaVencimiento < :endOfDay
        ORDER BY fechaCreacion DESC
    """)
    suspend fun getTasksDueToday(startOfDay: Long, endOfDay: Long): List<Task>
    
    /**
     * Obtiene tareas que vencen pronto (próximos 7 días)
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE fechaVencimiento IS NOT NULL 
        AND fechaVencimiento >= :now 
        AND fechaVencimiento <= :nextWeek
        ORDER BY fechaVencimiento ASC
    """)
    suspend fun getTasksDueSoon(now: Long, nextWeek: Long): List<Task>
    
    /**
     * Obtiene tareas vencidas
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE fechaVencimiento IS NOT NULL 
        AND fechaVencimiento < :now
        AND estado = 'PENDIENTE'
        ORDER BY fechaVencimiento ASC
    """)
    suspend fun getOverdueTasks(now: Long): List<Task>
    
    /**
     * Busca tareas por título dentro de una categoría
     */
    @Query("SELECT * FROM tasks WHERE categoriaId = :categoryId AND archivada = 0 AND titulo LIKE '%' || :query || '%' ORDER BY CASE WHEN estado = 'PENDIENTE' THEN 0 ELSE 1 END, CASE prioridad WHEN 'ALTA' THEN 0 WHEN 'MEDIA' THEN 1 WHEN 'BAJA' THEN 2 ELSE 3 END, fechaCreacion DESC")
    fun searchTasksByCategory(categoryId: Long, query: String): Flow<List<Task>>

    /**
     * Busca tareas por título en todas las categorías
     */
    @Query("SELECT * FROM tasks WHERE archivada = 0 AND titulo LIKE '%' || :query || '%' ORDER BY CASE WHEN estado = 'PENDIENTE' THEN 0 ELSE 1 END, CASE prioridad WHEN 'ALTA' THEN 0 WHEN 'MEDIA' THEN 1 WHEN 'BAJA' THEN 2 ELSE 3 END, fechaCreacion DESC")
    fun searchAllTasks(query: String): Flow<List<Task>>

    /**
     * Obtiene tareas que están realmente completadas (con todas las subtareas completadas)
     * Esto requiere una consulta más compleja que considere las subtareas
     */
    @Query("""
        SELECT DISTINCT t.* FROM tasks t
        WHERE t.id IN (
            SELECT s.taskId FROM subtasks s
            WHERE s.taskId = t.id
            GROUP BY s.taskId
            HAVING COUNT(*) = (
                SELECT COUNT(*) FROM subtasks s2 
                WHERE s2.taskId = s.taskId AND s2.estado = 'COMPLETADA'
            )
        )
        OR (t.id NOT IN (SELECT DISTINCT taskId FROM subtasks) AND t.estado = 'COMPLETADA')
        ORDER BY t.fechaCreacion DESC
    """)
    fun getReallyCompletedTasks(): Flow<List<Task>>
    
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

    @Query("SELECT * FROM tasks WHERE archivada = 1 AND categoriaId = :categoryId ORDER BY fechaCreacion DESC")
    fun getArchivedTasksByCategory(categoryId: Long): Flow<List<Task>>

    @Query("UPDATE tasks SET archivada = 1 WHERE id = :taskId")
    suspend fun archiveTask(taskId: Long)

    @Query("UPDATE tasks SET archivada = 0 WHERE id = :taskId")
    suspend fun unarchiveTask(taskId: Long)

    @Update
    suspend fun updateTasks(tasks: List<Task>)
}
