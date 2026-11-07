"""Network callback registration and routing without a live runtime."""

from __future__ import annotations

import asyncio
import base64
import contextlib
import gc
import os
from collections.abc import Awaitable, Callable, Coroutine
from typing import Any, cast
from unittest.mock import AsyncMock, Mock

import pytest

from copilot import (
    CopilotClient,
    CopilotRequestHandler,
    NetworkAuthenticateNegotiateRequest,
    NetworkConfig,
    NetworkResolveProxyRequest,
    NetworkScope,
    RuntimeConnection,
)
from copilot._jsonrpc import JsonRpcClient
from copilot.generated.rpc import ServerRpc


class FakeJsonRpcClient:
    def __init__(self, *, fail_method: str | None = None, block_method: str | None = None) -> None:
        self.fail_method = fail_method
        self.block_method = block_method
        self.blocked = asyncio.Event()
        self.stopped = False
        self.socket = Mock(spec=["terminate"])
        self.requests: list[tuple[str, dict[str, Any]]] = []
        self.request_handlers: dict[str, Any] = {}
        self.session_requested = asyncio.Event()

    async def request(self, method: str, params: dict[str, Any], **kwargs: Any) -> Any:
        self.requests.append((method, params))
        if method == self.fail_method:
            raise RuntimeError(f"{method} failed")
        if method == self.block_method:
            self.blocked.set()
            await asyncio.Event().wait()
        if method in {"session.create", "session.resume"}:
            self.session_requested.set()
            response = {"sessionId": params["sessionId"]}
            callback = kwargs.get("on_response_inline")
            if callback is not None:
                callback(response)
            return response
        return None

    async def stop(self) -> None:
        self.stopped = True

    def set_request_handler(self, method: str, handler: Any, **_options: Any) -> None:
        self.request_handlers[method] = handler

    def set_raw_request_handler(self, method: str, handler: Any) -> None:
        pass

    def set_notification_method_handler(self, method: str, handler: Any) -> None:
        pass


def make_client(
    fake: FakeJsonRpcClient, verify: Callable[[], Awaitable[None]] | None = None, **options: Any
) -> CopilotClient:
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"), **options)

    async def connect() -> None:
        client._process = fake.socket
        client._client = cast(JsonRpcClient, fake)
        client._rpc = ServerRpc(client._client)
        client._register_client_global_handlers()

    client._connect_to_server = AsyncMock(side_effect=connect)
    client._verify_protocol_version = AsyncMock(side_effect=verify)
    return client


async def _none(_request: Any) -> str | None:
    return None


SESSION_FS = {
    "initial_working_directory": "/workspace",
    "session_state_path": "/state",
    "conventions": "posix",
}
SESSION_FS_REGISTRATION = (
    "sessionFs.setProvider",
    {"initialCwd": "/workspace", "sessionStatePath": "/state", "conventions": "posix"},
)

# Wire method -> (callback option, request params, error label).
METHODS: dict[str, tuple[str, dict[str, str], str]] = {
    "network.resolveProxy": ("resolve_proxy", {"url": "https://api.example/"}, "Proxy resolution"),
    "network.authenticateNegotiate": (
        "authenticate_negotiate",
        {"spn": "HTTP/proxy.example"},
        "Negotiate authentication",
    ),
}


@pytest.mark.parametrize(
    ("network", "registrations"),
    [
        (None, []),
        ({}, []),
        ({"scope": "runtime"}, []),
        ({"resolve_proxy": _none}, [{"resolveProxy": {}}]),
        (
            {"scope": "runtime", "authenticate_negotiate": _none},
            [{"scope": "runtime", "authenticateNegotiate": {}}],
        ),
        (
            {"scope": NetworkScope.CONNECTION, "resolve_proxy": _none},
            [{"scope": "connection", "resolveProxy": {}}],
        ),
        (
            {"resolve_proxy": _none, "authenticate_negotiate": _none},
            [{"resolveProxy": {}, "authenticateNegotiate": {}}],
        ),
    ],
)
async def test_start_registers_only_configured_callbacks_and_scope_before_other_providers(
    network, registrations
) -> None:
    fake = FakeJsonRpcClient()
    client = make_client(fake, network=network, session_fs=SESSION_FS)

    await client.start()

    assert fake.requests == [
        *(("network.setProvider", registration) for registration in registrations),
        SESSION_FS_REGISTRATION,
    ]


