"""Copilot CLI version and platform asset information.

At publish time, CLI_VERSION is overwritten by scripts/inject-cli-version.mjs
with the concrete version string (e.g. "1.0.64-1"). In development (editable
installs, running from source) the sentinel value None disables automatic
download — callers must set an explicit path or COPILOT_CLI_PATH.
"""

from __future__ import annotations

import platform
import sys

# Sentinel: None means "no pinned version" (dev/editable install).
# Overwritten at publish time by scripts/inject-cli-version.mjs.
# DO NOT reformat this line — the inject script matches it exactly.
CLI_VERSION: str | None = None

# Maps (sys.platform, platform.machine()) → (archive filename, binary name inside archive).
PLATFORM_ASSETS: dict[tuple[str, str], tuple[str, str]] = {
    ("linux", "x86_64"): ("copilot-linux-x64.tar.gz", "copilot"),
    ("linux", "aarch64"): ("copilot-linux-arm64.tar.gz", "copilot"),
    ("linux", "arm64"): ("copilot-linux-arm64.tar.gz", "copilot"),
    ("darwin", "x86_64"): ("copilot-darwin-x64.tar.gz", "copilot"),
    ("darwin", "arm64"): ("copilot-darwin-arm64.tar.gz", "copilot"),
    ("win32", "AMD64"): ("copilot-win32-x64.zip", "copilot.exe"),
    ("win32", "ARM64"): ("copilot-win32-arm64.zip", "copilot.exe"),
}

# Musl (Alpine) variants — detected at runtime via _is_musl().
_MUSL_ASSETS: dict[str, tuple[str, str]] = {
    "x86_64": ("copilot-linuxmusl-x64.tar.gz", "copilot"),
    "aarch64": ("copilot-linuxmusl-arm64.tar.gz", "copilot"),
    "arm64": ("copilot-linuxmusl-arm64.tar.gz", "copilot"),
}

_DOWNLOAD_BASE_URL = "https://github.com/github/copilot-cli/releases/download"

# The native in-process (FFI) runtime library (`runtime.node`) is NOT part of the
# GitHub Releases `copilot-<platform>` archive (that ships only the CLI binary). It
# lives in the npm platform package `@github/copilot-<npm-platform>`, under
# `package/prebuilds/<npm-platform>/runtime.node`. Mirrors the .NET SDK targets,
# which download the same npm tarball.
_NPM_REGISTRY_BASE_URL = "https://registry.npmjs.org"

# Maps (sys.platform, platform.machine()) → npm platform name (glibc Linux/macOS/Windows).
NPM_PLATFORMS: dict[tuple[str, str], str] = {
    ("linux", "x86_64"): "linux-x64",
    ("linux", "aarch64"): "linux-arm64",
    ("linux", "arm64"): "linux-arm64",
    ("darwin", "x86_64"): "darwin-x64",
    ("darwin", "arm64"): "darwin-arm64",
    ("win32", "AMD64"): "win32-x64",
    ("win32", "ARM64"): "win32-arm64",
}

# Musl (Alpine) npm platform variants — detected at runtime via _is_musl().
_MUSL_NPM_PLATFORMS: dict[str, str] = {
    "x86_64": "linuxmusl-x64",
    "aarch64": "linuxmusl-arm64",
    "arm64": "linuxmusl-arm64",
}


def _is_musl() -> bool:
    """Detect whether the current Linux system uses musl libc (e.g. Alpine)."""
    if sys.platform != "linux":
        return False
    try:
        import subprocess

        result = subprocess.run(["ldd", "--version"], capture_output=True, text=True, timeout=5)
        # musl's ldd prints "musl libc" in its output
        output = result.stdout + result.stderr
        return "musl" in output.lower()
    except (FileNotFoundError, subprocess.TimeoutExpired, OSError):
        return False


def get_platform_key() -> tuple[str, str]:
    """Return the (sys.platform, machine) key for the current platform."""
    return (sys.platform, platform.machine())


def get_asset_info() -> tuple[str, str]:
    """Return (archive_filename, binary_name) for the current platform.

    Raises RuntimeError if the platform is not supported.
    """
    key = get_platform_key()

    # On Linux, check for musl/Alpine first
    if key[0] == "linux" and _is_musl():
        musl_info = _MUSL_ASSETS.get(key[1])
        if musl_info:
            return musl_info

    info = PLATFORM_ASSETS.get(key)
    if info is None:
        raise RuntimeError(
            f"Unsupported platform: {key[0]}/{key[1]}. "
            f"Supported platforms: {', '.join(f'{p}/{m}' for p, m in PLATFORM_ASSETS)}"
        )
    return info


def get_download_url(version: str, archive_name: str) -> str:
    """Return the download URL for a given version and archive."""
    import os

    base = os.environ.get("COPILOT_CLI_DOWNLOAD_BASE_URL", _DOWNLOAD_BASE_URL)
    return f"{base}/v{version}/{archive_name}"


def get_checksums_url(version: str) -> str:
    """Return the URL for the SHA256SUMS.txt file."""
    import os

    base = os.environ.get("COPILOT_CLI_DOWNLOAD_BASE_URL", _DOWNLOAD_BASE_URL)
    return f"{base}/v{version}/SHA256SUMS.txt"


def get_npm_platform() -> str:
    """Return the npm platform name (e.g. ``linux-x64``) for the current host.

    Used to locate the native in-process runtime library. Raises RuntimeError if
    the platform is not supported.
    """
    key = get_platform_key()

    if key[0] == "linux" and _is_musl():
        musl = _MUSL_NPM_PLATFORMS.get(key[1])
        if musl:
            return musl

    npm_platform = NPM_PLATFORMS.get(key)
    if npm_platform is None:
        raise RuntimeError(
            f"Unsupported platform for in-process runtime: {key[0]}/{key[1]}. "
            f"Supported platforms: {', '.join(f'{p}/{m}' for p, m in NPM_PLATFORMS)}"
        )
    return npm_platform


def get_runtime_lib_packument_url(npm_platform: str) -> str:
    """Return the npm packument URL for the platform runtime package."""
    import os

    base = os.environ.get("COPILOT_NPM_REGISTRY_URL", _NPM_REGISTRY_BASE_URL).rstrip("/")
    return f"{base}/@github/copilot-{npm_platform}"


def get_runtime_lib_url(version: str, npm_platform: str) -> str:
    """Return the download URL for the platform runtime tarball.

    Mirrors the .NET targets' URL layout
    ``<registry>/@github/copilot-<platform>/-/copilot-<platform>-<version>.tgz``.
    """
    import os

    base = os.environ.get("COPILOT_NPM_REGISTRY_URL", _NPM_REGISTRY_BASE_URL).rstrip("/")
    return f"{base}/@github/copilot-{npm_platform}/-/copilot-{npm_platform}-{version}.tgz"
