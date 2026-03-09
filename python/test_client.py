"""
CopilotClient Unit Tests

This file is for unit tests. Where relevant, prefer to add e2e tests in e2e/*.py instead.
"""

import pytest

from copilot import CopilotClient, PermissionHandler, define_tool
from copilot.types import ModelCapabilities, ModelInfo, ModelLimits, ModelSupports
from e2e.testharness import CLI_PATH


class TestPermissionHandlerRequired:
    @pytest.mark.asyncio
    async def test_create_session_raises_without_permission_handler(self):
        client = CopilotClient({"cli_path": CLI_PATH})
        await client.start()
        try:
            with pytest.raises(ValueError, match="on_permission_request.*is required"):
                await client.create_session({})
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_raises_without_permission_handler(self):
        client = CopilotClient({"cli_path": CLI_PATH})
        await client.start()
        try:
            session = await client.create_session(
                {"on_permission_request": PermissionHandler.approve_all}
            )
            with pytest.raises(ValueError, match="on_permission_request.*is required"):
                await client.resume_session(session.session_id, {})
        finally:
            await client.force_stop()


class TestURLParsing:
    def test_parse_port_only_url(self):
        client = CopilotClient({"cli_url": "8080", "log_level": "error"})
        assert client._actual_port == 8080
        assert client._actual_host == "localhost"
        assert client._is_external_server

    def test_parse_host_port_url(self):
        client = CopilotClient({"cli_url": "127.0.0.1:9000", "log_level": "error"})
        assert client._actual_port == 9000
        assert client._actual_host == "127.0.0.1"
        assert client._is_external_server

    def test_parse_http_url(self):
        client = CopilotClient({"cli_url": "http://localhost:7000", "log_level": "error"})
        assert client._actual_port == 7000
        assert client._actual_host == "localhost"
        assert client._is_external_server

    def test_parse_https_url(self):
        client = CopilotClient({"cli_url": "https://example.com:443", "log_level": "error"})
        assert client._actual_port == 443
        assert client._actual_host == "example.com"
        assert client._is_external_server

    def test_invalid_url_format(self):
        with pytest.raises(ValueError, match="Invalid cli_url format"):
            CopilotClient({"cli_url": "invalid-url", "log_level": "error"})

    def test_invalid_port_too_high(self):
        with pytest.raises(ValueError, match="Invalid port in cli_url"):
            CopilotClient({"cli_url": "localhost:99999", "log_level": "error"})

    def test_invalid_port_zero(self):
        with pytest.raises(ValueError, match="Invalid port in cli_url"):
            CopilotClient({"cli_url": "localhost:0", "log_level": "error"})

    def test_invalid_port_negative(self):
        with pytest.raises(ValueError, match="Invalid port in cli_url"):
            CopilotClient({"cli_url": "localhost:-1", "log_level": "error"})

    def test_cli_url_with_use_stdio(self):
        with pytest.raises(ValueError, match="cli_url is mutually exclusive"):
            CopilotClient({"cli_url": "localhost:8080", "use_stdio": True, "log_level": "error"})

    def test_cli_url_with_cli_path(self):
        with pytest.raises(ValueError, match="cli_url is mutually exclusive"):
            CopilotClient(
                {"cli_url": "localhost:8080", "cli_path": "/path/to/cli", "log_level": "error"}
            )

    def test_use_stdio_false_when_cli_url(self):
        client = CopilotClient({"cli_url": "8080", "log_level": "error"})
        assert not client.options["use_stdio"]

    def test_is_external_server_true(self):
        client = CopilotClient({"cli_url": "localhost:8080", "log_level": "error"})
        assert client._is_external_server


class TestAuthOptions:
    def test_accepts_github_token(self):
        client = CopilotClient(
            {"cli_path": CLI_PATH, "github_token": "gho_test_token", "log_level": "error"}
        )
        assert client.options.get("github_token") == "gho_test_token"

    def test_default_use_logged_in_user_true_without_token(self):
        client = CopilotClient({"cli_path": CLI_PATH, "log_level": "error"})
        assert client.options.get("use_logged_in_user") is True

    def test_default_use_logged_in_user_false_with_token(self):
        client = CopilotClient(
            {"cli_path": CLI_PATH, "github_token": "gho_test_token", "log_level": "error"}
        )
        assert client.options.get("use_logged_in_user") is False

    def test_explicit_use_logged_in_user_true_with_token(self):
        client = CopilotClient(
            {
                "cli_path": CLI_PATH,
                "github_token": "gho_test_token",
                "use_logged_in_user": True,
                "log_level": "error",
            }
        )
        assert client.options.get("use_logged_in_user") is True

    def test_explicit_use_logged_in_user_false_without_token(self):
        client = CopilotClient(
            {"cli_path": CLI_PATH, "use_logged_in_user": False, "log_level": "error"}
        )
        assert client.options.get("use_logged_in_user") is False

    def test_github_token_with_cli_url_raises(self):
        with pytest.raises(
            ValueError, match="github_token and use_logged_in_user cannot be used with cli_url"
        ):
            CopilotClient(
                {
                    "cli_url": "localhost:8080",
                    "github_token": "gho_test_token",
                    "log_level": "error",
                }
            )

    def test_use_logged_in_user_with_cli_url_raises(self):
        with pytest.raises(
            ValueError, match="github_token and use_logged_in_user cannot be used with cli_url"
        ):
            CopilotClient(
                {"cli_url": "localhost:8080", "use_logged_in_user": False, "log_level": "error"}
            )


