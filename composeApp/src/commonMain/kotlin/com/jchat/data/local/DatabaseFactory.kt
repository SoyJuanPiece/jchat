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
    return JChatDatabase(driver)
}
