package com.jchat

import android.app.Application
import com.jchat.data.local.DatabaseDriverFactory
import com.jchat.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class JChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@JChatApplication)
            properties(
                mapOf(
                    "SUPABASE_URL" to BuildConfig.SUPABASE_URL,
                    "SUPABASE_ANON_KEY" to BuildConfig.SUPABASE_ANON_KEY,
                )
            )
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
