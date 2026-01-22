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

    @pytest.mark.asyncio
    async def test_should_get_status_with_version_and_protocol_info(self):
        client = CopilotClient({"cli_path": CLI_PATH, "use_stdio": True})

        try:
            await client.start()

            status = await client.get_status()
            assert "version" in status
            assert isinstance(status["version"], str)
            assert "protocolVersion" in status
            assert isinstance(status["protocolVersion"], int)
            assert status["protocolVersion"] >= 1

            await client.stop()
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_should_get_auth_status(self):
        client = CopilotClient({"cli_path": CLI_PATH, "use_stdio": True})

        try:
            await client.start()

            auth_status = await client.get_auth_status()
            assert "isAuthenticated" in auth_status
            assert isinstance(auth_status["isAuthenticated"], bool)
            if auth_status["isAuthenticated"]:
                assert "authType" in auth_status
                assert "statusMessage" in auth_status

            await client.stop()
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_should_list_models_when_authenticated(self):
        client = CopilotClient({"cli_path": CLI_PATH, "use_stdio": True})

        try:
            await client.start()

            auth_status = await client.get_auth_status()
            if not auth_status["isAuthenticated"]:
                # Skip if not authenticated - models.list requires auth
                await client.stop()
                return

            models = await client.list_models()
            assert isinstance(models, list)
            if len(models) > 0:
                model = models[0]
                assert "id" in model
                assert "name" in model
                assert "capabilities" in model
                assert "supports" in model["capabilities"]
                assert "limits" in model["capabilities"]

            await client.stop()
        finally:
            await client.force_stop()
