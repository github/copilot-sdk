"""
Tests for sub-agent hooks — verifies preToolUse/postToolUse and
subagentStart/subagentStop for sub-agents spawned via the task tool.
"""

from __future__ import annotations

import asyncio
import json
import os
import sys
from datetime import UTC, datetime
from uuid import uuid4

import httpx
import pytest

from copilot import (
    CopilotRequestContext,
    CopilotRequestHandler,
    SubagentStartHookInput,
    SubagentStartHookOutput,
    SubagentStopHookInput,
    SubagentStopHookOutput,
)
from copilot.client import CopilotClient, RuntimeConnection
from copilot.session import PermissionHandler

from .testharness import E2ETestContext
from .testharness.helper import write_file

pytestmark = pytest.mark.asyncio(loop_scope="module")

CHILD_CONTEXT = "Subagent start hook verified: read the requested file."
STOP_RESPONSE_PREFIX = "Subagent stop hook verified: "


class _RecordingRequestHandler(CopilotRequestHandler):
    def __init__(self) -> None:
        self.records: list[dict[str, str | None]] = []
        self.child_prompts: list[str] = []
        self.modified_parent_request = asyncio.Event()

    async def send_request(
        self, request: httpx.Request, ctx: CopilotRequestContext
    ) -> httpx.Response:
        self.records.append(
            {
                "url": str(request.url),
                "agent_id": ctx.agent_id,
                "parent_agent_id": ctx.parent_agent_id,
                "interaction_type": ctx.interaction_type,
            }
        )
        if _is_inference_url(str(request.url)):
            body = json.loads(request.content)
            if ctx.parent_agent_id:
                for message in body.get("messages", []):
                    if message["role"] != "user":
                        continue
                    content = message.get("content")
                    parts = [content] if isinstance(content, str) else content
                    if isinstance(parts, list):
                        for part in parts:
                            if isinstance(part, str):
                                text = part
                            elif isinstance(part, dict):
                                text = part.get("text")
                            else:
                                continue
                            if isinstance(text, str) and f"{CHILD_CONTEXT}\n\n" in text:
                                self.child_prompts.append(text)
            elif STOP_RESPONSE_PREFIX in json.dumps(body):
                self.modified_parent_request.set()
        return await super().send_request(request, ctx)


def _is_inference_url(url: str) -> bool:
    u = url.lower()
    return (
        u.endswith("/chat/completions")
        or u.endswith("/responses")
        or u.endswith("/v1/messages")
        or u.endswith("/messages")
    )


def _assert_subagent_request_metadata(records: list[dict[str, str | None]]) -> None:
    inference = [r for r in records if _is_inference_url(r["url"] or "")]
    assert len(inference) > 0, "request handler should observe inference requests"

    subagent_request = next((r for r in inference if r["parent_agent_id"]), None)
    assert subagent_request is not None, (
        "sub-agent inference request should carry a parent_agent_id"
    )
    assert subagent_request["agent_id"], "sub-agent inference request should carry an agent_id"
    assert subagent_request["interaction_type"], (
        "sub-agent inference request should carry an interaction_type"
    )
    assert subagent_request["parent_agent_id"] != subagent_request["agent_id"]


