"""E2E Client Tests"""

import pytest

from copilot import CopilotClient, PermissionHandler, StopError

from .testharness import CLI_PATH


class TestClient:
    @pytest.mark.asyncio
    async def test_should_start_and_connect_to_server_using_stdio(self):
        client = CopilotClient({"cli_path": CLI_PATH, "use_stdio": True})

        try:
            await client.start()
            assert client.get_state() == "connected"

            pong = await client.ping("test message")
            assert pong.message == "pong: test message"
            assert pong.timestamp >= 0

            await client.stop()
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
            assert pong.message == "pong: test message"
            assert pong.timestamp >= 0

            await client.stop()
            assert client.get_state() == "disconnected"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_should_raise_exception_group_on_failed_cleanup(self):
        import asyncio

        client = CopilotClient({"cli_path": CLI_PATH})

        try:
            await client.create_session({"on_permission_request": PermissionHandler.approve_all})

            # Kill the server process to force cleanup to fail
            process = client._process
            assert process is not None
            process.kill()
            await asyncio.sleep(0.1)

            with pytest.raises(ExceptionGroup) as exc_info:
                await client.stop()
            assert len(exc_info.value.exceptions) > 0
            assert isinstance(exc_info.value.exceptions[0], StopError)
            assert "Failed to destroy session" in exc_info.value.exceptions[0].message
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_should_force_stop_without_cleanup(self):
        client = CopilotClient({"cli_path": CLI_PATH})

        await client.create_session({"on_permission_request": PermissionHandler.approve_all})
        await client.force_stop()
        assert client.get_state() == "disconnected"

    @pytest.mark.asyncio
    async def test_should_get_status_with_version_and_protocol_info(self):
        client = CopilotClient({"cli_path": CLI_PATH, "use_stdio": True})

        try:
            await client.start()

            status = await client.get_status()
            assert hasattr(status, "version")
            assert isinstance(status.version, str)
            assert hasattr(status, "protocolVersion")
            assert isinstance(status.protocolVersion, int)
            assert status.protocolVersion >= 1

            await client.stop()
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_should_get_auth_status(self):
        client = CopilotClient({"cli_path": CLI_PATH, "use_stdio": True})

        try:
            await client.start()

            auth_status = await client.get_auth_status()
            assert hasattr(auth_status, "isAuthenticated")
            assert isinstance(auth_status.isAuthenticated, bool)
            if auth_status.isAuthenticated:
                assert hasattr(auth_status, "authType")
                assert hasattr(auth_status, "statusMessage")

            await client.stop()
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_should_list_models_when_authenticated(self):
        client = CopilotClient({"cli_path": CLI_PATH, "use_stdio": True})

        try:
            await client.start()

            auth_status = await client.get_auth_status()
            if not auth_status.isAuthenticated:
                # Skip if not authenticated - models.list requires auth
                await client.stop()
                return

            models = await client.list_models()
            assert isinstance(models, list)
            if len(models) > 0:
                model = models[0]
                assert hasattr(model, "id")
                assert hasattr(model, "name")
                assert hasattr(model, "capabilities")
                assert hasattr(model.capabilities, "supports")
                assert hasattr(model.capabilities, "limits")

            await client.stop()
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_should_cache_models_list(self):
        """Test that list_models caches results to avoid rate limiting"""
        client = CopilotClient({"cli_path": CLI_PATH, "use_stdio": True})

        try:
            await client.start()

            auth_status = await client.get_auth_status()
            if not auth_status.isAuthenticated:
                # Skip if not authenticated - models.list requires auth
                await client.stop()
                return

            # First call should fetch from backend
            models1 = await client.list_models()
            assert isinstance(models1, list)

            # Second call should return from cache (different list object but same content)
            models2 = await client.list_models()
            assert models2 is not models1, "Should return a copy, not the same object"
            assert len(models2) == len(models1), "Cached results should have same content"
            if len(models1) > 0:
                assert models1[0].id == models2[0].id, "Cached models should match"

            # After stopping, cache should be cleared
            await client.stop()

            # Restart and verify cache is empty
            await client.start()

            # Check authentication again after restart
            auth_status = await client.get_auth_status()
            if not auth_status.isAuthenticated:
                await client.stop()
                return

            models3 = await client.list_models()
            assert models3 is not models1, "Cache should be cleared after disconnect"

            await client.stop()
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_should_report_error_with_stderr_when_cli_fails_to_start(self):
        """Test that CLI startup errors include stderr output in the error message."""
        client = CopilotClient(
            {
                "cli_path": CLI_PATH,
                "cli_args": ["--nonexistent-flag-for-testing"],
                "use_stdio": True,
            }
        )

        try:
            with pytest.raises(RuntimeError) as exc_info:
                await client.start()

            error_message = str(exc_info.value)
            # Verify we get the stderr output in the error message
            assert "stderr" in error_message, (
                f"Expected error to contain 'stderr', got: {error_message}"
            )
            assert "nonexistent" in error_message, (
                f"Expected error to contain 'nonexistent', got: {error_message}"
            )

            # Verify subsequent calls also fail (don't hang)
            with pytest.raises(Exception) as exc_info2:
                session = await client.create_session(
                    {"on_permission_request": PermissionHandler.approve_all}
                )
                await session.send("test")
            # Error message varies by platform (EINVAL on Windows, EPIPE on Linux)
            error_msg = str(exc_info2.value).lower()
            assert "invalid" in error_msg or "pipe" in error_msg or "closed" in error_msg
        finally:
            await client.force_stop()
