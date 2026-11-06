// Run:  cd firestore-rules-tests && npm i && npm test      (needs Java for the Firestore emulator)
// Encodes docs/spec/07_MULTI_ACCOUNT.md as amended. WRITTEN BUT NOT EXECUTED by the author — run before deploying.
const fs = require("fs");
const path = require("path");
const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { doc, setDoc, getDoc, updateDoc, deleteDoc, getDocs, collection, deleteField } = require("firebase/firestore");

let env;
const W = "wearer1", C = "care1", C2 = "care2", W2 = "wearer2";
const ev = (id) => ({ id, startTime: 1, endTime: null, triggerType: "AUTO", label: "NONE", isResolved: false });
const NOW = () => Date.now();

beforeAll(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-biocue",
    firestore: { rules: fs.readFileSync(path.join(__dirname, "../firestore.rules"), "utf8") },
  });
});
afterAll(() => env.cleanup());

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, "users", W), { uid: W, role: "WEARER", displayName: "W" });
    await setDoc(doc(db, "users", W2), { uid: W2, role: "WEARER", displayName: "W2" });
    await setDoc(doc(db, "users", C), { uid: C, role: "CAREGIVER", displayName: "C" });
    await setDoc(doc(db, "users", C2), { uid: C2, role: "CAREGIVER", displayName: "C2" });
    await setDoc(doc(db, "pairings", `${W}_${C}`), { id: `${W}_${C}`, wearerUid: W, caregiverUid: C, status: "APPROVED", createdAt: 1 });
    await setDoc(doc(db, "pairings", `${W}_${C2}`), { id: `${W}_${C2}`, wearerUid: W, caregiverUid: C2, status: "PENDING", createdAt: 1 });
    await setDoc(doc(db, "wearerData", W, "events", "e1"), ev("e1"));
  });
});

const as = (uid) => env.authenticatedContext(uid).firestore();
const anon = () => env.unauthenticatedContext().firestore();
const seed = (fn) => env.withSecurityRulesDisabled((c) => fn(c.firestore()));

describe("profiles", () => {
  test("any signed-in user can GET a profile by uid (needed after a code lookup)", async () => {
    await assertSucceeds(getDoc(doc(as(C), "users", W)));
  });
  test("nobody can LIST users (no directory of wearers or live codes)", async () => {
    await assertFails(getDocs(collection(as(C), "users")));
  });
  test("signed-out users read nothing", async () => { await assertFails(getDoc(doc(anon(), "users", W))); });
  test("a new user can choose a role once", async () => {
    await assertSucceeds(setDoc(doc(as("new1"), "users", "new1"), { uid: "new1", role: "CAREGIVER", displayName: "n" }));
  });
  test("an invalid role is refused", async () => {
    await assertFails(setDoc(doc(as("new2"), "users", "new2"), { uid: "new2", role: "ADMIN", displayName: "n" }));
  });
  test("a role can never change (07), by update or by overwrite", async () => {
    await assertFails(updateDoc(doc(as(C), "users", C), { role: "WEARER" }));
    await assertFails(setDoc(doc(as(C), "users", C), { uid: C, role: "WEARER", displayName: "C" }));
  });
  test("a profile cannot be deleted (delete + create would bypass immutability)", async () => {
    await assertFails(deleteDoc(doc(as(C), "users", C)));
  });
  test("nobody edits someone else's profile", async () => { await assertFails(updateDoc(doc(as(C), "users", W), { displayName: "x" })); });
  test("only a wearer can hold a pairing code", async () => {
    await assertFails(updateDoc(doc(as(C), "users", C), { pairingCode: "123456", pairingCodeExpiresAt: 9 }));
    await assertSucceeds(updateDoc(doc(as(W), "users", W), { pairingCode: "123456", pairingCodeExpiresAt: 9 }));
  });
});

describe("pairing codes", () => {
  test("a caregiver can GET a code by value", async () => {
    await seed((db) => setDoc(doc(db, "active_codes", "555555"), { wearerUid: W, expiresAt: NOW() + 600000 }));
    await assertSucceeds(getDoc(doc(as(C), "active_codes", "555555")));
  });
  test("nobody can LIST codes", async () => {
    await assertFails(getDocs(collection(as(C), "active_codes")));
    await assertFails(getDocs(collection(as(W), "active_codes")));
  });
  test("a wearer can claim a code for themselves", async () => {
    await assertSucceeds(setDoc(doc(as(W), "active_codes", "111111"), { wearerUid: W, expiresAt: NOW() + 1000 }));
  });
  test("a wearer cannot hijack another wearer's live code, but may reuse an expired one", async () => {
    await seed((db) => setDoc(doc(db, "active_codes", "222222"), { wearerUid: W2, expiresAt: NOW() + 600000 }));
    await assertFails(setDoc(doc(as(W), "active_codes", "222222"), { wearerUid: W, expiresAt: NOW() + 1000 }));
    await seed((db) => setDoc(doc(db, "active_codes", "333333"), { wearerUid: W2, expiresAt: 1 }));
    await assertSucceeds(setDoc(doc(as(W), "active_codes", "333333"), { wearerUid: W, expiresAt: NOW() + 1000 }));
  });
  test("a caregiver cannot create a code", async () => {
    await assertFails(setDoc(doc(as(C), "active_codes", "444444"), { wearerUid: C, expiresAt: 9 }));
  });
  test("single-use: the owning wearer can delete (consume) their code; nobody else can", async () => {
    await seed((db) => setDoc(doc(db, "active_codes", "666666"), { wearerUid: W, expiresAt: NOW() + 600000 }));
    await assertFails(deleteDoc(doc(as(C), "active_codes", "666666")));
    await assertSucceeds(deleteDoc(doc(as(W), "active_codes", "666666")));
  });
});

