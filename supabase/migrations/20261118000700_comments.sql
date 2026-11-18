-- Hearts on comments and replies, plus a notification when someone comments on your post. NOT APPLIED YET.

create table public.comment_likes (
  user_id uuid references public.profiles(id) on delete cascade,
  comment_id uuid references public.comments(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, comment_id)
);

alter table public.comment_likes enable row level security;

create policy "comment likes read" on public.comment_likes
  for select to authenticated using (true);

create policy "comment likes own" on public.comment_likes
  for all to authenticated
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create or replace function public.notify_comment()
returns trigger language plpgsql security definer set search_path = public as $$
declare owner uuid;
begin
  select author_id into owner from public.posts where id = new.post_id;
  if owner is not null and owner <> new.author_id then
    insert into public.notifications (user_id, actor_id, kind, ref_id)
    values (owner, new.author_id, 'comment', new.post_id);
  end if;
  return new;
end;
$$;

drop trigger if exists on_comment_notify on public.comments;
create trigger on_comment_notify after insert on public.comments
  for each row execute function public.notify_comment();
