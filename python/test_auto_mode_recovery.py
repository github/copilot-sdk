# Copyright (c) Microsoft Corporation. All rights reserved.

import asyncio
import json
from contextlib import asynccontextmanager
from unittest.mock import AsyncMock, patch
from uuid import uuid4

import pytest

from copilot import CopilotClient, RuntimeConnection
from copilot.client import (
    CloudSessionOptions,
    CloudSessionRepository,
    _HookInvokeRequest,
    _HooksAdapter,
    _session_ids_overlap,
)
from copilot.session import PermissionHandler, SessionCapabilities
from copilot.session_events import CapabilitiesChangedUI, session_event_from_dict

AUTO_MODE_RECOVERY = {
    "requestId": "offer-1",
    "originalModel": "explicit-model",
    "allowAlways": False,
    "interactionId": "interaction-1",
    "originatingMessageId": "user-1",
}


def test_ui_capability_positional_arguments_keep_their_original_meaning():
    capabilities = CapabilitiesChangedUI(True, False, True)
    assert capabilities.to_dict() == {"canvases": True, "elicitation": False, "mcpApps": True}
    assert capabilities.auto_mode_recovery is None
    recovered = CapabilitiesChangedUI.from_dict({"autoModeRecovery": True, "canvases": False})
    assert recovered.auto_mode_recovery is True
    assert recovered.canvases is False


class Runtime:
    def __init__(self, capabilities: SessionCapabilities | None = None):
        self.capabilities = capabilities
        self.requests = []

    async def request(self, method, params, **kwargs):
        self.requests.append((method, params))
        if method in ("session.create", "session.resume"):
            response = {"sessionId": params["sessionId"], "capabilities": self.capabilities}
        elif method in ("session.detach", "session.delete", "session.options.update"):
            response = {"success": True}
        else:
            raise AssertionError(f"Unexpected RPC: {method}")
        if callback := kwargs.get("on_response_inline"):
            callback(response)
        return response


def client_for(runtime):
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1"))
    client._client = runtime
    return client


async def open_session(client, resume, **kwargs):
    kwargs["on_permission_request"] = PermissionHandler.approve_all
    if resume:
        return await client.resume_session("session-1", **kwargs)
    return await client.create_session(**kwargs)


@pytest.mark.asyncio
@pytest.mark.parametrize(
    "failure_method", ["session.eventLog.registerInterest", "session.options.update"]
)
@pytest.mark.parametrize("cloud", [False, True])
@pytest.mark.parametrize("cleanup_failure", [False, True])
async def test_post_create_failure_rolls_back_only_server_assigned_sessions(
    failure_method, cloud, cleanup_failure
):
    class SetupRuntime(Runtime):
        async def request(self, method, params, **kwargs):
            if method == "session.create":
                response = {
                    "sessionId": params.get("sessionId", "cloud-setup-failed"),
                    "capabilities": {"ui": {"autoModeRecovery": True}},
                }
                self.requests.append((method, params))
                kwargs["on_response_inline"](response)
                return response
            if method == failure_method:
                raise RuntimeError("post-create setup failed")
            if method == "session.eventLog.registerInterest":
                return {}
            if method == "session.delete":
                self.requests.append((method, params))
                return {"success": not cleanup_failure, "error": "cloud rollback rejected"}
            return await super().request(method, params, **kwargs)

    runtime = SetupRuntime()
    client = client_for(runtime)
    with pytest.raises(RuntimeError, match="post-create setup failed") as failure:
        await client.create_session(
            cloud=CloudSessionOptions() if cloud else None,
            session_id=None if cloud else "caller-owned",
            on_permission_request=PermissionHandler.approve_all,
            auto_mode_recovery=True,
            on_auto_mode_switch_request=lambda _request, _invocation: "no",
            on_mcp_auth_request=lambda _request, _invocation: None,
            skip_custom_instructions=True,
        )
    assert client._sessions == {}
    deletions = [params for method, params in runtime.requests if method == "session.delete"]
    assert deletions == ([{"sessionId": "cloud-setup-failed"}] if cloud else [])
    if cloud and cleanup_failure:
        assert "cloud rollback rejected" in str(failure.value.__cause__)
        with pytest.raises(RuntimeError, match="cloud rollback rejected"):
            await client.resume_session("cloud-setup-failed")


@pytest.mark.asyncio
@pytest.mark.parametrize(
    "setup_method", ["session.eventLog.registerInterest", "session.options.update"]
)
@pytest.mark.parametrize("cleanup_failure", [False, True])
async def test_cancelled_cloud_setup_returns_before_owned_rollback(setup_method, cleanup_failure):
    entered, detach_entered, release_detach = asyncio.Event(), asyncio.Event(), asyncio.Event()

    class CloudRuntime(Runtime):
        async def request(self, method, params, **kwargs):
            if method == "session.create":
                response = {
                    "sessionId": "cancelled-cloud",
                    "capabilities": {"ui": {"autoModeRecovery": True}},
                }
                kwargs["on_response_inline"](response)
                return response
            if method == setup_method:
                entered.set()
                await asyncio.Future()
            if method == "session.eventLog.registerInterest":
                return {}
            if method == "session.detach":
                detach_entered.set()
                await release_detach.wait()
            if method == "session.delete":
                self.requests.append((method, params))
                return {"success": not cleanup_failure, "error": "cloud rollback rejected"}
            return await super().request(method, params, **kwargs)

    runtime = CloudRuntime()
    client = client_for(runtime)
    created = asyncio.create_task(
        client.create_session(
            cloud=CloudSessionOptions(),
            on_permission_request=PermissionHandler.approve_all,
            auto_mode_recovery=True,
            on_auto_mode_switch_request=lambda _request, _invocation: "no",
            on_mcp_auth_request=lambda _request, _invocation: None,
            skip_custom_instructions=True,
        )
    )
    resumed = None
    try:
        await asyncio.wait_for(entered.wait(), 5)
        owner = client._sessions["cancelled-cloud"]
        created.cancel()
        await asyncio.wait_for(detach_entered.wait(), 5)
        with pytest.raises(asyncio.CancelledError):
            await asyncio.wait_for(asyncio.shield(created), 5)
        assert client._sessions["cancelled-cloud"] is owner
        resumed = asyncio.create_task(
            client.resume_session(
                "cancelled-cloud", on_permission_request=PermissionHandler.approve_all
            )
        )
        async with asyncio.timeout(5):
            while True:
                lock = client._session_initializations.get("cancelled-cloud")
                if lock is not None and lock.users:
                    break
                await asyncio.sleep(0)
        assert not resumed.done()
        assert not any(method == "session.resume" for method, _ in runtime.requests)
        release_detach.set()
        if cleanup_failure:
            with pytest.raises(RuntimeError, match="cloud rollback rejected"):
                await asyncio.wait_for(resumed, 5)
        else:
            replacement = await asyncio.wait_for(resumed, 5)
            assert client._sessions["cancelled-cloud"] is replacement
            await replacement.disconnect()
        assert [params for method, params in runtime.requests if method == "session.delete"] == [
            {"sessionId": "cancelled-cloud"}
        ]
    finally:
        release_detach.set()
        if not created.done():
            created.cancel()
        await asyncio.gather(created, return_exceptions=True)
        if resumed is not None:
            await asyncio.gather(resumed, return_exceptions=True)
        await asyncio.gather(*client._auto_mode_recovery_cleanup_tasks, return_exceptions=True)


@pytest.mark.asyncio
@pytest.mark.parametrize("threaded", [False, True])
@pytest.mark.parametrize("cancel_waiter", [False, True])
async def test_cloud_setup_fences_server_assigned_id(threaded, cancel_waiter):
    entered, release = asyncio.Event(), asyncio.Event()

    class CloudRuntime(Runtime):
        async def request(self, method, params, **kwargs):
            if method == "session.create":
                response = {
                    "sessionId": "cloud-session",
                    "capabilities": {"ui": {"autoModeRecovery": True}},
                }
                if threaded:
                    await asyncio.to_thread(kwargs["on_response_inline"], response)
                else:
                    kwargs["on_response_inline"](response)
                return response
            if method == "session.eventLog.registerInterest":
                entered.set()
                await release.wait()
                raise RuntimeError("MCP registration failed")
            return await super().request(method, params, **kwargs)

    runtime = CloudRuntime()
    client = client_for(runtime)
    created = asyncio.create_task(
        client.create_session(
            cloud=CloudSessionOptions(),
            on_permission_request=PermissionHandler.approve_all,
            auto_mode_recovery=True,
            on_auto_mode_switch_request=lambda _request, _invocation: "no",
            on_mcp_auth_request=lambda _request, _invocation: None,
        )
    )
    resumed = None
    try:
        await asyncio.wait_for(entered.wait(), 5)
        owner = client._sessions["cloud-session"]
        resumed = asyncio.create_task(
            client.resume_session(
                "cloud-session",
                on_permission_request=PermissionHandler.approve_all,
                auto_mode_recovery=True,
                on_auto_mode_switch_request=lambda _request, _invocation: "no",
            )
        )
        async with asyncio.timeout(5):
            while True:
                if any(method == "session.resume" for method, _params in runtime.requests):
                    pytest.fail("resume overtook server-assigned cloud initialization")
                lock = client._session_initializations.get("cloud-session")
                if lock is not None and lock.users >= 2:
                    break
                await asyncio.sleep(0)
        assert client._sessions["cloud-session"] is owner
        if cancel_waiter:
            resumed.cancel()
            with pytest.raises(asyncio.CancelledError):
                await resumed
            assert lock.inline_reservation is not None
            assert not lock.inline_reservation.cancelled()
            resumed = asyncio.create_task(
                client.resume_session(
                    "cloud-session",
                    on_permission_request=PermissionHandler.approve_all,
                    auto_mode_recovery=True,
                    on_auto_mode_switch_request=lambda _request, _invocation: "no",
                )
            )
        release.set()
        with pytest.raises(RuntimeError, match="MCP registration failed"):
            await created
        with pytest.raises(RuntimeError, match="does not support"):
            await resumed
        assert sum(method == "session.detach" for method, _params in runtime.requests) == 2
        assert [params for method, params in runtime.requests if method == "session.delete"] == [
            {"sessionId": "cloud-session"}
        ]
    finally:
        release.set()
        await asyncio.gather(created, return_exceptions=True)
        if resumed is not None:
            await asyncio.gather(resumed, return_exceptions=True)


