#![allow(clippy::unwrap_used)]

use github_copilot_sdk::rpc::{
    CardDigest, CatalogCandidate, CatalogMcpServerCandidate, CatalogSearchResult,
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
