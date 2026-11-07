# ==============================================================================
# BioCue - Mata Panda Studio Single-Pane Controller
# ==============================================================================
APP_NAME     := BioCue
PACKAGE_NAME := com.mtpnda.biocue
PLATFORMS    := android wear
WEBHOST_DIR  ?= /Users/valdiearsanur/Workspace/macbook-air-webhost

-include $(HOME)/.gemini/config/plugins/central-agents/skills/custom-release-manager/mtpnda.mk

# ------------------------------------------------------------------------------
# BioCue-Specific Custom Hardware, Mock & Testing Targets:
# ------------------------------------------------------------------------------
install-android:
	@echo -e "$(BOLD)$(GREEN)🤖 Installing BioCue Companion APK to connected device/emulator via Fastlane...$(RESET)"
	$(FASTLANE) android install $(if $(DEVICE),device:"$(DEVICE)",)$(if $(BUILD),build:true,)

install-wear:
	@echo -e "$(BOLD)$(GREEN)⌚ Installing BioCue Wear OS APK to connected watch/emulator via Fastlane...$(RESET)"
	$(FASTLANE) android install_wear $(if $(DEVICE),device:"$(DEVICE)",)$(if $(BUILD),build:true,)

connect-watch:
	@python3 tools/watch_auto_connect.py $(if $(IP),--ip $(IP),) $(if $(PORT),--port $(PORT),)

pair-watch:
	@if [ -z "$(IP)" ] || [ -z "$(PORT)" ] || [ -z "$(CODE)" ]; then \
		echo -e "$(RED)Error: Missing arguments.$(RESET)"; \
		echo "Usage: make pair-watch IP=<watch_ip> PORT=<pairing_port> CODE=<6_digit_code>"; \
		exit 1; \
	fi
	@python3 tools/watch_auto_connect.py --ip $(IP) --pair $(PORT) $(CODE)

disconnect-watch:
	@echo -e "$(BOLD)$(CYAN)🔌 Disconnecting wireless watch connections...$(RESET)"
	@adb disconnect

install-mock:
	@echo -e "$(BOLD)$(GREEN)🧪 Installing Sensor Mock Emitter to connected device/emulator...$(RESET)"
	cd android && ./gradlew :mock-emitter:installDebug

mock-web:
	@echo -e "$(BOLD)$(CYAN)🌐 Starting BioCue Mock Sensor Web Console on http://localhost:8080...$(RESET)"
	python3 tools/mock_sensor_console.py --web

mock-cli:
	python3 tools/mock_sensor_console.py $(ARGS)

test: test-release test-backend
	@echo -e "$(BOLD)$(GREEN)✅ All test suites passed!$(RESET)"

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

test-suite:
	@echo -e "$(BOLD)$(CYAN)🤖 Running Multi-Device Automated Tester (Watch & Mobile)...$(RESET)"
	python3 tests/test_watch_and_mobile_suite.py

test-android:
	@echo -e "$(BOLD)$(CYAN)🧪 Running Android & Wear OS Unit Tests...$(RESET)"
	cd android && ./gradlew :mobile:testDebugUnitTest :wear:testDebugUnitTest :core-detection:test

clean:
	@echo -e "$(BOLD)$(YELLOW)🧹 Cleaning build artifacts...$(RESET)"
	rm -rf build/
	cd android && ./gradlew clean --quiet 2>/dev/null || true
	@echo -e "$(BOLD)$(GREEN)✓ Workspace cleaned.$(RESET)"
