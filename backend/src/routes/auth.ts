// BioCue Authentication Router
// Implements specs/16_DENO_AUTH_AND_DATA_PLANE.md

import { Hono } from "hono";
import { Database } from "../db.ts";
import { createSessionToken, authMiddleware, type SessionPayload, type AuthEnv } from "../auth.ts";

export const authRouter = new Hono<AuthEnv>();

const GOOGLE_CLIENT_ID = Deno.env.get("GOOGLE_CLIENT_ID") || "";
const GOOGLE_CLIENT_SECRET = Deno.env.get("GOOGLE_CLIENT_SECRET") || "";

// GET /auth/google -> Starts Google OAuth 2.0 PKCE flow
authRouter.get("/auth/google", (c) => {
  const host = c.req.header("host") || "localhost:8000";
  const proto = c.req.header("x-forwarded-proto") || (host.startsWith("localhost") ? "http" : "https");
  const redirectUri = `${proto}://${host}/auth/callback`;
  const isMobile = c.req.query("mobile") === "true";

  if (!GOOGLE_CLIENT_ID) {
    // Graceful dev fallback: instant test login redirect
    const mockEmail = c.req.query("email") || "wearer@biocue.local";
    const mockName = c.req.query("name") || "Alex Wearer";
    const mockUid = `mock_${mockEmail.replace(/[^a-zA-Z0-9]/g, "_")}`;
    const target = isMobile
      ? `/auth/callback?code=mock_code&uid=${mockUid}&email=${encodeURIComponent(mockEmail)}&name=${encodeURIComponent(mockName)}&mobile=true`
      : `/auth/callback?code=mock_code&uid=${mockUid}&email=${encodeURIComponent(mockEmail)}&name=${encodeURIComponent(mockName)}`;
    return c.redirect(target);
  }

  const googleAuthUrl = new URL("https://accounts.google.com/o/oauth2/v2/auth");
  googleAuthUrl.searchParams.set("client_id", GOOGLE_CLIENT_ID);
  googleAuthUrl.searchParams.set("redirect_uri", redirectUri);
  googleAuthUrl.searchParams.set("response_type", "code");
  googleAuthUrl.searchParams.set("scope", "openid email profile");
  googleAuthUrl.searchParams.set("prompt", "select_account");
  googleAuthUrl.searchParams.set("state", isMobile ? "mobile" : "web");

  return c.redirect(googleAuthUrl.toString());
});

// GET /auth/callback -> Handles OAuth callback and redirects to biocue://
authRouter.get("/auth/callback", async (c) => {
  const code = c.req.query("code");
  const state = c.req.query("state");
  const isMobile = state === "mobile" || c.req.query("mobile") === "true";

  const db = await Database.init();

  let uid = "";
  let email = "";
  let displayName = "";
  let photoUrl: string | undefined;

  if (code === "mock_code") {
    // Development mock
    uid = c.req.query("uid") || "wearer_alex_test";
    email = c.req.query("email") || "alex@biocue.local";
    displayName = c.req.query("name") || "Alex (Test Wearer)";
  } else if (code && GOOGLE_CLIENT_ID && GOOGLE_CLIENT_SECRET) {
    // Real Google OAuth code exchange
    try {
      const host = c.req.header("host") || "localhost:8000";
      const proto = c.req.header("x-forwarded-proto") || (host.startsWith("localhost") ? "http" : "https");
      const redirectUri = `${proto}://${host}/auth/callback`;

      const tokenRes = await fetch("https://oauth2.googleapis.com/token", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
          code,
          client_id: GOOGLE_CLIENT_ID,
          client_secret: GOOGLE_CLIENT_SECRET,
          redirect_uri: redirectUri,
          grant_type: "authorization_code",
        }),
      });

      const tokenData = await tokenRes.json();
      if (!tokenData.id_token) throw new Error(tokenData.error_description || "Failed to exchange Google code");

      // Verify and decode user profile from Google tokeninfo
      const userInfoRes = await fetch(`https://oauth2.googleapis.com/tokeninfo?id_token=${tokenData.id_token}`);
      const userInfo = await userInfoRes.json();

      uid = `google_${userInfo.sub}`;
      email = userInfo.email;
      displayName = userInfo.name || userInfo.email.split("@")[0];
      photoUrl = userInfo.picture;
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      return c.html(`<h3>Google Authentication Error</h3><p>${msg}</p><a href="/signin">Try again</a>`, 400);
    }
  } else {
    return c.json({ error: "Missing authorization code" }, 400);
  }

  // Upsert user in Deno KV
  const user = await db.upsertUser({
    uid,
    email,
    displayName,
    photoUrl,
    modes: ["WEARER"],
  });

  const sessionToken = await createSessionToken(user);

  // If mobile, redirect to native iOS/Android scheme
  if (isMobile) {
    return c.redirect(`biocue://auth/callback?token=${encodeURIComponent(sessionToken)}`);
  }

  // Web portal redirect
  return c.redirect(`/signin?token=${encodeURIComponent(sessionToken)}`);
});

// POST /api/auth/token -> Direct ID Token exchange (for native Google / Apple SDK fallback)
authRouter.post("/api/auth/token", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { idToken, provider = "google", email: customEmail, displayName: customName } = body;

  if (!idToken && !customEmail) {
    return c.json({ error: "idToken or email is required" }, 400);
  }

  const db = await Database.init();
  let uid = "";
  let email = customEmail || "";
  let displayName = customName || "";
  let photoUrl: string | undefined;

  if (idToken && provider === "google") {
    try {
      const res = await fetch(`https://oauth2.googleapis.com/tokeninfo?id_token=${idToken}`);
      if (!res.ok) throw new Error("Invalid Google ID token");
      const data = await res.json();
      uid = `google_${data.sub}`;
      email = data.email;
      displayName = data.name || email.split("@")[0];
      photoUrl = data.picture;
    } catch (_e) {
      return c.json({ error: "Failed to verify Google ID token" }, 401);
    }
  } else {
    uid = `user_${(email || "guest").replace(/[^a-zA-Z0-9]/g, "_")}`;
    displayName = displayName || email.split("@")[0];
  }

  const user = await db.upsertUser({
    uid,
    email,
    displayName,
    photoUrl,
    modes: ["WEARER"],
  });

  const token = await createSessionToken(user);
  return c.json({ token, user });
});

// GET /api/auth/me -> Returns current user profile
authRouter.get("/api/auth/me", authMiddleware, async (c) => {
  const session = c.get("user") as SessionPayload;
  const db = await Database.init();
  const user = await db.getUser(session.sub);
  if (!user) return c.json({ error: "User not found" }, 404);
  return c.json(user);
});

// PUT /api/auth/profile -> Updates current user profile & modes
authRouter.put("/api/auth/profile", authMiddleware, async (c) => {
  const session = c.get("user") as SessionPayload;
  const body = await c.req.json().catch(() => ({}));
  const db = await Database.init();

  const user = await db.getUser(session.sub);
  if (!user) return c.json({ error: "User not found" }, 404);

  if (body.displayName) user.displayName = String(body.displayName).trim();
  if (Array.isArray(body.modes)) {
    const validModes = body.modes.filter((m: string) => m === "WEARER" || m === "CAREGIVER");
    if (validModes.length > 0) user.modes = validModes;
  }

  const updated = await db.upsertUser(user);
  const token = await createSessionToken(updated);
  return c.json({ user: updated, token });
});