describe("pairings", () => {
  test("a caregiver can request pairing as PENDING", async () => {
    await assertSucceeds(setDoc(doc(as("care3"), "pairings", `${W2}_care3`), { id: "x", wearerUid: W2, caregiverUid: "care3", status: "PENDING", createdAt: 1 }));
  });
  test("…only if their profile role is CAREGIVER", async () => {
    await assertFails(setDoc(doc(as(W2), "pairings", `${W}_${W2}`), { wearerUid: W, caregiverUid: W2, status: "PENDING", createdAt: 1 }));
  });
  test("a caregiver cannot self-approve", async () => {
    await assertFails(setDoc(doc(as("care4"), "pairings", `${W2}_care4`), { wearerUid: W2, caregiverUid: "care4", status: "APPROVED", createdAt: 1 }));
    await assertFails(updateDoc(doc(as(C2), "pairings", `${W}_${C2}`), { status: "APPROVED" }));
  });
  test("a caregiver cannot request as someone else", async () => {
    await assertFails(setDoc(doc(as(C2), "pairings", `${W}_${C}`), { wearerUid: W, caregiverUid: C, status: "PENDING", createdAt: 1 }));
  });
  test("the wearer approves and removes (status + removedAt only)", async () => {
    await assertSucceeds(updateDoc(doc(as(W), "pairings", `${W}_${C2}`), { status: "APPROVED" }));
    await assertSucceeds(updateDoc(doc(as(W), "pairings", `${W}_${C}`), { status: "REMOVED", removedAt: NOW() }));
    await assertFails(updateDoc(doc(as(W), "pairings", `${W}_${C}`), { caregiverUid: "someoneElse" }));
  });
  test("07 Deny: the wearer deletes the pairing; a caregiver cannot", async () => {
    await assertFails(deleteDoc(doc(as(C2), "pairings", `${W}_${C2}`)));
    await assertSucceeds(deleteDoc(doc(as(W), "pairings", `${W}_${C2}`)));
  });
  test("a caregiver can LEAVE (set their own to REMOVED) but nothing else", async () => {
    await assertSucceeds(updateDoc(doc(as(C), "pairings", `${W}_${C}`), { status: "REMOVED", removedAt: NOW() }));
    await assertFails(updateDoc(doc(as(C2), "pairings", `${W}_${C2}`), { status: "APPROVED" }));
  });
  test("07 re-pairing: a REMOVED caregiver can set the SAME document back to PENDING", async () => {
    await seed((db) => updateDoc(doc(db, "pairings", `${W}_${C}`), { status: "REMOVED", removedAt: 5 }));
    await assertSucceeds(updateDoc(doc(as(C), "pairings", `${W}_${C}`), { status: "PENDING", removedAt: deleteField(), createdAt: NOW(), caregiverName: "Maya" }));
  });
  test("…but not from PENDING/APPROVED, and never straight to APPROVED", async () => {
    await assertFails(updateDoc(doc(as(C), "pairings", `${W}_${C}`), { status: "PENDING", createdAt: NOW() }));          // currently APPROVED
    await seed((db) => updateDoc(doc(db, "pairings", `${W}_${C}`), { status: "REMOVED", removedAt: 5 }));
    await assertFails(updateDoc(doc(as(C), "pairings", `${W}_${C}`), { status: "APPROVED", createdAt: NOW() }));
  });
  test("a re-request cannot change who the pairing is between", async () => {
    await seed((db) => updateDoc(doc(db, "pairings", `${W}_${C}`), { status: "REMOVED", removedAt: 5 }));
    await assertFails(updateDoc(doc(as(C), "pairings", `${W}_${C}`), { status: "PENDING", wearerUid: W2, createdAt: NOW() }));
  });
  test("a pairing is invisible to outsiders", async () => { await assertFails(getDoc(doc(as(W2), "pairings", `${W}_${C}`))); });
});

