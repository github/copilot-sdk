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

_DOWNLOAD_BASE_URL = "https://github.com/github/copilot-cli/releases/download"

# Maps (sys.platform, platform.machine()) to the platform segment used by release assets.
RUNTIME_PLATFORMS: dict[tuple[str, str], str] = {
    ("linux", "x86_64"): "linux-x64",
    ("linux", "aarch64"): "linux-arm64",
    ("linux", "arm64"): "linux-arm64",
    ("darwin", "x86_64"): "darwin-x64",
    ("darwin", "arm64"): "darwin-arm64",
    ("win32", "AMD64"): "win32-x64",
    ("win32", "ARM64"): "win32-arm64",
}

# Musl (Alpine) runtime platform variants — detected at runtime via _is_musl().
_MUSL_RUNTIME_PLATFORMS: dict[str, str] = {
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


def get_download_url(version: str, archive_name: str) -> str:
    """Return the download URL for a given version and archive."""
    import os

    base = os.environ.get("COPILOT_CLI_DOWNLOAD_BASE_URL", _DOWNLOAD_BASE_URL).rstrip("/")
    return f"{base}/v{version}/{archive_name}"


def get_checksums_url(version: str) -> str:
    """Return the URL for the SHA256SUMS.txt file."""
    import os

    base = os.environ.get("COPILOT_CLI_DOWNLOAD_BASE_URL", _DOWNLOAD_BASE_URL).rstrip("/")
    return f"{base}/v{version}/SHA256SUMS.txt"


def get_runtime_platform() -> str:
    """Return the release asset platform name (e.g. ``linux-x64``) for this host.

    The name matches the ``prebuilds`` folder embedded in the release package.
    Raises RuntimeError if the platform is not supported.
    """
    key = get_platform_key()

    if key[0] == "linux" and _is_musl():
        musl = _MUSL_RUNTIME_PLATFORMS.get(key[1])
        if musl:
            return musl

    runtime_platform = RUNTIME_PLATFORMS.get(key)
    if runtime_platform is None:
        raise RuntimeError(
            f"Unsupported Copilot runtime platform: {key[0]}/{key[1]}. "
            f"Supported platforms: {', '.join(f'{p}/{m}' for p, m in RUNTIME_PLATFORMS)}"
        )
    return runtime_platform


def get_release_asset_name(version: str, runtime_platform: str | None = None) -> str:
    """Return the unified runtime package asset name for a version and platform."""
    platform_name = runtime_platform or get_runtime_platform()
    return f"github-copilot-{version}-{platform_name}.tgz"


def get_cli_binary_name() -> str:
    """Return the CLI executable name inside the release package."""
    return "copilot.exe" if sys.platform == "win32" else "copilot"
