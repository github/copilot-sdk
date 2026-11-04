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

from copilot import CopilotClient, RuntimeConnection
from copilot.rpc import EventLogReadRequest, ModelSwitchAutoTierStatus
from copilot.session import PermissionHandler
from copilot.session_events import (
    AutoTier,
    SessionAutoTierSwitchFailedData,
    SessionModelChangeData,
)

from .testharness import DEFAULT_GITHUB_TOKEN, E2ETestContext, get_next_event_of_type

pytestmark = pytest.mark.asyncio(loop_scope="module")


async def pending_auto_tier(session) -> AutoTier | None:
    return (await session.rpc.model.get_current()).pending_auto_tier


def create_auto_client(ctx: E2ETestContext) -> CopilotClient:
    return CopilotClient(
        connection=RuntimeConnection.for_stdio(path=ctx.cli_path),
        working_directory=ctx.work_dir,
        env=ctx.get_env(),
        github_token=DEFAULT_GITHUB_TOKEN,
    )


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
            superseded = await session.set_auto_tier("fast")
            assert superseded.status == ModelSwitchAutoTierStatus.PENDING
            assert superseded.pending_auto_tier == AutoTier.FAST
            assert superseded.superseded_auto_tier == AutoTier.EFFICIENCY
            assert await pending_auto_tier(session) == AutoTier.FAST

            replaced_fast = await session.set_auto_tier("intelligence")
            assert replaced_fast.status == ModelSwitchAutoTierStatus.PENDING
            assert replaced_fast.pending_auto_tier == AutoTier.INTELLIGENCE
            assert replaced_fast.superseded_auto_tier == AutoTier.FAST
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
            await session.set_model("auto", auto_tier="fast")
            assert await pending_auto_tier(session) == AutoTier.FAST

            # Supplying None clears it. Omission, a value, and None are three distinct
            # outcomes, which is why the argument cannot collapse to a plain optional.
            await session.set_model("auto", auto_tier=None)
            assert await pending_auto_tier(session) is None
        finally:
            await session.disconnect()

    async def test_should_restore_and_override_fast_auto_tier_on_cold_resume(
        self, ctx: E2ETestContext
    ):
        client = create_auto_client(ctx)
        fast_session = await client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            model="auto",
            capi={"auto_tier": "fast", "enable_web_socket_responses": False},
        )
        tierless_session = await client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            model="auto",
            capi={"enable_web_socket_responses": False},
        )
        fast_session_id = fast_session.session_id
        tierless_session_id = tierless_session.session_id

        await fast_session.send_and_wait("Reply with exactly AUTO_TIER_COLD_RESUME_READY.")
        await tierless_session.send_and_wait("Reply with exactly AUTO_TIER_TIERLESS_READY.")
        assert (await fast_session.rpc.model.get_current()).auto_tier == AutoTier.FAST
        assert (await tierless_session.rpc.model.get_current()).auto_tier is None

        await fast_session.disconnect()
        await tierless_session.disconnect()
        await client.stop()

        restored_client = create_auto_client(ctx)
        restored_fast = await restored_client.resume_session(
            fast_session_id,
            on_permission_request=PermissionHandler.approve_all,
        )
        restored_tierless = await restored_client.resume_session(
            tierless_session_id,
            on_permission_request=PermissionHandler.approve_all,
        )
        assert (await restored_fast.rpc.model.get_current()).auto_tier == AutoTier.FAST
        assert (await restored_tierless.rpc.model.get_current()).auto_tier is None
        await restored_fast.disconnect()
        await restored_tierless.disconnect()
        await restored_client.stop()

        override_client = create_auto_client(ctx)
        overridden = await override_client.resume_session(
            fast_session_id,
            on_permission_request=PermissionHandler.approve_all,
            model="auto",
            capi={"auto_tier": "balance", "enable_web_socket_responses": False},
        )
        assert (await overridden.rpc.model.get_current()).auto_tier == AutoTier.BALANCE
        await overridden.disconnect()
        await override_client.stop()

    async def test_should_commit_fast_auto_tier_after_successful_turn(self, ctx: E2ETestContext):
        client = create_auto_client(ctx)
        session = await client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            model="auto",
            capi={"auto_tier": "efficiency", "enable_web_socket_responses": False},
        )
        try:
            model_change_task = get_next_event_of_type(session, "session.model_change")
            staged = await session.set_auto_tier("fast")
            assert staged.status == ModelSwitchAutoTierStatus.PENDING
            assert staged.effective_auto_tier == AutoTier.EFFICIENCY
            assert staged.pending_auto_tier == AutoTier.FAST

            before_turn = await session.rpc.model.get_current()
            assert before_turn.auto_tier == AutoTier.EFFICIENCY
            assert before_turn.pending_auto_tier == AutoTier.FAST

            await session.send_and_wait("Reply with exactly AUTO_TIER_FAST_COMMITTED.")

            model_change = await model_change_task
            assert isinstance(model_change.data, SessionModelChangeData)
            assert model_change.data.previous_model == "auto"
            assert model_change.data.new_model == "auto"
            assert model_change.data.previous_auto_tier == AutoTier.EFFICIENCY
            assert model_change.data.auto_tier == AutoTier.FAST

            committed = await session.rpc.model.get_current()
            assert committed.auto_tier == AutoTier.FAST
            assert committed.pending_auto_tier is None
            assert committed.activating_auto_tier is None
        finally:
            await session.disconnect()
            await client.stop()

    async def test_should_preserve_effective_tier_when_fast_activation_fails(
        self, ctx: E2ETestContext
    ):
        client = create_auto_client(ctx)
        session = await client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            model="auto",
            capi={"auto_tier": "efficiency", "enable_web_socket_responses": False},
        )
        session_id = session.session_id

        await session.send_and_wait("Reply with exactly AUTO_TIER_INITIAL_READY.")

        fast_committed = False

        def on_event(event):
            nonlocal fast_committed
            if isinstance(event.data, SessionModelChangeData):
                fast_committed |= event.data.auto_tier == AutoTier.FAST

        unsubscribe = session.on(on_event)
        failure_task = get_next_event_of_type(session, "session.auto_tier_switch_failed")

        staged = await session.set_auto_tier("fast")
        assert staged.status == ModelSwitchAutoTierStatus.PENDING
        assert staged.effective_auto_tier == AutoTier.EFFICIENCY
        assert staged.pending_auto_tier == AutoTier.FAST

        await session.send_and_wait("Reply with exactly AUTO_TIER_FAILURE_RECOVERED.")

        failure = await failure_task
        assert failure.ephemeral is True
        assert isinstance(failure.data, SessionAutoTierSwitchFailedData)
        assert failure.data.effective_auto_tier == AutoTier.EFFICIENCY
        assert failure.data.requested_auto_tier == AutoTier.FAST
        assert failure.data.reason.value == "request_failed"
        assert fast_committed is False

        current = await session.rpc.model.get_current()
        assert current.auto_tier == AutoTier.EFFICIENCY
        assert current.pending_auto_tier is None
        assert current.activating_auto_tier is None

        unsubscribe()
        await session.disconnect()
        await client.stop()

        resumed_client = create_auto_client(ctx)
        try:
            resumed = await resumed_client.resume_session(
                session_id,
                on_permission_request=PermissionHandler.approve_all,
            )
            resumed_current = await resumed.rpc.model.get_current()
            assert resumed_current.auto_tier == AutoTier.EFFICIENCY
            assert resumed_current.pending_auto_tier is None
            assert resumed_current.activating_auto_tier is None

            persisted = await resumed.rpc.event_log.read(EventLogReadRequest(max=100, wait_ms=0))
            assert not any(
                event.type.value == "session.auto_tier_switch_failed" for event in persisted.events
            )
            await resumed.disconnect()
        finally:
            await resumed_client.stop()
