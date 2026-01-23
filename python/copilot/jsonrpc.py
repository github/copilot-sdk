"""
Minimal async JSON-RPC 2.0 client for stdio transport

This uses threading to handle blocking IO in an async-friendly way.
Much simpler and more reliable than pure asyncio subprocess.
"""

import asyncio
import inspect
import json
import threading
import uuid
from collections.abc import Awaitable
from typing import Any, Callable, Optional, Union


class JsonRpcError(Exception):
    """JSON-RPC error response"""

    def __init__(self, code: int, message: str, data: Any = None):
        self.code = code
        self.message = message
        self.data = data
        super().__init__(f"JSON-RPC Error {code}: {message}")


RequestHandler = Callable[[dict], Union[dict, Awaitable[dict]]]


class JsonRpcClient:
    """
    Minimal async JSON-RPC 2.0 client for stdio transport

    Uses threads for blocking IO but provides async interface.
    """

    def __init__(self, process):
        """
        Create client from subprocess.Popen with stdin/stdout pipes

        Args:
            process: subprocess.Popen with stdin=PIPE, stdout=PIPE
        """
        self.process = process
        self.pending_requests: dict[str, asyncio.Future] = {}
        self.notification_handler: Optional[Callable[[str, dict], None]] = None
        self.request_handlers: dict[str, RequestHandler] = {}
        self._running = False
        self._read_thread: Optional[threading.Thread] = None
        self._loop: Optional[asyncio.AbstractEventLoop] = None
        self._write_lock = threading.Lock()
        self._pending_lock = threading.Lock()

    def start(self, loop: Optional[asyncio.AbstractEventLoop] = None):
        """Start listening for messages in background thread"""
        if not self._running:
            self._running = True
            # Always use the provided loop or get the running loop
            self._loop = loop or asyncio.get_running_loop()
            self._read_thread = threading.Thread(target=self._read_loop, daemon=True)
            self._read_thread.start()

    async def stop(self):
        """Stop listening and clean up"""
        self._running = False
        if self._read_thread:
            self._read_thread.join(timeout=1.0)

    async def request(
        self, method: str, params: Optional[dict] = None, timeout: float = 30.0
    ) -> Any:
        """
        Send a JSON-RPC request and wait for response

        Args:
            method: Method name
            params: Optional parameters
            timeout: Request timeout in seconds (default 30s)

        Returns:
            The result from the response

        Raises:
            JsonRpcError: If server returns an error
            asyncio.TimeoutError: If request times out
        """
        request_id = str(uuid.uuid4())

        # Use the stored loop to ensure consistency with the reader thread
        if not self._loop:
            raise RuntimeError("Client not started. Call start() first.")

        future = self._loop.create_future()
        with self._pending_lock:
            self.pending_requests[request_id] = future

        message = {
            "jsonrpc": "2.0",
            "id": request_id,
            "method": method,
            "params": params or {},
        }

        await self._send_message(message)

        try:
            return await asyncio.wait_for(future, timeout=timeout)
        finally:
            with self._pending_lock:
                self.pending_requests.pop(request_id, None)

    async def notify(self, method: str, params: Optional[dict] = None):
        """
        Send a JSON-RPC notification (no response expected)

        Args:
            method: Method name
            params: Optional parameters
        """
        message = {
            "jsonrpc": "2.0",
            "method": method,
            "params": params or {},
        }
        await self._send_message(message)

    def set_notification_handler(self, handler: Callable[[str, dict], None]):
        """Set handler for incoming notifications from server"""
        self.notification_handler = handler

    def set_request_handler(self, method: str, handler: RequestHandler):
        if handler is None:
            self.request_handlers.pop(method, None)
        else:
            self.request_handlers[method] = handler

    async def _send_message(self, message: dict):
        """Send a JSON-RPC message with Content-Length header"""
        loop = self._loop or asyncio.get_event_loop()

        def write():
            content = json.dumps(message, separators=(",", ":"))
            content_bytes = content.encode("utf-8")
            header = f"Content-Length: {len(content_bytes)}\r\n\r\n"
            with self._write_lock:
                self.process.stdin.write(header.encode("utf-8"))
                self.process.stdin.write(content_bytes)
                self.process.stdin.flush()

        # Run in thread pool to avoid blocking
        await loop.run_in_executor(None, write)

    def _read_loop(self):
        """Read messages from the stream (runs in thread)"""
        try:
            while self._running:
                message = self._read_message()
                if message:
                    self._handle_message(message)
        except Exception as e:
            if self._running:
                print(f"JSON-RPC read loop error: {e}")

    def _read_exact(self, num_bytes: int) -> bytes:
        """
        Read exactly num_bytes, handling partial/short reads from pipes.

        Args:
            num_bytes: Number of bytes to read

        Returns:
            Bytes read from stream

        Raises:
            EOFError: If stream ends before reading all bytes
        """
        chunks = []
        remaining = num_bytes
        while remaining > 0:
            chunk = self.process.stdout.read(remaining)
            if not chunk:
                raise EOFError("Unexpected end of stream while reading JSON-RPC message")
            chunks.append(chunk)
            remaining -= len(chunk)
        return b"".join(chunks)

    def _read_message(self) -> Optional[dict]:
        """
        Read a single JSON-RPC message with Content-Length header (blocking)

        Returns:
            Parsed JSON message or None if connection closed
        """
        # Read header line
        header_line = self.process.stdout.readline()
        if not header_line:
            return None

        # Parse Content-Length
        header = header_line.decode("utf-8").strip()
        if not header.startswith("Content-Length:"):
            return None

        content_length = int(header.split(":")[1].strip())

        # Read empty line
        self.process.stdout.readline()

        # Read exact content using loop to handle short reads
        content_bytes = self._read_exact(content_length)
        content = content_bytes.decode("utf-8")

        return json.loads(content)

    def _handle_message(self, message: dict):
        """Handle an incoming message (response or notification)"""
        # Check if it's a response to our request
        if "id" in message:
            with self._pending_lock:
                future = self.pending_requests.get(message["id"])

            if future is not None:
                loop = future.get_loop()

                if "error" in message:
                    error = message["error"]
                    exc = JsonRpcError(
                        error.get("code", -1),
                        error.get("message", "Unknown error"),
                        error.get("data"),
                    )
                    loop.call_soon_threadsafe(future.set_exception, exc)
                elif "result" in message:
                    loop.call_soon_threadsafe(future.set_result, message["result"])
                else:
                    exc = ValueError("Invalid JSON-RPC response")
                    loop.call_soon_threadsafe(future.set_exception, exc)
                return

        # Check if it's a notification from server
        if "method" in message and "id" not in message:
            if self.notification_handler and self._loop:
                method = message["method"]
                params = message.get("params", {})
                # Schedule notification handler on the event loop for thread safety
                self._loop.call_soon_threadsafe(self.notification_handler, method, params)
            return

        # Otherwise handle as incoming request (tool.call, etc.)
        if "method" in message and "id" in message:
            self._handle_request(message)

    def _handle_request(self, message: dict):
        handler = self.request_handlers.get(message["method"])
        if not handler:
            if self._loop:
                asyncio.run_coroutine_threadsafe(
                    self._send_error_response(
                        message["id"], -32601, f"Method not found: {message['method']}", None
                    ),
                    self._loop,
                )
            return
        if not self._loop:
            return
        asyncio.run_coroutine_threadsafe(
            self._dispatch_request(message, handler),
            self._loop,
        )

    async def _dispatch_request(self, message: dict, handler: RequestHandler):
        try:
            params = message.get("params", {})
            outcome = handler(params)
            if inspect.isawaitable(outcome):
                outcome = await outcome
            if outcome is None:
                outcome = {}
            if not isinstance(outcome, dict):
                raise ValueError("Request handler must return a dict")
            await self._send_response(message["id"], outcome)
        except JsonRpcError as exc:
            await self._send_error_response(message["id"], exc.code, exc.message, exc.data)
        except Exception as exc:  # pylint: disable=broad-except
            await self._send_error_response(message["id"], -32603, str(exc), None)

    async def _send_response(self, request_id: str, result: dict):
        response = {
            "jsonrpc": "2.0",
            "id": request_id,
            "result": result,
        }
        await self._send_message(response)

    async def _send_error_response(
        self, request_id: str, code: int, message: str, data: Optional[dict]
    ):
        response = {
            "jsonrpc": "2.0",
            "id": request_id,
            "error": {
                "code": code,
                "message": message,
                "data": data,
            },
        }
        await self._send_message(response)
