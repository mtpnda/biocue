# 04. User Interface & State

This document outlines the exact UI hierarchy, components, and visual rules necessary to reconstruct the application front-end for both the Watch (Wearer) and Phone (Caregiver).

## 1. Wear OS Interface (The Wearer)
**Core Principle:** The watch UI must be a "zero-anxiety" interface. It must NEVER display raw numbers (HR, HRV) or chaotic, infinite animations.

### 1.1 Setup & Permissions
- **Permission Flow:** The app sequentially requests required permissions (Sensors, Activity, Location, Notifications).
- **Location Services Check:** Even if permissions are granted, the app MUST check if device-level Location Services are enabled via `LocationManager`. If disabled, it displays a full-screen prompt ("Location Services Disabled") with a button that redirects the wearer to the Wear OS system Settings (`ACTION_LOCATION_SOURCE_SETTINGS`). The wearer cannot proceed to the Anchor screen until device location is toggled on.

### 1.2 The Anchor Screen (Default)
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

**Communication Constraint:** There is no generic "Message Caregiver" feature. The ONLY time a caregiver receives a notification/message is when a meltdown is detected (either Auto Tier 2 or a Manual SOS event).

### 2.0 Authentication & Onboarding
- **Sign In / Sign Up:** The app must provide an authentication gateway before accessing the dashboard.
- **Registration Data:** When registering a new account, the app must collect: `Email`, `Password`, `Name`, `Gender`, and `Birthdate`.
- **Identity Display:** Across all screens (both Mobile and Watch), the app MUST display the wearer's **Name** (e.g., "Alex"), NEVER their raw email address.

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
- **Pattern Review Nudge:**
  - **Scenario A (Unlabeled ML Event):** The wearer ignored the prompt. The card shows "A stress spike was detected but [Name] didn't label it." The caregiver must select the wearer's internal state: `🌩️ Overwhelmed` | `✨ Excited` | `🏃 Active` | `🤷 Unsure / Wasn't Present`.
  - **Scenario B (Manual Report or Wearer-Labeled Event):** The wearer explicitly reported their state (e.g., "Overwhelmed"). The card shows "What was happening around [Name]?" The caregiver must select environmental context: `🔊 Loud Noises` | `👥 Crowded Space` | `📋 Task Demand` | `🔄 Routine Change` | `🤷 Unknown Context`.
  - Once the caregiver selects an option, the event is marked as resolved and moves to the Journal.

### 2.2 Navigation
- Utilize a `BottomNavigation` bar or `Scaffold` if extending beyond the dashboard (e.g., Settings, History).
- Current scope (v1) focuses entirely on the single Dashboard Screen.
