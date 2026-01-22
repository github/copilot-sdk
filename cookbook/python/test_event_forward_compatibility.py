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

from copilot.generated.session_events import SessionEventType, session_event_from_dict


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
