package com.smartReward.backend.repository



import com.smartReward.backend.model.Transaction
import org.springframework.data.jpa.repository.JpaRepository

interface TransactionRepository : JpaRepository<Transaction, Long>