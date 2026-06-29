"""E2E coverage for SDK sub-agent callback hook rejection."""

import pytest

from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")

UNSUPPORTED_SDK_HOOKS_MESSAGE = "SDK hook callbacks are no longer supported"


async def _allow(*_args):
    return {"permissionDecision": "allow"}


async def _noop(*_args):
    return None


class TestSubagentHooks:
    async def test_rejects_sdk_callback_hooks_for_sub_agents(self, ctx: E2ETestContext):
        with pytest.raises(Exception, match=UNSUPPORTED_SDK_HOOKS_MESSAGE):
            await ctx.client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                hooks={"on_pre_tool_use": _allow, "on_post_tool_use": _noop},
            )
