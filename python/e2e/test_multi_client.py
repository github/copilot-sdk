"""E2E Multi-Client Broadcast Tests

Tests that verify the protocol v3 broadcast model works correctly when
multiple clients are connected to the same CLI server session.
"""

import asyncio
import os
import shutil
import tempfile

import pytest
import pytest_asyncio
from pydantic import BaseModel, Field

from copilot import CopilotClient, define_tool
from copilot.client import ExternalServerConfig, SubprocessConfig
from copilot.session import PermissionHandler, PermissionRequestResult
from copilot.tools import ToolInvocation

from .testharness import get_final_assistant_message
from .testharness.proxy import CapiProxy

pytestmark = pytest.mark.asyncio(loop_scope="module")


class MultiClientContext:
    """Extended test context that manages two clients connected to the same CLI server."""

    def __init__(self):
        self.cli_path: str = ""
        self.home_dir: str = ""
        self.work_dir: str = ""
        self.proxy_url: str = ""
        self._proxy: CapiProxy | None = None
        self._client1: CopilotClient | None = None
        self._client2: CopilotClient | None = None

    async def setup(self):
        from .testharness.context import get_cli_path_for_tests

        self.cli_path = get_cli_path_for_tests()
        self.home_dir = tempfile.mkdtemp(prefix="copilot-multi-config-")
        self.work_dir = tempfile.mkdtemp(prefix="copilot-multi-work-")

        self._proxy = CapiProxy()
        self.proxy_url = await self._proxy.start()

        github_token = (
            "fake-token-for-e2e-tests" if os.environ.get("GITHUB_ACTIONS") == "true" else None
        )

        # Client 1 uses TCP mode so a second client can connect to the same server
        self._client1 = CopilotClient(
            SubprocessConfig(
                cli_path=self.cli_path,
                cwd=self.work_dir,
                env=self.get_env(),
                use_stdio=False,
                github_token=github_token,
            )
        )

        # Trigger connection by creating and disconnecting an init session
        init_session = await self._client1.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        await init_session.disconnect()

        # Read the actual port from client 1 and create client 2
        actual_port = self._client1.actual_port
        assert actual_port is not None, "Client 1 should have an actual port after connecting"

        self._client2 = CopilotClient(ExternalServerConfig(url=f"localhost:{actual_port}"))

    async def teardown(self, test_failed: bool = False):
        if self._client2:
            try:
                await self._client2.stop()
            except Exception:
                pass
            self._client2 = None

        if self._client1:
            try:
                await self._client1.stop()
            except Exception:
                pass
            self._client1 = None

        if self._proxy:
            await self._proxy.stop(skip_writing_cache=test_failed)
            self._proxy = None

        if self.home_dir and os.path.exists(self.home_dir):
            shutil.rmtree(self.home_dir, ignore_errors=True)
        if self.work_dir and os.path.exists(self.work_dir):
            shutil.rmtree(self.work_dir, ignore_errors=True)

    async def configure_for_test(self, test_file: str, test_name: str):
        import re

        sanitized_name = re.sub(r"[^a-zA-Z0-9]", "_", test_name).lower()
        # Use the same snapshot directory structure as the standard context
        from .testharness.context import SNAPSHOTS_DIR

        snapshot_path = SNAPSHOTS_DIR / test_file / f"{sanitized_name}.yaml"
        abs_snapshot_path = str(snapshot_path.resolve())

        if self._proxy:
            await self._proxy.configure(abs_snapshot_path, self.work_dir)

        # Clear temp directories between tests
        from pathlib import Path

        for item in Path(self.home_dir).iterdir():
            if item.is_dir():
                shutil.rmtree(item, ignore_errors=True)
            else:
                item.unlink(missing_ok=True)
        for item in Path(self.work_dir).iterdir():
            if item.is_dir():
                shutil.rmtree(item, ignore_errors=True)
            else:
                item.unlink(missing_ok=True)

    def get_env(self) -> dict:
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
        if not self._client1:
            raise RuntimeError("Context not set up")
        return self._client1

    @property
    def client2(self) -> CopilotClient:
        if not self._client2:
            raise RuntimeError("Context not set up")
        return self._client2


@pytest.hookimpl(tryfirst=True, hookwrapper=True)
def pytest_runtest_makereport(item, call):
    outcome = yield
    rep = outcome.get_result()
    if rep.when == "call" and rep.failed:
        item.session.stash.setdefault("any_test_failed", False)
        item.session.stash["any_test_failed"] = True


@pytest_asyncio.fixture(scope="module", loop_scope="module")
async def mctx(request):
    """Multi-client test context fixture."""
    context = MultiClientContext()
    await context.setup()
    yield context
    any_failed = request.session.stash.get("any_test_failed", False)
    await context.teardown(test_failed=any_failed)


@pytest_asyncio.fixture(autouse=True, loop_scope="module")
async def configure_multi_test(request, mctx):
    """Automatically configure the proxy for each test."""
    module_name = request.module.__name__.split(".")[-1]
    test_file = module_name[5:] if module_name.startswith("test_") else module_name
    test_name = request.node.name
    if test_name.startswith("test_"):
        test_name = test_name[5:]
    await mctx.configure_for_test(test_file, test_name)
    yield


