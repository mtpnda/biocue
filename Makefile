# ==============================================================================
# BioCue - Centralized Multiplatform Release & Build Controller
# ==============================================================================
# Single pane of glass for releasing across:
#   1. Local Webhost / Tailscale (MacBook Air binary server)
#   2. GitHub Releases (public tags & release assets)
#   3. Apple App Store & TestFlight (macOS & iOS)
#   4. Google Play Store (Internal, Beta & Production Staged Rollout)
# ==============================================================================

.DEFAULT_GOAL := help
SHELL         := /bin/bash

# Configurable Parameters (Override via CLI, e.g. `make release-github TAG=v1.2.0`)
TAG          ?= v1.0.0
WEBHOST_DIR  ?= /Users/valdiearsanur/Workspace/macbook-air-webhost
ROLLOUT      ?= 0.10
FASTLANE     := bundle exec fastlane

# Terminal Colors
BOLD  := \033[1m
CYAN  := \033[36m
GREEN := \033[32m
YELLOW:= \033[33m
RED   := \033[31m
RESET := \033[0m

.PHONY: help doctor clean build-all build-mac build-ios build-android build-mobile build-wear build-mock \
        release-local release-mac-local release-ios-local release-android-local release-wear-local sync-webhost \
        release-github release-mac-github release-android-github \
        release-appstore release-mac-appstore release-ios-appstore release-testflight \
        release-playstore release-playstore-internal release-playstore-listing metadata \
        install install-android install-mobile install-wear install-mock \
        mock-web mock-cli \
        test test-release test-reviewer test-android-reviewer test-suite test-android test-mac test-ios

# ------------------------------------------------------------------------------
# Help & Overview
# ------------------------------------------------------------------------------
help:
	@echo ""
	@echo -e "$(BOLD)$(CYAN)======================================================================$(RESET)"
	@echo -e "$(BOLD)$(CYAN)  BioCue - Multiplatform Release Controller$(RESET)"
	@echo -e "$(BOLD)$(CYAN)======================================================================$(RESET)"
	@echo ""
	@echo -e "$(BOLD)🌐 1. LOCAL & TAILSCALE WEBHOST RELEASES (MacBook Air)$(RESET)"
	@echo -e "  $(GREEN)make release-local$(RESET)          Build present platforms and deploy to Webhost"
	@echo -e "  $(GREEN)make release-mac-local$(RESET)      Build macOS DMG and deploy to Webhost"
	@echo -e "  $(GREEN)make release-ios-local$(RESET)      Build iOS client IPA and deploy to Webhost"
	@echo -e "  $(GREEN)make release-android-local$(RESET)  Build Android APK & AAB and deploy to Webhost"
	@echo -e "  $(GREEN)make sync-webhost$(RESET)           Copy already-built artifacts directly to $(WEBHOST_DIR)"
	@echo ""
	@echo -e "$(BOLD)🐙 2. GITHUB PUBLIC RELEASES$(RESET)"
	@echo -e "  $(GREEN)make release-github$(RESET)         Publish release assets to GitHub tag (TAG=$(TAG))"
	@echo ""
	@echo -e "$(BOLD)🍎 3. APPLE APP STORE & TESTFLIGHT$(RESET)"
	@echo -e "  $(GREEN)make release-appstore$(RESET)       Submit Apple platforms to App Store Connect"
	@echo -e "  $(GREEN)make release-mac-appstore$(RESET)   Build and upload to Mac App Store"
	@echo -e "  $(GREEN)make release-ios-appstore$(RESET)   Build and upload to iOS App Store"
	@echo -e "  $(GREEN)make release-testflight$(RESET)     Upload to Apple TestFlight"
	@echo ""
	@echo -e "$(BOLD)🤖 4. GOOGLE PLAY STORE$(RESET)"
	@echo -e "  $(GREEN)make release-playstore$(RESET)      Publish signed AAB to Google Play Production (ROLLOUT=$(ROLLOUT))"
	@echo -e "  $(GREEN)make release-playstore-internal$(RESET) Upload AAB to Google Play Internal Testing"
	@echo -e "  $(GREEN)make release-playstore-listing$(RESET)  Update store text and graphics"
	@echo -e "  $(GREEN)make metadata$(RESET)                Alias for release-playstore-listing"
	@echo ""
	@echo -e "$(BOLD)🔨 5. LOCAL BUILDS & DEVICE INSTALLATION$(RESET)"
	@echo -e "  $(GREEN)make build-all$(RESET)              Compile all platforms locally into ./build/"
	@echo -e "  $(GREEN)make build-android$(RESET)          Build Android Companion release APK & AAB"
	@echo -e "  $(GREEN)make build-wear$(RESET)             Build Wear OS release APK & AAB"
	@echo -e "  $(GREEN)make build-mock$(RESET)             Build Sensor Mock Emitter dev APK"
	@echo -e "  $(GREEN)make install-android$(RESET)        Install Companion APK directly to connected Android phone/tablet/emulator"
	@echo -e "  $(GREEN)make install-wear$(RESET)           Install Wear OS APK directly to connected smartwatch/emulator"
	@echo -e "  $(GREEN)make install-mock$(RESET)           Install Sensor Mock Emitter dev APK to connected device/emulator"
	@echo -e "  $(GREEN)make mock-web$(RESET)               Start Web GUI Console on http://localhost:8080"
	@echo -e "  $(GREEN)make mock-cli ARGS=\"...\"$(RESET)       Run Mock CLI command (e.g. ARGS=\"--scenario overwhelmed\")"
	@echo -e "  $(GREEN)make install$(RESET)                Alias for install-android"
	@echo ""
	@echo -e "$(BOLD)🧪 6. TESTS & QUALITY GATES$(RESET)"
	@echo -e "  $(GREEN)make test$(RESET)                   Run all automated unit & verification tests"
	@echo -e "  $(GREEN)make test-backend$(RESET)           Run Deno website & Cloud Functions unit tests"
	@echo -e "  $(GREEN)make run-backend$(RESET)            Start local Deno web server on http://localhost:8000"
	@echo -e "  $(GREEN)make deploy-deno$(RESET)            Deploy website & API to Deno Deploy"
	@echo -e "  $(GREEN)make test-release$(RESET)           Run release automation verification tests"
	@echo -e "  $(GREEN)make test-reviewer$(RESET)          Run sequential Play Store reviewer automation test"
	@echo -e "  $(GREEN)make test-suite$(RESET)             Run multi-device automated test suite (Watch & Tablet)"
	@echo -e "  $(GREEN)make test-android$(RESET)           Run Android & Wear OS Robolectric/JUnit unit tests"
	@echo ""
	@echo -e "$(BOLD)🔍 7. ENVIRONMENT & UTILITIES$(RESET)"
	@echo -e "  $(GREEN)make doctor$(RESET)                 Verify required build tools & environment"
	@echo -e "  $(GREEN)make clean$(RESET)                  Clean build caches and generated artifacts"
	@echo ""

# ------------------------------------------------------------------------------
# 1. Environment Health Check
# ------------------------------------------------------------------------------
doctor:
	@echo -e "$(BOLD)$(CYAN)🔍 Checking BioCue Release Toolchain...$(RESET)"
	@command -v bundle >/dev/null 2>&1 && echo -e "  ✓ Bundler:     $$(bundle --version)" || echo -e "  $(RED)✗ Bundler not found$(RESET)"
	@command -v fastlane >/dev/null 2>&1 && echo -e "  ✓ Fastlane:    $$(fastlane --version | head -n 1)" || echo -e "  $(RED)✗ Fastlane not found$(RESET)"
	@command -v xcodebuild >/dev/null 2>&1 && echo -e "  ✓ Xcode:       $$(xcodebuild -version | head -n 1)" || echo -e "  $(YELLOW)⚠ Xcode not found (needed for Apple platforms)$(RESET)"
	@test -d "$(WEBHOST_DIR)" && echo -e "  ✓ Webhost Dir: $(WEBHOST_DIR) exists" || echo -e "  $(YELLOW)⚠ Webhost directory $(WEBHOST_DIR) does not exist$(RESET)"
	@echo ""

# ------------------------------------------------------------------------------
# 2. Local & Tailscale Webhost Releases
# ------------------------------------------------------------------------------
release-local:
	@echo -e "$(BOLD)$(GREEN)🚀 Releasing all platforms to Local Webhost at $(WEBHOST_DIR)...$(RESET)"
	OUTPUT_DIR="$(WEBHOST_DIR)" $(FASTLANE) internal_release folder:"$(WEBHOST_DIR)"

release-mac-local:
	@echo -e "$(BOLD)$(GREEN)🖥️  Building and deploying macOS DMG to $(WEBHOST_DIR)...$(RESET)"
	OUTPUT_DIR="$(WEBHOST_DIR)" $(FASTLANE) mac internal folder:"$(WEBHOST_DIR)"

release-ios-local:
	@echo -e "$(BOLD)$(GREEN)📱 Building and deploying iOS IPA to $(WEBHOST_DIR)...$(RESET)"
	OUTPUT_DIR="$(WEBHOST_DIR)" $(FASTLANE) ios internal folder:"$(WEBHOST_DIR)"

release-android-local:
	@echo -e "$(BOLD)$(GREEN)🤖 Building and deploying Android APK/AAB to $(WEBHOST_DIR)...$(RESET)"
	OUTPUT_DIR="$(WEBHOST_DIR)" $(FASTLANE) android internal folder:"$(WEBHOST_DIR)"

release-wear-local:
	@echo -e "$(BOLD)$(GREEN)⌚ Building and deploying Wear OS APK/AAB to $(WEBHOST_DIR)...$(RESET)"
	OUTPUT_DIR="$(WEBHOST_DIR)" $(FASTLANE) copy_to_folder folder:"$(WEBHOST_DIR)"

sync-webhost:
	@echo -e "$(BOLD)$(GREEN)🌐 Copying existing build artifacts directly to $(WEBHOST_DIR)...$(RESET)"
	OUTPUT_DIR="$(WEBHOST_DIR)" $(FASTLANE) copy_to_folder folder:"$(WEBHOST_DIR)"

# ------------------------------------------------------------------------------
# 3. GitHub Public Releases
# ------------------------------------------------------------------------------
release-github:
	@echo -e "$(BOLD)$(GREEN)🐙 Publishing Full Release $(TAG) to GitHub...$(RESET)"
	RELEASE_TAG="$(TAG)" $(FASTLANE) external_release tag:"$(TAG)"

# ------------------------------------------------------------------------------
# 4. Apple App Store & TestFlight
# ------------------------------------------------------------------------------
release-appstore:
	@echo -e "$(BOLD)$(GREEN)🍎 Submitting to App Store Connect...$(RESET)"
	$(FASTLANE) appstore_release

release-mac-appstore:
	@echo -e "$(BOLD)$(GREEN)🖥️  Submitting macOS app to Mac App Store...$(RESET)"
	$(FASTLANE) mac release_appstore

release-ios-appstore:
	@echo -e "$(BOLD)$(GREEN)📱 Submitting iOS app to App Store...$(RESET)"
	$(FASTLANE) ios release_appstore

release-testflight:
	@echo -e "$(BOLD)$(GREEN)📱 Uploading to TestFlight...$(RESET)"
	$(FASTLANE) ios testflight_upload external:false

# ------------------------------------------------------------------------------
# 5. Google Play Store
# ------------------------------------------------------------------------------
release-playstore:
	@echo -e "$(BOLD)$(GREEN)🤖 Publishing to Google Play Store (Rollout: $(ROLLOUT))...$(RESET)"
	ROLLOUT_PERCENTAGE="$(ROLLOUT)" $(FASTLANE) playstore_release

release-playstore-internal:
	@echo -e "$(BOLD)$(GREEN)🤖 Uploading to Google Play Internal Testing...$(RESET)"
	$(FASTLANE) android release_playstore_internal

release-playstore-listing:
	@echo -e "$(BOLD)$(GREEN)🤖 Updating Google Play Store Listing (text & graphics)...$(RESET)"
	$(FASTLANE) android release_playstore_listing

metadata: release-playstore-listing

# ------------------------------------------------------------------------------
# 6. Local Builds & Device Installation
# ------------------------------------------------------------------------------
build-all:
	@echo -e "$(BOLD)$(CYAN)🔨 Building all platforms locally via Fastlane...$(RESET)"
	$(FASTLANE) build_all

build-mac:
	@echo -e "$(BOLD)$(CYAN)🖥️  Building macOS Release App & DMG via Fastlane...$(RESET)"
	$(FASTLANE) mac build

build-ios:
	@echo -e "$(BOLD)$(CYAN)📱 Building iOS Client IPA via Fastlane...$(RESET)"
	$(FASTLANE) ios build

build-mobile: build-android

build-android:
	@echo -e "$(BOLD)$(CYAN)🤖 Building Android Release APK & AAB via Fastlane...$(RESET)"
	$(FASTLANE) android build

build-wear:
	@echo -e "$(BOLD)$(CYAN)⌚ Building Wear OS Release APK & AAB...$(RESET)"
	cd android && ./gradlew :wear:bundleRelease :wear:assembleRelease

build-mock:
	@echo -e "$(BOLD)$(CYAN)🧪 Building Sensor Mock Emitter App...$(RESET)"
	cd android && ./gradlew :mock-emitter:assembleDebug

install: install-android

install-mobile: install-android

install-android:
	@echo -e "$(BOLD)$(GREEN)🤖 Installing BioCue Companion APK to connected device/emulator via Fastlane...$(RESET)"
	$(FASTLANE) android install $(if $(DEVICE),device:"$(DEVICE)",)$(if $(BUILD),build:true,)

install-wear:
	@echo -e "$(BOLD)$(GREEN)⌚ Installing BioCue Wear OS APK to connected watch/emulator via Fastlane...$(RESET)"
	$(FASTLANE) android install_wear $(if $(DEVICE),device:"$(DEVICE)",)$(if $(BUILD),build:true,)

install-mock:
	@echo -e "$(BOLD)$(GREEN)🧪 Installing Sensor Mock Emitter to connected device/emulator...$(RESET)"
	cd android && ./gradlew :mock-emitter:installDebug

mock-web:
	@echo -e "$(BOLD)$(CYAN)🌐 Starting BioCue Mock Sensor Web Console on http://localhost:8080...$(RESET)"
	python3 tools/mock_sensor_console.py --web

mock-cli:
	python3 tools/mock_sensor_console.py $(ARGS)

# ------------------------------------------------------------------------------
# 7. Tests & Quality Gates
# ------------------------------------------------------------------------------
test: test-release test-backend
	@echo -e "$(BOLD)$(GREEN)✅ All test suites passed!$(RESET)"

test-release:
	@echo -e "$(BOLD)$(CYAN)🧪 Running Release Automation Suite...$(RESET)"
	python3 tests/test_release_automation.py

test-backend:
	@echo -e "$(BOLD)$(CYAN)🧪 Running Cloud Functions Backend Tests...$(RESET)"
	cd backend/functions && node --test test/*.test.js
	@echo -e "$(BOLD)$(CYAN)🧪 Running Deno Website & API Tests...$(RESET)"
	cd backend && deno task test

run-backend:
	@echo -e "$(BOLD)$(GREEN)🚀 Starting BioCue Deno Web Server (http://localhost:8000)...$(RESET)"
	cd backend && deno task start

deploy-deno:
	@echo -e "$(BOLD)$(CYAN)🚀 Deploying BioCue to Deno Deploy...$(RESET)"
	@if [ -n "$$DENO_DEPLOY_TOKEN" ]; then \
		cd backend && deno run -Ar jsr:@deno/deployctl deploy --project=biocue src/main.ts; \
	else \
		echo -e "$(YELLOW)Notice: DENO_DEPLOY_TOKEN is not set in environment.$(RESET)"; \
		echo -e "BioCue is linked to Deno Deploy via GitHub repository integration (like mtpnda/arcscreen)."; \
		echo -e "Commits pushed to 'origin main' automatically trigger Deno Deploy builds."; \
		echo -e "To deploy via CLI directly, run: export DENO_DEPLOY_TOKEN=<token> && make deploy-deno"; \
	fi

test-reviewer: test-android-reviewer

test-suite:
	@echo -e "$(BOLD)$(CYAN)🤖 Running Multi-Device Automated Tester (Watch & Mobile)...$(RESET)"
	python3 tests/test_watch_and_mobile_suite.py

test-android-reviewer:
	@echo -e "$(BOLD)$(CYAN)🤖 Running Play Store Reviewer Sequential UI Automation...$(RESET)"
	python3 tests/test_playstore_reviewer.py --skip-install

test-android:
	@echo -e "$(BOLD)$(CYAN)🧪 Running Android & Wear OS Unit Tests...$(RESET)"
	cd android && ./gradlew :mobile:testDebugUnitTest :wear:testDebugUnitTest :core-detection:test

# ------------------------------------------------------------------------------
# 8. Clean & Maintenance
# ------------------------------------------------------------------------------
clean:
	@echo -e "$(BOLD)$(YELLOW)🧹 Cleaning build artifacts...$(RESET)"
	rm -rf build/
	cd android && ./gradlew clean --quiet 2>/dev/null || true
	@echo -e "$(BOLD)$(GREEN)✓ Workspace cleaned.$(RESET)"

uninstall-mac:
	@echo -e "$(BOLD)$(YELLOW)🧹 Uninstalling BioCue from local Mac via Fastlane...$(RESET)"
	$(FASTLANE) mac uninstall
