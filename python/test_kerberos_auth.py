"""Kerberos authentication callback registration and routing without a live runtime."""

from __future__ import annotations

import asyncio
import base64
import contextlib
import os
from collections.abc import Awaitable, Callable
from typing import Any, cast
from unittest.mock import AsyncMock, Mock

import pytest

from copilot import (
    CopilotClient,
    CopilotRequestHandler,
    KerberosAuthAcquireRequest,
    KerberosAuthConfig,
    KerberosAuthMode,
    KerberosAuthScope,
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


async def _unavailable(_request: KerberosAuthAcquireRequest) -> str | None:
    return None


SESSION_FS = {
    "initial_working_directory": "/workspace",
    "session_state_path": "/state",
    "conventions": "posix",
}


@pytest.mark.parametrize(
    ("config", "wire_params"),
    [
        ({"mode": "fallback"}, {"mode": "fallback"}),
        (
            {"mode": KerberosAuthMode.ALWAYS, "scope": "runtime"},
            {"mode": "always", "scope": "runtime"},
        ),
        (
            {"mode": "always", "scope": KerberosAuthScope.CONNECTION},
            {"mode": "always", "scope": "connection"},
        ),
    ],
)
async def test_start_registers_provider_before_other_providers(config, wire_params) -> None:
    fake = FakeJsonRpcClient()
    client = make_client(
        fake, kerberos_auth={**config, "callback": _unavailable}, session_fs=SESSION_FS
    )

    await client.start()

    assert fake.requests == [
        ("kerberosAuth.setProvider", wire_params),
        (
            "sessionFs.setProvider",
            {"initialCwd": "/workspace", "sessionStatePath": "/state", "conventions": "posix"},
        ),
    ]


async def test_start_without_option_does_not_register_or_handle() -> None:
    fake = FakeJsonRpcClient()
    client = make_client(fake)

    await client.start()

    assert fake.requests == []
    with pytest.raises(RuntimeError, match="No kerberos_auth client-global handler registered"):
        await fake.request_handlers["kerberosAuth.acquire"]({"spn": "HTTP/proxy"})


async def test_registration_failure_fails_start() -> None:
    fake = FakeJsonRpcClient(fail_method="kerberosAuth.setProvider")
    client = make_client(
        fake, kerberos_auth={"mode": "always", "callback": _unavailable}, session_fs=SESSION_FS
    )

    with pytest.raises(RuntimeError, match="kerberosAuth.setProvider failed"):
        await client.start()

    assert [method for method, _ in fake.requests] == ["kerberosAuth.setProvider"]
    assert (client._state, client._client) == ("error", None)


async def test_cancelled_registration_closes_connection_and_propagates() -> None:
    fake = FakeJsonRpcClient(block_method="kerberosAuth.setProvider")
    client = make_client(
        fake, kerberos_auth={"mode": "always", "callback": _unavailable}, session_fs=SESSION_FS
    )
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
    ) == (["kerberosAuth.setProvider"], "disconnected", None, True)


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
        kerberos_auth={"mode": "always", "scope": "runtime", "callback": _unavailable},
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
        ["kerberosAuth.setProvider", method],
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

    client = make_client(fake, verify, kerberos_auth={"mode": "always", "callback": _unavailable})
    starting = asyncio.create_task(client.start())
    await asyncio.wait_for(verifying.wait(), timeout=1)

    await asyncio.wait_for(
        asyncio.gather(starting, getattr(client, method)(session_id="session")), timeout=5
    )

    assert [name for name, _ in fake.requests] == ["kerberosAuth.setProvider", wire_method]


