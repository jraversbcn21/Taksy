package com.example.taksy.service

import com.example.taksy.data.Reminder

interface ReminderSchedulerContract {
    fun scheduleReminder(reminder: Reminder)
    fun cancelReminder(reminderId: Long)
}
