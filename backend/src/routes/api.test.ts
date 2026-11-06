// BioCue Deno API Integration Test Suite
// Verifies Auth, Pairings, and Events routes

import { assertEquals, assertExists } from "https://deno.land/std@0.224.0/assert/mod.ts";
import { app } from "../main.ts";

Deno.test("API: /auth/google redirects to callback or provider", async () => {
  const req = new Request("http://localhost:8000/auth/google", {
    headers: { "x-forwarded-proto": "http", "host": "localhost:8000" },
  });
  const res = await app.fetch(req);
  assertEquals(res.status, 302);
  const loc = res.headers.get("location") || "";
  assertExists(loc);
});

Deno.test("API: Full Auth, Pairing, and Event lifecycle", async () => {
  // 1. Direct auth exchange for Wearer
  const wearerReq = new Request("http://localhost:8000/api/auth/token", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: "wearer_test@biocue.local", displayName: "Alex Wearer" }),
  });
  const wearerRes = await app.fetch(wearerReq);
  assertEquals(wearerRes.status, 200);
  const wearerData = await wearerRes.json();
  assertExists(wearerData.token);
  const wearerToken = wearerData.token;
  const wearerUid = wearerData.user.uid;

  // 2. Direct auth exchange for Caregiver
  const cgReq = new Request("http://localhost:8000/api/auth/token", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: "caregiver_test@biocue.local", displayName: "Sam Caregiver" }),
  });
  const cgRes = await app.fetch(cgReq);
  assertEquals(cgRes.status, 200);
  const cgData = await cgRes.json();
  assertExists(cgData.token);
  const cgToken = cgData.token;

  // 3. GET /api/auth/me for Wearer
  const meReq = new Request("http://localhost:8000/api/auth/me", {
    headers: { Authorization: `Bearer ${wearerToken}` },
  });
  const meRes = await app.fetch(meReq);
  assertEquals(meRes.status, 200);
  const meData = await meRes.json();
  assertEquals(meData.uid, wearerUid);

  // 4. Wearer generates pairing code
  const codeReq = new Request("http://localhost:8000/api/pairings/code", {
    method: "POST",
    headers: { Authorization: `Bearer ${wearerToken}` },
  });
  const codeRes = await app.fetch(codeReq);
  assertEquals(codeRes.status, 200);
  const codeData = await codeRes.json();
  assertEquals(codeData.code.length, 6);
  const pairingCode = codeData.code;

  // 5. Caregiver claims pairing code
  const claimReq = new Request("http://localhost:8000/api/pairings/claim", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${cgToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ code: pairingCode }),
  });
  const claimRes = await app.fetch(claimReq);
  assertEquals(claimRes.status, 200);
  const claimData = await claimRes.json();
  assertEquals(claimData.status, "APPROVED");
  assertEquals(claimData.wearerUid, wearerUid);
  const pairingId = claimData.id;

  // 6. Caregiver views their pairings
  const listReq = new Request("http://localhost:8000/api/pairings", {
    headers: { Authorization: `Bearer ${cgToken}` },
  });
  const listRes = await app.fetch(listReq);
  assertEquals(listRes.status, 200);
  const listData = await listRes.json();
  assertEquals(listData.length >= 1, true);

  // 7. Wearer records an event
  const eventId = `evt_${Date.now()}`;
  const evtReq = new Request("http://localhost:8000/api/events", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${wearerToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      id: eventId,
      startTime: Date.now() - 1000,
      triggerType: "AUTO",
      label: "OVERWHELMED_SENSORY",
      isResolved: false,
    }),
  });
  const evtRes = await app.fetch(evtReq);
  assertEquals(evtRes.status, 200);

  // 8. Caregiver fetches events for wearer
  const fetchReq = new Request(`http://localhost:8000/api/events/${wearerUid}`, {
    headers: { Authorization: `Bearer ${cgToken}` },
  });
  const fetchRes = await app.fetch(fetchReq);
  assertEquals(fetchRes.status, 200);
  const events = await fetchRes.json();
  assertEquals(events.length >= 1, true);
  assertEquals(events[0].id, eventId);

  // 9. Wearer triggers meltdown alert
  const meltdownReq = new Request("http://localhost:8000/api/events/meltdown", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${wearerToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      kind: "meltdown",
      eventId,
      lat: 37.7749,
      lng: -122.4194,
    }),
  });
  const meltdownRes = await app.fetch(meltdownReq);
  assertEquals(meltdownRes.status, 200);
  const meltdownData = await meltdownRes.json();
  assertEquals(meltdownData.success, true);

  // 10. Caregiver posts reflection review
  const reviewReq = new Request(`http://localhost:8000/api/pairings/${pairingId}/review`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${cgToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      eventId,
      note: "Grounding breathing exercise assisted calmly.",
    }),
  });
  const reviewRes = await app.fetch(reviewReq);
  assertEquals(reviewRes.status, 200);

  // 11. Revoke pairing
  const revokeReq = new Request(`http://localhost:8000/api/pairings/${pairingId}`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${wearerToken}` },
  });
  const revokeRes = await app.fetch(revokeReq);
  assertEquals(revokeRes.status, 200);
  const revokeData = await revokeRes.json();
  assertEquals(revokeData.status, "REMOVED");
});
