# BioCue — Product Requirements Document

*Version 2.0 — August 2026*

---

## 1. Problem

Autistic individuals experience sensory overload that can escalate into meltdowns. Their body shows measurable warning signs (heart rate spikes, irregular heartbeat rhythm, repetitive movements) **1–3 minutes before** the person consciously realizes what's happening.

Today, there is no consumer tool that catches these early warning signs and helps the person act before they lose executive function.

**Why existing tools fail:**
- Fitness trackers show raw heart rate numbers → triggers health anxiety in autistic users
- No tracker distinguishes exercise from a meltdown
- No tracker distinguishes joyful excitement from distress
- Caregivers have no way to know what's happening remotely without hovering

**Evidence:** Goodwin et al. (2019), Imbiriba et al. (2023), Cheng et al. (2020, PMID 32818581). Autistic individuals have atypically low baseline HRV — all thresholds must be personalized, not population-based.

---

## 2. Users

| | **Wearer** | **Caregiver** |
|---|---|---|
| **Who** | The autistic person (any level) | Parent, partner, therapist, or support person |
| **Devices** | Galaxy Watch 4 Classic + their own phone | Their own phone (linked via the same app) |
| **Role** | Primary user. Owns all data. Full control. | Observer. Gets notified only when needed. |
| **App mode** | Wearer Mode (default) | Caregiver Mode (same app, different view) |

**One app, two modes.** The wearer and caregiver install the same app. They switch roles in settings.

---

## 3. What the Watch Measures

Three sensors on the Galaxy Watch 4 Classic:

| Sensor | What it tells us |
|---|---|
| **Heart rate** | How fast the heart is beating right now |
| **Heart rate variability (IBI)** | How steady the rhythm between beats is — it gets irregular under stress |
| **Accelerometer** | Movement patterns — detects pacing, stimming, sitting still, walking, running |

These three signals are combined into **one simple Stress Level** (shown to the user as a percentage or color gradient — never as raw numbers).

Movement is shown **separately** as a human-readable activity label. Based on accelerometer and step cadence data, the algorithm translates raw motion into these specific definitions:

