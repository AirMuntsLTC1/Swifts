-- Creates notification rows for likes, follows, and chat messages. NOT APPLIED YET.

create or replace function public.notify_like()
returns trigger language plpgsql security definer set search_path = public as $$
declare owner uuid;
begin
  select author_id into owner from public.posts where id = new.post_id;
  if owner is not null and owner <> new.user_id then
    insert into public.notifications (user_id, actor_id, kind, ref_id)
    values (owner, new.user_id, 'like', new.post_id);
  end if;
  return new;
end;
$$;

drop trigger if exists on_like_notify on public.likes;
create trigger on_like_notify after insert on public.likes
  for each row execute function public.notify_like();

create or replace function public.notify_follow()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  insert into public.notifications (user_id, actor_id, kind, ref_id)
  values (new.followee_id, new.follower_id, 'follow', null);
  return new;
end;
$$;

drop trigger if exists on_follow_notify on public.follows;
create trigger on_follow_notify after insert on public.follows
  for each row execute function public.notify_follow();

create or replace function public.notify_message()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  insert into public.notifications (user_id, actor_id, kind, ref_id)
  select m.user_id, new.sender_id, 'message', new.conversation_id
  from public.conversation_members m
  where m.conversation_id = new.conversation_id
    and m.user_id <> new.sender_id;
  return new;
end;
$$;

drop trigger if exists on_message_notify on public.messages;
create trigger on_message_notify after insert on public.messages
  for each row execute function public.notify_message();

-- Users can mark their own notifications as read.
create policy "notifications update own" on public.notifications
  for update to authenticated
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);
