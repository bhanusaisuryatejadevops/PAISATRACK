package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.BillReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillReminderDao {

    @Query("SELECT * FROM bill_reminders ORDER BY isPaidThisCycle ASC, dueDayOfMonth ASC")
    fun getAllReminders(): Flow<List<BillReminderEntity>>

    @Query("SELECT * FROM bill_reminders WHERE isPaidThisCycle = 0 ORDER BY dueDayOfMonth ASC")
    fun getPendingReminders(): Flow<List<BillReminderEntity>>

    @Query("SELECT * FROM bill_reminders")
    suspend fun getAllRemindersSnapshot(): List<BillReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: BillReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<BillReminderEntity>)

    @Update
    suspend fun updateReminder(reminder: BillReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: BillReminderEntity)

    @Query("DELETE FROM bill_reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bill_reminders")
    suspend fun clearAll()
}
