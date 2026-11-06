import { OpenAPIHono } from "@hono/zod-openapi";
import { cors } from "hono/cors";
import { renderHomePage } from "./pages/home.ts";
import { renderPrivacyPage } from "./pages/privacy.ts";
import { renderTermsPage } from "./pages/terms.ts";
import { renderSupportPage } from "./pages/support.ts";
import { renderSignInPage } from "./pages/signin.ts";
import { configRouter } from "./routes/config.ts";
import { supportRouter } from "./routes/support.ts";
import { resolveLocale } from "./i18n/index.ts";

export const app = new OpenAPIHono();

// Global middleware
app.use("*", cors());

// Web Pages & Health Check (Multi-Country Localized)
// First-time visit to / automatically routes to Indonesian site if in ID, or Global site if outside ID.
app.get("/", (c) => {
  const targetLocale = resolveLocale(c.req.url, c.req.raw.headers);
  return c.redirect(`/${targetLocale}`);
});

app.get("/en", (c) => c.html(renderHomePage(c.req.url, c.req.raw.headers)));
app.get("/en/", (c) => c.html(renderHomePage(c.req.url, c.req.raw.headers)));
app.get("/en-id", (c) => c.html(renderHomePage(c.req.url, c.req.raw.headers)));
app.get("/en-id/", (c) => c.html(renderHomePage(c.req.url, c.req.raw.headers)));
app.get("/id-id", (c) => c.html(renderHomePage(c.req.url, c.req.raw.headers)));
app.get("/id-id/", (c) => c.html(renderHomePage(c.req.url, c.req.raw.headers)));
app.get("/id", (c) => c.redirect("/id-id"));
app.get("/id/", (c) => c.redirect("/id-id"));

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
app.get("/en-id/support", (c) => c.html(renderSupportPage(c.req.url, c.req.raw.headers)));
app.get("/id-id/support", (c) => c.html(renderSupportPage(c.req.url, c.req.raw.headers)));

// Portal & Sign In
app.get("/signin", (c) => c.html(renderSignInPage(c.req.url, c.req.raw.headers)));
app.get("/login", (c) => c.redirect("/signin"));

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
