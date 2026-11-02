import pytest

from copilot.rpc import PermissionDecisionApproveOnce, PermissionDecisionUserNotAvailable
from copilot.session import CopilotSession, PermissionHandler, PermissionNoResult
from copilot.session_events import (
    AssistedApprovalRecommendation,
    PermissionPromptRequestCommands,
    PermissionRequestCustomTool,
    PermissionRequestedData,
    PermissionRequestRead,
)


def test_permission_event_exposes_managed_approval_required() -> None:
    data = PermissionRequestedData.from_dict(
        {
            "permissionRequest": {
                "kind": "read",
                "intention": "Read managed content",
                "path": "/workspace/file.txt",
                "managedApprovalRequired": True,
            },
            "requestId": "permission-1",
        }
    )

    assert data.permission_request.managed_approval_required is True
    assert data.to_dict()["permissionRequest"]["managedApprovalRequired"] is True


def test_managed_metadata_preserves_existing_positional_constructor_order() -> None:
    request = PermissionRequestCustomTool(
        "Run a custom tool",
        "custom_tool",
        {"value": 1},
        "tool-call-1",
    )

    assert request.tool_call_id == "tool-call-1"
    assert request.managed_approval_required is None

    read_request = PermissionRequestRead(
        "Read content",
        "/workspace/file.txt",
        True,
        False,
        "Use the sandbox",
        "tool-call-2",
    )

    assert read_request.managed_approval_required is True
    assert read_request.request_sandbox_bypass is False
    assert read_request.request_sandbox_bypass_reason == "Use the sandbox"
    assert read_request.tool_call_id == "tool-call-2"


def test_approve_all_rejects_managed_settings_session() -> None:
    request = PermissionRequestRead(
        intention="Read ordinary content",
        path="/workspace/file.txt",
    )

    with pytest.raises(RuntimeError, match="managed settings are enabled"):
        PermissionHandler.approve_all(
            request,
            {"session_id": "session-1", "managed_settings_enabled": True},
        )


def test_approve_all_rejects_managed_request_in_managed_settings_session() -> None:
    request = PermissionRequestRead(
        intention="Read managed content",
        path="/workspace/file.txt",
        managed_approval_required=True,
    )

    with pytest.raises(RuntimeError, match="managed settings are enabled"):
        PermissionHandler.approve_all(
            request,
            {"session_id": "session-1", "managed_settings_enabled": True},
        )


def test_approve_all_approves_ordinary_request() -> None:
    request = PermissionRequestRead(
        intention="Read ordinary content",
        path="/workspace/file.txt",
    )

    assert isinstance(
        PermissionHandler.approve_all(
            request,
            {"session_id": "session-1", "managed_settings_enabled": False},
        ),
        PermissionDecisionApproveOnce,
    )


def test_approve_all_leaves_managed_request_pending_when_session_flag_is_absent() -> None:
    request = PermissionRequestRead(
        intention="Read managed content",
        path="/workspace/file.txt",
        managed_approval_required=True,
    )

    assert isinstance(
        PermissionHandler.approve_all(request, {"session_id": "session-1"}),
        PermissionNoResult,
    )


async def test_legacy_permission_callback_rejects_no_result() -> None:
    request = PermissionRequestRead(
        intention="Read managed content",
        path="/workspace/file.txt",
        managed_approval_required=True,
    )
    session = CopilotSession("session-1", client=None)
    session._register_permission_handler(lambda _request, _invocation: PermissionNoResult())

    result = await session._handle_permission_request(request)

    assert isinstance(result, PermissionDecisionUserNotAvailable)


async def test_permission_callback_receives_derived_prompt_context() -> None:
    data = PermissionRequestedData.from_dict(
        {
            "requestId": "permission-1",
            "permissionRequest": {
                "kind": "shell",
                "fullCommandText": "ls",
                "intention": "List files",
                "commands": [],
                "possiblePaths": [],
                "possibleUrls": [],
                "hasWriteFileRedirection": False,
                "canOfferSessionApproval": False,
                "managedApprovalRequired": True,
            },
            "promptRequest": {
                "kind": "commands",
                "fullCommandText": "ls",
                "intention": "List files",
                "commandIdentifiers": ["ls"],
                "canOfferSessionApproval": False,
                "assistedApproval": {"recommendation": "approve"},
            },
        }
    )
    captured = {}
    session = CopilotSession("session-1", client=None, managed_settings_enabled=True)

    def handler(_request, invocation):
        captured.update(invocation)
        return PermissionNoResult()

    await session._execute_permission_and_respond(
        data.request_id,
        data.permission_request,
        handler,
        data.prompt_request,
    )

    prompt_request = captured["prompt_request"]
    assert isinstance(prompt_request, PermissionPromptRequestCommands)
    assert prompt_request.managed_approval_required is None
    assert prompt_request.assisted_approval is not None
    assert prompt_request.assisted_approval.recommendation is AssistedApprovalRecommendation.APPROVE
    assert captured["managed_settings_enabled"] is True
