"""
Mode = "empty" support: ToolSet builder, BUILTIN_TOOLS_ISOLATED, and helpers
that translate Mode = "empty" into runtime-level session options.

The runtime is mode-agnostic; the SDK is what turns ``mode="empty"`` into the
right combination of options on the wire (no environment_context, telemetry
off, custom instructions off, etc.). Callers can opt back in field-by-field.
"""

from __future__ import annotations

import re
from collections.abc import Iterable
from typing import Any, Literal

CopilotClientMode = Literal["copilot-cli", "empty"]

_TOOL_NAME_REGEX = re.compile(r"^[a-zA-Z0-9_-]+$")


def _validate_tool_name(kind: str, name: str) -> None:
    if not name:
        raise ValueError(f"invalid {kind} tool name: must not be empty")
    if name == "*":
        return
    if not _TOOL_NAME_REGEX.match(name):
        raise ValueError(
            f"invalid {kind} tool name {name!r}: tool names must match "
            r"/^[a-zA-Z0-9_-]+$/ or be the wildcard '*'"
        )


class ToolSet:
    """Builder for source-qualified tool filter patterns.

    ``ToolSet`` accumulates entries like ``builtin:bash``, ``mcp:*``, or
    ``custom:my_tool`` for use in
    :class:`CopilotClient.create_session`'s ``available_tools`` /
    ``excluded_tools`` parameters.

    Tool classification (``builtin``/``mcp``/``custom``) is determined by the
    runtime at registration time — not by name parsing — so
    ``add_builtin("foo")`` only matches tools the runtime registered as
    built-in.
    """

    def __init__(self) -> None:
        self._items: list[str] = []

    def add_builtin(self, name: str | Iterable[str]) -> ToolSet:
        """Add a built-in tool pattern (``"bash"``/``"*"``/an iterable of names)."""
        if isinstance(name, str):
            _validate_tool_name("builtin", name)
            self._items.append(f"builtin:{name}")
        else:
            for n in name:
                _validate_tool_name("builtin", n)
                self._items.append(f"builtin:{n}")
        return self

    def add_custom(self, name: str) -> ToolSet:
        """Add a custom-tool pattern (e.g. ``"my_tool"`` or ``"*"``)."""
        _validate_tool_name("custom", name)
        self._items.append(f"custom:{name}")
        return self

    def add_mcp(self, tool_name: str) -> ToolSet:
        """Add an MCP tool pattern (e.g. ``"github-list_issues"`` or ``"*"``)."""
        _validate_tool_name("mcp", tool_name)
        self._items.append(f"mcp:{tool_name}")
        return self

    def to_list(self) -> list[str]:
        """Return a defensive copy of the accumulated filter strings."""
        return list(self._items)

    def __iter__(self):
        return iter(self.to_list())

    def __len__(self) -> int:
        return len(self._items)


#: Built-in tools that operate only within a single session — no host FS
#: access outside the session, no cross-session state, no host environment
#: access, no network. Safe to enable in ``mode="empty"`` scenarios without
#: leaking host capabilities.
#:
#: Contract: tools in this set MUST NOT be extended (even behind options or
#: args) to read or write state outside the session boundary. Adding
#: cross-session or host-state behavior to one of these tools is a breaking
#: change that requires removing it from this set.
BUILTIN_TOOLS_ISOLATED: list[str] = [
    "ask_user",
    "task_complete",
    "exit_plan_mode",
    "task",
    "read_agent",
    "write_agent",
    "list_agents",
    "send_inbox",
    "context_board",
    "skill",
]


def _normalize_tool_filter(value: Any) -> list[str] | None:
    """Accept ``ToolSet``, ``list[str]``, or ``None``; return a list or ``None``.

    Reject plain ``str`` explicitly — ``list("foo")`` would silently shred it
    into characters, sending an invalid tool filter list on the wire.
    """
    if value is None:
        return None
    if isinstance(value, ToolSet):
        return value.to_list()
    if isinstance(value, str):
        raise TypeError(
            "tool filter must be a ToolSet or list[str], not str. "
            'Pass a single-element list (e.g. ["builtin:bash"]) or a '
            "ToolSet (e.g. ToolSet().add_builtin('bash'))."
        )
    return list(value)


def _validate_tool_filter_list(field: str, items: list[str] | None) -> None:
    """Reject bare ``"*"`` entries (must use ``builtin:*``/``mcp:*``/``custom:*``)."""
    if items is None:
        return
    for entry in items:
        if entry == "*":
            raise ValueError(
                f"invalid {field} entry '*': there is no bare wildcard. "
                "Use ToolSet().add_builtin('*'), .add_mcp('*'), or "
                ".add_custom('*') to target a specific source."
            )


def _system_message_for_mode(
    mode: CopilotClientMode | None,
    supplied: Any,
) -> Any:
    """Apply empty-mode environment_context stripping to a system message dict.

    The caller passes the already-normalized wire payload (a ``dict`` with
    ``mode`` / ``content`` / ``sections``) or ``None``. The caller's value
    wins if it already specifies an ``environment_context`` override.
    """
    if mode != "empty":
        return supplied
    remove_action = {"action": "remove"}
    if supplied is None:
        return {"mode": "customize", "sections": {"environment_context": remove_action}}
    supplied_mode = supplied.get("mode", "")
    if supplied_mode == "replace":
        return supplied
    if supplied_mode == "customize":
        sections = supplied.get("sections") or {}
        if "environment_context" in sections:
            return supplied
        merged = {**supplied, "sections": {**sections, "environment_context": remove_action}}
        return merged
    # append (or unspecified): promote to customize so we can also strip
    # environment_context. The runtime appends ``content`` in both modes, so
    # the caller's text is preserved verbatim.
    out: dict[str, Any] = {
        "mode": "customize",
        "sections": {"environment_context": remove_action},
    }
    if "content" in supplied and supplied["content"] is not None:
        out["content"] = supplied["content"]
    return out


