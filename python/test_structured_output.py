"""Structured-output admission, correlation, and lifecycle tests."""

import asyncio
from datetime import UTC, datetime
from unittest.mock import AsyncMock, Mock
from uuid import uuid4

import pytest
from pydantic import BaseModel, ConfigDict, ValidationError

from copilot.session import CopilotSession
from copilot.session_events import SessionEvent


class Inventory(BaseModel):
    model_config = ConfigDict(extra="forbid")
    count: int
    color: str


def event(kind, data, agent_id=None):
    return SessionEvent.from_dict(
        {
            "id": str(uuid4()),
            "timestamp": datetime.now(UTC).isoformat(),
            "type": kind,
            "data": data,
            **({"agentId": agent_id} if agent_id else {}),
        }
    )


def assistant(content='{"count":42,"color":"red"}', origin="user-1", **extra):
    return event(
        "assistant.message",
        {"messageId": str(uuid4()), "content": content, "originatingMessageId": origin, **extra},
    )


def user(origin="user-1"):
    return event("user.message", {"messageId": origin, "content": "inventory"})


def idle(**data):
    return event("session.idle", data)


def fake_session(events=(), admission_error=None):
    client = Mock()
    session = CopilotSession("session-1", client)

    async def request(method, params):
        assert method == "session.send"
        for item in events:
            session._dispatch_event(item)
        if admission_error:
            raise admission_error
        return {"messageId": "user-1"}

    client.request = AsyncMock(side_effect=request)
    return session, client


def assert_clean(session):
    assert not session._structured_waits
    assert not session._event_handlers


@pytest.mark.asyncio
async def test_typed_output_buffers_pre_ack_events_and_infers_schema():
    session, client = fake_session([user(), assistant(), idle()])
    result = await session.send_and_wait_typed("inventory", Inventory, timeout=1)
    assert result == Inventory(count=42, color="red")
    params = client.request.call_args.args[1]
    assert params["responseFormat"] == {
        "type": "json_schema",
        "jsonSchema": {"name": "response", "strict": True, "schema": Inventory.model_json_schema()},
    }
    assert_clean(session)


@pytest.mark.asyncio
async def test_raw_schema_is_forwarded_unchanged_and_result_remains_an_event():
    schema = {"type": "object", "properties": {"count": {"type": "integer", "minimum": 2}}}
    final = assistant()
    session, client = fake_session([final, idle()])
    result = await session.send_and_wait("inventory", response_schema=schema, timeout=1)
    assert result is final
    assert client.request.call_args.args[1]["responseFormat"]["jsonSchema"]["schema"] == schema
    assert "additionalProperties" not in schema
    assert_clean(session)


@pytest.mark.asyncio
async def test_correlation_tool_commentary_stop_corrections_and_autopilot():
    subagent = assistant('{"count":999,"color":"wrong"}')
    subagent.agent_id = "child"
    events = [
        idle(),
        event("session.error", {"errorType": "test", "message": "before this run"}),
        user(),
        assistant("working", toolRequests=[{"toolCallId": "tool-1", "name": "inventory"}]),
        assistant(),
        idle(mode="autopilot"),
        assistant('{"count":99,"color":"blue"}'),
        subagent,
        assistant('{"count":123,"color":"wrong"}', origin="other-user"),
        idle(),
    ]
    session, _ = fake_session(events)
    assert await session.send_and_wait_typed("inventory", Inventory, timeout=1) == Inventory(
        count=99, color="blue"
    )
    assert_clean(session)


