"""Existing callers keep their construction style when a request gains fields."""

import dataclasses

import pytest

from copilot.generated import rpc


def _contract() -> rpc.CatalogClientContract:
    return rpc.CatalogClientContract(
        protocol_version=3, required_capabilities=["mcp-install-planning"]
    )


def _candidate() -> rpc.MCPPlanInstallSourceCandidate:
    return rpc.MCPPlanInstallSourceCandidate(candidate_handle="candidate", search_id="search")


def _positional(cls: type) -> list[str]:
    return [field.name for field in dataclasses.fields(cls) if not field.kw_only]


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
