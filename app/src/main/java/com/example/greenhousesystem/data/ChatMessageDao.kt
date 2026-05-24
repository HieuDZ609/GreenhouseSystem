package com.example.greenhousesystem.data
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.greenhousesystem.model.ChatMessage
import kotlinx.coroutines.flow.Flow
@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_ai ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert
    suspend fun insertMessage(message: ChatMessage)
}