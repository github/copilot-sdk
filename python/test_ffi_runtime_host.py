import asyncio
import threading
import time
from unittest.mock import patch

import pytest

from copilot import CopilotClient, RuntimeConnection
from copilot._ffi_runtime_host import FfiRuntimeHost


class _TestLibrary:
    def __init__(self) -> None:
        self.allow_close = False
        self.close_calls = 0
        self.shutdown_calls = 0
        self.shutdown = threading.Event()

    def connection_close(self, _connection_id: int) -> bool:
        self.close_calls += 1
        return self.allow_close

    def host_shutdown(self, _server_id: int) -> bool:
        self.shutdown_calls += 1
        self.shutdown.set()
        return True


def test_dispose_retains_callback_until_connection_close_succeeds():
    library = _TestLibrary()
    with (
        patch("copilot._ffi_runtime_host._load_library", return_value=library),
        patch("copilot._ffi_runtime_host._CLEANUP_RETRY_INTERVAL_SECONDS", 0.01),
    ):
        host = FfiRuntimeHost("test-runtime", None)
        callback = object()
        host._server_id = 11
        host._connection_id = 21
        host._outbound_callback = callback

        host.dispose()

        assert host._outbound_callback is callback
        assert host._connection_id == 21
        assert library.close_calls == 1
        assert library.shutdown_calls == 0

        library.allow_close = True
        assert library.shutdown.wait(5), "Deferred native cleanup did not complete"

        assert host._outbound_callback is None
        assert host._connection_id == 0
        assert library.close_calls >= 2
        assert library.shutdown_calls == 1

        close_calls_after_cleanup = library.close_calls
        host.dispose()
        time.sleep(0.05)
        assert library.close_calls == close_calls_after_cleanup
        assert library.shutdown_calls == 1


@pytest.mark.asyncio
@pytest.mark.parametrize("method", ["stop", "force_stop"])
async def test_client_shutdown_keeps_event_loop_responsive_until_native_cleanup_finishes(method):
    loop = asyncio.get_running_loop()
    loop_response = threading.Event()
    loop_responded_during_shutdown = False

    class BlockingLibrary(_TestLibrary):
        def host_shutdown(self, server_id: int) -> bool:
            nonlocal loop_responded_during_shutdown
            loop.call_soon_threadsafe(loop_response.set)
            loop_responded_during_shutdown = loop_response.wait(5)
            return super().host_shutdown(server_id)

    library = BlockingLibrary()
    library.allow_close = True
    with patch("copilot._ffi_runtime_host._load_library", return_value=library):
        host = FfiRuntimeHost("test-runtime", None)
    host._server_id = 11
    host._connection_id = 21
    host._outbound_callback = object()

    client = CopilotClient(connection=RuntimeConnection.for_stdio(path="copilot"))
    client._ffi_host = host
    client._process = host.process
    client._state = "connected"

    await getattr(client, method)()

    assert loop_responded_during_shutdown, "Native shutdown blocked the asyncio event loop"
    assert library.shutdown.is_set(), "Client shutdown returned before native cleanup"
    assert library.shutdown_calls == 1
    assert host._server_id == 0
    assert host._connection_id == 0
    assert host._outbound_callback is None
    assert client._ffi_host is None
    assert client._process is None
    assert client._state == "disconnected"
