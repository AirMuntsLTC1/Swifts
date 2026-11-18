-- Post images: bucket and storage rules. Each user uploads only into their own folder.
-- Run once in the Supabase SQL Editor after 20261118001000.
-- Note: the bucket is public, so an image's URL works for anyone who has it. Paths include the user's id and a random file name.

insert into storage.buckets (id, name, public)
values ('posts', 'posts', true)
on conflict (id) do nothing;

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
