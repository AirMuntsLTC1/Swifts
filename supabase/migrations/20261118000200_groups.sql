-- Groups: owner membership, joins limited to public groups, group posts need membership. NOT APPLIED YET.

-- Creator becomes the owner member automatically.
create or replace function public.handle_new_group()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.group_members (group_id, user_id, role)
  values (new.id, new.created_by, 'owner');
  return new;
end;
$$;

drop trigger if exists on_group_created on public.groups;
create trigger on_group_created
  after insert on public.groups
  for each row execute function public.handle_new_group();

-- Replace the old open join policy: only public groups can be joined directly.
drop policy if exists "group_members self" on public.group_members;

create policy "group_members join public" on public.group_members
  for insert to authenticated
  with check (
    auth.uid() = user_id
    and exists (select 1 from public.groups g where g.id = group_id and g.privacy = 'public')
  );

create policy "group_members leave own" on public.group_members
  for delete to authenticated
  using (auth.uid() = user_id);

-- Group posts need membership. Home posts (group_id null) stay open to the author.
drop policy if exists "posts insert own" on public.posts;

create policy "posts insert own" on public.posts
  for insert to authenticated
  with check (
    auth.uid() = author_id
    and (
      group_id is null
      or exists (
        select 1 from public.group_members m
        where m.group_id = posts.group_id and m.user_id = auth.uid()
      )
    )
  );
