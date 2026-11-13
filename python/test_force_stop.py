# Copyright (c) Microsoft Corporation. All rights reserved.

"""Public TCP-backed tests for SDK-local force-stop retirement."""

import asyncio
import gc
import json
import logging
import threading
import weakref
from collections.abc import AsyncIterator
from datetime import UTC, datetime

import pytest

from copilot import CopilotClient, RuntimeConnection
from copilot._sdk_protocol_version import SDK_PROTOCOL_VERSION
from copilot.session import PermissionHandler


class Peer:
    def __init__(self):
        self.requests: list[str] = []
        self.sends_received = asyncio.Event()
        self.expected_sends = 1
        self.completion_supported = True
        self.send_count = 0
        self.failures: list[Exception] = []
        self.writers: list[asyncio.StreamWriter] = []
        self.tasks: list[asyncio.Task] = []

    async def serve(self, reader: asyncio.StreamReader, writer: asyncio.StreamWriter) -> None:
        self.writers.append(writer)
        try:
            while True:
                try:
                    header = await reader.readuntil(b"\r\n\r\n")
                except asyncio.IncompleteReadError as error:
                    if error.partial:
                        raise
                    return
                lengths = [
                    int(line.split(b":", 1)[1])
                    for line in header.split(b"\r\n")
                    if line.lower().startswith(b"content-length:")
                ]
                assert len(lengths) == 1
                assert 0 < lengths[0] <= 128 * 1024
                request = json.loads(await reader.readexactly(lengths[0]))
                method = request["method"]
                self.requests.append(method)
                result = None
                rpc_error = None
                if method == "connect":
                    result = {
                        "ok": True,
                        "protocolVersion": SDK_PROTOCOL_VERSION,
                        "version": "force-stop-test",
                    }
                elif method == "ping":
                    result = {
                        "message": "pong",
                        "timestamp": datetime.now(UTC).isoformat(),
                        "protocolVersion": SDK_PROTOCOL_VERSION,
                    }
                elif method == "session.create":
                    result = {"sessionId": request["params"]["sessionId"]}
                elif method == "session.send":
                    self.send_count += 1
                    result = {"messageId": f"message-{self.send_count}"}
                elif method == "session.sendAndWait":
                    if self.completion_supported:
                        self.send_count += 1
                        if self.send_count == self.expected_sends:
                            self.sends_received.set()
                        continue
                    rpc_error = {"code": -32601, "message": "Method not found"}
                elif method == "$/cancelRequest":
                    continue
                elif method == "session.detach":
                    result = {"success": True}
                else:
                    raise AssertionError(f"Unexpected RPC: {method}")
                body = json.dumps(
                    {
                        "jsonrpc": "2.0",
                        "id": request["id"],
                        **({"error": rpc_error} if rpc_error else {"result": result}),
                    }
                ).encode()
                writer.write(f"Content-Length: {len(body)}\r\n\r\n".encode() + body)
                await writer.drain()
                if self.send_count == self.expected_sends:
                    self.sends_received.set()
        except Exception as error:
            self.failures.append(error)
        finally:
            writer.close()
            await writer.wait_closed()


@pytest.fixture
async def connected_peer() -> AsyncIterator[tuple[CopilotClient, Peer]]:
    peer = Peer()

    def accepted(reader: asyncio.StreamReader, writer: asyncio.StreamWriter) -> None:
        peer.tasks.append(asyncio.create_task(peer.serve(reader, writer)))

    server = await asyncio.start_server(accepted, "127.0.0.1", 0)
    client = CopilotClient(
        connection=RuntimeConnection.for_uri(f"127.0.0.1:{server.sockets[0].getsockname()[1]}"),
    )
    try:
        await client.start()
        yield client, peer
    finally:
        try:
            await client.force_stop()
        finally:
            server.close()
            await server.wait_closed()
            for writer in peer.writers:
                writer.close()
            if peer.tasks:
                await asyncio.gather(*peer.tasks)
            assert peer.failures == []


@pytest.mark.parametrize("count", [1, 2])
@pytest.mark.parametrize("completion_supported", [True, False])
async def test_force_stop_rejects_acknowledged_plain_waits(
    connected_peer, count, completion_supported
):
    client, peer = connected_peer
    peer.expected_sends = count
    peer.completion_supported = completion_supported
    session = await client.create_session(
        session_id="retirement-session", on_permission_request=PermissionHandler.approve_all
    )
    waits = [
        asyncio.create_task(session.send_and_wait("hold the response", timeout=float("inf")))
        for _ in range(count)
    ]
    try:
        await asyncio.wait_for(peer.sends_received.wait(), 5)
        # The later reply proves every preceding send reached the peer.
        await client.ping()
        await client.force_stop()

        for wait in waits:
            with pytest.raises(RuntimeError, match="Session closed before response completed"):
                await wait
        await session.disconnect()
        assert "session.detach" not in peer.requests
    finally:
        for wait in waits:
            wait.cancel()
        await asyncio.gather(*waits, return_exceptions=True)


