# Copyright (c) Microsoft Corporation. All rights reserved.

"""Python callback/configuration boundary against the built in-process AHP host."""

import asyncio
import json
import logging
import os
import shutil
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import uuid4

import pytest
import pytest_asyncio

from copilot import (
    AhpAuthenticationResult,
    AhpHostAuthentication,
    AhpHostOptions,
    AhpSessionAuthentication,
    CopilotClient,
    RuntimeConnection,
)
from copilot.rpc import HostLocalServerOptions
from copilot.session import PermissionHandler

from .testharness.ahp import AhpTestClient
from .testharness.context import get_cli_path_for_tests

pytestmark = [
    pytest.mark.asyncio(loop_scope="module"),
    pytest.mark.skipif(
        os.environ.get("COPILOT_RUNTIME_HOST_E2E") != "1",
        reason="Requires the built host; set COPILOT_RUNTIME_HOST_E2E=1",
    ),
]


@pytest_asyncio.fixture(autouse=True, loop_scope="module")
async def configure_test():
    # These boundary tests use a local BYOK endpoint, not replay snapshots.
    yield


def future(seconds=60):
    return (datetime.now(UTC) + timedelta(seconds=seconds)).isoformat()


@pytest_asyncio.fixture(loop_scope="module")
async def boundary():
    directory = Path.cwd() / f".host-authentication-{uuid4()}"
    directory.mkdir()
    home, work = directory / "home", directory / "work"
    home.mkdir()
    work.mkdir()
    clients = []

    def client():
        instance = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=get_cli_path_for_tests()),
            working_directory=str(work),
            use_logged_in_user=False,
            env={
                **os.environ,
                "COPILOT_HOME": str(home),
                "HOME": str(home),
                "USERPROFILE": str(home),
                "GH_TOKEN": "",
                "GITHUB_TOKEN": "",
                "COPILOT_GITHUB_TOKEN": "",
                "COPILOT_HMAC_KEY": "",
                "CAPI_HMAC_KEY": "",
            },
        )
        clients.append(instance)
        return instance

    try:
        async with AhpTestClient(
            Path(__file__).parents[2] / "test/harness/hostAuthenticationDriver.mjs"
        ) as ahp:
            yield client, work, ahp
    finally:
        try:
            await asyncio.gather(*(instance.stop() for instance in clients))
        finally:
            shutil.rmtree(directory)


@pytest.mark.parametrize("mode", ["none", "custom"])
async def test_byok_create_and_cold_resume_trust_admission_identity(boundary, mode):
    make_client, work, ahp = boundary
    owner = make_client()
    bodies = []
    authorizations = []

    async def completion(reader, writer):
        try:
            headers = (await reader.readuntil(b"\r\n\r\n")).decode()
            length = next(
                int(line.split(":", 1)[1])
                for line in headers.splitlines()
                if line.lower().startswith("content-length:")
            )
            bodies.append((await reader.readexactly(length)).decode())
            authorizations.append(
                next(
                    line.split(":", 1)[1].strip()
                    for line in headers.splitlines()
                    if line.lower().startswith("authorization:")
                )
            )
            chunks = [
                {
                    "delta": {"role": "assistant", "content": "PYTHON_HOST_BYOK_OK"},
                    "finish_reason": None,
                },
                {"delta": {}, "finish_reason": "stop"},
            ]
            payload = (
                "".join(
                    "data: "
                    + json.dumps(
                        {
                            "id": "local",
                            "object": "chat.completion.chunk",
                            "created": 1,
                            "model": "local-model",
                            "choices": [{"index": 0, **chunk}],
                        }
                    )
                    + "\n\n"
                    for chunk in chunks
                )
                + "data: [DONE]\n\n"
            )
            data = payload.encode()
            writer.write(
                (
                    "HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\n"
                    f"Content-Length: {len(data)}\r\nConnection: close\r\n\r\n"
                ).encode()
                + data
            )
            await writer.drain()
        finally:
            writer.close()
            await writer.wait_closed()

    endpoint = await asyncio.start_server(completion, "127.0.0.1", 0)
    provider = {
        "type": "openai",
        "wire_api": "completions",
        "base_url": f"http://127.0.0.1:{endpoint.sockets[0].getsockname()[1]}/v1",
        "api_key": "inference-only-key",
    }
    requests, originals, validations = [], [], []

    async def authenticate(request):
        validations.append(request.token)
        return (
            AhpAuthenticationResult("owner", future())
            if request.token == "admission-only-key"
            else None
        )

    policy = (
        AhpHostAuthentication("custom", "owner", authenticate)
        if mode == "custom"
        else AhpHostAuthentication("none")
    )

    async def factory(request):
        requests.append(request)
        assert request.authentication == AhpSessionAuthentication(
            mode, "owner" if mode == "custom" else None
        )
        assert "authentication" not in request.config
        assert "github_token" not in request.config
        config = {
            **request.config,
            "provider": provider,
            "model": "local-model",
            "extension_sdk_path": str(Path(__file__).parents[2] / "nodejs/dist"),
            "on_permission_request": PermissionHandler.approve_all,
        }
        session = (
            await owner.resume_session(request.session_id, **config)
            if hasattr(request, "session_id")
            else await owner.create_session(**config)
        )
        originals.append(session)
        return session

    uri = ""
    try:
        for resume in (False, True):
            if resume:
                owner = make_client()
            host = await owner.start_ahp_host(
                AhpHostOptions(
                    authentication=policy,
                    local_server=HostLocalServerOptions(),
                    allow_creating_new_sessions=not resume,
                    create_session=factory,
                    resume_session=factory,
                )
            )
            try:
                await ahp.request({"op": "connect", "url": host.url, "token": host.token})
                if mode == "custom":
                    with pytest.raises(RuntimeError):
                        await ahp.request({"op": "create", "workDir": str(work)})
                resources = await ahp.request({"op": "authenticate", "token": "admission-only-key"})
                if mode == "none":
                    assert resources == {"resources": []}
                if resume:
                    await ahp.request({"op": "attach", "uri": uri})
                else:
                    uri = (await ahp.request({"op": "create", "workDir": str(work)}))["uri"]
                assert (await ahp.request({"op": "turn"}))["text"] == "PYTHON_HOST_BYOK_OK"
                assert len(requests) == (2 if resume else 1)
            finally:
                await host.dispose()
                await ahp.request({"op": "close"})
                if originals:
                    await originals[-1].disconnect()
                await owner.stop()
        assert len(bodies) >= 2
        assert all("admission-only-key" not in body for body in bodies)
        assert authorizations and all(
            value == "Bearer inference-only-key" for value in authorizations
        )
        assert validations == (["admission-only-key"] * 2 if mode == "custom" else [])
    finally:
        endpoint.close()
        await endpoint.wait_closed()


