"""Unit tests for the ``ToolSet`` builder and empty-mode helpers."""

from __future__ import annotations

import pytest

from copilot import BUILTIN_TOOLS_ISOLATED, CopilotClient, ToolSet, UriRuntimeConnection
from copilot._mode import (
    _enable_file_hooks_default,
    _enable_host_git_operations_default,
    _enable_on_demand_instruction_discovery_default,
    _enable_session_store_default,
    _enable_session_telemetry_default,
    _enable_skills_default,
    _post_create_options_patch,
    _require_available_tools_for_empty_mode,
    _require_storage_for_empty_mode,
    _skip_embedding_retrieval_default,
    _system_message_for_mode,
    _validate_tool_filter_list,
)


class TestToolSet:
    def test_add_builtin_string(self):
        ts = ToolSet().add_builtin("bash")
        assert ts.to_list() == ["builtin:bash"]

    def test_add_builtin_wildcard(self):
        ts = ToolSet().add_builtin("*")
        assert ts.to_list() == ["builtin:*"]

    def test_add_builtin_iterable(self):
        ts = ToolSet().add_builtin(["bash", "edit"])
        assert ts.to_list() == ["builtin:bash", "builtin:edit"]

    def test_add_builtin_isolated(self):
        ts = ToolSet().add_builtin(BUILTIN_TOOLS_ISOLATED)
        assert ts.to_list() == [f"builtin:{name}" for name in BUILTIN_TOOLS_ISOLATED]

    def test_add_mcp(self):
        ts = ToolSet().add_mcp("github-list_issues")
        assert ts.to_list() == ["mcp:github-list_issues"]

    def test_add_mcp_wildcard(self):
        assert ToolSet().add_mcp("*").to_list() == ["mcp:*"]

    def test_add_custom(self):
        assert ToolSet().add_custom("my_tool").to_list() == ["custom:my_tool"]

    def test_chained(self):
        ts = ToolSet().add_builtin(BUILTIN_TOOLS_ISOLATED).add_mcp("*").add_custom("*")
        assert ts.to_list()[-2:] == ["mcp:*", "custom:*"]

    def test_rejects_bad_name(self):
        with pytest.raises(ValueError, match="tool names must match"):
            ToolSet().add_builtin("has space")

    def test_rejects_empty(self):
        with pytest.raises(ValueError, match="must not be empty"):
            ToolSet().add_custom("")

    def test_rejects_colon(self):
        with pytest.raises(ValueError, match="tool names must match"):
            ToolSet().add_mcp("server:tool")

    def test_iterable_protocol(self):
        ts = ToolSet().add_builtin("bash").add_mcp("*")
        assert list(ts) == ["builtin:bash", "mcp:*"]
        assert len(ts) == 2


class TestEmptyModeValidation:
    def test_empty_mode_requires_storage(self):
        with pytest.raises(ValueError, match="requires base_directory"):
            _require_storage_for_empty_mode(
                mode="empty",
                base_directory=None,
                session_fs_set=False,
                is_uri_connection=False,
            )

    def test_empty_mode_accepts_base_directory(self):
        _require_storage_for_empty_mode(
            mode="empty",
            base_directory="/tmp/x",
            session_fs_set=False,
            is_uri_connection=False,
        )

    def test_empty_mode_accepts_session_fs(self):
        _require_storage_for_empty_mode(
            mode="empty",
            base_directory=None,
            session_fs_set=True,
            is_uri_connection=False,
        )

    def test_empty_mode_accepts_uri_connection(self):
        _require_storage_for_empty_mode(
            mode="empty",
            base_directory=None,
            session_fs_set=False,
            is_uri_connection=True,
        )

    def test_copilot_cli_mode_no_storage_required(self):
        _require_storage_for_empty_mode(
            mode="copilot-cli",
            base_directory=None,
            session_fs_set=False,
            is_uri_connection=False,
        )

    def test_empty_mode_requires_available_tools(self):
        with pytest.raises(ValueError, match="available_tools"):
            _require_available_tools_for_empty_mode("empty", None)

    def test_empty_mode_accepts_available_tools(self):
        _require_available_tools_for_empty_mode("empty", ["builtin:bash"])

    def test_copilot_cli_mode_no_tool_filter_required(self):
        _require_available_tools_for_empty_mode("copilot-cli", None)


