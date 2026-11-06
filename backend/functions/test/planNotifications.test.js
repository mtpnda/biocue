"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const { planNotifications } = require("../src/planNotifications");

const NOW = 1_700_000_000_000;
const wearer = { uid: "w1", name: "Alex", modes: ["WEARER"] };
const event = (over = {}) => ({ id: "e1", triggerType: "AUTO", startTime: NOW - 5_000, ...over });
const pairing = (cg, status) => ({ caregiverUid: cg, status });
const phone = (t, platform = "android") => ({ token: t, platform });
const base = (over = {}) => ({
  kind: "meltdown", wearer, event: event(), nowMs: NOW, location: null, alreadyPushedTo: [],
  pairings: [pairing("c1", "APPROVED")], devicesByUid: { c1: [phone("tok-c1")] }, ...over,
});

test("only APPROVED caregivers are pushed; pending and removed are not", () => {
  const plan = planNotifications(base({
    pairings: [pairing("c1", "APPROVED"), pairing("c2", "PENDING"), pairing("c3", "REMOVED")],
    devicesByUid: { c1: [phone("t1")], c2: [phone("t2")], c3: [phone("t3")] },
  }));
  assert.deepEqual(plan.messages.map((m) => m.token), ["t1"]);
});

test("the message names the wearer and carries no health detail", () => {
  const [m] = planNotifications(base()).messages;
  assert.equal(m.notification.title, "Alex may need support.");
  const everything = JSON.stringify(m).toLowerCase();
  for (const forbidden of ["heart", "hrv", "bpm", "rmssd", "stress level", "overwhelmed", "label"]) {
    assert.equal(everything.includes(forbidden), false, `must not mention '${forbidden}'`);
  }
});

test("an SOS says so; BOTH kinds carry the one GPS fix (spec 03/04), and only when a fix exists", () => {
  const sos = planNotifications(base({ kind: "sos", event: event({ triggerType: "MANUAL", label: "OVERWHELMED" }), location: { lat: 37.77, lng: -122.41 } })).messages[0];
  assert.equal(sos.notification.title, "Alex pressed SOS.");
  assert.equal(sos.data.lat, "37.77"); assert.equal(sos.data.lng, "-122.41");
  const melt = planNotifications(base({ location: { lat: 1, lng: 2 } })).messages[0];
  assert.equal(melt.data.lat, "1"); assert.equal(melt.data.lng, "2");
  const none = planNotifications(base()).messages[0];
  assert.equal("lat" in none.data, false); assert.equal("lng" in none.data, false);
});

test("a caregiver with two phones gets two messages; one with none is reported as skipped", () => {
  const plan = planNotifications(base({
    pairings: [pairing("c1", "APPROVED"), pairing("c2", "APPROVED")],
    devicesByUid: { c1: [phone("a"), phone("b", "ios")], c2: [] },
  }));
  assert.deepEqual(plan.messages.map((m) => m.token), ["a", "b"]);
  assert.deepEqual(plan.skipped, [{ caregiverUid: "c2", reason: "no-device" }]);
});

test("a caregiver already pushed for this event is not pushed again", () => {
  const plan = planNotifications(base({ alreadyPushedTo: ["c1"] }));
  assert.deepEqual(plan.messages, []);
  assert.deepEqual(plan.skipped, [{ caregiverUid: "c1", reason: "already-notified" }]);
});

test("a user without wearer mode cannot trigger a push", () => {
  assert.throws(() => planNotifications(base({ wearer: { uid: "w1", name: "Alex", modes: ["CAREGIVER"] } })), /not-a-wearer/);
});

test("the event must match the kind and be recent: no pushing for old or mismatched events", () => {
  assert.throws(() => planNotifications(base({ kind: "meltdown", event: event({ triggerType: "MANUAL" }) })), /event-mismatch/);
  assert.throws(() => planNotifications(base({ kind: "sos", event: event({ triggerType: "AUTO" }) })), /event-mismatch/);
  assert.throws(() => planNotifications(base({ event: event({ startTime: NOW - 10 * 60_000 }) })), /event-too-old/);
  assert.throws(() => planNotifications(base({ kind: "digest" })), /bad-kind/);
});

test("2-in-1: a pairing to the wearer themselves is never pushed", () => {
  const plan = planNotifications(base({ pairings: [pairing("w1", "APPROVED")], devicesByUid: { w1: [phone("mine")] } }));
  assert.deepEqual(plan.messages, []);
  assert.deepEqual(plan.skipped, [{ caregiverUid: "w1", reason: "self" }]);
});

test("data values are all strings (FCM requires it) and urgency is set per platform", () => {
  const a = planNotifications(base({ kind: "sos", event: event({ triggerType: "MANUAL", label: "OVERWHELMED" }), location: { lat: 1.5, lng: 2 },
    devicesByUid: { c1: [phone("a", "android"), phone("i", "ios")] } })).messages;
  for (const m of a) for (const v of Object.values(m.data)) assert.equal(typeof v, "string");
  assert.equal(a[0].android.priority, "high"); assert.equal(a[0].android.notification.channelId, "alerts");
  assert.equal(a[1].apns.headers["apns-priority"], "10");
  assert.equal(a[1].apns.payload.aps["interruption-level"], "time-sensitive");
  assert.equal("android" in a[1], false); assert.equal("apns" in a[0], false);
});

test("a manual push is only for a distress report: choosing Excited or Active alerts nobody", () => {
  for (const label of ["EXCITED", "ACTIVE", "NONE"]) {
    assert.throws(() => planNotifications(base({ kind: "sos", event: event({ triggerType: "MANUAL", label }) })), /not-distress/);
  }
});

test("recipients line up with messages so the caller knows which caregiver each one is for", () => {
  const plan = planNotifications(base({ pairings: [pairing("c1", "APPROVED"), pairing("c2", "APPROVED")],
    devicesByUid: { c1: [phone("a"), phone("b")], c2: [phone("c")] } }));
  assert.deepEqual(plan.recipients, [{ caregiverUid: "c1", token: "a" }, { caregiverUid: "c1", token: "b" }, { caregiverUid: "c2", token: "c" }]);
  assert.equal(plan.recipients.length, plan.messages.length);
});
