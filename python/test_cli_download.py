"""Tests for unified Copilot release-package provisioning."""

from __future__ import annotations

import hashlib
import io
import os
import tarfile
from concurrent.futures import ThreadPoolExecutor
from http.client import IncompleteRead
from threading import Barrier
from unittest.mock import MagicMock, patch

import pytest

from copilot import _cli_download, _cli_version, _ffi_runtime_host


def _release_package(runtime_platform: str) -> bytes:
    wrapper_name = "copilot-runtime.exe" if os.name == "nt" else "copilot-runtime"
    members = {
        f"package/prebuilds/{runtime_platform}/{wrapper_name}": b"wrapper",
        f"package/prebuilds/{runtime_platform}/runtime.node": b"runtime",
        f"package/ripgrep/bin/{runtime_platform}/rg": b"ripgrep",
        "package/definitions/future.json": b"{}",
        "package/app.js": b"excluded",
        "package/LICENSE.md": b"excluded",
    }
    buffer = io.BytesIO()
    with tarfile.open(fileobj=buffer, mode="w:gz") as archive:
        for name, content in members.items():
            info = tarfile.TarInfo(name)
            info.mode = 0o755 if name.endswith((wrapper_name, "/rg")) else 0o644
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


def _release_fetches(version: str, runtime_platform: str, data: bytes):
    asset_name = f"github-copilot-{version}-{runtime_platform}.tgz"
    checksum = hashlib.sha256(data).hexdigest()

    def fetch(url: str, *, timeout: int) -> bytes:
        del timeout
        if url.endswith("/SHA256SUMS.txt"):
            return f"{checksum}  {asset_name}\n".encode()
        assert url.endswith(f"/{asset_name}")
        return data

    return fetch


def test_release_asset_uses_platform_package_name(monkeypatch):
    monkeypatch.setenv("COPILOT_CLI_DOWNLOAD_BASE_URL", "https://mirror.example/releases/")

    name = _cli_version.get_release_asset_name("1.2.3-4", "linux-x64")

    assert name == "github-copilot-1.2.3-4-linux-x64.tgz"
    assert (
        _cli_version.get_download_url("1.2.3-4", name)
        == "https://mirror.example/releases/v1.2.3-4/github-copilot-1.2.3-4-linux-x64.tgz"
    )


def test_rejects_release_package_checksum_mismatch(tmp_path):
    runtime_platform = "linux-x64"
    data = _release_package(runtime_platform)
    asset_name = f"github-copilot-1.2.3-{runtime_platform}.tgz"

    def fetch(url: str, *, timeout: int) -> bytes:
        del timeout
        if url.endswith("/SHA256SUMS.txt"):
            return f"{'0' * 64}  {asset_name}\n".encode()
        return data

    with (
        patch.object(_cli_download, "get_cache_dir", return_value=tmp_path / "cache"),
        patch.object(_cli_download, "get_runtime_platform", return_value=runtime_platform),
        patch.object(_cli_download, "_fetch_url_bytes", side_effect=fetch),
    ):
        with pytest.raises(RuntimeError, match="Checksum mismatch"):
            _cli_download.ensure_runtime_wrapper(version="1.2.3")


def test_rejects_release_package_without_checksum(tmp_path):
    runtime_platform = "linux-x64"

    def fetch(url: str, *, timeout: int) -> bytes:
        del timeout
        assert url.endswith("/SHA256SUMS.txt")
        return f"{'0' * 64}  another-file.tgz\n".encode()

    with (
        patch.object(_cli_download, "get_cache_dir", return_value=tmp_path / "cache"),
        patch.object(_cli_download, "get_runtime_platform", return_value=runtime_platform),
        patch.object(_cli_download, "_fetch_url_bytes", side_effect=fetch),
    ):
        with pytest.raises(RuntimeError, match="SHA256SUMS.txt does not contain"):
            _cli_download.ensure_runtime_wrapper(version="1.2.3")


