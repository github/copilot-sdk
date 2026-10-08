# Copyright (c) Microsoft Corporation. All rights reserved.

import asyncio
from unittest.mock import AsyncMock, Mock

import pytest

from copilot import AhpHostOptions, CopilotClient, RuntimeConnection
from copilot._jsonrpc import JsonRpcClient
from copilot.generated.rpc import ServerRpc
from copilot.rpc import HostGitHubEnvironmentOptions, HostLocalServerOptions


def client_fixture():
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))

    async def request(method, params, **_):
        if method == "host.start":
            return {"hostId": params["hostId"], "url": "ws://127.0.0.1:12345", "token": "secret"}
        if method == "host.publishSession":
            return {
                "sessionId": params["sessionId"],
                "sessionUri": f"copilot:/{params['sessionId']}",
            }
        if method == "host.listSessions":
            return {"sessions": []}
        if method in ("session.create", "session.resume"):
            return {"sessionId": params["sessionId"]}
        return {}

    rpc = Mock(request=AsyncMock(side_effect=request))
    client._client = rpc
    client._rpc = ServerRpc(rpc)
    client._state = "connected"
    return client, rpc


def handoff(host, *, resume=False, **config):
    return {
        "hostId": host.host_id,
        "handoffId": "participation",
        "resume": resume,
        "config": {"sessionId": "session", "workingDirectory": "/workspace", **config},
    }


def release(client, host):
    client._ahp_hosts.notification(
        "host.sessionReleased", {"hostId": host.host_id, "handoffId": "participation"}
    )


@pytest.mark.parametrize("options", [None, AhpHostOptions()])
async def test_requires_explicit_transport(options):
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    client.start = AsyncMock()
    with pytest.raises(ValueError, match="At least one"):
        await client.start_ahp_host(options)
    client.start.assert_not_called()


@pytest.mark.parametrize("local", [False, True])
async def test_github_hosting_with_optional_local_transport(local):
    client, rpc = client_fixture()

    async def start(method, params):
        assert method == "host.start"
        return {
            "hostId": params["hostId"],
            "environmentId": "environment-1",
            **({"url": "ws://127.0.0.1:12345", "token": "local-token"} if local else {}),
        }

    rpc.request.side_effect = start
    host = await client.start_ahp_host(
        AhpHostOptions(
            compute_id="stable-installation-id",
            github_environment=HostGitHubEnvironmentOptions(
                name="Application", compute_id="stable-installation-id"
            ),
            local_server=HostLocalServerOptions() if local else None,
        )
    )
    assert rpc.request.call_args.args[1] == {
        "hostId": host.host_id,
        "computeId": "stable-installation-id",
        "githubEnvironment": {"name": "Application", "computeId": "stable-installation-id"},
        **({"localServer": {}} if local else {}),
    }
    assert host.environment_id == "environment-1"
    assert host.url == ("ws://127.0.0.1:12345" if local else None)
    assert host.token == ("local-token" if local else None)
    assert host.pid is None


@pytest.mark.parametrize("compute_id", [None, "", "stable-local-compute"])
async def test_local_compute_identity_is_forwarded_without_sdk_defaults(compute_id):
    client, rpc = client_fixture()
    host = await client.start_ahp_host(
        AhpHostOptions(local_server=HostLocalServerOptions(), compute_id=compute_id)
    )
    assert rpc.request.call_args.args[1] == {
        "hostId": host.host_id,
        "localServer": {},
        **({"computeId": compute_id} if compute_id is not None else {}),
    }


async def test_handle_uses_original_transport_and_forwards_every_disposal():
    client, rpc = client_fixture()
    exited = Mock()
    host = await client.start_ahp_host(
        AhpHostOptions(
            local_server=HostLocalServerOptions(port=0, require_connection_token=False),
            on_exit=exited,
        )
    )
    assert host.pid is None
    params = rpc.request.call_args_list[0].args[1]
    assert params == {
        "hostId": host.host_id,
        "localServer": {"port": 0, "requireConnectionToken": False},
    }
    replacement = Mock(request=AsyncMock())
    client._client = replacement
    published = await host.publish_session("resident")
    assert published.session_id == "resident"
    sessions = await host.list_sessions()
    assert sessions.sessions == []
    assert rpc.request.call_args_list[-1].args[:2] == (
        "host.listSessions",
        {"hostId": host.host_id},
    )
    assert await asyncio.gather(host.dispose(), host.dispose()) == [None, None]
    assert await host.dispose() is None
    assert sum(call.args[0] == "host.dispose" for call in rpc.request.call_args_list) == 3
    replacement.request.assert_not_called()
    for _ in range(2):
        client._ahp_hosts.notification(
            "host.exited", {"hostId": host.host_id, "reason": "disposed"}
        )
    client._ahp_hosts.disconnect()
    await asyncio.sleep(0)
    exited.assert_called_once()
    assert exited.call_args.args[0].reason.value == "disposed"


