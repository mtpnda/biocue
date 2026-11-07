# 20. Hybrid Intelligence & On-Chip SLM Architecture

> **Normative for:** `core-detection/.../ml/*`, `wear/.../wear/*` (Wear OS), `ios/.../Wearer/*` (iOS Companion), and `backend/src/pages/whitepaper.ts`.  
> **Status:** Authoritative & Normative.

---

## 1. Executive Summary & Architecture Philosophy

BioCue rejects the industry standard of either relying on simplistic static thresholds (e.g. `HR > 100`) or outsourcing sensitive neurological and mental health data to cloud Large Language Models (LLMs).

Instead, BioCue enforces a **4-Tier Hybrid Multi-Stage Topology**:
1. **Tier 0 (Hardware & Duty-Cycle):** Ultra-low-power hardware sensor gating and Kalman/EMA rolling baseline profiling.
2. **Tier 1 (Statistical Machine Learning):** Real-time Z-score autonomic-motor decoupling and continuous logistic regression scoring.
3. **Tier 2 (Deterministic Crisis Guardian):** Formally verified, deterministic state machine for life-safety and emergency escalation (zero hallucination tolerance).
4. **Tier 3 (Semantic NLP & On-Chip SLM):** Sub-watt Small Language Model (SLM) inference running exclusively on the phone's Neural Processing Unit (NPU) for personalized cognitive reframing and pattern mining, operating 100% offline.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        WEAR OS SMARTWATCH                              │
│  ┌───────────────────────┐             ┌────────────────────────────┐  │
│  │ Hardware Offbody &    │             │ Adaptive Rolling Baseline  │  │
│  │ Accelerometer Gating  │             │   (Calm-Gated EMA Model)   │  │
│  └──────────┬────────────┘             └─────────────▲──────────────┘  │
│             ▼                                        │                 │
│  ┌───────────────────────┐             ┌─────────────┴──────────────┐  │
│  │ 30s/90s Optical PPG   │────────────▶│ Feature Extractor (IBI/    │  │
│  │ Duty-Cycle Engine     │             │ RMSSD, Slope, Periodicity) │  │
│  └───────────────────────┘             └─────────────┬──────────────┘  │
└──────────────────────────────────────────────────────┼─────────────────┘
                                                       │ Local BLE / Data Layer
                                                       ▼ (Zero Cloud Relay)
┌────────────────────────────────────────────────────────────────────────┐
│                         SMARTPHONE COMPANION                           │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Tier 1: Statistical Logistic Regression & Decoupled Arousal Model │  │
│  │    (Autonomic Vagal RMSSD Drop vs. Physical Stillness Score)     │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
│                                     │                                  │
│             ┌───────────────────────┴───────────────────────┐          │
│             ▼                                               ▼          │
│  ┌─────────────────────────────┐             ┌──────────────────────┐  │
│  │ Tier 2: Crisis Guardian     │             │ Tier 3: On-Chip SLM  │  │
│  │ Deterministic State Machine │             │ Cognitive Companion  │  │
│  │ • 0-3m: Silent Presence     │             │ (Gemma 2B / CoreML)  │  │
│  │ • 3-7m: Somatic Pacer       │             │ • Thermal Governor   │  │
│  │ • 7-12m: Safety Check       │             │ • Battery Gated      │  │
│  │ • 12-15m: Grace Alarm       │             │ • 100% Offline NPU   │  │
│  │ • 15m+: Circle Escalation   │             │ • Pattern Discovery  │  │
│  └─────────────────────────────┘             └──────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Mathematical Formulations

### 2.1 Inter-Beat Interval (IBI) & RMSSD Calculation
Given heart rate readings $HR_i$ in BPM:
$$RR_i = \frac{60000.0}{HR_i} \text{ (ms)}$$

Artifact filter criteria:
$$\text{Valid}(RR_i) \iff 300\text{ ms} \le RR_i \le 1500\text{ ms} \quad \land \quad |RR_i - RR_{i-1}| \le 300\text{ ms}$$

For a sliding window of $N$ valid intervals ($15 \le N \le 30$):
$$RMSSD = \sqrt{\frac{1}{N-1} \sum_{i=1}^{N-1} (RR_{i+1} - RR_i)^2}$$

### 2.2 Autonomic-Motor Decoupling Model
Commercial wearables equate motor stillness ($Acc_{mag} < 0.05\text{g}$) with calm sleep. Under neurodivergent sensory overload, autonomic hyperarousal occurs while the body is immobilized in bed.

BioCue models this via standardized $Z$-scores against the wearer's calibrated baseline:
$$hrZ = \frac{w.hrMean - \mu_{HR}}{\max(\sigma_{HR}, 1.0)}$$
$$hrvZ = \frac{\mu_{RMSSD} - w.rmssd}{\max(\sigma_{RMSSD}, 1.0)}$$

