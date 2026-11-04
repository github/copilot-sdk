// Existing request structs keep their published shape; inputs added later are only
// reachable through the private-field options types, which serialise flat.

#![allow(clippy::unwrap_used)]

use github_copilot_sdk::Error;
use github_copilot_sdk::rpc::{
    CatalogClientContract, CatalogSearchOptions, CatalogSearchRequest, CatalogSearchResult,
    ClientRpcCatalog, ClientRpcMcp, McpPlanInstallOptions, McpPlanInstallRequest,
    McpPlanInstallResult, McpPlanInstallSource, McpPlanInstallSourceCandidate,
    McpPlanInstallSourceCandidateKind, McpPlanScope,
};
use serde_json::json;

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

#[test]
fn options_omit_unset_inputs() {
    let plan = serde_json::to_value(McpPlanInstallOptions::new(contract(), candidate())).unwrap();
    assert!(plan.get("scope").is_none());
    assert!(plan.get("policySessionId").is_none());
    assert!(plan.get("legacy").is_none());
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
