// Supabase Edge Function: GET /feed?lang=xx&cursor=ISO&limit=N
import { createClient } from "npm:@supabase/supabase-js@2";

const SUPPORTED = new Set(["en", "fil", "es", "ar", "hi", "zh", "fr", "pt"]);
const SELECT =
  "id, author_id, body, lang, visibility, created_at, " +
  "author:profiles!posts_author_id_fkey(username, display_name, avatar_url), " +
  "post_media(kind, storage_path, thumb_path)";

Deno.serve(async (req: Request): Promise<Response> => {
  const url = new URL(req.url);
  const lang = url.searchParams.get("lang") ?? "en";
  const cursor = url.searchParams.get("cursor");
  const limit = Math.min(Number(url.searchParams.get("limit") ?? 20), 50);

  if (!SUPPORTED.has(lang)) return json({ error: "unsupported_lang" }, 400);

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: req.headers.get("Authorization") ?? "" } } },
  );

  let query = supabase
    .from("posts")
    .select(SELECT)
    .eq("lang", lang)
    .order("created_at", { ascending: false })
    .limit(limit);

  if (cursor) query = query.lt("created_at", cursor);

  const { data, error } = await query;
  if (error) return json({ error: error.message }, 500);

  const nextCursor = data.length === limit ? data[data.length - 1].created_at : null;
  return json({ items: data, next_cursor: nextCursor }, 200);
});

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8" },
  });
}