def test_cli_and_runtime_share_one_staged_bundle(tmp_path, monkeypatch):
    version = "1.2.3"
    runtime_platform = "linux-x64"
    cli_name = "copilot.exe" if os.name == "nt" else "copilot"
    wrapper_name = "copilot-runtime.exe" if os.name == "nt" else "copilot-runtime"
    data = _release_package(runtime_platform)
    cache_dir = tmp_path / "cache"
    install_dir = cache_dir / "prebuilds" / runtime_platform
    fetch = _release_fetches(version, runtime_platform, data)

    with (
        patch.object(_cli_download, "get_cache_dir", return_value=cache_dir),
        patch.object(_cli_download, "get_runtime_platform", return_value=runtime_platform),
        patch.object(_cli_download, "_fetch_url_bytes", side_effect=fetch) as fetch_mock,
    ):
        cli = _cli_download.download_cli(version)
        monkeypatch.setenv("COPILOT_SKIP_CLI_DOWNLOAD", "1")
        wrapper = _cli_download.ensure_runtime_wrapper(version)
        assert _cli_download.get_cached_cli_path(version) == str(install_dir / cli_name)

    assert cli == str(install_dir / cli_name)
    assert wrapper == str(install_dir / wrapper_name)
    assert (install_dir / cli_name).read_bytes() == b"wrapper"
    assert not (cache_dir / cli_name).exists()
    assert not (cache_dir / "packages").exists()
    assert (install_dir / wrapper_name).read_bytes() == b"wrapper"
    assert (install_dir / "runtime.node").read_bytes() == b"runtime"
    assert (install_dir / "ripgrep" / "bin" / runtime_platform / "rg").read_bytes() == b"ripgrep"
    assert (install_dir / "definitions" / "future.json").read_bytes() == b"{}"
    assert not (install_dir / "app.js").exists()
    assert (install_dir / ".hostless-runtime-assets-v2").is_file()
    assert fetch_mock.call_count == 2
    if os.name != "nt":
        assert (install_dir / cli_name).stat().st_mode & 0o111
        assert (install_dir / wrapper_name).stat().st_mode & 0o111


def test_concurrent_staging_materializes_one_complete_bundle(tmp_path):
    version = "1.2.3"
    runtime_platform = "linux-x64"
    wrapper_name = "copilot-runtime.exe" if os.name == "nt" else "copilot-runtime"
    data = _release_package(runtime_platform)
    cache_dir = tmp_path / "cache"
    fetch = _release_fetches(version, runtime_platform, data)
    fetch_barrier = Barrier(2)

    def concurrent_fetch(url: str, *, timeout: int) -> bytes:
        fetch_barrier.wait(timeout=10)
        return fetch(url, timeout=timeout)

    with (
        patch.object(_cli_download, "get_cache_dir", return_value=cache_dir),
        patch.object(_cli_download, "get_runtime_platform", return_value=runtime_platform),
        patch.object(_cli_download, "_fetch_url_bytes", side_effect=concurrent_fetch) as fetch_mock,
        ThreadPoolExecutor(max_workers=2) as executor,
    ):
        futures = [executor.submit(_cli_download.ensure_runtime_wrapper, version) for _ in range(2)]
        wrappers = [future.result() for future in futures]

    install_dir = cache_dir / "prebuilds" / runtime_platform
    expected_wrapper = str(install_dir / wrapper_name)
    assert wrappers == [expected_wrapper, expected_wrapper]
    assert (install_dir / wrapper_name).read_bytes() == b"wrapper"
    assert (install_dir / "runtime.node").read_bytes() == b"runtime"
    assert (install_dir / ".hostless-runtime-assets-v2").is_file()
    assert not list((cache_dir / "prebuilds").glob(".runtime-bundle-*"))
    assert not (cache_dir / "packages").exists()
    assert fetch_mock.call_count == 4


def test_force_restages_complete_bundle_and_compatibility_alias(tmp_path):
    version = "1.2.3"
    runtime_platform = "linux-x64"
    cli_name = "copilot.exe" if os.name == "nt" else "copilot"
    wrapper_name = "copilot-runtime.exe" if os.name == "nt" else "copilot-runtime"
    data = _release_package(runtime_platform)
    cache_dir = tmp_path / "cache"
    install_dir = cache_dir / "prebuilds" / runtime_platform
    install_dir.mkdir(parents=True)
    (install_dir / cli_name).write_bytes(b"old-alias")
    (install_dir / wrapper_name).write_bytes(b"old-wrapper")
    (install_dir / "runtime.node").write_bytes(b"old-runtime")
    (install_dir / ".hostless-runtime-assets-v2").write_text("1\n", encoding="ascii")
    fetch = _release_fetches(version, runtime_platform, data)

    with (
        patch.object(_cli_download, "get_cache_dir", return_value=cache_dir),
        patch.object(_cli_download, "get_runtime_platform", return_value=runtime_platform),
        patch.object(_cli_download, "_fetch_url_bytes", side_effect=fetch) as fetch_mock,
    ):
        cli = _cli_download.download_cli(version, force=True)

    assert cli == str(install_dir / cli_name)
    assert (install_dir / cli_name).read_bytes() == b"wrapper"
    assert (install_dir / wrapper_name).read_bytes() == b"wrapper"
    assert (install_dir / "runtime.node").read_bytes() == b"runtime"
    assert fetch_mock.call_count == 2


