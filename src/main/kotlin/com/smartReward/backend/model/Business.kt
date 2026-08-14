package com.smartReward.backend.model



import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "businesses")
data class Business(

    @Id
    val id: String, // example: "taj", "club_abc"

    val name: String,

    val createdAt: Long = System.currentTimeMillis()
)