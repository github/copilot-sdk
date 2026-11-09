"""TCP child-process startup regressions against a stand-in CLI."""

import asyncio
import os
import socket
import subprocess
import sys

import pytest

import copilot.client as client_module
from copilot import CopilotClient, RuntimeConnection
from copilot._jsonrpc import ProcessExitedError
from copilot._sdk_protocol_version import get_sdk_protocol_version

EXITS_EARLY = "import sys; sys.stderr.write('stub boom\\n'); sys.exit(3)"

# Announces a port, then writes far more stderr than a pipe buffer holds. The marker
# file is written only if every write completes, i.e. only if the SDK drains the pipe.
CHATTY_AFTER_PORT = """
import pathlib, socket, sys, time
gate, marker = map(pathlib.Path, sys.argv[1:3])
server = socket.socket()
server.bind(("127.0.0.1", 0))
server.listen()
print(f"listening on port {server.getsockname()[1]}", flush=True)
while not gate.exists():
    time.sleep(0.01)
for _ in range(1024):
    sys.stderr.write("x" * 1023 + "\\n")
    sys.stderr.flush()
open(marker, "w").close()
time.sleep(60)
"""

CHATTY_WITHOUT_NEWLINE = """
import socket, sys, time
server = socket.socket()
server.bind(("127.0.0.1", 0))
server.listen()
sys.stderr.write("x" * (128 * 1024) + "unterminated-diagnostic")
sys.stderr.flush()
print(f"listening on port {server.getsockname()[1]}", flush=True)
time.sleep(60)
"""

NEVER_ANNOUNCES_PORT = """
import sys, time
sys.stderr.write("old diagnostic\\n" + "x" * (128 * 1024) + "\\nstartup failed\\n")
sys.stderr.flush()
time.sleep(60)
"""

EXITS_AFTER_REQUEST = """
import json, socket, sys
server = socket.socket()
server.bind(("127.0.0.1", 0))
server.listen()
print(f"listening on port {server.getsockname()[1]}", flush=True)
conn, _ = server.accept()
stream = conn.makefile("rwb")

def read_request():
    header = stream.readline()
    assert stream.readline() == b"\\r\\n"
    return json.loads(stream.read(int(header.split(b":")[1])))

request = read_request()
assert request["method"] == "connect"
response = json.dumps({
    "jsonrpc": "2.0", "id": request["id"],
    "result": {"ok": True, "protocolVersion": int(sys.argv[1]), "version": "test"},
}).encode()
stream.write(f"Content-Length: {len(response)}\\r\\n\\r\\n".encode() + response)
stream.flush()
assert read_request()["method"] == "ping"
sys.stderr.write("old diagnostic\\n" + "x" * (128 * 1024) + "\\nstub boom\\n")
sys.stderr.flush()
sys.exit(3)
"""


def tcp_client(*args: str) -> CopilotClient:
    return CopilotClient(connection=RuntimeConnection.for_tcp(path=sys.executable, args=args))


@pytest.fixture
def spawned(monkeypatch):
    """Every child the client spawns, still reachable after the client drops its reference."""
    processes: list[subprocess.Popen] = []
    popen = subprocess.Popen

    def record(*args, **kwargs):
        process = popen(*args, **kwargs)
        processes.append(process)
        return process

    monkeypatch.setattr(client_module.subprocess, "Popen", record)
    yield processes
    for process in processes:
        if process.poll() is None:
            process.kill()
            process.wait(timeout=5)


async def test_tcp_startup_failure_reports_exit_code_and_stderr():
    client = tcp_client("-c", EXITS_EARLY)
    try:
        with pytest.raises(RuntimeError) as error:
            await client.start()
    finally:
        await client.force_stop()

    message = str(error.value)
    assert "CLI process exited before announcing port" in message
    assert "exit code 3" in message
    assert "stderr: stub boom" in message


async def test_tcp_child_stderr_is_drained_after_port_announcement(tmp_path):
    gate = tmp_path / "start-stderr"
    marker = tmp_path / "stderr-written"
    client = tcp_client("-c", CHATTY_AFTER_PORT, str(gate), str(marker))
    try:
        await client._start_cli_server()
        assert not marker.exists()
        gate.touch()
        async with asyncio.timeout(10):
            while not marker.exists():
                await asyncio.sleep(0.05)
    finally:
        await client.force_stop()


