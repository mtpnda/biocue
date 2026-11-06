import { Dictionary } from "../types.ts";
import { enDictionary } from "./en.ts";

export const enIdDictionary: Dictionary = {
  ...enDictionary,
  locale: "en-id",
  localeName: "English (Indonesia)",
  flag: "🇮🇩",
};