@pytest.mark.parametrize("network", [None, {"scope": "runtime"}])
@pytest.mark.parametrize("method", list(METHODS))
async def test_start_without_callbacks_does_not_handle(network, method) -> None:
    fake = FakeJsonRpcClient()
    client = make_client(fake, network=network)

    await client.start()

    with pytest.raises(RuntimeError, match="No network client-global handler registered"):
        await fake.request_handlers[method](METHODS[method][1])


@pytest.mark.parametrize("method", list(METHODS))
async def test_unconfigured_callback_fails_its_method(method) -> None:
    option, params, _label = METHODS[method]
    other = next(name for name, *_ in METHODS.values() if name != option)
    fake = FakeJsonRpcClient()
    client = make_client(fake, network={other: _none})

    await client.start()

    with pytest.raises(RuntimeError, match=f"No network {option} callback registered"):
        await fake.request_handlers[method](params)


async def test_uses_the_callbacks_configured_when_the_client_was_constructed() -> None:
    async def constructed(_request: Any) -> str:
        return "PROXY constructed.example:8080"

    async def replaced(_request: Any) -> str:
        return "replaced"

    network: dict[str, Any] = {"resolve_proxy": constructed}
    fake = FakeJsonRpcClient()
    client = make_client(fake, network=network)
    network["resolve_proxy"] = replaced
    network["authenticate_negotiate"] = replaced

    await client.start()
    answer = await fake.request_handlers["network.resolveProxy"]({"url": "https://api.example/"})

    assert (fake.requests, answer) == (
        [("network.setProvider", {"resolveProxy": {}})],
        {"proxy": "PROXY constructed.example:8080"},
    )
    with pytest.raises(RuntimeError, match="No network authenticate_negotiate callback"):
        await fake.request_handlers["network.authenticateNegotiate"]({"spn": "HTTP/proxy"})


async def test_registration_failure_fails_start() -> None:
    fake = FakeJsonRpcClient(fail_method="network.setProvider")
    client = make_client(fake, network={"resolve_proxy": _none}, session_fs=SESSION_FS)

    with pytest.raises(RuntimeError, match="network.setProvider failed"):
        await client.start()

    assert [method for method, _ in fake.requests] == ["network.setProvider"]
    assert (client._state, client._client) == ("error", None)


async def test_cancelled_registration_closes_connection_and_propagates() -> None:
    fake = FakeJsonRpcClient(block_method="network.setProvider")
    client = make_client(fake, network={"authenticate_negotiate": _none}, session_fs=SESSION_FS)
    starting = asyncio.create_task(client.start())
    await asyncio.wait_for(fake.blocked.wait(), timeout=1)

    starting.cancel()
    with pytest.raises(asyncio.CancelledError):
        await starting

    assert (
        [method for method, _ in fake.requests],
        client._state,
        client._client,
        fake.stopped,
    ) == (["network.setProvider"], "disconnected", None, True)


