"""
CopilotClient Unit Tests

This file is for unit tests. Where relevant, prefer to add e2e tests in e2e/*.py instead.
"""

import asyncio
import inspect
import json
import os
import subprocess
import threading
from concurrent.futures import ThreadPoolExecutor
from dataclasses import fields
from datetime import UTC, datetime
from tempfile import TemporaryDirectory
from unittest.mock import AsyncMock, Mock, patch

import pytest

import copilot.client as client_module
from copilot import (
    CanvasProviderIdentity,
    CapiSessionOptions,
    CopilotClient,
    DisableBypassPermissionsModes,
    ExtensionInfo,
    ModelBillingTokenPrices,
    ModelBillingTokenPricesLongContext,
    ModelSwitchAutoTierStatus,
    PermissionMode,
    RuntimeConnection,
    StdioRuntimeConnection,
    define_tool,
)
from copilot._jsonrpc import JsonRpcClient, JsonRpcError
from copilot.client import (
    CloudSessionOptions,
    CloudSessionRepository,
    CopilotExpAssignmentResponse,
    ExpConfigEntry,
    ManagedSettings,
    ManagedSettingsPermissions,
    ModelBilling,
    ModelCapabilities,
    ModelInfo,
    ModelLimits,
    ModelSupports,
    _InternalTestOptions,
)
from copilot.generated.rpc import AutoTier as AutoTierEnum
from copilot.generated.rpc import (
    DiagnosticLogLevel,
    DiagnosticsConfiguration,
    DiagnosticSourcesConfiguration,
    MCPDiagnosticSourceConfiguration,
    SandboxConfig,
    SessionFSReadFileBytesRequest,
    SessionFSSetProviderCapabilities,
    SessionFSWriteFileBytesRequest,
)
from copilot.session import CopilotSession, PermissionHandler
from copilot.session_events import (
    McpOauthRequestReason,
    McpOauthRequiredData,
    McpOauthRequiredStaticClientConfig,
    McpOauthWWWAuthenticateParams,
    SessionEvent,
    SessionEventType,
)
from copilot.session_fs_provider import SessionFsProvider, create_session_fs_adapter
from copilot.tools import Tool
from e2e.testharness import CLI_PATH


@pytest.mark.parametrize("env", [{}, {"FOO": "bar"}])
def test_inprocess_rejects_env(env, monkeypatch):
    monkeypatch.setattr("copilot._cli_download.ensure_runtime_wrapper", lambda: CLI_PATH)
    with pytest.raises(
        ValueError, match="env is not supported with RuntimeConnection.for_inprocess"
    ):
        CopilotClient(connection=RuntimeConnection.for_inprocess(), env=env)


def test_inprocess_working_directory_rejection_recommends_session_config(monkeypatch):
    monkeypatch.setattr("copilot._cli_download.ensure_runtime_wrapper", lambda: CLI_PATH)
    with pytest.raises(
        ValueError,
        match="working_directory is not supported.*Set working_directory on individual sessions",
    ):
        CopilotClient(connection=RuntimeConnection.for_inprocess(), working_directory="/tmp")


@pytest.mark.parametrize("transport", ["stdio", "inprocess"])
async def test_internal_test_options_override_only_owned_runtime(monkeypatch, tmp_path, transport):
    (tmp_path / "copilot-runtime").touch()
    original = os.environ.copy()
    inherited = {"HTTP_PROXY": "inherited", "UNRELATED": "preserved"}
    options = _InternalTestOptions(
        copilot_api_url="http://replay",
        github_api_url="http://github-replay",
        proxy_url="http://interception",
        no_proxy="",
        ca_certificate_path="/test/ca.pem",
    )
    monkeypatch.setattr(
        "copilot._cli_download.ensure_runtime_wrapper",
        lambda: str(tmp_path / "copilot-runtime"),
    )
    if transport == "inprocess":
        captured = Mock(return_value=Mock())
        monkeypatch.setattr(client_module.FfiRuntimeHost, "create", captured)
        client = CopilotClient(connection=RuntimeConnection.for_inprocess(), _test_options=options)
        await client._start_inprocess_ffi()
        environment = captured.call_args.kwargs["environment"]
    else:
        captured = Mock(return_value=Mock())
        monkeypatch.setattr(client_module.subprocess, "Popen", captured)
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=str(tmp_path / "copilot-runtime")),
            env=inherited,
            _test_options=options,
        )
        await client._start_cli_server()
        environment = captured.call_args.kwargs["env"]
    assert (
        environment.items()
        >= {
            "COPILOT_API_URL": "http://replay",
            "COPILOT_DEBUG_GITHUB_API_URL": "http://github-replay",
            "HTTP_PROXY": "http://interception",
            "HTTPS_PROXY": "http://interception",
            "NO_PROXY": "",
            "NODE_EXTRA_CA_CERTS": "/test/ca.pem",
            "SSL_CERT_FILE": "/test/ca.pem",
            "REQUESTS_CA_BUNDLE": "/test/ca.pem",
            "CURL_CA_BUNDLE": "/test/ca.pem",
            "GIT_SSL_CAINFO": "/test/ca.pem",
        }.items()
    )
    if transport != "inprocess":
        assert environment["UNRELATED"] == "preserved"
    assert inherited == {"HTTP_PROXY": "inherited", "UNRELATED": "preserved"}
    assert os.environ == original
    assert _InternalTestOptions()._to_environment() == {}


@pytest.mark.parametrize("transport", ["stdio", "tcp"])
@pytest.mark.parametrize("source", ["downloaded", "explicit", "environment"])
async def test_only_the_downloaded_runtime_is_started_as_the_executable(
    monkeypatch, tmp_path, transport, source
):
    # Windows will not start a program named only on the command line once its path
    # reaches MAX_PATH; Popen's executable becomes CreateProcess's application name.
    runtime = tmp_path / "copilot-runtime"
    runtime.touch()
    monkeypatch.delenv("COPILOT_CLI_PATH", raising=False)
    monkeypatch.setattr("copilot._cli_download.ensure_runtime_wrapper", lambda: str(runtime))
    if source == "environment":
        monkeypatch.setenv("COPILOT_CLI_PATH", str(runtime))
    path = str(runtime) if source == "explicit" else None
    connection = (
        RuntimeConnection.for_stdio(path=path)
        if transport == "stdio"
        else RuntimeConnection.for_tcp(path=path)
    )
    popen = Mock(side_effect=OSError("spawn stopped"))
    monkeypatch.setattr(client_module.subprocess, "Popen", popen)
    client = CopilotClient(connection=connection)

    with pytest.raises(OSError, match="spawn stopped"):
        await client._start_cli_server()

    assert popen.call_args.args[0][0] == str(runtime)
    expected = str(runtime) if source == "downloaded" else None
    assert popen.call_args.kwargs.get("executable") == expected


def test_internal_test_options_reject_external_runtime():
    with pytest.raises(ValueError, match="requires a runtime owned by this client"):
        CopilotClient(
            connection=RuntimeConnection.for_uri("localhost:8080"),
            _test_options=_InternalTestOptions(),
        )


def test_high_level_configs_omit_legacy_remote_control():
    assert "enable_remote_sessions" not in inspect.signature(CopilotClient).parameters
    assert "remote_session" not in inspect.signature(CopilotClient.create_session).parameters
    assert "remote_session" not in inspect.signature(CopilotClient.resume_session).parameters


def test_session_fs_capabilities_preserve_positional_sqlite():
    capabilities = SessionFSSetProviderCapabilities(True)

    assert capabilities.sqlite is True
    assert capabilities.binary is None
    assert capabilities.read_file_range is None
    assert [field.name for field in fields(capabilities)] == [
        "sqlite",
        "binary",
        "read_file_range",
    ]
    assert capabilities.to_dict() == {"sqlite": True}
    assert SessionFSSetProviderCapabilities.from_dict(
        {"sqlite": True, "binary": False}
    ) == SessionFSSetProviderCapabilities(True, binary=False)


def test_inprocess_connection_has_no_child_process_options():
    connection = RuntimeConnection.for_inprocess()

    assert list(inspect.signature(RuntimeConnection.for_inprocess).parameters) == [
        "run_on_ui_thread"
    ]
    assert not hasattr(connection, "path")
    assert not hasattr(connection, "args")


@pytest.mark.asyncio
@pytest.mark.parametrize("awaitable", [False, True])
@pytest.mark.parametrize("action_id", [0, 42, 0xFFFFFFFF])
async def test_inprocess_ui_thread_rpc_waits_for_scheduler(
    awaitable, action_id, monkeypatch, tmp_path
):
    monkeypatch.setattr(
        "copilot._cli_download.ensure_runtime_wrapper",
        lambda: str(tmp_path / "copilot-runtime"),
    )
    started = asyncio.Event()
    completion = asyncio.get_running_loop().create_future()

    def schedule(action):
        action()
        started.set()
        return completion if awaitable else None

    client = CopilotClient(connection=RuntimeConnection.for_inprocess(run_on_ui_thread=schedule))
    host = Mock()
    client._ffi_host = host
    client._client = Mock(spec=JsonRpcClient)
    handlers = {}
    client._client.set_request_handler.side_effect = (
        lambda method, handler, *, cancellable=False, ordered=False: handlers.__setitem__(
            method, handler
        )
    )
    client._register_client_global_handlers()
    host.configure_ui_thread.assert_called_once_with(True)

    request = asyncio.create_task(handlers["uiThread.run"]({"actionId": action_id}))
    try:
        await started.wait()
        host.run_ui_action.assert_called_once_with(action_id)
        if awaitable:
            assert not request.done(), "UI RPC completed before the scheduler"
            completion.set_result(None)
        assert await request is None
    finally:
        if not completion.done():
            completion.set_result(None)
        await request


@pytest.mark.asyncio
@pytest.mark.parametrize("action_id", [-1, 0x100000000])
async def test_inprocess_ui_thread_rpc_rejects_out_of_range_action_ids(
    action_id, monkeypatch, tmp_path
):
    monkeypatch.setattr(
        "copilot._cli_download.ensure_runtime_wrapper",
        lambda: str(tmp_path / "copilot-runtime"),
    )
    schedule = Mock(side_effect=lambda action: action())
    client = CopilotClient(connection=RuntimeConnection.for_inprocess(run_on_ui_thread=schedule))
    host = Mock()
    client._ffi_host = host
    client._client = Mock(spec=JsonRpcClient)
    handlers = {}
    client._client.set_request_handler.side_effect = (
        lambda method, handler, *, cancellable=False, ordered=False: handlers.__setitem__(
            method, handler
        )
    )
    client._register_client_global_handlers()

    with pytest.raises(ValueError, match="unsigned 32-bit integer"):
        await handlers["uiThread.run"]({"actionId": action_id})
    schedule.assert_not_called()
    host.run_ui_action.assert_not_called()


@pytest.mark.asyncio
async def test_inprocess_forwards_integration_id_to_native_host(
    monkeypatch: pytest.MonkeyPatch, tmp_path
):
    monkeypatch.setenv("GITHUB_COPILOT_INTEGRATION_ID", "  microsoft/vscode  ")
    monkeypatch.setattr(
        "copilot._cli_download.ensure_runtime_wrapper",
        lambda: str(tmp_path / "copilot-runtime"),
    )
    host = Mock()
    host.process = Mock()
    host.start_blocking = Mock()
    create_host = Mock(return_value=host)
    monkeypatch.setattr(client_module.FfiRuntimeHost, "create", create_host)
    client = CopilotClient(connection=RuntimeConnection.for_inprocess())

    await client._start_inprocess_ffi()

    assert (
        "--cli-login-sync-integration-id=microsoft/vscode" in create_host.call_args.kwargs["args"]
    )


@pytest.mark.parametrize("platform", ["linux", "win32"])
async def test_inprocess_scopes_environment_with_platform_casing(monkeypatch, tmp_path, platform):
    monkeypatch.setattr(client_module.sys, "platform", platform)
    monkeypatch.setattr(
        os,
        "environ",
        {
            **os.environ,
            "OLDPWD": "bad\udcff",
            "BAD\udcff": "value",
            "UTF8_ENV": "caf\u00e9",
            "TMPDIR": "bad\udcff",
        },
    )
    monkeypatch.setattr(
        "copilot._cli_download.ensure_runtime_wrapper",
        lambda: str(tmp_path / "copilot-runtime"),
    )
    host = Mock()
    create_host = Mock(return_value=host)
    monkeypatch.setattr(client_module.FfiRuntimeHost, "create", create_host)
    original = os.environ.copy()
    client = CopilotClient(
        connection=RuntimeConnection.for_inprocess(),
        base_directory="typed-home",
        github_token="typed-token",
        _test_options=_InternalTestOptions(proxy_url="http://proxy"),
    )

    await client._start_inprocess_ffi()

    environment = create_host.call_args.kwargs["environment"]
    assert environment["COPILOT_HOME"] == "typed-home"
    assert environment["COPILOT_SDK_AUTH_TOKEN"] == "typed-token"
    assert environment["COPILOT_CACHE_HOME"] == os.path.join("typed-home", "cache")
    assert environment["HTTP_PROXY"] == "http://proxy"
    assert "OLDPWD" not in environment
    assert "BAD\udcff" not in environment
    assert environment["UTF8_ENV"] == "caf\u00e9"
    assert environment["TMPDIR"] == "\ufffd"
    if platform == "win32":
        assert "copilot_home" not in environment
        assert "copilot_sdk_auth_token" not in environment
        assert "http_proxy" not in environment
    else:
        assert environment["http_proxy"] == "http://proxy"
    assert os.environ == original


def test_explicit_child_process_path_does_not_require_runtime_bundle(tmp_path):
    explicit = tmp_path / "copilot"
    connection = RuntimeConnection.for_stdio(path=str(explicit))

    CopilotClient(connection=connection, env={"PATH": str(tmp_path)})

    assert connection.path == str(explicit)


def test_copilot_cli_path_does_not_require_runtime_bundle(tmp_path):
    explicit = tmp_path / "copilot"
    connection = RuntimeConnection.for_stdio()

    CopilotClient(
        connection=connection,
        env={"PATH": str(tmp_path), "COPILOT_CLI_PATH": str(explicit)},
    )

    assert connection.path == str(explicit)


def test_session_metadata_reads_and_writes_client_names():
    wire = {
        "sessionId": "session-1",
        "startTime": "2026-01-01T00:00:00+00:00",
        "modifiedTime": "2026-01-02T00:00:00+00:00",
        "isRemote": False,
    }
    client_names = {"clientName": "github/cli", "lastClientName": "vscode"}
    with_clients = client_module.SessionMetadata.from_dict({**wire, **client_names})
    without_clients = client_module.SessionMetadata.from_dict(wire)

    assert (
        with_clients.client_name,
        with_clients.last_client_name,
        with_clients.to_dict(),
    ) == ("github/cli", "vscode", {**wire, **client_names})
    assert (
        without_clients.client_name,
        without_clients.last_client_name,
        without_clients.to_dict(),
    ) == (None, None, wire)


class TestBuiltinPluginDirectories:
    @staticmethod
    async def _start_client(paths=None):
        client = CopilotClient(
            connection=RuntimeConnection.for_uri("localhost:1234"),
            builtin_plugin_directories=paths,
        )
        client._connect_to_server = AsyncMock()
        client._verify_protocol_version = AsyncMock()
        client._client = Mock()
        client._client.request = AsyncMock(return_value={})

        await client.start()
        return client

    @pytest.mark.asyncio
    @pytest.mark.parametrize("paths", [None, []])
    async def test_default_or_empty_does_not_call_rpc(self, paths):
        client = await self._start_client(paths)

        client._client.request.assert_not_awaited()

    @pytest.mark.asyncio
    async def test_configured_paths_call_rpc_once_before_start_completes(self):
        paths = [
            os.path.abspath("plugins/core"),
            os.path.abspath("plugins/github"),
        ]

        client = await self._start_client(paths)

        client._client.request.assert_awaited_once_with(
            "plugins.builtin.set",
            {"paths": paths},
        )

    def test_relative_path_is_rejected(self):
        with pytest.raises(ValueError, match="builtin_plugin_directories.*absolute paths"):
            CopilotClient(
                connection=RuntimeConnection.for_uri("localhost:1234"),
                builtin_plugin_directories=["plugins/core"],
            )


class TestClientStartup:
    @pytest.mark.asyncio
    async def test_handshake_error_survives_owned_process_cleanup_failure(self):
        from copilot import EntraAuthConfig

        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path="copilot"),
            entra=EntraAuthConfig(client_id="host-entra-client"),
        )
        cleanup_error = subprocess.TimeoutExpired("copilot", 5)
        child = Mock(poll=Mock(return_value=None), wait=Mock(side_effect=cleanup_error))
        rpc = Mock(
            request=AsyncMock(side_effect=JsonRpcError(-32601, "Unhandled method connect")),
            stop=AsyncMock(),
        )

        async def connect():
            client._cli_process = child
            client._process = child
            client._client = rpc

        client._start_cli_server = AsyncMock()
        client._connect_to_server = AsyncMock(side_effect=connect)
        try:
            with pytest.raises(
                RuntimeError, match="does not support Entra client configuration"
            ) as error:
                await client.start()
            assert error.value.__cause__ is cleanup_error
            assert client._state == "error"
            assert client._client is None
            assert client._cli_process is child
            rpc.stop.assert_awaited_once()
        finally:
            child.wait.side_effect = None
            await client.force_stop()

    @pytest.mark.asyncio
    @pytest.mark.parametrize("transport", ["stdio", "tcp"])
    async def test_unsupported_entra_handshake_cleans_up_owned_runtime(self, transport):
        from copilot import EntraAuthConfig

        connection = (
            RuntimeConnection.for_stdio(path="copilot")
            if transport == "stdio"
            else RuntimeConnection.for_tcp(path="copilot")
        )
        client = CopilotClient(
            connection=connection,
            entra=EntraAuthConfig(client_id="host-entra-client"),
        )
        child = Mock(poll=Mock(return_value=None))
        stream = child if transport == "stdio" else Mock()
        rpc = Mock(
            request=AsyncMock(side_effect=JsonRpcError(-32601, "Unhandled method connect")),
            stop=AsyncMock(),
        )

        async def spawn():
            client._cli_process = child

        async def connect():
            client._process = stream
            client._client = rpc
            client._rpc = Mock()

        client._start_cli_server = AsyncMock(side_effect=spawn)
        client._connect_to_server = AsyncMock(side_effect=connect)
        try:
            with pytest.raises(RuntimeError, match="does not support Entra client configuration"):
                await client.start()

            child.kill.assert_called_once()
            if transport == "tcp":
                stream.terminate.assert_called_once()
            rpc.stop.assert_awaited_once()
            assert rpc.request.call_args.args[0] == "connect"
            assert client._cli_process is None
            assert client._process is None
            assert client._client is None
            assert client._rpc is None
            assert client._state == "error"
        finally:
            await client.force_stop()


