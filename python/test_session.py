"""CopilotSession unit tests."""

import asyncio
from dataclasses import FrozenInstanceError
from datetime import UTC, datetime
from unittest.mock import AsyncMock, Mock
from uuid import uuid4

import pytest

from _session_test_helpers import get_next_event_of_type, wait_for_event
from copilot import AgentMessageSource, MessageSource
from copilot._jsonrpc import JsonRpcError
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


def _external_tool_request(tool_name: str, request_id: str = "request-1") -> SessionEvent:
    return _event(
        ExternalToolRequestedData(
            request_id=request_id,
            session_id="session-1",
            tool_call_id=f"{request_id}-tool-call",
            tool_name=tool_name,
            arguments={},
        ),
        SessionEventType.EXTERNAL_TOOL_REQUESTED,
    )


async def _wait_for_tool_result(client: Mock, request_id: str) -> dict:
    for _ in range(20):
        for call in client.request.await_args_list:
            if (
                call.args[0] == "session.tools.handlePendingToolCall"
                and call.args[1]["requestId"] == request_id
            ):
                return call.args[1]
        await asyncio.sleep(0)
    raise AssertionError(f"tool result was not sent for {request_id}")


@pytest.mark.parametrize("use_send_and_wait", [False, True])
@pytest.mark.asyncio
async def test_completion_captures_live_idle_before_send_reply_without_idle_in_history(
    use_send_and_wait,
):
    client = Mock()
    session = CopilotSession("session-1", client)
    intermediate = _event(
        AssistantMessageData(content="working", message_id="assistant-1"),
        SessionEventType.ASSISTANT_MESSAGE,
    )
    assistant = _event(
        AssistantMessageData(content="done", message_id="assistant-2"),
        SessionEventType.ASSISTANT_MESSAGE,
    )
    idle = _event(SessionIdleData(), SessionEventType.SESSION_IDLE)
    idle.ephemeral = True
    history = [intermediate, assistant]
    received = []
    unsubscribe = session.on(received.append)

    async def respond(method, params):
        assert params["sessionId"] == session.session_id
        if method == "session.getMessages":
            return {"events": [event.to_dict() for event in history]}
        assert method == "session.send"
        # No suspension: even a create_task(async_waiter()) cannot subscribe in time.
        session._dispatch_event(intermediate)
        session._dispatch_event(assistant)
        session._dispatch_event(idle)
        return {"messageId": "message-1"}

    client.request = AsyncMock(side_effect=respond)
    try:
        if use_send_and_wait:
            message = await session.send_and_wait("hello", timeout=1)
            assert message is not None
            assert message is assistant
        else:
            idle_task = get_next_event_of_type(session, "session.idle", timeout=1)
            try:
                assert await session.send("hello") == "message-1"
                assert received == [intermediate, assistant, idle]
                assert await idle_task is idle
                messages = [
                    event for event in received if event.type == SessionEventType.ASSISTANT_MESSAGE
                ]
                assert messages[-1] is assistant
            finally:
                idle_task.cancel()
                await asyncio.gather(idle_task, return_exceptions=True)

        client.request.assert_awaited_once()
        persisted = await session.get_events()
        assert [event.type for event in persisted] == [
            SessionEventType.ASSISTANT_MESSAGE,
            SessionEventType.ASSISTANT_MESSAGE,
        ]
        assert persisted[-1].data.content == "done"
    finally:
        unsubscribe()


@pytest.mark.asyncio
async def test_event_waiters_capture_abort_and_recovery_before_rpc_replies():
    client = Mock()
    session = CopilotSession("session-1", client)
    idle = _event(SessionIdleData(), SessionEventType.SESSION_IDLE)
    assistant = _event(
        AssistantMessageData(content="recovered", message_id="assistant-1"),
        SessionEventType.ASSISTANT_MESSAGE,
    )

    async def respond(method, params):
        if method == "session.abort":
            session._dispatch_event(idle)
            return {}
        assert method == "session.send"
        session._dispatch_event(assistant)
        session._dispatch_event(idle)
        return {"messageId": "message-1"}

    client.request = AsyncMock(side_effect=respond)
    aborted = get_next_event_of_type(session, "session.idle", timeout=1)
    try:
        await session.abort()
        assert await aborted is idle
    finally:
        aborted.cancel()
        await asyncio.gather(aborted, return_exceptions=True)

    recovery = wait_for_event(
        session,
        lambda event: (
            isinstance(event.data, AssistantMessageData) and event.data.content == "recovered"
        ),
        timeout=1,
    )
    recovered_idle = get_next_event_of_type(session, "session.idle", timeout=1)
    try:
        await session.send("recover")
        assert await recovery is assistant
        assert await recovered_idle is idle
    finally:
        recovery.cancel()
        recovered_idle.cancel()
        await asyncio.gather(recovery, recovered_idle, return_exceptions=True)


