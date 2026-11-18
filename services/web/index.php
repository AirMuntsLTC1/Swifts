<?php
require_once __DIR__ . '/theme.php';
?>
<!DOCTYPE html>
<html lang="en" data-theme="<?= htmlspecialchars($currentTheme) ?>">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Swifts</title>
  <?php renderGlobalTheme(); ?>
  <style>
    body { margin: 0; font-family: system-ui, sans-serif; }
    .navbar {
      display: flex; justify-content: space-between; align-items: center;
      padding: 12px 16px;
      background: var(--bg-surface);
      border-bottom: 1px solid var(--border-subtle);
    }
    .navbar h1 { margin: 0; font-size: 22px; font-weight: 700; letter-spacing: -0.4px; color: var(--text-main); }
    .card {
      margin: 16px; padding: 16px;
      background: var(--bg-card);
      border: 1px solid var(--border-subtle);
      border-radius: 12px;
    }
    .card p { margin: 0; color: var(--text-main); }
    .card small { color: var(--text-muted); }
    .btn {
      display: inline-block; margin-top: 12px; padding: 8px 14px;
      border-radius: 999px;
      background: var(--brand-primary); color: var(--bg-card);
      text-decoration: none; font-weight: 600;
    }
    .btn:hover { background: var(--brand-hover); }
  </style>
</head>
<body>
  <header class="navbar">
    <h1>Swifts</h1>
    <?php renderThemeButton(); ?>
  </header>
  <main>
    <section class="card">
      <p>Welcome to Swifts.</p>
      <small>Muted text sample</small>
      <br>
      <a class="btn" href="#">Primary action</a>
    </section>
  </main>
</body>
</html>
