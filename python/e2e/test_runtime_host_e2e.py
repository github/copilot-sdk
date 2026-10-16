"""All SDKs replay the same application-owned AHP hosting conversations."""

import asyncio
import json
import os
from contextlib import asynccontextmanager

import pytest
import pytest_asyncio
from pydantic import BaseModel, Field

from copilot import (
    AhpHost,
    AhpHostOptions,
    AhpSessionCreateRequest,
    AhpSessionResumeRequest,
    CopilotClient,
    CopilotSession,
    define_tool,
)
from copilot.session import PermissionHandler
from copilot.tools import ToolInvocation

from .testharness import E2ETestContext
from .testharness.ahp import AhpTestClient
from .testharness.context import DEFAULT_GITHUB_TOKEN

pytestmark = [
    pytest.mark.asyncio(loop_scope="module"),
    pytest.mark.skipif(
        os.environ.get("COPILOT_RUNTIME_HOST_E2E") != "1",
        reason="Requires an integrated runtime; set COPILOT_RUNTIME_HOST_E2E=1",
    ),
]
TOOL_PROMPT = "Use the magic_number tool with seed 'hello' and tell me the result"
COMPOSED_PROMPT = (
    "Call magic_number with seed 'hello' and client_echo with text 'ping', then report both results"
)
MARKER = "APPLICATION_OWNED_AHP_PROMPT"


@pytest_asyncio.fixture(loop_scope="module")
async def ahp():
    async with AhpTestClient() as client:
        yield client


class Seed(BaseModel):
    seed: str = Field(description="A seed value")


class Application:
    def __init__(self, client: CopilotClient, work_dir: str):
        self.client = client
        self.work_dir = work_dir
        self.sessions: list[CopilotSession] = []
        self.releases: list[CopilotSession] = []
        self.release_event = asyncio.Event()
        self.exit_event = asyncio.Event()
        self.exits = []
        self.tool_calls: list[str] = []
        self.hook_calls: list[str] = []
        self.create_calls = 0
        self.resume_calls = 0
        self.create_ids: list[str] = []
        self.resume_ids: list[str] = []

        @define_tool("magic_number", description="Returns a magic number")
        def magic_number(params: Seed, invocation: ToolInvocation) -> str:
            assert params.seed == "hello"
            self.tool_calls.append(invocation.session_id)
            return f"MAGIC_{params.seed}_42"

        self.tool = magic_number

    async def pre_tool(self, _input, invocation):
        self.hook_calls.append(invocation["session_id"])
        return None

    def config(self, requested: dict) -> dict:
        return {
            **requested,
            "on_permission_request": PermissionHandler.approve_all,
            "system_message": {"mode": "append", "content": MARKER},
            "tools": [self.tool],
            "hooks": {"on_pre_tool_use": self.pre_tool},
        }

    async def create(self, request: AhpSessionCreateRequest) -> CopilotSession:
        self.create_calls += 1
        assert not request.cancellation_event.is_set()
        assert request.config["working_directory"] == self.work_dir
        requested_id = request.config["session_id"]
        assert requested_id
        self.create_ids.append(requested_id)
        session = await self.client.create_session(**self.config(request.config))
        assert session.session_id == requested_id
        self.sessions.append(session)
        return session

    async def resume(self, request: AhpSessionResumeRequest) -> CopilotSession:
        self.resume_calls += 1
        assert not request.cancellation_event.is_set()
        assert request.config["continue_pending_work"] is False
        assert request.config["working_directory"] == self.work_dir
        self.resume_ids.append(request.session_id)
        session = await self.client.resume_session(
            request.session_id, **self.config(request.config)
        )
        assert session.session_id == request.session_id
        self.sessions.append(session)
        return session

    def release(self, session: CopilotSession) -> None:
        self.releases.append(session)
        self.release_event.set()

    def exited(self, event) -> None:
        self.exits.append(event)
        self.exit_event.set()

    def options(self) -> AhpHostOptions:
        from copilot.rpc import HostLocalServerOptions

        return AhpHostOptions(
            local_server=HostLocalServerOptions(),
            create_session=self.create,
            resume_session=self.resume,
            on_session_released=self.release,
            on_exit=self.exited,
        )


@asynccontextmanager
async def connect(ahp: AhpTestClient, host: AhpHost, client_id: str | None = None):
    assert host.url is not None
    assert host.environment_id is None
    command = {
        "op": "connect",
        "url": host.url,
        "token": host.token,
        "githubToken": DEFAULT_GITHUB_TOKEN,
    }
    if client_id is not None:
        command["clientId"] = client_id
    connection = await ahp.request(command)
    try:
        yield connection["clientId"]
    finally:
        await ahp.request({"op": "close", "clientId": connection["clientId"]})


async def assert_tools(ctx: E2ETestContext, app: Application, session_id: str):
    assert app.tool_calls == [session_id]
    assert session_id in app.hook_calls
    exchanges = await ctx.get_exchanges()
    assert any(MARKER in json.dumps(exchange["request"]["messages"]) for exchange in exchanges)
    assert any(
        tool["function"]["name"] == "magic_number"
        for exchange in exchanges
        for tool in exchange["request"]["tools"]
    )