def _empty_mode_bool_default(
    mode: CopilotClientMode | None,
    supplied: bool | None,
    empty_default: bool,
) -> bool | None:
    if mode == "empty" and supplied is None:
        return empty_default
    return supplied


def _enable_session_telemetry_default(
    mode: CopilotClientMode | None,
    supplied: bool | None,
) -> bool | None:
    """Empty mode defaults telemetry to False; caller value wins."""
    return _empty_mode_bool_default(mode, supplied, False)


def _skip_embedding_retrieval_default(
    mode: CopilotClientMode | None,
    supplied: bool | None,
) -> bool | None:
    """Empty mode defaults embedding retrieval to off; caller value wins."""
    return _empty_mode_bool_default(mode, supplied, True)


def _embedding_cache_storage_default(
    mode: CopilotClientMode | None,
    supplied: Literal["persistent", "in-memory"] | None,
) -> Literal["persistent", "in-memory"] | None:
    """Empty mode defaults embedding cache storage to in-memory; caller value wins."""
    if mode == "empty" and supplied is None:
        return "in-memory"
    return supplied


def _enable_on_demand_instruction_discovery_default(
    mode: CopilotClientMode | None,
    supplied: bool | None,
) -> bool | None:
    """Empty mode defaults on-demand instruction discovery to False."""
    return _empty_mode_bool_default(mode, supplied, False)


def _enable_file_hooks_default(
    mode: CopilotClientMode | None,
    supplied: bool | None,
) -> bool | None:
    """Empty mode defaults file hooks to False; caller value wins."""
    return _empty_mode_bool_default(mode, supplied, False)


def _enable_host_git_operations_default(
    mode: CopilotClientMode | None,
    supplied: bool | None,
) -> bool | None:
    """Empty mode defaults host git operations to False; caller value wins."""
    return _empty_mode_bool_default(mode, supplied, False)


def _enable_session_store_default(
    mode: CopilotClientMode | None,
    supplied: bool | None,
) -> bool | None:
    """Empty mode defaults the session store to False; caller value wins."""
    return _empty_mode_bool_default(mode, supplied, False)


def _enable_skills_default(
    mode: CopilotClientMode | None,
    supplied: bool | None,
) -> bool | None:
    """Empty mode defaults skills to False; caller value wins."""
    return _empty_mode_bool_default(mode, supplied, False)


def _mcp_oauth_token_storage_default(
    mode: CopilotClientMode | None,
    supplied: Literal["persistent", "in-memory"] | None,
) -> Literal["persistent", "in-memory"] | None:
    """Empty mode defaults MCP OAuth token storage to in-memory; caller value wins."""
    if mode == "empty" and supplied is None:
        return "in-memory"
    return supplied


def _post_create_options_patch(
    mode: CopilotClientMode | None,
    skip_custom_instructions: bool | None,
    custom_agents_local_only: bool | None,
    coauthor_enabled: bool | None,
    manage_schedule_enabled: bool | None,
) -> dict[str, Any] | None:
    """Build the patch sent via ``session.options.update`` after create/resume.

    In empty mode the four overridable flags default to safe values
    (caller-supplied values win); ``installedPlugins=[]`` is unconditional.
    Returns ``None`` if no patch should be sent.
    """
    if mode == "empty":
        patch: dict[str, Any] = {
            "skipCustomInstructions": (
                skip_custom_instructions if skip_custom_instructions is not None else True
            ),
            "customAgentsLocalOnly": (
                custom_agents_local_only if custom_agents_local_only is not None else True
            ),
            "coauthorEnabled": coauthor_enabled if coauthor_enabled is not None else False,
            "manageScheduleEnabled": (
                manage_schedule_enabled if manage_schedule_enabled is not None else False
            ),
            "installedPlugins": [],
        }
        return patch
    patch = {}
    if skip_custom_instructions is not None:
        patch["skipCustomInstructions"] = skip_custom_instructions
    if custom_agents_local_only is not None:
        patch["customAgentsLocalOnly"] = custom_agents_local_only
    if coauthor_enabled is not None:
        patch["coauthorEnabled"] = coauthor_enabled
    if manage_schedule_enabled is not None:
        patch["manageScheduleEnabled"] = manage_schedule_enabled
    return patch or None


def _require_storage_for_empty_mode(
    *,
    mode: CopilotClientMode | None,
    base_directory: str | None,
    session_fs_set: bool,
    is_uri_connection: bool,
) -> None:
    if mode != "empty":
        return
    if base_directory or session_fs_set or is_uri_connection:
        return
    raise ValueError(
        "CopilotClient(mode='empty') requires base_directory, session_fs, "
        "or a UriRuntimeConnection. Empty mode needs explicit per-tenant "
        "storage and won't fall back to ~/.copilot."
    )


def _require_available_tools_for_empty_mode(
    mode: CopilotClientMode | None,
    available_tools: list[str] | None,
) -> None:
    if mode == "empty" and available_tools is None:
        raise ValueError(
            "CopilotClient is in mode='empty' but create_session was called "
            "without available_tools. Empty mode requires every session to "
            "explicitly opt into the tools it wants — e.g. "
            "ToolSet().add_builtin(BUILTIN_TOOLS_ISOLATED)."
        )
