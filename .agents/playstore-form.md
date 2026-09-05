# Google Play Store - Compliance & Store Listing Documentation

This document serves as the single source of truth for how BioCue is configured in the Google Play Console. Use this as a reference for future updates or if the app gets rejected and needs an appeal.

## 1. App Access (Reviewer Credentials)
**Problem:** Google Play reviewers cannot reliably use "Sign in with Google" because attempting to log into a shared Gmail account from a foreign testing facility triggers Google's 2FA (Two-Factor Authentication) blocks, resulting in an app rejection.
**Solution:** We enabled standard Email/Password authentication in Firebase strictly for the reviewers. 
**Credentials Provided to Google:**
- **Email:** `mvasecondary@gmail.com`
- **Password:** (Stored securely by the developer in Firebase)

## 2. Account & Data Deletion
**Requirement:** Apps that allow account creation (even via SSO) must provide a web link for users to request data deletion without having the app installed.
**Solution:** The developer generated a Google Form requesting the user's email address and stating that all BioCue data will be wiped within 7 days.
- **Form URL:** https://docs.google.com/forms/d/e/1FAIpQLSfKDVmJFnKwFZUYM-maTMwOzs6HUrlVb_4bgmx3NHr9WC0uWw/viewform

## 3. Data Safety & Privacy Form
BioCue collects and encrypts data in transit via Firebase.

### Data Types Collected
- **Personal Info:** Name, Email address, User IDs
- **Location:** Approximate location, Precise location
- **Health and fitness:** Health info, Fitness info
- **App info and performance:** Crash logs, Diagnostics
- **Device or other IDs:** Device or other IDs

### Data Usage & Handling (Cheat Sheet)
All data collected by BioCue follows these strict handling rules:
- **Collected or Shared:** `Collected` only (BioCue does not sell or "Share" data with 3rd party advertising agencies).
- **Processed Ephemerally:** `No` (Data is stored in Firebase to track historical vitals and maintain sessions).
- **Required:** `Required` (All declared data is strictly necessary for the app to function; it is not optional).

#### Purposes by Data Type:
- **Name, Email, User IDs:** `App functionality`, `Account management`
- **Location (Precise & Approximate):** `App functionality`
- **Health & Fitness Info:** `App functionality`
- **Crash Logs & Diagnostics:** `Analytics`
- **Device IDs:** `App functionality`, `Account management`, `Analytics`, `Fraud prevention, security, and compliance`

## 4. Privacy Policy
- **Privacy Policy URL:** https://doc-hosting.flycricket.io/biocue-privacy-policy/33a06b5e-bf73-4159-8138-247d89b63eff/privacy

## 5. Store Listing Text
**App Name:** BioCue: Stress & Vitals Sync
**Short Description:** Monitor vitals and track emotional stress in real-time from Watch to Phone.
**Full Description:**
BioCue is an innovative health and wellness companion designed to seamlessly bridge the gap between smartwatch vitals and caregiver support. Built with neurodiversity in mind, BioCue provides a zero-anxiety tracking experience for the wearer while delivering rich, real-time insights to a trusted caregiver or family member.

Using advanced biometric monitoring, BioCue reads continuous data directly from your Wear OS smartwatch and instantly syncs it to a detailed dashboard on your phone.

KEY FEATURES
• Zero-Anxiety Watch Interface: A calming, ambient smartwatch design that operates quietly in the background without overwhelming the wearer with clinical numbers.
• Caregiver Dashboard: A rich mobile interface displaying real-time heart rate and activity levels.
• The Fusion Score: An intelligent metric that analyzes biometric data to detect stress levels and emotional dysregulation, allowing caregivers to step in before a meltdown occurs.
• Real-Time Sync: Powered by a high-speed data layer, ensuring the caregiver's dashboard updates the exact second the wearer's vitals change.
• Retrospective Labeling: Review historical events and track baseline calibrations over time to understand triggers and patterns.

Whether you are supporting a loved one with ADHD, autism, or simply looking for a robust way to monitor stress and physical activity, BioCue provides the insights you need with the privacy and calm experience they deserve.

(Note: BioCue requires both an Android smartphone and a compatible Wear OS smartwatch to function correctly. This app is designed for general wellness and stress management and is not a certified medical device.)

## 6. Graphical Assets
All generated visual assets for the Play Store upload are saved in this `.agents` directory:
- **App Icon (512x512):** `.agents/play_store_icon.png`
- **Feature Graphic (1024x500):** `.agents/play_store_feature_graphic.png`
