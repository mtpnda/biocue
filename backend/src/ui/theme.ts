// BioCue Global Stylesheet & Design Language System (DLS)
// Powered strictly by design/dls.tokens.json and design/dls.html principles.
// Calming, neurodivergent-friendly typography, warm tactile oat canvas, zero-anxiety palette.

export const GLOBAL_STYLES = `
  @import url('https://fonts.googleapis.com/css2?family=Atkinson+Hyperlegible+Next:ital,wght@0,400..700;1,400..700&family=Newsreader:ital,opsz,wght@0,6..72,400..700;1,6..72,400..700&display=swap');

  :root, [data-theme="calm"] {
    --surface-bg: #F2F0EA;
    --surface-card: #FCFBF8;
    --surface-raised: #F7F4EE;
    --border-subtle: #E7E2D9;
    --border-strong: #DAD4C8;
    --text-primary: #221F1B;
    --text-secondary: #6F695F;
    --text-muted: #9C958A;
    --action-primary: #2C2823;
    --action-onPrimary: #FBF6EC;
    --shadow-tint: #2B261E;
    
    --state-calm-chip: #86EFAC;
    --state-calm-soft: #DEF7E5;
    --state-calm-ink: #2F7D4E;
    
    --state-rising-chip: #FCD34D;
    --state-rising-soft: #FEF4D6;
    --state-rising-ink: #B4690E;
    
    --state-alert-chip: #FCA5A5;
    --state-alert-soft: #FBE3E1;
    --state-alert-ink: #C0453C; /* Warm Coral, never alarming red! */
    
    --state-info-chip: #93C5FD;
    --state-info-soft: #DDEAFB;
    --state-info-ink: #3F6CC4;
    
    --state-neutral-chip: #D1D5DB;
    --state-neutral-soft: #EDEEF0;
    --state-neutral-ink: #5C6068;

    --radius-sm: 12px;
    --radius-md: 16px;
    --radius-card: 20px;
    --radius-lg: 22px;
    --radius-xl: 28px;
    --radius-pill: 999px;

    --space-xs: 4px;
    --space-sm: 8px;
    --space-md: 12px;
    --space-lg: 16px;
    --space-xl: 20px;
    --space-xxl: 24px;
    --space-section: 36px;
    --space-hero: 64px;

    --size-touchTarget: 48px;
    --size-buttonHeight: 52px;
    --size-iconButtonVisual: 40px;
    --size-avatar: 44px;
    --size-meterHeight: 12px;

    --font-serif: 'Newsreader', 'Iowan Old Style', Georgia, serif;
    --font-sans: 'Atkinson Hyperlegible Next', 'Atkinson Hyperlegible', system-ui, -apple-system, sans-serif;
    --font-mono: ui-monospace, Menlo, Monaco, monospace;

    --shadow-card: 0 4px 14px color-mix(in srgb, var(--shadow-tint) 7%, transparent);
    --shadow-raised: 0 8px 24px color-mix(in srgb, var(--shadow-tint) 11%, transparent);
    --shadow-tactile: 0 3px 0 var(--border-strong);
    --shadow-btn: 0 3px 0 var(--shadow-tint);
  }

  [data-theme="high-contrast"] {
    --surface-bg: #FFFFFF;
    --surface-card: #FFFFFF;
    --surface-raised: #F2F2F2;
    --border-subtle: #4A4A4A;
    --border-strong: #000000;
    --text-primary: #000000;
    --text-secondary: #333333;
    --text-muted: #4A4A4A;
    --action-primary: #000000;
    --action-onPrimary: #FFFFFF;
    --shadow-tint: #000000;

    --state-calm-chip: #86EFAC;
    --state-calm-soft: #E3F6EA;
    --state-calm-ink: #14532D;

    --state-rising-chip: #FCD34D;
    --state-rising-soft: #FFF3C4;
    --state-rising-ink: #6B3F00;

    --state-alert-chip: #FCA5A5;
    --state-alert-soft: #FFE2DE;
    --state-alert-ink: #8A1F17;

    --state-info-chip: #93C5FD;
    --state-info-soft: #DCE9FF;
    --state-info-ink: #1A3A8F;

    --state-neutral-chip: #D1D5DB;
    --state-neutral-soft: #EDEEF0;
    --state-neutral-ink: #2B2E33;
    
    --shadow-card: 0 0 0 2px #000000;
    --shadow-raised: 0 0 0 3px #000000;
    --shadow-tactile: 0 3px 0 #000000;
    --shadow-btn: 0 3px 0 #000000;
  }

  *, *::before, *::after {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
  }

  html {
    scroll-behavior: smooth;
    -webkit-text-size-adjust: 100%;
  }

  body {
    background: var(--surface-bg);
    color: var(--text-primary);
    font-family: var(--font-sans);
    font-size: 15.5px;
    line-height: 23px;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    overflow-x: hidden;
  }

  h1, h2, h3, h4, h5, h6 {
    font-family: var(--font-serif);
    font-weight: 500;
    color: var(--text-primary);
    letter-spacing: -0.02em;
    line-height: 1.18;
  }

  h1 { font-size: clamp(34px, 5.5vw, 56px); margin-bottom: 20px; }
  h2 { font-size: clamp(26px, 3.8vw, 38px); margin-bottom: 16px; }
  h3 { font-size: clamp(20px, 2.8vw, 26px); margin-bottom: 12px; }
  h4 { font-size: 19px; }

  p {
    font-size: 15.5px;
    line-height: 1.6;
    color: var(--text-secondary);
  }

  a {
    color: inherit;
    text-decoration: none;
    transition: color 150ms ease, opacity 150ms ease;
  }

  a:hover {
    color: var(--text-primary);
  }

  button, input, select, textarea {
    font-family: inherit;
    color: inherit;
  }

  :focus-visible {
    outline: 2px solid var(--action-primary);
    outline-offset: 3px;
  }

  .wrap {
    width: 100%;
    max-width: 1140px;
    margin: 0 auto;
    padding: 0 24px;
  }

  /* Header & Navigation */
  .site-header {
    position: sticky;
    top: 0;
    z-index: 100;
    background: color-mix(in srgb, var(--surface-bg) 92%, transparent);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    border-bottom: 1px solid var(--border-subtle);
    transition: background 200ms ease, border-color 200ms ease;
  }

  .header-wrap {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 72px;
    gap: 16px;
  }

  .desktop-nav {
    display: flex;
    align-items: center;
    gap: 24px;
  }

  .nav-link {
    font-size: 14.5px;
    font-weight: 600;
    color: var(--text-secondary);
    padding: 8px 12px;
    border-radius: var(--radius-pill);
    transition: all 150ms ease;
  }

  .nav-link:hover, .nav-link.active {
    color: var(--text-primary);
    background: var(--surface-raised);
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .mobile-menu-btn {
    display: none;
    align-items: center;
    justify-content: center;
    width: 44px;
    height: 44px;
    border-radius: var(--radius-md);
    background: var(--surface-raised);
    border: 1px solid var(--border-subtle);
    color: var(--text-primary);
    cursor: pointer;
    font-size: 20px;
  }

  .mobile-drawer {
    display: none;
    position: fixed;
    top: 72px;
    left: 0;
    right: 0;
    bottom: 0;
    background: var(--surface-card);
    padding: 24px;
    flex-direction: column;
    gap: 16px;
    z-index: 99;
    overflow-y: auto;
    border-top: 1px solid var(--border-subtle);
  }

  .mobile-drawer.open {
    display: flex;
  }

  .mobile-nav-link {
    font-size: 18px;
    font-weight: 600;
    padding: 14px 16px;
    border-radius: var(--radius-md);
    background: var(--surface-raised);
    border: 1px solid var(--border-subtle);
    color: var(--text-primary);
  }

  /* Buttons & Touch Targets */
  .pill {
    min-height: var(--size-buttonHeight);
    padding: 0 24px;
    border-radius: var(--radius-pill);
    font-family: inherit;
    font-weight: 700;
    font-size: 15.5px;
    line-height: 20px;
    cursor: pointer;
    border: 1px solid transparent;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    transition: transform 150ms ease, box-shadow 150ms ease, background 150ms ease;
    white-space: nowrap;
    text-decoration: none;
  }

  .pill:active {
    transform: translateY(1px);
  }

  .pill-sm {
    min-height: 40px;
    padding: 0 16px;
    font-size: 13.5px;
  }

  .pill-primary {
    background: var(--action-primary);
    color: var(--action-onPrimary);
    box-shadow: var(--shadow-btn);
  }

  .pill-primary:hover {
    color: var(--action-onPrimary);
    background: #1a1714;
    transform: translateY(-1px);
  }

  .pill-outline {
    background: var(--surface-card);
    color: var(--text-primary);
    border: 1.5px solid var(--border-strong);
    box-shadow: 0 2px 0 var(--border-strong);
  }

  .pill-outline:hover {
    background: var(--surface-raised);
    transform: translateY(-1px);
  }

  .pill-coral {
    background: var(--state-alert-soft);
    color: var(--state-alert-ink);
    border: 1.5px solid var(--state-alert-chip);
    box-shadow: 0 2px 0 var(--state-alert-chip);
  }

  .pill-coral:hover {
    background: #fadcd9;
  }

  .pill-calm {
    background: var(--state-calm-soft);
    color: var(--state-calm-ink);
    border: 1.5px solid var(--state-calm-chip);
  }

  /* Soft Cards & Surfaces */
  .softcard {
    background: var(--surface-card);
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-card);
    padding: 24px;
    box-shadow: var(--shadow-card);
    transition: transform 200ms ease, box-shadow 200ms ease;
  }

  .softcard.hoverable:hover {
    transform: translateY(-3px);
    box-shadow: var(--shadow-raised);
  }

  .softcard-raised {
    background: var(--surface-raised);
    border: 1px solid var(--border-strong);
    border-radius: var(--radius-card);
    padding: 24px;
  }

  /* Status Pills & Indicators */
  .status {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    min-height: 32px;
    padding: 0 12px;
    border-radius: var(--radius-pill);
    background: var(--surface-raised);
    font-size: 13px;
    font-weight: 600;
    color: var(--text-primary);
    border: 1px solid var(--border-subtle);
  }

  .status i {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    display: inline-block;
  }

  .pulse-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--state-calm-ink);
    box-shadow: 0 0 0 3px var(--state-calm-soft);
    display: inline-block;
  }

  .meter {
    height: var(--size-meterHeight);
    border-radius: var(--radius-pill);
    background: var(--surface-raised);
    margin: 12px 0;
    overflow: hidden;
    border: 1px solid var(--border-subtle);
  }

  .meter span {
    display: block;
    height: 100%;
    background: var(--state-calm-chip);
    transition: width 400ms cubic-bezier(0.2, 0.7, 0.3, 1), background-color 300ms ease;
  }

  /* Form Elements */
  .field, select, input[type="text"], input[type="email"], textarea {
    min-height: var(--size-touchTarget);
    border: 1px solid var(--border-strong);
    border-radius: var(--radius-md);
    background: var(--surface-card);
    color: var(--text-primary);
    padding: 10px 16px;
    font-size: 15px;
    width: 100%;
    box-sizing: border-box;
    transition: border-color 150ms ease, box-shadow 150ms ease;
  }

  .field:focus, input:focus, textarea:focus {
    outline: none;
    border-color: var(--action-primary);
    box-shadow: 0 0 0 3px color-mix(in srgb, var(--action-primary) 15%, transparent);
  }

  /* Footer */
  .site-footer {
    border-top: 1px solid var(--border-subtle);
    background: var(--surface-card);
    padding: 64px 0 36px;
    margin-top: auto;
  }

  /* Reduced Motion */
  @media (prefers-reduced-motion: reduce) {
    *, *::before, *::after {
      animation-duration: 0.01ms !important;
      animation-iteration-count: 1 !important;
      transition-duration: 0.01ms !important;
      scroll-behavior: auto !important;
    }
  }

  /* Responsive Queries */
  @media (max-width: 860px) {
    .desktop-nav {
      display: none;
    }
    .mobile-menu-btn {
      display: inline-flex;
    }
    .hide-mobile {
      display: none !important;
    }
    .header-wrap {
      height: 64px;
    }
  }
`;