class TestClientShutdown:
    @pytest.mark.asyncio
    @pytest.mark.parametrize("first_failure", ["create", "resume"])
    async def test_failed_same_id_create_and_resume_preserve_live_resident(self, first_failure):
        client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
        rpc = Mock(stop=AsyncMock())
        client._client = rpc
        client._state = "connected"
        resume_started = asyncio.Event()
        create_started = asyncio.Event()
        send_started = asyncio.Event()
        resume_response = asyncio.get_running_loop().create_future()
        create_response = asyncio.get_running_loop().create_future()
        create_count = 0

        async def mock_request(method, params, **kwargs):
            nonlocal create_count
            if method == "session.create":
                create_count += 1
                if create_count > 1:
                    create_started.set()
                    return await create_response
                result = {"sessionId": params["sessionId"]}
                kwargs["on_response_inline"](result)
                return result
            if method == "session.sendAndWait":
                raise JsonRpcError(-32601, "Method not found")
            if method == "session.send":
                send_started.set()
                return {"messageId": "pending-message"}
            if method == "session.resume":
                resume_started.set()
                return await resume_response
            raise AssertionError(f"unexpected request {method}")

        rpc.request = mock_request
        resident = await client.create_session()
        waiting = asyncio.create_task(resident.send_and_wait("pending response"))
        resuming = creating = None
        try:
            await asyncio.wait_for(send_started.wait(), timeout=5)
            resuming = asyncio.create_task(client.resume_session(resident.session_id))
            await asyncio.wait_for(resume_started.wait(), timeout=5)
            creating = asyncio.create_task(client.create_session(session_id=resident.session_id))
            await asyncio.wait_for(create_started.wait(), timeout=5)
            attempts = {
                "create": (create_response, creating),
                "resume": (resume_response, resuming),
            }
            for name in (first_failure, "resume" if first_failure == "create" else "create"):
                response, task = attempts[name]
                response.set_exception(RuntimeError(f"{name} rejected"))
                with pytest.raises(RuntimeError, match=f"{name} rejected"):
                    await task
            assert client._get_session(resident.session_id) is resident
            resident.on(lambda _: None)
            for index, (event_type, data) in enumerate(
                [
                    (
                        "user.message",
                        {"messageId": "pending-message", "content": "pending response"},
                    ),
                    ("session.idle", {}),
                ]
            ):
                client._handle_notification(
                    "session.event",
                    {
                        "sessionId": resident.session_id,
                        "event": {
                            "id": f"00000000-0000-4000-8000-{index:012d}",
                            "timestamp": "2026-01-01T00:00:00Z",
                            "type": event_type,
                            "data": data,
                            "parentId": None,
                        },
                    },
                )
            assert await asyncio.wait_for(waiting, timeout=5) is None
        finally:
            tasks = [task for task in (resuming, creating, waiting) if task is not None]
            for task in tasks:
                if not task.done():
                    task.cancel()
            await asyncio.gather(*tasks, return_exceptions=True)
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_shutdown_retires_resident_when_recovery_resume_is_unsupported(self):
        client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
        rpc = Mock(stop=AsyncMock())
        client._client = rpc
        client._state = "connected"
        resume_started = asyncio.Event()
        send_started = asyncio.Event()
        detach_started = asyncio.Event()
        release_detach = asyncio.Event()
        resume_response = asyncio.get_running_loop().create_future()

        async def mock_request(method, params, **kwargs):
            if method == "session.create":
                result = {"sessionId": params["sessionId"]}
                kwargs["on_response_inline"](result)
                return result
            if method == "session.sendAndWait":
                raise JsonRpcError(-32601, "Method not found")
            if method == "session.send":
                send_started.set()
                return {"messageId": "pending-message"}
            if method == "session.resume":
                resume_started.set()
                return await resume_response
            if method == "session.detach":
                detach_started.set()
                await release_detach.wait()
                return {"success": True}
            raise AssertionError(f"unexpected request {method}")

        rpc.request = mock_request
        resident = await client.create_session()
        waiting = asyncio.create_task(resident.send_and_wait("pending response"))
        resuming = stopping = None
        try:
            await asyncio.wait_for(send_started.wait(), timeout=5)
            resuming = asyncio.create_task(
                client.resume_session(
                    resident.session_id,
                    auto_mode_recovery=True,
                    on_auto_mode_switch_request=lambda _request, _invocation: "no",
                )
            )
            await asyncio.wait_for(resume_started.wait(), timeout=5)
            stopping = asyncio.create_task(client.stop())
            await asyncio.wait_for(detach_started.wait(), timeout=5)
            resume_response.set_result({"sessionId": resident.session_id})
            with pytest.raises(RuntimeError, match="does not support auto_mode_recovery"):
                await resuming
            release_detach.set()
            await stopping
            with pytest.raises(RuntimeError, match="disconnected"):
                resident.on(lambda _: None)
            with pytest.raises(RuntimeError, match="Session closed"):
                await asyncio.wait_for(waiting, timeout=5)
            assert client._get_session(resident.session_id) is None
        finally:
            release_detach.set()
            tasks = [task for task in (resuming, stopping, waiting) if task is not None]
            for task in tasks:
                if not task.done():
                    task.cancel()
            await asyncio.gather(*tasks, return_exceptions=True)
            await client.force_stop()

    @pytest.mark.asyncio
    @pytest.mark.parametrize("first_failure", [0, 1])
    async def test_overlapping_failed_resumes_restore_live_resident(self, first_failure):
        client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
        rpc = Mock(stop=AsyncMock())
        client._client = rpc
        client._state = "connected"
        responses = [asyncio.get_running_loop().create_future() for _ in range(2)]
        started = [asyncio.Event(), asyncio.Event()]
        send_started = asyncio.Event()
        resume_count = 0

        async def mock_request(method, params, **kwargs):
            nonlocal resume_count
            if method == "session.create":
                result = {"sessionId": params["sessionId"]}
                kwargs["on_response_inline"](result)
                return result
            if method == "session.sendAndWait":
                raise JsonRpcError(-32601, "Method not found")
            if method == "session.send":
                send_started.set()
                return {"messageId": "pending-message"}
            if method == "session.resume":
                index = resume_count
                resume_count += 1
                started[index].set()
                return await responses[index]
            raise AssertionError(f"unexpected request {method}")

        rpc.request = mock_request
        resident = await client.create_session()
        waiting = asyncio.create_task(resident.send_and_wait("pending response"))
        resumes = []
        try:
            await asyncio.wait_for(send_started.wait(), timeout=5)
            for index in range(2):
                resumes.append(asyncio.create_task(client.resume_session(resident.session_id)))
                await asyncio.wait_for(started[index].wait(), timeout=5)
            for index in (first_failure, 1 - first_failure):
                responses[index].set_exception(RuntimeError("resume rejected"))
                with pytest.raises(RuntimeError, match="resume rejected"):
                    await resumes[index]
                resident.on(lambda _: None)
            assert client._get_session(resident.session_id) is resident
            for index, (event_type, data) in enumerate(
                [
                    (
                        "user.message",
                        {"messageId": "pending-message", "content": "pending response"},
                    ),
                    ("session.idle", {}),
                ]
            ):
                client._handle_notification(
                    "session.event",
                    {
                        "sessionId": resident.session_id,
                        "event": {
                            "id": f"00000000-0000-4000-8000-{index:012d}",
                            "timestamp": "2026-01-01T00:00:00Z",
                            "type": event_type,
                            "data": data,
                            "parentId": None,
                        },
                    },
                )
            assert await asyncio.wait_for(waiting, timeout=5) is None
        finally:
            for task in (*resumes, waiting):
                if not task.done():
                    task.cancel()
            await asyncio.gather(*resumes, waiting, return_exceptions=True)
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_failed_resume_and_queued_delete_retire_resident(self):
        client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
        rpc = Mock(stop=AsyncMock())
        client._client = rpc
        client._state = "connected"
        resume_started = asyncio.Event()
        send_started = asyncio.Event()
        resume_response = asyncio.get_running_loop().create_future()

        async def mock_request(method, params, **kwargs):
            if method == "session.create":
                result = {"sessionId": params["sessionId"]}
                kwargs["on_response_inline"](result)
                return result
            if method == "session.sendAndWait":
                raise JsonRpcError(-32601, "Method not found")
            if method == "session.send":
                send_started.set()
                return {"messageId": "pending-message"}
            if method == "session.resume":
                resume_started.set()
                return await resume_response
            if method == "session.delete":
                return {"success": True}
            raise AssertionError(f"unexpected request {method}")

        rpc.request = mock_request
        resident = await client.create_session()
        waiting = asyncio.create_task(resident.send_and_wait("pending response"))
        resuming = None
        deleting = None
        try:
            await asyncio.wait_for(send_started.wait(), timeout=5)
            resuming = asyncio.create_task(client.resume_session(resident.session_id))
            await asyncio.wait_for(resume_started.wait(), timeout=5)
            deleting = asyncio.create_task(client.delete_session(resident.session_id))
            resume_response.set_exception(RuntimeError("resume rejected"))
            with pytest.raises(RuntimeError, match="resume rejected"):
                await resuming
            await deleting

            assert client._get_session(resident.session_id) is None
            with pytest.raises(RuntimeError, match="disconnected"):
                resident.on(lambda _: None)
            with pytest.raises(RuntimeError, match="Session closed"):
                await asyncio.wait_for(waiting, timeout=5)
        finally:
            for task in (resuming, deleting, waiting):
                if task is not None and not task.done():
                    task.cancel()
            await asyncio.gather(
                *(task for task in (resuming, deleting, waiting) if task is not None),
                return_exceptions=True,
            )
            await client.force_stop()

    @pytest.mark.asyncio
    @pytest.mark.parametrize("shutdown", ["stop", "force_stop"])
    @pytest.mark.parametrize("restart", [False, True])
    async def test_shutdown_rejects_cloud_publication_after_session_snapshot(
        self, shutdown, restart
    ):
        client = CopilotClient(
            connection=RuntimeConnection.for_uri("localhost:1234"),
            session_fs={
                "initial_working_directory": "/",
                "session_state_path": "/session-state",
                "conventions": "posix",
            },
        )
        loop = asyncio.get_running_loop()
        rpc = JsonRpcClient(Mock())
        rpc._loop = loop
        request_sent = asyncio.Event()
        provider_started = asyncio.Event()
        transport_stop_started = asyncio.Event()
        release_provider = threading.Event()
        release_transport_stop = asyncio.Event()
        retained_sessions = []
        messages = []

        async def send_message(message):
            messages.append(message)
            request_sent.set()

        async def stop_transport():
            transport_stop_started.set()
            await release_transport_stop.wait()

        def create_provider(session):
            retained_sessions.append(session)
            loop.call_soon_threadsafe(provider_started.set)
            release_provider.wait()
            return Mock(spec=SessionFsProvider)

        rpc._send_message = AsyncMock(side_effect=send_message)
        rpc.stop = stop_transport
        client._client = rpc
        client._state = "connected"
        creation = asyncio.create_task(
            client.create_session(
                cloud=CloudSessionOptions(
                    repository=CloudSessionRepository(
                        owner="github", name="copilot-sdk", branch="main"
                    )
                ),
                create_session_fs_handler=create_provider,
            )
        )
        response = None
        stopping = None
        try:
            await asyncio.wait_for(request_sent.wait(), timeout=5)
            response = asyncio.create_task(
                asyncio.to_thread(
                    rpc._handle_message,
                    {
                        "id": messages[0]["id"],
                        "result": {"sessionId": "server-assigned-session"},
                    },
                )
            )
            await asyncio.wait_for(provider_started.wait(), timeout=5)
            stopping = asyncio.create_task(getattr(client, shutdown)())
            await asyncio.wait_for(transport_stop_started.wait(), timeout=5)
            if restart:
                release_transport_stop.set()
                await asyncio.wait_for(stopping, timeout=5)
                next_rpc = Mock(request=AsyncMock())

                async def connect():
                    client._client = next_rpc

                client._connect_to_server = AsyncMock(side_effect=connect)
                client._verify_protocol_version = AsyncMock()
                client._set_session_fs_provider = AsyncMock()
                await client.start()
                assert client._state == "connected"
            release_provider.set()
            await asyncio.wait_for(response, timeout=5)
            with pytest.raises(RuntimeError, match="not connected"):
                await asyncio.wait_for(creation, timeout=5)
            assert client._sessions == {}
            with pytest.raises(RuntimeError, match="disconnected"):
                retained_sessions[0].on(lambda _: None)
            assert retained_sessions[0]._client_session_apis.session_fs is None
            assert [message["method"] for message in messages] == ["session.create"]
            assert rpc.pending_requests == {}
        finally:
            release_provider.set()
            release_transport_stop.set()
            if response is not None:
                await asyncio.wait_for(response, timeout=5)
            if stopping is not None:
                await asyncio.wait_for(stopping, timeout=5)
            if not creation.done():
                creation.cancel()
                await asyncio.gather(creation, return_exceptions=True)

    @pytest.mark.asyncio
    async def test_stop_requests_runtime_shutdown_for_owned_process(self):
        calls: list[str] = []
        process = Mock()
        process.poll.side_effect = [None, 0]
        process.wait.return_value = 0

        class Runtime:
            async def shutdown(self, *, timeout=None):
                calls.append("runtime.shutdown")

        client = CopilotClient(connection=RuntimeConnection.for_stdio(path="copilot"))
        client._rpc = Mock(runtime=Runtime())
        client._process = process
        client._cli_process = process
        client._is_external_server = False

        await client.stop()

        assert calls == ["runtime.shutdown"]
        process.stdin.close.assert_called_once()
        process.wait.assert_called_once_with(timeout=10)
        process.terminate.assert_not_called()
        process.kill.assert_not_called()

    @pytest.mark.asyncio
    async def test_stop_still_shuts_down_runtime_when_session_detach_never_answers(
        self, monkeypatch
    ):
        calls: list[str] = []
        process = Mock()
        process.poll.side_effect = [None, 0]
        process.wait.return_value = 0

        class Runtime:
            async def shutdown(self, *, timeout=None):
                calls.append("runtime.shutdown")

        async def detach(method, params):
            assert method == "session.detach"
            calls.append(f"session.detach {params['sessionId']}")
            await asyncio.Event().wait()

        monkeypatch.setattr(client_module, "_RUNTIME_SHUTDOWN_TIMEOUT_SECONDS", 0.2)
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path="copilot"))
        client._rpc = Mock(runtime=Runtime())
        client._process = process
        client._cli_process = process
        client._is_external_server = False
        sessions = [
            CopilotSession(session_id, Mock(request=AsyncMock(side_effect=detach)))
            for session_id in ("first", "second")
        ]
        client._sessions = {session.session_id: session for session in sessions}
        tasks = []
        for session in sessions:
            started = asyncio.Event()

            async def blocked(started=started):
                started.set()
                await asyncio.Future()

            task = asyncio.create_task(blocked())
            tasks.append(task)
            session._pending_external_tools["request-1"] = task
            await asyncio.wait_for(started.wait(), timeout=1)

        try:
            with pytest.raises(ExceptionGroup) as raised:
                await asyncio.wait_for(client.stop(), timeout=5)

            assert calls[0] == "session.detach first"
            assert calls[-1] == "runtime.shutdown"
            assert [error.message for error in raised.value.exceptions] == [
                f"Failed to disconnect session {session_id}: session.detach did not complete "
                "within the 0.2s shutdown timeout"
                for session_id in ("first", "second")
            ]
            assert client._sessions == {}
            process.stdin.close.assert_called_once()
            for session in sessions:
                with pytest.raises(RuntimeError, match="disconnected"):
                    session.on(lambda _: None)
            await asyncio.wait_for(asyncio.gather(*tasks, return_exceptions=True), timeout=1)
            assert all(task.cancelled() for task in tasks)
        finally:
            for task in tasks:
                task.cancel()
            await asyncio.gather(*tasks, return_exceptions=True)

    @pytest.mark.asyncio
    async def test_force_stop_and_external_stop_do_not_request_runtime_shutdown(self):
        calls: list[str] = []
        process = Mock()
        process.wait.return_value = 0

        class Runtime:
            async def shutdown(self):
                calls.append("runtime.shutdown")

        force_client = CopilotClient(connection=RuntimeConnection.for_stdio(path="copilot"))
        force_client._rpc = Mock(runtime=Runtime())
        force_client._process = process
        force_client._cli_process = process
        force_client._is_external_server = False

        await force_client.force_stop()

        assert calls == []
        process.kill.assert_called_once()

        external_client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
        external_client._rpc = Mock(runtime=Runtime())
        external_client._is_external_server = True

        await external_client.stop()

        assert calls == []

    @pytest.mark.asyncio
    async def test_force_stop_waits_for_owned_process_exit(self):
        process = Mock()
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path="copilot"))
        client._process = process
        client._cli_process = process
        loop = asyncio.get_running_loop()
        event_loop_thread = threading.current_thread()
        wait_entered = loop.create_future()
        release_exit = threading.Event()

        def wait(*, timeout):
            assert threading.current_thread() is not event_loop_thread
            loop.call_soon_threadsafe(wait_entered.set_result, None)
            release_exit.wait()
            return 0

        process.wait.side_effect = wait
        stopping = asyncio.create_task(client.force_stop())
        try:
            completed, _ = await asyncio.wait(
                [stopping, wait_entered], timeout=5, return_when=asyncio.FIRST_COMPLETED
            )
            assert wait_entered in completed, "force_stop returned before waiting for CLI exit"
            assert not stopping.done(), "force_stop completed while the CLI was still running"
            assert client._process is process
            assert client._cli_process is process

            release_exit.set()
            await asyncio.wait_for(stopping, timeout=5)
            process.kill.assert_called_once()
            process.wait.assert_called_once_with(
                timeout=client_module._CLI_PROCESS_EXIT_TIMEOUT_SECONDS
            )
            assert client._process is None
            assert client._cli_process is None
            assert client._state == "disconnected"

            await client.force_stop()
            process.kill.assert_called_once()
            process.wait.assert_called_once()
        finally:
            release_exit.set()
            await asyncio.gather(stopping, return_exceptions=True)

    @pytest.mark.asyncio
    @pytest.mark.parametrize("release_before_timeout", [True, False])
    async def test_force_stop_bounds_stdin_close_without_blocking_event_loop(
        self, release_before_timeout
    ):
        process = Mock()
        process.args = ["copilot"]
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path="copilot"))
        client._process = process
        client._cli_process = process
        loop = asyncio.get_running_loop()
        event_loop_thread = threading.current_thread()
        close_entered = asyncio.Event()
        wait_entered = asyncio.Event()
        release_close = threading.Event()
        close_finished = threading.Event()

        def close():
            loop.call_soon_threadsafe(close_entered.set)
            try:
                assert threading.current_thread() is not event_loop_thread
                assert release_close.wait(15), "stdin closer was not released"
            finally:
                close_finished.set()

        def wait(*, timeout):
            assert threading.current_thread() is not event_loop_thread
            loop.call_soon_threadsafe(wait_entered.set)
            return 0

        process.stdin.close.side_effect = close
        process.wait.side_effect = wait
        stopping = asyncio.create_task(client.force_stop())
        try:
            await asyncio.wait_for(close_entered.wait(), timeout=5)
            assert not stopping.done(), "force_stop failed before stdin closure was released"
            await asyncio.wait_for(wait_entered.wait(), timeout=5)
            if release_before_timeout:
                release_close.set()
                await asyncio.wait_for(stopping, timeout=5)
                assert client._process is None
                assert client._cli_process is None
            else:
                with pytest.raises(subprocess.TimeoutExpired) as raised:
                    await asyncio.wait_for(
                        stopping, timeout=client_module._CLI_PROCESS_EXIT_TIMEOUT_SECONDS + 5
                    )
                assert raised.value.cmd == process.args
                assert raised.value.timeout == client_module._CLI_PROCESS_EXIT_TIMEOUT_SECONDS
                assert client._process is process
                assert client._cli_process is process
                assert client._state == "disconnected"
            process.kill.assert_called_once()
            process.wait.assert_called_once_with(
                timeout=client_module._CLI_PROCESS_EXIT_TIMEOUT_SECONDS
            )
        finally:
            release_close.set()
            await asyncio.gather(stopping, return_exceptions=True)
            assert await asyncio.to_thread(close_finished.wait, 5), "stdin closer did not finish"
            process.stdin.close.side_effect = None
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_force_stop_preserves_queued_cleanup_after_timeout(self):
        process = Mock()
        process.args = ["copilot"]
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path="copilot"))
        client._process = process
        client._cli_process = process
        loop = asyncio.get_running_loop()
        worker_entered = asyncio.Event()
        stdin_closed = asyncio.Event()
        process_reaped = asyncio.Event()
        release_worker = threading.Event()

        def occupy_worker():
            loop.call_soon_threadsafe(worker_entered.set)
            assert release_worker.wait(15), "executor worker was not released"

        process.stdin.close.side_effect = lambda: loop.call_soon_threadsafe(stdin_closed.set)
        process.wait.side_effect = lambda **_: loop.call_soon_threadsafe(process_reaped.set)
        with ThreadPoolExecutor(max_workers=1) as executor:
            loop.set_default_executor(executor)
            occupied = loop.run_in_executor(None, occupy_worker)
            try:
                await asyncio.wait_for(worker_entered.wait(), timeout=5)
                with pytest.raises(subprocess.TimeoutExpired):
                    await asyncio.wait_for(
                        client.force_stop(),
                        timeout=client_module._CLI_PROCESS_EXIT_TIMEOUT_SECONDS + 5,
                    )
                process.kill.assert_called_once()
                process.stdin.close.assert_not_called()
                process.wait.assert_not_called()

                release_worker.set()
                await asyncio.wait_for(occupied, timeout=5)
                await asyncio.wait_for(stdin_closed.wait(), timeout=5)
                await asyncio.wait_for(process_reaped.wait(), timeout=5)
                process.stdin.close.assert_called_once()
                process.wait.assert_called_once_with(
                    timeout=client_module._CLI_PROCESS_EXIT_TIMEOUT_SECONDS
                )
            finally:
                release_worker.set()
                await occupied
                await client.force_stop()

    @pytest.mark.asyncio
    @pytest.mark.parametrize("failure", ["kill", "wait", "timeout"])
    async def test_force_stop_reports_owned_process_failure(self, failure):
        process = Mock()
        error = (
            subprocess.TimeoutExpired("copilot", client_module._CLI_PROCESS_EXIT_TIMEOUT_SECONDS)
            if failure == "timeout"
            else PermissionError(f"CLI {failure} failed")
        )
        if failure == "kill":
            process.kill.side_effect = error
        else:
            process.wait.side_effect = error
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path="copilot"))
        client._process = process
        client._cli_process = process
        rpc_client = Mock(stop=AsyncMock())
        client._client = rpc_client
        client._rpc = Mock()

        with pytest.raises(type(error)) as raised:
            await client.force_stop()

        assert raised.value is error
        assert client._process is process
        assert client._cli_process is process
        assert client._client is None
        assert client._rpc is None
        assert client._state == "disconnected"
        rpc_client.stop.assert_awaited_once()
        if failure == "kill":
            process.wait.assert_not_called()
        else:
            process.wait.assert_called_once_with(
                timeout=client_module._CLI_PROCESS_EXIT_TIMEOUT_SECONDS
            )

        process.kill.side_effect = None
        process.wait.side_effect = None
        process.wait.return_value = 0
        await client.force_stop()
        assert client._process is None
        assert client._cli_process is None

    @pytest.mark.asyncio
    async def test_force_stop_external_server_clears_process_references(self):
        process = Mock()
        client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
        client._is_external_server = True
        client._process = process
        client._cli_process = process

        await client.force_stop()

        process.terminate.assert_called_once()
        process.kill.assert_not_called()
        process.wait.assert_not_called()
        assert client._process is None
        assert client._cli_process is None

    @pytest.mark.asyncio
    async def test_force_stop_cancels_pending_external_tools(self):
        client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
        session = CopilotSession("session-1", Mock())
        cancelled = asyncio.Event()
        started = asyncio.Event()

        async def blocked():
            started.set()
            try:
                await asyncio.Future()
            finally:
                cancelled.set()

        task = asyncio.create_task(blocked())
        session._pending_external_tools["request-1"] = task
        client._sessions["session-1"] = session
        await asyncio.wait_for(started.wait(), timeout=1)

        await client.force_stop()

        await asyncio.wait_for(cancelled.wait(), timeout=1)
        assert session._destroyed

    @pytest.mark.asyncio
    async def test_connection_close_cancels_pending_external_tools(self):
        client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
        session = CopilotSession("session-1", Mock())
        cancelled = asyncio.Event()
        started = asyncio.Event()

        async def blocked():
            started.set()
            try:
                await asyncio.Future()
            finally:
                cancelled.set()

        task = asyncio.create_task(blocked())
        session._pending_external_tools["request-1"] = task
        client._sessions["session-1"] = session
        await asyncio.wait_for(started.wait(), timeout=1)

        client._client = Mock(_loop=asyncio.get_running_loop())
        await asyncio.to_thread(client._handle_connection_close)

        await asyncio.wait_for(cancelled.wait(), timeout=1)
        assert not session._destroyed
        assert client._sessions == {"session-1": session}

    def test_connection_close_tolerates_event_loop_close_race(self):
        client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
        session = CopilotSession("session-1", Mock())
        loop = Mock()
        loop.is_closed.return_value = False
        loop.call_soon_threadsafe.side_effect = RuntimeError("Event loop is closed")
        client._client = Mock(_loop=loop)
        client._sessions["session-1"] = session
        client._github_token_providers["registration-1"] = Mock()

        client._handle_connection_close()

        assert client._sessions == {"session-1": session}
        assert client._github_token_providers == {}


