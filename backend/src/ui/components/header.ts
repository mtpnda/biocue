// Header Component for BioCue Web
// Responsive mobile-friendly navigation with theme switch and language selection

import { renderLogo } from "./logo.ts";
import { renderLocaleSwitcher, Locale, getDictionary } from "../../i18n/index.ts";

export interface HeaderOptions {
  activeTab?: string;
  showNav?: boolean;
  locale?: Locale;
}

export function renderHeader(opts: HeaderOptions = {}): string {
  const showNav = opts.showNav ?? true;
  const locale: Locale = opts.locale || "en";
  const dict = getDictionary(locale);
  const isHomePage = !opts.activeTab;
  const targetPrefix = isHomePage ? "" : (locale === "en" ? "/en" : `/${locale}`);
  const homeHref = locale === "en" ? "/en" : `/${locale}`;

  return `
  <header class="site-header">
    <div class="wrap header-wrap">
      <!-- Logo -->
      <a href="${homeHref}" style="display:inline-flex;align-items:center;text-decoration:none;color:inherit;flex-shrink:0;">
        ${renderLogo({ size: 34 })}
      </a>

      <!-- Desktop Navigation -->
      ${showNav ? `
      <nav class="desktop-nav" aria-label="Main Navigation">
        <a href="${targetPrefix}#features" class="nav-link">${dict.nav.features}</a>
        <a href="${targetPrefix}#how-it-works" class="nav-link">${dict.nav.howItWorks}</a>
        <a href="/whitepaper" class="nav-link ${opts.activeTab === 'whitepaper' ? 'active' : ''}">Whitepaper</a>
        <a href="${targetPrefix}#simulator" class="nav-link">${dict.nav.simulator}</a>
        <a href="${targetPrefix}#faq" class="nav-link">${dict.nav.faq}</a>
        <a href="/support" class="nav-link ${opts.activeTab === 'support' ? 'active' : ''}">${dict.nav.support}</a>
      </nav>
      ` : ''}

      <!-- Header Actions (Right) -->
      <div class="header-actions">
        <!-- DLS Theme Toggle -->
        <button 
          id="dlsThemeToggle"
          type="button"
          class="hit circle"
          style="width:38px;height:38px;border-radius:50%;background:var(--surface-raised);border:1px solid var(--border-strong);cursor:pointer;color:var(--text-primary);display:inline-flex;align-items:center;justify-content:center;"
          title="Toggle Calm / High Contrast Theme"
          aria-label="Toggle Calm or High Contrast Theme"
          onclick="
            const current = document.documentElement.getAttribute('data-theme') || 'calm';
            const next = current === 'calm' ? 'high-contrast' : 'calm';
            document.documentElement.setAttribute('data-theme', next);
            localStorage.setItem('biocue_theme', next);
            this.innerHTML = next === 'calm' ? '🌿' : '👁️';
          "
        >
          🌿
        </button>

        ${renderLocaleSwitcher(locale)}

        <a href="${targetPrefix}#download" class="pill pill-primary pill-sm hide-mobile" style="text-decoration:none;">
          ${dict.nav.getApp}
        </a>

        ${showNav ? `
        <!-- Mobile Menu Hamburger Button -->
        <button
          id="mobileMenuToggle"
          class="mobile-menu-btn"
          aria-label="Toggle navigation drawer"
          aria-expanded="false"
          aria-controls="mobileNavDrawer"
          onclick="
            const d = document.getElementById('mobileNavDrawer');
            const isOpen = d.classList.toggle('open');
            this.setAttribute('aria-expanded', isOpen);
            this.innerHTML = isOpen ? '✕' : '☰';
          "
        >
          ☰
        </button>
        ` : ''}
      </div>
    </div>
  </header>

  <!-- Mobile Drawer -->
  ${showNav ? `
  <div id="mobileNavDrawer" class="mobile-drawer" role="dialog" aria-label="Mobile Navigation">
    <a href="${targetPrefix}#features" class="mobile-nav-link" onclick="document.getElementById('mobileNavDrawer').classList.remove('open');">${dict.nav.features}</a>
    <a href="${targetPrefix}#how-it-works" class="mobile-nav-link" onclick="document.getElementById('mobileNavDrawer').classList.remove('open');">${dict.nav.howItWorks}</a>
    <a href="/whitepaper" class="mobile-nav-link" onclick="document.getElementById('mobileNavDrawer').classList.remove('open');">Whitepaper</a>
    <a href="${targetPrefix}#simulator" class="mobile-nav-link" onclick="document.getElementById('mobileNavDrawer').classList.remove('open');">${dict.nav.simulator}</a>
    <a href="${targetPrefix}#faq" class="mobile-nav-link" onclick="document.getElementById('mobileNavDrawer').classList.remove('open');">${dict.nav.faq}</a>
    <a href="/support" class="mobile-nav-link" onclick="document.getElementById('mobileNavDrawer').classList.remove('open');">${dict.nav.support}</a>
    <a href="${targetPrefix}#download" class="pill pill-primary" style="margin-top:12px;width:100%;justify-content:center;" onclick="document.getElementById('mobileNavDrawer').classList.remove('open');">
      ${dict.nav.getApp}
    </a>
  </div>
  ` : ''}
  `;
}
