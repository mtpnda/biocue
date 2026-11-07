// BioCue Master Layout Engine
// Delivers coherent DLS styling, accessible semantics, and locale-aware navigation.

import { GLOBAL_STYLES } from "./theme.ts";
import { renderHeader } from "./components/header.ts";
import { renderFooter } from "./components/footer.ts";
import { Locale } from "../i18n/index.ts";

export interface PageOptions {
  title?: string;
  description?: string;
  content: string;
  showHeader?: boolean;
  showFooter?: boolean;
  showNav?: boolean;
  activeTab?: string;
  locale?: Locale;
  extraHead?: string;
  scripts?: string;
}

export function renderLayout(opts: PageOptions): string {
  const title = opts.title ?? "BioCue — Bio-Signal Cueing & Sensory Overload Protection";
  const desc = opts.description ?? "Predict sensory overloads 5-15 minutes early using Wear OS physiological sensing, local Room DB scoring, and zero-anxiety calm anchor design.";
  const showHeader = opts.showHeader ?? true;
  const showFooter = opts.showFooter ?? true;
  const locale = opts.locale || "en";

  return `<!DOCTYPE html>
<html lang="${locale.split('-')[0]}" data-theme="calm">
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover"/>
  <meta name="theme-color" content="#F2F0EA"/>
  <title>${title}</title>
  <meta name="description" content="${desc}"/>

  <!-- Favicon & Official App Icons (Mindful Contours) -->
  <link rel="icon" type="image/png" sizes="32x32" href="/icon.png"/>
  <link rel="icon" type="image/svg+xml" href="/icon.svg"/>
  <link rel="shortcut icon" href="/favicon.ico"/>
  <link rel="apple-touch-icon" sizes="180x180" href="/icon.png"/>

  <!-- OpenGraph / Social Meta -->
  <meta property="og:title" content="${title}"/>
  <meta property="og:description" content="${desc}"/>
  <meta property="og:type" content="website"/>
  <meta property="og:image" content="https://biocue.matapandastudio.deno.net/icon.png"/>
  <meta property="og:image:width" content="512"/>
  <meta property="og:image:height" content="512"/>
  <meta name="twitter:card" content="summary_large_image"/>
  <meta name="twitter:image" content="https://biocue.matapandastudio.deno.net/icon.png"/>

  <!-- Google Fonts Preconnect -->
  <link rel="preconnect" href="https://fonts.googleapis.com"/>
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin/>

  <!-- DLS Theme Initializer (Prevents FOUC) -->
  <script>
    try {
      const savedTheme = localStorage.getItem('biocue_theme') || 'calm';
      document.documentElement.setAttribute('data-theme', savedTheme);
    } catch (_) {}
  </script>

  <!-- Universal BioCue Design Language System Stylesheet -->
  <style>
    ${GLOBAL_STYLES}
  </style>

  ${opts.extraHead ?? ''}
</head>
<body>
  ${showHeader ? renderHeader({ showNav: opts.showNav, activeTab: opts.activeTab, locale }) : ''}

  <main id="main-content">
    ${opts.content}
  </main>

  ${showFooter ? renderFooter({ locale }) : ''}

  <!-- Interactive Global Scripts -->
  <script>
    // Smooth scrolling for hash links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
      anchor.addEventListener('click', function (e) {
        const href = this.getAttribute('href');
        if (href !== '#' && document.querySelector(href)) {
          e.preventDefault();
          document.querySelector(href).scrollIntoView({ behavior: 'smooth' });
        }
      });
    });

    // Close dropdowns on outer click
    document.addEventListener('click', (e) => {
      const switcher = document.querySelector('.locale-switcher');
      if (switcher && !switcher.contains(e.target)) {
        const dd = switcher.querySelector('.locale-dropdown');
        if (dd) dd.style.display = 'none';
      }
    });
  </script>

  ${opts.scripts ?? ''}
</body>
</html>`;
}
