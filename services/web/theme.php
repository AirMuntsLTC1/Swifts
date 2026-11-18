<?php
declare(strict_types=1);

/**
 * Global theme for every Swifts web page.
 * Load it first on each page, before any output:
 *   require_once __DIR__ . '/theme.php';
 * Colors live only in theme.json. Default mode is light.
 */

const THEME_COOKIE  = 'swifts_theme';
const THEME_DEFAULT = 'light';
const THEME_MODES   = ['light', 'dark'];

/** Reads theme.json once per request. */
function themeConfig(): array
{
    static $config = null;
    if ($config === null) {
        $path = __DIR__ . '/theme.json';
        $raw = is_file($path) ? file_get_contents($path) : false;
        if ($raw === false) {
            throw new RuntimeException('theme.json is missing');
        }
        $config = json_decode($raw, true, 512, JSON_THROW_ON_ERROR);
    }
    return $config;
}

/** Returns "light" or "dark". Anything else falls back to the default. */
function currentTheme(): string
{
    $value = $_COOKIE[THEME_COOKIE] ?? THEME_DEFAULT;
    return in_array($value, THEME_MODES, true) ? $value : THEME_DEFAULT;
}

$currentTheme = currentTheme();

/** Flips the theme cookie and redirects back to the same page. */
function handleThemeToggle(): void
{
    if (!isset($_GET['toggle_theme'])) {
        return;
    }
    $next = currentTheme() === 'dark' ? 'light' : 'dark';
    setcookie(THEME_COOKIE, $next, [
        'expires'  => time() + 60 * 60 * 24 * 365,
        'path'     => '/',
        'secure'   => !empty($_SERVER['HTTPS']),
        'httponly' => true,
        'samesite' => 'Lax',
    ]);
    $params = $_GET;
    unset($params['toggle_theme']);
    $path  = strtok($_SERVER['REQUEST_URI'] ?? '/', '?') ?: '/';
    $query = http_build_query($params);
    header('Location: ' . $path . ($query !== '' ? '?' . $query : ''), true, 303);
    exit;
}

handleThemeToggle();

/** Turns theme tokens into CSS custom properties. Invalid entries are skipped. */
function themeTokensCss(array $tokens): string
{
    $css = '';
    foreach ($tokens as $name => $value) {
        if (!preg_match('/^[a-z][a-z0-9-]*$/', (string) $name)) {
            continue;
        }
        if (!is_string($value) || !preg_match('/^#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$/', $value)) {
            continue;
        }
        $css .= "--{$name}: {$value}; ";
    }
    return $css;
}

/** Call inside <head>. Outputs the color variables and base styles. */
function renderGlobalTheme(): void
{
    $cfg   = themeConfig();
    $light = themeTokensCss($cfg['light'] ?? []);
    $dark  = themeTokensCss($cfg['dark'] ?? []);
    ?>
<meta name="color-scheme" content="light dark">
<style>
:root { <?= $light ?> color-scheme: light; }
:root[data-theme="dark"] { <?= $dark ?> color-scheme: dark; }
html, body { background: var(--bg-main); color: var(--text-main); }
.theme-toggle {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--brand-primary);
  color: var(--bg-card);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
}
.theme-toggle:hover { background: var(--brand-hover); }
</style>
    <?php
}

/** Call inside the navigation bar or a settings header. */
function renderThemeButton(): void
{
    global $currentTheme;
    $label = $currentTheme === 'dark' ? 'Light mode' : 'Dark mode';
    $query = http_build_query(array_merge($_GET, ['toggle_theme' => '1']));
    $href  = htmlspecialchars('?' . $query, ENT_QUOTES, 'UTF-8');
    echo '<a class="theme-toggle" href="' . $href . '">'
        . htmlspecialchars($label, ENT_QUOTES, 'UTF-8') . '</a>';
}