class TestPermissionHandlerOptional:
    @pytest.mark.asyncio
    async def test_create_session_allows_missing_permission_handler(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            session = await client.create_session()
            assert session.session_id
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_session_allows_none_permission_handler(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            session = await client.create_session(on_permission_request=None)
            assert session.session_id
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_allows_none_permission_handler(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )
            resumed = await client.resume_session(session.session_id, on_permission_request=None)
            assert resumed.session_id == session.session_id
        finally:
            await client.force_stop()


class TestCreateSessionConfig:
    @pytest.mark.asyncio
    async def test_diagnostics_forwarded_and_omitted_on_create_and_cold_resume(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured: list[tuple[str, dict]] = []

            async def mock_request(method, params, **kwargs):
                captured.append((method, params))
                result = {"sessionId": params["sessionId"], "workspacePath": None}
                callback = kwargs.get("on_response_inline")
                if callback is not None:
                    callback(result)
                return result

            client._client.request = mock_request
            await client.create_session(
                session_id="diagnostics-create",
                diagnostics=DiagnosticsConfiguration(
                    sources=DiagnosticSourcesConfiguration(
                        mcp=MCPDiagnosticSourceConfiguration(level=DiagnosticLogLevel.DEBUG)
                    )
                ),
            )
            await client.resume_session(
                "diagnostics-resume",
                diagnostics=DiagnosticsConfiguration(
                    sources=DiagnosticSourcesConfiguration(
                        mcp=MCPDiagnosticSourceConfiguration(level=DiagnosticLogLevel.TRACE)
                    )
                ),
            )
            await client.create_session(session_id="diagnostics-default-create")
            await client.resume_session("diagnostics-default-resume")

            payloads = {(method, params["sessionId"]): params for method, params in captured}
            assert payloads[("session.create", "diagnostics-create")]["diagnostics"] == {
                "sources": {"mcp": {"level": "debug"}}
            }
            assert payloads[("session.resume", "diagnostics-resume")]["diagnostics"] == {
                "sources": {"mcp": {"level": "trace"}}
            }
            assert "diagnostics" not in payloads[("session.create", "diagnostics-default-create")]
            assert "diagnostics" not in payloads[("session.resume", "diagnostics-default-resume")]
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_ask_user_variant_forwarded_on_create_and_cold_resume(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured: list[tuple[str, dict]] = []

            async def mock_request(method, params, **kwargs):
                captured.append((method, params))
                result = {"sessionId": params["sessionId"], "workspacePath": None}
                callback = kwargs.get("on_response_inline")
                if callback is not None:
                    callback(result)
                return result

            client._client.request = mock_request
            await client.create_session(
                session_id="ask-user-create",
                ask_user_variant="elicitation",
            )
            await client.resume_session(
                "ask-user-cold-resume",
                ask_user_variant="legacy",
            )
            await client.create_session(session_id="ask-user-default-create")
            await client.resume_session("ask-user-default-cold-resume")

            payloads = {(method, params["sessionId"]): params for method, params in captured}
            assert (
                payloads[("session.create", "ask-user-create")]["askUserVariant"] == "elicitation"
            )
            assert (
                payloads[("session.resume", "ask-user-cold-resume")]["askUserVariant"] == "legacy"
            )
            assert "askUserVariant" not in payloads[("session.create", "ask-user-default-create")]
            assert (
                "askUserVariant" not in payloads[("session.resume", "ask-user-default-cold-resume")]
            )
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    @pytest.mark.parametrize("method", ["create", "resume"])
    async def test_ask_user_variant_rejects_unknown_values(self, method):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))

        with pytest.raises(ValueError, match="ask_user_variant"):
            if method == "create":
                await client.create_session(ask_user_variant="unknown")  # type: ignore[arg-type]
            else:
                await client.resume_session(
                    "ask-user-cold-resume",
                    ask_user_variant="unknown",  # type: ignore[arg-type]
                )

    @pytest.mark.asyncio
    async def test_additional_directories_forwarded_on_create_and_resume(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured: list[tuple[str, dict]] = []

            async def mock_request(method, params, **kwargs):
                captured.append((method, params))
                if method == "session.create":
                    result = {"sessionId": params["sessionId"], "workspacePath": None}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                if method == "session.resume":
                    return {"sessionId": params["sessionId"], "workspacePath": None}
                return {}

            client._client.request = mock_request
            await client.create_session(
                session_id="create-with-additional-directories",
                additional_directories=["/repo/shared", "/repo/generated"],
            )
            await client.resume_session(
                "resume-with-additional-directories",
                additional_directories=["/repo/resumed"],
            )

            create_payload = next(
                params for method, params in captured if method == "session.create"
            )
            resume_payload = next(
                params for method, params in captured if method == "session.resume"
            )
            assert create_payload["additionalDirectories"] == ["/repo/shared", "/repo/generated"]
            assert resume_payload["additionalDirectories"] == ["/repo/resumed"]
        finally:
            await client.force_stop()

    @staticmethod
    def _mock_session_requests(client, *, detach_success=True):
        async def mock_request(method, params, **kwargs):
            if method in ("session.create", "session.resume"):
                result = {"sessionId": params["sessionId"], "workspacePath": None}
                callback = kwargs.get("on_response_inline")
                if callback is not None:
                    callback(result)
                return result
            if method == "session.detach":
                return {"success": True} if detach_success else {"success": False, "error": "busy"}
            raise AssertionError(f"unexpected request {method}")

        client._client.request = mock_request

    @pytest.mark.asyncio
    async def test_disconnect_stops_tracking_session_but_keeps_resumed_replacement(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            self._mock_session_requests(client)

            created = await client.create_session()
            session_id = created.session_id
            assert client._get_session(session_id) is created
            await created.disconnect()
            assert client._get_session(session_id) is None

            replaced = await client.resume_session(session_id)
            replacement = await client.resume_session(session_id)
            assert client._get_session(session_id) is replacement
            await replaced.disconnect()
            assert client._get_session(session_id) is replacement
            await replacement.disconnect()
            assert client._get_session(session_id) is None
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_failed_resume_does_not_restore_disconnected_session(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        client._client = Mock(stop=AsyncMock())
        client._state = "connected"
        self._mock_session_requests(client)
        try:
            resident = await client.create_session()
            request = client._client.request

            async def fail_resume(method, params, **kwargs):
                if method == "session.resume":
                    await resident.disconnect()
                    raise RuntimeError("resume rejected")
                return await request(method, params, **kwargs)

            client._client.request = fail_resume
            with pytest.raises(RuntimeError, match="resume rejected"):
                await client.resume_session(resident.session_id)
            assert client._get_session(resident.session_id) is None
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    @pytest.mark.parametrize("shutdown", ["stop", "force_stop"])
    @pytest.mark.parametrize("restart", [False, True])
    async def test_failed_resume_retires_displaced_resident_after_shutdown(self, shutdown, restart):
        client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
        rpc = Mock(stop=AsyncMock())
        client._client = rpc
        client._state = "connected"
        self._mock_session_requests(client)
        resume_started = asyncio.Event()
        send_started = asyncio.Event()
        resume_response = asyncio.get_running_loop().create_future()
        request = rpc.request

        async def mock_request(method, params, **kwargs):
            if method == "session.sendAndWait":
                raise JsonRpcError(-32601, "Method not found")
            if method == "session.resume":
                resume_started.set()
                return await resume_response
            if method == "session.send":
                send_started.set()
                return {"messageId": "pending-message"}
            return await request(method, params, **kwargs)

        rpc.request = mock_request
        resident = await client.create_session()
        waiting = asyncio.create_task(resident.send_and_wait("pending response"))
        resuming = None
        try:
            await asyncio.wait_for(send_started.wait(), timeout=5)
            resuming = asyncio.create_task(client.resume_session(resident.session_id))
            await asyncio.wait_for(resume_started.wait(), timeout=5)
            await getattr(client, shutdown)()
            replacement = None
            if restart:
                next_rpc = Mock(stop=AsyncMock())

                async def connect():
                    client._client = next_rpc

                client._connect_to_server = AsyncMock(side_effect=connect)
                client._verify_protocol_version = AsyncMock()
                await client.start()
                self._mock_session_requests(client)
                replacement = await client.create_session(session_id=resident.session_id)

            resume_response.set_exception(RuntimeError("resume rejected"))
            with pytest.raises(RuntimeError, match="resume rejected"):
                await resuming
            with pytest.raises(RuntimeError, match="disconnected"):
                resident.on(lambda _: None)
            with pytest.raises(RuntimeError, match="Session closed"):
                await asyncio.wait_for(waiting, timeout=5)
            assert client._get_session(resident.session_id) is replacement
            if replacement is not None:
                replacement.on(lambda _: None)
        finally:
            for task in (resuming, waiting):
                if task is not None and not task.done():
                    task.cancel()
            await asyncio.gather(
                *(task for task in (resuming, waiting) if task is not None), return_exceptions=True
            )
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_failed_disconnect_keeps_session_tracked(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            self._mock_session_requests(client, detach_success=False)

            session = await client.create_session()
            with pytest.raises(RuntimeError, match="busy"):
                await session.disconnect()
            assert client._get_session(session.session_id) is session
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_mcp_auth_handler_registers_interest_in_create_session(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured: list[tuple[str, dict]] = []

            async def mock_request(method, params, **kwargs):
                captured.append((method, params))
                if method == "session.eventLog.registerInterest":
                    return {"id": "interest-1"}
                if method == "session.create":
                    result = {"sessionId": params["sessionId"], "workspacePath": None}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                on_mcp_auth_request=lambda request: {"kind": "cancelled"},
            )

            create_method, create_payload = captured[0]
            interest_method, interest_payload = captured[1]
            assert create_method == "session.create"
            assert interest_method == "session.eventLog.registerInterest"
            assert interest_payload["eventType"] == "mcp.oauth_required"
            assert interest_payload["sessionId"] == create_payload["sessionId"]
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_mcp_auth_interest_is_not_registered_without_handler(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured: list[tuple[str, dict]] = []

            async def mock_request(method, params, **kwargs):
                captured.append((method, params))
                if method == "session.create":
                    result = {"sessionId": params["sessionId"], "workspacePath": None}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                if method == "session.resume":
                    return {"sessionId": params["sessionId"], "workspacePath": None}
                return {}

            client._client.request = mock_request
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                on_event=lambda event: None,
            )
            await client.resume_session(
                "session-without-auth",
                on_permission_request=PermissionHandler.approve_all,
                on_event=lambda event: None,
            )

            assert session.session_id
            assert not any(
                method == "session.eventLog.registerInterest"
                and params["eventType"] == "mcp.oauth_required"
                for method, params in captured
            )
            assert any(
                method == "session.create" and params["requestPermission"] is True
                for method, params in captured
            )
            assert any(
                method == "session.resume" and params["requestPermission"] is True
                for method, params in captured
            )
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_mcp_auth_handler_registers_interest_after_resume(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured: list[tuple[str, dict]] = []

            async def mock_request(method, params, **kwargs):
                captured.append((method, params))
                if method == "session.eventLog.registerInterest":
                    return {"id": "interest-1"}
                if method == "session.resume":
                    return {"sessionId": params["sessionId"], "workspacePath": None}
                return {}

            client._client.request = mock_request
            await client.resume_session(
                "session-with-auth",
                on_permission_request=PermissionHandler.approve_all,
                on_mcp_auth_request=lambda request: {"kind": "cancelled"},
            )

            resume_method, resume_payload = captured[0]
            interest_method, interest_payload = captured[1]
            assert resume_method == "session.resume"
            assert resume_payload["requestPermission"] is True
            assert interest_method == "session.eventLog.registerInterest"
            assert interest_payload == {
                "sessionId": "session-with-auth",
                "eventType": "mcp.oauth_required",
            }
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_mcp_auth_handler_registers_interest_after_cloud_create_only_with_handler(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured: list[tuple[str, dict]] = []
            create_count = 0

            async def mock_request(method, params, **kwargs):
                nonlocal create_count
                captured.append((method, params))
                if method == "session.eventLog.registerInterest":
                    return {"id": "interest-1"}
                if method == "session.create":
                    create_count += 1
                    result = {
                        "sessionId": f"server-assigned-session-{create_count}",
                        "workspacePath": None,
                    }
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            cloud = CloudSessionOptions(
                repository=CloudSessionRepository(
                    owner="github",
                    name="copilot-sdk",
                    branch="main",
                )
            )

            client._client.request = mock_request
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                cloud=cloud,
            )

            assert not any(
                method == "session.eventLog.registerInterest"
                and params["eventType"] == "mcp.oauth_required"
                for method, params in captured
            )

            captured.clear()
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                on_mcp_auth_request=lambda request: {"kind": "cancelled"},
                cloud=cloud,
            )

            create_method, _create_payload = captured[0]
            interest_method, interest_payload = captured[1]
            assert create_method == "session.create"
            assert interest_method == "session.eventLog.registerInterest"
            assert interest_payload == {
                "sessionId": "server-assigned-session-2",
                "eventType": "mcp.oauth_required",
            }
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    @pytest.mark.parametrize("supported", [True, False, None])
    @pytest.mark.parametrize(
        "response_kind", ["runtimeRefresh", "token", "cancelled", "cancelled_with_token"]
    )
    async def test_mcp_auth_runtime_refresh_response_requires_capability(
        self, supported, response_kind
    ):
        submitted = asyncio.Event()
        captured = []
        client = Mock()

        async def request(method, params, **_kwargs):
            captured.append((method, params))
            submitted.set()
            return {"success": True}

        client.request = AsyncMock(side_effect=request)
        session = CopilotSession("session-1", client)

        def handle_mcp_auth(auth_request, invocation):
            assert invocation == {"sessionId": "session-1"}
            assert auth_request["reason"] == "refresh"
            assert auth_request["runtimeRefreshSupported"] is bool(supported)
            if response_kind == "token":
                # A usable token wins even if a handler also requests a handoff.
                return {"kind": "runtimeRefresh", "accessToken": "host-token"}
            if response_kind == "cancelled_with_token":
                return {"kind": "cancelled", "accessToken": "host-token"}
            return {"kind": response_kind}

        session._register_mcp_auth_handler(handle_mcp_auth)
        session._dispatch_event(
            SessionEvent(
                data=McpOauthRequiredData(
                    request_id="oauth-request",
                    server_name="oauth-server",
                    server_url="https://example.com/mcp",
                    reason=McpOauthRequestReason.REFRESH,
                    runtime_refresh_supported=supported,
                ),
                id="oauth-event",
                timestamp="2026-01-01T00:00:00Z",
                type=SessionEventType.MCP_OAUTH_REQUIRED,
                ephemeral=True,
                parent_id=None,
            )
        )
        await asyncio.wait_for(submitted.wait(), timeout=1)
        assert captured == [
            (
                "session.mcp.oauth.handlePendingRequest",
                {
                    "sessionId": "session-1",
                    "requestId": "oauth-request",
                    "result": (
                        {"kind": "token", "accessToken": "host-token"}
                        if response_kind == "token"
                        else {
                            "kind": (
                                "runtimeRefresh"
                                if response_kind == "runtimeRefresh" and supported
                                else "cancelled"
                            )
                        }
                    ),
                },
            )
        ]

    @pytest.mark.asyncio
    async def test_mcp_auth_required_event_sends_host_token(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured: list[tuple[str, dict]] = []

            async def mock_request(method, params, **kwargs):
                if method == "session.mcp.oauth.handlePendingRequest":
                    captured.append((method, params))
                    request_handled.set()
                    return {"success": True}
                if method == "session.create":
                    result = {"sessionId": params["sessionId"], "workspacePath": None}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                if method == "session.eventLog.registerInterest":
                    return {"id": "interest-1"}
                return {}

            client._client.request = mock_request
            observed_request = None
            request_handled = asyncio.Event()

            def handle_mcp_auth_request(request, invocation):
                nonlocal observed_request
                observed_request = request
                assert invocation == {"sessionId": session.session_id}
                return {
                    "accessToken": "host-token",
                    "tokenType": "Bearer",
                }

            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                on_mcp_auth_request=handle_mcp_auth_request,
            )

            session._dispatch_event(
                SessionEvent(
                    data=McpOauthRequiredData(
                        request_id="oauth-request",
                        server_name="oauth-server",
                        server_url="https://example.com/mcp",
                        reason=McpOauthRequestReason.INITIAL,
                        www_authenticate_params=McpOauthWWWAuthenticateParams(
                            resource_metadata_url="https://example.com/.well-known/oauth-protected-resource"
                        ),
                        resource_metadata='{"resource":"https://example.com/mcp"}',
                        static_client_config=McpOauthRequiredStaticClientConfig(
                            client_id="static-client",
                            client_secret="static-secret",
                            grant_type="client_credentials",
                            public_client=False,
                            scope="configured.read",
                        ),
                    ),
                    id="evt-1",
                    timestamp="2026-01-01T00:00:00Z",
                    type=SessionEventType.MCP_OAUTH_REQUIRED,
                    ephemeral=True,
                    parent_id=None,
                )
            )

            await asyncio.wait_for(request_handled.wait(), timeout=1.0)

            assert observed_request is not None
            assert observed_request["resourceMetadata"] == '{"resource":"https://example.com/mcp"}'
            assert observed_request["wwwAuthenticateParams"]["resourceMetadataUrl"] == (
                "https://example.com/.well-known/oauth-protected-resource"
            )
            assert observed_request["staticClientConfig"] == {
                "clientId": "static-client",
                "clientSecret": "static-secret",
                "grantType": "client_credentials",
                "publicClient": False,
                "scope": "configured.read",
            }
            assert captured == [
                (
                    "session.mcp.oauth.handlePendingRequest",
                    {
                        "sessionId": session.session_id,
                        "requestId": "oauth-request",
                        "result": {
                            "kind": "token",
                            "accessToken": "host-token",
                            "tokenType": "Bearer",
                        },
                    },
                )
            ]

            observed_request = None
            request_handled.clear()
            session._dispatch_event(
                SessionEvent(
                    data=McpOauthRequiredData(
                        request_id="oauth-request-without-metadata",
                        server_name="oauth-server",
                        server_url="https://example.com/mcp",
                        reason=McpOauthRequestReason.INITIAL,
                    ),
                    id="evt-2",
                    timestamp="2026-01-01T00:00:00Z",
                    type=SessionEventType.MCP_OAUTH_REQUIRED,
                    ephemeral=True,
                    parent_id=None,
                )
            )

            await asyncio.wait_for(request_handled.wait(), timeout=1.0)

            assert observed_request is not None
            assert "resourceMetadata" not in observed_request
            assert "wwwAuthenticateParams" not in observed_request
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_session_forwards_cloud_options(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.create":
                    # Cloud sessions: server assigns the id if the client didn't.
                    sid = params.get("sessionId") or "server-assigned-session"
                    result = {"sessionId": sid, "workspacePath": None}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                cloud=CloudSessionOptions(
                    repository=CloudSessionRepository(
                        owner="github",
                        name="copilot-sdk",
                        branch="main",
                    )
                ),
            )

            assert captured["session.create"]["cloud"] == {
                "repository": {
                    "owner": "github",
                    "name": "copilot-sdk",
                    "branch": "main",
                }
            }
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    @pytest.mark.parametrize("cleanup_failure", [False, True])
    async def test_cloud_create_deletes_session_on_binary_provider_validation_failure(
        self, cleanup_failure, caplog
    ):
        client = CopilotClient(
            connection=RuntimeConnection.for_uri("localhost:1234"),
            session_fs={
                "initial_working_directory": "/",
                "session_state_path": "/session-state",
                "conventions": "posix",
                "capabilities": {"binary": True},
            },
        )
        requests = []

        async def mock_request(method, params, **kwargs):
            requests.append((method, params))
            if method == "session.create":
                result = {"sessionId": "server-assigned-session"}
                kwargs["on_response_inline"](result)
                return result
            if cleanup_failure:
                return {"success": False, "error": "cleanup rejected"}
            return {"success": True}

        client._client = Mock(request=mock_request)
        with pytest.raises(ValueError, match="does not implement SessionFsBinaryProvider"):
            await client.create_session(
                cloud=CloudSessionOptions(
                    repository=CloudSessionRepository(
                        owner="github", name="copilot-sdk", branch="main"
                    )
                ),
                create_session_fs_handler=lambda _: Mock(spec=SessionFsProvider),
            )

        assert "sessionId" not in requests[0][1]
        assert requests[-1] == ("session.delete", {"sessionId": "server-assigned-session"})
        assert "server-assigned-session" not in client._sessions
        if cleanup_failure:
            assert "Failed to delete cloud session" in caplog.text

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        "valid_provider,replace_session", [(False, False), (True, False), (True, True)]
    )
    async def test_cloud_create_cancellation_waits_for_provider_validation(
        self, valid_provider, replace_session
    ):
        client = CopilotClient(
            connection=RuntimeConnection.for_uri("localhost:1234"),
            session_fs={
                "initial_working_directory": "/",
                "session_state_path": "/session-state",
                "conventions": "posix",
                "capabilities": {"binary": True},
            },
        )
        loop = asyncio.get_running_loop()
        provider_started = asyncio.Event()
        callback_finished = asyncio.Event()
        delete_called = asyncio.Event()
        release_provider = threading.Event()
        requests = []
        retained_sessions = []

        async def mock_request(method, params, **kwargs):
            requests.append((method, params))
            if method == "session.create":
                result = {"sessionId": "server-assigned-session"}

                def invoke_callback():
                    try:
                        kwargs["on_response_inline"](result)
                    finally:
                        loop.call_soon_threadsafe(callback_finished.set)

                await asyncio.to_thread(invoke_callback)
                return result
            if method == "session.delete":
                delete_called.set()
                return {"success": True}
            return {}

        def create_provider(session):
            retained_sessions.append(session)
            loop.call_soon_threadsafe(provider_started.set)
            release_provider.wait()
            provider = Mock(spec=SessionFsProvider)
            if valid_provider:
                provider.read_file_bytes = AsyncMock(return_value=b"image")
                provider.write_file_bytes = AsyncMock()
            return provider

        client._client = Mock(request=mock_request)
        creation = asyncio.create_task(
            client.create_session(
                cloud=CloudSessionOptions(
                    repository=CloudSessionRepository(
                        owner="github", name="copilot-sdk", branch="main"
                    )
                ),
                create_session_fs_handler=create_provider,
            )
        )
        resumption = None
        try:
            await asyncio.wait_for(provider_started.wait(), timeout=5)
            creation.cancel()
            with pytest.raises(asyncio.CancelledError):
                await asyncio.wait_for(creation, timeout=5)
            assert not delete_called.is_set()

            successor = None
            if replace_session:
                provider = Mock(spec=SessionFsProvider)
                provider.read_file_bytes = AsyncMock(return_value=b"image")
                provider.write_file_bytes = AsyncMock()
                resumption = asyncio.create_task(
                    client.resume_session(
                        "server-assigned-session", create_session_fs_handler=lambda _: provider
                    )
                )
                async with asyncio.timeout(5):
                    while True:
                        if resumption.done():
                            pytest.fail(
                                f"Resume overtook provider validation: {resumption.result()!r}"
                            )
                        with client._sessions_lock:
                            lock = client._session_initializations.get("server-assigned-session")
                            if lock is not None and lock.users >= 2:
                                break
                        await asyncio.sleep(0)
                assert not any(method == "session.resume" for method, _ in requests)

            release_provider.set()
            await asyncio.wait_for(callback_finished.wait(), timeout=5)
            if resumption is not None:
                successor = await asyncio.wait_for(resumption, timeout=5)
            if valid_provider:
                assert not delete_called.is_set()
            else:
                await asyncio.wait_for(delete_called.wait(), timeout=5)
                assert requests[-1] == (
                    "session.delete",
                    {"sessionId": "server-assigned-session"},
                )
            assert client._sessions == (
                {"server-assigned-session": successor} if successor is not None else {}
            )
            if successor is not None:
                successor.on(lambda _: None)()
            with pytest.raises(RuntimeError, match="disconnected"):
                retained_sessions[0].on(lambda _: None)
            assert retained_sessions[0]._client_session_apis.session_fs is None
        finally:
            release_provider.set()
            if not creation.done():
                creation.cancel()
                await asyncio.gather(creation, return_exceptions=True)
            if resumption is not None and not resumption.done():
                resumption.cancel()
                await asyncio.gather(resumption, return_exceptions=True)

    @pytest.mark.asyncio
    @pytest.mark.parametrize("operation", ["create", "resume"])
    async def test_failed_provider_setup_retires_retained_factory_session(self, operation):
        client = CopilotClient(
            connection=RuntimeConnection.for_uri("localhost:1234"),
            session_fs={
                "initial_working_directory": "/",
                "session_state_path": "/session-state",
                "conventions": "posix",
                "capabilities": {"binary": True},
            },
        )
        client._client = Mock(request=AsyncMock())
        original = CopilotSession("session-1", client._client)
        retained_sessions = []

        def create_provider(session):
            retained_sessions.append(session)
            return Mock(spec=SessionFsProvider)

        if operation == "resume":
            client._sessions["session-1"] = original
        with pytest.raises(ValueError, match="does not implement SessionFsBinaryProvider"):
            if operation == "resume":
                await client.resume_session("session-1", create_session_fs_handler=create_provider)
            else:
                await client.create_session(
                    session_id="session-1", create_session_fs_handler=create_provider
                )

        client._client.request.assert_not_awaited()
        assert client._sessions == ({"session-1": original} if operation == "resume" else {})
        original.on(lambda _: None)()
        with pytest.raises(RuntimeError, match="disconnected"):
            retained_sessions[0].on(lambda _: None)

    @pytest.mark.asyncio
    @pytest.mark.parametrize("operation", ["create", "resume"])
    @pytest.mark.parametrize("cancelled", [False, True])
    async def test_failed_session_admission_retires_retained_factory_session(
        self, operation, cancelled
    ):
        client = CopilotClient(
            connection=RuntimeConnection.for_uri("localhost:1234"),
            session_fs={
                "initial_working_directory": "/",
                "session_state_path": "/session-state",
                "conventions": "posix",
            },
        )
        request_started = asyncio.Event()
        release_request = asyncio.Event()
        retained_sessions = []
        requests = []

        async def mock_request(method, params, **kwargs):
            requests.append((method, params))
            request_started.set()
            await release_request.wait()
            raise RuntimeError("admission rejected")

        def create_provider(session):
            retained_sessions.append(session)
            return Mock(spec=SessionFsProvider)

        client._client = Mock(request=mock_request)
        original = CopilotSession("session-1", client._client)
        if operation == "resume":
            client._sessions["session-1"] = original
            admission = asyncio.create_task(
                client.resume_session("session-1", create_session_fs_handler=create_provider)
            )
        else:
            admission = asyncio.create_task(
                client.create_session(
                    session_id="session-1", create_session_fs_handler=create_provider
                )
            )
        try:
            await asyncio.wait_for(request_started.wait(), timeout=5)
            if cancelled:
                admission.cancel()
                with pytest.raises(asyncio.CancelledError):
                    await asyncio.wait_for(admission, timeout=5)
            else:
                release_request.set()
                with pytest.raises(RuntimeError, match="admission rejected"):
                    await asyncio.wait_for(admission, timeout=5)

            assert [method for method, _ in requests] == [f"session.{operation}"]
            assert client._sessions == ({"session-1": original} if operation == "resume" else {})
            original.on(lambda _: None)()
            with pytest.raises(RuntimeError, match="disconnected"):
                retained_sessions[0].on(lambda _: None)
            assert retained_sessions[0]._client_session_apis.session_fs is None
        finally:
            release_request.set()
            if not admission.done():
                admission.cancel()
                await asyncio.gather(admission, return_exceptions=True)

    @pytest.mark.asyncio
    async def test_cloud_create_preserves_session_when_interest_registration_fails(self):
        client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
        requests = []

        async def mock_request(method, params, **kwargs):
            requests.append((method, params))
            if method == "session.create":
                result = {"sessionId": "server-assigned-session"}
                kwargs["on_response_inline"](result)
                return result
            if method == "session.eventLog.registerInterest":
                raise RuntimeError("interest registration failed")
            return {}

        client._client = Mock(request=mock_request)
        with pytest.raises(RuntimeError, match="interest registration failed"):
            await client.create_session(
                cloud=CloudSessionOptions(
                    repository=CloudSessionRepository(
                        owner="github", name="copilot-sdk", branch="main"
                    )
                ),
                on_mcp_auth_request=lambda request: {"kind": "cancelled"},
            )

        assert "sessionId" not in requests[0][1]
        assert (
            "session.eventLog.registerInterest",
            {
                "sessionId": "server-assigned-session",
                "eventType": "mcp.oauth_required",
            },
        ) in requests
        assert not any(method == "session.delete" for method, _ in requests)
        assert "server-assigned-session" not in client._sessions

    @pytest.mark.asyncio
    async def test_create_and_resume_session_forward_github_mcp_tool_config(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request
            config = {
                "enable_all_tools": True,
                "additional_toolsets": ["repos"],
                "additional_tools": ["get_issue"],
                "enable_insiders_mode": True,
                "disable_form_deferral": True,
            }
            session = await client.create_session(github_mcp_tool_config=config)
            await client.resume_session(session.session_id, github_mcp_tool_config=config)

            expected = {
                "enableAllTools": True,
                "additionalToolsets": ["repos"],
                "additionalTools": ["get_issue"],
                "enableInsidersMode": True,
                "disableFormDeferral": True,
            }
            assert captured["session.create"]["githubMcpToolConfig"] == expected
            assert captured["session.resume"]["githubMcpToolConfig"] == expected
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_and_resume_session_forward_reasoning_summary(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                reasoning_summary="concise",
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                reasoning_summary="none",
            )

            assert captured["session.create"]["reasoningSummary"] == "concise"
            assert captured["session.resume"]["reasoningSummary"] == "none"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_and_resume_session_forward_enable_experimental_mode(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                enable_experimental_mode=False,
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                enable_experimental_mode=True,
            )

            assert captured["session.create"]["isExperimentalMode"] is False
            assert captured["session.resume"]["isExperimentalMode"] is True
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_and_resume_session_forward_managed_settings(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                enable_managed_settings=True,
                excluded_managed_settings=["sandbox", "allowedMcpServers"],
                enforce_managed_model_defaults=True,
                interactive_user=True,
                managed_settings=ManagedSettings(
                    permissions=ManagedSettingsPermissions(
                        disable_bypass_permissions_mode=DisableBypassPermissionsModes.ALLOW_AUTO_ONLY,
                        disable_assisted_permissions_mode=True,
                        deny=["Shell(git push)"],
                        ask=["Domain(publish.example)"],
                        allow=["Read(**)"],
                        limit_to=["Domain(github.com)"],
                    )
                ),
            )
            resumed_session = await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                excluded_managed_settings=["deniedMcpServers"],
                enforce_managed_model_defaults=True,
                interactive_user=True,
                managed_settings=ManagedSettings(
                    permissions=ManagedSettingsPermissions(
                        disable_bypass_permissions_mode="future-fail-closed-mode",
                        ask=["Domain(publish.example)"],
                    )
                ),
            )

            assert session._managed_settings_enabled is True
            assert resumed_session._managed_settings_enabled is True
            assert captured["session.create"]["enableManagedSettings"] is True
            assert captured["session.create"]["excludedManagedSettings"] == [
                "sandbox",
                "allowedMcpServers",
            ]
            assert captured["session.resume"]["excludedManagedSettings"] == ["deniedMcpServers"]
            assert captured["session.create"]["enforceManagedModelDefaults"] is True
            assert captured["session.resume"]["enforceManagedModelDefaults"] is True
            assert captured["session.create"]["interactiveUser"] is True
            assert captured["session.resume"]["interactiveUser"] is True
            assert captured["session.create"]["managedSettings"] == {
                "permissions": {
                    "disableBypassPermissionsMode": "allow-auto-only",
                    "disableAssistedPermissionsMode": True,
                    "deny": ["Shell(git push)"],
                    "ask": ["Domain(publish.example)"],
                    "allow": ["Read(**)"],
                    "limitTo": ["Domain(github.com)"],
                }
            }
            assert captured["session.resume"]["managedSettings"] == {
                "permissions": {
                    "disableBypassPermissionsMode": "future-fail-closed-mode",
                    "ask": ["Domain(publish.example)"],
                }
            }

            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                managed_settings=ManagedSettings(
                    permissions=ManagedSettingsPermissions(
                        disable_bypass_permissions_mode=DisableBypassPermissionsModes.DISABLE,
                    )
                ),
            )
            assert captured["session.create"]["managedSettings"] == {
                "permissions": {
                    "disableBypassPermissionsMode": "disable",
                }
            }
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_and_resume_session_forward_explicit_interactive_user_false(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                interactive_user=False,
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                interactive_user=False,
            )

            assert captured["session.create"]["interactiveUser"] is False
            assert captured["session.resume"]["interactiveUser"] is False
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    @pytest.mark.parametrize("permission_mode", [None, *PermissionMode])
    async def test_create_and_resume_session_forward_permission_mode(self, permission_mode):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    if "permissionMode" in params:
                        # A refused result proves the SDK surfaces the outcome,
                        # not the request.
                        result["permissionModeResult"] = {"success": False, "mode": "manual"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                permission_mode=permission_mode,
            )
            resumed = await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                permission_mode=permission_mode,
            )

            for method in ("session.create", "session.resume"):
                if permission_mode is None:
                    assert "permissionMode" not in captured[method]
                else:
                    assert captured[method]["permissionMode"] == permission_mode.value
            for opened in (session, resumed):
                if permission_mode is None:
                    assert opened.permission_mode_result is None
                else:
                    assert opened.permission_mode_result is not None
                    assert opened.permission_mode_result.success is False
                    assert opened.permission_mode_result.mode == PermissionMode.MANUAL
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_and_resume_session_default_enable_experimental_mode_by_mode(self):
        with TemporaryDirectory() as base_directory:
            client = CopilotClient(
                connection=RuntimeConnection.for_stdio(path=CLI_PATH),
                mode="empty",
                base_directory=base_directory,
            )
            await client.start()
            try:
                captured = {}

                async def mock_request(method, params, **kwargs):
                    captured[method] = params
                    if method in ("session.create", "session.resume"):
                        result = {"sessionId": params.get("sessionId") or "session-1"}
                        callback = kwargs.get("on_response_inline")
                        if callback is not None:
                            callback(result)
                        return result
                    if method == "session.options.update":
                        return {"success": True}
                    return {}

                client._client.request = mock_request
                session = await client.create_session(
                    on_permission_request=PermissionHandler.approve_all,
                    available_tools=[],
                )
                await client.resume_session(
                    session.session_id,
                    on_permission_request=PermissionHandler.approve_all,
                    available_tools=[],
                )

                assert captured["session.create"]["isExperimentalMode"] is False
                assert captured["session.resume"]["isExperimentalMode"] is False
                assert "interactiveUser" not in captured["session.create"]
                assert "interactiveUser" not in captured["session.resume"]
            finally:
                await client.force_stop()

    async def test_managed_settings_omitted_when_not_supplied(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.create":
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )

            assert "managedSettings" not in captured["session.create"]
            assert "interactiveUser" not in captured["session.create"]
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_managed_settings_preserves_empty_arrays(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.create":
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                managed_settings=ManagedSettings(
                    permissions=ManagedSettingsPermissions(deny=[], ask=[], allow=[], limit_to=[])
                ),
            )

            assert captured["session.create"]["managedSettings"] == {
                "permissions": {"deny": [], "ask": [], "allow": [], "limitTo": []}
            }
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_and_resume_session_forward_context_tier(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                context_tier="long_context",
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                context_tier="default",
            )

            assert captured["session.create"]["contextTier"] == "long_context"
            assert captured["session.resume"]["contextTier"] == "default"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_and_resume_session_forward_tool_metadata(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request
            metadata = {"github.com/copilot:safeForTelemetry": {"name": True, "inputsNames": False}}
            tool = Tool(name="my_tool", description="a tool", metadata=metadata)
            plain_tool = Tool(name="plain_tool", description="a tool")

            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                tools=[tool, plain_tool],
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                tools=[tool],
            )

            create_tools = captured["session.create"]["tools"]
            assert create_tools[0]["metadata"] == metadata
            # Omitted when unset.
            assert "metadata" not in create_tools[1]
            assert captured["session.resume"]["tools"][0]["metadata"] == metadata
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_and_resume_session_forward_tool_is_terminal(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request
            tool = Tool(name="my_tool", description="a tool", is_terminal=True)
            plain_tool = Tool(name="plain_tool", description="a tool")

            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                tools=[tool, plain_tool],
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                tools=[tool],
            )

            create_tools = captured["session.create"]["tools"]
            assert create_tools[0]["isTerminal"] is True
            # Omitted when left at its default.
            assert "isTerminal" not in create_tools[1]
            assert captured["session.resume"]["tools"][0]["isTerminal"] is True
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_and_resume_session_forward_canvas_provider(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                extension_info=ExtensionInfo(source="github-app", name="counter"),
                canvas_provider=CanvasProviderIdentity(id="app:builtin:window-1", name="Built-in"),
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                canvas_provider=CanvasProviderIdentity(id="app:builtin:window-1"),
            )

            assert captured["session.create"]["canvasProvider"] == {
                "id": "app:builtin:window-1",
                "name": "Built-in",
            }
            assert captured["session.create"]["extensionInfo"] == {
                "source": "github-app",
                "name": "counter",
            }
            assert captured["session.resume"]["canvasProvider"] == {
                "id": "app:builtin:window-1",
            }
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_and_resume_session_forward_new_session_options(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                enable_citations=True,
                enable_file_change_tracking=True,
                excluded_builtin_agents=["explore"],
                session_limits={"max_ai_credits": 30},
                autopilot={"continuation_enabled": False, "max_continues": 0},
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                enable_citations=False,
                enable_file_change_tracking=False,
                excluded_builtin_agents=["task"],
                session_limits={"max_ai_credits": 15},
                autopilot={"max_continues": 2},
            )

            assert captured["session.create"]["enableCitations"] is True
            assert captured["session.create"]["enableFileChangeTracking"] is True
            assert captured["session.create"]["excludedBuiltinAgents"] == ["explore"]
            assert captured["session.create"]["sessionLimits"] == {"maxAiCredits": 30}
            assert captured["session.create"]["autopilot"] == {
                "continuationEnabled": False,
                "maxContinues": 0,
            }
            assert captured["session.resume"]["enableCitations"] is False
            assert captured["session.resume"]["enableFileChangeTracking"] is False
            assert captured["session.resume"]["excludedBuiltinAgents"] == ["task"]
            assert captured["session.resume"]["sessionLimits"] == {"maxAiCredits": 15}
            assert captured["session.resume"]["autopilot"] == {"maxContinues": 2}
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        ("create_capi", "resume_capi", "expected_create", "expected_resume"),
        [
            (None, None, None, None),
            ({}, {}, {}, {}),
            (
                {"enable_web_socket_responses": False},
                {"enable_web_socket_responses": True},
                {"enableWebSocketResponses": False},
                {"enableWebSocketResponses": True},
            ),
            (
                {"enable_web_socket_responses": True},
                {"enable_web_socket_responses": False},
                {"enableWebSocketResponses": True},
                {"enableWebSocketResponses": False},
            ),
            (
                {"auto_tier": "efficiency"},
                {"auto_tier": "efficiency"},
                {"autoTier": "efficiency"},
                {"autoTier": "efficiency"},
            ),
            (
                {"auto_tier": "balance"},
                {"auto_tier": "balance"},
                {"autoTier": "balance"},
                {"autoTier": "balance"},
            ),
            (
                {"auto_tier": "intelligence"},
                {"auto_tier": "intelligence"},
                {"autoTier": "intelligence"},
                {"autoTier": "intelligence"},
            ),
            (
                {"auto_tier": "fast"},
                {"auto_tier": "fast"},
                {"autoTier": "fast"},
                {"autoTier": "fast"},
            ),
            (
                {"auto_tier": "balance", "enable_web_socket_responses": False},
                {"auto_tier": "balance", "enable_web_socket_responses": True},
                {"autoTier": "balance", "enableWebSocketResponses": False},
                {"autoTier": "balance", "enableWebSocketResponses": True},
            ),
        ],
    )
    async def test_create_and_resume_session_forward_capi_options(
        self,
        create_capi: CapiSessionOptions | None,
        resume_capi: CapiSessionOptions | None,
        expected_create: dict[str, object] | None,
        expected_resume: dict[str, object] | None,
    ):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                model="auto",
                capi=create_capi,
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                capi=resume_capi,
            )

            for method, expected in (
                ("session.create", expected_create),
                ("session.resume", expected_resume),
            ):
                if expected is None:
                    assert "capi" not in captured[method]
                else:
                    assert captured[method]["capi"] == expected
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_and_resume_session_forward_plugin_directories_and_large_output(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request

            plugin_dirs = ["/tmp/plugins/a", "/tmp/plugins/b"]
            disabled_mcp_servers = ["local-files", "remote-github"]
            sandbox_config = SandboxConfig(
                enabled=True, add_current_working_directory=False, sandbox_mcp_servers=True
            )
            expected_sandbox_config_wire = {
                "enabled": True,
                "addCurrentWorkingDirectory": False,
                "sandboxMcpServers": True,
            }
            large_output = {
                "enabled": True,
                "max_size_bytes": 1024,
                "output_directory": "/tmp/large-output",
            }
            expected_large_output_wire = {
                "enabled": True,
                "maxSizeBytes": 1024,
                "outputDir": "/tmp/large-output",
            }

            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                plugin_directories=plugin_dirs,
                disabled_mcp_servers=disabled_mcp_servers,
                sandbox_config=sandbox_config,
                large_output=large_output,
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                plugin_directories=plugin_dirs,
                disabled_mcp_servers=disabled_mcp_servers,
                sandbox_config=sandbox_config,
                large_output=large_output,
            )

            assert captured["session.create"]["pluginDirectories"] == plugin_dirs
            assert captured["session.create"]["disabledMcpServers"] == disabled_mcp_servers
            assert captured["session.create"]["sandboxConfig"] == expected_sandbox_config_wire
            assert captured["session.create"]["largeOutput"] == expected_large_output_wire
            assert captured["session.resume"]["pluginDirectories"] == plugin_dirs
            assert captured["session.resume"]["disabledMcpServers"] == disabled_mcp_servers
            assert captured["session.resume"]["sandboxConfig"] == expected_sandbox_config_wire
            assert captured["session.resume"]["largeOutput"] == expected_large_output_wire

            empty_session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                disabled_mcp_servers=[],
                sandbox_config=SandboxConfig(enabled=False),
            )
            await client.resume_session(
                empty_session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                disabled_mcp_servers=[],
                sandbox_config=SandboxConfig(enabled=False),
            )
            assert captured["session.create"]["disabledMcpServers"] == []
            assert captured["session.resume"]["disabledMcpServers"] == []
            assert captured["session.create"]["sandboxConfig"] == {"enabled": False}
            assert captured["session.resume"]["sandboxConfig"] == {"enabled": False}

            omitted_session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            await client.resume_session(
                omitted_session.session_id,
                on_permission_request=PermissionHandler.approve_all,
            )
            assert "disabledMcpServers" not in captured["session.create"]
            assert "disabledMcpServers" not in captured["session.resume"]
            assert "sandboxConfig" not in captured["session.create"]
            assert "sandboxConfig" not in captured["session.resume"]
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_and_resume_session_forward_explicit_empty_custom_agents(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request

            # An explicitly empty list must reach the runtime: on resume it replaces
            # the session's custom agents with none, which omitting the key cannot express.
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                custom_agents=[],
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                custom_agents=[],
            )
            assert captured["session.create"]["customAgents"] == []
            assert captured["session.resume"]["customAgents"] == []

            omitted_session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            await client.resume_session(
                omitted_session.session_id,
                on_permission_request=PermissionHandler.approve_all,
            )
            assert "customAgents" not in captured["session.create"]
            assert "customAgents" not in captured["session.resume"]
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_and_resume_session_forward_explicit_empty_mcp_servers(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request

            # An explicitly empty map must reach the runtime: on resume it replaces
            # the session's servers with none, which omitting the key cannot express.
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                mcp_servers={},
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                mcp_servers={},
            )
            assert captured["session.create"]["mcpServers"] == {}
            assert captured["session.resume"]["mcpServers"] == {}

            omitted_session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            await client.resume_session(
                omitted_session.session_id,
                on_permission_request=PermissionHandler.approve_all,
            )
            assert "mcpServers" not in captured["session.create"]
            assert "mcpServers" not in captured["session.resume"]
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_and_resume_session_forward_explicit_empty_tools(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request

            # An explicitly empty list must reach the runtime: on resume it replaces
            # the session's external tools with none, which omitting the key cannot
            # express.
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                tools=[],
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                tools=[],
            )
            assert captured["session.create"]["tools"] == []
            assert captured["session.resume"]["tools"] == []

            omitted_session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            await client.resume_session(
                omitted_session.session_id,
                on_permission_request=PermissionHandler.approve_all,
            )
            assert "tools" not in captured["session.create"]
            assert "tools" not in captured["session.resume"]
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_and_resume_session_forward_explicit_empty_commands(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request

            # An explicitly empty list must reach the runtime: on resume it replaces
            # the session's slash commands with none, which omitting the key cannot
            # express.
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                commands=[],
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                commands=[],
            )
            assert captured["session.create"]["commands"] == []
            assert captured["session.resume"]["commands"] == []

            omitted_session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            await client.resume_session(
                omitted_session.session_id,
                on_permission_request=PermissionHandler.approve_all,
            )
            assert "commands" not in captured["session.create"]
            assert "commands" not in captured["session.resume"]
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_and_resume_session_forward_memory(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request

            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                memory={"enabled": True},
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                memory={"enabled": False},
            )

            assert captured["session.create"]["memory"] == {"enabled": True}
            assert captured["session.resume"]["memory"] == {"enabled": False}
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_and_resume_session_omit_memory_when_unset(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request

            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
            )

            assert "memory" not in captured["session.create"]
            assert "memory" not in captured["session.resume"]
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_and_resume_session_forward_exp_assignments(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request

            create_assignments = CopilotExpAssignmentResponse(
                configs=[ExpConfigEntry(id="exp-create")]
            )
            resume_assignments = CopilotExpAssignmentResponse(
                configs=[ExpConfigEntry(id="exp-resume")]
            )

            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                exp_assignments=create_assignments,
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                exp_assignments=resume_assignments,
            )

            assert captured["session.create"]["expAssignments"] == {
                "Features": [],
                "Flights": {},
                "Configs": [{"Id": "exp-create", "Parameters": {}}],
                "AssignmentContext": "",
            }
            assert captured["session.resume"]["expAssignments"] == {
                "Features": [],
                "Flights": {},
                "Configs": [{"Id": "exp-resume", "Parameters": {}}],
                "AssignmentContext": "",
            }
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_and_resume_session_omit_exp_assignments_when_unset(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request

            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
            )

            assert "expAssignments" not in captured["session.create"]
            assert "expAssignments" not in captured["session.resume"]
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_and_resume_session_forward_feature_flags(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method in ("session.create", "session.resume"):
                    result = {"sessionId": params.get("sessionId") or "session-1"}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request
            feature_flags = {"ENABLED_TEST_FLAG": True, "DISABLED_TEST_FLAG": False}

            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                feature_flags=feature_flags,
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                feature_flags=feature_flags,
            )

            assert captured["session.create"]["featureFlags"] == feature_flags
            assert captured["session.resume"]["featureFlags"] == feature_flags
        finally:
            await client.force_stop()


