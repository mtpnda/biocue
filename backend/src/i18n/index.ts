import { Dictionary, Locale } from "./types.ts";
import { enDictionary } from "./dictionaries/en.ts";
import { idIdDictionary } from "./dictionaries/id-id.ts";

export * from "./types.ts";

export const DICTIONARIES: Record<Locale, Dictionary> = {
  "en": enDictionary,
  "id": idIdDictionary,
};

export const SUPPORTED_LOCALES: Locale[] = ["en", "id"];

export function resolveLocale(urlStr: string, headers?: Headers): Locale {
  try {
    const url = new URL(urlStr);
    const path = url.pathname.toLowerCase();

    // 1. Direct path prefix
    if (path === "/en" || path === "/en-us" || path.startsWith("/en/") || path.startsWith("/en-us/")) {
      return "en";
    }
    if (path === "/id" || path === "/id-id" || path.startsWith("/id/") || path.startsWith("/id-id/")) {
      return "id";
    }

    // 2. Cookie check
    if (headers) {
      const cookieHeader = headers.get("cookie") || "";
      const match = cookieHeader.match(/biocue_locale=([a-zA-Z-]+)/);
      if (match) {
        const val = match[1].toLowerCase();
        if (val === "id" || val === "id-id") return "id";
        if (val === "en" || val === "en-us" || val === "en-id") return "en";
      }

      // 3. Country / CDN header check
      const country = (headers.get("cf-ipcountry") || headers.get("x-country-code") || "").toUpperCase();
      if (country === "ID") {
        return "id";
      }

      // 4. Accept-Language header
      const acceptLang = (headers.get("accept-language") || "").toLowerCase();
      if (acceptLang.startsWith("id") || acceptLang.includes(",id") || acceptLang.includes("id-")) {
        return "id";
      }
    }

    return "en";
  } catch {
    return "en";
  }
}

export function getDictionary(locale: Locale): Dictionary {
  return DICTIONARIES[locale] || enDictionary;
}

export function renderLocaleSwitcher(currentLocale: Locale): string {
  const options = [
    { code: "en", label: "🇺🇸 English (US)", shortLabel: "🇺🇸 EN", href: "/en" },
    { code: "id", label: "🇮🇩 Bahasa Indonesia", shortLabel: "🇮🇩 ID", href: "/id" },
  ];

  const current = options.find((o) => o.code === currentLocale) || options[0];

  return `
    <div class="locale-switcher" style="position:relative;display:inline-block;">
      <button 
        type="button" 
        class="locale-btn"
        aria-label="Change language: English (US) or Bahasa Indonesia"
        aria-expanded="false"
        onclick="
          const m = this.nextElementSibling;
          const open = m.style.display === 'block';
          m.style.display = open ? 'none' : 'block';
          this.setAttribute('aria-expanded', !open);
        "
        style="
          display:inline-flex;
          align-items:center;
          gap:6px;
          min-height:38px;
          padding:0 12px;
          border-radius:var(--radius-pill);
          background:var(--surface-raised);
          border:1px solid var(--border-strong);
          font-size:13px;
          font-weight:600;
          color:var(--text-primary);
          cursor:pointer;
        "
      >
        <span class="locale-label-full">${current.label}</span>
        <span class="locale-label-short">${current.shortLabel}</span>
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg>
      </button>

      <div 
        class="locale-dropdown"
        style="
          display:none;
          position:absolute;
          right:0;
          top:calc(100% + 6px);
          background:var(--surface-card);
          border:1px solid var(--border-strong);
          border-radius:var(--radius-md);
          box-shadow:var(--shadow-raised);
          padding:6px;
          min-width:190px;
          z-index:200;
        "
      >
        ${options.map((opt) => `
          <a 
            href="${opt.href}" 
            onclick="document.cookie = 'biocue_locale=${opt.code};path=/;max-age=31536000'; window.location.href='${opt.href}'; return false;"
            style="
              display:flex;
              align-items:center;
              gap:8px;
              padding:8px 12px;
              border-radius:var(--radius-sm);
              font-size:13px;
              font-weight:${opt.code === currentLocale ? "700" : "500"};
              color:${opt.code === currentLocale ? "var(--action-primary)" : "var(--text-secondary)"};
              background:${opt.code === currentLocale ? "var(--surface-raised)" : "transparent"};
              text-decoration:none;
            "
          >
            ${opt.label}
          </a>
        `).join("")}
      </div>
    </div>
  `;
}
