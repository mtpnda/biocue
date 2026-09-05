#!/bin/bash
set -e

# 1. Define variables
KEYSTORE_FILE="biocue-release-key.jks"
KEY_ALIAS="biocue"
KEY_PASSWORD="BiocueReleasePass2026!" # Change this if you want
PROPERTIES_FILE="local.properties"

echo "====================================="
echo "🚀 BioCue Release Build Script"
echo "====================================="

# 2. Check if Keystore exists, if not, generate it (Industry standard: keytool)
if [ ! -f "$KEYSTORE_FILE" ]; then
    echo "🔑 Generating new Release Keystore ($KEYSTORE_FILE)..."
    keytool -genkey -v \
        -keystore $KEYSTORE_FILE \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -alias $KEY_ALIAS \
        -dname "CN=BioCue, OU=Engineering, O=Matapanda Studio, C=US" \
        -storepass $KEY_PASSWORD -keypass $KEY_PASSWORD

    echo "🔒 Adding secrets to $PROPERTIES_FILE (Ignored by Git)"
    echo "" >> $PROPERTIES_FILE
    echo "RELEASE_STORE_FILE=../$KEYSTORE_FILE" >> $PROPERTIES_FILE
    echo "RELEASE_STORE_PASSWORD=$KEY_PASSWORD" >> $PROPERTIES_FILE
    echo "RELEASE_KEY_ALIAS=$KEY_ALIAS" >> $PROPERTIES_FILE
    echo "RELEASE_KEY_PASSWORD=$KEY_PASSWORD" >> $PROPERTIES_FILE
else
    echo "✅ Keystore ($KEYSTORE_FILE) already exists. Using existing keys."
fi

# 3. Clean and build the Android App Bundles (.aab)
echo "📦 Building Release App Bundles for Mobile and Wear..."
./gradlew :mobile:bundleRelease :wear:bundleRelease

echo "====================================="
echo "🎉 SUCCESS! Your bundles are ready to drag-and-drop to Google Play:"
echo "📱 Phone Bundle: mobile/build/outputs/bundle/release/mobile-release.aab"
echo "⌚ Watch Bundle: wear/build/outputs/bundle/release/wear-release.aab"
echo "====================================="
