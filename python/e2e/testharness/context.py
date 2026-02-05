"""
Test context for E2E tests.

Provides isolated directories and a replaying proxy for testing the SDK.
"""

import os
import re
import shutil
import tempfile
from pathlib import Path
from typing import Optional

from copilot import CopilotClient

from .proxy import CapiProxy


def get_cli_path_for_tests() -> str:
    """Get CLI path for E2E tests. Uses node_modules CLI during development."""
    # Look for CLI in sibling nodejs directory's node_modules
    base_path = Path(__file__).parents[3]
    full_path = base_path / "nodejs" / "node_modules" / "@github" / "copilot" / "index.js"
    if full_path.exists():
        return str(full_path.resolve())

    raise RuntimeError("CLI not found for tests. Run 'npm install' in the nodejs directory.")


CLI_PATH = get_cli_path_for_tests()
SNAPSHOTS_DIR = Path(__file__).parents[3] / "test" / "snapshots"


class E2ETestContext:
    """Holds shared resources for E2E tests."""

    def __init__(self):
        self.cli_path: str = ""
        self.home_dir: str = ""
        self.work_dir: str = ""
        self.proxy_url: str = ""
        self._proxy: Optional[CapiProxy] = None
        self._client: Optional[CopilotClient] = None

    async def setup(self):
        """Set up the test context with a shared client."""
        self.cli_path = get_cli_path_for_tests()

        self.home_dir = tempfile.mkdtemp(prefix="copilot-test-config-")
        self.work_dir = tempfile.mkdtemp(prefix="copilot-test-work-")

        self._proxy = CapiProxy()
        self.proxy_url = await self._proxy.start()

        # Create the shared client (like Node.js/Go do)
        # Use fake token in CI to allow cached responses without real auth
        github_token = "fake-token-for-e2e-tests" if os.environ.get("CI") == "true" else None
        self._client = CopilotClient(
            {
                "cli_path": self.cli_path,
                "cwd": self.work_dir,
                "env": self.get_env(),
                "github_token": github_token,
            }
        )

    async def teardown(self, test_failed: bool = False):
        """Clean up the test context.

        Args:
            test_failed: If True, skip writing snapshots to avoid corruption.
        """
        if self._client:
            await self._client.stop()
            self._client = None

        if self._proxy:
            await self._proxy.stop(skip_writing_cache=test_failed)
            self._proxy = None

        if self.home_dir and os.path.exists(self.home_dir):
            shutil.rmtree(self.home_dir, ignore_errors=True)

        if self.work_dir and os.path.exists(self.work_dir):
            shutil.rmtree(self.work_dir, ignore_errors=True)

    async def configure_for_test(self, test_file: str, test_name: str):
        """
        Configure the proxy for a specific test.

        Args:
            test_file: The test file name (e.g., "session" from "test_session.py")
            test_name: The test name (e.g., "should_have_stateful_conversation")
        """
        sanitized_name = re.sub(r"[^a-zA-Z0-9]", "_", test_name).lower()
        snapshot_path = SNAPSHOTS_DIR / test_file / f"{sanitized_name}.yaml"
        abs_snapshot_path = str(snapshot_path.resolve())

        if self._proxy:
            await self._proxy.configure(abs_snapshot_path, self.work_dir)

        # Clear temp directories between tests (but leave them in place)
        for item in Path(self.home_dir).iterdir():
            if item.is_dir():
                shutil.rmtree(item)
            else:
                item.unlink()
        for item in Path(self.work_dir).iterdir():
            if item.is_dir():
                shutil.rmtree(item)
            else:
                item.unlink()

    def get_env(self) -> dict:
        """Return environment variables configured for isolated testing."""
        env = os.environ.copy()

        env.update(
            {
                "COPILOT_API_URL": self.proxy_url,
                "XDG_CONFIG_HOME": self.home_dir,
                "XDG_STATE_HOME": self.home_dir,
            }
        )
        return env

    @property
    def client(self) -> CopilotClient:
        """Return the shared CopilotClient instance."""
        if not self._client:
            raise RuntimeError("Context not set up. Call setup() first.")
        return self._client

    async def get_exchanges(self):
        """Retrieve the captured HTTP exchanges from the proxy."""
        if not self._proxy:
            raise RuntimeError("Proxy not started")
        return await self._proxy.get_exchanges()
