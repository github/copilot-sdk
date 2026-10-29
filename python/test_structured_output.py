"""Structured-output admission, correlation, and lifecycle tests."""

import asyncio
from datetime import UTC, datetime
from unittest.mock import AsyncMock, Mock
from uuid import uuid4

import pytest
from pydantic import BaseModel, ConfigDict, Field, ValidationError

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


@pytest.mark.parametrize("structured", [False, True])
@pytest.mark.parametrize("before_ack", [False, True])
@pytest.mark.parametrize(
    "source",
    [
        "user",
        "assistant",
        "autopilot",
        "system",
        "session-memory-rollover",
        "marked",
        "pre-message",
        "steering",
    ],
)
@pytest.mark.asyncio
async def test_wait_correlates_errors_independently_of_message_events(
    structured, before_ack, source
):
    events = [
        assistant(origin="other" if source == "pre-message" else "user-1"),
        assistant("other", "other")
        if source == "assistant"
        else event(
            "user.message",
            {
                "messageId": "other",
                "content": "other",
                "source": "sdk" if source in ("steering", "pre-message") else source,
                "isAutopilotContinuation": source == "marked",
            },
        ),
        event(
            "session.error",
            {
                "errorType": "query",
                "message": "provider failed",
                "originatingMessageId": "other" if source in ("user", "assistant") else "user-1",
            },
        ),
        idle(),
    ]
    session, client = fake_session(events if before_ack else ())
    sending = asyncio.Event()
    original = client.request.side_effect

    async def request(method, params):
        result = await original(method, params)
        sending.set()
        return result

    client.request.side_effect = request
    waiting = asyncio.create_task(
        session.send_and_wait(
            "waited", response_schema={"type": "object"} if structured else None, timeout=1
        )
    )
    await sending.wait()
    if not before_ack:
        for item in events:
            session._dispatch_event(item)
    if source in ("user", "assistant"):
        result = await waiting
        assert result is not None
        assert result.data.content == '{"count":42,"color":"red"}'
    else:
        with pytest.raises(Exception, match="provider failed"):
            await waiting
    assert_clean(session)


@pytest.mark.parametrize("before_ack", [False, True])
@pytest.mark.parametrize("blocked", [False, True])
@pytest.mark.asyncio
async def test_plain_wait_ignores_prior_idle_and_correlates_its_start(before_ack, blocked):
    session, client = fake_session()
    admission = asyncio.get_running_loop().create_future()
    sending = asyncio.Event()

    async def request(method, params):
        sending.set()
        return await admission

    client.request.side_effect = request
    pending = asyncio.create_task(session.send_and_wait("queued", timeout=1))
    await sending.wait()
    if not before_ack:
        admission.set_result({"messageId": "user-1"})
        await asyncio.sleep(0)
    session._dispatch_event(assistant("previous", "other"))
    session._dispatch_event(
        event(
            "session.warning",
            {
                "warningType": "policy_blocked",
                "message": "Blocked",
                "originatingMessageId": "other",
            },
        )
    )
    session._dispatch_event(
        event(
            "session.warning",
            {
                "warningType": "policy_blocked",
                "message": "Blocked",
                "originatingMessageId": "user-1",
            },
            agent_id="child",
        )
    )
    session._dispatch_event(idle(aborted=True))
    session._dispatch_event(idle())
    if before_ack:
        admission.set_result({"messageId": "user-1"})
    await asyncio.sleep(0)
    assert not pending.done()
    final = assistant("current")
    session._dispatch_event(
        event(
            "session.warning",
            {
                "warningType": "policy_blocked",
                "message": "Blocked",
                "originatingMessageId": "user-1",
            },
        )
        if blocked
        else final
    )
    session._dispatch_event(idle())
    assert await pending is (None if blocked else final)
    assert_clean(session)


@pytest.mark.parametrize("terminal", ["session.idle", "session.autopilot_stopped"])
@pytest.mark.asyncio
async def test_plain_wait_rejects_current_abort(terminal):
    session, _ = fake_session(
        [
            user(),
            event(terminal, {"aborted": True, "reason": "aborted", "requestId": "user-1"}),
        ]
    )
    with pytest.raises(RuntimeError, match="aborted"):
        await session.send_and_wait("cancel", timeout=1)
    assert_clean(session)


