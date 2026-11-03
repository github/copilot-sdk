"""Replay failures must survive language-wrapper cleanup."""

import asyncio
import contextlib
import json
import logging
import subprocess
import sys
from unittest.mock import Mock, call

import httpx
import pytest

from e2e.testharness.proxy import CapiProxy


def _exchange_context(monkeypatch):
    monkeypatch.setenv("COPILOT_CLI_PATH", sys.executable)
    from e2e.testharness.context import E2ETestContext

    context = E2ETestContext()
    context._proxy = CapiProxy()
    return context


def _run_with_frozen_clock(monkeypatch, scenario):
    async def run():
        now = 0.0

        def advance():
            nonlocal now
            now += 1.0

        # This loop belongs only to this scenario; no framework or other test shares it.
        with monkeypatch.context() as scoped:
            scoped.setattr(asyncio.get_running_loop(), "time", lambda: now)
            await scenario(advance)

    asyncio.run(run())


async def _checkpoint_before_exit(checkpoint, operation):
    receipt = asyncio.create_task(checkpoint.wait())
    try:
        done, _ = await asyncio.wait([receipt, operation], return_when=asyncio.FIRST_COMPLETED)
        if receipt not in done:
            result = await operation
            pytest.fail(f"Operation exited before its checkpoint: {result!r}")
    finally:
        receipt.cancel()
        with contextlib.suppress(asyncio.CancelledError):
            await receipt


async def test_exchange_wait_checks_buffered_readiness_at_owner_expiry(monkeypatch):
    context = _exchange_context(monkeypatch)
    ready = [{"request": {"id": "one"}}, {"request": {"id": "two"}}]
    calls = []

    async def get_exchanges(*, timeout=None):
        calls.append(timeout)
        return ready

    monkeypatch.setattr(context._proxy, "get_exchanges", get_exchanges)

    assert await context.wait_for_exchanges(2, timeout=0) is ready
    assert calls == [None]


@pytest.mark.parametrize("direct_proxy", [False, True])
async def test_direct_exchange_snapshot_has_no_competing_http_deadline(monkeypatch, direct_proxy):
    context = _exchange_context(monkeypatch)
    context._proxy._proxy_url = "http://proxy"
    ready = [{"request": {"id": "one"}}]
    actual_client = httpx.AsyncClient
    clients = []

    def respond(request):
        assert request.method == "GET"
        assert str(request.url) == "http://proxy/exchanges"
        return httpx.Response(200, json=ready)

    def create_client(*, timeout):
        assert timeout is None
        client = actual_client(timeout=timeout, transport=httpx.MockTransport(respond))
        assert client.timeout.as_dict() == dict.fromkeys(("connect", "read", "write", "pool"))
        clients.append(client)
        return client

    monkeypatch.setattr(httpx, "AsyncClient", create_client)
    getter = context._proxy.get_exchanges if direct_proxy else context.get_exchanges
    assert await getter() == ready
    assert len(clients) == 1
    assert clients[0].is_closed


async def test_exchange_wait_final_pending_snapshot_preserves_count_and_diagnostic(monkeypatch):
    context = _exchange_context(monkeypatch)
    calls = []

    async def get_exchanges(*, timeout=None):
        calls.append(timeout)
        return [{"request": {"id": "one"}}]

    monkeypatch.setattr(context._proxy, "get_exchanges", get_exchanges)

    with pytest.raises(
        TimeoutError, match=r"^Timed out waiting for 2 chat completion request\(s\)$"
    ) as failure:
        await context.wait_for_exchanges(2, timeout=0)

    assert isinstance(failure.value.__cause__, TimeoutError)
    assert calls == [None]


