# Copyright (c) Microsoft Corporation. All rights reserved.

from unittest.mock import Mock, patch

import pytest

from copilot import CopilotClient, RuntimeConnection


class TestURLParsing:
    @pytest.mark.parametrize(
        ("url", "host", "port"),
        [
            ("8080", "localhost", 8080),
            ("127.0.0.1:9000", "127.0.0.1", 9000),
            ("[::1]:9000", "::1", 9000),
            ("http://localhost:7000", "localhost", 7000),
            ("http://[::1]:7000", "::1", 7000),
            ("localhost:8080", "localhost", 8080),
            ("https:9000", "https", 9000),
        ],
    )
    def test_parse_raw_tcp_url(self, url, host, port):
        client = CopilotClient(connection=RuntimeConnection.for_uri(url))
        assert client.runtime_port == port
        assert client._actual_host == host
        assert client._is_external_server

    @pytest.mark.parametrize(
        "url",
        [
            "https://example.com:443",
            "HtTpS://example.com:443",
            "https://[::1]:9000",
            "https://localhost:9000",
        ],
    )
    def test_reject_https_url_before_connecting(self, url):
        with pytest.raises(
            ValueError,
            match="HTTPS runtime connections are not supported: "
            "this transport uses plaintext TCP, not TLS",
        ):
            CopilotClient(
                connection=RuntimeConnection.for_uri(url, connection_token="connection-secret")
            )

    @pytest.mark.parametrize(
        ("url", "message"),
        [
            ("[not-ipv6]:1234", "Invalid cli_url format"),
            ("invalid-url", "Invalid cli_url format"),
            ("localhost:99999", "Invalid port in cli_url"),
            ("localhost:0", "Invalid port in cli_url"),
            ("localhost:-1", "Invalid port in cli_url"),
        ],
    )
    def test_reject_invalid_url(self, url, message):
        with pytest.raises(ValueError, match=message):
            CopilotClient(connection=RuntimeConnection.for_uri(url))

    @pytest.mark.asyncio
    async def test_connect_via_tcp_uses_family_independent_resolution(self):
        client = CopilotClient(connection=RuntimeConnection.for_uri("[::1]:9000"))
        fake_socket = Mock()
        fake_socket.makefile.return_value = Mock()

        with patch("socket.create_connection", return_value=fake_socket) as create_connection:
            await client._connect_via_tcp()

        create_connection.assert_called_once_with(("::1", 9000), timeout=10)
        client._process.terminate()
