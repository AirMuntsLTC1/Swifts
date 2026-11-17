-- Swifts initial schema. NOT APPLIED YET: run on Supabase after the app is complete.
create extension if not exists "pgcrypto";

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  username text unique not null check (username ~ '^[a-z0-9_.]{3,30}$'),
  display_name text not null default '',
  avatar_url text,
  bio text not null default '',
  lang text not null default 'en',
  city text, hometown text, work text, school text,
  created_at timestamptz not null default now()
);

create table public.follows (
  follower_id uuid references public.profiles(id) on delete cascade,
  followee_id uuid references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (follower_id, followee_id),
  check (follower_id <> followee_id)
);

create table public.posts (
  id uuid primary key default gen_random_uuid(),
  author_id uuid not null references public.profiles(id) on delete cascade,
  body text not null default '' check (char_length(body) <= 5000),
  lang text not null default 'und',
  visibility text not null default 'public' check (visibility in ('public','followers','private')),
  group_id uuid,
  created_at timestamptz not null default now()
);
create index posts_feed_idx on public.posts (lang, created_at desc);

create table public.post_media (
  id uuid primary key default gen_random_uuid(),
  post_id uuid not null references public.posts(id) on delete cascade,
  kind text not null check (kind in ('image','video')),
  storage_path text not null,
  thumb_path text,
  position int not null default 0
);

create table public.comments (
  id uuid primary key default gen_random_uuid(),
  post_id uuid not null references public.posts(id) on delete cascade,
  parent_id uuid references public.comments(id) on delete cascade,
  author_id uuid not null references public.profiles(id) on delete cascade,
  body text not null check (char_length(body) between 1 and 2000),
  lang text not null default 'und',
  created_at timestamptz not null default now()
);

create table public.likes (
  user_id uuid references public.profiles(id) on delete cascade,
  post_id uuid references public.posts(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, post_id)
);

create table public.hashtags (
  id uuid primary key default gen_random_uuid(),
  tag text unique not null check (tag ~ '^[a-z0-9_]{1,50}$')
);

create table public.post_hashtags (
  post_id uuid references public.posts(id) on delete cascade,
  hashtag_id uuid references public.hashtags(id) on delete cascade,
  primary key (post_id, hashtag_id)
);

create table public.groups (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  description text not null default '',
  privacy text not null default 'public' check (privacy in ('public','private')),
  cover_url text,
  created_by uuid not null references public.profiles(id) on delete cascade,
  lang text not null default 'und',
  created_at timestamptz not null default now()
);

create table public.group_members (
  group_id uuid references public.groups(id) on delete cascade,
  user_id uuid references public.profiles(id) on delete cascade,
  role text not null default 'member' check (role in ('owner','admin','member')),
  primary key (group_id, user_id)
);

alter table public.posts add constraint posts_group_fk
  foreign key (group_id) references public.groups(id) on delete set null;

create table public.stories (
  id uuid primary key default gen_random_uuid(),
  author_id uuid not null references public.profiles(id) on delete cascade,
  media_path text not null,
  overlay jsonb not null default '{}'::jsonb,
  location_label text,
  expires_at timestamptz not null default (now() + interval '24 hours'),
  created_at timestamptz not null default now()
);

create table public.conversations (
  id uuid primary key default gen_random_uuid(),
  is_group boolean not null default false,
  created_at timestamptz not null default now()
);

create table public.conversation_members (
  conversation_id uuid references public.conversations(id) on delete cascade,
  user_id uuid references public.profiles(id) on delete cascade,
  primary key (conversation_id, user_id)
);

create table public.messages (
  id uuid primary key default gen_random_uuid(),
  conversation_id uuid not null references public.conversations(id) on delete cascade,
  sender_id uuid not null references public.profiles(id) on delete cascade,
  body text not null default '',
  story_id uuid references public.stories(id) on delete set null,
  created_at timestamptz not null default now()
);

