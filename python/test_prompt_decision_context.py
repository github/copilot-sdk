"""A labeled answer to a user-input, plan-review or elicitation prompt carries its
decision context on the wire; an unlabeled answer carries no ``decisionContext``."""

import json
from unittest.mock import AsyncMock, Mock

import pytest

from copilot import CopilotClient, RuntimeConnection, create_attributed_result
from copilot.rpc import (
    PermissionDecisionContext,
    PermissionDecisionOutcome,
    PermissionDecisionSource,
    PermissionDecisionSurface,
    PermissionResponseCapability,
)
from copilot.session import (
    CopilotSession,
    ElicitationContext,
    ElicitationHandlerResult,
    ExitPlanModeRequest,
    ExitPlanModeResult,
    UserInputRequest,
    UserInputResponse,
)

PERSON_ANSWERED = PermissionDecisionContext(
    source=PermissionDecisionSource.HUMAN_RESPONSE,
    response_capability=PermissionResponseCapability.INTERACTIVE,
    surface=PermissionDecisionSurface.SDK,
    outcome=PermissionDecisionOutcome.PROMPTED_USER,
)

PERSON_ANSWERED_ON_WIRE = {
    "source": "human_response",
    "responseCapability": "interactive",
    "surface": "sdk",
    "outcome": "prompted_user",
}

LABELED = pytest.mark.parametrize("labeled", [True, False], ids=["person", "unlabeled"])


def _client_with_session() -> tuple[CopilotClient, CopilotSession]:
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    session = CopilotSession("session-1", Mock())
    client._sessions[session.session_id] = session
    return client, session


def _on_wire(reply: dict) -> dict:
    # The JSON-RPC layer serializes the reply as-is, so it must already be plain JSON.
    return json.loads(json.dumps(reply))


@LABELED
async def test_user_input_reply_carries_decision_context_at_top_level(labeled: bool) -> None:
    client, session = _client_with_session()

    def handler(request: UserInputRequest, invocation: dict[str, str]) -> UserInputResponse:
        answer: UserInputResponse = {"answer": "Only src/app.ts", "wasFreeform": True}
        return create_attributed_result(answer, PERSON_ANSWERED) if labeled else answer

    session._register_user_input_handler(handler)

    reply = await client._handle_user_input_request(
        {"sessionId": session.session_id, "question": "Which files?", "requestId": "input-1"}
    )

    expected: dict = {"answer": "Only src/app.ts", "wasFreeform": True}
    if labeled:
        expected["decisionContext"] = PERSON_ANSWERED_ON_WIRE
    assert _on_wire(reply) == expected


@LABELED
async def test_exit_plan_mode_reply_carries_decision_context_at_top_level(labeled: bool) -> None:
    client, session = _client_with_session()

    def handler(request: ExitPlanModeRequest, invocation: dict[str, str]) -> ExitPlanModeResult:
        review: ExitPlanModeResult = {"approved": True, "selectedAction": "interactive"}
        return create_attributed_result(review, PERSON_ANSWERED) if labeled else review

    session._register_exit_plan_mode_handler(handler)

    reply = await client._handle_exit_plan_mode_request(
        {
            "sessionId": session.session_id,
            "summary": "Review the plan",
            "actions": ["interactive", "autopilot"],
            "recommendedAction": "autopilot",
            "requestId": "plan-1",
        }
    )

    expected: dict = {"approved": True, "selectedAction": "interactive"}
    if labeled:
        expected["decisionContext"] = PERSON_ANSWERED_ON_WIRE
    assert _on_wire(reply) == expected


@LABELED
async def test_elicitation_answer_sends_decision_context_beside_result(labeled: bool) -> None:
    rpc_client = Mock(request=AsyncMock(return_value={"success": True}))
    session = CopilotSession("session-1", rpc_client)

    def handler(context: ElicitationContext) -> ElicitationHandlerResult:
        answer: ElicitationHandlerResult = {"action": "accept", "content": {"color": "blue"}}
        return create_attributed_result(answer, PERSON_ANSWERED) if labeled else answer

    session._register_elicitation_handler(handler)

    await session._handle_elicitation_request(
        {"session_id": session.session_id, "message": "Pick a color"}, "elicitation-1"
    )

    expected: dict = {
        "sessionId": "session-1",
        "requestId": "elicitation-1",
        "result": {"action": "accept", "content": {"color": "blue"}},
    }
    if labeled:
        expected["decisionContext"] = PERSON_ANSWERED_ON_WIRE
    rpc_client.request.assert_awaited_once_with("session.ui.handlePendingElicitation", expected)
