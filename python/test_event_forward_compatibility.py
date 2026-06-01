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
    AttachmentGitHubReferenceType,
    Data,
    ElicitationCompletedAction,
    ElicitationRequestedMode,
    ElicitationRequestedSchema,
    PermissionPromptRequestMemory,
    PermissionRequestMemory,
    PermissionRequestMemoryAction,
    SessionEventType,
    SessionTaskCompleteData,
    UserMessageAgentMode,
    session_event_from_dict,
    session_event_to_dict,
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

    def test_known_event_preserves_top_level_agent_id(self):
        """Known events should preserve the top-level sub-agent envelope ID."""
        known_event = {
            "id": str(uuid4()),
            "timestamp": datetime.now().isoformat(),
            "parentId": None,
            "agentId": "agent-1",
            "type": "user.message",
            "data": {"content": "Hello"},
        }

        event = session_event_from_dict(known_event)
        assert event.agent_id == "agent-1"
        assert session_event_to_dict(event)["agentId"] == "agent-1"

    def test_unknown_event_preserves_top_level_agent_id(self):
        """Unknown events should preserve the top-level sub-agent envelope ID."""
        unknown_event = {
            "id": str(uuid4()),
            "timestamp": datetime.now().isoformat(),
            "parentId": None,
            "agentId": "future-agent",
            "type": "session.future_feature_from_server",
            "data": {"key": "value"},
        }

        event = session_event_from_dict(unknown_event)
        assert event.type == SessionEventType.UNKNOWN
        assert event.agent_id == "future-agent"
        serialized = session_event_to_dict(event)
        assert serialized["agentId"] == "future-agent"
        assert serialized["type"] == "session.future_feature_from_server"

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

    def test_explicit_generated_symbols_remain_available(self):
        """Explicit generated helper symbols should remain importable."""
        assert ElicitationCompletedAction.ACCEPT.value == "accept"
        assert UserMessageAgentMode.INTERACTIVE.value == "interactive"
        assert ElicitationRequestedMode.FORM.value == "form"
        assert AttachmentGitHubReferenceType.PR.value == "pr"

        schema = ElicitationRequestedSchema(
            properties={"answer": {"type": "string"}}, type="object"
        )
        assert schema.to_dict()["type"] == "object"

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

    def test_missing_optional_fields_remain_none_after_parsing(self):
        """Generated event models should leave missing optional fields as None.

        Regression test for github/copilot-sdk issues #1139, #1140, and #1141:
        the Python codegen previously baked JSON Schema `default` values into
        ``obj.get(key, default)`` for optional fields, so ``from_dict()`` returned
        the schema default instead of ``None`` and broke ``from_dict(to_dict(x))``
        round-trips for instances where the field was ``None``.
        """
        from copilot.generated.session_events import (
            _load_PermissionPromptRequest,
            _load_PermissionRequest,
        )

        # #1141: PermissionRequest.action defaults to None when missing.
        request = _load_PermissionRequest({"kind": "memory", "fact": "remember this"})
        assert isinstance(request, PermissionRequestMemory)
        assert request.action is None
        assert PermissionRequestMemoryAction.STORE.value == "store"  # sanity

        # #1140: PermissionPromptRequest.action defaults to None when missing.
        prompt_request = _load_PermissionPromptRequest({"kind": "memory", "fact": "remember this"})
        assert isinstance(prompt_request, PermissionPromptRequestMemory)
        assert prompt_request.action is None

        # #1139: SessionTaskCompleteData.summary defaults to None when missing.
        task_complete = SessionTaskCompleteData.from_dict({"success": True})
        assert task_complete.summary is None

        # Explicit JSON null should also map to None.
        task_complete_null = SessionTaskCompleteData.from_dict({"success": True, "summary": None})
        assert task_complete_null.summary is None

    def test_optional_fields_round_trip_none(self):
        """``from_dict(to_dict(x))`` should equal ``x`` when optional fields are None.

        Regression test for github/copilot-sdk issues #1139, #1140, and #1141.
        """
        # #1139: SessionTaskCompleteData round-trip with summary=None.
        task = SessionTaskCompleteData(success=None, summary=None)
        assert SessionTaskCompleteData.from_dict(task.to_dict()) == task

        # #1140: PermissionPromptRequestMemory round-trip with action=None.
        prompt = PermissionPromptRequestMemory(fact="test-fact")
        assert prompt.action is None
        assert "action" not in prompt.to_dict()
        assert PermissionPromptRequestMemory.from_dict(prompt.to_dict()) == prompt

        # #1141: PermissionRequestMemory round-trip with action=None.
        permission = PermissionRequestMemory(fact="test-fact")
        assert permission.action is None
        assert "action" not in permission.to_dict()
        assert PermissionRequestMemory.from_dict(permission.to_dict()) == permission

        # PermissionRequest is now a discriminated union; the dispatch loader
        # should round-trip via the correct variant class.
        from copilot.generated.session_events import _load_PermissionRequest

        round_tripped = _load_PermissionRequest(permission.to_dict())
        assert isinstance(round_tripped, PermissionRequestMemory)
        assert round_tripped == permission
        # PermissionPromptRequest likewise.
        from copilot.generated.session_events import _load_PermissionPromptRequest

        round_tripped_prompt = _load_PermissionPromptRequest(prompt.to_dict())
        assert isinstance(round_tripped_prompt, PermissionPromptRequestMemory)
        assert round_tripped_prompt == prompt
