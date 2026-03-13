-- ─── JCHAT SUPABASE SCHEMA ───
-- This script sets up the database for the JChat application.
-- It includes tables for profiles, chats, and messages, with RLS enabled.

-- 1. PROFILES TABLE
-- Extends the Supabase Auth 'users' table.
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID REFERENCES auth.users(id) ON DELETE CASCADE PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    display_name TEXT NOT NULL,
    avatar_url TEXT,
    status TEXT NOT NULL DEFAULT 'OFFLINE',
    last_seen_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- 2. CHATS TABLE
-- Defines a conversation between users.
CREATE TABLE IF NOT EXISTS public.chats (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.chats ENABLE ROW LEVEL SECURITY;

-- 3. CHAT PARTICIPANTS
-- Linking table to handle who belongs to which chat.
CREATE TABLE IF NOT EXISTS public.chat_participants (
    chat_id UUID REFERENCES public.chats(id) ON DELETE CASCADE,
    profile_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (chat_id, profile_id)
);

ALTER TABLE public.chat_participants ENABLE ROW LEVEL SECURITY;

-- 4. MESSAGES TABLE
CREATE TABLE IF NOT EXISTS public.messages (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    chat_id UUID REFERENCES public.chats(id) ON DELETE CASCADE NOT NULL,
    sender_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL NOT NULL,
    content TEXT,
    content_type TEXT NOT NULL DEFAULT 'TEXT',
    media_url TEXT,
    reply_to_message_id UUID REFERENCES public.messages(id) ON DELETE SET NULL,
    reply_preview TEXT,
    status TEXT NOT NULL DEFAULT 'SENT',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);

ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;

-- 5. BLOCKED USERS TABLE
CREATE TABLE IF NOT EXISTS public.blocked_users (
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    blocked_user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (user_id, blocked_user_id),
    CONSTRAINT blocked_users_no_self_block CHECK (user_id <> blocked_user_id)
);

ALTER TABLE public.blocked_users ENABLE ROW LEVEL SECURITY;

-- 6. SUPPORT REPORTS TABLE
CREATE TABLE IF NOT EXISTS public.support_reports (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    message TEXT NOT NULL,
    client TEXT NOT NULL DEFAULT 'jchat-kmp',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.support_reports ENABLE ROW LEVEL SECURITY;

-- ─── ROW LEVEL SECURITY (RLS) POLICIES ───

-- Profiles: Anyone can read profiles, but only owners can update theirs.
CREATE POLICY "Public profiles are viewable by everyone" ON public.profiles
    FOR SELECT USING (true);

CREATE POLICY "Users can update own profile" ON public.profiles
    FOR UPDATE USING (auth.uid() = id);

-- Chat Participants: Users can see chats they are part of.
CREATE POLICY "Users can see their own memberships" ON public.chat_participants
    FOR SELECT USING (auth.uid() = profile_id);

-- Chats: Users can see chats where they are a participant.
CREATE POLICY "Users can see their own chats" ON public.chats
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.chat_participants
            WHERE chat_id = public.chats.id AND profile_id = auth.uid()
        )
    );

-- Messages: Users can see messages from chats they belong to.
CREATE POLICY "Users can see messages in their chats" ON public.messages
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.chat_participants
            WHERE chat_id = public.messages.chat_id AND profile_id = auth.uid()
        )
    );

CREATE POLICY "Users can insert messages into their chats" ON public.messages
    FOR INSERT WITH CHECK (
        auth.uid() = sender_id AND
        EXISTS (
            SELECT 1 FROM public.chat_participants
            WHERE chat_id = public.messages.chat_id AND profile_id = auth.uid()
        )
    );

CREATE POLICY "Users can view own blocked users" ON public.blocked_users
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own blocked users" ON public.blocked_users
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own blocked users" ON public.blocked_users
    FOR DELETE USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own support reports" ON public.support_reports
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can view own support reports" ON public.support_reports
    FOR SELECT USING (auth.uid() = user_id);

-- ─── FUNCTIONS & TRIGGERS ───

-- Automatically create a profile when a new user signs up.
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, username, display_name, avatar_url)
    VALUES (
        new.id,
        new.raw_user_meta_data->>'username',
        COALESCE(new.raw_user_meta_data->>'display_name', new.raw_user_meta_data->>'username'),
        new.raw_user_meta_data->>'avatar_url'
    );
    RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
