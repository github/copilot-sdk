"""
E2E coverage for ``session.tasks.*`` and pending-handler RPCs.

Mirrors ``dotnet/test/RpcTasksAndHandlersTests.cs`` (snapshot category
``rpc_tasks_and_handlers``).
"""

from __future__ import annotations

import asyncio

import pytest

from copilot.generated.rpc import (
    ApprovalKind,
    CommandsHandlePendingCommandRequest,
    HandlePendingToolCallRequest,
    PermissionDecision,
    PermissionDecisionApproveForIonApproval,
    PermissionDecisionKind,
    PermissionDecisionRequest,
    TaskInfoType,
    TasksCancelRequest,
    TasksPromoteToBackgroundRequest,
    TasksRemoveRequest,
    TasksStartAgentRequest,
    UIElicitationResponse,
    UIElicitationResponseAction,
    UIHandlePendingElicitationRequest,
)
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


async def _find_agent_task(session, task_id: str):
    task_list = await session.rpc.tasks.list()
    return next((t for t in (task_list.tasks or []) if t.id == task_id), None)


async def _wait_for_agent_task(session, task_id: str, predicate, timeout: float, message: str):
    deadline = asyncio.get_running_loop().time() + timeout
    last_task = None
    while True:
        last_task = await _find_agent_task(session, task_id)
        if predicate(last_task):
            return last_task
        if asyncio.get_running_loop().time() >= deadline:
            pytest.fail(f"{message}; last observed task: {last_task!r}")
        await asyncio.sleep(0.25)


async def _assert_implemented_failure(awaitable, method: str) -> None:
    with pytest.raises(Exception) as excinfo:
        _ = await awaitable
    assert f"Unhandled method {method}".lower() not in str(excinfo.value).lower()


