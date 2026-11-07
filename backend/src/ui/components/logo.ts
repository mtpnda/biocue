// BioCue Brand Logo Component
// Features official "Mindful Contours" bilateral cerebral branding with sage equilibrium bead

export interface LogoOptions {
  size?: number;
  showTag?: boolean;
}

export function renderLogo(opts: LogoOptions = {}): string {
  const size = opts.size ?? 36;
  const showTag = opts.showTag ?? true;
  const radius = Math.round(size * 0.25);

  return `
    <div style="display:inline-flex;align-items:center;gap:12px;user-select:none;">
      <img src="/icon.png" width="${size}" height="${size}" alt="BioCue Icon" style="width:${size}px;height:${size}px;border-radius:${radius}px;object-fit:cover;box-shadow:0 2px 6px rgba(0,0,0,0.18);flex-shrink:0;background:#1E1F20;" />
      <div style="display:flex;flex-direction:column;line-height:1.15;">
        <span style="font-family:var(--font-serif);font-size:${size >= 36 ? '24px' : '20px'};font-weight:600;letter-spacing:-0.03em;color:var(--text-primary);">BioCue</span>
        ${showTag ? `<span style="font-size:11px;font-weight:700;letter-spacing:0.04em;text-transform:uppercase;color:var(--text-muted);margin-top:2px;">Sensory Protection</span>` : ''}
      </div>
    </div>
  `;
}
