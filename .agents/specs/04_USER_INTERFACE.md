# 04. User Interface & State Architecture

> **Canonical Specification.** Defines the exact UI hierarchy, component composition, state handling, and visual guidelines for **Wear OS (Watch)**, **Wearer Mobile App**, **Caregiver Mobile App**, and the **Settings Portal** across Android (Jetpack Compose) and iOS (SwiftUI).

---

## 1. Wear OS Interface (The Wearer Smartwatch)

**Core Principle:** The watch UI is a **Zero-Anxiety Interface**. It must NEVER display raw numbers (HR, HRV), chaotic line charts, flashing BPM counters, or startling sirens. It acts quietly in the background as an unobtrusive bio-signal anchor.

### 1.1 Setup, Sensors & Connectivity
- **Sequential Permission Flow:** Requests Body Sensors, Physical Activity, Location, and Notification permissions.
- **Location Services Enforcement:** Prompts wearer if system Location Services are disabled (`ACTION_LOCATION_SOURCE_SETTINGS`). The wearer cannot proceed to the Anchor screen until device location is enabled for emergency SOS capability.
- **Autonomous Local Connectivity:** Background Wi-Fi & Bluetooth engine continuously maintains local telemetry synchronization with the wearer's phone without requiring manual reconnection.

### 1.2 The Anchor Screen (Default)
- **Background:** Solid black (`#000000`) to preserve OLED battery life and minimize visual glare.
- **Anchor Button:** A single, large, centrally positioned circular button occupying 60% of the screen area.
- **Botanical / Minimal Motifs:** Subtle organic contour or anchor icon. No harsh text.
- **Autonomic Tier State Transitions:**
  - *Tier 0 (Calm / Baseline):* Soft neutral charcoal container (`#2C2C2E`), quiescent resting state.
  - *Tier 1 (Rising Autonomic Tone):* Soft warm amber glow (`#FCD34D`), triggers a single gentle haptic pulse.
  - *Tier 2 (Crisis / Overload):* Soft coral aura (`#FCA5A5`), rhythmic de-escalation pulses, offering one-tap SOS.

### 1.3 Quick Emoji Check-In Carousel
Triggered when the wearer taps the Anchor button or confirms a rising stress notification:
- **Layout:** Horizontal swipeable pager (`HorizontalPager` / `TabView`).
- **Items:**
  - 🌩️ `Overwhelmed` (Sensory overload, meltdown precursor)
  - ✨ `Excited` (Positive high-arousal, exercise, play)
  - 🏃 `Active` (Intentional physical exertion)
- **Confirmation:** Tapping confirms the label, flashes a calm confirmation ("✅ Got it!"), and dismisses back to the Anchor. Auto-dismisses if untouched for 30 seconds.

---

## 2. Mobile Interface: Wearer Screen (Android & iOS)

**Core Principle:** The Wearer companion app is a **Self-Regulation Sanctuary**. It gives the wearer agency over their sensory profile, empathetic co-regulation tools, and sovereign control over their caregiver circle.

```
┌─────────────────────────────────────────────────────────────┐
│  Alex's BioCue         [⌚ Connected · 92%]       [ ⚙️ ]   │ ◄ Top Bar & Settings Action
├─────────────────────────────────────────────────────────────┤
│  STATUS METER CARD                                          │
│  Alex is calm ✨                                            │
│  [========================                   ] (Resting)    │ ◄ Autonomic Tone Meter
│  🪑 Sitting Still · Baseline 64 ms                          │
├─────────────────────────────────────────────────────────────┤
│  BIOCUE BUDDY CO-REGULATION CARD                            │
│  "Alex, take a moment to pause. You are safe. Would you     │
│   like to slow down together?"                              │ ◄ Empathetic Buddy
│  [ 🫁 Guided Breathing ]  [ 💭 Write Note ]                 │
├─────────────────────────────────────────────────────────────┤
│  BOTANICAL BREATHING PACER CARD                             │
│                    ╭───────────╮                            │
│                    │     🌿    │                            │ ◄ Decoupled Pulsing Orb
│                    ╰───────────╯                            │   (Zero Container Overflow)
│            Breathe in slowly... (4s)                        │ ◄ Stable Guidance Below Orb
├─────────────────────────────────────────────────────────────┤
│  JOURNAL · TODAY                                 [ < ] [ > ]│ ◄ Daily Journal Browser
│  ╭────────────────────────────────────────────────────────╮ │
│  │ 🌩️  Overwhelmed                              1:25 PM  │ │ ◄ Decomposed Event Title
│  │     [ Crowded Space ]                                  │ │ ◄ Structured Context Chip
│  │     Auto-detected                                      │ │   (6.dp Rounded Rectangle)
│  ╰────────────────────────────────────────────────────────╯ │
└─────────────────────────────────────────────────────────────┘
```

