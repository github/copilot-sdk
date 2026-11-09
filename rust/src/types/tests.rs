/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use std::path::PathBuf;

use serde_json::{Value, json};

use super::{
    Attachment, AttachmentSelectionPosition, AttachmentSelectionRange, GitHubReferenceType,
    McpServerConfig, McpStdioServerConfig, PermissionRequestData, ProviderConfig,
    ResumeSessionConfig, SessionConfig, SessionId, Tool, ensure_attachment_display_names,
};
use crate::generated::session_events::{
    AssistedApprovalAssessment, AssistedApprovalCompletedRecommendation,
};
use crate::rpc::SandboxConfig;

#[test]
fn local_model_configuration_survives_public_type_serialization() {
    let provider = json!({
        "name": "local", "type": "winml", "baseUrl": "model.gguf",
        "localModel": { "device": "gpu", "future": [1, true] },
    });
    let model = json!({
        "id": "model", "provider": "local",
        "autoLocal": { "source": "azureml", "future": [1, true] },
        "metadata": { "autoLocal": { "source": "azureml" }, "future": [1, true] },
    });
    let decoded: super::NamedProviderConfig = serde_json::from_value(provider.clone()).unwrap();
    assert_eq!(serde_json::to_value(decoded).unwrap(), provider);
    let decoded: super::ProviderModelConfig = serde_json::from_value(model.clone()).unwrap();
    assert_eq!(serde_json::to_value(decoded).unwrap(), model);
}

#[test]
fn permission_request_conversion_preserves_original_json_and_borrowed_data() {
    for original in [
        json!({
            "kind": "future-permission",
            "path": "/workspace/report.txt",
            "managedApprovalRequired": null,
            "permissionRequest": {"nested": "request-specific field"},
            "future": {"nullable": null, "values": [false, 0, {"key": "value"}]},
        }),
        json!(null),
        json!([null, {"future": ["nested"]}]),
    ] {
        let data = PermissionRequestData {
            extra: json!({
                "requestId": "permission-1",
                "permissionRequest": original,
                "futureEnvelopeField": "not part of the original request",
            }),
            ..Default::default()
        };
        let before = serde_json::to_value(&data).unwrap();
        assert_eq!(Value::from(&data), original);
        assert_eq!(serde_json::to_value(&data).unwrap(), before);
        assert_eq!(Value::from(data), original);
    }
}

#[test]
fn permission_request_conversion_preserves_legacy_direct_json() {
    for original in [
        json!({
            "kind": "write",
            "path": "/workspace/report.txt",
            "future": {"nullable": null, "values": [false, 0, {"key": "value"}]},
        }),
        json!(null),
        json!([null, {"future": ["nested"]}]),
    ] {
        let data = PermissionRequestData {
            extra: original.clone(),
            ..Default::default()
        };
        assert_eq!(Value::from(&data), original);
        assert_eq!(Value::from(data), original);
    }
}

#[test]
fn permission_request_exposes_assisted_approval_assessment() {
    let data = PermissionRequestData {
        extra: json!({
            "assistedApprovalAssessment": {
                "status": "completed",
                "recommendation": "requireApproval",
                "rationale": "The command modifies protected files."
            }
        }),
        ..Default::default()
    };

    let assessment = data
        .assisted_approval_assessment()
        .unwrap()
        .expect("assessment");
    let AssistedApprovalAssessment::Completed(completed) = assessment else {
        panic!("expected completed assessment");
    };
    assert_eq!(
        completed.recommendation,
        AssistedApprovalCompletedRecommendation::RequireApproval
    );
    assert_eq!(
        completed.rationale.as_deref(),
        Some("The command modifies protected files.")
    );
}

