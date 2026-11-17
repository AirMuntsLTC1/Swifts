"""Verifies every table in contracts/tables.json has a CREATE TABLE and RLS in the migrations."""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
expected = json.loads((ROOT / "contracts" / "tables.json").read_text())["tables"]
sql = "\n".join(p.read_text() for p in sorted((ROOT / "supabase" / "migrations").glob("*.sql")))

errors = []
for table in expected:
    if not re.search(rf"create table public\.{table}\b", sql):
        errors.append(f"missing table: {table}")
    if not re.search(rf"alter table public\.{table} enable row level security", sql):
        errors.append(f"RLS not enabled: {table}")

if errors:
    print("\n".join(errors))
    sys.exit(1)
print(f"OK: {len(expected)} tables, all with RLS")