@pytest.mark.asyncio
@pytest.mark.parametrize("recovery", [False, True])
async def test_prefix_lookup_uses_configured_storage(recovery):
    canonical = "abcdef12-abcd-4123-8123-123456789abc"

    class ScopedRuntime(Runtime):
        async def request(self, method, params, **kwargs):
            if method == "sessions.findByPrefix":
                self.requests.append((method, params))
                assert params == {"prefix": "abcdef12", "configDir": "configured-home"}
                return {"sessionId": canonical}
            return await super().request(method, params, **kwargs)

    runtime = ScopedRuntime({"ui": {"autoModeRecovery": True}})
    client = client_for(runtime)
    session = await client.resume_session(
        "abcdef12",
        config_directory="configured-home",
        on_permission_request=PermissionHandler.approve_all,
        auto_mode_recovery=recovery,
        on_auto_mode_switch_request=lambda _request, _invocation: "no",
    )
    try:
        assert session.session_id == (canonical if recovery else "abcdef12")
        resume = next(params for method, params in runtime.requests if method == "session.resume")
        assert resume["configDir"] == "configured-home"
        assert resume["sessionId"] == (canonical if recovery else "abcdef12")
        assert sum(method == "sessions.findByPrefix" for method, _ in runtime.requests) == int(
            recovery
        )
    finally:
        await session.disconnect()


@pytest.mark.asyncio
@pytest.mark.parametrize("recovery", [False, True])
async def test_exact_hex_provider_resume_skips_host_lookup(recovery):
    requests = []

    async def on_request(_callback, message):
        requests.append((message["method"], message["params"]))
        if message["method"] == "session.resume":
            assert message["params"]["sessionId"] == "abcdef0"
            return {
                "result": {
                    "sessionId": "abcdef0",
                    "capabilities": {"ui": {"autoModeRecovery": True}},
                }
            }

    async with session_fs_peer(on_request=on_request) as (client, _callback):
        session = await client.resume_session(
            "abcdef0",
            config_directory="configured-home",
            auto_mode_recovery=recovery,
            on_auto_mode_switch_request=lambda _request, _invocation: "no",
            create_session_fs_handler=lambda _session: object(),
        )
        assert session.session_id == "abcdef0"
        assert not any(method == "sessions.findByPrefix" for method, _ in requests)
        await session.disconnect()


@pytest.mark.asyncio
@pytest.mark.parametrize("resume", [False, True])
async def test_requires_handler_before_starting(resume):
    client = client_for(None)
    with patch.object(client, "start", new_callable=AsyncMock) as start:
        with pytest.raises(ValueError, match="requires on_auto_mode_switch_request"):
            await open_session(
                client, resume, auto_mode_recovery=True, on_event=lambda _event: None
            )
        start.assert_not_awaited()


@pytest.mark.asyncio
@pytest.mark.parametrize("resume", [False, True])
async def test_legacy_handler_does_not_enroll(resume):
    runtime = Runtime()
    client = client_for(runtime)
    received = []

    def handler(request, _invocation):
        received.append(request)
        return "yes_always"

    session = await open_session(client, resume, on_auto_mode_switch_request=handler)
    try:
        assert runtime.requests[0][1]["requestAutoModeSwitch"] is True
        assert "requestAutoModeRecovery" not in runtime.requests[0][1]
        result = await client._handle_auto_mode_switch_request(
            {"sessionId": session.session_id, "errorCode": "user_weekly_rate_limited"}
        )
        assert result == {"response": "yes_always"}
        assert received == [{"errorCode": "user_weekly_rate_limited"}]
    finally:
        await session.disconnect()


@pytest.mark.asyncio
@pytest.mark.parametrize("resume", [False, True])
@pytest.mark.parametrize("caps", [None, {}, {"ui": {}}, {"ui": {"autoModeRecovery": False}}])
async def test_unsupported_enrollment_detaches_and_unregisters(resume, caps):
    runtime = Runtime(caps)
    client = client_for(runtime)
    with pytest.raises(RuntimeError, match="does not support auto_mode_recovery"):
        await open_session(
            client,
            resume,
            auto_mode_recovery=True,
            on_auto_mode_switch_request=lambda _request, _invocation: "no",
        )
    assert [method for method, _ in runtime.requests] == [
        "session.resume" if resume else "session.create",
        "session.detach",
    ]
    assert not client._sessions


@pytest.mark.asyncio
@pytest.mark.parametrize("cleanup_failure", [False, True])
async def test_rejected_cloud_enrollment_deletes_server_assigned_session(cleanup_failure, caplog):
    class CloudRuntime(Runtime):
        async def request(self, method, params, **kwargs):
            self.requests.append((method, params))
            if method == "session.create":
                assert "sessionId" not in params
                response = {"sessionId": "cloud-recovery-rejected"}
                kwargs["on_response_inline"](response)
                return response
            if method == "session.detach":
                return {"success": True}
            if method == "session.delete":
                return {"success": not cleanup_failure, "error": "cloud rollback rejected"}
            raise AssertionError(f"Unexpected RPC: {method}")

    runtime = CloudRuntime()
    client = client_for(runtime)
    with pytest.raises(RuntimeError, match="does not support auto_mode_recovery"):
        await client.create_session(
            cloud=CloudSessionOptions(
                repository=CloudSessionRepository(owner="github", name="copilot-sdk", branch="main")
            ),
            on_permission_request=PermissionHandler.approve_all,
            auto_mode_recovery=True,
            on_auto_mode_switch_request=lambda _request, _invocation: "no",
        )
    assert runtime.requests[-2:] == [
        ("session.detach", {"sessionId": "cloud-recovery-rejected"}),
        ("session.delete", {"sessionId": "cloud-recovery-rejected"}),
    ]
    assert not client._sessions
    if cleanup_failure:
        assert "cloud-recovery-rejected" in caplog.text
        assert "cloud rollback rejected" in caplog.text


@pytest.mark.asyncio
async def test_cancelled_cloud_enrollment_keeps_reuse_behind_deletion():
    delete_started = asyncio.Event()
    release_delete = asyncio.Event()
    delete_finished = asyncio.Event()
    retry_started = asyncio.Event()

    class CloudRuntime(Runtime):
        async def request(self, method, params, **kwargs):
            if method == "session.create" and "sessionId" not in params:
                self.requests.append((method, params))
                response = {"sessionId": "cloud-recovery-rejected"}
                kwargs["on_response_inline"](response)
                return response
            if method == "session.delete":
                delete_started.set()
                await release_delete.wait()
                self.requests.append((method, params))
                delete_finished.set()
                return {"success": True}
            if method == "session.resume":
                assert delete_finished.is_set(), "same-ID resume overtook cloud rollback"
            return await super().request(method, params, **kwargs)

    runtime = CloudRuntime()
    client = client_for(runtime)
    creation = asyncio.create_task(
        client.create_session(
            cloud=CloudSessionOptions(
                repository=CloudSessionRepository(owner="github", name="copilot-sdk", branch="main")
            ),
            auto_mode_recovery=True,
            on_auto_mode_switch_request=lambda _request, _invocation: "no",
        )
    )
    retry = None
    unrelated = None
    replacement = None

    async def reuse():
        retry_started.set()
        return await client.resume_session(
            "cloud-recovery-rejected", on_permission_request=PermissionHandler.approve_all
        )

    try:
        await asyncio.wait_for(delete_started.wait(), 5)
        creation.cancel()
        with pytest.raises(asyncio.CancelledError):
            await creation
        retry = asyncio.create_task(reuse())
        await asyncio.wait_for(retry_started.wait(), 5)
        unrelated = await client.create_session(session_id="unrelated")
        release_delete.set()
        replacement = await asyncio.wait_for(retry, 5)
        assert client._sessions[replacement.session_id] is replacement
        assert not replacement._destroyed
        assert [method for method, _ in runtime.requests].count("session.delete") == 1
    finally:
        release_delete.set()
        await asyncio.gather(creation, return_exceptions=True)
        if retry is not None:
            replacement = await asyncio.wait_for(retry, 5)
        for session in (replacement, unrelated):
            if session is not None:
                await session.disconnect()


