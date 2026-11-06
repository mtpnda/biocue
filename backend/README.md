# BioCue Firebase & Firestore Setup Guide

Since we have integrated the app with Firebase Auth and Firestore for real-time syncing between the Wearer and Caregiver, you need to configure your Firebase Console to support this securely.

## 1. Firebase Project Setup

1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Click **Create a project** (or add to an existing one).
3. Register your Android app:
   - **Package name**: `com.mtpnda.biocue`
   - Download the `google-services.json` file.
   - Place this file in the `mobile/` directory (and `wear/` directory if you plan to connect the watch directly to Firebase later). *(Note: I see you already have a `google-services.json` in `mobile/`, so you might have done this!)*

## 2. Enable Authentication

1. In the Firebase Console, go to **Build > Authentication**.
2. Click **Get Started**.
3. Go to the **Sign-in method** tab and enable **Google Sign-In**. BioCue's `LoginScreen` exclusively uses Google Sign-In via Android Credential Manager and Firebase Authentication for streamlined, passwordless access. Ensure your SHA-1 fingerprints (debug, upload, and Play App Signing) are registered under **Project Settings > Your apps**.

## 3. Enable Firestore Database

1. Go to **Build > Firestore Database**.
2. Click **Create database**.
3. Choose **Start in production mode** (we will apply strict security rules immediately).
4. Choose a location close to your users (e.g., `asia-southeast2` for Jakarta).

## 4. Deploying Firestore Security Rules & Indexes

To ensure strict data privacy—specifically your rule: *"when removed in wearer side, care giver do not have right to see any wearer data"*—we must enforce this at the database level. 

Instead of configuring this manually in the console, all the required configuration files are already generated in the root of your project:
- `firestore.rules`: Contains the strict multi-account security rules locking out caregivers when pairings are removed.
- `firestore.indexes.json`: Contains the composite indexes needed for complex queries (like querying pairings by `wearerUid` + `status`).
- `firebase.json` & `.firebaserc`: Links these files to your `biocueapp` project.

You can automatically apply these to your production database using the Firebase CLI. Simply run the following commands in your terminal from the root directory:

```bash
# 1. Log in to your Google Account (opens a browser)
firebase login

# 2. Deploy the rules and indexes to Firestore
firebase deploy --only firestore
```

## Summary of the Privacy Flow
By using the `get()` function in the `firestore.rules` file, the moment a Wearer changes a pairing status from `APPROVED` to `REMOVED`, the Caregiver's database connection to the wearer's vitals is instantly severed by Google's servers. They cannot read any historical or future data.


---

# BioCue Deno Web Server & Edge Portal

The `backend/` directory also houses the official public website, legal compliance pages (Privacy Policy, Terms of Service), support helpdesk, and remote configuration endpoint for BioCue. It is built using [Deno](https://deno.com) and [Hono](https://hono.dev), with styling generated directly from the BioCue Design Language System (`design/dls.tokens.json`).

## 1. Prerequisites
- [Deno](https://deno.com) 1.40+ (or latest) installed on your system.

## 2. Local Development & Running
From the repository root or the `backend/` directory:
```bash
# Via root Makefile:
make run-backend

# Or directly in backend:
cd backend
deno task dev
```
The server will start on `http://localhost:8000`.

## 3. Automated Testing & Verification
```bash
# Run all backend tests (Node Cloud Functions + Deno Website):
make test-backend

# Or run Deno test suite directly:
cd backend
deno task test
```

## 4. Endpoints & Pages
- `GET /`: Automatic regional redirect based on visitor country and language.
- `GET /en`: English Global Homepage with interactive Sensory Anchor Simulator.
- `GET /id-id`: Bahasa Indonesia Homepage.
- `GET /privacy`: Comprehensive Privacy Policy (On-device isolation, zero continuous GPS, account deletion).
- `GET /terms`: Terms of Service & Assistive Care Agreement (Non-medical disclaimer).
- `GET /support`: Helpdesk knowledge base and interactive support ticket submission form.
- `GET /signin`: Calm, passwordless sign-in entrance for the Caregiver / Wearer web portal.
- `GET /health`: Edge health check endpoint.
- `GET /api/config`: Remote configuration and version metadata.
- `POST /api/support`: Support ticket submission stored in Deno KV.

## 5. Deployment to Deno Deploy
BioCue is configured to deploy to Deno Deploy via GitHub repository integration (`mtpnda/biocue`).
Every commit pushed to `origin main` automatically triggers a build and deployment.

To deploy directly via CLI:
```bash
export DENO_DEPLOY_TOKEN=<your-token>
make deploy-deno
```