class TestRpcTasksAndHandlers:
    async def test_should_list_task_state_and_return_false_for_missing_task_operations(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            tasks = await session.rpc.tasks.list()
            assert tasks.tasks is not None
            assert len(tasks.tasks) == 0

            promote = await session.rpc.tasks.promote_to_background(
                TasksPromoteToBackgroundRequest(id="missing-task")
            )
            assert promote.promoted is False

            cancel = await session.rpc.tasks.cancel(TasksCancelRequest(id="missing-task"))
            assert cancel.cancelled is False

            remove = await session.rpc.tasks.remove(TasksRemoveRequest(id="missing-task"))
            assert remove.removed is False
        finally:
            await session.disconnect()

    async def test_should_report_implemented_error_for_missing_task_agent_type(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            await _assert_implemented_failure(
                session.rpc.tasks.start_agent(
                    TasksStartAgentRequest(
                        agent_type="missing-agent-type",
                        prompt="Say hi",
                        name="sdk-test-task",
                    )
                ),
                "session.tasks.startAgent",
            )
        finally:
            await session.disconnect()

    async def test_should_return_expected_results_for_missing_pending_handler_request_ids(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            tool = await session.rpc.tools.handle_pending_tool_call(
                HandlePendingToolCallRequest(
                    request_id="missing-tool-request",
                    result="tool result",
                )
            )
            assert tool.success is False

            command = await session.rpc.commands.handle_pending_command(
                CommandsHandlePendingCommandRequest(
                    request_id="missing-command-request",
                    error="command error",
                )
            )
            assert command.success is True

            elicitation = await session.rpc.ui.handle_pending_elicitation(
                UIHandlePendingElicitationRequest(
                    request_id="missing-elicitation-request",
                    result=UIElicitationResponse(action=UIElicitationResponseAction.CANCEL),
                )
            )
            assert elicitation.success is False

            permission = await session.rpc.permissions.handle_pending_permission_request(
                PermissionDecisionRequest(
                    request_id="missing-permission-request",
                    result=PermissionDecision(
                        kind=PermissionDecisionKind.REJECT,
                        feedback="not approved",
                    ),
                )
            )
            assert permission.success is False

            permanent = await session.rpc.permissions.handle_pending_permission_request(
                PermissionDecisionRequest(
                    request_id="missing-permanent-permission-request",
                    result=PermissionDecision(
                        kind=PermissionDecisionKind.APPROVE_PERMANENTLY,
                        domain="example.com",
                    ),
                )
            )
            assert permanent.success is False

            session_approval = await session.rpc.permissions.handle_pending_permission_request(
                PermissionDecisionRequest(
                    request_id="missing-session-approval-request",
                    result=PermissionDecision(
                        kind=PermissionDecisionKind.APPROVE_FOR_SESSION,
                        approval=PermissionDecisionApproveForIonApproval(
                            kind=ApprovalKind.CUSTOM_TOOL,
                            tool_name="missing-tool",
                        ),
                    ),
                )
            )
            assert session_approval.success is False

            location_approval = await session.rpc.permissions.handle_pending_permission_request(
                PermissionDecisionRequest(
                    request_id="missing-location-approval-request",
                    result=PermissionDecision(
                        kind=PermissionDecisionKind.APPROVE_FOR_LOCATION,
                        location_key="missing-location",
                        approval=PermissionDecisionApproveForIonApproval(
                            kind=ApprovalKind.CUSTOM_TOOL,
                            tool_name="missing-tool",
                        ),
                    ),
                )
            )
            assert location_approval.success is False
        finally:
            await session.disconnect()

    async def test_should_report_implemented_error_for_invalid_task_agent_model(
        self, ctx: E2ETestContext
    ):
        """Invalid model name for agent task returns an error without 'Unhandled method'."""
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            with pytest.raises(Exception) as excinfo:
                await session.rpc.tasks.start_agent(
                    TasksStartAgentRequest(
                        agent_type="general-purpose",
                        prompt="Say hi",
                        name="sdk-test-invalid-model",
                        model="not-a-real-model",
                    )
                )
            text = str(excinfo.value).lower()
            assert "unhandled method session.tasks.startagent" not in text

            tasks = await session.rpc.tasks.list()
            assert tasks.tasks is not None
            assert len(tasks.tasks) == 0, "Task list should be empty after invalid start"
        finally:
            await session.disconnect()

    async def test_should_start_background_agent_and_report_task_details(self, ctx: E2ETestContext):
        """Start a background agent task and verify task details then remove it."""
        from copilot.generated.rpc import TaskInfoExecutionMode, TaskInfoStatus

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            ready = await session.send_and_wait(
                "Reply with TASK_AGENT_READY exactly.",
                timeout=60.0,
            )
            assert ready is not None
            assert "TASK_AGENT_READY" in (ready.data.content or "")

            start_result = await session.rpc.tasks.start_agent(
                TasksStartAgentRequest(
                    agent_type="general-purpose",
                    prompt="Reply with TASK_AGENT_DONE exactly.",
                    name="sdk-background-agent",
                    description="SDK background agent coverage",
                )
            )
            task_id = start_result.agent_id
            assert task_id, "Expected a task ID from start_agent"

            found_task = await _wait_for_agent_task(
                session,
                task_id,
                lambda task: task is not None,
                30.0,
                f"Task {task_id} not found in tasks list",
            )
            assert found_task.id == task_id
            assert found_task.description == "SDK background agent coverage"
            assert found_task.type == TaskInfoType.AGENT
            assert found_task.agent_type == "general-purpose"
            assert found_task.execution_mode == TaskInfoExecutionMode.BACKGROUND
            assert found_task.prompt == "Reply with TASK_AGENT_DONE exactly."

            found_task = await _wait_for_agent_task(
                session,
                task_id,
                lambda task: (
                    task is None
                    or task.status
                    in (
                        TaskInfoStatus.COMPLETED,
                        TaskInfoStatus.FAILED,
                        TaskInfoStatus.CANCELLED,
                        TaskInfoStatus.IDLE,
                    )
                ),
                60.0,
                f"Task {task_id} did not produce a final observable state",
            )
            assert found_task is not None, f"Task {task_id} disappeared before it completed"
            assert "TASK_AGENT_DONE" in (found_task.latest_response or found_task.result or "")

            if found_task.status == TaskInfoStatus.IDLE:
                cancel = await session.rpc.tasks.cancel(TasksCancelRequest(id=task_id))
                assert cancel.cancelled is True

            # Remove the task
            remove = await session.rpc.tasks.remove(TasksRemoveRequest(id=task_id))
            assert remove.removed is True

            after_remove = await session.rpc.tasks.list()
            assert not any(t.id == task_id for t in (after_remove.tasks or []))
        finally:
            await session.disconnect()
