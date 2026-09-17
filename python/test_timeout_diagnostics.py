"""Regression tests for diagnostic evidence retained after an async timeout."""

import asyncio
import io
import os
import signal
import subprocess
import sys
from pathlib import Path
from types import SimpleNamespace

import pytest

from copilot._jsonrpc import JsonRpcClient
from copilot.client import CopilotClient, RuntimeConnection
from copilot.session import CopilotSession
from e2e import timeout_diagnostics
from e2e.timeout_diagnostics import (
    _dump_awaitable,
    _sample_native_threads,
    add_timeout_diagnostics,
)


@pytest.mark.parametrize("blocked_write", [False, True])
async def test_timeout_report_identifies_pending_rpc_without_payloads(
    tmp_path, monkeypatch, blocked_write
):
    rpc = JsonRpcClient(None)
    rpc._loop = asyncio.get_running_loop()
    sent = asyncio.Event()
    release_write = asyncio.Event()

    async def send_message(message):
        sent.set()
        if blocked_write:
            await release_write.wait()

    monkeypatch.setattr(rpc, "_send_message", send_message)
    task = asyncio.create_task(
        rpc.request(
            "session.resume",
            {"sessionId": "diagnostic-session", "githubToken": "secret-not-in-diagnostics"},
        ),
        name="pending-resume",
    )
    try:
        await sent.wait()
        client = SimpleNamespace(_client=rpc, _sessions={}, _ffi_host=None)
        item = SimpleNamespace(
            nodeid="test_session_config_e2e.py::test_resume",
            config=SimpleNamespace(rootpath=tmp_path),
            funcargs={"ctx": SimpleNamespace(_client=client)},
            stash=pytest.Stash(),
        )
        call = SimpleNamespace(
            excinfo=SimpleNamespace(value=Exception("Timeout (>300.0s) from pytest-timeout."))
        )
        report = SimpleNamespace(failed=True, when="call", sections=[])
        # Snapshotting must not try to acquire a potentially orphaned SDK lock.
        with rpc._pending_lock:
            add_timeout_diagnostics(item, call, report)

        title, text = report.sections[0]
        assert title == "Async timeout diagnostics"
        assert "Phase: call" in text
        assert "Task pending-resume" in text
        assert "JsonRpcClient.request" in text
        assert "outbound method=session.resume" in text
        assert "session_id=diagnostic-session" in text
        assert f"pending request_id={next(iter(rpc.pending_requests))} done=False" in text
        assert "pending_locked=True" in text
        if blocked_write:
            assert "<locals>.send_message" in text
        else:
            assert "awaiting FutureIter" in text
        assert "secret-not-in-diagnostics" not in text
        assert "githubToken" not in text
        (artifact,) = (tmp_path / ".pytest-diagnostics").glob("*.txt")
        assert artifact.read_text(encoding="utf-8") == text
        assert report.failed
        assert not task.done()
    finally:
        task.cancel()
        await asyncio.gather(task, return_exceptions=True)


async def test_teardown_dump_follows_async_generator_and_disconnect_lock(monkeypatch):
    rpc = JsonRpcClient(None)
    rpc._loop = asyncio.get_running_loop()
    sent = asyncio.Event()

    async def send_message(message):
        sent.set()

    monkeypatch.setattr(rpc, "_send_message", send_message)
    session = CopilotSession("diagnostic-session", rpc)
    disconnect = asyncio.create_task(session.disconnect())
    teardown_started = asyncio.Event()

    async def fixture():
        yield
        teardown_started.set()
        await session.disconnect()

    generator = fixture()
    await anext(generator)

    async def finalize():
        await anext(generator)

    finalizer = None
    try:
        await sent.wait()
        finalizer = asyncio.create_task(finalize())
        await teardown_started.wait()
        output = io.StringIO()
        _dump_awaitable(finalizer.get_coro(), output)
        text = output.getvalue()
        assert "async_generator_asend" in text
        assert "<locals>.fixture" in text
        assert "CopilotSession.disconnect" in text
        assert "Lock.acquire" in text
        assert not finalizer.done()
        assert not disconnect.done()
    finally:
        tasks = [disconnect] + ([finalizer] if finalizer is not None else [])
        for task in tasks:
            task.cancel()
        await asyncio.gather(*tasks, return_exceptions=True)
        await generator.aclose()


