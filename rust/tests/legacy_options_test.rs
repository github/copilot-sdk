// Existing request structs keep their published shape; inputs added later are only
// reachable through the private-field options types, which serialise flat.

#![allow(clippy::unwrap_used)]

use github_copilot_sdk::Error;
use github_copilot_sdk::rpc::{
    CatalogClientContract, CatalogSearchOptions, CatalogSearchRequest, CatalogSearchResult,
    ClientRpcCatalog, ClientRpcMcp, ClientRpcSessions, McpPlanInstallOptions,
    McpPlanInstallRequest, McpPlanInstallResult, McpPlanInstallSource,
    McpPlanInstallSourceCandidate, McpPlanInstallSourceCandidateKind, McpPlanScope,
    PermissionsSetRequiredOptions, PermissionsSetRequiredRequest, PermissionsSetRequiredResult,
    SessionRpcPermissions, SessionsCloseOptions, SessionsCloseRequest, SessionsCloseResult,
};
use serde_json::json;

#[test]
fn permission_required_preserves_exhaustive_request_literals() {
    let request = PermissionsSetRequiredRequest { required: true };
    assert_eq!(
        serde_json::to_value(request).unwrap(),
        json!({"required": true})
    );
}

#[test]
fn permission_required_options_preserve_host_and_provider_wire_inputs() {
    for required in [false, true] {
        assert_eq!(
            serde_json::to_value(PermissionsSetRequiredOptions::new(required)).unwrap(),
            json!({"required": required})
        );
        assert_eq!(
            serde_json::to_value(
                PermissionsSetRequiredOptions::new(required).provider_id("provider"),
            )
            .unwrap(),
            json!({"required": required, "providerId": "provider"})
        );
    }
}

#[test]
fn permission_requirements_preserve_exhaustive_literals_and_provider_options() {
    let request = PermissionsSetRequiredRequest { required: true };
    assert_eq!(
        serde_json::to_value(request).unwrap(),
        json!({"required": true})
    );
    assert_eq!(
        serde_json::to_value(PermissionsSetRequiredOptions::new(false)).unwrap(),
        json!({"required": false})
    );
    assert_eq!(
        serde_json::to_value(PermissionsSetRequiredOptions::new(false).provider_id("provider"))
            .unwrap(),
        json!({"required": false, "providerId": "provider"})
    );
}

#[allow(dead_code)]
async fn permission_required_entry_points_compile(
    permissions: SessionRpcPermissions<'_>,
) -> Result<PermissionsSetRequiredResult, Error> {
    permissions
        .set_required(PermissionsSetRequiredRequest { required: true })
        .await?;
    permissions
        .set_required_with_options(
            PermissionsSetRequiredOptions::new(false).provider_id("provider"),
        )
        .await
}

#[test]
fn session_close_preserves_exhaustive_request_literals() {
    let request = SessionsCloseRequest {
        session_id: "legacy".into(),
    };
    assert_eq!(
        serde_json::to_value(request).unwrap(),
        json!({"sessionId": "legacy"})
    );
}

#[test]
fn session_close_options_preserve_absent_and_explicit_detach_values() {
    let options = SessionsCloseOptions::new("legacy".into());
    assert_eq!(
        serde_json::to_value(options).unwrap(),
        json!({"sessionId": "legacy"})
    );
    for detach in [false, true] {
        let options = SessionsCloseOptions::new("legacy".into()).detach_session_end_hooks(detach);
        assert_eq!(
            serde_json::to_value(options).unwrap(),
            json!({"sessionId": "legacy", "detachSessionEndHooks": detach})
        );
    }
}

#[allow(dead_code)]
async fn session_close_entry_points_compile(
    sessions: ClientRpcSessions<'_>,
) -> Result<SessionsCloseResult, Error> {
    sessions
        .close(SessionsCloseRequest {
            session_id: "legacy".into(),
        })
        .await?;
    sessions
        .close_with_options(
            SessionsCloseOptions::new("legacy".into()).detach_session_end_hooks(true),
        )
        .await
}

