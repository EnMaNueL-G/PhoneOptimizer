package com.enmanuelgil.optimizer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelgil.optimizer.core.OptimizationEngine
import com.enmanuelgil.optimizer.core.PrivilegedHelper
import com.enmanuelgil.optimizer.core.SystemMonitor
import com.enmanuelgil.optimizer.model.DeviceStats
import com.enmanuelgil.optimizer.model.OptimizationProfile
import com.enmanuelgil.optimizer.model.OptimizationResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val monitor = SystemMonitor(application)
    private val engine = OptimizationEngine(application)

    private val _stats = MutableStateFlow(DeviceStats())
    val stats: StateFlow<DeviceStats> = _stats.asStateFlow()

    private val _isOptimizing = MutableStateFlow(false)
    val isOptimizing: StateFlow<Boolean> = _isOptimizing.asStateFlow()

    private val _optimizationProgress = MutableStateFlow("")
    val optimizationProgress: StateFlow<String> = _optimizationProgress.asStateFlow()

    private val _lastResult = MutableStateFlow<OptimizationResult?>(null)
    val lastResult: StateFlow<OptimizationResult?> = _lastResult.asStateFlow()

    private val _selectedProfile = MutableStateFlow(OptimizationProfile.RECOMMENDED)
    val selectedProfile: StateFlow<OptimizationProfile> = _selectedProfile.asStateFlow()

    private val _privilegesStatus = MutableStateFlow(PrivilegesStatus.UNKNOWN)
    val privilegesStatus: StateFlow<PrivilegesStatus> = _privilegesStatus.asStateFlow()

    private var monitorJob: Job? = null

    init {
        startMonitoring()
        checkPrivileges()
    }

    fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = viewModelScope.launch {
            while (isActive) {
                try { _stats.value = monitor.getStats() } catch (e: Exception) {}
                delay(3_000)
            }
        }
    }

    fun stopMonitoring() { monitorJob?.cancel() }

    fun setProfile(profile: OptimizationProfile) { _selectedProfile.value = profile }

    fun optimize() {
        if (_isOptimizing.value) return
        viewModelScope.launch {
            _isOptimizing.value = true
            _optimizationProgress.value = "Iniciando optimización..."
            _lastResult.value = null
            try {
                val result = engine.optimize(_selectedProfile.value) { progress ->
                    _optimizationProgress.value = progress
                }
                _lastResult.value = result
            } catch (e: Exception) {
                _lastResult.value = OptimizationResult(success = false, errorMessage = e.message)
            } finally {
                _isOptimizing.value = false
                _optimizationProgress.value = ""
            }
        }
    }

    fun checkPrivileges() {
        _privilegesStatus.value = if (PrivilegedHelper.hasWriteSecureSettings(getApplication()))
            PrivilegesStatus.GRANTED
        else
            PrivilegesStatus.NOT_GRANTED
    }

    fun forceStopApp(packageName: String) {
        viewModelScope.launch { engine.forceStopApp(packageName) }
    }
}

enum class PrivilegesStatus { UNKNOWN, NOT_GRANTED, GRANTED }
