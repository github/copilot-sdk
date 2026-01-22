"""E2E Client Tests"""

import pytest

from copilot import CopilotClient

from .testharness import CLI_PATH


class TestClient:
    @pytest.mark.asyncio
    async def test_should_start_and_connect_to_server_using_stdio(self):
        client = CopilotClient({"cli_path": CLI_PATH, "use_stdio": True})

        try:
            await client.start()
            assert client.get_state() == "connected"

            pong = await client.ping("test message")
            assert pong["message"] == "pong: test message"
            assert pong["timestamp"] >= 0

            errors = await client.stop()
            assert len(errors) == 0
            assert client.get_state() == "disconnected"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_should_start_and_connect_to_server_using_tcp(self):
        client = CopilotClient({"cli_path": CLI_PATH, "use_stdio": False})

        try:
            await client.start()
            assert client.get_state() == "connected"

            pong = await client.ping("test message")
            assert pong["message"] == "pong: test message"
            assert pong["timestamp"] >= 0

            errors = await client.stop()
            assert len(errors) == 0
            assert client.get_state() == "disconnected"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_should_return_errors_on_failed_cleanup(self):
        import asyncio

        client = CopilotClient({"cli_path": CLI_PATH})

        try:
            await client.create_session()

            # Kill the server process to force cleanup to fail
            process = client._process
            assert process is not None
            process.kill()
            await asyncio.sleep(0.1)

            errors = await client.stop()
            assert len(errors) > 0
            assert "Failed to destroy session" in errors[0]["message"]
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_should_force_stop_without_cleanup(self):
        client = CopilotClient({"cli_path": CLI_PATH})

        await client.create_session()
        await client.force_stop()
        assert client.get_state() == "disconnected"
