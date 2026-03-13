# Supabase CLI Workflow (JChat)

This repository is configured to manage database changes through Supabase migrations.

## Current project ref

- `ppincerggnnauznalbjd`

## Prerequisites

1. Install Supabase CLI (already done in this environment):
   - Binary path: `/home/codespace/.local/bin/supabase`
2. Authenticate CLI with your personal access token:

```bash
supabase login
# or
export SUPABASE_ACCESS_TOKEN="your-token"
```

3. Link this repo to the hosted project:

```bash
supabase link --project-ref ppincerggnnauznalbjd --password "your-db-password"
```

## Migrations

Initial migration is already created at:

- `supabase/migrations/20260313134449_init_jchat_schema.sql`

Apply migrations to the remote database:

```bash
supabase db push
```

Create a new migration:

```bash
supabase migration new your_change_name
```

Then edit the generated SQL file and push again:

```bash
supabase db push
```

## Notes

- If `supabase db push` fails with auth errors, make sure `SUPABASE_ACCESS_TOKEN` is set and valid.
- If it fails on link step, verify the database password in Supabase dashboard:
  - Project Settings -> Database -> Database password.
