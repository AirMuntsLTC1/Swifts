-- Replaces the profile function so new sign-ups use the Google name and email. Safe to run.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, username, display_name, avatar_url)
  values (
    new.id,
    coalesce(
      nullif(lower(new.raw_user_meta_data->>'username'), ''),
      left(regexp_replace(lower(split_part(new.email, '@', 1)), '[^a-z0-9_.]', '', 'g'), 25)
        || '_' || substr(new.id::text, 1, 4)
    ),
    coalesce(new.raw_user_meta_data->>'full_name', new.raw_user_meta_data->>'name', ''),
    new.raw_user_meta_data->>'avatar_url'
  );
  insert into public.user_settings (user_id) values (new.id);
  return new;
end;
$$;