@pytest.mark.asyncio
async def test_rejected_resume_keeps_the_resumed_callback_owner():
    runtime = Runtime()
    client = client_for(runtime)
    resident = await open_session(
        client,
        False,
        on_auto_mode_switch_request=lambda _request, _invocation: "yes_always",
    )
    try:
        with pytest.raises(RuntimeError, match="does not support auto_mode_recovery"):
            await client.resume_session(
                resident.session_id,
                on_permission_request=PermissionHandler.approve_all,
                auto_mode_recovery=True,
                on_auto_mode_switch_request=lambda _request, _invocation: "no",
            )
        assert all(method != "session.detach" for method, _ in runtime.requests)
        assert client._sessions[resident.session_id] is not resident
        assert not resident._destroyed
        assert await client._handle_auto_mode_switch_request(
            {"sessionId": resident.session_id, "errorCode": "user_weekly_rate_limited"}
        ) == {"response": "no"}
    finally:
        await resident.disconnect()


@pytest.mark.asyncio
@pytest.mark.parametrize("resume", [False, True])
@pytest.mark.parametrize("cleanup_failure", [False, True])
async def test_cancelled_enrollment_keeps_callbacks_until_cleanup_settles(resume, cleanup_failure):
    detach_started = asyncio.Event()
    release_detach = asyncio.Event()
    callback_invocations = []

    class DetachingRuntime(Runtime):
        async def request(self, method, params, **kwargs):
            if method == "session.detach":
                detach_started.set()
                await release_detach.wait()
                if cleanup_failure:
                    raise RuntimeError("detach rejected")
            return await super().request(method, params, **kwargs)

    client = client_for(DetachingRuntime())

    def handler(request, invocation):
        callback_invocations.append(invocation["session_id"])
        return "yes_always"

    def end_hook(_input, invocation):
        return {"sessionSummary": f"cleanup for {invocation['session_id']}"}

    initialization = asyncio.create_task(
        open_session(
            client,
            resume,
            auto_mode_recovery=True,
            on_auto_mode_switch_request=handler,
            hooks={"on_session_end": end_hook},
        )
    )
    try:
        await asyncio.wait_for(detach_started.wait(), 5)
        session_id = next(iter(client._sessions))
        cleanup_tasks = tuple(client._auto_mode_recovery_cleanup_tasks)
        initialization.cancel()
        with pytest.raises(asyncio.CancelledError):
            await initialization
        assert await client._handle_auto_mode_switch_request({"sessionId": session_id}) == {
            "response": "yes_always"
        }
        assert callback_invocations == [session_id]
        hook_request = _HookInvokeRequest.from_dict(
            {"sessionId": session_id, "hookType": "sessionEnd", "input": {}}
        )
        hooks = _HooksAdapter(client._get_session)
        assert (await hooks.invoke(hook_request)).output == {
            "sessionSummary": f"cleanup for {session_id}"
        }
        release_detach.set()
        await asyncio.gather(*cleanup_tasks, return_exceptions=True)
        with pytest.raises(ValueError, match="unknown session"):
            await client._handle_auto_mode_switch_request({"sessionId": session_id})
        with pytest.raises(ValueError, match="unknown session"):
            await hooks.invoke(hook_request)
        if cleanup_failure:
            with pytest.raises(RuntimeError, match="detach rejected"):
                await client.resume_session(session_id)
        else:
            replacement = await client.resume_session(session_id)
            await replacement.disconnect()
    finally:
        release_detach.set()
        await asyncio.gather(initialization, return_exceptions=True)
        await asyncio.gather(*client._auto_mode_recovery_cleanup_tasks, return_exceptions=True)


@pytest.mark.asyncio
@pytest.mark.parametrize("resume", [False, True])
@pytest.mark.parametrize("cancel_request", [False, True])
async def test_unsupported_enrollment_does_not_wait_for_detach(resume, cancel_request):
    started = asyncio.Event()
    release = asyncio.Event()
    finished = asyncio.Event()
    cancelled = asyncio.Event()

    class UnresponsiveRuntime(Runtime):
        async def request(self, method, params, **kwargs):
            if method == "session.detach":
                started.set()
                try:
                    await release.wait()
                except asyncio.CancelledError:
                    cancelled.set()
                    raise
                finally:
                    finished.set()
            return await super().request(method, params, **kwargs)

    client = client_for(UnresponsiveRuntime())
    initialization = asyncio.create_task(
        open_session(
            client,
            resume,
            auto_mode_recovery=True,
            on_auto_mode_switch_request=lambda _request, _invocation: "no",
        )
    )
    try:
        await asyncio.wait_for(started.wait(), 5)
        rejected_session = next(iter(client._sessions.values()))
        if cancel_request:
            initialization.cancel()
            with pytest.raises(asyncio.CancelledError):
                await initialization
            assert not cancelled.is_set(), "caller cancellation stopped remote cleanup"
        else:
            with pytest.raises(RuntimeError, match="does not support auto_mode_recovery") as error:
                await asyncio.wait_for(asyncio.shield(initialization), 15)
            assert isinstance(error.value.__cause__, TimeoutError)
        if cancel_request:
            assert client._sessions[rejected_session.session_id] is rejected_session
            assert not rejected_session._destroyed
        else:
            assert not client._sessions
            assert rejected_session._destroyed
            assert rejected_session._auto_mode_switch_handler is None
    finally:
        release.set()
        await asyncio.gather(initialization, return_exceptions=True)
        await asyncio.wait_for(finished.wait(), 5)
        await asyncio.gather(*client._auto_mode_recovery_cleanup_tasks, return_exceptions=True)
    assert not client._sessions
    assert rejected_session._destroyed
    assert rejected_session._auto_mode_switch_handler is None


@pytest.mark.asyncio
@pytest.mark.parametrize("resume", [False, True])
async def test_cancellation_before_cleanup_write_still_detaches(resume):
    release_write = asyncio.Event()
    detached = asyncio.Event()

    class CancelledEnrollmentRuntime(Runtime):
        async def request(self, method, params, **kwargs):
            if method == "session.detach":
                await release_write.wait()
            response = await super().request(method, params, **kwargs)
            if method in ("session.create", "session.resume"):
                asyncio.current_task().cancel()
            elif method == "session.detach":
                detached.set()
            return response

    runtime = CancelledEnrollmentRuntime()
    client = client_for(runtime)
    initialization = asyncio.create_task(
        open_session(
            client,
            resume,
            auto_mode_recovery=True,
            on_auto_mode_switch_request=lambda _request, _invocation: "no",
        )
    )
    try:
        with pytest.raises(asyncio.CancelledError):
            await initialization
        assert client._sessions
    finally:
        release_write.set()
        await asyncio.gather(initialization, return_exceptions=True)
        await asyncio.wait_for(detached.wait(), 5)
        await asyncio.gather(*client._auto_mode_recovery_cleanup_tasks, return_exceptions=True)
    assert not client._sessions


@pytest.mark.asyncio
@pytest.mark.parametrize("resume", [False, True])
@pytest.mark.parametrize("retry_resume", [False, True])
async def test_late_cleanup_finishes_before_same_id_reuse(resume, retry_resume):
    cleanup_started = asyncio.Event()
    release_cleanup = asyncio.Event()
    cleanup_finished = asyncio.Event()
    retry_started = asyncio.Event()

    class HeldDetachRuntime(Runtime):
        async def request(self, method, params, **kwargs):
            if method == "session.detach" and not cleanup_finished.is_set():
                cleanup_started.set()
                await release_cleanup.wait()
                response = await super().request(method, params, **kwargs)
                cleanup_finished.set()
                return response
            return await super().request(method, params, **kwargs)

    runtime = HeldDetachRuntime()
    client = client_for(runtime)
    session_id = "session-1"
    initialization = asyncio.create_task(
        open_session(
            client,
            resume,
            **({} if resume else {"session_id": session_id}),
            auto_mode_recovery=True,
            on_auto_mode_switch_request=lambda _request, _invocation: "no",
        )
    )
    replacement = None
    unrelated = None
    retry = None

    async def reuse():
        retry_started.set()
        return await open_session(
            client, retry_resume, **({} if retry_resume else {"session_id": session_id})
        )

    try:
        await asyncio.wait_for(cleanup_started.wait(), 5)
        initialization.cancel()
        with pytest.raises(asyncio.CancelledError):
            await initialization
        cancelled_retry = asyncio.create_task(reuse())
        await asyncio.wait_for(retry_started.wait(), 5)
        cancelled_retry.cancel()
        with pytest.raises(asyncio.CancelledError):
            await cancelled_retry
        assert not cleanup_finished.is_set()
        retry_started.clear()
        retry = asyncio.create_task(reuse())
        await asyncio.wait_for(retry_started.wait(), 5)
        # An unrelated round trip can complete while this ID's detach is held.
        unrelated = await client.create_session(
            session_id="unrelated", on_permission_request=PermissionHandler.approve_all
        )
        release_cleanup.set()
        replacement = await asyncio.wait_for(retry, 5)
        await asyncio.wait_for(cleanup_finished.wait(), 5)
        assert [
            method for method, params in runtime.requests if params["sessionId"] == session_id
        ] == [
            "session.resume" if resume else "session.create",
            "session.detach",
            "session.resume" if retry_resume else "session.create",
        ]
        assert client._sessions[session_id] is replacement
        assert not replacement._destroyed
    finally:
        release_cleanup.set()
        await asyncio.gather(initialization, return_exceptions=True)
        if retry is not None:
            replacement = await asyncio.wait_for(retry, 5)
        for session in (replacement, unrelated):
            if session is not None:
                await session.disconnect()


