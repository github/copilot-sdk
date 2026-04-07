"""E2E tests for session configuration including model capabilities overrides."""

import base64
import os

import pytest

from copilot import ModelCapabilitiesOverride, ModelSupportsOverride
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


def has_image_url_content(exchanges: list[dict]) -> bool:
    """Check if any exchange contains an image_url content part in user messages."""
    for ex in exchanges:
        for msg in ex.get("request", {}).get("messages", []):
            if msg.get("role") == "user" and isinstance(msg.get("content"), list):
                if any(p.get("type") == "image_url" for p in msg["content"]):
                    return True
    return False


PNG_1X1 = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
)
VIEW_IMAGE_PROMPT = "Use the view tool to look at the file test.png and describe what you see"


class TestSessionConfig:
    """Tests for session configuration including model capabilities overrides."""

    async def test_vision_disabled_then_enabled_via_setmodel(self, ctx: E2ETestContext):
        png_path = os.path.join(ctx.work_dir, "test.png")
        with open(png_path, "wb") as f:
            f.write(PNG_1X1)

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            model_capabilities=ModelCapabilitiesOverride(
                supports=ModelSupportsOverride(vision=False)
            ),
        )

        # Turn 1: vision off — no image_url expected
        await session.send_and_wait(VIEW_IMAGE_PROMPT)
        traffic_after_t1 = await ctx.get_exchanges()
        assert not has_image_url_content(traffic_after_t1)

        # Switch vision on
        await session.set_model(
            "claude-sonnet-4.5",
            model_capabilities=ModelCapabilitiesOverride(
                supports=ModelSupportsOverride(vision=True)
            ),
        )

        # Turn 2: vision on — image_url expected in new exchanges
        await session.send_and_wait(VIEW_IMAGE_PROMPT)
        traffic_after_t2 = await ctx.get_exchanges()
        new_exchanges = traffic_after_t2[len(traffic_after_t1) :]
        assert has_image_url_content(new_exchanges)

        await session.disconnect()

    async def test_vision_enabled_then_disabled_via_setmodel(self, ctx: E2ETestContext):
        png_path = os.path.join(ctx.work_dir, "test.png")
        with open(png_path, "wb") as f:
            f.write(PNG_1X1)

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            model_capabilities=ModelCapabilitiesOverride(
                supports=ModelSupportsOverride(vision=True)
            ),
        )

        # Turn 1: vision on — image_url expected
        await session.send_and_wait(VIEW_IMAGE_PROMPT)
        traffic_after_t1 = await ctx.get_exchanges()
        assert has_image_url_content(traffic_after_t1)

        # Switch vision off
        await session.set_model(
            "claude-sonnet-4.5",
            model_capabilities=ModelCapabilitiesOverride(
                supports=ModelSupportsOverride(vision=False)
            ),
        )

        # Turn 2: vision off — no image_url expected in new exchanges
        await session.send_and_wait(VIEW_IMAGE_PROMPT)
        traffic_after_t2 = await ctx.get_exchanges()
        new_exchanges = traffic_after_t2[len(traffic_after_t1) :]
        assert not has_image_url_content(new_exchanges)

        await session.disconnect()