async def test_tcp_child_stderr_without_newlines_is_logged_before_exit(caplog):
    client = tcp_client("-c", CHATTY_WITHOUT_NEWLINE)
    try:
        await client._start_cli_server()
        async with asyncio.timeout(5):
            while "unterminated-diagnostic" not in "".join(
                record.getMessage().removeprefix("[CLI] ")
                for record in caplog.records
                if record.name == "copilot.client"
            ):
                await asyncio.sleep(0.01)
        assert client._process is not None
        assert client._process.poll() is None
    finally:
        await client.force_stop()


async def test_tcp_startup_timeout_reports_bounded_stderr_from_live_child():
    client = tcp_client("-c", NEVER_ANNOUNCES_PORT)
    try:
        with pytest.raises(RuntimeError, match="Timeout waiting for CLI server to start") as error:
            await client._start_cli_server()
        assert client._process is not None
        assert client._process.poll() is None
    finally:
        await client.force_stop()

    message = str(error.value)
    assert "startup failed" in message
    assert "stderr:" in message
    assert "old diagnostic" not in message
    assert len(message) < 65 * 1024


async def test_tcp_startup_timeout_stops_spawned_child(spawned):
    client = tcp_client("-c", NEVER_ANNOUNCES_PORT)
    try:
        with pytest.raises(RuntimeError, match="Timeout waiting for CLI server to start") as error:
            await client.start()
        assert "stderr:" in str(error.value)
        [process] = spawned
        assert process.poll() is not None
        assert client._cli_process is None
        assert client._state == "error"
    finally:
        await client.force_stop()


async def test_cancelled_tcp_startup_stops_spawned_child(spawned):
    client = tcp_client("-c", NEVER_ANNOUNCES_PORT)
    start = asyncio.create_task(client.start())
    try:
        async with asyncio.timeout(5):
            while not spawned:
                await asyncio.sleep(0.01)
        start.cancel()
        with pytest.raises(asyncio.CancelledError):
            await start
        [process] = spawned
        assert process.poll() is not None
        assert client._cli_process is None
        assert client._state == "disconnected"
    finally:
        start.cancel()
        await client.force_stop()


async def test_tcp_connect_failure_stops_spawned_child(spawned, monkeypatch):
    client = tcp_client(
        "-c", "import time; print('listening on port 1', flush=True); time.sleep(60)"
    )

    def refuse_connection(*args, **kwargs):
        raise ConnectionRefusedError("stub connection refused")

    monkeypatch.setattr(socket, "create_connection", refuse_connection)
    try:
        with pytest.raises(RuntimeError, match="stub connection refused"):
            await client.start()
        [process] = spawned
        assert process.poll() is not None
        assert client._cli_process is None
    finally:
        await client.force_stop()


async def test_tcp_pending_request_reports_exit_code_and_bounded_stderr():
    client = tcp_client("-c", EXITS_AFTER_REQUEST, str(get_sdk_protocol_version()))
    try:
        await client.start()
        process = client._cli_process
        rpc = client._client
        assert process is not None
        assert rpc is not None
        with pytest.raises(ProcessExitedError) as error:
            await asyncio.wait_for(client.ping(), timeout=10)
    finally:
        await client.force_stop()

    message = str(error.value)
    assert "CLI process exited with code 3" in message
    assert "stub boom" in message
    assert "old diagnostic" not in message
    assert len(message) < 65 * 1024


async def test_tcp_socket_eof_before_exit_reports_final_child_diagnostics(tmp_path, monkeypatch):
    gate = tmp_path / "allow-exit"
    script = EXITS_AFTER_REQUEST.replace(
        "sys.exit(3)",
        """
import pathlib, time
stream.close()
conn.close()
while not pathlib.Path(sys.argv[2]).exists():
    time.sleep(0.01)
sys.stderr.write("final diagnostic\\n")
sys.stderr.flush()
sys.exit(3)
""",
    )
    client = tcp_client("-c", script, str(get_sdk_protocol_version()), str(gate))
    try:
        await client.start()
        process = client._cli_process
        assert process is not None
        wait = process.wait

        def allow_exit(*args, **kwargs):
            gate.touch()
            return wait(*args, **kwargs)

        with monkeypatch.context() as patch:
            patch.setattr(process, "wait", allow_exit)
            with pytest.raises(ProcessExitedError) as error:
                await asyncio.wait_for(client.ping(), timeout=10)
        assert "CLI process exited with code 3" in str(error.value)
        assert "final diagnostic" in str(error.value)
    finally:
        await client.force_stop()


