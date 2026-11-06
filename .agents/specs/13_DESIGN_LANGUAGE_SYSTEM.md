# 13. Design Language System (DLS)

> **Added by the merge, at the product owner's request.** One centralized design language that both platforms and the HTML reference are generated from, so the app can be **reskinned** by editing one file.

## 1. Principle

`design/dls.tokens.json` is the **only** place a colour, size, radius, shadow, motion value or font name is written down. Everything else is derived:

```
design/dls.tokens.json ──► tools/gen_dls.py ──┬─► design/dls.html                      (the living reference page)
   (themes: calm, high-contrast, …)           ├─► Android  DesignTokens.kt             (generated, do not edit)
                                              └─► iPhone   DesignTokens.swift          (generated, do not edit)
```

* Components consume **semantic** tokens (`surface.card`, `text.primary`, `action.primary`, `state.rising.ink`) — never raw hex and never a palette name like "charcoal".
* A **theme** is a complete mapping of every semantic token. Adding a skin = adding a theme; no component changes.
* The generator **fails** if a theme leaves a token undefined, if an alias points nowhere, or if a required text/background pair misses its contrast target (WCAG AA: 4.5:1 body text, 3:1 large text and UI shapes).

## 2. Layers

| Layer | Contents |
|---|---|
| **Primitives** (`palette`) | raw colours (`oat-100`, `ink-900`, `sage-300`…) — never used directly by components |
| **Semantic tokens** (`themes.<name>`) | `surface.*`, `border.*`, `text.*`, `action.*`, `state.{calm,rising,alert,info,neutral}.{chip,soft,ink}` |
| **Scales** | radius, spacing, size (touch target 48), elevation (warm tinted shadow), motion (duration + easing), typography roles |
| **Components** | documented in `dls.html`: SoftCard, PillButton, OutlinedPillButton, CoralOutlinedPillButton, CircleIconButton (40 visual / 48 tap), StatusPill, EmojiAvatar, CalmTextField, PairingCode, SectionHeader, InlineNotice, StatusMeterCard, JournalCard, ReviewCard, SosAlertCard |

## 3. Rules the system encodes (from the UI refactor and 04)

* Calm and zero-anxiety: no live vitals shown; colour carries meaning only (calm / rising / support); support state is **coral, never alarming red**.
* Touch targets ≥ **48** dp/pt; visual circle buttons are 40 inside a 48 hit area.
* Fonts: **Newsreader** (titles) and **Atkinson Hyperlegible Next** (body); both are variable fonts — weights are declared, never faux-bolded; platforms fall back to system serif/sans if a font cannot load.
* Identity is always the **name**, never an email (04).
* Motion is short and eased; no looping decoration.

## 4. Reskinning

1. Copy a theme in `dls.tokens.json`, change values, run `python3 tools/gen_dls.py --theme <name>`.
2. Open `design/dls.html` — switch themes live to check every component and the contrast table.
3. Rebuild the apps. No component code changes.

## 5. Status

| Part | Status |
|---|---|
| `design/dls.tokens.json` (40 palette entries, 2 themes × 26 semantic tokens, scales, typography, 17 contrast rules) | written |
| `tools/gen_dls.py` | **17 tests run**, test-first: alias/hex/missing-token errors, WCAG contrast, Kotlin + Swift + HTML emitters, generate / check / refuse-on-failing-contrast |
| `design/dls.html` | generated (41 KB, self-contained, 15 components, contrast tables per theme, typography, scales, rules, theme switcher). Structure is tested; **I could not open it in a browser here, so it has not been visually reviewed** |
| Generated `DlsTokens.kt` / `DlsTokens.swift` | generated; Kotlin parses; **not compiled** (no SDK / Swift toolchain) |
| **Bridge (done):** `Color.kt` (25 colours), `Tokens.kt` (shadow), `Theme.swift` (15), `Components.swift` (shadow) now **alias the generated tokens** with the same names, so no component changed; values are identical to the `calm` theme | written; parse-checked; **not built** |
| **Guard (done, run):** a test fails if any hand-written UI file contains a colour constructor (`Color(0x…)`, `Color(hex:)`, `Color(.sRGB`) — mutation-checked on both platforms | enforces the reskin property |
| **Still hand-written (not yet bridged):** radius / spacing / size / motion constants (`Tokens.kt`, `Theme.swift`), typography (`Type.kt`), fonts | open — same mechanical bridge; the generated `DlsScales` / `DlsType` already exist |
| **Known gap:** event-label colours are `#rrggbb` **strings** in core `EventTypes` (also Swift, and in the parity fixtures). The DLS-correct design is a semantic *tone* mapped by the UI; changing it touches the parity fixtures | open |
| The only warning: calm `text.muted` on `surface.card` is **2.87:1** (< 3.0). It is documented as decorative-only; changing it is a design call | open — your decision |