| Icon | Label | Technical Definition (How it's detected) | Why it matters |
|---|---|---|---|
| 🪑 | **Sitting Still** | Minimal accelerometer energy, step cadence near 0. | Baseline state. If stress spikes while sitting still, it strongly indicates internal distress, not physical exertion. |
| 🚶 | **Walking** | Moderate movement energy + regular step cadence (e.g., 50–90 steps/min). | Normal locomotion. A slight heart rate elevation is expected here. |
| 🏃 | **Exercise** | High broadband movement energy + high step cadence (>100 steps/min). | Automatically suppresses stress alerts to prevent false alarms during workouts. |
| 🔄 | **Pacing** | Moderate energy + irregular step cadence + frequent directional turning. | A common self-regulation and anxiety behavior leading up to a meltdown. |
| 👐 | **Stimming** | High repetitiveness in the 0.5–4 Hz band (rocking, hand flapping), low step cadence. | A primary self-regulation behavior. When combined with a stress spike, it triggers the emoji check-in to ask if it's distress (meltdown) or joy (happy flapping). |

---

## 4. What the Algorithm Detects Automatically

The algorithm (detailed in [Meltdown Detection Algorithm](./Meltdown%20Detection%20Algorithm%20and%20Tech%20Design.md)) handles most situations without any user input:

| Situation | How it's detected | What happens |
|---|---|---|
| Stress is rising | HR unusually high *for this person, at this time of day* + heartbeat rhythm going irregular | Tier 1: gentle haptic on watch |
| Stress is escalating | HR still climbing over 3+ minutes, sustained above threshold | Tier 2: caregiver gets a push notification |
| It's exercise, not a meltdown | Step cadence >100/min + broadband movement | Auto-suppressed. No alert. No user action needed. |
| Stimming detected | Repetitive motion in 0.5–4 Hz band (rocking, flapping) | Factored into the stress score as context |
| Watch removed | Wear detection sensor | Scoring paused. Caregiver notified after 10 min. |
| Poor sleep last night | Prior-night data from Health Connect | Alert threshold lowered (more sensitive today) |
| Body calming down | HR returns to personal baseline for 4+ windows | Event auto-ends. Glow fades. |

### What the algorithm CANNOT distinguish (needs user input):

| Ambiguous case | Why sensors can't tell | Resolved by |
|---|---|---|
| Overwhelmed vs. Excited | Joy-flapping and distress-rocking produce identical HR + HRV + movement patterns | Emoji check-in on the watch (🌩️ vs ✨) |
| Missed event | Meltdown happened but the algorithm didn't catch it | Caregiver logs it manually with approximate time |

---

## 5. The Watch Experience (Wearer)

### Calm State (default)
- A single **Anchor button** (abstract icon, no text) on a dark, statically calm watch face.
- No numbers. No heart rate. No stress percentage. Nothing to read.

### Tier 1 — Early Warning
1. **Gentle haptic tap** on the wrist (vibration).
2. The Anchor button **starts glowing softly** — no text, no alarm, just a subtle visual cue.
3. If the wearer **ignores it** → the glow fades after 30 seconds. No consequence.
4. If the wearer **taps it** → the Emoji Check-In carousel opens.

### Emoji Check-In (swipeable carousel)
- One large emoji displayed at a time. Swipe left/right to change:
  - 🌩️ Overwhelmed
  - ✨ Excited
  - 🏃 Active
- Tap the emoji to confirm. **"✅ Got it!"** shown for 2 seconds, then fades back to Anchor.
- If ignored, the carousel fades after 30 seconds. Un-labeled events go to Pattern Review later.

### Event Ends
- **Auto-ends** when the algorithm detects the body has returned to baseline.
- **OR** the wearer can manually dismiss by tapping the Anchor again.
- The glow gently fades. No fanfare.

---

## 6. The Phone App — Wearer Mode

### Top: Current State
- **Stress Level** — a simple visual meter (like a battery gauge). Low / Medium / High. No raw numbers.
- **Activity Label** — what the accelerometer detects right now: 🚶 Walking, 🔄 Pacing, 🪑 Still, etc.

### Middle: Quick Actions (3 buttons, one tap each)
1. **🔇 Mute My Phone** — silences all notifications, calls, texts to reduce sensory input
2. **🎵 Play Calming Audio** — plays the user's pre-configured calming playlist through connected headphones
3. **💬 Message Caregiver** — sends a pre-written text: "I need space right now" or "Can you come help?"

### Bottom: Journal Timeline
- Scrollable list of today's (and past) stress events.
- Each entry shows: **time range** · **location** · **activity** · **how you labeled it**
- Example: `2:15 PM – 2:40 PM · Safeway · Pacing · 🌩️ Overwhelmed`
- Un-labeled events (emoji check-in was skipped) appear as reviewable cards with context cues.

---

## 7. The Phone App — Caregiver Mode

### Default View: Status & Context
- **Status Card:** Simple, non-anxious display at the top: **"Alex is calm ✨"** or **"Alex may need support 🌩️"**
- **Current State:** The same simple visual Stress Level meter and Activity Label that the wearer sees.
- **Today's Journal:** The caregiver sees the timeline of events. *Why? Because autistic wearers are fully capable of managing their data, and providing the caregiver with daily context prevents them from "flying blind," which reduces caregiver anxiety more effectively than hiding data.*

### Push Notification (Tier 2 only)
- Caregiver gets a notification ONLY when stress has been sustained for 5+ minutes.
- Message: "Alex's stress has been elevated for 5 minutes. They may need support."
- Includes last known location (if permission granted).

### Pattern Review
- Shows only **un-labeled events** (ones the wearer skipped the emoji check-in for).
- Each card includes context cues to jog memory: location, noise level, activity, calendar event.
- **Nudges the caregiver to ask the wearer first** before labeling.
- Labeling options: 🌩️ Overwhelmed · ✨ Excited · 🏃 Active · 🤷 Unsure / Wasn't Present
- "Unsure" labels are dropped from the ML training set.
- Caregiver can also **log missed events** (meltdowns the algorithm didn't catch) with approximate time.

---

## 8. Success Metrics

| Metric | Target | Timeline |
|---|---|---|
| Useful early warning for ≥50% of episodes | ≥50% hit rate | 90 days post-enrollment |
| False alarms | ≤2 per week | 90 days (improving with labeling) |
| Wearer anxiety | Maintained or decreased (self-report) | 60 days |
| Caregiver response time | <30 seconds after Tier 2 notification | 30 days |
| Watch battery life | ≥16 hours (full waking day) | Launch |

---

## 9. Out of Scope (v1)

- EDA and skin temperature (Watch 4 doesn't have them — reserved for Watch 5+)
- Manual location geofencing (adds executive function tax)
- Aggressive haptic buzzes or lock-screen hijacks (breaks predictability)
- Medical device claims (this is a wellness/informational product, not FDA-regulated)
- Non-Samsung Wear OS watches (post-v1, degraded mode)

---

## 10. Hardware

**Reference device:** Samsung Galaxy Watch 4 Classic (Wear OS 3)

| Sensor | Available | Notes |
|---|---|---|
| Heart rate (PPG) | ✅ | Via Samsung Health Sensor SDK |
| Inter-beat interval (IBI) | ✅ | For heart rate variability |
| Accelerometer | ✅ | 25–50 Hz, batched delivery |
| EDA | ❌ | Not on Watch 4 hardware |
| Skin temperature | ❌ | Added in Watch 5+ |
| Wear detection | ✅ | Off-body suppression |
| Sleep data | ✅ | Via Health Connect (from Samsung Health) |
