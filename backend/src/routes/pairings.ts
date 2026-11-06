// BioCue Pairings Router (Caregiver Circle & Verification)
// Implements specs/16_DENO_AUTH_AND_DATA_PLANE.md & specs/07_MULTI_ACCOUNT.md

import { Hono } from "hono";
import { Database } from "../db.ts";
import { authMiddleware, type SessionPayload, type AuthEnv } from "../auth.ts";

export const pairingsRouter = new Hono<AuthEnv>();

pairingsRouter.use("*", authMiddleware);

// POST /api/pairings/code -> Generate 6-digit pairing code (Wearer)
pairingsRouter.post("/code", async (c) => {
  const session = c.get("user") as SessionPayload;
  const db = await Database.init();

  const user = await db.getUser(session.sub);
  if (!user) return c.json({ error: "User not found" }, 404);

  const code = await db.createPairingCode(user.uid, user.displayName);
  const expiresAt = Date.now() + 15 * 60 * 1000;

  return c.json({
    code,
    expiresAt,
    ttlSeconds: 900,
  });
});

// POST /api/pairings/claim -> Claim 6-digit pairing code (Caregiver)
pairingsRouter.post("/claim", async (c) => {
  const session = c.get("user") as SessionPayload;
  const body = await c.req.json().catch(() => ({}));
  const code = String(body.code || "").trim();

  if (!code || code.length !== 6) {
    return c.json({ error: "A 6-digit pairing code is required" }, 400);
  }

  const db = await Database.init();
  const caregiver = await db.getUser(session.sub);
  if (!caregiver) return c.json({ error: "Caregiver profile not found" }, 404);

  try {
    const pairing = await db.claimPairingCode(caregiver.uid, caregiver.displayName, code);
    return c.json(pairing);
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : String(err);
    return c.json({ error: msg }, 400);
  }
});

// GET /api/pairings -> List active pairings for user
pairingsRouter.get("/", async (c) => {
  const session = c.get("user") as SessionPayload;
  const db = await Database.init();
  const pairings = await db.getPairingsForUser(session.sub);
  return c.json(pairings);
});

// DELETE /api/pairings/:id -> Revoke a pairing
pairingsRouter.delete("/:id", async (c) => {
  const session = c.get("user") as SessionPayload;
  const pairingId = c.req.param("id");
  const db = await Database.init();

  try {
    const revoked = await db.revokePairing(pairingId, session.sub);
    return c.json(revoked);
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : String(err);
    return c.json({ error: msg }, 403);
  }
});

// POST /api/pairings/:id/review -> Add caregiver journal reflection note
pairingsRouter.post("/:id/review", async (c) => {
  const session = c.get("user") as SessionPayload;
  const pairingId = c.req.param("id");
  const body = await c.req.json().catch(() => ({}));

  const note = String(body.note || "").trim();
  const eventId = String(body.eventId || "").trim();

  if (!note || !eventId) {
    return c.json({ error: "Both eventId and note are required" }, 400);
  }

  const db = await Database.init();
  const pairing = await db.getPairing(pairingId);
  if (!pairing || pairing.status !== "APPROVED") {
    return c.json({ error: "Pairing is not approved or active" }, 403);
  }

  if (pairing.caregiverUid !== session.sub) {
    return c.json({ error: "Only the assigned caregiver can add review notes" }, 403);
  }

  const reviewId = `rev_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`;
  const review = {
    id: reviewId,
    pairingId,
    eventId,
    caregiverUid: session.sub,
    note,
    createdAt: Date.now(),
  };

  await db.addCaregiverReview(review);
  return c.json(review);
});

// GET /api/pairings/:id/reviews -> Get reviews for a pairing
pairingsRouter.get("/:id/reviews", async (c) => {
  const session = c.get("user") as SessionPayload;
  const pairingId = c.req.param("id");
  const db = await Database.init();

  const pairing = await db.getPairing(pairingId);
  if (!pairing) return c.json({ error: "Pairing not found" }, 404);

  if (pairing.wearerUid !== session.sub && pairing.caregiverUid !== session.sub) {
    return c.json({ error: "Unauthorized" }, 403);
  }

  const reviews = await db.getReviewsForPairing(pairingId);
  return c.json(reviews);
});
