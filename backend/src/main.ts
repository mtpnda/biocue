import { OpenAPIHono } from "@hono/zod-openapi";
import { cors } from "hono/cors";
import { renderHomePage } from "./pages/home.ts";
import { renderPrivacyPage } from "./pages/privacy.ts";
import { renderTermsPage } from "./pages/terms.ts";
import { renderSupportPage } from "./pages/support.ts";
import { renderSignInPage } from "./pages/signin.ts";
import { renderWhitepaperPage } from "./pages/whitepaper.ts";
import { configRouter } from "./routes/config.ts";
import { supportRouter } from "./routes/support.ts";
import { authRouter } from "./routes/auth.ts";
import { pairingsRouter } from "./routes/pairings.ts";
import { eventsRouter } from "./routes/events.ts";
import { resolveLocale } from "./i18n/index.ts";

export const app = new OpenAPIHono();

// Global middleware
app.use("*", cors());

// Authentication and Sovereign Data Plane Routes
app.route("/", authRouter);
app.route("/api/pairings", pairingsRouter);
app.route("/api/events", eventsRouter);

// Web Pages & Health Check (Multi-Country Localized)
// First-time visit to / automatically routes to Indonesian site (/id) if in ID, or US English site (/en) if outside ID.
app.get("/", (c) => {
  const targetLocale = resolveLocale(c.req.url, c.req.raw.headers);
  return c.redirect(`/${targetLocale}`);
});

app.get("/en", (c) => c.html(renderHomePage(c.req.url, c.req.raw.headers)));
app.get("/en/", (c) => c.html(renderHomePage(c.req.url, c.req.raw.headers)));
app.get("/en-us", (c) => c.redirect("/en"));
app.get("/en-us/", (c) => c.redirect("/en"));
app.get("/en-id", (c) => c.redirect("/en"));
app.get("/en-id/", (c) => c.redirect("/en"));
app.get("/id", (c) => c.html(renderHomePage(c.req.url, c.req.raw.headers)));
app.get("/id/", (c) => c.html(renderHomePage(c.req.url, c.req.raw.headers)));
app.get("/id-id", (c) => c.redirect("/id"));
app.get("/id-id/", (c) => c.redirect("/id"));

// Section anchor redirects
app.get("/features", (c) => {
  const targetLocale = resolveLocale(c.req.url, c.req.raw.headers);
  return c.redirect(`/${targetLocale}#features`);
});
app.get("/how-it-works", (c) => {
  const targetLocale = resolveLocale(c.req.url, c.req.raw.headers);
  return c.redirect(`/${targetLocale}#how-it-works`);
});
app.get("/simulator", (c) => {
  const targetLocale = resolveLocale(c.req.url, c.req.raw.headers);
  return c.redirect(`/${targetLocale}#simulator`);
});
app.get("/faq", (c) => {
  const targetLocale = resolveLocale(c.req.url, c.req.raw.headers);
  return c.redirect(`/${targetLocale}#faq`);
});

// Legal & Support pages
app.get("/whitepaper", (c) => c.html(renderWhitepaperPage(c.req.url, c.req.raw.headers)));
app.get("/whitepaper/", (c) => c.redirect("/whitepaper"));
app.get("/en/whitepaper", (c) => c.html(renderWhitepaperPage(c.req.url, c.req.raw.headers)));
app.get("/id/whitepaper", (c) => c.html(renderWhitepaperPage(c.req.url, c.req.raw.headers)));
app.get("/en-id/whitepaper", (c) => c.redirect("/en/whitepaper"));
app.get("/id-id/whitepaper", (c) => c.redirect("/id/whitepaper"));

app.get("/privacy", (c) => c.html(renderPrivacyPage(c.req.url, c.req.raw.headers)));
app.get("/privacy-policy", (c) => c.redirect("/privacy"));
app.get("/terms", (c) => c.html(renderTermsPage(c.req.url, c.req.raw.headers)));
app.get("/terms-of-service", (c) => c.redirect("/terms"));
app.get("/terms-and-conditions", (c) => c.redirect("/terms"));

app.get("/support", (c) => c.html(renderSupportPage(c.req.url, c.req.raw.headers)));
app.get("/support/", (c) => c.redirect("/support"));
app.get("/help", (c) => c.redirect("/support"));
app.get("/contact", (c) => c.redirect("/support"));
app.get("/en/support", (c) => c.html(renderSupportPage(c.req.url, c.req.raw.headers)));
app.get("/id/support", (c) => c.html(renderSupportPage(c.req.url, c.req.raw.headers)));
app.get("/en-id/support", (c) => c.redirect("/en/support"));
app.get("/id-id/support", (c) => c.redirect("/id/support"));

// Portal & Sign In
app.get("/signin", (c) => c.html(renderSignInPage(c.req.url, c.req.raw.headers)));
app.get("/login", (c) => c.redirect("/signin"));

// Static Assets: Official Brand Icon & Favicon (Mindful Contours)
let iconPngBytes: Uint8Array | null = null;
let iconSvgText = "";

try {
  iconPngBytes = await Deno.readFile(new URL("./assets/icon.png", import.meta.url));
} catch (e) {
  console.warn("Could not read ./assets/icon.png:", e);
}

try {
  iconSvgText = await Deno.readTextFile(new URL("./assets/icon.svg", import.meta.url));
} catch (e) {
  console.warn("Could not read ./assets/icon.svg:", e);
}

const serveIconPng = (c: any) => {
  if (!iconPngBytes) return c.text("Icon not found", 404);
  return c.body(iconPngBytes, 200, {
    "Content-Type": "image/png",
    "Cache-Control": "public, max-age=86400, immutable",
  });
};

const serveIconSvg = (c: any) => {
  if (!iconSvgText) return c.text("SVG not found", 404);
  return c.body(iconSvgText, 200, {
    "Content-Type": "image/svg+xml",
    "Cache-Control": "public, max-age=86400, immutable",
  });
};

app.get("/icon.png", serveIconPng);
app.get("/favicon.ico", serveIconPng);
app.get("/apple-touch-icon.png", serveIconPng);
app.get("/apple-touch-icon-precomposed.png", serveIconPng);
app.get("/icon.svg", serveIconSvg);
app.get("/logo.svg", serveIconSvg);

// Downloads
app.get("/download", (c) => c.redirect("/#download"));
app.get("/download/android", (c) => c.redirect("https://github.com/mtpnda/biocue/releases/latest"));
app.get("/download/wear", (c) => c.redirect("https://github.com/mtpnda/biocue/releases/latest"));

// System Health
app.get("/health", (c) => c.text("BioCue Edge Web Server (Deno KV) is healthy"));

// Mount API routes
app.route("/api/config", configRouter);
app.route("/api/support", supportRouter);

// Start server when executed directly
if (import.meta.main) {
  const port = Number(Deno.env.get("PORT") || 8000);
  console.log(`🌿 BioCue Web Server listening on http://localhost:${port}`);
  Deno.serve({ port }, app.fetch);
}
