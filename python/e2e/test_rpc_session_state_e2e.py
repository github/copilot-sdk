"""
E2E coverage for session-scoped state RPCs.

Mirrors ``dotnet/test/RpcSessionStateTests.cs`` (snapshot category
``rpc_session_state``).
"""

from __future__ import annotations

import pytest

from copilot.generated.rpc import (
    HistoryTruncateRequest,
    MCPOauthLoginRequest,
    ModelSwitchToRequest,
    ModeSetRequest,
    NameSetRequest,
    PermissionsSetApproveAllRequest,
    PlanUpdateRequest,
    SessionMode,
    SessionsForkRequest,
    WorkspacesCreateFileRequest,
    WorkspacesReadFileRequest,
)
from copilot.generated.session_events import AssistantMessageData, UserMessageData
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


def _conversation_messages(events) -> list[tuple[str, str]]:
    out: list[tuple[str, str]] = []
    for evt in events:
        match evt.data:
            case UserMessageData() as data:
                out.append(("user", data.content or ""))
            case AssistantMessageData() as data:
                out.append(("assistant", data.content or ""))
    return out


async def _assert_implemented_failure(awaitable, method: str) -> None:
    with pytest.raises(Exception) as excinfo:
        _ = await awaitable
    assert f"Unhandled method {method}".lower() not in str(excinfo.value).lower()


