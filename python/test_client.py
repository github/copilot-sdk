"""
CopilotClient Unit Tests

This file is for unit tests. Where relevant, prefer to add e2e tests in e2e/*.py instead.
"""

import pytest

from copilot import CopilotClient
from e2e.testharness import CLI_PATH


class TestHandleToolCallRequest:
    @pytest.mark.asyncio
    async def test_returns_failure_when_tool_not_registered(self):
        client = CopilotClient({"cli_path": CLI_PATH})
        await client.start()

        try:
            session = await client.create_session()

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
