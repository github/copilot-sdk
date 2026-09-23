"""
JsonRpcClient Unit Tests

Tests for the JSON-RPC client implementation, focusing on proper handling
of large payloads and short reads from pipes.
"""

import asyncio
import io
import json
import os
import subprocess
import sys
import threading
import time

import pytest

from copilot._jsonrpc import JsonRpcClient, ProcessExitedError


class MockProcess:
    """Mock subprocess.Popen for testing JSON-RPC client"""

    def __init__(self):
        self.stdin = io.BytesIO()
        self.stdout = None  # Will be set per test
        self.returncode = None

    def poll(self):
        return self.returncode


@pytest.mark.asyncio
async def test_send_message_supports_streams_without_process_poll():
    class StreamProcess:
        def __init__(self):
            self.stdin = io.BytesIO()
            self.stdout = io.BytesIO()
            self.stderr = None

    process = StreamProcess()
    client = JsonRpcClient(process)

    await client._send_message({"jsonrpc": "2.0", "method": "ping"})

    assert process.stdin.getvalue() == (
        b'Content-Length: 33\r\n\r\n{"jsonrpc":"2.0","method":"ping"}'
    )


@pytest.mark.asyncio
@pytest.mark.parametrize("operation", ["request", "notify"])
@pytest.mark.parametrize("eof", ["before-request", "after-request"])
async def test_rejects_messages_after_stdout_eof_while_process_is_alive(operation, eof):
    with subprocess.Popen(
        [
            # Windows' venv launcher retains stdout while waiting for the interpreter.
            sys._base_executable,
            "-c",
            """
import os
import sys
if sys.argv[1] == "after-request":
    header = sys.stdin.buffer.readline()
    sys.stdin.buffer.readline()
    sys.stdin.buffer.read(int(header.split(b":")[1]))
os.close(sys.stdout.fileno())
sys.stdin.buffer.read()
""",
            eof,
        ],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
    ) as process:
        client = JsonRpcClient(process)
        loop = asyncio.get_running_loop()
        closed = asyncio.Event()

        def on_close():
            loop.call_soon_threadsafe(closed.set)

        client.on_close = on_close
        client.start()
        try:
            if eof == "after-request":
                with pytest.raises(ProcessExitedError):
                    await asyncio.wait_for(client.request("initial"), timeout=5)
            await asyncio.wait_for(closed.wait(), timeout=5)
            assert process.poll() is None

            with pytest.raises(ProcessExitedError):
                await asyncio.wait_for(getattr(client, operation)("ping"), timeout=5)

            assert client.pending_requests == {}
        finally:
            if process.poll() is None:
                process.kill()
            await asyncio.to_thread(process.wait, timeout=5)
            await client.stop()


@pytest.mark.asyncio
async def test_terminal_error_includes_stderr_when_stdout_closes_during_start(monkeypatch):
    process = MockProcess()
    process.returncode = 1
    process.stdout = io.BytesIO()
    process.stderr = io.BytesIO(b"startup diagnostic\n")
    client = JsonRpcClient(process)
    original_start = threading.Thread.start

    def start_thread(thread):
        original_start(thread)
        if thread is client._read_thread:
            thread.join(timeout=5)
            assert not thread.is_alive(), "EOF reader did not finish"

    try:
        with monkeypatch.context() as patch:
            patch.setattr(threading.Thread, "start", start_thread)
            client.start()

        with pytest.raises(ProcessExitedError, match="stderr: startup diagnostic"):
            await client.request("connect")
    finally:
        await client.stop()


