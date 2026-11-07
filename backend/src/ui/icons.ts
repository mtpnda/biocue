// BioCue Bespoke Sensory Iconography
// Neurodivergent-friendly vector glyphs crafted strictly on a 24x24 grid with 2px rounded strokes.
// Zero chaotic colors, zero harsh angles, zero alarming flash cues.

export type IconName =
  | "calm"
  | "overwhelmed"
  | "excited"
  | "active"
  | "brain"
  | "loud-noises"
  | "crowded"
  | "task-demand"
  | "routine-change"
  | "unknown-context"
  | "caregiver"
  | "soothing-audio"
  | "privacy-lock"
  | "sos-location"
  | "pairing-link"
  | "telemetry-sync"
  | "check"
  | "arrow-right"
  | "shield"
  | "heart-pulse";

export function renderIcon(name: IconName, size = 24, className = ""): string {
  const common = `width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="${className}"`;

  switch (name) {
    case "calm":
      return `<svg ${common}><path d="M12 22V12"/><path d="M12 12C12 7 16 3 21 3c0 5-4 9-9 9Z" fill="currentColor" fill-opacity="0.2"/><path d="M12 12C12 7 16 3 21 3c0 5-4 9-9 9Z"/><path d="M12 15c-3 0-6-2-7-5 3.5 0 6.5 2 7 5Z" fill="currentColor" fill-opacity="0.2"/><path d="M12 15c-3 0-6-2-7-5 3.5 0 6.5 2 7 5Z"/></svg>`;
    
    case "overwhelmed":
      return `<svg ${common}><path d="M17.5 13a4.5 4.5 0 0 0-1.2-8.8A6 6 0 0 0 5 8.5 4.5 4.5 0 0 0 6.5 17h11a3.5 3.5 0 0 0 0-7Z" fill="currentColor" fill-opacity="0.18"/><path d="M17.5 13a4.5 4.5 0 0 0-1.2-8.8A6 6 0 0 0 5 8.5 4.5 4.5 0 0 0 6.5 17h11a3.5 3.5 0 0 0 0-7Z"/><path d="M8 20.5c1-.7 2-.7 3 0s2 .7 3 0 2-.7 3 0"/></svg>`;

    case "excited":
      return `<svg ${common}><path d="M12 3c0 4.5 4.5 9 9 9-4.5 0-9 4.5-9 9 0-4.5-4.5-9-9-9 4.5 0 9-4.5 9-9Z" fill="currentColor" fill-opacity="0.2"/><path d="M12 3c0 4.5 4.5 9 9 9-4.5 0-9 4.5-9 9 0-4.5-4.5-9-9-9 4.5 0 9-4.5 9-9Z"/><circle cx="19" cy="5" r="1.5" fill="currentColor"/><circle cx="5" cy="19" r="1.5" fill="currentColor"/></svg>`;

    case "active":
      return `<svg ${common}><circle cx="16" cy="4" r="2" fill="currentColor" fill-opacity="0.2"/><circle cx="16" cy="4" r="2"/><path d="M7 21l3-6 4 2 2-5-4-3-3 2-3-2"/><path d="M14 17l2 4"/><path d="M3 13h4"/></svg>`;

    case "brain":
      return `<svg width="${size}" height="${size}" viewBox="0 0 512 512" fill="none" class="${className}"><path d="M 220,116 L 166,91 L 106,136 L 91,216 L 131,276 L 96,341 L 126,411 L 206,411 L 226,371" stroke="currentColor" stroke-width="26" stroke-linecap="round" stroke-linejoin="round"/><path d="M 292,116 L 346,91 L 406,136 L 421,216 L 381,276 L 416,341 L 386,411 L 306,411 L 286,371" stroke="currentColor" stroke-width="26" stroke-linecap="round" stroke-linejoin="round"/><path d="M 226,166 L 171,166 L 156,221 L 191,271 L 161,321 L 191,361 L 226,341" stroke="currentColor" stroke-width="20" stroke-linecap="round" stroke-linejoin="round"/><path d="M 286,166 L 341,166 L 356,221 L 321,271 L 351,321 L 321,361 L 286,341" stroke="currentColor" stroke-width="20" stroke-linecap="round" stroke-linejoin="round"/><circle cx="256" cy="256" r="22" fill="#86EFAC"/></svg>`;

    case "loud-noises":
      return `<svg ${common}><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" fill="currentColor" fill-opacity="0.18"/><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M15.54 8.46a5 5 0 0 1 0 7.07"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14"/></svg>`;

    case "crowded":
      return `<svg ${common}><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4" fill="currentColor" fill-opacity="0.2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>`;

    case "task-demand":
      return `<svg ${common}><rect x="4" y="5" width="16" height="16" rx="3" fill="currentColor" fill-opacity="0.15"/><rect x="4" y="5" width="16" height="16" rx="3"/><path d="M9 3h6a1 1 0 0 1 1 1v1H8V4a1 1 0 0 1 1-1Z"/><path d="M8 11h8"/><path d="M8 15h5"/></svg>`;

    case "routine-change":
      return `<svg ${common}><path d="M21 12a9 9 0 0 0-15.5-6.36L3 8"/><polyline points="3 3 3 8 8 8"/><path d="M3 12a9 9 0 0 0 15.5 6.36L21 16"/><polyline points="21 21 21 16 16 16"/></svg>`;

    case "unknown-context":
      return `<svg ${common}><circle cx="12" cy="12" r="9" fill="currentColor" fill-opacity="0.15"/><circle cx="12" cy="12" r="9"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><circle cx="12" cy="17" r="0.75" fill="currentColor"/></svg>`;

    case "caregiver":
      return `<svg ${common}><path d="M19 21v-2a5 5 0 0 0-5-5H10a5 5 0 0 0-5 5v2"/><circle cx="12" cy="7" r="4" fill="currentColor" fill-opacity="0.18"/><circle cx="12" cy="7" r="4"/></svg>`;

    case "soothing-audio":
      return `<svg ${common}><path d="M12 2a10 10 0 0 0-9 14.5L2 22l5.5-1A10 10 0 1 0 12 2Z" fill="currentColor" fill-opacity="0.18"/><path d="M8 12a4 4 0 0 1 8 0"/><path d="M10 12a2 2 0 0 1 4 0"/><circle cx="12" cy="12" r="1" fill="currentColor"/></svg>`;

    case "privacy-lock":
      return `<svg ${common}><rect x="4" y="11" width="16" height="10" rx="3" fill="currentColor" fill-opacity="0.18"/><rect x="4" y="11" width="16" height="10" rx="3"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/><circle cx="12" cy="16" r="1" fill="currentColor"/></svg>`;

    case "sos-location":
      return `<svg ${common}><path d="M12 21s-7-5.5-7-11.5a7 7 0 1 1 14 0c0 6-7 11.5-7 11.5Z" fill="currentColor" fill-opacity="0.18"/><path d="M12 21s-7-5.5-7-11.5a7 7 0 1 1 14 0c0 6-7 11.5-7 11.5Z"/><circle cx="12" cy="9.5" r="2.5" fill="currentColor"/></svg>`;

    case "pairing-link":
      return `<svg ${common}><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>`;

    case "telemetry-sync":
      return `<svg ${common}><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/></svg>`;

    case "check":
      return `<svg ${common}><polyline points="20 6 9 17 4 12"/></svg>`;

    case "arrow-right":
      return `<svg ${common}><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>`;

    case "shield":
      return `<svg ${common}><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>`;

    case "heart-pulse":
      return `<svg ${common}><path d="M19.5 12.572l-7.5 7.428l-7.5 -7.428a5 5 0 1 1 7.5 -6.566a5 5 0 1 1 7.5 6.572"/><path d="M12 7v5"/></svg>`;

    default:
      return `<svg ${common}><circle cx="12" cy="12" r="10"/></svg>`;
  }
}
