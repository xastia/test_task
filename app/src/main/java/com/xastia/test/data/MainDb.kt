package com.xastia.test.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.xastia.test.domain.models.Result

@Database (entities = [Result::class], version = 1)
abstract class MainDb: RoomDatabase() {
    abstract fun getDao(): Dao
    companion object {
        fun getDb(context: Context) :MainDb {
            return Room.databaseBuilder(
                context.applicationContext,
                MainDb::class.java,
                "history.db"
            ).build()
        }
    }
}