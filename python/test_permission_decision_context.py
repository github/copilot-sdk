from unittest.mock import AsyncMock, MagicMock

from copilot.rpc import (
    PermissionDecisionApproveOnce,
    PermissionDecisionContext,
    PermissionDecisionOutcome,
    PermissionDecisionSource,
    PermissionDecisionSurface,
)
from copilot.session import (
    AttributedPermissionResult,
    CopilotSession,
    PermissionNoResult,
    with_decision_context,
)
from copilot.session_events import PermissionRequestRead


def _context() -> PermissionDecisionContext:
    return PermissionDecisionContext(
        outcome=PermissionDecisionOutcome.AUTO_APPROVED,
        source=PermissionDecisionSource.HOST_POLICY,
        surface=PermissionDecisionSurface.SDK,
    )


def _session_with_captured_rpc() -> tuple[CopilotSession, AsyncMock]:
    session = CopilotSession("session-1", client=None)
    handle = AsyncMock()
    rpc = MagicMock()
    rpc.permissions.handle_pending_permission_request = handle
    session._rpc = rpc
    return session, handle


async def test_decision_context_serialized_as_sibling_of_result() -> None:
    session, handle = _session_with_captured_rpc()
    request = PermissionRequestRead(intention="Read", path="/workspace/file.txt")

    def handler(_request, _invocation):
        return with_decision_context(PermissionDecisionApproveOnce(), _context())

    await session._execute_permission_and_respond("permission-1", request, handler)

    handle.assert_awaited_once()
    sent = handle.await_args.args[0]
    params = sent.to_dict()

    assert params["decisionContext"] == {
        "outcome": "auto_approved",
        "source": "host_policy",
        "surface": "sdk",
    }
    assert "decisionContext" not in params["result"]
    assert params["result"]["kind"] == "approve-once"


async def test_no_context_omits_decision_context_key() -> None:
    session, handle = _session_with_captured_rpc()
    request = PermissionRequestRead(intention="Read", path="/workspace/file.txt")

    def handler(_request, _invocation):
        return PermissionDecisionApproveOnce()

    await session._execute_permission_and_respond("permission-1", request, handler)

    handle.assert_awaited_once()
    params = handle.await_args.args[0].to_dict()

    assert "decisionContext" not in params
    assert params["result"]["kind"] == "approve-once"


def test_with_decision_context_replaces_rather_than_nests() -> None:
    first = PermissionDecisionContext(
        outcome=PermissionDecisionOutcome.PROMPTED_USER,
        source=PermissionDecisionSource.HUMAN_RESPONSE,
        surface=PermissionDecisionSurface.TUI,
    )
    second = _context()

    once_wrapped = with_decision_context(PermissionDecisionApproveOnce(), first)
    twice_wrapped = with_decision_context(once_wrapped, second)

    assert isinstance(twice_wrapped, AttributedPermissionResult)
    assert isinstance(twice_wrapped.result, PermissionDecisionApproveOnce)
    assert twice_wrapped.decision_context is second


async def test_no_result_with_context_still_suppresses_response() -> None:
    session, handle = _session_with_captured_rpc()
    request = PermissionRequestRead(intention="Read", path="/workspace/file.txt")

    def handler(_request, _invocation):
        return with_decision_context(PermissionNoResult(), _context())

    await session._execute_permission_and_respond("permission-1", request, handler)

    handle.assert_not_awaited()
