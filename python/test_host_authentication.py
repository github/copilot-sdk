# Copyright (c) Microsoft Corporation. All rights reserved.

import asyncio
import json
import logging
from datetime import UTC, datetime, timedelta
from typing import Any
from unittest.mock import AsyncMock

import pytest

from copilot import (
    AhpAuthenticationResult,
    AhpHostAuthentication,
    AhpHostOptions,
    AhpSessionAuthentication,
    CopilotClient,
    RuntimeConnection,
)
from copilot._jsonrpc import JsonRpcClient
from copilot.rpc import HostGitHubEnvironmentOptions, HostLocalServerOptions
from test_host import client_fixture, handoff
from test_jsonrpc import MockProcess


def future():
    return (datetime.now(UTC) + timedelta(minutes=1)).isoformat()


def custom(callback):
    return AhpHostAuthentication("custom", require_identity="owner", authenticate=callback)


async def test_authentication_cancelled_before_handler_task_starts():
    client, _ = client_fixture()
    authenticate = AsyncMock(return_value=AhpAuthenticationResult("owner", future()))
    host = await client.start_ahp_host(
        AhpHostOptions(authentication=custom(authenticate), local_server=HostLocalServerOptions())
    )

    class RecordingJsonRpcClient(JsonRpcClient):
        def __init__(self):
            super().__init__(MockProcess())
            self.responses: list[tuple[Any, ...]] = []
            self.done = asyncio.Event()

        async def _send_response(self, request_id: str, result: Any) -> None:
            self.responses.append((request_id, result))
            self.done.set()

        async def _send_error_response(
            self, request_id: str, code: int, message: str, data: dict[str, Any] | None
        ) -> None:
            self.responses.append((request_id, code, message, data))
            self.done.set()

    rpc = RecordingJsonRpcClient()
    rpc._loop = asyncio.get_running_loop()
    rpc.set_request_handler(
        "host.validateAuthentication", client._ahp_hosts.validate_authentication, ordered=True
    )
    rpc.set_notification_handler(
        lambda method, params: client._ahp_hosts.notification(method, params)
    )
    params = {"hostId": host.host_id, "requestId": "validation", "token": "secret"}
    # Queue both messages before the loop can start the validation coroutine.
    rpc._handle_message(
        {"jsonrpc": "2.0", "id": 1, "method": "host.validateAuthentication", "params": params}
    )
    rpc._handle_message(
        {"jsonrpc": "2.0", "method": "host.authenticationCancelled", "params": params}
    )
    await asyncio.wait_for(rpc.done.wait(), 1)
    assert rpc.responses == [(1, -32603, "AHP authentication ended", None)]
    authenticate.assert_not_called()
    assert not client._ahp_hosts._authentications


@pytest.mark.parametrize("mode", ["copilot-cli", "empty"])
@pytest.mark.parametrize(
    "authentication,mission_control",
    [
        (None, False),
        (AhpHostAuthentication("custom", " ", AsyncMock()), False),
        (AhpHostAuthentication("custom", "owner"), False),
        (AhpHostAuthentication("none"), True),
        (custom(AsyncMock()), True),
    ],
)
async def test_invalid_policy_fails_before_connection(mode, authentication, mission_control):
    client = CopilotClient(mode=mode, connection=RuntimeConnection.for_uri("localhost:1234"))
    client.start = AsyncMock()
    with pytest.raises(ValueError, match="authentication"):
        await client.start_ahp_host(
            AhpHostOptions(
                authentication=authentication,
                local_server=HostLocalServerOptions(),
                github_environment=(
                    HostGitHubEnvironmentOptions(name="test", compute_id="test")
                    if mission_control
                    else None
                ),
            )
        )
    client.start.assert_not_called()
    assert not client._ahp_hosts._hosts