@pytest.mark.parametrize("kind", ["async", "sync", "task", "coroutine"])
async def test_acquire_routes_to_callback(monkeypatch, kind) -> None:
    calls: list[KerberosAuthAcquireRequest] = []

    def acquire(request: KerberosAuthAcquireRequest) -> str | None:
        calls.append(request)
        if request.spn == "HTTP/failing.example":
            raise RuntimeError("lookup failed")
        if request.spn == "HTTP/unavailable.example":
            return None
        return base64.b64encode(request.spn.encode()).decode()

    async def acquire_async(request: KerberosAuthAcquireRequest) -> str | None:
        await asyncio.sleep(0)
        return acquire(request)

    callbacks: dict[str, Callable[[KerberosAuthAcquireRequest], Any]] = {
        "async": acquire_async,
        "sync": acquire,
        # Ordinary callables that need a running loop, or return work for the client's loop.
        "task": lambda request: asyncio.create_task(acquire_async(request)),
        "coroutine": lambda request: acquire_async(request),
    }
    config: KerberosAuthConfig = {"mode": "fallback", "callback": callbacks[kind]}
    rpc = JsonRpcClient(Mock())
    sent: list[dict[str, Any]] = []

    async def send(message: dict[str, Any]) -> None:
        sent.append(message)

    monkeypatch.setattr(rpc, "_send_message", send)
    client = CopilotClient(
        connection=RuntimeConnection.for_uri("localhost:1234"), kerberos_auth=config
    )
    client._client = rpc
    client._register_client_global_handlers()
    handler = rpc.request_handlers["kerberosAuth.acquire"]
    spns = [
        "HTTP/proxy.example",
        "ldap/directory.example@EXAMPLE.COM",
        "HTTP/unavailable.example",
        "HTTP/failing.example",
    ]

    for request_id, spn in enumerate(spns):
        message = {
            "jsonrpc": "2.0",
            "id": request_id,
            "method": "kerberosAuth.acquire",
            "params": {"spn": spn},
        }
        await rpc._dispatch_request(message, handler, None)

    assert (calls, sent) == (
        [KerberosAuthAcquireRequest(spn) for spn in spns],
        [
            {"jsonrpc": "2.0", "id": 0, "result": {"token": "SFRUUC9wcm94eS5leGFtcGxl"}},
            {
                "jsonrpc": "2.0",
                "id": 1,
                "result": {"token": "bGRhcC9kaXJlY3RvcnkuZXhhbXBsZUBFWEFNUExFLkNPTQ=="},
            },
            {"jsonrpc": "2.0", "id": 2, "result": {}},
            {
                "jsonrpc": "2.0",
                "id": 3,
                "error": {"code": -32603, "message": "lookup failed", "data": None},
            },
        ],
    )


@pytest.mark.parametrize("kind", ["async", "task"])
async def test_runtime_cancel_request_cancels_async_callback(monkeypatch, kind) -> None:
    loop = asyncio.get_running_loop()
    entered = asyncio.Event()
    cancelled = asyncio.Event()
    responded = asyncio.Event()

    async def acquire(_request: KerberosAuthAcquireRequest) -> str | None:
        entered.set()
        try:
            await asyncio.Event().wait()
        except asyncio.CancelledError:
            cancelled.set()
            raise
        return None

    callbacks: dict[str, Callable[[KerberosAuthAcquireRequest], Any]] = {
        "async": acquire,
        "task": lambda request: asyncio.create_task(acquire(request)),
    }
    rpc = JsonRpcClient(Mock())
    rpc._loop = loop
    sent: list[dict[str, Any]] = []

    async def send(message: dict[str, Any]) -> None:
        sent.append(message)
        responded.set()

    monkeypatch.setattr(rpc, "_send_message", send)
    client = CopilotClient(
        connection=RuntimeConnection.for_uri("localhost:1234"),
        kerberos_auth={"mode": "always", "callback": callbacks[kind]},
    )
    client._client = rpc
    client._register_client_global_handlers()

    rpc._handle_request(
        {
            "jsonrpc": "2.0",
            "id": 7,
            "method": "kerberosAuth.acquire",
            "params": {"spn": "HTTP/proxy.example"},
        }
    )
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


def _wire_kerberos_client(
    monkeypatch: pytest.MonkeyPatch, callback: Callable[[KerberosAuthAcquireRequest], Any]
) -> tuple[CopilotClient, JsonRpcClient, list[dict[str, Any]], asyncio.Event]:
    rpc = JsonRpcClient(Mock())
    rpc._loop = asyncio.get_running_loop()
    sent: list[dict[str, Any]] = []
    responded = asyncio.Event()

    async def send(message: dict[str, Any]) -> None:
        sent.append(message)
        responded.set()

    monkeypatch.setattr(rpc, "_send_message", send)
    client = CopilotClient(
        connection=RuntimeConnection.for_uri("localhost:1234"),
        kerberos_auth={"mode": "always", "callback": callback},
    )
    client._client = rpc
    client._register_client_global_handlers()
    return client, rpc, sent, responded


def _acquire_message(request_id: int) -> dict[str, Any]:
    return {
        "jsonrpc": "2.0",
        "id": request_id,
        "method": "kerberosAuth.acquire",
        "params": {"spn": "HTTP/proxy.example"},
    }