@pytest.mark.parametrize(
    "outcome", ["success", "error", "timeout", "cancel-before-start", "cancel-after-start"]
)
@pytest.mark.asyncio
async def test_event_waiter_unsubscribes_for_every_outcome(outcome):
    session = Mock(spec=CopilotSession)
    unsubscribe = session.on.return_value
    waiter = get_next_event_of_type(
        session, "session.idle", timeout=0 if outcome == "timeout" else 1
    )
    session.on.assert_called_once()
    on_event = session.on.call_args.args[0]

    if outcome == "success":
        idle = _event(SessionIdleData(), SessionEventType.SESSION_IDLE)
        on_event(idle)
        on_event(idle)
        assert await waiter is idle
    elif outcome == "error":
        on_event(
            _event(
                SessionErrorData(error_type="notification", message="turn failed"),
                SessionEventType.SESSION_ERROR,
            )
        )
        with pytest.raises(RuntimeError, match="turn failed"):
            await waiter
    elif outcome == "timeout":
        with pytest.raises(TimeoutError):
            await waiter
    else:
        if outcome == "cancel-after-start":
            loop = asyncio.get_running_loop()
            started = loop.create_future()
            loop.call_soon(started.set_result, None)
            await started
        waiter.cancel()
        with pytest.raises(asyncio.CancelledError):
            await waiter

    unsubscribe.assert_called_once()


@pytest.mark.parametrize("fail_on_session_error", [None, False, True])
@pytest.mark.asyncio
async def test_event_waiter_preserves_caller_error_policy(fail_on_session_error):
    session = Mock(spec=CopilotSession)
    options = (
        {} if fail_on_session_error is None else {"fail_on_session_error": fail_on_session_error}
    )
    waiter = wait_for_event(
        session, lambda event: isinstance(event.data, SessionIdleData), timeout=1, **options
    )
    on_event = session.on.call_args.args[0]
    on_event(
        _event(
            SessionErrorData(error_type="rate_limit", message="rate limited"),
            SessionEventType.SESSION_ERROR,
        )
    )
    idle = _event(SessionIdleData(), SessionEventType.SESSION_IDLE)
    on_event(idle)

    if fail_on_session_error:
        with pytest.raises(RuntimeError, match="rate limited"):
            await waiter
    else:
        assert await waiter is idle
    session.on.return_value.assert_called_once()


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
async def test_send_and_wait_ignores_child_events():
    client = Mock()
    sent = asyncio.Event()

    async def respond(method, params):
        assert method == "session.send"
        sent.set()
        return {"messageId": "message-1"}

    client.request = AsyncMock(side_effect=respond)
    session = CopilotSession("session-1", client)
    received = []
    session.on(received.append)
    pending = asyncio.create_task(session.send_and_wait("delegate", timeout=1))
    try:
        await asyncio.wait_for(sent.wait(), timeout=1)

        child_message = _event(
            AssistantMessageData(content="child reply", message_id="child-message"),
            SessionEventType.ASSISTANT_MESSAGE,
        )
        child_message.agent_id = "child-1"
        child_error = _event(
            SessionErrorData(error_type="query", message="child failed"),
            SessionEventType.SESSION_ERROR,
        )
        child_error.agent_id = "child-1"
        child_idle = _event(SessionIdleData(), SessionEventType.SESSION_IDLE)
        child_idle.agent_id = "child-1"
        for event in (child_message, child_error, child_idle):
            session._dispatch_event(event)
        assert received == [child_message, child_error, child_idle]
        await asyncio.sleep(0)
        assert not pending.done()

        session._dispatch_event(_event(SessionIdleData(), SessionEventType.SESSION_IDLE))
        assert await asyncio.wait_for(pending, timeout=1) is None
    finally:
        pending.cancel()
        await asyncio.gather(pending, return_exceptions=True)