@pytest.mark.parametrize(
    "error_factory",
    [
        pytest.param(lambda: httpx.ReadTimeout("snapshot peer stalled"), id="transport"),
        pytest.param(lambda: ValueError("malformed snapshot"), id="body"),
        pytest.param(lambda: asyncio.CancelledError("snapshot cancelled"), id="cancellation"),
    ],
)
async def test_exchange_wait_preserves_final_snapshot_fault(monkeypatch, error_factory):
    context = _exchange_context(monkeypatch)
    failure = error_factory()
    calls = []

    async def get_exchanges(*, timeout=None):
        calls.append(timeout)
        raise failure

    monkeypatch.setattr(context._proxy, "get_exchanges", get_exchanges)

    with pytest.raises(type(failure)) as caught:
        await context.wait_for_exchanges(2, timeout=0)

    assert caught.value is failure
    assert calls == [None]


def test_exchange_wait_does_not_mistake_probe_timeout_for_owner_expiry(monkeypatch):
    context = _exchange_context(monkeypatch)
    failure = TimeoutError("probe itself failed")
    calls = []

    async def get_exchanges(*, timeout=None):
        calls.append(timeout)
        raise failure

    monkeypatch.setattr(context._proxy, "get_exchanges", get_exchanges)

    async def scenario(_advance):
        with pytest.raises(TimeoutError) as caught:
            await context.wait_for_exchanges(2, timeout=1)
        assert caught.value is failure
        assert calls == [None]

    _run_with_frozen_clock(monkeypatch, scenario)


def test_exchange_wait_joins_admitted_probe_before_final_snapshot(monkeypatch):
    context = _exchange_context(monkeypatch)

    async def scenario(advance):
        entered = asyncio.Event()
        cleanup_started = asyncio.Event()
        cleanup_release = asyncio.Event()
        cleanup_finished = asyncio.Event()
        ready = [{"request": {"id": "one"}}, {"request": {"id": "two"}}]
        calls = []

        async def get_exchanges(*, timeout=None):
            calls.append(timeout)
            if len(calls) == 1:
                entered.set()
                try:
                    await asyncio.Future()
                finally:
                    cleanup_started.set()
                    await cleanup_release.wait()
                    cleanup_finished.set()
            assert cleanup_finished.is_set(), "Final snapshot overlapped the original probe"
            return ready

        monkeypatch.setattr(context._proxy, "get_exchanges", get_exchanges)
        operation = asyncio.create_task(context.wait_for_exchanges(2, timeout=1))
        try:
            await _checkpoint_before_exit(entered, operation)
            advance()
            await _checkpoint_before_exit(cleanup_started, operation)
            assert calls == [None]
            assert not operation.done()
            cleanup_release.set()
            assert await operation is ready
            assert cleanup_finished.is_set()
            assert calls == [None, None]
        finally:
            cleanup_release.set()
            operation.cancel()
            with contextlib.suppress(asyncio.CancelledError):
                await operation

    _run_with_frozen_clock(monkeypatch, scenario)


async def test_exchange_wait_cancels_and_closes_final_snapshot_http_client(monkeypatch):
    context = _exchange_context(monkeypatch)
    context._proxy._proxy_url = "http://proxy"
    entered = asyncio.Event()
    request_finished = asyncio.Event()
    clients = []
    actual_client = httpx.AsyncClient

    async def respond(request):
        assert request.method == "GET"
        assert str(request.url) == "http://proxy/exchanges"
        entered.set()
        try:
            await asyncio.Future()
        finally:
            request_finished.set()

    def create_client(*, timeout):
        assert timeout is None
        client = actual_client(timeout=timeout, transport=httpx.MockTransport(respond))
        assert client.timeout.as_dict() == dict.fromkeys(("connect", "read", "write", "pool"), None)
        clients.append(client)
        return client

    monkeypatch.setattr(httpx, "AsyncClient", create_client)
    operation = asyncio.create_task(context.wait_for_exchanges(2, timeout=0))
    try:
        await _checkpoint_before_exit(entered, operation)
        operation.cancel()
        with pytest.raises(asyncio.CancelledError):
            await operation
        assert request_finished.is_set()
        assert len(clients) == 1
        assert clients[0].is_closed
    finally:
        operation.cancel()
        with contextlib.suppress(asyncio.CancelledError):
            await operation


