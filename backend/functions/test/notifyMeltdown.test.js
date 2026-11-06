"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const { handleNotifyMeltdown } = require("../src/notifyMeltdown");

const NOW = 1_700_000_000_000;
// A fake of the Firebase boundary. It records what the handler did; it contains no logic of its own.
function fakeDeps(over = {}) {
  const calls = { sent: [], saved: [], deleted: [] };
  const deps = {
    calls,
    nowMs: () => NOW,
    getUser: async () => ({ name: "Alex", modes: ["WEARER"] }),
    getEvent: async () => ({ id: "e1", triggerType: "AUTO", label: "NONE", startTime: NOW - 3_000 }),
    getPairings: async () => [{ caregiverUid: "c1", status: "APPROVED" }, { caregiverUid: "c2", status: "PENDING" }],
    getDevices: async (uid) => ({ c1: [{ id: "d1", token: "tok-c1", platform: "android" }], c2: [{ id: "d2", token: "tok-c2", platform: "ios" }] }[uid] || []),
    getPushLog: async () => [],
    savePushLog: async (w, e, uids) => { calls.saved.push({ w, e, uids }); },
    sendEach: async (messages) => { calls.sent.push(...messages); return messages.map(() => ({ success: true })); },
    deleteDevice: async (uid, id) => { calls.deleted.push({ uid, id }); },
    ...over,
  };
  return deps;
}
const call = (data = {}, auth = { uid: "w1" }) => ({ auth, data: { kind: "meltdown", eventId: "e1", ...data } });

test("a call without a signed-in user is refused", async () => {
  await assert.rejects(handleNotifyMeltdown(call({}, null), fakeDeps()), /unauthenticated/);
});

test("an approved caregiver is pushed, the push is logged, and the result says how many went out", async () => {
  const deps = fakeDeps();
  const result = await handleNotifyMeltdown(call(), deps);
  assert.equal(result.sent, 1);
  assert.deepEqual(deps.calls.sent.map((m) => m.token), ["tok-c1"]);
  assert.deepEqual(deps.calls.saved, [{ w: "w1", e: "e1", uids: ["c1"] }]);
});

test("calling again for the same event pushes nobody twice", async () => {
  const deps = fakeDeps({ getPushLog: async () => ["c1"] });
  const result = await handleNotifyMeltdown(call(), deps);
  assert.equal(result.sent, 0);
  assert.deepEqual(deps.calls.sent, []);
});

test("a token FCM says is dead is deleted, and that caregiver is not recorded as notified", async () => {
  const deps = fakeDeps({ sendEach: async (m) => m.map(() => ({ success: false, error: { code: "messaging/registration-token-not-registered" } })) });
  const result = await handleNotifyMeltdown(call(), deps);
  assert.equal(result.sent, 0);
  assert.deepEqual(deps.calls.deleted, [{ uid: "c1", id: "d1" }]);
  assert.deepEqual(deps.calls.saved, [{ w: "w1", e: "e1", uids: [] }]);
});

test("nonsense coordinates are dropped but the alert still goes out", async () => {
  for (const bad of [{ lat: 200, lng: 5 }, { lat: NaN, lng: 5 }, { lat: "x", lng: 1 }]) {
    const deps = fakeDeps();
    const result = await handleNotifyMeltdown(call(bad), deps);
    assert.equal(result.sent, 1);
    assert.equal("lat" in deps.calls.sent[0].data, false);
  }
  const ok = fakeDeps(); await handleNotifyMeltdown(call({ lat: 37.77, lng: -122.41 }), ok);
  assert.equal(ok.calls.sent[0].data.lat, "37.77");
});

test("a missing profile or event is refused and nothing is sent", async () => {
  const noEvent = fakeDeps({ getEvent: async () => null });
  await assert.rejects(handleNotifyMeltdown(call(), noEvent), /event-not-found/);
  const noUser = fakeDeps({ getUser: async () => null });
  await assert.rejects(handleNotifyMeltdown(call(), noUser), /no-profile/);
  assert.deepEqual(noEvent.calls.sent.concat(noUser.calls.sent), []);
});

test("a transient send failure keeps the token and leaves the caregiver eligible for a retry", async () => {
  const deps = fakeDeps({ sendEach: async (m) => m.map(() => ({ success: false, error: { code: "messaging/internal-error" } })) });
  const result = await handleNotifyMeltdown(call(), deps);
  assert.equal(result.sent, 0);
  assert.deepEqual(deps.calls.deleted, []);
  assert.deepEqual(deps.calls.saved, [{ w: "w1", e: "e1", uids: [] }]);
});