class TestSessionEventRouting:
    @pytest.mark.asyncio
    @pytest.mark.parametrize("transport", ["stdio", "tcp"])
    async def test_session_event_notification_looks_up_session_under_lock(self, transport):
        class LockRecordingSessions(dict):
            def __init__(self, lock: threading.Lock):
                super().__init__()
                self.lock = lock
                self.lock_held_on_get: list[bool] = []

            def get(self, key, default=None):
                self.lock_held_on_get.append(self.lock.locked())
                return super().get(key, default)

        if transport == "stdio":
            client = CopilotClient()
            client._process = Mock()
            connect = client._connect_via_stdio
        else:
            client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:9000"))
            connect = client._connect_via_tcp

        fake_socket = Mock()
        with (
            patch.object(client_module, "JsonRpcClient") as rpc_client_class,
            patch("socket.create_connection", return_value=fake_socket),
        ):
            await connect()
        handler = rpc_client_class.return_value.set_notification_handler.call_args.args[0]

        sessions = LockRecordingSessions(client._sessions_lock)
        session = Mock()
        sessions["s1"] = session
        client._sessions = sessions

        handler(
            "session.event",
            {
                "sessionId": "s1",
                "event": {
                    "id": "11111111-1111-1111-1111-111111111111",
                    "timestamp": "2026-09-18T22:00:00+00:00",
                    "type": "session.idle",
                    "data": {},
                },
            },
        )

        assert sessions.lock_held_on_get == [True]
        session._dispatch_event.assert_called_once()
        assert session._dispatch_event.call_args.args[0].type.value == "session.idle"