@pytest.mark.parametrize("outcome", ["failed", "cancelled"])
@pytest.mark.parametrize(
    ("options", "method"),
    [
        ({"extension_launch_provider": Mock()}, "registerExtensionLaunchProvider"),
        ({"builtin_plugin_directories": [os.path.abspath("plugins")]}, "plugins.builtin.set"),
        ({"session_fs": SESSION_FS}, "sessionFs.setProvider"),
        ({"request_handler": CopilotRequestHandler()}, "llmInference.setProvider"),
    ],
)
async def test_later_startup_failure_closes_registered_connection(options, method, outcome) -> None:
    # A runtime-scoped registration is exclusive, so a failed start must not keep holding it.
    cancelled = outcome == "cancelled"
    fake = FakeJsonRpcClient(**{"block_method" if cancelled else "fail_method": method})
    client = make_client(
        fake,
        network={"scope": "runtime", "resolve_proxy": _none, "authenticate_negotiate": _none},
        **options,
    )
    starting = asyncio.create_task(client.start())
    if cancelled:
        await asyncio.wait_for(fake.blocked.wait(), timeout=1)
        starting.cancel()

    with pytest.raises(
        asyncio.CancelledError if cancelled else RuntimeError,
        match=None if cancelled else f"{method} failed",
    ):
        await starting

    assert (
        [name for name, _ in fake.requests],
        client._state,
        client._client,
        fake.stopped,
        fake.socket.terminate.called,
    ) == (
        ["network.setProvider", method],
        "disconnected" if cancelled else "error",
        None,
        True,
        True,
    )


@pytest.mark.parametrize(
    ("method", "wire_method"),
    [("create_session", "session.create"), ("resume_session", "session.resume")],
)
async def test_registers_before_sessions_requested_during_startup(method, wire_method) -> None:
    fake = FakeJsonRpcClient()
    verifying = asyncio.Event()

    async def verify() -> None:
        # Hold startup after the transport exists. A session request that does not wait for
        # startup arrives meanwhile; one that waits cannot, so the timeout releases startup.
        verifying.set()
        with contextlib.suppress(TimeoutError):
            await asyncio.wait_for(fake.session_requested.wait(), timeout=0.5)

    client = make_client(fake, verify, network={"authenticate_negotiate": _none})
    starting = asyncio.create_task(client.start())
    await asyncio.wait_for(verifying.wait(), timeout=1)

    await asyncio.wait_for(
        asyncio.gather(starting, getattr(client, method)(session_id="session")), timeout=5
    )

    assert [name for name, _ in fake.requests] == ["network.setProvider", wire_method]


@pytest.mark.parametrize("kind", ["async", "sync", "task", "coroutine"])
async def test_answers_concurrent_lookups_missing_values_and_failures(monkeypatch, kind) -> None:
    calls: list[Any] = []

    def resolve_proxy(request: NetworkResolveProxyRequest) -> str | None:
        calls.append(request)
        if request.url == "https://failing.example/":
            raise RuntimeError("lookup failed")
        if request.url.startswith("https:"):
            return "PROXY proxy.example:8080; HTTPS proxy.example:443; DIRECT"
        return None

    def authenticate_negotiate(request: NetworkAuthenticateNegotiateRequest) -> str | None:
        calls.append(request)
        if request.spn == "HTTP/failing.example":
            raise RuntimeError("token failed")
        if request.spn == "HTTP/unavailable.example":
            return None
        return base64.b64encode(request.spn.encode()).decode()

    def adapt(callback: Callable[[Any], str | None]) -> Callable[[Any], Any]:
        async def run_async(request: Any) -> str | None:
            await asyncio.sleep(0)
            return callback(request)

        return {
            "async": run_async,
            "sync": callback,
            # Ordinary callables that need a running loop, or return work for the client's loop.
            "task": lambda request: asyncio.create_task(run_async(request)),
            "coroutine": lambda request: run_async(request),
        }[kind]

    rpc = JsonRpcClient(Mock())
    sent: list[dict[str, Any]] = []

    async def send(message: dict[str, Any]) -> None:
        sent.append(message)

    monkeypatch.setattr(rpc, "_send_message", send)
    client = CopilotClient(
        connection=RuntimeConnection.for_uri("localhost:1234"),
        network={
            "resolve_proxy": adapt(resolve_proxy),
            "authenticate_negotiate": adapt(authenticate_negotiate),
        },
    )
    client._client = rpc
    client._register_client_global_handlers()
    requests = [
        ("network.resolveProxy", {"url": "https://api.example:8443/"}),
        ("network.resolveProxy", {"url": "http://example.com/path"}),
        ("network.resolveProxy", {"url": "https://failing.example/"}),
        ("network.authenticateNegotiate", {"spn": "HTTP/proxy.example"}),
        ("network.authenticateNegotiate", {"spn": "ldap/directory.example@EXAMPLE.COM"}),
        ("network.authenticateNegotiate", {"spn": "HTTP/unavailable.example"}),
        ("network.authenticateNegotiate", {"spn": "HTTP/failing.example"}),
    ]

    await asyncio.gather(
        *(
            rpc._dispatch_request(
                {"jsonrpc": "2.0", "id": request_id, "method": method, "params": params},
                rpc.request_handlers[method],
                None,
            )
            for request_id, (method, params) in enumerate(requests)
        )
    )

    def error(message: str) -> dict[str, Any]:
        return {"code": -32603, "message": message, "data": None}

    assert (sorted(calls, key=repr), sorted(sent, key=lambda message: message["id"])) == (
        sorted(
            [NetworkResolveProxyRequest(params["url"]) for _, params in requests[:3]]
            + [NetworkAuthenticateNegotiateRequest(params["spn"]) for _, params in requests[3:]],
            key=repr,
        ),
        [
            {
                "jsonrpc": "2.0",
                "id": 0,
                "result": {"proxy": "PROXY proxy.example:8080; HTTPS proxy.example:443; DIRECT"},
            },
            {"jsonrpc": "2.0", "id": 1, "result": {}},
            {"jsonrpc": "2.0", "id": 2, "error": error("lookup failed")},
            {"jsonrpc": "2.0", "id": 3, "result": {"token": "SFRUUC9wcm94eS5leGFtcGxl"}},
            {
                "jsonrpc": "2.0",
                "id": 4,
                "result": {"token": "bGRhcC9kaXJlY3RvcnkuZXhhbXBsZUBFWEFNUExFLkNPTQ=="},
            },
            {"jsonrpc": "2.0", "id": 5, "result": {}},
            {"jsonrpc": "2.0", "id": 6, "error": error("token failed")},
        ],
    )


