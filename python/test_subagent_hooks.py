# Copyright (c) Microsoft Corporation. All rights reserved.

from datetime import UTC, datetime
from typing import NotRequired, get_origin, get_type_hints

import pytest

from copilot import (
    SessionHooks,
    SubagentStartHookInput,
    SubagentStartHookOutput,
    SubagentStopHookInput,
    SubagentStopHookOutput,
)
from copilot import session as session_types
from copilot.session import CopilotSession


@pytest.mark.asyncio
@pytest.mark.parametrize(
    ("hook", "handler", "input_type"),
    [
        ("preToolUse", "on_pre_tool_use", "PreToolUseHookInput"),
        ("preMcpToolCall", "on_pre_mcp_tool_call", "PreMcpToolCallHookInput"),
        ("postToolUse", "on_post_tool_use", "PostToolUseHookInput"),
        ("postToolUseFailure", "on_post_tool_use_failure", "PostToolUseFailureHookInput"),
        ("userPromptSubmitted", "on_user_prompt_submitted", "UserPromptSubmittedHookInput"),
        ("userPromptTransformed", "on_user_prompt_transformed", "UserPromptTransformedHookInput"),
        ("sessionStart", "on_session_start", "SessionStartHookInput"),
        ("sessionEnd", "on_session_end", "SessionEndHookInput"),
        ("errorOccurred", "on_error_occurred", "ErrorOccurredHookInput"),
        ("agentStop", "on_agent_stop", "AgentStopHookInput"),
        ("subagentStart", "on_subagent_start", "SubagentStartHookInput"),
        ("subagentStop", "on_subagent_stop", "SubagentStopHookInput"),
    ],
)
@pytest.mark.parametrize("enriched", [True, False])
async def test_typed_hook_identity_retention_and_optional_declarations(
    hook, handler, input_type, enriched
):
    captured = []
    session = CopilotSession("callback-owner", None)
    session._register_hooks({handler: lambda data, invocation: captured.append((data, invocation))})
    payload = {"sessionId": "reported-session", "timestamp": 1, "cwd": "work"}
    identity = {"agentId": "child-actor", "agentType": "explore", "parentAgentId": "parent-actor"}
    if enriched:
        payload.update(identity)
    await session._handle_hooks_invoke(hook, payload)
    assert len(captured) == 1
    data, invocation = captured[0]
    assert invocation == {"session_id": "callback-owner"}
    assert data["sessionId"] == "reported-session"
    declaration = getattr(session_types, input_type)
    for key, value in identity.items():
        assert data.get(key) == (value if enriched else None)
        assert key in declaration.__annotations__
        if not (hook == "subagentStop" and key == "agentType"):
            assert get_origin(get_type_hints(declaration, include_extras=True)[key]) is NotRequired


@pytest.mark.asyncio
async def test_subagent_start_dispatches_normalized_input_and_context():
    captured: list[tuple[SubagentStartHookInput, dict[str, str]]] = []

    async def on_start(
        input_data: SubagentStartHookInput, invocation: dict[str, str]
    ) -> SubagentStartHookOutput:
        captured.append((input_data, invocation))
        return {"additionalContext": "Report the entire file"}

    hooks: SessionHooks = {"on_subagent_start": on_start}
    session = CopilotSession("parent", None)
    session._register_hooks(hooks)

    output = await session._handle_hooks_invoke(
        "subagentStart",
        {
            "sessionId": "parent",
            "timestamp": 1_700_000_000_000,
            "cwd": "work",
            "transcriptPath": "transcript.jsonl",
            "agentName": "explore",
            "agentDisplayName": "Explorer",
            "agentDescription": "Reads files",
        },
    )

    assert output == {"additionalContext": "Report the entire file"}
    assert captured == [
        (
            {
                "sessionId": "parent",
                "timestamp": datetime.fromtimestamp(1_700_000_000, tz=UTC),
                "workingDirectory": "work",
                "transcriptPath": "transcript.jsonl",
                "agentName": "explore",
                "agentDisplayName": "Explorer",
                "agentDescription": "Reads files",
            },
            {"session_id": "parent"},
        )
    ]


@pytest.mark.asyncio
@pytest.mark.parametrize(
    "hook_output",
    [
        {"decision": "block", "reason": "Read the rest of the file"},
        {"modifiedResponse": "Verified: Hello from subagent test!"},
        {"decision": "allow", "modifiedResponse": "Verified: Hello from subagent test!"},
    ],
)
async def test_subagent_stop_dispatches_normalized_input_and_verdict(
    hook_output: SubagentStopHookOutput,
):
    captured: list[tuple[SubagentStopHookInput, dict[str, str]]] = []

    def on_stop(
        input_data: SubagentStopHookInput, invocation: dict[str, str]
    ) -> SubagentStopHookOutput:
        captured.append((input_data, invocation))
        return hook_output

    hooks: SessionHooks = {"on_subagent_stop": on_stop}
    session = CopilotSession("parent", None)
    session._register_hooks(hooks)

    output = await session._handle_hooks_invoke(
        "subagentStop",
        {
            "sessionId": "parent",
            "timestamp": 1_700_000_000_000,
            "cwd": "work",
            "transcriptPath": "transcript.jsonl",
            "agentName": "explore",
            "agentType": "explore",
            "stopReason": "end_turn",
            "response": "Hello from subagent test!",
        },
    )

    assert output == hook_output
    assert captured == [
        (
            {
                "sessionId": "parent",
                "timestamp": datetime.fromtimestamp(1_700_000_000, tz=UTC),
                "workingDirectory": "work",
                "transcriptPath": "transcript.jsonl",
                "agentName": "explore",
                "agentType": "explore",
                "stopReason": "end_turn",
                "response": "Hello from subagent test!",
            },
            {"session_id": "parent"},
        )
    ]


@pytest.mark.asyncio
async def test_subagent_stop_without_handler_returns_none():
    session = CopilotSession("parent", None)
    session._register_hooks({"on_subagent_start": lambda _input, _invocation: None})

    assert await session._handle_hooks_invoke("subagentStop", {}) is None


@pytest.mark.asyncio
async def test_subagent_start_without_handler_returns_none():
    session = CopilotSession("parent", None)
    session._register_hooks({"on_subagent_stop": lambda _input, _invocation: None})

    assert await session._handle_hooks_invoke("subagentStart", {}) is None