@pytest.mark.parametrize("method", ["configure", "set_user"])
@pytest.mark.parametrize("status", [200, 500])
async def test_proxy_configuration_request_has_no_competing_http_deadline(
    monkeypatch, method, status
):
    proxy = CapiProxy()
    proxy._proxy_url = "http://proxy"
    actual_client = httpx.AsyncClient
    clients = []
    requests = []
    if method == "configure":
        path = "/config"
        payload = {"filePath": "fixture.yaml", "workDir": "fixture-workdir"}

        async def invoke():
            await proxy.configure(payload["filePath"], payload["workDir"])

        failure = pytest.raises(RuntimeError, match=r"^Proxy config failed with status 500$")
    else:
        path = "/copilot-user-config"
        payload = {"token": "fixture-token", "response": {"login": "fixture-user"}}

        async def invoke():
            await proxy.set_copilot_user_by_token(payload["token"], payload["response"])

        failure = pytest.raises(AssertionError)

    def respond(request):
        requests.append(request)
        return httpx.Response(status)

    def create_client(**kwargs):
        client = actual_client(transport=httpx.MockTransport(respond), **kwargs)
        clients.append(client)
        return client

    factory = Mock(side_effect=create_client)
    monkeypatch.setattr(httpx, "AsyncClient", factory)
    with contextlib.nullcontext() if status == 200 else failure:
        await invoke()
    assert len(requests) == 1
    assert requests[0].method == "POST"
    assert str(requests[0].url) == f"http://proxy{path}"
    assert json.loads(requests[0].content) == payload
    factory.assert_called_once_with(timeout=None)
    assert len(clients) == 1
    assert clients[0].timeout.as_dict() == dict.fromkeys(("connect", "read", "write", "pool"))
    assert clients[0].is_closed


@pytest.mark.parametrize("status", [200, 500])
async def test_oauth_request_snapshot_has_no_competing_http_deadline(monkeypatch, status):
    monkeypatch.setenv("COPILOT_CLI_PATH", sys.executable)
    from e2e.test_mcp_oauth_e2e import _requests

    ready = [{"method": "POST", "path": "/mcp"}]
    clients = []
    requests = []
    actual_client = httpx.AsyncClient

    def respond(request):
        requests.append(request)
        return httpx.Response(status, json=ready)

    def create_client(**kwargs):
        client = actual_client(transport=httpx.MockTransport(respond), **kwargs)
        clients.append(client)
        return client

    factory = Mock(side_effect=create_client)
    monkeypatch.setattr(httpx, "AsyncClient", factory)
    if status == 200:
        assert await _requests("http://oauth") == ready
    else:
        with pytest.raises(httpx.HTTPStatusError) as failure:
            await _requests("http://oauth")
        assert failure.value.response.status_code == 500
        assert failure.value.request is requests[0]
    assert len(requests) == 1
    assert requests[0].method == "GET"
    assert str(requests[0].url) == "http://oauth/__requests"
    factory.assert_called_once_with(timeout=None)
    assert len(clients) == 1
    assert clients[0].timeout.as_dict() == dict.fromkeys(("connect", "read", "write", "pool"))
    assert clients[0].is_closed


async def test_oauth_request_snapshot_cancellation_closes_http_client(monkeypatch):
    monkeypatch.setenv("COPILOT_CLI_PATH", sys.executable)
    from e2e.test_mcp_oauth_e2e import _requests

    entered = asyncio.Event()
    finished = asyncio.Event()
    clients = []
    requests = []
    actual_client = httpx.AsyncClient

    async def respond(request):
        requests.append(request)
        entered.set()
        try:
            await asyncio.Future()
        finally:
            finished.set()

    def create_client(**kwargs):
        client = actual_client(transport=httpx.MockTransport(respond), **kwargs)
        clients.append(client)
        return client

    factory = Mock(side_effect=create_client)
    monkeypatch.setattr(httpx, "AsyncClient", factory)
    operation = asyncio.create_task(_requests("http://oauth"))
    try:
        await _checkpoint_before_exit(entered, operation)
        operation.cancel()
        with pytest.raises(asyncio.CancelledError):
            await operation
        assert finished.is_set()
        assert len(requests) == 1
        assert requests[0].method == "GET"
        assert str(requests[0].url) == "http://oauth/__requests"
        factory.assert_called_once_with(timeout=None)
        assert len(clients) == 1
        assert clients[0].timeout.as_dict() == dict.fromkeys(("connect", "read", "write", "pool"))
        assert clients[0].is_closed
    finally:
        operation.cancel()
        with contextlib.suppress(asyncio.CancelledError):
            await operation


