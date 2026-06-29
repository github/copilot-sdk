"""E2E coverage for SDK lifecycle callback hook rejection."""

import pytest

from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")

UNSUPPORTED_SDK_HOOKS_MESSAGE = "SDK hook callbacks are no longer supported"


async def _allow(*_args):
    return {"permissionDecision": "allow"}


async def _noop(*_args):
    return None


async def _modified_prompt(*_args):
    return {"modifiedPrompt": "not used"}


async def _post_failure(*_args):
    return {"additionalContext": "not used"}


async def assert_unsupported_hooks(ctx: E2ETestContext, hooks: dict):
    with pytest.raises(Exception, match=UNSUPPORTED_SDK_HOOKS_MESSAGE):
        await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            hooks=hooks,
        )


class TestHooksExtended:
    @pytest.mark.parametrize(
        "hooks",
        [
            {"on_user_prompt_submitted": _modified_prompt},
            {"on_session_start": _noop},
            {"on_session_end": _noop},
            {"on_error_occurred": _noop},
            {"on_pre_tool_use": _allow},
            {"on_post_tool_use": _noop},
            {"on_post_tool_use": _noop, "on_post_tool_use_failure": _post_failure},
        ],
    )
    async def test_rejects_sdk_callback_hooks(self, ctx: E2ETestContext, hooks: dict):
        await assert_unsupported_hooks(ctx, hooks)
