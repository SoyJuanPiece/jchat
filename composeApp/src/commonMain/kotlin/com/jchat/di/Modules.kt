package com.jchat.di

import com.jchat.data.local.DatabaseDriverFactory
import com.jchat.data.local.LocalDataSource
import com.jchat.data.local.createDatabase
import com.jchat.data.remote.RemoteDataSource
import com.jchat.data.remote.SupabaseConfig
import com.jchat.data.remote.createSupabaseClient
import com.jchat.data.repository.ChatRepositoryImpl
import com.jchat.domain.repository.IChatRepository
import com.jchat.presentation.chatlist.ChatListViewModel
import com.jchat.presentation.conversation.ConversationViewModel
import com.jchat.presentation.profile.ProfileViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin DI modules for the JChat application.
 *
 * Usage – call [initKoin] from each platform's entry point, providing
 * a [platformModule] that supplies platform-specific implementations
 * (e.g., [DatabaseDriverFactory]).
 */

/** Core application-level bindings. */
val dataModule = module {
    // Database
    single { createDatabase(get<DatabaseDriverFactory>()) }
    singleOf(::LocalDataSource)

    // Supabase – supply SupabaseConfig via the platform module or a build-config
    single {
        createSupabaseClient(
            SupabaseConfig(
                supabaseUrl = getProperty("SUPABASE_URL", "https://ppincerggnnauznalbjd.supabase.co"),
                supabaseAnonKey = getProperty("SUPABASE_ANON_KEY", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBwaW5jZXJnZ25uYXV6bmFsYmpkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzMzNjA4MTIsImV4cCI6MjA4ODkzNjgxMn0.7BTZKcgjUprF-iI-wEyoBvy5fihAImEkQ3_PXI6QTaM"),
            )
        )
    }
    singleOf(::RemoteDataSource)

    // Repository
    singleOf(::ChatRepositoryImpl) bind IChatRepository::class
}

val presentationModule = module {
    factoryOf(::ChatListViewModel)
    factoryOf(::ProfileViewModel)
    factory { (chatId: String) -> ConversationViewModel(chatId, get()) }
}

/** Convenience list of all shared modules. */
val appModules = listOf(dataModule, presentationModule)