create table public.notifications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  actor_id uuid references public.profiles(id) on delete cascade,
  kind text not null,
  ref_id uuid,
  read boolean not null default false,
  created_at timestamptz not null default now()
);

create table public.user_settings (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  lang text not null default 'en',
  who_can_message text not null default 'everyone' check (who_can_message in ('everyone','followers','none')),
  push_enabled boolean not null default true
);

create table public.translations (
  entity_type text not null check (entity_type in ('post','comment','group')),
  entity_id uuid not null,
  lang text not null,
  body text not null,
  primary key (entity_type, entity_id, lang)
);

create table public.reports (
  id uuid primary key default gen_random_uuid(),
  reporter_id uuid not null references public.profiles(id) on delete cascade,
  entity_type text not null,
  entity_id uuid not null,
  reason text not null,
  created_at timestamptz not null default now()
);

-- Row Level Security: enabled on every table
alter table public.profiles enable row level security;
alter table public.follows enable row level security;
alter table public.posts enable row level security;
alter table public.post_media enable row level security;
alter table public.comments enable row level security;
alter table public.likes enable row level security;
alter table public.hashtags enable row level security;
alter table public.post_hashtags enable row level security;
alter table public.groups enable row level security;
alter table public.group_members enable row level security;
alter table public.stories enable row level security;
alter table public.conversations enable row level security;
alter table public.conversation_members enable row level security;
alter table public.messages enable row level security;
alter table public.notifications enable row level security;
alter table public.user_settings enable row level security;
alter table public.translations enable row level security;
alter table public.reports enable row level security;

create policy "profiles readable" on public.profiles for select to authenticated using (true);
create policy "profiles self update" on public.profiles for update to authenticated using (auth.uid() = id) with check (auth.uid() = id);

create policy "posts visible" on public.posts for select to authenticated
  using (visibility = 'public' or author_id = auth.uid()
    or (visibility = 'followers' and exists (select 1 from public.follows f where f.follower_id = auth.uid() and f.followee_id = author_id)));
create policy "posts insert own" on public.posts for insert to authenticated with check (auth.uid() = author_id);
create policy "posts update own" on public.posts for update to authenticated using (auth.uid() = author_id);
create policy "posts delete own" on public.posts for delete to authenticated using (auth.uid() = author_id);

create policy "likes read" on public.likes for select to authenticated using (true);
create policy "likes own" on public.likes for all to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "comments read" on public.comments for select to authenticated using (true);
create policy "comments insert own" on public.comments for insert to authenticated with check (auth.uid() = author_id);
create policy "comments delete own" on public.comments for delete to authenticated using (auth.uid() = author_id);

create policy "follows read" on public.follows for select to authenticated using (true);
create policy "follows own" on public.follows for all to authenticated using (auth.uid() = follower_id) with check (auth.uid() = follower_id);

create policy "notifications own" on public.notifications for select to authenticated using (auth.uid() = user_id);
create policy "settings own" on public.user_settings for all to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "translations read" on public.translations for select to authenticated using (true);
create policy "reports insert" on public.reports for insert to authenticated with check (auth.uid() = reporter_id);
create policy "stories read" on public.stories for select to authenticated using (expires_at > now());
create policy "stories insert own" on public.stories for insert to authenticated with check (auth.uid() = author_id);
create policy "hashtags read" on public.hashtags for select to authenticated using (true);
create policy "groups read" on public.groups for select to authenticated using (privacy = 'public' or exists (select 1 from public.group_members m where m.group_id = id and m.user_id = auth.uid()));
create policy "groups insert own" on public.groups for insert to authenticated with check (auth.uid() = created_by);
create policy "group_members read" on public.group_members for select to authenticated using (true);
create policy "group_members self" on public.group_members for insert to authenticated with check (auth.uid() = user_id);
create policy "post_media read" on public.post_media for select to authenticated using (true);
create policy "post_hashtags read" on public.post_hashtags for select to authenticated using (true);