class TestRuntimeHost:
    async def test_creates_application_session_and_preserves_callbacks(
        self, ctx: E2ETestContext, ahp: AhpTestClient
    ):
        await ctx.configure_for_test(
            "multi_client", "both_clients_see_tool_request_and_completion_events"
        )
        app = Application(ctx.client, ctx.work_dir)
        try:
            async with await ctx.client.start_ahp_host(app.options()) as host:
                assert host.pid is None
                async with connect(ahp, host) as client_id:
                    created = await ahp.request(
                        {"op": "create", "clientId": client_id, "workDir": ctx.work_dir}
                    )
                    session_id = created["sessionId"]
                    assert app.create_calls == 1
                    runtime_id = app.sessions[0].session_id
                    assert app.create_ids == [runtime_id]
                    assert runtime_id != session_id
                    response = await ahp.request(
                        {
                            "op": "turn",
                            "clientId": client_id,
                            "sessionId": session_id,
                            "prompt": TOOL_PROMPT,
                        }
                    )
                    assert "MAGIC_hello_42" in response["text"]
                    await assert_tools(ctx, app, runtime_id)
                    await asyncio.gather(host.dispose(), host.dispose())
                    await ahp.request({"op": "stopped", "clientId": client_id, "url": host.url})
                    await asyncio.wait_for(app.release_event.wait(), 10)
                    await asyncio.wait_for(app.exit_event.wait(), 10)
                    assert len(app.releases) == 1
                    assert app.releases[0] is app.sessions[0]
                    assert len(app.exits) == 1
                    assert await app.sessions[0].get_events()
                    assert (await ctx.client.ping("still alive")).message == "pong: still alive"
        finally:
            await ctx.client.stop()

    async def test_publishes_exact_resident_session_without_factory(
        self, ctx: E2ETestContext, ahp: AhpTestClient
    ):
        await ctx.configure_for_test(
            "multi_client", "both_clients_see_tool_request_and_completion_events"
        )
        app = Application(ctx.client, ctx.work_dir)
        try:
            original = await ctx.client.create_session(
                **app.config({"working_directory": ctx.work_dir})
            )
            async with await ctx.client.start_ahp_host(app.options()) as host:
                published = await host.publish_session(original.session_id)
                assert published.session_id == original.session_id
                assert published.session_uri == f"ahp-session:/{original.session_id}"
                async with connect(ahp, host) as client_id:
                    await ahp.request(
                        {"op": "attach", "clientId": client_id, "sessionId": original.session_id}
                    )
                    response = await ahp.request(
                        {
                            "op": "turn",
                            "clientId": client_id,
                            "sessionId": original.session_id,
                            "prompt": TOOL_PROMPT,
                        }
                    )
                    assert "MAGIC_hello_42" in response["text"]
                    await assert_tools(ctx, app, original.session_id)
                    await host.dispose()
                    await ahp.request({"op": "stopped", "clientId": client_id, "url": host.url})
                    assert app.create_calls == app.resume_calls == 0
                    assert not app.releases
                    assert await original.get_events()
                    assert (await ctx.client.ping("still alive")).message == "pong: still alive"
        finally:
            await ctx.client.stop()

    async def test_resumes_after_runtime_restart_and_composes_tools(
        self, ctx: E2ETestContext, ahp: AhpTestClient
    ):
        await ctx.configure_for_test(
            "runtime_host", "app_resume_callback_composes_tools_after_history"
        )
        app = Application(ctx.client, ctx.work_dir)
        try:
            async with await ctx.client.start_ahp_host(app.options()) as host:
                async with connect(ahp, host) as client_id:
                    created = await ahp.request(
                        {
                            "op": "create",
                            "clientId": client_id,
                            "workDir": ctx.work_dir,
                            "clientTools": True,
                        }
                    )
                    session_id = created["sessionId"]
                    runtime_id = app.sessions[0].session_id
                    assert app.create_calls == 1
                    assert app.create_ids == [runtime_id]
                    assert runtime_id != session_id
                    first = await ahp.request(
                        {
                            "op": "turn",
                            "clientId": client_id,
                            "sessionId": session_id,
                            "prompt": "What is 2+2?",
                        }
                    )
                    assert "4" in first["text"]
                    await host.dispose()
                    await ahp.request({"op": "stopped", "clientId": client_id, "url": host.url})
                    await asyncio.wait_for(app.release_event.wait(), 10)
                    assert app.releases[0] is app.sessions[0]
            await ctx.client.stop()

            resumed = Application(ctx.client, ctx.work_dir)
            async with await ctx.client.start_ahp_host(resumed.options()) as replacement:
                assert replacement.pid is None
                async with connect(ahp, replacement, client_id) as resumed_client_id:
                    attached = await ahp.request(
                        {
                            "op": "attach",
                            "clientId": resumed_client_id,
                            "sessionId": session_id,
                            "clientTools": True,
                        }
                    )
                    assert [turn["message"]["text"] for turn in attached["history"]] == [
                        "What is 2+2?"
                    ]
                    assert resumed.create_calls == 0
                    assert resumed.resume_calls == 1
                    assert resumed.resume_ids == [runtime_id]
                    assert resumed.sessions[0].session_id == runtime_id
                    assert resumed.sessions[0] is not app.sessions[0]
                    response = await ahp.request(
                        {
                            "op": "turn",
                            "clientId": resumed_client_id,
                            "sessionId": session_id,
                            "prompt": COMPOSED_PROMPT,
                            "clientTools": True,
                        }
                    )
                    assert "MAGIC_hello_42" in response["text"]
                    assert "CLIENT_ECHO_ping" in response["text"]
                    assert response["clientToolCalls"] == 1
                    await assert_tools(ctx, resumed, runtime_id)
                    await replacement.dispose()
                    await ahp.request(
                        {"op": "stopped", "clientId": resumed_client_id, "url": replacement.url}
                    )
                    await asyncio.wait_for(resumed.release_event.wait(), 10)
                    assert len(resumed.releases) == 1
                    assert resumed.releases[0] is resumed.sessions[0]
                    assert await resumed.sessions[0].get_events()
        finally:
            await ctx.client.stop()