describe("caregiver reviews (07: pairing-scoped subcollection)", () => {
  const rev = (extra = {}) => ({ label: "OVERWHELMED", isResolved: false, updatedAt: NOW(), ...extra });
  test("a caregiver of an APPROVED pairing can write a review", async () => {
    await assertSucceeds(setDoc(doc(as(C), "pairings", `${W}_${C}`, "reviews", "e1"), rev()));
  });
  test("a PENDING caregiver cannot", async () => {
    await assertFails(setDoc(doc(as(C2), "pairings", `${W}_${C2}`, "reviews", "e1"), rev()));
  });
  test("a review may contain only label, isResolved and updatedAt, with the right types", async () => {
    await assertFails(setDoc(doc(as(C), "pairings", `${W}_${C}`, "reviews", "e1"), rev({ extra: "x" })));
    await assertFails(setDoc(doc(as(C), "pairings", `${W}_${C}`, "reviews", "e1"), rev({ isResolved: "yes" })));
  });
  test("the wearer can read reviews but cannot write them; an outsider can do neither", async () => {
    await seed((db) => setDoc(doc(db, "pairings", `${W}_${C}`, "reviews", "e1"), rev()));
    await assertSucceeds(getDoc(doc(as(W), "pairings", `${W}_${C}`, "reviews", "e1")));
    await assertFails(setDoc(doc(as(W), "pairings", `${W}_${C}`, "reviews", "e1"), rev()));
    await assertFails(getDoc(doc(as(W2), "pairings", `${W}_${C}`, "reviews", "e1")));
  });
  test("after removal the caregiver can no longer write reviews", async () => {
    await seed((db) => updateDoc(doc(db, "pairings", `${W}_${C}`), { status: "REMOVED", removedAt: 5 }));
    await assertFails(setDoc(doc(as(C), "pairings", `${W}_${C}`, "reviews", "e2"), rev()));
  });
});

describe("wearer events", () => {
  test("the wearer reads and creates their own events", async () => {
    await assertSucceeds(getDoc(doc(as(W), "wearerData", W, "events", "e1")));
    await assertSucceeds(setDoc(doc(as(W), "wearerData", W, "events", "e2"), ev("e2")));
  });
  test("an APPROVED caregiver can read; a PENDING one, a REMOVED one and an outsider cannot", async () => {
    await assertSucceeds(getDoc(doc(as(C), "wearerData", W, "events", "e1")));
    await assertFails(getDoc(doc(as(C2), "wearerData", W, "events", "e1")));
    await assertFails(getDoc(doc(as(W2), "wearerData", W, "events", "e1")));
    await seed((db) => updateDoc(doc(db, "pairings", `${W}_${C}`), { status: "REMOVED", removedAt: 5 }));
    await assertFails(getDoc(doc(as(C), "wearerData", W, "events", "e1")));
  });
  test("07: caregivers are READ-ONLY on wearer data — no update, create or delete", async () => {
    await assertFails(updateDoc(doc(as(C), "wearerData", W, "events", "e1"), { label: "EXCITED", isResolved: true }));
    await assertFails(setDoc(doc(as(C), "wearerData", W, "events", "e9"), ev("e9")));
    await assertFails(deleteDoc(doc(as(C), "wearerData", W, "events", "e1")));
  });
  test("a wearer cannot write into another wearer's space; a caregiver account cannot write its own", async () => {
    await assertFails(setDoc(doc(as(W2), "wearerData", W, "events", "e5"), ev("e5")));
    await assertFails(setDoc(doc(as(C), "wearerData", C, "events", "e6"), ev("e6")));
  });
});

describe("push (docs/spec/12)", () => {
  const dev = (extra = {}) => ({ token: "abc", platform: "android", updatedAt: NOW(), ...extra });
  test("a user registers and reads their OWN device token", async () => {
    await assertSucceeds(setDoc(doc(as(C), "users", C, "devices", "inst1"), dev()));
    await assertSucceeds(getDoc(doc(as(C), "users", C, "devices", "inst1")));
  });
  test("nobody else can read or write another user's tokens (a token is a way to message that person)", async () => {
    await seed((db) => setDoc(doc(db, "users", C, "devices", "inst1"), dev()));
    await assertFails(getDoc(doc(as(W), "users", C, "devices", "inst1")));
    await assertFails(setDoc(doc(as(W), "users", C, "devices", "inst1"), dev({ token: "evil" })));
  });
  test("a token document has a fixed shape", async () => {
    await assertFails(setDoc(doc(as(C), "users", C, "devices", "i2"), dev({ platform: "web" })));
    await assertFails(setDoc(doc(as(C), "users", C, "devices", "i2"), dev({ extra: 1 })));
    await assertFails(setDoc(doc(as(C), "users", C, "devices", "i2"), dev({ token: 5 })));
  });
  test("the push log is server-only: no client can read or write it", async () => {
    await assertFails(setDoc(doc(as(W), "wearerData", W, "pushLog", "e1"), { caregivers: [] }));
    await seed((db) => setDoc(doc(db, "wearerData", W, "pushLog", "e1"), { caregivers: ["c1"] }));
    await assertFails(getDoc(doc(as(W), "wearerData", W, "pushLog", "e1")));
    await assertFails(getDoc(doc(as(C), "wearerData", W, "pushLog", "e1")));
  });
});
