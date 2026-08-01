"""Tests for the Python SDK's public module boundary."""

from importlib.util import find_spec

from copilot import rpc, session_events


def test_generated_implementation_package_is_private():
    """The implementation package follows Python's private-name convention."""
    assert find_spec("copilot._generated") is not None
    assert find_spec("copilot.generated") is None


def test_generated_types_remain_available_from_public_modules():
    """Renaming the implementation package preserves the documented exports."""
    assert "SessionUpdateOptionsParams" in rpc.__all__
    assert "AssistantMessageData" in session_events.__all__
