"""
E2E coverage for ``session.tasks.*`` and pending-handler RPCs.

Mirrors ``dotnet/test/RpcTasksAndHandlersTests.cs`` (snapshot category
``rpc_tasks_and_handlers``).
"""

from __future__ import annotations

import pytest

from copilot.generated.rpc import (
    CommandsHandlePendingCommandRequest,
    HandlePendingToolCallRequest,
    PermissionDecision,
    PermissionDecisionKind,
    PermissionDecisionRequest,
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
        finally:
            await session.disconnect()