@pytest.mark.parametrize("terminal", ["idle", "autopilot", "error"])
@pytest.mark.asyncio
async def test_plain_wait_preserves_pre_ack_outcome_through_retirement(terminal):
    session, client = fake_session()
    final = assistant()

    async def request(method, params):
        session._dispatch_event(final)
        session._dispatch_event(
            event("session.autopilot_stopped", {"reason": "task_complete", "requestId": "user-1"})
            if terminal == "autopilot"
            else event("session.error", {"errorType": "test", "message": "original failure"})
            if terminal == "error"
            else idle()
        )
        session._mark_disconnected()
        return {"messageId": "user-1"}

    client.request.side_effect = request
    if terminal == "error":
        with pytest.raises(Exception, match="original failure"):
            await session.send_and_wait("finish", timeout=1)
    else:
        assert await session.send_and_wait("finish", timeout=1) is final


@pytest.mark.parametrize("structured", [False, True])
@pytest.mark.asyncio
async def test_autopilot_error_terminal_does_not_require_session_error(structured):
    session, _ = fake_session(
        [event("session.autopilot_stopped", {"reason": "error", "requestId": "user-1"})]
    )
    with pytest.raises(RuntimeError, match="Autopilot request failed"):
        await session.send_and_wait(
            "fail", response_schema={"type": "object"} if structured else None, timeout=1
        )
    assert_clean(session)


@pytest.mark.parametrize("structured", [False, True])
@pytest.mark.asyncio
async def test_autopilot_terminal_buffers_pre_ack_and_ignores_other_requests(structured):
    final = assistant('{"count":7,"color":"blue"}')
    session, _ = fake_session(
        [
            assistant(),
            event(
                "user.message",
                {
                    "content": "Continue",
                    "messageId": "other",
                    "source": "autopilot",
                    "isAutopilotContinuation": True,
                },
            ),
            event("session.autopilot_stopped", {"reason": "task_complete", "requestId": "other"}),
            event("session.autopilot_stopped", {"reason": "task_complete"}),
            event(
                "session.autopilot_stopped",
                {"reason": "task_complete", "requestId": "user-1"},
                agent_id="child",
            ),
            idle(mode="autopilot"),
            final,
            event("session.autopilot_stopped", {"reason": "task_complete", "requestId": "user-1"}),
        ]
    )
    result = await session.send_and_wait(
        "inventory", response_schema={"type": "object"} if structured else None, timeout=1
    )
    assert result is final
    assert not session._response_waits
    assert_clean(session)


@pytest.mark.parametrize("structured", [False, True])
@pytest.mark.asyncio
async def test_autopilot_terminal_isolates_concurrent_waits(structured):
    session, client = fake_session()
    admitted = asyncio.Queue()

    async def request(method, params):
        admitted.put_nowait(params["prompt"])
        return {"messageId": params["prompt"]}

    client.request.side_effect = request
    schema = {"type": "object"} if structured else None
    first = asyncio.create_task(session.send_and_wait("first", response_schema=schema, timeout=1))
    second = asyncio.create_task(session.send_and_wait("second", response_schema=schema, timeout=1))
    await admitted.get()
    await admitted.get()
    first_message = assistant(origin="first")
    second_message = assistant('{"count":7,"color":"blue"}', origin="second")
    session._dispatch_event(first_message)
    session._dispatch_event(
        event("session.autopilot_stopped", {"reason": "task_complete", "requestId": "first"})
    )
    session._dispatch_event(second_message)
    session._dispatch_event(
        event("session.autopilot_stopped", {"reason": "task_complete", "requestId": "second"})
    )
    assert await first is first_message
    assert await second is second_message
    assert_clean(session)


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
async def test_typed_output_validates_schema_aliases_including_nested_models():
    class Answer(BaseModel):
        model_config = ConfigDict(extra="forbid", validate_by_alias=False, validate_by_name=True)
        value: int = Field(alias="answer")

    class Result(BaseModel):
        model_config = ConfigDict(extra="forbid", validate_by_alias=False, validate_by_name=True)
        value: Answer = Field(alias="result")
        values: list[Answer] = Field(alias="results")

    content = '{"result":{"answer":42},"results":[{"answer":99}]}'
    session, client = fake_session([assistant(content), idle()])
    result = await session.send_and_wait_typed("answer", Result, timeout=1)
    assert result.value.value == 42
    assert result.values[0].value == 99
    schema = client.request.call_args.args[1]["responseFormat"]["jsonSchema"]["schema"]
    assert schema == Result.model_json_schema()
    assert set(schema["properties"]) == {"result", "results"}
    assert set(schema["$defs"]["Answer"]["properties"]) == {"answer"}
    assert not Result.model_config["validate_by_alias"]
    assert not Answer.model_config["validate_by_alias"]
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
