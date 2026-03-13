alter table public.messages
	add column if not exists reply_to_message_id uuid references public.messages(id) on delete set null,
	add column if not exists reply_preview text;

create index if not exists idx_messages_reply_to on public.messages(reply_to_message_id);