@pytest.mark.parametrize("separator", ["T", "t"])
@pytest.mark.parametrize("zone", ["Z", "z", "+00:00"])
async def test_authentication_accepts_rfc3339_case_variants(separator, zone):
    client, _ = client_fixture()
    expiry = future().replace("T", separator).replace("+00:00", zone)
    authenticate = AsyncMock(return_value=AhpAuthenticationResult("owner", expiry))
    host = await client.start_ahp_host(
        AhpHostOptions(authentication=custom(authenticate), local_server=HostLocalServerOptions())
    )
    assert await client._ahp_hosts.validate_authentication(
        {"hostId": host.host_id, "requestId": "validation", "token": "admission-only"}
    ) == {"identity": "owner", "expiresAt": expiry}
    assert not client._ahp_hosts._authentications
    client._ahp_hosts.disconnect()


@pytest.mark.parametrize("failure", ["null", "throw", "identity", "missing", "invalid", "past"])
async def test_authentication_rejects_invalid_results(failure):
    client, rpc = client_fixture()

    async def authenticate(request):
        assert request.token == "decrypted-secret"
        assert not request.cancellation_event.is_set()
        if failure == "null":
            return None
        if failure == "throw":
            raise ValueError("validator unavailable")
        if failure == "missing":
            return {"identity": "owner"}
        return AhpAuthenticationResult(
            "wrong" if failure == "identity" else "owner",
            {"invalid": "tomorrow", "past": "2000-01-01T00:00:00Z"}.get(failure, future()),
        )

    host = await client.start_ahp_host(
        AhpHostOptions(authentication=custom(authenticate), local_server=HostLocalServerOptions())
    )
    params = {"hostId": host.host_id, "requestId": "validation", "token": "decrypted-secret"}
    if failure == "throw":
        with pytest.raises(RuntimeError, match="AHP authentication failed"):
            await client._ahp_hosts.validate_authentication(params)
    else:
        assert await client._ahp_hosts.validate_authentication(params) is None
    assert rpc.request.call_args.args[1]["authentication"] == {
        "mode": "custom",
        "requireIdentity": "owner",
    }
    assert not client._ahp_hosts._authentications


@pytest.mark.parametrize("error_type", [RuntimeError, asyncio.CancelledError])
async def test_authenticator_failure_does_not_disclose_token_in_rpc_or_logs(caplog, error_type):
    client, _ = client_fixture()
    token = "synthetic-private-admission-token"
    factory = AsyncMock()

    async def authenticate(request):
        raise error_type(f"invalid token: {request.token}")

    host = await client.start_ahp_host(
        AhpHostOptions(
            authentication=custom(authenticate),
            local_server=HostLocalServerOptions(),
            create_session=factory,
        )
    )
    rpc = JsonRpcClient(MockProcess())
    rpc._send_response = AsyncMock()
    rpc._send_error_response = AsyncMock()
    with caplog.at_level(logging.DEBUG, logger="copilot"):
        await rpc._dispatch_request(
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "host.validateAuthentication",
                "params": {"hostId": host.host_id, "requestId": "validation", "token": token},
            },
            client._ahp_hosts.validate_authentication,
            None,
        )
    rpc._send_response.assert_not_called()
    expected = (
        "AHP authentication was cancelled"
        if error_type is asyncio.CancelledError
        else "AHP authentication failed"
    )
    rpc._send_error_response.assert_awaited_once_with(1, -32603, expected, None)
    assert token not in json.dumps(rpc._send_error_response.call_args.args)
    assert token not in caplog.text
    assert "invalid token:" not in caplog.text
    assert not client._ahp_hosts._authentications
    factory.assert_not_called()
    assert any("host.validateAuthentication" in record.message for record in caplog.records)


