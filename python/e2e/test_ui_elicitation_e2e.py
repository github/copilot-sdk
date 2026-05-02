"""E2E UI Elicitation Tests (single-client)

Mirrors nodejs/test/e2e/ui_elicitation.test.ts — single-client scenarios.

Uses the shared ``ctx`` fixture from conftest.py.
"""

import pytest

from copilot.session import (
    ElicitationContext,
    ElicitationParams,
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

        with pytest.raises(RuntimeError, match="not supported"):
            await session.ui.select("test", ["a", "b"])

        with pytest.raises(RuntimeError, match="not supported"):
            await session.ui.input("test")

        with pytest.raises(RuntimeError, match="not supported"):
            await session.ui.elicitation(
                {
                    "message": "Enter name",
                    "requestedSchema": {
                        "type": "object",
                        "properties": {"name": {"type": "string"}},
                        "required": ["name"],
                    },
                }
            )

        await session.disconnect()

    async def test_session_with_elicitation_handler_reports_capability(self, ctx: E2ETestContext):
        """Session created with onElicitationContext reports elicitation capability."""

        async def handler(
            context: ElicitationContext,
        ) -> ElicitationResult:
            return {"action": "accept", "content": {}}

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            on_elicitation_request=handler,
        )

        assert session.capabilities.get("ui", {}).get("elicitation") is True

        await session.disconnect()

    async def test_session_without_elicitation_handler_reports_no_capability(
        self, ctx: E2ETestContext
    ):
        """Session created without onElicitationContext reports no elicitation capability."""
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )

        assert session.capabilities.get("ui", {}).get("elicitation") in (False, None)

        await session.disconnect()

    async def test_sends_request_elicitation_when_handler_provided(self, ctx: E2ETestContext):
        """Session is created successfully with requestElicitation=true when handler is provided."""

        async def handler(_: ElicitationContext) -> ElicitationResult:
            return {"action": "accept", "content": {}}

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            on_elicitation_request=handler,
        )

        assert session.session_id is not None
        await session.disconnect()

    async def test_session_without_elicitation_handler_creates_successfully(
        self, ctx: E2ETestContext
    ):
        """Session without an elicitation handler still creates successfully."""
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )

        assert session.session_id is not None
        await session.disconnect()

    async def test_confirm_returns_true_when_handler_accepts(self, ctx: E2ETestContext):
        async def handler(context: ElicitationContext) -> ElicitationResult:
            assert context["message"] == "Confirm?"
            schema = context.get("requestedSchema") or {}
            assert "confirmed" in (schema.get("properties") or {})
            return {"action": "accept", "content": {"confirmed": True}}

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            on_elicitation_request=handler,
        )

        assert session.capabilities.get("ui", {}).get("elicitation") is True
        assert (await session.ui.confirm("Confirm?")) is True

        await session.disconnect()

    async def test_confirm_returns_false_when_handler_declines(self, ctx: E2ETestContext):
        async def handler(_: ElicitationContext) -> ElicitationResult:
            return {"action": "decline"}

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            on_elicitation_request=handler,
        )

        assert (await session.ui.confirm("Confirm?")) is False

        await session.disconnect()

    async def test_select_returns_selected_option(self, ctx: E2ETestContext):
        async def handler(context: ElicitationContext) -> ElicitationResult:
            assert context["message"] == "Choose"
            schema = context.get("requestedSchema") or {}
            assert "selection" in (schema.get("properties") or {})
            return {"action": "accept", "content": {"selection": "beta"}}

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            on_elicitation_request=handler,
        )

        assert (await session.ui.select("Choose", ["alpha", "beta"])) == "beta"

        await session.disconnect()

    async def test_input_returns_freeform_value(self, ctx: E2ETestContext):
        async def handler(context: ElicitationContext) -> ElicitationResult:
            assert context["message"] == "Enter value"
            schema = context.get("requestedSchema") or {}
            assert "value" in (schema.get("properties") or {})
            return {"action": "accept", "content": {"value": "typed value"}}

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            on_elicitation_request=handler,
        )

        result = await session.ui.input(
            "Enter value",
            {
                "title": "Value",
                "description": "A value to test",
                "minLength": 1,
                "maxLength": 20,
                "default": "default",
            },
        )
        assert result == "typed value"

        await session.disconnect()

    async def test_elicitation_returns_all_action_shapes(self, ctx: E2ETestContext):
        responses: list[ElicitationResult] = [
            {"action": "accept", "content": {"name": "Mona"}},
            {"action": "decline"},
            {"action": "cancel"},
        ]

        async def handler(context: ElicitationContext) -> ElicitationResult:
            assert context["message"] == "Name?"
            return responses.pop(0)

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            on_elicitation_request=handler,
        )

        params: ElicitationParams = {
            "message": "Name?",
            "requestedSchema": {
                "type": "object",
                "properties": {"name": {"type": "string"}},
                "required": ["name"],
            },
        }

        accept = await session.ui.elicitation(params)
        decline = await session.ui.elicitation(params)
        cancel = await session.ui.elicitation(params)

        assert accept["action"] == "accept"
        assert (accept.get("content") or {}).get("name") == "Mona"
        assert decline["action"] == "decline"
        assert cancel["action"] == "cancel"

        await session.disconnect()
