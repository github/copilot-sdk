"""
E2E coverage for session-scoped MCP lifecycle RPC methods.

Mirrors ``dotnet/test/E2E/RpcMcpLifecycleE2ETests.cs`` (snapshot category
``rpc_mcp_lifecycle``).
"""

from __future__ import annotations

import uuid
from pathlib import Path

import pytest

from copilot.rpc import (
    MCPConfigureGitHubRequest,
    MCPIsServerRunningRequest,
    MCPListToolsRequest,
    MCPOauthRespondRequest,
    MCPRegisterExternalClientRequest,
    MCPReloadWithConfigRequest,
    MCPRestartServerRequest,
    MCPStartServerRequest,
    MCPStopServerRequest,
    MCPUnregisterExternalClientRequest,
)
from copilot.session import PermissionHandler
from copilot.session_events import McpServerStatus

from .testharness import E2ETestContext, wait_for_condition

pytestmark = pytest.mark.asyncio(loop_scope="module")

TEST_MCP_SERVER = str(
    (Path(__file__).parents[2] / "test" / "harness" / "test-mcp-server.mjs").resolve()
)
TEST_HARNESS_DIR = str((Path(__file__).parents[2] / "test" / "harness").resolve())


def _test_mcp_servers(*server_names: str) -> dict[str, dict]:
    return {
        server_name: {
            "command": "node",
            "args": [TEST_MCP_SERVER],
            "tools": ["*"],
            "working_directory": TEST_HARNESS_DIR,
        }
        for server_name in server_names
    }


def _wire_mcp_server_config() -> dict:
    return {
        "command": "node",
        "args": [TEST_MCP_SERVER],
        "tools": ["*"],
        "cwd": TEST_HARNESS_DIR,
    }


async def _wait_for_mcp_server_status(
    session,
    server_name: str,
    expected_status: McpServerStatus = McpServerStatus.CONNECTED,
) -> None:
    last_status = "<not listed>"

    async def connected() -> bool:
        nonlocal last_status
        result = await session.rpc.mcp.list()
        server = next((s for s in result.servers if s.name == server_name), None)
        if server is not None:
            last_status = server.status
        if server is None:
            last_status = "<not listed>"
            return False
        return server.status == expected_status

    await wait_for_condition(
        connected,
        timeout=60.0,
        poll_interval=0.2,
        timeout_message=(
            f"{server_name} did not reach {expected_status.value}; last status was {last_status}"
        ),
    )


async def _wait_for_mcp_running(session, server_name: str, expected_running: bool) -> None:
    async def matches() -> bool:
        result = await session.rpc.mcp.is_server_running(
            MCPIsServerRunningRequest(server_name=server_name)
        )
        return result.running is expected_running

    await wait_for_condition(
        matches,
        timeout=60.0,
        poll_interval=0.2,
        timeout_message=f"{server_name} running={expected_running}",
    )


def _assert_not_unhandled_method(message: str) -> None:
    assert "Unhandled method".lower() not in message.lower()


