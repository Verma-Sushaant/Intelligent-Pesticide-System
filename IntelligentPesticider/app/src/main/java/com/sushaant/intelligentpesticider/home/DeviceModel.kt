package com.sushaant.intelligentpesticider.home

data class DeviceModel(
    val deviceId: String = "",
    var name: String = "Loading...",
    var online: Boolean = false,
    var lastSeen: Long = 0L,
    var lastUpdated: Long = 0L
)