#[test]
fn programmatic_event_provenance_preserves_legacy_payloads() {
    use serde::Serialize;
    use serde::de::DeserializeOwned;

    use super::ProgrammaticToolCallData;
    use crate::session_events::{
        PermissionRequestedData, ToolExecutionCompleteData, ToolExecutionStartData,
    };

    fn assert_round_trip<T: DeserializeOwned + Serialize>(data: serde_json::Value) {
        for parent_id in [None, Some("program-root")] {
            let mut wire = data.clone();
            if let Some(parent_id) = parent_id {
                wire["programToolCallId"] = json!(parent_id);
            }
            let legacy: T = serde_json::from_value(wire.clone()).unwrap();
            assert_eq!(serde_json::to_value(legacy).unwrap(), data);
            let enriched: ProgrammaticToolCallData<T> =
                serde_json::from_value(wire.clone()).unwrap();
            assert_eq!(enriched.program_tool_call_id.as_deref(), parent_id);
            assert_eq!(serde_json::to_value(&enriched.data).unwrap(), data);
            assert_eq!(serde_json::to_value(enriched).unwrap(), wire);
        }
    }

    assert_round_trip::<PermissionRequestedData>(json!({
        "requestId": "permission-1",
        "permissionRequest": {
            "kind": "read",
            "path": "fixture.rs",
            "intention": "inspect"
        }
    }));
    assert_round_trip::<ToolExecutionStartData>(json!({
        "toolCallId": "child-1",
        "toolName": "view"
    }));
    assert_round_trip::<ToolExecutionCompleteData>(json!({
        "toolCallId": "child-1",
        "success": true
    }));
}

#[test]
fn programmatic_typed_events_preserve_parent_ownership() {
    use super::{ProgrammaticToolCallEvent, SessionEventWithMetadata};

    for (event_type, data) in [
        (
            "tool.execution_start",
            json!({
                "toolCallId": "child-1",
                "toolName": "view",
                "arguments": {"programToolCallId": "ordinary-argument"}
            }),
        ),
        (
            "tool.execution_complete",
            json!({"toolCallId": "child-1", "success": true}),
        ),
        (
            "permission.requested",
            json!({
                "requestId": "permission-1",
                "permissionRequest": {
                    "kind": "read",
                    "path": "fixture.rs",
                    "intention": "inspect"
                }
            }),
        ),
    ] {
        let plain_wire = json!({
            "id": "event-1",
            "timestamp": "2026-11-05T00:00:00Z",
            "parentId": "event-0",
            "ephemeral": false,
            "agentId": "agent-1",
            "type": event_type,
            "data": data
        });
        for parent_id in [None, Some("program-root")] {
            let mut wire = plain_wire.clone();
            if let Some(parent_id) = parent_id {
                wire["data"]["programToolCallId"] = json!(parent_id);
            }
            let typed: ProgrammaticToolCallEvent = serde_json::from_value(wire.clone()).unwrap();
            assert_eq!(typed.program_tool_call_id.as_deref(), parent_id);
            assert_eq!(serde_json::to_value(typed).unwrap(), wire, "{event_type}");

            wire["checkpoint"] = json!(true);
            let checkpointed: SessionEventWithMetadata<ProgrammaticToolCallEvent> =
                serde_json::from_value(wire.clone()).unwrap();
            assert_eq!(checkpointed.checkpoint, Some(true));
            assert_eq!(
                checkpointed.event.program_tool_call_id.as_deref(),
                parent_id
            );
            assert_eq!(
                serde_json::to_value(checkpointed).unwrap(),
                wire,
                "{event_type} checkpoint"
            );
        }
        for invalid in [json!(42), json!(false), json!([]), json!({})] {
            let mut wire = plain_wire.clone();
            wire["data"]["programToolCallId"] = invalid;
            assert!(
                serde_json::from_value::<ProgrammaticToolCallEvent>(wire.clone()).is_err(),
                "invalid ownership accepted: {wire}"
            );
        }
    }
}

