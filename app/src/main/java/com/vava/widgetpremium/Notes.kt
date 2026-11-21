package com.vava.widgetpremium

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Ghi chú hoặc việc cần làm gắn với một ngày dương (dateKey dạng yyyy-MM-dd), có thể kèm giờ nhắc. */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val text: String,
    val remindAtMs: Long? = null,
    val done: Boolean = false
)

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY dateKey, id")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE remindAtMs IS NOT NULL AND done = 0 AND remindAtMs > :nowMs")
    suspend fun upcomingReminders(nowMs: Long): List<Note>

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)
}

@Database(entities = [Note::class], version = 1, exportSchema = false)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile private var instance: NoteDatabase? = null

        fun get(context: Context): NoteDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, NoteDatabase::class.java, "notes.db")
                .build().also { instance = it }
        }
    }
}

/** Lưu ghi chú và giữ báo thức nhắc khớp với dữ liệu. Mọi dữ liệu chỉ nằm trên máy. */
object NoteRepository {
    fun observe(context: Context): Flow<List<Note>> = NoteDatabase.get(context).noteDao().observeAll()

    suspend fun save(context: Context, note: Note) {
        val dao = NoteDatabase.get(context).noteDao()
        val id = if (note.id == 0L) dao.insert(note) else { dao.update(note); note.id }
        sync(context, note.copy(id = id))
    }

    suspend fun delete(context: Context, note: Note) {
        ReminderScheduler.cancel(context, note.id)
        NoteDatabase.get(context).noteDao().delete(note)
    }

    suspend fun toggleDone(context: Context, note: Note) = save(context, note.copy(done = !note.done))

    suspend fun rescheduleAll(context: Context) {
        NoteDatabase.get(context).noteDao().upcomingReminders(System.currentTimeMillis())
            .forEach { ReminderScheduler.schedule(context, it) }
    }

    private fun sync(context: Context, note: Note) {
        val at = note.remindAtMs
        if (at != null && !note.done && ReminderRules.isValid(at, System.currentTimeMillis())) {
            ReminderScheduler.schedule(context, note)
        } else {
            ReminderScheduler.cancel(context, note.id)
        }
    }
}