@pytest.mark.parametrize("error", [None, AssertionError("ordinary failure")])
def test_non_timeout_does_not_collect_diagnostics(error):
    item = SimpleNamespace()
    call = SimpleNamespace(excinfo=None if error is None else SimpleNamespace(value=error))
    report = SimpleNamespace(failed=error is not None, sections=[])
    add_timeout_diagnostics(item, call, report)
    assert report.sections == []


@pytest.mark.parametrize("process_id", [None, 12345])
def test_native_sample_is_bounded_and_targets_requested_process(tmp_path, monkeypatch, process_id):
    calls = []

    def run(args, **kwargs):
        calls.append((args, kwargs))
        return SimpleNamespace(returncode=0)

    monkeypatch.setattr(subprocess, "run", run)
    path = tmp_path / "native.sample.txt"
    output = io.StringIO()
    _sample_native_threads(path, output, process_id)
    args, options = calls[0]
    expected_pid = os.getpid() if process_id is None else process_id
    assert args == ["sample", str(expected_pid), "1", "-file", str(path)]
    assert options["timeout"] == 10
    assert "Native sample exit=0" in output.getvalue()


async def test_timeout_samples_owned_cli_from_pending_test_frame(tmp_path, monkeypatch):
    rpc = JsonRpcClient(None)
    rpc._loop = asyncio.get_running_loop()
    context_client = SimpleNamespace(_client=rpc, _sessions={}, _ffi_host=None)
    item = SimpleNamespace(
        nodeid="test_external_resume",
        config=SimpleNamespace(rootpath=tmp_path),
        funcargs={"ctx": SimpleNamespace(_client=context_client)},
    )
    owner = CopilotClient(connection=RuntimeConnection.for_stdio(path=sys.executable))
    process = subprocess.Popen(
        [sys.executable, "-c", "import sys; sys.stdin.read()"],
        stdin=subprocess.PIPE,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    owner._cli_process = process
    started = asyncio.Event()
    samples = []

    async def pending_test(server):
        started.set()
        await asyncio.Future()

    def sample(path, output, process_id=None):
        samples.append(process_id)
        path.write_text("native stack", encoding="utf-8")

    task = asyncio.create_task(pending_test(owner))
    try:
        await started.wait()
        monkeypatch.setattr(sys, "platform", "darwin")
        monkeypatch.setattr(timeout_diagnostics, "_sample_native_threads", sample)
        text, path = timeout_diagnostics._collect_timeout_diagnostics(item)

        assert samples == [process.pid]
        assert f"CLI pid={process.pid}" in text
        assert path is not None
        (native_sample,) = path.parent.glob("*.sample.txt")
        assert native_sample.read_text(encoding="utf-8") == "native stack"
    finally:
        task.cancel()
        await asyncio.gather(task, return_exceptions=True)
        process.terminate()
        process.wait(timeout=10)
        if process.stdin is not None:
            process.stdin.close()
        owner._cli_process = None


@pytest.mark.parametrize(
    "error", [FileNotFoundError(), subprocess.TimeoutExpired("sample", timeout=10)]
)
def test_native_sample_failure_preserves_diagnostics(tmp_path, monkeypatch, error):
    def run(*args, **kwargs):
        raise error

    monkeypatch.setattr(subprocess, "run", run)
    output = io.StringIO()
    _sample_native_threads(tmp_path / "native.sample.txt", output)
    assert f"Native sample unavailable: {type(error).__name__}" in output.getvalue()


async def test_signal_snapshot_precedes_task_cleanup_and_keeps_original_failure(
    tmp_path, monkeypatch
):
    installed = []
    failure = pytest.fail.Exception("Timeout (>300.0s) from pytest-timeout.")

    def original_handler(signum, frame):
        raise failure

    monkeypatch.setattr(signal, "SIGALRM", 12345, raising=False)
    monkeypatch.setattr(signal, "getsignal", lambda signum: original_handler)
    monkeypatch.setattr(signal, "signal", lambda signum, handler: installed.append(handler))
    rpc = JsonRpcClient(None)
    rpc._loop = asyncio.get_running_loop()
    client = SimpleNamespace(_client=rpc, _sessions={}, _ffi_host=None)
    item = SimpleNamespace(
        nodeid="test_teardown",
        config=SimpleNamespace(rootpath=tmp_path),
        funcargs={"ctx": SimpleNamespace(_client=client)},
        stash=pytest.Stash(),
    )
    hook = timeout_diagnostics.pytest_timeout_set_timer(item, SimpleNamespace(method="signal"))
    next(hook)
    with pytest.raises(StopIteration):
        next(hook)
    started = asyncio.Event()

    async def teardown_waiting_for_disconnect():
        started.set()
        await asyncio.Future()

    task = asyncio.create_task(teardown_waiting_for_disconnect())
    try:
        await started.wait()
        with pytest.raises(pytest.fail.Exception) as caught:
            installed[0](signal.SIGALRM, None)
        assert caught.value is failure
    finally:
        task.cancel()
        await asyncio.gather(task, return_exceptions=True)

    report = SimpleNamespace(failed=True, when="teardown", sections=[])
    add_timeout_diagnostics(item, SimpleNamespace(excinfo=SimpleNamespace(value=failure)), report)
    text = report.sections[0][1]
    assert "Phase: teardown" in text
    assert (
        "in test_signal_snapshot_precedes_task_cleanup_and_keeps_original_failure.<locals>." in text
    )
    assert "teardown_waiting_for_disconnect" in text
    assert not item.stash


@pytest.mark.parametrize("phase", ["call", "teardown"] if hasattr(signal, "SIGALRM") else ["call"])
def test_timeout_report_survives_xdist_without_stdout_capture(tmp_path, phase):
    # Exercise the real signal timeout on POSIX. On Windows emulate its exception
    # at the event-loop boundary, since the thread timeout terminates the worker.
    (tmp_path / "conftest.py").write_text(
        """
import pytest
from e2e.timeout_diagnostics import add_timeout_diagnostics
from e2e.timeout_diagnostics import pytest_timeout_set_timer as pytest_timeout_set_timer

@pytest.hookimpl(hookwrapper=True)
def pytest_runtest_makereport(item, call):
    outcome = yield
    add_timeout_diagnostics(item, call, outcome.get_result())
""",
        encoding="utf-8",
    )
    (tmp_path / "test_stalled.py").write_text(
        """
import asyncio
import signal
import pytest

@pytest.fixture
def runner():
    with asyncio.Runner() as runner:
        yield runner

def stall(runner):
    loop = runner.get_loop()
    if not hasattr(signal, "SIGALRM"):
        run_once = loop._run_once
        def interrupt_loop():
            run_once()
            loop._run_once = run_once
            pytest.fail("Timeout (>1.0s) from pytest-timeout.")
        loop._run_once = interrupt_loop

    async def stalled_rpc():
        await asyncio.Future()

    runner.run(stalled_rpc())

@pytest.fixture
def cleanup(runner):
    yield
    if PHASE == "teardown":
        stall(runner)

@pytest.mark.timeout(
    1 if hasattr(signal, "SIGALRM") else 10,
    method="signal" if hasattr(signal, "SIGALRM") else "thread",
)
def test_stalled(runner, cleanup):
    if PHASE == "call":
        stall(runner)
""",
        encoding="utf-8",
    )
    with (tmp_path / "test_stalled.py").open("a", encoding="utf-8") as source:
        source.write(f"\nPHASE = {phase!r}\n")
    env = dict(os.environ)
    env["PYTHONPATH"] = str(Path(__file__).parent.resolve())
    result = subprocess.run(
        [
            sys.executable,
            "-m",
            "pytest",
            "-v",
            "-s",
            "-n",
            "1",
            "--dist=loadfile",
            "--basetemp",
            str(tmp_path / "workers"),
            "--rootdir",
            str(tmp_path),
            str(tmp_path / "test_stalled.py"),
        ],
        cwd=tmp_path,
        env=env,
        capture_output=True,
        text=True,
        timeout=30,
        check=False,
    )
    assert result.returncode == 1, result.stdout + result.stderr
    assert ("1 failed" if phase == "call" else "1 error") in result.stdout
    assert "Async timeout diagnostics" in result.stdout
    assert f"Phase: {phase}" in result.stdout
    assert "stalled_rpc" in result.stdout
    assert "awaiting FutureIter" in result.stdout
    (artifact,) = (tmp_path / ".pytest-diagnostics").glob("*.txt")
    assert "stalled_rpc" in artifact.read_text(encoding="utf-8")
