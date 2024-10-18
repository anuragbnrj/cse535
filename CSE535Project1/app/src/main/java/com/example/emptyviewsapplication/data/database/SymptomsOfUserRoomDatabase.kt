package com.example.emptyviewsapplication.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.emptyviewsapplication.data.dao.SymptomsOfUserDao
import com.example.emptyviewsapplication.data.entity.SymptomsOfUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
    entities = [SymptomsOfUser::class],
    version = 1,
    exportSchema = false
)
abstract class SymptomsOfUserRoomDatabase : RoomDatabase() {

    abstract fun symptomsOfUserDao(): SymptomsOfUserDao

    private class SymptomsOfUserDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    val symptomsOfUserDao = database.symptomsOfUserDao()

                    // Delete all content here.
                    symptomsOfUserDao.deleteAll()

                    // Add sample data.
                    var symptomsOfUser = SymptomsOfUser(1, 4, 4)
                    symptomsOfUserDao.upsertSymptomsOfUser(symptomsOfUser)
                    symptomsOfUser = SymptomsOfUser(2, 3, 5)
                    symptomsOfUserDao.upsertSymptomsOfUser(symptomsOfUser)

                    // TODO: Add your own sample data!
                    symptomsOfUser = SymptomsOfUser(3, 2, 2)
                    symptomsOfUserDao.upsertSymptomsOfUser(symptomsOfUser)
                }
            }
        }
    }


    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: SymptomsOfUserRoomDatabase? = null

        fun getDatabase(
            context: Context,
            scope: CoroutineScope
        ): SymptomsOfUserRoomDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SymptomsOfUserRoomDatabase::class.java,
                    "symptomsOfUser_database"
                )
                    .addCallback(SymptomsOfUserDatabaseCallback(scope))
                    .build()
                INSTANCE = instance

                // return instance
                instance
            }
        }
    }

}