class _OAuthStartupProcess:
    def __init__(self):
        self.pid = 123
        self.returncode = None
        self.stdout = self
        self.output = asyncio.StreamReader()
        self.stderr = asyncio.StreamReader()
        self.read_entered = asyncio.Event()
        self.read_cleanup_entered = asyncio.Event()
        self.read_join_release = asyncio.Event()
        self.read_join_release.set()
        self.read_finished = asyncio.Event()
        self.reap_entered = asyncio.Event()
        self.reap_release = asyncio.Event()
        self.reaped = asyncio.Event()
        self.reads = 0
        self.terminations = 0
        self.kills = 0

    async def readline(self):
        self.reads += 1
        self.read_entered.set()
        try:
            return await self.output.readline()
        finally:
            self.read_cleanup_entered.set()
            await self.read_join_release.wait()
            self.read_finished.set()

    def terminate(self):
        self.terminations += 1

    def kill(self):
        self.kills += 1
        self.reap_release.set()

    async def wait(self):
        self.reap_entered.set()
        await self.reap_release.wait()
        self.returncode = 0
        self.stderr.feed_eof()
        self.reaped.set()
        return self.returncode


def _install_oauth_startup_process(monkeypatch, process):
    monkeypatch.setenv("COPILOT_CLI_PATH", sys.executable)
    import e2e.test_mcp_oauth_e2e as oauth

    spawns = []

    async def spawn(*args, **kwargs):
        spawns.append((args, kwargs))
        return process

    monkeypatch.setattr(asyncio, "create_subprocess_exec", spawn)
    return oauth, spawns


def test_oauth_startup_diagnostic_does_not_retire_admitted_read(monkeypatch):
    async def scenario(advance):
        process = _OAuthStartupProcess()
        process.reap_release.set()
        oauth, spawns = _install_oauth_startup_process(monkeypatch, process)
        registered = asyncio.Event()
        diagnostic = asyncio.Event()
        loop = asyncio.get_running_loop()
        call_at = loop.call_at

        def schedule(when, callback, *args, **kwargs):
            handle = call_at(when, callback, *args, **kwargs)
            if when - loop.time() == 10:
                registered.set()
            return handle

        warning = Mock(side_effect=lambda *_args: diagnostic.set())
        monkeypatch.setattr(loop, "call_at", schedule)
        monkeypatch.setattr(logging.getLogger(oauth.__name__), "warning", warning)
        operation = asyncio.create_task(oauth._start_oauth_mcp_server())
        try:
            await _checkpoint_before_exit(process.read_entered, operation)
            await _checkpoint_before_exit(registered, operation)
            for _ in range(9):
                advance()
            await asyncio.sleep(0)
            assert not diagnostic.is_set()
            advance()
            await _checkpoint_before_exit(diagnostic, operation)
            for _ in range(60):
                advance()
            await asyncio.sleep(0)
            assert not operation.done()
            assert not process.read_finished.is_set()
            assert process.terminations == process.kills == 0
            assert len(spawns) == process.reads == 1
            warning.assert_called_once_with(
                "OAuth MCP server PID %s has no startup line after 10s; still waiting",
                process.pid,
            )
            process.output.feed_data(b"Listening: http://oauth\n")
            url, returned_process = await operation
            assert url == "http://oauth"
            assert returned_process is process
            assert process.read_finished.is_set()
            assert process.terminations == 0
        finally:
            process.read_join_release.set()
            process.reap_release.set()
            if not operation.done():
                operation.cancel()
                with contextlib.suppress(asyncio.CancelledError):
                    await operation
            await oauth._stop_process(process)

    _run_with_frozen_clock(monkeypatch, scenario)