def _wire_network_client(
    monkeypatch: pytest.MonkeyPatch, network: NetworkConfig
) -> tuple[CopilotClient, JsonRpcClient, list[dict[str, Any]], asyncio.Event]:
    rpc = JsonRpcClient(Mock())
    rpc._loop = asyncio.get_running_loop()
    sent: list[dict[str, Any]] = []
    finished = asyncio.Event()
    dispatch = rpc._dispatch_request

    async def send(message: dict[str, Any]) -> None:
        sent.append(message)

    async def dispatch_request(*args: Any, **kwargs: Any) -> None:
        try:
            await dispatch(*args, **kwargs)
        finally:
            finished.set()

    monkeypatch.setattr(rpc, "_send_message", send)
    monkeypatch.setattr(rpc, "_dispatch_request", dispatch_request)
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"), network=network)
    client._client = rpc
    client._register_client_global_handlers()
    return client, rpc, sent, finished


async def _report_connection_lost(rpc: JsonRpcClient) -> None:
    """Report connection loss from another thread, like the reader thread does."""
    on_close = rpc.on_close
    assert on_close is not None
    await asyncio.to_thread(on_close)


def _message(method: str, request_id: int) -> dict[str, Any]:
    return {"jsonrpc": "2.0", "id": request_id, "method": method, "params": METHODS[method][1]}


def _pending_callback(
    entered: asyncio.Event, cancelled: asyncio.Event, calls: list[Any] | None = None
) -> Callable[[Any], Coroutine[Any, Any, str | None]]:
    """A callback that never settles and reports its entry and cancellation."""

    async def callback(request: Any) -> str | None:
        if calls is not None:
            calls.append(request)
        entered.set()
        try:
            await asyncio.Event().wait()
        except asyncio.CancelledError:
            cancelled.set()
            raise
        return None

    return callback


