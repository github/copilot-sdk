"""
Test that unknown/malformed session events are handled gracefully.
"""

from datetime import datetime
from uuid import uuid4

import pytest

from copilot import CopilotClient


class TestUnknownEventHandling:
    """Test graceful handling of unknown and malformed session events."""

    def test_event_parsing_with_unknown_type(self):
        """Verify that unknown event types map to UNKNOWN enum value."""
        from copilot.generated.session_events import SessionEventType, session_event_from_dict
        
        unknown_event = {
            "id": str(uuid4()),
            "timestamp": datetime.now().isoformat(),
            "parentId": None,
            "type": "session.completely_new_event_from_future",
            "data": {},
        }
        
        event = session_event_from_dict(unknown_event)
        assert event.type == SessionEventType.UNKNOWN, \
            f"Expected UNKNOWN, got {event.type}"

    def test_malformed_data_raises_exception(self):
        """Malformed data should raise exceptions (caught by handler)."""
        from copilot.generated.session_events import session_event_from_dict
        
        # Bad UUID format
        malformed_event = {
            "id": "not-a-uuid",
            "timestamp": datetime.now().isoformat(),
            "parentId": None,
            "type": "session.start",
            "data": {},
        }
        
        with pytest.raises((ValueError, AssertionError)):
            session_event_from_dict(malformed_event)
        
        # Bad timestamp format
        malformed_event2 = {
            "id": str(uuid4()),
            "timestamp": "invalid-timestamp",
            "parentId": None,
            "type": "session.start",
            "data": {},
        }
        
        with pytest.raises(Exception):
            session_event_from_dict(malformed_event2)

    def test_handler_catches_parsing_exceptions(self):
        """The notification handler should catch and ignore parsing exceptions."""
        from copilot.generated.session_events import session_event_from_dict
        
        events_dispatched = []
        
        def mock_dispatch(event):
            events_dispatched.append(event)
        
        # Test 1: Known event should work
        known_event = {
            "id": str(uuid4()),
            "timestamp": datetime.now().isoformat(),
            "parentId": None,
            "type": "session.start",
            "data": {},
        }
        
        try:
            event = session_event_from_dict(known_event)
            mock_dispatch(event)
        except Exception as e:
            pytest.fail(f"Known event should not raise: {e}")
        
        assert len(events_dispatched) == 1, "Known event should be dispatched"
        
        # Test 2: Unknown event type should use UNKNOWN enum
        unknown_event = {
            "id": str(uuid4()),
            "timestamp": datetime.now().isoformat(),
            "parentId": None,
            "type": "session.future_unknown_event",
            "data": {},
        }
        
        try:
            event = session_event_from_dict(unknown_event)
            mock_dispatch(event)
        except Exception as e:
            pytest.fail(f"Unknown event should not raise: {e}")
        
        assert len(events_dispatched) == 2, "Unknown event should be dispatched with UNKNOWN type"
        
        # Test 3: Malformed event - simulate what the handler does
        malformed_event = {
            "id": "not-a-valid-uuid",
            "timestamp": datetime.now().isoformat(),
            "parentId": None,
            "type": "session.start",
            "data": {},
        }
        
        # This simulates the try-except in the notification handler
        try:
            event = session_event_from_dict(malformed_event)
            mock_dispatch(event)
        except Exception:
            # Handler catches and returns, event not dispatched
            pass
        
        assert len(events_dispatched) == 2, "Malformed event should not be dispatched"

