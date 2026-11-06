"use strict";
// Thin shim: wires the real Firebase services into the (unit-tested) handler. NOT run here — no Firebase project/emulator.
// Deploy:  firebase deploy --only functions      (requires the Blaze plan)   Contract: docs/spec/12_PUSH_AND_GPS.md
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const { handleNotifyMeltdown } = require("./src/notifyMeltdown");

admin.initializeApp();
const db = admin.firestore();

const deps = {
  nowMs: () => Date.now(),
  getUser: async (uid) => {
    const d = await db.doc(`users/${uid}`).get();
    if (!d.exists) return null;
    const u = d.data();
    // 2-in-1: `modes` (array). Accounts created before that carry a single `role`.
    return { name: u.displayName || "Someone", modes: Array.isArray(u.modes) ? u.modes : (u.role ? [u.role] : []) };
  },
  getEvent: async (uid, id) => { const d = await db.doc(`wearerData/${uid}/events/${id}`).get(); return d.exists ? d.data() : null; },
  getPairings: async (uid) => (await db.collection("pairings").where("wearerUid", "==", uid).get()).docs.map((d) => d.data()),
  getDevices: async (uid) => (await db.collection(`users/${uid}/devices`).get()).docs.map((d) => ({ id: d.id, ...d.data() })),
  getPushLog: async (w, e) => { const d = await db.doc(`wearerData/${w}/pushLog/${e}`).get(); return d.exists ? d.data().caregivers || [] : []; },
  savePushLog: (w, e, uids) => db.doc(`wearerData/${w}/pushLog/${e}`).set({ caregivers: uids, at: Date.now() }),
  sendEach: async (messages) => {
    if (messages.length === 0) return [];
    const res = await admin.messaging().sendEach(messages);
    return res.responses.map((r) => ({ success: r.success, error: r.error ? { code: r.error.code } : undefined }));
  },
  deleteDevice: (uid, id) => db.doc(`users/${uid}/devices/${id}`).delete(),
};

exports.notifyMeltdown = onCall(async (request) => {
  try {
    return await handleNotifyMeltdown({ auth: request.auth, data: request.data || {} }, deps);
  } catch (e) {
    const map = { unauthenticated: "unauthenticated", "not-a-wearer": "permission-denied", "event-not-found": "not-found", "no-profile": "failed-precondition",
      "bad-kind": "invalid-argument", "event-mismatch": "invalid-argument", "event-too-old": "failed-precondition", "not-distress": "failed-precondition" };
    throw new HttpsError(map[e.message] || "internal", e.message);
  }
});
