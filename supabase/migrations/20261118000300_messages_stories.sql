-- Messages (direct chats), stories storage, and story/message rules. NOT APPLIED YET.

-- Starts a direct chat, or returns the existing one. Runs with elevated rights so both members can be added.
create or replace function public.start_conversation(other uuid)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  me uuid := auth.uid();
  conv uuid;
begin
  if me is null then raise exception 'not signed in'; end if;
  if other = me then raise exception 'cannot message yourself'; end if;
  if exists (select 1 from public.user_settings s where s.user_id = other and s.who_can_message = 'none') then
    raise exception 'messages_disabled';
  end if;

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

-- Conversations and messages: members only.
create policy "conversations read member" on public.conversations
  for select to authenticated
  using (exists (select 1 from public.conversation_members m
                 where m.conversation_id = conversations.id and m.user_id = auth.uid()));

create policy "conv members read" on public.conversation_members
  for select to authenticated
  using (exists (select 1 from public.conversation_members m
                 where m.conversation_id = conversation_members.conversation_id and m.user_id = auth.uid()));

create policy "messages read member" on public.messages
  for select to authenticated
  using (exists (select 1 from public.conversation_members m
                 where m.conversation_id = messages.conversation_id and m.user_id = auth.uid()));

create policy "messages send member" on public.messages
  for insert to authenticated
  with check (sender_id = auth.uid()
    and exists (select 1 from public.conversation_members m
                where m.conversation_id = messages.conversation_id and m.user_id = auth.uid()));

-- Stories: owners can delete their own.
create policy "stories delete own" on public.stories
  for delete to authenticated
  using (auth.uid() = author_id);

-- Story images: public bucket, each user uploads only into their own folder.
insert into storage.buckets (id, name, public)
values ('stories', 'stories', true)
on conflict (id) do nothing;

create policy "stories upload own folder" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'stories' and (storage.foldername(name))[1] = auth.uid()::text);

create policy "stories public read" on storage.objects
  for select using (bucket_id = 'stories');
