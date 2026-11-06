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