#[test]
fn builtin_agent_allowlist_preserves_omission_empty_and_subset_on_create_and_resume() {
    for agents in [
        None,
        Some(Vec::<String>::new()),
        Some(vec!["explore".into()]),
    ] {
        let mut create = SessionConfig::default();
        let mut resume = ResumeSessionConfig::new(SessionId::new("allowlist-session"));
        if let Some(ref agents) = agents {
            create = create.with_included_builtin_agents(agents.clone());
            resume = resume.with_included_builtin_agents(agents.clone());
        }
        let create = serde_json::to_value(create.into_wire(None).unwrap().0).unwrap();
        let resume = serde_json::to_value(resume.into_wire().unwrap().0).unwrap();
        for payload in [create, resume] {
            match &agents {
                None => assert!(payload.get("includedBuiltinAgents").is_none(), "{payload}"),
                Some(agents) => assert_eq!(payload["includedBuiltinAgents"], json!(agents)),
            }
        }
    }
}

#[test]
fn programmatic_typed_first_event_preserves_null_parent() {
    use super::{ProgrammaticToolCallEvent, SessionEventWithMetadata};

    let mut wire = json!({
        "id": "event-0",
        "timestamp": "2026-11-06T00:00:00Z",
        "parentId": null,
        "type": "session.info",
        "data": {"infoType": "status", "message": "Initial event"}
    });
    let typed: ProgrammaticToolCallEvent = serde_json::from_value(wire.clone()).unwrap();
    assert_eq!(typed.event.parent_id, None);
    assert_eq!(typed.program_tool_call_id, None);
    assert_eq!(serde_json::to_value(&typed).unwrap(), wire);

    wire["checkpoint"] = json!(true);
    let checkpointed = SessionEventWithMetadata {
        event: typed,
        checkpoint: Some(true),
    };
    assert_eq!(serde_json::to_value(checkpointed).unwrap(), wire);
}

#[test]
#[should_panic(expected = "tool parameter schema must be a JSON object")]
fn tool_with_parameters_panics_on_non_object_value() {
    let _ = Tool::new("noop").with_parameters(json!(null));
}

#[test]
fn tool_annotations_serialize_only_when_set() {
    let plain = serde_json::to_value(Tool::new("plain")).unwrap();
    assert!(plain.get("idempotent").is_none());

    let annotated = serde_json::to_value(Tool::new("lookup").with_idempotent(true)).unwrap();
    assert_eq!(annotated["idempotent"], json!(true));
}

#[test]
fn mcp_servers_serialize_in_insertion_order() {
    use indexmap::IndexMap;

    // Regression: `mcp_servers` was a `HashMap`, so the server keys (and
    // thus the `session.create` payload) serialized in a per-process
    // random order; `IndexMap` pins them to insertion order. The long
    // sequence makes a `HashMap` regression reproduce this exact order by
    // chance only 1/N!, avoiding a flaky false pass.
    let order = [
        "zebra", "quartz", "delta", "ivy", "mango", "bravo", "xenon", "amber", "falcon", "ceres",
        "nova", "kelp", "otter", "yodel", "plum", "garnet",
    ];
    let mut servers = IndexMap::new();
    for name in order {
        servers.insert(
            name.to_string(),
            McpServerConfig::Stdio(McpStdioServerConfig {
                command: "run".to_string(),
                ..Default::default()
            }),
        );
    }

    let (wire, _runtime) = SessionConfig::default()
        .with_mcp_servers(servers)
        .into_wire(None)
        .expect("into_wire should succeed");
    let json = serde_json::to_string(&wire).expect("serialize wire");

    let positions: Vec<usize> = order
        .iter()
        .map(|name| {
            json.find(&format!("\"{name}\""))
                .unwrap_or_else(|| panic!("server {name} missing from wire JSON"))
        })
        .collect();
    let mut ascending = positions.clone();
    ascending.sort_unstable();
    assert_eq!(
        positions, ascending,
        "mcp server keys must serialize in insertion order: {json}"
    );
}