@pytest.mark.asyncio
@pytest.mark.parametrize("operation", ["request", "notify"])
@pytest.mark.parametrize("phase", ["collecting-diagnostics", "queued-write"])
async def test_eof_rejects_writes_across_diagnostic_and_executor_windows(
    monkeypatch, operation, phase
):
    read_fd, write_fd = os.pipe()
    with os.fdopen(read_fd, "rb") as stdout, os.fdopen(write_fd, "wb") as stdout_writer:
        process = MockProcess()
        process.stdout = stdout
        client = JsonRpcClient(process)
        loop = asyncio.get_running_loop()
        diagnostics_started = asyncio.Event()
        writer_started = asyncio.Event()
        closed = asyncio.Event()
        release = threading.Event()
        original_error = client._get_process_exit_error
        task = None

        def collect_error():
            if (
                threading.current_thread() is client._read_thread
                and phase == "collecting-diagnostics"
            ):
                loop.call_soon_threadsafe(diagnostics_started.set)
                assert release.wait(timeout=5), "diagnostics barrier was not released"
            return original_error()

        def poll():
            if threading.current_thread() is not client._read_thread:
                loop.call_soon_threadsafe(writer_started.set)
                if phase == "queued-write":
                    assert release.wait(timeout=5), "writer barrier was not released"
            return None

        def on_close():
            loop.call_soon_threadsafe(closed.set)

        monkeypatch.setattr(client, "_get_process_exit_error", collect_error)
        monkeypatch.setattr(process, "poll", poll)
        client.on_close = on_close
        client.start()
        try:
            if phase == "collecting-diagnostics":
                stdout_writer.close()
                await asyncio.wait_for(diagnostics_started.wait(), timeout=5)
            task = asyncio.create_task(getattr(client, operation)("ping"))
            await asyncio.wait_for(writer_started.wait(), timeout=5)
            if phase == "queued-write":
                stdout_writer.close()
                await asyncio.wait_for(closed.wait(), timeout=5)
            release.set()

            with pytest.raises(ProcessExitedError):
                await asyncio.wait_for(task, timeout=5)
            assert process.stdin.getvalue() == b""
            assert client.pending_requests == {}
        finally:
            release.set()
            stdout_writer.close()
            if task is not None:
                if not task.done():
                    task.cancel()
                await asyncio.gather(task, return_exceptions=True)
            await client.stop()


class ShortReadStream:
    """
    Mock stream that simulates short reads from a pipe.

    This simulates the behavior of Unix pipes when reading data larger than
    the pipe buffer (typically 64KB). The read() method will return fewer
    bytes than requested, requiring multiple read calls.
    """

    def __init__(self, data: bytes, chunk_size: int = 32768):
        """
        Args:
            data: Complete data to be read
            chunk_size: Maximum bytes to return per read() call (simulates pipe buffer)
        """
        self.data = data
        self.chunk_size = chunk_size
        self.pos = 0

    def readline(self):
        """Read until newline"""
        end = self.data.find(b"\n", self.pos) + 1
        if end == 0:  # Not found
            result = self.data[self.pos :]
            self.pos = len(self.data)
        else:
            result = self.data[self.pos : end]
            self.pos = end
        return result

    def read(self, n: int) -> bytes:
        """
        Read at most n bytes, but may return fewer (short read).

        This simulates the behavior of pipes when data exceeds buffer size.
        """
        # Calculate how much we can return (limited by chunk_size)
        available = len(self.data) - self.pos
        to_read = min(n, available, self.chunk_size)

        result = self.data[self.pos : self.pos + to_read]
        self.pos += to_read
        return result


