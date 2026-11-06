"use strict";
// The Cloud Function's logic with every Firebase call INJECTED (deps) so it is testable with plain fakes.
// index.js wires the real Firestore / FCM into `deps`. Contract: docs/spec/12_PUSH_AND_GPS.md
const { planNotifications } = require("./planNotifications");

function cleanLocation(lat, lng) {
  const ok = typeof lat === "number" && typeof lng === "number" && Number.isFinite(lat) && Number.isFinite(lng)
    && Math.abs(lat) <= 90 && Math.abs(lng) <= 180;
  return ok ? { lat, lng } : null;
}

async function handleNotifyMeltdown(req, deps) {
  if (!req.auth) throw new Error("unauthenticated");
  const uid = req.auth.uid;
  const { kind, eventId } = req.data;
  const user = await deps.getUser(uid);
  if (!user) throw new Error("no-profile");
  const event = await deps.getEvent(uid, eventId);
  if (!event) throw new Error("event-not-found");
  const pairings = await deps.getPairings(uid);
  const alreadyPushedTo = await deps.getPushLog(uid, eventId);
  const devicesByUid = {};
  for (const p of pairings) if (p.status === "APPROVED") devicesByUid[p.caregiverUid] = await deps.getDevices(p.caregiverUid);
  const plan = planNotifications({ kind, wearer: { uid, name: user.name, modes: user.modes }, event, nowMs: deps.nowMs(),
    location: cleanLocation(req.data.lat, req.data.lng), alreadyPushedTo, pairings, devicesByUid });
  const results = await deps.sendEach(plan.messages);
  const notified = new Set(alreadyPushedTo);
  const DEAD = ["messaging/registration-token-not-registered", "messaging/invalid-registration-token"];
  for (let i = 0; i < plan.recipients.length; i++) {
    const r = plan.recipients[i];
    if (!results[i].success && results[i].error && DEAD.includes(results[i].error.code)) {
      const device = (devicesByUid[r.caregiverUid] || []).find((d) => d.token === r.token);
      if (device) await deps.deleteDevice(r.caregiverUid, device.id);
    }
  }
  plan.recipients.forEach((r, i) => { if (results[i].success) notified.add(r.caregiverUid); });
  await deps.savePushLog(uid, eventId, [...notified]);
  return { sent: results.filter((r) => r.success).length, skipped: plan.skipped };
}
module.exports = { handleNotifyMeltdown };
