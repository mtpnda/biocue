#!/usr/bin/env python3
"""
Unit and integration tests for BioCue Release & Deployment Automation.
Tests:
  1. Zero divergent release or uninstaller shell scripts in repository
  2. Pure file copy to custom output directory without mutating destination config files
  3. Fastlane configuration integrity (Appfile, Fastfile, Gemfile)
  4. Fastlane orchestration lane syntax verification (runner.execute dispatch)
  5. Disk image (DMG) structure, embedded uninstaller integration, and code signatures
  6. Makefile targets completeness
"""

import os
import shutil
import subprocess
import tempfile
import unittest

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
FASTFILE_PATH = os.path.join(PROJECT_ROOT, "fastlane", "Fastfile")
APPFILE_PATH = os.path.join(PROJECT_ROOT, "fastlane", "Appfile")
GEMFILE_PATH = os.path.join(PROJECT_ROOT, "Gemfile")
DMG_PATH = os.path.join(PROJECT_ROOT, "build", "BioCue.dmg")


def get_effective_fastfile():
    """Reads project Fastfile and follows central import references."""
    if not os.path.isfile(FASTFILE_PATH):
        return ""
    with open(FASTFILE_PATH, "r") as f:
        content = f.read()

    # Follow local central-agents import
    import re
    m = re.search(r'central_fastfile\s*=\s*File\.expand_path\(["\']([^"\']+)["\']\)', content)
    if not m:
        m = re.search(r'import\s*\((?:File\.expand_path\()?["\']([^"\']+)["\']\)?\)', content)
    if m:
        target = os.path.expanduser(m.group(1))
        if os.path.exists(target):
            with open(target, "r") as f_imp:
                content += "\n" + f_imp.read()
    return content


