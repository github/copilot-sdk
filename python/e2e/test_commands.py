"""E2E Commands Tests

Mirrors nodejs/test/e2e/commands.test.ts

Multi-client test: a second client joining a session with commands should
trigger a ``commands.changed`` broadcast event visible to the first client.
"""

import asyncio
import os
import shutil
import tempfile

import pytest
import pytest_asyncio

from copilot import CopilotClient
from copilot.client import ExternalServerConfig, SubprocessConfig
from copilot.session import CommandDefinition, PermissionHandler

from .testharness.context import SNAPSHOTS_DIR, get_cli_path_for_tests
from .testharness.proxy import CapiProxy

pytestmark = pytest.mark.asyncio(loop_scope="module")


# ---------------------------------------------------------------------------
# Multi-client context (TCP mode) — same pattern as test_multi_client.py
# ---------------------------------------------------------------------------


class CommandsMultiClientContext:
    """Test context that manages two clients connected to the same CLI server."""

    def __init__(self):
        self.cli_path: str = ""
        self.home_dir: str = ""
        self.work_dir: str = ""
        self.proxy_url: str = ""
        self._proxy: CapiProxy | None = None
        self._client1: CopilotClient | None = None
        self._client2: CopilotClient | None = None

    async def setup(self):
        self.cli_path = get_cli_path_for_tests()
        self.home_dir = tempfile.mkdtemp(prefix="copilot-cmd-config-")
        self.work_dir = tempfile.mkdtemp(prefix="copilot-cmd-work-")

        self._proxy = CapiProxy()
        self.proxy_url = await self._proxy.start()

        github_token = (
            "fake-token-for-e2e-tests" if os.environ.get("GITHUB_ACTIONS") == "true" else None
        )

        # Client 1 uses TCP mode so a second client can connect
        self._client1 = CopilotClient(
            SubprocessConfig(
                cli_path=self.cli_path,
                cwd=self.work_dir,
                env=self._get_env(),
                use_stdio=False,
                github_token=github_token,
            )
        )

        # Trigger connection to get the port
        init_session = await self._client1.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        await init_session.disconnect()

        actual_port = self._client1.actual_port
        assert actual_port is not None

        self._client2 = CopilotClient(ExternalServerConfig(url=f"localhost:{actual_port}"))

    async def teardown(self, test_failed: bool = False):
        for c in (self._client2, self._client1):
            if c:
                try:
                    await c.stop()
                except Exception:
                    pass  # Best-effort cleanup during teardown
        self._client1 = self._client2 = None

        if self._proxy:
            await self._proxy.stop(skip_writing_cache=test_failed)
            self._proxy = None

        for d in (self.home_dir, self.work_dir):
            if d and os.path.exists(d):
                shutil.rmtree(d, ignore_errors=True)

    async def configure_for_test(self, test_file: str, test_name: str):
        import re

        sanitized_name = re.sub(r"[^a-zA-Z0-9]", "_", test_name).lower()
        snapshot_path = SNAPSHOTS_DIR / test_file / f"{sanitized_name}.yaml"
        if self._proxy:
            await self._proxy.configure(str(snapshot_path.resolve()), self.work_dir)
        from pathlib import Path

        for d in (self.home_dir, self.work_dir):
            for item in Path(d).iterdir():
                if item.is_dir():
                    shutil.rmtree(item, ignore_errors=True)
                else:
                    item.unlink(missing_ok=True)

    def _get_env(self) -> dict:
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
    def client1(self) -> CopilotClient:
        assert self._client1 is not None
        return self._client1

    @property
    def client2(self) -> CopilotClient:
        assert self._client2 is not None
        return self._client2


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------


@pytest.hookimpl(tryfirst=True, hookwrapper=True)
def pytest_runtest_makereport(item, call):
    outcome = yield
    rep = outcome.get_result()
    if rep.when == "call" and rep.failed:
        item.session.stash.setdefault("any_test_failed", False)
        item.session.stash["any_test_failed"] = True


@pytest_asyncio.fixture(scope="module", loop_scope="module")
async def mctx(request):
    context = CommandsMultiClientContext()
    await context.setup()
    yield context
    any_failed = request.session.stash.get("any_test_failed", False)
    await context.teardown(test_failed=any_failed)


@pytest_asyncio.fixture(autouse=True, loop_scope="module")
async def configure_cmd_test(request, mctx):
    test_name = request.node.name
    if test_name.startswith("test_"):
        test_name = test_name[5:]
    await mctx.configure_for_test("multi_client", test_name)
    yield


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------


class TestCommands:
    async def test_client_receives_commands_changed_when_another_client_joins(
        self, mctx: CommandsMultiClientContext
    ):
        """Client receives commands.changed when another client joins with commands."""
        # Client 1 creates a session without commands
        session1 = await mctx.client1.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )

        # Listen for the commands.changed event
        commands_changed = asyncio.Event()
        commands_data: dict = {}

        def on_event(event):
            if event.type.value == "commands.changed":
                commands_data["commands"] = getattr(event.data, "commands", None)
                commands_changed.set()

        session1.on(on_event)

        # Client 2 joins the same session with commands
        session2 = await mctx.client2.resume_session(
            session1.session_id,
            on_permission_request=PermissionHandler.approve_all,
            commands=[
                CommandDefinition(
                    name="deploy",
                    description="Deploy the app",
                    handler=lambda ctx: None,
                ),
            ],
        )

        # Wait for the commands.changed event (with timeout)
        await asyncio.wait_for(commands_changed.wait(), timeout=15.0)

        # Verify the event contains the deploy command
        assert commands_data.get("commands") is not None
        cmd_names = [c.name for c in commands_data["commands"]]
        assert "deploy" in cmd_names

        await session2.disconnect()

