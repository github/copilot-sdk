"""Download and cache the Copilot CLI binary.

This module implements a download-at-first-use strategy for the Copilot CLI
binary, similar to the Rust SDK's build.rs approach but triggered at runtime.
The binary is cached in a shared directory compatible with the Rust SDK:

- Linux:   ~/.cache/github-copilot-sdk/cli/{version}/copilot
- macOS:   ~/Library/Caches/github-copilot-sdk/cli/{version}/copilot
- Windows: %LOCALAPPDATA%/github-copilot-sdk/cli/{version}/copilot.exe

Environment variables:
- COPILOT_CLI_EXTRACT_DIR: Override the cache directory (binary placed directly here).
- COPILOT_SKIP_CLI_DOWNLOAD: Set to "1" or "true" to disable auto-download.
- COPILOT_CLI_DOWNLOAD_BASE_URL: Override the GitHub Releases base URL.
"""

from __future__ import annotations

import base64
import hashlib
import io
import os
import re
import stat
import sys
import tarfile
import tempfile
import time
import zipfile
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import urlopen

from ._cli_version import (
    CLI_VERSION,
    get_asset_info,
    get_checksums_url,
    get_download_url,
    get_npm_platform,
    get_runtime_lib_packument_url,
    get_runtime_lib_url,
)

_CACHE_DIR_NAME = "github-copilot-sdk"
_MAX_RETRIES = 3


def _sanitize_version(version: str) -> str:
    """Sanitize version string for use as a directory name.

    Replaces any character not in [a-zA-Z0-9._-] with underscore.
    Matches the Rust SDK's sanitization logic.
    """
    return re.sub(r"[^a-zA-Z0-9._\-]", "_", version)


def get_cache_dir(version: str | None = None) -> Path:
    """Return the cache directory for CLI binaries.

    Args:
        version: CLI version string. If None, returns the root cache dir.
    """
    # COPILOT_CLI_EXTRACT_DIR overrides the entire version-specific directory
    # (binary lives directly at $dir/<binary>, no version subdir). Matches Rust SDK.
    extract_override = os.environ.get("COPILOT_CLI_EXTRACT_DIR")
    if extract_override:
        return Path(extract_override)

    if sys.platform == "darwin":
        root = Path.home() / "Library" / "Caches" / _CACHE_DIR_NAME
    elif sys.platform == "win32":
        local_app_data = os.environ.get("LOCALAPPDATA")
        if local_app_data:
            root = Path(local_app_data) / _CACHE_DIR_NAME
        else:
            root = Path.home() / "AppData" / "Local" / _CACHE_DIR_NAME
    else:
        xdg = os.environ.get("XDG_CACHE_HOME")
        if xdg:
            root = Path(xdg) / _CACHE_DIR_NAME
        else:
            root = Path.home() / ".cache" / _CACHE_DIR_NAME

    if version:
        return root / "cli" / _sanitize_version(version)
    return root / "cli"


def get_cached_cli_path(version: str | None = None) -> str | None:
    """Return the path to the cached CLI binary if it exists.

    Args:
        version: CLI version. Defaults to the pinned CLI_VERSION.

    Returns:
        Path to the binary, or None if not cached.
    """
    ver = version or CLI_VERSION
    if not ver:
        return None

    try:
        _, binary_name = get_asset_info()
    except RuntimeError:
        return None
    binary_path = get_cache_dir(ver) / binary_name

    if binary_path.exists():
        return str(binary_path)
    return None


def _should_skip_download() -> bool:
    """Check if auto-download is disabled via environment variable."""
    val = os.environ.get("COPILOT_SKIP_CLI_DOWNLOAD", "").lower()
    return val in ("1", "true", "yes")


def _fetch_checksums(version: str) -> dict[str, str]:
    """Fetch and parse the SHA256SUMS.txt file.

    Returns a dict mapping filename → sha256 hex digest.
    """
    url = get_checksums_url(version)
    last_exc: Exception | None = None
    for attempt in range(_MAX_RETRIES):
        try:
            with urlopen(url, timeout=30) as response:
                text = response.read().decode("utf-8")
            break
        except (HTTPError, URLError) as exc:
            last_exc = exc
            if attempt < _MAX_RETRIES - 1:
                time.sleep(2**attempt)
    else:
        raise RuntimeError(
            f"Failed to download checksums from {url}: {last_exc}\n\n"
            "If you are in an offline or firewalled environment, set "
            "COPILOT_CLI_PATH to point to a manually-installed binary."
        ) from last_exc

    checksums: dict[str, str] = {}
    for line in text.strip().splitlines():
        parts = line.split()
        if len(parts) == 2:
            digest, filename = parts
            # Some formats use *filename (binary mode indicator)
            checksums[filename.lstrip("*")] = digest
    return checksums


