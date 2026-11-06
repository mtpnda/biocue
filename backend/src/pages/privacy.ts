// BioCue Official Privacy Policy Page
// Neurodiversity-affirming, privacy-first legal documentation.
// Compliant with Google Play Store User Data policies, GDPR, and COPPA.

import { renderLayout } from "../ui/layout.ts";
import { resolveLocale, Locale } from "../i18n/index.ts";
import { renderIcon } from "../ui/icons.ts";

export function renderPrivacyPage(requestUrl?: string, headers?: Headers): string {
  const locale = resolveLocale(requestUrl || "https://biocue.app/", headers);

  const content = `
  <div class="wrap" style="padding-top:48px;padding-bottom:80px;max-width:880px;">
    <!-- Breadcrumb -->
    <div style="margin-bottom:24px;font-size:13.5px;color:var(--text-muted);display:flex;align-items:center;gap:8px;">
      <a href="${locale === 'en' ? '/' : `/${locale}`}" style="text-decoration:underline;">Home</a>
      <span>/</span>
      <span>Legal &amp; Privacy</span>
      <span>/</span>
      <span style="color:var(--text-primary);font-weight:600;">Privacy Policy</span>
    </div>

    <!-- Header Badge -->
    <div style="margin-bottom:28px;">
      <span class="status" style="background:var(--state-calm-soft);color:var(--state-calm-ink);border-color:var(--state-calm-chip);font-size:13px;padding:4px 14px;">
        ${renderIcon("privacy-lock", 16)} Official BioCue Privacy Policy
      </span>
      <h1 style="margin-top:16px;font-size:clamp(32px, 5vw, 48px);line-height:1.15;">
        Your Biometrics Belong to You.<br/>
        Zero Surveillance. Sovereign Data.
      </h1>
      <p style="font-size:18px;color:var(--text-secondary);line-height:1.6;margin-top:16px;">
        BioCue was founded on a fundamental ethical principle: autistic individuals and neurodivergent wearers deserve predictive sensory support without sacrificing their personal privacy or living under continuous digital surveillance.
      </p>
      <div style="display:flex;flex-wrap:wrap;gap:16px;margin-top:20px;font-size:13px;color:var(--text-muted);">
        <span><strong>Effective Date:</strong> November 1, 2026</span>
        <span>•</span>
        <span><strong>Version:</strong> 2.4 (Play Store &amp; Wear OS Compliant)</span>
        <span>•</span>
        <span><strong>Scope:</strong> Wear OS Smartwatch &amp; Android Companion App</span>
      </div>
    </div>

    <!-- Core Privacy Pillars Box -->
    <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(240px, 1fr));gap:20px;margin:36px 0 48px;">
      <div class="softcard" style="padding:20px;">
        <div style="color:var(--state-calm-ink);margin-bottom:12px;">${renderIcon("shield", 28)}</div>
        <h3 style="font-size:17px;margin-bottom:6px;">On-Device Isolation</h3>
        <p style="font-size:13.5px;margin:0;">Raw PPG pulse waves, HRV windows, and accelerometer traces are stored strictly on-device in your local Room database. Never uploaded to cloud servers.</p>
      </div>

      <div class="softcard" style="padding:20px;">
        <div style="color:var(--state-alert-ink);margin-bottom:12px;">${renderIcon("sos-location", 28)}</div>
        <h3 style="font-size:17px;margin-bottom:6px;">Zero Continuous GPS</h3>
        <p style="font-size:13.5px;margin:0;">No background tracking or geofencing. GPS location is fetched on-demand strictly when an emergency SOS beacon is explicitly triggered.</p>
      </div>

      <div class="softcard" style="padding:20px;">
        <div style="color:var(--action-primary);margin-bottom:12px;">${renderIcon("pairing-link", 28)}</div>
        <h3 style="font-size:17px;margin-bottom:6px;">Instant Disconnection</h3>
        <p style="font-size:13.5px;margin:0;">Removing a caregiver severs cloud read permissions instantly and immediately purges all wearer data from the caregiver's local phone cache.</p>
      </div>
    </div>

    <hr style="border:none;border-top:1px solid var(--border-subtle);margin:40px 0;"/>

    <!-- Legal Sections -->
    <article style="display:flex;flex-direction:column;gap:36px;font-size:15.5px;line-height:1.65;color:var(--text-secondary);">
      
      <section id="section-1">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:12px;">1. Information We Collect and Process</h2>
        <p>
          BioCue collects only the minimum necessary information to predict sensory overloads and support wearer regulation:
        </p>
        <ul style="margin:12px 0 16px 24px;display:flex;flex-direction:column;gap:8px;">
          <li><strong>Physiological Sensor Telemetry (Local Only):</strong> Photoplethysmography (PPG) pulse rate intervals, calculated Heart Rate Variability (HRV RMSSD), and 3-axis accelerometer motion signatures captured by your Wear OS smartwatch.</li>
          <li><strong>Derived Event Classifications:</strong> Categorical arousal states (e.g., <em>Calm Baseline</em>, <em>Rising Tension</em>, <em>Sensory Overload Surge</em>, <em>Active Movement</em>) generated entirely on-device by our local Fusion ML scoring engine.</li>
          <li><strong>Account &amp; Pairing Data:</strong> Google Account ID or Firebase Authentication UID, user-defined display name, and temporary 6-digit numeric pairing codes.</li>
          <li><strong>Contextual User Annotations:</strong> Optional post-event journal tags entered by the wearer or caregiver (e.g., acoustic overload, crowded spaces, routine transition).</li>
          <li><strong>Emergency SOS Coordinates:</strong> Single-snapshot GPS latitude and longitude captured exclusively during an active, user-initiated or Tier 2 confirmed SOS event.</li>
        </ul>
      </section>

      <section id="section-2">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:12px;">2. Complete Local Biometric Isolation</h2>
        <p>
          Unlike conventional fitness trackers or corporate wellness platforms, <strong>BioCue never transmits raw biometric streams, raw ECG data, or high-frequency accelerometer logs to our servers or third parties.</strong>
        </p>
        <p style="margin-top:10px;">
          All physiological algorithms run in real-time within the Android Companion App's local SQLite/Room database container. Sensor windows captured on Wear OS are delivered directly to your paired phone via Google Play Services local Wearable DataLayer (Bluetooth Low Energy or local Wi-Fi peer-to-peer). When cloud sync is active, only lightweight event labels and timestamps are synchronized.
        </p>
      </section>

      <section id="section-3">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:12px;">3. Caregiver Circle Sovereignty &amp; Instant Data Purge</h2>
        <p>
          BioCue operates on a consent-first 2-in-1 account architecture. A Wearer may authorize up to two (2) trusted caregivers (such as parents, educators, or support workers) to observe their derived state:
        </p>
        <ul style="margin:12px 0 16px 24px;display:flex;flex-direction:column;gap:8px;">
          <li>Caregiver pairing requires an explicit 6-digit one-time authorization code generated on the wearer's device, valid for only 10 minutes.</li>
          <li><strong>Irrevocable Removal:</strong> The wearer retains total unilateral control to remove any caregiver at any time from the Companion App.</li>
          <li><strong>Immediate Server-Side Cutoff:</strong> The moment a wearer removes a caregiver, Firestore security rules instantly invalidate the pairing document, blocking all future read and write operations.</li>
          <li><strong>Client-Side Wipe:</strong> The caregiver's mobile app automatically purges all cached events, notes, and records relating to that wearer upon revocation.</li>
        </ul>
      </section>

      <section id="section-4">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:12px;">4. Zero Continuous GPS Tracking Guarantee</h2>
        <p>
          Continuous background GPS location tracking is an invasive practice that creates severe psychological distress and drains smartwatch battery life. <strong>BioCue has an absolute architectural prohibition against persistent background location tracking.</strong>
        </p>
        <p style="margin-top:10px;">
          GPS coordinates are retrieved solely on an on-demand basis when the wearer presses the "SOS I Need Help" button or an acute Tier 2 meltdown is confirmed. This single location fix is delivered directly to approved caregivers for emergency assistance and is stored in volatile memory only.
        </p>
      </section>

      <section id="section-5">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:12px;">5. Third-Party Services &amp; Infrastructure</h2>
        <p>
          To deliver real-time caregiver synchronization, BioCue uses industry-standard, secure infrastructure:
        </p>
        <ul style="margin:12px 0 16px 24px;display:flex;flex-direction:column;gap:8px;">
          <li><strong>Firebase Authentication &amp; Firestore (Google Cloud):</strong> For encrypted user credentials, access tokens, and event metadata. Data is encrypted in transit via TLS 1.3 and at rest via AES-256.</li>
          <li><strong>Firebase Cloud Messaging (FCM):</strong> Used to deliver low-latency meltdown alerts to authorized caregiver devices.</li>
          <li><strong>Deno Edge Hosting:</strong> Powers the BioCue public website and support portal with zero tracking cookies.</li>
        </ul>
        <p style="margin-top:10px;">
          We do not sell, rent, monetize, or disclose your personal or biometric information to any advertising brokers, data brokers, or commercial third parties.
        </p>
      </section>

      <section id="section-6">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:12px;">6. Children's Privacy (COPPA &amp; GDPR Article 8)</h2>
        <p>
          BioCue is frequently used by autistic children and adolescents under the supervision of parents and legal guardians. Because raw biometrics never leave the child's local device, and caregiver connections require explicit parental consent, BioCue fully complies with the Children's Online Privacy Protection Act (COPPA) and the General Data Protection Regulation (GDPR).
        </p>
      </section>

      <section id="section-7">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:12px;">7. Data Retention &amp; Permanent Account Deletion</h2>
        <p>
          You have the right to inspect, export, or permanently delete your account and all associated data at any time:
        </p>
        <div class="softcard-raised" style="margin:16px 0;">
          <h4 style="font-size:16px;font-weight:700;margin-bottom:8px;">How to Permanently Delete Your BioCue Account:</h4>
          <ol style="margin-left:20px;display:flex;flex-direction:column;gap:6px;font-size:14.5px;">
            <li>Open the <strong>BioCue Companion App</strong> on your Android phone.</li>
            <li>Navigate to <strong>Settings → Account &amp; Privacy → Delete Account</strong>.</li>
            <li>Confirm your identity via Google Sign-In re-authentication.</li>
            <li>All Firestore user profiles, caregiver pairings, event summaries, and cloud tokens are permanently purged within 60 seconds. Local Room databases on your phone and watch are instantly erased.</li>
          </ol>
          <p style="margin-top:12px;font-size:13.5px;color:var(--text-secondary);">
            Alternatively, you may email our privacy team directly at <a href="mailto:privacy@biocue.app" style="text-decoration:underline;color:var(--text-primary);font-weight:600;">privacy@biocue.app</a> with your account email address, and our automated compliance pipeline will complete your deletion request within 48 hours.
          </p>
        </div>
      </section>

      <section id="section-8">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:12px;">8. Contact Our Data Protection Lead</h2>
        <p>
          If you have questions, feedback, or concerns regarding your privacy rights under BioCue, please reach out to our privacy officer:
        </p>
        <p style="margin-top:8px;">
          <strong>BioCue Privacy &amp; Data Rights Team</strong><br/>
          Email: <a href="mailto:privacy@biocue.app" style="text-decoration:underline;color:var(--text-primary);">privacy@biocue.app</a><br/>
          Website: <a href="https://biocue.app/support" style="text-decoration:underline;color:var(--text-primary);">biocue.app/support</a><br/>
          Open Source Repository: <a href="https://github.com/mtpnda/biocue" target="_blank" rel="noopener" style="text-decoration:underline;color:var(--text-primary);">github.com/mtpnda/biocue</a>
        </p>
      </section>

    </article>
  </div>
  `;

  return renderLayout({
    title: "Privacy Policy — BioCue Sovereign Biometrics",
    description: "Read the BioCue Privacy Policy. Guaranteed on-device biometric isolation, zero continuous GPS tracking, and instant caregiver revocation.",
    content,
    locale,
    activeTab: "privacy",
  });
}