#[test]
fn session_info_preserves_struct_update_construction_and_internal_wire_metadata() {
    use github_copilot_sdk::session_events::SessionInfoData;

    let info = SessionInfoData {
        info_type: "notification".to_string(),
        message: "message".to_string(),
        tip: Some("tip".to_string()),
        url: Some("https://example.com".to_string()),
        ..Default::default()
    };
    let mut wire = serde_json::to_value(info).unwrap();
    assert_eq!(
        wire,
        json!({
            "infoType": "notification",
            "message": "message",
            "tip": "tip",
            "url": "https://example.com",
        })
    );
    wire["responsesReasoning"] = json!({
        "model": "model",
        "initialEffort": "low",
        "effort": "high",
    });
    let restored: SessionInfoData = serde_json::from_value(wire.clone()).unwrap();
    assert_eq!(serde_json::to_value(restored).unwrap(), wire);
}

fn contract() -> CatalogClientContract {
    CatalogClientContract {
        protocol_version: 3,
        required_capabilities: vec!["mcp-install-planning".to_string()],
    }
}

fn candidate() -> McpPlanInstallSource {
    McpPlanInstallSource::Candidate(McpPlanInstallSourceCandidate {
        candidate_handle: "candidate".to_string(),
        kind: McpPlanInstallSourceCandidateKind::Candidate,
        search_id: "search".to_string(),
    })
}

#[test]
fn existing_exhaustive_literals_keep_their_wire_shape() {
    let plan = McpPlanInstallRequest {
        contract: contract(),
        scope: Some(McpPlanScope::User),
        source: candidate(),
    };
    let search = CatalogSearchRequest {
        contract: contract(),
        kinds: None,
        limit: Some(4),
        page: None,
        query: "catalogue query".to_string(),
    };

    let plan = serde_json::to_value(plan).unwrap();
    let search = serde_json::to_value(search).unwrap();
    assert_eq!(plan["scope"], "user");
    assert!(plan.get("policySessionId").is_none());
    assert_eq!(search["limit"], 4);
    assert!(search.get("policySessionId").is_none());
}

#[test]
fn options_serialise_the_legacy_request_and_additions_at_one_level() {
    let plan = McpPlanInstallOptions::new(contract(), candidate())
        .scope(McpPlanScope::User)
        .policy_session_id("session");
    let search = CatalogSearchOptions::new(contract(), "catalogue query".to_string())
        .limit(4)
        .policy_session_id("session");

    assert_eq!(
        serde_json::to_value(plan).unwrap(),
        json!({
            "contract": {"protocolVersion": 3, "requiredCapabilities": ["mcp-install-planning"]},
            "source": {"kind": "candidate", "candidateHandle": "candidate", "searchId": "search"},
            "scope": "user",
            "policySessionId": "session",
        })
    );
    assert_eq!(
        serde_json::to_value(search).unwrap(),
        json!({
            "contract": {"protocolVersion": 3, "requiredCapabilities": ["mcp-install-planning"]},
            "query": "catalogue query",
            "limit": 4,
            "policySessionId": "session",
        })
    );
}

// Both entry points exist on the same namespace with unchanged signatures.
#[allow(dead_code)]
async fn entry_points_compile(
    mcp: ClientRpcMcp<'_>,
    catalog: ClientRpcCatalog<'_>,
) -> Result<(McpPlanInstallResult, CatalogSearchResult), Error> {
    mcp.plan_install(McpPlanInstallRequest {
        contract: contract(),
        scope: None,
        source: candidate(),
    })
    .await?;
    catalog
        .search(CatalogSearchRequest {
            contract: contract(),
            kinds: None,
            limit: None,
            page: None,
            query: "catalogue query".to_string(),
        })
        .await?;
    let plan = mcp
        .plan_install_with_options(McpPlanInstallOptions::new(contract(), candidate()))
        .await?;
    let search = catalog
        .search_with_options(CatalogSearchOptions::new(
            contract(),
            "catalogue query".to_string(),
        ))
        .await?;
    Ok((plan, search))
}
