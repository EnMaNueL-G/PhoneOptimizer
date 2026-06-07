package com.enmanuelgil.optimizer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelgil.optimizer.core.*
import com.enmanuelgil.optimizer.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val monitor = SystemMonitor(application)
    private val engine = OptimizationEngine(application)
    private val appUsageMonitor = AppUsageMonitor(application)
    private val resolver = application.contentResolver

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

    private val _adBlockEnabled = MutableStateFlow(false)
    val adBlockEnabled: StateFlow<Boolean> = _adBlockEnabled.asStateFlow()

    private val _topApps = MutableStateFlow<List<AppMemoryInfo>>(emptyList())
    val topApps: StateFlow<List<AppMemoryInfo>> = _topApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _optimizationHistory = MutableStateFlow<List<OptimizationRecord>>(emptyList())
    val optimizationHistory: StateFlow<List<OptimizationRecord>> = _optimizationHistory.asStateFlow()

    private var monitorJob: Job? = null

    init {
        startMonitoring()
        checkPrivileges()
        loadHistory()
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
            val tempBefore = _stats.value.temperatureBattery
            try {
                val result = engine.optimize(_selectedProfile.value, tempBefore) { progress ->
                    _optimizationProgress.value = progress
                }
                _lastResult.value = result
                val record = OptimizationRecord(
                    profileName = _selectedProfile.value.displayName,
                    ramFreedMb = result.ramFreedMb,
                    appsKilled = result.appsKilled,
                    temperatureBefore = tempBefore,
                    actionCount = result.actionsTaken.size
                )
                HistoryManager.save(getApplication(), record)
                loadHistory()
            } catch (e: Exception) {
                _lastResult.value = OptimizationResult(success = false, errorMessage = e.message)
            } finally {
                _isOptimizing.value = false
                _optimizationProgress.value = ""
            }
        }
    }

    fun checkPrivileges() {
        val granted = PrivilegedHelper.hasWriteSecureSettings(getApplication())
        _privilegesStatus.value = if (granted) PrivilegesStatus.GRANTED else PrivilegesStatus.NOT_GRANTED
        if (granted) _adBlockEnabled.value = AdBlockManager.isEnabled(resolver)
    }

    fun toggleAdBlock(enable: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = if (enable) AdBlockManager.enable(resolver) else AdBlockManager.disable(resolver)
            if (ok) _adBlockEnabled.value = enable
        }
    }

    fun loadTopApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingApps.value = true
            _topApps.value = appUsageMonitor.getTopApps()
            _isLoadingApps.value = false
        }
    }

    fun forceStopApp(packageName: String) {
        viewModelScope.launch {
            engine.forceStopApp(packageName)
            delay(800)
            loadTopApps()
        }
    }

    fun loadHistory() {
        _optimizationHistory.value = HistoryManager.load(getApplication())
    }

    fun clearHistory() {
        HistoryManager.clear(getApplication())
        _optimizationHistory.value = emptyList()
    }
}

enum class PrivilegesStatus { UNKNOWN, NOT_GRANTED, GRANTED }