class TestRpcMcpLifecycle:
    async def test_should_list_tools_and_report_running_status_for_connected_server(
        self, ctx: E2ETestContext
    ):
        server_name = "rpc-lifecycle-list-server"
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            mcp_servers=_test_mcp_servers(server_name),
        ) as session:
            await _wait_for_mcp_server_status(session, server_name)

            tools = await session.rpc.mcp.list_tools(MCPListToolsRequest(server_name=server_name))
            assert tools.tools is not None
            assert len(tools.tools) > 0
            assert all((tool.name or "").strip() for tool in tools.tools)

            running = await session.rpc.mcp.is_server_running(
                MCPIsServerRunningRequest(server_name=server_name)
            )
            assert running.running is True

            missing = await session.rpc.mcp.is_server_running(
                MCPIsServerRunningRequest(server_name=f"missing-{uuid.uuid4().hex}")
            )
            assert missing.running is False

    async def test_should_throw_when_listing_tools_for_unconnected_server(
        self, ctx: E2ETestContext
    ):
        server_name = "rpc-lifecycle-unconnected-host"
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            mcp_servers=_test_mcp_servers(server_name),
        ) as session:
            await _wait_for_mcp_server_status(session, server_name)

            with pytest.raises(Exception) as excinfo:
                await session.rpc.mcp.list_tools(
                    MCPListToolsRequest(server_name=f"missing-{uuid.uuid4().hex}")
                )
            message = str(excinfo.value)
            _assert_not_unhandled_method(message)
            assert "not connected" in message.lower()

    async def test_should_stop_running_mcp_server(self, ctx: E2ETestContext):
        server_name = "rpc-lifecycle-stop-server"
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            mcp_servers=_test_mcp_servers(server_name),
        ) as session:
            await _wait_for_mcp_server_status(session, server_name)
            assert (
                await session.rpc.mcp.is_server_running(
                    MCPIsServerRunningRequest(server_name=server_name)
                )
            ).running is True

            await session.rpc.mcp.stop_server(MCPStopServerRequest(server_name=server_name))

            await _wait_for_mcp_running(session, server_name, expected_running=False)

    async def test_should_start_and_restart_mcp_server(self, ctx: E2ETestContext):
        host_server = "rpc-lifecycle-host-server"
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            mcp_servers=_test_mcp_servers(host_server),
        ) as session:
            await _wait_for_mcp_server_status(session, host_server)

            started_server = "rpc-lifecycle-started-server"
            config = _wire_mcp_server_config()

            await session.rpc.mcp.start_server(
                MCPStartServerRequest(server_name=started_server, config=config)
            )
            await _wait_for_mcp_running(session, started_server, expected_running=True)

            tools = await session.rpc.mcp.list_tools(
                MCPListToolsRequest(server_name=started_server)
            )
            assert len(tools.tools) > 0

            await session.rpc.mcp.restart_server(
                MCPRestartServerRequest(server_name=started_server, config=config)
            )
            await _wait_for_mcp_running(session, started_server, expected_running=True)

    async def test_should_register_and_unregister_external_mcp_client(self, ctx: E2ETestContext):
        host_server = "rpc-lifecycle-extclient-host"
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            mcp_servers=_test_mcp_servers(host_server),
        ) as session:
            await _wait_for_mcp_server_status(session, host_server)

            external_name = "rpc-lifecycle-external-client"
            assert (
                await session.rpc.mcp.is_server_running(
                    MCPIsServerRunningRequest(server_name=external_name)
                )
            ).running is False

            await session.rpc.mcp.register_external_client(
                MCPRegisterExternalClientRequest(
                    server_name=external_name,
                    client={"id": external_name},
                    transport={"kind": "in-process"},
                    config={"command": "noop"},
                )
            )
            assert (
                await session.rpc.mcp.is_server_running(
                    MCPIsServerRunningRequest(server_name=external_name)
                )
            ).running is True

            await session.rpc.mcp.unregister_external_client(
                MCPUnregisterExternalClientRequest(server_name=external_name)
            )
            assert (
                await session.rpc.mcp.is_server_running(
                    MCPIsServerRunningRequest(server_name=external_name)
                )
            ).running is False

    async def test_should_reload_mcp_servers_with_config(self, ctx: E2ETestContext):
        host_server = "rpc-lifecycle-reload-host"
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            mcp_servers=_test_mcp_servers(host_server),
        ) as session:
            await _wait_for_mcp_server_status(session, host_server)

            result = await session.rpc.mcp.reload_with_config(
                MCPReloadWithConfigRequest(
                    config={"mcpServers": {}, "disabledServers": []},
                )
            )

            assert result is not None
            assert result.filtered_servers is not None
            assert result.filtered_servers == []

    async def test_should_configure_github_mcp_server(self, ctx: E2ETestContext):
        host_server = "rpc-lifecycle-configure-host"
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            mcp_servers=_test_mcp_servers(host_server),
        ) as session:
            await _wait_for_mcp_server_status(session, host_server)

            result = await session.rpc.mcp.configure_git_hub(
                MCPConfigureGitHubRequest(auth_info={"type": "api-key"})
            )

            assert result is not None
            assert result.changed is False

    async def test_should_respond_to_mcp_oauth_request_without_pending_request(
        self, ctx: E2ETestContext
    ):
        host_server = "rpc-lifecycle-oauth-host"
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            mcp_servers=_test_mcp_servers(host_server),
        ) as session:
            await _wait_for_mcp_server_status(session, host_server)

            result = await session.rpc.mcp.oauth.respond(
                MCPOauthRespondRequest(request_id=f"missing-{uuid.uuid4().hex}")
            )
            assert result is not None