class TestReadExact:
    """Tests for the _read_exact() method that handles short reads"""

    def test_read_exact_single_chunk(self):
        """Test reading data that fits in a single chunk"""
        content = b"Hello, World!"
        mock_stream = ShortReadStream(content, chunk_size=1024)

        process = MockProcess()
        process.stdout = mock_stream

        client = JsonRpcClient(process)
        result = client._read_exact(len(content))

        assert result == content

    def test_read_exact_multiple_chunks(self):
        """Test reading data that requires multiple chunks (short reads)"""
        # Create 100KB of data
        content = b"x" * 100000
        # Simulate 32KB chunks (typical pipe behavior)
        mock_stream = ShortReadStream(content, chunk_size=32768)

        process = MockProcess()
        process.stdout = mock_stream

        client = JsonRpcClient(process)
        result = client._read_exact(len(content))

        assert result == content
        assert len(result) == 100000

    def test_read_exact_at_64kb_boundary(self):
        """Test reading exactly 64KB (common pipe buffer size)"""
        content = b"y" * 65536  # Exactly 64KB
        mock_stream = ShortReadStream(content, chunk_size=65536)

        process = MockProcess()
        process.stdout = mock_stream

        client = JsonRpcClient(process)
        result = client._read_exact(len(content))

        assert result == content
        assert len(result) == 65536

    def test_read_exact_exceeds_64kb(self):
        """Test reading data that exceeds 64KB (triggers the bug without fix)"""
        # 80KB - larger than typical pipe buffer
        content = b"z" * 81920
        # Simulate reading with 64KB limit (macOS pipe buffer)
        mock_stream = ShortReadStream(content, chunk_size=65536)

        process = MockProcess()
        process.stdout = mock_stream

        client = JsonRpcClient(process)
        result = client._read_exact(len(content))

        assert result == content
        assert len(result) == 81920

    def test_read_exact_empty_stream_raises_eof(self):
        """Test that reading from closed stream raises EOFError"""
        mock_stream = ShortReadStream(b"", chunk_size=1024)

        process = MockProcess()
        process.stdout = mock_stream

        client = JsonRpcClient(process)

        with pytest.raises(EOFError, match="Unexpected end of stream"):
            client._read_exact(10)

    def test_read_exact_partial_data_raises_eof(self):
        """Test that stream ending mid-message raises EOFError"""
        # Only 50 bytes available, but we request 100
        content = b"a" * 50
        mock_stream = ShortReadStream(content, chunk_size=1024)

        process = MockProcess()
        process.stdout = mock_stream

        client = JsonRpcClient(process)

        with pytest.raises(EOFError, match="Unexpected end of stream"):
            client._read_exact(100)


@pytest.mark.asyncio
async def test_process_exit_waits_for_stderr_reader():
    process = MockProcess()
    process.returncode = 1
    client = JsonRpcClient(process)
    future = asyncio.get_running_loop().create_future()
    client.pending_requests["request-id"] = future

    def capture_stderr():
        time.sleep(0.01)
        with client._stderr_lock:
            client._stderr_output.append("unsupported argument\n")

    client._stderr_thread = threading.Thread(target=capture_stderr)
    client._stderr_thread.start()

    client._fail_pending_requests()

    await asyncio.sleep(0)
    with pytest.raises(ProcessExitedError, match=r"stderr: unsupported argument"):
        future.result()


