// BioCue Brand Logo Component
// Symmetric dual-hemisphere cerebral contour with equilibrium botanical cue

export interface LogoOptions {
  size?: number;
  showTag?: boolean;
}

export function renderLogo(opts: LogoOptions = {}): string {
  const size = opts.size ?? 36;
  const showTag = opts.showTag ?? true;

  return `
    <div style="display:inline-flex;align-items:center;gap:12px;user-select:none;">
      <div style="width:${size}px;height:${size}px;border-radius:12px;background:var(--action-primary);color:var(--action-onPrimary);display:flex;align-items:center;justify-content:center;box-shadow:0 3px 0 var(--shadow-tint);flex-shrink:0;">
        <svg width="${size * 0.6}" height="${size * 0.6}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M9.5 2A4.5 4.5 0 0 0 5 6.5a4.4 4.4 0 0 0 .5 2A4.5 4.5 0 0 0 4 12a4.5 4.5 0 0 0 1.5 3.4A4.5 4.5 0 0 0 9 20h.5"/>
          <path d="M14.5 2A4.5 4.5 0 0 1 19 6.5a4.4 4.4 0 0 1-.5 2A4.5 4.5 0 0 1 20 12a4.5 4.5 0 0 1-1.5 3.4A4.5 4.5 0 0 1 15 20h-.5"/>
          <path d="M12 4v16"/>
          <path d="M9.5 9a2.5 2.5 0 0 0-2.5 2.5c0 1.4 1.1 2.5 2.5 2.5"/>
          <path d="M14.5 9a2.5 2.5 0 0 1 2.5 2.5c0 1.4-1.1 2.5-2.5 2.5"/>
        </svg>
      </div>
      <div style="display:flex;flex-direction:column;line-height:1.15;">
        <span style="font-family:var(--font-serif);font-size:${size >= 36 ? '24px' : '20px'};font-weight:600;letter-spacing:-0.03em;color:var(--text-primary);">BioCue</span>
        ${showTag ? `<span style="font-size:11px;font-weight:700;letter-spacing:0.04em;text-transform:uppercase;color:var(--text-muted);margin-top:2px;">Sensory Protection</span>` : ''}
      </div>
    </div>
  `;
}
