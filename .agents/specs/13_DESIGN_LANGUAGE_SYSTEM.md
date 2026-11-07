# 13. BioCue Design Language System (DLS) & Component Standard

> **Canonical Specification.** Single source of truth for all visual tokens, typographic hierarchies, color contrast constraints, and UI component contracts across **iOS (SwiftUI)**, **Android (Jetpack Compose)**, and **Web (Deno Edge / SSR)**.  
> **Source Token File:** `design/dls.tokens.json`  
> **Generator Script:** `misc/tools/gen_dls.py`

---

## 1. Core Principles & Anti-Slop Visual Philosophy

1. **Strict Invariant: Zero Light Text on Light Backgrounds**:
   - The primary theme is a warm, paper-textured organic surface (`#F2F0EA` / `#FCFBF8`).
   - Text rendered on any surface, card, or soft-tinted pill must **NEVER** use light grey, pastel ink, or unstyled system `.primary` (which flips to white in Dark Mode).
   - Every single `Text` element must explicitly bind to `Palette.inkPrimary`, `Palette.inkSecondary`, or an authorized high-contrast state ink.
   - iOS applications must enforce `.preferredColorScheme(.light)` to prevent iOS Dark Mode from turning unstyled headers white against light cards.

2. **Contrast Targets (WCAG AAA & AA+)**:
   - **Body & Headline Text (`text.primary`)**: Minimum **13:1** contrast ratio against all background and card surfaces.
   - **Secondary & Caption Text (`text.secondary`)**: Minimum **8:1** contrast ratio (far exceeding WCAG AA 4.5:1).
   - **Muted Text / Badges (`text.muted`)**: Minimum **5.5:1** contrast ratio (exceeding WCAG AA 4.5:1). Never use faint, unreadable grey.
   - **State Inks (`state.*.ink`)**: Minimum **6.0:1** contrast ratio against their corresponding `.soft` tinted pill backgrounds.

3. **Zero Anxiety & Sensory Calm**:
   - High stress or spike alerts use **warm coral (`#9C2A22`)**, **NEVER** aggressive, hyper-stimulating emergency red.
   - Transitions are smooth, physical, and restrained ($150\text{ms} - 250\text{ms}$, cubic-bezier). No looping, frantic, or distracting decorative animations.

4. **Deterministic Token Compilation**:
   - `design/dls.tokens.json` is the **only** place hex colors, sizes, radii, or fonts are declared.
   - Running `python3 misc/tools/gen_dls.py --theme calm` deterministically emits:
     - `design/dls.html` (Living visual catalog)
     - `android/mobile/src/main/java/com/mtpnda/biocue/ui/theme/DlsTokens.kt`
     - `ios/BioCue/Sources/Design/DlsTokens.swift`

---

## 2. Master Palette & Verified Semantic Token Matrix

### 2.1 Surfaces & Inks (Calm Theme)

| Token Key | Hex Value | SwiftUI Binding | Compose Binding | Purpose & Contrast Guarantee |
| :--- | :--- | :--- | :--- | :--- |
| `surface.bg` | `#F2F0EA` | `Palette.bg` | `DlsTokens.surfaceBg` | Full-screen organic warm canvas. |
| `surface.card` | `#FCFBF8` | `Palette.card` | `DlsTokens.surfaceCard` | Elevated card container. |
| `surface.raised`| `#F7F4EE` | `Palette.surface2`| `DlsTokens.surfaceRaised`| Inputs, badges, inner groupings. |
| `border.subtle`| `#E7E2D9` | `Palette.line` | `DlsTokens.borderSubtle` | 1pt card borders and dividers. |
| `border.strong`| `#DAD4C8` | `Palette.lineStrong`| `DlsTokens.borderStrong`| Outlined button borders (1.5pt). |
| `text.primary` | `#191612` | `Palette.inkPrimary`| `DlsTokens.textPrimary` | **15.2:1 contrast** on cards; titles & body. |
| `text.secondary`| `#3D362C`| `Palette.inkSecondary`| `DlsTokens.textSecondary`| **9.2:1 contrast**; subtitles & helper copy. |
| `text.muted`   | `#544B3E` | `Palette.inkMuted` | `DlsTokens.textMuted` | **6.4:1 contrast**; timestamps & tags. |
| `action.primary`| `#1C1814`| `Palette.charcoal` | `DlsTokens.actionPrimary`| Primary button background & dark crests. |
| `action.onPrimary`| `#FBF6EC`| `Palette.onCharcoal`| `DlsTokens.actionOnPrimary`| High-contrast cream text on dark pills. |

