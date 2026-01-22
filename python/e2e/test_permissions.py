"""
Tests for permission callback functionality
"""

import asyncio

import pytest

from copilot import PermissionRequest, PermissionRequestResult

from .testharness import E2ETestContext
from .testharness.helper import read_file, write_file

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestPermissions:
    async def test_permission_handler_for_write_operations(self, ctx: E2ETestContext):
        """Test that permission handler is invoked for write operations"""
        permission_requests = []

        def on_permission_request(
            request: PermissionRequest, invocation: dict
        ) -> PermissionRequestResult:
            permission_requests.append(request)
            assert invocation["session_id"] == session.session_id
            # Approve the permission
            return {"kind": "approved"}

        session = await ctx.client.create_session({"on_permission_request": on_permission_request})

        write_file(ctx.work_dir, "test.txt", "original content")

        await session.send_and_wait(
            {"prompt": "Edit test.txt and replace 'original' with 'modified'"}
        )

        # Should have received at least one permission request
        assert len(permission_requests) > 0

        # Should include write permission request
        write_requests = [req for req in permission_requests if req.get("kind") == "write"]
        assert len(write_requests) > 0

        await session.destroy()

    async def test_permission_handler_for_shell_commands(self, ctx: E2ETestContext):
        """Test that permission handler is invoked for shell commands"""
        permission_requests = []

        def on_permission_request(
            request: PermissionRequest, invocation: dict
        ) -> PermissionRequestResult:
            permission_requests.append(request)
            # Approve the permission
            return {"kind": "approved"}

        session = await ctx.client.create_session({"on_permission_request": on_permission_request})

        await session.send_and_wait({"prompt": "Run 'echo hello' and tell me the output"})

        # Should have received at least one shell permission request
        shell_requests = [req for req in permission_requests if req.get("kind") == "shell"]
        assert len(shell_requests) > 0

        await session.destroy()

    async def test_deny_permission(self, ctx: E2ETestContext):
        """Test denying permissions"""

        def on_permission_request(
            request: PermissionRequest, invocation: dict
        ) -> PermissionRequestResult:
            # Deny all permissions
            return {"kind": "denied-interactively-by-user"}

        session = await ctx.client.create_session({"on_permission_request": on_permission_request})

        original_content = "protected content"
        write_file(ctx.work_dir, "protected.txt", original_content)

        await session.send_and_wait(
            {"prompt": "Edit protected.txt and replace 'protected' with 'hacked'."}
        )

        # Verify the file was NOT modified
        content = read_file(ctx.work_dir, "protected.txt")
        assert content == original_content

        await session.destroy()

    async def test_without_permission_handler(self, ctx: E2ETestContext):
        """Test that sessions work without permission handler (default behavior)"""
        # Create session without on_permission_request handler
        session = await ctx.client.create_session()

        message = await session.send_and_wait({"prompt": "What is 2+2?"})

        assert message is not None
        assert "4" in message.data.content

        await session.destroy()

    async def test_async_permission_handler(self, ctx: E2ETestContext):
        """Test async permission handler"""
        permission_requests = []

        async def on_permission_request(
            request: PermissionRequest, invocation: dict
        ) -> PermissionRequestResult:
            permission_requests.append(request)
            # Simulate async permission check (e.g., user prompt)
            await asyncio.sleep(0.01)
            return {"kind": "approved"}

        session = await ctx.client.create_session({"on_permission_request": on_permission_request})

        await session.send_and_wait({"prompt": "Run 'echo test' and tell me what happens"})

        assert len(permission_requests) > 0

        await session.destroy()

    async def test_resume_session_with_permission_handler(self, ctx: E2ETestContext):
        """Test resuming session with permission handler"""
        permission_requests = []

        # Create session without permission handler
        session1 = await ctx.client.create_session()
        session_id = session1.session_id
        await session1.send_and_wait({"prompt": "What is 1+1?"})

        # Resume with permission handler
        def on_permission_request(
            request: PermissionRequest, invocation: dict
        ) -> PermissionRequestResult:
            permission_requests.append(request)
            return {"kind": "approved"}

        session2 = await ctx.client.resume_session(
            session_id, {"on_permission_request": on_permission_request}
        )

        await session2.send_and_wait({"prompt": "Run 'echo resumed' for me"})

        # Should have permission requests from resumed session
        assert len(permission_requests) > 0

        await session2.destroy()

    async def test_permission_handler_errors(self, ctx: E2ETestContext):
        """Test that permission handler errors are handled gracefully"""

        def on_permission_request(
            request: PermissionRequest, invocation: dict
        ) -> PermissionRequestResult:
            raise RuntimeError("Handler error")

        session = await ctx.client.create_session({"on_permission_request": on_permission_request})

        message = await session.send_and_wait(
            {"prompt": "Run 'echo test'. If you can't, say 'failed'."}
        )

        # Should handle the error and deny permission
        assert message is not None
        content_lower = message.data.content.lower()
        assert any(word in content_lower for word in ["fail", "cannot", "unable", "permission"])

        await session.destroy()

    async def test_tool_call_id_in_permission_requests(self, ctx: E2ETestContext):
        """Test that toolCallId is included in permission requests"""
        received_tool_call_id = False

        def on_permission_request(
            request: PermissionRequest, invocation: dict
        ) -> PermissionRequestResult:
            nonlocal received_tool_call_id
            if request.get("toolCallId"):
                received_tool_call_id = True
                assert isinstance(request["toolCallId"], str)
                assert len(request["toolCallId"]) > 0
            return {"kind": "approved"}

        session = await ctx.client.create_session({"on_permission_request": on_permission_request})

        await session.send_and_wait({"prompt": "Run 'echo test'"})

        assert received_tool_call_id

        await session.destroy()
