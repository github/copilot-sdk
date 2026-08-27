"""CopilotSession unit tests."""

import asyncio
import logging
from datetime import UTC, datetime
from unittest.mock import AsyncMock, MagicMock, Mock
from uuid import uuid4

import pytest

from copilot.session import CommandDefinition, CopilotSession, McpAuthRequest
from copilot.session_events import (
    AssistantMessageData,
    ElicitationRequestedData,
    ExternalToolRequestedData,
    PermissionRequestedData,
    PermissionRequestRead,
    SessionEvent,
    SessionEventType,
    SessionIdleData,
    SessionMode,
)
from copilot.tools import Tool, define_tool


def _event(data, event_type: SessionEventType) -> SessionEvent:
    return SessionEvent(
        data=data,
        id=uuid4(),
        timestamp=datetime.now(UTC),
        type=event_type,
    )


def _session_with_mock_rpc() -> tuple[CopilotSession, MagicMock]:
    session = CopilotSession("session-1", client=None)
    rpc = MagicMock()
    rpc.tools.handle_pending_tool_call = AsyncMock()
    rpc.permissions.handle_pending_permission_request = AsyncMock()
    rpc.mcp.oauth.handle_pending_request = AsyncMock()
    rpc.commands.handle_pending_command = AsyncMock()
    rpc.ui.handle_pending_elicitation = AsyncMock()
    session._rpc = rpc
    return session, rpc


def _log_field(record: logging.LogRecord, name: str) -> object:
    return record.__dict__[name]


@pytest.mark.asyncio
async def test_send_and_wait_skips_autopilot_continuation_idle():
    client = Mock()
    client.request = AsyncMock(return_value={"messageId": "message-1"})
    session = CopilotSession("session-1", client)

    pending = asyncio.create_task(session.send_and_wait("keep going"))
    await asyncio.sleep(0)
    client.request.assert_awaited_once()

    session._dispatch_event(
        _event(
            AssistantMessageData(content="intermediate", message_id="assistant-1"),
            SessionEventType.ASSISTANT_MESSAGE,
        )
    )
    session._dispatch_event(
        _event(
            SessionIdleData(mode=SessionMode.AUTOPILOT),
            SessionEventType.SESSION_IDLE,
        )
    )
    assert not pending.done()

    session._dispatch_event(
        _event(
            AssistantMessageData(content="final", message_id="assistant-2"),
            SessionEventType.ASSISTANT_MESSAGE,
        )
    )
    session._dispatch_event(
        _event(
            SessionIdleData(mode=SessionMode.INTERACTIVE),
            SessionEventType.SESSION_IDLE,
        )
    )

    result = await asyncio.wait_for(pending, timeout=1)
    assert result is not None
    assert isinstance(result.data, AssistantMessageData)
    assert result.data.content == "final"


@pytest.mark.asyncio
async def test_tool_handler_failure_logs_stage_and_reports_rpc_error(
    caplog: pytest.LogCaptureFixture,
) -> None:
    session, rpc = _session_with_mock_rpc()
    caplog.set_level(logging.ERROR, logger="copilot.session")

    def handler(_invocation):
        raise RuntimeError("handler exploded")

    await session._execute_tool_and_respond(
        "request-1",
        "failing_tool",
        "tool-call-1",
        {"token": "top-secret"},
        handler,
    )

    sent = rpc.tools.handle_pending_tool_call.await_args.args[0].to_dict()
    assert sent == {"requestId": "request-1", "error": "handler exploded"}
    record = next(record for record in caplog.records if "Tool call failed" in record.message)
    assert record.levelno == logging.ERROR
    assert record.exc_info is not None
    assert _log_field(record, "stage") == "InvokingHandler"
    assert _log_field(record, "session_id") == "session-1"
    assert _log_field(record, "request_id") == "request-1"
    assert _log_field(record, "tool_call_id") == "tool-call-1"
    assert _log_field(record, "tool_name") == "failing_tool"
    assert "top-secret" not in caplog.text


