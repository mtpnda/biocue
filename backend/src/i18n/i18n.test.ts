import { assertEquals } from "jsr:@std/assert";
import { resolveLocale, getDictionary, renderLocaleSwitcher } from "./index.ts";

Deno.test("resolveLocale parses direct path prefixes", () => {
  assertEquals(resolveLocale("https://biocue.app/en"), "en");
  assertEquals(resolveLocale("https://biocue.app/en/"), "en");
  assertEquals(resolveLocale("https://biocue.app/en-id"), "en-id");
  assertEquals(resolveLocale("https://biocue.app/id-id"), "id-id");
  assertEquals(resolveLocale("https://biocue.app/id"), "id-id");
});

Deno.test("resolveLocale falls back to en for default root", () => {
  assertEquals(resolveLocale("https://biocue.app/"), "en");
});

Deno.test("resolveLocale respects cf-ipcountry header", () => {
  const headers = new Headers({ "cf-ipcountry": "ID" });
  assertEquals(resolveLocale("https://biocue.app/", headers), "en-id");

  const idHeaders = new Headers({ "cf-ipcountry": "ID", "accept-language": "id-ID,id;q=0.9" });
  assertEquals(resolveLocale("https://biocue.app/", idHeaders), "id-id");
});

Deno.test("getDictionary returns valid translation schema", () => {
  const en = getDictionary("en");
  assertEquals(en.locale, "en");
  assertEquals(en.hero.titleHighlight.length > 0, true);

  const id = getDictionary("id-id");
  assertEquals(id.locale, "id-id");
  assertEquals(id.hero.titleHighlight.length > 0, true);
});

Deno.test("renderLocaleSwitcher outputs options", () => {
  const html = renderLocaleSwitcher("en");
  assertEquals(html.includes("locale-switcher"), true);
  assertEquals(html.includes("/en-id"), true);
  assertEquals(html.includes("/id-id"), true);
});
