# 04. User Interface & State

This document outlines the exact UI hierarchy, components, and visual rules necessary to reconstruct the application front-end for both the Watch (Wearer) and Phone (Caregiver).

## 1. Wear OS Interface (The Wearer)
**Core Principle:** The watch UI must be a "zero-anxiety" interface. It must NEVER display raw numbers (HR, HRV) or chaotic, infinite animations.

### 1.1 The Anchor Screen (Default)
- **Background:** Solid black (`#000000`) to save OLED battery and reduce glare.
- **Component:** A single, large, centrally aligned circular button taking up 60% of the screen.
- **Iconography:** Use an abstract, calm shape (like a smooth wave or anchor). Do NOT use aggressive icons or text.
- **State Changes:** 
  - *Tier 0 (Calm):* Button is a dim, neutral color (e.g., `#333333`).
  - *Tier 1 (Warning):* Button transitions to a soft, glowing color (e.g., `#4A90E2`) and triggers a single, gentle haptic tap.

### 1.2 Emoji Check-In (Swipeable Carousel)
Triggered when the wearer taps the Anchor button.
- **Layout:** Horizontal `Pager` or `Swipeable` carousel.
- **Items (One per screen):**
  - 🌩️ Overwhelmed
  - ✨ Excited
  - 🏃 Active
- **Interaction:** Tapping the emoji confirms the label. The screen flashes "✅ Got it!" and returns to the Anchor. If no interaction occurs for 30 seconds, it auto-dismisses.

---

## 2. Mobile Interface (The Caregiver)
**Core Principle:** The Phone UI must instantly convey the wearer's status without causing caregiver panic, and it must provide a structured journal for ML labeling.

### 2.1 The Dashboard Screen (Main View)
Implemented as a scrolling vertical `Column` in Jetpack Compose.

#### A. Status Header (Top)
- **Text Component:** Displays the current wearer state based on the Tier.
  - *Tier 0:* "Alex is calm ✨" (Green/Neutral hue)
  - *Tier 1:* "Alex's stress is rising 📈" (Yellow/Warning hue)
- **Visual Meter:** A simple, horizontal pill-shaped progress bar indicating stress level (Low/Med/High). No percentage numbers.
- **Activity Label:** A small chip below the meter displaying the current motion (e.g., "🪑 Sitting Still", "🚶 Walking", "🔄 Pacing").

#### B. SOS Location Card (Conditional)
- **Trigger:** Only appears if `Tier == 2` AND `sosLatitude != null`.
- **Styling:** `Card` with `errorContainer` colors (Bright Red).
- **Contents:** 
  - Title: "🚨 SOS Location Received"
  - Body: Latitude and Longitude values.
  - Action (Optional): "Open in Google Maps" button.

#### C. The Journal (Bottom List)
- **Layout:** A `LazyColumn` of historical and current events.
- **Event Card Structure:** 
  - Time range (e.g., `2:15 PM – 2:40 PM`)
  - Activity context (e.g., `Pacing`)
  - The wearer's label (e.g., `🌩️ Overwhelmed`).
- **Pattern Review Nudge:** If an event is missing a label (because the wearer ignored the watch carousel), the card shows a "Needs Review" chip with four tagging options for the caregiver:
  - `🌩️ Overwhelmed` | `✨ Excited` | `🏃 Active` | `🤷 Unsure`

### 2.2 Navigation
- Utilize a `BottomNavigation` bar or `Scaffold` if extending beyond the dashboard (e.g., Settings, History).
- Current scope (v1) focuses entirely on the single Dashboard Screen.
