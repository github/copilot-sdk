"""E2E Session Tests"""

import pytest

from copilot import CopilotClient
from copilot.types import Tool

from .testharness import E2ETestContext, get_final_assistant_message, get_next_event_of_type

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestSessions:
    async def test_should_create_and_destroy_sessions(self, ctx: E2ETestContext):
        session = await ctx.client.create_session({"model": "fake-test-model"})
        assert session.session_id

        messages = await session.get_messages()
        assert len(messages) > 0
        assert messages[0].type.value == "session.start"
        assert messages[0].data.session_id == session.session_id
        assert messages[0].data.selected_model == "fake-test-model"

        await session.destroy()

        with pytest.raises(Exception, match="Session not found"):
            await session.get_messages()

    async def test_should_have_stateful_conversation(self, ctx: E2ETestContext):
        session = await ctx.client.create_session()

        assistant_message = await session.send_and_wait({"prompt": "What is 1+1?"})
        assert assistant_message is not None
        assert "2" in assistant_message.data.content

        second_message = await session.send_and_wait(
            {"prompt": "Now if you double that, what do you get?"}
        )
        assert second_message is not None
        assert "4" in second_message.data.content

    async def test_should_create_a_session_with_appended_systemMessage_config(
        self, ctx: E2ETestContext
    ):
        system_message_suffix = "End each response with the phrase 'Have a nice day!'"
        session = await ctx.client.create_session(
            {"system_message": {"mode": "append", "content": system_message_suffix}}
        )

        await session.send({"prompt": "What is your full name?"})
        assistant_message = await get_final_assistant_message(session)
        assert "GitHub" in assistant_message.data.content
        assert "Have a nice day!" in assistant_message.data.content

        # Also validate the underlying traffic
        traffic = await ctx.get_exchanges()
        system_message = _get_system_message(traffic[0])
        assert "GitHub" in system_message
        assert system_message_suffix in system_message

    async def test_should_create_a_session_with_replaced_systemMessage_config(
        self, ctx: E2ETestContext
    ):
        test_system_message = "You are an assistant called Testy McTestface. Reply succinctly."
        session = await ctx.client.create_session(
            {"system_message": {"mode": "replace", "content": test_system_message}}
        )

        await session.send({"prompt": "What is your full name?"})
        assistant_message = await get_final_assistant_message(session)
        assert "GitHub" not in assistant_message.data.content
        assert "Testy" in assistant_message.data.content

        # Also validate the underlying traffic
        traffic = await ctx.get_exchanges()
        system_message = _get_system_message(traffic[0])
        assert system_message == test_system_message  # Exact match

    async def test_should_create_a_session_with_availableTools(self, ctx: E2ETestContext):
        session = await ctx.client.create_session({"available_tools": ["view", "edit"]})

        await session.send({"prompt": "What is 1+1?"})
        await get_final_assistant_message(session)

        # It only tells the model about the specified tools and no others
        traffic = await ctx.get_exchanges()
        tools = traffic[0]["request"]["tools"]
        tool_names = [t["function"]["name"] for t in tools]
        assert len(tool_names) == 2
        assert "view" in tool_names
        assert "edit" in tool_names

    async def test_should_create_a_session_with_excludedTools(self, ctx: E2ETestContext):
        session = await ctx.client.create_session({"excluded_tools": ["view"]})

        await session.send({"prompt": "What is 1+1?"})
        await get_final_assistant_message(session)

        # It has other tools, but not the one we excluded
        traffic = await ctx.get_exchanges()
        tools = traffic[0]["request"]["tools"]
        tool_names = [t["function"]["name"] for t in tools]
        assert "edit" in tool_names
        assert "grep" in tool_names
        assert "view" not in tool_names

    # TODO: This test shows there's a race condition inside client.ts. If createSession
    # is called concurrently and autoStart is on, it may start multiple child processes.
    # This needs to be fixed. Right now it manifests as being unable to delete the temp
    # directories during afterAll even though we stopped all the clients.
    @pytest.mark.skip(reason="Known race condition - see TypeScript test")
    async def test_should_handle_multiple_concurrent_sessions(self, ctx: E2ETestContext):
        import asyncio

        s1, s2, s3 = await asyncio.gather(
            ctx.client.create_session(),
            ctx.client.create_session(),
            ctx.client.create_session(),
        )

        # All sessions should have unique IDs
        session_ids = {s1.session_id, s2.session_id, s3.session_id}
        assert len(session_ids) == 3

        # All are connected
        for s in [s1, s2, s3]:
            messages = await s.get_messages()
            assert len(messages) > 0
            assert messages[0].type.value == "session.start"
            assert messages[0].data.session_id == s.session_id

        # All can be destroyed
        await asyncio.gather(s1.destroy(), s2.destroy(), s3.destroy())
        for s in [s1, s2, s3]:
            with pytest.raises(Exception, match="Session not found"):
                await s.get_messages()

    async def test_should_resume_a_session_using_the_same_client(self, ctx: E2ETestContext):
        # Create initial session
        session1 = await ctx.client.create_session()
        session_id = session1.session_id
        answer = await session1.send_and_wait({"prompt": "What is 1+1?"})
        assert answer is not None
        assert "2" in answer.data.content

        # Resume using the same client
        session2 = await ctx.client.resume_session(session_id)
        assert session2.session_id == session_id
        answer2 = await get_final_assistant_message(session2)
        assert "2" in answer2.data.content

    async def test_should_resume_a_session_using_a_new_client(self, ctx: E2ETestContext):
        # Create initial session
        session1 = await ctx.client.create_session()
        session_id = session1.session_id
        answer = await session1.send_and_wait({"prompt": "What is 1+1?"})
        assert answer is not None
        assert "2" in answer.data.content

        # Resume using a new client
        new_client = CopilotClient(
            {"cli_path": ctx.cli_path, "cwd": ctx.work_dir, "env": ctx.get_env()}
        )

        try:
            session2 = await new_client.resume_session(session_id)
            assert session2.session_id == session_id

            # TODO: There's an inconsistency here. When resuming with a new client,
            # we don't see the session.idle message in the history, which means we
            # can't use get_final_assistant_message.
            messages = await session2.get_messages()
            message_types = [m.type.value for m in messages]
            assert "user.message" in message_types
            assert "session.resume" in message_types
        finally:
            await new_client.force_stop()

    async def test_should_throw_error_resuming_nonexistent_session(self, ctx: E2ETestContext):
        with pytest.raises(Exception):
            await ctx.client.resume_session("non-existent-session-id")

    async def test_should_list_sessions(self, ctx: E2ETestContext):
        import asyncio

        # Create a couple of sessions and send messages to persist them
        session1 = await ctx.client.create_session()
        await session1.send_and_wait({"prompt": "Say hello"})
        session2 = await ctx.client.create_session()
        await session2.send_and_wait({"prompt": "Say goodbye"})

        # Small delay to ensure session files are written to disk
        await asyncio.sleep(0.2)

        # List sessions and verify they're included
        sessions = await ctx.client.list_sessions()
        assert isinstance(sessions, list)

        session_ids = [s.sessionId for s in sessions]
        assert session1.session_id in session_ids
        assert session2.session_id in session_ids

        # Verify session metadata structure
        for session_data in sessions:
            assert hasattr(session_data, "sessionId")
            assert hasattr(session_data, "startTime")
            assert hasattr(session_data, "modifiedTime")
            assert hasattr(session_data, "isRemote")
            # summary is optional
            assert isinstance(session_data.sessionId, str)
            assert isinstance(session_data.startTime, str)
            assert isinstance(session_data.modifiedTime, str)
            assert isinstance(session_data.isRemote, bool)

    async def test_should_delete_session(self, ctx: E2ETestContext):
        import asyncio

        # Create a session and send a message to persist it
        session = await ctx.client.create_session()
        await session.send_and_wait({"prompt": "Hello"})
        session_id = session.session_id

        # Small delay to ensure session file is written to disk
        await asyncio.sleep(0.2)

        # Verify session exists in the list
        sessions = await ctx.client.list_sessions()
        session_ids = [s.sessionId for s in sessions]
        assert session_id in session_ids

        # Delete the session
        await ctx.client.delete_session(session_id)

        # Verify session no longer exists in the list
        sessions_after = await ctx.client.list_sessions()
        session_ids_after = [s.sessionId for s in sessions_after]
        assert session_id not in session_ids_after

        # Verify we cannot resume the deleted session
        with pytest.raises(Exception):
            await ctx.client.resume_session(session_id)

    async def test_should_create_session_with_custom_tool(self, ctx: E2ETestContext):
        # This test uses the low-level Tool() API to show that Pydantic is optional
        def get_secret_number_handler(invocation):
            key = invocation["arguments"].get("key", "")
            return {
                "textResultForLlm": "54321" if key == "ALPHA" else "unknown",
                "resultType": "success",
            }

        session = await ctx.client.create_session(
            {
                "tools": [
                    Tool(
                        name="get_secret_number",
                        description="Gets the secret number",
                        handler=get_secret_number_handler,
                        parameters={
                            "type": "object",
                            "properties": {"key": {"type": "string", "description": "Key"}},
                            "required": ["key"],
                        },
                    )
                ]
            }
        )

        answer = await session.send_and_wait({"prompt": "What is the secret number for key ALPHA?"})
        assert answer is not None
        assert "54321" in answer.data.content

    async def test_should_create_session_with_custom_provider(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            {
                "provider": {
                    "type": "openai",
                    "base_url": "https://api.openai.com/v1",
                    "api_key": "fake-key",
                }
            }
        )
        assert session.session_id

    async def test_should_create_session_with_azure_provider(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            {
                "provider": {
                    "type": "azure",
                    "base_url": "https://my-resource.openai.azure.com",
                    "api_key": "fake-key",
                    "azure": {
                        "api_version": "2024-02-15-preview",
                    },
                }
            }
        )
        assert session.session_id

    async def test_should_resume_session_with_custom_provider(self, ctx: E2ETestContext):
        session = await ctx.client.create_session()
        session_id = session.session_id

        # Resume the session with a provider
        session2 = await ctx.client.resume_session(
            session_id,
            {
                "provider": {
                    "type": "openai",
                    "base_url": "https://api.openai.com/v1",
                    "api_key": "fake-key",
                }
            },
        )

        assert session2.session_id == session_id

    async def test_should_abort_a_session(self, ctx: E2ETestContext):
        import asyncio

        session = await ctx.client.create_session()

        # Set up event listeners BEFORE sending to avoid race conditions
        wait_for_tool_start = asyncio.create_task(
            get_next_event_of_type(session, "tool.execution_start", timeout=60.0)
        )
        wait_for_session_idle = asyncio.create_task(
            get_next_event_of_type(session, "session.idle", timeout=30.0)
        )

        # Send a message that will trigger a long-running shell command
        await session.send(
            {
                "prompt": (
                    "run the shell command 'sleep 100' "
                    "(note this works on both bash and PowerShell)"
                )
            }
        )

        # Wait for the tool to start executing
        _ = await wait_for_tool_start

        # Abort the session while the tool is running
        await session.abort()

        # Wait for session to become idle after abort
        _ = await wait_for_session_idle

        # The session should still be alive and usable after abort
        messages = await session.get_messages()
        assert len(messages) > 0

        # Verify an abort event exists in messages
        abort_events = [m for m in messages if m.type.value == "abort"]
        assert len(abort_events) > 0, "Expected an abort event in messages"

        # We should be able to send another message
        answer = await session.send_and_wait({"prompt": "What is 2+2?"})
        assert "4" in answer.data.content

    async def test_should_receive_streaming_delta_events_when_streaming_is_enabled(
        self, ctx: E2ETestContext
    ):
        import asyncio

        session = await ctx.client.create_session({"streaming": True})

        delta_contents = []
        done_event = asyncio.Event()

        def on_event(event):
            if event.type.value == "assistant.message_delta":
                delta = getattr(event.data, "delta_content", None)
                if delta:
                    delta_contents.append(delta)
            elif event.type.value == "session.idle":
                done_event.set()

        session.on(on_event)

        await session.send({"prompt": "What is 2+2?"})

        # Wait for completion
        try:
            await asyncio.wait_for(done_event.wait(), timeout=60)
        except asyncio.TimeoutError:
            pytest.fail("Timed out waiting for session.idle")

        # Should have received delta events
        assert len(delta_contents) > 0, "Expected to receive delta events"

        # Get the final message to compare
        assistant_message = await get_final_assistant_message(session)

        # Accumulated deltas should equal the final message
        accumulated = "".join(delta_contents)
        assert accumulated == assistant_message.data.content, (
            f"Accumulated deltas don't match final message.\n"
            f"Accumulated: {accumulated!r}\nFinal: {assistant_message.data.content!r}"
        )

        # Final message should contain the answer
        assert "4" in assistant_message.data.content

    async def test_should_pass_streaming_option_to_session_creation(self, ctx: E2ETestContext):
        # Verify that the streaming option is accepted without errors
        session = await ctx.client.create_session({"streaming": True})

        assert session.session_id

        # Session should still work normally
        await session.send({"prompt": "What is 1+1?"})
        assistant_message = await get_final_assistant_message(session)
        assert "2" in assistant_message.data.content

    async def test_should_receive_session_events(self, ctx: E2ETestContext):
        import asyncio

        session = await ctx.client.create_session()
        received_events = []
        idle_event = asyncio.Event()

        def on_event(event):
            received_events.append(event)
            if event.type.value == "session.idle":
                idle_event.set()

        session.on(on_event)

        # Send a message to trigger events
        await session.send({"prompt": "What is 100+200?"})

        # Wait for session to become idle
        try:
            await asyncio.wait_for(idle_event.wait(), timeout=60)
        except asyncio.TimeoutError:
            pytest.fail("Timed out waiting for session.idle")

        # Should have received multiple events
        assert len(received_events) > 0
        event_types = [e.type.value for e in received_events]
        assert "user.message" in event_types
        assert "assistant.message" in event_types
        assert "session.idle" in event_types

        # Verify the assistant response contains the expected answer
        assistant_message = await get_final_assistant_message(session)
        assert "300" in assistant_message.data.content

    async def test_should_create_session_with_custom_config_dir(self, ctx: E2ETestContext):
        import os

        custom_config_dir = os.path.join(ctx.home_dir, "custom-config")
        session = await ctx.client.create_session({"config_dir": custom_config_dir})

        assert session.session_id

        # Session should work normally with custom config dir
        await session.send({"prompt": "What is 1+1?"})
        assistant_message = await get_final_assistant_message(session)
        assert "2" in assistant_message.data.content


def _get_system_message(exchange: dict) -> str:
    messages = exchange.get("request", {}).get("messages", [])
    for msg in messages:
        if msg.get("role") == "system":
            return msg.get("content", "")
    return ""
