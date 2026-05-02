"""
E2E coverage for ``mcp.config.*`` server-scoped RPCs.

Mirrors ``dotnet/test/RpcMcpConfigTests.cs`` (snapshot category
``rpc_mcp_config``).
"""

from __future__ import annotations

import uuid

import pytest

from copilot.generated.rpc import (
    MCPConfigAddRequest,
    MCPConfigDisableRequest,
    MCPConfigEnableRequest,
    MCPConfigRemoveRequest,
    MCPConfigUpdateRequest,
    MCPServerConfig,
    MCPServerConfigHTTPOauthGrantType,
    MCPServerConfigType,
)

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


def _server_config(servers: dict, name: str) -> MCPServerConfig:
    assert name in servers, f"Expected MCP server '{name}' to be present."
    return servers[name]


class TestRpcMcpConfig:
    async def test_should_call_server_mcp_config_rpcs(self, ctx: E2ETestContext):
        await ctx.client.start()

        server_name = f"sdk-test-{uuid.uuid4().hex}"
        config = MCPServerConfig(command="node", args=[])
        updated_config = MCPServerConfig(command="node", args=["--version"])

        initial = await ctx.client.rpc.mcp.config.list()
        assert server_name not in initial.servers

        try:
            await ctx.client.rpc.mcp.config.add(
                MCPConfigAddRequest(name=server_name, config=config)
            )
            after_add = await ctx.client.rpc.mcp.config.list()
            assert server_name in after_add.servers

            await ctx.client.rpc.mcp.config.update(
                MCPConfigUpdateRequest(name=server_name, config=updated_config)
            )
            after_update = await ctx.client.rpc.mcp.config.list()
            updated = _server_config(after_update.servers, server_name)
            assert updated.command == "node"
            assert updated.args is not None and updated.args[0] == "--version"

            await ctx.client.rpc.mcp.config.disable(MCPConfigDisableRequest(names=[server_name]))
            await ctx.client.rpc.mcp.config.enable(MCPConfigEnableRequest(names=[server_name]))
        finally:
            await ctx.client.rpc.mcp.config.remove(MCPConfigRemoveRequest(name=server_name))

        after_remove = await ctx.client.rpc.mcp.config.list()
        assert server_name not in after_remove.servers

    async def test_should_round_trip_http_mcp_oauth_config_rpc(self, ctx: E2ETestContext):
        await ctx.client.start()

        server_name = f"sdk-http-oauth-{uuid.uuid4().hex}"
        config = MCPServerConfig(
            type=MCPServerConfigType.HTTP,
            url="https://example.com/mcp",
            headers={"Authorization": "Bearer token"},
            oauth_client_id="client-id",
            oauth_public_client=False,
            oauth_grant_type=MCPServerConfigHTTPOauthGrantType.CLIENT_CREDENTIALS,
            tools=["*"],
            timeout=3000,
        )
        updated_config = MCPServerConfig(
            type=MCPServerConfigType.HTTP,
            url="https://example.com/updated-mcp",
            oauth_client_id="updated-client-id",
            oauth_public_client=True,
            oauth_grant_type=MCPServerConfigHTTPOauthGrantType.AUTHORIZATION_CODE,
            tools=["updated-tool"],
            timeout=4000,
        )

        try:
            await ctx.client.rpc.mcp.config.add(
                MCPConfigAddRequest(name=server_name, config=config)
            )
            after_add = await ctx.client.rpc.mcp.config.list()
            added = _server_config(after_add.servers, server_name)
            assert added.type == MCPServerConfigType.HTTP
            assert added.url == "https://example.com/mcp"
            assert added.headers is not None
            assert added.headers["Authorization"] == "Bearer token"
            assert added.oauth_client_id == "client-id"
            assert added.oauth_public_client is False
            assert added.oauth_grant_type == MCPServerConfigHTTPOauthGrantType.CLIENT_CREDENTIALS

            await ctx.client.rpc.mcp.config.update(
                MCPConfigUpdateRequest(name=server_name, config=updated_config)
            )
            after_update = await ctx.client.rpc.mcp.config.list()
            updated = _server_config(after_update.servers, server_name)
            assert updated.url == "https://example.com/updated-mcp"
            assert updated.oauth_client_id == "updated-client-id"
            assert updated.oauth_public_client is True
            assert updated.oauth_grant_type == MCPServerConfigHTTPOauthGrantType.AUTHORIZATION_CODE
            assert updated.tools is not None and updated.tools[0] == "updated-tool"
            assert updated.timeout == 4000
        finally:
            await ctx.client.rpc.mcp.config.remove(MCPConfigRemoveRequest(name=server_name))

        after_remove = await ctx.client.rpc.mcp.config.list()
        assert server_name not in after_remove.servers
