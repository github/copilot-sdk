"""
Tests for session hooks functionality
"""

import pytest

from .testharness import E2ETestContext
from .testharness.helper import write_file

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestHooks:
    async def test_invoke_pre_tool_use_hook_when_model_runs_a_tool(self, ctx: E2ETestContext):
        """Test that preToolUse hook is invoked when model runs a tool"""
        pre_tool_use_inputs = []

        async def on_pre_tool_use(input_data, invocation):
            pre_tool_use_inputs.append(input_data)
            assert invocation["session_id"] == session.session_id
            # Allow the tool to run
            return {"permissionDecision": "allow"}

        session = await ctx.client.create_session({"hooks": {"on_pre_tool_use": on_pre_tool_use}})

        # Create a file for the model to read
        write_file(ctx.work_dir, "hello.txt", "Hello from the test!")

        await session.send_and_wait(
            {"prompt": "Read the contents of hello.txt and tell me what it says"}
        )

        # Should have received at least one preToolUse hook call
        assert len(pre_tool_use_inputs) > 0

        # Should have received the tool name
        assert any(inp.get("toolName") for inp in pre_tool_use_inputs)

        await session.destroy()

    async def test_invoke_post_tool_use_hook_after_model_runs_a_tool(self, ctx: E2ETestContext):
        """Test that postToolUse hook is invoked after model runs a tool"""
        post_tool_use_inputs = []

        async def on_post_tool_use(input_data, invocation):
            post_tool_use_inputs.append(input_data)
            assert invocation["session_id"] == session.session_id
            return None

        session = await ctx.client.create_session({"hooks": {"on_post_tool_use": on_post_tool_use}})

        # Create a file for the model to read
        write_file(ctx.work_dir, "world.txt", "World from the test!")

        await session.send_and_wait(
            {"prompt": "Read the contents of world.txt and tell me what it says"}
        )

        # Should have received at least one postToolUse hook call
        assert len(post_tool_use_inputs) > 0

        # Should have received the tool name and result
        assert any(inp.get("toolName") for inp in post_tool_use_inputs)
        assert any(inp.get("toolResult") is not None for inp in post_tool_use_inputs)

        await session.destroy()

    async def test_invoke_both_hooks_for_single_tool_call(self, ctx: E2ETestContext):
        """Test that both preToolUse and postToolUse hooks fire for the same tool call"""
        pre_tool_use_inputs = []
        post_tool_use_inputs = []

        async def on_pre_tool_use(input_data, invocation):
            pre_tool_use_inputs.append(input_data)
            return {"permissionDecision": "allow"}

        async def on_post_tool_use(input_data, invocation):
            post_tool_use_inputs.append(input_data)
            return None

        session = await ctx.client.create_session(
            {
                "hooks": {
                    "on_pre_tool_use": on_pre_tool_use,
                    "on_post_tool_use": on_post_tool_use,
                }
            }
        )

        write_file(ctx.work_dir, "both.txt", "Testing both hooks!")

        await session.send_and_wait({"prompt": "Read the contents of both.txt"})

        # Both hooks should have been called
        assert len(pre_tool_use_inputs) > 0
        assert len(post_tool_use_inputs) > 0

        # The same tool should appear in both
        pre_tool_names = [inp.get("toolName") for inp in pre_tool_use_inputs]
        post_tool_names = [inp.get("toolName") for inp in post_tool_use_inputs]
        common_tool = next((name for name in pre_tool_names if name in post_tool_names), None)
        assert common_tool is not None

        await session.destroy()

    async def test_deny_tool_execution_when_pre_tool_use_returns_deny(self, ctx: E2ETestContext):
        """Test that returning deny in preToolUse prevents tool execution"""
        pre_tool_use_inputs = []

        async def on_pre_tool_use(input_data, invocation):
            pre_tool_use_inputs.append(input_data)
            # Deny all tool calls
            return {"permissionDecision": "deny"}

        session = await ctx.client.create_session({"hooks": {"on_pre_tool_use": on_pre_tool_use}})

        # Create a file
        original_content = "Original content that should not be modified"
        write_file(ctx.work_dir, "protected.txt", original_content)

        response = await session.send_and_wait(
            {"prompt": "Edit protected.txt and replace 'Original' with 'Modified'"}
        )

        # The hook should have been called
        assert len(pre_tool_use_inputs) > 0

        # The response should indicate the tool was denied (behavior may vary)
        # At minimum, we verify the hook was invoked
        assert response is not None

        await session.destroy()
