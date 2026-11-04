"""Replay failures must survive language-wrapper cleanup."""

import asyncio
import contextlib
import subprocess
import sys
from unittest.mock import Mock

import httpx
import pytest

from e2e.testharness.proxy import CapiProxy


@pytest.mark.parametrize(
    ("status", "exit_code", "message"),
    [
        (200, 0, None),
        (500, 1, "Replay cache misses"),
        (200, 1, "Proxy exited with code 1"),
    ],
)
async def test_shutdown_reports_replay_failures(monkeypatch, status, exit_code, message):
    diagnostic = "Replay cache misses (1): No cached response found"

    def respond(request):
        assert request.method == "POST"
        assert str(request.url) == "http://proxy/stop?skipWritingCache=true"
        return httpx.Response(status, text=diagnostic if status != 200 else "")

    client = httpx.AsyncClient(transport=httpx.MockTransport(respond))
    monkeypatch.setattr(httpx, "AsyncClient", lambda: client)
    process = Mock(returncode=exit_code)
    proxy = CapiProxy()
    proxy._process = process
    proxy._proxy_url = "http://proxy"

    if message is None:
        await proxy.stop(skip_writing_cache=True)
    else:
        with pytest.raises(RuntimeError, match=message):
            await proxy.stop(skip_writing_cache=True)

    process.wait.assert_called_once()
    assert proxy._process is None
    assert proxy._proxy_url is None
    await proxy.stop()


@pytest.mark.parametrize("cancel_during_reaping", [False, True])
async def test_cancelled_teardown_reaps_proxy(monkeypatch, tmp_path, cancel_during_reaping):
    monkeypatch.setenv("COPILOT_CLI_PATH", sys.executable)
    from e2e.testharness.context import E2ETestContext

    request_started = asyncio.Event()
    reaping_started = asyncio.Event()
    loop = asyncio.get_running_loop()

    async def respond(request):
        assert request.method == "POST"
        assert str(request.url) == "http://proxy/stop?skipWritingCache=true"
        request_started.set()
        await asyncio.Future()

    client = httpx.AsyncClient(transport=httpx.MockTransport(respond))
    monkeypatch.setattr(httpx, "AsyncClient", lambda: client)
    process = subprocess.Popen(
        [sys.executable, "-c", "import sys; sys.stdin.read()"],
        stdin=subprocess.PIPE,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    original_wait = process.wait

    def wait_for_exit(timeout):
        loop.call_soon_threadsafe(reaping_started.set)
        return original_wait(timeout=timeout)

    monkeypatch.setattr(process, "wait", wait_for_exit)
    proxy = CapiProxy()
    proxy._process = process
    proxy._proxy_url = "http://proxy"
    context = E2ETestContext()
    context._proxy = proxy
    context.home_dir = str(tmp_path / "home")
    context.work_dir = str(tmp_path / "work")
    (tmp_path / "home").mkdir()
    (tmp_path / "work").mkdir()
    teardown = asyncio.create_task(context.teardown(test_failed=True))
    reaping = None

    try:
        await request_started.wait()
        teardown.cancel()
        if cancel_during_reaping:
            reaping = asyncio.create_task(reaping_started.wait())
            done, _ = await asyncio.wait([teardown, reaping], return_when=asyncio.FIRST_COMPLETED)
            assert reaping in done, "Cancelled teardown returned without reaping its proxy"
            teardown.cancel()

        with pytest.raises(asyncio.CancelledError):
            await teardown

        assert process.returncode is not None, "Cancelled teardown left its proxy running"
        assert proxy._process is None
        assert proxy.url is None
        assert context._proxy is None
        assert not (tmp_path / "home").exists()
        assert not (tmp_path / "work").exists()
    finally:
        if reaping is not None:
            reaping.cancel()
            with contextlib.suppress(asyncio.CancelledError):
                await reaping
        if not teardown.done():
            teardown.cancel()
        with contextlib.suppress(asyncio.CancelledError):
            await teardown
        if process.poll() is None:
            process.kill()
        original_wait(timeout=5)
        assert process.stdin is not None
        process.stdin.close()