@pytest.mark.asyncio
async def test_set_tools_sends_complete_wire_payload_and_installs_handlers_on_success():
    client = Mock()
    client.request = AsyncMock(return_value={})
    session = CopilotSession("session-1", client)

    async def handled(_invocation):
        return ToolResult(text_result_for_llm="new result")

    await session.set_tools(
        [
            Tool(
                name="new_tool",
                description="",
                parameters={"type": "object"},
                handler=handled,
                overrides_built_in_tool=True,
                skip_permission=True,
                defer="never",
                metadata={"owner": "test"},
                is_terminal=True,
            ),
            Tool(name="declaration_only", description=None),  # type: ignore[arg-type]
        ]
    )

    client.request.assert_awaited_once_with(
        "session.tools.set",
        {
            "sessionId": "session-1",
            "tools": [
                {
                    "description": "",
                    "name": "new_tool",
                    "defer": "never",
                    "isTerminal": True,
                    "metadata": {"owner": "test"},
                    "overridesBuiltInTool": True,
                    "parameters": {"type": "object"},
                    "skipPermission": True,
                },
                {"description": "", "name": "declaration_only"},
            ],
        },
    )

    assert session._get_tool_handler("new_tool") is handled
    assert session._get_tool_handler("declaration_only") is None


@pytest.mark.asyncio
async def test_set_tools_keeps_previous_handlers_while_rpc_is_pending_then_swaps():
    rpc_started = asyncio.Event()
    release_rpc = asyncio.Event()
    client = Mock()

    async def request(method, params):
        if method == "session.tools.set":
            rpc_started.set()
            await release_rpc.wait()
            return {}
        return {"success": True}

    client.request = AsyncMock(side_effect=request)
    session = CopilotSession("session-1", client)
    calls: list[str] = []

    async def old_handler(_invocation):
        calls.append("old")
        return ToolResult(text_result_for_llm="old result")

    async def new_handler(_invocation):
        calls.append("new")
        return ToolResult(text_result_for_llm="new result")

    session._register_tools([Tool("old_tool", "Old", old_handler)])
    replace = asyncio.create_task(session.set_tools([Tool("new_tool", "New", new_handler)]))
    try:
        await asyncio.wait_for(rpc_started.wait(), timeout=1)
        session._dispatch_event(_external_tool_request("old_tool", "old-request"))
        await _wait_for_tool_result(client, "old-request")
        assert calls == ["old"]
        assert session._get_tool_handler("new_tool") is None

        release_rpc.set()
        await asyncio.wait_for(replace, timeout=1)
        assert session._get_tool_handler("old_tool") is None
        assert session._get_tool_handler("new_tool") is new_handler

        session._dispatch_event(_external_tool_request("new_tool", "new-request"))
        await _wait_for_tool_result(client, "new-request")
        assert calls == ["old", "new"]
    finally:
        release_rpc.set()
        replace.cancel()
        await asyncio.gather(replace, return_exceptions=True)


@pytest.mark.asyncio
async def test_set_tools_rejection_leaves_handlers_unchanged():
    client = Mock()
    client.request = AsyncMock(side_effect=JsonRpcError(-32602, "invalid params"))
    session = CopilotSession("session-1", client)

    async def old_handler(_invocation):
        return ToolResult(text_result_for_llm="old result")

    async def new_handler(_invocation):
        return ToolResult(text_result_for_llm="new result")

    session._register_tools([Tool("old_tool", "Old", old_handler)])

    with pytest.raises(JsonRpcError):
        await session.set_tools([Tool("new_tool", "New", new_handler)])

    assert session._get_tool_handler("old_tool") is old_handler
    assert session._get_tool_handler("new_tool") is None


@pytest.mark.asyncio
async def test_set_tools_empty_list_removes_handlers_after_success():
    client = Mock()
    client.request = AsyncMock(return_value={})
    session = CopilotSession("session-1", client)

    async def old_handler(_invocation):
        return ToolResult(text_result_for_llm="old result")

    session._register_tools([Tool("old_tool", "Old", old_handler)])

    await session.set_tools([])

    client.request.assert_awaited_once_with(
        "session.tools.set", {"sessionId": "session-1", "tools": []}
    )
    assert session._get_tool_handler("old_tool") is None