#[test]
fn provider_config_api_key_command_round_trips_and_is_optional() {
    let command = "credential-helper --profile sdk";
    let provider = ProviderConfig::new("https://models.example.test").with_api_key_command(command);
    let wire = serde_json::to_value(&provider).unwrap();
    assert_eq!(wire["apiKeyCommand"], command);
    let round_trip: ProviderConfig = serde_json::from_value(wire).unwrap();
    assert_eq!(round_trip.api_key_command.as_deref(), Some(command));
    assert!(!format!("{provider:?}").contains(command));
    let omitted = serde_json::to_value(ProviderConfig::new("https://models.example.test")).unwrap();
    assert!(omitted.get("apiKeyCommand").is_none());
}

#[test]
fn ensures_display_names_for_variants_that_support_them() {
    let mut attachments = vec![
        Attachment::File {
            path: PathBuf::from("/tmp/file.rs"),
            display_name: None,
            line_range: None,
        },
        Attachment::Selection {
            file_path: PathBuf::from("/tmp/src/lib.rs"),
            display_name: None,
            text: "fn main() {}".to_string(),
            selection: AttachmentSelectionRange {
                start: AttachmentSelectionPosition {
                    line: 0,
                    character: 0,
                },
                end: AttachmentSelectionPosition {
                    line: 0,
                    character: 10,
                },
            },
        },
        Attachment::Blob {
            data: "Zm9v".to_string(),
            mime_type: "image/png".to_string(),
            display_name: None,
        },
        Attachment::GitHubReference {
            number: 7,
            title: "Track regressions".to_string(),
            reference_type: GitHubReferenceType::Issue,
            state: "open".to_string(),
            url: "https://example.com/issues/7".to_string(),
        },
        Attachment::File {
            path: PathBuf::from("/tmp/notes.md"),
            display_name: Some("Release notes".to_string()),
            line_range: None,
        },
        Attachment::File {
            path: PathBuf::from("/tmp/blank.md"),
            display_name: Some("  ".to_string()),
            line_range: None,
        },
    ];

    ensure_attachment_display_names(&mut attachments);

    assert_eq!(attachments[0].display_name(), Some("file.rs"));
    assert_eq!(attachments[1].display_name(), Some("lib.rs"));
    assert_eq!(attachments[2].display_name(), Some("attachment"));
    assert_eq!(attachments[3].display_name(), None);
    assert_eq!(
        attachments[3].label(),
        Some("Track regressions".to_string())
    );
    // A caller's label is kept; only a blank one is replaced.
    assert_eq!(attachments[4].display_name(), Some("Release notes"));
    assert_eq!(attachments[5].display_name(), Some("blank.md"));
}

