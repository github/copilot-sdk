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
        except RuntimeError as exc:
            print(f"Error: {exc}", file=sys.stderr)
            sys.exit(1)
    else:
        parser.print_help()
        sys.exit(1)
