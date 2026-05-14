"""
Tests for sub-agent hooks functionality — verifies preToolUse/postToolUse hooks
fire for tool calls made by sub-agents spawned via the task tool.
"""

import os

import pytest

from copilot.client import CopilotClient, SubprocessConfig
from copilot.session import PermissionHandler

from .testharness import E2ETestContext
from .testharness.helper import write_file

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestSubagentHooks:
    async def test_should_invoke_pretooluse_and_posttooluse_hooks_for_sub_agent_tool_calls(
        self, ctx: E2ETestContext
    ):
        """Test that preToolUse/postToolUse hooks fire for sub-agent tool calls"""
        hook_log = []

        async def on_pre_tool_use(input_data, invocation):
            hook_log.append({
                "kind": "pre",
                "toolName": input_data.get("toolName"),
                "sessionId": input_data.get("sessionId"),
            })
            return {"permissionDecision": "allow"}

        async def on_post_tool_use(input_data, invocation):
            hook_log.append({
                "kind": "post",
                "toolName": input_data.get("toolName"),
                "sessionId": input_data.get("sessionId"),
            })
            return None

        # Create a client with the session-based subagents feature flag
        env = ctx.get_env()
        env["COPILOT_EXP_COPILOT_CLI_SESSION_BASED_SUBAGENTS"] = "true"
        github_token = (
            "fake-token-for-e2e-tests" if os.environ.get("GITHUB_ACTIONS") == "true" else None
        )
        client = CopilotClient(
            SubprocessConfig(
                cli_path=ctx.cli_path,
                cwd=ctx.work_dir,
                env=env,
                github_token=github_token,
            )
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

        # input.sessionId distinguishes parent from sub-agent
        assert view_pre[0]["sessionId"] != task_pre[0]["sessionId"], (
            "Sub-agent tool hooks should have a different sessionId than parent tool hooks"
        )

        await session.disconnect()
        await client.stop()