def test_skip_download_returns_none_without_cached_bundle(tmp_path, monkeypatch):
    monkeypatch.setenv("COPILOT_SKIP_CLI_DOWNLOAD", "true")
    runtime_platform = "linux-x64"

    with (
        patch.object(_cli_download, "get_cache_dir", return_value=tmp_path / "cache"),
        patch.object(_cli_download, "get_runtime_platform", return_value=runtime_platform),
        patch.object(_cli_download, "_fetch_url_bytes") as fetch_mock,
    ):
        assert _cli_download.get_or_download_cli("1.2.3") is None

    fetch_mock.assert_not_called()


def test_cached_cli_rejects_alias_from_incomplete_bundle(tmp_path):
    version = "1.2.3"
    runtime_platform = "linux-x64"
    cli_name = "copilot.exe" if os.name == "nt" else "copilot"
    wrapper_name = "copilot-runtime.exe" if os.name == "nt" else "copilot-runtime"
    cache_dir = tmp_path / "cache"
    install_dir = cache_dir / "prebuilds" / runtime_platform
    install_dir.mkdir(parents=True)
    (install_dir / cli_name).write_bytes(b"stale-wrapper")
    (install_dir / wrapper_name).write_bytes(b"wrapper")

    with (
        patch.object(_cli_download, "get_cache_dir", return_value=cache_dir),
        patch.object(_cli_download, "get_runtime_platform", return_value=runtime_platform),
    ):
        assert _cli_download.get_cached_cli_path(version) is None
        with pytest.raises(RuntimeError, match="Incomplete Copilot runtime bundle"):
            _cli_download.download_cli(version)


def test_explicit_cli_reuses_library_from_canonical_staged_bundle(tmp_path):
    version = "1.2.3"
    runtime_platform = "linux-x64"
    data = _release_package(runtime_platform)
    cache_dir = tmp_path / "cache"
    cli_dir = tmp_path / "external"
    cli_dir.mkdir()
    cli_path = cli_dir / ("copilot.exe" if os.name == "nt" else "copilot")
    cli_path.write_bytes(b"external")
    fetch = _release_fetches(version, runtime_platform, data)

    with (
        patch.object(_cli_download, "get_cache_dir", return_value=cache_dir),
        patch.object(_cli_download, "get_runtime_platform", return_value=runtime_platform),
        patch.object(_cli_download, "_fetch_url_bytes", side_effect=fetch) as fetch_mock,
        patch("copilot._ffi_runtime_host.resolve_library_path", return_value=None),
    ):
        library = _cli_download.ensure_runtime_library(str(cli_path), version)
        wrapper = _cli_download.ensure_runtime_wrapper(version)

    assert library == str(cli_dir / _ffi_runtime_host._natural_library_name())
    assert (cli_dir / _ffi_runtime_host._natural_library_name()).read_bytes() == b"runtime"
    assert (cache_dir / "prebuilds" / runtime_platform / "runtime.node").read_bytes() == b"runtime"
    assert not (cache_dir / "packages").exists()
    assert wrapper.endswith(
        os.path.join(
            "prebuilds",
            runtime_platform,
            "copilot-runtime.exe" if os.name == "nt" else "copilot-runtime",
        )
    )
    assert fetch_mock.call_count == 2


def test_resolve_library_path_accepts_adjacent_runtime_node(tmp_path):
    wrapper = tmp_path / ("copilot-runtime.exe" if os.name == "nt" else "copilot-runtime")
    wrapper.write_bytes(b"wrapper")
    runtime_node = tmp_path / "runtime.node"
    runtime_node.write_bytes(b"runtime")

    assert _ffi_runtime_host.resolve_library_path(str(wrapper)) == str(runtime_node)


def test_rejects_cached_wrapper_without_runtime_node(tmp_path):
    runtime_platform = "linux-x64"
    wrapper_name = "copilot-runtime.exe" if os.name == "nt" else "copilot-runtime"
    cache_dir = tmp_path / "cache"
    install_dir = cache_dir / "prebuilds" / runtime_platform
    install_dir.mkdir(parents=True)
    (install_dir / wrapper_name).write_bytes(b"wrapper")

    with (
        patch.object(_cli_download, "get_cache_dir", return_value=cache_dir),
        patch.object(_cli_download, "get_runtime_platform", return_value=runtime_platform),
    ):
        with pytest.raises(RuntimeError, match="Incomplete Copilot runtime bundle"):
            _cli_download.ensure_runtime_wrapper(version="1.2.3")
