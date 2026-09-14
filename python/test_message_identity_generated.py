from copilot.generated.rpc import QueuePendingItems
from copilot.generated.session_events import UserMessageData


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
