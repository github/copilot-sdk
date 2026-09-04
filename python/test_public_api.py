"""Tests for the Python SDK's public module boundary.

These guard the intent documented in ``copilot/_generated/__init__.py``:
the code-generated implementation package is private, and the documented
``copilot.rpc`` / ``copilot.session_events`` shims are the supported access
points. See https://github.com/github/copilot-sdk/issues/2048.
"""

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


def test_public_rpc_surface_excludes_internal_types():
    """Types annotated ``visibility: "internal"`` must not leak through
    ``copilot.rpc``, even when the internal type is a synthesized RPC
    method params/result type rather than a named schema definition.
    """
    for name in rpc.__all__:
        assert not name.startswith("_"), f"{name} should not be exported publicly"

    # Regression coverage for a codegen bug where internal-marked method
    # params/result types (e.g. the internal MCP config RPCs) were annotated
    # "# Internal" in the generated source but still leaked into `__all__`
    # because the export-list builder didn't check the internal-type set.
    from copilot._generated import rpc as _generated_rpc

    assert not hasattr(rpc, "MCPConfigureGitHubRequest")
    assert "MCPConfigureGitHubRequest" not in rpc.__all__
    assert hasattr(_generated_rpc, "_MCPConfigureGitHubRequest")


def test_public_session_events_surface_excludes_internal_types():
    for name in session_events.__all__:
        assert not name.startswith("_"), f"{name} should not be exported publicly"
