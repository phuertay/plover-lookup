package com.phuertay.ploverlookup

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Dictionary::class, Entry::class],
    version = 1,
    exportSchema = false,
)
abstract class LookupDatabase : RoomDatabase() {
    abstract fun lookupDao(): LookupDao

    companion object {
        fun create(context: Context): LookupDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                LookupDatabase::class.java,
                "plover_lookup.db",
            ).addCallback(object : Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.execSQL("PRAGMA foreign_keys = ON")
                }
            }).build()
        }
    }
}
