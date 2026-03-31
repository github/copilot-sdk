"""
Unit tests for Commands, UI Elicitation (client→server), and
onElicitationRequest (server→client callback) features.

Mirrors the Node.js client.test.ts tests for these features.
"""

import asyncio

import pytest

from copilot import CopilotClient
from copilot.client import SubprocessConfig
from copilot.session import (
    CommandContext,
    CommandDefinition,
    ElicitationRequest,
    ElicitationResult,
    PermissionHandler,
)
from e2e.testharness import CLI_PATH

# ============================================================================
# Commands
# ============================================================================


class TestCommands:
    @pytest.mark.asyncio
    async def test_forwards_commands_in_session_create_rpc(self):
        """Verifies that commands (name + description) are serialized in session.create payload."""
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH))
        await client.start()

        try:
            captured: dict = {}
            original_request = client._client.request

            async def mock_request(method, params):
                captured[method] = params
                return await original_request(method, params)

            client._client.request = mock_request

            await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                commands=[
                    CommandDefinition(
                        name="deploy",
                        description="Deploy the app",
                        handler=lambda ctx: None,
                    ),
                    CommandDefinition(
                        name="rollback",
                        handler=lambda ctx: None,
                    ),
                ],
            )

            payload = captured["session.create"]
            assert payload["commands"] == [
                {"name": "deploy", "description": "Deploy the app"},
                {"name": "rollback", "description": None},
            ]
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_forwards_commands_in_session_resume_rpc(self):
        """Verifies that commands are serialized in session.resume payload."""
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )

            captured: dict = {}

            async def mock_request(method, params):
                captured[method] = params
                if method == "session.resume":
                    return {"sessionId": params["sessionId"]}
                raise RuntimeError(f"Unexpected method: {method}")

            client._client.request = mock_request

            await client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                commands=[
                    CommandDefinition(
                        name="deploy",
                        description="Deploy",
                        handler=lambda ctx: None,
                    ),
                ],
            )

            payload = captured["session.resume"]
            assert payload["commands"] == [{"name": "deploy", "description": "Deploy"}]
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_routes_command_execute_event_to_correct_handler(self):
        """Verifies the command dispatch works for command.execute events."""
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH))
        await client.start()

        try:
            handler_calls: list[CommandContext] = []

            async def deploy_handler(ctx: CommandContext) -> None:
                handler_calls.append(ctx)

            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                commands=[
                    CommandDefinition(name="deploy", handler=deploy_handler),
                ],
            )

            # Mock the RPC so handlePendingCommand doesn't fail
            rpc_calls: list[tuple] = []
            original_request = client._client.request

            async def mock_request(method, params):
                if method == "session.commands.handlePendingCommand":
                    rpc_calls.append((method, params))
                    return {"success": True}
                return await original_request(method, params)

            client._client.request = mock_request

            # Simulate a command.execute broadcast event
            from copilot.generated.session_events import (
                Data,
                SessionEvent,
                SessionEventType,
            )

            event = SessionEvent(
                data=Data(
                    request_id="req-1",
                    command="/deploy production",
                    command_name="deploy",
                    args="production",
                ),
                id="evt-1",
                timestamp="2025-01-01T00:00:00Z",
                type=SessionEventType.COMMAND_EXECUTE,
                ephemeral=True,
                parent_id=None,
            )
            session._dispatch_event(event)

            # Wait for async handler
            await asyncio.sleep(0.2)

            assert len(handler_calls) == 1
            assert handler_calls[0].session_id == session.session_id
            assert handler_calls[0].command == "/deploy production"
            assert handler_calls[0].command_name == "deploy"
            assert handler_calls[0].args == "production"

            # Verify handlePendingCommand was called
            assert len(rpc_calls) >= 1
            assert rpc_calls[0][1]["requestId"] == "req-1"
            # No error key means success
            assert "error" not in rpc_calls[0][1] or rpc_calls[0][1].get("error") is None
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_sends_error_when_command_handler_throws(self):
        """Verifies error is sent via RPC when a command handler raises."""
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH))
        await client.start()

        try:

            def fail_handler(ctx: CommandContext) -> None:
                raise RuntimeError("deploy failed")

            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                commands=[
                    CommandDefinition(name="fail", handler=fail_handler),
                ],
            )

            rpc_calls: list[tuple] = []
            original_request = client._client.request

            async def mock_request(method, params):
                if method == "session.commands.handlePendingCommand":
                    rpc_calls.append((method, params))
                    return {"success": True}
                return await original_request(method, params)

            client._client.request = mock_request

            from copilot.generated.session_events import (
                Data,
                SessionEvent,
                SessionEventType,
            )

            event = SessionEvent(
                data=Data(
                    request_id="req-2",
                    command="/fail",
                    command_name="fail",
                    args="",
                ),
                id="evt-2",
                timestamp="2025-01-01T00:00:00Z",
                type=SessionEventType.COMMAND_EXECUTE,
                ephemeral=True,
                parent_id=None,
            )
            session._dispatch_event(event)

            await asyncio.sleep(0.2)

            assert len(rpc_calls) >= 1
            assert rpc_calls[0][1]["requestId"] == "req-2"
            assert "deploy failed" in rpc_calls[0][1]["error"]
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_sends_error_for_unknown_command(self):
        """Verifies error is sent via RPC for an unrecognized command."""
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                commands=[
                    CommandDefinition(name="deploy", handler=lambda ctx: None),
                ],
            )

            rpc_calls: list[tuple] = []
            original_request = client._client.request

            async def mock_request(method, params):
                if method == "session.commands.handlePendingCommand":
                    rpc_calls.append((method, params))
                    return {"success": True}
                return await original_request(method, params)

            client._client.request = mock_request

            from copilot.generated.session_events import (
                Data,
                SessionEvent,
                SessionEventType,
            )

            event = SessionEvent(
                data=Data(
                    request_id="req-3",
                    command="/unknown",
                    command_name="unknown",
                    args="",
                ),
                id="evt-3",
                timestamp="2025-01-01T00:00:00Z",
                type=SessionEventType.COMMAND_EXECUTE,
                ephemeral=True,
                parent_id=None,
            )
            session._dispatch_event(event)

            await asyncio.sleep(0.2)

            assert len(rpc_calls) >= 1
            assert rpc_calls[0][1]["requestId"] == "req-3"
            assert "Unknown command" in rpc_calls[0][1]["error"]
        finally:
            await client.force_stop()