class TestRpcSessionState:
    async def test_should_call_session_rpc_model_get_current(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            model="claude-sonnet-4.5",
        )
        try:
            result = await session.rpc.model.get_current()
            assert result.model_id
        finally:
            await session.disconnect()

    async def test_should_call_session_rpc_model_switch_to(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            model="claude-sonnet-4.5",
        )
        try:
            before = await session.rpc.model.get_current()
            assert before.model_id

            result = await session.rpc.model.switch_to(
                ModelSwitchToRequest(model_id="gpt-4.1", reasoning_effort="high")
            )
            after = await session.rpc.model.get_current()

            assert result.model_id == "gpt-4.1"
            # SwitchToAsync does not mutate session state — it only resolves the override.
            assert after.model_id == before.model_id
        finally:
            await session.disconnect()

    async def test_should_get_and_set_session_mode(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            initial = await session.rpc.mode.get()
            assert initial == SessionMode.INTERACTIVE

            await session.rpc.mode.set(ModeSetRequest(mode=SessionMode.PLAN))
            assert await session.rpc.mode.get() == SessionMode.PLAN

            await session.rpc.mode.set(ModeSetRequest(mode=SessionMode.INTERACTIVE))
            assert await session.rpc.mode.get() == SessionMode.INTERACTIVE
        finally:
            await session.disconnect()

    async def test_should_read_update_and_delete_plan(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            initial = await session.rpc.plan.read()
            assert initial.exists is False
            assert initial.content is None

            plan_content = "# Test Plan\n\n- Step 1\n- Step 2"
            await session.rpc.plan.update(PlanUpdateRequest(content=plan_content))

            after_update = await session.rpc.plan.read()
            assert after_update.exists is True
            assert after_update.content == plan_content

            await session.rpc.plan.delete()

            after_delete = await session.rpc.plan.read()
            assert after_delete.exists is False
            assert after_delete.content is None
        finally:
            await session.disconnect()

    async def test_should_call_workspace_file_rpc_methods(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            initial = await session.rpc.workspaces.list_files()
            assert initial.files is not None

            await session.rpc.workspaces.create_file(
                WorkspacesCreateFileRequest(path="test.txt", content="Hello, workspace!")
            )

            after_create = await session.rpc.workspaces.list_files()
            assert "test.txt" in after_create.files

            file = await session.rpc.workspaces.read_file(
                WorkspacesReadFileRequest(path="test.txt")
            )
            assert file.content == "Hello, workspace!"

            workspace = await session.rpc.workspaces.get_workspace()
            assert workspace.workspace is not None
            assert workspace.workspace.id is not None
        finally:
            await session.disconnect()

    async def test_should_get_and_set_session_metadata(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            await session.rpc.name.set(NameSetRequest(name="SDK test session"))
            name = await session.rpc.name.get()
            assert name.name == "SDK test session"

            sources = await session.rpc.instructions.get_sources()
            assert sources.sources is not None
        finally:
            await session.disconnect()

    async def test_should_fork_session_with_persisted_messages(self, ctx: E2ETestContext):
        source_prompt = "Say FORK_SOURCE_ALPHA exactly."
        fork_prompt = "Now say FORK_CHILD_BETA exactly."

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            initial_answer = await session.send_and_wait(source_prompt, timeout=60.0)
            assert initial_answer is not None
            assert "FORK_SOURCE_ALPHA" in (initial_answer.data.content or "")

            source_messages = await session.get_messages()
            source_conversation = _conversation_messages(source_messages)
            assert any(
                role == "user" and content == source_prompt for role, content in source_conversation
            )
            assert any(
                role == "assistant" and "FORK_SOURCE_ALPHA" in content
                for role, content in source_conversation
            )

            fork = await ctx.client.rpc.sessions.fork(
                SessionsForkRequest(session_id=session.session_id)
            )
            assert (fork.session_id or "").strip()
            assert fork.session_id != session.session_id

            forked_session = await ctx.client.resume_session(
                fork.session_id,
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                forked_messages = await forked_session.get_messages()
                forked_conversation = _conversation_messages(forked_messages)
                assert forked_conversation[: len(source_conversation)] == source_conversation

                fork_answer = await forked_session.send_and_wait(fork_prompt, timeout=60.0)
                assert fork_answer is not None
                assert "FORK_CHILD_BETA" in (fork_answer.data.content or "")

                source_after_fork = _conversation_messages(await session.get_messages())
                assert all(content != fork_prompt for _, content in source_after_fork)

                fork_after_prompt = _conversation_messages(await forked_session.get_messages())
                assert any(
                    role == "user" and content == fork_prompt for role, content in fork_after_prompt
                )
                assert any(
                    role == "assistant" and "FORK_CHILD_BETA" in content
                    for role, content in fork_after_prompt
                )
            finally:
                await forked_session.disconnect()
        finally:
            await session.disconnect()

    async def test_should_report_error_when_forking_session_without_persisted_events(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            with pytest.raises(Exception) as excinfo:
                await ctx.client.rpc.sessions.fork(
                    SessionsForkRequest(session_id=session.session_id)
                )
            text = str(excinfo.value).lower()
            assert "not found or has no persisted events" in text
            assert "unhandled method sessions.fork" not in text
        finally:
            await session.disconnect()

    async def test_should_call_session_usage_and_permission_rpcs(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            metrics = await session.rpc.usage.get_metrics()
            assert metrics.session_start_time > 0
            if metrics.total_nano_aiu is not None:
                assert metrics.total_nano_aiu >= 0
            if metrics.token_details is not None:
                for detail in metrics.token_details.values():
                    assert detail.token_count >= 0
            for model_metric in metrics.model_metrics.values():
                if model_metric.total_nano_aiu is not None:
                    assert model_metric.total_nano_aiu >= 0
                if model_metric.token_details is not None:
                    for detail in model_metric.token_details.values():
                        assert detail.token_count >= 0

            try:
                approve_all = await session.rpc.permissions.set_approve_all(
                    PermissionsSetApproveAllRequest(enabled=True)
                )
                assert approve_all.success

                reset = await session.rpc.permissions.reset_session_approvals()
                assert reset.success
            finally:
                await session.rpc.permissions.set_approve_all(
                    PermissionsSetApproveAllRequest(enabled=False)
                )
        finally:
            await session.disconnect()

    async def test_should_report_implemented_errors_for_unsupported_session_rpc_paths(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            await _assert_implemented_failure(
                session.rpc.history.truncate(HistoryTruncateRequest(event_id="missing-event")),
                "session.history.truncate",
            )
            await _assert_implemented_failure(
                session.rpc.mcp.oauth.login(MCPOauthLoginRequest(server_name="missing-server")),
                "session.mcp.oauth.login",
            )
        finally:
            await session.disconnect()

    async def test_should_compact_session_history_after_messages(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            await session.send_and_wait("What is 2+2?", timeout=60.0)
            result = await session.rpc.history.compact()
            assert result is not None
        finally:
            await session.disconnect()