@pytest.mark.parametrize("kind", ["async", "task"])
@pytest.mark.parametrize("method", list(METHODS))
async def test_runtime_cancel_request_cancels_async_callback(monkeypatch, method, kind) -> None:
    entered = asyncio.Event()
    cancelled = asyncio.Event()
    pending = _pending_callback(entered, cancelled)
    callback = {
        "async": pending,
        "task": lambda request: asyncio.create_task(pending(request)),
    }[kind]
    _client, rpc, sent, responded = _wire_network_client(
        monkeypatch, {METHODS[method][0]: callback}
    )

    rpc._handle_request(_message(method, 7))
    await asyncio.wait_for(entered.wait(), timeout=5)
    rpc._handle_cancel_request({"id": 7})
    await asyncio.wait_for(responded.wait(), timeout=5)
    await asyncio.wait_for(cancelled.wait(), timeout=5)

    assert (sent, rpc._incoming_request_cancellations) == (
        [
            {
                "jsonrpc": "2.0",
                "id": 7,
                "error": {"code": -32800, "message": "Request cancelled", "data": None},
            }
        ],
        {},
    )


@pytest.mark.parametrize("when", ["before dispatch", "with the answer"])
@pytest.mark.parametrize("method", list(METHODS))
async def test_runtime_cancellation_wins_over_an_answer(monkeypatch, method, when) -> None:
    calls: list[Any] = []
    rpcs: list[JsonRpcClient] = []

    def callback(request: Any) -> str | None:
        calls.append(request)
        if when == "with the answer":
            # The runtime cancels the request as the callback answers.
            rpcs[0]._handle_cancel_request({"id": 7})
        return "late"

    _client, rpc, sent, responded = _wire_network_client(
        monkeypatch, {METHODS[method][0]: callback}
    )
    rpcs.append(rpc)

    rpc._handle_request(_message(method, 7))
    if when == "before dispatch":
        # The reader thread handles the cancellation before the event loop dispatches the request.
        rpc._handle_cancel_request({"id": 7})
    await asyncio.wait_for(responded.wait(), timeout=5)

    assert (len(calls), sent, rpc._incoming_request_cancellations) == (
        0 if when == "before dispatch" else 1,
        [
            {
                "jsonrpc": "2.0",
                "id": 7,
                "error": {"code": -32800, "message": "Request cancelled", "data": None},
            }
        ],
        {},
    )


@pytest.mark.parametrize("method", list(METHODS))
async def test_late_failure_after_runtime_cancellation_is_not_unhandled(
    monkeypatch, method
) -> None:
    entered = asyncio.Event()
    release = asyncio.Event()
    failed = asyncio.Event()

    async def stubborn(_request: Any) -> str | None:
        entered.set()
        try:
            await asyncio.Event().wait()
        except asyncio.CancelledError:
            pass
        await release.wait()
        failed.set()
        raise RuntimeError("late failure")

    unhandled: list[object] = []
    loop = asyncio.get_running_loop()
    previous = loop.get_exception_handler()
    loop.set_exception_handler(lambda _loop, context: unhandled.append(context.get("message")))
    try:
        _client, rpc, sent, responded = _wire_network_client(
            monkeypatch, {METHODS[method][0]: stubborn}
        )
        rpc._handle_request(_message(method, 7))
        await asyncio.wait_for(entered.wait(), timeout=5)
        rpc._handle_cancel_request({"id": 7})
        await asyncio.wait_for(responded.wait(), timeout=5)
        release.set()
        await asyncio.wait_for(failed.wait(), timeout=5)
        for _ in range(10):
            await asyncio.sleep(0)
        # An unretrieved task exception is reported when the task is collected.
        gc.collect()
    finally:
        loop.set_exception_handler(previous)

    assert ([message["error"]["code"] for message in sent], unhandled) == ([-32800], [])


