-- Audit fixes: privacy leaks, missing RLS checks, chat rules, sign-up collisions, and missing indexes.
-- Run once in the Supabase SQL Editor. Earlier migrations are already applied and are not edited.

-- 1. Membership check as a helper, so policies do not recurse into each other.
create or replace function public.is_group_member(gid uuid)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.group_members m where m.group_id = gid and m.user_id = auth.uid());
$$;

-- 2. Private groups are visible only to their members.
drop policy if exists "groups read" on public.groups;
create policy "groups read" on public.groups for select to authenticated
  using (privacy = 'public' or public.is_group_member(id));

-- 3. Private group rosters are visible only to their members.
drop policy if exists "group_members read" on public.group_members;
create policy "group_members read" on public.group_members for select to authenticated
  using (
    exists (select 1 from public.groups g where g.id = group_members.group_id and g.privacy = 'public')
    or public.is_group_member(group_members.group_id)
  );

-- 4. Posts inside private groups are visible only to members. Visibility rules still apply.
drop policy if exists "posts visible" on public.posts;
create policy "posts visible" on public.posts for select to authenticated
  using (
    author_id = auth.uid()
    or (
      (group_id is null
        or exists (select 1 from public.groups g where g.id = posts.group_id and g.privacy = 'public')
        or public.is_group_member(posts.group_id))
      and (visibility = 'public'
        or (visibility = 'followers' and exists (
              select 1 from public.follows f
              where f.follower_id = auth.uid() and f.followee_id = posts.author_id)))
    )
  );

-- 5. Comments and post media follow the post: you cannot read or add them on a post you cannot see.
drop policy if exists "comments read" on public.comments;
create policy "comments read" on public.comments for select to authenticated
  using (exists (select 1 from public.posts p where p.id = comments.post_id));

drop policy if exists "comments insert own" on public.comments;
create policy "comments insert own" on public.comments for insert to authenticated
  with check (auth.uid() = author_id and exists (select 1 from public.posts p where p.id = comments.post_id));

drop policy if exists "post_media read" on public.post_media;
create policy "post_media read" on public.post_media for select to authenticated
  using (exists (select 1 from public.posts p where p.id = post_media.post_id));

drop policy if exists "post_media insert own" on public.post_media;
create policy "post_media insert own" on public.post_media for insert to authenticated
  with check (exists (select 1 from public.posts p where p.id = post_media.post_id and p.author_id = auth.uid()));

-- 6. Likes need a visible post. You can only remove your own like.
drop policy if exists "likes own" on public.likes;
drop policy if exists "likes insert own" on public.likes;
create policy "likes insert own" on public.likes for insert to authenticated
  with check (auth.uid() = user_id and exists (select 1 from public.posts p where p.id = likes.post_id));
drop policy if exists "likes delete own" on public.likes;
create policy "likes delete own" on public.likes for delete to authenticated
  using (auth.uid() = user_id);

-- 7. Hashtags: any signed-in user can create a tag; only the author can attach tags to their post.
drop policy if exists "hashtags insert" on public.hashtags;
create policy "hashtags insert" on public.hashtags for insert to authenticated with check (true);

drop policy if exists "post_hashtags insert own" on public.post_hashtags;
create policy "post_hashtags insert own" on public.post_hashtags for insert to authenticated
  with check (exists (select 1 from public.posts p where p.id = post_hashtags.post_id and p.author_id = auth.uid()));

-- 8. Story files: owners can delete their own files, so expired stories do not leave orphans.
drop policy if exists "stories delete own folder" on storage.objects;
create policy "stories delete own folder" on storage.objects for delete to authenticated
  using (bucket_id = 'stories' and (storage.foldername(name))[1] = auth.uid()::text);

-- 9. Chats: honour "followers only", and stop two concurrent starts from creating duplicate chats.
create or replace function public.start_conversation(other uuid)
returns uuid language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid();
  conv uuid;
  setting text;
begin
  if me is null then raise exception 'not signed in'; end if;
  if other = me then raise exception 'cannot message yourself'; end if;

  select s.who_can_message into setting from public.user_settings s where s.user_id = other;
  if setting = 'none' then raise exception 'messages_disabled'; end if;
  if setting = 'followers' and not exists (
       select 1 from public.follows f where f.follower_id = me and f.followee_id = other) then
    raise exception 'messages_followers_only';
  end if;

  perform pg_advisory_xact_lock(hashtextextended(least(me, other)::text || ':' || greatest(me, other)::text, 0));

  select c.id into conv
  from public.conversations c
  where c.is_group = false
    and exists (select 1 from public.conversation_members a where a.conversation_id = c.id and a.user_id = me)
    and exists (select 1 from public.conversation_members b where b.conversation_id = c.id and b.user_id = other)
    and (select count(*) from public.conversation_members x where x.conversation_id = c.id) = 2
  limit 1;

  if conv is null then
    insert into public.conversations (is_group) values (false) returning id into conv;
    insert into public.conversation_members (conversation_id, user_id) values (conv, me), (conv, other);
  end if;
  return conv;
end;
$$;

-- 10. Sign-up: a requested username that is invalid or taken falls back to a generated one,
--     so the auth user is never blocked by a unique-key error.
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
declare
  requested text := lower(coalesce(new.raw_user_meta_data->>'username', ''));
  generated text := left(regexp_replace(lower(split_part(new.email, '@', 1)), '[^a-z0-9_.]', '', 'g'), 25)
                    || '_' || substr(new.id::text, 1, 4);
  chosen text;
begin
  if requested ~ '^[a-z0-9_.]{3,30}$'
     and not exists (select 1 from public.profiles p where p.username = requested) then
    chosen := requested;
  else
    chosen := generated;
  end if;

  insert into public.profiles (id, username, display_name, avatar_url)
  values (new.id, chosen,
          coalesce(new.raw_user_meta_data->>'full_name', new.raw_user_meta_data->>'name', ''),
          new.raw_user_meta_data->>'avatar_url');
  insert into public.user_settings (user_id) values (new.id);
  return new;
end;
$$;

-- 11. Indexes for the hot paths: post comments, chat history, notifications, feeds, rosters, hashtags.
create index if not exists comments_post_created_idx on public.comments (post_id, created_at);
create index if not exists comments_parent_idx on public.comments (parent_id);
create index if not exists comments_author_idx on public.comments (author_id);
create index if not exists comment_likes_comment_idx on public.comment_likes (comment_id);
create index if not exists messages_conversation_created_idx on public.messages (conversation_id, created_at desc);
create index if not exists messages_sender_idx on public.messages (sender_id);
create index if not exists notifications_user_created_idx on public.notifications (user_id, created_at desc);
create index if not exists post_media_post_idx on public.post_media (post_id, position);
create index if not exists post_hashtags_hashtag_idx on public.post_hashtags (hashtag_id);
create index if not exists likes_post_idx on public.likes (post_id);
create index if not exists follows_followee_idx on public.follows (followee_id);
create index if not exists group_members_user_idx on public.group_members (user_id);
create index if not exists conversation_members_user_idx on public.conversation_members (user_id);
create index if not exists posts_author_created_idx on public.posts (author_id, created_at desc);
create index if not exists posts_group_idx on public.posts (group_id);
create index if not exists stories_author_expires_idx on public.stories (author_id, expires_at desc);
create index if not exists reports_entity_idx on public.reports (entity_type, entity_id);
