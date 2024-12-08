package com.example.nearchat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _distance = MutableLiveData<Int>()
    val distance: LiveData<Int> get() = _distance

    fun setDistance(distance: Int) {
        _distance.value = distance
    }
}