#[test]
fn github_anchored_attachment_variants_round_trip() {
    let cases = vec![
        (
            "github_commit",
            json!({
                "type": "github_commit",
                "message": "Fix the thing",
                "oid": "abc123",
                "repo": { "id": 1, "name": "repo", "owner": "octocat" },
                "url": "https://github.com/octocat/repo/commit/abc123"
            }),
        ),
        (
            "github_release",
            json!({
                "type": "github_release",
                "name": "v1.2.3",
                "repo": { "name": "repo", "owner": "octocat" },
                "tagName": "v1.2.3",
                "url": "https://github.com/octocat/repo/releases/tag/v1.2.3"
            }),
        ),
        (
            "github_actions_job",
            json!({
                "type": "github_actions_job",
                "conclusion": "failure",
                "jobId": 99,
                "jobName": "build",
                "repo": { "name": "repo", "owner": "octocat" },
                "url": "https://github.com/octocat/repo/actions/runs/1/job/99",
                "workflowName": "CI"
            }),
        ),
        (
            "github_repository",
            json!({
                "type": "github_repository",
                "description": "An example repository",
                "ref": "main",
                "repo": { "name": "repo", "owner": "octocat" },
                "url": "https://github.com/octocat/repo"
            }),
        ),
        (
            "github_file_diff",
            json!({
                "type": "github_file_diff",
                "base": {
                    "path": "src/lib.rs",
                    "ref": "main",
                    "repo": { "name": "repo", "owner": "octocat" }
                },
                "head": {
                    "path": "src/lib.rs",
                    "ref": "feature",
                    "repo": { "name": "repo", "owner": "octocat" }
                },
                "url": "https://github.com/octocat/repo/compare/main...feature"
            }),
        ),
        (
            "github_tree_comparison",
            json!({
                "type": "github_tree_comparison",
                "base": {
                    "repo": { "name": "repo", "owner": "octocat" },
                    "revision": "main"
                },
                "head": {
                    "repo": { "name": "repo", "owner": "octocat" },
                    "revision": "feature"
                },
                "url": "https://github.com/octocat/repo/compare/main...feature"
            }),
        ),
        (
            "github_url",
            json!({
                "type": "github_url",
                "url": "https://github.com/octocat/repo/wiki"
            }),
        ),
        (
            "github_file",
            json!({
                "type": "github_file",
                "path": "src/main.rs",
                "ref": "main",
                "repo": { "name": "repo", "owner": "octocat" },
                "url": "https://github.com/octocat/repo/blob/main/src/main.rs"
            }),
        ),
        (
            "github_snippet",
            json!({
                "type": "github_snippet",
                "lineRange": { "start": 10, "end": 20 },
                "path": "src/main.rs",
                "ref": "main",
                "repo": { "name": "repo", "owner": "octocat" },
                "url": "https://github.com/octocat/repo/blob/main/src/main.rs#L10-L20"
            }),
        ),
    ];

    for (expected_type, input) in cases {
        let attachment: Attachment = serde_json::from_value(input.clone())
            .unwrap_or_else(|err| panic!("{expected_type} should deserialize: {err}"));

        // Serialize to a string first: parsing into `serde_json::Value` would
        // silently dedupe a duplicate `type` key, hiding the exact regression
        // this test guards against (e.g. a wrapped generated struct emitting its
        // own `type` alongside the enum tag).
        let serialized_string = serde_json::to_string(&attachment)
            .unwrap_or_else(|err| panic!("{expected_type} should serialize: {err}"));

        // Exactly one `type` key, carrying the expected discriminator.
        assert_eq!(
            serialized_string.matches("\"type\":").count(),
            1,
            "{expected_type} must serialize a single `type` key"
        );

        let serialized: serde_json::Value = serde_json::from_str(&serialized_string)
            .unwrap_or_else(|err| panic!("{expected_type} should reparse: {err}"));
        assert_eq!(
            serialized.get("type").and_then(|value| value.as_str()),
            Some(expected_type),
            "{expected_type} must serialize the correct discriminator"
        );

        // Round-trips without dropping fields.
        assert_eq!(
            serialized, input,
            "{expected_type} should round-trip without data loss"
        );
        let reparsed: Attachment = serde_json::from_value(serialized)
            .unwrap_or_else(|err| panic!("{expected_type} should re-deserialize: {err}"));
        assert_eq!(
            reparsed, attachment,
            "{expected_type} should re-deserialize to the same value"
        );
    }
}

#[test]
fn session_configs_redact_sandbox_config_in_debug() {
    let sandbox: SandboxConfig = serde_json::from_value(json!({
        "enabled": true,
        "userPolicy": {
            "network": {
                "proxy": { "url": "http://proxy.example", "password": "proxy-secret" }
            }
        }
    }))
    .unwrap();
    let create = SessionConfig::default().with_sandbox_config(sandbox.clone());
    let resume =
        ResumeSessionConfig::new(SessionId::from("resume-sandbox")).with_sandbox_config(sandbox);
    for debug in [format!("{create:?}"), format!("{resume:?}")] {
        assert!(
            debug.contains("sandbox_config: Some(\"<redacted>\")"),
            "{debug}"
        );
        assert!(!debug.contains("proxy-secret"), "{debug}");
    }
}
