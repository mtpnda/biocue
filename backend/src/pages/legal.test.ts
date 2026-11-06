// BioCue Legal Compliance & Policy Audit Tests

import { assertEquals } from "jsr:@std/assert";
import { app } from "../main.ts";

Deno.test("Privacy Policy includes required compliance sections", async () => {
  const res = await app.request("/privacy");
  assertEquals(res.status, 200);
  const html = await res.text();

  // 1. On-device isolation of physiological telemetry
  assertEquals(html.includes("Room database"), true);

  // 2. Zero continuous background GPS tracking
  assertEquals(html.includes("Zero Continuous GPS"), true);

  // 3. Instant caregiver removal and cache wipe
  assertEquals(html.includes("Irrevocable Removal"), true);

  // 4. Data retention and account deletion
  assertEquals(html.includes("section-7"), true);
  assertEquals(html.includes("privacy@biocue.app"), true);

  // 5. COPPA & Children's privacy
  assertEquals(html.includes("COPPA"), true);
});

Deno.test("Terms of Service enforces non-medical disclaimer and sovereignty", async () => {
  const res = await app.request("/terms");
  assertEquals(res.status, 200);
  const html = await res.text();

  // 1. Assistive non-medical disclaimer
  assertEquals(html.includes("Non-Medical"), true);
  assertEquals(html.includes("FDA-cleared"), true);

  // 2. Wearer sovereignty
  assertEquals(html.includes("sovereign owner"), true);

  // 3. Emergency SOS limitations
  assertEquals(html.includes("Emergency SOS Limitations"), true);
});
