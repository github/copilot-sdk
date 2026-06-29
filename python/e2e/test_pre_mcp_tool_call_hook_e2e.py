"""E2E coverage for SDK preMcpToolCall callback hook rejection."""

import pytest

from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")

UNSUPPORTED_SDK_HOOKS_MESSAGE = "SDK hook callbacks are no longer supported"


async def _pre_mcp_tool_call(*_args):
    return {"metaToUse": {"injected": "by-hook"}}


class TestPreMcpToolCallHook:
    async def test_rejects_sdk_premcptoolcall_callback_hook(self, ctx: E2ETestContext):
        with pytest.raises(Exception, match=UNSUPPORTED_SDK_HOOKS_MESSAGE):
            await ctx.client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                hooks={"on_pre_mcp_tool_call": _pre_mcp_tool_call},
            )