class TestSubagentHooks:
    async def test_should_apply_subagent_lifecycle_hook_outputs(self, ctx: E2ETestContext):
        """Test tool and lifecycle hooks for a real subagent under the replay proxy."""
        hook_log = []
        start_calls: list[tuple[SubagentStartHookInput, dict[str, str]]] = []
        stop_calls: list[tuple[SubagentStopHookInput, dict[str, str]]] = []
        subagent_stopped = asyncio.Event()
        request_handler = _RecordingRequestHandler()
        waiting_text = (
            "I've launched an explore agent to read subagent-test.txt. "
            "Waiting for it to complete..."
        )
        parent_waiting = asyncio.Event()
        parent_session_id = str(uuid4())
        final_text = (
            "The explore agent successfully read the file. "
            "The contents of **subagent-test.txt** are:\n\n"
            "```\nHello from subagent test!\n```"
        )

        async def on_pre_tool_use(input_data, invocation):
            hook_log.append(
                {
                    "kind": "pre",
                    "toolName": input_data.get("toolName"),
                    "sessionId": input_data.get("sessionId"),
                }
            )
            return {"permissionDecision": "allow"}

        async def on_post_tool_use(input_data, invocation):
            hook_log.append(
                {
                    "kind": "post",
                    "toolName": input_data.get("toolName"),
                    "sessionId": input_data.get("sessionId"),
                }
            )
            # A fast child can inject its result before the fixture's waiting reply is requested.
            if (
                input_data.get("toolName") == "view"
                and input_data.get("sessionId") != parent_session_id
            ):
                await parent_waiting.wait()
            return None

        async def on_subagent_start(
            input_data: SubagentStartHookInput, invocation: dict[str, str]
        ) -> SubagentStartHookOutput:
            start_calls.append((input_data, invocation))
            return {"additionalContext": CHILD_CONTEXT}

        async def on_subagent_stop(
            input_data: SubagentStopHookInput, invocation: dict[str, str]
        ) -> SubagentStopHookOutput:
            stop_calls.append((input_data, invocation))
            subagent_stopped.set()
            return {"modifiedResponse": f"{STOP_RESPONSE_PREFIX}{input_data['response']}"}

        # Create a client with the session-based subagents feature flag
        env = ctx.get_env()
        env["COPILOT_EXP_COPILOT_CLI_SESSION_BASED_SUBAGENTS"] = "true"
        github_token = (
            "fake-token-for-e2e-tests" if os.environ.get("GITHUB_ACTIONS") == "true" else None
        )
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=ctx.cli_path),
            working_directory=ctx.work_dir,
            env=env,
            github_token=github_token,
            request_handler=request_handler,
        )

        try:
            session = await client.create_session(
                session_id=parent_session_id,
                on_permission_request=PermissionHandler.approve_all,
                hooks={
                    "on_pre_tool_use": on_pre_tool_use,
                    "on_post_tool_use": on_post_tool_use,
                    "on_subagent_start": on_subagent_start,
                    "on_subagent_stop": on_subagent_stop,
                },
            )
            try:
                write_file(ctx.work_dir, "subagent-test.txt", "Hello from subagent test!")

                def on_event(event):
                    if (
                        not event.agent_id
                        and event.type.value == "assistant.message"
                        and event.data.content == waiting_text
                    ):
                        parent_waiting.set()

                unsubscribe = session.on(on_event)
                try:
                    response = await session.send_and_wait(
                        "Use the task tool to spawn an explore agent that reads the file "
                        "subagent-test.txt in the current directory and reports its contents. "
                        "You must use the task tool."
                    )
                    assert response is not None and not response.agent_id
                    assert response.data.content == final_text
                    replies = [
                        event.data.content
                        for event in await session.get_events()
                        if not event.agent_id
                        and event.type.value == "assistant.message"
                        and event.data.content in (waiting_text, final_text)
                    ]
                    assert replies == [waiting_text, final_text]
                    await asyncio.wait_for(subagent_stopped.wait(), 120)
                finally:
                    parent_waiting.set()
                    unsubscribe()

                assert len(start_calls) == 1, f"unexpected subagent starts: {start_calls}"
                assert stop_calls, "subagentStop should fire after the explore agent finishes"
                started, start_invocation = start_calls[0]
                stopped, stop_invocation = stop_calls[-1]
                assert start_invocation == stop_invocation == {"session_id": session.session_id}
                assert started["sessionId"] == stopped["sessionId"] == session.session_id
                assert stopped["timestamp"] >= started["timestamp"]
                assert started["agentName"] == stopped["agentName"] == "explore"
                assert stopped["agentType"] == "explore"
                assert stopped["agentId"]
                assert stopped["stopReason"] == "end_turn"
                assert "Hello from subagent test!" in stopped["response"]
                for input_data in (started, stopped):
                    assert isinstance(input_data["timestamp"], datetime)
                    assert input_data["timestamp"].tzinfo == UTC
                    assert os.path.normcase(
                        os.path.normpath(input_data["workingDirectory"])
                    ) == os.path.normcase(os.path.normpath(ctx.work_dir))
                    assert isinstance(input_data["transcriptPath"], str)
                    assert input_data.get("agentDisplayName") is None
                    assert input_data.get("agentDescription") is None

                task_pre = [h for h in hook_log if h["kind"] == "pre" and h["toolName"] == "task"]
                assert task_pre, "preToolUse should fire for the parent's 'task' tool call"

                view_pre = [h for h in hook_log if h["kind"] == "pre" and h["toolName"] == "view"]
                view_post = [h for h in hook_log if h["kind"] == "post" and h["toolName"] == "view"]
                assert view_pre, "preToolUse should fire for the sub-agent's 'view' tool call"
                assert view_post, "postToolUse should fire for the sub-agent's 'view' tool call"
                assert view_pre[0]["sessionId"] != task_pre[0]["sessionId"], (
                    "Sub-agent tool hooks should have a different sessionId than parent tool hooks"
                )
                _assert_subagent_request_metadata(request_handler.records)
                assert any(
                    f'{CHILD_CONTEXT}\n\nRead the file "subagent-test.txt"' in prompt
                    for prompt in request_handler.child_prompts
                ), "start context must be prepended to the child's model prompt"
                await asyncio.wait_for(request_handler.modified_parent_request.wait(), 120)
            finally:
                first_error = sys.exception()
                try:
                    await session.disconnect()
                except BaseException as cleanup_error:
                    if first_error is None:
                        raise
                    first_error.add_note(f"session.disconnect failed: {cleanup_error!r}")
        finally:
            first_error = sys.exception()
            try:
                await client.stop()
            except BaseException as cleanup_error:
                if first_error is None:
                    raise
                first_error.add_note(f"client.stop failed: {cleanup_error!r}")