def _verify_checksum(data: bytes, expected_hash: str, filename: str) -> None:
    """Verify SHA-256 checksum of downloaded data."""
    actual = hashlib.sha256(data).hexdigest()
    if actual != expected_hash:
        raise RuntimeError(
            f"Checksum mismatch for {filename}:\n  expected: {expected_hash}\n  actual:   {actual}"
        )


def _extract_tar_gz(data: bytes, binary_name: str, dest_dir: Path) -> Path:
    """Extract the CLI binary from a .tar.gz archive."""
    with tarfile.open(fileobj=io.BytesIO(data), mode="r:gz") as tf:
        # Find the binary in the archive (may be at top level or in a subdirectory)
        members = tf.getnames()
        target_member = None
        for name in members:
            if name == binary_name or name.endswith(f"/{binary_name}"):
                target_member = name
                break

        if target_member is None:
            raise RuntimeError(
                f"Binary '{binary_name}' not found in archive. Archive contains: {members}"
            )

        member = tf.getmember(target_member)
        f = tf.extractfile(member)
        if f is None:
            raise RuntimeError(f"Could not extract '{target_member}' from archive")

        dest_path = dest_dir / binary_name
        with open(dest_path, "wb") as out:
            out.write(f.read())

    return dest_path


def _extract_zip(data: bytes, binary_name: str, dest_dir: Path) -> Path:
    """Extract the CLI binary from a .zip archive."""
    with zipfile.ZipFile(io.BytesIO(data)) as zf:
        names = zf.namelist()
        target_member = None
        for name in names:
            if name == binary_name or name.endswith(f"/{binary_name}"):
                target_member = name
                break

        if target_member is None:
            raise RuntimeError(
                f"Binary '{binary_name}' not found in archive. Archive contains: {names}"
            )

        dest_path = dest_dir / binary_name
        with zf.open(target_member) as src, open(dest_path, "wb") as out:
            out.write(src.read())

    return dest_path


def download_cli(version: str | None = None, *, force: bool = False) -> str:
    """Download the Copilot CLI binary and cache it.

    Args:
        version: CLI version to download. Defaults to the pinned CLI_VERSION.
        force: If True, re-download even if already cached.

    Returns:
        Path to the cached binary.

    Raises:
        RuntimeError: If the version is not set, download fails, or
                      checksum verification fails.
    """
    ver = version or CLI_VERSION
    if not ver:
        raise RuntimeError(
            "No CLI version pinned. This is a development install — "
            "set COPILOT_CLI_PATH or install a published wheel."
        )

    archive_name, binary_name = get_asset_info()
    cache_dir = get_cache_dir(ver)
    binary_path = cache_dir / binary_name

    # Return cached binary if available (unless force)
    if not force and binary_path.exists():
        return str(binary_path)

    # Fetch checksums
    checksums = _fetch_checksums(ver)
    expected_hash = checksums.get(archive_name)
    if not expected_hash:
        raise RuntimeError(
            f"No checksum found for '{archive_name}' in SHA256SUMS.txt. "
            f"Available files: {list(checksums.keys())}"
        )

    # Download archive with retries
    url = get_download_url(ver, archive_name)
    last_exc: Exception | None = None
    data: bytes | None = None
    for attempt in range(_MAX_RETRIES):
        try:
            with urlopen(url, timeout=120) as response:
                data = response.read()
            break
        except (HTTPError, URLError) as exc:
            last_exc = exc
            if attempt < _MAX_RETRIES - 1:
                time.sleep(2**attempt)
    if data is None:
        raise RuntimeError(
            f"Failed to download runtime from {url}: {last_exc}\n\n"
            "If you are in an offline or firewalled environment, you can:\n"
            f"1. Manually download the archive from: {url}\n"
            f"2. Extract the '{binary_name}' binary to: {binary_path}\n"
            "Or set COPILOT_CLI_PATH to point to an existing binary."
        ) from last_exc

    # Verify checksum
    _verify_checksum(data, expected_hash, archive_name)

    # Extract to a temporary directory, then atomically move into place.
    # This prevents partial/corrupt cache entries if the process is interrupted.
    cache_dir.mkdir(parents=True, exist_ok=True)
    staging_dir = Path(tempfile.mkdtemp(dir=cache_dir, prefix=".download-"))
    try:
        if archive_name.endswith(".tar.gz"):
            extracted = _extract_tar_gz(data, binary_name, staging_dir)
        elif archive_name.endswith(".zip"):
            extracted = _extract_zip(data, binary_name, staging_dir)
        else:
            raise RuntimeError(f"Unknown archive format: {archive_name}")

        # Make executable on Unix
        if sys.platform != "win32":
            extracted.chmod(extracted.stat().st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)

        # Atomic rename into final location. Handle concurrent processes:
        # another process may have written the file while we were downloading.
        try:
            extracted.replace(binary_path)
        except OSError:
            if not force and binary_path.exists():
                return str(binary_path)
            raise
    finally:
        # Clean up staging directory
        try:
            staging_dir.rmdir()
        except OSError:
            # May not be empty if rename failed or other files were extracted
            import shutil

            shutil.rmtree(staging_dir, ignore_errors=True)

    return str(binary_path)


