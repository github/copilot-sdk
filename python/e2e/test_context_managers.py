"""E2E Context Manager Tests"""

import pytest

from copilot import CopilotClient

from .testharness import CLI_PATH

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestCopilotClientContextManager:
    async def test_should_auto_start_and_cleanup_with_context_manager(self, ctx):
        """Test that CopilotClient context manager auto-starts and cleans up."""
        async with ctx.client as client:
            assert client.get_state() == "connected"
            # Verify we can use the client
            pong = await client.ping("test")
            assert pong.message == "pong: test"

        # After exiting context, client should be disconnected
        assert client.get_state() == "disconnected"

    async def test_should_create_session_in_context(self, ctx):
        """Test creating and using a session within client context."""
        async with ctx.client as client:
            session = await client.create_session({"model": "fake-test-model"})
            assert session.session_id

            # Verify session is usable
            messages = await session.get_messages()
            assert len(messages) > 0
            assert messages[0].type.value == "session.start"

        # After exiting context, verify cleanup happened
        assert client.get_state() == "disconnected"

    async def test_should_cleanup_multiple_sessions(self, ctx):
        """Test that all sessions are cleaned up when client context exits."""
        async with ctx.client as client:
            session1 = await client.create_session()
            session2 = await client.create_session()
            session3 = await client.create_session()

            assert session1.session_id
            assert session2.session_id
            assert session3.session_id

        # All sessions should be cleaned up
        assert client.get_state() == "disconnected"

    async def test_should_propagate_exceptions(self, ctx):
        """Test that exceptions within context are propagated."""
        with pytest.raises(ValueError, match="test error"):
            async with ctx.client as client:
                assert client.get_state() == "connected"
                raise ValueError("test error")

        # Client should still be cleaned up even after exception
        assert client.get_state() == "disconnected"

    async def test_should_handle_cleanup_errors_gracefully(self, ctx):
        """Test that cleanup errors don't prevent context from exiting."""
        async with ctx.client as client:
            await client.create_session()

            # Kill the process to force cleanup to fail
            if client._process:
                client._process.kill()

        # Context should still exit successfully despite cleanup errors
        assert client.get_state() == "disconnected"


class TestCopilotSessionContextManager:
    async def test_should_cleanup_session_with_context_manager(self, ctx):
        """Test that CopilotSession context manager cleans up session."""
        async with await ctx.client.create_session() as session:
            assert session.session_id
            # Send a message to verify session is working
            await session.send({"prompt": "Hello!"})

        # After exiting context, session should be destroyed
        with pytest.raises(Exception, match="Session not found"):
            await session.get_messages()

    async def test_should_propagate_exceptions_in_session_context(self, ctx):
        """Test that exceptions within session context are propagated."""
        with pytest.raises(ValueError, match="test session error"):
            async with await ctx.client.create_session() as session:
                assert session.session_id
                raise ValueError("test session error")

        # Session should still be cleaned up after exception
        with pytest.raises(Exception, match="Session not found"):
            await session.get_messages()

    async def test_nested_context_managers(self, ctx):
        """Test using nested context managers for client and session."""
        async with ctx.client as client:
            async with await client.create_session() as session:
                assert session.session_id
                await session.send({"prompt": "Test message"})

            # Session should be cleaned up
            with pytest.raises(Exception, match="Session not found"):
                await session.get_messages()

        # Client should be cleaned up
        assert client.get_state() == "disconnected"

    async def test_multiple_sequential_session_contexts(self, ctx):
        """Test creating multiple sessions sequentially with context managers."""
        async with ctx.client as client:
            # First session
            async with await client.create_session() as session1:
                session1_id = session1.session_id
                await session1.send({"prompt": "First session"})

            # Second session (after first is cleaned up)
            async with await client.create_session() as session2:
                session2_id = session2.session_id
                await session2.send({"prompt": "Second session"})

            # Both sessions should be different
            assert session1_id != session2_id

            # First session should be destroyed
            with pytest.raises(Exception, match="Session not found"):
                await session1.get_messages()