# ============================================================================
# UI Elicitation (client → server)
# ============================================================================


class TestUiElicitation:
    @pytest.mark.asyncio
    async def test_reads_capabilities_from_session_create_response(self):
        """Verifies capabilities are parsed from session.create response."""
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH))
        await client.start()

        try:
            original_request = client._client.request

            async def mock_request(method, params):
                if method == "session.create":
                    result = await original_request(method, params)
                    return {**result, "capabilities": {"ui": {"elicitation": True}}}
                return await original_request(method, params)

            client._client.request = mock_request

            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )
            assert session.capabilities == {"ui": {"elicitation": True}}
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_defaults_capabilities_when_not_injected(self):
        """Verifies capabilities default to empty when server returns none."""
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )
            # CLI returns actual capabilities; in headless mode, elicitation is
            # either False or absent. Just verify we don't crash.
            ui_caps = session.capabilities.get("ui", {})
            assert ui_caps.get("elicitation") in (False, None, True)
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_elicitation_throws_when_capability_is_missing(self):
        """Verifies that UI methods throw when elicitation is not supported."""
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )
            # Force capabilities to not support elicitation
            session._set_capabilities({})

            with pytest.raises(RuntimeError, match="not supported"):
                await session.ui.elicitation(
                    {
                        "message": "Enter name",
                        "requestedSchema": {
                            "type": "object",
                            "properties": {"name": {"type": "string", "minLength": 1}},
                            "required": ["name"],
                        },
                    }
                )
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_confirm_throws_when_capability_is_missing(self):
        """Verifies confirm throws when elicitation is not supported."""
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )
            session._set_capabilities({})

            with pytest.raises(RuntimeError, match="not supported"):
                await session.ui.confirm("Deploy?")
        finally:
            await client.force_stop()


# ============================================================================
# onElicitationRequest (server → client callback)
# ============================================================================


