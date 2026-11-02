"""Controlled regressions for the session E2E timeout test's teardown."""

import asyncio
from types import SimpleNamespace
from unittest.mock import AsyncMock, Mock

import pytest

from copilot.session import CopilotSession


@pytest.mark.asyncio
@pytest.mark.parametrize("admitted", [False, True], ids=["before-admission", "after-admission"])
@pytest.mark.parametrize("detach_success", [True, False], ids=["detach-ok", "detach-error"])
async def test_timeout_e2e_cleanup_without_idle(monkeypatch, admitted, detach_success):
    # Import the E2E body without preparing or launching a runtime.
    monkeypatch.setenv("COPILOT_CLI_PATH", __file__)
    from e2e import test_session_e2e

    client = Mock()
    session = CopilotSession("timeout-session", client)
    ctx = SimpleNamespace(client=SimpleNamespace(create_session=AsyncMock(return_value=session)))
    timeout_context = asyncio.timeout(None)
    monkeypatch.setattr(asyncio, "timeout_at", lambda _: timeout_context)
    request_cancelled = False
    active_work: asyncio.Task | None = None
    detached = False

    async def request(method, params):
        nonlocal request_cancelled, active_work, detached
        assert params["sessionId"] == session.session_id
        if method == "session.sendAndWait":
            assert params["prompt"] == "Run 'sleep 2 && echo done'"
            if admitted:
                active_work = asyncio.create_task(asyncio.Event().wait())
            # Expire the real SDK deadline at a controlled admission boundary.
            timeout_context.reschedule(asyncio.get_running_loop().time())
            try:
                await asyncio.Future()
            finally:
                request_cancelled = True
        if method == "session.abort":
            return {}
        assert method == "session.detach"
        assert request_cancelled
        if detach_success:
            if active_work is not None:
                active_work.cancel()
                await asyncio.gather(active_work, return_exceptions=True)
            detached = True
        return {"success": detach_success, "error": None if detach_success else "detach failed"}

    client.request = AsyncMock(side_effect=request)
    body = test_session_e2e.TestSessions().test_sendandwait_throws_on_timeout(ctx)
    try:
        if detach_success:
            await asyncio.wait_for(body, timeout=5)
            assert detached
            assert session._destroyed
            assert active_work is None or active_work.done()
        else:
            with pytest.raises(RuntimeError, match="Failed to detach.*detach failed"):
                await asyncio.wait_for(body, timeout=5)
            assert not detached
            assert not session._destroyed
            assert active_work is None or not active_work.done()

        assert timeout_context.expired()
        assert request_cancelled
        assert (active_work is not None) == admitted
        assert not session._response_waits
        assert not session._event_handlers
        assert [call.args[0] for call in client.request.await_args_list] == [
            "session.sendAndWait",
            "session.detach",
        ]
    finally:
        if active_work is not None:
            active_work.cancel()
            await asyncio.gather(active_work, return_exceptions=True)
