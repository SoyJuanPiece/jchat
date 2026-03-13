package com.jchat

import android.app.Application
import com.benasher44.uuid.uuid4
import com.jchat.data.local.DatabaseDriverFactory
import com.jchat.data.local.LocalDataSource
import com.jchat.di.appModules
import com.jchat.domain.model.Chat
import com.jchat.domain.model.OnlineStatus
import com.jchat.domain.model.Profile
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class JChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@JChatApplication)
            modules(
                appModules + module {
                    single<DatabaseDriverFactory> {
                        AndroidDatabaseDriverFactory(androidContext())
                    }
                }
            )
        }
    }
}
