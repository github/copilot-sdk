from __future__ import annotations

import asyncio

import pytest
from test_installation_confirmation import _connect

from copilot import (
    EntraAuthOptions,
    EntraTokenAcquireRequestKind,
    EntraTokenAcquireResultKind,
    EntraTokenProviderResult,
)
from copilot._jsonrpc import JsonRpcError


def _request(kind="identity"):
    request = {"kind": kind, "clientId": "host-client", "reason": "signIn"}
    if kind == "token":
        request.update(
            accountId="host-account",
            scopes=["resource/.default"],
            clientId="resource-client",
            reason="renewal",
            audience="resource",
            tenant="tenant",
            skipCachedAccessToken=True,
        )
    return {"request": request}


@pytest.mark.parametrize("mats_app_name", [None, "SDK Test Host"])
async def test_typed_identity_and_resource_acquisition_does_not_serialize_callback(mats_app_name):
    observed = []

    async def provider(request, context):
        assert not context.cancelled.is_set()
        observed.append(request.to_dict())
        if request.kind == EntraTokenAcquireRequestKind.IDENTITY:
            return EntraTokenProviderResult(
                kind=EntraTokenAcquireResultKind.IDENTITY,
                account_id="host-account",
                username="display-name",
            )
        return EntraTokenProviderResult(
            kind=EntraTokenAcquireResultKind.TOKEN,
            account_id=request.account_id,
            access_token="host-resource-token",
        )

    options = EntraAuthOptions("host-client", provider, mats_app_name=mats_app_name)
    expected = {"clientId": "host-client"}
    if mats_app_name is not None:
        expected["matsAppName"] = mats_app_name
    assert options.to_dict() == expected
    harness = await _connect(entra=options)
    try:
        assert observed == []
        assert await harness.peer.request(1, "entraToken.getToken", _request()) == {
            "kind": "identity",
            "accountId": "host-account",
            "username": "display-name",
        }
        assert await harness.peer.request(2, "entraToken.getToken", _request("token")) == {
            "kind": "token",
            "accountId": "host-account",
            "accessToken": "host-resource-token",
        }
        assert observed == [_request()["request"], _request("token")["request"]]
    finally:
        await harness.close()


async def test_cancellation_rejects_late_success_and_preserves_other_requests():
    entered = asyncio.get_running_loop().create_future()

    async def provider(request, context):
        if request.kind == EntraTokenAcquireRequestKind.TOKEN:
            return EntraTokenProviderResult(kind=EntraTokenAcquireResultKind.UNAVAILABLE)
        entered.set_result(context)
        await context.cancelled.wait()
        return EntraTokenProviderResult(
            kind=EntraTokenAcquireResultKind.IDENTITY, account_id="late-account"
        )

    harness = await _connect(entra=EntraAuthOptions("host-client", provider))
    acquisition = asyncio.create_task(harness.peer.request(10, "entraToken.getToken", _request()))
    try:
        context = await asyncio.wait_for(entered, timeout=5)
        await harness.peer.notify("$/cancelRequest", {"id": 10})
        with pytest.raises(JsonRpcError) as error:
            await acquisition
        assert error.value.code == -32800
        assert context.cancelled.is_set()
        assert await harness.peer.request(11, "entraToken.getToken", _request("token")) == {
            "kind": "unavailable"
        }
    finally:
        if not acquisition.done():
            acquisition.cancel()
        await asyncio.gather(acquisition, return_exceptions=True)
        await harness.close()


async def test_retirement_before_dispatch_does_not_invoke_the_provider(monkeypatch):
    observed = []

    def provider(request, context):
        observed.append(context)
        return EntraTokenProviderResult(kind=EntraTokenAcquireResultKind.UNAVAILABLE)

    harness = await _connect(entra=EntraAuthOptions("host-client", provider))
    incoming_cancellation = harness.sdk_rpc.incoming_request_cancelled_event
    loop = asyncio.get_running_loop()

    def retire_before_dispatch(request_id):
        retired = incoming_cancellation(request_id)
        loop.call_soon(retired.set)
        return retired

    monkeypatch.setattr(harness.sdk_rpc, "incoming_request_cancelled_event", retire_before_dispatch)
    try:
        with pytest.raises(JsonRpcError) as error:
            await harness.peer.request(12, "entraToken.getToken", _request())
        assert error.value.code == -32800
        assert observed == []
    finally:
        await harness.close()


async def test_disconnect_cancels_the_callback_on_its_original_connection():
    entered = asyncio.get_running_loop().create_future()
    finished = asyncio.Event()

    async def provider(request, context):
        entered.set_result(context)
        await context.cancelled.wait()
        finished.set()
        return EntraTokenProviderResult(kind=EntraTokenAcquireResultKind.CANCELLED)

    harness = await _connect(entra=EntraAuthOptions("host-client", provider))
    acquisition = asyncio.create_task(harness.peer.request(20, "entraToken.getToken", _request()))
    try:
        context = await asyncio.wait_for(entered, timeout=5)
        await harness.close_peer_connection()
        await asyncio.wait_for(context.cancelled.wait(), timeout=5)
        await asyncio.wait_for(finished.wait(), timeout=5)
    finally:
        acquisition.cancel()
        await asyncio.gather(acquisition, return_exceptions=True)
        await harness.close()


async def test_old_connection_close_does_not_close_a_replacement_adapter():
    def provider(request, context):
        return EntraTokenProviderResult(kind=EntraTokenAcquireResultKind.UNAVAILABLE)

    first = await _connect(entra=EntraAuthOptions("host-client", provider))
    second = await _connect(entra=EntraAuthOptions("host-client", provider))
    original_adapter = first.client._entra_token_adapter  # noqa: SLF001
    on_close = first.sdk_rpc.on_close
    assert on_close is not None
    try:
        first.client._client = second.sdk_rpc  # noqa: SLF001
        first.client._entra_token_adapter = second.client._entra_token_adapter  # noqa: SLF001
        on_close()
        assert await second.peer.request(21, "entraToken.getToken", _request()) == {
            "kind": "unavailable"
        }
    finally:
        first.client._client = first.sdk_rpc  # noqa: SLF001
        first.client._entra_token_adapter = original_adapter  # noqa: SLF001
        try:
            await first.close()
        finally:
            await second.close()


async def test_callback_errors_do_not_expose_credentials():
    def provider(request, context):
        raise RuntimeError("credential-from-host-exception")

    harness = await _connect(entra=EntraAuthOptions("host-client", provider))
    try:
        with pytest.raises(JsonRpcError) as error:
            await harness.peer.request(30, "entraToken.getToken", _request())
        assert error.value.code == -32603
        assert error.value.message == "Entra token provider failed"
    finally:
        await harness.close()


async def test_provider_cancelled_awaitable_is_reported_as_a_redacted_provider_failure():
    observed = []

    async def provider(request, context):
        observed.append(context)
        raise asyncio.CancelledError("credential-from-host-exception")

    harness = await _connect(entra=EntraAuthOptions("host-client", provider))
    try:
        with pytest.raises(JsonRpcError) as error:
            await harness.peer.request(40, "entraToken.getToken", _request())
        assert error.value.code == -32603
        assert error.value.message == "Entra token provider failed"
        assert len(observed) == 1
        assert observed[0].cancelled.is_set()
    finally:
        await harness.close()


def test_result_repr_redacts_credentials():
    result = EntraTokenProviderResult(
        kind=EntraTokenAcquireResultKind.TOKEN, account_id="account", access_token="secret-value"
    )
    assert "secret-value" not in repr(result)
    assert "account" not in repr(result)
