package com.jchat

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.jchat.data.local.DatabaseDriverFactory
import com.jchat.db.JChatDatabase

class IosDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver =
        NativeSqliteDriver(JChatDatabase.Schema, "jchat.db")
}
