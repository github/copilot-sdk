import threading
import time
from unittest.mock import patch

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
