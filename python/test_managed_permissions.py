from copilot.rpc import PermissionDecisionApproveOnce
from copilot.session import PermissionHandler, PermissionNoResult
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


def test_approve_all_leaves_managed_request_pending() -> None:
    request = PermissionRequestRead(
        intention="Read managed content",
        path="/workspace/file.txt",
        managed_approval_required=True,
    )

    assert isinstance(
        PermissionHandler.approve_all(request, {"sessionId": "session-1"}), PermissionNoResult
    )


def test_approve_all_approves_ordinary_request() -> None:
    request = PermissionRequestRead(
        intention="Read ordinary content",
        path="/workspace/file.txt",
    )

    assert isinstance(
        PermissionHandler.approve_all(request, {"sessionId": "session-1"}),
        PermissionDecisionApproveOnce,
    )
