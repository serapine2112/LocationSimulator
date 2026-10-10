
package com.example.locationsimulator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SimulationState {

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private val _latitude = MutableStateFlow(-6.1754)
    val latitude = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow(106.8272)
    val longitude = _longitude.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun setRunning(
        running: Boolean,
        latitude: Double = _latitude.value,
        longitude: Double = _longitude.value
    ) {
        _latitude.value = latitude
        _longitude.value = longitude
        _errorMessage.value = null
        _isRunning.value = running
    }

    fun setError(message: String) {
        _errorMessage.value = message
        _isRunning.value = false
    }
}