@pytest.mark.asyncio
async def test_public_deletion_waits_for_cleanup_and_fences_resume():
    cleanup_started = asyncio.Event()
    release_cleanup = asyncio.Event()
    delete_started = asyncio.Event()
    release_delete = asyncio.Event()
    deleting_started = asyncio.Event()
    resume_started = asyncio.Event()

    class HeldDeletionRuntime(Runtime):
        async def request(self, method, params, **kwargs):
            if method == "session.detach":
                cleanup_started.set()
                await release_cleanup.wait()
            if method == "session.delete":
                self.requests.append((method, params))
                delete_started.set()
                await release_delete.wait()
                return {"success": True}
            return await super().request(method, params, **kwargs)

    runtime = HeldDeletionRuntime()
    client = client_for(runtime)
    initialization = asyncio.create_task(
        open_session(
            client,
            True,
            auto_mode_recovery=True,
            on_auto_mode_switch_request=lambda _request, _invocation: "no",
        )
    )

    async def delete():
        deleting_started.set()
        await client.delete_session("session-1")

    async def resume():
        resume_started.set()
        return await open_session(client, True)

    deleting = retry = None
    replacement = None
    try:
        await asyncio.wait_for(cleanup_started.wait(), 5)
        initialization.cancel()
        with pytest.raises(asyncio.CancelledError):
            await initialization
        owner = client._sessions["session-1"]
        deleting = asyncio.create_task(delete())
        await deleting_started.wait()
        assert not delete_started.is_set(), "deletion overtook pending detach"
        assert client._sessions["session-1"] is owner
        release_cleanup.set()
        await asyncio.wait_for(delete_started.wait(), 5)
        retry = asyncio.create_task(resume())
        await resume_started.wait()
        assert not retry.done(), "resume overtook pending deletion"
        assert "session-1" not in client._sessions
        release_delete.set()
        await asyncio.wait_for(deleting, 5)
        replacement = await asyncio.wait_for(retry, 5)
        assert client._sessions["session-1"] is replacement
        assert [method for method, _ in runtime.requests] == [
            "session.resume",
            "session.detach",
            "session.delete",
            "session.resume",
        ]
    finally:
        release_cleanup.set()
        release_delete.set()
        await asyncio.gather(initialization, return_exceptions=True)
        if deleting is not None:
            await asyncio.gather(deleting, return_exceptions=True)
        if retry is not None:
            replacement = await retry
        if replacement is not None:
            await replacement.disconnect()


@pytest.mark.asyncio
async def test_public_deletion_preserves_owner_from_replacement_connection():
    delete_started = asyncio.Event()
    release_delete = asyncio.Event()

    class DeletingRuntime(Runtime):
        async def request(self, method, params, **kwargs):
            if method == "session.delete":
                self.requests.append((method, params))
                delete_started.set()
                await release_delete.wait()
                return {"success": True}
            return await super().request(method, params, **kwargs)

    old_runtime = DeletingRuntime()
    client = client_for(old_runtime)
    original = await client.create_session(
        session_id="session-1", on_permission_request=PermissionHandler.approve_all
    )
    replacement_runtime = Runtime()
    replacement_client = client_for(replacement_runtime)
    replacement = await replacement_client.create_session(
        session_id="session-1", on_permission_request=PermissionHandler.approve_all
    )
    deleting = asyncio.create_task(client.delete_session("session-1"))
    try:
        await asyncio.wait_for(delete_started.wait(), 5)
        client._client = replacement_runtime
        client._sessions["session-1"] = replacement
        release_delete.set()
        await asyncio.wait_for(deleting, 5)
        assert client._sessions["session-1"] is replacement
        assert not replacement._destroyed
        assert [method for method, _ in replacement_runtime.requests] == ["session.create"]
    finally:
        release_delete.set()
        await deleting
        await original.disconnect()
        await replacement.disconnect()


@pytest.mark.asyncio
@pytest.mark.parametrize("resume", [False, True])
@pytest.mark.parametrize("shutdown", ["stop", "force_stop", "connection_loss"])
async def test_failed_cleanup_blocks_same_id_reuse_until_connection_retires(resume, shutdown):
    cleanup_started = asyncio.Event()
    release_cleanup = asyncio.Event()
    retry_started = asyncio.Event()
    transport_stop_started = asyncio.Event()
    release_transport_stop = asyncio.Event()

    class FailedDetachRuntime(Runtime):
        async def request(self, method, params, **kwargs):
            if method == "session.detach":
                cleanup_started.set()
                await release_cleanup.wait()
                raise RuntimeError("detach failed")
            return await super().request(method, params, **kwargs)

    runtime = FailedDetachRuntime()
    runtime._loop = asyncio.get_running_loop()
    client = client_for(runtime)

    async def stop_transport():
        transport_stop_started.set()
        await release_transport_stop.wait()

    runtime.stop = AsyncMock(side_effect=stop_transport)

    async def retire_connection():
        if shutdown == "connection_loss":
            await stop_transport()
            client._handle_connection_close()
        else:
            await getattr(client, shutdown)()

    initialization = asyncio.create_task(
        open_session(
            client,
            resume,
            **({} if resume else {"session_id": "session-1"}),
            auto_mode_recovery=True,
            on_auto_mode_switch_request=lambda _request, _invocation: "no",
        )
    )

    async def reuse():
        retry_started.set()
        return await client.resume_session(
            "session-1",
            on_permission_request=PermissionHandler.approve_all,
        )

    retry = None
    stopping = None
    replacement = None
    try:
        await asyncio.wait_for(cleanup_started.wait(), 5)
        initialization.cancel()
        with pytest.raises(asyncio.CancelledError):
            await initialization
        retry = asyncio.create_task(reuse())
        await asyncio.wait_for(retry_started.wait(), 5)
        initialization_lock = client._session_initializations["session-1"]
        deadline = asyncio.get_running_loop().time() + 5
        while not initialization_lock.locked():
            assert asyncio.get_running_loop().time() < deadline, (
                "same-ID retry did not acquire the cleanup gate"
            )
            await asyncio.sleep(0)
        assert not retry.done()

        release_cleanup.set()
        with pytest.raises(RuntimeError, match="detach failed"):
            await asyncio.wait_for(retry, 5)
        assert [method for method, _ in runtime.requests] == [
            "session.resume" if resume else "session.create"
        ]

        stopping = asyncio.create_task(retire_connection())
        await asyncio.wait_for(transport_stop_started.wait(), 5)
        with pytest.raises(RuntimeError, match="detach failed"):
            await asyncio.wait_for(reuse(), 5)
        release_transport_stop.set()
        await asyncio.wait_for(stopping, 5)
        if shutdown == "connection_loss":
            runtime.stop.assert_not_awaited()
        else:
            runtime.stop.assert_awaited_once()
            assert client._client is None

        next_runtime = Runtime()

        async def connect():
            client._client = next_runtime

        with (
            patch.object(client, "_connect_to_server", new=AsyncMock(side_effect=connect)),
            patch.object(client, "_verify_protocol_version", new=AsyncMock()),
        ):
            await asyncio.wait_for(client.start(), 5)
        replacement = await asyncio.wait_for(reuse(), 5)
        assert replacement.session_id == "session-1"
        assert client._sessions["session-1"] is replacement
        assert not replacement._destroyed
        assert [method for method, _ in next_runtime.requests] == ["session.resume"]
        assert "requestAutoModeRecovery" not in next_runtime.requests[0][1]
    finally:
        release_cleanup.set()
        release_transport_stop.set()
        await asyncio.gather(initialization, return_exceptions=True)
        if retry is not None:
            await asyncio.gather(retry, return_exceptions=True)
        if stopping is not None:
            await asyncio.wait_for(stopping, 5)
        if replacement is not None:
            await replacement.disconnect()


@pytest.mark.asyncio
@pytest.mark.parametrize("retry_resume", [False, True])
async def test_same_id_initialization_waits_before_replacing_provisional_owner(retry_resume):
    first_created = asyncio.Event()
    release_response = asyncio.Event()
    detach_started = asyncio.Event()
    release_detach = asyncio.Event()
    retry_started = asyncio.Event()

    class AttachedRuntime(Runtime):
        def __init__(self):
            super().__init__()
            self.attached = False
            self.first = True

        async def request(self, method, params, **kwargs):
            if (
                method in ("session.create", "session.resume")
                and params["sessionId"] == "session-1"
            ):
                if self.first:
                    self.first = False
                    self.attached = True
                    response = await super().request(method, params, **kwargs)
                    first_created.set()
                    await release_response.wait()
                    return response
                if self.attached:
                    raise RuntimeError("duplicate runtime attachment")
                self.attached = True
            if method == "session.detach":
                detach_started.set()
                await release_detach.wait()
                self.attached = False
            return await super().request(method, params, **kwargs)

    runtime = AttachedRuntime()
    client = client_for(runtime)
    first = asyncio.create_task(
        open_session(
            client,
            False,
            session_id="session-1",
            auto_mode_recovery=True,
            on_auto_mode_switch_request=lambda _request, _invocation: "no",
        )
    )
    retry = None
    replacement = None

    async def reuse():
        retry_started.set()
        return await open_session(
            client, retry_resume, **({} if retry_resume else {"session_id": "session-1"})
        )

    try:
        await asyncio.wait_for(first_created.wait(), 5)
        owner = client._sessions["session-1"]
        retry = asyncio.create_task(reuse())
        await asyncio.wait_for(retry_started.wait(), 5)
        assert client._sessions.get("session-1") is owner
        release_response.set()
        await asyncio.wait_for(detach_started.wait(), 5)
        assert not retry.done()
        release_detach.set()
        with pytest.raises(RuntimeError, match="does not support auto_mode_recovery"):
            await first
        replacement = await asyncio.wait_for(retry, 5)
        assert runtime.attached
        assert client._sessions["session-1"] is replacement
    finally:
        release_response.set()
        release_detach.set()
        await asyncio.gather(first, return_exceptions=True)
        if retry is not None:
            results = await asyncio.gather(retry, return_exceptions=True)
            if not isinstance(results[0], BaseException):
                await results[0].disconnect()


