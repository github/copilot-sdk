#![allow(clippy::unwrap_used)]

use github_copilot_sdk::rpc::{CatalogCandidate, CatalogCandidateSource, CatalogSearchResult};

const OPAQUE_MCP_HANDLE: &str = "opaque:mcp/01-do-not-parse";
const OPAQUE_SKILL_HANDLE: &str = "opaque:skill/02-do-not-parse";

#[test]
fn catalog_search_result_preserves_candidate_semantics() {
    let result: CatalogSearchResult = serde_json::from_value(serde_json::json!({
        "kind": "succeeded",
        "searchId": "search-01",
        "candidates": [
            {
                "kind": "mcp-server",
                "handle": OPAQUE_MCP_HANDLE,
                "handleExpiresAt": "2026-09-02T12:00:00Z",
                "mediaType": "application/mcp-server-card+json",
                "installability": "installable",
                "displayName": "Example MCP",
                "rawCard": {"secret": "must-not-survive"},
                "source": {"kind": "url", "url": "https://catalog.example/mcp.json"},
                "provenance": {
                    "authority": "catalog.example",
                    "observedAt": "2026-09-02T11:00:00Z",
                    "mediaType": "application/mcp-server-card+json"
                }
            },
            {
                "kind": "ai-skill",
                "handle": OPAQUE_SKILL_HANDLE,
                "handleExpiresAt": "2026-09-02T12:00:00Z",
                "mediaType": "application/ai-skill",
                "installability": "not-installable-kind",
                "displayName": "Example skill",
                "rawCard": {"secret": "must-not-survive"},
                "source": {"kind": "embedded"},
                "provenance": {
                    "authority": "catalog.example",
                    "observedAt": "2026-09-02T11:00:00Z",
                    "mediaType": "application/ai-skill"
                }
            }
        ],
        "truncated": false,
        "negotiated": {
            "runtimeProtocolVersion": 1,
            "grantedCapabilities": ["mcp-server-card", "ai-skill-discovery"]
        }
    }))
    .unwrap();

    let CatalogSearchResult::Succeeded(success) = &result else {
        panic!("expected a successful catalogue search");
    };
    let CatalogCandidate::McpServer(mcp) = &success.candidates[0] else {
        panic!("expected an MCP server candidate");
    };
    let CatalogCandidate::AiSkill(skill) = &success.candidates[1] else {
        panic!("expected an AI skill candidate");
    };
    assert_eq!(mcp.handle, OPAQUE_MCP_HANDLE);
    assert_eq!(skill.handle, OPAQUE_SKILL_HANDLE);
    assert!(matches!(mcp.source, CatalogCandidateSource::Url(_)));
    assert!(matches!(skill.source, CatalogCandidateSource::Embedded(_)));

    let wire = serde_json::to_value(&result).unwrap();
    for candidate in wire["candidates"].as_array().unwrap() {
        let fields = candidate.as_object().unwrap();
        for forbidden in ["card", "cardData", "rawCard"] {
            assert!(
                !fields.contains_key(forbidden),
                "candidate leaked {forbidden}"
            );
        }
    }
}

#[test]
fn catalog_search_result_preserves_refusals_and_failures() {
    let authentication: CatalogSearchResult = serde_json::from_value(serde_json::json!({
        "kind": "authentication-required",
        "reason": "no-credential",
        "message": "Sign in is required."
    }))
    .unwrap();
    assert!(matches!(
        authentication,
        CatalogSearchResult::AuthenticationRequired(_)
    ));

    let network: CatalogSearchResult = serde_json::from_value(serde_json::json!({
        "kind": "network-failure",
        "reason": "timeout",
        "retryAfterSeconds": 30,
        "message": "The catalogue timed out."
    }))
    .unwrap();
    let CatalogSearchResult::NetworkFailure(failure) = network else {
        panic!("expected a network failure");
    };
    assert_eq!(failure.retry_after_seconds, Some(30));
}
