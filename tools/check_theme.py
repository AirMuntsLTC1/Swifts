"""Checks the global theme: theme.json is complete, every theme.json color also exists in
Theme.kt, and no web page writes its own colors (only theme.php defines them)."""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
REQUIRED = ["bg-main", "bg-surface", "bg-card", "text-main", "text-muted",
            "border-subtle", "brand-primary", "brand-hover"]
errors = []

cfg = json.loads((ROOT / "services/web/theme.json").read_text())
for mode in ("light", "dark"):
    for key in REQUIRED:
        if key not in cfg.get(mode, {}):
            errors.append(f"theme.json: {mode} is missing {key}")

# Every token value must appear in Theme.kt (ARGB form: FF + hex).
kt = (ROOT / "android/app/src/main/java/com/swifts/app/ui/theme/Theme.kt").read_text().upper()
for mode in ("light", "dark"):
    for key, value in cfg.get(mode, {}).items():
        if f"FF{value.lstrip('#').upper()}" not in kt:
            errors.append(f"Theme.kt is missing {mode} {key} {value}")

hard = re.compile(r"(?<![\w-])#[0-9a-fA-F]{3,8}(?![\w-])|\bwhite\b|\bblack\b", re.I)
for page in sorted((ROOT / "services/web").glob("*.php")):
    if page.name == "theme.php":
        continue
    for lineno, line in enumerate(page.read_text().splitlines(), 1):
        if hard.search(line):
            errors.append(f"{page.relative_to(ROOT)}:{lineno} hardcoded color")

if errors:
    print("\n".join(errors))
    sys.exit(1)
print("OK: theme complete, Android matches every token, no hardcoded colors in pages")
