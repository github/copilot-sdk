import json

import pytest

from copilot.generated.rpc import QueuePendingItems, SendMessageItem, SendRequest
from copilot.generated.session_events import ToolExecutionStartData, UserMessageData


@pytest.mark.parametrize(
    ("projection", "base"),
    [
        (SendRequest, {"prompt": "hello"}),
        (SendMessageItem, {"prompt": "hello"}),
        (
            QueuePendingItems,
            {
                "id": "queue-1",
                "messageId": "canonical-1",
                "kind": "message",
                "displayText": "hello",
                "agentMode": "interactive",
            },
        ),
        (
            UserMessageData,
            {"content": "hello", "messageId": "canonical-1", "interactionId": "agent-loop-1"},
        ),
    ],
)
@pytest.mark.parametrize(
    "value",
    [
        None,
        "01234567-89ab-4cde-8f01-23456789abcd",
        "01234567-89AB-4CDE-8F01-23456789ABCD",
        "not-a-uuid",
        "",
    ],
)
def test_admission_correlation_optional_round_trip(projection, base, value):
    expected = dict(base)
    if value is not None:
        expected["clientCorrelationId"] = value
    decoded = projection.from_dict(json.loads(json.dumps(expected)))
    assert decoded.client_correlation_id == value
    assert json.loads(json.dumps(decoded.to_dict())) == expected
    future = projection.from_dict({**expected, "futureField": {"enabled": True}})
    assert future.to_dict() == expected
    explicit_null = projection.from_dict({**base, "clientCorrelationId": None})
    assert explicit_null.to_dict() == base


def test_queue_pending_message_id_uses_camel_case_and_is_optional():
    item = QueuePendingItems.from_dict(
        {
            "id": "queue-1",
            "messageId": "message-1",
            "kind": "message",
            "displayText": "hello",
            "agentMode": "interactive",
        }
    )

    assert item.message_id == "message-1"
    assert item.to_dict()["messageId"] == "message-1"

    older_item = QueuePendingItems.from_dict(
        {
            "id": "queue-2",
            "kind": "command",
            "displayText": "/help",
            "agentMode": "interactive",
        }
    )

    assert older_item.message_id is None
    assert "messageId" not in older_item.to_dict()


def test_user_message_id_uses_camel_case_and_is_optional():
    message = UserMessageData.from_dict({"content": "hello", "messageId": "message-1"})

    assert message.message_id == "message-1"
    assert message.to_dict()["messageId"] == "message-1"

    older_message = UserMessageData.from_dict({"content": "hello"})

    assert older_message.message_id is None
    assert "messageId" not in older_message.to_dict()


def test_tool_start_trace_context_is_optional_and_preserved():
    for context in [
        {},
        {
            "traceparent": "00-11111111111111111111111111111111-2222222222222222-01",
            "tracestate": "vendor=value",
        },
        {"traceparent": "00-11111111111111111111111111111111-2222222222222222-00"},
        {"traceparent": "invalid", "tracestate": "invalid"},
        {"traceparent": ""},
    ]:
        wire = {"toolCallId": "tool-call-a", "toolName": "client-tool", **context}
        data = ToolExecutionStartData.from_dict(wire)

        assert data.traceparent == context.get("traceparent")
        assert data.tracestate == context.get("tracestate")
        assert data.to_dict() == wire