@pytest.mark.parametrize("resume", [False, True])
async def test_factories_preserve_configuration_and_release_exact_original_once(resume):
    client, _ = client_fixture()
    released = Mock()
    originals = []
    cancellations = []

    async def create(request):
        cancellations.append(request.cancellation_event)
        original = await client.create_session(**request.config)
        originals.append(original)
        return original

    async def restore(request):
        cancellations.append(request.cancellation_event)
        original = await client.resume_session(request.session_id, **request.config)
        originals.append(original)
        return original

    host = await client.start_ahp_host(
        AhpHostOptions(
            local_server=HostLocalServerOptions(),
            create_session=create,
            resume_session=restore,
            on_session_released=released,
        )
    )
    config = (
        {"continuePendingWork": False, "suppressResumeEvent": True}
        if resume
        else {"streaming": True}
    )
    result = await client._ahp_hosts.materialize(
        handoff(
            host,
            resume=resume,
            model="test-model",
            configDir="/config",
            mcpOAuthTokenStorage="in-memory",
            gitHubToken="test-auth",
            additionalDirectories=[],
            enableMcpApps=True,
            enableExperimentalMode=False,
            infiniteSessions={"enabled": False, "backgroundCompactionThreshold": 0.7},
            featureFlags={"Arbitrary.MixedCase": True},
            **config,
        )
    )
    assert result == {"sessionId": "session"}
    client._ahp_hosts.notification(
        "host.sessionReleased", {"hostId": "wrong-host", "handoffId": "participation"}
    )
    assert not cancellations[0].is_set()
    release(client, host)
    release(client, host)
    client._ahp_hosts.disconnect()
    await asyncio.sleep(0)
    assert cancellations[0].is_set()
    released.assert_called_once_with(originals[0])
    assert client._get_session("session") is originals[0]


async def test_resume_can_return_a_retained_original_without_reconfiguration():
    client, rpc = client_fixture()
    original = await client.create_session(session_id="session", working_directory="/workspace")
    factory = AsyncMock(return_value=original)
    host = await client.start_ahp_host(
        AhpHostOptions(local_server=HostLocalServerOptions(), resume_session=factory)
    )
    rpc.request.reset_mock()
    assert await client._ahp_hosts.materialize(handoff(host, resume=True)) == {
        "sessionId": "session"
    }
    rpc.request.assert_not_called()


@pytest.mark.parametrize("wrong", ["settings", "identity", "owner"])
async def test_invalid_factory_result_is_rejected_and_released_without_destruction(wrong):
    client, rpc = client_fixture()
    other, _ = client_fixture()
    released = Mock()
    originals = []

    async def create(request):
        config = dict(request.config)
        if wrong == "settings":
            config["working_directory"] = "/wrong"
        if wrong == "identity":
            config["session_id"] = "wrong"
        original = await (other if wrong == "owner" else client).create_session(**config)
        originals.append(original)
        return original

    host = await client.start_ahp_host(
        AhpHostOptions(
            local_server=HostLocalServerOptions(),
            create_session=create,
            on_session_released=released,
        )
    )
    with pytest.raises(ValueError, match="AHP callback must"):
        await client._ahp_hosts.materialize(handoff(host))
    await asyncio.sleep(0)
    released.assert_called_once_with(originals[0])
    assert not any(
        call.args[0] in ("session.destroy", "session.detach") for call in rpc.request.call_args_list
    )


async def test_cancelled_handoff_unblocks_before_late_factory_and_releases_it_once():
    client, _ = client_fixture()
    started, finish, delivered = asyncio.Event(), asyncio.Event(), asyncio.Event()
    requests, originals, released = [], [], []

    async def create(request):
        requests.append(request)
        originals.append(await client.create_session(**request.config))
        started.set()
        await finish.wait()
        return originals[0]

    def on_released(session):
        released.append(session)
        delivered.set()

    host = await client.start_ahp_host(
        AhpHostOptions(
            local_server=HostLocalServerOptions(),
            create_session=create,
            on_session_released=on_released,
        )
    )
    task = asyncio.create_task(client._ahp_hosts.materialize(handoff(host)))
    await asyncio.wait_for(started.wait(), 1)
    with pytest.raises(ValueError, match="handoff already exists"):
        await client._ahp_hosts.materialize(handoff(host))
    release(client, host)
    with pytest.raises(RuntimeError, match="handoff ended"):
        await asyncio.wait_for(task, 1)
    assert requests[0].cancellation_event.is_set()
    assert released == []
    finish.set()
    await asyncio.wait_for(delivered.wait(), 1)
    client._ahp_hosts.disconnect()
    assert released == originals
    assert client._get_session("session") is originals[0]


