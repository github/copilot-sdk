from __future__ import annotations

import asyncio
import os
from typing import Any, cast

import pytest

from copilot import (
    CopilotClient,
    InstallationConfirmationContext,
    RuntimeConnection,
)
from copilot._jsonrpc import JsonRpcClient, JsonRpcError
from copilot.rpc import InstallationConfirmationRequest, InstallationDecision


class _EndpointProcess:
    def __init__(self, stdin, stdout) -> None:
        self.stdin = stdin
        self.stdout = stdout
        self.stderr = None
        self.returncode: int | None = None

    def poll(self) -> int | None:
        return self.returncode

    def terminate(self) -> None:
        self.returncode = 0
        for stream in (self.stdin, self.stdout):
            try:
                stream.close()
            except OSError:
                pass


class _Peer:
    def __init__(self, client: JsonRpcClient) -> None:
        self.client = client

    async def request(self, request_id: int | str, method: str, params: dict[str, Any]) -> Any:
        assert self.client._loop is not None  # noqa: SLF001
        future = self.client._loop.create_future()  # noqa: SLF001
        with self.client._pending_lock:  # noqa: SLF001
            pending_requests = cast(
                dict[int | str, asyncio.Future[Any]], self.client.pending_requests
            )
            pending_requests[request_id] = future
        await self.client._send_message(  # noqa: SLF001
            {"jsonrpc": "2.0", "id": request_id, "method": method, "params": params}
        )
        try:
            return await asyncio.wait_for(future, timeout=5)
        finally:
            with self.client._pending_lock:  # noqa: SLF001
                pending_requests = cast(
                    dict[int | str, asyncio.Future[Any]], self.client.pending_requests
                )
                pending_requests.pop(request_id, None)

    async def notify(self, method: str, params: dict[str, Any]) -> None:
        await self.client.notify(method, params)


class _Harness:
    def __init__(
        self,
        client: CopilotClient,
        sdk_rpc: JsonRpcClient,
        peer_rpc: JsonRpcClient,
        sdk_process: _EndpointProcess,
        peer_process: _EndpointProcess,
    ) -> None:
        self.client = client
        self.sdk_rpc = sdk_rpc
        self.peer_rpc = peer_rpc
        self.peer = _Peer(peer_rpc)
        self.sdk_process = sdk_process
        self.peer_process = peer_process

    async def close_peer_connection(self) -> None:
        self.peer_process.stdin.close()
        await asyncio.sleep(0)

    async def close(self) -> None:
        self.peer_process.terminate()
        self.sdk_process.terminate()
        await self.peer_rpc.stop()
        await self.sdk_rpc.stop()


def _pipe_process_pair() -> tuple[_EndpointProcess, _EndpointProcess]:
    sdk_to_peer_read, sdk_to_peer_write = os.pipe()
    peer_to_sdk_read, peer_to_sdk_write = os.pipe()
    sdk_process = _EndpointProcess(
        os.fdopen(sdk_to_peer_write, "wb", buffering=0),
        os.fdopen(peer_to_sdk_read, "rb", buffering=0),
    )
    peer_process = _EndpointProcess(
        os.fdopen(peer_to_sdk_write, "wb", buffering=0),
        os.fdopen(sdk_to_peer_read, "rb", buffering=0),
    )
    return sdk_process, peer_process


def _request(name: str) -> dict[str, Any]:
    return {
        "confirmationId": f"challenge-{name}",
        "operationId": name,
        "policySessionId": "original-session",
        "expiresAt": "2026-09-24T03:00:00Z",
        "reviewFingerprint": f"fingerprint-{name}",
        "review": {
            "resource": "mcp",
            "review": {
                "action": "install",
                "identity": {"canonicalName": "io.example/server", "serverName": "example"},
                "provenance": {
                    "authority": "cards.example.test",
                    "validatedAt": "2026-09-24T02:59:00Z",
                    "cardDigest": {
                        "algorithm": "sha256-rfc8785",
                        "value": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                    },
                    "mediaType": "application/mcp-server-card+json",
                },
                "target": {"scope": "user", "configKey": "example"},
                "policy": {"decision": "allowed", "source": "none"},
                "selectedChoice": {
                    "choiceId": "remote-choice",
                    "installMethod": "remote",
                    "endpoint": "https://example.test/mcp",
                    "transport": "streamable-http",
                    "requiredValues": [],
                    "secretPlaceholders": [],
                },
                "configurationChange": {
                    "operation": "add",
                    "scope": "user",
                    "configKey": "example",
                    "changedFields": ["type", "url", "headers", "tools"],
                    "secretReferences": [],
                },
                "inputs": [],
                "suppliedSecrets": [],
                "secretStorage": "keychain",
                "effectiveConfiguration": {
                    "transport": "streamable-http",
                    "url": "https://example.test/mcp",
                    "headers": {"X-Region": "eu"},
                    "tools": ["*"],
                },
            },
        },
    }


