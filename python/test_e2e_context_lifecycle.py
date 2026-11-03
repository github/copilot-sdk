"""The module-shared E2E runtime must not carry sessions across replay fixtures."""

import asyncio
import contextlib
import sys
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import AsyncMock, Mock

import httpx
import pytest

from copilot import CopilotClient, RuntimeConnection
from copilot.session import CopilotSession


@pytest.fixture
def shared_context(tmp_path, monkeypatch):
    monkeypatch.setenv("COPILOT_CLI_PATH", sys.executable)
    from e2e.testharness.context import E2ETestContext

    ctx = E2ETestContext()
    ctx.home_dir = str(tmp_path / "home")
    ctx.work_dir = str(tmp_path / "work")
    Path(ctx.home_dir).mkdir()
    Path(ctx.work_dir).mkdir()
    ctx._proxy = Mock(configure=AsyncMock())
    ctx._client = CopilotClient(connection=RuntimeConnection.for_stdio(path=sys.executable))
    ctx._client._client = Mock(request=AsyncMock(return_value={"success": True}))
    return ctx


def register_session(ctx, session_id):
    session = CopilotSession(session_id, ctx.client._client)
    ctx.client._register_session(session)
    return session


@pytest.mark.parametrize("inprocess", [False, True], ids=["stdio", "inprocess"])
async def test_reconfiguration_cancels_and_joins_previous_background_request(
    shared_context, inprocess
):
    ctx = shared_context
    ctx._client_inprocess = inprocess
    await ctx.configure_for_test("compaction", "previous")
    previous_snapshot = ctx._proxy.configure.await_args.args[0]
    home_marker = Path(ctx.home_dir) / "session-state"
    work_marker = Path(ctx.work_dir) / "session-file"
    home_marker.write_text("owned by the previous session")
    work_marker.write_text("owned by the previous session")
    session = register_session(ctx, "previous-session")
    request_started = asyncio.Event()
    release_request = asyncio.Event()
    cancellation_started = asyncio.Event()
    release_cancellation = asyncio.Event()
    requests_replayed_against = []

    async def respond(request):
        assert request.url.path == "/chat/completions"
        request_started.set()
        try:
            await release_request.wait()
        except asyncio.CancelledError:
            cancellation_started.set()
            await release_cancellation.wait()
            raise
        requests_replayed_against.append(ctx._proxy.configure.await_args.args[0])
        return httpx.Response(200, json={"choices": []})

    async with httpx.AsyncClient(transport=httpx.MockTransport(respond)) as http:
        background_request = asyncio.create_task(http.post("http://proxy/chat/completions"))

        async def detach(method, params):
            assert method == "session.detach"
            assert params == {"sessionId": session.session_id}
            background_request.cancel()
            with contextlib.suppress(asyncio.CancelledError):
                await background_request
            return {"success": True}

        ctx.client._client.request.side_effect = detach
        await request_started.wait()
        reconfigure = asyncio.create_task(ctx.configure_for_test("compaction", "next"))
        cancelled = asyncio.create_task(cancellation_started.wait())
        try:
            done, _ = await asyncio.wait(
                [reconfigure, cancelled], return_when=asyncio.FIRST_COMPLETED
            )
            if reconfigure in done:
                # Old behavior switches caches while the previous session still owns
                # a request. Release it to observe which cache actually receives it.
                await reconfigure
                release_request.set()
                await background_request
            else:
                assert not reconfigure.done(), "Reconfiguration did not join session shutdown"
                assert ctx._proxy.configure.await_args.args[0] == previous_snapshot
                assert home_marker.exists(), "Deleted state before its owner stopped"
                assert work_marker.exists(), "Deleted files before their owner stopped"
                release_cancellation.set()
                await reconfigure

            assert requests_replayed_against == [], (
                "The previous session's background request reached the next replay fixture: "
                f"{requests_replayed_against}"
            )
            assert background_request.cancelled()
            assert session._destroyed
            assert ctx._proxy.configure.await_count == 2
            assert ctx._proxy.configure.await_args.args[0].endswith("next.yaml")
            assert not work_marker.exists()
            assert home_marker.exists() == inprocess
            ctx.client._client.request.assert_awaited_once_with(
                "session.detach", {"sessionId": session.session_id}
            )
        finally:
            release_cancellation.set()
            background_request.cancel()
            cancelled.cancel()
            await asyncio.gather(background_request, cancelled, reconfigure, return_exceptions=True)


@pytest.mark.parametrize("body_failed", [False, True], ids=["passed-body", "failed-body"])
async def test_function_fixture_disconnects_sessions_without_stopping_shared_runtime(
    shared_context, body_failed
):
    from e2e.conftest import configure_test

    ctx = shared_context
    request = SimpleNamespace(
        module=SimpleNamespace(__name__="e2e.test_compaction_e2e"),
        node=SimpleNamespace(name="test_current"),
    )
    fixture = configure_test.__wrapped__(request, ctx)
    await anext(fixture)
    first = register_session(ctx, "first")
    second = register_session(ctx, "second")
    try:
        if body_failed:
            with pytest.raises(ValueError, match="test body failed"):
                await fixture.athrow(ValueError("test body failed"))
        else:
            with pytest.raises(StopAsyncIteration):
                await anext(fixture)

        assert first._destroyed, "Function fixture left the first session alive"
        assert second._destroyed, "Function fixture left the second session alive"
        assert [call.args for call in ctx.client._client.request.await_args_list] == [
            ("session.detach", {"sessionId": "first"}),
            ("session.detach", {"sessionId": "second"}),
        ]
        ctx._proxy.stop.assert_not_called()
        assert ctx.client._client is not None, "Cleanup stopped the module-shared transport"
    finally:
        await fixture.aclose()


async def test_failed_disconnect_preserves_replay_fixture_and_attempts_other_sessions(
    shared_context,
):
    ctx = shared_context
    await ctx.configure_for_test("compaction", "previous")
    marker = Path(ctx.work_dir) / "owned-file"
    marker.write_text("still in use")
    failed = register_session(ctx, "failed")
    closed = register_session(ctx, "closed")

    async def detach(method, params):
        assert method == "session.detach"
        return {"success": params["sessionId"] != "failed", "error": "held session"}

    ctx.client._client.request.side_effect = detach
    with pytest.raises(ExceptionGroup, match="disconnect E2E sessions") as errors:
        await ctx.configure_for_test("compaction", "next")

    assert len(errors.value.exceptions) == 1
    assert "Failed to detach session failed: held session" in str(errors.value.exceptions[0])
    assert not failed._destroyed
    assert closed._destroyed, "One failed disconnect abandoned another session"
    assert ctx._proxy.configure.await_count == 1, "Switched snapshots after failed shutdown"
    assert marker.exists(), "Deleted files still owned by a live session"
