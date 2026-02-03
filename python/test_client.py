"""
CopilotClient Unit Tests

This file is for unit tests. Where relevant, prefer to add e2e tests in e2e/*.py instead.
"""

from unittest.mock import MagicMock, patch

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
        client = CopilotClient({"github_token": "gho_test_token", "log_level": "error"})
        assert client.options.get("github_token") == "gho_test_token"

    def test_default_use_logged_in_user_true_without_token(self):
        client = CopilotClient({"log_level": "error"})
        assert client.options.get("use_logged_in_user") is True

    def test_default_use_logged_in_user_false_with_token(self):
        client = CopilotClient({"github_token": "gho_test_token", "log_level": "error"})
        assert client.options.get("use_logged_in_user") is False

    def test_explicit_use_logged_in_user_true_with_token(self):
        client = CopilotClient(
            {"github_token": "gho_test_token", "use_logged_in_user": True, "log_level": "error"}
        )
        assert client.options.get("use_logged_in_user") is True

    def test_explicit_use_logged_in_user_false_without_token(self):
        client = CopilotClient({"use_logged_in_user": False, "log_level": "error"})
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


class TestCLIPathResolution:
    """Test that CLI path resolution works correctly, especially on Windows."""

    @pytest.mark.asyncio
    async def test_cli_path_resolved_with_which(self):
        """Test that shutil.which() is used to resolve the CLI path."""
        # Create a mock resolved path
        mock_resolved_path = "/usr/local/bin/copilot"

        with patch("copilot.client.shutil.which", return_value=mock_resolved_path):
            with patch("copilot.client.subprocess.Popen") as mock_popen:
                # Mock the process and its stdout for TCP mode
                mock_process = MagicMock()
                mock_process.stdout.readline.return_value = b"listening on port 8080\n"
                mock_popen.return_value = mock_process

                client = CopilotClient(
                    {"cli_path": "copilot", "use_stdio": False, "log_level": "error"}
                )

                try:
                    await client._start_cli_server()

                    # Verify that subprocess.Popen was called with the resolved path
                    mock_popen.assert_called_once()
                    args = mock_popen.call_args[0][0]
                    assert args[0] == mock_resolved_path
                finally:
                    if client._process:
                        client._process = None

    @pytest.mark.asyncio
    async def test_cli_path_not_resolved_when_which_returns_none(self):
        """Test that original path is used when shutil.which() returns None."""
        original_path = "/custom/path/to/copilot"

        with patch("copilot.client.shutil.which", return_value=None):
            with patch("copilot.client.subprocess.Popen") as mock_popen:
                # Mock the process and its stdout for TCP mode
                mock_process = MagicMock()
                mock_process.stdout.readline.return_value = b"listening on port 8080\n"
                mock_popen.return_value = mock_process

                client = CopilotClient(
                    {"cli_path": original_path, "use_stdio": False, "log_level": "error"}
                )

                try:
                    await client._start_cli_server()

                    # Verify that subprocess.Popen was called with the original path
                    mock_popen.assert_called_once()
                    args = mock_popen.call_args[0][0]
                    assert args[0] == original_path
                finally:
                    if client._process:
                        client._process = None
