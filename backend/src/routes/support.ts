// BioCue Support Inquiries API Route
// Stores submissions in Deno KV for support team triaging

import { Hono } from "hono";
import { z } from "zod";

export const supportRouter = new Hono();

const supportSchema = z.object({
  name: z.string().min(1).max(100),
  email: z.string().email(),
  device: z.string().min(1).max(100),
  message: z.string().min(5).max(4000),
});

supportRouter.post("/", async (c) => {
  try {
    const body = await c.req.json();
    const parsed = supportSchema.safeParse(body);

    if (!parsed.success) {
      return c.json({ error: "Invalid form fields", details: parsed.error.issues }, 400);
    }

    const { name, email, device, message } = parsed.data;
    const ticketId = `ticket_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
    const record = {
      id: ticketId,
      name,
      email,
      device,
      message,
      createdAt: new Date().toISOString(),
      status: "OPEN",
    };

    // Store in Deno KV if available
    try {
      const kv = await Deno.openKv();
      await kv.set(["support_tickets", ticketId], record);
    } catch (_kvErr) {
      // Graceful fallback for non-KV environments
      console.log("[BioCue Support Ticket Received]", record);
    }

    return c.json({ success: true, ticketId, message: "Ticket received successfully" }, 201);
  } catch (err) {
    console.error("[Support Router Error]", err);
    return c.json({ error: "Failed to process inquiry" }, 500);
  }
});
