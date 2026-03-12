package com.jchat.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * Configuration holder for the Supabase connection.
 *
 * In a production app, [supabaseUrl] and [supabaseKey] should be loaded
 * from a secure build-config source (e.g., BuildConfig or a secrets file),
 * not hard-coded here.
 */
data class SupabaseConfig(
    val supabaseUrl: String,
    val supabaseAnonKey: String,
)

/**
 * Creates and configures a [SupabaseClient] with all required plugins:
 * - [Auth] for user authentication
 * - [Postgrest] for database queries (REST)
 * - [Realtime] for live subscriptions
 * - [Storage] for file uploads (photos, audio)
 */
fun createSupabaseClient(config: SupabaseConfig): SupabaseClient =
    createSupabaseClient(
        supabaseUrl = config.supabaseUrl,
        supabaseKey = config.supabaseAnonKey,
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }
