# Sensory Overload / Meltdown Early-Warning System
## Algorithm & Technical Design Document

*Version 0.1 — August 2026*
*Target platform: Wear OS smartwatches (Samsung Galaxy Watch4 series and later). Reference device: Galaxy Watch 4 Classic.*

---

# PART A — ALGORITHM DESIGN

## A1. Design Principles

1. **Personalized, not population-based.** Autistic individuals show atypically low baseline HRV and highly variable arousal profiles (Cheng et al., 2020). All thresholds are computed per individual against their own rolling baseline.
2. **Multi-signal fusion.** No single signal alerts on its own; HR, HRV, and movement must agree, cutting false alarms.
3. **Simple and interpretable first.** Rule-based z-score fusion at launch; per-person logistic regression / XGBoost only after labeled episodes accumulate (Imbiriba et al., 2023 found logistic regression the most consistent classifier).
4. **Quiet by design.** The wearer's device never sirens or flashes. Alerts go silently to the caregiver's phone; the wearer receives at most a gentle haptic prompt.
5. **Fail soft.** Loss of connectivity or sensor dropout degrades to on-watch buffering — never silent data loss.

## A2. Input Signals (Galaxy Watch 4 Classic)

| Signal | Source | Rate | Use |
|---|---|---|---|
| Heart rate (BPM) | Samsung Health Sensor SDK, continuous HR tracker | ~1 Hz | Arousal level + slope |
| Inter-beat interval (IBI) | Same tracker (IBI list with quality flags) | per beat | Compute RMSSD (HRV) |
| 3-axis accelerometer | Sensor SDK continuous accelerometer (raw X/Y/Z) | 25–50 Hz | Movement energy, repetitiveness, exercise gate |
| Sleep summary (prior night) | Samsung Health / Health Connect | daily | Vulnerability multiplier |
| Off-body / wear detection | Wear OS Health Services | event | Suppress scoring when watch removed |

**Not available on Watch 4 hardware (documented for future devices):** electrodermal activity (EDA), continuous skin temperature (added Watch 5+). Architecture keeps a plug-in slot for both.

## A3. Processing Pipeline

### Stage 0 — Enrollment & Baseline (Days 1–14, no alerts)
- Collect HR, IBI, accelerometer during normal life.
- Build circadian baseline buckets: for each hour-of-day bucket h, store rolling mean and standard deviation:
  - μ_HR(h), σ_HR(h)
  - μ_RMSSD(h), σ_RMSSD(h)
  - μ_ACC(h), σ_ACC(h)
- Baselines keep updating for life via exponential moving average (α ≈ 0.05/day) so the system adapts to medication changes, growth, seasons.
- Minimum data quality gate: a bucket needs ≥ 3 hours of clean data before it can be used for scoring.

### Stage 1 — Feature Extraction (sliding 60 s window, 30 s hop)
Per window compute:
- `HR_mean`, then **HR_z** = (HR_mean − μ_HR(h)) / σ_HR(h)
- `RMSSD` from artifact-filtered IBIs (discard IBIs flagged low-quality or outside 250–2000 ms; require ≥ 20 valid IBIs, else mark window "HRV-invalid"), then **HRV_z** = (μ_RMSSD(h) − RMSSD) / σ_RMSSD(h)  *(inverted: HRV drop → positive score)*
- **HR_slope** = least-squares slope of HR over trailing 3 min (bpm/min), normalized
- `ACC_mag` = mean |acceleration vector| − 1 g → **movement energy**
- **Repetitiveness R** = ratio of band power in 0.5–4 Hz (rocking/flapping band) to total power, via FFT on the dominant axis
- `StepCadence` from Health Services step detector (for exercise gate)

### Stage 2 — Context Gates (false-alarm suppression)
Score is suppressed (window skipped) when any of:
- **Off-body:** wear detection reports not worn.
- **Exercise:** StepCadence > 100 steps/min, OR movement energy high with locomotion signature (broadband accel, low repetitiveness) AND HR rise proportional to movement.
- **Charging / just-donned:** first 5 min after wear.
The dangerous signature this preserves: **high HR + suppressed HRV while relatively still, or with repetitive (non-locomotive) movement.**

### Stage 3 — Fusion Score
```
S = 0.35·HR_z + 0.35·HRV_z + 0.15·R_norm + 0.15·HR_slope_norm
S_adj = S × (1 + 0.2·poor_sleep_flag)
```
- `poor_sleep_flag` = 1 if prior-night sleep < individual's 25th percentile duration or efficiency.
- If the window is HRV-invalid, re-weight: S = 0.55·HR_z + 0.20·R_norm + 0.25·HR_slope_norm, and require a higher threshold (see A4).

### Stage 4 — Decision Logic (hysteresis)
- **Tier 1 (early warning):** S_adj ≥ T1 in ≥ 2 of last 3 windows. Default T1 = 1.5.
- **Tier 2 (escalation):** S_adj ≥ T2 in ≥ 3 of last 4 windows, or Tier 1 sustained ≥ 5 min. Default T2 = 2.5.
- **Recovery:** S_adj < 1.0 for 4 consecutive windows → state returns to calm.
- **Cooldown:** after any alert, 10 min minimum before the same tier can fire again.
- Thresholds T1/T2 are per-user tunable and auto-adjusted by the learning loop (A6).

### Stage 5 — Tiered Response
| Tier | Wearer's watch | Caregiver's phone |
|---|---|---|
| 1 — Early warning | Gentle single haptic + optional pictogram prompt ("break?") — configurable, can be fully silent | Quiet notification: "Elevated stress signals" + live HR sparkline |
| 2 — Escalation | Nothing new (no alarms, no sounds) | High-priority alert: sustained distress pattern, live HR, last known location, time in state |
| Recovery | Optional calm haptic | "Returning to baseline" note + episode summary |

### Stage 6 — Post-Episode Logging
Every Tier ≥ 1 event stores a 15-min pre / 15-min post signal snapshot for the learning loop and caregiver review.

## A4. Learning Loop (Personalization)
1. Caregiver marks each alert: **true / false / unsure**, and can log **missed events** (meltdown with no alert) with approximate time.
2. After ~30 labeled episodes: train per-user **logistic regression** on Stage-1 features (windows −5 min to 0 min before labeled onsets = positive class; matched calm windows = negative class). Replace the fixed-weight formula when cross-validated AUROC beats it.
3. After ~100 episodes: optionally upgrade to gradient boosting (XGBoost), following single-subject results in the 2023 E4 study.
4. Threshold auto-tuning: maintain target false-alarm rate ≤ 2/week by nudging T1/T2 weekly.

## A5. Expected Performance Envelope (honest framing)
- Literature ceiling with research-grade sensors: AUROC ≈ 0.80 at 3-min horizon (Imbiriba 2023).
- Consumer PPG is noisier during motion → expect lower initial performance; the exercise gate and HRV-invalid fallback exist to protect precision at the cost of some recall.
- Success metric for v1: **caregivers receive a useful early warning for ≥ 50% of episodes with ≤ 2 false alarms/week**, improving with labeling.

---

# PART B — TECHNICAL DESIGN (Wear OS)

## B1. System Architecture

```
┌─────────────────────────────┐        ┌──────────────────────────────┐
│  WATCH (Wear OS, Watch 4C)  │  BLE   │  PHONE (Android, caregiver    │
│                             │◄──────►│  or wearer's paired phone)    │
│  ┌───────────────────────┐  │ Data   │  ┌────────────────────────┐  │
│  │ SensorService (FGS)   │  │ Layer  │  │ Ingest & SignalStore   │  │
│  │ Samsung Health Sensor │  │        │  │ (Room DB)              │  │
│  │ SDK: HR+IBI, ACC      │  │        │  ├────────────────────────┤  │
│  ├───────────────────────┤  │        │  │ Feature Engine +       │  │
│  │ Ring buffer (4 h)     │  │        │  │ Scoring Engine         │  │
│  ├───────────────────────┤  │        │  ├────────────────────────┤  │
│  │ Lightweight on-watch  │  │        │  │ Alert Manager ─────────┼──┼─► FCM ─► Caregiver app
│  │ fallback scorer       │  │        │  ├────────────────────────┤  │
│  ├───────────────────────┤  │        │  │ Baseline Store +       │  │
│  │ Haptic prompt UI      │  │        │  │ Learning Loop (opt.    │  │
│  │ (Compose for Wear OS) │  │        │  │ on-device training)    │  │
│  └───────────────────────┘  │        │  └────────────────────────┘  │
└─────────────────────────────┘        └──────────────────────────────┘
```

**Split of responsibilities:** the watch only collects, buffers, and streams; all heavy computation (FFT, baselines, scoring, ML) runs phone-side. A minimal on-watch scorer (HR_z + movement only) covers Bluetooth-out-of-range periods.

## B2. Watch App (Wear OS)

