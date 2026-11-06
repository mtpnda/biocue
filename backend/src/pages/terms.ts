// BioCue Official Terms of Service Page
// Defines user obligations, non-medical assistive cueing boundaries, and wearer sovereignty.

import { renderLayout } from "../ui/layout.ts";
import { resolveLocale, Locale } from "../i18n/index.ts";
import { renderIcon } from "../ui/icons.ts";

export function renderTermsPage(requestUrl?: string, headers?: Headers): string {
  const locale = resolveLocale(requestUrl || "https://biocue.app/", headers);

  const content = `
  <div class="wrap" style="padding-top:48px;padding-bottom:80px;max-width:880px;">
    <!-- Breadcrumb -->
    <div style="margin-bottom:24px;font-size:13.5px;color:var(--text-muted);display:flex;align-items:center;gap:8px;">
      <a href="${locale === 'en' ? '/' : `/${locale}`}" style="text-decoration:underline;">Home</a>
      <span>/</span>
      <span>Legal &amp; Privacy</span>
      <span>/</span>
      <span style="color:var(--text-primary);font-weight:600;">Terms of Service</span>
    </div>

    <!-- Header Badge -->
    <div style="margin-bottom:28px;">
      <span class="status" style="background:var(--state-info-soft);color:var(--state-info-ink);border-color:var(--state-info-chip);font-size:13px;padding:4px 14px;">
        ${renderIcon("task-demand", 16)} Official Terms of Service
      </span>
      <h1 style="margin-top:16px;font-size:clamp(32px, 5vw, 48px);line-height:1.15;">
        Terms of Service &amp; Assistive Care Agreement
      </h1>
      <p style="font-size:18px;color:var(--text-secondary);line-height:1.6;margin-top:16px;">
        Please review these terms carefully before installing or using BioCue on Wear OS, Android devices, or associated cloud synchronization services.
      </p>
      <div style="display:flex;flex-wrap:wrap;gap:16px;margin-top:20px;font-size:13px;color:var(--text-muted);">
        <span><strong>Last Updated:</strong> November 1, 2026</span>
        <span>•</span>
        <span><strong>Agreement Version:</strong> 2.1</span>
      </div>
    </div>

    <!-- Critical Non-Medical Notice Callout -->
    <div class="softcard" style="background:var(--state-alert-soft);border:1.5px solid var(--state-alert-chip);padding:24px;margin-bottom:40px;">
      <div style="display:flex;gap:14px;align-items:flex-start;">
        <div style="color:var(--state-alert-ink);flex-shrink:0;margin-top:2px;">
          ${renderIcon("loud-noises", 24)}
        </div>
        <div>
          <h3 style="font-size:18px;color:var(--state-alert-ink);margin-bottom:6px;">Important Non-Medical Assistive Cueing Disclaimer</h3>
          <p style="font-size:14.5px;color:var(--text-primary);line-height:1.6;margin:0;">
            BioCue is an <strong>assistive bio-signal cueing, wellness, and sensory awareness tool</strong>. It is <strong>NOT</strong> an FDA-cleared diagnostic medical device, medical monitor, life-support system, or clinical intervention. BioCue does not diagnose, treat, prevent, or cure any neurological, psychiatric, cardiac, or physiological medical condition. Never disregard professional medical advice or emergency medical services based on BioCue indicators.
          </p>
        </div>
      </div>
    </div>

    <hr style="border:none;border-top:1px solid var(--border-subtle);margin:40px 0;"/>

    <!-- Legal Sections -->
    <article style="display:flex;flex-direction:column;gap:36px;font-size:15.5px;line-height:1.65;color:var(--text-secondary);">

      <section id="section-1">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:12px;">1. Acceptance of Terms</h2>
        <p>
          By downloading, installing, accessing, or using BioCue on any smartwatch, tablet, phone, or web browser, you agree to be bound by these Terms of Service. If you are entering into these terms on behalf of a minor or dependent individual as a parent or legal guardian, you represent that you possess full legal authority to bind that individual to this agreement.
        </p>
      </section>

      <section id="section-2">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:12px;">2. User Sovereignty &amp; 2-in-1 Account Model</h2>
        <p>
          BioCue supports a flexible 2-in-1 account architecture allowing a user to operate as a Wearer, a Caregiver, or both simultaneously:
        </p>
        <ul style="margin:12px 0 16px 24px;display:flex;flex-direction:column;gap:8px;">
          <li><strong>Wearer Rights:</strong> The individual wearing the smartwatch is the sovereign owner of all generated bio-signal telemetry. The wearer has the sole, uninhibited right to grant, pause, or permanently revoke caregiver visibility at any time.</li>
          <li><strong>Caregiver Responsibilities:</strong> Approved caregivers agree to use shared status data strictly for supportive, grounding, and protective purposes. Caregivers agree not to use BioCue for unconsented disciplinary coercion, unlawful surveillance, or invasive tracking.</li>
          <li><strong>Pairing Limit:</strong> A single wearer may authorize up to two (2) concurrent caregivers. Additional requests will require removing an existing pairing.</li>
        </ul>
      </section>

      <section id="section-3">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:12px;">3. Emergency SOS Limitations</h2>
        <p>
          The BioCue Emergency SOS beacon feature transmits on-demand alerts and an instantaneous GPS fix to your paired caregiver circle over standard mobile networks. <strong>BioCue does not connect to 911, 112, emergency police, ambulance dispatch, or certified monitoring centers.</strong>
        </p>
        <p style="margin-top:10px;">
          Emergency SOS functionality requires active network connectivity (Wi-Fi, Bluetooth tethering, or cellular data) and hardware GPS fix acquisition. You acknowledge that physical obstacles, battery depletion, or carrier outages may prevent delivery of emergency notices.
        </p>
      </section>

      <section id="section-4">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:12px;">4. Caregiver Removal &amp; Immediate Disconnection</h2>
        <p>
          When a wearer selects "Remove Caregiver" in the Companion App:
        </p>
        <ul style="margin:12px 0 16px 24px;display:flex;flex-direction:column;gap:8px;">
          <li>All cloud permissions granted to that caregiver are revoked immediately at the database level.</li>
          <li>The caregiver’s local device cache of wearer history is immediately wiped by the mobile client.</li>
          <li>Neither party may hold BioCue liable for any lack of notifications or data availability following revocation.</li>
        </ul>
      </section>

      <section id="section-5">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:12px;">5. Intellectual Property &amp; Open Source License</h2>
        <p>
          The BioCue core client applications, detection schemas, and design token implementations are licensed under permissive open source terms (Apache 2.0 / MIT). The trademarks "BioCue", the BioCue Neural Crest emblem, and associated brand trade dress remain the exclusive property of the BioCue contributors.
        </p>
      </section>

      <section id="section-6">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:12px;">6. Disclaimer of Warranties</h2>
        <p>
          BIOCUE IS PROVIDED ON AN "AS IS" AND "AS AVAILABLE" BASIS, WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, OR UNINTERRUPTED AVAILABILITY.
        </p>
        <p style="margin-top:10px;">
          WE DO NOT WARRANT THAT BIOCUE WILL PREDICT OR DETECT 100% OF SENSORY OVERLOAD EPISODES, MELTDOWNS, PANIC ATTACKS, OR CARDIAC EVENTS, NOR THAT ALGORITHMIC CLASSIFICATIONS WILL BE FREE FROM FALSE POSITIVES OR DELAYS.
        </p>
      </section>

      <section id="section-7">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:12px;">7. Limitation of Liability</h2>
        <p>
          TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, IN NO EVENT SHALL THE BIOCUE DEVELOPERS, CONTRIBUTORS, AFFILIATES, OR LICENSORS BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL, CONSEQUENTIAL, OR EXEMPLARY DAMAGES ARISING OUT OF OR IN CONNECTION WITH YOUR USE OR INABILITY TO USE THE APP OR SERVICES.
        </p>
      </section>

      <section id="section-8">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:12px;">8. Questions and Legal Notices</h2>
        <p>
          For any questions concerning these Terms of Service or to submit formal legal inquiries, please contact:
        </p>
        <p style="margin-top:8px;">
          <strong>BioCue Legal Department</strong><br/>
          Email: <a href="mailto:legal@biocue.app" style="text-decoration:underline;color:var(--text-primary);">legal@biocue.app</a><br/>
          Support Portal: <a href="/support" style="text-decoration:underline;color:var(--text-primary);">biocue.app/support</a>
        </p>
      </section>

    </article>
  </div>
  `;

  return renderLayout({
    title: "Terms of Service — BioCue Assistive Care Agreement",
    description: "Read the BioCue Terms of Service. Non-medical assistive cueing boundaries, wearer sovereignty, caregiver pairing, and open source licensing.",
    content,
    locale,
    activeTab: "terms",
  });
}