@pytest.mark.asyncio
@pytest.mark.parametrize("resume", [False, True])
async def test_complete_context_round_trip(resume):
    runtime = Runtime({"ui": {"autoModeRecovery": True}})
    client = client_for(runtime)
    received = []

    def handler(request, invocation):
        received.append((request, invocation))
        return "yes"

    session = await open_session(
        client, resume, auto_mode_recovery=True, on_auto_mode_switch_request=handler
    )
    try:
        assert runtime.requests[0][1]["requestAutoModeRecovery"] is True
        assert runtime.requests[0][1]["requestAutoModeSwitch"] is True
        assert session.capabilities["ui"]["autoModeRecovery"] is True
        offer = {
            "errorCode": "model_overloaded",
            "retryAfterSeconds": 3.5,
            "autoModeRecovery": AUTO_MODE_RECOVERY,
        }
        result = await client._handle_auto_mode_switch_request(
            {"sessionId": session.session_id, **offer}
        )
        assert result == {"response": "yes"}
        assert received == [(offer, {"session_id": session.session_id})]
    finally:
        await session.disconnect()


def dispatch(session, event_type, data):
    event = session_event_from_dict(
        {
            "id": str(uuid4()),
            "timestamp": "2026-09-25T12:00:00Z",
            "parentId": None,
            "type": event_type,
            "ephemeral": True,
            "data": data,
        }
    )
    assert event.type.value == event_type, (
        "Recovery notifications must use generated event variants"
    )
    session._dispatch_event(event)


@pytest.mark.asyncio
async def test_capability_changes_preserve_and_revoke_acknowledgment():
    runtime = Runtime({"ui": {"autoModeRecovery": True, "elicitation": True}})
    session = await open_session(client_for(runtime), False)
    try:
        dispatch(session, "capabilities.changed", {"ui": {"elicitation": False}})
        assert session.capabilities["ui"] == {"autoModeRecovery": True, "elicitation": False}
        dispatch(session, "capabilities.changed", {"ui": {"autoModeRecovery": False}})
        assert session.capabilities["ui"] == {"autoModeRecovery": False, "elicitation": False}
    finally:
        await session.disconnect()


@pytest.mark.parametrize(
    "left,right,overlap",
    [
        ("session-1", "session-1", True),
        ("session-1", "session-123", False),
        ("session-a", "SESSION-A", False),
        ("abcdef12", "abcdef12-custom", False),
        ("abcdef", "abcdef12-abcd-4123-8123-123456789abc", False),
        ("abcdef12", "abcdef12-abcd-4123-8123-123456789abc", True),
        ("ABCDEF12-ABCD", "abcdef12-abcd-4123-8123-123456789abc", True),
    ],
)
def test_session_id_overlap_is_limited_to_uuid_aliases(left, right, overlap):
    assert _session_ids_overlap(left, right) is overlap
    assert _session_ids_overlap(right, left) is overlap


@asynccontextmanager
async def session_fs_peer(on_create=None, on_request=None, *, session_fs=True):
    writers = []
    tasks = set()
    callbacks = {}
    next_id = 100
    ready = asyncio.Event()

    async def write(writer, message):
        body = json.dumps({"jsonrpc": "2.0", **message}).encode()
        writer.write(f"Content-Length: {len(body)}\r\n\r\n".encode() + body)
        await writer.drain()

    async def callback(session_id, method="sessionFs.writeFile", params=None):
        nonlocal next_id
        await ready.wait()
        next_id += 1
        request_id = next_id
        response = asyncio.get_running_loop().create_future()
        callbacks[request_id] = response
        await write(
            writers[0],
            {
                "id": request_id,
                "method": method,
                "params": params or {"sessionId": session_id, "path": "/state", "content": "{}"},
            },
        )
        return await response

    async def respond(writer, message):
        if on_request is not None:
            response = await on_request(callback, message)
            if response is not None:
                await write(writer, {"id": message["id"], **response})
                return
        if message["method"] == "session.create" and on_create is not None:
            await on_create(callback, message["params"])
        result = (
            {"sessionId": message["params"]["sessionId"]}
            if message["method"] in ("session.create", "session.resume")
            else {"success": True}
        )
        await write(writer, {"id": message["id"], "result": result})

    async def accept(reader, writer):
        writers.append(writer)
        ready.set()
        try:
            while True:
                header = await reader.readuntil(b"\r\n\r\n")
                length = int(header.decode().split(":")[1].strip())
                message = json.loads(await reader.readexactly(length))
                if "method" in message:
                    task = asyncio.create_task(respond(writer, message))
                    tasks.add(task)
                else:
                    callbacks.pop(message["id"]).set_result(message)
        except asyncio.IncompleteReadError:
            pass

    server = await asyncio.start_server(accept, "127.0.0.1", 0)
    port = server.sockets[0].getsockname()[1]
    client = CopilotClient(
        connection=RuntimeConnection.for_uri(f"127.0.0.1:{port}"),
        session_fs={
            "initial_working_directory": "/workspace",
            "session_state_path": "/sessions",
            "conventions": "posix",
        }
        if session_fs
        else None,
    )
    await client._connect_via_tcp()
    try:
        yield client, callback
    finally:
        await client.stop()
        for task in tasks:
            if not task.done():
                task.cancel()
        await asyncio.gather(*tasks, return_exceptions=True)
        for writer in writers:
            writer.close()
            await writer.wait_closed()
        server.close()
        await server.wait_closed()


@pytest.mark.asyncio
@pytest.mark.parametrize("operation", ["create", "resume", "delete"])
async def test_callback_rejects_same_id_initialization(operation):
    observed = []

    async def on_create(callback, _params):
        response = await callback("reentry")
        assert response["result"] is None

    async with session_fs_peer(on_create) as (client, _callback):

        class Provider:
            async def write_file(self, _path, _content, _mode=None):
                try:
                    if operation == "delete":
                        await asyncio.wait_for(client.delete_session("reentry"), 1)
                    elif operation == "resume":
                        await asyncio.wait_for(
                            client.resume_session(
                                "reentry",
                                on_permission_request=PermissionHandler.approve_all,
                                create_session_fs_handler=lambda _session: Provider(),
                            ),
                            1,
                        )
                    else:
                        await asyncio.wait_for(
                            client.create_session(
                                session_id="reentry",
                                on_permission_request=PermissionHandler.approve_all,
                                create_session_fs_handler=lambda _session: Provider(),
                            ),
                            1,
                        )
                except Exception as error:
                    observed.append(error)

        session = await client.create_session(
            session_id="reentry",
            on_permission_request=PermissionHandler.approve_all,
            create_session_fs_handler=lambda _session: Provider(),
        )
        try:
            assert len(observed) == 1
            assert isinstance(observed[0], RuntimeError), repr(observed[0])
            assert "active session callback" in str(observed[0])
        finally:
            await session.disconnect()


@pytest.mark.asyncio
async def test_canonical_callback_rejects_wait_on_its_alias_permit():
    canonical_id = "abcdef12-3456-4789-abcd-0123456789ab"
    observed = []

    async def on_request(callback, message):
        if message["method"] == "sessions.findByPrefix":
            assert message["params"] == {"prefix": "abcdef12"}
            return {"result": {"sessionId": canonical_id}}
        if message["method"] == "session.resume":
            return {
                "result": {
                    "sessionId": canonical_id,
                    "capabilities": {"ui": {"autoModeRecovery": True}},
                }
            }
        if message["method"] == "session.options.update":
            reply = await callback(canonical_id)
            assert "error" not in reply, reply

    async with session_fs_peer(on_request=on_request) as (client, _callback):

        class Provider:
            async def write_file(self, _path, _content, _mode=None):
                try:
                    await asyncio.wait_for(
                        client.resume_session(
                            "abcdef12-9999-4789-abcd-0123456789ab",
                            create_session_fs_handler=lambda _session: Provider(),
                        ),
                        1,
                    )
                except Exception as error:
                    observed.append(error)

        session = await client.resume_session(
            canonical_id,
            auto_mode_recovery=True,
            on_auto_mode_switch_request=lambda _request, _invocation: "no",
            create_session_fs_handler=lambda _session: Provider(),
            skip_custom_instructions=True,
        )
        try:
            assert len(observed) == 1
            assert isinstance(observed[0], RuntimeError), repr(observed[0])
            assert "active session callback" in str(observed[0])
        finally:
            await session.disconnect()


@pytest.mark.asyncio
async def test_rejected_cleanup_hook_rejects_same_id_resume():
    observed = []
    cleanup_entered = asyncio.Event()

    async def on_request(callback, message):
        if message["method"] == "session.detach" and not cleanup_entered.is_set():
            cleanup_entered.set()
            reply = await callback(
                "cleanup-hook",
                "hooks.invoke",
                {"sessionId": "cleanup-hook", "hookType": "sessionEnd", "input": {}},
            )
            assert "error" not in reply, reply

    async with session_fs_peer(on_request=on_request) as (client, _callback):

        async def hook(_input, _invocation):
            try:
                await asyncio.wait_for(
                    client.resume_session(
                        "cleanup-hook", create_session_fs_handler=lambda _session: object()
                    ),
                    1,
                )
            except Exception as error:
                observed.append(error)
            return None

        with pytest.raises(RuntimeError, match="does not support auto_mode_recovery"):
            await client.create_session(
                session_id="cleanup-hook",
                create_session_fs_handler=lambda _session: object(),
                auto_mode_recovery=True,
                on_auto_mode_switch_request=lambda _request, _invocation: "no",
                hooks={"on_session_end": hook},
            )
        assert len(observed) == 1
        assert isinstance(observed[0], RuntimeError), repr(observed[0])
        assert "active session callback" in str(observed[0])
        replacement = await client.resume_session(
            "cleanup-hook", create_session_fs_handler=lambda _session: object()
        )
        await replacement.disconnect()