@pytest.mark.parametrize("kind", ["task", "async"])
async def test_callback_cancelled_by_itself_replies_promptly(monkeypatch, kind) -> None:
    # A credential broker can cancel its own lookup while the request stays live; the runtime
    # must get an answer instead of waiting for its callback deadline.
    lookups: list[asyncio.Task[Any]] = []
    entered = asyncio.Event()

    def lookup() -> asyncio.Task[Any]:
        lookups.append(asyncio.create_task(asyncio.Event().wait()))
        entered.set()
        return lookups[-1]

    async def acquire(_request: KerberosAuthAcquireRequest) -> str | None:
        await lookup()
        return None

    callbacks: dict[str, Callable[[KerberosAuthAcquireRequest], Any]] = {
        "task": lambda _request: lookup(),
        "async": acquire,
    }
    _client, rpc, sent, responded = _wire_kerberos_client(monkeypatch, callbacks[kind])

    rpc._handle_request(_acquire_message(7))
    await asyncio.wait_for(entered.wait(), timeout=5)
    lookups[0].cancel()
    await asyncio.wait_for(responded.wait(), timeout=5)

    assert (sent, rpc._incoming_request_cancellations) == (
        [
            {
                "jsonrpc": "2.0",
                "id": 7,
                "error": {
                    "code": -32603,
                    "message": "Kerberos credential callback was cancelled",
                    "data": None,
                },
            }
        ],
        {},
    )


@pytest.mark.parametrize("teardown", ["connection_lost", "stop", "force_stop"])
async def test_connection_close_cancels_pending_callback(monkeypatch, teardown) -> None:
    calls: list[KerberosAuthAcquireRequest] = []
    entered = asyncio.Event()
    cancelled = asyncio.Event()

    async def acquire(request: KerberosAuthAcquireRequest) -> str | None:
        calls.append(request)
        entered.set()
        try:
            await asyncio.Event().wait()
        except asyncio.CancelledError:
            cancelled.set()
            raise
        return None

    client, rpc, sent, responded = _wire_kerberos_client(monkeypatch, acquire)
    handler = rpc.request_handlers["kerberosAuth.acquire"]

    rpc._handle_request(_acquire_message(7))
    await asyncio.wait_for(entered.wait(), timeout=5)
    if teardown == "connection_lost":
        # The reader thread reports connection loss.
        await asyncio.to_thread(rpc.on_close)
    else:
        await getattr(client, teardown)()
    await asyncio.wait_for(cancelled.wait(), timeout=5)
    await asyncio.wait_for(responded.wait(), timeout=5)
    # A request that races the close fails without starting another lookup.
    await rpc._dispatch_request(_acquire_message(8), handler, None)

    closed = {"code": -32603, "message": "Kerberos auth connection closed", "data": None}
    assert (len(calls), sent) == (
        1,
        [
            {"jsonrpc": "2.0", "id": 7, "error": closed},
            {"jsonrpc": "2.0", "id": 8, "error": closed},
        ],
    )


async def test_stale_connection_close_keeps_replacement_adapter_open(monkeypatch) -> None:
    def acquire(_request: KerberosAuthAcquireRequest) -> str | None:
        return "token"

    client, old_rpc, _sent, _responded = _wire_kerberos_client(monkeypatch, acquire)
    old_adapter = client._kerberos_auth_adapter
    # A reconnect installs a replacement connection and adapter before the old reader
    # reports its close.
    new_rpc = JsonRpcClient(Mock())
    new_rpc._loop = asyncio.get_running_loop()
    client._client = new_rpc
    client._register_client_global_handlers()
    replacement = client._kerberos_auth_adapter
    assert old_adapter is not None and replacement is not None

    await asyncio.to_thread(old_rpc.on_close)
    for _ in range(10):
        if old_adapter._closed:
            break
        await asyncio.sleep(0)
    result = await replacement.acquire(
        KerberosAuthAcquireRequest.from_dict({"spn": "HTTP/proxy.example"})
    )

    assert (old_adapter._closed, replacement._closed, result.token) == (True, False, "token")


@pytest.mark.parametrize(
    ("config", "message"),
    [
        ({"mode": "never"}, "kerberos_auth.mode must be either"),
        ({"mode": "always", "scope": "process"}, "kerberos_auth.scope must be either"),
    ],
)
def test_rejects_invalid_config(config, message) -> None:
    with pytest.raises(ValueError, match=message):
        CopilotClient(
            connection=RuntimeConnection.for_uri("localhost:1234"),
            kerberos_auth=cast(KerberosAuthConfig, {**config, "callback": _unavailable}),
        )
