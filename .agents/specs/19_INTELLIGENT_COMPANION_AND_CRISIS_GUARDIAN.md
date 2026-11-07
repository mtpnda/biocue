# 19. Intelligent Companion & Crisis Guardian Engine (BioCue Buddy)

> **Normative for:** `ios/.../Wearer/*`, `android/mobile/...`, `core-detection/.../domain/*`, and `wear/.../wear/*`.  
> **Status:** Authoritative & Normative.

---

## 1. Problem Statement & Moral Imperative

A wearable stress detection system that merely logs data and silent-drops events after 15 minutes fails the wearer when they need help the most. In acute neurodivergent overwhelm, panic attacks, depressive crisis, or potential self-harm:
1. **Loss of Executive Function:** A wearer who presses "Anchor" (or whose vitals spike) is often incapable of typing rigid category codes or remembering to tap "End Event".
2. **The "Silent Agony" Blindspot:** When stress persists for 10–20+ minutes without wearer input, previous systems treated the event as "orphaned". In reality, prolonged elevated autonomic arousal without interaction is the primary signature of **incapacitation, crisis, or self-harm**.
3. **The Loneliness of Distress:** The wearer is often alone when experiencing an episode. The device must not act as a cold surveillance sensor; it must act as a **compassionate, intelligent on-device Buddy** that provides warmth, grounding, de-escalation, and an active safety net.

---

## 2. Free-Text Labeling & Contextual Notes Contract

Event labeling MUST NOT be restricted to predefined enumeration chips (`OVERWHELMED`, `EXCITED`, `ACTIVE`).

### 2.1 Schema & Storage
- `StressEvent.label`: Stores either a standard classification token or arbitrary wearer-authored free text (up to 255 UTF-8 characters).
- `ReviewEvent.cues`: Extended to carry wearer contextual notes.
- `EventIngest`: When a custom free-text string arrives:
  - Preserves exact wearer wording.
  - Generates semantic representation and matches emotional valence.
  - Assigns contextual icon:
    - Keywords `["hurt", "pain", "harm", "cut", "bleed"]` $\rightarrow$ 🩹 (Safety priority)
    - Keywords `["sad", "cry", "lonely", "alone", "hopeless"]` $\rightarrow$ 🌧️
    - Keywords `["noise", "loud", "sound", "music"]` $\rightarrow$ 🔊
    - Keywords `["crowd", "people", "party", "school", "work"]` $\rightarrow$ 👥
    - Default free-text note $\rightarrow$ 💭

---

## 3. The On-Device Intelligent Companion ("BioCue Buddy")

### 3.1 Local On-Device Intelligence Architecture
The Buddy runs 100% locally on the phone (zero cloud telemetry of raw thoughts, feelings, or vitals):
- **Apple Foundation / CoreML / Micro-LLM & Cognitive Grounding Engine:**
  Evaluates:
  - Current autonomic state: Heart Rate ($HR$), HRV ($RMSSD$), $HR_{slope}$, Acceleration.
  - Episode duration: Time elapsed since event start ($t - t_{start}$).
  - Wearer's historical patterns and past successful soothing interventions.
- **Empathetic Interaction Tiers:**
  1. **Phase 1: Silent Presence & Validation ($0 - 3$ min):**
     Soft non-intrusive card: *"I see your stress is rising. I'm right here with you. You are safe."*
  2. **Phase 2: Active Co-Regulation ($3 - 7$ min):**
     Offers interactive micro-tools:
     - **Paced Breathing Pacer:** Visual gentle expanding/contracting orb with optional haptic rhythm (4s in, 4s hold, 6s out).
     - **5-4-3-2-1 Sensory Grounding:** Guides the wearer step-by-step through their environment.
     - **Grounding Mantra:** Affirmations customized to the wearer's preferences.
  3. **Phase 3: Safety & Wellbeing Check ($7 - 12$ min):**
     Gentle prompt: *"Checking in on you. Are you feeling safe right now?"*
     - `[ I am safe · Just need time ]` $\rightarrow$ Silences alarms, continues quiet soothing.
     - `[ I need help · Alert Circle ]` $\rightarrow$ Prompts caregiver emergency notification.
     - `[ Call Crisis Lifeline (988) ]` $\rightarrow$ Direct phone dialer link.
  4. **Phase 4: Automatic De-escalation & Closure ($HR$ returns to baseline):**
     When resting vitals confirm physiological recovery for $> 120\text{s}$:
     *"Your heart is finding its calm again. Would you like to wrap up this event and add a note?"*

---

## 4. The Guardian Protocol: Prolonged Stress & Emergency Escalation

```
[Event Triggered (Manual or Tier 1/2)]
                │
                ▼
        T = 0 to 3 mins: Silent Presence & Vitals Tracking
                │
                ▼
        T = 3 to 7 mins: Empathetic Grounding & Breathing Prompt
                │
                ▼
        T = 7 to 12 mins: Safety Check-in ("Are you safe?")
                │
                ├──────────────────────────────────────────┐
                │ Wearer taps "I'm safe / still coping"    │ Wearer does NOT interact
                ▼                                          ▼
   Continue quiet monitoring                       T = 12 mins: Vitals check
                                                           │
                                                           ▼ (Stress remains high)
                                                   T = 15 mins: Safety Alarm
                                                   (Audible chime + watch pulse)
                                                   30-second Grace Countdown
                                                           │
                                                           ├───────────────────────┐
                                                           │ Wearer taps "I'm OK"  │ Timeout (30s)
                                                           ▼                       ▼
                                                   Alarm dismissed         CRISIS BROADCAST:
                                                                           • High-priority push to circle
                                                                           • GPS fix attached
                                                                           • "Prolonged high stress,
                                                                              unresponsive to check-ins"
```

### 4.1 Unresponsiveness Detection Criteria
An event is escalated to `CRISIS_UNRESPONSIVE` if and only if ALL conditions are met:
1. Event duration $> 12\text{ minutes}$.
2. Mean physiological stress remains elevated ($Score \ge 1.5$ or $HR > Baseline + 20\text{ BPM}$).
3. Zero wearer interactions recorded on watch or phone for $> 10\text{ minutes}$.
4. Wearer does not dismiss the 30-second Grace Countdown safety prompt.

### 4.2 Crisis Broadcast Payload (To Caregivers)
- Encrypted notification dispatched via FCM / Caregiver Channel:
  - **Title:** `⚠️ Safety Alert: [WearerName] Needs Attention`
  - **Body:** `Prolonged high stress detected for 15+ minutes with no response to safety check-ins. Please reach out immediately.`
  - **Location:** Lat/Lng fix captured from watch location service.
  - **Direct Actions:** One-tap Call Wearer, Open Navigation Directions.

---

## 5. UI/UX Specifications

### 5.1 The Buddy Card (Wearer Dashboard)
- Sits above the Journal card during active or recent events.
- Warm, organic container styled with `StatePalette.info` or `StatePalette.calm`.
- Displays dynamic empathetic copy reflecting actual vitals duration.
- Contains:
  - **Micro-Reflection Input:** Inline TextField allowing instant free-text notes.
  - **Interactive Pacer Button:** Launches full-screen or modal breathing/grounding exercise.
  - **Safety Check Pill:** "I'm feeling better" (ends event) or "I need support".

### 5.2 Review Card Free-Text Extension
- Below the preset emotion chips, a clean outlined input field:
  - Placeholder: `"Or write what you're experiencing in your own words..."`
  - Trailing action: `"Save note"` pill button.
  - Dispatches `onResolve(eventId, text)` to record rich contextual reflections into the journal.
