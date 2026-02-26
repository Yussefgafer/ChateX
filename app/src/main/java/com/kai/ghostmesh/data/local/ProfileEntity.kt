package com.kai.ghostmesh.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val status: String,
    val lastSeen: Long = System.currentTimeMillis(),
    val color: Int = 0xFF00FF7F.toInt()
)
