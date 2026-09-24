#![allow(clippy::unwrap_used)]

use github_copilot_sdk::rpc::{
    CardDigest, CardDigestAlgorithm, CatalogAgentPluginCandidateKind, CatalogAiSkillCandidateKind,
    CatalogCandidate, CatalogMcpServerCandidate, CatalogMcpServerCandidateKind,
    CatalogMcpServerInstallability, CatalogSearchResult, CatalogTrustSnapshot,
    McpServerCardMediaType,
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

#[test]
fn candidate_trust_preserves_raw_metadata_and_standalone_typed_variants() {
    for status in [
        "current",
        "absent",
        "stale",
        "downgraded",
        "revoked",
        "unsupported",
        "malformed",
    ] {
        let mut trust = json!({
            "schemaVersion": "v1",
            "status": status,
            "eligibility": "unknown",
            "provenance": {
                "source": "agent-finder",
                "observedAt": "2026-09-02T11:00:00Z"
            }
        });
        if status == "current" {
            trust["tier"] = json!("T1");
        }
        let mut wire = search_result_wire()["candidates"][0].clone();
        wire["trust"] = trust.clone();
        let candidate: CatalogMcpServerCandidate = serde_json::from_value(wire.clone()).unwrap();
        let snapshot: CatalogTrustSnapshot = serde_json::from_value(trust.clone()).unwrap();
        assert!(matches!(
            (status, &snapshot),
            ("current", CatalogTrustSnapshot::Current(_))
                | ("absent", CatalogTrustSnapshot::Absent(_))
                | ("stale", CatalogTrustSnapshot::Stale(_))
                | ("downgraded", CatalogTrustSnapshot::Downgraded(_))
                | ("revoked", CatalogTrustSnapshot::Revoked(_))
                | ("unsupported", CatalogTrustSnapshot::Unsupported(_))
                | ("malformed", CatalogTrustSnapshot::Malformed(_))
        ));
        assert_eq!(candidate.trust.as_ref().unwrap(), &trust);
        assert_eq!(serde_json::to_value(snapshot).unwrap(), trust);
        assert_eq!(serde_json::to_value(candidate).unwrap(), wire);
    }
}

#[test]
fn optional_trust_does_not_reject_candidates_or_discard_unbounded_metadata() {
    let current = json!({
        "schemaVersion": "v1",
        "status": "current",
        "tier": "T1",
        "eligibility": "unknown",
        "provenance": {
            "source": "agent-finder",
            "observedAt": "2026-09-02T11:00:00Z"
        }
    });
    let mut inputs = vec![
        json!(42),
        json!("private-invalid"),
        json!([]),
        json!({}),
        json!({"schemaVersion": "v2"}),
        json!({"schemaVersion": "v2", "status": null, "eligibility": 42}),
        json!({"schemaVersion": "v2", "provenance": {"observedAt": "changed-format"}}),
        json!({"schemaVersion": "v2", "extra": "x".repeat(4097)}),
    ];
    for (field, value) in [
        ("schemaVersion", "v2"),
        ("status", "future-status"),
        ("eligibility", "approved"),
        ("tier", "T3"),
    ] {
        let mut snapshot = current.clone();
        snapshot[field] = json!(value);
        inputs.push(snapshot);
    }
    let mut unknown_authority = current.clone();
    unknown_authority["provenance"]["source"] = json!("future-authority");
    inputs.push(unknown_authority);
    for field in [
        "schemaVersion",
        "status",
        "eligibility",
        "provenance",
        "tier",
    ] {
        let mut snapshot = current.clone();
        snapshot.as_object_mut().unwrap().remove(field);
        inputs.push(snapshot);
    }
    for time in ["not-a-date", "2026-09-18T10:00:00", "2026-02-30T10:00:00Z"] {
        let mut snapshot = current.clone();
        snapshot["provenance"]["observedAt"] = json!(time);
        inputs.push(snapshot);
    }
    // Hosts must receive both sides of their bounds intact before projecting trust.
    for extra in [
        json!("x".repeat(4096)),
        json!("x".repeat(4097)),
        json!({ "x".repeat(64): 0 }),
        json!({ "x".repeat(65): "private-key" }),
        json!([[["bounded"]]]),
        json!([[[["too-deep"]]]]),
        json!([[[[[["private-depth"]]]]]]),
        json!(vec![0; 119]),
        json!(vec![0; 120]),
        json!(vec![0; 129]),
    ] {
        let mut snapshot = current.clone();
        snapshot["extra"] = extra;
        inputs.push(snapshot);
    }

    for trust in inputs {
        let mut wire = search_result_wire();
        for candidate in wire["candidates"].as_array_mut().unwrap() {
            candidate["trust"] = trust.clone();
        }
        let result: CatalogSearchResult = serde_json::from_value(wire).unwrap();
        let CatalogSearchResult::Succeeded(result) = result else {
            panic!("optional trust must not discard a successful search");
        };
        assert_eq!(result.candidates.len(), 2);
        for candidate in result.candidates {
            assert_eq!(serde_json::to_value(candidate).unwrap()["trust"], trust);
        }
    }
}

#[test]
fn optional_trust_keeps_missing_and_null_metadata_unavailable() {
    for trust in [None, Some(Value::Null)] {
        let mut wire = search_result_wire()["candidates"][0].clone();
        if let Some(trust) = trust {
            wire["trust"] = trust;
        }
        let candidate: CatalogMcpServerCandidate = serde_json::from_value(wire).unwrap();
        assert!(candidate.trust.is_none());
    }
}

#[test]
fn trust_rejects_unknown_and_missing_discriminators() {
    for status in [None, Some(Value::Null), Some(json!("future-status"))] {
        let mut wire = json!({
            "schemaVersion": "v1",
            "eligibility": "unknown",
            "provenance": {
                "source": "agent-finder",
                "observedAt": "2026-09-02T11:00:00Z"
            }
        });
        if let Some(status) = status {
            wire["status"] = status;
        }
        assert!(serde_json::from_value::<CatalogTrustSnapshot>(wire).is_err());
    }
}
