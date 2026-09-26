# Copyright (c) Microsoft Corporation. All rights reserved.

"""Deterministic controls for the event-log E2E scenarios' background-event races."""

import asyncio
from types import SimpleNamespace
from unittest.mock import AsyncMock

import pytest

from copilot.rpc import EventsCursorStatus, EventsReadResult, SessionsSaveRequest
from copilot.session_events import (
    PlanChangedOperation,
    SessionEventType,
    SessionPlanChangedData,
    SessionTitleChangedData,
)
from e2e import test_rpc_event_log_e2e as scenario


@pytest.mark.asyncio
@pytest.mark.parametrize("ephemeral", [False, True])
async def test_tail_scenario_ignores_only_background_ephemeral_events(ephemeral):
    background = SimpleNamespace(
        type=SessionEventType.SESSION_INDEXED_SEARCH,
        ephemeral=ephemeral,
    )
    update_plan = AsyncMock()
    save_session = AsyncMock()

    async def read(request):
        assert request.cursor == "tail-before-indexing"
        if save_session.await_count:
            update_plan.assert_awaited_once()
            events = [
                SimpleNamespace(
                    type=SessionEventType.SESSION_PLAN_CHANGED,
                    data=SessionPlanChangedData(operation=PlanChangedOperation.CREATE),
                    ephemeral=False,
                )
            ]
        else:
            events = [] if ephemeral and request.include_ephemeral is False else [background]
        return EventsReadResult(
            cursor="tail-after-indexing",
            cursor_status=EventsCursorStatus.OK,
            events=events,
            has_more=False,
        )

    session = SimpleNamespace(
        session_id="tail-session",
        log=AsyncMock(),
        rpc=SimpleNamespace(
            plan=SimpleNamespace(update=update_plan),
            event_log=SimpleNamespace(
                tail=AsyncMock(return_value=SimpleNamespace(cursor="tail-before-indexing")),
                read=read,
            ),
        ),
        disconnect=AsyncMock(),
    )
    ctx = SimpleNamespace(
        client=SimpleNamespace(
            create_session=AsyncMock(return_value=session),
            rpc=SimpleNamespace(sessions=SimpleNamespace(save=save_session)),
        )
    )

    run = (
        scenario.TestRpcEventLog().test_should_return_tail_cursor_and_read_empty_when_no_new_events
    )
    if ephemeral:
        await run(ctx)
        update_plan.assert_awaited_once()
        save_session.assert_awaited_once_with(SessionsSaveRequest(session_id="tail-session"))
        session.log.assert_awaited_once_with("Ephemeral event after tail", ephemeral=True)
    else:
        with pytest.raises(AssertionError):
            await run(ctx)

    session.disconnect.assert_awaited_once()


@pytest.mark.asyncio
@pytest.mark.parametrize("background_wakeups", [0, 2])
async def test_filtered_long_poll_scenario_continues_after_unmatched_events(background_wakeups):
    expected_title = None
    cursors = ["initial-tail", *[f"background-{i}" for i in range(background_wakeups)]]
    reads = []

    async def set_name(request):
        nonlocal expected_title
        expected_title = request.name

    async def read(request):
        index = len(reads)
        assert request.cursor == cursors[index]
        assert request.types == ["session.title_changed"]
        assert request.wait_ms == 5000
        reads.append(request)
        if index < background_wakeups:
            return EventsReadResult(
                cursor=cursors[index + 1],
                cursor_status=EventsCursorStatus.OK,
                events=[],
                has_more=False,
            )
        assert expected_title is not None
        return EventsReadResult(
            cursor="after-title",
            cursor_status=EventsCursorStatus.OK,
            events=[
                SimpleNamespace(
                    type=SessionEventType.SESSION_TITLE_CHANGED,
                    data=SessionTitleChangedData(title=expected_title),
                )
            ],
            has_more=False,
        )

    session = SimpleNamespace(
        rpc=SimpleNamespace(
            name=SimpleNamespace(set=set_name),
            event_log=SimpleNamespace(
                tail=AsyncMock(return_value=SimpleNamespace(cursor=cursors[0])),
                read=read,
            ),
        ),
        disconnect=AsyncMock(),
    )
    ctx = SimpleNamespace(client=SimpleNamespace(create_session=AsyncMock(return_value=session)))

    await (
        scenario.TestRpcEventLog().test_should_long_poll_with_types_filter_for_title_changed_event(
            ctx
        )
    )

    assert len(reads) == background_wakeups + 1
    session.disconnect.assert_awaited_once()


@pytest.mark.asyncio
async def test_filtered_long_poll_scenario_cancels_reader_on_name_set_failure():
    read_started = asyncio.Event()
    read_cancelled = asyncio.Event()
    reader = None

    async def read(_request):
        nonlocal reader
        reader = asyncio.current_task()
        read_started.set()
        try:
            await asyncio.Future()
        finally:
            read_cancelled.set()

    async def set_name(_request):
        await read_started.wait()
        raise RuntimeError("controlled name.set failure")

    session = SimpleNamespace(
        rpc=SimpleNamespace(
            name=SimpleNamespace(set=set_name),
            event_log=SimpleNamespace(
                tail=AsyncMock(return_value=SimpleNamespace(cursor="tail")),
                read=read,
            ),
        ),
        disconnect=AsyncMock(),
    )
    ctx = SimpleNamespace(client=SimpleNamespace(create_session=AsyncMock(return_value=session)))

    suite = scenario.TestRpcEventLog()
    try:
        with pytest.raises(RuntimeError, match="controlled name.set failure"):
            await suite.test_should_long_poll_with_types_filter_for_title_changed_event(ctx)
        assert read_cancelled.is_set()
        session.disconnect.assert_awaited_once()
    finally:
        if reader is not None:
            reader.cancel()
            await asyncio.gather(reader, return_exceptions=True)
