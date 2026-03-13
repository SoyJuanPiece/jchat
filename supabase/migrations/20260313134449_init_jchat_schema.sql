-- JChat baseline schema and security policies.

create extension if not exists "pgcrypto";

create table if not exists public.profiles (
	id uuid references auth.users(id) on delete cascade primary key,
	username text unique not null,
	display_name text not null,
	avatar_url text,
	status text not null default 'OFFLINE',
	last_seen_at timestamptz default now(),
	created_at timestamptz default now()
);

create table if not exists public.chats (
	id uuid default gen_random_uuid() primary key,
	created_at timestamptz default now()
);

create table if not exists public.chat_participants (
	chat_id uuid references public.chats(id) on delete cascade,
	profile_id uuid references public.profiles(id) on delete cascade,
	joined_at timestamptz default now(),
	primary key (chat_id, profile_id)
);

create table if not exists public.messages (
	id uuid default gen_random_uuid() primary key,
	chat_id uuid references public.chats(id) on delete cascade not null,
	sender_id uuid references public.profiles(id) on delete set null not null,
	content text,
	content_type text not null default 'TEXT',
	media_url text,
	status text not null default 'SENT',
	created_at timestamptz default now(),
	updated_at timestamptz default now(),
	is_deleted boolean default false
);

create index if not exists idx_profiles_username on public.profiles(username);
create index if not exists idx_chat_participants_profile_id on public.chat_participants(profile_id);
create index if not exists idx_messages_chat_id_created_at on public.messages(chat_id, created_at);
create index if not exists idx_messages_sender_id on public.messages(sender_id);

alter table public.profiles enable row level security;
alter table public.chats enable row level security;
alter table public.chat_participants enable row level security;
alter table public.messages enable row level security;

drop policy if exists "profiles_select_all" on public.profiles;
create policy "profiles_select_all"
on public.profiles
for select
to authenticated
using (true);

drop policy if exists "profiles_update_self" on public.profiles;
create policy "profiles_update_self"
on public.profiles
for update
to authenticated
using (auth.uid() = id)
with check (auth.uid() = id);

drop policy if exists "profiles_insert_self" on public.profiles;
create policy "profiles_insert_self"
on public.profiles
for insert
to authenticated
with check (auth.uid() = id);

drop policy if exists "chats_select_member" on public.chats;
create policy "chats_select_member"
on public.chats
for select
to authenticated
using (
	exists (
		select 1
		from public.chat_participants cp
		where cp.chat_id = public.chats.id
		  and cp.profile_id = auth.uid()
	)
);

drop policy if exists "chats_insert_authenticated" on public.chats;
create policy "chats_insert_authenticated"
on public.chats
for insert
to authenticated
with check (auth.role() = 'authenticated');

drop policy if exists "chat_participants_select_member" on public.chat_participants;
create policy "chat_participants_select_member"
on public.chat_participants
for select
to authenticated
using (
	exists (
		select 1
		from public.chat_participants cp
		where cp.chat_id = public.chat_participants.chat_id
		  and cp.profile_id = auth.uid()
	)
);

drop policy if exists "chat_participants_insert_flow" on public.chat_participants;
create policy "chat_participants_insert_flow"
on public.chat_participants
for insert
to authenticated
with check (
	auth.uid() = profile_id
	or exists (
		select 1
		from public.chat_participants cp
		where cp.chat_id = public.chat_participants.chat_id
		  and cp.profile_id = auth.uid()
	)
);

drop policy if exists "messages_select_member" on public.messages;
create policy "messages_select_member"
on public.messages
for select
to authenticated
using (
	exists (
		select 1
		from public.chat_participants cp
		where cp.chat_id = public.messages.chat_id
		  and cp.profile_id = auth.uid()
	)
);

drop policy if exists "messages_insert_sender_member" on public.messages;
create policy "messages_insert_sender_member"
on public.messages
for insert
to authenticated
with check (
	auth.uid() = sender_id
	and exists (
		select 1
		from public.chat_participants cp
		where cp.chat_id = public.messages.chat_id
		  and cp.profile_id = auth.uid()
	)
);

drop policy if exists "messages_update_sender" on public.messages;
create policy "messages_update_sender"
on public.messages
for update
to authenticated
using (auth.uid() = sender_id)
with check (auth.uid() = sender_id);

create or replace function public.handle_new_user()
returns trigger as $$
begin
	insert into public.profiles (id, username, display_name, avatar_url)
	values (
		new.id,
		new.raw_user_meta_data->>'username',
		coalesce(new.raw_user_meta_data->>'display_name', new.raw_user_meta_data->>'username'),
		new.raw_user_meta_data->>'avatar_url'
	)
	on conflict (id) do nothing;
	return new;
end;
$$ language plpgsql security definer;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
	after insert on auth.users
	for each row execute function public.handle_new_user();

create or replace function public.set_message_updated_at()
returns trigger as $$
begin
	new.updated_at = now();
	return new;
end;
$$ language plpgsql;

drop trigger if exists trigger_set_message_updated_at on public.messages;
create trigger trigger_set_message_updated_at
	before update on public.messages
	for each row execute function public.set_message_updated_at();

alter publication supabase_realtime add table public.messages;

insert into storage.buckets (id, name, public)
values ('chat-media', 'chat-media', true)
on conflict (id) do nothing;

drop policy if exists "chat_media_select" on storage.objects;
create policy "chat_media_select"
on storage.objects
for select
to authenticated
using (bucket_id = 'chat-media');

drop policy if exists "chat_media_insert" on storage.objects;
create policy "chat_media_insert"
on storage.objects
for insert
to authenticated
with check (
	bucket_id = 'chat-media'
	and auth.role() = 'authenticated'
);

drop policy if exists "chat_media_update" on storage.objects;
create policy "chat_media_update"
on storage.objects
for update
to authenticated
using (bucket_id = 'chat-media' and auth.role() = 'authenticated')
with check (bucket_id = 'chat-media' and auth.role() = 'authenticated');

drop policy if exists "chat_media_delete" on storage.objects;
create policy "chat_media_delete"
on storage.objects
for delete
to authenticated
using (bucket_id = 'chat-media' and auth.role() = 'authenticated');
