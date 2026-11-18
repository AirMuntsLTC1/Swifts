-- Post media and profile photos: storage buckets, owner-folder upload rules, and post_media insert/delete rules.
-- Run once in the Supabase SQL Editor. Safe to run again.
--
-- Ordering note: this file is named 20261118000800 as requested. That sorts before 20261118001000 and
-- 20261118001100, which are already applied. The SQL Editor ignores order, so this runs. `supabase db push`
-- would refuse it, so if you use db push later, rename this file to 20261118001200.
--
-- Paths: posts/<user_id>/<file>, avatars/<user_id>/<file>. Buckets are public, so a URL works for anyone who has it.

-- 1. Buckets with size and type limits. Re-running this updates the limits.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values
  ('posts', 'posts', true, 52428800,
    array['image/jpeg','image/png','image/webp','image/gif','video/mp4','video/quicktime','video/webm']),
  ('avatars', 'avatars', true, 5242880,
    array['image/jpeg','image/png','image/webp'])
on conflict (id) do update
  set public = excluded.public,
      file_size_limit = excluded.file_size_limit,
      allowed_mime_types = excluded.allowed_mime_types;

-- 2. Post media: upload, read and delete only inside your own folder.
drop policy if exists "posts media upload own folder" on storage.objects;
create policy "posts media upload own folder" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'posts' and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists "posts media public read" on storage.objects;
create policy "posts media public read" on storage.objects
  for select using (bucket_id = 'posts');

drop policy if exists "posts media delete own folder" on storage.objects;
create policy "posts media delete own folder" on storage.objects
  for delete to authenticated
  using (bucket_id = 'posts' and (storage.foldername(name))[1] = auth.uid()::text);

-- 3. Avatars: same rules, in their own bucket.
drop policy if exists "avatars upload own folder" on storage.objects;
create policy "avatars upload own folder" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists "avatars public read" on storage.objects;
create policy "avatars public read" on storage.objects
  for select using (bucket_id = 'avatars');

drop policy if exists "avatars delete own folder" on storage.objects;
create policy "avatars delete own folder" on storage.objects
  for delete to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);

-- 4. post_media rows: you can attach or remove media only on your own post.
drop policy if exists "post_media insert own" on public.post_media;
create policy "post_media insert own" on public.post_media
  for insert to authenticated
  with check (exists (select 1 from public.posts p
                      where p.id = post_media.post_id and p.author_id = auth.uid()));

drop policy if exists "post_media delete own" on public.post_media;
create policy "post_media delete own" on public.post_media
  for delete to authenticated
  using (exists (select 1 from public.posts p
                 where p.id = post_media.post_id and p.author_id = auth.uid()));
