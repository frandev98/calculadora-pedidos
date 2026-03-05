package com.francisco.calculadorapedidos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [OrderRecordEntity::class], version = 1, exportSchema = false)
abstract class FuxionDatabase : RoomDatabase() {

    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: FuxionDatabase? = null

        fun getDatabase(context: Context): FuxionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FuxionDatabase::class.java,
                    "fuxion_relational_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}