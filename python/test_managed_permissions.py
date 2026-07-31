from typing import Any

from copilot.rpc import PermissionDecisionApproveOnce, PermissionDecisionUserNotAvailable
from copilot.session import CopilotSession, PermissionHandler, PermissionNoResult
from copilot.session_events import PermissionRequestedData, PermissionRequestRead


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


def test_approve_all_approves_ordinary_request_even_with_managed_settings_enabled() -> None:
    request = PermissionRequestRead(
        intention="Read ordinary content",
        path="/workspace/file.txt",
    )

    assert isinstance(
        PermissionHandler.approve_all(
            request,
            {"session_id": "session-1", "managed_settings_enabled": True},
        ),
        PermissionDecisionApproveOnce,
    )


def test_approve_all_leaves_managed_request_pending() -> None:
    request = PermissionRequestRead(
        intention="Read managed content",
        path="/workspace/file.txt",
        managed_approval_required=True,
    )

    assert isinstance(
        PermissionHandler.approve_all(
            request,
            {"session_id": "session-1", "managed_settings_enabled": True},
        ),
        PermissionNoResult,
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

    legacy_invocation: Any = {"session_id": "session-1"}

    assert isinstance(PermissionHandler.approve_all(request, legacy_invocation), PermissionNoResult)


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
