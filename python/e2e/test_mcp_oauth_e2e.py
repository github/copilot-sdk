import asyncio
import json
import os
from pathlib import Path
from typing import Any

import httpx
import pytest

from copilot.generated.rpc import (
    MCPAppsCallToolRequest,
    MCPListToolsRequest,
    MCPOauthHandlePendingRequest,
    MCPOauthPendingRequestResponse,
    MCPOauthPendingRequestResponseKind,
)
from copilot.session import MCPServerConfig, PermissionHandler
from copilot.session_events import McpServerStatus

from .testharness import E2ETestContext, wait_for_condition

TEST_MCP_OAUTH_SERVER = str(
    (Path(__file__).parents[2] / "test" / "harness" / "test-mcp-oauth-server.mjs").resolve()
)
EXPECTED_TOKEN = "sdk-host-token"
REFRESH_TOKEN = f"{EXPECTED_TOKEN}-refresh"
UPSCOPE_TOKEN = f"{EXPECTED_TOKEN}-upscope"
REAUTH_TOKEN = f"{EXPECTED_TOKEN}-reauth"

pytestmark = pytest.mark.asyncio(loop_scope="module")


async def _start_oauth_mcp_server() -> tuple[str, asyncio.subprocess.Process]:
    process = await asyncio.create_subprocess_exec(
        "node",
        TEST_MCP_OAUTH_SERVER,
        stdout=asyncio.subprocess.PIPE,
        stderr=asyncio.subprocess.PIPE,
        env={**os.environ, "EXPECTED_TOKEN": EXPECTED_TOKEN},
    )
    assert process.stdout is not None

    try:
        line = await asyncio.wait_for(process.stdout.readline(), timeout=10)
    except TimeoutError as exc:
        await _stop_process(process)
        assert process.stderr is not None
        stderr = (await process.stderr.read()).decode(errors="replace")
        raise TimeoutError(f"Timed out waiting for OAuth MCP server: {stderr}") from exc
    if not line:
        assert process.stderr is not None
        stderr = (await process.stderr.read()).decode(errors="replace")
        raise RuntimeError(f"OAuth MCP server exited before listening: {stderr}")
    text = line.decode().strip()
    if text.startswith("Listening: "):
        return text.removeprefix("Listening: "), process

    await _stop_process(process)
    raise RuntimeError(f"Unexpected OAuth MCP server startup line: {text}")


async def _stop_process(process: asyncio.subprocess.Process) -> None:
    if process.returncode is not None:
        return
    process.terminate()
    try:
        await asyncio.wait_for(process.wait(), timeout=5)
    except TimeoutError:
        process.kill()
        await process.wait()


async def _requests(base_url: str) -> list[dict[str, Any]]:
    async with httpx.AsyncClient() as client:
        response = await client.get(f"{base_url}/__requests")
        response.raise_for_status()
        return response.json()


async def _wait_for_mcp_server_status(
    session, server_name: str, expected_status: McpServerStatus = McpServerStatus.CONNECTED
) -> None:
    last_status = "<not listed>"

    async def matches() -> bool:
        nonlocal last_status
        result = await session.rpc.mcp.list()
        server = next((s for s in result.servers if s.name == server_name), None)
        last_status = server.status.value if server is not None else "<not listed>"
        return server is not None and server.status == expected_status

    await wait_for_condition(
        matches,
        timeout=60.0,
        poll_interval=0.2,
        timeout_message=(
            f"{server_name} did not reach {expected_status.value}; last status was {last_status}"
        ),
    )


