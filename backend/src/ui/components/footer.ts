// Footer Component for BioCue Web
// Fully localized across English (US) and Bahasa Indonesia

import { renderLogo } from "./logo.ts";
import { Locale, getDictionary } from "../../i18n/index.ts";

export interface FooterOptions {
  locale?: Locale;
}

export function renderFooter(opts: FooterOptions = {}): string {
  const locale = opts.locale || "en";
  const dict = getDictionary(locale);
  const prefix = locale === "en" ? "/en" : `/${locale}`;

  return `
  <footer class="site-footer">
    <div class="wrap">
      <div style="display:flex;justify-content:space-between;flex-wrap:wrap;gap:40px;margin-bottom:48px;">
        <!-- Brand Summary -->
        <div style="max-width:360px;">
          ${renderLogo({ size: 32, showTag: true })}
          <p style="margin-top:16px;font-size:14px;line-height:1.6;color:var(--text-secondary);">
            ${dict.footer.desc}
          </p>
          <div style="margin-top:16px;display:inline-flex;align-items:center;gap:8px;font-size:12.5px;color:var(--state-calm-ink);background:var(--state-calm-soft);padding:4px 12px;border-radius:var(--radius-pill);border:1px solid var(--state-calm-chip);">
            <span class="pulse-dot"></span>
            <span>${dict.footer.badge}</span>
          </div>
        </div>

        <!-- Links Columns -->
        <div style="display:flex;gap:56px;flex-wrap:wrap;">
          <div>
            <h4 style="font-family:var(--font-sans);font-size:13px;font-weight:700;text-transform:uppercase;letter-spacing:0.06em;color:var(--text-muted);margin-bottom:16px;">${dict.footer.colProduct}</h4>
            <ul style="list-style:none;display:flex;flex-direction:column;gap:12px;font-size:14.5px;">
              <li><a href="${prefix}#features">${dict.footer.linkFeatures}</a></li>
              <li><a href="${prefix}#how-it-works">${dict.footer.linkHowItWorks}</a></li>
              <li><a href="/whitepaper" style="font-weight:600;color:var(--text-primary);">${dict.footer.linkWhitepaper}</a></li>
              <li><a href="${prefix}#simulator">${dict.footer.linkSimulator}</a></li>
              <li><a href="${prefix}#faq">${dict.footer.linkFaq}</a></li>
              <li><a href="${prefix}#download">${dict.footer.linkDownload}</a></li>
            </ul>
          </div>

          <div>
            <h4 style="font-family:var(--font-sans);font-size:13px;font-weight:700;text-transform:uppercase;letter-spacing:0.06em;color:var(--text-muted);margin-bottom:16px;">${dict.footer.colTrust}</h4>
            <ul style="list-style:none;display:flex;flex-direction:column;gap:12px;font-size:14.5px;">
              <li><a href="/privacy" style="font-weight:600;">${dict.footer.linkPrivacy}</a></li>
              <li><a href="/terms" style="font-weight:600;">${dict.footer.linkTerms}</a></li>
              <li><a href="/privacy#section-7">${dict.footer.linkDataDeletion}</a></li>
              <li><a href="/privacy#section-3">${dict.footer.linkCaregiverPolicy}</a></li>
            </ul>
          </div>

          <div>
            <h4 style="font-family:var(--font-sans);font-size:13px;font-weight:700;text-transform:uppercase;letter-spacing:0.06em;color:var(--text-muted);margin-bottom:16px;">${dict.footer.colSupport}</h4>
            <ul style="list-style:none;display:flex;flex-direction:column;gap:12px;font-size:14.5px;">
              <li><a href="/support" style="font-weight:600;">${dict.footer.linkHelpCenter}</a></li>
              <li><a href="https://github.com/mtpnda/biocue" target="_blank" rel="noopener">${dict.footer.linkGithub}</a></li>
              <li><a href="https://github.com/mtpnda/biocue/releases" target="_blank" rel="noopener">${dict.footer.linkReleases}</a></li>
              <li><a href="mailto:support@biocue.app">support@biocue.app</a></li>
            </ul>
          </div>
        </div>
      </div>

      <!-- Bottom Bar -->
      <div style="border-top:1px solid var(--border-subtle);padding-top:28px;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:16px;font-size:13px;color:var(--text-muted);">
        <div>
          ${dict.footer.copyright}
        </div>
        <div style="display:flex;align-items:center;gap:16px;">
          <span>${dict.footer.disclaimer}</span>
          <span style="display:inline-flex;align-items:center;gap:6px;">
            <span style="width:6px;height:6px;border-radius:50%;background:var(--state-calm-chip);"></span>
            <span>${dict.footer.edgeStatus}</span>
          </span>
        </div>
      </div>
    </div>
  </footer>
  `;
}
