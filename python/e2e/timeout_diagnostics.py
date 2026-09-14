"""Failure-only diagnostics for async E2E timeouts, including xdist workers."""

import asyncio
import gc
import inspect
import io
import os
import signal
import subprocess
import sys
import threading
import time
import traceback
import uuid
from pathlib import Path

import pytest

from copilot._jsonrpc import JsonRpcClient

_TIMEOUT_DIAGNOSTICS = pytest.StashKey[tuple[str, Path | None]]()


@pytest.hookimpl(hookwrapper=True, optionalhook=True)
def pytest_timeout_set_timer(item, settings):
    yield
    if settings.method != "signal" or threading.current_thread() is not threading.main_thread():
        return
    original_handler = signal.getsignal(signal.SIGALRM)
    if not callable(original_handler):
        return

    def capture_timeout(signum, frame):
        try:
            original_handler(signum, frame)
        except pytest.fail.Exception as exc:
            if "from pytest-timeout" in str(exc):
                # Capture before fixture finalizers/Runner.close cancel the
                # suspended tasks. In particular, makereport is too late for teardown.
                item.stash[_TIMEOUT_DIAGNOSTICS] = _collect_timeout_diagnostics(item)
            raise

    signal.signal(signal.SIGALRM, capture_timeout)


def _dump_awaitable(awaitable, output, seen=None):
    if seen is None:
        seen = set()
    while awaitable is not None and id(awaitable) not in seen:
        seen.add(id(awaitable))
        frame = None
        next_awaitable = None
        for frame_attr, await_attr in (
            ("cr_frame", "cr_await"),
            ("ag_frame", "ag_await"),
            ("gi_frame", "gi_yieldfrom"),
        ):
            frame = getattr(awaitable, frame_attr, None)
            if frame is not None:
                next_awaitable = getattr(awaitable, await_attr, None)
                break
        if frame is not None:
            code = frame.f_code
            print(f"  {code.co_filename}:{frame.f_lineno} in {code.co_qualname}", file=output)
            # Do not dump arbitrary locals, RPC payloads, prompts, tokens, or results.
            if code is JsonRpcClient.request.__code__:
                values = frame.f_locals
                params = values.get("params") or {}
                print(
                    f"    outbound method={values.get('method')}"
                    f" request_id={values.get('request_id')}"
                    f" session_id={params.get('sessionId')}"
                    f" elapsed={time.perf_counter() - values['request_start']:.3f}s",
                    file=output,
                )
            elif code is JsonRpcClient._dispatch_request.__code__:
                message = frame.f_locals["message"]
                print(
                    f"    inbound method={message.get('method')} request_id={message.get('id')}",
                    file=output,
                )
        else:
            print(f"  awaiting {type(awaitable).__name__}", file=output)
            # Async fixture finalizers await an asend object, which hides ag_await.
            if type(awaitable).__name__ in ("async_generator_asend", "async_generator_athrow"):
                for referent in gc.get_referents(awaitable):
                    if inspect.isasyncgen(referent):
                        _dump_awaitable(referent, output, seen)
        awaitable = next_awaitable


def _dump_client(client, output):
    rpc = client._client
    if rpc is not None:
        reader = rpc._read_thread
        print(
            f"JSON-RPC running={rpc._running}"
            f" reader_alive={reader is not None and reader.is_alive()}"
            f" write_locked={rpc._write_lock.locked()}"
            f" pending_locked={rpc._pending_lock.locked()}",
            file=output,
        )
        # A timeout may have interrupted a lock owner: snapshots must not acquire locks.
        for request_id, future in rpc.pending_requests.copy().items():
            print(
                f"  pending request_id={request_id} done={future.done()}"
                f" cancelled={future.cancelled()}",
                file=output,
            )
    for session_id, session in client._sessions.copy().items():
        print(
            f"Session {session_id} destroyed={session._destroyed}"
            f" disconnect_locked={session._disconnect_lock.locked()}",
            file=output,
        )
    host = client._ffi_host
    if host is not None:
        print(
            f"FFI server_id={host._server_id} connection_id={host._connection_id}"
            f" disposed={host._disposed} starting={host._starting}"
            f" operation_locked={host._operation_lock.locked()}"
            f" dispose_locked={host._dispose_lock.locked()}"
            f" receive_closed={host._receive_buffer._closed}"
            f" receive_bytes={len(host._receive_buffer._buffer)}",
            file=output,
        )