Where $hrvZ$ is inverted: a plummeting $RMSSD$ represents vagal withdrawal and produces a positive distress contribution.

$$Score = (0.35 \cdot hrZ) + (0.35 \cdot hrvZ) + (0.15 \cdot Repetitiveness) + (0.15 \cdot HR_{slope})$$

$$\text{Stress \%} = \frac{1}{1 + e^{-1.2(Score - 1.0)}} \times 100$$

- **Tier 0 (Calm):** $Score < 1.5$ ($\text{Stress \%} < 50\%$)
- **Tier 1 (Rising):** $1.5 \le Score \le 2.5$ ($50\% \le \text{Stress \%} < 75\%$)
- **Tier 2 (Overload):** $Score > 2.5$ ($\text{Stress \%} \ge 75\%$)

When $Acc_{mag} < 0.08\text{g}$ and hour $\in [21, 6]$, the activity is classified as **"Resting in Bed"**, triggering nocturnal sensory co-regulation rather than daytime movement pacing.

---

## 3. On-Chip Small Language Model (SLM) Specification

### 3.1 Model Selection Criteria
The selected model must run locally on modern mobile system-on-chip (SoC) silicon:
1. **Primary Model Family:** **Gemma 2B INT4** (via Google MediaPipe LLM Inference) on Android / **CoreML quantized Gemma/Phi-3 or Apple Foundation Model** on iOS.
2. **Parameters:** $1.1\text{B} - 2.6\text{B}$ parameters.
3. **Quantization:** 4-bit integer weights (INT4 / AWQ), keeping the model footprint under $\le 1.25\text{ GB}$.
4. **Execution Target:**
   - Apple Silicon: Apple Neural Engine (ANE) + unified memory.
   - Android Silicon: Qualcomm Hexagon NPU / Google Tensor TPU via MediaPipe / NNAPI / ONNX Runtime.

### 3.2 Watch Boundary (Strict Zero-LLM Invariant)
- **Invariant:** Under NO circumstances may any language model or transformer tensor graph execute on the Wear OS smartwatch.
- **Justification:** Watch batteries are limited to $240 - 360\text{ mAh}$. An LLM execution would deplete the battery in $< 20\text{ minutes}$ and trigger thermal skin irritation ($> 42^\circ\text{C}$). The watch runs strictly $O(1)$ arithmetic Z-score scoring.

### 3.3 Hardware & Resource Governor (Phone-Side)
The on-chip SLM is strictly regulated by `HardwareResourceGovernor`:
1. **Battery Gate:** If battery level is $< 20\%$ and not charging, SLM deep synthesis is bypassed; the system falls back instantly to Tier 1/3 (Semantic NLP + Template Synthesis, $0\text{ mA}$ extra draw).
2. **Thermal Gate:** If iOS `ProcessInfo.thermalState` or Android `PowerManager.thermalStatus` exceeds nominal/fair, SLM inference is suppressed.
3. **Low Power Mode Gate:** If iOS Low Power Mode or Android Battery Saver is active, SLM execution is suspended.
4. **Latency Budget:** Inference must begin streaming within $< 1.2\text{ seconds}$ on NPU.
5. **Data Privacy:** 0 bytes transmitted over network. All prompts and outputs exist in memory and local SQLite/Room only.

---

## 4. Deterministic Safety Isolation (Non-Negotiable)

The on-chip SLM is strictly restricted to **Tier 3 (Cognitive Reframing & Pattern Discovery)**.
It is architecturally air-gapped from:
1. **Biometric Scoring:** Remains 100% statistical ML ($hrZ, hrvZ, \text{Score}$).
2. **The Guardian Safety Net:** Emergency countdowns, 988 lifeline links, and caregiver SOS alerts are executed exclusively by the deterministic state machine in [`GuardianEngine.swift`](file:///Users/valdiearsanur/Workspace/biocue/ios/Packages/BioCueCore/Sources/BioCueCore/Detection/GuardianEngine.swift) and [`GuardianEngine.kt`](file:///Users/valdiearsanur/Workspace/biocue/android/core-detection/src/main/java/com/mtpnda/biocue/core/guardian/GuardianEngine.kt).
3. **No Hallucinated Safety Decisions:** The SLM cannot cancel, delay, or trigger an emergency distress alert. Only explicit wearer interaction or the deterministic 15-minute unresponsiveness rule can alter safety states.

---

## 5. Verification Commands
- `cd ios/Packages/BioCueCore && swift test` (Verifies Z-score scoring, Guardian state machine, and resource governor).
- `cd android && ./gradlew :core-detection:test` (Verifies Kotlin detection engine and baseline profiler).
- `cd backend && deno task test` (Verifies web whitepaper publication and API endpoints).
