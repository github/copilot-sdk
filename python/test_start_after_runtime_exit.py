"""start() after the runtime a client spawned has exited, against stand-in runtimes."""

import asyncio
import subprocess
import sys
from unittest.mock import AsyncMock

import pytest

from copilot import CopilotClient, RuntimeConnection
from copilot._jsonrpc import ProcessExitedError
from copilot._sdk_protocol_version import get_sdk_protocol_version

# Serves the `connect` handshake over stdio or one TCP connection, then reads one `ping`
# and never answers it. After that, by mode: "alive" runs until the stop file exists;
# "exits" exits with code 3; "descendant" first starts a process that inherits the
# transport (stdout and stderr for stdio, the connection for TCP) and runs until the stop
# file exists, then exits with code 3.
RUNTIME = """
import json, os, socket, subprocess, sys, time
transport, mode, protocol_version, stop = sys.argv[1:5]
if transport == "tcp":
    server = socket.socket()
    server.bind(("127.0.0.1", 0))
    server.listen()
    print(f"listening on port {server.getsockname()[1]}", flush=True)
    connection, _ = server.accept()
    reader = writer = connection.makefile("rwb")
else:
    reader, writer = sys.stdin.buffer, sys.stdout.buffer

def read_request():
    header = reader.readline()
    assert reader.readline() == b"\\r\\n"
    return json.loads(reader.read(int(header.split(b":")[1])))

request = read_request()
assert request["method"] == "connect"
response = json.dumps({
    "jsonrpc": "2.0", "id": request["id"],
    "result": {"ok": True, "protocolVersion": int(protocol_version), "version": "test"},
}).encode()
writer.write(f"Content-Length: {len(response)}\\r\\n\\r\\n".encode() + response)
writer.flush()
assert read_request()["method"] == "ping"
wait_for_stop = "import os, sys, time\\nwhile not os.path.exists(sys.argv[1]): time.sleep(0.02)"
if mode == "descendant":
    holder = [sys.executable, "-c", wait_for_stop, stop]
    if transport == "tcp":
        subprocess.Popen(holder, stdin=subprocess.DEVNULL, pass_fds=(connection.fileno(),))
    else:
        subprocess.Popen(holder, stdin=subprocess.DEVNULL, stdout=sys.stdout, stderr=sys.stderr)
if mode == "alive":
    while not os.path.exists(stop):
        time.sleep(0.02)
sys.exit(3)
"""

ROWS = [
    # transport, runtime mode, client state when start() is called again, starts anew
    pytest.param("stdio", "alive", "connected", False, id="stdio, runtime alive"),
    pytest.param("stdio", "exits", "disconnected", True, id="stdio, runtime exited"),
    pytest.param(
        "stdio",
        "descendant",
        "connected",
        True,
        id="stdio, runtime exited while a descendant holds its output",
    ),
    pytest.param("tcp", "alive", "connected", False, id="tcp, runtime alive"),
    pytest.param("tcp", "exits", "disconnected", True, id="tcp, runtime exited"),
    # Requests over TCP do not check the spawned process, and a launcher can exit while the
    # runtime it started keeps serving the connection, so only the connection closing counts.
    pytest.param(
        "tcp",
        "descendant",
        "connected",
        False,
        id="tcp, runtime exited while a descendant holds its connection",
        marks=pytest.mark.skipif(
            sys.platform == "win32", reason="passing a socket to a child needs pass_fds"
        ),
    ),
    pytest.param("uri", "alive", "connected", False, id="uri, external runtime alive"),
]


@pytest.mark.parametrize(("transport", "mode", "state_before_start", "starts_anew"), ROWS)
async def test_start_after_runtime_exit(tmp_path, transport, mode, state_before_start, starts_anew):
    stop = tmp_path / "stop"
    script = (RUNTIME, transport, mode, str(get_sdk_protocol_version()), str(stop))
    external: subprocess.Popen | None = None
    if transport == "uri":
        external = subprocess.Popen(
            [sys.executable, "-c", RUNTIME, "tcp", *script[2:]],
            stdin=subprocess.DEVNULL,
            stdout=subprocess.PIPE,
        )
        assert external.stdout is not None
        announcement = await asyncio.to_thread(external.stdout.readline)
        connection = RuntimeConnection.for_uri(f"127.0.0.1:{int(announcement.split()[-1])}")
    elif transport == "tcp":
        connection = RuntimeConnection.for_tcp(path=sys.executable, args=("-c", *script))
    else:
        connection = RuntimeConnection.for_stdio(path=sys.executable, args=("-c", *script))
    client = CopilotClient(connection=connection)
    connect = AsyncMock(wraps=client._connect_to_server)
    client._connect_to_server = connect
    ping: asyncio.Task | None = None
    try:
        async with asyncio.timeout(30):
            await client.start()
            spawned = client._cli_process
            ping = asyncio.create_task(client.ping("in flight"))
            if mode == "alive":
                await asyncio.sleep(0.5)
            else:
                assert spawned is not None
                await asyncio.to_thread(spawned.wait)
                # Give the reader time to see the transport close, if it does.
                while mode == "exits" and client._state == "connected":
                    await asyncio.sleep(0.01)
                await asyncio.sleep(0.5)
            state_before = client._state

            await client.start()

            assert state_before == state_before_start
            assert connect.await_count == (2 if starts_anew else 1)
            assert client._state == "connected"
            if starts_anew:
                assert client._cli_process is not spawned
                with pytest.raises(ProcessExitedError, match="CLI process exited"):
                    await ping
            else:
                assert client._cli_process is spawned
                assert not ping.done()
    finally:
        stop.touch()
        if ping is not None:
            ping.cancel()
            await asyncio.gather(ping, return_exceptions=True)
        await client.force_stop()
        if external is not None:
            external.kill()
            await asyncio.to_thread(external.wait, 5)