### 2.1 Status Meter Card
- **Autonomic Tone Indicator:** Dynamic header: `"Alex is calm ✨"` (Tier 0) or `"Alex's stress is rising 📈"` (Tier 1).
- **Progress Track:** Smooth capsule meter reflecting real-time stress index. No raw heart rate numbers or medical panic cues.
- **Activity Context:** Displays current movement classifier (e.g., `🪑 Sitting Still`, `🚶 Walking`, `🔄 Pacing`).

### 2.2 BioCue Buddy Co-Regulation Card
- Sits prominently above or adjacent to active intervention modules.
- **Empathetic Contextual Validation:** Provides supportive, non-judgmental guidance tailored to duration and severity (e.g., *"Alex, take a moment to pause. You are safe. Would you like to slow down together?"*).
- **Quick Action Triggers:** One-tap activation of somatic breathing, sensory grounding exercises, or custom note reflections.

### 2.3 Botanical Breathing Pacer Card
- **Decoupled Orb Design:** The animated pulsing orb (`Inhale` / `Hold` / `Exhale`) is decoupled from the guidance text.
- **Zero Overflow Guarantee:** Guidance text is stably anchored **below** the visual orb in a dedicated fixed-height column, guaranteeing zero text clipping or container overflow regardless of screen size, font scale, or animation phase.
- **Pacing Cadences:** Defaults to gentle 4-4-4 box breathing or 4-7-8 autonomic vagal nerve down-regulation.

### 2.4 Sensory Overload & Free-Text Check-In Modal
- Accessible via the Buddy card or manual check-in button.
- **Preset Sensory Chips:** Fast selection of primary state (`Overwhelmed`, `Excited`, `Active`).
- **Free-Text Semantic Input:** Supports arbitrary free-text reflections (e.g. *"Headache, sensory overload from grocery store fluorescent lights and loud noises, resting in dark room"*).
- **Local Sentiment & Keyword Parsing:** On-device semantic engine categorizes triggers into local contextual clusters without cloud analysis.

### 2.5 Decomposed Daily Journal Card
- **Calendar Browsing:** Navigate across days (`< Today >`, `Yesterday`, `-N days ago`).
- **Decomposed Multi-Tag Flat-Mapping:**
  - Multi-tag concatenations from legacy or composite reviews (e.g. `OVERWHELMED - Crowded Space + OVERWHELMED - Loud Noises`) are defensively flat-mapped into separate event records or parsed into a primary emotion title and dedicated context chips.
- **Context Chip Styling:** Uses a clean rectangular badge (`RoundedCornerShape(6.dp)` / `cornerRadius: 6`, `Surface2` background, `LineBorder` outline) instead of a 50% radius capsule, eliminating awkward oval distortion and text spillage.

---

## 3. Mobile Interface: Caregiver Screen (Android & iOS)

**Core Principle:** Provides peace of mind to approved caregivers without subjecting the wearer to continuous surveillance. Caregivers only receive actionable alerts during genuine autonomic spikes.

### 3.1 Status Monitor
- Shows the paired wearer's verified state: `"Alex is calm ✨"` or `"Alex's stress is rising 📈"`.
- Displays time since last sync without exposing raw biometric streams.

