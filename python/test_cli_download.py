"""Tests for the in-process runtime library download integrity checks."""

from __future__ import annotations

import base64
import hashlib
import io
import os
import tarfile
from http.client import IncompleteRead
from unittest.mock import MagicMock, patch

import pytest

from copilot import _cli_download, _ffi_runtime_host


def _integrity(data: bytes, algo: str = "sha512") -> str:
    digest = hashlib.new(algo, data).digest()
    return f"{algo}-{base64.b64encode(digest).decode('ascii')}"


def _runtime_package(npm_platform: str) -> bytes:
    wrapper_name = "copilot-runtime.exe" if os.name == "nt" else "copilot-runtime"
    members = {
        f"package/prebuilds/{npm_platform}/{wrapper_name}": b"wrapper",
        f"package/prebuilds/{npm_platform}/runtime.node": b"runtime",
        "package/copilot": b"excluded",
        "package/copilot.exe": b"excluded",
        f"package/ripgrep/bin/{npm_platform}/rg": b"ripgrep",
        "package/definitions/future.json": b"{}",
        "package/app.js": b"excluded",
        "package/LICENSE.md": b"excluded",
        "package/README.md": b"excluded",
    }
    buffer = io.BytesIO()
    with tarfile.open(fileobj=buffer, mode="w:gz") as archive:
        for name, content in members.items():
            info = tarfile.TarInfo(name)
            info.size = len(content)
            archive.addfile(info, io.BytesIO(content))
    return buffer.getvalue()


def test_fetch_url_bytes_retries_truncated_response():
    truncated_response = MagicMock()
    truncated_response.__enter__.return_value.read.side_effect = IncompleteRead(b"partial", 4)
    complete_response = MagicMock()
    complete_response.__enter__.return_value.read.return_value = b"complete"

    with (
        patch.object(
            _cli_download,
            "urlopen",
            side_effect=[truncated_response, complete_response],
        ) as urlopen,
        patch.object(_cli_download.time, "sleep") as sleep,
    ):
        assert _cli_download._fetch_url_bytes("https://example/runtime", timeout=30) == b"complete"

    assert urlopen.call_count == 2
    sleep.assert_called_once_with(1)


class TestVerifyIntegrity:
    def test_accepts_matching_checksum(self):
        data = b"native-library-bytes"
        _cli_download._verify_integrity(data, _integrity(data))

    def test_rejects_mismatched_checksum(self):
        with pytest.raises(RuntimeError, match="Integrity mismatch"):
            _cli_download._verify_integrity(b"tampered", _integrity(b"original"))

    def test_rejects_unsupported_algorithm(self):
        # Fail closed rather than silently skipping verification of native code.
        with pytest.raises(RuntimeError, match="Unsupported integrity algorithm"):
            _cli_download._verify_integrity(b"bytes", "md5-deadbeef")


class TestEnsureRuntimeLibraryFailsClosed:
    def test_raises_when_integrity_unavailable(self, tmp_path):
        """A missing npm integrity value must abort the download, not load unverified code."""
        cli_path = tmp_path / "copilot"
        cli_path.write_bytes(b"#!/bin/sh\n")

        with (
            patch("copilot._ffi_runtime_host.resolve_library_path", return_value=None),
            patch.object(_cli_download, "_should_skip_download", return_value=False),
            patch.object(_cli_download, "get_npm_platform", return_value="linux-x64"),
            patch.object(_cli_download, "get_runtime_lib_url", return_value="https://example/lib"),
            patch.object(_cli_download, "_fetch_url_bytes", return_value=b"tarball-bytes"),
            patch.object(_cli_download, "_fetch_runtime_integrity", return_value=None),
            patch.object(_cli_download, "_extract_runtime_node") as extract,
        ):
            with pytest.raises(RuntimeError, match="refusing to load unverified native code"):
                _cli_download.ensure_runtime_library(str(cli_path), version="1.2.3")

        # The library bytes must never be extracted/written when verification is impossible.
        extract.assert_not_called()


def test_resolve_library_path_accepts_adjacent_runtime_node(tmp_path):
    wrapper = tmp_path / ("copilot-runtime.exe" if os.name == "nt" else "copilot-runtime")
    wrapper.write_bytes(b"wrapper")
    runtime_node = tmp_path / "runtime.node"
    runtime_node.write_bytes(b"runtime")

    assert _ffi_runtime_host.resolve_library_path(str(wrapper)) == str(runtime_node)


