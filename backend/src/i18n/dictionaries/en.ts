import { Dictionary } from "../types.ts";

export const enDictionary: Dictionary = {
  locale: "en",
  localeName: "English (US)",
  flag: "🇺🇸",

  meta: {
    title: "BioCue — Sensory Overload Prediction & Bio-Signal Cueing",
    description: "Free, open-source Wear OS smartwatch and Android companion system predicting sensory meltdowns 5–15 minutes early through on-device physiological cues and zero-anxiety calm anchor design.",
  },

  nav: {
    features: "Features",
    howItWorks: "How It Works",
    whitepaper: "Whitepaper",
    simulator: "Sensory Simulator",
    faq: "FAQ",
    support: "Help & Support",
    download: "Get BioCue",
    getApp: "Download Free →",
  },

  hero: {
    badge: "🌱 100% Free & Open-Source Assistive Technology",
    titleLine1: "Sensory Overload Protection",
    titleHighlight: "Before Meltdown Peaks",
    titleLine2: "Quietly Grounded on Wear OS.",
    sub: "BioCue continuously monitors autonomic nervous system tone on your smartwatch to predict sensory overload 5–15 minutes early. Gentle haptic reminders, a soothing visual anchor, and trusted caregiver circle sync—engineered with zero panic numbers.",
    ctaDownload: "Download for Wear OS & Android",
    ctaExplore: "Test the Sensory Anchor Simulator ↓",
    stat1Val: "5–15 min",
    stat1Label: "Early Window for Grounding",
    stat2Val: "100%",
    stat2Label: "On-Device Biometric Isolation",
    stat3Val: "0",
    stat3Label: "Chaotic Heart Rate Numbers",
    stat4Val: "Free",
    stat4Label: "Completely Free for Every Family",
  },

  simulator: {
    badge: "Interactive Sensory Anchor",
    title: "Designed to De-escalate, Never to Alarm",
    sub: "Standard fitness watches display flashing red numbers and erratic heart charts that heighten panic. BioCue replaces clinical clutter with organic botanical sprouts and soothing color shifts.",
    calmBtn: "🌱 Calm Equilibrium",
    risingBtn: "✨ Rising Arousal",
    alertBtn: "🌩️ Sensory Overload",
    activeBtn: "🏃 Healthy Movement",
    watchViewTitle: "Wear OS Watch Face (Zero-Anxiety Anchor)",
    phoneViewTitle: "Companion App (Wearer & Caregiver View)",
  },

  howItWorks: {
    badge: "Three-Stage Protection Loop",
    title: "How BioCue Keeps You Grounded",
    sub: "From quiet background sensor capture to gentle early intervention, BioCue bridges physiological changes with actionable self-regulation.",
    step1Title: "1. Ambient Sensor Fusion",
    step1Desc: "Your Wear OS smartwatch continuously samples photoplethysmography (PPG) pulse intervals and 3-axis accelerometer data, measuring Heart Rate Variability (HRV RMSSD) and physical motion without draining battery.",
    step1Badge: "Low-energy Wear OS Health Services API",
    step2Title: "2. On-Device Prediction",
    step2Desc: "Our on-device Fusion ML algorithm calculates autonomic tone locally in your phone's Room database. It differentiates between healthy exercise and internal sensory stress—zero raw vitals ever leave your device.",
    step2Badge: "Local SQLite/Room DB Machine Learning",
    step3Title: "3. Gentle Cue & Support",
    step3Desc: "Before stress peaks into an involuntary crisis, the watch emits a soft, rhythmic haptic pulse. If distress escalates, the wearer can activate guided breathing or send a one-tap SOS beacon to up to 2 approved caregivers.",
    step3Badge: "Encrypted 2-in-1 Caregiver Circle Relay",
  },

  features: {
    badge: "Crafted for Autistic & Sensory-Sensitive Individuals",
    title: "Thoughtful Engineering for Sensory Well-Being",
    sub: "Every interaction, animation, and color token adheres to our neurodivergent-affirming Design Language System.",
    cards: [
      {
        icon: "calm",
        title: "Zero-Anxiety Anchor UI",
        desc: "No erratic lines, alarming pulse counters, or red warning sirens. Subtle botanical states and organic breathing pacers maintain cognitive ease."
      },
      {
        icon: "brain",
        title: "On-Device Fusion Scoring",
        desc: "Combines Heart Rate Variability (HRV RMSSD), heart rate acceleration, and 3-axis motion to accurately separate exercise from sensory overwhelm."
      },
      {
        icon: "caregiver",
        title: "Sovereign Caregiver Circle",
        desc: "Connect up to 2 approved caregivers via temporary 6-digit PIN. One-tap removal instantly revokes database access and wipes caregiver device caches."
      },
      {
        icon: "sos-location",
        title: "Consent-First Emergency GPS",
        desc: "Zero continuous background tracking. A single GPS location fix is acquired solely when the wearer explicitly triggers an SOS emergency beacon."
      },
      {
        icon: "privacy-lock",
        title: "100% Private Local Storage",
        desc: "Raw biometrics remain locked in the local Room database on your phone. Only lightweight categorized stress event labels are synced to paired caregivers."
      },
      {
        icon: "excited",
        title: "Full 16+ Hour Battery Life",
        desc: "Designed to run all day on a single charge. Intelligently throttles PPG sensor windows and avoids high-power background loops."
      }
    ]
  },

  comparison: {
    badge: "Why BioCue Stands Apart",
    title: "Traditional Monitors vs. BioCue Sovereign Care",
    sub: "Most commercial wearables are built for athletic optimization or intrusive surveillance. BioCue is built for sensory autonomy.",
    colCapability: "Capability",
    colBiocue: "BioCue Platform",
    colFitness: "Consumer Fitness Trackers",
    colSurveillance: "Invasive Surveillance Apps",
    rows: [
      {
        capability: "Meltdown Prediction",
        biocue: "5–15 min autonomic early warning",
        fitness: "None (post-hoc athletic logs)",
        surveillance: "None (location-only alerts)"
      },
      {
        capability: "Smartwatch Display",
        biocue: "Zero-Anxiety Anchor (no panic numbers)",
        fitness: "Flashing BPM & noisy charts",
        surveillance: "Not applicable (phone only)"
      },
      {
        capability: "Biometric Privacy",
        biocue: "100% on-device local Room DB",
        fitness: "Uploaded to corporate health clouds",
        surveillance: "Cloud-tracked indefinitely"
      },
      {
        capability: "GPS Surveillance",
        biocue: "Strictly forbidden (On-demand SOS only)",
        fitness: "Continuous route tracking",
        surveillance: "24/7 background geofencing"
      },
      {
        capability: "Caregiver Sovereignty",
        biocue: "Wearer controls pairing; instant wipe",
        fitness: "No caregiver circles",
        surveillance: "Unilateral surveillance; no wearer say"
      },
      {
        capability: "Software & Access",
        biocue: "100% Free & Open Source",
        fitness: "Proprietary hardware lock-in",
        surveillance: "Commercial paywall restrictions"
      }
    ]
  },

  faq: {
    badge: "Common Questions",
    title: "Frequently Asked Questions",
    sub: "Transparent answers about our sensors, privacy boundaries, open-source ethos, and daily wear.",
    items: [
      {
        q: "Is BioCue really 100% free?",
        a: "Yes. BioCue is completely free, non-commercial, and open source. We believe assistive tools that safeguard neurodivergent autonomy and dignity should never be hidden behind subscriptions or monetization paywalls.",
      },
      {
        q: "How does BioCue predict sensory overloads before they occur?",
        a: "Before a sensory meltdown or shutdown manifests externally, the sympathetic nervous system activates, causing subtle drops in Heart Rate Variability (HRV RMSSD) and characteristic heart rate elevation while physical motion remains static. BioCue’s Fusion algorithm detects this unique bio-signal signature 5–15 minutes early.",
      },
      {
        q: "Why doesn't the watch display live heart rate numbers?",
        a: "Seeing a numerical reading like '148 BPM' often creates an anxiety feedback loop, where the fear of the number itself increases heart rate. BioCue uses color cues (Calm Sprout, Rising Amber, Supportive Coral) and calm breathing animations to keep the wearer centered.",
      },
      {
        q: "What devices do I need to use BioCue?",
        a: "You need any Wear OS 3.0+ smartwatch equipped with a standard optical heart rate sensor (such as Samsung Galaxy Watch 4/5/6/7, Google Pixel Watch 1/2/3, or Mobvoi TicWatch Pro 5) and an Android companion smartphone or tablet running Android 8.0 or newer.",
      },
      {
        q: "Can a caregiver track my location all the time?",
        a: "No. Continuous GPS tracking is strictly prohibited in our software architecture. GPS location is only queried when you explicitly press the SOS button or confirm an acute meltdown, providing emergency help without continuous surveillance.",
      },
      {
        q: "How do I remove a paired caregiver?",
        a: "Open the BioCue Companion app on your phone, go to Caregiver Circle, and tap 'Remove Caregiver'. The server instantly deletes the connection, and the caregiver's device immediately wipes its local cache of your history.",
      },
    ],
  },

  download: {
    badge: "Available Now",
    title: "Install BioCue on Your Smartwatch & Phone",
    sub: "Get started in minutes with our standalone Wear OS application and Android companion client.",
    wearTitle: "Wear OS Watch App",
    wearSub: "Zero-Anxiety watch face, quick-access Wear OS Tile, and ambient PPG sensor monitoring.",
    wearCta: "Download Wear OS APK / Play Store",
    mobileTitle: "Android Companion App",
    mobileSub: "Local Room database scoring, sensory journal, and 2-in-1 Caregiver Circle management.",
    mobileCta: "Download Android Companion",
    githubTitle: "Open-Source Repository",
    githubSub: "Explore the full source code, inspect security rules, or build your own custom release.",
    githubCta: "View on GitHub ↗",
  },

  footer: {
    desc: "BioCue is an open-source assistive bio-signal cueing platform combining Wear OS 3+ smartwatch physiological sensing with an Android Native companion app and encrypted Caregiver Circles.",
    badge: "100% Free & Sovereign On-Device Biometrics",
    colProduct: "Product",
    colTrust: "Trust & Legal",
    colSupport: "Support & Open Source",
    linkFeatures: "Features",
    linkHowItWorks: "How It Works",
    linkWhitepaper: "Technical Whitepaper",
    linkSimulator: "Sensory Simulator",
    linkFaq: "FAQ",
    linkDownload: "Download Release",
    linkPrivacy: "Privacy Policy",
    linkTerms: "Terms of Service",
    linkDataDeletion: "Data Deletion Rights",
    linkCaregiverPolicy: "Caregiver Removal Policy",
    linkHelpCenter: "Help Center & Support",
    linkGithub: "GitHub Repository ↗",
    linkReleases: "Releases & Changelogs ↗",
    copyright: "© 2026 BioCue. 100% Free & Open Source. Designed with neurodiversity and sovereignty at core.",
    disclaimer: "Non-diagnostic wellness & assistive cueing tool",
    edgeStatus: "Edge Deno KV Active",
  },
};
