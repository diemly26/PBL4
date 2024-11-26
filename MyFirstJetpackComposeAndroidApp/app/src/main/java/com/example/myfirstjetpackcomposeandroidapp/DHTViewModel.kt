package com.example.myfirstjetpackcomposeandroidapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DHTViewModel : ViewModel() {
    var temperature by mutableStateOf("--")
    var humidity by mutableStateOf("--")
    var isLoading by mutableStateOf(false)

    private var job: Job? = null

    fun startAutoUpdate() {
        job = viewModelScope.launch {
            while (true) {
                isLoading = true
                try {
                    val data = RetrofitInstance.api.getSensorData()
                    temperature = data.temperature.toString()
                    humidity = data.humidity.toString()
                } catch (e: Exception) {
                    temperature = "Error"
                    humidity = "Error"
                } finally {
                    isLoading = false
                }
                delay(5000)
            }
        }
    }

    fun stopAutoUpdate() {
        job?.cancel()
    }
}
