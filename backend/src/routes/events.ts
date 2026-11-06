// BioCue Events & Real-Time Alert Router
// Implements specs/16_DENO_AUTH_AND_DATA_PLANE.md & specs/12_PUSH_AND_GPS.md

import { Hono } from "hono";
import { Database, type StressEvent } from "../db.ts";
import { authMiddleware, type SessionPayload, type AuthEnv } from "../auth.ts";

export const eventsRouter = new Hono<AuthEnv>();

eventsRouter.use("*", authMiddleware);

// POST /api/events -> Ingest stress event (Wearer)
eventsRouter.post("/", async (c) => {
  const session = c.get("user") as SessionPayload;
  const body = await c.req.json().catch(() => ({}));

  if (!body.id || !body.startTime) {
    return c.json({ error: "id and startTime are required" }, 400);
  }

  const db = await Database.init();
  const event: StressEvent = {
    id: String(body.id),
    userId: session.sub,
    startTime: Number(body.startTime),
    endTime: body.endTime ? Number(body.endTime) : undefined,
    triggerType: body.triggerType === "AUTO" ? "AUTO" : "MANUAL",
    label: String(body.label || "NONE"),
    isResolved: Boolean(body.isResolved),
  };

  await db.saveEvent(event);
  return c.json(event);
});

// GET /api/events/:wearerUid -> List stress events
eventsRouter.get("/:wearerUid", async (c) => {
  const session = c.get("user") as SessionPayload;
  const wearerUid = c.req.param("wearerUid");
  const db = await Database.init();

  // Authorization check: must be wearer themselves or an approved caregiver
  if (session.sub !== wearerUid) {
    const pairings = await db.getPairingsForUser(session.sub);
    const hasApprovedPairing = pairings.some(
      (p) => p.status === "APPROVED" && p.wearerUid === wearerUid && p.caregiverUid === session.sub
    );
    if (!hasApprovedPairing) {
      return c.json({ error: "Unauthorized: not an approved caregiver for this wearer" }, 403);
    }
  }

  const limit = Math.min(Number(c.req.query("limit") || 50), 100);
  const events = await db.getEventsForWearer(wearerUid, limit);
  return c.json(events);
});

// POST /api/events/meltdown -> Port of Cloud Functions notifyMeltdown
eventsRouter.post("/meltdown", async (c) => {
  const session = c.get("user") as SessionPayload;
  const body = await c.req.json().catch(() => ({}));
  const { kind, eventId, lat, lng } = body;

  if (kind !== "meltdown" && kind !== "sos") {
    return c.json({ error: "Invalid kind: must be meltdown or sos" }, 400);
  }

  const db = await Database.init();
  const user = await db.getUser(session.sub);
  if (!user) return c.json({ error: "User profile not found" }, 404);

  const event = await db.getEvent(session.sub, eventId);
  if (!event) return c.json({ error: "Event not found" }, 404);

  const nowMs = Date.now();
  if (nowMs - event.startTime > 120_000) {
    return c.json({ error: "Event is too old to trigger meltdown push" }, 400);
  }

  const pairings = await db.getPairingsForUser(session.sub);
  const approvedPairings = pairings.filter((p) => p.status === "APPROVED" && p.wearerUid === session.sub);

  const alreadyPushed = await db.getPushLog(session.sub, eventId);
  const notifiedCaregivers: string[] = [...alreadyPushed];

  let sentCount = 0;
  for (const p of approvedPairings) {
    if (p.caregiverUid === session.sub) continue;
    if (alreadyPushed.includes(p.caregiverUid)) continue;

    const devices = await db.getDevices(p.caregiverUid);
    if (devices.length === 0) continue;

    // Simulate/send push notification
    console.log(`[Push Notification] -> Caregiver ${p.caregiverName} (${p.caregiverUid}): ${kind.toUpperCase()} from ${user.displayName}`);
    notifiedCaregivers.push(p.caregiverUid);
    sentCount++;
  }

  await db.savePushLog(session.sub, eventId, notifiedCaregivers);

  return c.json({
    success: true,
    sent: sentCount,
    totalApprovedCaregivers: approvedPairings.length,
    location: (typeof lat === "number" && typeof lng === "number") ? { lat, lng } : null,
  });
});

// POST /api/devices -> Register device push token
eventsRouter.post("/devices", async (c) => {
  const session = c.get("user") as SessionPayload;
  const body = await c.req.json().catch(() => ({}));

  if (!body.token) return c.json({ error: "Push token is required" }, 400);

  const db = await Database.init();
  const device = {
    id: String(body.id || `dev_${session.sub.slice(-6)}_${Date.now()}`),
    uid: session.sub,
    token: String(body.token),
    platform: (body.platform === "ios" ? "ios" : "android") as "ios" | "android",
    updatedAt: Date.now(),
  };

  await db.registerDevice(device);
  return c.json(device);
});

// GET /api/events/stream -> Server-Sent Events (SSE) live updates using Deno KV watch
eventsRouter.get("/stream", (c) => {
  const session = c.get("user") as SessionPayload;
  const wearerUid = c.req.query("wearerUid") || session.sub;

  const body = new ReadableStream({
    async start(controller) {
      const db = await Database.init();
      const watcher = db.rawKv.watch([["events_by_time", wearerUid]]);
      const encoder = new TextEncoder();

      controller.enqueue(encoder.encode(`event: connected\ndata: ${JSON.stringify({ wearerUid })}\n\n`));

      try {
        for await (const [entry] of watcher) {
          if (entry?.value) {
            const data = JSON.stringify(entry.value);
            controller.enqueue(encoder.encode(`event: event_update\ndata: ${data}\n\n`));
          }
        }
      } catch (_err) {
        // Stream closed
      }
    },
  });

  return new Response(body, {
    headers: {
      "Content-Type": "text/event-stream",
      "Cache-Control": "no-cache",
      "Connection": "keep-alive",
    },
  });
});
