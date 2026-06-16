"""
Copyright (c) Microsoft Corporation.

Tests for system message sections functionality
"""

import pytest

from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestSystemMessageSections:
    async def test_should_use_replaced_identity_section_in_response(
        self, ctx: E2ETestContext
    ):
        """Test that replacing the identity section causes the assistant to adopt the custom identity"""
        session = await ctx.client.create_session(
            system_message={
                "mode": "customize",
                "sections": {
                    "identity": {
                        "action": "replace",
                        "content": "You are a helpful gardening assistant called Botanica. You only answer questions about plants and gardening.",
                    },
                },
            },
            on_permission_request=PermissionHandler.approve_all,
        )

        response = await session.send_and_wait("Who are you?")

        assert response is not None, "Expected a response from the assistant"
        content = response.data.content.lower()
        assert (
            "botanica" in content or "garden" in content or "plant" in content
        ), f"Expected response to reflect the replaced identity section, but got: {response.data.content}"

        await session.disconnect()
