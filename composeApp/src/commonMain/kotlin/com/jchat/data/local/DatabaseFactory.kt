package com.jchat.data.local

import app.cash.sqldelight.db.SqlDriver
import com.jchat.db.JChatDatabase

/**
 * Platform-agnostic factory interface for creating the SQLDelight [SqlDriver].
 *
 * Concrete implementations live in `androidMain` and `iosMain`.
 */
interface DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

/**
 * Creates and returns a fully initialised [JChatDatabase].
 */
fun createDatabase(driverFactory: DatabaseDriverFactory): JChatDatabase {
    val driver = driverFactory.createDriver()
    val database = JChatDatabase(driver)

    // Backward-compatible hotfix for existing installs created before reply support.
    runCatching {
        driver.execute(
            identifier = null,
            sql = "ALTER TABLE messages ADD COLUMN reply_to_message_id TEXT",
            parameters = 0,
        )
    }
    runCatching {
        driver.execute(
            identifier = null,
            sql = "ALTER TABLE messages ADD COLUMN reply_preview TEXT",
            parameters = 0,
        )
    }
    runCatching {
        driver.execute(
            identifier = null,
            sql = "CREATE TABLE IF NOT EXISTS app_settings (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL, updated_at INTEGER NOT NULL DEFAULT 0)",
            parameters = 0,
        )
    }

    return database
}
