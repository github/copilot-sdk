"""
JsonRpcClient Unit Tests

Tests for the JSON-RPC client implementation, focusing on proper handling
of large payloads and short reads from pipes.
"""

import io
import json

import pytest

from copilot.jsonrpc import JsonRpcClient


class MockProcess:
    """Mock subprocess.Popen for testing JSON-RPC client"""

    def __init__(self):
        self.stdin = io.BytesIO()
        self.stdout = None  # Will be set per test
        self.returncode = None

    def poll(self):
        return self.returncode


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


class TestReadMessageWithLargePayloads:
    """Tests for _read_message() with large JSON-RPC messages"""

    def create_jsonrpc_message(self, content_dict: dict) -> bytes:
        """Create a complete JSON-RPC message with Content-Length header"""
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