- **Language/UI:** Kotlin + Compose for Wear OS. Min target: Wear OS 3 (Watch 4 launched on Wear OS 3; supports up to Wear OS 5).
- **Sensor access:** **Samsung Health Sensor SDK** (`samsung-health-sensor-api.aar`) — public SDK, supported on Galaxy Watch4 and later. Trackers used:
  - `HEART_RATE` continuous tracker → BPM + IBI list + IBI quality/status flags.
  - `ACCELEROMETER` continuous tracker → raw X/Y/Z, delivered in batched events without waking the CPU (SDK's continuous trackers batch in the sensor processor — key for battery).
  - Requires Health Platform installed on watch; request `BODY_SENSORS` permission (+ `BODY_SENSORS_BACKGROUND` where applicable).
- **Fallbacks for non-Samsung Wear OS watches (future):** AndroidX **Health Services** `MeasureClient`/`PassiveMonitoringClient` for HR; standard `SensorManager` for accelerometer. HRV availability varies by OEM — feature-flag HRV off when IBI is not exposed.
- **Service model:** a Foreground Service with `health` foreground-service type, started on wear-on; `AmbientLifecycleObserver` friendly; survives doze via sensor batching.
- **On-watch buffering:** ring buffer (Room or flat file) holding ≥ 4 h of compressed samples; flushes to phone via **Wearable Data Layer API** (`ChannelClient` for bulk, `MessageClient` for alerts) whenever connected.
- **Wearer UI:** single watch face complication/tile showing system state (calm / elevated) with pictograms; configurable haptic prompts; a large "I need help" button as a manual override (one tap, no menus — designed for degraded motor control).

## B3. Phone App (Android)

- **Ingest:** `WearableListenerService` receives batches; writes to Room `SignalStore`.
- **Feature Engine:** Kotlin coroutines pipeline; FFT via JTransforms or Kotlin DSP; windows computed on 60 s / 30 s hop schedule driven by incoming batches (not wall-clock alarms — battery).
- **Baseline Store:** per hour-bucket EMA of μ/σ triplets; persisted; exportable.
- **Scoring Engine:** implements Stages 2–4 exactly as Part A; all constants in a remote-config-able `DetectionProfile` per user.
- **Alert Manager:**
  - Local high-priority notification on the paired phone.
  - **Firebase Cloud Messaging** push to registered caregiver devices (multi-caregiver supported), payload: tier, score trace, HR sparkline data, battery, last location (if location permission granted on wearer's phone).
  - Delivery acknowledgment; if no caregiver ACK within N min at Tier 2, escalate to SMS fallback (optional, via caregiver-configured service).
- **Sleep input:** read prior-night sleep session from **Health Connect** (Samsung Health syncs sleep there).
- **Learning loop:** labeled episodes stored locally; logistic regression trained on-device (simple SGD — feature count is tiny); optional XGBoost via ONNX Runtime Mobile later.

## B4. Data & Privacy Design

- **Local-first:** raw signals never leave the watch–phone pair by default. Caregiver pushes contain derived scores + short HR sparkline only.
- Optional encrypted cloud backup (user opt-in) for baseline portability.
- Health data storage: SQLCipher-encrypted Room DB; Android Keystore for keys.
- Consent model: wearer/guardian consent flow at enrollment; clear data-deletion path.
- Compliance posture: this is a **wellness/informational product, not a medical device** — mirror Samsung's own SDK disclaimer ("for fitness and wellness, not medical use") in all UI, and avoid diagnostic claims. Revisit regulatory classification (FDA/CE) before any clinical claims.

## B5. Battery Budget (Watch 4 Classic realities)

| Consumer | Mitigation |
|---|---|
| Continuous PPG (HR+IBI) | SDK continuous tracker batches in sensor hub; accept ~duty-cycled PPG if battery < 30% (drop HRV, keep HR at lower rate) |
| Accelerometer 25–50 Hz | Batched delivery; downsample to 25 Hz; on-watch pre-computation of window energy only |
| BLE streaming | Flush every 5 min (not per-sample); larger gaps when phone absent |
| Target | Full waking-day coverage (~16 h) on one charge; nightly charging assumed (sleep comes from phone/Health Connect, or accept no overnight signals in v1) |

## B6. Failure Modes & Handling

| Failure | Behavior |
|---|---|
| Phone out of BLE range | On-watch fallback scorer (HR_z + movement, higher threshold); buffer signals; sync + retro-score on reconnect |
| Watch removed | Wear-detection gate suppresses scoring; caregiver notified "watch off" after 10 min (configurable) |
| Poor PPG quality (motion) | IBI quality flags → HRV-invalid path (A3, Stage 3) |
| Sensor SDK unavailable (non-Samsung watch) | Health Services HR-only mode, HRV features disabled |
| Caregiver phone offline | FCM retries; SMS fallback at Tier 2 (optional) |

## B7. Device Support Matrix (v1)

| Device | HR | IBI/HRV | Accel | Skin temp | EDA | Status |
|---|---|---|---|---|---|---|
| Galaxy Watch 4 / 4 Classic | ✅ | ✅ | ✅ | ❌ | ❌ | **Reference device** |
| Galaxy Watch 5/6/7/8 | ✅ | ✅ | ✅ | ✅ (plug-in slot) | ⚠️ device-dependent | Supported |
| Other Wear OS (Pixel Watch etc.) | ✅ | ⚠️ OEM-dependent | ✅ | ❌ | ❌ | Degraded mode, post-v1 |

## B8. Build Roadmap

1. **M0 — Sensor spike (1–2 wks):** Wear OS app streaming HR+IBI+accel to phone; verify IBI quality on Watch 4 Classic on-wrist.
2. **M1 — Data collection build (2–4 wks):** baseline pipeline, ring buffer, sync, caregiver labeling UI. Run 2-week enrollment on first user.
3. **M2 — Detection v1:** rule-based scoring + tiered alerts; begin false-alarm tuning.
4. **M3 — Learning loop:** labeling → per-user logistic regression; threshold auto-tune.
5. **M4 — Hardening:** battery tuning, failure modes, multi-caregiver, privacy review.
6. **M5 — Field validation:** compare alert log vs caregiver diary for ≥ 4 weeks; report hit rate / false-alarm rate against the A5 success metric.

---

## Appendix: Key SDK References
- Samsung Health Sensor SDK — introduction & data specifications: developer.samsung.com/health/sensor
- Wearable Data Layer API: developer.android.com/training/wearables/data/data-layer
- AndroidX Health Services on Wear OS: developer.android.com/health-and-fitness/guides/health-services
- Health Connect (sleep): developer.android.com/health-and-fitness/guides/health-connect

*Companion document: "Meltdown_Detection_Research_Evidence.md" — peer-reviewed evidence base for each algorithm component.*
