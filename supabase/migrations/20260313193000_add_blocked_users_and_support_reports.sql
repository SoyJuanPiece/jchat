-- Add blocked users and support reports tables for Settings module.

create table if not exists public.blocked_users (
    user_id uuid references public.profiles(id) on delete cascade,
    blocked_user_id uuid references public.profiles(id) on delete cascade,
    created_at timestamptz default now(),
    primary key (user_id, blocked_user_id),
    constraint blocked_users_no_self_block check (user_id <> blocked_user_id)
);

create index if not exists idx_blocked_users_user_id on public.blocked_users(user_id);
create index if not exists idx_blocked_users_blocked_user_id on public.blocked_users(blocked_user_id);

alter table public.blocked_users enable row level security;

drop policy if exists "blocked_users_select_own" on public.blocked_users;
create policy "blocked_users_select_own"
on public.blocked_users
for select
to authenticated
using (auth.uid() = user_id);

drop policy if exists "blocked_users_insert_own" on public.blocked_users;
create policy "blocked_users_insert_own"
on public.blocked_users
for insert
to authenticated
with check (auth.uid() = user_id);

drop policy if exists "blocked_users_delete_own" on public.blocked_users;
create policy "blocked_users_delete_own"
on public.blocked_users
for delete
to authenticated
using (auth.uid() = user_id);

create table if not exists public.support_reports (
    id uuid default gen_random_uuid() primary key,
    user_id uuid references public.profiles(id) on delete cascade not null,
    message text not null,
    client text not null default 'jchat-kmp',
    created_at timestamptz default now()
);

create index if not exists idx_support_reports_user_id on public.support_reports(user_id);
create index if not exists idx_support_reports_created_at on public.support_reports(created_at);

alter table public.support_reports enable row level security;

drop policy if exists "support_reports_insert_own" on public.support_reports;
create policy "support_reports_insert_own"
on public.support_reports
for insert
to authenticated
with check (auth.uid() = user_id);

drop policy if exists "support_reports_select_own" on public.support_reports;
create policy "support_reports_select_own"
on public.support_reports
for select
to authenticated
using (auth.uid() = user_id);
