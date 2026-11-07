import { assertEquals } from "jsr:@std/assert";
import { resolveLocale, getDictionary, renderLocaleSwitcher } from "./index.ts";

Deno.test("resolveLocale parses direct path prefixes", () => {
  assertEquals(resolveLocale("https://biocue.app/en"), "en");
  assertEquals(resolveLocale("https://biocue.app/en/"), "en");
  assertEquals(resolveLocale("https://biocue.app/en-us"), "en");
  assertEquals(resolveLocale("https://biocue.app/id"), "id");
  assertEquals(resolveLocale("https://biocue.app/id/"), "id");
  assertEquals(resolveLocale("https://biocue.app/id-id"), "id");
});

Deno.test("resolveLocale falls back to en for default root", () => {
  assertEquals(resolveLocale("https://biocue.app/"), "en");
});

Deno.test("resolveLocale respects cf-ipcountry header and maps to id", () => {
  const headers = new Headers({ "cf-ipcountry": "ID" });
  assertEquals(resolveLocale("https://biocue.app/", headers), "id");

  const idHeaders = new Headers({ "cf-ipcountry": "ID", "accept-language": "id-ID,id;q=0.9" });
  assertEquals(resolveLocale("https://biocue.app/", idHeaders), "id");
});

Deno.test("getDictionary returns valid translation schema for US English and Bahasa Indonesia", () => {
  const en = getDictionary("en");
  assertEquals(en.locale, "en");
  assertEquals(en.localeName, "English (US)");
  assertEquals(en.flag, "🇺🇸");
  assertEquals(en.features.cards.length, 6);
  assertEquals(en.comparison.rows.length, 6);
  assertEquals(en.footer.badge.length > 0, true);

  const id = getDictionary("id");
  assertEquals(id.locale, "id");
  assertEquals(id.localeName, "Bahasa Indonesia");
  assertEquals(id.flag, "🇮🇩");
  assertEquals(id.features.cards.length, 6);
  assertEquals(id.comparison.rows.length, 6);
  assertEquals(id.footer.badge.length > 0, true);
  assertEquals(id.features.cards[0].title, "Tampilan Bebas Cemas (Zero-Anxiety)");
});

Deno.test("renderLocaleSwitcher outputs strictly US English and Bahasa Indonesia (no Global)", () => {
  const html = renderLocaleSwitcher("en");
  assertEquals(html.includes("locale-switcher"), true);
  assertEquals(html.includes("🇺🇸 English (US)"), true);
  assertEquals(html.includes("🇮🇩 Bahasa Indonesia"), true);
  assertEquals(html.includes("href=\"/en\""), true);
  assertEquals(html.includes("href=\"/id\""), true);
  assertEquals(html.includes("Global"), false);
  assertEquals(html.includes("English (Indonesia)"), false);
});
