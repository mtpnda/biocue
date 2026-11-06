# 12. Push Notifications (FCM) & SOS GPS

> **Added by the merge, at the product owner's request.** Implements the requirement in 01 ("a caregiver receives a stress alert within 30 s of a Tier 2 meltdown") and 04 §2 ("the ONLY time a caregiver receives a notification is when a meltdown is detected — Auto Tier 2 or Manual SOS") and 07 ("GPS … shared only via push notification").

## 1. What is sent, to whom

| Trigger | Who is notified | Message | Location |
|---|---|---|---|
| Auto **Tier 2** (a new AUTO event opens) | every **approved** caregiver of that wearer | "**{Name}** may need support." | the one GPS fix, if the watch returned it in time (03: Tier 2 triggers the SOS GPS ping) |
| Manual **SOS** — the wearer reports **Overwhelmed** on the watch | same | "**{Name}** pressed SOS." | the one GPS fix, if returned in time |
| Manual event labelled **Excited** or **Active** | **nobody** (not a distress report) | — | — |
| Anything else (Tier 1, labels, edits, removals) | **nobody** | — | — |

Never in a push: heart rate, HRV, activity, event labels, or any health detail. The title uses the wearer's **name** (never an email — 04).
A 2-in-1 user (see 07/09) receives pushes only for people they **follow** (caregiver mode); their own events alert only on their own phone.

## 2. Architecture

```
wearer phone ──(callable)──► Cloud Function notifyMeltdown ──► FCM ──► caregiver phones (Android: FCM · iPhone: APNs via FCM)
   │ event + optional GPS            │ admin SDK reads: caller's mode, approved pairings, device tokens
   └ never writes GPS anywhere       └ planNotifications() — a PURE function (unit-tested)
```

* **Why a Cloud Function.** A client cannot be trusted to choose recipients, and Firestore rules cannot send messages. The function derives recipients itself from `pairings` (status `APPROVED`), so a modified client cannot push to strangers.
* **Callable**, authenticated. It rejects callers without wearer mode, events that are not the caller's own, events older than 2 minutes, a kind that does not match the event (`meltdown` ⇔ `AUTO`, `sos` ⇔ `MANUAL`), and a manual event that is not an *Overwhelmed* report.
  *Assumption to confirm:* "Manual SOS" (04) is taken to mean a manual event labelled **Overwhelmed**; the watch has no separate SOS button today.
* **De-duplication.** One push per (event, caregiver): the function records `wearerData/{uid}/pushLog/{eventId}` (server-only write) and skips repeats. The wearer's app also rate-limits its own calls (once per event).
* **Device tokens.** Each install registers `users/{uid}/devices/{installId}` = {`token`, `platform` `android|ios`, `updatedAt`} (owner read/write). The function deletes tokens FCM reports as invalid. Sign-out removes this device's token.
* **Requires** Firebase Blaze (Cloud Functions). Android 13+ needs `POST_NOTIFICATIONS`; iOS needs the Push Notifications capability, a APNs key uploaded in the Firebase console, and (for urgency) the Time Sensitive Notifications capability.
* **Latency budget (01: ≤ 30 s):** detection → app call → function → FCM. If the GPS fix is not back within **8 s** the push goes **without** location rather than waiting.

## 3. Message shape (data contract)

```
notification: { title: "Alex may need support.", body: "Open BioCue to check in." }      // SOS: "Alex pressed SOS."
data: { kind: "meltdown" | "sos", wearerUid, eventId, lat?, lng?, sentAt }
android: priority high · channel "alerts"        apns: priority 10 · interruption-level time-sensitive
```
Tapping opens the follower's dashboard for that wearer. `lat/lng` are present only when a fix was returned in time, only in the push, and are **never written to Firestore or Room**.

## 4. SOS GPS (spec 03, now working end to end)

1. Tier 2 or SOS on the wearer's phone → request **one** fix from the watch: Android Data Layer message `/sos_gps_request`; iPhone link `SOS_GPS_REQUEST` (already implemented).
2. The **watch** takes exactly one high-accuracy fix and replies once: Data Layer `/sos_location` DataItem; link `SOS_LOCATION`. (Continuous GPS stays forbidden — 01.)
3. The phone holds the coordinates **in memory only**, passes them to `notifyMeltdown`, and discards them.
4. The follower's app may show the 04 "SOS Location" card (latitude/longitude + "Open in Google Maps") from the push payload **in memory**; it is not available after the app restarts (by design — it was never stored).

*Defects fixed by this work:* the watch ignored `/sos_gps_request` (only `/ping` was handled) and the phone never consumed `/sos_location`.

## 5. Verification

| Part | Status |
|---|---|
| `planNotifications()` — recipients, content, no health detail, location rules, de-duplication, trust checks, platform urgency (11 tests) | **run**, test-first (`node --test`); mutation-checked |
| Cloud Function handler `handleNotifyMeltdown()` with injected Firebase boundaries — auth, happy path, idempotency, dead-token cleanup, bad coordinates, missing event/profile, transient failures (7 tests) | **run**, test-first; mutation-checked |
| `functions/index.js` (the shim wiring real Firestore/FCM), rules for `users/{uid}/devices` and the server-only push log, rules tests | written; syntax-checked; **not run** (no Firebase project/emulator) |
| **Android client**: SOS GPS reply on the watch, receiving `/sos_location`, GPS cooldown, removal of the fake address | written; parse-checked; **not built or run** |
| **Android + iPhone FCM clients**: device-token registration, `FirebaseMessagingService` / APNs setup, and the wearer app **calling `notifyMeltdown`** | **NOT WRITTEN YET** — until they exist no push is ever sent |
| iPhone GPS → push call | **not written** (the BLE GPS request/reply already exists) |
| End-to-end latency ≤ 30 s | **not measured** |