### 3.2 Pattern Review Nudge Cards
Appears when an event requires caregiver contextual feedback:
- **Scenario A (Unlabeled ML Event):** The wearer did not label a detected spike. Prompt: *"A stress spike was detected. What state was Alex in?"* Options: `🌩️ Overwhelmed`, `✨ Excited`, `🏃 Active`, `🤷 Unsure`.
- **Scenario B (Wearer-Reported Event):** The wearer pressed Anchor. Prompt: *"What was happening around Alex?"* Options: `🔊 Loud Noises`, `👥 Crowded Space`, `📋 Task Demand`, `🔄 Routine Change`, `🤷 Unknown`.

### 3.3 Emergency SOS Location Card
- **Conditional Visibility:** Displays ONLY when `Tier == 2` AND `sosLatitude != null`.
- **High-Contrast Warning Card:** Soft Coral container (`StatePalette.alert.soft`), displaying alert timestamp and Google Maps / Apple Maps directions trigger.
- **Consent-First Policy:** Zero continuous background GPS tracking; single coordinate fix acquired strictly on SOS trigger.

---

## 4. Settings Screen: Pairing & Circle Management

**Architectural Requirement:** Per user specification, all Caregiver Circle management, Pairing Code generation, and Connection controls MUST live in the **Settings Screen**, keeping primary dashboards clean and focused on bio-signal regulation.

```
┌─────────────────────────────────────────────────────────────┐
│  < Settings                                                 │
├─────────────────────────────────────────────────────────────┤
│  CAREGIVER CIRCLE                                           │
│  Wearer Pairing Code                                        │
│  ╭────────────────────────────────────────────────────────╮ │
│  │                     4 8 2  1 9 5                       │ │ ◄ 6-Digit PIN (Wearer)
│  │           Expires in 08:42 · Single-use                │ │
│  ╰────────────────────────────────────────────────────────╯ │
│  [ Generate New Code ]                                      │
│                                                             │
│  Connected Caregivers (1/2)                                 │
│  • Sarah (Parent) ────────────── [ Revoke Access 🗑️ ]       │ ◄ 1-Tap Instant Wipe
├─────────────────────────────────────────────────────────────┤
│  APPEARANCE & THEME                                         │
│  Theme Mode                 [ 🌿 Calm ]  [ 👁️ Contrast ]   │ ◄ DLS Theme Toggle
├─────────────────────────────────────────────────────────────┤
│  ABOUT & TRANSPARENCY                                       │
│  • Technical Whitepaper (Autonomic Intelligence)        ↗   │
│  • Privacy Policy & Zero-GPS Guarantee                  ↗   │
│  • BioCue Platform v0.1.13 (Build 61)                       │
└─────────────────────────────────────────────────────────────┘
```

### 4.1 Pairing Code Generation (Wearer)
- Generates a cryptographically random, temporary **6-digit numeric PIN** with a 10-minute expiry window.
- Displayed in high-visibility Newsreader Serif (40sp, spaced digits).
- Single-use consumption: as soon as an approved caregiver enters the code, it is purged.

### 4.2 Pairing Code Redemption (Caregiver)
- Text field with 6-digit numeric input formatter.
- Links caregiver's device to wearer's read-only event stream upon wearer authorization.

### 4.3 Connected Caregivers Management
- Lists all active caregivers linked to the wearer (maximum of 2).
- **Instant Revocation:** One-tap `"Revoke Access"` immediately invalidates the caregiver's token in Deno KV/Firestore and instructs the caregiver app to purge all local cached data.

### 4.4 Theme & System Preferences
- **Theme Switcher:** Toggles between DLS Calm Botanical Theme (`#F2F0EA` warm oat surface) and High-Contrast Accessibility Theme (`#FFFFFF` stark white surface with deep `#000000` text).
- **Direct Documentation Links:** Direct links to the Technical Whitepaper (`/whitepaper`), Open-Source GitHub Repository, and Help Center.