### 2.2 Emotional State Triples (`chip`, `soft`, `ink`)

| State | Chip (Indicator) | Soft (Background) | Ink (Text/Icon) | Contrast on Soft | Meaning |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Calm** | `#86EFAC` | `#DEF7E5` | `#1A6337` | **7.1:1 (AAA)** | Tier 0 resting baseline, connected. |
| **Rising** | `#FCD34D` | `#FEF4D6` | `#874A00` | **6.4:1 (AAA)** | Tier 1 sensory activation, pacing. |
| **Alert** | `#FCA5A5` | `#FBE3E1` | `#9C2A22` | **6.7:1 (AAA)** | Tier 2 support spike, SOS (Coral). |
| **Info** | `#93C5FD` | `#DDEAFB` | `#1B4F9C` | **6.5:1 (AAA)** | Requests, invitations, Bluetooth. |
| **Neutral** | `#D1D5DB` | `#EDEEF0` | `#383C44` | **7.4:1 (AAA)** | Disconnected, inactive, standbys. |

---

## 3. Typography Scale & Hierarchy

| Role | Font Family | Size (pt/sp) | Weight | Line Height | Usage Contract |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `displayMedium` | Newsreader (Serif) | 34 | 500 Medium | 40 | Large hero headings (BioCue splash). |
| `headlineLarge` | Newsreader (Serif) | 28 | 500 Medium | 34 | Section titles ("My Circle", "Join Circle"). |
| `headlineSmall` | Newsreader (Serif) | 23 | 500 Medium | 29 | Sheet headers ("Profile", "Access removed"). |
| `titleLarge` | Newsreader (Serif) | 22 | 500 Medium | 28 | Card headers, prominent statuses. |
| `titleMedium` | Atkinson (Sans) | 17 | 700 Bold | 24 | Names, card row titles, bold labels. |
| `bodyLarge` | Atkinson (Sans) | 15 | 400 Regular | 22 | Primary body copy, explanatory text. |
| `bodyMedium` | Atkinson (Sans) | 14 | 400 Regular | 20 | Secondary descriptions, multi-line instructions. |
| `bodySmall` | Atkinson (Sans) | 13 | 600 SemiBold| 18 | Captions, metadata, helper text. |
| `labelLarge` | Atkinson (Sans) | 15.5 | 700 Bold | 20 | Button text, interactive triggers. |
| `labelMedium` | Atkinson (Sans) | 13 | 700 Bold | 18 | Badge text, chip tags, step numbers. |
| `labelSmall` | Atkinson (Sans) | 11.5 | 700 Bold | 16 | Tracked uppercase headers (`+0.5pt`). |

---

## 4. Comprehensive Component Catalog & State Contracts

The following 18 components form the complete, closed BioCue design library. Developers and AI agents **MUST NOT** invent custom ad-hoc layouts or guess styles.

### 4.1 Surface & Containers

1. **`calmBackground()`**:
   - Background: `Palette.bg` (`#F2F0EA`).
   - Default Ink: `Palette.inkPrimary` (`#191612`).
   - Behavior: Covers `ignoresSafeArea()`. Ensures no text child can ever default to unstyled white.

2. **`SoftCard<Content>`**:
   - Container: `Palette.card` (`#FCFBF8`).
   - Border: 1pt `Palette.line` (`#E7E2D9`).
   - Radius: 20pt (`Radius.card`).
   - Padding: 18pt default (customizable).
   - Shadow: Warm soft shadow (`DlsTokens.shadowTint.opacity(0.10)`, radius 4, y 2).
   - Guaranteed Child Text: `Palette.inkPrimary`.

### 4.2 Buttons & Actions (Minimum Touch Target: 48pt)

3. **`PillButton` (Primary Action)**:
   - Height: 52pt (`Metrics.buttonHeight`), Full Width.
   - Background: `Palette.charcoal` (`#1C1814`) in `Capsule()`.
   - Text: `Palette.onCharcoal` (`#FBF6EC`), `AppFont.labelLarge`.
   - Disabled State: Background `Palette.line`, text `Palette.inkMuted`.
   - Loading State: Circular progress spinner tinted `Palette.onCharcoal`.

