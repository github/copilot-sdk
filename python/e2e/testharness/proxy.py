"""
Replaying CAPI proxy for E2E tests.

This manages a child process that acts as a replaying proxy to AI endpoints.
It spawns the shared test harness server from test/harness/server.ts.
"""

import os
import platform
import re
import subprocess
from typing import Any, Optional

import httpx


class CapiProxy:
    """Manages a replaying proxy server for E2E tests."""

    def __init__(self):
        self._process: Optional[subprocess.Popen] = None
        self._proxy_url: Optional[str] = None

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

        # Read the first line to get the listening URL
        line = self._process.stdout.readline()
        if not line:
            self._process.kill()
            raise RuntimeError("Failed to read proxy URL")

        # Parse "Listening: http://..." from output
        match = re.search(r"Listening: (http://[^\s]+)", line.strip())
        if not match:
            self._process.kill()
            raise RuntimeError(f"Unexpected proxy output: {line}")

        self._proxy_url = match.group(1)
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

    @property
    def url(self) -> Optional[str]:
        """Return the proxy URL, or None if not started."""
        return self._proxy_url
