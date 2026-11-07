// BioCue Localization Dictionary Types — 100% Free & Open-Source Assistive Platform

export type Locale = "en" | "id";

export interface FaqItem {
  q: string;
  a: string;
}

export interface FeatureCard {
  icon: "calm" | "brain" | "caregiver" | "sos-location" | "privacy-lock" | "excited";
  title: string;
  desc: string;
}

export interface ComparisonRow {
  capability: string;
  biocue: string;
  fitness: string;
  surveillance: string;
}

export interface Dictionary {
  locale: Locale;
  localeName: string;
  flag: string;

  meta: {
    title: string;
    description: string;
  };

  nav: {
    features: string;
    howItWorks: string;
    whitepaper: string;
    simulator: string;
    faq: string;
    support: string;
    download: string;
    getApp: string;
  };

  hero: {
    badge: string;
    titleLine1: string;
    titleHighlight: string;
    titleLine2: string;
    sub: string;
    ctaDownload: string;
    ctaExplore: string;
    stat1Val: string;
    stat1Label: string;
    stat2Val: string;
    stat2Label: string;
    stat3Val: string;
    stat3Label: string;
    stat4Val: string;
    stat4Label: string;
  };

  simulator: {
    badge: string;
    title: string;
    sub: string;
    calmBtn: string;
    risingBtn: string;
    alertBtn: string;
    activeBtn: string;
    watchViewTitle: string;
    phoneViewTitle: string;
  };

  howItWorks: {
    badge: string;
    title: string;
    sub: string;
    step1Title: string;
    step1Desc: string;
    step1Badge: string;
    step2Title: string;
    step2Desc: string;
    step2Badge: string;
    step3Title: string;
    step3Desc: string;
    step3Badge: string;
  };

  features: {
    badge: string;
    title: string;
    sub: string;
    cards: FeatureCard[];
  };

  comparison: {
    badge: string;
    title: string;
    sub: string;
    colCapability: string;
    colBiocue: string;
    colFitness: string;
    colSurveillance: string;
    rows: ComparisonRow[];
  };

  faq: {
    badge: string;
    title: string;
    sub: string;
    items: FaqItem[];
  };

  download: {
    badge: string;
    title: string;
    sub: string;
    wearTitle: string;
    wearSub: string;
    wearCta: string;
    mobileTitle: string;
    mobileSub: string;
    mobileCta: string;
    githubTitle: string;
    githubSub: string;
    githubCta: string;
  };

  footer: {
    desc: string;
    badge: string;
    colProduct: string;
    colTrust: string;
    colSupport: string;
    linkFeatures: string;
    linkHowItWorks: string;
    linkWhitepaper: string;
    linkSimulator: string;
    linkFaq: string;
    linkDownload: string;
    linkPrivacy: string;
    linkTerms: string;
    linkDataDeletion: string;
    linkCaregiverPolicy: string;
    linkHelpCenter: string;
    linkGithub: string;
    linkReleases: string;
    copyright: string;
    disclaimer: string;
    edgeStatus: string;
  };
}
