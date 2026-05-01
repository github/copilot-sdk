"""E2E Client Tests"""

import pytest

from copilot import CopilotClient
from copilot.client import (
    ModelCapabilities,
    ModelInfo,
    ModelLimits,
    ModelSupports,
    StopError,
    SubprocessConfig,
)
from copilot.session import PermissionHandler

from .testharness import CLI_PATH


class TestClient:
    @pytest.mark.asyncio
    async def test_should_start_and_connect_to_server_using_stdio(self):
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH, use_stdio=True))

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
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH, use_stdio=False))

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

        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH))

        try:
            await client.create_session(on_permission_request=PermissionHandler.approve_all)

            # Kill the server process to force cleanup to fail
            process = client._process
            assert process is not None
            process.kill()
            await asyncio.sleep(0.1)

            try:
                await client.stop()
            except ExceptionGroup as exc:
                assert len(exc.exceptions) > 0
                assert isinstance(exc.exceptions[0], StopError)
                assert "Failed to disconnect session" in exc.exceptions[0].message
            else:
                assert client.get_state() == "disconnected"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_should_force_stop_without_cleanup(self):
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH))

        await client.create_session(on_permission_request=PermissionHandler.approve_all)
        await client.force_stop()
        assert client.get_state() == "disconnected"

    @pytest.mark.asyncio
    async def test_should_get_status_with_version_and_protocol_info(self):
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH, use_stdio=True))

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
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH, use_stdio=True))

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
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH, use_stdio=True))

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
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH, use_stdio=True))

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
            SubprocessConfig(
                cli_path=CLI_PATH,
                cli_args=["--nonexistent-flag-for-testing"],
                use_stdio=True,
            )
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
                    on_permission_request=PermissionHandler.approve_all
                )
                await session.send("test")
            # Error message varies by platform (EINVAL on Windows, EPIPE on Linux)
            error_msg = str(exc_info2.value).lower()
            assert "invalid" in error_msg or "pipe" in error_msg or "closed" in error_msg
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_should_not_throw_when_disposing_session_after_stopping_client(self):
        """Disconnecting a session after the client is stopped must not raise."""
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH, use_stdio=True))

        try:
            await client.start()
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )

            # Stop the client first; subsequent session disconnect should be harmless.
            await client.stop()

            # Should not raise.
            await session.disconnect()
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_should_throw_when_create_session_called_without_permission_handler(self):
        """`create_session` requires an `on_permission_request` handler."""
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH, use_stdio=True))

        try:
            await client.start()
            with pytest.raises((TypeError, ValueError)) as exc_info:
                await client.create_session()  # type: ignore[call-arg]

            message = str(exc_info.value)
            # Accept either 'on_permission_request' missing-arg or runtime validation error.
            assert "on_permission_request" in message or "permission" in message.lower(), (
                f"Expected message to reference permission handler, got: {message}"
            )

            await client.stop()
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_should_throw_when_resume_session_called_without_permission_handler(self):
        """`resume_session` requires an `on_permission_request` handler."""
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH, use_stdio=True))

        try:
            await client.start()
            with pytest.raises((TypeError, ValueError)) as exc_info:
                await client.resume_session("some-session-id")  # type: ignore[call-arg]

            message = str(exc_info.value)
            assert "on_permission_request" in message or "permission" in message.lower(), (
                f"Expected message to reference permission handler, got: {message}"
            )

            await client.stop()
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_list_models_with_custom_handler_calls_handler(self):
        """A custom `on_list_models` handler is invoked instead of the CLI RPC."""
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

        call_count = 0

        def on_list_models():
            nonlocal call_count
            call_count += 1
            return custom_models

        client = CopilotClient(
            SubprocessConfig(cli_path=CLI_PATH, use_stdio=True),
            on_list_models=on_list_models,
        )

        try:
            await client.start()

            models = await client.list_models()
            assert call_count == 1
            assert len(models) == 1
            assert models[0].id == "my-custom-model"

            await client.stop()
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_list_models_with_custom_handler_works_without_start(self):
        """The custom `on_list_models` handler is callable even before `start()`."""
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

        call_count = 0

        def on_list_models():
            nonlocal call_count
            call_count += 1
            return custom_models

        client = CopilotClient(
            SubprocessConfig(cli_path=CLI_PATH, use_stdio=True),
            on_list_models=on_list_models,
        )

        try:
            models = await client.list_models()
            assert call_count == 1
            assert len(models) == 1
            assert models[0].id == "no-start-model"
        finally:
            await client.force_stop()