def _sample_native_threads(path: Path, output):
    try:
        result = subprocess.run(
            ["sample", str(os.getpid()), "1", "-file", str(path)],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            timeout=10,
            check=False,
        )
        print(f"Native sample exit={result.returncode} file={path}", file=output)
        if result.returncode:
            print(result.stdout, file=output)
    except (OSError, subprocess.TimeoutExpired) as exc:
        print(f"Native sample unavailable: {type(exc).__name__}", file=output)


def _collect_timeout_diagnostics(item):
    output = io.StringIO()
    print(f"Test: {item.nodeid}\nPID: {os.getpid()}", file=output)
    client = None
    try:
        context = item.funcargs.get("ctx")
        client = getattr(context, "_client", None)
        loops = set()
        if client is not None:
            _dump_client(client, output)
            if client._client is not None and client._client._loop is not None:
                loops.add(client._client._loop)
        for fixture in item.funcargs.values():
            if isinstance(fixture, asyncio.Runner):
                loops.add(fixture.get_loop())
        for loop in loops:
            print(f"Event loop running={loop.is_running()} closed={loop.is_closed()}", file=output)
            for task in sorted(asyncio.all_tasks(loop), key=lambda task: task.get_name()):
                print(
                    f"Task {task.get_name()} done={task.done()} cancelling={task.cancelling()}",
                    file=output,
                )
                _dump_awaitable(task.get_coro(), output)
        names = {thread.ident: thread.name for thread in threading.enumerate()}
        for ident, frame in sys._current_frames().items():
            print(f"Thread {ident} ({names.get(ident, 'native')})", file=output)
            traceback.print_stack(frame, file=output)
    except Exception as exc:
        print(f"Diagnostic collection failed: {type(exc).__name__}", file=output)

    path = None
    try:
        directory = item.config.rootpath / ".pytest-diagnostics"
        directory.mkdir(exist_ok=True)
        stem = f"{os.getpid()}-{uuid.uuid4().hex}"
        path = directory / f"{stem}.txt"
        path.write_text(output.getvalue(), encoding="utf-8")
        if sys.platform == "darwin" and getattr(client, "_ffi_host", None) is not None:
            _sample_native_threads(directory / f"{stem}.sample.txt", output)
        print(f"Diagnostics saved to {path}", file=output)
        path.write_text(output.getvalue(), encoding="utf-8")
    except Exception as exc:
        print(f"Diagnostic artifact unavailable: {type(exc).__name__}", file=output)
    return output.getvalue(), path


def add_timeout_diagnostics(item, call, report):
    """Attach the timeout snapshot to a report so it survives xdist's suppressed stdout."""
    if call.excinfo is None:
        return
    error = str(call.excinfo.value)
    if not report.failed or "Timeout (" not in error or "from pytest-timeout" not in error:
        return
    snapshot = item.stash.get(_TIMEOUT_DIAGNOSTICS, None)
    if snapshot is None:
        snapshot = _collect_timeout_diagnostics(item)
    else:
        del item.stash[_TIMEOUT_DIAGNOSTICS]
    text, path = snapshot
    text = f"Phase: {report.when}\n{text}"
    if path is not None:
        try:
            path.write_text(text, encoding="utf-8")
        except OSError as exc:
            text += f"Diagnostic artifact update failed: {type(exc).__name__}\n"
    report.sections.append(("Async timeout diagnostics", text))


def cancel_timed_out_test(call):
    """Cancel only the test task abandoned by a timeout outside its coroutine."""
    if call.when != "call" or call.excinfo is None:
        return False
    error = call.excinfo.value
    if "Timeout (" not in str(error) or "from pytest-timeout" not in str(error):
        return False
    traceback_entry = error.__traceback__
    while traceback_entry is not None:
        frame = traceback_entry.tb_frame
        if frame.f_code is asyncio.BaseEventLoop.run_until_complete.__code__:
            task = frame.f_locals.get("future")
            if isinstance(task, asyncio.Task) and not task.done():
                # The signal interrupts the runner, not its task. Let cancellation
                # release that task's locks when the fixture's loop next resumes.
                return task.cancel()
            return False
        traceback_entry = traceback_entry.tb_next
    return False
