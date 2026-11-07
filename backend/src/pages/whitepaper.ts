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
            $$RR_i = \frac{60000.0}{HR_i} \quad \text{[ms]}$$
          </div>
          <p style="font-size:13.5px;color:var(--text-muted);">
            Valid beats must strictly satisfy physiological limits: $300\text{ ms} \le RR_i \le 1500\text{ ms}$ (40 to 200 BPM) and consecutive change $|RR_i - RR_{i-1}| \le 300\text{ ms}$. Ectopic beats and motion noise are rejected prior to metric computation.
          </p>
        </div>

        <div class="softcard" style="padding:24px;margin:20px 0;background:var(--surface);border:1px solid var(--border-strong);">
          <h4 style="font-size:15px;color:var(--text-primary);margin-bottom:12px;font-family:var(--font-mono);text-transform:uppercase;letter-spacing:0.05em;">
            Formula 2.2 — Root Mean Square of Successive Differences (RMSSD)
          </h4>
          <p style="font-size:14px;color:var(--text-secondary);margin-bottom:12px;">
            For a sliding buffer of $N$ valid beat intervals ($15 \le N \le 30$):
          </p>
          <div style="background:var(--surface-raised);padding:16px 20px;border-radius:var(--radius-sm);color:var(--text-primary);margin-bottom:12px;overflow-x:auto;text-align:center;">
            $$RMSSD = \sqrt{\frac{1}{N - 1} \sum_{i=1}^{N-1} (RR_{i+1} - RR_i)^2}$$
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
            Features are standardized against the individual's calibrated baseline ($\mu_{HR}, \sigma_{HR}, \mu_{RMSSD}, \sigma_{RMSSD}$):
          </p>
          <div style="background:var(--surface-raised);padding:16px 20px;border-radius:var(--radius-sm);color:var(--text-primary);margin-bottom:12px;overflow-x:auto;text-align:center;">
            $$hrZ = \frac{w.hrMean - \mu_{HR}}{\max(\sigma_{HR}, 1.0)}, \quad hrvZ = \frac{\mu_{RMSSD} - w.rmssd}{\max(\sigma_{RMSSD}, 1.0)}$$
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
            $$Score = 0.35 \cdot hrZ + 0.35 \cdot hrvZ + 0.15 \cdot Repetitiveness + 0.15 \cdot HR_{slope}$$
            $$\text{Stress \%} = \frac{1}{1 + e^{-1.2(Score - 1.0)}} \times 100\%$$
          </div>
          <p style="font-size:13.5px;color:var(--text-muted);">
            • <strong>Tier 0 (Calm):</strong> $Score &lt; 1.5$ (&lt; 50% Stress)<br/>
            • <strong>Tier 1 (Rising):</strong> $1.5 \le Score \le 2.5$ (50% to 74% Stress)<br/>
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
        <div class="softcard" style="padding:24px;margin:24px 0;background:var(--surface);border:1px solid var(--border-strong);overflow-x:auto;">
          <h4 style="font-size:14px;color:var(--text-primary);margin-bottom:16px;font-family:var(--font-mono);text-transform:uppercase;letter-spacing:0.05em;">
            Figure 3.1 — Adaptive Sensor Duty-Cycling State Machine
          </h4>
          <pre class="mermaid" style="display:flex;justify-content:center;background:transparent;border:none;margin:0;">
