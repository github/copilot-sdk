"""Unit tests for the E2E harness's Copilot CLI platform-package resolution.

Regression coverage for github/copilot-sdk#2103: the harness used to return the
first ``@github/copilot-*`` directory in alphabetical order instead of the package
built for the current platform.
"""

from __future__ import annotations

from pathlib import Path

import pytest

from copilot._cli_version import get_npm_platform
from e2e.testharness import context


def _make_package(github_modules: Path, name: str) -> Path:
    """Create ``<github_modules>/<name>/index.js`` and return the entrypoint path."""
    package_dir = github_modules / name
    package_dir.mkdir(parents=True, exist_ok=True)
    index = package_dir / "index.js"
    index.write_text("// fake CLI entrypoint\n")
    return index


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


class TestFindCliInNodeModules:
    def test_skips_alphabetically_earlier_foreign_package(self, tmp_path):
        # The #2103 regression: "aardvark" sorts before every real platform name.
        _make_package(tmp_path, "copilot-aardvark-x64")
        expected = _make_package(tmp_path, "copilot-darwin-arm64")
        found = context._find_cli_in_node_modules(tmp_path, ["copilot-darwin-arm64"])
        assert found == str(expected.resolve())

    def test_returns_none_when_no_candidate_is_installed(self, tmp_path):
        _make_package(tmp_path, "copilot-win32-x64")
        assert context._find_cli_in_node_modules(tmp_path, ["copilot-darwin-arm64"]) is None

    def test_ignores_non_platform_copilot_packages(self, tmp_path):
        _make_package(tmp_path, "copilot-language-server")
        assert context._find_cli_in_node_modules(tmp_path, ["copilot-linux-x64"]) is None

    def test_prefers_earlier_candidate_when_both_libc_variants_exist(self, tmp_path):
        expected = _make_package(tmp_path, "copilot-linuxmusl-x64")
        _make_package(tmp_path, "copilot-linux-x64")
        found = context._find_cli_in_node_modules(
            tmp_path, ["copilot-linuxmusl-x64", "copilot-linux-x64"]
        )
        assert found == str(expected.resolve())

    def test_returns_none_when_package_dir_has_no_index_js(self, tmp_path):
        (tmp_path / "copilot-linux-x64").mkdir()
        assert context._find_cli_in_node_modules(tmp_path, ["copilot-linux-x64"]) is None

    def test_returns_none_when_github_modules_is_absent(self, tmp_path):
        missing = tmp_path / "missing"
        assert context._find_cli_in_node_modules(missing, ["copilot-linux-x64"]) is None


class TestGetCliPathForTests:
    def test_env_var_takes_precedence(self, tmp_path, monkeypatch):
        cli = tmp_path / "custom-cli.js"
        cli.write_text("// custom entrypoint\n")
        monkeypatch.setenv("COPILOT_CLI_PATH", str(cli))
        assert context.get_cli_path_for_tests() == str(cli.resolve())

    def test_error_names_the_packages_tried_and_the_remedy(self, monkeypatch):
        monkeypatch.delenv("COPILOT_CLI_PATH", raising=False)
        monkeypatch.setattr(
            context, "_cli_platform_package_names", lambda *_: ["copilot-linux-x64"]
        )
        monkeypatch.setattr(context, "_find_cli_in_node_modules", lambda *_: None)
        with pytest.raises(RuntimeError) as excinfo:
            context.get_cli_path_for_tests()
        message = str(excinfo.value)
        assert "copilot-linux-x64" in message
        assert "npm install" in message
        assert "COPILOT_CLI_PATH" in message