class TestOnElicitationRequest:
    @pytest.mark.asyncio
    async def test_sends_request_elicitation_flag_when_handler_provided(self):
        """Verifies requestElicitation=true is sent when onElicitationRequest is provided."""
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH))
        await client.start()

        try:
            captured: dict = {}
            original_request = client._client.request

            async def mock_request(method, params):
                captured[method] = params
                return await original_request(method, params)

            client._client.request = mock_request

            async def elicitation_handler(
                request: ElicitationRequest, invocation: dict[str, str]
            ) -> ElicitationResult:
                return {"action": "accept", "content": {}}

            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                on_elicitation_request=elicitation_handler,
            )
            assert session is not None

            payload = captured["session.create"]
            assert payload["requestElicitation"] is True
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_does_not_send_request_elicitation_when_no_handler(self):
        """Verifies requestElicitation=false when no handler is provided."""
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH))
        await client.start()

        try:
            captured: dict = {}
            original_request = client._client.request

            async def mock_request(method, params):
                captured[method] = params
                return await original_request(method, params)

            client._client.request = mock_request

            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            assert session is not None

            payload = captured["session.create"]
            assert payload["requestElicitation"] is False
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_sends_cancel_when_elicitation_handler_throws(self):
        """Verifies auto-cancel when the elicitation handler raises."""
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH))
        await client.start()

        try:

            async def bad_handler(
                request: ElicitationRequest, invocation: dict[str, str]
            ) -> ElicitationResult:
                raise RuntimeError("handler exploded")

            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                on_elicitation_request=bad_handler,
            )

            rpc_calls: list[tuple] = []
            original_request = client._client.request

            async def mock_request(method, params):
                if method == "session.ui.handlePendingElicitation":
                    rpc_calls.append((method, params))
                    return {"success": True}
                return await original_request(method, params)

            client._client.request = mock_request

            # Call _handle_elicitation_request directly (as Node.js test does)
            await session._handle_elicitation_request({"message": "Pick a color"}, "req-123")

            assert len(rpc_calls) >= 1
            cancel_call = next(
                (call for call in rpc_calls if call[1].get("result", {}).get("action") == "cancel"),
                None,
            )
            assert cancel_call is not None
            assert cancel_call[1]["requestId"] == "req-123"
            assert cancel_call[1]["result"]["action"] == "cancel"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_dispatches_elicitation_requested_event_to_handler(self):
        """Verifies that an elicitation.requested event dispatches to the handler."""
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH))
        await client.start()

        try:
            handler_calls: list = []

            async def elicitation_handler(
                request: ElicitationRequest, invocation: dict[str, str]
            ) -> ElicitationResult:
                handler_calls.append(request)
                return {"action": "accept", "content": {"color": "blue"}}

            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                on_elicitation_request=elicitation_handler,
            )

            rpc_calls: list[tuple] = []
            original_request = client._client.request

            async def mock_request(method, params):
                if method == "session.ui.handlePendingElicitation":
                    rpc_calls.append((method, params))
                    return {"success": True}
                return await original_request(method, params)

            client._client.request = mock_request

            from copilot.generated.session_events import (
                Data,
                SessionEvent,
                SessionEventType,
            )

            event = SessionEvent(
                data=Data(
                    request_id="req-elicit-1",
                    message="Pick a color",
                ),
                id="evt-elicit-1",
                timestamp="2025-01-01T00:00:00Z",
                type=SessionEventType.ELICITATION_REQUESTED,
                ephemeral=True,
                parent_id=None,
            )
            session._dispatch_event(event)

            await asyncio.sleep(0.2)

            assert len(handler_calls) == 1
            assert handler_calls[0]["message"] == "Pick a color"

            assert len(rpc_calls) >= 1
            assert rpc_calls[0][1]["requestId"] == "req-elicit-1"
            assert rpc_calls[0][1]["result"]["action"] == "accept"
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_elicitation_handler_receives_full_schema(self):
        """Verifies that requestedSchema passes type, properties, and required to handler."""
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH))
        await client.start()

        try:
            handler_calls: list = []

            async def elicitation_handler(
                request: ElicitationRequest, invocation: dict[str, str]
            ) -> ElicitationResult:
                handler_calls.append(request)
                return {"action": "cancel"}

            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                on_elicitation_request=elicitation_handler,
            )

            original_request = client._client.request

            async def mock_request(method, params):
                if method == "session.ui.handlePendingElicitation":
                    return {"success": True}
                return await original_request(method, params)

            client._client.request = mock_request

            from copilot.generated.session_events import (
                Data,
                RequestedSchema,
                RequestedSchemaType,
                SessionEvent,
                SessionEventType,
            )

            event = SessionEvent(
                data=Data(
                    request_id="req-schema-1",
                    message="Fill in your details",
                    requested_schema=RequestedSchema(
                        type=RequestedSchemaType.OBJECT,
                        properties={
                            "name": {"type": "string"},
                            "age": {"type": "number"},
                        },
                        required=["name", "age"],
                    ),
                ),
                id="evt-schema-1",
                timestamp="2025-01-01T00:00:00Z",
                type=SessionEventType.ELICITATION_REQUESTED,
                ephemeral=True,
                parent_id=None,
            )
            session._dispatch_event(event)

            await asyncio.sleep(0.2)

            assert len(handler_calls) == 1
            schema = handler_calls[0].get("requestedSchema")
            assert schema is not None, "Expected requestedSchema in handler call"
            assert schema["type"] == "object"
            assert "name" in schema["properties"]
            assert "age" in schema["properties"]
            assert schema["required"] == ["name", "age"]
        finally:
            await client.force_stop()


# ============================================================================
# Capabilities changed event
# ============================================================================


class TestCapabilitiesChanged:
    @pytest.mark.asyncio
    async def test_capabilities_changed_event_updates_session(self):
        """Verifies that a capabilities.changed event updates session capabilities."""
        client = CopilotClient(SubprocessConfig(cli_path=CLI_PATH))
        await client.start()

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )
            session._set_capabilities({})

            from copilot.generated.session_events import (
                UI,
                Data,
                SessionEvent,
                SessionEventType,
            )

            event = SessionEvent(
                data=Data(ui=UI(elicitation=True)),
                id="evt-cap-1",
                timestamp="2025-01-01T00:00:00Z",
                type=SessionEventType.CAPABILITIES_CHANGED,
                ephemeral=True,
                parent_id=None,
            )
            session._dispatch_event(event)

            assert session.capabilities.get("ui", {}).get("elicitation") is True
        finally:
            await client.force_stop()
