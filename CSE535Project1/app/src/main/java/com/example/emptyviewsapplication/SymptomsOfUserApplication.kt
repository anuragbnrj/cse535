package com.example.emptyviewsapplication

import android.app.Application
import com.example.emptyviewsapplication.data.database.SymptomsOfUserRoomDatabase
import com.example.emptyviewsapplication.data.repository.SymptomsOfUserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class SymptomsOfUserApplication : Application() {
    // No need to cancel this scope as it'll be torn down with the process
    private val applicationScope = CoroutineScope(SupervisorJob())

    // Using by lazy so the database and the repository are only created when they're needed
    // rather than when the application starts
    private val database by lazy { SymptomsOfUserRoomDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { SymptomsOfUserRepository(database.symptomsOfUserDao()) }

}