@pytest.mark.asyncio
async def test_set_tools_serializes_concurrent_calls_and_allows_later_after_failure():
    set_requests: list[dict] = []
    releases = [asyncio.Event(), asyncio.Event()]
    client = Mock()

    async def request(method, params):
        assert method == "session.tools.set"
        index = len(set_requests)
        set_requests.append(params)
        await releases[index].wait()
        if index == 0:
            raise JsonRpcError(-32602, "invalid params")
        return {}

    client.request = AsyncMock(side_effect=request)
    session = CopilotSession("session-1", client)

    async def first_handler(_invocation):
        return ToolResult(text_result_for_llm="first")

    async def second_handler(_invocation):
        return ToolResult(text_result_for_llm="second")

    first = asyncio.create_task(session.set_tools([Tool("first_tool", "First", first_handler)]))
    second = asyncio.create_task(session.set_tools([Tool("second_tool", "Second", second_handler)]))
    try:
        for _ in range(20):
            if len(set_requests) == 1:
                break
            await asyncio.sleep(0)
        assert [request["tools"][0]["name"] for request in set_requests] == ["first_tool"]

        releases[0].set()
        with pytest.raises(JsonRpcError):
            await asyncio.wait_for(first, timeout=1)

        for _ in range(20):
            if len(set_requests) == 2:
                break
            await asyncio.sleep(0)
        assert [request["tools"][0]["name"] for request in set_requests] == [
            "first_tool",
            "second_tool",
        ]

        releases[1].set()
        await asyncio.wait_for(second, timeout=1)
        assert session._get_tool_handler("first_tool") is None
        assert session._get_tool_handler("second_tool") is second_handler
    finally:
        for release in releases:
            release.set()
        first.cancel()
        second.cancel()
        await asyncio.gather(first, second, return_exceptions=True)


@pytest.mark.asyncio
async def test_set_tools_caller_cancellation_after_request_still_installs_accepted_handlers():
    rpc_started = asyncio.Event()
    release_rpc = asyncio.Event()
    client = Mock()

    async def request(method, params):
        assert method == "session.tools.set"
        rpc_started.set()
        await release_rpc.wait()
        return {}

    client.request = AsyncMock(side_effect=request)
    session = CopilotSession("session-1", client)

    async def new_handler(_invocation):
        return ToolResult(text_result_for_llm="new result")

    replace = asyncio.create_task(session.set_tools([Tool("new_tool", "New", new_handler)]))
    try:
        await asyncio.wait_for(rpc_started.wait(), timeout=1)
        replace.cancel()
        with pytest.raises(asyncio.CancelledError):
            await replace

        release_rpc.set()
        for _ in range(20):
            if session._get_tool_handler("new_tool") is new_handler:
                break
            await asyncio.sleep(0)
        assert session._get_tool_handler("new_tool") is new_handler
    finally:
        release_rpc.set()


@pytest.mark.asyncio
async def test_set_tools_cancelled_while_queued_sends_nothing():
    set_requests: list[dict] = []
    release_first = asyncio.Event()
    client = Mock()

    async def request(method, params):
        assert method == "session.tools.set"
        set_requests.append(params)
        await release_first.wait()
        return {}

    client.request = AsyncMock(side_effect=request)
    session = CopilotSession("session-1", client)

    async def first_handler(_invocation):
        return ToolResult(text_result_for_llm="first")

    async def queued_handler(_invocation):
        return ToolResult(text_result_for_llm="queued")

    first = asyncio.create_task(session.set_tools([Tool("first_tool", "First", first_handler)]))
    queued = asyncio.create_task(session.set_tools([Tool("queued_tool", "Queued", queued_handler)]))
    try:
        for _ in range(20):
            if set_requests:
                break
            await asyncio.sleep(0)
        assert len(set_requests) == 1

        queued.cancel()
        with pytest.raises(asyncio.CancelledError):
            await queued

        release_first.set()
        await asyncio.wait_for(first, timeout=1)
        assert [request["tools"][0]["name"] for request in set_requests] == ["first_tool"]
        assert session._get_tool_handler("first_tool") is first_handler
        assert session._get_tool_handler("queued_tool") is None
    finally:
        release_first.set()
        first.cancel()
        queued.cancel()
        await asyncio.gather(first, queued, return_exceptions=True)


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