class TestReadMessageWithLargePayloads:
    """Tests for _read_message() with large JSON-RPC messages"""

    def create_jsonrpc_message(self, content_dict: dict) -> bytes:
        """Create a complete JSON-RPC message with a Content-Length header."""
        content = json.dumps(content_dict, separators=(",", ":"))
        content_bytes = content.encode("utf-8")
        header = f"Content-Length: {len(content_bytes)}\r\n\r\n"
        return header.encode("utf-8") + content_bytes

    def test_read_message_small_payload(self):
        """Test reading a small JSON-RPC message"""
        message = {"jsonrpc": "2.0", "id": "1", "result": {"status": "ok"}}
        full_data = self.create_jsonrpc_message(message)

        mock_stream = ShortReadStream(full_data, chunk_size=1024)
        process = MockProcess()
        process.stdout = mock_stream

        client = JsonRpcClient(process)
        result = client._read_message()

        assert result == message

    def test_read_message_large_payload_70kb(self):
        """Test reading a 70KB JSON-RPC message (exceeds typical pipe buffer)"""
        # Simulate a large response with context echo (common pattern)
        large_content = "x" * 70000  # 70KB of data
        message = {
            "jsonrpc": "2.0",
            "id": "1",
            "result": {"content": large_content, "status": "complete"},
        }

        full_data = self.create_jsonrpc_message(message)
        # Simulate 64KB pipe buffer limit
        mock_stream = ShortReadStream(full_data, chunk_size=65536)

        process = MockProcess()
        process.stdout = mock_stream

        client = JsonRpcClient(process)
        result = client._read_message()

        assert result == message
        assert len(result["result"]["content"]) == 70000

    def test_read_message_large_payload_100kb(self):
        """Test reading a 100KB JSON-RPC message"""
        large_content = "y" * 100000  # 100KB
        message = {
            "jsonrpc": "2.0",
            "id": "2",
            "result": {"data": large_content, "metadata": {"size": 100000}},
        }

        full_data = self.create_jsonrpc_message(message)
        # Simulate short reads with 32KB chunks
        mock_stream = ShortReadStream(full_data, chunk_size=32768)

        process = MockProcess()
        process.stdout = mock_stream

        client = JsonRpcClient(process)
        result = client._read_message()

        assert result == message
        assert len(result["result"]["data"]) == 100000

    def test_read_message_exactly_64kb_content(self):
        """Test reading message with exactly 64KB of content"""
        content_64kb = "z" * 65536  # Exactly 64KB
        message = {"jsonrpc": "2.0", "id": "3", "result": {"content": content_64kb}}

        full_data = self.create_jsonrpc_message(message)
        mock_stream = ShortReadStream(full_data, chunk_size=65536)

        process = MockProcess()
        process.stdout = mock_stream

        client = JsonRpcClient(process)
        result = client._read_message()

        assert result == message
        assert len(result["result"]["content"]) == 65536

    def test_read_message_multiple_messages_in_sequence(self):
        """Test reading multiple large messages in sequence"""
        message1 = {"jsonrpc": "2.0", "id": "1", "result": {"data": "a" * 50000}}
        message2 = {"jsonrpc": "2.0", "id": "2", "result": {"data": "b" * 80000}}

        data1 = self.create_jsonrpc_message(message1)
        data2 = self.create_jsonrpc_message(message2)
        full_data = data1 + data2

        mock_stream = ShortReadStream(full_data, chunk_size=32768)
        process = MockProcess()
        process.stdout = mock_stream

        client = JsonRpcClient(process)

        result1 = client._read_message()
        assert result1 == message1

        result2 = client._read_message()
        assert result2 == message2


class ClosingStream:
    """Stream that immediately returns empty bytes (simulates process death / EOF)."""

    def readline(self):
        return b""

    def read(self, n: int) -> bytes:
        return b""


class TestOnClose:
    """Tests for the on_close callback when the read loop exits unexpectedly."""

    def test_on_close_called_on_unexpected_exit(self):
        """on_close fires when the stream closes while client is still running."""
        import asyncio

        process = MockProcess()
        process.stdout = ClosingStream()

        client = JsonRpcClient(process)

        called = threading.Event()
        client.on_close = lambda: called.set()

        loop = asyncio.new_event_loop()
        try:
            client.start(loop=loop)
            assert called.wait(timeout=2), "on_close was not called within 2 seconds"
        finally:
            loop.close()

    def test_on_close_not_called_on_intentional_stop(self):
        """on_close should not fire when stop() is called intentionally."""
        import asyncio

        r_fd, w_fd = os.pipe()
        process = MockProcess()
        process.stdout = os.fdopen(r_fd, "rb")

        client = JsonRpcClient(process)

        called = threading.Event()
        client.on_close = lambda: called.set()

        loop = asyncio.new_event_loop()
        try:
            client.start(loop=loop)

            # Intentional stop sets _running = False before the thread sees EOF
            loop.run_until_complete(client.stop())
            os.close(w_fd)

            time.sleep(0.5)
            assert not called.is_set(), "on_close should not be called on intentional stop"
        finally:
            loop.close()
