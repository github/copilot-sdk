# Copyright (c) Microsoft Corporation. All rights reserved.

"""Existing callers keep their construction style when an RPC record gains fields."""

import dataclasses

import pytest

from copilot.generated import rpc
from copilot.generated.session_events import (
    AssistedApprovalRecommendation,
    PermissionAssistedApproval,
    SessionInfoData,
    UserMessageData,
    UserMessageDelivery,
)


def _contract() -> rpc.CatalogClientContract:
    return rpc.CatalogClientContract(
        protocol_version=3, required_capabilities=["mcp-install-planning"]
    )


def _candidate() -> rpc.MCPPlanInstallSourceCandidate:
    return rpc.MCPPlanInstallSourceCandidate(candidate_handle="candidate", search_id="search")


def _positional(cls: type) -> list[str]:
    return [field.name for field in dataclasses.fields(cls) if not field.kw_only]


def test_session_info_preserves_positional_arguments_and_internal_metadata():
    info = SessionInfoData("notification", "message", "tip", "https://example.com")
    expected = {
        "infoType": "notification",
        "message": "message",
        "tip": "tip",
        "url": "https://example.com",
    }

    assert _positional(SessionInfoData) == ["info_type", "message", "tip", "url"]
    assert info.to_dict() == expected
    wire = {
        **expected,
        "responsesReasoning": {
            "model": "model",
            "initialEffort": "low",
            "effort": "high",
        },
    }
    assert SessionInfoData.from_dict(wire).to_dict() == wire


def test_managed_settings_resolve_preserves_existing_positional_arguments():
    request = rpc.ManagedSettingsResolveRequest("my-app", None, "account-123", "/work/project")
    expected = {
        "clientName": "my-app",
        "selectionId": "account-123",
        "workingDirectory": "/work/project",
    }

    assert request.selection_id == "account-123"
    assert request.working_directory == "/work/project"
    assert _positional(rpc.ManagedSettingsResolveRequest) == [
        "client_name",
        "git_hub_token",
        "selection_id",
        "working_directory",
    ]
    assert request.to_dict() == expected
    assert rpc.ManagedSettingsResolveRequest.from_dict(expected) == request


@pytest.mark.parametrize(
    ("record", "expected"),
    [
        (
            rpc.Plugin(True, "market", "plugin", "direct-source"),
            {
                "enabled": True,
                "marketplace": "market",
                "name": "plugin",
                "directSourceId": "direct-source",
            },
        ),
        (
            rpc.InstalledPluginInfo(True, "market", "plugin", "description"),
            {
                "enabled": True,
                "marketplace": "market",
                "name": "plugin",
                "description": "description",
            },
        ),
        (
            rpc.MarketplacePluginInfo("plugin", "description"),
            {"name": "plugin", "description": "description"},
        ),
    ],
)
def test_plugin_records_preserve_existing_positional_arguments(record, expected):
    assert record.assignment is None
    assert record.to_dict() == expected
    assert type(record).from_dict(expected) == record


@pytest.mark.parametrize(
    "record_type", [rpc.Plugin, rpc.InstalledPluginInfo, rpc.MarketplacePluginInfo]
)
def test_plugin_assignment_round_trips_by_keyword(record_type):
    assignment = {"state": "defaultEnabled", "reason": "Company setup"}
    record = record_type.from_dict(
        {"name": "plugin", "marketplace": "market", "enabled": True, "assignment": assignment}
    )

    assert record_type.__dataclass_fields__["assignment"].kw_only
    assert record.assignment.state == "defaultEnabled"
    assert record.to_dict()["assignment"] == assignment
    assert record_type.from_dict(record.to_dict()) == record


def test_plan_install_positional_scope_still_binds_scope():
    request = rpc.MCPPlanInstallRequest(_contract(), _candidate(), rpc.MCPPlanScope.USER)

    assert request.scope is rpc.MCPPlanScope.USER
    assert request.policy_session_id is None
    assert "policySessionId" not in request.to_dict()
    assert request.to_dict()["scope"] == "user"


def test_added_fields_are_keyword_only():
    assert _positional(rpc.MCPPlanInstallRequest) == ["contract", "source", "scope"]
    assert _positional(rpc.CatalogSearchRequest) == ["contract", "query", "kinds", "limit", "page"]
    with pytest.raises(TypeError):
        rpc.MCPPlanInstallRequest(_contract(), _candidate(), rpc.MCPPlanScope.USER, "session")