async def test_tcp_connect_failure_reports_current_child_diagnostics(monkeypatch):
    client = tcp_client("-c", "print('listening on port 1', flush=True); " + EXITS_EARLY)

    def refuse_connection(*args, **kwargs):
        assert client._cli_process is not None
        client._cli_process.wait(timeout=5)
        raise ConnectionRefusedError("stub connection refused")

    monkeypatch.setattr(socket, "create_connection", refuse_connection)
    try:
        with pytest.raises(RuntimeError) as error:
            await client.start()
    finally:
        await client.force_stop()

    assert "CLI process exited with code 3" in str(error.value)
    assert "stderr: stub boom" in str(error.value)


async def test_tcp_stderr_preserves_lines_across_short_reads(monkeypatch, caplog):
    client = tcp_client(
        "-c", "import sys; sys.stderr.write('first line\\nsecond line\\nlast line'); sys.exit(3)"
    )
    read = os.read

    def short_read(fd, size):
        process = client._cli_process
        if process is not None and process.stderr is not None and fd == process.stderr.fileno():
            size = 1
        return read(fd, size)

    monkeypatch.setattr(os, "read", short_read)
    try:
        with pytest.raises(RuntimeError, match="exit code 3"):
            await client.start()
    finally:
        await client.force_stop()

    assert [
        record.getMessage() for record in caplog.records if record.getMessage().startswith("[CLI]")
    ] == ["[CLI] first line", "[CLI] second line", "[CLI] last line"]


async def test_tcp_connection_loss_bounds_wait_for_live_child(monkeypatch):
    script = EXITS_AFTER_REQUEST.replace(
        "sys.exit(3)", "import time; stream.close(); conn.close(); time.sleep(60)"
    )
    client = tcp_client("-c", script, str(get_sdk_protocol_version()))
    try:
        await client.start()
        process = client._cli_process
        assert process is not None
        wait_calls = []
        wait = process.wait

        def record_wait(*args, **kwargs):
            wait_calls.append(kwargs)
            return wait(*args, **kwargs)

        with monkeypatch.context() as patch:
            patch.setattr(process, "wait", record_wait)
            with pytest.raises(ProcessExitedError) as error:
                await asyncio.wait_for(client.ping(), timeout=10)
            assert wait_calls == [{"timeout": 1.0}]
            assert "connection" in str(error.value).lower()
            assert "process exited" not in str(error.value)
            assert process.poll() is None
    finally:
        await client.force_stop()


async def test_tcp_restart_failure_does_not_use_previous_child_diagnostics(tmp_path):
    script = (
        """
import pathlib, sys
marker = pathlib.Path(sys.argv[2])
if marker.exists():
    sys.stderr.write("replacement boom\\n")
    sys.exit(7)
marker.touch()
"""
        + EXITS_AFTER_REQUEST
    )
    client = tcp_client("-c", script, str(get_sdk_protocol_version()), str(tmp_path / "started"))
    try:
        await client.start()
        previous_process = client._cli_process
        previous_client = client._client
        assert previous_process is not None
        assert previous_client is not None
        with pytest.raises(ProcessExitedError):
            await asyncio.wait_for(client.ping(), timeout=10)
        await asyncio.to_thread(previous_process.wait, timeout=5)
        async with asyncio.timeout(5):
            while client._state != "disconnected":
                await asyncio.sleep(0.01)
        previous_client.process.terminate()
        await previous_client.stop()

        with pytest.raises(RuntimeError) as error:
            await client.start()
    finally:
        await client.force_stop()

    assert "exit code 7" in str(error.value)
    assert "stderr: replacement boom" in str(error.value)
    assert "stub boom" not in str(error.value)