class TestOverridesBuiltInTool:
    @pytest.mark.asyncio
    async def test_overrides_built_in_tool_sent_in_tool_definition(self):
        client = CopilotClient({"cli_path": CLI_PATH})
        await client.start()

        try:
            captured = {}
            original_request = client._client.request

            async def mock_request(method, params):
                captured[method] = params
                return await original_request(method, params)

            client._client.request = mock_request

            @define_tool(description="Custom grep", overrides_built_in_tool=True)
            def grep(params) -> str:
                return "ok"

            await client.create_session(
                {"tools": [grep], "on_permission_request": PermissionHandler.approve_all}
            )
            tool_defs = captured["session.create"]["tools"]
            assert len(tool_defs) == 1
            assert tool_defs[0]["name"] == "grep"
            assert tool_defs[0]["overridesBuiltInTool"] is True
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_sends_overrides_built_in_tool(self):
        client = CopilotClient({"cli_path": CLI_PATH})
        await client.start()

        try:
            session = await client.create_session(
                {"on_permission_request": PermissionHandler.approve_all}
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params):
                captured[method] = params
                return await original_request(method, params)

            client._client.request = mock_request

            @define_tool(description="Custom grep", overrides_built_in_tool=True)
            def grep(params) -> str:
                return "ok"

            await client.resume_session(
                session.session_id,
                {"tools": [grep], "on_permission_request": PermissionHandler.approve_all},
            )
            tool_defs = captured["session.resume"]["tools"]
            assert len(tool_defs) == 1
            assert tool_defs[0]["overridesBuiltInTool"] is True
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

        client = CopilotClient({"cli_path": CLI_PATH, "on_list_models": handler})
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

        client = CopilotClient({"cli_path": CLI_PATH, "on_list_models": handler})
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

        client = CopilotClient({"cli_path": CLI_PATH, "on_list_models": handler})
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

        client = CopilotClient({"cli_path": CLI_PATH, "on_list_models": handler})
        models = await client.list_models()
        assert len(handler_calls) == 1
        assert models == custom_models


class TestSessionConfigForwarding:
    @pytest.mark.asyncio
    async def test_create_session_forwards_client_name(self):
        client = CopilotClient({"cli_path": CLI_PATH})
        await client.start()

        try:
            captured = {}
            original_request = client._client.request

            async def mock_request(method, params):
                captured[method] = params
                return await original_request(method, params)

            client._client.request = mock_request
            await client.create_session(
                {"client_name": "my-app", "on_permission_request": PermissionHandler.approve_all}
            )
            assert captured["session.create"]["clientName"] == "my-app"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_forwards_client_name(self):
        client = CopilotClient({"cli_path": CLI_PATH})
        await client.start()

        try:
            session = await client.create_session(
                {"on_permission_request": PermissionHandler.approve_all}
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params):
                captured[method] = params
                if method == "session.resume":
                    # Return a fake response to avoid needing real auth
                    return {"sessionId": session.session_id}
                return await original_request(method, params)

            client._client.request = mock_request
            await client.resume_session(
                session.session_id,
                {"client_name": "my-app", "on_permission_request": PermissionHandler.approve_all},
            )
            assert captured["session.resume"]["clientName"] == "my-app"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_session_forwards_agent(self):
        client = CopilotClient({"cli_path": CLI_PATH})
        await client.start()

        try:
            captured = {}
            original_request = client._client.request

            async def mock_request(method, params):
                captured[method] = params
                return await original_request(method, params)

            client._client.request = mock_request
            await client.create_session(
                {
                    "agent": "test-agent",
                    "custom_agents": [{"name": "test-agent", "prompt": "You are a test agent."}],
                    "on_permission_request": PermissionHandler.approve_all,
                }
            )
            assert captured["session.create"]["agent"] == "test-agent"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_forwards_agent(self):
        client = CopilotClient({"cli_path": CLI_PATH})
        await client.start()

        try:
            session = await client.create_session(
                {"on_permission_request": PermissionHandler.approve_all}
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params):
                captured[method] = params
                if method == "session.resume":
                    return {"sessionId": session.session_id}
                return await original_request(method, params)

            client._client.request = mock_request
            await client.resume_session(
                session.session_id,
                {
                    "agent": "test-agent",
                    "custom_agents": [{"name": "test-agent", "prompt": "You are a test agent."}],
                    "on_permission_request": PermissionHandler.approve_all,
                },
            )
            assert captured["session.resume"]["agent"] == "test-agent"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_set_model_sends_correct_rpc(self):
        client = CopilotClient({"cli_path": CLI_PATH})
        await client.start()

        try:
            session = await client.create_session(
                {"on_permission_request": PermissionHandler.approve_all}
            )

            captured = {}
            original_request = client._client.request

            async def mock_request(method, params):
                captured[method] = params
                if method == "session.model.switchTo":
                    return {}
                return await original_request(method, params)

            client._client.request = mock_request
            await session.set_model("gpt-4.1")
            assert captured["session.model.switchTo"]["sessionId"] == session.session_id
            assert captured["session.model.switchTo"]["modelId"] == "gpt-4.1"
        finally:
            await client.force_stop()
