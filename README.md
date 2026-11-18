# Swifts

Multi-language social platform. No Flutter.

- Android client: Kotlin + Jetpack Compose (android/)
- Database and auth: Supabase (supabase/migrations/, RLS on every table)
- Edge functions: TypeScript (supabase/functions/)
- Media worker: PHP (services/media-php/)
- Web pages: PHP with the global theme (services/web/)
- Tooling and checks: Python (tools/)
- Native core: C++ (native/)

Deploy the database with `supabase db push --remote` and functions with `supabase functions deploy <name>`.
CI runs on every push: contracts, theme rules, PHP lint, C++ compile, and the signed arm64 debug APK.
