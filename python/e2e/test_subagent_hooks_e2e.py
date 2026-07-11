"""
Tests for sub-agent hooks functionality — verifies preToolUse/postToolUse hooks
fire for tool calls made by sub-agents spawned via the task tool.
"""

from __future__ import annotations

import os

import httpx
import pytest

from copilot import CopilotRequestContext, CopilotRequestHandler
from copilot.client import CopilotClient, RuntimeConnection
from copilot.session import PermissionHandler

from .testharness import E2ETestContext
from .testharness.helper import write_file

pytestmark = pytest.mark.asyncio(loop_scope="module")


class _RecordingRequestHandler(CopilotRequestHandler):
    def __init__(self) -> None:
        self.records: list[dict[str, str | None]] = []

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
    async def test_should_invoke_pretooluse_and_posttooluse_hooks_for_sub_agent_tool_calls(
        self, ctx: E2ETestContext
    ):
        """Test that preToolUse/postToolUse hooks fire for sub-agent tool calls"""
        hook_log = []
        request_handler = _RecordingRequestHandler()

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
            return None

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

        session = await client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            hooks={
                "on_pre_tool_use": on_pre_tool_use,
                "on_post_tool_use": on_post_tool_use,
            },
        )

        # Create a file for the sub-agent to read
        write_file(ctx.work_dir, "subagent-test.txt", "Hello from subagent test!")

        await session.send_and_wait(
            "Use the task tool to spawn an explore agent that reads the file "
            "subagent-test.txt in the current directory and reports its contents. "
            "You must use the task tool."
        )

        # Parent tool hooks fire for "task"
        task_pre = [h for h in hook_log if h["kind"] == "pre" and h["toolName"] == "task"]
        assert len(task_pre) >= 1, "preToolUse should fire for the parent's 'task' tool call"

        # Sub-agent tool hooks fire for "view"
        view_pre = [h for h in hook_log if h["kind"] == "pre" and h["toolName"] == "view"]
        view_post = [h for h in hook_log if h["kind"] == "post" and h["toolName"] == "view"]
        assert len(view_pre) > 0, "preToolUse should fire for the sub-agent's 'view' tool call"
        assert len(view_post) > 0, "postToolUse should fire for the sub-agent's 'view' tool call"

        # input.session_id distinguishes parent from sub-agent
        assert view_pre[0]["sessionId"] != task_pre[0]["sessionId"], (
            "Sub-agent tool hooks should have a different sessionId than parent tool hooks"
        )
        _assert_subagent_request_metadata(request_handler.records)

        await session.disconnect()
        await client.stop()
