// BioCue Localization Dictionary Types — 100% Free & Open-Source Assistive Platform

export type Locale = "en" | "en-id" | "id-id";

export interface FaqItem {
  q: string;
  a: string;
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
    step2Title: string;
    step2Desc: string;
    step3Title: string;
    step3Desc: string;
  };

  features: {
    badge: string;
    title: string;
    sub: string;
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
}
