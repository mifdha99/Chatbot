package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.data.model.AvatarExpression
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Pure-Kotlin SQLite implementation of MesraDatabase and ChatDao.
 * Works directly on Android without requiring KSP or kapt code generation,
 * preventing "MesraDatabase_Impl does not exist" runtime crashes on devices.
 */
class MesraDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    private val messagesFlow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())

    private val daoImpl = object : ChatDao {
        override fun getAllMessagesFlow(): Flow<List<ChatMessageEntity>> {
            return messagesFlow.asStateFlow()
        }

        override suspend fun getRecentMessages(limit: Int): List<ChatMessageEntity> =
            withContext(Dispatchers.IO) {
                queryRecentInternal(limit)
            }

        override suspend fun getMessageCount(): Int =
            withContext(Dispatchers.IO) {
                queryCountInternal()
            }

        override suspend fun insertMessage(message: ChatMessageEntity): Long =
            withContext(Dispatchers.IO) {
                val id = insertInternal(message)
                refreshFlowInternal()
                id
            }

        override suspend fun clearAllMessages() =
            withContext(Dispatchers.IO) {
                clearInternal()
                refreshFlowInternal()
            }
    }

    init {
        runCatching {
            refreshFlowInternal()
        }
    }

    fun chatDao(): ChatDao = daoImpl

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_MESSAGES (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                text TEXT NOT NULL,
                isFromUser INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                expression TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        onCreate(db)
    }

    @Synchronized
    private fun insertInternal(message: ChatMessageEntity): Long {
        return runCatching {
            val db = writableDatabase
            val values = ContentValues().apply {
                if (message.id > 0L) {
                    put("id", message.id)
                }
                put("text", message.text)
                put("isFromUser", if (message.isFromUser) 1 else 0)
                put("timestamp", message.timestamp)
                put("expression", message.expression)
            }
            db.insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }.getOrDefault(System.currentTimeMillis())
    }

    @Synchronized
    private fun queryCountInternal(): Int {
        return runCatching {
            val db = readableDatabase
            db.rawQuery("SELECT COUNT(*) FROM $TABLE_MESSAGES", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        }.getOrDefault(messagesFlow.value.size)
    }

    @Synchronized
    private fun queryRecentInternal(limit: Int): List<ChatMessageEntity> {
        return runCatching {
            val db = readableDatabase
            val result = mutableListOf<ChatMessageEntity>()
            db.rawQuery(
                "SELECT id, text, isFromUser, timestamp, expression FROM $TABLE_MESSAGES ORDER BY timestamp DESC, id DESC LIMIT ?",
                arrayOf(limit.toString())
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    result.add(
                        ChatMessageEntity(
                            id = cursor.getLong(0),
                            text = cursor.getString(1).orEmpty(),
                            isFromUser = cursor.getInt(2) != 0,
                            timestamp = cursor.getLong(3),
                            expression = cursor.getString(4) ?: AvatarExpression.SMILE.name
                        )
                    )
                }
            }
            result
        }.getOrDefault(messagesFlow.value.takeLast(limit).reversed())
    }

    @Synchronized
    private fun clearInternal() {
        runCatching {
            writableDatabase.delete(TABLE_MESSAGES, null, null)
        }
    }

    @Synchronized
    private fun refreshFlowInternal() {
        runCatching {
            val db = readableDatabase
            val all = mutableListOf<ChatMessageEntity>()
            db.rawQuery(
                "SELECT id, text, isFromUser, timestamp, expression FROM $TABLE_MESSAGES ORDER BY timestamp ASC, id ASC",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    all.add(
                        ChatMessageEntity(
                            id = cursor.getLong(0),
                            text = cursor.getString(1).orEmpty(),
                            isFromUser = cursor.getInt(2) != 0,
                            timestamp = cursor.getLong(3),
                            expression = cursor.getString(4) ?: AvatarExpression.SMILE.name
                        )
                    )
                }
            }
            messagesFlow.value = all
        }
    }

    companion object {
        private const val DATABASE_NAME = "mesra_ai_database"
        private const val DATABASE_VERSION = 1
        private const val TABLE_MESSAGES = "chat_messages"

        @Volatile
        private var INSTANCE: MesraDatabase? = null

        fun getDatabase(context: Context): MesraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MesraDatabase(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