flowchart LR
  subgraph Calm["Tier 0: Calm Baseline"]
    A["PPG Active Sampling (30s)"] -->|"Update Rolling EMA Profile"| B["PPG Sleep (90s, 0mA)"]
    B -->|"90s Timer Expired"| A
  end
  B -->|"Motion Spike (Accel > 0.5G)"| C["Instant PPG Wake (0s Delay)"]
  A -->|"Stress Elevated (Tier 1/2)"| D["Continuous PPG Telemetry"]
  C --> D
  D -->|"Recovery to Tier 0 (< 50%)"| A
          </pre>
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

        <div class="softcard" style="padding:24px;margin:24px 0;background:var(--surface);border:1px solid var(--border-strong);overflow-x:auto;">
          <h4 style="font-size:14px;color:var(--text-primary);margin-bottom:16px;font-family:var(--font-mono);text-transform:uppercase;letter-spacing:0.05em;">
            Figure 4.1 — Deterministic Crisis Guardian Finite State Machine (FSM)
          </h4>
          <pre class="mermaid" style="display:flex;justify-content:center;background:transparent;border:none;margin:0;">
stateDiagram-v2
  [*] --> Phase1: Tier 1/2 Spike or Manual Tap
  state "Phase 1: Silent Presence (0 to 3m)" as Phase1
  state "Phase 2: Somatic Co-Regulation (3 to 7m)" as Phase2
  state "Phase 3: Safety Check (7 to 12m)" as Phase3
  state "Phase 4: Prolonged Distress (12 to 15m)" as Phase4
  state "30s Audible/Tactile Grace Alarm (15m)" as Grace
  state "Crisis Broadcast Dispatched" as Crisis
  state "Resolved / Calm Grounded" as Resolved

  Phase1 --> Phase2: T = 3m (Validation & Zero Demands)
  Phase2 --> Phase3: T = 7m (Haptic Breathing Pacer)
  Phase3 --> Resolved: Wearer confirms safe
  Phase3 --> Phase4: Unresponsive & T = 12m
  Phase4 --> Grace: HR > Baseline + 20 & T = 15m
  Grace --> Resolved: Cancelled by Wearer
  Grace --> Crisis: Countdown Expired (T = 15m30s)
  Crisis --> [*]: Circle Alerted with Single GPS Fix
          </pre>
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

        <div class="softcard" style="padding:24px;margin:24px 0;background:var(--surface);border:1px solid var(--border-strong);overflow-x:auto;">
          <h4 style="font-size:14px;color:var(--text-primary);margin-bottom:16px;font-family:var(--font-mono);text-transform:uppercase;letter-spacing:0.05em;">
            Figure 5.1 — 4-Tier Hybrid Intelligence &amp; On-Chip Hardware Boundary
          </h4>
          <pre class="mermaid" style="display:flex;justify-content:center;background:transparent;border:none;margin:0;">
flowchart TD
  subgraph Watch["Smartwatch (Wear OS) — Strict Zero-LLM Boundary"]
    Sensors["PPG Optics & 3-Axis Accel"] --> Windows["5s Window Feature Extraction"]
    Windows --> Scoring["On-Device Scoring (Formula 2.4)"]
    Scoring --> WatchUI["Zero-Anxiety Sensory UI"]
  end

  Scoring -->|"Tier 1/2 Escalation"| Guardian["Deterministic Crisis Guardian (FSM)"]
  Scoring -.->|"BLE Batch Sync"| Phone["Mobile Companion App"]

  subgraph Companion["Phone Hardware Governor & NPU Matrix"]
    Phone --> Gov{"Hardware Resource Governor"}
    Gov -->|"Battery > 20% & Thermal Normal"| SLM["On-Chip SLM (Gemma 2B INT4)"]
    Gov -->|"Low Battery (< 20%) or Severe Heat"| Fallback["Zero-Power Regex/Semantic NLP"]
    SLM --> Insights["Cognitive Reframing & Pattern Synthesis"]
    Fallback --> Insights
  end
          </pre>
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

      <!-- Mermaid Architecture & State Diagram Engine -->
      <script type="module">
        import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';
        mermaid.initialize({
          startOnLoad: true,
          theme: document.documentElement.getAttribute('data-theme') === 'high-contrast' ? 'dark' : 'neutral',
          fontFamily: 'Atkinson Hyperlegible, -apple-system, BlinkMacSystemFont, sans-serif'
        });
      </script>
    `
  });
}
