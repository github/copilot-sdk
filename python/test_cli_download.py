"""Tests for unified Copilot release-package provisioning."""

from __future__ import annotations

import errno
import hashlib
import io
import os
import subprocess
import sys
import tarfile
from concurrent.futures import ThreadPoolExecutor
from contextlib import ExitStack
from http.client import IncompleteRead
from threading import Barrier, Event
from types import SimpleNamespace
from unittest.mock import MagicMock, patch

import pytest

from copilot import CopilotClient, RuntimeConnection, _cli_download, _cli_version, _ffi_runtime_host


@pytest.fixture(autouse=True)
def _clear_ambient_skip_download(monkeypatch):
    monkeypatch.delenv("COPILOT_SKIP_CLI_DOWNLOAD", raising=False)


def _release_package(runtime_platform: str) -> bytes:
    wrapper_name = "copilot-runtime.exe" if os.name == "nt" else "copilot-runtime"
    members = {
        f"package/prebuilds/{runtime_platform}/{wrapper_name}": b"wrapper",
        f"package/prebuilds/{runtime_platform}/runtime.node": b"runtime",
        f"package/ripgrep/bin/{runtime_platform}/rg": b"ripgrep",
        "package/definitions/future.json": b"{}",
        "package/app.js": b"excluded",
        "package/cli-main.js": b"excluded",
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


@pytest.mark.parametrize(
    ("version", "repository", "tag"),
    [
        ("1.2.3", "copilot-cli", "v1.2.3"),
        ("1.2.3-4", "copilot-cli", "v1.2.3-4"),
        ("1.2.3-unstable.r123.gabcdef0", "copilot-sdk", "runtime-1.2.3-unstable.r123.gabcdef0"),
        ("1.2.3-4.unstable.r123.gabcdef0", "copilot-sdk", "runtime-1.2.3-4.unstable.r123.gabcdef0"),
        ("0.0.0-0.unstable.r1.g0000000", "copilot-sdk", "runtime-0.0.0-0.unstable.r1.g0000000"),
        ("01.2.3-unstable.r123.gabcdef0", "copilot-cli", "v01.2.3-unstable.r123.gabcdef0"),
        ("1.02.3-unstable.r123.gabcdef0", "copilot-cli", "v1.02.3-unstable.r123.gabcdef0"),
        ("1.2.03-unstable.r123.gabcdef0", "copilot-cli", "v1.2.03-unstable.r123.gabcdef0"),
        ("1.2.3-04.unstable.r123.gabcdef0", "copilot-cli", "v1.2.3-04.unstable.r123.gabcdef0"),
    ],
)
def test_pinned_runtime_uses_matching_release_and_mirror(monkeypatch, version, repository, tag):
    monkeypatch.delenv("COPILOT_CLI_DOWNLOAD_BASE_URL", raising=False)
    asset = _cli_version.get_release_asset_name(version, "linux-x64")
    expected = f"https://github.com/github/{repository}/releases/download/{tag}"
    assert _cli_version.get_download_url(version, asset) == f"{expected}/{asset}"
    assert _cli_version.get_checksums_url(version) == f"{expected}/SHA256SUMS.txt"
    monkeypatch.setenv("COPILOT_CLI_DOWNLOAD_BASE_URL", "https://mirror.example/releases/")
    assert (
        _cli_version.get_checksums_url(version)
        == f"https://mirror.example/releases/{tag}/SHA256SUMS.txt"
    )


@pytest.mark.parametrize(
    "member_name",
    ["package/../outside", r"package\..\outside"],
)
def test_hostless_runtime_path_rejects_traversal(member_name):
    with pytest.raises(RuntimeError, match="Unsafe runtime package path"):
        _cli_download._hostless_runtime_path(member_name, "linux-x64")


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


@pytest.mark.parametrize("version", ["1.2.3", "1.2.3-4.unstable.r123.gabcdef0"])
def test_cli_and_runtime_share_one_staged_bundle(tmp_path, monkeypatch, version):
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
    assert not (install_dir / "cli-main.js").exists()
    assert (install_dir / ".hostless-runtime-assets-v2").is_file()
    assert fetch_mock.call_count == 2
    if os.name != "nt":
        assert (install_dir / cli_name).stat().st_mode & 0o111
        assert (install_dir / wrapper_name).stat().st_mode & 0o111


def test_cached_cli_probe_treats_a_concurrently_removed_bundle_as_a_miss(tmp_path):
    runtime_platform = "linux-x64"
    cli_name = "copilot.exe" if os.name == "nt" else "copilot"
    wrapper_name = "copilot-runtime.exe" if os.name == "nt" else "copilot-runtime"
    cache_dir = tmp_path / "cache"
    install_dir = cache_dir / "prebuilds" / runtime_platform
    install_dir.mkdir(parents=True)
    for name in (cli_name, wrapper_name, "runtime.node", ".hostless-runtime-assets-v2"):
        (install_dir / name).write_bytes(b"x")
    real_is_file = _cli_download.Path.is_file

    def is_file_then_concurrent_removal(path):
        # A repair in another process removes the file right after is_file() saw it.
        found = real_is_file(path)
        if found and path.parent == install_dir:
            path.unlink()
        return found

    with (
        patch.object(_cli_download, "get_cache_dir", return_value=cache_dir),
        patch.object(_cli_download, "get_runtime_platform", return_value=runtime_platform),
        patch.object(_cli_download.Path, "is_file", is_file_then_concurrent_removal),
    ):
        assert _cli_download.get_cached_cli_path("1.2.3") is None


@pytest.mark.skipif(os.name == "nt", reason="POSIX directory permissions")
def test_cli_alias_is_created_when_the_bundle_parent_is_read_only(tmp_path, monkeypatch):
    runtime_platform = "linux-x64"
    cli_name = "copilot"
    cache_dir = tmp_path / "cache"
    prebuilds = cache_dir / "prebuilds"
    install_dir = prebuilds / runtime_platform
    install_dir.mkdir(parents=True)
    (install_dir / "copilot-runtime").write_bytes(b"wrapper")
    (install_dir / "copilot-runtime").chmod(0o755)
    (install_dir / "runtime.node").write_bytes(b"runtime")
    (install_dir / ".hostless-runtime-assets-v2").write_text("1\n", encoding="ascii")
    monkeypatch.setenv("COPILOT_SKIP_CLI_DOWNLOAD", "1")
    # A pre-provisioned bundle: its platform directory is writable, its parent is not.
    prebuilds.chmod(0o555)
    try:
        with (
            patch.object(_cli_download, "get_cache_dir", return_value=cache_dir),
            patch.object(_cli_download, "get_runtime_platform", return_value=runtime_platform),
        ):
            cli = _cli_download.download_cli("1.2.3")
    finally:
        prebuilds.chmod(0o755)

    assert cli == str(install_dir / cli_name)
    assert (install_dir / cli_name).read_bytes() == b"wrapper"


@pytest.mark.skipif(os.name == "nt", reason="POSIX directory flock")
def test_cli_alias_waits_with_an_unreadable_lock_file(tmp_path, monkeypatch):
    import fcntl
    import threading

    runtime_platform = "linux-x64"
    cache_dir = tmp_path / "cache"
    prebuilds = cache_dir / "prebuilds"
    install_dir = prebuilds / runtime_platform
    install_dir.mkdir(parents=True)
    (install_dir / "copilot-runtime").write_bytes(b"wrapper")
    (install_dir / "copilot-runtime").chmod(0o755)
    (install_dir / "runtime.node").write_bytes(b"runtime")
    (install_dir / ".hostless-runtime-assets-v2").write_text("1\n", encoding="ascii")
    # A previous installer used a restrictive umask; its lock is inaccessible.
    lock_path = prebuilds / f".runtime-bundle.{runtime_platform}.lock"
    lock_path.write_bytes(b"")
    lock_path.chmod(0o000)
    monkeypatch.setenv("COPILOT_SKIP_CLI_DOWNLOAD", "1")
    waiting = threading.Event()
    flock = fcntl.flock

    def observe_contention(fd, operation):
        if operation == fcntl.LOCK_EX:
            with pytest.raises(BlockingIOError):
                flock(fd, operation | fcntl.LOCK_NB)
            waiting.set()
        return flock(fd, operation)

    with (
        patch.object(_cli_download, "get_cache_dir", return_value=cache_dir),
        patch.object(_cli_download, "get_runtime_platform", return_value=runtime_platform),
    ):
        holder = os.open(prebuilds, os.O_RDONLY)
        try:
            flock(holder, fcntl.LOCK_EX)
            with (
                patch.object(fcntl, "flock", side_effect=observe_contention),
                ThreadPoolExecutor(max_workers=1) as executor,
            ):
                future = executor.submit(_cli_download.download_cli, "1.2.3")
                future.add_done_callback(lambda _: waiting.set())
                try:
                    assert waiting.wait(10), "Installer never reached the held directory lock"
                    if future.done():
                        future.result()
                    assert not (install_dir / "copilot").exists()
                finally:
                    flock(holder, fcntl.LOCK_UN)
                cli = future.result(timeout=10)
        finally:
            os.close(holder)
            lock_path.chmod(0o600)

    assert cli == str(install_dir / "copilot")
    assert (install_dir / "copilot").read_bytes() == b"wrapper"


@pytest.mark.parametrize("repair", [False, True])
def test_concurrent_staging_materializes_one_complete_bundle(tmp_path, repair):
    version = "1.2.3"
    runtime_platform = "linux-x64"
    wrapper_name = "copilot-runtime.exe" if os.name == "nt" else "copilot-runtime"
    data = _release_package(runtime_platform)
    cache_dir = tmp_path / "cache"
    install_dir = cache_dir / "prebuilds" / runtime_platform
    if repair:
        install_dir.mkdir(parents=True)
        (install_dir / wrapper_name).write_bytes(b"old-wrapper")
        (install_dir / "runtime.node").write_bytes(b"old-runtime")
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
    assert len(fetch_mock.call_args_list) == 4


@pytest.mark.skipif(os.name != "nt", reason="Windows byte-range locking")
@pytest.mark.parametrize("lock_contents", [b"", b"\0"])
def test_windows_cold_install_waits_for_the_bundle_lock(tmp_path, lock_contents):
    import msvcrt

    version = "1.2.3"
    runtime_platform = "win32-x64"
    cache_dir = tmp_path / "cache"
    install_dir = cache_dir / "prebuilds" / runtime_platform
    install_dir.parent.mkdir(parents=True)
    lock_path = install_dir.parent / f".runtime-bundle.{runtime_platform}.lock"
    lock_path.write_bytes(lock_contents)
    data = _release_package(runtime_platform)
    waiting = Event()
    locking = msvcrt.locking
    sleep = _cli_download.time.sleep

    def observe_contention(seconds):
        waiting.set()
        sleep(seconds)

    with lock_path.open("r+b") as holder:
        # Windows can lock byte 0 even before the lock file has any contents.
        locking(holder.fileno(), msvcrt.LK_NBLCK, 1)
        with (
            patch.object(_cli_download, "get_cache_dir", return_value=cache_dir),
            patch.object(_cli_download, "get_runtime_platform", return_value=runtime_platform),
            patch.object(
                _cli_download,
                "_fetch_url_bytes",
                side_effect=_release_fetches(version, runtime_platform, data),
            ),
            patch.object(_cli_download.time, "sleep", side_effect=observe_contention),
            ThreadPoolExecutor(max_workers=1) as executor,
        ):
            future = executor.submit(_cli_download.ensure_runtime_wrapper, version)
            future.add_done_callback(lambda _: waiting.set())
            try:
                assert waiting.wait(10), "Installer never reached the held bundle lock"
                if future.done():
                    future.result()
                assert not install_dir.exists()
            finally:
                locking(holder.fileno(), msvcrt.LK_UNLCK, 1)
            wrapper = future.result(timeout=10)

    assert wrapper == str(install_dir / "copilot-runtime.exe")
    assert (install_dir / "copilot-runtime.exe").read_bytes() == b"wrapper"
    assert (install_dir / "runtime.node").read_bytes() == b"runtime"
    assert (install_dir / ".hostless-runtime-assets-v2").is_file()
    assert not list(install_dir.parent.glob(".runtime-bundle-*"))


def test_runtime_bundle_repair_holds_cross_process_lock(tmp_path):
    import shutil

    version = "1.2.3"
    runtime_platform = "linux-x64"
    wrapper_name = "copilot-runtime.exe" if os.name == "nt" else "copilot-runtime"
    cache_dir = tmp_path / "cache"
    install_dir = cache_dir / "prebuilds" / runtime_platform
    install_dir.mkdir(parents=True)
    (install_dir / wrapper_name).write_bytes(b"old-wrapper")
    (install_dir / "runtime.node").write_bytes(b"old-runtime")
    lock_path = (
        install_dir.parent / f".runtime-bundle.{runtime_platform}.lock"
        if os.name == "nt"
        else install_dir.parent
    )
    data = _release_package(runtime_platform)
    remove_tree = shutil.rmtree

    def assert_other_installer_cannot_enter():
        result = subprocess.run(
            [
                sys.executable,
                "-c",
                """
import os
import sys
if os.name == "nt":
    import msvcrt
    with open(sys.argv[1], "a+b") as lock:
        # Older installers seed empty files before trying to lock them.
        if lock.tell() == 0:
            lock.write(b"\\0")
            lock.flush()
        lock.seek(0)
        try:
            msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
        except OSError:
            sys.exit(0)
else:
    import fcntl
    lock = os.open(sys.argv[1], os.O_RDONLY)
    try:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except BlockingIOError:
        sys.exit(0)
sys.exit("Another installer can enter while the old bundle is being removed")
""",
                str(lock_path),
            ],
            capture_output=True,
            text=True,
            timeout=10,
        )
        assert result.returncode == 0, result.stderr

    def remove_under_lock(path, *args, **kwargs):
        if path == install_dir:
            assert_other_installer_cannot_enter()
        return remove_tree(path, *args, **kwargs)

    with (
        patch.object(_cli_download, "get_cache_dir", return_value=cache_dir),
        patch.object(_cli_download, "get_runtime_platform", return_value=runtime_platform),
        patch.object(
            _cli_download,
            "_fetch_url_bytes",
            side_effect=_release_fetches(version, runtime_platform, data),
        ),
        patch.object(shutil, "rmtree", side_effect=remove_under_lock),
        ExitStack() as stack,
    ):
        if os.name == "nt":
            import msvcrt

            locking = msvcrt.locking

            def check_immediately_after_acquisition(fd, operation, length):
                result = locking(fd, operation, length)
                if operation == msvcrt.LK_NBLCK:
                    assert_other_installer_cannot_enter()
                return result

            stack.enter_context(
                patch.object(msvcrt, "locking", side_effect=check_immediately_after_acquisition)
            )
        wrapper = _cli_download.ensure_runtime_wrapper(version)

    assert wrapper == str(install_dir / wrapper_name)
    assert (install_dir / wrapper_name).read_bytes() == b"wrapper"
    assert (install_dir / "runtime.node").read_bytes() == b"runtime"
    assert (install_dir / ".hostless-runtime-assets-v2").is_file()


def _complete_bundle_without_alias(tmp_path):
    cache_dir = tmp_path / "cache"
    install_dir = cache_dir / "prebuilds" / "linux-x64"
    install_dir.mkdir(parents=True)
    (install_dir / "copilot-runtime").write_bytes(b"wrapper")
    (install_dir / "copilot-runtime").chmod(0o755)
    (install_dir / "runtime.node").write_bytes(b"runtime")
    (install_dir / ".hostless-runtime-assets-v2").write_text("1\n", encoding="ascii")
    return cache_dir, install_dir


@pytest.mark.skipif(os.name == "nt", reason="POSIX flock")
def test_cli_alias_is_created_when_the_cache_cannot_be_locked(tmp_path, monkeypatch):
    import fcntl

    cache_dir, install_dir = _complete_bundle_without_alias(tmp_path)
    monkeypatch.setenv("COPILOT_SKIP_CLI_DOWNLOAD", "1")

    def nfs_flock(fd, operation):
        # Linux NFS rejects an exclusive flock on a read-only descriptor.
        raise OSError(errno.EBADF, "Bad file descriptor")

    with (
        patch.object(_cli_download, "get_cache_dir", return_value=cache_dir),
        patch.object(_cli_download, "get_runtime_platform", return_value="linux-x64"),
        patch.object(fcntl, "flock", side_effect=nfs_flock),
    ):
        cli = _cli_download.download_cli("1.2.3")

    assert cli == str(install_dir / "copilot")
    assert (install_dir / "copilot").read_bytes() == b"wrapper"


@pytest.mark.skipif(
    os.name == "nt" or (hasattr(os, "geteuid") and os.geteuid() == 0),
    reason="POSIX directory permissions, not as root",
)
def test_cli_alias_is_created_when_the_bundle_parent_is_unreadable(tmp_path, monkeypatch):
    cache_dir, install_dir = _complete_bundle_without_alias(tmp_path)
    monkeypatch.setenv("COPILOT_SKIP_CLI_DOWNLOAD", "1")
    # Searchable but not readable: the platform directory is still reachable.
    install_dir.parent.chmod(0o311)
    try:
        with (
            patch.object(_cli_download, "get_cache_dir", return_value=cache_dir),
            patch.object(_cli_download, "get_runtime_platform", return_value="linux-x64"),
        ):
            cli = _cli_download.download_cli("1.2.3")
    finally:
        install_dir.parent.chmod(0o755)

    assert cli == str(install_dir / "copilot")
    assert (install_dir / "copilot").read_bytes() == b"wrapper"


def test_windows_bundle_lock_proceeds_when_an_existing_lock_is_read_only(tmp_path):
    pair_dir = tmp_path / "linux-x64"
    lock_path = tmp_path / ".runtime-bundle.linux-x64.lock"
    lock_path.write_bytes(b"\0")
    msvcrt = SimpleNamespace(LK_LOCK=1, LK_NBLCK=2, LK_UNLCK=0, locking=MagicMock())
    real_open = _cli_download.Path.open

    def read_only_lock(path, mode="r", *args, **kwargs):
        if path == lock_path and "+" in mode:
            raise PermissionError(errno.EACCES, "Access is denied", str(path))
        return real_open(path, mode, *args, **kwargs)

    entered = False
    with (
        patch.object(_cli_download, "sys", SimpleNamespace(platform="win32")),
        patch.dict(sys.modules, {"msvcrt": msvcrt}),
        patch.object(_cli_download.Path, "open", read_only_lock),
    ):
        with _cli_download._runtime_bundle_lock(pair_dir):
            entered = True

    assert entered
    msvcrt.locking.assert_not_called()


@pytest.mark.skipif(
    os.name == "nt" or (hasattr(os, "geteuid") and os.geteuid() == 0),
    reason="POSIX directory permissions, not as root",
)
def test_disabled_download_reports_the_missing_bundle_on_a_read_only_cache(tmp_path, monkeypatch):
    read_only = tmp_path / "read-only"
    read_only.mkdir()
    monkeypatch.setenv("COPILOT_SKIP_CLI_DOWNLOAD", "1")
    read_only.chmod(0o555)
    try:
        with (
            patch.object(_cli_download, "get_cache_dir", return_value=read_only / "cache"),
            patch.object(_cli_download, "get_runtime_platform", return_value="linux-x64"),
        ):
            with pytest.raises(RuntimeError, match="automatic downloads are disabled"):
                _cli_download.ensure_runtime_wrapper("1.2.3")
    finally:
        read_only.chmod(0o755)

    assert not (read_only / "cache").exists()


def test_windows_bundle_lock_proceeds_when_the_cache_does_not_exist_yet(tmp_path):
    msvcrt = SimpleNamespace(LK_LOCK=1, LK_NBLCK=2, LK_UNLCK=0, locking=MagicMock())
    entered = False
    with (
        patch.object(_cli_download, "sys", SimpleNamespace(platform="win32")),
        patch.dict(sys.modules, {"msvcrt": msvcrt}),
    ):
        with _cli_download._runtime_bundle_lock(tmp_path / "missing" / "linux-x64"):
            entered = True

    assert entered
    msvcrt.locking.assert_not_called()


def test_windows_bundle_lock_waits_past_ten_contention_attempts(tmp_path):
    pair_dir = tmp_path / "linux-x64"
    msvcrt = SimpleNamespace(
        LK_LOCK=1,
        LK_NBLCK=2,
        LK_UNLCK=0,
        locking=MagicMock(
            side_effect=[OSError(errno.EACCES, "Lock held by another installer")] * 12
            + [None, None]
        ),
    )
    with (
        patch.object(_cli_download, "sys", SimpleNamespace(platform="win32")),
        patch.dict(sys.modules, {"msvcrt": msvcrt}),
        patch.object(_cli_download.time, "sleep"),
    ):
        with _cli_download._runtime_bundle_lock(pair_dir):
            assert msvcrt.locking.call_count == 13
            assert all(call.args[1] == msvcrt.LK_NBLCK for call in msvcrt.locking.call_args_list)

    assert msvcrt.locking.call_count == 14
    assert msvcrt.locking.call_args.args[1:] == (msvcrt.LK_UNLCK, 1)


def test_windows_bundle_lock_propagates_non_contention_errors(tmp_path):
    failure = OSError(errno.EBADF, "Invalid lock descriptor")
    msvcrt = SimpleNamespace(
        LK_LOCK=1, LK_NBLCK=2, LK_UNLCK=0, locking=MagicMock(side_effect=failure)
    )
    with (
        patch.object(_cli_download, "sys", SimpleNamespace(platform="win32")),
        patch.dict(sys.modules, {"msvcrt": msvcrt}),
        patch.object(_cli_download.time, "sleep") as sleep,
    ):
        with pytest.raises(OSError) as exc_info:
            with _cli_download._runtime_bundle_lock(tmp_path / "linux-x64"):
                pytest.fail("A failed lock must not admit the installer")

    assert exc_info.value is failure
    msvcrt.locking.assert_called_once()
    sleep.assert_not_called()


def test_cli_reuses_alias_published_after_cache_miss(tmp_path):
    version = "1.2.3"
    runtime_platform = "linux-x64"
    cli_name = "copilot.exe" if os.name == "nt" else "copilot"
    wrapper_name = "copilot-runtime.exe" if os.name == "nt" else "copilot-runtime"
    cache_dir = tmp_path / "cache"
    install_dir = cache_dir / "prebuilds" / runtime_platform
    install_dir.mkdir(parents=True)
    wrapper_path = install_dir / wrapper_name
    wrapper_path.write_bytes(b"wrapper")
    (install_dir / "runtime.node").write_bytes(b"runtime")
    (install_dir / ".hostless-runtime-assets-v2").write_text("1\n", encoding="ascii")
    cli_path = install_dir / cli_name
    published_inode = None

    def publish_competing_alias(*args, **kwargs):
        nonlocal published_inode
        cli_path.write_bytes(wrapper_path.read_bytes())
        cli_path.chmod(0o755)
        published_inode = cli_path.stat().st_ino
        return str(wrapper_path)

    with (
        patch.object(_cli_download, "get_cache_dir", return_value=cache_dir),
        patch.object(_cli_download, "get_runtime_platform", return_value=runtime_platform),
        patch.object(_cli_download, "ensure_runtime_wrapper", side_effect=publish_competing_alias),
    ):
        assert _cli_download.get_cached_cli_path(version) is None
        assert _cli_download.download_cli(version) == str(cli_path)

    assert cli_path.read_bytes() == b"wrapper"
    assert cli_path.stat().st_ino == published_inode
    assert not list(install_dir.glob(".cli-*"))


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


@pytest.mark.parametrize("cached", [False, True])
def test_inprocess_explicit_cli_does_not_provision_another_runtime(tmp_path, monkeypatch, cached):
    version = "1.2.3"
    runtime_platform = _cli_version.get_runtime_platform()
    cache_dir = tmp_path / "cache"
    cli_dir = tmp_path / "external"
    cli_dir.mkdir()
    cli_path = cli_dir / ("copilot.exe" if os.name == "nt" else "copilot")
    cli_path.write_bytes(b"external")
    data = _release_package(runtime_platform)
    monkeypatch.setenv("COPILOT_CLI_PATH", str(cli_path))
    monkeypatch.setattr(_cli_download, "CLI_VERSION", version)

    with (
        patch.object(_cli_download, "get_cache_dir", return_value=cache_dir),
        patch.object(_cli_download, "get_runtime_platform", return_value=runtime_platform),
        patch.object(
            _cli_download,
            "_fetch_url_bytes",
            side_effect=_release_fetches(version, runtime_platform, data),
        ) as fetch_mock,
    ):
        if cached:
            _cli_download.ensure_runtime_wrapper(version)
        fetch_mock.reset_mock()

        with pytest.raises(RuntimeError, match="In-process runtime library not found next to"):
            CopilotClient(connection=RuntimeConnection.for_inprocess())

        fetch_mock.assert_not_called()
    assert list(cli_dir.iterdir()) == [cli_path]


@pytest.mark.parametrize("prebuilds", [False, True])
async def test_inprocess_explicit_cli_uses_its_own_library(tmp_path, monkeypatch, prebuilds):
    cli_path = tmp_path / ("copilot.exe" if os.name == "nt" else "copilot")
    cli_path.write_bytes(b"external")
    library_dir = tmp_path
    if prebuilds:
        library_dir = tmp_path / "prebuilds" / _cli_version.get_runtime_platform()
        library_dir.mkdir(parents=True)
    library = library_dir / "runtime.node"
    library.write_bytes(b"explicit runtime")
    monkeypatch.setenv("COPILOT_CLI_PATH", str(cli_path))

    with patch.object(
        _ffi_runtime_host.FfiRuntimeHost,
        "create",
        side_effect=RuntimeError("native loading stopped"),
    ) as create_host:
        client = CopilotClient(connection=RuntimeConnection.for_inprocess())
        with pytest.raises(RuntimeError, match="native loading stopped"):
            await client.start()

    assert create_host.call_args.args == (str(library),)
    assert create_host.call_args.kwargs["cli_entrypoint"] == str(cli_path)


@pytest.mark.parametrize("cli_contents", [None, b""])
def test_inprocess_explicit_cli_rejects_missing_or_empty_entrypoint(
    tmp_path, monkeypatch, cli_contents
):
    cli_path = tmp_path / ("copilot.exe" if os.name == "nt" else "copilot")
    if cli_contents is not None:
        cli_path.write_bytes(cli_contents)
    (tmp_path / "runtime.node").write_bytes(b"selected runtime")
    monkeypatch.setenv("COPILOT_CLI_PATH", str(cli_path))

    with pytest.raises(RuntimeError, match="COPILOT_CLI_PATH.*non-empty CLI file"):
        CopilotClient(connection=RuntimeConnection.for_inprocess())


def test_download_runtime_can_provision_library_from_canonical_staged_bundle(tmp_path):
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
