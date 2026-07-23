"""
CopilotClient Unit Tests

This file is for unit tests. Where relevant, prefer to add e2e tests in e2e/*.py instead.
"""

import asyncio
import inspect
from datetime import UTC, datetime
from unittest.mock import AsyncMock, Mock, patch

import pytest

from copilot import (
    CanvasProviderIdentity,
    CapiSessionOptions,
    CopilotClient,
    ExtensionInfo,
    ModelBillingTokenPrices,
    ModelBillingTokenPricesLongContext,
    RuntimeConnection,
    StdioRuntimeConnection,
    define_tool,
)
from copilot.client import (
    CloudSessionOptions,
    CloudSessionRepository,
    CopilotExpAssignmentResponse,
    ExpConfigEntry,
    ModelBilling,
    ModelCapabilities,
    ModelInfo,
    ModelLimits,
    ModelSupports,
)
from copilot.session import PermissionHandler
from copilot.session_events import (
    McpOauthRequestReason,
    McpOauthRequiredData,
    McpOauthRequiredStaticClientConfig,
    McpOauthWWWAuthenticateParams,
    SessionEvent,
    SessionEventType,
)
from copilot.tools import Tool
from e2e.testharness import CLI_PATH


def test_inprocess_connection_has_no_child_process_options():
    connection = RuntimeConnection.for_inprocess()

    assert list(inspect.signature(RuntimeConnection.for_inprocess).parameters) == []
    assert not hasattr(connection, "path")
    assert not hasattr(connection, "args")


class TestClientShutdown:
    @pytest.mark.asyncio
    async def test_stop_requests_runtime_shutdown_for_owned_process(self):
        calls: list[str] = []
        process = Mock()
        process.poll.return_value = None
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
        # The runtime never self-exits after runtime.shutdown (it keeps its
        # JSON-RPC server alive to send the response and leaves termination to
        # the caller), so stop() terminates the owned process. The mocked
        # process exits on terminate() (wait returns immediately), so we never
        # escalate to kill().
        process.terminate.assert_called_once()
        process.kill.assert_not_called()

    @pytest.mark.asyncio
    async def test_force_stop_and_external_stop_do_not_request_runtime_shutdown(self):
        calls: list[str] = []
        process = Mock()

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
    async def test_force_stop_external_server_clears_process_references(self):
        process = Mock()
        client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
        client._is_external_server = True
        client._process = process
        client._cli_process = process

        await client.force_stop()

        process.terminate.assert_called_once()
        assert client._process is None
        assert client._cli_process is None


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
    async def test_mcp_auth_required_event_sends_host_token(self):
        client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
        await client.start()
        try:
            captured: list[tuple[str, dict]] = []

            async def mock_request(method, params, **kwargs):
                if method == "session.mcp.oauth.handlePendingRequest":
                    captured.append((method, params))
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
                        ),
                    ),
                    id="evt-1",
                    timestamp="2026-01-01T00:00:00Z",
                    type=SessionEventType.MCP_OAUTH_REQUIRED,
                    ephemeral=True,
                    parent_id=None,
                )
            )

            for _ in range(200):
                if captured:
                    break
                await asyncio.sleep(0.005)

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

            for _ in range(200):
                if observed_request is not None:
                    break
                await asyncio.sleep(0.005)

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
                excluded_builtin_agents=["explore"],
                session_limits={"max_ai_credits": 30},
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                enable_citations=False,
                excluded_builtin_agents=["task"],
                session_limits={"max_ai_credits": 15},
            )

            assert captured["session.create"]["enableCitations"] is True
            assert captured["session.create"]["excludedBuiltinAgents"] == ["explore"]
            assert captured["session.create"]["sessionLimits"] == {"maxAiCredits": 30}
            assert captured["session.resume"]["enableCitations"] is False
            assert captured["session.resume"]["excludedBuiltinAgents"] == ["task"]
            assert captured["session.resume"]["sessionLimits"] == {"maxAiCredits": 15}
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_and_resume_session_forward_capi_options(self):
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
            create_capi: CapiSessionOptions = {"enable_web_socket_responses": False}
            resume_capi: CapiSessionOptions = {"enable_web_socket_responses": True}

            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                capi=create_capi,
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                capi=resume_capi,
            )

            assert captured["session.create"]["capi"] == {
                "enableWebSocketResponses": False,
            }
            assert captured["session.resume"]["capi"] == {
                "enableWebSocketResponses": True,
            }
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
                large_output=large_output,
            )
            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                plugin_directories=plugin_dirs,
                large_output=large_output,
            )

            assert captured["session.create"]["pluginDirectories"] == plugin_dirs
            assert captured["session.create"]["largeOutput"] == expected_large_output_wire
            assert captured["session.resume"]["pluginDirectories"] == plugin_dirs
            assert captured["session.resume"]["largeOutput"] == expected_large_output_wire
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


class TestURLParsing:
    def test_parse_port_only_url(self):
        client = CopilotClient(connection=RuntimeConnection.for_uri("8080"))
        assert client._runtime_port == 8080
        assert client._actual_host == "localhost"
        assert client._is_external_server

    def test_parse_host_port_url(self):
        client = CopilotClient(connection=RuntimeConnection.for_uri("127.0.0.1:9000"))
        assert client._runtime_port == 9000
        assert client._actual_host == "127.0.0.1"
        assert client._is_external_server

    def test_parse_http_url(self):
        client = CopilotClient(connection=RuntimeConnection.for_uri("http://localhost:7000"))
        assert client._runtime_port == 7000
        assert client._actual_host == "localhost"
        assert client._is_external_server

    def test_parse_https_url(self):
        client = CopilotClient(connection=RuntimeConnection.for_uri("https://example.com:443"))
        assert client._runtime_port == 443
        assert client._actual_host == "example.com"
        assert client._is_external_server

    def test_invalid_url_format(self):
        with pytest.raises(ValueError, match="Invalid cli_url format"):
            CopilotClient(connection=RuntimeConnection.for_uri("invalid-url"))

    def test_invalid_port_too_high(self):
        with pytest.raises(ValueError, match="Invalid port in cli_url"):
            CopilotClient(connection=RuntimeConnection.for_uri("localhost:99999"))

    def test_invalid_port_zero(self):
        with pytest.raises(ValueError, match="Invalid port in cli_url"):
            CopilotClient(connection=RuntimeConnection.for_uri("localhost:0"))

    def test_invalid_port_negative(self):
        with pytest.raises(ValueError, match="Invalid port in cli_url"):
            CopilotClient(connection=RuntimeConnection.for_uri("localhost:-1"))

    def test_is_external_server_true(self):
        client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:8080"))
        assert client._is_external_server


class TestSessionFsConfig:
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
                    "max_prompt_tokens": 100_000,
                    "max_output_tokens": 4096,
                },
            )

            provider = captured["session.resume"]["provider"]
            assert provider["baseUrl"] == "https://example.com/provider"
            assert provider["headers"] == {"Authorization": "Bearer resume-token"}
            assert provider["modelId"] == "gpt-4o"
            assert provider["wireModel"] == "my-finetune-v3"
            assert provider["maxPromptTokens"] == 100_000
            assert provider["maxOutputTokens"] == 4096
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


class TestCustomAgentWireFormat:
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

    @pytest.mark.asyncio
    async def test_event_routes_to_handler(self):
        from copilot.generated.rpc import GitHubTelemetryNotification

        received: list = []

        def on_telemetry(notification):
            received.append(notification)

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

            # Notifications dispatch onto the event loop; yield until delivered.
            for _ in range(100):
                if received:
                    break
                await asyncio.sleep(0.01)

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
