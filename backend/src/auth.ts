// BioCue JWT Authentication Utilities and Middleware

import { sign, verify } from "hono/jwt";
import type { MiddlewareHandler } from "hono";
import type { UserProfile } from "./db.ts";

const JWT_SECRET = Deno.env.get("JWT_SECRET") || "biocue-sovereign-jwt-secret-key-32chars";

export interface SessionPayload {
  sub: string;
  email: string;
  name: string;
  modes: ("WEARER" | "CAREGIVER")[];
  exp: number;
  iat: number;
  [key: string]: unknown;
}

export type AuthEnv = {
  Variables: {
    user: SessionPayload;
  };
};

export async function createSessionToken(user: UserProfile): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const payload: SessionPayload = {
    sub: user.uid,
    email: user.email,
    name: user.displayName,
    modes: user.modes,
    iat: now,
    exp: now + 30 * 24 * 60 * 60, // 30 days
  };
  return await sign(payload, JWT_SECRET, "HS256");
}

export async function verifySessionToken(token: string): Promise<SessionPayload | null> {
  try {
    const payload = await verify(token, JWT_SECRET, "HS256");
    return payload as unknown as SessionPayload;
  } catch (_e) {
    return null;
  }
}

export const authMiddleware: MiddlewareHandler<AuthEnv> = async (c, next) => {
  const authHeader = c.req.header("Authorization");
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return c.json({ error: "Missing or invalid authorization token" }, 401);
  }

  const token = authHeader.slice(7).trim();
  const payload = await verifySessionToken(token);
  if (!payload) {
    return c.json({ error: "Unauthorized or expired token" }, 401);
  }

  c.set("user", payload);
  await next();
};
