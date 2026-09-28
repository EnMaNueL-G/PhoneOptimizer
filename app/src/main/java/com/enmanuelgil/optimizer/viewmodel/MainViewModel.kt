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

    private val app = application
    private val monitor = SystemMonitor(application)
    private val engine = OptimizationEngine(application)
    private val appUsageMonitor = AppUsageMonitor(application)

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

    private val _canRestore = MutableStateFlow(false)
    val canRestore: StateFlow<Boolean> = _canRestore.asStateFlow()

    private val _topApps = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val topApps: StateFlow<List<AppUsageInfo>> = _topApps.asStateFlow()

    private val _hasUsageAccess = MutableStateFlow(false)
    val hasUsageAccess: StateFlow<Boolean> = _hasUsageAccess.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _optimizationHistory = MutableStateFlow<List<OptimizationRecord>>(emptyList())
    val optimizationHistory: StateFlow<List<OptimizationRecord>> = _optimizationHistory.asStateFlow()

    /** Mensaje corto para mostrar abajo (Snackbar). */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var monitorJob: Job? = null

    init {
        refreshPermissions()
        loadHistory()
    }

    /** Lecturas cada 3 s SOLO mientras la app está a la vista (MainActivity.onStart/onStop). */
    fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        monitorJob = viewModelScope.launch {
            while (isActive) {
                try { _stats.value = monitor.getStats() } catch (_: Exception) {}
                delay(3_000)
            }
        }
    }

    fun stopMonitoring() { monitorJob?.cancel(); monitorJob = null }

    fun setProfile(profile: OptimizationProfile) { _selectedProfile.value = profile }

    fun optimize() {
        if (_isOptimizing.value) return
        viewModelScope.launch {
            _isOptimizing.value = true
            _optimizationProgress.value = "Iniciando..."
            _lastResult.value = null
            val profile = _selectedProfile.value
            try {
                val result = engine.optimize(profile) { _optimizationProgress.value = it }
                _lastResult.value = result
                if (result.actionsTaken.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        HistoryManager.save(app, OptimizationRecord(
                            profileName = profile.displayName,
                            actionCount = result.actionsTaken.count { !it.startsWith("✗") }
                        ))
                    }
                    loadHistory()
                }
                _canRestore.value = PrivilegedHelper.hasBackup(app)
            } catch (e: Exception) {
                _lastResult.value = OptimizationResult(success = false, errorMessage = e.message)
            } finally {
                _isOptimizing.value = false
                _optimizationProgress.value = ""
            }
        }
    }

    /** Volver a leer permisos (al volver de Ajustes de Android). */
    fun refreshPermissions() {
        val granted = PrivilegedHelper.hasWriteSecureSettings(app)
        _privilegesStatus.value = if (granted) PrivilegesStatus.GRANTED else PrivilegesStatus.NOT_GRANTED
        _adBlockEnabled.value = granted && AdBlockManager.isEnabled(app)
        _canRestore.value = PrivilegedHelper.hasBackup(app)
        val usage = appUsageMonitor.hasUsageAccess()
        val changed = usage != _hasUsageAccess.value
        _hasUsageAccess.value = usage
        if (changed && usage) loadTopApps()
    }

    fun toggleAdBlock(enable: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = if (enable) AdBlockManager.enable(app) else AdBlockManager.disable(app)
            _adBlockEnabled.value = AdBlockManager.isEnabled(app)
            if (!ok) _message.value = "El sistema no aceptó el cambio de DNS"
            else if (enable) _message.value = "Bloqueo activado. Si alguna red se queda sin internet, desactívalo."
        }
    }

    fun restoreSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            val (ok, fail) = PrivilegedHelper.restoreAll(app)
            _canRestore.value = PrivilegedHelper.hasBackup(app)
            _message.value = when {
                ok == 0 && fail == 0 -> "No había ajustes que restaurar"
                fail == 0 -> "Ajustes originales restaurados ($ok)"
                else -> "Restaurados $ok, fallaron $fail (¿se quitó el permiso?)"
            }
        }
    }

    fun loadTopApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingApps.value = true
            _hasUsageAccess.value = appUsageMonitor.hasUsageAccess()
            _topApps.value = appUsageMonitor.getTopApps()
            _isLoadingApps.value = false
        }
    }

    fun openUsageAccess() = appUsageMonitor.openUsageAccessSettings()

    /** Solo Android 13 o anterior (ver OptimizationEngine.canCloseBackgroundApps). */
    fun closeBackgroundApps() {
        viewModelScope.launch {
            val n = engine.requestCloseBackgroundApps()
            _message.value = if (n > 0)
                "Se pidió a Android cerrar tus apps en segundo plano ($n revisadas). Algunos fabricantes lo ignoran."
            else "No se pudo pedir el cierre"
        }
    }

    fun consumeMessage() { _message.value = null }

    fun loadHistory() {
        _optimizationHistory.value = HistoryManager.load(app)
    }

    fun clearHistory() {
        HistoryManager.clear(app)
        _optimizationHistory.value = emptyList()
    }
}

enum class PrivilegesStatus { UNKNOWN, NOT_GRANTED, GRANTED }