@pytest.mark.asyncio
async def test_recovery_resume_waits_for_failed_ordinary_resume():
    ordinary_entered = asyncio.Event()
    release_ordinary = asyncio.Event()
    recovery_entered = asyncio.Event()
    release_recovery = asyncio.Event()
    detached = asyncio.Event()

    async def on_request(_callback, message):
        if message["method"] == "session.resume":
            if message["params"].get("requestAutoModeRecovery"):
                recovery_entered.set()
                await release_recovery.wait()
            else:
                ordinary_entered.set()
                await release_ordinary.wait()
                return {"error": {"code": -32603, "message": "ordinary resume rejected"}}
        elif message["method"] == "session.detach":
            detached.set()

    async with session_fs_peer(on_request=on_request) as (client, _callback):
        options = {"create_session_fs_handler": lambda _session: object()}
        ordinary = asyncio.create_task(client.resume_session("provisional", **options))
        await asyncio.wait_for(ordinary_entered.wait(), 5)
        recovery = asyncio.create_task(
            client.resume_session(
                "provisional",
                auto_mode_recovery=True,
                on_auto_mode_switch_request=lambda _request, _invocation: "no",
                **options,
            )
        )
        try:
            # The pending recovery either waits locally or incorrectly reaches the peer.
            async def admitted_or_waiting():
                while not recovery_entered.is_set():
                    lock = client._session_initializations.get("provisional")
                    if lock is not None and lock.locked():
                        return
                    await asyncio.sleep(0)

            await asyncio.wait_for(admitted_or_waiting(), 5)
            release_ordinary.set()
            with pytest.raises(Exception, match="ordinary resume rejected"):
                await ordinary
            release_recovery.set()
            with pytest.raises(RuntimeError, match="does not support auto_mode_recovery"):
                await recovery
            assert detached.is_set(), "recovery treated a rejected provisional owner as resident"
        finally:
            release_ordinary.set()
            release_recovery.set()
            await asyncio.gather(ordinary, recovery, return_exceptions=True)


@pytest.mark.asyncio
async def test_cancelling_recovery_wait_does_not_cancel_ordinary_resume():
    ordinary_entered = asyncio.Event()
    release_ordinary = asyncio.Event()

    async def on_request(_callback, message):
        if message["method"] == "session.resume":
            assert not message["params"].get("requestAutoModeRecovery")
            ordinary_entered.set()
            await release_ordinary.wait()

    async with session_fs_peer(on_request=on_request) as (client, _callback):
        options = {"create_session_fs_handler": lambda _session: object()}
        ordinary = asyncio.create_task(client.resume_session("cancel-wait", **options))
        await asyncio.wait_for(ordinary_entered.wait(), 5)
        recovery = asyncio.create_task(
            client.resume_session(
                "cancel-wait",
                auto_mode_recovery=True,
                on_auto_mode_switch_request=lambda _request, _invocation: "no",
                **options,
            )
        )
        try:

            async def waiting():
                while True:
                    lock = client._session_initializations.get("cancel-wait")
                    if lock is not None and lock.locked():
                        return
                    await asyncio.sleep(0)

            await asyncio.wait_for(waiting(), 5)
            recovery.cancel()
            with pytest.raises(asyncio.CancelledError):
                await recovery
            release_ordinary.set()
            session = await asyncio.wait_for(ordinary, 5)
            await session.disconnect()
        finally:
            release_ordinary.set()
            await asyncio.gather(ordinary, recovery, return_exceptions=True)


@pytest.mark.asyncio
@pytest.mark.parametrize("resident", [False, True])
@pytest.mark.parametrize("rpc_rejected", [False, True])
async def test_abbreviated_recovery_resume_uses_canonical_owner(resident, rpc_rejected):
    canonical_id = "abcdef12-3456-4789-abcd-0123456789ab"
    requests = []
    invocations = []
    cleanup_replies = []

    async def on_request(callback, message):
        requests.append((message["method"], message["params"]))
        if message["method"] == "sessions.findByPrefix":
            assert message["params"] == {"prefix": "abcdef12"}
            return {"result": {"sessionId": canonical_id}}
        if message["method"] == "session.resume":
            assert message["params"]["sessionId"] == canonical_id
            if rpc_rejected:
                return {"error": {"code": -32603, "message": "resume rejected"}}
            return {"result": {"sessionId": canonical_id}}
        if message["method"] == "session.detach":
            reply = await callback(
                canonical_id,
                "autoModeSwitch.request",
                {"sessionId": canonical_id, "errorCode": "user_weekly_rate_limited"},
            )
            cleanup_replies.append(reply)

    async with session_fs_peer(on_request=on_request, session_fs=False) as (client, callback):
        options = {"create_session_fs_handler": lambda _session: object()}
        previous = None
        if resident:
            previous = await client.create_session(
                session_id=canonical_id,
                on_auto_mode_switch_request=lambda _request, _invocation: "yes_always",
                **options,
            )

        def accepted(_request, invocation):
            invocations.append(invocation["session_id"])
            return "yes"

        with pytest.raises(
            Exception,
            match=("resume rejected" if rpc_rejected else "does not support auto_mode_recovery"),
        ):
            await client.resume_session(
                "abcdef12",
                auto_mode_recovery=True,
                on_auto_mode_switch_request=accepted,
                **options,
            )
        detaches = [params for method, params in requests if method == "session.detach"]
        assert detaches == (
            [{"sessionId": canonical_id}] if not resident and not rpc_rejected else []
        ), requests
        assert all(method != "session.delete" for method, _params in requests)
        assert "abcdef12" not in client._sessions
        current = client._sessions.get(canonical_id)
        if rpc_rejected:
            assert current is previous
        elif resident:
            assert current is not None and current is not previous
            reply = await callback(
                canonical_id,
                "autoModeSwitch.request",
                {"sessionId": canonical_id, "errorCode": "user_weekly_rate_limited"},
            )
            assert reply.get("result") == {"response": "yes"}, reply
        else:
            assert current is None
        assert invocations == ([canonical_id] if not rpc_rejected else [])
        assert all(reply.get("result") == {"response": "yes"} for reply in cleanup_replies)


@pytest.mark.asyncio
@pytest.mark.parametrize("resident", [False, True])
@pytest.mark.parametrize("outcome", ["accepted", "lookup-rejected", "resume-rejected", "ambiguous"])
async def test_uuid_prefix_callback_ownership(resident, outcome):
    canonical_id = "abcdef12-3456-4789-abcd-0123456789ab"
    requests = []
    invocations = []
    lookup_replies = []
    resume_replies = []
    setup_ids = []

    async def consent(callback):
        return await callback(
            canonical_id,
            "autoModeSwitch.request",
            {"sessionId": canonical_id, "errorCode": "user_weekly_rate_limited"},
        )

    async def on_request(callback, message):
        method, params = message["method"], message["params"]
        requests.append((method, params))
        if method == "sessions.findByPrefix":
            lookup_replies.append(await consent(callback))
            if outcome == "lookup-rejected":
                return {"error": {"code": -32601, "message": "lookup rejected"}}
            if outcome == "ambiguous":
                # Another persisted session shares this prefix but is not in the local map.
                return {"result": {}}
            return {"result": {"sessionId": canonical_id}}
        if method == "session.resume":
            resume_replies.append(await consent(callback))
            if outcome == "ambiguous":
                return {"error": {"code": -32000, "message": "ambiguous prefix"}}
            if outcome == "resume-rejected":
                return {"error": {"code": -32000, "message": "resume rejected"}}
            return {
                "result": {
                    "sessionId": canonical_id,
                    "capabilities": {"ui": {"autoModeRecovery": True}},
                }
            }

    async with session_fs_peer(on_request=on_request, session_fs=False) as (client, callback):
        previous = None
        if resident:
            previous = await client.create_session(
                session_id=canonical_id,
                create_session_fs_handler=lambda _session: object(),
                on_auto_mode_switch_request=lambda _request, _invocation: "yes_always",
            )

        def provider(session):
            setup_ids.append(session.session_id)
            assert client._sessions.get(canonical_id) is previous
            return object()

        register = client._register_session

        def register_after_setup(session):
            provider(session)
            return register(session)

        client._register_session = register_after_setup

        def accepted(_request, invocation):
            invocations.append(invocation["session_id"])
            return "yes"

        async def resume():
            return await client.resume_session(
                "ABCDEF12-3456",
                auto_mode_recovery=True,
                on_auto_mode_switch_request=accepted,
                create_session_fs_handler=provider,
            )

        if outcome == "accepted":
            session = await resume()
            assert session.session_id == canonical_id
            assert client._sessions.get(canonical_id) is session
        else:
            expected_error = {
                "lookup-rejected": "lookup rejected",
                "resume-rejected": "resume rejected",
                "ambiguous": "no unique session",
            }[outcome]
            with pytest.raises(Exception, match=expected_error):
                await resume()
            assert client._sessions.get(canonical_id) is previous
        assert "ABCDEF12-3456" not in client._sessions
        expected_resume = outcome in ("accepted", "resume-rejected")
        final_reply = await consent(callback)
        if outcome == "accepted":
            assert final_reply.get("result") == {"response": "yes"}, final_reply
        elif resident:
            assert final_reply.get("result") == {"response": "yes_always"}, final_reply
        else:
            assert "unknown session" in final_reply.get("error", {}).get("message", ""), final_reply
        assert all(reply.get("result") == {"response": "yes"} for reply in resume_replies), (
            resume_replies
        )
        assert lookup_replies, "resume did not resolve the persisted prefix"
        for reply in lookup_replies:
            if resident:
                assert reply.get("result") == {"response": "yes_always"}, reply
            else:
                assert "unknown session" in reply.get("error", {}).get("message", ""), reply
        assert [params for method, params in requests if method == "sessions.findByPrefix"] == [
            {"prefix": "ABCDEF12-3456"}
        ]
        assert [
            params["sessionId"] for method, params in requests if method == "session.resume"
        ] == ([canonical_id] if expected_resume else [])
        assert setup_ids == ([canonical_id] if expected_resume else [])
        assert invocations == (
            [canonical_id] * (2 if outcome == "accepted" else 1) if expected_resume else []
        )
        assert all(
            method not in ("session.detach", "session.delete") for method, _params in requests
        )