class TestSessionFsConfig:
    @pytest.mark.asyncio
    async def test_binary_write_preserves_bytes_and_rejects_invalid_base64(self):
        provider = Mock(spec=SessionFsProvider)
        provider.read_file_bytes = AsyncMock(return_value=b"\x00\xff")
        provider.write_file_bytes = AsyncMock()
        adapter = create_session_fs_adapter(provider)
        request = SessionFSWriteFileBytesRequest(
            session_id="session", path="/image.png", content="AP/+AQ==", mode=0o600
        )
        assert await adapter.write_file_bytes(request) is None
        provider.write_file_bytes.assert_awaited_once_with("/image.png", b"\x00\xff\xfe\x01", 0o600)
        for content in ("AA==AAAA", "AA==\n", "A" * (64 * 1024 * 1024)):
            error = await adapter.write_file_bytes(
                SessionFSWriteFileBytesRequest(
                    session_id="session", path="/image.png", content=content
                )
            )
            assert error is not None
            assert error.code.value == "UNKNOWN"
        provider.write_file_bytes.assert_awaited_once()

    @pytest.mark.asyncio
    async def test_binary_read_rejects_oversized_provider_bytes(self):
        provider = Mock(spec=SessionFsProvider)
        provider.read_file_bytes = AsyncMock(
            return_value=bytes((64 * 1024 * 1024 - 1024) // 4 * 3 + 1)
        )
        provider.write_file_bytes = AsyncMock()
        result = await create_session_fs_adapter(provider).read_file_bytes(
            SessionFSReadFileBytesRequest(session_id="session", path="/image.png")
        )

        provider.read_file_bytes.assert_awaited_once_with("/image.png")
        assert result.content == ""
        assert result.error is not None
        assert result.error.code.value == "UNKNOWN"
        assert (
            result.error.message == "sessionFs.readFileBytes content exceeds the binary read limit"
        )

    def test_missing_initial_cwd(self):
        with pytest.raises(ValueError, match="session_fs.initial_working_directory is required"):
            CopilotClient(
                connection=RuntimeConnection.for_stdio(path=CLI_PATH),
                log_level="error",
                session_fs={
                    "initial_working_directory": "",
                    "session_state_path": "/session-state",
                    "conventions": "posix",
                },
            )

    def test_missing_session_state_path(self):
        with pytest.raises(ValueError, match="session_fs.session_state_path is required"):
            CopilotClient(
                connection=RuntimeConnection.for_stdio(path=CLI_PATH),
                log_level="error",
                session_fs={
                    "initial_working_directory": "/",
                    "session_state_path": "",
                    "conventions": "posix",
                },
            )


class TestAuthOptions:
    def test_accepts_github_token(self):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            github_token="gho_test_token",
            log_level="error",
        )
        assert isinstance(client._options.connection, StdioRuntimeConnection)
        assert client._options.github_token == "gho_test_token"

    def test_default_use_logged_in_user_true_without_token(self):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH), log_level="error"
        )
        assert isinstance(client._options.connection, StdioRuntimeConnection)
        assert client._options.use_logged_in_user is True

    def test_default_use_logged_in_user_false_with_token(self):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            github_token="gho_test_token",
            log_level="error",
        )
        assert isinstance(client._options.connection, StdioRuntimeConnection)
        assert client._options.use_logged_in_user is False

    def test_explicit_use_logged_in_user_true_with_token(self):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            github_token="gho_test_token",
            use_logged_in_user=True,
            log_level="error",
        )
        assert isinstance(client._options.connection, StdioRuntimeConnection)
        assert client._options.use_logged_in_user is True

    def test_explicit_use_logged_in_user_false_without_token(self):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            use_logged_in_user=False,
            log_level="error",
        )
        assert isinstance(client._options.connection, StdioRuntimeConnection)
        assert client._options.use_logged_in_user is False


