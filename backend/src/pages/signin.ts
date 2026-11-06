// BioCue Sign-in & Caregiver Portal Entrance

import { renderLayout } from "../ui/layout.ts";
import { resolveLocale, Locale } from "../i18n/index.ts";
import { renderIcon } from "../ui/icons.ts";

export function renderSignInPage(requestUrl?: string, headers?: Headers): string {
  const locale = resolveLocale(requestUrl || "https://biocue.app/", headers);

  const content = `
  <div class="wrap" style="padding:80px 24px;max-width:480px;margin:0 auto;text-align:center;">
    <div style="margin-bottom:28px;">
      <div style="display:inline-flex;margin-bottom:16px;">
        <div style="width:56px;height:56px;border-radius:16px;background:var(--action-primary);color:var(--action-onPrimary);display:flex;align-items:center;justify-content:center;box-shadow:var(--shadow-btn);">
          ${renderIcon("brain", 32)}
        </div>
      </div>
      <h1 style="font-size:28px;margin-bottom:8px;">Sign in to BioCue</h1>
      <p style="font-size:15px;color:var(--text-secondary);">
        Access your Caregiver Circle, review journal reflections, or manage privacy preferences.
      </p>
    </div>

    <div class="softcard" style="padding:32px 28px;text-align:left;">
      <div style="margin-bottom:20px;">
        <label style="display:block;font-size:13.5px;font-weight:700;margin-bottom:6px;color:var(--text-primary);">
          Account Email
        </label>
        <input type="email" placeholder="alex@example.com" class="field" id="portalEmail"/>
      </div>

      <div style="margin-bottom:24px;">
        <label style="display:block;font-size:13.5px;font-weight:700;margin-bottom:6px;color:var(--text-primary);">
          Single-Use 6-Digit Pairing PIN
        </label>
        <input type="text" placeholder="e.g. 482917" maxlength="6" class="field" style="font-family:var(--font-serif);font-size:18px;letter-spacing:4px;text-align:center;"/>
        <small style="display:block;margin-top:6px;font-size:12px;color:var(--text-muted);">
          Generated in your phone Companion App under Caregiver Circle.
        </small>
      </div>

      <button type="button" class="pill pill-primary" style="width:100%;justify-content:center;margin-bottom:16px;" onclick="alert('Portal access is linked directly via your paired Android Companion App.')">
        Connect to Web Portal
      </button>

      <div style="text-align:center;border-top:1px solid var(--border-subtle);padding-top:16px;margin-top:16px;">
        <p style="font-size:12.5px;color:var(--text-muted);margin:0;">
          BioCue prioritizes passwordless biometric authentication and local device pairing for privacy.
        </p>
      </div>
    </div>

    <div style="margin-top:24px;">
      <a href="${locale === 'en' ? '/' : `/${locale}`}" style="font-size:14px;color:var(--text-secondary);text-decoration:underline;">
        ← Return to BioCue Homepage
      </a>
    </div>
  </div>
  `;

  return renderLayout({
    title: "Sign In — BioCue Portal",
    description: "Sign in to the BioCue Caregiver Circle and Wearer Portal.",
    content,
    showHeader: false,
    showFooter: true,
    locale,
    activeTab: "signin",
  });
}