class TestEnsureRuntimeWrapper:
    def test_materializes_pair_from_absent_cache_with_stripped_environment(
        self, tmp_path, monkeypatch
    ):
        npm_platform = "win32-x64" if os.name == "nt" else "linux-x64"
        wrapper_name = "copilot-runtime.exe" if os.name == "nt" else "copilot-runtime"
        data = _runtime_package(npm_platform)
        cache_dir = tmp_path / "cache"
        empty_path = tmp_path / "empty-path"
        empty_path.mkdir()
        assert not cache_dir.exists()

        for name in (
            "COPILOT_CLI_PATH",
            "COPILOT_RUNTIME_HOST_COMMAND",
            "COPILOT_RUNTIME_PROVIDER_LIB",
        ):
            monkeypatch.delenv(name, raising=False)
        monkeypatch.setenv("PATH", str(empty_path))

        with (
            patch.object(_cli_download, "get_cache_dir", return_value=cache_dir),
            patch.object(_cli_download, "get_npm_platform", return_value=npm_platform),
            patch.object(_cli_download, "_should_skip_download", return_value=False),
            patch.object(_cli_download, "_fetch_url_bytes", return_value=data),
            patch.object(
                _cli_download,
                "_fetch_runtime_integrity",
                return_value=_integrity(data),
            ),
        ):
            wrapper = _cli_download.ensure_runtime_wrapper(version="1.2.3")

        install_dir = cache_dir / "prebuilds" / npm_platform
        assert wrapper == str(install_dir / wrapper_name)
        assert (install_dir / wrapper_name).read_bytes() == b"wrapper"
        assert (install_dir / "runtime.node").read_bytes() == b"runtime"
        assert (install_dir / "ripgrep" / "bin" / npm_platform / "rg").read_bytes() == b"ripgrep"
        assert (install_dir / "definitions" / "future.json").read_bytes() == b"{}"
        assert not (install_dir / "app.js").exists()
        assert not (install_dir / "copilot").exists()
        assert not (install_dir / "copilot.exe").exists()
        assert (install_dir / ".hostless-runtime-assets-v2").is_file()
        if os.name != "nt":
            assert (install_dir / wrapper_name).stat().st_mode & 0o111

    def test_rejects_cached_wrapper_without_runtime_node(self, tmp_path):
        npm_platform = "win32-x64" if os.name == "nt" else "linux-x64"
        wrapper_name = "copilot-runtime.exe" if os.name == "nt" else "copilot-runtime"
        cache_dir = tmp_path / "cache"
        install_dir = cache_dir / "prebuilds" / npm_platform
        install_dir.mkdir(parents=True)
        (install_dir / wrapper_name).write_bytes(b"wrapper")

        with (
            patch.object(_cli_download, "get_cache_dir", return_value=cache_dir),
            patch.object(_cli_download, "get_npm_platform", return_value=npm_platform),
        ):
            with pytest.raises(RuntimeError, match="Incomplete Copilot runtime bundle"):
                _cli_download.ensure_runtime_wrapper(version="1.2.3")

    def test_upgrades_pair_only_cache_with_retained_assets(self, tmp_path):
        npm_platform = "win32-x64" if os.name == "nt" else "linux-x64"
        wrapper_name = "copilot-runtime.exe" if os.name == "nt" else "copilot-runtime"
        cache_dir = tmp_path / "cache"
        install_dir = cache_dir / "prebuilds" / npm_platform
        install_dir.mkdir(parents=True)
        (install_dir / wrapper_name).write_bytes(b"old-wrapper")
        (install_dir / "runtime.node").write_bytes(b"old-runtime")
        (install_dir / "copilot").write_bytes(b"legacy-sea")
        (install_dir / ".hostless-runtime-assets-v1").write_text("1\n", encoding="ascii")
        data = _runtime_package(npm_platform)

        with (
            patch.object(_cli_download, "get_cache_dir", return_value=cache_dir),
            patch.object(_cli_download, "get_npm_platform", return_value=npm_platform),
            patch.object(_cli_download, "_should_skip_download", return_value=False),
            patch.object(_cli_download, "_fetch_url_bytes", return_value=data),
            patch.object(_cli_download, "_fetch_runtime_integrity", return_value=_integrity(data)),
        ):
            wrapper = _cli_download.ensure_runtime_wrapper(version="1.2.3")

        assert wrapper == str(install_dir / wrapper_name)
        assert (install_dir / wrapper_name).read_bytes() == b"wrapper"
        assert not (install_dir / "copilot").exists()
        assert (install_dir / ".hostless-runtime-assets-v2").is_file()
        assert (install_dir / "ripgrep" / "bin" / npm_platform / "rg").is_file()