class TestSessionIdleTimeoutSeconds:
    def test_accepts_session_idle_timeout_seconds(self):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            session_idle_timeout_seconds=600,
            log_level="error",
        )
        assert isinstance(client._options.connection, StdioRuntimeConnection)
        assert client._options.session_idle_timeout_seconds == 600

    def test_default_session_idle_timeout_seconds_is_none(self):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH), log_level="error"
        )
        assert isinstance(client._options.connection, StdioRuntimeConnection)
        assert client._options.session_idle_timeout_seconds is None


class TestCopilotHome:
    def test_accepts_copilot_home(self):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            base_directory="/custom/copilot/home",
            log_level="error",
        )
        assert isinstance(client._options.connection, StdioRuntimeConnection)
        assert client._options.base_directory == "/custom/copilot/home"

    def test_default_copilot_home_is_none(self):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH), log_level="error"
        )
        assert isinstance(client._options.connection, StdioRuntimeConnection)
        assert client._options.base_directory is None


class TestOverridesBuiltInTool:
    @pytest.mark.asyncio
    async def test_overrides_built_in_tool_sent_in_tool_definition(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request

            @define_tool(description="Custom grep", overrides_built_in_tool=True)
            def grep(params) -> str:
                return "ok"

            await client.create_session(
                on_permission_request=PermissionHandler.approve_all, tools=[grep]
            )
            tool_defs = captured["session.create"]["tools"]
            assert len(tool_defs) == 1
            assert tool_defs[0]["name"] == "grep"
            assert tool_defs[0]["overridesBuiltInTool"] is True
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_sends_overrides_built_in_tool(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )

            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                # Return a fake response instead of calling the real CLI,
                # which would fail without auth credentials.
                return {"sessionId": params["sessionId"]}

            client._client.request = mock_request

            @define_tool(description="Custom grep", overrides_built_in_tool=True)
            def grep(params) -> str:
                return "ok"

            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                tools=[grep],
            )
            tool_defs = captured["session.resume"]["tools"]
            assert len(tool_defs) == 1
            assert tool_defs[0]["overridesBuiltInTool"] is True
        finally:
            await client.force_stop()


class TestDefer:
    @pytest.mark.asyncio
    async def test_defer_sent_in_tool_definition(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request

            @define_tool(description="Fetch issue details", defer="auto")
            def lookup_issue(params) -> str:
                return "ok"

            await client.create_session(
                on_permission_request=PermissionHandler.approve_all, tools=[lookup_issue]
            )
            tool_defs = captured["session.create"]["tools"]
            assert len(tool_defs) == 1
            assert tool_defs[0]["name"] == "lookup_issue"
            assert tool_defs[0]["defer"] == "auto"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_sends_defer(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )

            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                return {"sessionId": params["sessionId"]}

            client._client.request = mock_request

            @define_tool(description="Fetch issue details", defer="auto")
            def lookup_issue(params) -> str:
                return "ok"

            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                tools=[lookup_issue],
            )
            tool_defs = captured["session.resume"]["tools"]
            assert len(tool_defs) == 1
            assert tool_defs[0]["defer"] == "auto"
        finally:
            await client.force_stop()


class TestInstructionDirectories:
    @pytest.mark.asyncio
    async def test_create_session_sends_instruction_directories(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.create":
                    sid = params.get("sessionId") or "session-id"
                    result = {"sessionId": sid, "workspacePath": None}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return {}

            client._client.request = mock_request

            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                instruction_directories=["C:\\extra-instructions", "C:\\more-instructions"],
            )

            assert captured["session.create"]["instructionDirectories"] == [
                "C:\\extra-instructions",
                "C:\\more-instructions",
            ]
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_sends_instruction_directories(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            captured = {}

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.resume":
                    return {"sessionId": params["sessionId"], "workspacePath": None}
                return {}

            client._client.request = mock_request

            await client.resume_session(
                "session-id",
                on_permission_request=PermissionHandler.approve_all,
                instruction_directories=["C:\\resume-instructions"],
            )

            assert captured["session.resume"]["instructionDirectories"] == [
                "C:\\resume-instructions"
            ]
        finally:
            await client.force_stop()


class TestModelBilling:
    def test_token_prices_round_trip(self):
        """ModelBilling.from_dict/to_dict round-trips tokenPrices and longContext."""
        wire = {
            "multiplier": 1.5,
            "tokenPrices": {
                "inputPrice": 2.0,
                "outputPrice": 8.0,
                "cachePrice": 0.5,
                "batchSize": 1000000,
                "contextMax": 128000,
                "longContext": {
                    "inputPrice": 4.0,
                    "outputPrice": 16.0,
                    "cachePrice": 1.0,
                    "contextMax": 1000000,
                },
            },
        }

        billing = ModelBilling.from_dict(wire)

        assert billing.multiplier == 1.5
        assert isinstance(billing.token_prices, ModelBillingTokenPrices)
        prices = billing.token_prices
        assert prices.input_price == 2.0
        assert prices.output_price == 8.0
        assert prices.cache_price == 0.5
        assert prices.batch_size == 1000000
        assert prices.context_max == 128000
        assert isinstance(prices.long_context, ModelBillingTokenPricesLongContext)
        long_context = prices.long_context
        assert long_context.input_price == 4.0
        assert long_context.output_price == 16.0
        assert long_context.cache_price == 1.0
        assert long_context.context_max == 1000000

        assert billing.to_dict() == wire

    def test_token_prices_absent(self):
        """ModelBilling without tokenPrices leaves token_prices unset."""
        billing = ModelBilling.from_dict({"multiplier": 1.0})
        assert billing.token_prices is None
        assert billing.to_dict() == {"multiplier": 1.0}

    def test_token_prices_empty_object_round_trip(self):
        """ModelBilling preserves present but empty tokenPrices."""
        billing = ModelBilling.from_dict({"tokenPrices": {}})

        assert isinstance(billing.token_prices, ModelBillingTokenPrices)
        prices = billing.token_prices
        assert prices.input_price is None
        assert prices.output_price is None
        assert prices.cache_price is None
        assert prices.batch_size is None
        assert prices.context_max is None
        assert prices.long_context is None
        assert billing.to_dict() == {"tokenPrices": {}}

    def test_long_context_empty_object_round_trip(self):
        """ModelBilling preserves present but empty longContext."""
        billing = ModelBilling.from_dict({"tokenPrices": {"longContext": {}}})

        assert isinstance(billing.token_prices, ModelBillingTokenPrices)
        prices = billing.token_prices
        assert isinstance(prices.long_context, ModelBillingTokenPricesLongContext)
        long_context = prices.long_context
        assert long_context.input_price is None
        assert long_context.output_price is None
        assert long_context.cache_price is None
        assert long_context.context_max is None
        assert billing.to_dict() == {"tokenPrices": {"longContext": {}}}


class TestOnListModels:
    @pytest.mark.asyncio
    async def test_list_models_with_custom_handler(self):
        """Test that on_list_models handler is called instead of RPC"""
        custom_models = [
            ModelInfo(
                id="my-custom-model",
                name="My Custom Model",
                capabilities=ModelCapabilities(
                    supports=ModelSupports(vision=False, reasoning_effort=False),
                    limits=ModelLimits(max_context_window_tokens=128000),
                ),
            )
        ]

        handler_calls = []

        def handler():
            handler_calls.append(1)
            return custom_models

        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            on_list_models=handler,
        )
        await client.start()
        try:
            models = await client.list_models()
            assert len(handler_calls) == 1
            assert models == custom_models
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_list_models_handler_caches_results(self):
        """Test that on_list_models results are cached"""
        custom_models = [
            ModelInfo(
                id="cached-model",
                name="Cached Model",
                capabilities=ModelCapabilities(
                    supports=ModelSupports(vision=False, reasoning_effort=False),
                    limits=ModelLimits(max_context_window_tokens=128000),
                ),
            )
        ]

        handler_calls = []

        def handler():
            handler_calls.append(1)
            return custom_models

        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            on_list_models=handler,
        )
        await client.start()
        try:
            await client.list_models()
            await client.list_models()
            assert len(handler_calls) == 1  # Only called once due to caching
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_list_models_async_handler(self):
        """Test that async on_list_models handler works"""
        custom_models = [
            ModelInfo(
                id="async-model",
                name="Async Model",
                capabilities=ModelCapabilities(
                    supports=ModelSupports(vision=False, reasoning_effort=False),
                    limits=ModelLimits(max_context_window_tokens=128000),
                ),
            )
        ]

        async def handler():
            return custom_models

        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            on_list_models=handler,
        )
        await client.start()
        try:
            models = await client.list_models()
            assert models == custom_models
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_list_models_handler_without_start(self):
        """Test that on_list_models works without starting the CLI connection"""
        custom_models = [
            ModelInfo(
                id="no-start-model",
                name="No Start Model",
                capabilities=ModelCapabilities(
                    supports=ModelSupports(vision=False, reasoning_effort=False),
                    limits=ModelLimits(max_context_window_tokens=128000),
                ),
            )
        ]

        handler_calls = []

        def handler():
            handler_calls.append(1)
            return custom_models

        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            on_list_models=handler,
        )
        models = await client.list_models()
        assert len(handler_calls) == 1
        assert models == custom_models