def test_mcp_app_tool_call_preserves_positional_arguments():
    request = rpc.MCPAppsCallToolRequest("server", "server", "tool", {"value": 42})
    expected = {
        "originServerName": "server",
        "serverName": "server",
        "toolName": "tool",
        "arguments": {"value": 42},
    }

    assert request.to_dict() == expected
    assert rpc.MCPAppsCallToolRequest.from_dict(expected) == request


@pytest.mark.parametrize("meta", [{}, {"com.example/context": {"enabled": False, "value": None}}])
def test_mcp_app_tool_call_metadata_round_trips_by_keyword(meta):
    request = rpc.MCPAppsCallToolRequest("server", "server", "tool", {"value": 42}, meta=meta)
    expected = {
        "originServerName": "server",
        "serverName": "server",
        "toolName": "tool",
        "arguments": {"value": 42},
        "_meta": meta,
    }

    assert request.to_dict() == expected
    assert rpc.MCPAppsCallToolRequest.from_dict(expected) == request


def test_event_log_read_positional_arguments_keep_their_meaning():
    request = rpc.EventLogReadRequest(
        ["agent"],
        rpc.EventsAgentScope.PRIMARY,
        "cursor",
        rpc.EventsReadDirection.BACKWARD,
        False,
        100,
        ["user.message"],
        1234,
    )

    assert request.to_dict() == {
        "agentIds": ["agent"],
        "agentScope": "primary",
        "cursor": "cursor",
        "direction": "backward",
        "includeEphemeral": False,
        "max": 100,
        "types": ["user.message"],
        "waitMs": 1234,
    }
    assert _positional(rpc.EventLogReadRequest) == [
        "agent_ids",
        "agent_scope",
        "cursor",
        "direction",
        "include_ephemeral",
        "max",
        "types",
        "wait_ms",
    ]


def test_event_log_read_view_round_trips_by_keyword():
    request = rpc.EventLogReadRequest(wait_ms=1234, view=rpc.EventsReadView.REPLAY)

    assert rpc.EventLogReadRequest.__dataclass_fields__["view"].kw_only
    assert request.to_dict() == {"waitMs": 1234, "view": "replay"}
    assert rpc.EventLogReadRequest.from_dict(request.to_dict()) == request


def test_search_positional_arguments_keep_their_meaning():
    request = rpc.CatalogSearchRequest(_contract(), "catalogue query", None, 4)

    assert request.limit == 4
    assert request.policy_session_id is None
    assert "policySessionId" not in request.to_dict()


def test_new_fields_round_trip_by_keyword():
    plan = rpc.MCPPlanInstallRequest(
        _contract(), _candidate(), rpc.MCPPlanScope.USER, policy_session_id="session"
    )
    search = rpc.CatalogSearchRequest(_contract(), "catalogue query", policy_session_id="session")

    assert rpc.MCPPlanInstallRequest.from_dict(plan.to_dict()) == plan
    assert rpc.CatalogSearchRequest.from_dict(search.to_dict()) == search
    assert plan.to_dict()["policySessionId"] == "session"
    assert search.to_dict()["policySessionId"] == "session"


def test_existing_action_export_keeps_its_name():
    assert "Action" in rpc.__all__
    assert {member.value for member in rpc.Action} == {"preserve", "transform"}
    assert rpc.ProtocolMarkerSectionOverride.__dataclass_fields__["action"].type in (
        rpc.Action,
        "Action",
    )


def test_listed_server_positional_arguments_keep_their_meaning():
    metadata = rpc.McpServerMetadata(instructions="use it")
    server = rpc.MCPServer("server", rpc.McpServerStatus.CONNECTED, "Server", "failed", metadata)

    assert server.server_metadata == metadata
    assert server.owned is None
    assert "owned" not in server.to_dict()
    assert _positional(rpc.MCPServer) == [
        "name",
        "status",
        "display_name",
        "error",
        "server_metadata",
        "source",
        "source_plugin",
        "source_plugin_version",
    ]


def test_listed_server_owned_marker_round_trips_by_keyword():
    server = rpc.MCPServer.from_dict(
        {"name": "server", "status": "stopped", "owned": {"installationId": "installation"}}
    )

    assert server.owned.installation_id == "installation"
    assert rpc.MCPServer.from_dict(server.to_dict()) == server
    assert server.to_dict()["owned"] == {"installationId": "installation"}


def test_workspace_diff_positional_arguments_keep_their_meaning():
    change = rpc.WorkspaceDiffFileChange(
        rpc.WorkspaceDiffFileChangeType.MODIFIED, "patch", "app.txt", True, "old.txt"
    )

    assert (change.is_truncated, change.old_path, change.contents, _positional(type(change))) == (
        True,
        "old.txt",
        None,
        ["change_type", "diff", "path", "is_truncated", "old_path"],
    )
    assert "contents" not in change.to_dict()