@pytest.mark.parametrize("resume", [False, True])
async def test_cancelled_factory_returns_rpc_error_and_releases_handoff(resume):
    client, _ = client_fixture()
    requests = []

    async def factory(request):
        requests.append(request)
        work = asyncio.create_task(asyncio.sleep(0))
        work.cancel()
        await work

    host = await client.start_ahp_host(
        AhpHostOptions(
            local_server=HostLocalServerOptions(),
            create_session=factory,
            resume_session=factory,
        )
    )
    rpc = JsonRpcClient(Mock())
    rpc._send_message = AsyncMock()
    await asyncio.wait_for(
        rpc._dispatch_request(
            {
                "jsonrpc": "2.0",
                "id": "materialize",
                "method": "host.materializeSession",
                "params": handoff(host, resume=resume),
            },
            client._ahp_hosts.materialize,
            cancellation_id=None,
        ),
        1,
    )
    rpc._send_message.assert_awaited_once()
    response = rpc._send_message.call_args.args[0]
    assert response["id"] == "materialize"
    assert response["error"]["code"] == -32603
    assert response["error"]["message"] == "AHP session factory was cancelled"
    assert requests[0].cancellation_event.is_set()
    assert not client._ahp_hosts._handoffs


async def test_start_failure_removes_factory_and_exit_callback():
    client, rpc = client_fixture()
    exited = Mock()
    rpc.request.side_effect = RuntimeError("bind failed")
    with pytest.raises(RuntimeError, match="bind failed"):
        await client.start_ahp_host(
            AhpHostOptions(
                local_server=HostLocalServerOptions(), create_session=AsyncMock(), on_exit=exited
            )
        )
    client._ahp_hosts.disconnect()
    await asyncio.sleep(0)
    exited.assert_not_called()


@pytest.mark.parametrize("startup_fails", [False, True])
@pytest.mark.parametrize("cancel_mode", ["cancel", "timeout"])
async def test_cancelled_start_settles_before_cleanup_on_original_connection(
    startup_fails, cancel_mode
):
    client, rpc = client_fixture()
    accepted, finish = asyncio.Event(), asyncio.Event()
    disposed, finish_dispose = asyncio.Event(), asyncio.Event()
    host_ids = []

    async def request(method, params, **_):
        if method == "host.start":
            host_ids.append(params["hostId"])
            accepted.set()
            await finish.wait()
            if startup_fails:
                raise RuntimeError("bind failed")
            return {"hostId": params["hostId"], "url": "ws://127.0.0.1:12345"}
        assert method == "host.dispose"
        assert params == {"hostId": host_ids[0]}
        disposed.set()
        await finish_dispose.wait()
        return {}

    rpc.request.side_effect = request
    task = asyncio.create_task(
        client.start_ahp_host(
            AhpHostOptions(local_server=HostLocalServerOptions(), create_session=AsyncMock())
        )
    )
    await asyncio.wait_for(accepted.wait(), 1)
    if cancel_mode == "cancel":
        task.cancel()
        with pytest.raises(asyncio.CancelledError):
            await asyncio.wait_for(task, 1)
    else:
        with pytest.raises(TimeoutError):
            await asyncio.wait_for(task, 0)
    assert host_ids[0] in client._ahp_hosts._hosts
    replacement = Mock(request=AsyncMock())
    client._client = replacement
    finish.set()
    if not startup_fails:
        await asyncio.wait_for(disposed.wait(), 1)
        assert host_ids[0] in client._ahp_hosts._hosts
        finish_dispose.set()
    await asyncio.wait_for(asyncio.gather(*client._ahp_hosts._tasks), 1)
    assert not client._ahp_hosts._hosts
    assert disposed.is_set() is not startup_fails
    replacement.request.assert_not_called()


@pytest.mark.parametrize("resume", [False, True])
async def test_failed_factory_does_not_leave_capture_on_retained_original(resume):
    client, rpc = client_fixture()
    originals = []

    async def create(request):
        originals.append(await client.create_session(**request.config))
        raise RuntimeError("application setup failed")

    async def restore(request):
        if originals:
            return originals[0]
        originals.append(await client.resume_session(request.session_id, **request.config))
        raise RuntimeError("application setup failed")

    host = await client.start_ahp_host(
        AhpHostOptions(
            local_server=HostLocalServerOptions(), create_session=create, resume_session=restore
        )
    )
    with pytest.raises(RuntimeError, match="application setup failed"):
        await client._ahp_hosts.materialize(handoff(host, resume=resume))
    rpc.request.reset_mock()
    assert await client._ahp_hosts.materialize(
        handoff(host, resume=True, continuePendingWork=False, suppressResumeEvent=True)
    ) == {"sessionId": "session"}
    rpc.request.assert_not_called()
    assert client._get_session("session") is originals[0]


async def test_factory_error_propagates_and_release_callback_errors_are_logged(caplog):
    client, _ = client_fixture()
    host = await client.start_ahp_host(
        AhpHostOptions(
            local_server=HostLocalServerOptions(),
            create_session=AsyncMock(side_effect=ValueError("factory failed")),
        )
    )
    with pytest.raises(ValueError, match="factory failed"):
        await client._ahp_hosts.materialize(handoff(host))

    def fail_release(_):
        raise ValueError("release failed")

    async def create(request):
        return await client.create_session(**request.config)

    host = await client.start_ahp_host(
        AhpHostOptions(
            local_server=HostLocalServerOptions(),
            create_session=create,
            on_session_released=fail_release,
        )
    )
    await client._ahp_hosts.materialize(handoff(host))
    release(client, host)
    await asyncio.sleep(0)
    assert "AHP session release callback failed" in caplog.text