class TestSessionConfigForwarding:
    @pytest.mark.asyncio
    @pytest.mark.parametrize("trust", [None, "session", "user"])
    async def test_create_session_forwards_working_directory_trust(self, trust, tmp_path):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                trust_working_directory=trust,
                working_directory=str(tmp_path),
                config_directory=str(tmp_path / "config"),
            )
            if trust is None:
                assert "trustWorkingDirectory" not in captured["session.create"]
            else:
                assert captured["session.create"]["trustWorkingDirectory"] == trust
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_session_forwards_client_name(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all, client_name="my-app"
            )
            assert captured["session.create"]["clientName"] == "my-app"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_forwards_client_name(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.resume":
                    # Return a fake response to avoid needing real auth
                    return {"sessionId": session.session_id}
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                client_name="my-app",
            )
            assert captured["session.resume"]["clientName"] == "my-app"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_session_forwards_enable_session_telemetry(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                enable_session_telemetry=False,
            )
            assert captured["session.create"]["enableSessionTelemetry"] is False
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_forwards_enable_session_telemetry(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.resume":
                    return {"sessionId": session.session_id}
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                enable_session_telemetry=False,
            )
            assert captured["session.resume"]["enableSessionTelemetry"] is False
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_session_forwards_enable_on_demand_instruction_discovery(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                enable_on_demand_instruction_discovery=False,
            )
            assert captured["session.create"]["enableOnDemandInstructionDiscovery"] is False
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_forwards_enable_on_demand_instruction_discovery(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.resume":
                    return {"sessionId": session.session_id}
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                enable_on_demand_instruction_discovery=False,
            )
            assert captured["session.resume"]["enableOnDemandInstructionDiscovery"] is False
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_session_forwards_provider_headers(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.create":
                    sid = params.get("sessionId") or "session-id"
                    result = {"sessionId": sid}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                provider={
                    "base_url": "https://example.com/provider",
                    "headers": {"Authorization": "Bearer provider-token"},
                    "model_id": "gpt-4o",
                    "wire_model": "my-finetune-v3",
                    "model_provider": "lm_studio",
                    "api_key_command": "credential-helper --profile sdk",
                    "max_prompt_tokens": 100_000,
                    "max_output_tokens": 4096,
                    "transport": "websockets",
                },
            )

            provider = captured["session.create"]["provider"]
            assert provider["baseUrl"] == "https://example.com/provider"
            assert provider["headers"] == {"Authorization": "Bearer provider-token"}
            assert provider["modelId"] == "gpt-4o"
            assert provider["wireModel"] == "my-finetune-v3"
            assert provider["modelProvider"] == "lm_studio"
            assert provider["apiKeyCommand"] == "credential-helper --profile sdk"
            assert "api_key_command" not in provider
            assert provider["maxPromptTokens"] == 100_000
            assert provider["maxOutputTokens"] == 4096
            assert provider["transport"] == "websockets"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_forwards_provider_headers(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.resume":
                    return {"sessionId": session.session_id}
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                provider={
                    "base_url": "https://example.com/provider",
                    "headers": {"Authorization": "Bearer resume-token"},
                    "model_id": "gpt-4o",
                    "wire_model": "my-finetune-v3",
                    "model_provider": "ollama",
                    "api_key_command": "credential-helper --profile resumed",
                    "max_prompt_tokens": 100_000,
                    "max_output_tokens": 4096,
                },
            )

            provider = captured["session.resume"]["provider"]
            assert provider["baseUrl"] == "https://example.com/provider"
            assert provider["headers"] == {"Authorization": "Bearer resume-token"}
            assert provider["modelId"] == "gpt-4o"
            assert provider["wireModel"] == "my-finetune-v3"
            assert provider["modelProvider"] == "ollama"
            assert provider["apiKeyCommand"] == "credential-helper --profile resumed"
            assert "api_key_command" not in provider
            assert provider["maxPromptTokens"] == 100_000
            assert provider["maxOutputTokens"] == 4096
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_session_forwards_named_provider_model_provider(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.create":
                    sid = params.get("sessionId") or "session-id"
                    result = {"sessionId": sid}
                    callback = kwargs.get("on_response_inline")
                    if callback is not None:
                        callback(result)
                    return result
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                providers=[
                    {
                        "name": "local",
                        "type": "openai",
                        "base_url": "http://localhost:11434/v1",
                        "model_provider": "ollama",
                    }
                ],
                models=[{"id": "llama3", "provider": "local"}],
            )

            provider = captured["session.create"]["providers"][0]
            assert provider["name"] == "local"
            assert provider["baseUrl"] == "http://localhost:11434/v1"
            assert provider["modelProvider"] == "ollama"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_session_send_forwards_request_headers(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.send":
                    return {"messageId": "msg-1"}
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await session.send(
                "hello",
                request_headers={"Authorization": "Bearer turn-token"},
            )

            assert captured["session.send"]["prompt"] == "hello"
            assert captured["session.send"]["requestHeaders"] == {
                "Authorization": "Bearer turn-token"
            }
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_session_forwards_agent(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                agent="test-agent",
                custom_agents=[{"name": "test-agent", "prompt": "You are a test agent."}],
            )
            assert captured["session.create"]["agent"] == "test-agent"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_forwards_agent(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.resume":
                    return {"sessionId": session.session_id}
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                agent="test-agent",
                custom_agents=[{"name": "test-agent", "prompt": "You are a test agent."}],
            )
            assert captured["session.resume"]["agent"] == "test-agent"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_session_defaults_include_sub_agent_streaming_events_to_true(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            assert captured["session.create"]["includeSubAgentStreamingEvents"] is True
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_session_preserves_explicit_false_include_sub_agent_streaming_events(
        self,
    ):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                include_sub_agent_streaming_events=False,
            )
            assert captured["session.create"]["includeSubAgentStreamingEvents"] is False
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_defaults_include_sub_agent_streaming_events_to_true(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.resume":
                    return {"sessionId": session.session_id}
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
            )
            assert captured["session.resume"]["includeSubAgentStreamingEvents"] is True
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_preserves_explicit_false_include_sub_agent_streaming_events(
        self,
    ):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.resume":
                    return {"sessionId": session.session_id}
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                include_sub_agent_streaming_events=False,
            )
            assert captured["session.resume"]["includeSubAgentStreamingEvents"] is False
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        ("mode", "setting", "expected"),
        [
            ("copilot-cli", None, None),
            ("empty", None, None),
            ("copilot-cli", False, False),
            ("copilot-cli", True, True),
            ("empty", False, False),
            ("empty", True, True),
        ],
    )
    async def test_resume_transcript_recovery_wire_and_report(
        self, mode, setting, expected, tmp_path
    ):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            mode=mode,
            base_directory=str(tmp_path),
        )
        await client.start()
        try:
            captured = {}

            async def mock_request(method, params, **_kwargs):
                captured[method] = params
                if method == "session.resume":
                    if params["sessionId"] == "transcript-unrepaired":
                        return {"sessionId": params["sessionId"]}
                    return {
                        "sessionId": params["sessionId"],
                        "transcriptRecovery": {
                            "plannedBackupPath": "backup.jsonl",
                            "invalidLineNumbers": [3, 4],
                            "sessionStartMoved": True,
                        },
                    }
                if method == "session.options.update":
                    return {"success": True}
                return {}

            client._client.request = mock_request
            session = await client.resume_session(
                "transcript-test",
                on_permission_request=PermissionHandler.approve_all,
                available_tools=[],
                allow_transcript_recovery=setting,
            )
            assert captured["session.resume"].get("allowTranscriptRecovery") is expected
            assert ("allowTranscriptRecovery" in captured["session.resume"]) == (
                expected is not None
            )
            assert session.transcript_recovery is not None
            assert session.transcript_recovery.planned_backup_path == "backup.jsonl"
            assert session.transcript_recovery.invalid_line_numbers == [3, 4]
            assert session.transcript_recovery.session_start_moved is True
            unrepaired = await client.resume_session(
                "transcript-unrepaired",
                on_permission_request=PermissionHandler.approve_all,
                available_tools=[],
            )
            assert unrepaired.transcript_recovery is None
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_forwards_continue_pending_work(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )

            captured: dict = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.resume":
                    return {"sessionId": session.session_id}
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                continue_pending_work=True,
            )
            assert captured["session.resume"]["continuePendingWork"] is True
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_omits_continue_pending_work_by_default(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )

            captured: dict = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.resume":
                    return {"sessionId": session.session_id}
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
            )
            assert "continuePendingWork" not in captured["session.resume"]
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_set_model_sends_correct_rpc(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.model.switchTo":
                    return {}
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await session.set_model(
                "gpt-4.1",
                reasoning_summary="detailed",
                context_tier="long_context",
            )
            assert captured["session.model.switchTo"]["sessionId"] == session.session_id
            assert captured["session.model.switchTo"]["modelId"] == "gpt-4.1"
            assert captured["session.model.switchTo"]["reasoningSummary"] == "detailed"
            assert captured["session.model.switchTo"]["contextTier"] == "long_context"
            assert "autoTier" not in captured["session.model.switchTo"]
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_set_model_sends_auto_tier(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.model.switchTo":
                    return {}
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await session.set_model("auto", auto_tier="fast")
            assert captured["session.model.switchTo"]["sessionId"] == session.session_id
            assert captured["session.model.switchTo"]["modelId"] == "auto"
            assert captured["session.model.switchTo"]["autoTier"] == "fast"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_set_model_sends_explicit_null_auto_tier(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.model.switchTo":
                    return {}
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await session.set_model("auto", auto_tier=None)
            # An explicit null must survive to the wire; omitting it would mean
            # "leave the preference alone" rather than "use default routing".
            assert "autoTier" in captured["session.model.switchTo"]
            assert captured["session.model.switchTo"]["autoTier"] is None
        finally:
            await client.force_stop()


class TestSetAutoTier:
    @pytest.mark.asyncio
    @pytest.mark.parametrize("auto_tier", ["efficiency", "balance", "intelligence", "fast", None])
    async def test_set_auto_tier_sends_correct_rpc(self, auto_tier):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.model.switchAutoTier":
                    return {
                        "status": "pending",
                        "effectiveAutoTier": "balance",
                        "pendingAutoTier": auto_tier,
                        "activatingAutoTier": None,
                    }
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            result = await session.set_auto_tier(auto_tier)

            params = captured["session.model.switchAutoTier"]
            assert params["sessionId"] == session.session_id
            assert "autoTier" in params
            assert params["autoTier"] == auto_tier

            assert result.status == ModelSwitchAutoTierStatus.PENDING
            assert result.effective_auto_tier == AutoTierEnum.BALANCE
            assert result.activating_auto_tier is None
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_set_auto_tier_accepts_the_enum_it_returns(self):
        """The tier on a result or event is an enum, so it has to be valid input too."""
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.model.switchAutoTier":
                    return {"status": "pending", "effectiveAutoTier": "fast"}
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await session.set_auto_tier(AutoTierEnum.FAST)

            params = captured["session.model.switchAutoTier"]
            # The value must be a plain string; the JSON-RPC encoder cannot
            # serialize an enum.
            assert params["autoTier"] == "fast"
            assert isinstance(params["autoTier"], str)
            json.dumps(params)
        finally:
            await client.force_stop()


class TestMcpOAuthTokenStorage:
    @pytest.mark.asyncio
    async def test_create_session_defaults_mcp_oauth_token_storage_to_in_memory_in_empty_mode(
        self,
    ):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            mode="empty",
            base_directory="/tmp/copilot-test",
        )
        await client.start()

        try:
            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                available_tools=[],
            )
            assert captured["session.create"]["mcpOAuthTokenStorage"] == "in-memory"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_session_does_not_send_mcp_oauth_token_storage_in_copilot_cli_mode(
        self,
    ):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            assert "mcpOAuthTokenStorage" not in captured["session.create"]
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_session_forwards_explicit_mcp_oauth_token_storage(self):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            mode="empty",
            base_directory="/tmp/copilot-test",
        )
        await client.start()

        try:
            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                available_tools=[],
                mcp_oauth_token_storage="persistent",
            )
            assert captured["session.create"]["mcpOAuthTokenStorage"] == "persistent"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_defaults_mcp_oauth_token_storage_to_in_memory_in_empty_mode(
        self,
    ):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            mode="empty",
            base_directory="/tmp/copilot-test",
        )
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                available_tools=[],
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.resume":
                    return {"sessionId": session.session_id}
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                available_tools=[],
            )
            assert captured["session.resume"]["mcpOAuthTokenStorage"] == "in-memory"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_forwards_explicit_mcp_oauth_token_storage(self):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            mode="empty",
            base_directory="/tmp/copilot-test",
        )
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                available_tools=[],
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.resume":
                    return {"sessionId": session.session_id}
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                available_tools=[],
                mcp_oauth_token_storage="persistent",
            )
            assert captured["session.resume"]["mcpOAuthTokenStorage"] == "persistent"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_session_defaults_memory_to_disabled_in_empty_mode(self):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            mode="empty",
            base_directory="/tmp/copilot-test",
        )
        await client.start()

        try:
            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                available_tools=[],
            )
            assert captured["session.create"]["memory"] == {"enabled": False}
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_session_forwards_explicit_memory_in_empty_mode(self):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            mode="empty",
            base_directory="/tmp/copilot-test",
        )
        await client.start()

        try:
            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                available_tools=[],
                memory={"enabled": True},
            )
            assert captured["session.create"]["memory"] == {"enabled": True}
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_defaults_memory_to_disabled_in_empty_mode(self):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            mode="empty",
            base_directory="/tmp/copilot-test",
        )
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                available_tools=[],
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.resume":
                    return {"sessionId": session.session_id}
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                available_tools=[],
            )
            assert captured["session.resume"]["memory"] == {"enabled": False}
        finally:
            await client.force_stop()


