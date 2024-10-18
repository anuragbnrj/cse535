package com.example.emptyviewsapplication.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.emptyviewsapplication.data.entity.SymptomsOfUser
import kotlinx.coroutines.flow.Flow

@Dao
interface SymptomsOfUserDao {

//    @Insert(onConflict = OnConflictStrategy.IGNORE)
    @Upsert
    suspend fun upsertSymptomsOfUser(symptomsOfUser: SymptomsOfUser)

//    @Delete
//    suspend fun deleteSymptomsOfUser(symptomsOfUser: SymptomsOfUser)

    @Query("SELECT * FROM SymptomsOfUser ORDER BY id ASC")
    fun getSymptomsOfUsersOrderedById(): Flow<List<SymptomsOfUser>>

    @Query("DELETE FROM SymptomsOfUser")
    fun deleteAll()
}