@pytest.mark.parametrize("kind", ["task", "async"])
@pytest.mark.parametrize("method", list(METHODS))
async def test_callback_cancelled_by_itself_replies_promptly(monkeypatch, method, kind) -> None:
    # A host lookup can cancel itself while the request stays live; the runtime must get an
    # answer instead of waiting for its callback deadline.
    lookups: list[asyncio.Task[Any]] = []
    entered = asyncio.Event()

    def lookup() -> asyncio.Task[Any]:
        lookups.append(asyncio.create_task(asyncio.Event().wait()))
        entered.set()
        return lookups[-1]

    async def run_lookup(_request: Any) -> str | None:
        await lookup()
        return None

    callback = {"task": lambda _request: lookup(), "async": run_lookup}[kind]
    option, _params, label = METHODS[method]
    _client, rpc, sent, responded = _wire_network_client(monkeypatch, {option: callback})

    rpc._handle_request(_message(method, 7))
    await asyncio.wait_for(entered.wait(), timeout=5)
    lookups[0].cancel()
    await asyncio.wait_for(responded.wait(), timeout=5)

    assert (sent, rpc._incoming_request_cancellations) == (
        [
            {
                "jsonrpc": "2.0",
                "id": 7,
                "error": {"code": -32603, "message": f"{label} request cancelled", "data": None},
            }
        ],
        {},
    )


@pytest.mark.parametrize("method", list(METHODS))
async def test_sync_callback_cancelled_by_itself_replies_promptly(monkeypatch, method) -> None:
    def cancelled(_request: Any) -> str | None:
        # For example, the result of a cancelled future.
        raise asyncio.CancelledError

    option, _params, label = METHODS[method]
    _client, rpc, sent, responded = _wire_network_client(monkeypatch, {option: cancelled})

    rpc._handle_request(_message(method, 7))
    await asyncio.wait_for(responded.wait(), timeout=5)

    assert (sent, rpc._incoming_request_cancellations) == (
        [
            {
                "jsonrpc": "2.0",
                "id": 7,
                "error": {"code": -32603, "message": f"{label} request cancelled", "data": None},
            }
        ],
        {},
    )


@pytest.mark.parametrize("teardown", ["connection_lost", "stop", "force_stop"])
@pytest.mark.parametrize("method", list(METHODS))
async def test_connection_close_cancels_pending_callback(monkeypatch, method, teardown) -> None:
    calls: list[Any] = []
    entered = asyncio.Event()
    cancelled = asyncio.Event()
    option, _params, label = METHODS[method]
    client, rpc, sent, finished = _wire_network_client(
        monkeypatch, {option: _pending_callback(entered, cancelled, calls)}
    )
    handler = rpc.request_handlers[method]

    rpc._handle_request(_message(method, 7))
    await asyncio.wait_for(entered.wait(), timeout=5)
    if teardown == "connection_lost":
        await _report_connection_lost(rpc)
    else:
        await getattr(client, teardown)()
    await asyncio.wait_for(cancelled.wait(), timeout=5)
    await asyncio.wait_for(finished.wait(), timeout=5)
    # A request that races the close fails without starting another lookup.
    with pytest.raises(RuntimeError, match=f"{label} connection closed"):
        await handler(METHODS[method][1])

    closed = {"code": -32603, "message": f"{label} connection closed", "data": None}
    expected = (
        [{"jsonrpc": "2.0", "id": 7, "error": closed}] if teardown == "connection_lost" else []
    )
    assert (len(calls), sent, rpc._incoming_request_cancellations) == (1, expected, {})


@pytest.mark.parametrize("answer", ["after_close", "with_close"])
@pytest.mark.parametrize("teardown", ["connection_lost", "stop", "force_stop"])
@pytest.mark.parametrize("method", list(METHODS))
async def test_connection_close_retires_callbacks_that_suppress_cancellation(
    monkeypatch, method, teardown, answer
) -> None:
    entered = asyncio.Event()
    cancelled = asyncio.Event()
    release = asyncio.Event()
    returned = asyncio.Event()

    async def stubborn(_request: Any) -> str | None:
        entered.set()
        try:
            await asyncio.Event().wait()
        except asyncio.CancelledError:
            cancelled.set()
        if answer == "after_close":
            await release.wait()
        # Otherwise the answer completes the callback in the same turn as the close.
        returned.set()
        return "late"

    option, _params, label = METHODS[method]
    client, rpc, sent, finished = _wire_network_client(monkeypatch, {option: stubborn})

    rpc._handle_request(_message(method, 7))
    try:
        await asyncio.wait_for(entered.wait(), timeout=5)
        if teardown == "connection_lost":
            await _report_connection_lost(rpc)
        else:
            await getattr(client, teardown)()
        await asyncio.wait_for(cancelled.wait(), timeout=5)
        await asyncio.wait_for(finished.wait(), timeout=5)
        # The callback's late result is discarded.
        release.set()
        await asyncio.wait_for(returned.wait(), timeout=5)

        closed = {"code": -32603, "message": f"{label} connection closed", "data": None}
        expected = (
            [{"jsonrpc": "2.0", "id": 7, "error": closed}] if teardown == "connection_lost" else []
        )
        assert (sent, rpc._incoming_request_cancellations) == (expected, {})
    finally:
        release.set()


