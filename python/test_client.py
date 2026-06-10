"""
CopilotClient Unit Tests

This file is for unit tests. Where relevant, prefer to add e2e tests in e2e/*.py instead.
"""

from datetime import UTC, datetime
from unittest.mock import AsyncMock, patch

import pytest

from copilot import (
    CopilotClient,
    RuntimeConnection,
    StdioRuntimeConnection,
    define_tool,
)
from copilot.client import (
    CloudSessionOptions,
    CloudSessionRepository,
    ModelCapabilities,
    ModelInfo,
    ModelLimits,
    ModelSupports,
)
from copilot.session import PermissionHandler
from e2e.testharness import CLI_PATH


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
                },
            )

            provider = captured["session.create"]["provider"]
            assert provider["baseUrl"] == "https://example.com/provider"
            assert provider["headers"] == {"Authorization": "Bearer provider-token"}
            assert provider["modelId"] == "gpt-4o"
            assert provider["wireModel"] == "my-finetune-v3"
            assert provider["maxPromptTokens"] == 100_000
            assert provider["maxOutputTokens"] == 4096
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