@pytest.mark.parametrize(
    ("events", "error"),
    [
        ([user(), idle()], "without a structured"),
        ([user(), assistant(" "), idle()], "without a structured"),
        (
            [
                assistant(),
                assistant("working", toolRequests=[{"toolCallId": "t", "name": "tool"}]),
                idle(),
            ],
            "without a structured",
        ),
        ([assistant(), idle(aborted=True)], "aborted"),
        (
            [user(), event("session.error", {"errorType": "test", "message": "provider failed"})],
            "provider failed",
        ),
    ],
)
@pytest.mark.asyncio
async def test_failed_runs_do_not_return_stale_or_missing_results(events, error):
    session, _ = fake_session(events)
    with pytest.raises(RuntimeError, match=error):
        await session.send_and_wait_typed("inventory", Inventory, timeout=1)
    assert_clean(session)


@pytest.mark.parametrize(
    "content",
    [
        "null",
        "not JSON",
        '{"count":"no","color":"red"}',
        '{"count":42}',
        '{"count":42,"color":"red","extra":1}',
    ],
)
@pytest.mark.asyncio
async def test_typed_output_validates_json(content):
    session, _ = fake_session([assistant(content), idle()])
    with pytest.raises(ValidationError):
        await session.send_and_wait_typed("inventory", Inventory, timeout=1)
    assert_clean(session)


@pytest.mark.asyncio
async def test_admission_error_is_not_hidden_by_buffered_success():
    session, _ = fake_session([assistant(), idle()], ValueError("invalid schema"))
    with pytest.raises(ValueError, match="invalid schema"):
        await session.send_and_wait_typed("inventory", Inventory, timeout=1)
    assert_clean(session)


@pytest.mark.parametrize("kind", ["timeout", "cancel", "disconnect"])
@pytest.mark.asyncio
async def test_wait_cleanup(kind):
    session, client = fake_session()
    admitted = asyncio.Event()

    async def request(method, params):
        admitted.set()
        return {"messageId": "user-1"}

    client.request.side_effect = request
    waiting = asyncio.create_task(
        session.send_and_wait_typed(
            "inventory", Inventory, timeout=0.05 if kind == "timeout" else 1
        )
    )
    await admitted.wait()
    if kind == "cancel":
        waiting.cancel()
        error = asyncio.CancelledError
    elif kind == "disconnect":
        session._mark_disconnected()
        error = RuntimeError
    else:
        error = TimeoutError
    with pytest.raises(error):
        await waiting
    assert_clean(session)


@pytest.mark.asyncio
async def test_concurrent_typed_waits_keep_their_own_origin():
    session, client = fake_session()
    admitted = asyncio.Queue()

    async def request(method, params):
        origin = params["prompt"]
        admitted.put_nowait(origin)
        return {"messageId": origin}

    client.request.side_effect = request
    first = asyncio.create_task(session.send_and_wait_typed("first", Inventory, timeout=1))
    second = asyncio.create_task(session.send_and_wait_typed("second", Inventory, timeout=1))
    await admitted.get()
    await admitted.get()
    session._dispatch_event(assistant(origin="first"))
    session._dispatch_event(assistant('{"count":7,"color":"blue"}', origin="second"))
    session._dispatch_event(idle())
    assert (await first).count == 42
    assert (await second).count == 7
    assert_clean(session)


@pytest.mark.asyncio
async def test_typed_immediate_is_rejected_before_admission():
    session, client = fake_session()
    with pytest.raises(ValueError, match="immediate"):
        await session.send_and_wait_typed("inventory", Inventory, mode="immediate")
    client.request.assert_not_called()
    assert_clean(session)


@pytest.mark.asyncio
async def test_disconnect_while_admission_is_pending_cancels_rpc_wait():
    session, client = fake_session()
    entered = asyncio.Event()
    cancelled = asyncio.Event()

    async def request(method, params):
        entered.set()
        try:
            await asyncio.Future()
        finally:
            cancelled.set()

    client.request.side_effect = request
    waiting = asyncio.create_task(session.send_and_wait_typed("inventory", Inventory))
    await entered.wait()
    session._mark_disconnected()
    with pytest.raises(RuntimeError, match="Session closed"):
        await asyncio.wait_for(waiting, 1)
    assert cancelled.is_set()
    assert_clean(session)
