package com.example.taksy.di

import android.content.Context
import com.example.taksy.data.AppDatabase
import com.example.taksy.data.CategoryDao
import com.example.taksy.data.ReminderDao
import com.example.taksy.data.SubtaskDao
import com.example.taksy.data.TaskDao
import com.example.taksy.repository.CategoryRepository
import com.example.taksy.repository.TaskRepository
import com.example.taksy.service.ReminderScheduler
import com.example.taksy.service.ReminderSchedulerContract
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    @Provides fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()
    @Provides fun provideSubtaskDao(db: AppDatabase): SubtaskDao = db.subtaskDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()

    @Provides
    @Singleton
    fun provideTaskRepository(
        appDatabase: AppDatabase,
        taskDao: TaskDao,
        subtaskDao: SubtaskDao,
        reminderDao: ReminderDao
    ): TaskRepository = TaskRepository(appDatabase, taskDao, subtaskDao, reminderDao)

    @Provides
    @Singleton
    fun provideCategoryRepository(categoryDao: CategoryDao): CategoryRepository =
        CategoryRepository(categoryDao)

    @Provides
    @Singleton
    fun provideReminderScheduler(@ApplicationContext context: Context): ReminderSchedulerContract =
        ReminderScheduler(context)
}
