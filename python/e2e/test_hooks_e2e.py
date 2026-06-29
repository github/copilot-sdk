"""E2E coverage for SDK callback hook rejection."""

import pytest

from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")

UNSUPPORTED_SDK_HOOKS_MESSAGE = "SDK hook callbacks are no longer supported"


async def _allow(*_args):
    return {"permissionDecision": "allow"}


async def _deny(*_args):
    return {"permissionDecision": "deny"}


async def _noop(*_args):
    return None


async def assert_unsupported_hooks(ctx: E2ETestContext, hooks: dict):
    with pytest.raises(Exception, match=UNSUPPORTED_SDK_HOOKS_MESSAGE):
        await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            hooks=hooks,
        )


class TestHooks:
    @pytest.mark.parametrize(
        "hooks",
        [
            {"on_pre_tool_use": _allow},
            {"on_post_tool_use": _noop},
            {"on_pre_tool_use": _deny},
            {"on_pre_tool_use": _allow, "on_post_tool_use": _noop},
        ],
    )
    async def test_rejects_sdk_callback_hooks(self, ctx: E2ETestContext, hooks: dict):
        await assert_unsupported_hooks(ctx, hooks)