class TestCopilotClientContextManager:
    @pytest.mark.asyncio
    async def test_aenter_calls_start_and_returns_self(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        with patch.object(client, "start", new_callable=AsyncMock) as mock_start:
            result = await client.__aenter__()
            mock_start.assert_awaited_once()
            assert result is client

    @pytest.mark.asyncio
    async def test_aexit_calls_stop(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        with patch.object(client, "stop", new_callable=AsyncMock) as mock_stop:
            await client.__aexit__(None, None, None)
            mock_stop.assert_awaited_once()


class TestCopilotSessionContextManager:
    @pytest.mark.asyncio
    async def test_aenter_returns_self(self):
        from copilot.session import CopilotSession

        session = CopilotSession.__new__(CopilotSession)
        result = await session.__aenter__()
        assert result is session

    @pytest.mark.asyncio
    async def test_aexit_calls_disconnect(self):
        from copilot.session import CopilotSession

        session = CopilotSession.__new__(CopilotSession)
        with patch.object(session, "disconnect", new_callable=AsyncMock) as mock_disconnect:
            await session.__aexit__(None, None, None)
            mock_disconnect.assert_awaited_once()


class TestProviderWireFormat:
    def test_api_key_command_is_forwarded_without_local_execution(self):
        client = CopilotClient.__new__(CopilotClient)
        wire = client._convert_provider_to_wire_format(
            {
                "base_url": "https://models.example.test",
                "api_key_command": "credential-helper --profile sdk",
            }
        )
        assert wire["apiKeyCommand"] == "credential-helper --profile sdk"
        assert "api_key_command" not in wire

    def test_api_key_command_is_omitted_when_absent(self):
        client = CopilotClient.__new__(CopilotClient)
        wire = client._convert_provider_to_wire_format({"base_url": "https://models.example.test"})
        assert "apiKeyCommand" not in wire


class TestCustomAgentWireFormat:
    @pytest.mark.parametrize("policy", ["preferred", "required"])
    def test_model_policy_is_forwarded_in_camel_case(self, policy):
        client = CopilotClient.__new__(CopilotClient)
        wire = client._convert_custom_agent_to_wire_format(
            {"name": "policy-agent", "prompt": "Use the authored model.", "model_policy": policy}
        )
        assert wire["modelPolicy"] == policy
        assert "model_policy" not in wire

    def test_model_field_is_forwarded_in_wire_format(self):
        """The model key in CustomAgentConfig should appear as 'model' in the wire payload."""
        from copilot.client import CopilotClient
        from copilot.session import CustomAgentConfig

        client = CopilotClient.__new__(CopilotClient)
        agent: CustomAgentConfig = {
            "name": "model-agent",
            "prompt": "You are a model agent.",
            "model": "claude-haiku-4.5",
        }
        wire = client._convert_custom_agent_to_wire_format(agent)
        assert wire["model"] == "claude-haiku-4.5"
        assert wire["name"] == "model-agent"
        assert wire["prompt"] == "You are a model agent."

    def test_model_field_is_omitted_when_absent(self):
        """When model is not set, it should not appear in the wire payload."""
        from copilot.client import CopilotClient
        from copilot.session import CustomAgentConfig

        client = CopilotClient.__new__(CopilotClient)
        agent: CustomAgentConfig = {
            "name": "no-model-agent",
            "prompt": "You are an agent without a model.",
        }
        wire = client._convert_custom_agent_to_wire_format(agent)
        assert "model" not in wire
        assert "modelPolicy" not in wire

    def test_reasoning_effort_is_forwarded_in_camel_case(self):
        from copilot.client import CopilotClient
        from copilot.session import CustomAgentConfig

        client = CopilotClient.__new__(CopilotClient)
        agent: CustomAgentConfig = {
            "name": "reasoning-agent",
            "prompt": "Think carefully.",
            "reasoning_effort": "high",
        }
        wire = client._convert_custom_agent_to_wire_format(agent)
        assert wire["reasoningEffort"] == "high"
        assert "reasoning_effort" not in wire

    def test_reasoning_effort_is_omitted_when_absent(self):
        from copilot.client import CopilotClient
        from copilot.session import CustomAgentConfig

        client = CopilotClient.__new__(CopilotClient)
        agent: CustomAgentConfig = {
            "name": "default-agent",
            "prompt": "Use runtime defaults.",
        }
        wire = client._convert_custom_agent_to_wire_format(agent)
        assert "reasoningEffort" not in wire


class TestPostToolUseFailureHookDispatch:
    """Unit tests for the postToolUseFailure handler dispatch."""

    @pytest.mark.asyncio
    async def test_dispatches_to_on_post_tool_use_failure(self):
        from copilot.session import CopilotSession, SessionHooks

        captured: dict = {}

        async def on_failure(input_data, invocation):
            captured["input"] = input_data
            captured["invocation"] = invocation
            return {"additionalContext": f"saw {input_data['toolName']}: {input_data['error']}"}

        session = CopilotSession.__new__(CopilotSession)
        CopilotSession.__init__(session, "sess-123", client=None)
        session._hooks = SessionHooks(on_post_tool_use_failure=on_failure)  # type: ignore[typeddict-item]

        result = await session._handle_hooks_invoke(
            "postToolUseFailure",
            {
                "sessionId": "sess-x",
                "timestamp": 1700000000,
                "cwd": "/work",
                "toolName": "tool-x",
                "toolArgs": {"foo": "bar"},
                "error": "boom",
            },
        )
        assert result == {"additionalContext": "saw tool-x: boom"}
        assert captured["input"]["toolName"] == "tool-x"
        assert captured["input"]["workingDirectory"] == "/work"
        assert captured["input"]["timestamp"] == datetime.fromtimestamp(1700000000 / 1000, tz=UTC)
        assert captured["invocation"] == {"session_id": "sess-123"}

    @pytest.mark.asyncio
    async def test_returns_none_when_no_handler_registered(self):
        from copilot.session import CopilotSession, SessionHooks

        session = CopilotSession.__new__(CopilotSession)
        CopilotSession.__init__(session, "sess-x", client=None)
        # Hooks registered, but no postToolUseFailure handler -> dispatch returns None.
        session._hooks = SessionHooks(on_post_tool_use=lambda i, v: None)  # type: ignore[typeddict-item]

        result = await session._handle_hooks_invoke(
            "postToolUseFailure",
            {
                "sessionId": "sess-x",
                "timestamp": 0,
                "cwd": "/",
                "toolName": "t",
                "toolArgs": None,
                "error": "e",
            },
        )
        assert result is None

    @pytest.mark.asyncio
    async def test_sync_handler_works(self):
        from copilot.session import CopilotSession, SessionHooks

        def on_failure(input_data, invocation):
            return {"additionalContext": "sync-ok"}

        session = CopilotSession.__new__(CopilotSession)
        CopilotSession.__init__(session, "sess-y", client=None)
        session._hooks = SessionHooks(on_post_tool_use_failure=on_failure)  # type: ignore[typeddict-item]

        result = await session._handle_hooks_invoke(
            "postToolUseFailure",
            {
                "sessionId": "sess-x",
                "timestamp": 0,
                "cwd": "/",
                "toolName": "t",
                "toolArgs": None,
                "error": "e",
            },
        )
        assert result == {"additionalContext": "sync-ok"}


class TestHookErrorObjectNormalization:
    """The CLI's error object reaches errorOccurred and sessionEnd handlers as a string."""

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        ("error", "expected"),
        [
            (
                {"name": "Error", "message": "model timeout", "stack": "Error: model timeout"},
                "model timeout",
            ),
            ("boom", "boom"),
            (None, None),
            ({"code": 7}, '{"code":7}'),
        ],
    )
    @pytest.mark.parametrize("hook_type", ["errorOccurred", "sessionEnd"])
    async def test_decodes_the_error(self, hook_type, error, expected):
        from copilot.session import CopilotSession, SessionHooks

        captured: dict = {}

        def handler(input_data, invocation):
            captured["input"] = input_data

        session = CopilotSession.__new__(CopilotSession)
        CopilotSession.__init__(session, "sess-123", client=None)
        session._hooks = SessionHooks(  # type: ignore[typeddict-item]
            on_error_occurred=handler, on_session_end=handler
        )

        await session._handle_hooks_invoke(
            hook_type,
            {"sessionId": "sess-x", "timestamp": 1700000000, "cwd": "/work", "error": error},
        )
        assert captured["input"]["error"] == expected

    @pytest.mark.asyncio
    async def test_leaves_an_absent_session_end_error_absent(self):
        from copilot.session import CopilotSession, SessionHooks

        captured: dict = {}

        def handler(input_data, invocation):
            captured["input"] = input_data

        session = CopilotSession.__new__(CopilotSession)
        CopilotSession.__init__(session, "sess-123", client=None)
        session._hooks = SessionHooks(on_session_end=handler)  # type: ignore[typeddict-item]

        await session._handle_hooks_invoke(
            "sessionEnd",
            {"sessionId": "sess-x", "timestamp": 1700000000, "cwd": "/work", "reason": "complete"},
        )
        assert "error" not in captured["input"]


class TestAgentStopHookDispatch:
    """Unit tests for the agentStop handler dispatch."""

    @pytest.mark.asyncio
    async def test_dispatches_to_on_agent_stop(self):
        from copilot.session import CopilotSession, SessionHooks

        captured: dict = {}

        async def on_agent_stop(input_data, invocation):
            captured["input"] = input_data
            captured["invocation"] = invocation
            return {"decision": "block", "reason": "finish the remaining work"}

        session = CopilotSession.__new__(CopilotSession)
        CopilotSession.__init__(session, "sess-123", client=None)
        session._hooks = SessionHooks(on_agent_stop=on_agent_stop)  # type: ignore[typeddict-item]

        result = await session._handle_hooks_invoke(
            "agentStop",
            {
                "sessionId": "sess-x",
                "timestamp": 1700000000,
                "cwd": "/work",
                "stopReason": "end_turn",
                "transcriptPath": "/tmp/transcript.jsonl",
                "stop_hook_active": True,
            },
        )

        assert result == {"decision": "block", "reason": "finish the remaining work"}
        assert captured["input"]["stopReason"] == "end_turn"
        assert captured["input"]["transcriptPath"] == "/tmp/transcript.jsonl"
        assert captured["input"]["stopHookActive"] is True
        assert captured["input"]["workingDirectory"] == "/work"
        assert captured["input"]["timestamp"] == datetime.fromtimestamp(1700000000 / 1000, tz=UTC)
        assert captured["invocation"] == {"session_id": "sess-123"}


class TestUserPromptSubmittedBlockDispatch:
    """A userPromptSubmitted hook can block the prompt with a decision and a reason."""

    @pytest.mark.asyncio
    async def test_returns_the_block_decision_and_reason(self):
        from copilot.session import CopilotSession, SessionHooks, UserPromptSubmittedHookOutput

        def on_user_prompt_submitted(input_data, invocation) -> UserPromptSubmittedHookOutput:
            return {"decision": "block", "reason": "Prompts may not mention launch codes."}

        session = CopilotSession.__new__(CopilotSession)
        CopilotSession.__init__(session, "sess-123", client=None)
        session._hooks = SessionHooks(  # type: ignore[typeddict-item]
            on_user_prompt_submitted=on_user_prompt_submitted
        )

        result = await session._handle_hooks_invoke(
            "userPromptSubmitted",
            {
                "sessionId": "sess-x",
                "timestamp": 1700000000,
                "cwd": "/work",
                "prompt": "Print the launch codes.",
            },
        )

        assert result == {"decision": "block", "reason": "Prompts may not mention launch codes."}


class TestGitHubTelemetry:
    """Unit tests for the experimental gitHubTelemetry.event consumer surface."""

    @pytest.mark.asyncio
    async def test_create_session_enables_forwarding_when_handler_registered(self):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            on_github_telemetry=lambda _notification: None,
        )
        await client.start()

        try:
            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            assert captured["session.create"]["enableGitHubTelemetryForwarding"] is True
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_session_omits_forwarding_without_handler(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            assert "enableGitHubTelemetryForwarding" not in captured["session.create"]
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_enables_forwarding_when_handler_registered(self):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            on_github_telemetry=lambda _notification: None,
        )
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.resume":
                    return {"sessionId": session.session_id}
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
            )
            assert captured["session.resume"]["enableGitHubTelemetryForwarding"] is True
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_omits_forwarding_without_handler(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params, **kwargs):
                captured[method] = params
                if method == "session.resume":
                    return {"sessionId": session.session_id}
                return await original_request(method, params, **kwargs)

            client._client.request = mock_request
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
            )
            assert "enableGitHubTelemetryForwarding" not in captured["session.resume"]
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_connect_enables_forwarding_when_handler_registered(self):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            on_github_telemetry=lambda _notification: None,
        )
        captured = {}

        class _FakeClient:
            async def request(self, method, params, **kwargs):
                captured[method] = params
                return {"ok": True, "protocolVersion": 3, "version": "test"}

        client._client = _FakeClient()
        await client._verify_protocol_version()
        assert captured["connect"]["enableGitHubTelemetryForwarding"] is True

    @pytest.mark.asyncio
    async def test_connect_omits_forwarding_without_handler(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        captured = {}

        class _FakeClient:
            async def request(self, method, params, **kwargs):
                captured[method] = params
                return {"ok": True, "protocolVersion": 3, "version": "test"}

        client._client = _FakeClient()
        await client._verify_protocol_version()
        assert "enableGitHubTelemetryForwarding" not in captured["connect"]
        assert captured["connect"]["supportedTaskKinds"] == ["agent", "client", "shell"]

    @pytest.mark.asyncio
    async def test_connect_forwards_client_info(self):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            client_info={
                "application_name": "acme-developer-portal",
                "application_version": "2.4.0",
                "integration_name": "copilot-assistant",
                "integration_version": "1.5.0",
            },
        )
        captured = {}

        class _FakeClient:
            async def request(self, method, params, **kwargs):
                captured[method] = params
                return {"ok": True, "protocolVersion": 3, "version": "test"}

        client._client = _FakeClient()
        await client._verify_protocol_version()
        assert captured["connect"]["clientInfo"] == {
            "editorName": "acme-developer-portal",
            "editorVersion": "2.4.0",
            "extensionName": "copilot-assistant",
            "extensionVersion": "1.5.0",
        }

    @pytest.mark.asyncio
    async def test_connect_omits_client_info_when_unset(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        captured = {}

        class _FakeClient:
            async def request(self, method, params, **kwargs):
                captured[method] = params
                return {"ok": True, "protocolVersion": 3, "version": "test"}

        client._client = _FakeClient()
        await client._verify_protocol_version()
        assert "clientInfo" not in captured["connect"]
        assert "entra" not in captured["connect"]

    @pytest.mark.asyncio
    async def test_connect_forwards_entra_configuration(self):
        from copilot import EntraAuthConfig

        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            entra=EntraAuthConfig(client_id="host-entra-client"),
        )
        captured = {}

        class _FakeClient:
            async def request(self, method, params, **kwargs):
                captured[method] = params
                return {"ok": True, "protocolVersion": 3, "version": "test"}

        client._client = _FakeClient()
        await client._verify_protocol_version()
        assert captured["connect"]["entra"] == {"clientId": "host-entra-client"}

    @pytest.mark.asyncio
    async def test_connect_forwards_partial_client_info_with_forwarding(self):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            client_info={"application_name": "example-app"},
            on_github_telemetry=lambda _notification: None,
        )
        captured = {}

        class _FakeClient:
            async def request(self, method, params, **kwargs):
                captured[method] = params
                return {"ok": True, "protocolVersion": 3, "version": "test"}

        client._client = _FakeClient()
        await client._verify_protocol_version()
        assert captured["connect"]["clientInfo"] == {"editorName": "example-app"}
        assert captured["connect"]["enableGitHubTelemetryForwarding"] is True

    @pytest.mark.asyncio
    async def test_connect_drops_empty_client_info_fields(self):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            client_info={"application_name": "example-app", "application_version": ""},
        )
        captured = {}

        class _FakeClient:
            async def request(self, method, params, **kwargs):
                captured[method] = params
                return {"ok": True, "protocolVersion": 3, "version": "test"}

        client._client = _FakeClient()
        await client._verify_protocol_version()
        assert captured["connect"]["clientInfo"] == {"editorName": "example-app"}

    @pytest.mark.asyncio
    async def test_connect_omits_all_empty_client_info(self):
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            client_info={
                "application_name": "",
                "application_version": "",
                "integration_name": "",
                "integration_version": "",
            },
        )
        captured = {}

        class _FakeClient:
            async def request(self, method, params, **kwargs):
                captured[method] = params
                return {"ok": True, "protocolVersion": 3, "version": "test"}

        client._client = _FakeClient()
        await client._verify_protocol_version()
        assert "clientInfo" not in captured["connect"]

    @pytest.mark.asyncio
    async def test_event_routes_to_handler(self):
        from copilot.generated.rpc import GitHubTelemetryNotification

        received: list = []
        delivered = asyncio.Event()

        def on_telemetry(notification):
            received.append(notification)
            delivered.set()

        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            on_github_telemetry=on_telemetry,
        )
        await client.start()

        try:
            # gitHubTelemetry.event is a JSON-RPC *notification*: the generated
            # client-global dispatcher wires it into the notification-handler
            # table, never the request-handler table. Regressing to request-style
            # dispatch would drop the runtime's id-less telemetry frames.
            assert "gitHubTelemetry.event" in client._client.notification_method_handlers
            assert "gitHubTelemetry.event" not in client._client.request_handlers

            # Drive a real id-less notification frame through the dispatcher to
            # exercise the full from_dict decode + adapter + user-callback path.
            client._client._handle_message(
                {
                    "jsonrpc": "2.0",
                    "method": "gitHubTelemetry.event",
                    "params": {
                        "sessionId": "sess-telemetry",
                        "restricted": True,
                        "event": {
                            "kind": "tool_call_executed",
                            "metrics": {"duration_ms": 12.5},
                            "properties": {"tool": "shell"},
                            "session_id": "sess-telemetry",
                        },
                    },
                }
            )

            await asyncio.wait_for(delivered.wait(), timeout=1.0)

            assert len(received) == 1
            notification = received[0]
            assert isinstance(notification, GitHubTelemetryNotification)
            assert notification.session_id == "sess-telemetry"
            assert notification.restricted is True
            assert notification.event.kind == "tool_call_executed"
            assert notification.event.metrics["duration_ms"] == 12.5
            assert notification.event.properties["tool"] == "shell"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_event_routes_to_async_handler(self):
        from copilot.generated.rpc import GitHubTelemetryNotification

        received: list = []
        delivered = asyncio.Event()

        async def on_telemetry(notification):
            await asyncio.sleep(0)
            received.append(notification)
            delivered.set()

        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=CLI_PATH),
            on_github_telemetry=on_telemetry,
        )
        await client.start()

        try:
            client._client._handle_message(
                {
                    "jsonrpc": "2.0",
                    "method": "gitHubTelemetry.event",
                    "params": {
                        "sessionId": "sess-async-telemetry",
                        "restricted": False,
                        "event": {
                            "kind": "tool_call_executed",
                            "metrics": {"duration_ms": 3.5},
                            "properties": {"tool": "python"},
                            "session_id": "sess-async-telemetry",
                        },
                    },
                }
            )

            await asyncio.wait_for(delivered.wait(), timeout=1)

            assert len(received) == 1
            notification = received[0]
            assert isinstance(notification, GitHubTelemetryNotification)
            assert notification.session_id == "sess-async-telemetry"
            assert notification.restricted is False
            assert notification.event.kind == "tool_call_executed"
            assert notification.event.metrics["duration_ms"] == 3.5
            assert notification.event.properties["tool"] == "python"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_event_not_forwarded_without_option(self):
        # Client-global handlers are always registered (so that hooks.invoke works),
        # but without the on_github_telemetry option the telemetry adapter is inert:
        # incoming events must not be forwarded to any callback.
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()

        try:
            assert client._on_github_telemetry is None

            # Dispatching a telemetry event is a harmless no-op when not opted in.
            client._client._handle_message(
                {
                    "jsonrpc": "2.0",
                    "method": "gitHubTelemetry.event",
                    "params": {
                        "sessionId": "sess-no-telemetry",
                        "restricted": False,
                        "event": {
                            "kind": "tool_call_executed",
                            "metrics": {"duration_ms": 1.0},
                            "properties": {"tool": "shell"},
                            "session_id": "sess-no-telemetry",
                        },
                    },
                }
            )
            await asyncio.sleep(0)
        finally:
            await client.force_stop()
