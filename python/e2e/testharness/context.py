"""
Test context for E2E tests.

Provides isolated directories and a replaying proxy for testing the SDK.
"""

import asyncio
import contextlib
import os
import re
import shutil
import subprocess
import tempfile
from functools import wraps
from pathlib import Path
from typing import Any

from copilot import CopilotClient, CopilotSession, RuntimeConnection
from copilot.client import _InternalTestOptions

from .proxy import CapiProxy


def _prepare_checkout_cli(repo_root: Path) -> str:
    npm = "npm.cmd" if os.name == "nt" else "npm"
    result = subprocess.run(
        [npm, "run", "--silent", "prepare:runtime", "--", "--print-path"],
        cwd=repo_root / "nodejs",
        capture_output=True,
        text=True,
        check=False,
    )
    output = result.stdout.strip()
    if result.returncode != 0 or not output:
        detail = result.stderr.strip() or output or f"exit code {result.returncode}"
        raise RuntimeError(f"Failed to prepare the same-checkout Copilot CLI: {detail}")
    cli_path = Path(output.splitlines()[-1])
    if not cli_path.is_file():
        raise RuntimeError(f"Same-checkout Copilot CLI was not found at {cli_path}")
    return str(cli_path.resolve())


def get_cli_path_for_tests() -> str:
    """Get CLI path for E2E tests.

    Uses COPILOT_CLI_PATH if set, otherwise resolves the same-checkout runtime.
    """
    env_path = os.environ.get("COPILOT_CLI_PATH")
    if env_path and Path(env_path).is_file():
        return str(Path(env_path).resolve())
    if env_path:
        raise RuntimeError(
            "COPILOT_CLI_PATH must select an existing same-checkout runtime for SDK tests"
        )

    return _prepare_checkout_cli(Path(__file__).parents[3])


CLI_PATH = get_cli_path_for_tests()
SNAPSHOTS_DIR = Path(__file__).parents[3] / "test" / "snapshots"
DEFAULT_GITHUB_TOKEN = "fake-token-for-e2e-tests"


def is_inprocess_transport() -> bool:
    """Return True when the E2E suite should run over the in-process (FFI) transport.

    Selected by the ``inprocess`` CI matrix cell via
    ``COPILOT_SDK_DEFAULT_CONNECTION=inprocess``. Mirrors the Node/.NET harnesses.
    """
    return (os.environ.get("COPILOT_SDK_DEFAULT_CONNECTION") or "").lower() == "inprocess"


class _TestClient(CopilotClient):
    def __init__(self, work_dir: str | None, **options: Any):
        super().__init__(**options)
        self._test_work_dir = work_dir

    @wraps(CopilotClient.create_session)
    async def create_session(self, **options: Any) -> CopilotSession:
        if (
            self._test_work_dir is not None
            and options.get("working_directory") is None
            and options.get("cloud") is None
        ):
            options["working_directory"] = self._test_work_dir
        return await super().create_session(**options)


