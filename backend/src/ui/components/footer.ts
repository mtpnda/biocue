// Footer Component for BioCue Web

import { renderLogo } from "./logo.ts";
import { Locale } from "../../i18n/index.ts";

export interface FooterOptions {
  locale?: Locale;
}

export function renderFooter(opts: FooterOptions = {}): string {
  const locale = opts.locale || "en";
  const prefix = locale === "en" ? "" : `/${locale}`;

  return `
  <footer class="site-footer">
    <div class="wrap">
      <div style="display:flex;justify-content:space-between;flex-wrap:wrap;gap:40px;margin-bottom:48px;">
        <!-- Brand Summary -->
        <div style="max-width:360px;">
          ${renderLogo({ size: 32, showTag: true })}
          <p style="margin-top:16px;font-size:14px;line-height:1.6;color:var(--text-secondary);">
            BioCue is an open-source assistive bio-signal cueing platform combining Wear OS 3+ smartwatch physiological sensing with an Android Native companion app and encrypted Caregiver Circles.
          </p>
          <div style="margin-top:16px;display:inline-flex;align-items:center;gap:8px;font-size:12.5px;color:var(--state-calm-ink);background:var(--state-calm-soft);padding:4px 12px;border-radius:var(--radius-pill);border:1px solid var(--state-calm-chip);">
            <span class="pulse-dot"></span>
            <span>100% Free &amp; Sovereign On-Device Biometrics</span>
          </div>
        </div>

        <!-- Links Columns -->
        <div style="display:flex;gap:56px;flex-wrap:wrap;">
          <div>
            <h4 style="font-family:var(--font-sans);font-size:13px;font-weight:700;text-transform:uppercase;letter-spacing:0.06em;color:var(--text-muted);margin-bottom:16px;">Product</h4>
            <ul style="list-style:none;display:flex;flex-direction:column;gap:12px;font-size:14.5px;">
              <li><a href="${prefix}#features">Features</a></li>
              <li><a href="${prefix}#how-it-works">How It Works</a></li>
              <li><a href="${prefix}#simulator">Sensory Simulator</a></li>
              <li><a href="${prefix}#faq">FAQ</a></li>
              <li><a href="${prefix}#download">Download Release</a></li>
            </ul>
          </div>

          <div>
            <h4 style="font-family:var(--font-sans);font-size:13px;font-weight:700;text-transform:uppercase;letter-spacing:0.06em;color:var(--text-muted);margin-bottom:16px;">Trust &amp; Legal</h4>
            <ul style="list-style:none;display:flex;flex-direction:column;gap:12px;font-size:14.5px;">
              <li><a href="/privacy" style="font-weight:600;">Privacy Policy</a></li>
              <li><a href="/terms" style="font-weight:600;">Terms of Service</a></li>
              <li><a href="/privacy#section-7">Data Deletion Rights</a></li>
              <li><a href="/privacy#section-3">Caregiver Removal Policy</a></li>
            </ul>
          </div>

          <div>
            <h4 style="font-family:var(--font-sans);font-size:13px;font-weight:700;text-transform:uppercase;letter-spacing:0.06em;color:var(--text-muted);margin-bottom:16px;">Support &amp; Open Source</h4>
            <ul style="list-style:none;display:flex;flex-direction:column;gap:12px;font-size:14.5px;">
              <li><a href="/support" style="font-weight:600;">Help Center &amp; Support</a></li>
              <li><a href="https://github.com/mtpnda/biocue" target="_blank" rel="noopener">GitHub Repository ↗</a></li>
              <li><a href="https://github.com/mtpnda/biocue/releases" target="_blank" rel="noopener">Releases &amp; Changelogs ↗</a></li>
              <li><a href="mailto:support@biocue.app">support@biocue.app</a></li>
            </ul>
          </div>
        </div>
      </div>

      <!-- Bottom Bar -->
      <div style="border-top:1px solid var(--border-subtle);padding-top:28px;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:16px;font-size:13px;color:var(--text-muted);">
        <div>
          © 2026 BioCue. 100% Free &amp; Open Source. Designed with neurodiversity and sovereignty at core.
        </div>
        <div style="display:flex;align-items:center;gap:16px;">
          <span>Non-diagnostic wellness &amp; assistive cueing tool</span>
          <span style="display:inline-flex;align-items:center;gap:6px;">
            <span style="width:6px;height:6px;border-radius:50%;background:var(--state-calm-chip);"></span>
            <span>Edge Deno KV Active</span>
          </span>
        </div>
      </div>
    </div>
  </footer>
  `;
}
