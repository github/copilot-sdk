"""
Test that unknown event types are handled gracefully for forward compatibility.

This test verifies that:
1. The session.usage_info event type is recognized
2. Unknown future event types map to UNKNOWN enum value
3. Real parsing errors (malformed data) are NOT suppressed and surface for visibility
"""

from datetime import datetime
from uuid import uuid4

import pytest

from copilot.generated.session_events import (
    Action,
    AgentMode,
    ContentElement,
    Data,
    Mode,
    ReferenceType,
    RequestedSchema,
    RequestedSchemaType,
    Resource,
    Result,
    ResultKind,
    SessionEventType,
    session_event_from_dict,
)


class TestEventForwardCompatibility:
    """Test forward compatibility for unknown event types."""

    def test_session_usage_info_is_recognized(self):
        """The session.usage_info event type should be in the enum."""
        assert SessionEventType.SESSION_USAGE_INFO.value == "session.usage_info"

    def test_unknown_event_type_maps_to_unknown(self):
        """Unknown event types should map to UNKNOWN enum value for forward compatibility."""
        unknown_event = {
            "id": str(uuid4()),
            "timestamp": datetime.now().isoformat(),
            "parentId": None,
            "type": "session.future_feature_from_server",
            "data": {},
        }

        event = session_event_from_dict(unknown_event)
        assert event.type == SessionEventType.UNKNOWN, f"Expected UNKNOWN, got {event.type}"

    def test_malformed_uuid_raises_error(self):
        """Malformed UUIDs should raise ValueError for visibility, not be suppressed."""
        malformed_event = {
            "id": "not-a-valid-uuid",
            "timestamp": datetime.now().isoformat(),
            "parentId": None,
            "type": "session.start",
            "data": {},
        }

        # This should raise an error and NOT be silently suppressed
        with pytest.raises(ValueError):
            session_event_from_dict(malformed_event)

    def test_malformed_timestamp_raises_error(self):
        """Malformed timestamps should raise an error for visibility."""
        malformed_event = {
            "id": str(uuid4()),
            "timestamp": "not-a-valid-timestamp",
            "parentId": None,
            "type": "session.start",
            "data": {},
        }

        # This should raise an error and NOT be silently suppressed
        with pytest.raises((ValueError, TypeError)):
            session_event_from_dict(malformed_event)

    def test_legacy_top_level_generated_symbols_remain_available(self):
        """Previously top-level generated helper symbols should remain importable."""
        assert Action.ACCEPT.value == "accept"
        assert AgentMode.INTERACTIVE.value == "interactive"
        assert Mode.FORM.value == "form"
        assert ReferenceType.PR.value == "pr"

        schema = RequestedSchema(
            properties={"answer": {"type": "string"}}, type=RequestedSchemaType.OBJECT
        )
        assert schema.to_dict()["type"] == "object"

        result = Result(
            content="Approved",
            kind=ResultKind.APPROVED,
            contents=[
                ContentElement(
                    type=ContentElement.from_dict({"type": "text", "text": "hello"}).type,
                    text="hello",
                    resource=Resource(uri="file://artifact.txt", text="artifact"),
                )
            ],
        )
        assert result.to_dict() == {
            "content": "Approved",
            "kind": "approved",
            "contents": [
                {
                    "type": "text",
                    "text": "hello",
                    "resource": {
                        "uri": "file://artifact.txt",
                        "text": "artifact",
                    },
                }
            ],
        }

    def test_data_shim_preserves_raw_mapping_values(self):
        """Compatibility Data should keep arbitrary nested mappings as plain dicts."""
        parsed = Data.from_dict(
            {
                "arguments": {"toolCallId": "call-1"},
                "input": {"step_name": "build"},
            }
        )
        assert parsed.arguments == {"toolCallId": "call-1"}
        assert isinstance(parsed.arguments, dict)
        assert parsed.input == {"step_name": "build"}
        assert isinstance(parsed.input, dict)

        constructed = Data(arguments={"tool_call_id": "call-1"})
        assert constructed.to_dict() == {"arguments": {"tool_call_id": "call-1"}}
