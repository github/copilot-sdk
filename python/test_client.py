"""
CopilotClient Unit Tests

This file is for unit tests. Where relevant, prefer to add e2e tests in e2e/*.py instead.
"""

import pytest

import copilot
from copilot import PermissionHandler
from e2e.testharness import CLI_PATH


class TestPermissionHandlerRequired:
    @pytest.mark.asyncio
    async def test_create_session_raises_without_permission_handler(self):
        client = copilot.cli_client(CLI_PATH)
        await client.start()
        try:
            with pytest.raises(ValueError, match="on_permission_request.*is required"):
                await client.create_session({})
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_resume_session_raises_without_permission_handler(self):
        client = copilot.cli_client(CLI_PATH)
        await client.start()
        try:
            session = await client.create_session(
                {"on_permission_request": PermissionHandler.approve_all}
            )
            with pytest.raises(ValueError, match="on_permission_request.*is required"):
                await client.resume_session(session.session_id, {})
        finally:
            await client.force_stop()


class TestHandleToolCallRequest:
    @pytest.mark.asyncio
    async def test_returns_failure_when_tool_not_registered(self):
        client = copilot.cli_client(CLI_PATH)
        await client.start()

        try:
            session = await client.create_session(
                {"on_permission_request": PermissionHandler.approve_all}
            )

            response = await client._handle_tool_call_request(
                {
                    "sessionId": session.session_id,
                    "toolCallId": "123",
                    "toolName": "missing_tool",
                    "arguments": {},
                }
            )

            assert response["result"]["resultType"] == "failure"
            assert response["result"]["error"] == "tool 'missing_tool' not supported"
        finally:
            await client.force_stop()


class TestURLParsing:
    def test_parse_port_only_url(self):
        client = copilot.network_client("8080", log_level="error")
        assert client._actual_port == 8080
        assert client._actual_host == "localhost"
        assert client._is_external_server

    def test_parse_host_port_url(self):
        client = copilot.network_client("127.0.0.1:9000", log_level="error")
        assert client._actual_port == 9000
        assert client._actual_host == "127.0.0.1"
        assert client._is_external_server

    def test_parse_http_url(self):
        client = copilot.network_client("http://localhost:7000", log_level="error")
        assert client._actual_port == 7000
        assert client._actual_host == "localhost"
        assert client._is_external_server

    def test_parse_https_url(self):
        client = copilot.network_client("https://example.com:443", log_level="error")
        assert client._actual_port == 443
        assert client._actual_host == "example.com"
        assert client._is_external_server

    def test_invalid_url_format(self):
        with pytest.raises(ValueError, match="Invalid cli_url format"):
            copilot.network_client("invalid-url", log_level="error")

    def test_invalid_port_too_high(self):
        with pytest.raises(ValueError, match="Invalid port in cli_url"):
            copilot.network_client("localhost:99999", log_level="error")

    def test_invalid_port_zero(self):
        with pytest.raises(ValueError, match="Invalid port in cli_url"):
            copilot.network_client("localhost:0", log_level="error")

    def test_invalid_port_negative(self):
        with pytest.raises(ValueError, match="Invalid port in cli_url"):
            copilot.network_client("localhost:-1", log_level="error")

    def test_use_stdio_false_when_network(self):
        client = copilot.network_client("8080", log_level="error")
        assert not client._use_stdio

    def test_is_external_server_true(self):
        client = copilot.network_client("localhost:8080", log_level="error")
        assert client._is_external_server


class TestAuthOptions:
    def test_accepts_github_token(self):
        client = copilot.cli_client(CLI_PATH, github_token="gho_test_token", log_level="error")
        assert client._github_token == "gho_test_token"

    def test_default_use_logged_in_user_true_without_token(self):
        client = copilot.cli_client(CLI_PATH, log_level="error")
        assert client._use_logged_in_user is True

    def test_default_use_logged_in_user_false_with_token(self):
        client = copilot.cli_client(CLI_PATH, github_token="gho_test_token", log_level="error")
        assert client._use_logged_in_user is False

    def test_explicit_use_logged_in_user_true_with_token(self):
        client = copilot.cli_client(
            CLI_PATH,
            github_token="gho_test_token",
            use_logged_in_user=True,
            log_level="error",
        )
        assert client._use_logged_in_user is True

    def test_explicit_use_logged_in_user_false_without_token(self):
        client = copilot.cli_client(CLI_PATH, use_logged_in_user=False, log_level="error")
        assert client._use_logged_in_user is False


class TestSessionConfigForwarding:
    @pytest.mark.asyncio
    async def test_create_session_forwards_client_name(self):
        client = copilot.cli_client(CLI_PATH)
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
        client = copilot.cli_client(CLI_PATH)
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
            await client.resume_session(
                session.session_id,
                {"client_name": "my-app", "on_permission_request": PermissionHandler.approve_all},
            )
            assert captured["session.resume"]["clientName"] == "my-app"
        finally:
            await client.force_stop()