async def _connect(handler=None) -> _Harness:
    sdk_process, peer_process = _pipe_process_pair()
    sdk_rpc = JsonRpcClient(sdk_process)
    peer_rpc = JsonRpcClient(peer_process)
    client = CopilotClient(
        connection=RuntimeConnection.for_uri("localhost:1234"),
        installation_confirmation_handler=handler,
    )
    client._client = sdk_rpc  # noqa: SLF001
    sdk_rpc.on_close = client._handle_connection_close  # noqa: SLF001
    loop = asyncio.get_running_loop()
    sdk_rpc.start(loop)
    peer_rpc.start(loop)
    client._register_client_global_handlers()  # noqa: SLF001
    return _Harness(client, sdk_rpc, peer_rpc, sdk_process, peer_process)


async def _wait_for(predicate, *, timeout: float = 5.0) -> None:
    deadline = asyncio.get_running_loop().time() + timeout
    while not predicate():
        if asyncio.get_running_loop().time() >= deadline:
            raise AssertionError("condition was not met before timeout")
        await asyncio.sleep(0)


async def test_presents_typed_full_review_and_echoes_challenge_and_fingerprint() -> None:
    seen: list[tuple[InstallationConfirmationRequest, InstallationConfirmationContext]] = []

    async def handler(
        incoming: InstallationConfirmationRequest,
        context: InstallationConfirmationContext,
    ) -> InstallationDecision:
        seen.append((incoming, context))
        assert not context.cancelled.is_set()
        assert incoming.operation_id == "a"
        assert incoming.policy_session_id == "original-session"
        assert incoming.review.review.action.value == "install"
        incoming.confirmation_id = "mutated"
        incoming.review_fingerprint = "mutated"
        return InstallationDecision.CONFIRM

    harness = await _connect(handler)
    try:
        response = await harness.peer.request(301, "installations.confirm", _request("a"))
        assert response == {
            "confirmationId": "challenge-a",
            "reviewFingerprint": "fingerprint-a",
            "decision": "confirm",
        }
        assert len(seen) == 1
    finally:
        await harness.close()


async def test_keeps_concurrent_reviews_independent_and_serves_other_rpc() -> None:
    decisions = {
        "a": asyncio.get_running_loop().create_future(),
        "b": asyncio.get_running_loop().create_future(),
    }
    seen: list[str] = []

    def handler(
        incoming: InstallationConfirmationRequest,
        _context: InstallationConfirmationContext,
    ):
        seen.append(incoming.operation_id)
        return decisions[incoming.operation_id]

    harness = await _connect(handler)
    try:
        first = asyncio.create_task(
            harness.peer.request(401, "installations.confirm", _request("a"))
        )
        second = asyncio.create_task(
            harness.peer.request(402, "installations.confirm", _request("b"))
        )
        await _wait_for(lambda: seen == ["a", "b"])

        with pytest.raises(JsonRpcError, match="No GitHub token provider"):
            await harness.peer.request(
                403,
                "gitHubToken.getToken",
                {"registrationId": "unknown", "host": "github.com", "reason": "initial"},
            )

        decisions["b"].set_result("decline")
        assert await second == {
            "confirmationId": "challenge-b",
            "reviewFingerprint": "fingerprint-b",
            "decision": "decline",
        }
        decisions["a"].set_result("confirm")
        assert (await first)["confirmationId"] == "challenge-a"
    finally:
        await harness.close()


async def test_cancel_request_retires_only_cancelled_review_and_drops_late_decision() -> None:
    late_decision = asyncio.get_running_loop().create_future()
    contexts: dict[str, InstallationConfirmationContext] = {}

    def handler(
        incoming: InstallationConfirmationRequest,
        context: InstallationConfirmationContext,
    ):
        contexts[incoming.operation_id] = context
        if incoming.operation_id == "a":
            return late_decision
        return "confirm"

    harness = await _connect(handler)
    try:
        first = asyncio.create_task(
            harness.peer.request(501, "installations.confirm", _request("a"))
        )
        await _wait_for(lambda: "a" in contexts)
        await harness.peer.notify("$/cancelRequest", {"id": 501})
        with pytest.raises(JsonRpcError) as cancelled:
            await first
        assert cancelled.value.code == -32800
        assert contexts["a"].cancelled.is_set()

        late_decision.set_result("confirm")
        response = await harness.peer.request(502, "installations.confirm", _request("b"))
        assert response["confirmationId"] == "challenge-b"
        assert not contexts["b"].cancelled.is_set()
    finally:
        await harness.close()


async def test_cancel_request_and_decision_ready_together_rejects_decision() -> None:
    late_decision = asyncio.get_running_loop().create_future()
    context: InstallationConfirmationContext | None = None

    def handler(
        _incoming: InstallationConfirmationRequest,
        incoming_context: InstallationConfirmationContext,
    ):
        nonlocal context
        context = incoming_context
        return late_decision

    harness = await _connect(handler)
    try:
        pending = asyncio.create_task(
            harness.peer.request(551, "installations.confirm", _request("a"))
        )
        await _wait_for(lambda: context is not None)
        harness.sdk_rpc._handle_cancel_request({"id": 551})  # noqa: SLF001
        late_decision.set_result("confirm")

        with pytest.raises(JsonRpcError) as cancelled:
            await pending
        assert cancelled.value.code == -32800
        assert context is not None
        assert context.cancelled.is_set()
    finally:
        await harness.close()


