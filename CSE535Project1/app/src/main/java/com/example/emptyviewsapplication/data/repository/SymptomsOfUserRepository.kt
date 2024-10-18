package com.example.emptyviewsapplication.data.repository

import androidx.annotation.WorkerThread
import com.example.emptyviewsapplication.data.dao.SymptomsOfUserDao
import com.example.emptyviewsapplication.data.entity.SymptomsOfUser
import kotlinx.coroutines.flow.Flow

class SymptomsOfUserRepository(private val symptomOfUserDao: SymptomsOfUserDao) {

    // Room executes all queries on a separate thread.
    // Observed Flow will notify the observer when the data has changed.
    val allSymptomsOfUsers: Flow<List<SymptomsOfUser>> = symptomOfUserDao.getSymptomsOfUsersOrderedById()

    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.
    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun upsert(symptomsOfUser: SymptomsOfUser) {
        symptomOfUserDao.upsertSymptomsOfUser(symptomsOfUser)
    }

}