// BioCue Official Support & Helpdesk Page
// Guides wearers and caregivers through sensor setup, pairing, and direct ticket submission.

import { renderLayout } from "../ui/layout.ts";
import { resolveLocale, Locale } from "../i18n/index.ts";
import { renderIcon } from "../ui/icons.ts";

export function renderSupportPage(requestUrl?: string, headers?: Headers): string {
  const locale = resolveLocale(requestUrl || "https://biocue.app/", headers);

  const content = `
  <div class="wrap" style="padding-top:48px;padding-bottom:80px;max-width:880px;">
    <!-- Breadcrumb -->
    <div style="margin-bottom:24px;font-size:13.5px;color:var(--text-muted);display:flex;align-items:center;gap:8px;">
      <a href="${locale === 'en' ? '/' : `/${locale}`}" style="text-decoration:underline;">Home</a>
      <span>/</span>
      <span style="color:var(--text-primary);font-weight:600;">Help &amp; Support</span>
    </div>

    <!-- Header Badge -->
    <div style="margin-bottom:32px;">
      <span class="status" style="background:var(--state-calm-soft);color:var(--state-calm-ink);border-color:var(--state-calm-chip);font-size:13px;padding:4px 14px;">
        ${renderIcon("caregiver", 16)} Community Helpdesk &amp; Knowledge Base
      </span>
      <h1 style="margin-top:16px;font-size:clamp(32px, 5vw, 48px);line-height:1.15;">
        How Can We Help You Stay Grounded?
      </h1>
      <p style="font-size:18px;color:var(--text-secondary);line-height:1.6;margin-top:16px;">
        Find step-by-step guides for Wear OS sensor pairing, caregiver circle permissions, or reach out directly to our volunteer support engineers.
      </p>
    </div>

    <!-- Quick Help Topics Grid -->
    <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(260px, 1fr));gap:20px;margin:32px 0 48px;">
      <div class="softcard">
        <div style="color:var(--action-primary);margin-bottom:12px;">${renderIcon("heart-pulse", 26)}</div>
        <h3 style="font-size:18px;margin-bottom:8px;">1. Sensor Permissions</h3>
        <p style="font-size:13.5px;line-height:1.55;color:var(--text-secondary);margin:0;">
          Ensure <code>BODY_SENSORS</code> and <code>ACTIVITY_RECOGNITION</code> permissions are granted in your watch Settings → Apps → BioCue.
        </p>
      </div>

      <div class="softcard">
        <div style="color:var(--state-info-ink);margin-bottom:12px;">${renderIcon("pairing-link", 26)}</div>
        <h3 style="font-size:18px;margin-bottom:8px;">2. Caregiver Pairing</h3>
        <p style="font-size:13.5px;line-height:1.55;color:var(--text-secondary);margin:0;">
          Generate a 6-digit PIN on the wearer's phone. The caregiver enters this code within 10 minutes to link their circle securely.
        </p>
      </div>

      <div class="softcard">
        <div style="color:var(--state-calm-ink);margin-bottom:12px;">${renderIcon("shield", 26)}</div>
        <h3 style="font-size:18px;margin-bottom:8px;">3. Battery Optimization</h3>
        <p style="font-size:13.5px;line-height:1.55;color:var(--text-secondary);margin:0;">
          Exclude the BioCue Companion app from Android aggressive battery optimization so background Bluetooth DataLayer events remain active.
        </p>
      </div>
    </div>

    <!-- Interactive Support Request Form -->
    <div class="softcard" style="padding:clamp(24px, 4vw, 40px);margin-bottom:56px;">
      <h2 style="font-size:24px;margin-bottom:8px;">Submit a Support Inquiry</h2>
      <p style="font-size:15px;color:var(--text-secondary);margin-bottom:28px;">
        Have a question or encountered a bug? Send a message to our team and we will respond within 24–48 hours.
      </p>

      <form id="supportForm" onsubmit="handleSupportSubmit(event)">
        <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(240px, 1fr));gap:20px;margin-bottom:20px;">
          <div>
            <label style="display:block;font-size:13.5px;font-weight:700;margin-bottom:6px;color:var(--text-primary);">
              Your Name
            </label>
            <input type="text" id="name" name="name" required placeholder="Alex" class="field"/>
          </div>

          <div>
            <label style="display:block;font-size:13.5px;font-weight:700;margin-bottom:6px;color:var(--text-primary);">
              Email Address
            </label>
            <input type="email" id="email" name="email" required placeholder="alex@example.com" class="field"/>
          </div>
        </div>

        <div style="margin-bottom:20px;">
          <label style="display:block;font-size:13.5px;font-weight:700;margin-bottom:6px;color:var(--text-primary);">
            Device &amp; Operating System
          </label>
          <select id="device" name="device" class="field">
            <option value="samsung-galaxy-watch">Samsung Galaxy Watch (Wear OS 3/4/5)</option>
            <option value="google-pixel-watch">Google Pixel Watch 1/2/3</option>
            <option value="ticwatch">Mobvoi TicWatch Pro 5</option>
            <option value="other-wearos">Other Wear OS 3+ Smartwatch</option>
            <option value="android-phone">Android Phone / Tablet Companion</option>
            <option value="general-question">General Question / Feedback</option>
          </select>
        </div>

        <div style="margin-bottom:24px;">
          <label style="display:block;font-size:13.5px;font-weight:700;margin-bottom:6px;color:var(--text-primary);">
            How Can We Help?
          </label>
          <textarea id="message" name="message" rows="5" required placeholder="Please describe the issue, error messages, or question in detail..." class="field" style="resize:vertical;"></textarea>
        </div>

        <button type="submit" id="submitBtn" class="pill pill-primary" style="width:100%;justify-content:center;">
          ${renderIcon("telemetry-sync", 18)} Send Support Request
        </button>

        <div id="formFeedback" style="display:none;margin-top:20px;padding:16px;border-radius:var(--radius-md);font-size:14.5px;"></div>
      </form>
    </div>

    <!-- Direct Contact Alternatives -->
    <div class="softcard-raised" style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:20px;">
      <div>
        <h3 style="font-size:18px;margin-bottom:4px;">Prefer GitHub or Direct Email?</h3>
        <p style="font-size:14px;color:var(--text-secondary);margin:0;">
          You can also file an issue on GitHub or email our developer team directly.
        </p>
      </div>
      <div style="display:flex;gap:12px;flex-wrap:wrap;">
        <a href="https://github.com/mtpnda/biocue/issues" target="_blank" rel="noopener" class="pill pill-outline pill-sm">
          GitHub Issues ↗
        </a>
        <a href="mailto:support@biocue.app" class="pill pill-primary pill-sm">
          support@biocue.app
        </a>
      </div>
    </div>
  </div>

  <script>
    async function handleSupportSubmit(e) {
      e.preventDefault();
      const btn = document.getElementById('submitBtn');
      const feedback = document.getElementById('formFeedback');
      btn.disabled = true;
      btn.innerText = 'Submitting inquiry...';

      const payload = {
        name: document.getElementById('name').value,
        email: document.getElementById('email').value,
        device: document.getElementById('device').value,
        message: document.getElementById('message').value,
      };

      try {
        const res = await fetch('/api/support', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });

        if (res.ok) {
          feedback.style.display = 'block';
          feedback.style.background = 'var(--state-calm-soft)';
          feedback.style.color = 'var(--state-calm-ink)';
          feedback.style.border = '1px solid var(--state-calm-chip)';
          feedback.innerText = '✓ Thank you! Your support ticket has been received. Our team will get back to you shortly.';
          document.getElementById('supportForm').reset();
        } else {
          throw new Error('Server returned error status');
        }
      } catch (err) {
        feedback.style.display = 'block';
        feedback.style.background = 'var(--state-alert-soft)';
        feedback.style.color = 'var(--state-alert-ink)';
        feedback.style.border = '1px solid var(--state-alert-chip)';
        feedback.innerText = 'Unable to send right now. Please email us directly at support@biocue.app.';
      } finally {
        btn.disabled = false;
        btn.innerText = 'Send Support Request';
      }
    }
  </script>
  `;

  return renderLayout({
    title: "Help & Support — BioCue Knowledge Base",
    description: "Get help with BioCue Wear OS pairing, sensor permissions, caregiver circles, and troubleshooting.",
    content,
    locale,
    activeTab: "support",
  });
}
