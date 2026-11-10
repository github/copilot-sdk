"""
Unit tests for ``on_shell_authentication_request``: shell password prompts that the
runtime forwards as ``shellAuthentication.request`` and closes with ``shellAuthentication.cancel``.

Mirrors the Node.js client.test.ts shell authentication tests.
"""

import asyncio
import gc
import logging
import sys
import weakref
from typing import Any
from unittest.mock import Mock

import pytest

from copilot import CopilotClient, RuntimeConnection
from copilot.session import (
    CopilotSession,
    PermissionHandler,
    ShellAuthenticationInvocation,
    ShellAuthenticationRequest,
    ShellAuthenticationResponse,
)
from e2e.testharness import CLI_PATH

REQUEST: ShellAuthenticationRequest = {
    "requestId": "secret-1",
    "program": "git",
    "prompt": "Password for 'https://me@example.com': ",
    "inputKind": "secret",
    "command": "git push",
    "cwd": "/work",
}

WIRE_REQUEST = {
    "sessionId": "session-1",
    "requestId": "secret-2",
    "program": "ssh",
    "prompt": "Enter passphrase for key '/k': ",
    "inputKind": "secret",
    "command": "ssh host",
    "cwd": "/work",
}


def _client() -> CopilotClient:
    return CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))


def _session_awaiting_cancel() -> tuple[CopilotSession, asyncio.Event]:
    """A session whose handler waits until its request is cancelled."""
    session = CopilotSession("session-1", Mock())
    started = asyncio.Event()

    async def handler(
        _request: ShellAuthenticationRequest, invocation: ShellAuthenticationInvocation
    ) -> ShellAuthenticationResponse:
        started.set()
        await invocation["cancelled"].wait()
        return {"action": "accept", "value": "too late"}

    session._register_shell_authentication_handler(handler)
    return session, started


