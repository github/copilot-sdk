"""CopilotSession unit tests."""

import asyncio
from dataclasses import FrozenInstanceError
from datetime import UTC, datetime
from unittest.mock import AsyncMock, Mock
from uuid import uuid4

import pytest

from copilot import AgentMessageSource, MessageSource
from copilot.session import Attachment, CopilotSession
from copilot.session_events import (
    AssistantMessageData,
    ExternalToolCompletedData,
    ExternalToolRequestedData,
    SessionErrorData,
    SessionEvent,
    SessionEventType,
    SessionIdleData,
    SessionMode,
)
from copilot.tools import Tool, ToolResult

MESSAGE_SOURCE_CASES = [
    pytest.param(None, None, id="omitted"),
    pytest.param("user", "user", id="user"),
    pytest.param("system", "system", id="system"),
    pytest.param(AgentMessageSource("reviewer"), "agent-reviewer", id="agent"),
    pytest.param(AgentMessageSource(""), "agent-", id="empty-agent-id"),
    pytest.param(AgentMessageSource(" Agent/É "), "agent- Agent/É ", id="opaque-agent-id"),
    pytest.param(
        AgentMessageSource("agent-reviewer"), "agent-agent-reviewer", id="prefixed-agent-id"
    ),
]


@pytest.mark.parametrize("agent_id", [None, 42, False, b"reviewer"])
def test_agent_message_source_rejects_non_string_ids(agent_id):
    with pytest.raises(TypeError, match="agent_id must be a string"):
        AgentMessageSource(agent_id)


def test_agent_message_source_is_frozen():
    source = AgentMessageSource("reviewer")
    with pytest.raises(FrozenInstanceError):
        setattr(source, "agent_id", "other")
    assert source.agent_id == "reviewer"


def _event(data, event_type: SessionEventType) -> SessionEvent:
    return SessionEvent(
        data=data,
        id=uuid4(),
        timestamp=datetime.now(UTC),
        type=event_type,
    )


@pytest.mark.asyncio
async def test_send_omits_source_for_plain_human_prompt(monkeypatch):
    monkeypatch.setattr("copilot.session.get_trace_context", lambda: {})
    client = Mock()
    client.request = AsyncMock(return_value={"messageId": "message-1"})
    session = CopilotSession("session-1", client)

    assert await session.send("hello") == "message-1"
    client.request.assert_awaited_once_with(
        "session.send", {"sessionId": "session-1", "prompt": "hello"}
    )


@pytest.mark.parametrize(("source", "wire"), MESSAGE_SOURCE_CASES)
@pytest.mark.parametrize("mode", [None, "enqueue", "immediate"])
@pytest.mark.asyncio
async def test_send_source_is_optional(source: MessageSource | None, wire, mode, monkeypatch):
    monkeypatch.setattr("copilot.session.get_trace_context", lambda: {})
    client = Mock()
    client.request = AsyncMock(return_value={"messageId": "message-1"})
    session = CopilotSession("session-1", client)

    assert await session.send("hello", source=source, mode=mode) == "message-1"
    expected = {"sessionId": "session-1", "prompt": "hello"}
    if source is not None:
        expected["source"] = wire
    if mode is not None:
        expected["mode"] = mode
    client.request.assert_awaited_once_with("session.send", expected)


@pytest.mark.parametrize(("source", "wire"), MESSAGE_SOURCE_CASES)
@pytest.mark.asyncio
async def test_send_source_preserves_other_options(source: MessageSource | None, wire, monkeypatch):
    trace = {
        "traceparent": "00-fedcba0987654321fedcba0987654321-abcdef1234567890-01",
        "tracestate": "vendor=source",
    }
    monkeypatch.setattr("copilot.session.get_trace_context", lambda: trace)
    client = Mock()
    client.request = AsyncMock(return_value={"messageId": "message-1"})
    session = CopilotSession("session-1", client)
    attachments: list[Attachment] = [{"type": "blob", "data": "aGk=", "mimeType": "text/plain"}]

    await session.send(
        "context updated",
        source=source,
        mode="immediate",
        agent_mode="plan",
        attachments=attachments,
        display_prompt="Context updated",
        request_headers={"X-Tag": "context"},
    )

    expected = {
        "sessionId": "session-1",
        "prompt": "context updated",
        "mode": "immediate",
        "agentMode": "plan",
        "attachments": attachments,
        "displayPrompt": "Context updated",
        "requestHeaders": {"X-Tag": "context"},
        **trace,
    }
    if source is not None:
        expected["source"] = wire
    client.request.assert_awaited_once_with("session.send", expected)


@pytest.mark.parametrize(("source", "wire"), MESSAGE_SOURCE_CASES)
@pytest.mark.parametrize("mode", [None, "enqueue", "immediate"])
@pytest.mark.asyncio
async def test_send_and_wait_source_allows_idle_without_assistant(
    source: MessageSource | None, wire, mode, monkeypatch
):
    monkeypatch.setattr("copilot.session.get_trace_context", lambda: {})
    client = Mock()
    session = CopilotSession("session-1", client)

    async def respond(method, params):
        assert method == "session.send"
        expected = {"sessionId": "session-1", "prompt": "context updated"}
        if source is not None:
            expected["source"] = wire
        if mode is not None:
            expected["mode"] = mode
        assert params == expected
        session._dispatch_event(_event(SessionIdleData(), SessionEventType.SESSION_IDLE))
        return {"messageId": "message-1"}

    client.request = AsyncMock(side_effect=respond)
    assert (
        await session.send_and_wait("context updated", source=source, mode=mode, timeout=1) is None
    )