def _fetch_url_bytes(url: str, *, timeout: int) -> bytes:
    """Download bytes from ``url`` with retries."""
    last_exc: Exception | None = None
    for attempt in range(_MAX_RETRIES):
        try:
            with urlopen(url, timeout=timeout) as response:
                return response.read()
        except (HTTPError, URLError) as exc:
            last_exc = exc
            if attempt < _MAX_RETRIES - 1:
                time.sleep(2**attempt)
    raise RuntimeError(f"Failed to download from {url}: {last_exc}") from last_exc


def _fetch_runtime_integrity(npm_platform: str, version: str) -> str | None:
    """Return the npm ``dist.integrity`` (Subresource Integrity) for the tarball.

    Best-effort: returns None if the packument can't be fetched or parsed.
    """
    import json

    url = get_runtime_lib_packument_url(npm_platform)
    try:
        raw = _fetch_url_bytes(url, timeout=30)
        packument = json.loads(raw)
        dist = packument.get("versions", {}).get(version, {}).get("dist", {})
        integrity = dist.get("integrity")
        return integrity if isinstance(integrity, str) else None
    except (RuntimeError, ValueError, KeyError):
        return None


def _verify_integrity(data: bytes, integrity: str) -> None:
    """Verify data against an npm Subresource Integrity string (e.g. ``sha512-<b64>``)."""
    algo, _, b64 = integrity.partition("-")
    algo = algo.lower()
    if algo not in ("sha512", "sha384", "sha256"):
        # Fail closed: an unrecognized algorithm means we cannot verify this native
        # library, so refuse rather than loading unverified native code.
        raise RuntimeError(
            f"Unsupported integrity algorithm '{algo}' for the in-process runtime "
            "library; refusing to load unverified native code."
        )
    expected = base64.b64decode(b64)
    actual = hashlib.new(algo, data).digest()
    if actual != expected:
        raise RuntimeError(
            f"Integrity mismatch for runtime library ({algo}): "
            "downloaded tarball does not match the npm registry checksum."
        )


def _extract_runtime_node(data: bytes, npm_platform: str) -> bytes:
    """Extract ``package/prebuilds/<npm_platform>/runtime.node`` from an npm tarball."""
    target = f"package/prebuilds/{npm_platform}/runtime.node"
    with tarfile.open(fileobj=io.BytesIO(data), mode="r:gz") as tf:
        for name in tf.getnames():
            if name == target or name.endswith(f"/prebuilds/{npm_platform}/runtime.node"):
                member = tf.getmember(name)
                extracted = tf.extractfile(member)
                if extracted is not None:
                    return extracted.read()
        raise RuntimeError(f"'{target}' not found in runtime package for {npm_platform}.")