@pytest.mark.parametrize("failure", ["null", "throw", "identity", "expiry", "cancel"])
async def test_custom_rejection_and_cancellation_do_not_materialize(boundary, failure):
    make_client, work, ahp = boundary
    client = make_client()
    started = asyncio.Event()
    requests = []
    factories = []

    async def authenticate(request):
        requests.append(request)
        started.set()
        if failure == "cancel":
            await request.cancellation_event.wait()
            return AhpAuthenticationResult("owner", future())
        if failure == "throw":
            raise RuntimeError("Unavailable")
        if failure == "null":
            return None
        return AhpAuthenticationResult(
            "wrong" if failure == "identity" else "owner",
            "2000-01-01T00:00:00Z" if failure == "expiry" else future(),
        )

    async def create(request):
        factories.append(request)
        raise AssertionError("Rejected credentials cannot materialize sessions")

    host = await client.start_ahp_host(
        AhpHostOptions(
            authentication=AhpHostAuthentication("custom", "owner", authenticate),
            local_server=HostLocalServerOptions(),
            create_session=create,
        )
    )
    try:
        await ahp.request({"op": "connect", "url": host.url, "token": host.token})
        task = asyncio.create_task(ahp.request({"op": "authenticate", "token": "decrypted-token"}))
        await asyncio.wait_for(started.wait(), 10)
        if failure == "cancel":
            await host.dispose()
        with pytest.raises(RuntimeError):
            await asyncio.wait_for(task, 20)
        assert requests[0].token == "decrypted-token"
        assert requests[0].cancellation_event.is_set()
        if failure != "cancel":
            with pytest.raises(RuntimeError):
                await ahp.request({"op": "create", "workDir": str(work)})
        assert factories == []
    finally:
        await host.dispose()
        await ahp.request({"op": "close"})


async def test_expired_custom_identity_requires_reauthentication(boundary):
    make_client, work, ahp = boundary
    client = make_client()
    validations = []
    issued = None
    attempts = 1
    before = 0
    expired_admission_errors = {
        "RPC error -32009: permission denied: application authentication was rejected",
        "RPC error -32603: internal error: application authentication failed",
        "RPC error -32007: authentication required for https://copilot.github.com/hosting/authentication",
    }

    async def authenticate(request):
        nonlocal issued
        assert request.token in ("short-lived", "renewed")
        expiry = future(60 if request.token == "renewed" else 2)
        result = AhpAuthenticationResult("owner", expiry)
        validations.append(request.token)
        issued = (request.token, expiry, result)
        return result

    def check_credential(token):
        assert len(validations) == before + 1
        assert issued is not None
        assert issued[0] == token
        assert issued[2].identity == "owner"
        assert issued[2].expires_at == issued[1]
        return datetime.fromisoformat(issued[1])

    def retry_expired(error):
        nonlocal issued, before, attempts
        if error not in expired_admission_errors:
            return False
        expiry = check_credential("short-lived")
        if expiry > datetime.now(UTC):
            return False
        logging.getLogger(__name__).warning(
            "Retrying initial authentication with an already-expired fixture credential: %s",
            error,
        )
        before = len(validations)
        issued = None
        attempts += 1
        return True

    host = await client.start_ahp_host(
        AhpHostOptions(
            authentication=AhpHostAuthentication("custom", "owner", authenticate),
            local_server=HostLocalServerOptions(),
        )
    )
    try:
        await ahp.request({"op": "connect", "url": host.url, "token": host.token})
        await ahp.request(
            {"op": "authenticate", "token": "short-lived"},
            retry_expired_error=retry_expired,
        )
        check_credential("short-lived")
        await ahp.request({"op": "expired"})
        with pytest.raises(RuntimeError):
            await ahp.request({"op": "create", "workDir": str(work)})
        issued = None
        before = len(validations)
        await ahp.request({"op": "authenticate", "token": "renewed"})
        check_credential("renewed")
        assert validations == ["short-lived"] * attempts + ["renewed"]
    finally:
        await host.dispose()
        await ahp.request({"op": "close"})