@pytest.mark.asyncio
async def test_define_tool_exception_logs_original_traceback(
    caplog: pytest.LogCaptureFixture,
) -> None:
    session, rpc = _session_with_mock_rpc()
    caplog.set_level(logging.ERROR, logger="copilot.session")

    def handler() -> str:
        raise RuntimeError("decorated handler exploded")

    tool_factory = define_tool("decorated_tool")
    tool = tool_factory(handler)
    assert isinstance(tool, Tool)
    assert tool.handler is not None

    await session._execute_tool_and_respond(
        "request-1", "decorated_tool", "tool-call-1", {}, tool.handler
    )

    sent = rpc.tools.handle_pending_tool_call.await_args.args[0].to_dict()
    assert sent == {"requestId": "request-1", "error": "decorated handler exploded"}
    record = next(record for record in caplog.records if "Tool call failed" in record.message)
    assert record.levelno == logging.ERROR
    assert record.exc_info is not None
    assert record.exc_info[0] is RuntimeError
    assert _log_field(record, "stage") == "InvokingHandler"


def test_missing_tool_handler_logs_registered_tools_without_arguments(
    caplog: pytest.LogCaptureFixture,
) -> None:
    session, _rpc = _session_with_mock_rpc()
    session._register_tools([Tool(name="known_tool", description="", handler=lambda _inv: None)])
    caplog.set_level(logging.WARNING, logger="copilot.session")

    session._dispatch_event(
        _event(
            ExternalToolRequestedData(
                request_id="request-1",
                session_id="session-1",
                tool_call_id="tool-call-1",
                tool_name="missing_tool",
                arguments={"token": "top-secret"},
            ),
            SessionEventType.EXTERNAL_TOOL_REQUESTED,
        )
    )

    record = next(record for record in caplog.records if "no handler registered" in record.message)
    assert record.levelno == logging.WARNING
    assert _log_field(record, "session_id") == "session-1"
    assert _log_field(record, "request_id") == "request-1"
    assert _log_field(record, "tool_name") == "missing_tool"
    assert _log_field(record, "registered_tools") == "known_tool"
    assert "top-secret" not in caplog.text


def test_missing_permission_handler_logs_without_request_contents(
    caplog: pytest.LogCaptureFixture,
) -> None:
    session, _rpc = _session_with_mock_rpc()
    caplog.set_level(logging.WARNING, logger="copilot.session")

    session._dispatch_event(
        _event(
            PermissionRequestedData(
                request_id="permission-1",
                permission_request=PermissionRequestRead(
                    intention="read sensitive file",
                    path="sensitive-file.txt",
                ),
            ),
            SessionEventType.PERMISSION_REQUESTED,
        )
    )

    record = next(
        record for record in caplog.records if "registered permission handler" in record.message
    )
    assert record.levelno == logging.WARNING
    assert _log_field(record, "session_id") == "session-1"
    assert _log_field(record, "request_id") == "permission-1"
    assert "sensitive-file.txt" not in caplog.text


@pytest.mark.asyncio
async def test_command_handler_and_error_delivery_failures_are_logged(
    caplog: pytest.LogCaptureFixture,
) -> None:
    session, rpc = _session_with_mock_rpc()
    rpc.commands.handle_pending_command.side_effect = OSError("connection gone")
    session._register_commands(
        [
            CommandDefinition(
                name="deploy",
                handler=lambda _ctx: (_ for _ in ()).throw(RuntimeError("deploy failed")),
            )
        ]
    )
    caplog.set_level(logging.WARNING, logger="copilot.session")

    await session._execute_command_and_respond("command-1", "deploy", "/deploy prod", "prod")

    assert any(
        record.levelno == logging.ERROR
        and "Command handler or response delivery failed" in record.message
        and _log_field(record, "command_name") == "deploy"
        for record in caplog.records
    )
    assert any(
        record.levelno == logging.WARNING
        and "Failed to deliver the command error back to the runtime" in record.message
        and _log_field(record, "command_name") == "deploy"
        for record in caplog.records
    )
    assert "/deploy prod" not in caplog.text


