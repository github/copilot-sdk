"""
E2E coverage for Auto routing tier switching (snapshot category ``auto_tier``).

The runtime stages an Auto routing preference instead of applying it immediately: a
request stays "unclaimed" until a later turn using the ``auto`` model mints a usable
model and token pair. These tests observe that staged state through
``model.get_current``, so they assert what the runtime actually recorded rather than
what the SDK serialized.
"""

from __future__ import annotations

import pytest

from copilot.rpc import ModelSwitchAutoTierStatus
from copilot.session import PermissionHandler
from copilot.session_events import AutoTier

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


async def pending_auto_tier(session) -> AutoTier | None:
    return (await session.rpc.model.get_current()).pending_auto_tier


class TestAutoTier:
    async def test_should_stage_and_reset_auto_tier_preference(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            model="auto",
        )
        try:
            assert await pending_auto_tier(session) is None

            staged = await session.set_auto_tier("efficiency")
            assert staged.status == ModelSwitchAutoTierStatus.PENDING
            assert staged.pending_auto_tier == AutoTier.EFFICIENCY
            assert await pending_auto_tier(session) == AutoTier.EFFICIENCY

            # A second request replaces the first and reports the one it displaced.
            superseded = await session.set_auto_tier("intelligence")
            assert superseded.status == ModelSwitchAutoTierStatus.PENDING
            assert superseded.pending_auto_tier == AutoTier.INTELLIGENCE
            assert superseded.superseded_auto_tier == AutoTier.EFFICIENCY
            assert await pending_auto_tier(session) == AutoTier.INTELLIGENCE

            # Passing None returns the session to provider-default routing. The status is
            # "unchanged" because provider-default was already the committed preference;
            # the request's effect is cancelling the staged one.
            reset = await session.set_auto_tier(None)
            assert reset.status == ModelSwitchAutoTierStatus.UNCHANGED
            assert reset.superseded_auto_tier == AutoTier.INTELLIGENCE
            assert await pending_auto_tier(session) is None
        finally:
            await session.disconnect()

    async def test_should_preserve_auto_tier_when_set_model_omits_it(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            model="auto",
        )
        try:
            await session.set_auto_tier("balance")
            assert await pending_auto_tier(session) == AutoTier.BALANCE

            # Omitting the argument leaves the staged preference alone.
            await session.set_model("auto")
            assert await pending_auto_tier(session) == AutoTier.BALANCE

            # Supplying a tier replaces it.
            await session.set_model("auto", auto_tier="intelligence")
            assert await pending_auto_tier(session) == AutoTier.INTELLIGENCE

            # Supplying None clears it. Omission, a value, and None are three distinct
            # outcomes, which is why the argument cannot collapse to a plain optional.
            await session.set_model("auto", auto_tier=None)
            assert await pending_auto_tier(session) is None
        finally:
            await session.disconnect()
