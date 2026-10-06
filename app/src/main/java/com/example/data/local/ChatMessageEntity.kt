package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AvatarExpression

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val expression: String = AvatarExpression.SMILE.name
) {
    val avatarExpression: AvatarExpression
        get() = AvatarExpression.fromString(expression)
}
