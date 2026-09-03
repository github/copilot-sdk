"""Download and cache the Copilot CLI runtime package.

The platform-specific GitHub release package contains the out-of-process runtime
wrapper, native runtime library, and runtime assets, but omits the legacy SEA
``copilot[.exe]``. Its bytes are downloaded and verified, then the filtered hostless
bundle is materialized directly into the SDK's existing cache layout. ``download_cli``
preserves the historical CLI filename by creating a compatibility alias from the
runtime wrapper inside the complete materialized bundle:

- Linux:   ~/.cache/github-copilot-sdk/cli/{version}/prebuilds/{platform}/copilot
- macOS:   ~/Library/Caches/github-copilot-sdk/cli/{version}/prebuilds/{platform}/copilot
- Windows: %LOCALAPPDATA%/github-copilot-sdk/cli/{version}/prebuilds/{platform}/copilot.exe

Environment variables:
- COPILOT_CLI_EXTRACT_DIR: Override the runtime bundle cache root.
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
from http.client import IncompleteRead
from pathlib import Path, PurePosixPath
from urllib.error import HTTPError, URLError
from urllib.request import urlopen

from ._cli_version import (
    CLI_VERSION,
    get_checksums_url,
    get_cli_binary_name,
    get_download_url,
    get_release_asset_name,
    get_runtime_platform,
)

_CACHE_DIR_NAME = "github-copilot-sdk"
_MAX_RETRIES = 3
_RETRIABLE_DOWNLOAD_ERRORS = (HTTPError, URLError, IncompleteRead)
_HOSTLESS_ASSETS_MARKER = ".hostless-runtime-assets-v2"


def _sanitize_version(version: str) -> str:
    """Sanitize version string for use as a directory name.

    Replaces any character not in [a-zA-Z0-9._-] with underscore.
    Matches the Rust SDK's sanitization logic.
    """
    return re.sub(r"[^a-zA-Z0-9._\-]", "_", version)


def get_cache_dir(version: str | None = None) -> Path:
    """Return the cache directory for runtime bundles.

    Args:
        version: CLI version string. If None, returns the root cache dir.
    """
    # COPILOT_CLI_EXTRACT_DIR overrides the entire version-specific directory.
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
    """Return the cached compatibility entrypoint for a complete runtime bundle.

    Args:
        version: CLI version. Defaults to the pinned CLI_VERSION.

    Returns:
        Path to the binary, or None if not cached.
    """
    ver = version or CLI_VERSION
    if not ver:
        return None

    try:
        runtime_platform = get_runtime_platform()
    except RuntimeError:
        return None
    binary_name = get_cli_binary_name()
    wrapper_name = "copilot-runtime.exe" if sys.platform == "win32" else "copilot-runtime"
    pair_dir = get_cache_dir(ver) / "prebuilds" / runtime_platform
    binary_path = pair_dir / binary_name
    required = (
        binary_path,
        pair_dir / wrapper_name,
        pair_dir / "runtime.node",
        pair_dir / _HOSTLESS_ASSETS_MARKER,
    )

    if all(path.is_file() and path.stat().st_size > 0 for path in required):
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
    try:
        text = _fetch_url_bytes(url, timeout=30).decode("utf-8")
    except (RuntimeError, UnicodeDecodeError) as exc:
        raise RuntimeError(
            f"Failed to download checksums from {url}: {exc}\n\n"
            "If you are in an offline or firewalled environment, set "
            "COPILOT_CLI_PATH to point to a manually-installed binary."
        ) from exc

    checksums: dict[str, str] = {}
    for line in text.strip().splitlines():
        parts = line.split()
        if len(parts) == 2 and re.fullmatch(r"[a-fA-F0-9]{64}", parts[0]):
            digest, filename = parts
            # Some formats use *filename (binary mode indicator)
            checksums[filename.lstrip("*")] = digest.lower()
    return checksums


def _verify_checksum(data: bytes, expected_hash: str, filename: str) -> None:
    """Verify SHA-256 checksum of downloaded data."""
    actual = hashlib.sha256(data).hexdigest()
    if actual != expected_hash:
        raise RuntimeError(
            f"Checksum mismatch for {filename}:\n  expected: {expected_hash}\n  actual:   {actual}"
        )


def _fetch_verified_release_package(version: str, runtime_platform: str) -> bytes:
    """Download and verify the unified platform release package."""
    asset_name = get_release_asset_name(version, runtime_platform)
    expected_hash = _fetch_checksums(version).get(asset_name)
    if not expected_hash:
        raise RuntimeError(f"SHA256SUMS.txt does not contain {asset_name}.")
    url = get_download_url(version, asset_name)
    data = _fetch_url_bytes(url, timeout=600)
    _verify_checksum(data, expected_hash, asset_name)
    return data


def _runtime_bundle_is_complete(pair_dir: Path, wrapper_name: str) -> bool:
    required = (
        pair_dir / wrapper_name,
        pair_dir / "runtime.node",
        pair_dir / _HOSTLESS_ASSETS_MARKER,
    )
    return all(path.is_file() and path.stat().st_size > 0 for path in required)


def download_cli(version: str | None = None, *, force: bool = False) -> str:
    """Provision a complete runtime bundle with a ``copilot[.exe]`` alias.

    Args:
        version: CLI version to download. Defaults to the pinned CLI_VERSION.
        force: If True, re-download even if already cached.

    Returns:
        Path to the compatibility entrypoint adjacent to the complete runtime bundle.

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

    binary_name = get_cli_binary_name()

    if not force:
        cached = get_cached_cli_path(ver)
        if cached is not None:
            return cached

    wrapper_path = Path(ensure_runtime_wrapper(ver, force=force))
    binary_path = wrapper_path.with_name(binary_name)
    fd, temp_name = tempfile.mkstemp(dir=wrapper_path.parent, prefix=".cli-")
    try:
        with os.fdopen(fd, "wb") as destination:
            destination.write(wrapper_path.read_bytes())
        staged = Path(temp_name)
        if sys.platform != "win32":
            staged.chmod(staged.stat().st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
        os.replace(staged, binary_path)
    except OSError:
        try:
            os.unlink(temp_name)
        except OSError:
            pass
        if not force:
            cached = get_cached_cli_path(ver)
            if cached is not None:
                return cached
        raise

    return str(binary_path)


def _fetch_url_bytes(url: str, *, timeout: int) -> bytes:
    """Download bytes from ``url`` with retries."""
    last_exc: Exception | None = None
    for attempt in range(_MAX_RETRIES):
        try:
            with urlopen(url, timeout=timeout) as response:
                return response.read()
        except _RETRIABLE_DOWNLOAD_ERRORS as exc:
            last_exc = exc
            if attempt < _MAX_RETRIES - 1:
                time.sleep(2**attempt)
    raise RuntimeError(f"Failed to download from {url}: {last_exc}") from last_exc


_HOSTLESS_EXCLUDED_TOP_LEVEL = {
    "app.js",
    "assets",
    "changelog.json",
    "copilot",
    "copilot.exe",
    "copilot-sdk",
    "foundry-local-sdk",
    "index.js",
    "LICENSE.md",
    "napi-oop-runtime",
    "npm-loader.js",
    "package.json",
    "preloads",
    "pvrecorder",
    "queries",
    "README.md",
    "sdk",
    "sea-loader.js",
    "webview",
}


def _hostless_runtime_path(member_name: str, runtime_platform: str) -> Path | None:
    parts = PurePosixPath(member_name).parts
    if not parts or parts[0] != "package" or len(parts) < 2:
        return None
    relative = parts[1:]
    top_level = relative[0]
    file_name = relative[-1]
    if (
        top_level in _HOSTLESS_EXCLUDED_TOP_LEVEL
        or (top_level.startswith("tree-sitter") and top_level.endswith(".wasm"))
        or (top_level.startswith("voice-") and top_level.endswith(".js"))
        or file_name == "cli-native.node"
        or "mediaremote-adapter" in relative
        or file_name.startswith("copilot-runtime-bin")
    ):
        return None
    if top_level == "prebuilds":
        if len(relative) < 3 or relative[1] != runtime_platform:
            return None
        relative = relative[2:]
    destination = Path(*relative)
    if destination.is_absolute() or ".." in destination.parts:
        raise RuntimeError(f"Unsafe runtime package path: {member_name}")
    return destination


def _materialize_runtime_bundle(data: bytes, runtime_platform: str, destination: Path) -> None:
    """Extract the hostless runtime tree, retaining unknown package assets by default."""
    with tarfile.open(fileobj=io.BytesIO(data), mode="r:gz") as archive:
        for member in archive:
            relative = _hostless_runtime_path(member.name, runtime_platform)
            if relative is None or member.isdir():
                continue
            if not member.isfile():
                raise RuntimeError(f"Unsupported runtime package entry: {member.name}")
            extracted = archive.extractfile(member)
            if extracted is None:
                raise RuntimeError(f"Failed to read runtime package entry: {member.name}")
            target = destination / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(extracted.read())
            if sys.platform != "win32":
                target.chmod(member.mode & 0o777)


def ensure_runtime_wrapper(version: str | None = None, force: bool = False) -> str:
    """Provision the runtime pair and retained assets from the release package."""
    ver = version or CLI_VERSION
    if not ver:
        raise RuntimeError("No runtime version is pinned.")
    runtime_platform = get_runtime_platform()
    wrapper_name = "copilot-runtime.exe" if sys.platform == "win32" else "copilot-runtime"
    pair_dir = get_cache_dir(ver) / "prebuilds" / runtime_platform
    wrapper_path = pair_dir / wrapper_name
    runtime_path = pair_dir / "runtime.node"
    assets_marker = pair_dir / _HOSTLESS_ASSETS_MARKER

    wrapper_exists = wrapper_path.is_file() and wrapper_path.stat().st_size > 0
    runtime_exists = runtime_path.is_file() and runtime_path.stat().st_size > 0
    if _runtime_bundle_is_complete(pair_dir, wrapper_name) and not force:
        return str(wrapper_path)
    if not force and wrapper_exists != runtime_exists:
        raise RuntimeError(
            f"Incomplete Copilot runtime bundle in {pair_dir}: "
            f"{wrapper_name} and runtime.node are required."
        )
    if _should_skip_download():
        raise RuntimeError(
            f"Copilot runtime bundle is not cached in {pair_dir} "
            "and automatic downloads are disabled."
        )

    data = _fetch_verified_release_package(ver, runtime_platform)
    import shutil

    pair_dir.parent.mkdir(parents=True, exist_ok=True)
    staging_dir = Path(tempfile.mkdtemp(dir=pair_dir.parent, prefix=".runtime-bundle-"))
    try:
        _materialize_runtime_bundle(data, runtime_platform, staging_dir)
        staged_wrapper = staging_dir / wrapper_name
        staged_runtime = staging_dir / "runtime.node"
        if (
            not staged_wrapper.is_file()
            or staged_wrapper.stat().st_size == 0
            or not staged_runtime.is_file()
            or staged_runtime.stat().st_size == 0
        ):
            raise RuntimeError("Copilot runtime wrapper and runtime.node must both be non-empty.")
        if sys.platform != "win32":
            staged_wrapper.chmod(
                staged_wrapper.stat().st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH
            )
        (staging_dir / assets_marker.name).write_text("1\n", encoding="ascii")
        try:
            if pair_dir.exists() and (force or not assets_marker.is_file()):
                shutil.rmtree(pair_dir, ignore_errors=True)
            staging_dir.replace(pair_dir)
        except OSError:
            if (
                wrapper_path.is_file()
                and wrapper_path.stat().st_size > 0
                and runtime_path.is_file()
                and runtime_path.stat().st_size > 0
                and assets_marker.is_file()
            ):
                return str(wrapper_path)
            raise
    finally:
        if staging_dir.exists():
            shutil.rmtree(staging_dir, ignore_errors=True)

    return str(wrapper_path)


def ensure_runtime_library(cli_path: str, version: str | None = None) -> str | None:
    """Ensure the native in-process (FFI) runtime library sits next to ``cli_path``.

    The canonical staged bundle contains ``prebuilds/<platform>/runtime.node``.
    This helper reuses that verified library and copies it next to the CLI binary
    under its natural platform name (``libcopilot_runtime.so`` / ``.dylib`` /
    ``copilot_runtime.dll``).

    Copying the library next to an external CLI is opt-in — this is only invoked when
    the in-process transport is selected (lazy) or via
    ``python -m copilot download-runtime --in-process`` (explicit). The default stdio
    path leaves the library in the canonical staged bundle.

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

    ver = version or CLI_VERSION
    if not ver:
        return None

    try:
        runtime_platform = get_runtime_platform()
    except RuntimeError:
        return None

    cli_dir = Path(cli_path).resolve().parent
    lib_path = cli_dir / _natural_library_name()
    if lib_path.exists():
        return str(lib_path)

    pair_dir = get_cache_dir(ver) / "prebuilds" / runtime_platform
    wrapper_name = "copilot-runtime.exe" if sys.platform == "win32" else "copilot-runtime"
    if _should_skip_download() and not _runtime_bundle_is_complete(pair_dir, wrapper_name):
        return None
    wrapper_path = Path(ensure_runtime_wrapper(ver))
    canonical_runtime = wrapper_path.with_name("runtime.node")

    # Write atomically next to the CLI so concurrent starts don't observe a partial
    # library. A rename within the same directory is atomic on POSIX and Windows.
    import shutil

    cli_dir.mkdir(parents=True, exist_ok=True)
    fd, tmp_name = tempfile.mkstemp(dir=cli_dir, prefix=".runtime-lib-")
    try:
        with os.fdopen(fd, "wb") as out, canonical_runtime.open("rb") as source:
            shutil.copyfileobj(source, out)
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

    # Check platform support before attempting download
    try:
        runtime_platform = get_runtime_platform()
    except RuntimeError:
        return None

    if _should_skip_download():
        pair_dir = get_cache_dir(ver) / "prebuilds" / runtime_platform
        wrapper_name = "copilot-runtime.exe" if sys.platform == "win32" else "copilot-runtime"
        if not _runtime_bundle_is_complete(pair_dir, wrapper_name):
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
            if args.in_process:
                path = download_cli(ver, force=args.force)
            else:
                path = ensure_runtime_wrapper(ver, force=args.force)
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
