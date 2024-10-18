package com.example.emptyviewsapplication.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.emptyviewsapplication.data.entity.SymptomsOfUser
import com.example.emptyviewsapplication.data.repository.SymptomsOfUserRepository
import kotlinx.coroutines.launch


class SymptomsOfUserViewModel(private val repository: SymptomsOfUserRepository): ViewModel() {

    // Using LiveData and caching what allWords returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    val allSymptomsOfUser: LiveData<List<SymptomsOfUser>> = repository.allSymptomsOfUsers.asLiveData()

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun upsert(symptomsOfUser: SymptomsOfUser) = viewModelScope.launch {
        repository.upsert(symptomsOfUser)
    }

}

class SymptomsOfUserViewModelFactory(private val repository: SymptomsOfUserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SymptomsOfUserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SymptomsOfUserViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