@pytest.mark.asyncio
@pytest.mark.parametrize(
    "session_id,lookup",
    [
        ("abcdef1", True),
        ("ABCDEF12", True),
        ("abcdef12-3456", True),
        ("abcdef12-3456-4789-abcd-0123456789a", True),
        ("abcdef", False),
        ("abc-def1", False),
        ("abcdefg", False),
        ("abcdeｆ1", False),
        ("abcdef1é", False),
        ("deadbeef_task1", False),
        ("session-123", False),
        ("abcdef12-3456-4789-abcd-0123456789ab", False),
        ("ABCDEF12-3456-4789-ABCD-0123456789AB", False),
        ("abcdef12-3456-4789-abcd-0123456789ab0", False),
    ],
)
async def test_resume_session_prefix_lookup_guard(session_id, lookup):
    canonical_id = "abcdef12-3456-4789-abcd-0123456789ab"
    expected_id = canonical_id if lookup else session_id
    requests = []

    async def on_request(_callback, message):
        requests.append((message["method"], message["params"]))
        if message["method"] == "sessions.findByPrefix":
            return {"result": {"sessionId": canonical_id}}
        if message["method"] == "session.resume":
            assert message["params"]["sessionId"] == expected_id
            return {
                "result": {
                    "sessionId": expected_id,
                    "capabilities": {"ui": {"autoModeRecovery": True}},
                }
            }

    async with session_fs_peer(on_request=on_request, session_fs=False) as (client, _callback):
        assert not requests
        session = await client.resume_session(
            session_id,
            auto_mode_recovery=True,
            on_auto_mode_switch_request=lambda _request, _invocation: "no",
        )
        assert session.session_id == expected_id
        assert [params for method, params in requests if method == "sessions.findByPrefix"] == (
            [{"prefix": session_id}] if lookup else []
        )


@pytest.mark.asyncio
async def test_cancelled_prefix_lookup_releases_canonical_admission():
    canonical_id = "abcdef12-3456-4789-abcd-0123456789ab"
    lookup_entered = asyncio.Event()
    release_lookup = asyncio.Event()
    create_entered = asyncio.Event()
    resumes = []

    async def on_request(_callback, message):
        if message["method"] == "sessions.findByPrefix":
            lookup_entered.set()
            await release_lookup.wait()
            return {"result": {"sessionId": canonical_id}}
        if message["method"] == "session.create":
            create_entered.set()
        if message["method"] == "session.resume":
            resumes.append(message)

    async with session_fs_peer(on_request=on_request, session_fs=False) as (client, _callback):
        options = {"create_session_fs_handler": lambda _session: object()}
        first = asyncio.create_task(
            client.resume_session(
                "abcdef12",
                auto_mode_recovery=True,
                on_auto_mode_switch_request=lambda _request, _invocation: "no",
                **options,
            )
        )
        await asyncio.wait_for(lookup_entered.wait(), 5)
        replacement = asyncio.create_task(client.create_session(session_id=canonical_id, **options))
        try:

            async def waiting():
                while not any(lock._waiters for lock in client._session_initializations.values()):
                    assert not create_entered.is_set(), "canonical create overtook prefix lookup"
                    await asyncio.sleep(0)

            await asyncio.wait_for(waiting(), 5)
            assert not create_entered.is_set()
            first.cancel()
            with pytest.raises(asyncio.CancelledError):
                await asyncio.wait_for(first, 5)
            session = await asyncio.wait_for(replacement, 5)
            assert create_entered.is_set()
            assert client._sessions.get(canonical_id) is session
            assert "abcdef12" not in client._sessions
            assert not resumes
        finally:
            release_lookup.set()
            if not first.done():
                first.cancel()
            await asyncio.gather(first, replacement, return_exceptions=True)


@pytest.mark.asyncio
@pytest.mark.parametrize("resume", [False, True])
@pytest.mark.parametrize("recovery", [False, True])
async def test_options_patch_failure_cannot_detach_replacement(resume, recovery):
    patch_entered = asyncio.Event()
    release_patch = asyncio.Event()
    replacement_entered = asyncio.Event()
    requests = []
    patches = 0

    async def on_request(_callback, message):
        nonlocal patches
        method, params = message["method"], message["params"]
        requests.append((method, params))
        if method in ("session.create", "session.resume"):
            if patch_entered.is_set():
                replacement_entered.set()
            return {
                "result": {
                    "sessionId": params["sessionId"],
                    "capabilities": {"ui": {"autoModeRecovery": True}},
                }
            }
        if method == "session.options.update":
            patches += 1
            if patches == 1:
                patch_entered.set()
                await release_patch.wait()
                return {"error": {"code": -32603, "message": "options patch rejected"}}

    async with session_fs_peer(on_request=on_request) as (client, callback):
        options = {"create_session_fs_handler": lambda _session: object()}
        first = asyncio.create_task(
            client.resume_session("patch-owner", skip_custom_instructions=True, **options)
            if resume
            else client.create_session(
                session_id="patch-owner", skip_custom_instructions=True, **options
            )
        )
        await asyncio.wait_for(patch_entered.wait(), 5)
        replacement = asyncio.create_task(
            client.resume_session(
                "patch-owner",
                auto_mode_recovery=recovery,
                on_auto_mode_switch_request=lambda _request, _invocation: "yes",
                **options,
            )
        )
        try:
            if resume and not recovery:
                # Ordinary resumes can replace an owner while its options patch is pending.
                await asyncio.wait_for(replacement_entered.wait(), 5)
            else:

                async def waiting_or_admitted():
                    while not replacement_entered.is_set():
                        lock = client._session_initializations.get("patch-owner")
                        if lock is not None and lock.locked():
                            return
                        await asyncio.sleep(0)

                await asyncio.wait_for(waiting_or_admitted(), 5)
            release_patch.set()
            with pytest.raises(Exception, match="options patch rejected"):
                await first
            session = await asyncio.wait_for(replacement, 5)
            assert client._sessions.get("patch-owner") is session
            assert not session._destroyed
            reply = await callback(
                "patch-owner",
                "autoModeSwitch.request",
                {"sessionId": "patch-owner", "errorCode": "user_weekly_rate_limited"},
            )
            assert reply.get("result") == {"response": "yes"}, reply
            expected_detaches = 0 if resume and not recovery else 1
            assert sum(method == "session.detach" for method, _ in requests) == expected_detaches
            if recovery or not resume:
                assert requests.index(("session.detach", {"sessionId": "patch-owner"})) < next(
                    index
                    for index, (method, _) in enumerate(requests)
                    if method == "session.resume" and index > 1
                )
        finally:
            release_patch.set()
            await asyncio.gather(first, replacement, return_exceptions=True)