async def test_connection_lost_cancels_both_callbacks(monkeypatch) -> None:
    entered = {method: asyncio.Event() for method in METHODS}
    cancelled = {method: asyncio.Event() for method in METHODS}
    network = cast(
        NetworkConfig,
        {
            option: _pending_callback(entered[method], cancelled[method])
            for method, (option, *_rest) in METHODS.items()
        },
    )
    _client, rpc, sent, _responded = _wire_network_client(monkeypatch, network)

    for request_id, method in enumerate(METHODS, start=1):
        rpc._handle_request(_message(method, request_id))
    await asyncio.wait_for(asyncio.gather(*(event.wait() for event in entered.values())), timeout=5)
    await _report_connection_lost(rpc)
    await asyncio.wait_for(
        asyncio.gather(*(event.wait() for event in cancelled.values())), timeout=5
    )
    for _ in range(100):
        if len(sent) == len(METHODS):
            break
        await asyncio.sleep(0)

    assert sorted(sent, key=lambda message: message["id"]) == [
        {
            "jsonrpc": "2.0",
            "id": request_id,
            "error": {"code": -32603, "message": f"{label} connection closed", "data": None},
        }
        for request_id, (_option, _params, label) in enumerate(METHODS.values(), start=1)
    ]


async def test_stale_connection_close_keeps_replacement_adapter_open(monkeypatch) -> None:
    client, old_rpc, _sent, _responded = _wire_network_client(
        monkeypatch,
        {
            "resolve_proxy": lambda _request: "PROXY proxy.example:8080",
            "authenticate_negotiate": lambda _request: "token",
        },
    )
    old_adapter = client._network_adapter
    # A reconnect installs a replacement connection and adapter before the old reader
    # reports its close.
    new_rpc = JsonRpcClient(Mock())
    new_rpc._loop = asyncio.get_running_loop()
    client._client = new_rpc
    client._register_client_global_handlers()
    replacement = client._network_adapter
    assert old_adapter is not None and replacement is not None

    await _report_connection_lost(old_rpc)
    for _ in range(10):
        if old_adapter._closed:
            break
        await asyncio.sleep(0)
    proxy = await replacement.resolve_proxy(NetworkResolveProxyRequest("https://a.example/"))
    token = await replacement.authenticate_negotiate(
        NetworkAuthenticateNegotiateRequest("HTTP/proxy.example")
    )

    assert (old_adapter._closed, replacement._closed, proxy.proxy, token.token) == (
        True,
        False,
        "PROXY proxy.example:8080",
        "token",
    )


@pytest.mark.parametrize(
    ("config", "message"),
    [
        ({"scope": "process", "resolve_proxy": _none}, "network.scope must be either"),
        ({"resolve_proxy": "not callable"}, "network.resolve_proxy must be callable"),
        (
            {"authenticate_negotiate": "not callable"},
            "network.authenticate_negotiate must be callable",
        ),
    ],
)
def test_rejects_invalid_config(config, message) -> None:
    with pytest.raises(ValueError, match=message):
        CopilotClient(
            connection=RuntimeConnection.for_uri("localhost:1234"),
            network=cast(NetworkConfig, config),
        )