@pytest.mark.asyncio
async def test_missing_command_handler_logs_registered_commands_without_command_text(
    caplog: pytest.LogCaptureFixture,
) -> None:
    session, rpc = _session_with_mock_rpc()
    session._register_commands([CommandDefinition(name="known", handler=lambda _ctx: None)])
    caplog.set_level(logging.WARNING, logger="copilot.session")

    await session._execute_command_and_respond(
        "command-1", "missing", "/missing secret-args", "secret-args"
    )

    sent = rpc.commands.handle_pending_command.await_args.args[0].to_dict()
    assert sent == {"requestId": "command-1", "error": "Unknown command: missing"}
    record = next(
        record
        for record in caplog.records
        if "command this client has no handler registered" in record.message
    )
    assert record.levelno == logging.WARNING
    assert _log_field(record, "command_name") == "missing"
    assert _log_field(record, "registered_commands") == "known"
    assert "secret-args" not in caplog.text


@pytest.mark.asyncio
async def test_elicitation_failure_and_cancellation_delivery_failure_are_logged(
    caplog: pytest.LogCaptureFixture,
) -> None:
    session, rpc = _session_with_mock_rpc()
    rpc.ui.handle_pending_elicitation.side_effect = OSError("connection gone")
    session._register_elicitation_handler(
        lambda _ctx: (_ for _ in ()).throw(RuntimeError("elicitation failed"))
    )
    caplog.set_level(logging.WARNING, logger="copilot.session")

    await session._handle_elicitation_request(
        {"session_id": "session-1", "message": "secret prompt"}, "elicitation-1"
    )

    assert any(
        record.levelno == logging.ERROR
        and "Elicitation handler or response delivery failed" in record.message
        for record in caplog.records
    )
    assert any(
        record.levelno == logging.WARNING
        and "Failed to deliver the elicitation cancellation back to the runtime" in record.message
        for record in caplog.records
    )
    assert "secret prompt" not in caplog.text


@pytest.mark.asyncio
async def test_mcp_oauth_failure_and_cancellation_delivery_failure_are_logged(
    caplog: pytest.LogCaptureFixture,
) -> None:
    session, rpc = _session_with_mock_rpc()
    rpc.mcp.oauth.handle_pending_request.side_effect = OSError("connection gone")
    caplog.set_level(logging.WARNING, logger="copilot.session")

    request: McpAuthRequest = {
        "requestId": "mcp-1",
        "serverName": "sensitive-server",
        "serverUrl": "https://sensitive.example",
        "reason": "initial",
    }
    await session._execute_mcp_auth_and_respond(
        request, lambda _request, _ctx: (_ for _ in ()).throw(RuntimeError("MCP auth failed"))
    )

    assert any(
        record.levelno == logging.WARNING
        and "MCP OAuth request failed; cancelling the pending request" in record.message
        for record in caplog.records
    )
    assert any(
        record.levelno == logging.WARNING
        and "Failed to deliver the MCP OAuth cancellation back to the runtime" in record.message
        for record in caplog.records
    )
    assert "sensitive-server" not in caplog.text
    assert "https://sensitive.example" not in caplog.text


def test_missing_elicitation_handler_logs_request_id_without_contents(
    caplog: pytest.LogCaptureFixture,
) -> None:
    session, _rpc = _session_with_mock_rpc()
    caplog.set_level(logging.WARNING, logger="copilot.session")

    session._dispatch_event(
        _event(
            ElicitationRequestedData(
                request_id="elicitation-1",
                message="secret prompt",
            ),
            SessionEventType.ELICITATION_REQUESTED,
        )
    )

    record = next(
        record for record in caplog.records if "registered elicitation handler" in record.message
    )
    assert record.levelno == logging.WARNING
    assert _log_field(record, "session_id") == "session-1"
    assert _log_field(record, "request_id") == "elicitation-1"
    assert "secret prompt" not in caplog.text
