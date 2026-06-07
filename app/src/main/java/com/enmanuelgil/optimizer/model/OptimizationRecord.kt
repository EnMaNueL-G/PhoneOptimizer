package com.enmanuelgil.optimizer.model

data class OptimizationRecord(
    val timestamp: Long = System.currentTimeMillis(),
    val profileName: String = "",
    val ramFreedMb: Long = 0L,
    val appsKilled: Int = 0,
    val temperatureBefore: Float = 0f,
    val temperatureAfter: Float = 0f,
    val actionCount: Int = 0
)