def test_workspace_diff_contents_round_trip_by_keyword():
    contents = rpc.WorkspaceDiffContents(before="port=3000\n", after="port=4000\n")
    change = rpc.WorkspaceDiffFileChange(
        rpc.WorkspaceDiffFileChangeType.MODIFIED,
        "patch",
        "app.txt",
        False,
        None,
        contents=contents,
    )
    encoded = change.to_dict()

    assert (encoded, rpc.WorkspaceDiffFileChange.from_dict(encoded)) == (
        {
            "changeType": "modified",
            "diff": "patch",
            "path": "app.txt",
            "isTruncated": False,
            "contents": {"before": "port=3000\n", "after": "port=4000\n"},
        },
        change,
    )


def test_user_message_positional_arguments_keep_their_meaning():
    message = UserMessageData("hi", None, None, None, UserMessageDelivery.STEERING)

    assert message.delivery == UserMessageDelivery.STEERING
    assert message.client_turn_id is None
    assert _positional(UserMessageData) == [
        "content",
        "agent_mode",
        "attachments",
        "causal_dependencies",
        "delivery",
        "interaction_id",
        "is_autopilot_continuation",
        "message_id",
        "native_document_path_fallback_paths",
        "parent_agent_task_id",
        "responses_reasoning",
        "source",
        "supported_native_document_mime_types",
        "transformed_content",
        "turn_id",
        "client_turn_id",
        "has_submitted_task",
    ]
    assert message.to_dict() == {"content": "hi", "delivery": "steering"}


def test_user_message_client_turn_id_round_trips_by_keyword():
    message = UserMessageData("hi", client_turn_id="host-turn-1")
    encoded = message.to_dict()

    assert encoded == {"content": "hi", "clientTurnId": "host-turn-1"}
    assert UserMessageData.from_dict(encoded) == message


def test_permission_assisted_approval_positional_arguments_keep_their_meaning():
    approval = PermissionAssistedApproval(
        AssistedApprovalRecommendation.REQUIRE_APPROVAL, None, "evaluation-1"
    )

    assert approval.evaluation_id == "evaluation-1"
    assert approval.approval_required_command_segments is None
    assert _positional(PermissionAssistedApproval) == [
        "recommendation",
        "evaluation",
        "evaluation_id",
        "failure_reason",
        "model",
        "reason",
        "approval_required_command_segments",
    ]
    attributed = PermissionAssistedApproval(
        AssistedApprovalRecommendation.REQUIRE_APPROVAL,
        approval_required_command_segments=[1],
    )
    encoded = attributed.to_dict()
    assert encoded == {
        "recommendation": "requireApproval",
        "approvalRequiredCommandSegments": [1],
    }
    assert PermissionAssistedApproval.from_dict(encoded) == attributed


def test_send_positional_arguments_keep_their_meaning():
    request = rpc.SendRequest("hi", None, None, True)

    assert request.billable is True
    assert _positional(rpc.SendRequest) == [
        "prompt",
        "agent_mode",
        "attachments",
        "billable",
        "display_prompt",
        "mode",
        "prepend",
        "request_headers",
        "required_tool",
        "response_format",
        "source",
        "traceparent",
        "tracestate",
        "wait",
    ]
    assert request.to_dict() == {"prompt": "hi", "billable": True}


def test_send_messages_positional_arguments_keep_their_meaning():
    request = rpc.SendMessagesRequest([], None, None, True)

    assert request.prepend is True
    assert _positional(rpc.SendMessagesRequest) == [
        "messages",
        "agent_mode",
        "mode",
        "prepend",
        "request_headers",
        "response_format",
        "traceparent",
        "tracestate",
        "wait",
    ]
    assert request.to_dict() == {"messages": [], "prepend": True}


@pytest.mark.parametrize(
    ("request_type", "required"),
    [(rpc.SendRequest, {"prompt": "hi"}), (rpc.SendMessagesRequest, {"messages": []})],
)
def test_send_client_turn_id_round_trips_by_keyword(request_type, required):
    request = request_type.from_dict({**required, "clientTurnId": "host-turn-1"})

    assert request.client_turn_id == "host-turn-1"
    assert request.to_dict() == {**required, "clientTurnId": "host-turn-1"}
    assert request_type(*required.values(), client_turn_id="host-turn-1") == request
