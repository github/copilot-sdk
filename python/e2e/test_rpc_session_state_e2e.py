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
            assert result.success, "Expected History.compact() to report success=True"
            assert result.messages_removed >= 0, "messages_removed must be non-negative"
            if result.context_window is not None:
                assert result.context_window.messages_length >= 0
                assert result.context_window.current_tokens >= 0

            # Session must still be usable after compaction
            name = await session.rpc.name.get()
            assert name is not None
        finally:
            await session.disconnect()

    async def test_should_set_and_get_each_session_mode_value(self, ctx: E2ETestContext):
        for mode in [SessionMode.INTERACTIVE, SessionMode.PLAN, SessionMode.AUTOPILOT]:
            session = await ctx.client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                await session.rpc.mode.set(ModeSetRequest(mode=mode))
                result = await session.rpc.mode.get()
                assert result == mode, f"Expected mode {mode} but got {result}"
            finally:
                await session.disconnect()

    async def test_should_reject_workspace_file_path_traversal(self, ctx: E2ETestContext):

        for traversal_path in [
            "../escaped.txt",
            "../../escaped.txt",
            "nested/../../../escaped.txt",
        ]:
            session = await ctx.client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                with pytest.raises(Exception) as excinfo:
                    await session.rpc.workspaces.create_file(
                        WorkspacesCreateFileRequest(
                            path=traversal_path,
                            content="should not land outside workspace",
                        )
                    )
                assert "workspace files directory" in str(excinfo.value).lower()

                with pytest.raises(Exception) as excinfo2:
                    await session.rpc.workspaces.read_file(
                        WorkspacesReadFileRequest(path=traversal_path)
                    )
                assert "workspace files directory" in str(excinfo2.value).lower()
            finally:
                await session.disconnect()

    async def test_should_create_workspace_file_with_nested_path_auto_creating_dirs(
        self, ctx: E2ETestContext
    ):
        import uuid

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            nested_path = f"nested-{uuid.uuid4().hex}/subdir/file.txt"
            await session.rpc.workspaces.create_file(
                WorkspacesCreateFileRequest(path=nested_path, content="nested content")
            )
            read = await session.rpc.workspaces.read_file(
                WorkspacesReadFileRequest(path=nested_path)
            )
            assert read.content == "nested content"

            listed = await session.rpc.workspaces.list_files()
            assert any(f.endswith("file.txt") for f in listed.files)
        finally:
            await session.disconnect()

    async def test_should_report_error_reading_nonexistent_workspace_file(
        self, ctx: E2ETestContext
    ):
        import uuid

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            with pytest.raises(Exception):
                await session.rpc.workspaces.read_file(
                    WorkspacesReadFileRequest(path=f"never-exists-{uuid.uuid4().hex}.txt")
                )
        finally:
            await session.disconnect()

    async def test_should_update_existing_workspace_file_with_update_operation(
        self, ctx: E2ETestContext
    ):
        import asyncio
        import uuid

        from copilot.generated.session_events import (
            SessionWorkspaceFileChangedData,
            WorkspaceFileChangedOperation,
        )

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            path = f"reused-{uuid.uuid4().hex}.txt"
            await session.rpc.workspaces.create_file(
                WorkspacesCreateFileRequest(path=path, content="v1")
            )

            update_future: asyncio.Future = asyncio.get_event_loop().create_future()

            def on_event(event):
                if (
                    isinstance(event.data, SessionWorkspaceFileChangedData)
                    and event.data.path == path
                    and event.data.operation == WorkspaceFileChangedOperation.UPDATE
                    and not update_future.done()
                ):
                    update_future.set_result(event)

            unsubscribe = session.on(on_event)
            try:
                await session.rpc.workspaces.create_file(
                    WorkspacesCreateFileRequest(path=path, content="v2")
                )
                evt = await asyncio.wait_for(update_future, timeout=15.0)
                assert evt.data.operation == WorkspaceFileChangedOperation.UPDATE

                read = await session.rpc.workspaces.read_file(WorkspacesReadFileRequest(path=path))
                assert read.content == "v2"
            finally:
                unsubscribe()
        finally:
            await session.disconnect()

    async def test_should_reject_empty_or_whitespace_session_name(self, ctx: E2ETestContext):
        for empty_name in ["", "   ", "\t\n  \r"]:
            session = await ctx.client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                with pytest.raises(Exception) as excinfo:
                    await session.rpc.name.set(NameSetRequest(name=empty_name))
                assert "empty" in str(excinfo.value).lower()
            finally:
                await session.disconnect()

    async def test_should_emit_title_changed_event_each_time_name_set_is_called(
        self, ctx: E2ETestContext
    ):
        import asyncio
        import uuid

        from copilot.generated.session_events import SessionTitleChangedData

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            title_a = f"Title-A-{uuid.uuid4().hex}"
            title_b = f"Title-B-{uuid.uuid4().hex}"

            first_task: asyncio.Future = asyncio.get_event_loop().create_future()
            second_task: asyncio.Future = asyncio.get_event_loop().create_future()

            def on_event(event):
                if isinstance(event.data, SessionTitleChangedData):
                    if event.data.title == title_a and not first_task.done():
                        first_task.set_result(event)
                    elif event.data.title == title_b and not second_task.done():
                        second_task.set_result(event)

            unsubscribe = session.on(on_event)
            try:
                await session.rpc.name.set(NameSetRequest(name=title_a))
                await asyncio.wait_for(first_task, timeout=15.0)

                await session.rpc.name.set(NameSetRequest(name=title_b))
                second_evt = await asyncio.wait_for(second_task, timeout=15.0)
                assert second_evt.data.title == title_b
            finally:
                unsubscribe()
        finally:
            await session.disconnect()

    async def test_should_fork_session_to_event_id_excluding_boundary_event(
        self, ctx: E2ETestContext
    ):
        first_prompt = "Say FORK_BOUNDARY_FIRST exactly."
        second_prompt = "Say FORK_BOUNDARY_SECOND exactly."

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            await session.send_and_wait(first_prompt, timeout=60.0)
            await session.send_and_wait(second_prompt, timeout=60.0)

            source_events = await session.get_messages()
            second_user_event = next(
                (
                    e
                    for e in source_events
                    if isinstance(e.data, UserMessageData) and e.data.content == second_prompt
                ),
                None,
            )
            assert second_user_event is not None, (
                "Expected the second user.message in persisted history"
            )
            boundary_event_id = str(second_user_event.id)

            fork = await ctx.client.rpc.sessions.fork(
                SessionsForkRequest(session_id=session.session_id, to_event_id=boundary_event_id)
            )
            assert (fork.session_id or "").strip()
            assert fork.session_id != session.session_id

            forked_session = await ctx.client.resume_session(
                fork.session_id,
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                forked_events = await forked_session.get_messages()
                forked_ids = {str(e.id) for e in forked_events}
                assert boundary_event_id not in forked_ids, (
                    "toEventId is exclusive — boundary event must not be in forked session"
                )

                forked_conv = _conversation_messages(forked_events)
                assert any(r == "user" and c == first_prompt for r, c in forked_conv)
                assert not any(r == "user" and c == second_prompt for r, c in forked_conv)
            finally:
                await forked_session.disconnect()
        finally:
            await session.disconnect()

    async def test_should_report_error_when_forking_session_to_unknown_event_id(
        self, ctx: E2ETestContext
    ):
        import uuid

        source_prompt = "Say FORK_UNKNOWN_EVENT_OK exactly."

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            await session.send_and_wait(source_prompt, timeout=60.0)

            bogus_event_id = str(uuid.uuid4())
            with pytest.raises(Exception) as excinfo:
                await ctx.client.rpc.sessions.fork(
                    SessionsForkRequest(session_id=session.session_id, to_event_id=bogus_event_id)
                )
            text = str(excinfo.value)
            assert f"Event {bogus_event_id} not found".lower() in text.lower()
            assert "Unhandled method sessions.fork".lower() not in text.lower()
        finally:
            await session.disconnect()
