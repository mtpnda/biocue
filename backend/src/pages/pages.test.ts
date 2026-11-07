// BioCue Web Pages & API Route Unit Tests

import { assertEquals } from "jsr:@std/assert";
import { app } from "../main.ts";

Deno.test("GET /health returns healthy text", async () => {
  const res = await app.request("/health");
  assertEquals(res.status, 200);
  const text = await res.text();
  assertEquals(text.includes("BioCue Edge Web Server"), true);
});

Deno.test("GET / redirects to localized route", async () => {
  const res = await app.request("/");
  assertEquals(res.status, 302);
  const location = res.headers.get("location");
  assertEquals(location?.startsWith("/"), true);
});

Deno.test("GET /en renders Home page with valid HTML and DLS tokens", async () => {
  const res = await app.request("/en");
  assertEquals(res.status, 200);
  const html = await res.text();
  assertEquals(html.includes("<!DOCTYPE html>"), true);
  assertEquals(html.includes("BioCue"), true);
  assertEquals(html.includes("Sensory Overload Protection"), true);
  assertEquals(html.includes("data-theme="), true);
  // Ensure NO pricing, currency, or payment terms are present
  assertEquals(html.includes("$"), false);
  assertEquals(html.toLowerCase().includes("usd"), false);
  assertEquals(html.toLowerCase().includes("idr"), false);
  assertEquals(html.toLowerCase().includes("rp "), false);
  assertEquals(html.toLowerCase().includes("payment"), false);
  assertEquals(html.toLowerCase().includes("monthly subscription"), false);
});

Deno.test("GET /whitepaper renders Technical Whitepaper with mathematical formulations and NPU architecture", async () => {
  const res = await app.request("/whitepaper");
  assertEquals(res.status, 200);
  const html = await res.text();
  assertEquals(html.includes("BioCue: An On-Device, Autonomic-Decoupled Hybrid Intelligence Architecture"), true);
  assertEquals(html.includes("Formula 2.1"), true);
  assertEquals(html.includes("Formula 2.4"), true);
  assertEquals(html.includes("Gemma 2B"), true);
  assertEquals(html.includes("HardwareResourceGovernor"), true);
});

Deno.test("GET /privacy renders Privacy Policy with sovereign clauses", async () => {
  const res = await app.request("/privacy");
  assertEquals(res.status, 200);
  const html = await res.text();
  assertEquals(html.includes("Privacy Policy"), true);
  assertEquals(html.includes("Zero Continuous GPS"), true);
  assertEquals(html.includes("On-Device Isolation"), true);
  assertEquals(html.includes("section-7"), true);
});

Deno.test("GET /terms renders Terms of Service with non-medical disclaimer", async () => {
  const res = await app.request("/terms");
  assertEquals(res.status, 200);
  const html = await res.text();
  assertEquals(html.includes("Terms of Service"), true);
  assertEquals(html.includes("Non-Medical"), true);
  assertEquals(html.includes("Caregiver"), true);
});

Deno.test("GET /support renders Helpdesk & Support Form", async () => {
  const res = await app.request("/support");
  assertEquals(res.status, 200);
  const html = await res.text();
  assertEquals(html.includes("Support Inquiry"), true);
  assertEquals(html.includes("supportForm"), true);
});

Deno.test("GET /signin renders portal entrance", async () => {
  const res = await app.request("/signin");
  assertEquals(res.status, 200);
  const html = await res.text();
  assertEquals(html.includes("Sign in to BioCue"), true);
});

Deno.test("GET /api/config returns valid platform metadata", async () => {
  const res = await app.request("/api/config");
  assertEquals(res.status, 200);
  const json = await res.json();
  assertEquals(json.app, "BioCue");
  assertEquals(json.privacy.zeroContinuousGps, true);
  assertEquals(json.privacy.onDeviceBiometricsOnly, true);
});

Deno.test("POST /api/support handles ticket submission", async () => {
  const payload = {
    name: "Alex",
    email: "alex@example.com",
    device: "samsung-galaxy-watch",
    message: "Question regarding sensor permission setting on Galaxy Watch 4.",
  };
  const res = await app.request("/api/support", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  assertEquals(res.status, 201);
  const json = await res.json();
  assertEquals(json.success, true);
  assertEquals(json.ticketId.startsWith("ticket_"), true);
});

Deno.test("GET /id renders Home page in Bahasa Indonesia with full section localization", async () => {
  const res = await app.request("/id");
  assertEquals(res.status, 200);
  const html = await res.text();
  assertEquals(html.includes("<!DOCTYPE html>"), true);
  assertEquals(html.includes("Perlindungan Sensory Overload"), true);
  assertEquals(html.includes("Tampilan Bebas Cemas (Zero-Anxiety)"), true);
  assertEquals(html.includes("Smartwatch Olahraga Konvensional vs. BioCue Berbasis Kedaulatan"), true);
  assertEquals(html.includes("Kapabilitas"), true);
  assertEquals(html.includes("API Health Services Wear OS hemat energi"), true);
  assertEquals(html.includes("Unduh Rilis"), true);
  assertEquals(html.includes("lang=\"id\""), true);
  // Ensure NO pricing, currency, or payment terms are present
  assertEquals(html.includes("$"), false);
});

Deno.test("GET /id-id redirects to /id and /en-us redirects to /en", async () => {
  const resId = await app.request("/id-id");
  assertEquals(resId.status, 302);
  assertEquals(resId.headers.get("location"), "/id");

  const resEn = await app.request("/en-us");
  assertEquals(resEn.status, 302);
  assertEquals(resEn.headers.get("location"), "/en");
});