@pytest.mark.parametrize(
    "exit_kind", ["malformed", "eof", "read_fault", "cancel", "cancel_reader_join", "cancel_reap"]
)
def test_oauth_startup_failure_joins_reader_and_reaps_before_publication(monkeypatch, exit_kind):
    async def scenario(_advance):
        process = _OAuthStartupProcess()
        oauth, spawns = _install_oauth_startup_process(monkeypatch, process)
        if exit_kind == "cancel_reader_join":
            process.read_join_release.clear()
        if exit_kind == "eof":
            stderr_read = process.stderr.read

            async def read_stderr(n=-1):
                assert process.reaped.is_set(), "Startup stderr read preceded process reaping"
                return await stderr_read(n)

            monkeypatch.setattr(process.stderr, "read", read_stderr)
        read_failure = ValueError("startup reader failed")
        operation = asyncio.create_task(oauth._start_oauth_mcp_server())
        try:
            await _checkpoint_before_exit(process.read_entered, operation)
            if exit_kind == "malformed":
                process.output.feed_data(b"unexpected startup output\n")
            elif exit_kind == "eof":
                assert process.returncode is None
                assert not process.stderr.at_eof()
                process.output.feed_eof()
            elif exit_kind == "read_fault":
                process.output.set_exception(read_failure)
            else:
                operation.cancel()
            if exit_kind == "cancel_reader_join":
                await _checkpoint_before_exit(process.read_cleanup_entered, operation)
                operation.cancel()
                assert not operation.done()
                assert not process.read_finished.is_set()
                assert process.terminations == 0
                process.read_join_release.set()
            await _checkpoint_before_exit(process.reap_entered, operation)
            assert not operation.done()
            assert process.read_finished.is_set()
            assert process.terminations == 1
            assert not process.reaped.is_set()
            if exit_kind == "cancel_reap":
                operation.cancel()
            process.reap_release.set()
            if exit_kind == "malformed":
                with pytest.raises(
                    RuntimeError,
                    match=r"^Unexpected OAuth MCP server startup line: unexpected startup output$",
                ):
                    await operation
            elif exit_kind == "eof":
                with pytest.raises(
                    RuntimeError, match=r"^OAuth MCP server exited before listening: $"
                ):
                    await operation
            elif exit_kind == "read_fault":
                with pytest.raises(ValueError) as failure:
                    await operation
                assert failure.value is read_failure
            else:
                with pytest.raises(asyncio.CancelledError):
                    await operation
            assert process.read_finished.is_set()
            assert process.reaped.is_set()
            assert len(spawns) == process.reads == process.terminations == 1
            assert process.kills == 0
        finally:
            process.read_join_release.set()
            process.reap_release.set()
            if not operation.done():
                operation.cancel()
                with contextlib.suppress(asyncio.CancelledError):
                    await operation
            await oauth._stop_process(process)

    _run_with_frozen_clock(monkeypatch, scenario)


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


def test_proxy_reaping_reports_shutdown_grace_expiry(monkeypatch, caplog):
    from e2e.testharness import proxy as proxy_harness

    timeout = proxy_harness.PROCESS_SHUTDOWN_TIMEOUT_SECONDS
    process = Mock(pid=1234, returncode=None)
    process.wait.side_effect = [subprocess.TimeoutExpired("proxy", timeout), 1]
    killed = []

    def kill_process_tree(target):
        killed.append(target)
        target.returncode = 1

    monkeypatch.setattr(proxy_harness, "_kill_process_tree", kill_process_tree)

    proxy_harness._reap_process(process)

    assert killed == [process]
    assert process.returncode == 1
    assert process.wait.call_args_list == [call(timeout=timeout), call(timeout=timeout)]
    assert caplog.record_tuples == [
        (
            proxy_harness.__name__,
            logging.WARNING,
            f"Proxy process 1234 did not exit within {timeout} seconds during shutdown; "
            "killing its process tree",
        )
    ]


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
