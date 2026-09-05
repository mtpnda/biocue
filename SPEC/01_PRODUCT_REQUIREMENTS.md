# 01. Product Requirements Document (PRD)

## Objective
BioCue is an offline-first, multi-device Android application (Phone + Wear OS) designed to predict and detect sensory overload meltdowns in autistic individuals. 

## Target Audience
- **The Wearer:** An autistic individual wearing a Wear OS 3+ smartwatch (e.g., Galaxy Watch 4). 
- **The Caregiver:** A parent or support worker running the mobile app.

## Core Philosophy
1. **Zero-Anxiety UX:** The watch interface must never display live vitals (heart rate numbers) or chaotic animations, which can induce stress. It acts quietly in the background with a single "I need help" SOS button.
2. **Extreme Privacy:** All data (physiological metrics, GPS coordinates, event journals) is stored strictly on-device in a local SQLite/Room database. No cloud syncing, no Firebase.
3. **Consent & Battery:** Continuous background GPS tracking is explicitly forbidden. GPS is only fetched via an on-demand "SOS Ping" during a severe meltdown.

## Success Metrics
- Useful early warning for ≥50% of episodes within 90 days.
- Watch battery life ≥16 hours (full waking day).
- Caregiver receives SOS coordinates within 30 seconds of a Tier 2 meltdown.