4. **`OutlinedPillButton` (Secondary Action)**:
   - Height: 52pt, Full Width.
   - Background: `Palette.card` (`#FCFBF8`) in `Capsule()`.
   - Border: 1.5pt `Palette.lineStrong` (`#DAD4C8`).
   - Text: `Palette.inkPrimary` (`#191612`), `AppFont.labelLarge`.

5. **`CoralOutlinedPillButton` (Destructive Action)**:
   - Height: 52pt, Full Width.
   - Background: `Palette.card` in `Capsule()`.
   - Border: 1.5pt `StatePalette.alert.chip` (`#FCA5A5`).
   - Text: `StatePalette.alert.ink` (`#9C2A22`), `AppFont.labelLarge`.

6. **`GhostButton` (Tertiary Action)**:
   - Min Height: 48pt (`Metrics.minTouch`), Centered or leading.
   - Background: Transparent.
   - Text: `Palette.inkSecondary` (`#3D362C`), `AppFont.bodySmall`.

7. **`CircleIconButton` (Icon Tap Target)**:
   - Hit Target: $48 \times 48\text{pt}$ (`Metrics.minTouch`).
   - Visual Circle: $40 \times 40\text{pt}$, background `Palette.surface2`, border 1pt `Palette.line`.
   - Icon: 16pt semi-bold, tinted `Palette.inkPrimary`.

### 4.3 Badges, Avatars & Tags

8. **`StatusPill`**:
   - Padding: $12\text{pt horizontal} \times 7\text{pt vertical}$ in `Capsule()`.
   - Background: `palette.soft`. Border: 1pt `palette.chip.opacity(0.6)`.
   - Dot Indicator: 8pt circle filled with `palette.chip`.
   - Text: `palette.ink` (or explicit override), `AppFont.labelMedium`.

9. **`EmojiAvatar`**:
   - Diameter: 40pt default (44pt for list items, 72–88pt for heroes).
   - Background: `Palette.surface2`. Border: 1pt `Palette.line`.
   - Text size: 45% of avatar diameter.

10. **`SectionHeader`**:
    - Style: Uppercase, tracking `+0.5pt`, `AppFont.labelSmall`.
    - Color: `Palette.inkSecondary` (`#3D362C`).
    - Spacing: 8pt top, 4pt leading padding.

### 4.4 Data Inputs & Pairing

11. **`CalmTextField`**:
    - Min Height: 56pt.
    - Background: `Palette.card`, border 1pt `Palette.line`, radius 16pt (`Radius.md`).
    - Input Font: `AppFont.bodyLarge`.
    - Input Text: `Palette.inkPrimary` (`#191612`).

12. **`PairingCodeView`**:
    - Typography: Newsreader Serif (40pt), tracking 6pt.
    - Container: `Palette.surface2`, radius 16pt, border 1pt `Palette.line`.
    - Ink: `Palette.inkPrimary` when populated, `Palette.inkMuted` when empty.
    - Accessibility: Screen reader announces digits spaced ("1 2 3 4 5 6").

13. **`InlineNotice`**:
    - Container: `tone.soft`, border 1pt `tone.chip`, radius 16pt.
    - Text: `Palette.inkPrimary`, `AppFont.bodyMedium`.
    - Padding: 14pt.

### 4.5 Domain Cards & Layout Modules

14. **`WatchLinkCard`**:
    - Header: 44pt EmojiAvatar ("⌚"), title (`AppFont.titleMedium`, `inkPrimary`), subtitle (`inkSecondary`).
    - Steps (when disconnected): Numbered instruction steps using `StepRow`.
    - Action: `PillButton("Connect watch")` or `OutlinedPillButton("Forget this watch")`.

15. **`StatusMeterCard`**:
    - Header: Emotion state title (`AppFont.titleLarge`, `palette.ink`).
    - Progress Bar: 12pt capsule container with animated filling capsule.
    - Activity Pill: Current activity label + "Updated just now" timestamp.

