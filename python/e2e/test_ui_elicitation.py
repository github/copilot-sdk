"""E2E UI Elicitation Tests (single-client)

Mirrors nodejs/test/e2e/ui_elicitation.test.ts — single-client scenarios.

Uses the shared ``ctx`` fixture from conftest.py.
"""

import pytest

from copilot.session import (
    ElicitationRequest,
    ElicitationResult,
    PermissionHandler,
)

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestUiElicitation:
    async def test_elicitation_methods_throw_in_headless_mode(self, ctx: E2ETestContext):
        """Elicitation methods throw when running in headless mode."""
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )

        # The SDK spawns the CLI headless — no TUI means no elicitation support.
        ui_caps = session.capabilities.get("ui", {})
        assert not ui_caps.get("elicitation")

        with pytest.raises(RuntimeError, match="not supported"):
            await session.ui.confirm("test")

    async def test_session_with_elicitation_handler_reports_capability(self, ctx: E2ETestContext):
        """Session created with onElicitationRequest reports elicitation capability."""

        async def handler(
            request: ElicitationRequest, invocation: dict[str, str]
        ) -> ElicitationResult:
            return {"action": "accept", "content": {}}

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            on_elicitation_request=handler,
        )

        assert session.capabilities.get("ui", {}).get("elicitation") is True

    async def test_session_without_elicitation_handler_reports_no_capability(
        self, ctx: E2ETestContext
    ):
        """Session created without onElicitationRequest reports no elicitation capability."""
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )

        assert session.capabilities.get("ui", {}).get("elicitation") in (False, None)
