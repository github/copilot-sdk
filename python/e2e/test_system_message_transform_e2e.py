"""
Copyright (c) Microsoft Corporation.

Tests for system message transform functionality
"""

import pytest

from copilot.session import PermissionHandler

from .testharness import E2ETestContext
from .testharness.helper import write_file

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestSystemMessageTransform:
    async def test_should_invoke_transform_callbacks_with_section_content(
        self, ctx: E2ETestContext
    ):
        """Test that transform callbacks are invoked with the section content"""
        identity_contents = []
        tone_contents = []

        async def identity_transform(content: str) -> str:
            identity_contents.append(content)
            return content

        async def tone_transform(content: str) -> str:
            tone_contents.append(content)
            return content

        session = await ctx.client.create_session(
            system_message={
                "mode": "customize",
                "sections": {
                    "identity": {"action": identity_transform},
                    "tone": {"action": tone_transform},
                },
            },
            on_permission_request=PermissionHandler.approve_all,
        )

        write_file(ctx.work_dir, "test.txt", "Hello transform!")

        await session.send_and_wait("Read the contents of test.txt and tell me what it says")

        # Both transform callbacks should have been invoked
        assert len(identity_contents) > 0
        assert len(tone_contents) > 0

        # Callbacks should have received non-empty content
        assert all(len(c) > 0 for c in identity_contents)
        assert all(len(c) > 0 for c in tone_contents)

        await session.disconnect()

    async def test_should_apply_transform_modifications_to_section_content(
        self, ctx: E2ETestContext
    ):
        """Test that transform modifications are applied to the section content"""

        async def identity_transform(content: str) -> str:
            return content + "\nTRANSFORM_MARKER"

        session = await ctx.client.create_session(
            system_message={
                "mode": "customize",
                "sections": {
                    "identity": {"action": identity_transform},
                },
            },
            on_permission_request=PermissionHandler.approve_all,
        )

        write_file(ctx.work_dir, "hello.txt", "Hello!")

        await session.send_and_wait("Read the contents of hello.txt")

        # Verify the transform result was actually applied to the system message
        traffic = await ctx.get_exchanges()
        system_message = _get_system_message(traffic[0])
        assert "TRANSFORM_MARKER" in system_message

        await session.disconnect()

    async def test_should_work_with_static_overrides_and_transforms_together(
        self, ctx: E2ETestContext
    ):
        """Test that static overrides and transforms work together"""
        identity_contents = []

        async def identity_transform(content: str) -> str:
            identity_contents.append(content)
            return content

        session = await ctx.client.create_session(
            system_message={
                "mode": "customize",
                "sections": {
                    "safety": {"action": "remove"},
                    "identity": {"action": identity_transform},
                },
            },
            on_permission_request=PermissionHandler.approve_all,
        )

        write_file(ctx.work_dir, "combo.txt", "Combo test!")

        await session.send_and_wait("Read the contents of combo.txt and tell me what it says")

        # The transform callback should have been invoked
        assert len(identity_contents) > 0

        await session.disconnect()


def _get_system_message(exchange: dict) -> str:
    messages = exchange.get("request", {}).get("messages", [])
    for msg in messages:
        if msg.get("role") == "system":
            return msg.get("content", "")
    return ""
