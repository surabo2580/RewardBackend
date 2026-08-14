package com.smartReward.backend.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "users")
data class User(

    @Id
    val id: String, // user123

    val name: String? = null,

    val createdAt: Long = System.currentTimeMillis()
)