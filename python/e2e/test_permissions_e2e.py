"""
Tests for permission callback functionality
"""

import asyncio

import pytest

from copilot.generated.session_events import (
    PermissionRequest,
    SessionIdleData,
    ToolExecutionCompleteData,
)
from copilot.session import PermissionHandler, PermissionRequestResult

from .testharness import E2ETestContext
from .testharness.helper import read_file, write_file

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestPermissions:
    async def test_should_invoke_permission_handler_for_write_operations(self, ctx: E2ETestContext):
        """Test that permission handler is invoked for write operations"""
        permission_requests = []

        def on_permission_request(
            request: PermissionRequest, invocation: dict
        ) -> PermissionRequestResult:
            permission_requests.append(request)
            assert invocation["session_id"] == session.session_id
            return PermissionRequestResult(kind="approve-once")

        session = await ctx.client.create_session(on_permission_request=on_permission_request)

        write_file(ctx.work_dir, "test.txt", "original content")

        await session.send_and_wait("Edit test.txt and replace 'original' with 'modified'")

        # Should have received at least one permission request
        assert len(permission_requests) > 0

        # Should include write permission request
        write_requests = [req for req in permission_requests if req.kind.value == "write"]
        assert len(write_requests) > 0

        await session.disconnect()

    async def test_should_deny_permission_when_handler_returns_denied(self, ctx: E2ETestContext):
        """Test denying permissions"""

        def on_permission_request(
            request: PermissionRequest, invocation: dict
        ) -> PermissionRequestResult:
            return PermissionRequestResult(kind="reject")

        session = await ctx.client.create_session(on_permission_request=on_permission_request)

        original_content = "protected content"
        write_file(ctx.work_dir, "protected.txt", original_content)

        await session.send_and_wait("Edit protected.txt and replace 'protected' with 'hacked'.")

        # Verify the file was NOT modified
        content = read_file(ctx.work_dir, "protected.txt")
        assert content == original_content

        await session.disconnect()

    async def test_should_deny_tool_operations_when_handler_explicitly_denies(
        self, ctx: E2ETestContext
    ):
        """Test that tool operations are denied when handler explicitly denies"""

        def deny_all(request, invocation):
            return PermissionRequestResult()

        session = await ctx.client.create_session(on_permission_request=deny_all)

        denied_events = []
        done_event = asyncio.Event()

        def on_event(event):
            match event.data:
                case ToolExecutionCompleteData(success=False) as data:
                    error = data.error
                    msg = (
                        error
                        if isinstance(error, str)
                        else (getattr(error, "message", None) if error is not None else None)
                    )
                    if msg and "Permission denied" in msg:
                        denied_events.append(event)
                case SessionIdleData():
                    done_event.set()

        session.on(on_event)

        await session.send("Run 'node --version'")
        await asyncio.wait_for(done_event.wait(), timeout=60)

        assert len(denied_events) > 0

        await session.disconnect()

    async def test_should_deny_tool_operations_when_handler_explicitly_denies_after_resume(
        self, ctx: E2ETestContext
    ):
        """Test that tool operations are denied after resume when handler explicitly denies"""
        session1 = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        session_id = session1.session_id
        await session1.send_and_wait("What is 1+1?")

        def deny_all(request, invocation):
            return PermissionRequestResult()

        session2 = await ctx.client.resume_session(session_id, on_permission_request=deny_all)

        denied_events = []
        done_event = asyncio.Event()

        def on_event(event):
            match event.data:
                case ToolExecutionCompleteData(success=False) as data:
                    error = data.error
                    msg = (
                        error
                        if isinstance(error, str)
                        else (getattr(error, "message", None) if error is not None else None)
                    )
                    if msg and "Permission denied" in msg:
                        denied_events.append(event)
                case SessionIdleData():
                    done_event.set()

        session2.on(on_event)

        await session2.send("Run 'node --version'")
        await asyncio.wait_for(done_event.wait(), timeout=60)

        assert len(denied_events) > 0

        await session2.disconnect()

    async def test_should_work_with_approve_all_permission_handler(self, ctx: E2ETestContext):
        """Test that sessions work with approve-all permission handler"""
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )

        message = await session.send_and_wait("What is 2+2?")

        assert message is not None
        assert "4" in message.data.content

        await session.disconnect()

    async def test_should_handle_async_permission_handler(self, ctx: E2ETestContext):
        """Test async permission handler"""
        permission_requests = []

        async def on_permission_request(
            request: PermissionRequest, invocation: dict
        ) -> PermissionRequestResult:
            permission_requests.append(request)
            await asyncio.sleep(0)
            return PermissionRequestResult(kind="approve-once")

        session = await ctx.client.create_session(on_permission_request=on_permission_request)

        await session.send_and_wait("Run 'echo test' and tell me what happens")

        assert len(permission_requests) > 0

        await session.disconnect()

    async def test_should_resume_session_with_permission_handler(self, ctx: E2ETestContext):
        """Test resuming session with permission handler"""
        permission_requests = []

        # Create initial session
        session1 = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        session_id = session1.session_id
        await session1.send_and_wait("What is 1+1?")

        # Resume with permission handler
        def on_permission_request(
            request: PermissionRequest, invocation: dict
        ) -> PermissionRequestResult:
            permission_requests.append(request)
            return PermissionRequestResult(kind="approve-once")

        session2 = await ctx.client.resume_session(
            session_id, on_permission_request=on_permission_request
        )

        await session2.send_and_wait("Run 'echo resumed' for me")

        # Should have permission requests from resumed session
        assert len(permission_requests) > 0

        await session2.disconnect()

    async def test_should_handle_permission_handler_errors_gracefully(self, ctx: E2ETestContext):
        """Test that permission handler errors are handled gracefully"""

        def on_permission_request(
            request: PermissionRequest, invocation: dict
        ) -> PermissionRequestResult:
            raise RuntimeError("Handler error")

        session = await ctx.client.create_session(on_permission_request=on_permission_request)

        message = await session.send_and_wait("Run 'echo test'. If you can't, say 'failed'.")

        # Should handle the error and deny permission
        assert message is not None
        content_lower = message.data.content.lower()
        assert any(word in content_lower for word in ["fail", "cannot", "unable", "permission"])

        await session.disconnect()

    async def test_should_receive_toolcallid_in_permission_requests(self, ctx: E2ETestContext):
        """Test that toolCallId is included in permission requests"""
        received_tool_call_id = False

        def on_permission_request(
            request: PermissionRequest, invocation: dict
        ) -> PermissionRequestResult:
            nonlocal received_tool_call_id
            if request.tool_call_id:
                received_tool_call_id = True
                assert isinstance(request.tool_call_id, str)
                assert len(request.tool_call_id) > 0
            return PermissionRequestResult(kind="approve-once")

        session = await ctx.client.create_session(on_permission_request=on_permission_request)

        await session.send_and_wait("Run 'echo test'")

        assert received_tool_call_id

        await session.disconnect()

    async def test_should_wait_for_slow_permission_handler(self, ctx: E2ETestContext):
        """Slow permission handler blocks tool execution until released."""
        handler_entered: asyncio.Future = asyncio.get_event_loop().create_future()
        release_handler: asyncio.Future = asyncio.get_event_loop().create_future()
        target_tool_call_id: asyncio.Future = asyncio.get_event_loop().create_future()
        lifecycle: list = []

        def add_event(phase: str, tool_call_id: str | None) -> None:
            lifecycle.append((phase, tool_call_id))

        async def slow_permission(request: PermissionRequest, invocation: dict):
            tool_call_id = request.tool_call_id
            add_event("permission-start", tool_call_id)
            if not target_tool_call_id.done():
                target_tool_call_id.set_result(tool_call_id)
            if not handler_entered.done():
                handler_entered.set_result(True)
            await asyncio.wait_for(release_handler, timeout=30.0)
            add_event("permission-complete", tool_call_id)
            return PermissionRequestResult(kind="approve-once")

        session = await ctx.client.create_session(on_permission_request=slow_permission)

        def on_event(event):
            if event.type.value == "tool.execution_start":
                add_event("tool-start", event.data.tool_call_id)
            elif event.type.value == "tool.execution_complete":
                add_event("tool-complete", event.data.tool_call_id)

        unsubscribe = session.on(on_event)
        try:
            asyncio.ensure_future(session.send("Run 'echo slow_handler_test'"))

            await asyncio.wait_for(handler_entered, timeout=30.0)
            target_id = await asyncio.wait_for(target_tool_call_id, timeout=30.0)

            # Tool should not have completed yet while handler is blocking
            assert not any(
                phase == "tool-complete" and tid == target_id for phase, tid in lifecycle
            ), "Tool completed before permission handler returned"

            release_handler.set_result(True)

            from .testharness.helper import get_final_assistant_message

            message = await get_final_assistant_message(session, timeout=60.0)

            perm_start = next(
                (
                    i
                    for i, (p, tid) in enumerate(lifecycle)
                    if p == "permission-start" and tid == target_id
                ),
                -1,
            )
            perm_complete = next(
                (
                    i
                    for i, (p, tid) in enumerate(lifecycle)
                    if p == "permission-complete" and tid == target_id
                ),
                -1,
            )
            tool_start = next(
                (
                    i
                    for i, (p, tid) in enumerate(lifecycle)
                    if p == "tool-start" and tid == target_id
                ),
                -1,
            )
            tool_complete = next(
                (
                    i
                    for i, (p, tid) in enumerate(lifecycle)
                    if p == "tool-complete" and tid == target_id
                ),
                -1,
            )

            assert perm_start >= 0
            assert perm_complete >= 0
            assert tool_start >= 0
            assert tool_complete >= 0
            assert perm_complete < tool_complete, (
                "Expected permission completion before target tool completion"
            )
            assert tool_start < tool_complete, (
                "Expected target tool start before target tool completion"
            )
            assert message is not None
            assert "slow_handler_test" in (message.data.content or "")
        finally:
            if not release_handler.done():
                release_handler.set_result(True)
            unsubscribe()
            await session.disconnect()

    async def test_should_deny_permission_with_noresult_kind(self, ctx: E2ETestContext):
        """NoResult permission kind leaves legacy permission requests unanswered."""

        permission_called = asyncio.get_event_loop().create_future()

        def deny_noresult(request: PermissionRequest, invocation: dict) -> PermissionRequestResult:
            if not permission_called.done():
                permission_called.set_result(True)
            return PermissionRequestResult(kind="no-result")

        session = await ctx.client.create_session(on_permission_request=deny_noresult)
        try:
            asyncio.ensure_future(session.send("Run 'node --version'"))
            await asyncio.wait_for(permission_called, timeout=30.0)
            await session.abort()
        finally:
            await session.disconnect()

    async def test_should_short_circuit_permission_handler_when_set_approve_all_enabled(
        self, ctx: E2ETestContext
    ):
        """When set_approve_all is true, the runtime short-circuits the handler."""
        from copilot.generated.rpc import PermissionsSetApproveAllRequest

        handler_call_count = 0

        def counting_handler(
            request: PermissionRequest, invocation: dict
        ) -> PermissionRequestResult:
            nonlocal handler_call_count
            handler_call_count += 1
            return PermissionRequestResult(kind="approve-once")

        session = await ctx.client.create_session(on_permission_request=counting_handler)
        try:
            set_result = await session.rpc.permissions.set_approve_all(
                PermissionsSetApproveAllRequest(enabled=True)
            )
            assert set_result.success

            tool_completed: asyncio.Future = asyncio.get_event_loop().create_future()

            def on_event(event):
                if (
                    event.type.value == "tool.execution_complete"
                    and event.data.success
                    and not tool_completed.done()
                ):
                    tool_completed.set_result(event)

            unsubscribe = session.on(on_event)
            try:
                await session.send_and_wait(
                    "Run 'echo test' and tell me what happens", timeout=60.0
                )
                await asyncio.wait_for(tool_completed, timeout=30.0)
                assert handler_call_count == 0, (
                    "Handler should not have been called when approve_all is enabled"
                )
            finally:
                unsubscribe()
        finally:
            try:
                from copilot.generated.rpc import PermissionsSetApproveAllRequest

                await session.rpc.permissions.set_approve_all(
                    PermissionsSetApproveAllRequest(enabled=False)
                )
            except Exception as exc:
                # Cleanup should not hide the primary test result, but should be visible in logs.
                print(f"Failed to disable approve_all during cleanup: {exc!r}")
            await session.disconnect()

    async def test_should_handle_concurrent_permission_requests_from_parallel_tools(
        self, ctx: E2ETestContext
    ):
        """Multiple simultaneous permission requests are all handled."""
        from copilot.tools import Tool, ToolInvocation, ToolResult

        permission_request_count = 0
        both_started: asyncio.Future = asyncio.get_event_loop().create_future()
        first_tool_called = False
        second_tool_called = False

        async def concurrent_permission(request: PermissionRequest, invocation: dict):
            nonlocal permission_request_count
            permission_request_count += 1
            if permission_request_count >= 2 and not both_started.done():
                both_started.set_result(True)
            await asyncio.wait_for(both_started, timeout=30.0)
            return PermissionRequestResult(kind="approve-once")

        def first_tool_handler(invocation: ToolInvocation) -> ToolResult:
            nonlocal first_tool_called
            first_tool_called = True
            return ToolResult(
                text_result_for_llm="first_permission_tool completed after permission approval",
                result_type="rejected",
            )

        def second_tool_handler(invocation: ToolInvocation) -> ToolResult:
            nonlocal second_tool_called
            second_tool_called = True
            return ToolResult(
                text_result_for_llm="second_permission_tool completed after permission approval",
                result_type="rejected",
            )

        session = await ctx.client.create_session(
            on_permission_request=concurrent_permission,
            tools=[
                Tool(
                    name="first_permission_tool",
                    description="First concurrent permission test tool",
                    parameters={"type": "object", "properties": {}},
                    handler=first_tool_handler,
                ),
                Tool(
                    name="second_permission_tool",
                    description="Second concurrent permission test tool",
                    parameters={"type": "object", "properties": {}},
                    handler=second_tool_handler,
                ),
            ],
        )
        try:
            idle_future: asyncio.Future = asyncio.get_event_loop().create_future()
            tool_completes = []

            def on_event(event):
                if event.type.value == "tool.execution_complete" and not event.data.success:
                    tool_completes.append(event)
                elif event.type.value == "session.idle" and not idle_future.done():
                    idle_future.set_result(True)

            unsubscribe = session.on(on_event)
            try:
                await session.send(
                    "Call both first_permission_tool and second_permission_tool in the same turn."
                    " Do not call any other tools."
                )
                await asyncio.wait_for(both_started, timeout=30.0)
                await asyncio.wait_for(idle_future, timeout=60.0)

                assert permission_request_count == 2, (
                    "Expected exactly 2 permission requests (one per tool)"
                )
                assert first_tool_called, "first_permission_tool handler should have been called"
                assert second_tool_called, "second_permission_tool handler should have been called"
                assert len(tool_completes) >= 2, (
                    "Expected tool.execution_complete events for both tools"
                )
            finally:
                unsubscribe()
        finally:
            await session.disconnect()