class TestShellAuthenticationHandler:
    @pytest.mark.asyncio
    async def test_returns_a_secret_only_for_a_well_formed_answer(
        self, caplog: pytest.LogCaptureFixture
    ):
        session = CopilotSession("session-1", Mock())

        def failing(_request, _invocation):
            raise RuntimeError("rejected HUNTER2-FAKE-0003")

        async def asynchronous(_request, _invocation):
            return {"action": "accept", "value": "HUNTER2-FAKE-0002"}

        answers = []
        with caplog.at_level(logging.ERROR, logger="copilot.session"):
            for handler in [
                lambda _request, _invocation: {"action": "accept", "value": "HUNTER2-FAKE-0001"},
                lambda _request, _invocation: {"action": "accept", "value": 42},
                failing,
                lambda _request, _invocation: {"action": "cancel"},
                asynchronous,
            ]:
                session._register_shell_authentication_handler(handler)
                answers.append(
                    await session._handle_shell_authentication_request(REQUEST, asyncio.Event())
                )
        assert answers == [
            {"action": "accept", "value": "HUNTER2-FAKE-0001"},
            {"action": "cancel"},
            {"action": "cancel"},
            {"action": "cancel"},
            {"action": "accept", "value": "HUNTER2-FAKE-0002"},
        ]
        assert [
            (
                r.getMessage(),
                getattr(r, "session_id", None),
                getattr(r, "request_id", None),
                r.exc_info,
            )
            for r in caplog.records
        ] == [
            ("Shell authentication handler failed with RuntimeError", "session-1", "secret-1", None)
        ]
        assert "HUNTER2" not in caplog.text

    @pytest.mark.asyncio
    async def test_cancels_without_a_handler(self):
        session = CopilotSession("session-1", Mock())
        assert await session._handle_shell_authentication_request(REQUEST, asyncio.Event()) == {
            "action": "cancel"
        }

    @pytest.mark.asyncio
    async def test_a_handler_whose_answer_is_cancelled_cancels_the_prompt(
        self, caplog: pytest.LogCaptureFixture
    ):
        session = CopilotSession("session-1", Mock())

        async def cancels_itself(_request, _invocation):
            raise asyncio.CancelledError

        def dismissed_dialog(_request, _invocation):
            dialog = asyncio.get_running_loop().create_future()
            asyncio.get_running_loop().call_soon(dialog.cancel)
            return dialog

        answers = []
        with caplog.at_level(logging.ERROR, logger="copilot.session"):
            for handler in [cancels_itself, dismissed_dialog]:
                session._register_shell_authentication_handler(handler)
                answers.append(
                    await session._handle_shell_authentication_request(REQUEST, asyncio.Event())
                )
        assert answers == [{"action": "cancel"}, {"action": "cancel"}]
        assert [r.getMessage() for r in caplog.records] == [
            "Shell authentication handler failed with CancelledError"
        ] * 2

    @pytest.mark.asyncio
    async def test_cancelling_the_request_task_still_propagates(self):
        session = CopilotSession("session-1", Mock())
        started = asyncio.Event()
        released = asyncio.Event()

        async def waits(_request, _invocation):
            started.set()
            await released.wait()
            return {"action": "cancel"}

        session._register_shell_authentication_handler(waits)
        pending = asyncio.create_task(
            session._handle_shell_authentication_request(REQUEST, asyncio.Event())
        )
        await started.wait()
        pending.cancel()
        with pytest.raises(asyncio.CancelledError):
            await pending
        released.set()
        await asyncio.sleep(0)

    @pytest.mark.asyncio
    async def test_runtime_cancel_aborts_a_pending_request(self):
        client = _client()
        session, started = _session_awaiting_cancel()
        client._sessions["session-1"] = session
        pending = asyncio.create_task(
            client._handle_shell_authentication_request(dict(WIRE_REQUEST))
        )
        await asyncio.wait_for(started.wait(), timeout=1)

        assert (
            await client._handle_shell_authentication_cancel(
                {"sessionId": "session-1", "requestId": "secret-2"}
            )
            == {}
        )
        assert await asyncio.wait_for(pending, timeout=1) == {"action": "cancel"}
        assert client._pending_shell_authentication_requests == {}
        assert session._pending_shell_authentication_requests == set()

    @pytest.mark.asyncio
    async def test_a_duplicate_request_is_cancelled_without_closing_the_open_one(self):
        client = _client()
        session, started = _session_awaiting_cancel()
        client._sessions["session-1"] = session
        pending = asyncio.create_task(
            client._handle_shell_authentication_request(dict(WIRE_REQUEST))
        )
        await asyncio.wait_for(started.wait(), timeout=1)

        duplicate = client._handle_shell_authentication_request(dict(WIRE_REQUEST))
        assert await asyncio.wait_for(duplicate, timeout=1) == {"action": "cancel"}
        assert not client._pending_shell_authentication_requests[("session-1", "secret-2")].is_set()
        assert not pending.done()

        await client._handle_shell_authentication_cancel(
            {"sessionId": "session-1", "requestId": "secret-2"}
        )
        assert await asyncio.wait_for(pending, timeout=1) == {"action": "cancel"}

    @pytest.mark.asyncio
    async def test_cancels_a_request_whose_handler_never_finishes(self):
        client = _client()
        session = CopilotSession("session-1", Mock())
        started = asyncio.Event()
        handler_cancelled = asyncio.Event()

        async def never_answers(_request, _invocation):
            started.set()
            try:
                await asyncio.Future()
            finally:
                handler_cancelled.set()

        session._register_shell_authentication_handler(never_answers)
        client._sessions["session-1"] = session
        pending = asyncio.create_task(
            client._handle_shell_authentication_request(dict(WIRE_REQUEST))
        )
        await asyncio.wait_for(started.wait(), timeout=1)

        await client._handle_shell_authentication_cancel(
            {"sessionId": "session-1", "requestId": "secret-2"}
        )
        assert await asyncio.wait_for(pending, timeout=1) == {"action": "cancel"}
        await asyncio.wait_for(handler_cancelled.wait(), timeout=1)

    @pytest.mark.asyncio
    async def test_a_handler_that_fails_after_its_prompt_closes_is_not_reported(
        self, caplog: pytest.LogCaptureFixture
    ):
        session = CopilotSession("session-1", Mock())
        started = asyncio.Event()
        stopped = asyncio.Event()

        async def fails_when_cancelled(_request, _invocation):
            started.set()
            try:
                await asyncio.Future()
            finally:
                stopped.set()
                raise RuntimeError("cleanup failed for HUNTER2-FAKE-0006")

        loop = asyncio.get_running_loop()
        reports: list[dict[str, Any]] = []
        loop.set_exception_handler(lambda _loop, context: reports.append(context))
        try:
            session._register_shell_authentication_handler(fails_when_cancelled)
            cancelled = asyncio.Event()
            with caplog.at_level(logging.ERROR):
                pending = asyncio.create_task(
                    session._handle_shell_authentication_request(REQUEST, cancelled)
                )
                await started.wait()
                cancelled.set()
                assert await pending == {"action": "cancel"}
                await stopped.wait()
                await asyncio.sleep(0)
                gc.collect()
        finally:
            loop.set_exception_handler(None)
        assert [str(report.get("exception")) for report in reports] == []
        assert "HUNTER2" not in caplog.text

    @pytest.mark.asyncio
    async def test_a_handler_that_ignores_cancellation_is_not_collected_while_running(
        self, monkeypatch: pytest.MonkeyPatch
    ):
        session = CopilotSession("session-1", Mock())
        started = asyncio.Event()
        handler_tasks: list[weakref.ref[asyncio.Task[Any]]] = []

        async def ignores_cancellation(_request, _invocation):
            task = asyncio.current_task()
            assert task is not None
            handler_tasks.append(weakref.ref(task))
            started.set()
            try:
                await asyncio.Future()
            except asyncio.CancelledError:
                pass
            try:
                await asyncio.Future()
            finally:
                raise RuntimeError("cleanup failed for HUNTER2-FAKE-0007")

        unraisable: list[Any] = []
        monkeypatch.setattr(sys, "unraisablehook", unraisable.append)
        session._register_shell_authentication_handler(ignores_cancellation)
        cancelled = asyncio.Event()
        pending = asyncio.create_task(
            session._handle_shell_authentication_request(REQUEST, cancelled)
        )
        await started.wait()
        cancelled.set()
        assert await pending == {"action": "cancel"}
        # The app may drop the session while its handler still runs.
        del session, pending
        gc.collect()
        assert [str(report.exc_value) for report in unraisable] == []
        task = handler_tasks[0]()
        assert task is not None and not task.done()
        task.cancel()
        await asyncio.wait({task})
        assert not task.cancelled() and task.exception() is None

    @pytest.mark.asyncio
    async def test_passes_the_runtime_request_to_the_handler(self):
        client = _client()
        session = CopilotSession("session-1", Mock())
        requests: list[ShellAuthenticationRequest] = []

        def record(request, _invocation):
            requests.append(request)
            return {"action": "cancel"}

        session._register_shell_authentication_handler(record)
        client._sessions["session-1"] = session
        await client._handle_shell_authentication_request(WIRE_REQUEST)
        assert requests == [
            {
                "requestId": "secret-2",
                "program": "ssh",
                "prompt": "Enter passphrase for key '/k': ",
                "inputKind": "secret",
                "command": "ssh host",
                "cwd": "/work",
            }
        ]

    @pytest.mark.asyncio
    async def test_cancels_a_malformed_request_without_asking(self):
        client = _client()
        session = CopilotSession("session-1", Mock())
        handler = Mock(return_value={"action": "accept", "value": "unused"})
        session._register_shell_authentication_handler(handler)
        client._sessions["session-1"] = session
        for params in [
            {**WIRE_REQUEST, "inputKind": "password"},
            {**WIRE_REQUEST, "requestId": 7},
            {**WIRE_REQUEST, "sessionId": "unknown-session"},
        ]:
            assert await client._handle_shell_authentication_request(params) == {"action": "cancel"}
        handler.assert_not_called()

    @pytest.mark.asyncio
    async def test_cancel_reaches_the_session_that_took_the_request_after_a_resume(self):
        client = _client()
        session, started = _session_awaiting_cancel()
        client._sessions["session-1"] = session
        pending = asyncio.create_task(
            client._handle_shell_authentication_request(dict(WIRE_REQUEST))
        )
        await asyncio.wait_for(started.wait(), timeout=1)
        # A resume registers a new session object while the prompt is open.
        client._sessions["session-1"] = CopilotSession("session-1", Mock())

        await client._handle_shell_authentication_cancel(
            {"sessionId": "session-1", "requestId": "secret-2"}
        )
        assert await asyncio.wait_for(pending, timeout=1) == {"action": "cancel"}

    @pytest.mark.asyncio
    async def test_force_stop_cancels_open_requests(self):
        client = _client()
        session, started = _session_awaiting_cancel()
        client._sessions["session-1"] = session
        pending = asyncio.create_task(
            client._handle_shell_authentication_request(dict(WIRE_REQUEST))
        )
        await asyncio.wait_for(started.wait(), timeout=1)

        await client.force_stop()
        assert await asyncio.wait_for(pending, timeout=1) == {"action": "cancel"}

    @pytest.mark.asyncio
    async def test_connection_close_cancels_open_requests(self):
        client = _client()
        session, started = _session_awaiting_cancel()
        client._sessions["session-1"] = session
        pending = asyncio.create_task(
            client._handle_shell_authentication_request(dict(WIRE_REQUEST))
        )
        await asyncio.wait_for(started.wait(), timeout=1)

        client._client = Mock(_loop=asyncio.get_running_loop())
        await asyncio.to_thread(client._handle_connection_close)
        assert await asyncio.wait_for(pending, timeout=1) == {"action": "cancel"}

    @pytest.mark.asyncio
    async def test_a_request_dispatched_after_connection_close_is_cancelled_without_asking(self):
        client = _client()
        session = CopilotSession("session-1", Mock())
        handler = Mock(return_value={"action": "accept", "value": "unused"})
        session._register_shell_authentication_handler(handler)
        client._sessions["session-1"] = session

        # The reader queued this request before it saw the connection close.
        client._client = Mock(_loop=asyncio.get_running_loop())
        await asyncio.to_thread(client._handle_connection_close)
        answer = await client._handle_shell_authentication_request(dict(WIRE_REQUEST))

        assert answer == {"action": "cancel"}
        handler.assert_not_called()
        assert client._pending_shell_authentication_requests == {}

    @pytest.mark.asyncio
    async def test_disconnecting_the_session_cancels_its_open_requests(self):
        session, started = _session_awaiting_cancel()
        pending = asyncio.create_task(
            session._handle_shell_authentication_request(REQUEST, asyncio.Event())
        )
        await asyncio.wait_for(started.wait(), timeout=1)

        session._mark_disconnected()
        assert await asyncio.wait_for(pending, timeout=1) == {"action": "cancel"}


class TestShellAuthenticationWireFlag:
    @pytest.mark.asyncio
    async def test_requests_shell_authentication_only_with_a_handler(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured: list[tuple[str, dict]] = []
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured.append((method, params))
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request

            await client.create_session(on_permission_request=PermissionHandler.approve_all)
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                on_shell_authentication_request=lambda _request, _invocation: {"action": "cancel"},
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                on_shell_authentication_request=lambda _request, _invocation: {"action": "cancel"},
            )

            flags = [
                (method, params.get("requestShellAuthentication"))
                for method, params in captured
                if method in ("session.create", "session.resume")
            ]
            assert flags == [
                ("session.create", None),
                ("session.create", True),
                ("session.resume", True),
            ]
        finally:
            await client.force_stop()