async def test_stale_unknown_and_string_cancel_id_do_not_touch_successor() -> None:
    contexts: dict[str, InstallationConfirmationContext] = {}

    def handler(
        incoming: InstallationConfirmationRequest,
        context: InstallationConfirmationContext,
    ):
        contexts[incoming.operation_id] = context
        return "decline"

    harness = await _connect(handler)
    try:
        await harness.peer.notify("$/cancelRequest", {"id": 601})
        await harness.peer.notify("$/cancelRequest", {"id": 999})
        await harness.peer.notify("$/cancelRequest", {"id": "602"})

        response = await harness.peer.request(602, "installations.confirm", _request("b"))
        assert response["decision"] == "decline"
        assert not contexts["b"].cancelled.is_set()
    finally:
        await harness.close()


async def test_connection_close_cancels_all_reviews_and_isolated_to_original_connection() -> None:
    decisions = {
        "a": asyncio.get_running_loop().create_future(),
        "b": asyncio.get_running_loop().create_future(),
    }
    originals: dict[str, InstallationConfirmationContext] = {}

    def first_handler(
        incoming: InstallationConfirmationRequest,
        context: InstallationConfirmationContext,
    ):
        originals[incoming.operation_id] = context
        return decisions[incoming.operation_id]

    first = await _connect(first_handler)
    second = await _connect(lambda _request, _context: "decline")
    try:
        pending_a = asyncio.create_task(
            first.peer.request(701, "installations.confirm", _request("a"))
        )
        pending_b = asyncio.create_task(
            first.peer.request(702, "installations.confirm", _request("b"))
        )
        await _wait_for(lambda: set(originals) == {"a", "b"})
        await first.close_peer_connection()
        await _wait_for(lambda: all(context.cancelled.is_set() for context in originals.values()))
        decisions["a"].set_result("confirm")
        decisions["b"].set_result("decline")
        pending_a.cancel()
        pending_b.cancel()
        await asyncio.gather(pending_a, pending_b, return_exceptions=True)

        response = await second.peer.request(703, "installations.confirm", _request("c"))
        assert response["decision"] == "decline"
    finally:
        await first.close()
        await second.close()


async def test_stopping_client_retires_pending_reviews() -> None:
    decision = asyncio.get_running_loop().create_future()
    context: InstallationConfirmationContext | None = None

    def handler(_incoming: InstallationConfirmationRequest, incoming_context):
        nonlocal context
        context = incoming_context
        return decision

    harness = await _connect(handler)
    try:
        pending = asyncio.create_task(
            harness.peer.request(801, "installations.confirm", _request("a"))
        )
        await _wait_for(lambda: context is not None)
        await harness.client.stop()
        await _wait_for(lambda: context is not None and context.cancelled.is_set())
        assert context is not None
        assert context.cancelled.is_set()
        decision.set_result("confirm")
        pending.cancel()
        await asyncio.gather(pending, return_exceptions=True)
    finally:
        await harness.close()


async def test_refuses_missing_handler_invalid_review_handler_errors_and_unknown_decisions():
    missing = await _connect()
    try:
        with pytest.raises(JsonRpcError, match="No installations client-global handler registered"):
            await missing.peer.request(901, "installations.confirm", _request("a"))
    finally:
        await missing.close()

    invalid = await _connect(lambda _request, _context: "confirm")
    try:
        with pytest.raises(JsonRpcError) as exc:
            await invalid.peer.request(902, "installations.confirm", {"confirmationId": "only"})
        assert exc.value.code == -32602
    finally:
        await invalid.close()

    failing = await _connect(lambda _request, _context: (_ for _ in ()).throw(RuntimeError("no")))
    try:
        with pytest.raises(JsonRpcError, match="no"):
            await failing.peer.request(903, "installations.confirm", _request("a"))
    finally:
        await failing.close()

    unknown = await _connect(lambda _request, _context: "approve")
    try:
        with pytest.raises(JsonRpcError, match="Invalid installation confirmation decision"):
            await unknown.peer.request(904, "installations.confirm", _request("a"))
    finally:
        await unknown.close()


async def test_does_not_infer_optional_legacy_session_authority() -> None:
    observed: list[str | None] = []

    def handler(
        incoming: InstallationConfirmationRequest,
        _context: InstallationConfirmationContext,
    ):
        observed.append(incoming.policy_session_id)
        return "decline"

    incoming = _request("legacy")
    del incoming["policySessionId"]
    harness = await _connect(handler)
    try:
        response = await harness.peer.request(1001, "installations.confirm", incoming)
        assert response["decision"] == "decline"
        assert observed == [None]
    finally:
        await harness.close()
