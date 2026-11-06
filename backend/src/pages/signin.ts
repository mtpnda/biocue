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

    <div class="softcard" style="padding:32px 28px;text-align:center;">
      <p style="font-size:14.5px;color:var(--text-secondary);margin-bottom:24px;">
        Sign in with your verified Google Account to manage Caregiver connections, review reflections, or sync mobile apps.
      </p>

      <a href="/auth/google" class="pill pill-primary" style="display:inline-flex;width:100%;justify-content:center;margin-bottom:16px;text-decoration:none;padding:14px 20px;font-size:15px;">
        <svg style="width:18px;height:18px;margin-right:10px;fill:currentColor;" viewBox="0 0 24 24">
          <path d="M12.545,10.239v3.821h5.445c-0.712,2.315-2.647,3.972-5.445,3.972c-3.332,0-6.033-2.701-6.033-6.032s2.701-6.032,6.033-6.032c1.498,0,2.866,0.549,3.921,1.453l2.814-2.814C17.503,2.988,15.139,2,12.545,2C7.021,2,2.543,6.477,2.543,12s4.478,10,10.002,10c8.396,0,10.249-7.85,9.426-11.761H12.545z"/>
        </svg>
        Continue with Google
      </a>

      <div style="text-align:center;border-top:1px solid var(--border-subtle);padding-top:16px;margin-top:16px;">
        <p style="font-size:12.5px;color:var(--text-muted);margin:0;">
          BioCue never accesses or stores your passwords. Pure sovereign token exchange powered by Deno Edge.
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
