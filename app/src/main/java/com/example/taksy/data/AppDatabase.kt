package com.example.taksy.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

/**
 * Base de datos principal de la aplicación usando Room
 */
@Database(
    entities = [Task::class, Subtask::class, Category::class, Reminder::class],
    version = 9,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun reminderDao(): ReminderDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // Migración de la versión 1 a la 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No hay cambios en el esquema, solo en el ordenamiento
                // La migración se ejecuta automáticamente
            }
        }
        
        // Migración de la versión 2 a la 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Crear tabla temporal sin la columna descripcion
                database.execSQL("""
                    CREATE TABLE tasks_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        titulo TEXT NOT NULL,
                        fechaCreacion INTEGER NOT NULL,
                        estado TEXT NOT NULL DEFAULT 'PENDIENTE'
                    )
                """)
                
                // Copiar datos de la tabla antigua a la nueva
                database.execSQL("""
                    INSERT INTO tasks_new (id, titulo, fechaCreacion, estado)
                    SELECT id, titulo, fechaCreacion, estado FROM tasks
                """)
                
                // Eliminar tabla antigua
                database.execSQL("DROP TABLE tasks")
                
                // Renombrar tabla nueva
                database.execSQL("ALTER TABLE tasks_new RENAME TO tasks")
                
                // Crear tabla de subtareas
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS subtasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        taskId INTEGER NOT NULL,
                        titulo TEXT NOT NULL,
                        estado TEXT NOT NULL DEFAULT 'PENDIENTE'
                    )
                """)
            }
        }
        
        // Migración de la versión 3 a la 4 - Agregar fechaVencimiento
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Agregar columna fechaVencimiento a la tabla tasks
                database.execSQL("ALTER TABLE tasks ADD COLUMN fechaVencimiento INTEGER")
            }
        }
        
        // Migración de la versión 4 a la 5 - Agregar categorías
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Agregar columna categoriaId a la tabla tasks
                database.execSQL("ALTER TABLE tasks ADD COLUMN categoriaId INTEGER")
                
                // Crear tabla de categorías
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nombre TEXT NOT NULL,
                        color TEXT NOT NULL,
                        icono TEXT NOT NULL DEFAULT 'label'
                    )
                """)
                
                // Insertar categorías predefinidas
                database.execSQL("""
                    INSERT OR REPLACE INTO categories (id, nombre, color, icono) VALUES
                    (1, 'Trabajo', '#2196F3', 'work'),
                    (2, 'Personal', '#4CAF50', 'person'),
                    (3, 'Compras', '#FF9800', 'shopping_cart'),
                    (4, 'Salud', '#F44336', 'health'),
                    (5, 'Estudio', '#9C27B0', 'school'),
                    (6, 'Hogar', '#795548', 'home')
                """)
            }
        }
        
        // Migración de la versión 5 a la 6 - Agregar recordatorios
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS reminders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        taskId INTEGER NOT NULL,
                        titulo TEXT NOT NULL,
                        descripcion TEXT,
                        fechaRecordatorio INTEGER NOT NULL,
                        activo INTEGER NOT NULL,
                        tipoRecordatorio TEXT NOT NULL,
                        fechaCreacion INTEGER NOT NULL,
                        FOREIGN KEY(taskId) REFERENCES tasks(id) ON DELETE CASCADE
                    )
                """)
            }
        }
        
        // Migración de la versión 6 a la 7 - Corregir tabla reminders
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Eliminar la tabla reminders mal creada
                database.execSQL("DROP TABLE IF EXISTS reminders")
                
                // Crear la tabla reminders correctamente
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS reminders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        taskId INTEGER NOT NULL,
                        titulo TEXT NOT NULL,
                        descripcion TEXT,
                        fechaRecordatorio INTEGER NOT NULL,
                        activo INTEGER NOT NULL,
                        tipoRecordatorio TEXT NOT NULL,
                        fechaCreacion INTEGER NOT NULL,
                        FOREIGN KEY(taskId) REFERENCES tasks(id) ON DELETE CASCADE
                    )
                """)
            }
        }
        
        // Migración de la versión 7 a la 8 - Agregar campo orden a categorías
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Agregar columna orden a la tabla categories
                database.execSQL("ALTER TABLE categories ADD COLUMN orden INTEGER NOT NULL DEFAULT 0")

                // Actualizar el orden de las categorías existentes
                database.execSQL("UPDATE categories SET orden = id WHERE orden = 0")
            }
        }

        // Migración de la versión 8 a la 9 - Agregar prioridad a tareas
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN prioridad TEXT NOT NULL DEFAULT 'NINGUNA'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ticksy_database"
                )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