def ensure_runtime_library(cli_path: str, version: str | None = None) -> str | None:
    """Ensure the native in-process (FFI) runtime library sits next to ``cli_path``.

    The library is NOT part of the GitHub Releases CLI archive; it ships in the npm
    platform package ``@github/copilot-<platform>`` under
    ``package/prebuilds/<platform>/runtime.node``. This helper downloads that tarball
    and writes the library next to the CLI binary under its natural platform name
    (``libcopilot_runtime.so`` / ``.dylib`` / ``copilot_runtime.dll``).

    This is opt-in — only invoked when the in-process transport is actually selected
    (lazy) or via ``python -m copilot download-runtime --in-process`` (explicit). The
    default stdio download path never fetches these extra bytes.

    Returns the absolute path to the library, or None if it could not be provisioned
    (e.g. download disabled or unsupported platform). Raises RuntimeError on
    download/verification failure.
    """
    # Import lazily to avoid a hard dependency for stdio-only users.
    from ._ffi_runtime_host import _natural_library_name, resolve_library_path

    # Already present (bundled prebuilds layout in dev, or a prior download)?
    existing = resolve_library_path(cli_path)
    if existing is not None:
        return existing

    if _should_skip_download():
        return None

    ver = version or CLI_VERSION
    if not ver:
        return None

    try:
        npm_platform = get_npm_platform()
    except RuntimeError:
        return None

    cli_dir = Path(cli_path).resolve().parent
    lib_path = cli_dir / _natural_library_name()
    if lib_path.exists():
        return str(lib_path)

    url = get_runtime_lib_url(ver, npm_platform)
    data = _fetch_url_bytes(url, timeout=600)

    integrity = _fetch_runtime_integrity(npm_platform, ver)
    if not integrity:
        # Fail closed: this native library is loaded into the host process, so it must
        # be verified before use. The npm packument (which carries dist.integrity) was
        # unavailable, so refuse rather than loading unverified native code — mirroring
        # the CLI download, which requires a checksum. Retry when the registry is
        # reachable, or install a runtime package that ships the library.
        raise RuntimeError(
            "No Subresource Integrity value available for the in-process runtime "
            f"library ({npm_platform}@{ver}); refusing to load unverified native code."
        )
    _verify_integrity(data, integrity)

    lib_bytes = _extract_runtime_node(data, npm_platform)

    # Write atomically next to the CLI so concurrent starts don't observe a partial
    # library. A rename within the same directory is atomic on POSIX and Windows.
    cli_dir.mkdir(parents=True, exist_ok=True)
    fd, tmp_name = tempfile.mkstemp(dir=cli_dir, prefix=".runtime-lib-")
    try:
        with os.fdopen(fd, "wb") as out:
            out.write(lib_bytes)
        os.replace(tmp_name, lib_path)
    except OSError:
        try:
            os.unlink(tmp_name)
        except OSError:
            # Best-effort cleanup of the temp file; ignore if it's already gone or
            # can't be removed (the OS reclaims it, and it doesn't affect correctness).
            pass
        if lib_path.exists():
            return str(lib_path)
        raise

    return str(lib_path)


def get_or_download_cli(version: str | None = None) -> str | None:
    """Get the cached CLI binary, downloading it if necessary.

    Returns None if:
    - No version is pinned (dev install)
    - Auto-download is disabled via COPILOT_SKIP_CLI_DOWNLOAD
    - The platform is unsupported

    Raises RuntimeError on download/verification failures.
    """
    ver = version or CLI_VERSION
    if not ver:
        return None

    # Check cache first
    cached = get_cached_cli_path(ver)
    if cached:
        return cached

    # Check if download is disabled
    if _should_skip_download():
        return None

    # Check platform support before attempting download
    try:
        get_asset_info()
    except RuntimeError:
        return None

    # Download
    return download_cli(ver)


def main() -> None:
    """CLI entry point for `python -m copilot download-runtime`."""
    import argparse

    parser = argparse.ArgumentParser(
        prog="python -m copilot",
        description="Copilot SDK utilities",
    )
    subparsers = parser.add_subparsers(dest="command")

    # download-runtime subcommand
    dl_parser = subparsers.add_parser(
        "download-runtime",
        help="Download the Copilot runtime",
    )
    dl_parser.add_argument(
        "--force",
        action="store_true",
        help="Re-download even if already cached",
    )
    dl_parser.add_argument(
        "--version",
        help="Runtime version to download (default: pinned version)",
    )
    dl_parser.add_argument(
        "--in-process",
        action="store_true",
        help=(
            "Also download the native in-process (FFI) runtime library "
            "(prebuilds/<platform>/runtime.node) and place it next to the CLI. "
            "Only needed for the experimental in-process transport."
        ),
    )

    args = parser.parse_args()

    if args.command == "download-runtime":
        ver = args.version or CLI_VERSION
        if not ver:
            print(
                "Error: No runtime version pinned (development install). "
                "Use --version to specify a version.",
                file=sys.stderr,
            )
            sys.exit(1)

        print(f"Downloading Copilot runtime v{ver}...")
        try:
            path = download_cli(ver, force=args.force)
            print(f"Runtime cached at: {path}")
            if args.in_process:
                print("Downloading in-process (FFI) runtime library...")
                lib_path = ensure_runtime_library(path, ver)
                if lib_path:
                    print(f"Runtime library cached at: {lib_path}")
                else:
                    print(
                        "Warning: could not provision the in-process runtime library "
                        "(download disabled or unsupported platform).",
                        file=sys.stderr,
                    )
        except RuntimeError as exc:
            print(f"Error: {exc}", file=sys.stderr)
            sys.exit(1)
    else:
        parser.print_help()
        sys.exit(1)