class TestReleaseAutomation(unittest.TestCase):

    def test_no_divergent_scripts_in_repo(self):
        """Verify repository contains no loose release or uninstaller shell scripts."""
        self.assertFalse(
            os.path.exists(os.path.join(PROJECT_ROOT, "scripts", "build_release_mac.sh")),
            "scripts/build_release_mac.sh must not exist; Fastlane is the single build engine",
        )
        self.assertFalse(
            os.path.exists(os.path.join(PROJECT_ROOT, "scripts", "uninstall_mac.sh")),
            "scripts/uninstall_mac.sh must not exist; Fastlane generates uninstaller dynamically",
        )

    def test_fastlane_defines_uninstaller_script_generator(self):
        """Verify Fastlane Fastfile defines build_uninstaller_script helper."""
        if not os.path.isfile(FASTFILE_PATH):
            self.skipTest("No Fastfile present in project")
        fastfile = get_effective_fastfile()
        self.assertIn("def build_uninstaller_script", fastfile)
        self.assertIn("tccutil reset", fastfile)
        self.assertIn("--permissions-only", fastfile)
        self.assertIn("--dry-run", fastfile)

    def test_fastlane_mac_uninstall_lane_definition(self):
        """Verify Fastlane defines comprehensive `mac uninstall` lane."""
        if not os.path.isfile(FASTFILE_PATH):
            self.skipTest("No Fastfile present in project")
        fastfile = get_effective_fastfile()
        self.assertIn("lane :uninstall do", fastfile)
        self.assertIn("tccutil reset", fastfile)
        self.assertIn("FileUtils.rm_rf", fastfile)

    def test_copy_to_custom_destination_preserves_configs(self):
        """Verify that releasing into a folder with existing configs leaves them untouched."""
        if not os.path.isfile(FASTFILE_PATH):
            self.skipTest("No Fastfile present in project")

        with tempfile.TemporaryDirectory() as temp_dir:
            downloads_dir = os.path.join(temp_dir, "downloads")
            os.makedirs(downloads_dir, exist_ok=True)

            # Create existing files that must NOT be overwritten or touched
            caddyfile_path = os.path.join(temp_dir, "Caddyfile")
            original_caddy_content = "# ORIGINAL CADDYFILE DO NOT TOUCH\n"
            with open(caddyfile_path, "w") as f:
                f.write(original_caddy_content)

            index_html_path = os.path.join(temp_dir, "index.html")
            original_index_content = "<html>ORIGINAL</html>\n"
            with open(index_html_path, "w") as f:
                f.write(original_index_content)

            # Run fastlane copy_to_folder into temp_dir
            result = subprocess.run(
                ["fastlane", "copy_to_folder", f"folder:{temp_dir}"],
                cwd=PROJECT_ROOT,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
            )
            self.assertEqual(result.returncode, 0, f"fastlane copy_to_folder failed: {result.stderr}")

            # Verify existing configs remained 100% untouched
            with open(caddyfile_path, "r") as f:
                self.assertEqual(f.read(), original_caddy_content)

            with open(index_html_path, "r") as f:
                self.assertEqual(f.read(), original_index_content)

    def test_fastlane_configuration_files(self):
        """Verify Fastlane Appfile, Fastfile, and Gemfile exist and have expected contents."""
        self.assertTrue(os.path.isfile(GEMFILE_PATH))
        self.assertTrue(os.path.isfile(APPFILE_PATH))
        self.assertTrue(os.path.isfile(FASTFILE_PATH))

        fastfile = get_effective_fastfile()
        self.assertIn("internal_release", fastfile)
        self.assertIn("copy_to_folder", fastfile)

    def test_fastlane_lanes_dryrun(self):
        """Verify fastlane CLI executes `fastlane lanes` cleanly without Ruby errors."""
        result = subprocess.run(
            ["fastlane", "lanes"],
            cwd=PROJECT_ROOT,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
        self.assertEqual(result.returncode, 0, f"fastlane lanes failed: {result.stderr}")
        self.assertIn("build_all", result.stdout)
        self.assertIn("internal_release", result.stdout)

    def test_fastlane_orchestration_lanes_syntax(self):
        """Verify top-level orchestration lanes use runner.execute and no undefined method calls."""
        fastfile = get_effective_fastfile()

        # Ensure no legacy/undefined platform methods are called
        self.assertNotIn("mac_internal(", fastfile)
        self.assertNotIn("ios_internal(", fastfile)
        self.assertNotIn("android_internal(", fastfile)

        # Ensure runner.execute syntax is used for build and release
        self.assertIn("runner.execute(:build", fastfile)
        self.assertIn("runner.execute(:internal", fastfile)

    def test_dmg_contains_single_uninstaller_app(self):
        """Verify that DMG disk image contains required app bundle and uninstaller."""
        if not os.path.isfile(DMG_PATH):
            self.skipTest(f"DMG not present at {DMG_PATH} (run make build-mac first)")

        with tempfile.TemporaryDirectory() as mount_point:
            attach = subprocess.run(
                ["hdiutil", "attach", "-readonly", "-nobrowse", "-mountpoint", mount_point, DMG_PATH],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                timeout=15,
            )
            self.assertEqual(attach.returncode, 0, f"Failed to attach DMG: {attach.stderr}")

            try:
                app_path = os.path.join(mount_point, "BioCue.app")
                apps_symlink = os.path.join(mount_point, "Applications")
                uninstaller_app = os.path.join(mount_point, "Uninstall BioCue.app")
                uninstaller_cmd = os.path.join(uninstaller_app, "Contents", "Resources", "uninstall.command")

                self.assertTrue(os.path.isdir(app_path), "BioCue.app missing from DMG")
                self.assertTrue(os.path.islink(apps_symlink), "Applications symlink missing from DMG")
                self.assertTrue(os.path.isdir(uninstaller_app), "Uninstall BioCue.app missing from DMG")
                self.assertTrue(os.path.isfile(uninstaller_cmd), "uninstall.command missing from app bundle Resources")
                self.assertTrue(os.access(uninstaller_cmd, os.X_OK), "uninstall.command in Resources is not marked +x")

                # Verify syntax of generated uninstaller inside DMG
                syntax_check = subprocess.run(
                    ["bash", "-n", uninstaller_cmd],
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE,
                    text=True,
                )
                self.assertEqual(syntax_check.returncode, 0, f"Syntax error in DMG uninstall.command: {syntax_check.stderr}")

                # Verify code signatures
                app_verify = subprocess.run(
                    ["codesign", "--verify", "--deep", "--strict", "--verbose=2", app_path],
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE,
                    text=True,
                )
                self.assertEqual(app_verify.returncode, 0, f"Code signature failed: {app_verify.stderr}")
            finally:
                subprocess.run(["hdiutil", "detach", mount_point, "-force", "-quiet"], timeout=10)

    def test_makefile_exists_and_defines_release_targets(self):
        """Verify Makefile exists and contains standard release targets."""
        makefile_path = os.path.join(PROJECT_ROOT, "Makefile")
        self.assertTrue(os.path.isfile(makefile_path), "Makefile missing at project root")

        with open(makefile_path, "r") as f:
            content = f.read()

        import re
        m = re.search(r'-?include\s+([^\n]+)', content)
        if m:
            inc_path = os.path.expanduser(m.group(1).replace("$(HOME)", "~"))
            if os.path.exists(inc_path):
                with open(inc_path, "r") as f_inc:
                    content += "\n" + f_inc.read()

        for target in ["help:", "doctor:", "release-local:", "sync-webhost:", "test-release:"]:
            self.assertIn(target, content, f"Target {target} missing from Makefile")

    def test_playstore_metadata_integrity(self):
        """Verify Google Play Store metadata and graphics adhere to console limits."""
        for module in ["mobile", "wear"]:
            play_dir = os.path.join(PROJECT_ROOT, "android", module, "src", "main", "play")
            if not os.path.isdir(play_dir):
                play_dir = os.path.join(PROJECT_ROOT, module, "src", "main", "play")
            self.assertTrue(os.path.isdir(play_dir), f"Play directory missing for {module}")

            # Check text listings
            listings_dir = os.path.join(play_dir, "listings", "en-US")
            title_file = os.path.join(listings_dir, "title.txt")
            short_file = os.path.join(listings_dir, "short-description.txt")
            full_file = os.path.join(listings_dir, "full-description.txt")

            self.assertTrue(os.path.isfile(title_file), f"title.txt missing for {module}")
            self.assertTrue(os.path.isfile(short_file), f"short-description.txt missing for {module}")
            self.assertTrue(os.path.isfile(full_file), f"full-description.txt missing for {module}")

            with open(title_file, "r") as f:
                title = f.read().strip()
                self.assertLessEqual(len(title), 30, f"{module} title exceeds 30 chars: '{title}' ({len(title)})")
                self.assertGreater(len(title), 0)

            with open(short_file, "r") as f:
                short = f.read().strip()
                self.assertLessEqual(len(short), 80, f"{module} short description exceeds 80 chars ({len(short)})")
                self.assertGreater(len(short), 0)

            with open(full_file, "r") as f:
                full = f.read().strip()
                self.assertLessEqual(len(full), 4000, f"{module} full description exceeds 4000 chars ({len(full)})")
                self.assertGreater(len(full), 100)

            # Check release notes
            notes_file = os.path.join(play_dir, "release-notes", "en-US", "default.txt")
            self.assertTrue(os.path.isfile(notes_file), f"default.txt release notes missing for {module}")
            with open(notes_file, "r") as f:
                notes = f.read().strip()
                self.assertLessEqual(len(notes), 500, f"{module} release notes exceed 500 chars ({len(notes)})")

            # Check graphics
            graphics_dir = os.path.join(listings_dir, "graphics")
            icon_file = os.path.join(graphics_dir, "icon", "icon.png")
            self.assertTrue(os.path.isfile(icon_file), f"icon.png missing in {module}")

        # Module-specific graphics verification
        mobile_graphics = os.path.join(PROJECT_ROOT, "android", "mobile", "src", "main", "play", "listings", "en-US", "graphics")
        if not os.path.isdir(mobile_graphics):
            mobile_graphics = os.path.join(PROJECT_ROOT, "mobile", "src", "main", "play", "listings", "en-US", "graphics")
        self.assertTrue(os.path.isfile(os.path.join(mobile_graphics, "feature-graphic", "feature-graphic.png")))
        self.assertTrue(os.path.isdir(os.path.join(mobile_graphics, "phone-screenshots")))
        self.assertTrue(os.path.isdir(os.path.join(mobile_graphics, "ten-inch-screenshots")))

        wear_graphics = os.path.join(PROJECT_ROOT, "android", "wear", "src", "main", "play", "listings", "en-US", "graphics")
        if not os.path.isdir(wear_graphics):
            wear_graphics = os.path.join(PROJECT_ROOT, "wear", "src", "main", "play", "listings", "en-US", "graphics")
        self.assertTrue(os.path.isdir(os.path.join(wear_graphics, "wear-screenshots")))

    def test_android_manifest_compliance(self):
        """Verify Android companion and Wear OS AndroidManifest.xml compliance."""
        mobile_manifest = os.path.join(PROJECT_ROOT, "android", "mobile", "src", "main", "AndroidManifest.xml")
        if not os.path.isfile(mobile_manifest):
            mobile_manifest = os.path.join(PROJECT_ROOT, "mobile", "src", "main", "AndroidManifest.xml")

        wear_manifest = os.path.join(PROJECT_ROOT, "android", "wear", "src", "main", "AndroidManifest.xml")
        if not os.path.isfile(wear_manifest):
            wear_manifest = os.path.join(PROJECT_ROOT, "wear", "src", "main", "AndroidManifest.xml")

        self.assertTrue(os.path.isfile(mobile_manifest))
        self.assertTrue(os.path.isfile(wear_manifest))

        with open(mobile_manifest, "r") as f:
            m_content = f.read()
            self.assertIn("android.permission.INTERNET", m_content)
            self.assertIn("android.permission.POST_NOTIFICATIONS", m_content)
            self.assertIn("WatchDataReceiverService", m_content)

        with open(wear_manifest, "r") as f:
            w_content = f.read()
            self.assertIn("android.hardware.type.watch", w_content)
            self.assertIn("com.google.android.wearable.standalone", w_content)
            self.assertIn("MainTileService", w_content)


if __name__ == "__main__":
    unittest.main()
