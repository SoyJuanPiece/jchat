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

        // Insert mock data for testing purposes
        val localDataSource: LocalDataSource by inject()
        insertMockData(localDataSource)
    }

    private fun insertMockData(local: LocalDataSource) {
        val now = Clock.System.now()

        val myProfile = Profile(
            id = uuid4().toString(),
            username = "me",
            displayName = "Yo Mismo",
            avatarUrl = "https://example.com/me.jpg",
            status = OnlineStatus.ONLINE,
            createdAt = now
        )

        val otherProfile = Profile(
            id = uuid4().toString(),
            username = "other",
            displayName = "Otra Persona",
            avatarUrl = "https://example.com/other.jpg",
            status = OnlineStatus.OFFLINE,
            createdAt = now
        )

        val anotherProfile = Profile(
            id = uuid4().toString(),
            username = "another",
            displayName = "Alguien Más",
            avatarUrl = "https://example.com/another.jpg",
            status = OnlineStatus.ONLINE,
            createdAt = now
        )

        local.upsertProfile(myProfile)
        local.upsertProfile(otherProfile)
        local.upsertProfile(anotherProfile)

        val chat1 = Chat(
            id = uuid4().toString(),
            participant = otherProfile,
            lastMessagePreview = "Hola, ¿cómo estás?",
            lastMessageAt = now,
            unreadCount = 1,
            createdAt = now
        )

        val chat2 = Chat(
            id = uuid4().toString(),
            participant = anotherProfile,
            lastMessagePreview = "Nos vemos mañana.",
            lastMessageAt = now,
            unreadCount = 0,
            createdAt = now
        )

        local.upsertChat(chat1)
        local.upsertChat(chat2)
    }
}
