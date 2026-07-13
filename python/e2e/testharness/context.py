"""
Test context for E2E tests.

Provides isolated directories and a replaying proxy for testing the SDK.
"""

import asyncio
import contextlib
import os
import re
import shutil
import tempfile
import time
from pathlib import Path
from typing import Any

from copilot import CopilotClient, RuntimeConnection

from .proxy import CapiProxy


def get_cli_path_for_tests() -> str:
    """Get CLI path for E2E tests.

    Uses COPILOT_CLI_PATH env var if set, otherwise node_modules CLI.
    """
    env_path = os.environ.get("COPILOT_CLI_PATH")
    if env_path and Path(env_path).exists():
        return str(Path(env_path).resolve())

    # Look for CLI in sibling nodejs directory's node_modules. As of CLI 1.0.64-1
    # the @github/copilot package is a thin loader; the runnable index.js ships in
    # the installed platform package (e.g. @github/copilot-linux-x64).
    base_path = Path(__file__).parents[3]
    github_modules = base_path / "nodejs" / "node_modules" / "@github"
    for platform_pkg in sorted(github_modules.glob("copilot-*")):
        candidate = platform_pkg / "index.js"
        if candidate.exists():
            return str(candidate.resolve())

    raise RuntimeError("CLI not found for tests. Run 'npm install' in the nodejs directory.")


CLI_PATH = get_cli_path_for_tests()
SNAPSHOTS_DIR = Path(__file__).parents[3] / "test" / "snapshots"
DEFAULT_GITHUB_TOKEN = "fake-token-for-e2e-tests"


def is_inprocess_transport() -> bool:
    """Return True when the E2E suite should run over the in-process (FFI) transport.

    Selected by the ``inprocess`` CI matrix cell via
    ``COPILOT_SDK_DEFAULT_CONNECTION=inprocess``. Mirrors the Node/.NET harnesses.
    """
    return (os.environ.get("COPILOT_SDK_DEFAULT_CONNECTION") or "").lower() == "inprocess"


class E2ETestContext:
    """Holds shared resources for E2E tests."""

    def __init__(self):
        self.cli_path: str = ""
        self.home_dir: str = ""
        self.work_dir: str = ""
        self.proxy_url: str = ""
        self._proxy: CapiProxy | None = None
        self._client: CopilotClient | None = None
        self._inprocess: bool = is_inprocess_transport()
        self._restore_env: list[tuple[str, str | None]] = []
        self._restore_cwd: str | None = None

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

        # Create the shared client (like Node.js/Go do). The in-process (FFI)
        # transport loads the runtime into this test host process, so it cannot
        # honor a per-client working_directory or env block: the worker inherits
        # this process's ambient cwd and environment. We therefore mirror the
        # per-test redirects, isolated home, and credentials onto the real process
        # (os.environ writes reach native getenv on CPython) and chdir into the
        # work dir, then create the client without working_directory/env. This
        # matches the Node/.NET in-process harnesses.
        if self._inprocess:
            self._apply_inprocess_environment()
            self._client = CopilotClient(
                connection=RuntimeConnection.for_inprocess(
                    path=self.cli_path,
                    args=tuple(cli_args or []),
                ),
                github_token=DEFAULT_GITHUB_TOKEN,
            )
        else:
            self._client = CopilotClient(
                connection=RuntimeConnection.for_stdio(
                    path=self.cli_path,
                    args=tuple(cli_args or []),
                ),
                working_directory=self.work_dir,
                env=self.get_env(),
                github_token=DEFAULT_GITHUB_TOKEN,
            )

    def _apply_inprocess_environment(self) -> None:
        """Mirror the isolated test environment onto the real process for in-process hosting.

        The in-process worker inherits this process's environment and cwd at
        spawn, so the per-test redirects must live on ``os.environ`` and the
        process cwd. Auth flows via GH_TOKEN/GITHUB_TOKEN (the FFI argv omits the
        stdio ``--auth-token-env`` wiring) and HMAC is disabled so host-side auth
        resolution matches the replay snapshots. Restored in ``teardown``.
        """
        inprocess_env = dict(self.get_env())
        inprocess_env.update(
            {
                "GH_TOKEN": DEFAULT_GITHUB_TOKEN,
                "GITHUB_TOKEN": DEFAULT_GITHUB_TOKEN,
                "COPILOT_HMAC_KEY": "",
                "CAPI_HMAC_KEY": "",
            }
        )
        for key, value in inprocess_env.items():
            self._restore_env.append((key, os.environ.get(key)))
            os.environ[key] = value

        self._restore_cwd = os.getcwd()
        os.chdir(self.work_dir)

    def add_runtime_env(self, key: str, value: str) -> None:
        """Set an env var seen by the runtime, honoring the active transport.

        Child-process transports read env from the client's env block, but the
        in-process worker inherits *this* process's environment, so the var must
        live on ``os.environ`` (and be restored in teardown). Must be called
        before the runtime starts (i.e., before the first ``create_session``).
        """
        if self._inprocess:
            self._restore_env.append((key, os.environ.get(key)))
            os.environ[key] = value
        else:
            options = self.client._options
            if options.env is None:
                options.env = {}
            options.env[key] = value

    def _restore_inprocess_environment(self) -> None:
        """Undo the in-process environment mirror and cwd change from setup."""
        for key, previous in reversed(self._restore_env):
            if previous is None:
                os.environ.pop(key, None)
            else:
                os.environ[key] = previous
        self._restore_env = []
        if self._restore_cwd is not None:
            with contextlib.suppress(OSError):
                os.chdir(self._restore_cwd)
            self._restore_cwd = None

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

        if self._inprocess:
            self._restore_inprocess_environment()

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
            base_path = Path(base_dir)
            base_path.mkdir(parents=True, exist_ok=True)
            for item in base_path.iterdir():
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
                # Route GitHub API calls (e.g. the MCP registry policy check) to
                # the replay proxy so MCP enablement stays hermetic. Without this
                # the CLI reaches the real api.github.com, which is slow/unreachable
                # on macOS CI runners and makes MCP servers time out before
                # reaching connected.
                "COPILOT_DEBUG_GITHUB_API_URL": self.proxy_url,
                "COPILOT_HOME": self.home_dir,
                "COPILOT_SDK_AUTH_TOKEN": DEFAULT_GITHUB_TOKEN,
                "GH_CONFIG_DIR": self.home_dir,
                "GH_TOKEN": DEFAULT_GITHUB_TOKEN,
                "XDG_CONFIG_HOME": self.home_dir,
                "XDG_STATE_HOME": self.home_dir,
                "GITHUB_TOKEN": DEFAULT_GITHUB_TOKEN,
                "COPILOT_MCP_APPS": "true",
                "MCP_APPS": "true",
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

    async def wait_for_exchanges(
        self, minimum_count: int = 1, timeout: float = 120.0
    ) -> list[dict[str, Any]]:
        """Wait until the proxy has captured at least the requested exchanges."""
        deadline = time.monotonic() + timeout
        exchanges: list[dict[str, Any]] = []
        while time.monotonic() < deadline:
            exchanges = await self.get_exchanges()
            if len(exchanges) >= minimum_count:
                return exchanges
            await asyncio.sleep(0.1)
        raise TimeoutError(f"Timed out waiting for {minimum_count} chat completion request(s)")