class TestMcpOAuth:
    async def test_should_satisfy_mcp_oauth_using_host_provided_token(self, ctx: E2ETestContext):
        url, process = await _start_oauth_mcp_server()
        server_name = "oauth-protected-mcp"
        observed_request = None

        def on_mcp_auth_request(request, _invocation):
            nonlocal observed_request
            observed_request = request
            return {
                "kind": "token",
                "accessToken": EXPECTED_TOKEN,
                "tokenType": "Bearer",
                "expiresIn": 3600,
            }

        try:
            mcp_servers: dict[str, MCPServerConfig] = {
                server_name: {
                    "type": "http",
                    "url": f"{url}/mcp",
                    "tools": ["*"],
                }
            }
            async with await ctx.client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                on_mcp_auth_request=on_mcp_auth_request,
                mcp_servers=mcp_servers,
            ) as session:
                await _wait_for_mcp_server_status(session, server_name)

                tools = await session.rpc.mcp.list_tools(
                    MCPListToolsRequest(server_name=server_name)
                )
                assert [tool.name for tool in tools.tools] == ["whoami"]

            assert observed_request is not None
            assert observed_request["serverName"] == server_name
            assert observed_request["serverUrl"] == f"{url}/mcp"
            assert observed_request["reason"] == "initial"
            assert observed_request["wwwAuthenticateParams"] == {
                "resourceMetadataUrl": f"{url}/.well-known/oauth-protected-resource",
                "scope": "mcp.read",
                "error": "invalid_token",
            }
            assert json.loads(observed_request["resourceMetadata"]) == {
                "resource": f"{url}/mcp",
                "authorization_servers": [url],
                "scopes_supported": ["mcp.read"],
                "bearer_methods_supported": ["header"],
            }

            requests = await _requests(url)
            assert any(request["authorization"] is None for request in requests)
            assert any(
                request["authorization"] == f"Bearer {EXPECTED_TOKEN}" for request in requests
            )
        finally:
            await _stop_process(process)

    async def test_should_resolve_pending_mcp_oauth_request_with_direct_rpc(
        self, ctx: E2ETestContext
    ):
        url, process = await _start_oauth_mcp_server()
        server_name = "oauth-direct-rpc-mcp"
        loop = asyncio.get_running_loop()
        observed_request = loop.create_future()
        release_handler = asyncio.Event()

        async def on_mcp_auth_request(request, _invocation):
            if not observed_request.done():
                observed_request.set_result(request)
            await release_handler.wait()
            return {"kind": "token", "accessToken": EXPECTED_TOKEN}

        try:
            mcp_servers: dict[str, MCPServerConfig] = {
                server_name: {
                    "type": "http",
                    "url": f"{url}/mcp",
                    "tools": ["*"],
                    "oauthClientId": "sdk-e2e-client",
                    "oauthPublicClient": True,
                }
            }
            async with await ctx.client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                on_mcp_auth_request=on_mcp_auth_request,
                mcp_servers=mcp_servers,
                enable_mcp_apps=True,
            ) as session:
                connected = asyncio.create_task(_wait_for_mcp_server_status(session, server_name))
                try:
                    request = await asyncio.wait_for(observed_request, timeout=30.0)
                    assert request["serverName"] == server_name
                    assert request["serverUrl"] == f"{url}/mcp"
                    assert request["reason"] == "initial"
                    assert request["wwwAuthenticateParams"] == {
                        "resourceMetadataUrl": f"{url}/.well-known/oauth-protected-resource",
                        "scope": "mcp.read",
                        "error": "invalid_token",
                    }

                    handled = await session.rpc.mcp.oauth.handle_pending_request(
                        MCPOauthHandlePendingRequest(
                            request_id=request["requestId"],
                            result=MCPOauthPendingRequestResponse(
                                kind=MCPOauthPendingRequestResponseKind.TOKEN,
                                access_token=EXPECTED_TOKEN,
                                token_type="Bearer",
                                expires_in=3600,
                            ),
                        )
                    )
                    assert handled.success is True

                    connected_result = await asyncio.wait_for(connected, timeout=60.0)
                    assert connected_result is None
                    tools = await session.rpc.mcp.list_tools(
                        MCPListToolsRequest(server_name=server_name)
                    )
                    assert [tool.name for tool in tools.tools] == ["whoami"]
                finally:
                    release_handler.set()
                    if not connected.done():
                        connected.cancel()
        finally:
            await _stop_process(process)

    async def test_should_request_replacement_tokens_across_mcp_oauth_lifecycle(
        self, ctx: E2ETestContext
    ):
        url, process = await _start_oauth_mcp_server()
        server_name = "oauth-lifecycle-mcp"
        observed_requests: list[dict[str, Any]] = []
        refresh_count = 0

        def on_mcp_auth_request(request, _invocation):
            nonlocal refresh_count
            observed_requests.append(request)
            if request["reason"] == "refresh":
                refresh_count += 1
                assert request["wwwAuthenticateParams"] == {"error": "invalid_token"}
                if refresh_count > 1:
                    return {"kind": "cancelled"}
                return {"kind": "token", "accessToken": REFRESH_TOKEN}
            if request["reason"] == "upscope":
                assert request["wwwAuthenticateParams"] == {
                    "resourceMetadataUrl": f"{url}/.well-known/oauth-protected-resource",
                    "scope": "mcp.write",
                    "error": "insufficient_scope",
                }
                return {"kind": "token", "accessToken": UPSCOPE_TOKEN}
            if request["reason"] == "reauth":
                return {"kind": "token", "accessToken": REAUTH_TOKEN}
            return {"kind": "token", "accessToken": EXPECTED_TOKEN}

        try:
            mcp_servers: dict[str, MCPServerConfig] = {
                server_name: {
                    "type": "http",
                    "url": f"{url}/mcp",
                    "tools": ["*"],
                }
            }
            async with await ctx.client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                on_mcp_auth_request=on_mcp_auth_request,
                mcp_servers=mcp_servers,
                enable_mcp_apps=True,
            ) as session:
                await _wait_for_mcp_server_status(session, server_name)

                for scenario in ("refresh", "upscope", "reauth"):
                    result = await session.rpc.mcp.apps.call_tool(
                        MCPAppsCallToolRequest(
                            origin_server_name=server_name,
                            server_name=server_name,
                            tool_name="whoami",
                            arguments={"scenario": scenario},
                        )
                    )
                    assert result["content"] == [{"type": "text", "text": "oauth-test-user"}]

            assert [request["reason"] for request in observed_requests] == [
                "initial",
                "refresh",
                "upscope",
                "refresh",
                "reauth",
            ]
            requests = await _requests(url)
            assert any(
                request["authorization"] == f"Bearer {REFRESH_TOKEN}" for request in requests
            )
            assert any(
                request["authorization"] == f"Bearer {UPSCOPE_TOKEN}" for request in requests
            )
            assert any(request["authorization"] == f"Bearer {REAUTH_TOKEN}" for request in requests)
        finally:
            await _stop_process(process)

    async def test_should_cancel_pending_mcp_oauth_request(self, ctx: E2ETestContext):
        url, process = await _start_oauth_mcp_server()
        server_name = "oauth-cancelled-mcp"
        observed_request = None

        def on_mcp_auth_request(request, _invocation):
            nonlocal observed_request
            observed_request = request
            return {"kind": "cancelled"}

        try:
            mcp_servers: dict[str, MCPServerConfig] = {
                server_name: {
                    "type": "http",
                    "url": f"{url}/mcp",
                    "tools": ["*"],
                }
            }
            async with await ctx.client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                on_mcp_auth_request=on_mcp_auth_request,
                mcp_servers=mcp_servers,
            ) as session:
                await _wait_for_mcp_server_status(session, server_name, McpServerStatus.NEEDS_AUTH)

                # The MCP connection is kicked off by session.create, but the SDK only registers
                # its `mcp.oauth_required` event interest once create returns. If the server's
                # initial 401 wins that race, the runtime records `needs-auth` WITHOUT invoking
                # the host callback, so `observed_request` is briefly None even after `needs-auth`
                # is observed. A later auth retry (now that interest is registered) invokes the
                # callback with the same `initial` reason. Wait for the callback rather than
                # sampling it the instant `needs-auth` first appears, which made this test flaky.
                await wait_for_condition(
                    lambda: observed_request is not None,
                    timeout=60.0,
                    poll_interval=0.2,
                    timeout_message=f"{server_name} OAuth request did not reach the host callback",
                )

            assert observed_request is not None
            assert observed_request["serverName"] == server_name
            assert observed_request["reason"] == "initial"
        finally:
            await _stop_process(process)
