"""Regression tests for timed-out tasks retaining module-fixture session locks."""

import os
import subprocess
import sys
from pathlib import Path
from types import SimpleNamespace

import pytest

from e2e.timeout_diagnostics import cancel_timed_out_test


@pytest.mark.parametrize(
    "cancel_test,withhold_all,teardown_error",
    [(False, False, True), (True, False, False), (True, True, True)],
)
def test_module_cleanup_after_timeout(tmp_path, cancel_test, withhold_all, teardown_error):
    (tmp_path / "conftest.py").write_text(
        """
import asyncio
import os
import signal
from types import SimpleNamespace

import pytest
import pytest_asyncio
import pytest_timeout

from copilot import CopilotClient, RuntimeConnection
from copilot._jsonrpc import JsonRpcClient
from copilot.session import CopilotSession
from e2e.timeout_diagnostics import add_timeout_diagnostics, cancel_timed_out_test
from e2e.timeout_diagnostics import pytest_timeout_set_timer as pytest_timeout_set_timer

active_item = None

def pytest_runtest_setup(item):
    global active_item
    active_item = item

@pytest.hookimpl(hookwrapper=True)
def pytest_runtest_makereport(item, call):
    outcome = yield
    report = outcome.get_result()
    add_timeout_diagnostics(item, call, report)
    if os.environ["CANCEL_TEST"] == "1" and report.when == "call" and report.failed:
        assert cancel_timed_out_test(call)

@pytest_asyncio.fixture(scope="module", loop_scope="module")
async def ctx():
    loop = asyncio.get_running_loop()
    rpc = JsonRpcClient(None)
    rpc._loop = loop
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    client._client = rpc
    client._state = "connected"
    session = CopilotSession("resumed-session", rpc)
    client._sessions[session.session_id] = session
    state = {"armed": False, "calls": 0}
    background = asyncio.create_task(asyncio.Event().wait())

    async def send(message):
        assert message["method"] == "session.detach"
        state["calls"] += 1
        if state["calls"] > 1 and os.environ["WITHHOLD_ALL"] == "0":
            rpc._handle_message({"id": message["id"], "result": {"success": True}})
        else:
            state["armed"] = True

    rpc._send_message = send
    if not hasattr(signal, "SIGALRM"):
        # Exercise the actual plugin's exception at the runner boundary on
        # Windows, where the real thread timeout would terminate the process.
        run_once = loop._run_once
        def interrupt_blocked_loop():
            if state["armed"] and not loop._ready:
                state["armed"] = False
                active_item.config.hook.pytest_timeout_cancel_timer(item=active_item)
                pytest_timeout.timeout_sigalrm(
                    active_item, pytest_timeout._get_item_settings(active_item)
                )
            run_once()
        loop._run_once = interrupt_blocked_loop

    yield SimpleNamespace(_client=client, session=session, state=state, background=background)
    try:
        state["armed"] = True
        await client.stop()
        assert state["calls"] == 2
        assert session._destroyed
        assert not session._disconnect_lock.locked()
        assert not rpc.pending_requests
    finally:
        background.cancel()
        await asyncio.gather(background, return_exceptions=True)
""",
        encoding="utf-8",
    )
    (tmp_path / "test_stalled.py").write_text(
        """
import signal
import pytest

pytestmark = [
    pytest.mark.asyncio(loop_scope="module"),
    pytest.mark.timeout(
        1 if hasattr(signal, "SIGALRM") else 20,
        method="signal" if hasattr(signal, "SIGALRM") else "thread",
    ),
]

async def test_first_detach_times_out(ctx):
    await ctx.session.disconnect()

async def test_later_test_passes(ctx):
    assert ctx.state["calls"] == 1
    assert not ctx.background.done()
""",
        encoding="utf-8",
    )
    env = dict(os.environ)
    env["CANCEL_TEST"] = str(int(cancel_test))
    env["WITHHOLD_ALL"] = str(int(withhold_all))
    env["PYTHONPATH"] = str(Path(__file__).parent.resolve())
    result = subprocess.run(
        [
            sys.executable,
            "-m",
            "pytest",
            "-v",
            "-s",
            "-n",
            "0",
            "--rootdir",
            str(tmp_path),
            "--basetemp",
            str(tmp_path / "workers"),
            str(tmp_path / "test_stalled.py"),
        ],
        cwd=tmp_path,
        env=env,
        capture_output=True,
        text=True,
        timeout=30,
        check=False,
    )
    output = result.stdout + result.stderr
    assert result.returncode == 1, output
    assert "1 failed, 1 passed" in output
    assert ("1 error" in output) == teardown_error
    assert "disconnect_locked=True" in output
    assert "outbound method=session.detach" in output
    assert "from pytest-timeout" in output


@pytest.mark.parametrize("when", ["setup", "call", "teardown"])
@pytest.mark.parametrize("error", [None, AssertionError("ordinary failure")])
def test_cleanup_ignores_other_failures(when, error):
    call = SimpleNamespace(
        when=when, excinfo=None if error is None else SimpleNamespace(value=error)
    )
    assert not cancel_timed_out_test(call)
