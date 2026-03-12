package com.jchat

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.jchat.data.local.DatabaseDriverFactory
import com.jchat.db.JChatDatabase

class AndroidDatabaseDriverFactory(private val context: Context) : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver =
        AndroidSqliteDriver(JChatDatabase.Schema, context, "jchat.db")
}
