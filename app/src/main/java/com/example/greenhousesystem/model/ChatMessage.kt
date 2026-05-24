package com.example.greenhousesystem.model
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "chat_ai")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0 ,
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)