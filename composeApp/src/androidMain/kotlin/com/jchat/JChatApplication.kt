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
            // Provide Supabase credentials via Koin properties
            properties(
                mapOf(
                    "SUPABASE_URL" to "YOUR_SUPABASE_URL", // TODO: Replace with your actual Supabase URL
                    "SUPABASE_ANON_KEY" to "YOUR_SUPABASE_ANON_KEY" // TODO: Replace with your actual Supabase Anon Key
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