@pytest.mark.parametrize("mode", [None, "enqueue", "immediate"])
@pytest.mark.asyncio
async def test_send_and_wait_forwards_agent_source_and_other_options(mode, monkeypatch):
    trace = {"traceparent": "00-fedcba0987654321fedcba0987654321-abcdef1234567890-01"}
    monkeypatch.setattr("copilot.session.get_trace_context", lambda: trace)
    client = Mock()
    session = CopilotSession("session-1", client)
    source = AgentMessageSource("reviewer")
    attachments: list[Attachment] = [{"type": "blob", "data": "aGk=", "mimeType": "text/plain"}]
    options = {
        "source": source,
        "mode": mode,
        "agent_mode": "plan",
        "attachments": attachments,
        "display_prompt": "Review complete",
        "request_headers": {"X-Tag": "review"},
    }
    assistant = _event(
        AssistantMessageData(content="done", message_id="assistant-1"),
        SessionEventType.ASSISTANT_MESSAGE,
    )

    async def respond(method, params):
        expected = {
            "sessionId": "session-1",
            "prompt": "Review complete",
            "source": "agent-reviewer",
            "agentMode": "plan",
            "attachments": attachments,
            "displayPrompt": "Review complete",
            "requestHeaders": {"X-Tag": "review"},
            **trace,
        }
        if mode is not None:
            expected["mode"] = mode
        assert method == "session.send"
        assert params == expected
        session._dispatch_event(assistant)
        session._dispatch_event(_event(SessionIdleData(), SessionEventType.SESSION_IDLE))
        return {"messageId": "message-1"}

    client.request = AsyncMock(side_effect=respond)
    send = AsyncMock(wraps=session.send)
    monkeypatch.setattr(session, "send", send)

    assert await session.send_and_wait("Review complete", **options, timeout=1) is assistant
    send.assert_awaited_once_with("Review complete", **options)
    assert send.await_args is not None
    assert send.await_args.kwargs["source"] is source


@pytest.mark.parametrize(("source", "wire"), MESSAGE_SOURCE_CASES[2:])
@pytest.mark.parametrize("rpc_error", [True, False])
@pytest.mark.asyncio
async def test_send_and_wait_source_preserves_errors(source, wire, rpc_error):
    client = Mock()
    session = CopilotSession("session-1", client)

    async def respond(method, params):
        assert method == "session.send"
        assert params["source"] == wire
        if rpc_error:
            raise RuntimeError("send failed")
        session._dispatch_event(
            _event(
                SessionErrorData(error_type="notification", message="agent failed"),
                SessionEventType.SESSION_ERROR,
            )
        )
        return {"messageId": "message-1"}

    client.request = AsyncMock(side_effect=respond)
    with pytest.raises(RuntimeError if rpc_error else Exception, match="send failed|agent failed"):
        await session.send_and_wait("context updated", source=source, timeout=1)


@pytest.mark.asyncio
async def test_send_and_wait_skips_autopilot_continuation_idle():
    client = Mock()
    client.request = AsyncMock(return_value={"messageId": "message-1"})
    session = CopilotSession("session-1", client)

    pending = asyncio.create_task(session.send_and_wait("keep going"))
    await asyncio.sleep(0)
    client.request.assert_awaited_once()

    session._dispatch_event(
        _event(
            AssistantMessageData(content="intermediate", message_id="assistant-1"),
            SessionEventType.ASSISTANT_MESSAGE,
        )
    )
    session._dispatch_event(
        _event(
            SessionIdleData(mode=SessionMode.AUTOPILOT),
            SessionEventType.SESSION_IDLE,
        )
    )
    assert not pending.done()

    session._dispatch_event(
        _event(
            AssistantMessageData(content="final", message_id="assistant-2"),
            SessionEventType.ASSISTANT_MESSAGE,
        )
    )
    session._dispatch_event(
        _event(
            SessionIdleData(mode=SessionMode.INTERACTIVE),
            SessionEventType.SESSION_IDLE,
        )
    )

    result = await asyncio.wait_for(pending, timeout=1)
    assert result is not None
    assert isinstance(result.data, AssistantMessageData)
    assert result.data.content == "final"


@pytest.mark.asyncio
async def test_external_tool_completed_cancels_blocked_handler():
    client = Mock()
    client.request = AsyncMock()
    session = CopilotSession("session-1", client)
    started = asyncio.Event()
    cancelled = asyncio.Event()

    async def blocked_tool(_invocation):
        started.set()
        try:
            await asyncio.Future()
        except asyncio.CancelledError:
            cancelled.set()
        return ToolResult(text_result_for_llm="late result")

    session._register_tools([Tool("blocked_tool", "Blocks", blocked_tool)])
    session._dispatch_event(
        _event(
            ExternalToolRequestedData(
                request_id="request-1",
                session_id="session-1",
                tool_call_id="tool-call-1",
                tool_name="blocked_tool",
            ),
            SessionEventType.EXTERNAL_TOOL_REQUESTED,
        )
    )
    await asyncio.wait_for(started.wait(), timeout=1)

    session._dispatch_event(
        _event(
            ExternalToolCompletedData(request_id="request-1"),
            SessionEventType.EXTERNAL_TOOL_COMPLETED,
        )
    )

    await asyncio.wait_for(cancelled.wait(), timeout=1)
    await asyncio.sleep(0)
    client.request.assert_not_awaited()


@pytest.mark.asyncio
async def test_disconnect_from_tool_task_does_not_cancel_detach_request():
    client = Mock()
    client.request = AsyncMock(return_value={"success": True})
    session = CopilotSession("session-1", client)
    current_task = asyncio.current_task()
    assert current_task is not None
    session._pending_external_tools["request-1"] = current_task

    await session.disconnect()

    client.request.assert_awaited_once_with("session.detach", {"sessionId": "session-1"})
