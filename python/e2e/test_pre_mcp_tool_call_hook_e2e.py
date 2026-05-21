"""
E2E tests for the preMcpToolCall hook, verifying meta manipulation scenarios:
setting meta, replacing meta, and removing meta.
"""

from __future__ import annotations

from pathlib import Path

import pytest

from copilot.session import MCPServerConfig, PermissionHandler

from .testharness import E2ETestContext

TEST_MCP_META_ECHO_SERVER = str(
    (Path(__file__).parents[2] / "test" / "harness" / "test-mcp-meta-echo-server.mjs").resolve()
)
TEST_HARNESS_DIR = str((Path(__file__).parents[2] / "test" / "harness").resolve())

pytestmark = pytest.mark.asyncio(loop_scope="module")


def meta_echo_mcp_config() -> dict[str, MCPServerConfig]:
    return {
        "meta-echo": {
            "command": "node",
            "args": [TEST_MCP_META_ECHO_SERVER],
            "cwd": TEST_HARNESS_DIR,
            "tools": ["*"],
        }
    }


class TestPreMcpToolCallHook:
    async def test_should_set_meta_via_premcptoolcall_hook(self, ctx: E2ETestContext):
        inputs: list[dict] = []

        async def on_pre_mcp_tool_call(input_data, invocation):
            inputs.append(input_data)
            return {"metaToUse": {"injected": "by-hook", "source": "test"}}

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            mcp_servers=meta_echo_mcp_config(),
            hooks={"on_pre_mcp_tool_call": on_pre_mcp_tool_call},
        )
        try:
            response = await session.send_and_wait(
                "Use the meta-echo/echo_meta tool with value 'test-set'."
                " Reply with just the raw tool result."
            )
            assert response is not None
            assert "injected" in (response.data.content or "")
            assert "by-hook" in (response.data.content or "")

            assert inputs
            assert inputs[0].get("serverName") == "meta-echo"
            assert inputs[0].get("toolName") == "echo_meta"
            assert inputs[0].get("workingDirectory")
            assert inputs[0].get("timestamp", 0) > 0
        finally:
            await session.disconnect()

    async def test_should_replace_meta_via_premcptoolcall_hook(self, ctx: E2ETestContext):
        inputs: list[dict] = []

        async def on_pre_mcp_tool_call(input_data, invocation):
            inputs.append(input_data)
            return {"metaToUse": {"completely": "replaced"}}

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            mcp_servers=meta_echo_mcp_config(),
            hooks={"on_pre_mcp_tool_call": on_pre_mcp_tool_call},
        )
        try:
            response = await session.send_and_wait(
                "Use the meta-echo/echo_meta tool with value 'test-replace'."
                " Reply with just the raw tool result."
            )
            assert response is not None
            assert "completely" in (response.data.content or "")
            assert "replaced" in (response.data.content or "")

            assert inputs
            assert inputs[0].get("serverName") == "meta-echo"
            assert inputs[0].get("toolName") == "echo_meta"
        finally:
            await session.disconnect()

    async def test_should_remove_meta_via_premcptoolcall_hook(self, ctx: E2ETestContext):
        inputs: list[dict] = []

        async def on_pre_mcp_tool_call(input_data, invocation):
            inputs.append(input_data)
            return {"metaToUse": None}

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            mcp_servers=meta_echo_mcp_config(),
            hooks={"on_pre_mcp_tool_call": on_pre_mcp_tool_call},
        )
        try:
            response = await session.send_and_wait(
                "Use the meta-echo/echo_meta tool with value 'test-remove'."
                " Reply with just the raw tool result."
            )
            assert response is not None
            assert '"meta":null' in (response.data.content or "") or '"meta": null' in (
                response.data.content or ""
            )
            assert "test-remove" in (response.data.content or "")

            assert inputs
            assert inputs[0].get("serverName") == "meta-echo"
            assert inputs[0].get("toolName") == "echo_meta"
        finally:
            await session.disconnect()
