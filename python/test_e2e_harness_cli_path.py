"""Unit tests for the E2E harness's Copilot CLI platform-package resolution.

Regression coverage for github/copilot-sdk#2103: the harness used to return the
first ``@github/copilot-*`` directory in alphabetical order instead of the package
built for the current platform.
"""

from __future__ import annotations

from copilot._cli_version import get_npm_platform
from e2e.testharness import context


class TestCliPlatformPackageNames:
    def test_non_linux_platform_yields_single_candidate(self):
        assert context._cli_platform_package_names("darwin-arm64") == ["copilot-darwin-arm64"]

    def test_windows_platform_yields_single_candidate(self):
        assert context._cli_platform_package_names("win32-x64") == ["copilot-win32-x64"]

    def test_glibc_linux_also_considers_musl_variant(self):
        assert context._cli_platform_package_names("linux-x64") == [
            "copilot-linux-x64",
            "copilot-linuxmusl-x64",
        ]

    def test_musl_linux_prefers_musl_then_falls_back_to_glibc(self):
        assert context._cli_platform_package_names("linuxmusl-arm64") == [
            "copilot-linuxmusl-arm64",
            "copilot-linux-arm64",
        ]

    def test_defaults_to_current_host_platform(self):
        assert context._cli_platform_package_names()[0] == f"copilot-{get_npm_platform()}"
