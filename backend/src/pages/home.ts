// BioCue Official Homepage
// 100% Free & Open-Source Assistive Bio-Signal Platform
// Adheres strictly to design/dls.tokens.json and zero-anxiety UI principles.

import { renderLayout } from "../ui/layout.ts";
import { resolveLocale, getDictionary } from "../i18n/index.ts";
import { renderIcon } from "../ui/icons.ts";

export function renderHomePage(requestUrl: string, headers?: Headers): string {
  const locale = resolveLocale(requestUrl, headers);
  const dict = getDictionary(locale);

  const extraHead = `
    <style>
      /* Hero Layout */
      .hero-section {
        padding: clamp(48px, 8vw, 84px) 0 48px;
        text-align: center;
      }
      .hero-content {
        max-width: 860px;
        margin: 0 auto;
      }
      .hero-badge {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        background: var(--surface-card);
        border: 1px solid var(--border-strong);
        border-radius: var(--radius-pill);
        padding: 6px 18px;
        font-size: 13.5px;
        font-weight: 700;
        box-shadow: var(--shadow-tactile);
        margin-bottom: 24px;
      }
      .hero-title {
        font-size: clamp(36px, 6.2vw, 62px);
        line-height: 1.1;
        letter-spacing: -0.03em;
        margin-bottom: 24px;
      }
      .hero-highlight {
        display: inline-block;
        background: var(--state-calm-soft);
        color: var(--state-calm-ink);
        padding: 0 12px;
        border-radius: var(--radius-md);
        border: 1px solid var(--state-calm-chip);
      }
      .hero-sub {
        font-size: clamp(17px, 2.3vw, 20px);
        line-height: 1.6;
        color: var(--text-secondary);
        max-width: 720px;
        margin: 0 auto 36px;
      }
      .hero-actions {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        justify-content: center;
        gap: 16px;
        margin-bottom: 54px;
      }

      /* Stat Ribbon */
      .stat-ribbon {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 18px;
        background: var(--surface-card);
        border: 1px solid var(--border-subtle);
        border-radius: var(--radius-card);
        padding: 28px 24px;
        box-shadow: var(--shadow-card);
        margin-bottom: 72px;
        text-align: left;
      }
      .stat-box {
        display: flex;
        flex-direction: column;
        gap: 6px;
        padding: 0 12px;
        border-right: 1px dashed var(--border-subtle);
      }
      .stat-box:last-child {
        border-right: none;
      }
      .stat-val {
        font-family: var(--font-serif);
        font-size: 34px;
        font-weight: 700;
        color: var(--text-primary);
        letter-spacing: -0.02em;
        line-height: 1;
      }
      .stat-label {
        font-size: 13.5px;
        color: var(--text-secondary);
        font-weight: 600;
      }

      /* Simulator Section */
      .sim-stage {
        background: var(--surface-card);
        border: 1px solid var(--border-subtle);
        border-radius: var(--radius-xl);
        padding: clamp(24px, 4vw, 44px);
        box-shadow: var(--shadow-card);
        margin-top: 36px;
      }
      .sim-controls {
        display: flex;
        flex-wrap: wrap;
        gap: 12px;
        justify-content: center;
        margin-bottom: 36px;
      }
      .sim-btn {
        min-height: 48px;
        padding: 0 20px;
        border-radius: var(--radius-pill);
        font-size: 14.5px;
        font-weight: 700;
        cursor: pointer;
        border: 1.5px solid var(--border-strong);
        background: var(--surface-raised);
        color: var(--text-primary);
        transition: all 150ms ease;
      }
      .sim-btn.active {
        background: var(--action-primary);
        color: var(--action-onPrimary);
        border-color: var(--action-primary);
        box-shadow: var(--shadow-btn);
      }
      .sim-viewports {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
        gap: 32px;
        align-items: center;
      }
      .watch-frame {
        width: 280px;
        height: 280px;
        border-radius: 50%;
        background: #11100E;
        border: 12px solid #2C2823;
        box-shadow: 0 12px 30px rgba(0,0,0,0.25);
        margin: 0 auto;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 24px;
        text-align: center;
        position: relative;
        transition: border-color 300ms ease;
      }
      .watch-sprout-halo {
        width: 80px;
        height: 80px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 12px;
        transition: background 400ms ease, color 400ms ease;
      }
      .phone-card-preview {
        background: var(--surface-raised);
        border: 1px solid var(--border-strong);
        border-radius: var(--radius-card);
        padding: 24px;
      }

      /* Step Cards */
      .step-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
        gap: 24px;
        margin-top: 36px;
      }
      .step-card {
        background: var(--surface-card);
        border: 1px solid var(--border-subtle);
        border-radius: var(--radius-card);
        padding: 32px 24px;
        box-shadow: var(--shadow-card);
        display: flex;
        flex-direction: column;
        gap: 16px;
      }
      .step-num {
        width: 40px;
        height: 40px;
        border-radius: var(--radius-md);
        background: var(--surface-raised);
        border: 1px solid var(--border-strong);
        display: flex;
        align-items: center;
        justify-content: center;
        font-family: var(--font-serif);
        font-size: 20px;
        font-weight: 700;
        color: var(--text-primary);
      }

      /* Comparison Table */
      .comp-table {
        width: 100%;
        border-collapse: separate;
        border-spacing: 0;
        margin-top: 36px;
        border-radius: var(--radius-card);
        overflow: hidden;
        border: 1px solid var(--border-subtle);
        background: var(--surface-card);
      }
      .comp-table th, .comp-table td {
        padding: 16px 20px;
        text-align: left;
        border-bottom: 1px solid var(--border-subtle);
        font-size: 14.5px;
      }
      .comp-table th {
        background: var(--surface-raised);
        font-weight: 700;
        font-size: 13.5px;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        color: var(--text-muted);
      }
      .comp-table tr:last-child td {
        border-bottom: none;
      }
      .comp-highlight {
        background: color-mix(in srgb, var(--state-calm-soft) 40%, transparent);
        font-weight: 600;
      }

      /* FAQ Accordion */
      .faq-list {
        display: flex;
        flex-direction: column;
        gap: 16px;
        margin-top: 36px;
      }
      .faq-card {
        background: var(--surface-card);
        border: 1px solid var(--border-subtle);
        border-radius: var(--radius-card);
        padding: 24px;
        cursor: pointer;
        box-shadow: var(--shadow-card);
      }
      .faq-q {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 16px;
        font-size: 17px;
        font-weight: 700;
        color: var(--text-primary);
      }
      .faq-a {
        margin-top: 14px;
        font-size: 15px;
        line-height: 1.6;
        color: var(--text-secondary);
        display: none;
      }
      .faq-card.open .faq-a {
        display: block;
      }
      .faq-card.open .faq-chevron {
        transform: rotate(180deg);
      }
      .faq-chevron {
        transition: transform 200ms ease;
      }
    </style>
  `;

  const content = `
    <!-- 1. Hero Section -->
    <section class="hero-section">
      <div class="wrap hero-content">
        <div class="hero-badge">
          ${renderIcon("calm", 18)}
          <span>${dict.hero.badge}</span>
        </div>

        <h1 class="hero-title">
          ${dict.hero.titleLine1}<br/>
          <span class="hero-highlight">${dict.hero.titleHighlight}</span><br/>
          ${dict.hero.titleLine2}
        </h1>

        <p class="hero-sub">
          ${dict.hero.sub}
        </p>

        <div class="hero-actions">
          <a href="#download" class="pill pill-primary">
            ${renderIcon("heart-pulse", 20)}
            <span>${dict.hero.ctaDownload}</span>
          </a>
          <a href="#simulator" class="pill pill-outline">
            <span>${dict.hero.ctaExplore}</span>
          </a>
        </div>

        <!-- 4-Stat Value Ribbon -->
        <div class="stat-ribbon">
          <div class="stat-box">
            <span class="stat-val">${dict.hero.stat1Val}</span>
            <span class="stat-label">${dict.hero.stat1Label}</span>
          </div>
          <div class="stat-box">
            <span class="stat-val">${dict.hero.stat2Val}</span>
            <span class="stat-label">${dict.hero.stat2Label}</span>
          </div>
          <div class="stat-box">
            <span class="stat-val">${dict.hero.stat3Val}</span>
            <span class="stat-label">${dict.hero.stat3Label}</span>
          </div>
          <div class="stat-box">
            <span class="stat-val">${dict.hero.stat4Val}</span>
            <span class="stat-label">${dict.hero.stat4Label}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 2. Interactive Sensory Anchor Simulator -->
    <section id="simulator" style="padding:var(--space-section) 0;">
      <div class="wrap">
        <div style="text-align:center;max-width:740px;margin:0 auto 24px;">
          <span class="status" style="background:var(--state-calm-soft);color:var(--state-calm-ink);border-color:var(--state-calm-chip);margin-bottom:14px;">
            <i></i>${dict.simulator.badge}
          </span>
          <h2>${dict.simulator.title}</h2>
          <p>${dict.simulator.sub}</p>
        </div>

        <div class="sim-stage">
          <!-- State Selector Tabs -->
          <div class="sim-controls" role="tablist">
            <button type="button" class="sim-btn active" data-state="calm" onclick="setSimState('calm')">${dict.simulator.calmBtn}</button>
            <button type="button" class="sim-btn" data-state="rising" onclick="setSimState('rising')">${dict.simulator.risingBtn}</button>
            <button type="button" class="sim-btn" data-state="alert" onclick="setSimState('alert')">${dict.simulator.alertBtn}</button>
            <button type="button" class="sim-btn" data-state="active" onclick="setSimState('active')">${dict.simulator.activeBtn}</button>
          </div>

          <!-- Dual Device Previews -->
          <div class="sim-viewports">
            <!-- Wear OS Smartwatch Round Face -->
            <div style="text-align:center;">
              <span class="section-header" style="display:block;margin-bottom:14px;">${dict.simulator.watchViewTitle}</span>
              <div class="watch-frame" id="watchFace">
                <div class="watch-sprout-halo" id="watchHalo" style="background:rgba(134,239,172,0.18);color:#86EFAC;">
                  <span id="watchIcon">${renderIcon("calm", 44)}</span>
                </div>
                <div style="color:#FCFBF8;font-family:var(--font-serif);font-size:18px;margin-bottom:4px;" id="watchStatusText">
                  Alex is calm
                </div>
                <div style="color:#DAD4C8;font-size:12px;margin-bottom:14px;" id="watchSubtitle">
                  Resting equilibrium
                </div>
                <!-- Zero-Anxiety Breathing Guide -->
                <div style="width:140px;height:6px;border-radius:999px;background:#2C2823;overflow:hidden;margin-bottom:14px;">
                  <div id="watchMeter" style="width:30%;height:100%;background:#86EFAC;transition:all 400ms ease;"></div>
                </div>
                <button type="button" style="background:#2C2823;color:#DAD4C8;border:1px solid #6F695F;font-size:11px;font-weight:700;padding:4px 14px;border-radius:999px;cursor:pointer;">
                  SOS Help
                </button>
              </div>
            </div>

            <!-- Android Companion Phone Surface -->
            <div>
              <span class="section-header" style="display:block;margin-bottom:14px;">${dict.simulator.phoneViewTitle}</span>
              <div class="phone-card-preview" id="phonePreview">
                <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px;">
                  <div style="display:flex;align-items:center;gap:10px;">
                    <span class="avatar" id="phoneAvatar">${renderIcon("calm", 22)}</span>
                    <div>
                      <div style="font-weight:700;font-size:16px;">Alex's Equilibrium</div>
                      <div style="font-size:12px;color:var(--text-muted);" id="phoneSubhead">Galaxy Watch 4 • Bluetooth Synced</div>
                    </div>
                  </div>
                  <span class="status" id="phoneBadge" style="background:var(--state-calm-soft);color:var(--state-calm-ink);border-color:var(--state-calm-chip);">
                    Calm
                  </span>
                </div>

                <div class="meter">
                  <span id="phoneMeter" style="width:28%;background:var(--state-calm-chip);"></span>
                </div>
                <div style="display:flex;justify-content:space-between;font-size:12px;color:var(--text-muted);margin-bottom:18px;">
                  <span>Equilibrium</span>
                  <span>Arousal</span>
                  <span>Support</span>
                </div>

                <div style="background:var(--surface-card);border:1px solid var(--border-subtle);border-radius:var(--radius-md);padding:14px;margin-bottom:16px;">
                  <div style="font-size:13px;font-weight:700;margin-bottom:4px;" id="phoneInsightTitle">Ambient Sensory Tone: Resting</div>
                  <div style="font-size:12.5px;color:var(--text-secondary);" id="phoneInsightDesc">Heart rate variability is stable. Autonomic nervous system shows parasympathetic regulation.</div>
                </div>

                <div style="display:flex;gap:8px;flex-wrap:wrap;">
                  <button type="button" class="pill pill-outline pill-sm">
                    ${renderIcon("soothing-audio", 16)} Grounding Audio
                  </button>
                  <button type="button" class="pill pill-coral pill-sm">
                    ${renderIcon("sos-location", 16)} Emergency Beacon
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 3. Three-Stage Protection Loop ("How It Works") -->
    <section id="how-it-works" style="padding:var(--space-section) 0;">
      <div class="wrap">
        <div style="text-align:center;max-width:720px;margin:0 auto;">
          <span class="status" style="background:var(--state-info-soft);color:var(--state-info-ink);border-color:var(--state-info-chip);margin-bottom:14px;">
            <i></i>${dict.howItWorks.badge}
          </span>
          <h2>${dict.howItWorks.title}</h2>
          <p>${dict.howItWorks.sub}</p>
        </div>

        <div class="step-grid">
          <div class="step-card">
            <div class="step-num">1</div>
            <h3 style="margin:0;font-size:20px;">${dict.howItWorks.step1Title}</h3>
            <p style="margin:0;font-size:14.5px;">${dict.howItWorks.step1Desc}</p>
            <div style="margin-top:auto;padding-top:12px;border-top:1px solid var(--border-subtle);font-size:12.5px;color:var(--text-muted);display:flex;align-items:center;gap:6px;">
              ${renderIcon("shield", 16)} ${dict.howItWorks.step1Badge}
            </div>
          </div>

          <div class="step-card">
            <div class="step-num">2</div>
            <h3 style="margin:0;font-size:20px;">${dict.howItWorks.step2Title}</h3>
            <p style="margin:0;font-size:14.5px;">${dict.howItWorks.step2Desc}</p>
            <div style="margin-top:auto;padding-top:12px;border-top:1px solid var(--border-subtle);font-size:12.5px;color:var(--text-muted);display:flex;align-items:center;gap:6px;">
              ${renderIcon("privacy-lock", 16)} ${dict.howItWorks.step2Badge}
            </div>
          </div>

          <div class="step-card">
            <div class="step-num">3</div>
            <h3 style="margin:0;font-size:20px;">${dict.howItWorks.step3Title}</h3>
            <p style="margin:0;font-size:14.5px;">${dict.howItWorks.step3Desc}</p>
            <div style="margin-top:auto;padding-top:12px;border-top:1px solid var(--border-subtle);font-size:12.5px;color:var(--text-muted);display:flex;align-items:center;gap:6px;">
              ${renderIcon("pairing-link", 16)} ${dict.howItWorks.step3Badge}
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 4. Core Features & Neurodivergent Architecture -->
    <section id="features" style="padding:var(--space-section) 0;">
      <div class="wrap">
        <div style="text-align:center;max-width:720px;margin:0 auto 36px;">
          <span class="status" style="background:var(--state-rising-soft);color:var(--state-rising-ink);border-color:var(--state-rising-chip);margin-bottom:14px;">
            <i></i>${dict.features.badge}
          </span>
          <h2>${dict.features.title}</h2>
          <p>${dict.features.sub}</p>
        </div>

        <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(320px, 1fr));gap:24px;">
          ${dict.features.cards.map((c) => {
            const cardStyles: Record<string, { bg: string; color: string; border?: string }> = {
              "calm": { bg: "var(--state-calm-soft)", color: "var(--state-calm-ink)" },
              "brain": { bg: "var(--state-info-soft)", color: "var(--state-info-ink)" },
              "caregiver": { bg: "var(--state-neutral-soft)", color: "var(--state-neutral-ink)" },
              "sos-location": { bg: "var(--state-alert-soft)", color: "var(--state-alert-ink)" },
              "privacy-lock": { bg: "var(--surface-raised)", color: "var(--text-primary)", border: "border:1px solid var(--border-strong);" },
              "excited": { bg: "var(--state-rising-soft)", color: "var(--state-rising-ink)" },
            };
            const s = cardStyles[c.icon] || { bg: "var(--surface-raised)", color: "var(--text-primary)", border: "" };
            return `
              <div class="softcard">
                <div style="width:44px;height:44px;border-radius:12px;background:${s.bg};color:${s.color};${s.border ?? ''}display:flex;align-items:center;justify-content:center;margin-bottom:16px;">
                  ${renderIcon(c.icon, 24)}
                </div>
                <h3 style="font-size:19px;margin-bottom:8px;">${c.title}</h3>
                <p style="font-size:14.5px;margin:0;">${c.desc}</p>
              </div>
            `;
          }).join("")}
        </div>
      </div>
    </section>

    <!-- 5. Architectural Comparison Matrix -->
    <section style="padding:var(--space-section) 0;">
      <div class="wrap">
        <div style="text-align:center;max-width:720px;margin:0 auto 24px;">
          <span class="status" style="margin-bottom:14px;">${dict.comparison.badge}</span>
          <h2>${dict.comparison.title}</h2>
          <p>${dict.comparison.sub}</p>
        </div>

        <div style="overflow-x:auto;">
          <table class="comp-table">
            <thead>
              <tr>
                <th>${dict.comparison.colCapability}</th>
                <th class="comp-highlight" style="color:var(--state-calm-ink);">${dict.comparison.colBiocue}</th>
                <th>${dict.comparison.colFitness}</th>
                <th>${dict.comparison.colSurveillance}</th>
              </tr>
            </thead>
            <tbody>
              ${dict.comparison.rows.map((row) => `
                <tr>
                  <td><strong>${row.capability}</strong></td>
                  <td class="comp-highlight">${row.biocue}</td>
                  <td>${row.fitness}</td>
                  <td>${row.surveillance}</td>
                </tr>
              `).join("")}
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- 6. FAQ Section -->
    <section id="faq" style="padding:var(--space-section) 0;">
      <div class="wrap" style="max-width:820px;">
        <div style="text-align:center;margin-bottom:36px;">
          <span class="status" style="margin-bottom:14px;">${dict.faq.badge}</span>
          <h2>${dict.faq.title}</h2>
          <p>${dict.faq.sub}</p>
        </div>

        <div class="faq-list">
          ${dict.faq.items.map((item, idx) => `
            <div class="faq-card ${idx === 0 ? 'open' : ''}" onclick="this.classList.toggle('open');">
              <div class="faq-q">
                <span>${item.q}</span>
                <span class="faq-chevron" style="color:var(--text-muted);">${renderIcon("arrow-right", 18)}</span>
              </div>
              <div class="faq-a">${item.a}</div>
            </div>
          `).join("")}
        </div>
      </div>
    </section>

    <!-- 7. Download & Release Hub -->
    <section id="download" style="padding:var(--space-hero) 0;">
      <div class="wrap">
        <div style="text-align:center;max-width:720px;margin:0 auto 40px;">
          <span class="status" style="background:var(--state-calm-soft);color:var(--state-calm-ink);border-color:var(--state-calm-chip);margin-bottom:14px;">
            <i></i>${dict.download.badge}
          </span>
          <h2>${dict.download.title}</h2>
          <p>${dict.download.sub}</p>
        </div>

        <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(300px, 1fr));gap:28px;max-width:980px;margin:0 auto;">
          <!-- Wear OS Card -->
          <div class="softcard" style="padding:32px;display:flex;flex-direction:column;justify-content:space-between;">
            <div>
              <div style="color:var(--action-primary);margin-bottom:16px;">${renderIcon("heart-pulse", 36)}</div>
              <h3 style="font-size:22px;margin-bottom:10px;">${dict.download.wearTitle}</h3>
              <p style="font-size:14.5px;color:var(--text-secondary);margin-bottom:24px;">${dict.download.wearSub}</p>
            </div>
            <a href="https://github.com/mtpnda/biocue/releases/latest" target="_blank" rel="noopener" class="pill pill-primary" style="width:100%;justify-content:center;">
              ${dict.download.wearCta}
            </a>
          </div>

          <!-- Android Companion Card -->
          <div class="softcard" style="padding:32px;display:flex;flex-direction:column;justify-content:space-between;border:2px solid var(--action-primary);">
            <div>
              <div style="color:var(--action-primary);margin-bottom:16px;">${renderIcon("caregiver", 36)}</div>
              <h3 style="font-size:22px;margin-bottom:10px;">${dict.download.mobileTitle}</h3>
              <p style="font-size:14.5px;color:var(--text-secondary);margin-bottom:24px;">${dict.download.mobileSub}</p>
            </div>
            <a href="https://github.com/mtpnda/biocue/releases/latest" target="_blank" rel="noopener" class="pill pill-primary" style="width:100%;justify-content:center;">
              ${dict.download.mobileCta}
            </a>
          </div>

          <!-- Open Source Card -->
          <div class="softcard" style="padding:32px;display:flex;flex-direction:column;justify-content:space-between;">
            <div>
              <div style="color:var(--action-primary);margin-bottom:16px;">${renderIcon("privacy-lock", 36)}</div>
              <h3 style="font-size:22px;margin-bottom:10px;">${dict.download.githubTitle}</h3>
              <p style="font-size:14.5px;color:var(--text-secondary);margin-bottom:24px;">${dict.download.githubSub}</p>
            </div>
            <a href="https://github.com/mtpnda/biocue" target="_blank" rel="noopener" class="pill pill-outline" style="width:100%;justify-content:center;">
              ${dict.download.githubCta}
            </a>
          </div>
        </div>
      </div>
    </section>
  `;

  const scripts = `
    <script>
      const STATES = {
        calm: {
          haloBg: 'rgba(134,239,172,0.18)',
          haloColor: '#86EFAC',
          icon: \`${renderIcon("calm", 44)}\`,
          status: 'Alex is calm',
          sub: 'Resting equilibrium',
          meter: '28%',
          meterColor: '#86EFAC',
          phoneBadge: 'Calm',
          phoneBadgeBg: 'var(--state-calm-soft)',
          phoneBadgeColor: 'var(--state-calm-ink)',
          phoneBadgeBorder: 'var(--state-calm-chip)',
          phoneMeter: '28%',
          phoneMeterColor: 'var(--state-calm-chip)',
          insightTitle: 'Ambient Sensory Tone: Resting',
          insightDesc: 'Heart rate variability is stable. Autonomic nervous system shows parasympathetic regulation.'
        },
        rising: {
          haloBg: 'rgba(252,211,77,0.22)',
          haloColor: '#FCD34D',
          icon: \`${renderIcon("excited", 44)}\`,
          status: 'Arousal rising',
          sub: 'Sensory stimulus detected',
          meter: '58%',
          meterColor: '#FCD34D',
          phoneBadge: 'Rising',
          phoneBadgeBg: 'var(--state-rising-soft)',
          phoneBadgeColor: 'var(--state-rising-ink)',
          phoneBadgeBorder: 'var(--state-rising-chip)',
          phoneMeter: '58%',
          phoneMeterColor: 'var(--state-rising-chip)',
          insightTitle: 'Autonomic Shift: Sympathetic Activation',
          insightDesc: 'HRV RMSSD is declining while physical motion is quiet. Early cue sent to smartwatch.'
        },
        alert: {
          haloBg: 'rgba(252,165,165,0.22)',
          haloColor: '#FCA5A5',
          icon: \`${renderIcon("overwhelmed", 44)}\`,
          status: 'Sensory surge',
          sub: 'Breathe softly & ground',
          meter: '86%',
          meterColor: '#FCA5A5',
          phoneBadge: 'Needs support',
          phoneBadgeBg: 'var(--state-alert-soft)',
          phoneBadgeColor: 'var(--state-alert-ink)',
          phoneBadgeBorder: 'var(--state-alert-chip)',
          phoneMeter: '86%',
          phoneMeterColor: 'var(--state-alert-chip)',
          insightTitle: 'Potential Sensory Meltdown',
          insightDesc: 'Tier 2 stress boundary reached. Gentle grounding vibration active; caregiver circle alerted.'
        },
        active: {
          haloBg: 'rgba(147,197,253,0.22)',
          haloColor: '#93C5FD',
          icon: \`${renderIcon("active", 44)}\`,
          status: 'Active movement',
          sub: 'Healthy physical exercise',
          meter: '45%',
          meterColor: '#93C5FD',
          phoneBadge: 'Active',
          phoneBadgeBg: 'var(--state-info-soft)',
          phoneBadgeColor: 'var(--state-info-ink)',
          phoneBadgeBorder: 'var(--state-info-chip)',
          phoneMeter: '45%',
          phoneMeterColor: 'var(--state-info-chip)',
          insightTitle: 'Kinetic Movement Filtered',
          insightDesc: 'Accelerometer motion matches heart rate rise. Normal physiological response; stress alerts suppressed.'
        }
      };

      function setSimState(key) {
        const state = STATES[key];
        if (!state) return;

        // Update control button highlights
        document.querySelectorAll('.sim-btn').forEach(btn => {
          btn.classList.toggle('active', btn.getAttribute('data-state') === key);
        });

        // Watch face updates
        const halo = document.getElementById('watchHalo');
        if (halo) {
          halo.style.background = state.haloBg;
          halo.style.color = state.haloColor;
        }
        const icon = document.getElementById('watchIcon');
        if (icon) icon.innerHTML = state.icon;
        const status = document.getElementById('watchStatusText');
        if (status) status.innerText = state.status;
        const sub = document.getElementById('watchSubtitle');
        if (sub) sub.innerText = state.sub;
        const meter = document.getElementById('watchMeter');
        if (meter) {
          meter.style.width = state.meter;
          meter.style.background = state.meterColor;
        }

        // Phone surface updates
        const badge = document.getElementById('phoneBadge');
        if (badge) {
          badge.innerText = state.phoneBadge;
          badge.style.background = state.phoneBadgeBg;
          badge.style.color = state.phoneBadgeColor;
          badge.style.borderColor = state.phoneBadgeBorder;
        }
        const pMeter = document.getElementById('phoneMeter');
        if (pMeter) {
          pMeter.style.width = state.phoneMeter;
          pMeter.style.background = state.phoneMeterColor;
        }
        const iTitle = document.getElementById('phoneInsightTitle');
        if (iTitle) iTitle.innerText = state.insightTitle;
        const iDesc = document.getElementById('phoneInsightDesc');
        if (iDesc) iDesc.innerText = state.insightDesc;
      }
    </script>
  `;

  return renderLayout({
    title: dict.meta.title,
    description: dict.meta.description,
    content,
    extraHead,
    scripts,
    locale,
    activeTab: "home",
  });
}
