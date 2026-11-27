package com.focusfarm.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_inventory")
data class UserInventoryEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val waterDroplets: Int = 0,
    val totalExperience: Int = 0,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
