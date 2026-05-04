"""
Replaying CAPI proxy for E2E tests.

This manages a child process that acts as a replaying proxy to AI endpoints.
It spawns the shared test harness server from test/harness/server.ts.
"""

import json
import os
import platform
import re
import subprocess
from typing import Any

import httpx


class CapiProxy:
    """Manages a replaying proxy server for E2E tests."""

    def __init__(self):
        self._process: subprocess.Popen | None = None
        self._proxy_url: str | None = None
        self._connect_proxy_url: str | None = None
        self._ca_file_path: str | None = None

    async def start(self) -> str:
        """Launch the proxy server and return its URL."""
        if self._proxy_url:
            return self._proxy_url

        # The harness server is in the shared test directory
        server_path = os.path.join(
            os.path.dirname(__file__), "..", "..", "..", "test", "harness", "server.ts"
        )
        server_path = os.path.abspath(server_path)

        # On Windows, use shell=True to find npx
        use_shell = platform.system() == "Windows"

        self._process = subprocess.Popen(
            ["npx", "tsx", server_path],
            stdout=subprocess.PIPE,
            stderr=None,  # Inherit stderr to parent for debugging
            text=True,
            cwd=os.path.dirname(server_path),
            shell=use_shell,
        )

        # Read until the server prints "Listening: http://..."; npm/npx may emit
        # wrapper output first on some platforms.
        line = ""
        match = None
        while True:
            line = self._process.stdout.readline()
            if not line:
                self._process.kill()
                raise RuntimeError("Failed to read proxy URL")
            match = re.search(r"Listening: (http://[^\s]+)", line.strip())
            if match:
                break

        self._proxy_url = match.group(1)
        metadata_match = re.search(r"(\{.*\})\s*$", line.strip())
        if not metadata_match:
            self._process.kill()
            raise RuntimeError(f"Proxy startup line missing CONNECT proxy metadata: {line}")
        try:
            metadata = json.loads(metadata_match.group(1))
        except json.JSONDecodeError as exc:
            self._process.kill()
            raise RuntimeError(f"Failed to parse proxy startup metadata: {line}") from exc
        self._connect_proxy_url = metadata.get("connectProxyUrl")
        self._ca_file_path = metadata.get("caFilePath")
        if not self._connect_proxy_url or not self._ca_file_path:
            self._process.kill()
            raise RuntimeError(f"Proxy startup metadata missing CONNECT proxy details: {line}")
        return self._proxy_url

    async def stop(self, skip_writing_cache: bool = False):
        """Gracefully shut down the proxy server.

        Args:
            skip_writing_cache: If True, the proxy won't write captured exchanges to disk.
        """
        if not self._process:
            return

        # Send stop request to the server
        if self._proxy_url:
            try:
                stop_url = f"{self._proxy_url}/stop"
                if skip_writing_cache:
                    stop_url += "?skipWritingCache=true"
                async with httpx.AsyncClient() as client:
                    await client.post(stop_url)
            except Exception:
                pass  # Best effort

        # Wait for process to exit
        self._process.wait()
        self._process = None
        self._proxy_url = None

    async def configure(self, file_path: str, work_dir: str):
        """Send configuration to the proxy."""
        if not self._proxy_url:
            raise RuntimeError("Proxy not started")

        async with httpx.AsyncClient() as client:
            resp = await client.post(
                f"{self._proxy_url}/config",
                json={"filePath": file_path, "workDir": work_dir},
            )
            if resp.status_code != 200:
                raise RuntimeError(f"Proxy config failed with status {resp.status_code}")

    async def get_exchanges(self) -> list[dict[str, Any]]:
        """Retrieve the captured HTTP exchanges from the proxy."""
        if not self._proxy_url:
            raise RuntimeError("Proxy not started")

        async with httpx.AsyncClient() as client:
            resp = await client.get(f"{self._proxy_url}/exchanges")
            return resp.json()

    async def set_copilot_user_by_token(self, token: str, response: dict[str, Any]) -> None:
        """Register a per-token response for /copilot_internal/user."""
        if not self._proxy_url:
            raise RuntimeError("Proxy not started")

        async with httpx.AsyncClient() as client:
            resp = await client.post(
                f"{self._proxy_url}/copilot-user-config",
                json={"token": token, "response": response},
            )
            assert resp.status_code == 200

    @property
    def url(self) -> str | None:
        """Return the proxy URL, or None if not started."""
        return self._proxy_url

    def get_proxy_env(self) -> dict[str, str]:
        """Return environment variables that route HTTPS traffic through the CONNECT proxy."""
        if not self._connect_proxy_url or not self._ca_file_path:
            return {}

        no_proxy = "127.0.0.1,localhost,::1"
        return {
            "HTTP_PROXY": self._connect_proxy_url,
            "HTTPS_PROXY": self._connect_proxy_url,
            "http_proxy": self._connect_proxy_url,
            "https_proxy": self._connect_proxy_url,
            "NO_PROXY": no_proxy,
            "no_proxy": no_proxy,
            "NODE_EXTRA_CA_CERTS": self._ca_file_path,
            "SSL_CERT_FILE": self._ca_file_path,
            "REQUESTS_CA_BUNDLE": self._ca_file_path,
            "CURL_CA_BUNDLE": self._ca_file_path,
            "GIT_SSL_CAINFO": self._ca_file_path,
            "GH_TOKEN": "",
            "GITHUB_TOKEN": "",
            "GH_ENTERPRISE_TOKEN": "",
            "GITHUB_ENTERPRISE_TOKEN": "",
        }
