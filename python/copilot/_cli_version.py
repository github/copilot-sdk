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
