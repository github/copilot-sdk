"""E2E UI Elicitation Tests (multi-client)

Mirrors nodejs/test/e2e/ui_elicitation.test.ts — multi-client scenarios.

Tests:
  - capabilities.changed fires when second client joins with elicitation handler
  - capabilities.changed fires when elicitation provider disconnects
"""

import asyncio
import os
import shutil
import tempfile

import pytest
import pytest_asyncio

from copilot import CopilotClient
from copilot.client import ExternalServerConfig, SubprocessConfig
from copilot.session import (
    ElicitationContext,
    ElicitationResult,
    PermissionHandler,
)

from .testharness.context import SNAPSHOTS_DIR, get_cli_path_for_tests
from .testharness.proxy import CapiProxy

pytestmark = pytest.mark.asyncio(loop_scope="module")


# ---------------------------------------------------------------------------
# Multi-client context (TCP mode) — same pattern as test_multi_client.py
# ---------------------------------------------------------------------------


class ElicitationMultiClientContext:
    """Test context managing multiple clients on one CLI server."""

    def __init__(self):
        self.cli_path: str = ""
        self.home_dir: str = ""
        self.work_dir: str = ""
        self.proxy_url: str = ""
        self._proxy: CapiProxy | None = None
        self._client1: CopilotClient | None = None
        self._client2: CopilotClient | None = None
        self._actual_port: int | None = None

    async def setup(self):
        self.cli_path = get_cli_path_for_tests()
        self.home_dir = tempfile.mkdtemp(prefix="copilot-elicit-config-")
        self.work_dir = tempfile.mkdtemp(prefix="copilot-elicit-work-")

        self._proxy = CapiProxy()
        self.proxy_url = await self._proxy.start()

        github_token = (
            "fake-token-for-e2e-tests" if os.environ.get("GITHUB_ACTIONS") == "true" else None
        )

        # Client 1 uses TCP mode so additional clients can connect
        self._client1 = CopilotClient(
            SubprocessConfig(
                cli_path=self.cli_path,
                cwd=self.work_dir,
                env=self._get_env(),
                use_stdio=False,
                github_token=github_token,
            )
        )

        # Trigger connection to obtain the TCP port
        init_session = await self._client1.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        await init_session.disconnect()

        self._actual_port = self._client1.actual_port
        assert self._actual_port is not None

        self._client2 = CopilotClient(ExternalServerConfig(url=f"localhost:{self._actual_port}"))

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

    def make_external_client(self) -> CopilotClient:
        """Create a new external client connected to the same CLI server."""
        assert self._actual_port is not None
        return CopilotClient(ExternalServerConfig(url=f"localhost:{self._actual_port}"))

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
    context = ElicitationMultiClientContext()
    await context.setup()
    yield context
    any_failed = request.session.stash.get("any_test_failed", False)
    await context.teardown(test_failed=any_failed)


@pytest_asyncio.fixture(autouse=True, loop_scope="module")
async def configure_elicit_multi_test(request, mctx):
    test_name = request.node.name
    if test_name.startswith("test_"):
        test_name = test_name[5:]
    await mctx.configure_for_test("multi_client", test_name)
    yield


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------


class TestUiElicitationMultiClient:
    async def test_capabilities_changed_when_second_client_joins_with_elicitation(
        self, mctx: ElicitationMultiClientContext
    ):
        """capabilities.changed fires when second client joins with elicitation handler."""
        # Client 1 creates session without elicitation
        session1 = await mctx.client1.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        assert session1.capabilities.get("ui", {}).get("elicitation") in (False, None)

        # Listen for capabilities.changed event
        cap_changed = asyncio.Event()
        cap_event_data: dict = {}

        def on_event(event):
            if event.type.value == "capabilities.changed":
                ui = getattr(event.data, "ui", None)
                if ui:
                    cap_event_data["elicitation"] = getattr(ui, "elicitation", None)
                cap_changed.set()

        unsubscribe = session1.on(on_event)

        # Client 2 joins WITH elicitation handler — triggers capabilities.changed
        async def handler(
            context: ElicitationContext,
        ) -> ElicitationResult:
            return {"action": "accept", "content": {}}

        session2 = await mctx.client2.resume_session(
            session1.session_id,
            on_permission_request=PermissionHandler.approve_all,
            on_elicitation_request=handler,
        )

        await asyncio.wait_for(cap_changed.wait(), timeout=15.0)
        unsubscribe()

        # The event should report elicitation as True
        assert cap_event_data.get("elicitation") is True

        # Client 1's capabilities should have been auto-updated
        assert session1.capabilities.get("ui", {}).get("elicitation") is True

        await session2.disconnect()

    async def test_capabilities_changed_when_elicitation_provider_disconnects(
        self, mctx: ElicitationMultiClientContext
    ):
        """capabilities.changed fires when elicitation provider disconnects."""
        # Client 1 creates session without elicitation
        session1 = await mctx.client1.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        assert session1.capabilities.get("ui", {}).get("elicitation") in (False, None)

        # Wait for elicitation to become available
        cap_enabled = asyncio.Event()

        def on_enabled(event):
            if event.type.value == "capabilities.changed":
                ui = getattr(event.data, "ui", None)
                if ui and getattr(ui, "elicitation", None) is True:
                    cap_enabled.set()

        unsub_enabled = session1.on(on_enabled)

        # Use a dedicated client so we can stop it independently
        client3 = mctx.make_external_client()

        async def handler(
            context: ElicitationContext,
        ) -> ElicitationResult:
            return {"action": "accept", "content": {}}

        # Client 3 joins WITH elicitation handler
        await client3.resume_session(
            session1.session_id,
            on_permission_request=PermissionHandler.approve_all,
            on_elicitation_request=handler,
        )

        await asyncio.wait_for(cap_enabled.wait(), timeout=15.0)
        unsub_enabled()
        assert session1.capabilities.get("ui", {}).get("elicitation") is True

        # Now listen for the capability being removed
        cap_disabled = asyncio.Event()

        def on_disabled(event):
            if event.type.value == "capabilities.changed":
                ui = getattr(event.data, "ui", None)
                if ui and getattr(ui, "elicitation", None) is False:
                    cap_disabled.set()

        unsub_disabled = session1.on(on_disabled)

        # Force-stop client 3 — destroys the socket, triggering server-side cleanup
        await client3.force_stop()

        await asyncio.wait_for(cap_disabled.wait(), timeout=15.0)
        unsub_disabled()
        assert session1.capabilities.get("ui", {}).get("elicitation") is False