class Capture:
    def __call__(self, *_args, **_kwargs):
        return None


async def captured_session(client: CopilotClient, kind: str):
    capture = Capture()
    reference = weakref.ref(capture)
    options = {"on_permission_request": PermissionHandler.approve_all}
    if kind == "permission":
        options["on_permission_request"] = capture
    elif kind == "user_input":
        options["on_user_input_request"] = capture
    elif kind == "hook":
        options["hooks"] = {"on_session_end": capture}
    session = await client.create_session(session_id="captured-session", **options)
    if kind == "event":
        session.on(capture)
    return session, reference


@pytest.mark.parametrize("kind", ["event", "permission", "user_input", "hook"])
async def test_force_stop_releases_retained_session_callbacks(connected_peer, kind):
    client, _peer = connected_peer
    session, reference = await captured_session(client, kind)
    assert reference() is not None

    await client.force_stop()
    gc.collect()

    assert reference() is None
    with pytest.raises(RuntimeError, match="Session is disconnected"):
        session.on(Capture())


PARSE_WARNING = "Failed to parse incoming JSON-RPC message"


class HeldHeaderRead:
    """Reader side of the socket file that holds the header read after the next frame.

    Once released, the held read either reads the closed socket file, or reads the
    closed socket itself, as a reader already inside the file's read does.
    """

    def __init__(self, stream, sock=None):
        self._stream = stream
        self._sock = sock
        self._frame_read = False
        self.holding = threading.Event()
        self.release = threading.Event()

    def __getattr__(self, name):
        return getattr(self._stream, name)

    def read(self, size):
        self._frame_read = True
        return self._stream.read(size)

    def readline(self):
        if self._frame_read:
            self.holding.set()
            self.release.wait(5)
            if self._sock is not None:
                self._sock.recv(1)
        return self._stream.readline()


def count_close_reports(client: CopilotClient) -> list[bool]:
    reports: list[bool] = []
    rpc = client._client
    report_close = rpc.on_close

    def on_close():
        reports.append(True)
        report_close()

    rpc.on_close = on_close
    return reports


def parse_warnings(caplog) -> list[logging.LogRecord]:
    return [record for record in caplog.records if record.getMessage() == PARSE_WARNING]


@pytest.mark.parametrize(
    "reader", ["waiting-for-frame", "between-frames-closed-file", "between-frames-closed-socket"]
)
async def test_force_stop_reports_close_without_parse_warning(connected_peer, caplog, reader):
    client, _peer = connected_peer
    caplog.set_level(logging.DEBUG, logger="copilot")
    process = client._process
    rpc = client._client
    reports = count_close_reports(client)
    held = None
    if reader.startswith("between-frames"):
        # The reply to the ping is the last frame the reader takes before force_stop
        # closes the socket underneath its next header read.
        sock = process._socket if reader == "between-frames-closed-socket" else None
        held = HeldHeaderRead(process.stdout, sock)
        process.stdout = held
        await client.ping()
        assert await asyncio.to_thread(held.holding.wait, 5)
    else:
        await client.ping()
        await asyncio.sleep(0.1)
    terminate = process.terminate

    def terminate_then_finish_read():
        terminate()
        if held is not None:
            held.release.set()
        rpc._read_thread.join(5)

    process.terminate = terminate_then_finish_read

    await client.force_stop()

    assert not rpc._read_thread.is_alive()
    assert reports == [True]
    assert parse_warnings(caplog) == []


async def test_malformed_frame_while_connected_still_logs_parse_warning(connected_peer, caplog):
    client, peer = connected_peer
    caplog.set_level(logging.DEBUG, logger="copilot")
    reports = count_close_reports(client)
    rpc = client._client

    peer.writers[0].write(b"Content-Length: 6\r\n\r\n{oops}")
    await peer.writers[0].drain()
    await asyncio.to_thread(rpc._read_thread.join, 5)

    assert not rpc._read_thread.is_alive()
    assert reports == [True]
    warnings = parse_warnings(caplog)
    assert len(warnings) == 1
    assert isinstance(warnings[0].exc_info[1], json.JSONDecodeError)