class TestToolFilterListValidation:
    def test_rejects_bare_wildcard(self):
        with pytest.raises(ValueError, match="bare wildcard"):
            _validate_tool_filter_list("available_tools", ["*"])

    def test_accepts_source_qualified_wildcard(self):
        _validate_tool_filter_list("available_tools", ["builtin:*", "mcp:*"])

    def test_accepts_none(self):
        _validate_tool_filter_list("available_tools", None)


class TestSystemMessageForMode:
    def test_copilot_cli_pass_through(self):
        assert _system_message_for_mode("copilot-cli", None) is None
        msg = {"mode": "append", "content": "hi"}
        assert _system_message_for_mode("copilot-cli", msg) is msg

    def test_empty_mode_none_supplied(self):
        out = _system_message_for_mode("empty", None)
        assert out == {
            "mode": "customize",
            "sections": {"environment_context": {"action": "remove"}},
        }

    def test_empty_mode_replace_pass_through(self):
        msg = {"mode": "replace", "content": "verbatim"}
        assert _system_message_for_mode("empty", msg) is msg

    def test_empty_mode_customize_adds_section(self):
        msg = {"mode": "customize", "sections": {"identity": {"action": "remove"}}}
        out = _system_message_for_mode("empty", msg)
        assert out["sections"]["environment_context"] == {"action": "remove"}
        assert out["sections"]["identity"] == {"action": "remove"}

    def test_empty_mode_customize_does_not_overwrite_existing(self):
        msg = {
            "mode": "customize",
            "sections": {"environment_context": {"action": "replace", "content": "X"}},
        }
        assert _system_message_for_mode("empty", msg) is msg

    def test_empty_mode_append_promoted_to_customize(self):
        msg = {"mode": "append", "content": "tip"}
        out = _system_message_for_mode("empty", msg)
        assert out["mode"] == "customize"
        assert out["content"] == "tip"
        assert out["sections"]["environment_context"] == {"action": "remove"}


class TestTelemetryDefault:
    def test_empty_mode_defaults_to_false(self):
        assert _enable_session_telemetry_default("empty", None) is False

    def test_empty_mode_caller_wins(self):
        assert _enable_session_telemetry_default("empty", True) is True

    def test_copilot_cli_does_not_change(self):
        assert _enable_session_telemetry_default("copilot-cli", None) is None


class TestPostCreatePatch:
    def test_empty_mode_defaults(self):
        patch = _post_create_options_patch("empty", None, None, None, None)
        assert patch == {
            "skipCustomInstructions": True,
            "customAgentsLocalOnly": True,
            "coauthorEnabled": False,
            "manageScheduleEnabled": False,
            "installedPlugins": [],
        }

    def test_empty_mode_caller_wins(self):
        patch = _post_create_options_patch("empty", False, False, True, True)
        assert patch == {
            "skipCustomInstructions": False,
            "customAgentsLocalOnly": False,
            "coauthorEnabled": True,
            "manageScheduleEnabled": True,
            "installedPlugins": [],
        }

    def test_copilot_cli_returns_none_when_unset(self):
        assert _post_create_options_patch("copilot-cli", None, None, None, None) is None

    def test_copilot_cli_passes_through_explicit_values(self):
        patch = _post_create_options_patch("copilot-cli", True, None, False, None)
        assert patch == {"skipCustomInstructions": True, "coauthorEnabled": False}


class TestClientConstruction:
    def test_empty_mode_without_storage_raises(self):
        with pytest.raises(ValueError, match="requires base_directory"):
            CopilotClient(mode="empty")

    def test_empty_mode_with_base_directory_ok(self, tmp_path):
        # Use URI connection to skip bundled-CLI discovery.
        client = CopilotClient(
            mode="empty",
            base_directory=str(tmp_path),
            connection=UriRuntimeConnection(url="http://localhost:1234"),
        )
        assert client._options.mode == "empty"

    def test_empty_mode_with_uri_connection_ok(self):
        client = CopilotClient(
            mode="empty",
            connection=UriRuntimeConnection(url="http://localhost:1234"),
        )
        assert client._options.mode == "empty"

    def test_default_mode_copilot_cli(self):
        client = CopilotClient(
            connection=UriRuntimeConnection(url="http://localhost:1234"),
        )
        assert client._options.mode == "copilot-cli"
