// BioCue Remote Configuration & Version Endpoint

import { Hono } from "hono";

export const configRouter = new Hono();

configRouter.get("/", (c) => {
  return c.json({
    app: "BioCue",
    version: "1.0.0",
    build: 100,
    platforms: {
      android: {
        package: "com.mtpnda.biocue",
        minSdk: 26,
        targetSdk: 34,
        releaseUrl: "https://github.com/mtpnda/biocue/releases/latest",
      },
      wear: {
        package: "com.mtpnda.biocue.wear",
        minWearOsVersion: 3.0,
        releaseUrl: "https://github.com/mtpnda/biocue/releases/latest",
      },
    },
    privacy: {
      zeroContinuousGps: true,
      onDeviceBiometricsOnly: true,
      policyUrl: "https://biocue.app/privacy",
    },
    support: {
      helpdeskUrl: "https://biocue.app/support",
      email: "support@biocue.app",
    },
  });
});