@pytest.mark.asyncio
@pytest.mark.parametrize("recovery_first", [False, True])
@pytest.mark.parametrize("alias_id", ["abcdef12", "ABCDEF12"])
async def test_in_flight_alias_resume_cannot_be_detached_by_rejected_recovery(
    recovery_first, alias_id
):
    canonical_id = "abcdef12-3456-4789-abcd-0123456789ab"
    ordinary_entered = asyncio.Event()
    recovery_entered = asyncio.Event()
    release_first = asyncio.Event()
    attached = False

    async def on_request(_callback, message):
        nonlocal attached
        method, params = message["method"], message["params"]
        if method == "sessions.findByPrefix":
            assert params == {"prefix": alias_id}
            return {"result": {"sessionId": canonical_id}}
        if method == "session.resume":
            recovery = params.get("requestAutoModeRecovery", False)
            assert params["sessionId"] == (canonical_id if recovery else alias_id)
            attached = True
            (recovery_entered if recovery else ordinary_entered).set()
            if recovery == recovery_first:
                await release_first.wait()
            return {"result": {"sessionId": canonical_id}}
        if method == "session.detach":
            attached = False

    async with session_fs_peer(on_request=on_request, session_fs=False) as (client, _callback):
        options = {"create_session_fs_handler": lambda _session: object()}

        async def recovery_resume():
            with pytest.raises(RuntimeError, match="does not support auto_mode_recovery"):
                await client.resume_session(
                    canonical_id,
                    auto_mode_recovery=True,
                    on_auto_mode_switch_request=lambda _request, _invocation: "no",
                    **options,
                )

        def ordinary_resume():
            return client.resume_session(alias_id, **options)

        first = asyncio.create_task(recovery_resume() if recovery_first else ordinary_resume())
        await asyncio.wait_for((recovery_entered if recovery_first else ordinary_entered).wait(), 5)
        second = asyncio.create_task(ordinary_resume() if recovery_first else recovery_resume())
        second_entered = ordinary_entered if recovery_first else recovery_entered
        try:

            async def waiting_or_accepted():
                while not second_entered.is_set():
                    # A queued alias waits on either an initialization or an ordinary resume.
                    locks = tuple(client._session_initializations.values())
                    if any(lock._waiters for lock in locks) or (
                        not recovery_first and any(lock.locked() for lock in locks)
                    ):
                        return
                    await asyncio.sleep(0)

            await asyncio.wait_for(waiting_or_accepted(), 5)
            if not recovery_first and second_entered.is_set():
                await asyncio.wait_for(second, 5)
            release_first.set()
            results = await asyncio.wait_for(asyncio.gather(first, second), 5)
            session = results[1 if recovery_first else 0]
            assert attached, "ordinary alias resume returned after its runtime binding was detached"
            assert client._sessions.get(canonical_id) is not None
            assert not session._destroyed
        finally:
            release_first.set()
            await asyncio.gather(first, second, return_exceptions=True)


@pytest.mark.asyncio
async def test_pending_consent_allows_independent_resume():
    entered = asyncio.Event()
    release = asyncio.Event()

    async def on_request(_callback, message):
        if message["method"] in ("session.create", "session.resume"):
            return {
                "result": {
                    "sessionId": message["params"]["sessionId"],
                    "capabilities": {"ui": {"autoModeRecovery": True}},
                }
            }

    async with session_fs_peer(on_request=on_request) as (client, callback):

        async def consent(_request, _invocation):
            entered.set()
            await release.wait()
            return "no"

        options = {"create_session_fs_handler": lambda _session: object()}
        session = await client.create_session(
            session_id="pending-consent",
            auto_mode_recovery=True,
            on_auto_mode_switch_request=consent,
            **options,
        )
        offer = asyncio.create_task(
            callback(
                session.session_id,
                "autoModeSwitch.request",
                {"sessionId": session.session_id, "errorCode": "user_weekly_rate_limited"},
            )
        )
        try:
            await asyncio.wait_for(entered.wait(), 5)
            replacement = await asyncio.wait_for(
                client.resume_session(session.session_id, **options), 5
            )
            assert client._sessions[session.session_id] is replacement
            assert not release.is_set()
        finally:
            release.set()
            await offer


@pytest.mark.asyncio
@pytest.mark.parametrize("resume", [False, True])
async def test_cancelled_options_patch_keeps_canonical_cleanup_fenced(resume):
    patch_entered = asyncio.Event()
    release_patch = asyncio.Event()
    detach_entered = asyncio.Event()
    release_detach = asyncio.Event()
    detach_finished = asyncio.Event()
    replacement_entered = asyncio.Event()
    premature_replacements = []
    canonical_id = "cancelled-patch-canonical"

    async def on_request(_callback, message):
        method, params = message["method"], message["params"]
        if method in ("session.create", "session.resume"):
            if params.get("requestAutoModeRecovery"):
                replacement_entered.set()
                if not detach_finished.is_set():
                    premature_replacements.append(params)
            return {
                "result": {
                    "sessionId": canonical_id,
                    "capabilities": {"ui": {"autoModeRecovery": True}},
                }
            }
        if method == "session.options.update":
            patch_entered.set()
            await release_patch.wait()
        if method == "session.detach":
            detach_entered.set()
            await release_detach.wait()
            detach_finished.set()

    async with session_fs_peer(on_request=on_request) as (client, callback):
        options = {
            "create_session_fs_handler": lambda _session: object(),
            "on_auto_mode_switch_request": lambda _request, _invocation: "yes",
        }
        first = asyncio.create_task(
            client.resume_session(canonical_id, skip_custom_instructions=True, **options)
            if resume
            else client.create_session(
                session_id=canonical_id, skip_custom_instructions=True, **options
            )
        )
        replacement = None
        try:
            await asyncio.wait_for(patch_entered.wait(), 5)
            first.cancel()
            await asyncio.wait_for(detach_entered.wait(), 5)
            assert not first.done(), "ordinary setup retains its existing cleanup wait"
            # A second cancellation releases the caller, not its owned cleanup task.
            first.cancel()
            with pytest.raises(asyncio.CancelledError):
                await first
            reply = await callback(
                canonical_id,
                "autoModeSwitch.request",
                {"sessionId": canonical_id, "errorCode": "user_weekly_rate_limited"},
            )
            assert reply.get("result") == {"response": "yes"}, reply
            replacement = asyncio.create_task(
                client.resume_session("cancelled-patch", auto_mode_recovery=True, **options)
            )

            async def waiting_or_admitted():
                while not replacement_entered.is_set():
                    lock = client._session_initializations.get("cancelled-patch")
                    if lock is not None and lock.locked():
                        return
                    await asyncio.sleep(0)

            await asyncio.wait_for(waiting_or_admitted(), 5)
            assert not replacement_entered.is_set()
            release_detach.set()
            session = await asyncio.wait_for(replacement, 5)
            assert client._sessions.get(canonical_id) is session
            assert session.session_id == canonical_id
            assert not premature_replacements
        finally:
            release_patch.set()
            release_detach.set()
            await asyncio.gather(first, return_exceptions=True)
            if replacement is not None:
                await asyncio.gather(replacement, return_exceptions=True)


@pytest.mark.asyncio
async def test_initialization_still_serializes_outside_callbacks():
    entered = asyncio.Queue()
    release = asyncio.Queue()

    async def on_create(_callback, params):
        if params["sessionId"] == "serialized":
            entered.put_nowait(None)
            await release.get()

    async with session_fs_peer(on_create) as (client, _callback):

        def create(session_id):
            return client.create_session(
                session_id=session_id,
                on_permission_request=PermissionHandler.approve_all,
                create_session_fs_handler=lambda _session: object(),
            )

        first = asyncio.create_task(create("serialized"))
        await asyncio.wait_for(entered.get(), 5)
        second = asyncio.create_task(create("serialized"))
        try:

            async def wait_for_queued():
                lock = client._session_initializations["serialized"]
                while not lock._waiters:
                    if second.done():
                        pytest.fail(f"Same-ID initialization did not wait: {second.result()!r}")
                    await asyncio.sleep(0)

            await asyncio.wait_for(wait_for_queued(), 5)
            assert entered.empty()
            other = await create("other")
            await other.disconnect()
            release.put_nowait(None)
            first_session = await asyncio.wait_for(first, 5)
            await asyncio.wait_for(entered.get(), 5)
            release.put_nowait(None)
            second_session = await asyncio.wait_for(second, 5)
            await first_session.disconnect()
            await second_session.disconnect()
        finally:
            release.put_nowait(None)
            release.put_nowait(None)
            for task in (first, second):
                if not task.done():
                    task.cancel()
            await asyncio.gather(first, second, return_exceptions=True)


@pytest.mark.asyncio
async def test_independent_same_id_rejects_until_all_callbacks_finish():
    entered = asyncio.Queue()
    releases = asyncio.Queue()
    initialization_entered = asyncio.Event()
    release_initialization = asyncio.Event()
    initializing = False

    async def on_create(_callback, params):
        if initializing and params["sessionId"] == "active":
            initialization_entered.set()
            await release_initialization.wait()

    class Provider:
        async def write_file(self, _path, _content, _mode=None):
            entered.put_nowait(None)
            await releases.get()

    async with session_fs_peer(on_create=on_create) as (client, callback):

        def options():
            return {
                "on_permission_request": PermissionHandler.approve_all,
                "create_session_fs_handler": lambda _session: Provider(),
            }

        session = await client.create_session(session_id="active", **options())
        pending = [asyncio.create_task(callback("active")) for _ in range(2)]
        initialization = None
        try:
            await asyncio.wait_for(entered.get(), 5)
            await asyncio.wait_for(entered.get(), 5)
            initializing = True
            initialization = asyncio.create_task(
                client.create_session(session_id="active", **options())
            )
            await asyncio.wait_for(initialization_entered.wait(), 5)
            for remaining in (2, 1):
                with pytest.raises(RuntimeError, match="active session callback"):
                    await client.create_session(session_id="active", **options())
                with pytest.raises(RuntimeError, match="active session callback"):
                    await client.resume_session("active", **options())
                other = await client.create_session(session_id="other", **options())
                await other.disconnect()
                releases.put_nowait(None)
                done, _ = await asyncio.wait(pending, return_when=asyncio.FIRST_COMPLETED)
                for task in done:
                    assert (await task)["result"] is None
                    pending.remove(task)
                assert len(pending) == remaining - 1
            release_initialization.set()
            await asyncio.wait_for(initialization, 5)
            resumed = await client.resume_session("active", **options())
            await resumed.disconnect()
        finally:
            release_initialization.set()
            for _ in pending:
                releases.put_nowait(None)
            await asyncio.gather(*pending)
            if initialization is not None:
                await asyncio.gather(initialization, return_exceptions=True)
            await session.disconnect()