class E2ETestContext:
    """Holds shared resources for E2E tests."""

    def __init__(self, *, use_stdio: bool = False):
        self.cli_path: str = ""
        self.home_dir: str = ""
        self.work_dir: str = ""
        self.proxy_url: str = ""
        self._proxy: CapiProxy | None = None
        self._client: CopilotClient | None = None
        self._inprocess: bool = is_inprocess_transport() and not use_stdio
        self._client_inprocess: bool = False

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
                "id": 12345,
                "copilot_plan": "individual_pro",
                "endpoints": {
                    "api": self.proxy_url,
                    "telemetry": "https://sdk-hydro-collector.example.com",
                },
                "analytics_tracking_id": "e2e-test-tracking-id",
            },
        )

        self._client_inprocess = self._inprocess and not cli_args
        runtime_env = self.get_env()
        test_options = _InternalTestOptions(
            copilot_api_url=runtime_env["COPILOT_API_URL"],
            github_api_url=runtime_env["COPILOT_DEBUG_GITHUB_API_URL"],
            proxy_url=runtime_env.get("HTTPS_PROXY"),
            no_proxy=runtime_env.get("NO_PROXY"),
            ca_certificate_path=runtime_env.get("NODE_EXTRA_CA_CERTS"),
        )
        for key in test_options._to_environment():
            runtime_env.pop(key, None)
        if self._client_inprocess:
            self._client = _TestClient(
                self.work_dir,
                connection=RuntimeConnection.for_inprocess(),
                github_token=DEFAULT_GITHUB_TOKEN,
                _test_options=test_options,
                base_directory=self.home_dir,
            )
        else:
            self._client = _TestClient(
                None,
                connection=RuntimeConnection.for_stdio(
                    path=self.cli_path,
                    args=tuple(cli_args or []),
                ),
                working_directory=self.work_dir,
                base_directory=self.home_dir,
                env=runtime_env,
                _test_options=test_options,
                github_token=DEFAULT_GITHUB_TOKEN,
            )

    def add_runtime_env(self, key: str, value: str) -> None:
        """Set a child runtime's environment before starting the client."""
        if self._client_inprocess:
            raise ValueError("In-process fixtures require typed client/session options")
        options = self.client._options
        if options.env is None:
            options.env = {}
        options.env[key] = value

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

        try:
            if self._proxy:
                await self._proxy.stop(skip_writing_cache=test_failed)
        finally:
            self._proxy = None
            if self.home_dir and os.path.exists(self.home_dir):
                shutil.rmtree(self.home_dir, ignore_errors=True)
            if self.work_dir and os.path.exists(self.work_dir):
                shutil.rmtree(self.work_dir, ignore_errors=True)

    async def disconnect_sessions(self) -> None:
        """Stop test-owned sessions without restarting the module-shared runtime."""
        if self._client is None:
            return
        with self._client._sessions_lock:
            sessions = list(self._client._sessions.values())
        errors: list[Exception] = []
        for session in sessions:
            try:
                # Last-owner detach awaits shutdown, including background work;
                # foreground idle alone does not establish this boundary.
                await session.disconnect()
            except Exception as error:
                errors.append(error)
        if errors:
            raise ExceptionGroup("Failed to disconnect E2E sessions", errors)

    async def configure_for_test(self, test_file: str, test_name: str):
        """
        Configure the proxy for a specific test.

        Args:
            test_file: The test file name (e.g., "session" from "test_session.py")
            test_name: The test name (e.g., "should_have_stateful_conversation")
        """
        await self.disconnect_sessions()

        sanitized_name = re.sub(r"[^a-zA-Z0-9]", "_", test_name).lower()
        snapshot_path = SNAPSHOTS_DIR / test_file / f"{sanitized_name}.yaml"
        abs_snapshot_path = str(snapshot_path.resolve())

        if self._proxy:
            await self._proxy.configure(abs_snapshot_path, self.work_dir)

        # Keep the in-process runtime's isolated home intact until teardown stops
        # the runtime. Removing its open state files on POSIX can leave later tests
        # using unlinked database state.
        cleanup_dirs = (
            (self.work_dir,) if self._client_inprocess else (self.home_dir, self.work_dir)
        )
        for base_dir in cleanup_dirs:
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
                "COPILOT_CACHE_HOME": str(Path(self.home_dir) / "cache"),
                "COPILOT_CLI_PATH": self.cli_path,
                "COPILOT_HMAC_KEY": "",
                "CAPI_HMAC_KEY": "",
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

    async def get_exchanges(self, *, timeout: float | None = None):
        """Retrieve the captured HTTP exchanges from the proxy."""
        if not self._proxy:
            raise RuntimeError("Proxy not started")
        return await self._proxy.get_exchanges(timeout=timeout)

    async def wait_for_exchanges(
        self, minimum_count: int = 1, timeout: float = 120.0
    ) -> list[dict[str, Any]]:
        """Wait until the proxy has captured at least the requested exchanges."""

        async def observe() -> list[dict[str, Any]]:
            while True:
                exchanges = await self.get_exchanges(timeout=None)
                if len(exchanges) >= minimum_count:
                    return exchanges
                await asyncio.sleep(0.1)

        observation = asyncio.create_task(observe())
        try:
            return await asyncio.wait_for(observation, timeout=timeout)
        except TimeoutError as error:
            if not observation.cancelled():
                return observation.result()
            exchanges = await self.get_exchanges(timeout=None)
            if len(exchanges) >= minimum_count:
                return exchanges
            raise TimeoutError(
                f"Timed out waiting for {minimum_count} chat completion request(s)"
            ) from error
