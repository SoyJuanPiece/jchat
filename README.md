# JChat — Kotlin Multiplatform Chat App

A high-performance, WhatsApp-inspired chat application built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**, targeting Android and iOS from a single shared codebase.

---

## ✨ Features

- **Offline-first** — messages are always read from the local SQLDelight database; network sync runs in the background.
- **Real-time updates** — Supabase Realtime WebSocket channel pushes new messages instantly.
- **Optimistic UI** — messages appear immediately with a "sending" indicator before the server confirms delivery.
- **Media messages** — asynchronous image/audio uploads to Supabase Storage with an in-chat progress bar.
- **MVI architecture** — clean, unidirectional data flow with `State`, `Intent`, and `Event` types per screen.

---

## 🛠 Tech Stack

| Layer | Library |
|---|---|
| Language | Kotlin (shared logic + UI) |
| UI | Compose Multiplatform 1.7.3 |
| Navigation | Jetbrains Navigation Compose |
| Local DB | SQLDelight 2.0.2 |
| Networking | Ktor 3.0.3 |
| Backend / Realtime | Supabase Kotlin SDK 3.0.2 |
| Dependency Injection | Koin 4.0.0 |
| Image loading | Coil 3.1.0 |
| Async | Kotlinx Coroutines 1.9.0 |

---

## 📁 Project Structure

```
jchat/
├── gradle/
│   └── libs.versions.toml          # Centralised version catalog
├── build.gradle.kts                # Root build file
├── settings.gradle.kts
├── gradle.properties
└── composeApp/
    ├── build.gradle.kts            # Module build + SQLDelight config
    └── src/
        ├── commonMain/
        │   ├── kotlin/com/jchat/
        │   │   ├── App.kt                          # Root Composable
        │   │   ├── di/
        │   │   │   └── Modules.kt                  # Koin DI modules
        │   │   ├── data/
        │   │   │   ├── local/
        │   │   │   │   ├── DatabaseFactory.kt      # Platform-agnostic DB factory
        │   │   │   │   └── LocalDataSource.kt      # SQLDelight queries wrapper
        │   │   │   ├── remote/
        │   │   │   │   ├── SupabaseClientFactory.kt
        │   │   │   │   └── RemoteDataSource.kt     # Supabase / Ktor calls + DTOs
        │   │   │   └── repository/
        │   │   │       └── ChatRepositoryImpl.kt   # Offline-first implementation
        │   │   ├── domain/
        │   │   │   ├── model/
        │   │   │   │   ├── Message.kt
        │   │   │   │   ├── Profile.kt
        │   │   │   │   └── Chat.kt
        │   │   │   └── repository/
        │   │   │       └── IChatRepository.kt      # Repository contract
        │   │   └── presentation/
        │   │       ├── navigation/
        │   │       │   └── JChatNavGraph.kt
        │   │       ├── chatlist/
        │   │       │   ├── ChatListScreen.kt       # LazyColumn chat list
        │   │       │   └── ChatListViewModel.kt    # MVI ViewModel
        │   │       └── conversation/
        │   │           ├── ConversationScreen.kt   # Chat bubbles + input bar
        │   │           └── ConversationViewModel.kt
        │   └── sqldelight/com/jchat/
        │       ├── Messages.sq                     # SQL schema: chats + messages
        │       └── Profiles.sq                     # SQL schema: user profiles
        ├── androidMain/
        │   ├── AndroidManifest.xml
        │   └── kotlin/com/jchat/
        │       ├── MainActivity.kt
        │       ├── JChatApplication.kt             # Koin initialisation
        │       └── AndroidDatabaseDriverFactory.kt
        └── iosMain/
            └── kotlin/com/jchat/
                ├── MainViewController.kt           # SwiftUI bridge
                └── IosDatabaseDriverFactory.kt
```

---

## ⚙️ Configuration

### Supabase credentials

Open `composeApp/src/commonMain/kotlin/com/jchat/di/Modules.kt` and replace the placeholder values with your project URL and anon key, or supply them via Gradle properties / environment variables:

```kotlin
SupabaseConfig(
    supabaseUrl     = getProperty("SUPABASE_URL",     "https://your-project.supabase.co"),
    supabaseAnonKey = getProperty("SUPABASE_ANON_KEY", "your-anon-key"),
)
```

### Supabase table schema

Run the following SQL in your Supabase project's SQL editor to create the required tables:

```sql
-- profiles
create table profiles (
  id           uuid primary key references auth.users,
  username     text not null unique,
  display_name text not null,
  avatar_url   text,
  status       text not null default 'offline',
  last_seen_at timestamptz,
  created_at   timestamptz not null default now()
);

-- chats
create table chats (
  id             uuid primary key default gen_random_uuid(),
  participant_id uuid not null references profiles(id),
  created_at     timestamptz not null default now()
);

-- messages
create table messages (
  id           uuid primary key default gen_random_uuid(),
  chat_id      uuid not null references chats(id),
  sender_id    uuid not null references profiles(id),
  content      text,
  content_type text not null default 'text',
  media_url    text,
  status       text not null default 'sent',
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now()
);

-- Enable Realtime
alter publication supabase_realtime add table messages;
```

---

## 🏗 Architecture — MVI Overview

```
┌──────────────┐   Intent   ┌─────────────┐  suspend/Flow  ┌──────────────────┐
│  Composable  │──────────▶│  ViewModel  │──────────────▶│  IChatRepository │
│  (View)      │◀──────────│  (State)    │◀──────────────│  (Domain layer)  │
└──────────────┘   State   └─────────────┘               └────────┬─────────┘
                                                                    │
                                              ┌─────────────────────┴──────────────────┐
                                              │                                        │
                                    ┌─────────┴────────┐                   ┌──────────┴────────┐
                                    │  LocalDataSource │                   │  RemoteDataSource │
                                    │   (SQLDelight)   │                   │  (Supabase/Ktor)  │
                                    └──────────────────┘                   └───────────────────┘
```

The repository always writes to the local DB first, then syncs to the remote. Reactive SQLDelight queries propagate any change automatically through the `Flow` chain to the UI.

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- Xcode 15+ (for iOS)
- JDK 17

### Build

```bash
# Android
./gradlew :composeApp:assembleDebug

# iOS (from Xcode or via command line)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

## Production Release

Android production artifacts are built from the tag-based GitHub Actions workflow in [.github/workflows/release.yml](.github/workflows/release.yml).

Required GitHub secrets:

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_STORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Optional version overrides:

- `APP_VERSION_NAME`
- `APP_VERSION_CODE`

What the release workflow does now:

- injects Supabase config into Android `BuildConfig`
- decodes the release keystore during CI
- builds signed `release` artifacts instead of `debug`
- uploads both APK and AAB to the GitHub Release

Local release build example:

```bash
export SUPABASE_URL="https://your-project.supabase.co"
export SUPABASE_ANON_KEY="your-anon-key"
export STORE_PASSWORD="..."
export KEY_ALIAS="..."
export KEY_PASSWORD="..."
export ANDROID_KEYSTORE_PATH="$PWD/composeApp/key.jks"

gradle :composeApp:assembleRelease :composeApp:bundleRelease
```
