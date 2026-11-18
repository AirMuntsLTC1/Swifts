-- Sends new chat messages to open apps in real time. Row Level Security still applies to each listener. NOT APPLIED YET.
do $$
begin
  alter publication supabase_realtime add table public.messages;
exception
  when duplicate_object then null;
end $$;
