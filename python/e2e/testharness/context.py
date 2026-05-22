"""
Test context for E2E tests.

Provides isolated directories and a replaying proxy for testing the SDK.
"""

import contextlib
import os
import re
import shutil
import tempfile
from pathlib import Path
from typing import Any

from copilot import CopilotClient, CopilotClientOptions, RuntimeConnection

from .proxy import CapiProxy


def get_cli_path_for_tests() -> str:
    """Get CLI path for E2E tests.

    Uses COPILOT_CLI_PATH env var if set, otherwise node_modules CLI.
    """
    env_path = os.environ.get("COPILOT_CLI_PATH")
    if env_path and Path(env_path).exists():
        return str(Path(env_path).resolve())

    # Look for CLI in sibling nodejs directory's node_modules
    base_path = Path(__file__).parents[3]
    full_path = base_path / "nodejs" / "node_modules" / "@github" / "copilot" / "index.js"
    if full_path.exists():
        return str(full_path.resolve())

    raise RuntimeError("CLI not found for tests. Run 'npm install' in the nodejs directory.")


CLI_PATH = get_cli_path_for_tests()
SNAPSHOTS_DIR = Path(__file__).parents[3] / "test" / "snapshots"
DEFAULT_GITHUB_TOKEN = "fake-token-for-e2e-tests"


class E2ETestContext:
    """Holds shared resources for E2E tests."""

    def __init__(self):
        self.cli_path: str = ""
        self.home_dir: str = ""
        self.work_dir: str = ""
        self.proxy_url: str = ""
        self._proxy: CapiProxy | None = None
        self._client: CopilotClient | None = None

    async def setup(self, cli_args: list[str] | None = None):
        """Set up the test context with a shared client.

        Args:
            cli_args: Optional extra CLI arguments passed to the CLI process.
        """
        self.cli_path = get_cli_path_for_tests()

        self.home_dir = os.path.realpath(tempfile.mkdtemp(prefix="copilot-test-config-"))
        self.work_dir = os.path.realpath(tempfile.mkdtemp(prefix="copilot-test-work-"))

        self._proxy = CapiProxy()
        self.proxy_url = await self._proxy.start()
        await self._proxy.set_copilot_user_by_token(
            DEFAULT_GITHUB_TOKEN,
            {
                "login": "e2e-test-user",
                "copilot_plan": "individual_pro",
                "endpoints": {
                    "api": self.proxy_url,
                    "telemetry": "https://localhost:1/telemetry",
                },
                "analytics_tracking_id": "e2e-test-tracking-id",
            },
        )

        # Create the shared client (like Node.js/Go do)
        self._client = CopilotClient(
            CopilotClientOptions(
                connection=RuntimeConnection.stdio(
                    path=self.cli_path,
                    args=tuple(cli_args or []),
                ),
                working_directory=self.work_dir,
                env=self.get_env(),
                github_token=DEFAULT_GITHUB_TOKEN,
            )
        )

    async def teardown(self, test_failed: bool = False):
        """Clean up the test context.

        Args:
            test_failed: If True, skip writing snapshots to avoid corruption.
        """
        if self._client:
            try:
                await self._client.stop()
            except ExceptionGroup:
                pass  # stop() completes all cleanup before raising; safe to ignore in teardown
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
        # Use ignore_errors=True / suppress(OSError) to handle race conditions
        # where files (e.g., SQLite session-store.db on Windows) may still be
        # held open by a background process during cleanup.
        for base_dir in (self.home_dir, self.work_dir):
            for item in Path(base_dir).iterdir():
                if item.is_dir():
                    shutil.rmtree(item, ignore_errors=True)
                else:
                    with contextlib.suppress(OSError):
                        item.unlink(missing_ok=True)

    def get_env(self) -> dict:
        """Return environment variables configured for isolated testing."""
        env = os.environ.copy()
        if self._proxy:
            env.update(self._proxy.get_proxy_env())

        env.update(
            {
                "COPILOT_API_URL": self.proxy_url,
                "base_directory": self.home_dir,
                "COPILOT_SDK_AUTH_TOKEN": DEFAULT_GITHUB_TOKEN,
                "GH_CONFIG_DIR": self.home_dir,
                "GH_TOKEN": DEFAULT_GITHUB_TOKEN,
                "XDG_CONFIG_HOME": self.home_dir,
                "XDG_STATE_HOME": self.home_dir,
                "GITHUB_TOKEN": DEFAULT_GITHUB_TOKEN,
            }
        )
        return env

    @property
    def client(self) -> CopilotClient:
        """Return the shared CopilotClient instance."""
        if not self._client:
            raise RuntimeError("Context not set up. Call setup() first.")
        return self._client

    async def set_copilot_user_by_token(self, token: str, response: dict[str, Any]) -> None:
        """Register a per-token response for the /copilot_internal/user endpoint."""
        if not self._proxy:
            raise RuntimeError("Proxy not started")
        await self._proxy.set_copilot_user_by_token(token, response)

    async def get_exchanges(self):
        """Retrieve the captured HTTP exchanges from the proxy."""
        if not self._proxy:
            raise RuntimeError("Proxy not started")
        return await self._proxy.get_exchanges()
