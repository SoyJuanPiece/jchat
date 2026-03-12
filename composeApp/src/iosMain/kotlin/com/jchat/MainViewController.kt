package com.jchat

import androidx.compose.ui.window.ComposeUIViewController
import com.jchat.data.local.DatabaseDriverFactory
import com.jchat.di.appModules
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun MainViewController() = ComposeUIViewController(
    configure = {
        startKoin {
            modules(
                appModules + module {
                    single<DatabaseDriverFactory> { IosDatabaseDriverFactory() }
                }
            )
        }
    }
) {
    App()
}
