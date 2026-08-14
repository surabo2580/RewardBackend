package com.smartReward.backend.dto

data class WalletResponse(
    val availablePoints: Int,
    val pendingPoints: Int
)