@pytest.mark.parametrize("ending", ["notification", "disconnect", "exit", "task"])
async def test_authentication_cancellation_fences_late_result(ending):
    client, _ = client_fixture()
    started, finish, completed = asyncio.Event(), asyncio.Event(), asyncio.Event()
    requests = []

    async def authenticate(request):
        requests.append(request)
        started.set()
        try:
            await finish.wait()
        except asyncio.CancelledError:
            await finish.wait()
        completed.set()
        return AhpAuthenticationResult("owner", future())

    host = await client.start_ahp_host(
        AhpHostOptions(authentication=custom(authenticate), local_server=HostLocalServerOptions())
    )
    params = {"hostId": host.host_id, "requestId": "validation", "token": "secret"}
    task = asyncio.create_task(client._ahp_hosts.validate_authentication(params))
    await asyncio.wait_for(started.wait(), 1)
    with pytest.raises(ValueError, match="already exists"):
        await client._ahp_hosts.validate_authentication(params)
    client._ahp_hosts.notification("host.authenticationCancelled", {**params, "hostId": "wrong"})
    assert not requests[0].cancellation_event.is_set()
    if ending == "notification":
        client._ahp_hosts.notification("host.authenticationCancelled", params)
    elif ending == "disconnect":
        client._ahp_hosts.disconnect()
    elif ending == "exit":
        client._ahp_hosts.notification(
            "host.exited", {"hostId": host.host_id, "reason": "disposed"}
        )
    else:
        task.cancel()
    with pytest.raises(asyncio.CancelledError if ending == "task" else RuntimeError):
        await asyncio.wait_for(task, 1)
    assert requests[0].cancellation_event.is_set()
    assert not client._ahp_hosts._authentications
    finish.set()
    await asyncio.wait_for(completed.wait(), 1)
    await asyncio.gather(*client._ahp_hosts._tasks)


@pytest.mark.parametrize("resume", [False, True])
@pytest.mark.parametrize("mode", ["none", "custom", "github"])
async def test_trusted_authentication_is_sibling_and_never_inference_configuration(resume, mode):
    client, rpc = client_fixture()
    expiry = future()
    requests = []

    async def authenticate(_):
        return AhpAuthenticationResult("owner", expiry)

    async def factory(request):
        requests.append(request)
        assert request.authentication == AhpSessionAuthentication(
            mode, None if mode == "none" else "owner"
        )
        assert "authentication" not in request.config
        assert "github_token" not in request.config
        if resume:
            return await client.resume_session(request.session_id, **request.config)
        return await client.create_session(**request.config)

    host = await client.start_ahp_host(
        AhpHostOptions(
            authentication=custom(authenticate)
            if mode == "custom"
            else AhpHostAuthentication(mode),
            local_server=HostLocalServerOptions(),
            create_session=factory,
            resume_session=factory,
        )
    )
    if mode == "custom":
        assert await client._ahp_hosts.validate_authentication(
            {"hostId": host.host_id, "requestId": "auth", "token": "admission-secret"}
        ) == {"identity": "owner", "expiresAt": expiry}
    params = handoff(host, resume=resume)
    params["authentication"] = {"mode": mode, **({"identity": "owner"} if mode != "none" else {})}
    assert await client._ahp_hosts.materialize(params) == {"sessionId": "session"}
    assert len(requests) == 1
    assert "admission-secret" not in repr(rpc.request.call_args_list)
    assert "authentication" not in rpc.request.call_args.args[1]
    client._ahp_hosts.disconnect()


@pytest.mark.parametrize("context", [{}, {"mode": "none"}, {"mode": "custom", "identity": "other"}])
async def test_invalid_trusted_context_never_invokes_factory(context):
    client, _ = client_fixture()
    factory = AsyncMock()
    host = await client.start_ahp_host(
        AhpHostOptions(
            authentication=custom(AsyncMock()),
            local_server=HostLocalServerOptions(),
            create_session=factory,
        )
    )
    params = {**handoff(host), "authentication": context}
    with pytest.raises(ValueError, match="trusted AHP authentication"):
        await client._ahp_hosts.materialize(params)
    factory.assert_not_called()
