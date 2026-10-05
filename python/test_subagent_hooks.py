# Copyright (c) Microsoft Corporation. All rights reserved.

from datetime import UTC, datetime

import pytest

from copilot import (
    SessionHooks,
    SubagentStartHookInput,
    SubagentStartHookOutput,
    SubagentStopHookInput,
    SubagentStopHookOutput,
)
from copilot.session import CopilotSession


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
