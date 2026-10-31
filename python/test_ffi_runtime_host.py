import asyncio
import gc
import threading
import time
import weakref
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
        self.ui_calls: list[tuple[int, int | bool]] = []
        self.ui_result = True

    def connection_set_ui_thread_handler(self, connection_id: int, enabled: bool) -> bool:
        self.ui_calls.append((connection_id, enabled))
        return self.ui_result

    def connection_run_ui_action(self, connection_id: int, action_id: int) -> bool:
        self.ui_calls.append((connection_id, action_id))
        return self.ui_result

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


class _OpeningLibrary(_TestLibrary):
    def __init__(self, connection_id: int) -> None:
        super().__init__()
        self.connection_id = connection_id

    def host_start(self, *_args: object) -> int:
        return 11

    def connection_open(self, *_args: object) -> int:
        return self.connection_id


def _release_host(host: FfiRuntimeHost | None, library: _OpeningLibrary) -> None:
    # Keep the class-level registries clean for later tests even when an
    # assertion fails while the host is still rooted.
    if host is None:
        return
    library.allow_close = True
    host.dispose()
    FfiRuntimeHost._callback_rooted_hosts.discard(host)
    FfiRuntimeHost._quarantined_hosts.discard(host)


def _start_and_drop_host(library: _OpeningLibrary) -> weakref.ref[FfiRuntimeHost]:
    with patch("copilot._ffi_runtime_host._load_library", return_value=library):
        host = FfiRuntimeHost("test-runtime", None)
    try:
        if library.connection_id:
            host.start_blocking()
        else:
            with pytest.raises(RuntimeError, match="connection_open failed"):
                host.start_blocking()
    except BaseException:
        _release_host(host, library)
        raise
    return weakref.ref(host)


def test_open_connection_roots_callback_until_connection_close_succeeds():
    library = _OpeningLibrary(connection_id=21)
    # Native code holds only the callback's address; the application dropping
    # every reference must not let the host -> callback cycle be collected.
    host_ref = _start_and_drop_host(library)
    try:
        gc.collect()
        host = host_ref()
        assert host is not None, "An open connection's outbound callback was collected"
        assert host._outbound_callback is not None

        library.allow_close = True
        host.dispose()
        assert host._outbound_callback is None
        del host
        gc.collect()
        assert host_ref() is None, "A closed connection's host stayed rooted"
    finally:
        _release_host(host_ref(), library)


def test_refused_connection_does_not_root_callback():
    library = _OpeningLibrary(connection_id=0)
    host_ref = _start_and_drop_host(library)
    try:
        gc.collect()
        assert host_ref() is None, "A refused connection's host stayed rooted"
    finally:
        _release_host(host_ref(), library)


def test_ui_actions_are_synchronous_connection_scoped_and_reject_after_disposal():
    library = _TestLibrary()
    library.allow_close = True
    with patch("copilot._ffi_runtime_host._load_library", return_value=library):
        host = FfiRuntimeHost("test-runtime", None)
    host._server_id = 11
    host._connection_id = 21
    try:
        host.configure_ui_thread(True)
        host.run_ui_action(42)
        assert library.ui_calls == [(21, True), (21, 42)]
        library.ui_result = False
        with pytest.raises(RuntimeError, match="retired, already executed"):
            host.run_ui_action(42)
    finally:
        host.dispose()
    with pytest.raises(RuntimeError, match="retired, already executed"):
        host.run_ui_action(43)
    assert library.ui_calls == [(21, True), (21, 42), (21, 42)]


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
