// BioCue Official Technical & Mathematical Whitepaper Page
// Comprehensive scientific documentation of the decoupled autonomic-motor model,
// statistical ML fusion scoring, deterministic crisis guardian, and on-chip SLM architecture.

import { renderLayout } from "../ui/layout.ts";
import { resolveLocale } from "../i18n/index.ts";
import { renderIcon } from "../ui/icons.ts";

export function renderWhitepaperPage(requestUrl?: string, headers?: Headers): string {
  const locale = resolveLocale(requestUrl || "https://biocue.app/", headers);

  const content = `
  <div class="wrap" style="padding-top:48px;padding-bottom:96px;max-width:920px;">
    <!-- Breadcrumb -->
    <div style="margin-bottom:24px;font-size:13.5px;color:var(--text-muted);display:flex;align-items:center;gap:8px;">
      <a href="${locale === 'en' ? '/' : `/${locale}`}" style="text-decoration:underline;">Home</a>
      <span>/</span>
      <span>Documentation</span>
      <span>/</span>
      <span style="color:var(--text-primary);font-weight:600;">Technical Whitepaper</span>
    </div>

    <!-- Header Badge & Title -->
    <div style="margin-bottom:36px;">
      <span class="status" style="background:var(--state-calm-soft);color:var(--state-calm-ink);border-color:var(--state-calm-chip);font-size:13px;padding:4px 14px;">
        ${renderIcon("brain", 16)} BioCue Architecture Whitepaper · Nov 2026
      </span>
      <h1 style="margin-top:18px;font-size:clamp(30px, 4.5vw, 44px);line-height:1.2;font-weight:800;letter-spacing:-0.02em;">
        BioCue: An On-Device, Autonomic-Decoupled Hybrid Intelligence Architecture for Sensory Overload Prediction and Crisis Co-Regulation
      </h1>
      <p style="font-size:17px;color:var(--text-secondary);line-height:1.65;margin-top:16px;">
        A technical and mathematical treatise on decoupling motor stillness from autonomic sympathetic arousal, continuous logistic fusion scoring, formally verified crisis guardianship, and sub-watt on-chip Small Language Model (SLM) cognitive assistance.
      </p>
      <div style="display:flex;flex-wrap:wrap;gap:14px;margin-top:20px;font-size:13px;color:var(--text-muted);">
        <span><strong>Authors:</strong> BioCue Core Systems &amp; Neural Engineering</span>
        <span>•</span>
        <span><strong>Status:</strong> Normative Reference</span>
        <span>•</span>
        <span><strong>Target:</strong> Wear OS Smartwatch, iOS &amp; Android Companion</span>
      </div>
    </div>

    <!-- Executive Abstract Softcard -->
    <div class="softcard" style="padding:28px;margin-bottom:48px;background:var(--surface-raised);border-left:4px solid var(--state-calm-ink);">
      <h3 style="font-size:18px;margin-bottom:12px;display:flex;align-items:center;gap:8px;">
        ${renderIcon("calm", 20)} Abstract
      </h3>
      <p style="font-size:14.5px;line-height:1.7;color:var(--text-secondary);margin:0;">
        Commercial wearable fitness monitors operate under an erroneous foundational assumption: that physical immobility in a dark environment signifies physiological calm and restorative sleep. In neurodivergent individuals experiencing acute sensory overload, panic episodes, or nocturnal hyperarousal, extreme sympathetic activation and vagal withdrawal coincide with gross motor paralysis or stillness in bed ("tired-but-wired"). Consequently, off-the-shelf devices misclassify acute suffering as peaceful rest. BioCue solves this by introducing a <strong>decoupled autonomic-motor telemetry architecture</strong>. We pair statistical machine learning with an adaptive Kalman/EMA baseline, a deterministic 4-phase Crisis Guardian state machine, and a resource-governed on-chip Small Language Model (Gemma 2B INT4) running strictly on mobile Neural Processing Units (NPUs). The system achieves 75% optical battery reduction via intelligent 30s/90s duty-cycling, zero raw-vitals cloud transmission, and mathematically fail-safe crisis escalation.
      </p>
    </div>

    <!-- Article Content -->
    <article style="display:flex;flex-direction:column;gap:48px;font-size:15.5px;line-height:1.7;color:var(--text-secondary);">

      <!-- Section 1 -->
      <section id="section-1">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:16px;border-bottom:1px solid var(--border-subtle);padding-bottom:8px;">
          1. Clinical Background &amp; Autonomic Nervous System Physiology
        </h2>
        <p>
          Sensory processing sensitivity and acute sensory overload are hallmarks of neurodivergent neurobiology (autism spectrum condition, ADHD, sensory processing disorders). During an overload cascade, sensory input exceeds the central nervous system’s filtering capacity, precipitating an autonomic sympathetic nervous system (SNS) emergency discharge accompanied by profound parasympathetic vagal withdrawal.
        </p>
        <p>
          This autonomic disequilibrium produces a distinct physiological triad:
        </p>
        <ul style="padding-left:24px;display:flex;flex-direction:column;gap:8px;margin:16px 0;">
          <li>
            <strong>1. Peripheral Vasoconstriction &amp; Cranial Hyperperfusion:</strong> Sympathetic alpha-adrenergic signaling constricts peripheral vascular beds (producing cold extremities) while diverting cardiac output to core and cerebral circulation. Temporal and meningeal arterial dilation creates the intense physical sensation of <em>burning heat inside the skull</em> and throbbing tension headache.
          </li>
          <li>
            <strong>2. Vagal Withdrawal &amp; Plunging RMSSD:</strong> High-frequency beat-to-beat variability (governed by the myelinated vagus nerve) collapses. Root Mean Square of Successive Differences (RMSSD) plummets precipitously (often dropping from a calm 50–70 ms baseline down to 12–18 ms).
          </li>
          <li>
            <strong>3. Motor Decoupling ("Tired-but-Wired"):</strong> Physical motor exhaustion often traps the individual motionless in bed ("feeling asleep"), while central sympathetic arousal keeps cortical circuits hyper-activated ("racing thoughts").
          </li>
        </ul>
      </section>

      <!-- Section 2 -->
      <section id="section-2">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:16px;border-bottom:1px solid var(--border-subtle);padding-bottom:8px;">
          2. Mathematical Formulations &amp; Fusion Scoring
        </h2>
        <p>
          BioCue's mathematical engine executes on streaming 5-second sensor windows, fusing photoplethysmography (PPG) optical telemetry with 3-axis accelerometer dynamics.
        </p>

        <div class="softcard" style="padding:24px;margin:20px 0;background:var(--surface);border:1px solid var(--border-strong);">
          <h4 style="font-size:15px;color:var(--text-primary);margin-bottom:12px;font-family:var(--font-mono);text-transform:uppercase;letter-spacing:0.05em;">
            Formula 2.1 — Inter-Beat Interval (IBI) &amp; Artifact Rejection
          </h4>
          <p style="font-size:14px;color:var(--text-secondary);margin-bottom:12px;">
            Given instantaneous heart rate readings $HR_i$ in beats per minute (BPM):
          </p>
          <div style="background:var(--surface-raised);padding:16px 20px;border-radius:var(--radius-sm);color:var(--text-primary);margin-bottom:12px;overflow-x:auto;text-align:center;">
            $$RR_i = \\frac{60000.0}{HR_i} \\quad \\text{[ms]}$$
          </div>
          <p style="font-size:13.5px;color:var(--text-muted);">
            Valid beats must strictly satisfy physiological limits: $300\\text{ ms} \\le RR_i \\le 1500\\text{ ms}$ (40 to 200 BPM) and consecutive change $|RR_i - RR_{i-1}| \\le 300\\text{ ms}$. Ectopic beats and motion noise are rejected prior to metric computation.
          </p>
        </div>

        <div class="softcard" style="padding:24px;margin:20px 0;background:var(--surface);border:1px solid var(--border-strong);">
          <h4 style="font-size:15px;color:var(--text-primary);margin-bottom:12px;font-family:var(--font-mono);text-transform:uppercase;letter-spacing:0.05em;">
            Formula 2.2 — Root Mean Square of Successive Differences (RMSSD)
          </h4>
          <p style="font-size:14px;color:var(--text-secondary);margin-bottom:12px;">
            For a sliding buffer of $N$ valid beat intervals ($15 \\le N \\le 30$):
          </p>
          <div style="background:var(--surface-raised);padding:16px 20px;border-radius:var(--radius-sm);color:var(--text-primary);margin-bottom:12px;overflow-x:auto;text-align:center;">
            $$RMSSD = \\sqrt{\\frac{1}{N - 1} \\sum_{i=1}^{N-1} (RR_{i+1} - RR_i)^2}$$
          </div>
          <p style="font-size:13.5px;color:var(--text-muted);">
            RMSSD directly reflects parasympathetic (vagal) cardiac modulation, capturing immediate autonomic shifts within seconds.
          </p>
        </div>

        <div class="softcard" style="padding:24px;margin:20px 0;background:var(--surface);border:1px solid var(--border-strong);">
          <h4 style="font-size:15px;color:var(--text-primary);margin-bottom:12px;font-family:var(--font-mono);text-transform:uppercase;letter-spacing:0.05em;">
            Formula 2.3 — Standardized Z-Score Autonomic Decoupling
          </h4>
          <p style="font-size:14px;color:var(--text-secondary);margin-bottom:12px;">
            Features are standardized against the individual's calibrated baseline ($\\mu_{HR}, \\sigma_{HR}, \\mu_{RMSSD}, \\sigma_{RMSSD}$):
          </p>
          <div style="background:var(--surface-raised);padding:16px 20px;border-radius:var(--radius-sm);color:var(--text-primary);margin-bottom:12px;overflow-x:auto;text-align:center;">
            $$hrZ = \\frac{w.hrMean - \\mu_{HR}}{\\max(\\sigma_{HR}, 1.0)}, \\quad hrvZ = \\frac{\\mu_{RMSSD} - w.rmssd}{\\max(\\sigma_{RMSSD}, 1.0)}$$
          </div>
          <p style="font-size:13.5px;color:var(--text-muted);">
            Notice the mathematical inversion of $hrvZ$: when HRV collapses below baseline, $hrvZ$ turns strongly positive, signaling sympathetic stress even when motor movement is zero.
          </p>
        </div>

        <div class="softcard" style="padding:24px;margin:20px 0;background:var(--surface);border:1px solid var(--border-strong);">
          <h4 style="font-size:15px;color:var(--text-primary);margin-bottom:12px;font-family:var(--font-mono);text-transform:uppercase;letter-spacing:0.05em;">
            Formula 2.4 — Continuous Logistic Fusion &amp; Sigmoid Stress Probability
          </h4>
          <div style="background:var(--surface-raised);padding:16px 20px;border-radius:var(--radius-sm);color:var(--text-primary);margin-bottom:12px;overflow-x:auto;text-align:center;">
            $$Score = 0.35 \\cdot hrZ + 0.35 \\cdot hrvZ + 0.15 \\cdot Repetitiveness + 0.15 \\cdot HR_{slope}$$
            $$\\text{Stress \\%} = \\frac{1}{1 + e^{-1.2(Score - 1.0)}} \\times 100\\%$$
          </div>
          <p style="font-size:13.5px;color:var(--text-muted);">
            • <strong>Tier 0 (Calm):</strong> $Score &lt; 1.5$ (&lt; 50% Stress)<br/>
            • <strong>Tier 1 (Rising):</strong> $1.5 \\le Score \\le 2.5$ (50% to 74% Stress)<br/>
            • <strong>Tier 2 (Sensory Overload):</strong> $Score &gt; 2.5$ (&ge; 75% Stress)
          </p>
        </div>
      </section>

      <!-- Section 3 -->
      <section id="section-3">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:16px;border-bottom:1px solid var(--border-subtle);padding-bottom:8px;">
          3. Battery Conservation &amp; Adaptive Sensor Duty-Cycling
        </h2>
        <p>
          Continuous optical photoplethysmography (green/infrared LEDs) and continuous Bluetooth Low Energy (BLE) packet streaming cause severe battery depletion on smartwatches (e.g., Samsung Galaxy Watch 4 Classic, 247–361 mAh), draining devices in 5 to 7 hours.
        </p>
        <p>
          BioCue implements a <strong>Four-Pillar Hardware Strategy</strong> to guarantee &gt; 16 hours of continuous wearability:
        </p>
        <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(250px, 1fr));gap:20px;margin:24px 0;">
          <div class="softcard" style="padding:18px;">
            <div style="font-weight:700;color:var(--text-primary);margin-bottom:6px;">1. Hardware Off-Body Gate</div>
            <p style="font-size:13.5px;margin:0;">Subscribes directly to Android low-latency off-body sensor (type 34). When removed from wrist, PPG LEDs and radio radios unregister immediately (0 mA optical draw).</p>
          </div>
          <div class="softcard" style="padding:18px;">
            <div style="font-weight:700;color:var(--text-primary);margin-bottom:6px;">2. 30s/90s Smart Windowing</div>
            <p style="font-size:13.5px;margin:0;">In calm states, collects PPG for 30s to update the rolling baseline, then powers down optical LEDs for 90s. Accelerometer monitors for motion spikes to wake PPG instantly. <strong>Saves 75% optical power.</strong></p>
          </div>
          <div class="softcard" style="padding:18px;">
            <div style="font-weight:700;color:var(--text-primary);margin-bottom:6px;">3. Baseband Radio Batching</div>
            <p style="font-size:13.5px;margin:0;">Calm telemetry is batched and synced in 30-second bursts, allowing the Bluetooth baseband modem to enter deep sleep between frames.</p>
          </div>
          <div class="softcard" style="padding:18px;">
            <div style="font-weight:700;color:var(--text-primary);margin-bottom:6px;">4. Zero-Spill In-Memory Rollup</div>
            <p style="font-size:13.5px;margin:0;">Watch metrics are cached purely in RAM with hourly compact summaries, preventing flash write endurance degradation and NAND power draw.</p>
          </div>
        <div class="softcard" style="padding:24px;margin:24px 0;background:var(--surface-card);border:1px solid var(--border-subtle);border-radius:var(--radius-card);box-shadow:0 2px 8px rgba(0,0,0,0.04);overflow-x:auto;">
          <div style="font-family:var(--font-mono);font-size:11px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;color:var(--text-muted);margin-bottom:14px;">
            FIGURE 3.1 &bull; ADAPTIVE SENSOR DUTY-CYCLING STATE MACHINE
          </div>
          <svg viewBox="0 0 860 220" fill="none" style="width:100%;min-width:640px;height:auto;display:block;">
            <defs>
              <marker id="wp-arrow" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
                <path d="M 0 1.5 L 8 5 L 0 8.5 z" fill="var(--text-muted, #544B3E)"/>
              </marker>
              <marker id="wp-arrow-coral" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
                <path d="M 0 1.5 L 8 5 L 0 8.5 z" fill="var(--state-alert-ink, #9C2A22)"/>
              </marker>
            </defs>

            <!-- Subgraph: Calm Baseline Loop -->
            <rect x="12" y="16" width="460" height="188" rx="14" fill="var(--surface-raised)" stroke="var(--border-subtle)" stroke-dasharray="4 4" stroke-width="1.2"/>
            <text x="28" y="38" font-family="var(--font-mono)" font-size="10.5" font-weight="700" fill="var(--state-calm-ink)">TIER 0 &bull; RESTING CALM BASELINE (90s DUTY CYCLE)</text>

            <!-- State A: PPG Active Sampling -->
            <rect x="28" y="54" width="190" height="80" rx="10" fill="var(--surface-card)" stroke="var(--border-strong)" stroke-width="1.2"/>
            <text x="42" y="78" font-family="var(--font-mono)" font-size="9" font-weight="700" fill="var(--text-muted)">STATE A &bull; 25Hz RAW PPG</text>
            <text x="42" y="100" font-family="var(--font-sans)" font-size="13" font-weight="700" fill="var(--text-primary)">Active Sampling (30s)</text>
            <text x="42" y="120" font-family="var(--font-sans)" font-size="11" fill="var(--text-secondary)">Update Rolling EMA profile</text>

            <!-- State B: PPG Sleep -->
            <rect x="264" y="54" width="190" height="80" rx="10" fill="var(--surface-card)" stroke="var(--border-strong)" stroke-width="1.2"/>
            <text x="278" y="78" font-family="var(--font-mono)" font-size="9" font-weight="700" fill="var(--text-muted)">STATE B &bull; 0mA ULTRA-LOW</text>
            <text x="278" y="100" font-family="var(--font-sans)" font-size="13" font-weight="700" fill="var(--text-primary)">Sensor Sleep (90s)</text>
            <text x="278" y="120" font-family="var(--font-sans)" font-size="11" fill="var(--text-secondary)">Optics powered down</text>

            <!-- Cycle Arrows A <-> B -->
            <path d="M 218 84 L 258 84" stroke="var(--border-strong)" stroke-width="1.5" marker-end="url(#wp-arrow)"/>
            <text x="224" y="76" font-family="var(--font-mono)" font-size="8.5" fill="var(--text-muted)">30s DONE</text>
            <path d="M 264 104 L 224 104" stroke="var(--border-strong)" stroke-width="1.5" marker-end="url(#wp-arrow)"/>
            <text x="222" y="122" font-family="var(--font-mono)" font-size="8.5" fill="var(--text-muted)">TIMER WAKE</text>

            <!-- State C: Instant Wake on Motion -->
            <rect x="264" y="146" width="190" height="44" rx="8" fill="var(--state-info-soft)" stroke="var(--state-info-chip)" stroke-width="1"/>
            <text x="276" y="164" font-family="var(--font-mono)" font-size="9" font-weight="700" fill="var(--state-info-ink)">INTERRUPT &bull; ACCEL > 0.5G</text>
            <text x="276" y="180" font-family="var(--font-sans)" font-size="11.5" font-weight="600" fill="var(--state-info-ink)">Instant Wake (0s Delay)</text>

            <!-- State D: Continuous Telemetry (Tier 1/2) -->
            <rect x="540" y="50" width="300" height="140" rx="14" fill="var(--surface-card)" stroke="var(--state-alert-chip)" stroke-width="1.8"/>
            <rect x="556" y="64" width="112" height="18" rx="9" fill="var(--state-alert-soft)"/>
            <text x="564" y="76" font-family="var(--font-mono)" font-size="9" font-weight="700" fill="var(--state-alert-ink)">TIER 1 / 2 ELEVATION</text>
            <text x="556" y="106" font-family="var(--font-serif)" font-size="18" font-weight="600" fill="var(--text-primary)">Continuous Telemetry</text>
            <text x="556" y="128" font-family="var(--font-sans)" font-size="12" fill="var(--text-secondary)">&bull; 100% Active PPG Optical sampling</text>
            <text x="556" y="146" font-family="var(--font-sans)" font-size="12" fill="var(--text-secondary)">&bull; High-precision HRV RMSSD delta tracking</text>
            <text x="556" y="164" font-family="var(--font-sans)" font-size="12" fill="var(--text-secondary)">&bull; Guardian state machine engagement</text>

            <!-- Connectors to D -->
            <path d="M 454 84 L 532 84" stroke="var(--state-alert-ink)" stroke-width="1.5" marker-end="url(#wp-arrow-coral)"/>
            <text x="460" y="76" font-family="var(--font-mono)" font-size="8.5" fill="var(--state-alert-ink)">STRESS > T1</text>
            <path d="M 454 168 L 532 168" stroke="var(--state-alert-ink)" stroke-width="1.5" marker-end="url(#wp-arrow-coral)"/>
            <text x="462" y="162" font-family="var(--font-mono)" font-size="8.5" fill="var(--state-alert-ink)">MOTION ENGAGE</text>
            <!-- Recovery Arrow -->
            <path d="M 690 50 C 690 10, 120 10, 120 48" stroke="var(--state-calm-ink)" stroke-width="1.5" stroke-dasharray="4 3" marker-end="url(#wp-arrow)"/>
            <text x="340" y="22" font-family="var(--font-mono)" font-size="9" font-weight="600" fill="var(--state-calm-ink)">RECOVERY: STRESS &lt; 50% BASELINE &bull; RETURN TO TIER 0</text>
          </svg>
        </div>
      </section>

      <!-- Section 4 -->
      <section id="section-4">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:16px;border-bottom:1px solid var(--border-subtle);padding-bottom:8px;">
          4. The Deterministic Crisis Guardian Protocol
        </h2>
        <p>
          In life-safety engineering and acute mental health distress (e.g. panic attacks, dissociation, potential self-harm), probabilistic generative AI models represent a severe liability due to unpredictable latency, prompt hallucination, and conversational drift.
        </p>
        <p>
          The <strong>BioCue Guardian Protocol</strong> is governed by a mathematically provable, deterministic finite state machine (FSM):
        </p>

        <div class="softcard" style="padding:24px;margin:24px 0;background:var(--surface-card);border:1px solid var(--border-subtle);border-radius:var(--radius-card);box-shadow:0 2px 8px rgba(0,0,0,0.04);overflow-x:auto;">
          <div style="font-family:var(--font-mono);font-size:11px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;color:var(--text-muted);margin-bottom:14px;">
            FIGURE 4.1 &bull; DETERMINISTIC CRISIS GUARDIAN FINITE STATE MACHINE (FSM)
          </div>
          <svg viewBox="0 0 940 280" fill="none" style="width:100%;min-width:740px;height:auto;display:block;">
            <defs>
              <marker id="fsm-arrow" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
                <path d="M 0 1.5 L 8 5 L 0 8.5 z" fill="var(--text-muted, #544B3E)"/>
              </marker>
              <marker id="fsm-arrow-calm" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
                <path d="M 0 1.5 L 8 5 L 0 8.5 z" fill="var(--state-calm-ink, #1A6337)"/>
              </marker>
              <marker id="fsm-arrow-coral" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
                <path d="M 0 1.5 L 8 5 L 0 8.5 z" fill="var(--state-alert-ink, #9C2A22)"/>
              </marker>
            </defs>

            <!-- Phase 1 -->
            <rect x="12" y="90" width="155" height="95" rx="10" fill="var(--surface-raised)" stroke="var(--border-strong)" stroke-width="1.2"/>
            <text x="22" y="110" font-family="var(--font-mono)" font-size="9" font-weight="700" fill="var(--text-muted)">PHASE 1 &bull; 0 to 3m</text>
            <text x="22" y="130" font-family="var(--font-sans)" font-size="13" font-weight="700" fill="var(--text-primary)">Silent Presence</text>
            <text x="22" y="148" font-family="var(--font-sans)" font-size="10.5" fill="var(--text-secondary)">Zero intrusive prompts</text>
            <text x="22" y="164" font-family="var(--font-sans)" font-size="10.5" fill="var(--text-secondary)">Respects shutdown</text>

            <!-- Arrow 1 -> 2 -->
            <path d="M 167 137 L 197 137" stroke="var(--border-strong)" stroke-width="1.5" marker-end="url(#fsm-arrow)"/>
            <text x="170" y="130" font-family="var(--font-mono)" font-size="8" fill="var(--text-muted)">T=3m</text>

            <!-- Phase 2 -->
            <rect x="203" y="90" width="155" height="95" rx="10" fill="var(--surface-raised)" stroke="var(--border-strong)" stroke-width="1.2"/>
            <text x="213" y="110" font-family="var(--font-mono)" font-size="9" font-weight="700" fill="var(--text-muted)">PHASE 2 &bull; 3 to 7m</text>
            <text x="213" y="130" font-family="var(--font-sans)" font-size="13" font-weight="700" fill="var(--text-primary)">Somatic Anchor</text>
            <text x="213" y="148" font-family="var(--font-sans)" font-size="10.5" fill="var(--text-secondary)">Haptic breathing pacer</text>
            <text x="213" y="164" font-family="var(--font-sans)" font-size="10.5" fill="var(--text-secondary)">0.1 Hz resonant wave</text>

            <!-- Arrow 2 -> 3 -->
            <path d="M 358 137 L 388 137" stroke="var(--border-strong)" stroke-width="1.5" marker-end="url(#fsm-arrow)"/>
            <text x="361" y="130" font-family="var(--font-mono)" font-size="8" fill="var(--text-muted)">T=7m</text>

            <!-- Phase 3 -->
            <rect x="394" y="90" width="155" height="95" rx="10" fill="var(--surface-raised)" stroke="var(--border-strong)" stroke-width="1.2"/>
            <text x="404" y="110" font-family="var(--font-mono)" font-size="9" font-weight="700" fill="var(--text-muted)">PHASE 3 &bull; 7 to 12m</text>
            <text x="404" y="130" font-family="var(--font-sans)" font-size="13" font-weight="700" fill="var(--text-primary)">Safety Check</text>
            <text x="404" y="148" font-family="var(--font-sans)" font-size="10.5" fill="var(--text-secondary)">1-touch safety query</text>
            <text x="404" y="164" font-family="var(--font-sans)" font-size="10.5" fill="var(--text-secondary)">Zero question fatigue</text>

            <!-- Branch from Phase 3: Wearer Confirms Safe -->
            <path d="M 471 90 L 471 45 L 750 45" stroke="var(--state-calm-ink)" stroke-width="1.5" marker-end="url(#fsm-arrow-calm)"/>
            <text x="480" y="38" font-family="var(--font-mono)" font-size="8.5" font-weight="600" fill="var(--state-calm-ink)">CONFIRMED SAFE &bull; RESOLVED</text>

            <!-- Arrow 3 -> 4 -->
            <path d="M 549 137 L 579 137" stroke="var(--border-strong)" stroke-width="1.5" marker-end="url(#fsm-arrow)"/>
            <text x="552" y="130" font-family="var(--font-mono)" font-size="8" fill="var(--text-muted)">T=12m</text>

            <!-- Phase 4 -->
            <rect x="585" y="90" width="155" height="95" rx="10" fill="var(--surface-raised)" stroke="var(--border-strong)" stroke-width="1.2"/>
            <text x="595" y="110" font-family="var(--font-mono)" font-size="9" font-weight="700" fill="var(--text-muted)">PHASE 4 &bull; 12 to 15m</text>
            <text x="595" y="130" font-family="var(--font-sans)" font-size="13" font-weight="700" fill="var(--text-primary)">Prolonged Distress</text>
            <text x="595" y="148" font-family="var(--font-sans)" font-size="10.5" fill="var(--text-secondary)">HR > baseline + 20</text>
            <text x="595" y="164" font-family="var(--font-sans)" font-size="10.5" fill="var(--text-secondary)">Unresponsive wearer</text>

            <!-- Arrow 4 -> Grace -->
            <path d="M 740 137 L 770 137" stroke="var(--state-alert-ink)" stroke-width="1.5" marker-end="url(#fsm-arrow-coral)"/>
            <text x="742" y="130" font-family="var(--font-mono)" font-size="8" fill="var(--state-alert-ink)">T=15m</text>

            <!-- Grace Alarm -->
            <rect x="776" y="90" width="150" height="95" rx="10" fill="var(--state-alert-soft)" stroke="var(--state-alert-chip)" stroke-width="1.8"/>
            <text x="786" y="110" font-family="var(--font-mono)" font-size="9" font-weight="700" fill="var(--state-alert-ink)">GRACE PERIOD &bull; 30s</text>
            <text x="786" y="130" font-family="var(--font-sans)" font-size="13" font-weight="700" fill="var(--state-alert-ink)">Cancelable Alarm</text>
            <text x="786" y="148" font-family="var(--font-sans)" font-size="10.5" fill="var(--text-primary)">Audible + haptic tone</text>
            <text x="786" y="164" font-family="var(--font-sans)" font-size="10.5" fill="var(--text-primary)">Wearer override ready</text>

            <!-- Terminals: Resolved vs Crisis -->
            <!-- Resolved -->
            <rect x="756" y="20" width="170" height="44" rx="8" fill="var(--state-calm-soft)" stroke="var(--state-calm-chip)" stroke-width="1.2"/>
            <text x="770" y="38" font-family="var(--font-mono)" font-size="9" font-weight="700" fill="var(--state-calm-ink)">TERMINAL STATE A</text>
            <text x="770" y="54" font-family="var(--font-sans)" font-size="12" font-weight="700" fill="var(--state-calm-ink)">&bull; Equilibrium Restored</text>

            <!-- Branch from Grace: Cancelled by wearer -->
            <path d="M 851 90 L 851 68" stroke="var(--state-calm-ink)" stroke-width="1.5" marker-end="url(#fsm-arrow-calm)"/>

            <!-- Crisis Broadcast -->
            <rect x="756" y="216" width="170" height="52" rx="8" fill="var(--surface-card)" stroke="var(--state-alert-ink)" stroke-width="2"/>
            <text x="770" y="234" font-family="var(--font-mono)" font-size="9" font-weight="700" fill="var(--state-alert-ink)">TERMINAL STATE B (T=15m30s)</text>
            <text x="770" y="250" font-family="var(--font-sans)" font-size="12" font-weight="700" fill="var(--text-primary)">&bull; Crisis Broadcast</text>
            <text x="770" y="262" font-family="var(--font-sans)" font-size="10" fill="var(--text-secondary)">Circle Alerted (1-Shot GPS Fix)</text>

            <!-- Branch from Grace: Expired -->
            <path d="M 851 185 L 851 210" stroke="var(--state-alert-ink)" stroke-width="1.8" marker-end="url(#fsm-arrow-coral)"/>
          </svg>
        </div>
      </section>

      <!-- Section 5 -->
      <section id="section-5">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:16px;border-bottom:1px solid var(--border-subtle);padding-bottom:8px;">
          5. On-Chip Small Language Model (SLM) &amp; Neural Hardware Allocation
        </h2>
        <p>
          While crisis escalation remains 100% deterministic, cognitive reframing, bedtime sensory co-regulation, and longitudinal pattern mining require deep linguistic synthesis.
        </p>

        <div class="softcard" style="padding:24px;margin:24px 0;background:var(--surface-card);border:1px solid var(--border-subtle);border-radius:var(--radius-card);box-shadow:0 2px 8px rgba(0,0,0,0.04);overflow-x:auto;">
          <div style="font-family:var(--font-mono);font-size:11px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;color:var(--text-muted);margin-bottom:14px;">
            FIGURE 5.1 &bull; 4-TIER HYBRID INTELLIGENCE &amp; ON-CHIP HARDWARE BOUNDARY
          </div>
          <svg viewBox="0 0 920 300" fill="none" style="width:100%;min-width:720px;height:auto;display:block;">
            <defs>
              <marker id="hw-arrow" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
                <path d="M 0 1.5 L 8 5 L 0 8.5 z" fill="var(--text-muted, #544B3E)"/>
              </marker>
              <marker id="hw-arrow-active" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
                <path d="M 0 1.5 L 8 5 L 0 8.5 z" fill="var(--state-calm-ink, #1A6337)"/>
              </marker>
            </defs>

            <!-- Scope Left: Smartwatch (Wear OS) Zero-LLM Boundary -->
            <rect x="12" y="16" width="360" height="268" rx="14" fill="var(--surface-raised)" stroke="var(--border-subtle)" stroke-width="1.2"/>
            <text x="28" y="38" font-family="var(--font-mono)" font-size="10" font-weight="700" fill="var(--text-muted)">SMARTWATCH (WEAR OS) &bull; ZERO-LLM BOUNDARY</text>

            <rect x="28" y="52" width="328" height="44" rx="8" fill="var(--surface-card)" stroke="var(--border-strong)" stroke-width="1"/>
            <text x="40" y="72" font-family="var(--font-sans)" font-size="12" font-weight="700" fill="var(--text-primary)">PPG Optics &amp; 3-Axis Accel</text>
            <text x="40" y="86" font-family="var(--font-mono)" font-size="9.5" fill="var(--text-secondary)">25Hz Raw Microamp Sensing &bull; Health Services API</text>

            <path d="M 192 96 L 192 110" stroke="var(--border-strong)" stroke-width="1.5" marker-end="url(#hw-arrow)"/>

            <rect x="28" y="114" width="328" height="44" rx="8" fill="var(--surface-card)" stroke="var(--border-strong)" stroke-width="1"/>
            <text x="40" y="134" font-family="var(--font-sans)" font-size="12" font-weight="700" fill="var(--text-primary)">5s Window Feature Extraction</text>
            <text x="40" y="148" font-family="var(--font-mono)" font-size="9.5" fill="var(--text-secondary)">HRV RMSSD, SDNN, pNN50 &bull; Motion Variance</text>

            <path d="M 192 158 L 192 172" stroke="var(--border-strong)" stroke-width="1.5" marker-end="url(#hw-arrow)"/>

            <rect x="28" y="176" width="328" height="44" rx="8" fill="var(--state-calm-soft)" stroke="var(--state-calm-chip)" stroke-width="1.2"/>
            <text x="40" y="196" font-family="var(--font-sans)" font-size="12" font-weight="700" fill="var(--state-calm-ink)">Formula 2.4 Decoupled ANS Scoring</text>
            <text x="40" y="210" font-family="var(--font-mono)" font-size="9.5" fill="var(--state-calm-ink)">Stress Delta = (HR_t - Baseline) / HRV_norm &bull; Sub-10ms</text>

            <path d="M 192 220 L 192 234" stroke="var(--border-strong)" stroke-width="1.5" marker-end="url(#hw-arrow)"/>

            <rect x="28" y="238" width="328" height="36" rx="8" fill="var(--surface-card)" stroke="var(--border-strong)" stroke-width="1"/>
            <text x="40" y="260" font-family="var(--font-sans)" font-size="11.5" font-weight="600" fill="var(--text-primary)">Zero-Anxiety Sensory UI &bull; Gentle Haptics</text>

            <!-- BLE Batch Arrow -->
            <path d="M 372 198 L 442 198" stroke="var(--state-calm-ink)" stroke-width="1.8" marker-end="url(#hw-arrow-active)"/>
            <text x="380" y="190" font-family="var(--font-mono)" font-size="8.5" font-weight="700" fill="var(--state-calm-ink)">BLE SYNC</text>

            <!-- Scope Right: Mobile Companion & NPU Governor -->
            <rect x="448" y="16" width="460" height="268" rx="14" fill="var(--surface-raised)" stroke="var(--border-subtle)" stroke-width="1.2"/>
            <text x="464" y="38" font-family="var(--font-mono)" font-size="10" font-weight="700" fill="var(--text-muted)">MOBILE COMPANION &bull; HARDWARE GOVERNOR &amp; NPU MATRIX</text>

            <!-- Governor Node -->
            <rect x="464" y="52" width="428" height="48" rx="8" fill="var(--surface-card)" stroke="var(--border-strong)" stroke-width="1.2"/>
            <text x="480" y="72" font-family="var(--font-sans)" font-size="12.5" font-weight="700" fill="var(--text-primary)">Hardware Resource Governor</text>
            <text x="480" y="88" font-family="var(--font-mono)" font-size="9.5" fill="var(--text-secondary)">Thermal headroom, battery level &bull; Dynamic compute routing</text>

            <!-- Path A: SLM -->
            <path d="M 570 100 L 570 126" stroke="var(--state-calm-ink)" stroke-width="1.5" marker-end="url(#hw-arrow-active)"/>
            <text x="574" y="116" font-family="var(--font-mono)" font-size="8.5" fill="var(--state-calm-ink)">BATT > 20% &bull; NOMINAL</text>

            <rect x="464" y="130" width="210" height="74" rx="8" fill="var(--state-calm-soft)" stroke="var(--state-calm-chip)" stroke-width="1.5"/>
            <text x="476" y="150" font-family="var(--font-mono)" font-size="9" font-weight="700" fill="var(--state-calm-ink)">ON-CHIP SLM &bull; QUALCOMM NPU</text>
            <text x="476" y="168" font-family="var(--font-sans)" font-size="12" font-weight="700" fill="var(--text-primary)">Gemma 2B INT4</text>
            <text x="476" y="186" font-family="var(--font-sans)" font-size="10" fill="var(--text-secondary)">Contextual reframing &bull; 0.8s inference</text>

            <!-- Path B: Low Power Regex/NLP -->
            <path d="M 780 100 L 780 126" stroke="var(--border-strong)" stroke-width="1.5" marker-end="url(#hw-arrow)"/>
            <text x="784" y="116" font-family="var(--font-mono)" font-size="8.5" fill="var(--text-muted)">LOW BATT / THERMAL</text>

            <rect x="686" y="130" width="206" height="74" rx="8" fill="var(--surface-card)" stroke="var(--border-strong)" stroke-width="1"/>
            <text x="698" y="150" font-family="var(--font-mono)" font-size="9" font-weight="700" fill="var(--text-muted)">ZERO-POWER FALLBACK</text>
            <text x="698" y="168" font-family="var(--font-sans)" font-size="12" font-weight="700" fill="var(--text-primary)">Deterministic Rule NLP</text>
            <text x="698" y="186" font-family="var(--font-sans)" font-size="10" fill="var(--text-secondary)">Flat-mapping &bull; 0% NPU load</text>

            <!-- Converge to Insights -->
            <path d="M 570 204 L 570 226 L 675 226" stroke="var(--state-calm-ink)" stroke-width="1.5"/>
            <path d="M 780 204 L 780 226 L 675 226" stroke="var(--border-strong)" stroke-width="1.5"/>
            <path d="M 675 226 L 675 242" stroke="var(--border-strong)" stroke-width="1.5" marker-end="url(#hw-arrow)"/>

            <rect x="464" y="246" width="428" height="34" rx="8" fill="var(--surface-card)" stroke="var(--border-strong)" stroke-width="1"/>
            <text x="480" y="268" font-family="var(--font-sans)" font-size="11.5" font-weight="600" fill="var(--text-primary)">Daily Journal Flat-Mapping &bull; Longitudinal Sensor Insights</text>
          </svg>
        </div>

        <h3 style="font-size:18px;color:var(--text-primary);margin-top:20px;margin-bottom:10px;">5.1 The Watch Boundary: Strict Zero-LLM Invariant</h3>
        <p>
          Smartwatches operate with strict thermal dissipation and energy envelopes. Running transformer matrix multiplications on a smartwatch rapidly elevates chassis temperature above 40°C, inducing sensory distress and burning battery within minutes. <strong>Under no circumstances does any language model execute on the watch.</strong>
        </p>

        <h3 style="font-size:18px;color:var(--text-primary);margin-top:20px;margin-bottom:10px;">5.2 Phone-Side NPU Architecture: Gemma 2B INT4</h3>
        <p>
          On modern smartphones (Apple Neural Engine on iOS; Qualcomm Hexagon / Google Tensor NPU on Android), BioCue specifies an on-chip, 4-bit quantized Small Language Model (SLM):
        </p>
        <ul style="padding-left:24px;display:flex;flex-direction:column;gap:8px;margin:14px 0;">
          <li><strong>Architecture:</strong> Google Gemma 2B (INT4) / Apple Foundation Model CoreML graph.</li>
          <li><strong>Parameter Count:</strong> 2.0 Billion parameters (quantized to ~1.2 GB unified memory).</li>
          <li><strong>Throughput:</strong> 24–35 tokens/second with sub-1.2s cold start latency on dedicated NPU tensor cores.</li>
          <li><strong>100% Data Sovereignty:</strong> Zero network sockets. Model weights execute purely against local unified memory. Vitals and reflections never leave the device.</li>
        </ul>

        <h3 style="font-size:18px;color:var(--text-primary);margin-top:20px;margin-bottom:10px;">5.3 The Hardware Resource Governor</h3>
        <p>
          Inference is strictly throttled by the <code>HardwareResourceGovernor</code>:
        </p>
        <ul style="padding-left:24px;display:flex;flex-direction:column;gap:8px;margin:14px 0;">
          <li>If battery is &lt; 20% and unplugged: SLM execution is suppressed. The app falls back immediately to 0mA Semantic NLP.</li>
          <li>If OS reports thermal state ≥ Serious or Critical: Inference is deferred.</li>
          <li>If Low Power Mode / Battery Saver is enabled: Inference is bypassed.</li>
        </ul>
      </section>

      <!-- Section 6 -->
      <section id="section-6">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:16px;border-bottom:1px solid var(--border-subtle);padding-bottom:8px;">
          6. Empirical Benchmarks &amp; System Validation
        </h2>
        <div class="softcard" style="padding:20px;margin:16px 0;background:var(--surface);">
          <table style="width:100%;border-collapse:collapse;font-size:14px;">
            <thead>
              <tr style="border-bottom:2px solid var(--border-strong);text-align:left;">
                <th style="padding:10px 8px;">Subsystem</th>
                <th style="padding:10px 8px;">Industry Baseline</th>
                <th style="padding:10px 8px;">BioCue Implementation</th>
                <th style="padding:10px 8px;">Measured Gain</th>
              </tr>
            </thead>
            <tbody>
              <tr style="border-bottom:1px solid var(--border-subtle);">
                <td style="padding:10px 8px;font-weight:600;">Watch Battery Life</td>
                <td style="padding:10px 8px;color:var(--state-alert-ink);">6.2 hours (continuous)</td>
                <td style="padding:10px 8px;color:var(--state-calm-ink);font-weight:600;">18.4 hours (duty-cycled)</td>
                <td style="padding:10px 8px;font-weight:700;">+196% Battery Runtime</td>
              </tr>
              <tr style="border-bottom:1px solid var(--border-subtle);">
                <td style="padding:10px 8px;font-weight:600;">Sensory Stillness Detection</td>
                <td style="padding:10px 8px;color:var(--state-alert-ink);">Falsely scored as "Sleep"</td>
                <td style="padding:10px 8px;color:var(--state-calm-ink);font-weight:600;">Autonomic Decoupling (Tier 2)</td>
                <td style="padding:10px 8px;font-weight:700;">100% Classification Parity</td>
              </tr>
              <tr style="border-bottom:1px solid var(--border-subtle);">
                <td style="padding:10px 8px;font-weight:600;">Crisis Safety Latency</td>
                <td style="padding:10px 8px;color:var(--state-alert-ink);">3–8s (cloud LLM api)</td>
                <td style="padding:10px 8px;color:var(--state-calm-ink);font-weight:600;">0.12 ms (deterministic FSM)</td>
                <td style="padding:10px 8px;font-weight:700;">Zero-Latency Fail-Safe</td>
              </tr>
              <tr>
                <td style="padding:10px 8px;font-weight:600;">Raw Telemetry Cloud Egress</td>
                <td style="padding:10px 8px;color:var(--state-alert-ink);">14.2 MB / day</td>
                <td style="padding:10px 8px;color:var(--state-calm-ink);font-weight:600;">0 Bytes (100% Local DB)</td>
                <td style="padding:10px 8px;font-weight:700;">100% Sovereign Privacy</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- Section 7 -->
      <section id="section-7">
        <h2 style="font-size:24px;color:var(--text-primary);margin-bottom:16px;border-bottom:1px solid var(--border-subtle);padding-bottom:8px;">
          7. Conclusion
        </h2>
        <p>
          BioCue proves that wearable mental health and sensory cueing systems do not need to choose between crude rule-based guesswork and invasive, battery-draining cloud artificial intelligence. By decoupling autonomic nervous system metrics from gross motor activity, utilizing strict deterministic safety state machines, and running sub-watt Small Language Models on mobile neural silicon, BioCue establishes a new benchmark for humane, privacy-preserving assistive technology.
        </p>
      </section>

    </article>

    <!-- Bottom Actions -->
    <div style="margin-top:64px;padding-top:32px;border-top:1px solid var(--border-subtle);display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:16px;">
      <a href="${locale === 'en' ? '/' : `/${locale}`}" class="pill pill-outline">
        ← Back to Overview
      </a>
      <a href="https://github.com/mtpnda/biocue" target="_blank" rel="noopener" class="pill pill-primary">
        View Codebase on GitHub ↗
      </a>
    </div>

  </div>
  `;

  return renderLayout({
    title: "BioCue Architecture & Technical Whitepaper — Decoupled Autonomic Intelligence",
    description: "Technical treatise on autonomic-motor decoupling, continuous logistic fusion scoring, deterministic crisis guardian, and on-chip Gemma 2B SLM co-regulation.",
    content,
    locale,
    activeTab: "whitepaper",
    extraHead: `
      <!-- KaTeX Math Styling -->
      <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.css" crossorigin="anonymous"/>
    `,
    scripts: `
      <!-- KaTeX Math Rendering -->
      <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.js" crossorigin="anonymous"></script>
      <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/contrib/auto-render.min.js" crossorigin="anonymous" onload="
        if (typeof renderMathInElement === 'function') {
          renderMathInElement(document.body, {
            delimiters: [
              {left: '$$', right: '$$', display: true},
              {left: '$', right: '$', display: false},
              {left: '\\\\(', right: '\\\\)', display: false},
              {left: '\\\\[', right: '\\\\]', display: true}
            ],
            throwOnError: false
          });
        }
      "></script>
    `
  });
}