class TestMultiClientBroadcast:
    async def test_both_clients_see_tool_request_and_completion_events(
        self, mctx: MultiClientContext
    ):
        """Both clients see tool request and completion events."""

        class SeedParams(BaseModel):
            seed: str = Field(description="A seed value")

        @define_tool("magic_number", description="Returns a magic number")
        def magic_number(params: SeedParams, invocation: ToolInvocation) -> str:
            return f"MAGIC_{params.seed}_42"

        # Client 1 creates a session with a custom tool
        session1 = await mctx.client1.create_session(
            on_permission_request=PermissionHandler.approve_all, tools=[magic_number]
        )

        # Client 2 resumes with NO tools — should not overwrite client 1's tools
        session2 = await mctx.client2.resume_session(
            session1.session_id, on_permission_request=PermissionHandler.approve_all
        )
        client1_events = []
        client2_events = []
        session1.on(lambda event: client1_events.append(event))
        session2.on(lambda event: client2_events.append(event))

        # Send a prompt that triggers the custom tool
        await session1.send("Use the magic_number tool with seed 'hello' and tell me the result")
        response = await get_final_assistant_message(session1)
        assert "MAGIC_hello_42" in (response.data.content or "")

        # Both clients should have seen the external_tool.requested event
        c1_tool_requested = [e for e in client1_events if e.type.value == "external_tool.requested"]
        c2_tool_requested = [e for e in client2_events if e.type.value == "external_tool.requested"]
        assert len(c1_tool_requested) > 0
        assert len(c2_tool_requested) > 0

        # Both clients should have seen the external_tool.completed event
        c1_tool_completed = [e for e in client1_events if e.type.value == "external_tool.completed"]
        c2_tool_completed = [e for e in client2_events if e.type.value == "external_tool.completed"]
        assert len(c1_tool_completed) > 0
        assert len(c2_tool_completed) > 0

        await session2.disconnect()

    async def test_one_client_approves_permission_and_both_see_the_result(
        self, mctx: MultiClientContext
    ):
        """One client approves a permission request and both see the result."""
        permission_requests = []

        # Client 1 creates a session and manually approves permission requests
        session1 = await mctx.client1.create_session(
            on_permission_request=lambda request, invocation: (
                permission_requests.append(request) or PermissionRequestResult(kind="approved")
            ),
        )

        # Client 2 resumes — its handler never resolves, so only client 1's approval takes effect
        session2 = await mctx.client2.resume_session(
            session1.session_id,
            on_permission_request=lambda request, invocation: asyncio.Future(),
        )

        client1_events = []
        client2_events = []
        session1.on(lambda event: client1_events.append(event))
        session2.on(lambda event: client2_events.append(event))

        # Send a prompt that triggers a write operation (requires permission)
        await session1.send("Create a file called hello.txt containing the text 'hello world'")
        response = await get_final_assistant_message(session1)
        assert response.data.content

        # Client 1 should have handled permission requests
        assert len(permission_requests) > 0

        # Both clients should have seen permission.requested events
        c1_perm_requested = [e for e in client1_events if e.type.value == "permission.requested"]
        c2_perm_requested = [e for e in client2_events if e.type.value == "permission.requested"]
        assert len(c1_perm_requested) > 0
        assert len(c2_perm_requested) > 0

        # Both clients should have seen permission.completed events with approved result
        c1_perm_completed = [e for e in client1_events if e.type.value == "permission.completed"]
        c2_perm_completed = [e for e in client2_events if e.type.value == "permission.completed"]
        assert len(c1_perm_completed) > 0
        assert len(c2_perm_completed) > 0
        for event in c1_perm_completed + c2_perm_completed:
            assert event.data.result.kind.value == "approved"

        await session2.disconnect()

    async def test_one_client_rejects_permission_and_both_see_the_result(
        self, mctx: MultiClientContext
    ):
        """One client rejects a permission request and both see the result."""
        # Client 1 creates a session and denies all permission requests
        session1 = await mctx.client1.create_session(
            on_permission_request=lambda request, invocation: PermissionRequestResult(
                kind="denied-interactively-by-user"
            ),
        )

        # Client 2 resumes — its handler never resolves
        session2 = await mctx.client2.resume_session(
            session1.session_id,
            on_permission_request=lambda request, invocation: asyncio.Future(),
        )

        client1_events = []
        client2_events = []
        session1.on(lambda event: client1_events.append(event))
        session2.on(lambda event: client2_events.append(event))

        # Create a file that the agent will try to edit
        test_file = os.path.join(mctx.work_dir, "protected.txt")
        with open(test_file, "w") as f:
            f.write("protected content")

        await session1.send("Edit protected.txt and replace 'protected' with 'hacked'.")
        await get_final_assistant_message(session1)

        # Verify the file was NOT modified (permission was denied)
        with open(test_file) as f:
            content = f.read()
        assert content == "protected content"

        # Both clients should have seen permission.requested and permission.completed
        c1_perm_requested = [e for e in client1_events if e.type.value == "permission.requested"]
        c2_perm_requested = [e for e in client2_events if e.type.value == "permission.requested"]
        assert len(c1_perm_requested) > 0
        assert len(c2_perm_requested) > 0

        # Both clients should see the denial
        c1_perm_completed = [e for e in client1_events if e.type.value == "permission.completed"]
        c2_perm_completed = [e for e in client2_events if e.type.value == "permission.completed"]
        assert len(c1_perm_completed) > 0
        assert len(c2_perm_completed) > 0
        for event in c1_perm_completed + c2_perm_completed:
            assert event.data.result.kind.value == "denied-interactively-by-user"

        await session2.disconnect()

    @pytest.mark.timeout(90)
    async def test_two_clients_register_different_tools_and_agent_uses_both(
        self, mctx: MultiClientContext
    ):
        """Two clients register different tools and agent uses both."""

        class CountryCodeParams(BaseModel):
            model_config = {"populate_by_name": True}
            country_code: str = Field(alias="countryCode", description="A two-letter country code")

        @define_tool("city_lookup", description="Returns a city name for a given country code")
        def city_lookup(params: CountryCodeParams, invocation: ToolInvocation) -> str:
            return f"CITY_FOR_{params.country_code}"

        @define_tool("currency_lookup", description="Returns a currency for a given country code")
        def currency_lookup(params: CountryCodeParams, invocation: ToolInvocation) -> str:
            return f"CURRENCY_FOR_{params.country_code}"

        # Client 1 creates a session with tool A
        session1 = await mctx.client1.create_session(
            on_permission_request=PermissionHandler.approve_all, tools=[city_lookup]
        )

        # Client 2 resumes with tool B (different tool, union should have both)
        session2 = await mctx.client2.resume_session(
            session1.session_id,
            on_permission_request=PermissionHandler.approve_all,
            tools=[currency_lookup],
        )

        # Send prompts sequentially to avoid nondeterministic tool_call ordering
        await session1.send(
            "Use the city_lookup tool with countryCode 'US' and tell me the result."
        )
        response1 = await get_final_assistant_message(session1)
        assert "CITY_FOR_US" in (response1.data.content or "")

        await session1.send(
            "Now use the currency_lookup tool with countryCode 'US' and tell me the result."
        )
        response2 = await get_final_assistant_message(session1)
        assert "CURRENCY_FOR_US" in (response2.data.content or "")

        await session2.disconnect()

    @pytest.mark.timeout(90)
    @pytest.mark.skip(
        reason="Flaky on CI: Python TCP socket close detection is too slow for snapshot replay"
    )
    async def test_disconnecting_client_removes_its_tools(self, mctx: MultiClientContext):
        """Disconnecting a client removes its tools from the session."""

        class InputParams(BaseModel):
            input: str = Field(description="Input value")

        @define_tool("stable_tool", description="A tool that persists across disconnects")
        def stable_tool(params: InputParams, invocation: ToolInvocation) -> str:
            return f"STABLE_{params.input}"

        @define_tool(
            "ephemeral_tool",
            description="A tool that will disappear when its client disconnects",
        )
        def ephemeral_tool(params: InputParams, invocation: ToolInvocation) -> str:
            return f"EPHEMERAL_{params.input}"

        # Client 1 creates a session with stable_tool
        session1 = await mctx.client1.create_session(
            on_permission_request=PermissionHandler.approve_all, tools=[stable_tool]
        )

        # Client 2 resumes with ephemeral_tool
        await mctx.client2.resume_session(
            session1.session_id,
            on_permission_request=PermissionHandler.approve_all,
            tools=[ephemeral_tool],
        )

        # Verify both tools work before disconnect.
        # Sequential prompts avoid nondeterministic tool_call ordering.
        await session1.send("Use the stable_tool with input 'test1' and tell me the result.")
        stable_response = await get_final_assistant_message(session1)
        assert "STABLE_test1" in (stable_response.data.content or "")

        await session1.send("Use the ephemeral_tool with input 'test2' and tell me the result.")
        ephemeral_response = await get_final_assistant_message(session1)
        assert "EPHEMERAL_test2" in (ephemeral_response.data.content or "")

        # Force disconnect client 2 without destroying the shared session
        await mctx.client2.force_stop()

        # Give the server time to process the connection close and remove tools
        await asyncio.sleep(0.5)

        # Recreate client2 for future tests (but don't rejoin the session)
        actual_port = mctx.client1.actual_port
        mctx._client2 = CopilotClient(ExternalServerConfig(url=f"localhost:{actual_port}"))

        # Now only stable_tool should be available
        await session1.send(
            "Use the stable_tool with input 'still_here'."
            " Also try using ephemeral_tool"
            " if it is available."
        )
        after_response = await get_final_assistant_message(session1)
        assert "STABLE_still_here" in (after_response.data.content or "")
        # ephemeral_tool should NOT have produced a result
        assert "EPHEMERAL_" not in (after_response.data.content or "")
