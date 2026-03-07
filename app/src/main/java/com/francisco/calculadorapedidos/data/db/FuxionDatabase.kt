package com.francisco.calculadorapedidos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.francisco.calculadorapedidos.data.Client

@Database(
    entities = [OrderRecordEntity::class, Client::class, DraftEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FuxionDatabase : RoomDatabase() {

    abstract fun orderDao(): OrderDao
    abstract fun clientDao(): ClientDao
    abstract fun draftDao(): DraftDao

    companion object {
        @Volatile
        private var INSTANCE: FuxionDatabase? = null

        fun getDatabase(context: Context): FuxionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FuxionDatabase::class.java,
                    "fuxion_relational_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}