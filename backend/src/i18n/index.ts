import { Dictionary, Locale } from "./types.ts";
import { enDictionary } from "./dictionaries/en.ts";
import { enIdDictionary } from "./dictionaries/en-id.ts";
import { idIdDictionary } from "./dictionaries/id-id.ts";

export * from "./types.ts";

export const DICTIONARIES: Record<Locale, Dictionary> = {
  "en": enDictionary,
  "en-id": enIdDictionary,
  "id-id": idIdDictionary,
};

export const SUPPORTED_LOCALES: Locale[] = ["en", "en-id", "id-id"];

export function resolveLocale(urlStr: string, headers?: Headers): Locale {
  try {
    const url = new URL(urlStr);
    const path = url.pathname.toLowerCase();

    // 1. Direct path prefix
    if (path === "/en" || path.startsWith("/en/")) {
      return "en";
    }
    if (path === "/en-id" || path.startsWith("/en-id/")) {
      return "en-id";
    }
    if (path === "/id" || path === "/id-id" || path.startsWith("/id-id/") || path.startsWith("/id/")) {
      return "id-id";
    }

    // 2. Cookie check
    if (headers) {
      const cookieHeader = headers.get("cookie") || "";
      const match = cookieHeader.match(/biocue_locale=([a-zA-Z-]+)/);
      if (match && SUPPORTED_LOCALES.includes(match[1] as Locale)) {
        return match[1] as Locale;
      }

      // 3. Country / CDN header check
      const country = (headers.get("cf-ipcountry") || headers.get("x-country-code") || "").toUpperCase();
      if (country === "ID") {
        const acceptLang = (headers.get("accept-language") || "").toLowerCase();
        if (acceptLang.includes("id")) {
          return "id-id";
        }
        return "en-id";
      }

      // 4. Accept-Language header
      const acceptLang = (headers.get("accept-language") || "").toLowerCase();
      if (acceptLang.startsWith("id")) {
        return "id-id";
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
    { code: "en", label: "🌐 Global (USD $)", href: "/en" },
    { code: "en-id", label: "🇮🇩 Indonesia (Rp / EN)", href: "/en-id" },
    { code: "id-id", label: "🇮🇩 Bahasa Indonesia (Rp)", href: "/id-id" },
  ];

  const current = options.find((o) => o.code === currentLocale) || options[0];

  return `
    <div class="locale-switcher" style="position:relative;display:inline-block;">
      <button 
        type="button" 
        class="locale-btn"
        aria-label="Change language or region"
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
        <span>${current.label}</span>
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
          min-width:210px;
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
