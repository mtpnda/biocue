"use strict";
// PURE: decides who is pushed, with what, and what is never included. No Firebase imports -> unit-testable here.
// Contract: docs/spec/12_PUSH_AND_GPS.md
function planNotifications(input) {
  if (!(input.wearer.modes || []).includes("WEARER")) throw new Error("not-a-wearer");
  if (input.kind !== "meltdown" && input.kind !== "sos") throw new Error("bad-kind");
  if ((input.kind === "meltdown") !== (input.event.triggerType === "AUTO")) throw new Error("event-mismatch");
  if (input.nowMs - input.event.startTime > 120_000) throw new Error("event-too-old");
  if (input.kind === "sos" && !String(input.event.label || "").startsWith("OVERWHELMED")) throw new Error("not-distress");
  const messages = [];
  const skipped = [];
  const recipients = [];
  for (const p of input.pairings) {
    if (p.status !== "APPROVED") continue;
    if (p.caregiverUid === input.wearer.uid) { skipped.push({ caregiverUid: p.caregiverUid, reason: "self" }); continue; }
    if ((input.alreadyPushedTo || []).includes(p.caregiverUid)) { skipped.push({ caregiverUid: p.caregiverUid, reason: "already-notified" }); continue; }
    const devices = input.devicesByUid[p.caregiverUid] || [];
    if (devices.length === 0) { skipped.push({ caregiverUid: p.caregiverUid, reason: "no-device" }); continue; }
    for (const d of devices) {
      recipients.push({ caregiverUid: p.caregiverUid, token: d.token });
      const sos = input.kind === "sos";
      const data = { kind: input.kind, wearerUid: input.wearer.uid, eventId: input.event.id, sentAt: String(input.nowMs) };
      if (input.location) { data.lat = String(input.location.lat); data.lng = String(input.location.lng); }
      messages.push({
        token: d.token,
        notification: { title: sos ? `${input.wearer.name} pressed SOS.` : `${input.wearer.name} may need support.`, body: "Open BioCue to check in." },
        data,
        ...(d.platform === "ios"
          ? { apns: { headers: { "apns-priority": "10" }, payload: { aps: { "interruption-level": "time-sensitive", sound: "default" } } } }
          : { android: { priority: "high", notification: { channelId: "alerts" } } }),
      });
    }
  }
  return { messages, skipped, recipients };
}
module.exports = { planNotifications };