16. **`SosAlertCard`**:
    - Container: `StatePalette.alert.soft`, border `StatePalette.alert.chip`.
    - Header: "High stress alert" (`StatePalette.alert.ink`).
    - Description: Multi-line advice text (`Palette.inkPrimary`).

17. **`StepRow`**:
    - Step Badge: $24 \times 24\text{pt}$ circle, card background, border `line`.
    - Text: `Palette.inkPrimary`, `AppFont.bodyMedium`.

18. **`SettingsRow`**:
    - Avatar: 44pt EmojiAvatar.
    - Text: Title (`titleMedium`, `inkPrimary`) + optional Subtitle (`bodySmall`, `inkSecondary`).
    - Trailing: Chevron icon (`chevron.right`, `inkSecondary`).
    - Min Height: 64pt, full width.

19. **`BioCueBuddyCard` (Empathetic Co-Regulation)**:
    - Container: `StatePalette.info.soft` or `Palette.card`, border `Palette.line`.
    - Avatar: Botanical or soft avatar (44pt).
    - Empathetic Copy: Atkinson Regular (14.5pt), `Palette.inkPrimary`.
    - Action Strip: Somatic breathing pacer trigger + free-text reflection shortcut button.

20. **`BreathingPacerCard` (Decoupled Non-Overflowing Pacer)**:
    - Pulsing Orb: Dynamic animated scale ($0.85 \leftrightarrow 1.25$) in `StatePalette.calm.soft` with `StatePalette.calm.chip` outline.
    - Guidance Text Anchoring: Stably positioned in a separate, fixed-height container **below** the visual orb. Never superimposed inside the expanding orb.
    - Guarantee: Zero text clipping, zero container overflow across all viewport widths.

21. **`JournalCard` (Decomposed Event List)**:
    - Date Header: Section title with previous/next chevron buttons.
    - Label Decomposition: Splits composite labels into an emotion title (`Overwhelmed`, `Excited`) and context badges.
    - Context Badge Token: `RoundedCornerShape(6.dp)` / `cornerRadius: 6`, `Palette.surface2` fill, 1pt `Palette.line` border. Strictly prohibits 50% radius capsules on multi-line text to prevent oval distortion.

22. **`FreeTextCheckInSheet` (Sensory Overload Reflection)**:
    - Preset State Chips: Quick-tap buttons for rapid labeling during reduced executive function.
    - Open-Ended Field: Multi-line text field (`CalmTextField`) with placeholder encouraging open expression of sensory environment (lighting, noise, internal sensation).

---

## 5. Mindful Contours Brand & Icon Motif

The canonical visual brand of BioCue is the **Mindful Contours** emblem:

1. **Backdrop:** Deep matte charcoal squircle / canvas (`#1C1C1E`).
2. **Concentric Contours:** Three nested, organic topographic curves rendered with smooth variable weight ($1.5\text{pt} - 2.5\text{pt}$):
   - Outer contour: Warm Oat (`#F2F0EA`, 45% opacity).
   - Middle contour: Subtle Sage (`#D1D5DB`, 75% opacity).
   - Inner contour: Focused contour framing the center core (`#F2F0EA`, 95% opacity).
3. **Mindfulness Anchor Bead:** A single, perfectly centered circular bead ($10\text{pt}$ diameter) filled with Sage Green (`#4E7252`) and a soft ambient glow, symbolizing grounded autonomic awareness.
4. **Usage:** Enforced across all app launcher icons (Wear OS, Android Mobile, iOS), web favicon (`/favicon.svg`), website headers, and Google Play Store store graphics.

---

## 6. Visual Regression & Quality Checklist

Before committing or releasing any UI code, verify against the following checklist:

- [x] **No unstyled Text**: Every `Text(...)` must have `.foregroundStyle(...)` or be enclosed in a component that enforces `Palette.inkPrimary`.
- [x] **Light Mode Locking**: Root application sets `.preferredColorScheme(.light)`.
- [x] **Touch Targets**: All interactive elements have $\ge 48\text{pt}$ hit areas (`Metrics.minTouch`).
- [x] **WCAG Contrast**: Automated verification via `python3 misc/tools/gen_dls.py --theme calm` must return **0 errors and 0 warnings**.
- [x] **Strict DLS Token Usage**: No arbitrary hex strings in views; only semantic `Palette` or `StatePalette` references.
