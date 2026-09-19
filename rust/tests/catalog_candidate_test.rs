#![allow(clippy::unwrap_used)]

use github_copilot_sdk::rpc::{
    CardDigest, CardDigestAlgorithm, CatalogAgentPluginCandidateKind, CatalogAiSkillCandidateKind,
    CatalogCandidate, CatalogMcpServerCandidate, CatalogMcpServerCandidateKind,
    CatalogMcpServerInstallability, CatalogSearchResult, McpServerCardMediaType,
};
use serde_json::{Value, json};

fn search_result_wire() -> Value {
    json!({
        "kind": "succeeded",
        "candidates": [
            {
                "kind": "mcp-server",
                "displayName": "Postgres",
                "description": "Query and inspect PostgreSQL databases.",
                "publisher": "GitHub",
                "handle": "opaque-mcp-handle",
                "handleExpiresAt": "2026-09-02T12:00:00Z",
                "mediaType": "application/mcp-server-card+json",
                "installability": "installable",
                "source": {
                    "kind": "url",
                    "url": "https://cards.example.test/postgres"
                },
                "provenance": {
                    "authority": "agentfinder.github.com",
                    "mediaType": "application/mcp-server-card+json",
                    "observedAt": "2026-09-02T11:00:00Z"
                }
            },
            {
                "kind": "ai-skill",
                "displayName": "Lint",
                "description": "Review code for focused lint and formatting issues.",
                "publisher": "GitHub",
                "handle": "opaque-skill-handle",
                "handleExpiresAt": "2026-09-02T12:00:00Z",
                "mediaType": "application/ai-skill",
                "installability": "not-installable-kind",
                "source": {
                    "kind": "url",
                    "url": "https://cards.example.test/lint"
                },
                "provenance": {
                    "authority": "agentfinder.github.com",
                    "mediaType": "application/ai-skill",
                    "observedAt": "2026-09-02T11:00:00Z"
                }
            }
        ],
        "negotiated": {
            "runtimeProtocolVersion": 3,
            "grantedCapabilities": ["mcp-server-card", "ai-skill-discovery"]
        },
        "searchId": "catalog-search-id",
        "truncated": false
    })
}

fn search_candidates() -> Vec<CatalogCandidate> {
    let result: CatalogSearchResult = serde_json::from_value(search_result_wire()).unwrap();
    let CatalogSearchResult::Succeeded(result) = result else {
        panic!("expected successful catalog search, got {result:?}");
    };
    result.candidates
}

#[test]
fn mcp_candidate_retains_its_variant_and_payload() {
    let candidates = search_candidates();
    let candidate = &candidates[0];
    assert!(
        matches!(candidate, CatalogCandidate::McpServer(_)),
        "expected McpServer, got {candidate:?}"
    );
    assert_eq!(
        serde_json::to_value(candidate).unwrap(),
        search_result_wire()["candidates"][0]
    );
}

#[test]
fn ai_skill_candidate_retains_its_variant_and_payload() {
    let candidates = search_candidates();
    let candidate = &candidates[1];
    assert!(
        matches!(candidate, CatalogCandidate::AiSkill(_)),
        "expected AiSkill, got {candidate:?}"
    );
    assert_eq!(
        serde_json::to_value(candidate).unwrap(),
        search_result_wire()["candidates"][1]
    );
}

#[test]
fn plugin_candidate_retains_its_variant_and_payload() {
    let wire = json!({
        "kind": "plugin",
        "displayName": "Unrequested plugin",
        "identity": "urn:github:copilot:plugin:example",
        "compatibilityTags": [],
        "mediaType": "application/vnd.github.copilot-plugin",
        "source": {
            "repository": "example/plugins",
            "path": "example"
        },
        "provenance": {
            "authority": "agentfinder.github.com",
            "mediaType": "application/vnd.github.copilot-plugin",
            "observedAt": "2026-09-02T11:00:00Z"
        }
    });
    let candidate: CatalogCandidate = serde_json::from_value(wire.clone()).unwrap();
    assert!(
        matches!(candidate, CatalogCandidate::Plugin(_)),
        "expected Plugin, got {candidate:?}"
    );
    assert_eq!(serde_json::to_value(candidate).unwrap(), wire);
}

#[test]
fn unknown_null_and_missing_candidate_kinds_are_rejected() {
    for kind in [Some(json!("future-kind")), Some(Value::Null), None] {
        let mut wire = search_result_wire()["candidates"][0].clone();
        if let Some(kind) = kind {
            wire["kind"] = kind;
        } else {
            wire.as_object_mut().unwrap().remove("kind");
        }
        assert!(
            serde_json::from_value::<CatalogCandidate>(wire.clone()).is_err(),
            "unexpectedly accepted candidate {wire}"
        );
    }
}

#[test]
fn typed_mcp_candidate_rejects_an_ai_skill_discriminator() {
    let wire = search_result_wire()["candidates"][1].clone();
    assert!(serde_json::from_value::<CatalogMcpServerCandidate>(wire).is_err());
}

#[test]
fn standalone_enums_still_accept_future_values() {
    assert_eq!(
        serde_json::from_str::<CatalogAiSkillCandidateKind>("\"future-kind\"").unwrap(),
        CatalogAiSkillCandidateKind::Unknown
    );
    assert_eq!(
        serde_json::from_str::<CatalogMcpServerCandidateKind>("\"future-kind\"").unwrap(),
        CatalogMcpServerCandidateKind::Unknown
    );
    assert_eq!(
        serde_json::from_str::<CatalogAgentPluginCandidateKind>("\"future-kind\"").unwrap(),
        CatalogAgentPluginCandidateKind::Unknown
    );
    assert_eq!(
        serde_json::from_str::<CardDigestAlgorithm>("\"future-algorithm\"").unwrap(),
        CardDigestAlgorithm::Unknown
    );
}

#[test]
fn unconstrained_mcp_fields_still_accept_future_values() {
    let mut wire = search_result_wire()["candidates"][0].clone();
    wire["installability"] = json!("future-installability");
    wire["mediaType"] = json!("application/future-mcp-card");
    wire["provenance"]["mediaType"] = json!("application/future-mcp-card");

    let candidate: CatalogCandidate = serde_json::from_value(wire).unwrap();
    let CatalogCandidate::McpServer(candidate) = candidate else {
        panic!("expected McpServer, got {candidate:?}");
    };
    assert_eq!(
        candidate.installability,
        CatalogMcpServerInstallability::Unknown
    );
    assert_eq!(candidate.media_type, McpServerCardMediaType::Unknown);
    assert_eq!(
        candidate.provenance.media_type,
        McpServerCardMediaType::Unknown
    );
}

#[test]
fn digest_algorithm_enforces_its_field_constraint() {
    let mut wire = json!({
        "algorithm": "sha256-rfc8785",
        "value": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    });
    let digest: CardDigest = serde_json::from_value(wire.clone()).unwrap();
    assert_eq!(serde_json::to_value(digest).unwrap(), wire);

    wire["algorithm"] = json!("future-algorithm");
    let error = serde_json::from_value::<CardDigest>(wire).unwrap_err();
    assert!(error.to_string().contains("sha256-rfc8785"));
}
