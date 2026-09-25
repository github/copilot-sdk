// Unit tests for generated API types -- struct construction and field
// access. These do not require a client, session, or replay proxy.

#![allow(clippy::unwrap_used)]

use github_copilot_sdk::rpc::{
    AcceptedEnqueueCommandResult, ConnectorAccountRequest, ConnectorCatalogStatus,
    ConnectorConnectRequest, ConnectorContinueRequest, ConnectorReconcileRequest,
    EnqueueCommandResult, Extension, ExtensionList, ExtensionSource, ExtensionStatus,
    ExtensionsDisableRequest, ExtensionsEnableRequest, FleetStartRequest, FleetStartResult,
    McpDisableRequest, McpEnableOptions, McpEnableRequest, McpInstallationOperationStatus,
    McpOauthLoginOptions, McpOauthLoginRequest, McpServer, McpStopServerRequest,
    MetadataContextAttributionResult, MetadataContextInfoResult, ModelSetAllowedModelsRequest,
    ModelSetAllowedModelsResult, ModelSwitchAutoTierRequest, ModelSwitchAutoTierResult,
    ModelSwitchAutoTierStatus, QueuePendingItems, QueuePendingItemsKind, SandboxConfig,
    SendAgentMode, SessionContextAttribution, SessionMetadataContextInfoResult,
    SessionMetadataGetContextAttributionResult, SessionMetadataSnapshot,
    SessionMetadataSnapshotResult, TasksStartAgentRequest, UnsupportedEnqueueCommandResult,
    UpdateSubagentSettingsRequest, WorkspaceSummary,
};
use github_copilot_sdk::session_events::{
    McpServerStatus, PermissionRequest, PermissionRequestedData, SessionEventData,
    TypedSessionEvent,
};
use github_copilot_sdk::{AutoTier, AutoTierPreference, SetModelOptions};

#[test]
fn operation_status_preserves_required_phases_and_original_identity() {
    for phase in [
        "preparing",
        "prepared",
        "awaiting-confirmation",
        "revalidating",
        "applying",
        "completed",
    ] {
        let mut wire = serde_json::json!({
            "phase": phase,
            "operationId": "original-operation",
            "cancellationRequested": true,
        });
        if phase == "completed" {
            wire["outcome"] = serde_json::json!({
                "kind": "cancelled",
                "operationId": "original-operation",
            });
        }
        let status: McpInstallationOperationStatus = serde_json::from_value(wire.clone()).unwrap();
        assert!(matches!(
            (phase, &status),
            ("preparing", McpInstallationOperationStatus::Preparing(_))
                | ("prepared", McpInstallationOperationStatus::Prepared(_))
                | (
                    "awaiting-confirmation",
                    McpInstallationOperationStatus::AwaitingConfirmation(_)
                )
                | (
                    "revalidating",
                    McpInstallationOperationStatus::Revalidating(_)
                )
                | ("applying", McpInstallationOperationStatus::Applying(_))
                | ("completed", McpInstallationOperationStatus::Completed(_))
        ));
        assert_eq!(serde_json::to_value(status).unwrap(), wire);
    }
}

#[test]
fn operation_status_refuses_unknown_phases_and_incomplete_terminal_results() {
    let original = serde_json::json!({
        "phase": "completed",
        "operationId": "original-operation",
        "cancellationRequested": false,
        "outcome": { "kind": "declined", "operationId": "original-operation" },
    });
    for field in ["phase", "operationId", "cancellationRequested", "outcome"] {
        let mut wire = original.clone();
        wire.as_object_mut().unwrap().remove(field);
        assert!(
            serde_json::from_value::<McpInstallationOperationStatus>(wire.clone()).is_err(),
            "unexpectedly accepted {wire}",
        );
    }
    for phase in [
        serde_json::Value::Null,
        serde_json::json!("future-phase"),
        serde_json::json!(42),
    ] {
        let mut wire = original.clone();
        wire["phase"] = phase;
        assert!(
            serde_json::from_value::<McpInstallationOperationStatus>(wire.clone()).is_err(),
            "unexpectedly accepted {wire}",
        );
    }
}

#[test]
fn context_info_preserves_null_before_initialisation_and_populated_token_fields() {
    let metadata: MetadataContextInfoResult =
        serde_json::from_value(serde_json::json!({})).unwrap();
    let session: SessionMetadataContextInfoResult =
        serde_json::from_value(serde_json::json!({})).unwrap();
    assert!(metadata.context_info.is_none());
    assert!(session.context_info.is_none());
    assert_eq!(
        serde_json::to_value(metadata).unwrap(),
        serde_json::json!({ "contextInfo": null })
    );
    assert_eq!(
        serde_json::to_value(session).unwrap(),
        serde_json::json!({ "contextInfo": null })
    );
    for context in [
        serde_json::Value::Null,
        serde_json::json!({
            "modelName": "test-model",
            "systemTokens": 10,
            "conversationTokens": 20,
            "toolDefinitionsTokens": 30,
            "mcpToolsTokens": 5,
            "totalTokens": 60,
            "promptTokenLimit": 100,
            "compactionThreshold": 80,
            "limit": 120,
            "bufferTokens": 25
        }),
    ] {
        let wire = serde_json::json!({ "contextInfo": context });
        let metadata: MetadataContextInfoResult = serde_json::from_value(wire.clone()).unwrap();
        let session: SessionMetadataContextInfoResult =
            serde_json::from_value(wire.clone()).unwrap();
        assert_eq!(metadata.context_info.is_none(), context.is_null());
        assert_eq!(session.context_info.is_none(), context.is_null());
        assert_eq!(serde_json::to_value(metadata).unwrap(), wire);
        assert_eq!(serde_json::to_value(session).unwrap(), wire);
    }
    for malformed in [
        serde_json::json!(42),
        serde_json::json!({ "modelName": "incomplete" }),
    ] {
        let wire = serde_json::json!({ "contextInfo": malformed });
        assert!(serde_json::from_value::<MetadataContextInfoResult>(wire.clone()).is_err());
        assert!(serde_json::from_value::<SessionMetadataContextInfoResult>(wire).is_err());
    }
}

#[test]
fn context_attribution_preserves_null_and_populated_metadata() {
    let metadata: MetadataContextAttributionResult =
        serde_json::from_value(serde_json::json!({})).unwrap();
    let session: SessionMetadataGetContextAttributionResult =
        serde_json::from_value(serde_json::json!({})).unwrap();
    assert!(metadata.context_attribution.is_none());
    assert!(session.context_attribution.is_none());
    assert_eq!(
        serde_json::to_value(metadata).unwrap(),
        serde_json::json!({ "contextAttribution": null })
    );
    assert_eq!(
        serde_json::to_value(session).unwrap(),
        serde_json::json!({ "contextAttribution": null })
    );
    for context in [
        serde_json::Value::Null,
        serde_json::to_value(SessionContextAttribution {
            model_id: "test-model".to_string(),
            model_source: "selected".to_string(),
            ..Default::default()
        })
        .unwrap(),
    ] {
        let wire = serde_json::json!({ "contextAttribution": context });
        let metadata: MetadataContextAttributionResult =
            serde_json::from_value(wire.clone()).unwrap();
        let session: SessionMetadataGetContextAttributionResult =
            serde_json::from_value(wire.clone()).unwrap();
        assert_eq!(metadata.context_attribution.is_none(), context.is_null());
        assert_eq!(session.context_attribution.is_none(), context.is_null());
        assert_eq!(serde_json::to_value(metadata).unwrap(), wire);
        assert_eq!(serde_json::to_value(session).unwrap(), wire);
    }
}

#[test]
fn workspace_metadata_preserves_null_and_populated_snapshots() {
    for workspace in [
        None,
        Some(WorkspaceSummary {
            id: "session-with-workspace".to_string(),
            cwd: Some("/workspace".to_string()),
            ..Default::default()
        }),
    ] {
        let missing = workspace.is_none();
        let wire = serde_json::to_value(SessionMetadataSnapshot {
            workspace,
            ..Default::default()
        })
        .unwrap();
        assert_eq!(wire["workspace"].is_null(), missing);
        let metadata: SessionMetadataSnapshot = serde_json::from_value(wire.clone()).unwrap();
        let session: SessionMetadataSnapshotResult = serde_json::from_value(wire.clone()).unwrap();
        assert_eq!(metadata.workspace.is_none(), missing);
        assert_eq!(session.workspace.is_none(), missing);
        assert_eq!(serde_json::to_value(metadata).unwrap(), wire);
        assert_eq!(serde_json::to_value(session).unwrap(), wire);

        let mut absent = wire;
        absent.as_object_mut().unwrap().remove("workspace");
        let metadata: SessionMetadataSnapshot = serde_json::from_value(absent.clone()).unwrap();
        let session: SessionMetadataSnapshotResult = serde_json::from_value(absent).unwrap();
        assert!(metadata.workspace.is_none());
        assert!(session.workspace.is_none());
        assert!(serde_json::to_value(metadata).unwrap()["workspace"].is_null());
        assert!(serde_json::to_value(session).unwrap()["workspace"].is_null());
    }
}

#[test]
fn subagent_settings_preserve_explicit_null_for_clearing_overrides() {
    let request: UpdateSubagentSettingsRequest =
        serde_json::from_value(serde_json::json!({})).unwrap();
    assert!(request.subagents.is_none());
    assert_eq!(
        serde_json::to_value(request).unwrap(),
        serde_json::json!({ "subagents": null })
    );
    for subagents in [
        serde_json::Value::Null,
        serde_json::json!({ "maxConcurrency": 2 }),
    ] {
        let wire = serde_json::json!({ "subagents": subagents });
        let request: UpdateSubagentSettingsRequest = serde_json::from_value(wire.clone()).unwrap();
        assert_eq!(request.subagents.is_none(), subagents.is_null());
        assert_eq!(serde_json::to_value(request).unwrap(), wire);
    }
}

#[test]
fn manual_mcp_requests_omit_unselected_owned_identity() {
    let payloads = [
        serde_json::to_value(McpEnableRequest {
            server_name: "manual".to_string(),
        })
        .unwrap(),
        serde_json::to_value(McpDisableRequest {
            server_name: "manual".to_string(),
        })
        .unwrap(),
        serde_json::to_value(McpStopServerRequest {
            server_name: "manual".to_string(),
        })
        .unwrap(),
        serde_json::to_value(McpOauthLoginRequest {
            server_name: "manual".to_string(),
            ..Default::default()
        })
        .unwrap(),
        serde_json::to_value(McpEnableOptions::new("manual")).unwrap(),
        serde_json::to_value(McpOauthLoginOptions::new("manual")).unwrap(),
    ];
    for payload in payloads {
        assert_eq!(payload, serde_json::json!({"serverName": "manual"}));
    }
}

#[test]
fn owned_mcp_identity_is_only_reachable_through_options() {
    assert_eq!(
        serde_json::to_value(
            McpEnableOptions::new("owned").expected_installation_id("a".repeat(32))
        )
        .unwrap(),
        serde_json::json!({"serverName": "owned", "expectedInstallationId": "a".repeat(32)})
    );
    assert_eq!(
        serde_json::to_value(
            McpOauthLoginOptions::new("owned")
                .expected_installation_id("a".repeat(32))
                .login_id("b".repeat(32))
        )
        .unwrap(),
        serde_json::json!({
            "serverName": "owned",
            "expectedInstallationId": "a".repeat(32),
            "loginId": "b".repeat(32),
        })
    );
}

#[test]
fn session_events_deserialize_auto_tier() {
    for event_type in ["session.start", "session.resume"] {
        for (tier, wire_tier) in [
            (Some(AutoTier::Efficiency), Some("efficiency")),
            (Some(AutoTier::Balance), Some("balance")),
            (Some(AutoTier::Intelligence), Some("intelligence")),
            (Some(AutoTier::Fast), Some("fast")),
            (None, None),
        ] {
            let mut wire = serde_json::json!({
                "id": "11111111-1111-1111-1111-111111111111",
                "timestamp": "2026-08-28T00:00:00Z",
                "parentId": null,
                "type": event_type,
                "data": {
                    "sessionId": "test-session", "version": 1,
                    "producer": "copilot", "copilotVersion": "1.0.82-1",
                    "startTime": "2026-08-28T00:00:00Z",
                    "resumeTime": "2026-08-28T00:00:00Z", "eventCount": 1
                }
            });
            if let Some(wire_tier) = wire_tier {
                wire["data"]["autoTier"] = serde_json::json!(wire_tier);
            }
            let event: TypedSessionEvent = serde_json::from_value(wire).unwrap();
            let actual: Option<AutoTier> = match event.payload {
                SessionEventData::SessionStart(data) if event_type == "session.start" => {
                    data.auto_tier
                }
                SessionEventData::SessionResume(data) if event_type == "session.resume" => {
                    data.auto_tier
                }
                _ => panic!("expected {event_type}"),
            };
            assert_eq!(actual, tier);
        }
    }
}

#[test]
fn extension_running_has_expected_status_and_source() {
    let extension = running_extension("project:demo", "demo");
    assert_eq!(extension.status, ExtensionStatus::Running);
    assert_eq!(extension.source, ExtensionSource::Project);
}

#[test]
fn disable_and_enable_requests_share_the_same_id() {
    let disable = ExtensionsDisableRequest {
        id: "project:demo".to_string(),
    };
    let enable = ExtensionsEnableRequest {
        id: disable.id.clone(),
    };
    assert_eq!(disable.id, enable.id);
}

#[test]
fn extension_list_contains_newly_added_extension_by_name() {
    let list = ExtensionList {
        extensions: vec![running_extension("project:late", "late")],
    };
    assert!(list.extensions.iter().any(|e| e.name == "late"));
}

#[test]
fn failed_extension_reports_failed_status() {
    let mut extension = running_extension("project:broken", "broken");
    extension.status = ExtensionStatus::Failed;
    assert_eq!(extension.status, ExtensionStatus::Failed);
}

#[test]
fn multiple_extensions_have_distinct_ids() {
    let list = ExtensionList {
        extensions: vec![
            running_extension("project:first", "first"),
            running_extension("user:second", "second"),
        ],
    };
    assert_eq!(list.extensions.len(), 2);
    assert_ne!(list.extensions[0].id, list.extensions[1].id);
}

#[test]
fn disabled_extension_preserves_disabled_status() {
    let mut extension = running_extension("project:disabled", "disabled");
    extension.status = ExtensionStatus::Disabled;
    assert_eq!(extension.status, ExtensionStatus::Disabled);
}

#[test]
fn fleet_start_request_and_result_fields_are_accessible() {
    let mut request = FleetStartRequest::default();
    request.prompt = Some("Use the custom tool".to_string());
    let result = FleetStartResult { started: true };
    assert_eq!(request.prompt.as_deref(), Some("Use the custom tool"));
    assert!(result.started);
}

#[test]
fn tasks_start_agent_request_fields_are_accessible() {
    let request = TasksStartAgentRequest {
        agent_type: "general-purpose".to_string(),
        prompt: "Say hi".to_string(),
        name: "sdk-test-task".to_string(),
        description: Some("SDK task agent".to_string()),
        model: None,
    };
    assert_eq!(request.agent_type, "general-purpose");
    assert_eq!(request.name, "sdk-test-task");
    assert_eq!(request.description.as_deref(), Some("SDK task agent"));
}

#[test]
fn connector_request_dtos_use_public_camel_case_wire_fields() {
    let account = ConnectorAccountRequest {
        account_id: "account-1".to_string(),
    };
    assert_eq!(
        serde_json::to_value(account).unwrap(),
        serde_json::json!({ "accountId": "account-1" })
    );

    let connector = ConnectorConnectRequest {
        account_id: "account-1".to_string(),
        connector_name: "github".to_string(),
    };
    assert_eq!(
        serde_json::to_value(connector).unwrap(),
        serde_json::json!({
            "accountId": "account-1",
            "connectorName": "github",
        })
    );

    let continuation = ConnectorContinueRequest {
        continuation_id: "continuation-1".to_string(),
        deadline_ms: 30_000,
        max_attempts: 5,
        poll_interval_ms: 1_000,
    };
    assert_eq!(
        serde_json::to_value(continuation).unwrap(),
        serde_json::json!({
            "continuationId": "continuation-1",
            "deadlineMs": 30_000,
            "maxAttempts": 5,
            "pollIntervalMs": 1_000,
        })
    );

    let reconcile = ConnectorReconcileRequest {
        account_id: "account-1".to_string(),
        refresh_catalog: Some(true),
    };
    assert_eq!(
        serde_json::to_value(reconcile).unwrap(),
        serde_json::json!({
            "accountId": "account-1",
            "refreshCatalog": true,
        })
    );
}

#[test]
fn connector_catalog_unknown_wire_value_has_a_distinct_variant() {
    let service_unknown: ConnectorCatalogStatus =
        serde_json::from_value(serde_json::json!("unknown")).unwrap();
    let future_status: ConnectorCatalogStatus =
        serde_json::from_value(serde_json::json!("future_status")).unwrap();

    assert_eq!(service_unknown, ConnectorCatalogStatus::UnknownValue);
    assert_eq!(future_status, ConnectorCatalogStatus::Unknown);
}

#[test]
fn model_allowed_models_request_and_result_preserve_contract_fields() {
    let replace = ModelSetAllowedModelsRequest {
        allowed_models: Some(vec!["gpt-5.4".to_string(), "gpt-5-mini".to_string()]),
    };
    assert_eq!(
        serde_json::to_value(&replace).unwrap(),
        serde_json::json!({ "allowedModels": ["gpt-5.4", "gpt-5-mini"] })
    );

    let clear = ModelSetAllowedModelsRequest::default();
    assert_eq!(clear.allowed_models, None);
    assert_eq!(serde_json::to_value(&clear).unwrap(), serde_json::json!({}));

    let explicit_null: ModelSetAllowedModelsRequest =
        serde_json::from_value(serde_json::json!({ "allowedModels": null })).unwrap();
    assert_eq!(explicit_null.allowed_models, None);

    let result = ModelSetAllowedModelsResult {
        allowed_models: Some(vec!["gpt-5.4".to_string()]),
        effective_allowed_models: Some(vec!["gpt-5.4".to_string()]),
        fallback_model: Some("gpt-5.4".to_string()),
        model_id: Some("gpt-5.4".to_string()),
    };
    assert_eq!(
        result.allowed_models.as_deref(),
        Some(["gpt-5.4".to_string()].as_slice())
    );
    assert_eq!(
        result.effective_allowed_models.as_deref(),
        Some(["gpt-5.4".to_string()].as_slice())
    );
    assert_eq!(result.fallback_model.as_deref(), Some("gpt-5.4"));
    assert_eq!(result.model_id.as_deref(), Some("gpt-5.4"));
}

#[test]
fn permission_event_exposes_managed_approval_required() {
    let data: PermissionRequestedData = serde_json::from_value(serde_json::json!({
        "permissionRequest": {
            "kind": "read",
            "intention": "Read managed content",
            "path": "/workspace/file.txt",
            "managedApprovalRequired": true
        },
        "requestId": "permission-1"
    }))
    .unwrap();

    let PermissionRequest::Read(request) = data.permission_request else {
        panic!("expected read permission request");
    };
    assert_eq!(request.managed_approval_required, Some(true));
}

#[test]
fn queue_pending_item_metadata_uses_camel_case_wire_names() {
    let item = QueuePendingItems {
        agent_mode: SendAgentMode::Interactive,
        display_text: "second message".to_string(),
        id: "batch-1".to_string(),
        kind: QueuePendingItemsKind::Message,
        message_id: Some("message-2".to_string()),
        source: Some("api".to_string()),
    };

    let serialized = serde_json::to_value(&item).unwrap();
    assert_eq!(serialized["id"], "batch-1");
    assert_eq!(serialized["messageId"], "message-2");
    assert_eq!(serialized["source"], "api");

    let deserialized: QueuePendingItems = serde_json::from_value(serialized).unwrap();
    assert_eq!(deserialized.message_id.as_deref(), Some("message-2"));
    assert_eq!(deserialized.source.as_deref(), Some("api"));
}

#[test]
fn queue_pending_message_id_is_optional_for_older_hosts() {
    let item: QueuePendingItems = serde_json::from_value(serde_json::json!({
        "agentMode": "interactive",
        "displayText": "/model gpt-5",
        "id": "command-1",
        "kind": "command"
    }))
    .unwrap();

    assert_eq!(item.message_id, None);
    assert_eq!(item.source, None);
    let serialized = serde_json::to_value(item).unwrap();
    assert!(serialized.get("messageId").is_none());
    assert!(serialized.get("source").is_none());
}

#[test]
fn enqueue_command_result_preserves_boolean_discriminator() {
    let accepted: EnqueueCommandResult = serde_json::from_value(serde_json::json!({
        "queued": true,
        "queueId": "queue-1"
    }))
    .unwrap();
    assert!(matches!(
        &accepted,
        EnqueueCommandResult::AcceptedEnqueueCommandResult(_)
    ));
    assert_eq!(
        serde_json::to_value(&accepted).unwrap(),
        serde_json::json!({ "queued": true, "queueId": "queue-1" })
    );

    let unsupported: EnqueueCommandResult =
        serde_json::from_value(serde_json::json!({ "queued": false, "queueId": null })).unwrap();
    assert!(matches!(
        &unsupported,
        EnqueueCommandResult::UnsupportedEnqueueCommandResult(_)
    ));
    assert_eq!(
        serde_json::to_value(&unsupported).unwrap(),
        serde_json::json!({ "queued": false })
    );

    assert!(
        serde_json::to_value(AcceptedEnqueueCommandResult {
            queued: false,
            queue_id: "queue-1".to_string(),
        })
        .is_err()
    );
    assert!(
        serde_json::to_value(UnsupportedEnqueueCommandResult {
            queued: true,
            queue_id: None,
        })
        .is_err()
    );
}

#[test]
fn sandbox_allow_bypass_round_trips_as_optional_camel_case() {
    let mut enabled = SandboxConfig::default();
    enabled.enabled = true;
    enabled.allow_bypass = Some(true);
    let value = serde_json::to_value(enabled).unwrap();
    assert_eq!(
        value,
        serde_json::json!({
            "allowBypass": true,
            "enabled": true,
        })
    );
    let round_tripped: SandboxConfig = serde_json::from_value(value).unwrap();
    assert_eq!(round_tripped.allow_bypass, Some(true));

    let mut omitted = SandboxConfig::default();
    omitted.enabled = true;
    assert_eq!(
        serde_json::to_value(omitted).unwrap(),
        serde_json::json!({ "enabled": true })
    );
}

fn running_extension(id: &str, name: &str) -> Extension {
    Extension {
        id: id.to_string(),
        name: name.to_string(),
        pid: Some(42),
        source: if id.starts_with("user:") {
            ExtensionSource::User
        } else {
            ExtensionSource::Project
        },
        status: ExtensionStatus::Running,
    }
}

#[test]
fn switch_auto_tier_request_serializes_explicit_null_tier() {
    // `autoTier` is a required field whose null value means "use provider-default
    // routing", so it must survive serialization rather than being skipped.
    let request = ModelSwitchAutoTierRequest {
        auto_tier: None,
        source: None,
    };
    let wire = serde_json::to_value(&request).unwrap();

    assert_eq!(wire.get("autoTier"), Some(&serde_json::Value::Null));
    assert!(wire.get("source").is_none());
}

#[test]
fn switch_auto_tier_request_serializes_each_tier() {
    for (tier, expected) in [
        (AutoTier::Efficiency, "efficiency"),
        (AutoTier::Balance, "balance"),
        (AutoTier::Intelligence, "intelligence"),
        (AutoTier::Fast, "fast"),
    ] {
        let request = ModelSwitchAutoTierRequest {
            auto_tier: Some(tier),
            source: None,
        };
        let wire = serde_json::to_value(&request).unwrap();
        assert_eq!(wire["autoTier"], serde_json::json!(expected));
    }
}

#[test]
fn switch_auto_tier_result_deserializes_full_snapshot() {
    let result: ModelSwitchAutoTierResult = serde_json::from_value(serde_json::json!({
        "status": "pending",
        "effectiveAutoTier": "balance",
        "pendingAutoTier": "fast",
        "activatingAutoTier": null,
        "supersededAutoTier": "efficiency"
    }))
    .unwrap();

    assert_eq!(result.status, ModelSwitchAutoTierStatus::Pending);
    assert_eq!(result.effective_auto_tier, Some(AutoTier::Balance));
    assert_eq!(result.pending_auto_tier, Some(AutoTier::Fast));
    assert_eq!(result.activating_auto_tier, None);
    assert_eq!(result.superseded_auto_tier, Some(AutoTier::Efficiency));
}

#[test]
fn set_model_options_distinguishes_unset_tier_from_reset() {
    let untouched = SetModelOptions::default();
    assert_eq!(untouched.auto_tier, None);

    let explicit = SetModelOptions::default().with_auto_tier(AutoTier::Intelligence);
    assert_eq!(
        explicit.auto_tier,
        Some(AutoTierPreference::Tier(AutoTier::Intelligence))
    );

    let cleared = SetModelOptions::default().with_reset_auto_tier();
    assert_eq!(cleared.auto_tier, Some(AutoTierPreference::Reset));
}

/// Names published by earlier releases must keep resolving after reference aliasing.
#[test]
#[allow(deprecated)]
fn released_nested_type_names_remain_usable() {
    use github_copilot_sdk::rpc::{
        DiagnosticEntrySource, DiagnosticsReadResultEntriesItemSource, McpInstallPlan,
        MetadataContextAttributionResultContextAttribution,
        MetadataContextAttributionResultContextAttributionCategories,
        MetadataContextAttributionResultContextAttributionCompactions,
        MetadataContextAttributionResultContextAttributionEntriesItem, ResponseFormatType,
        SendMessagesRequestResponseFormatType, SendRequestResponseFormat,
        SendRequestResponseFormatType, SessionDiagnosticsReadResultEntriesItemSource,
        SessionMetadataGetContextAttributionResultContextAttributionCategories,
        SessionMetadataGetContextAttributionResultContextAttributionCompactions,
        SessionMetadataGetContextAttributionResultContextAttributionEntriesItem,
    };

    let source: DiagnosticsReadResultEntriesItemSource = DiagnosticEntrySource::default();
    let _: SessionDiagnosticsReadResultEntriesItemSource = source;
    let format_type: SendRequestResponseFormatType = ResponseFormatType::default();
    let _: SendMessagesRequestResponseFormatType = format_type.clone();
    let format = SendRequestResponseFormat {
        r#type: format_type,
        ..Default::default()
    };
    assert!(serde_json::to_value(&format).is_ok());

    let attribution = MetadataContextAttributionResultContextAttribution {
        categories: MetadataContextAttributionResultContextAttributionCategories::default(),
        compactions: MetadataContextAttributionResultContextAttributionCompactions::default(),
        entries: vec![MetadataContextAttributionResultContextAttributionEntriesItem::default()],
        ..Default::default()
    };
    let _: SessionMetadataGetContextAttributionResultContextAttributionCategories =
        attribution.categories.clone();
    let _: SessionMetadataGetContextAttributionResultContextAttributionCompactions =
        attribution.compactions.clone();
    let _: Vec<SessionMetadataGetContextAttributionResultContextAttributionEntriesItem> =
        attribution.entries.clone();

    let plan = McpInstallPlan {
        transport_choices: vec![serde_json::json!({ "choiceId": "raw" })],
        ..Default::default()
    };
    assert_eq!(plan.transport_choices[0]["choiceId"], "raw");
}

/// The attachment `type` literal cannot be represented by its released field type, so it
/// must still round-trip on the wire instead of degrading to `"Unknown"`.
#[test]
fn github_reference_attachments_keep_their_type_literal_on_the_wire() {
    use github_copilot_sdk::rpc::{
        AttachmentGitHubReference, AttachmentGitHubReferenceType, PushAttachmentGitHubReference,
    };

    let wire = serde_json::json!({
        "number": 7,
        "referenceType": "pr",
        "state": "open",
        "title": "Example",
        "type": "github_reference",
        "url": "https://github.com/example/repo/pull/7"
    });
    let attachment: AttachmentGitHubReference = serde_json::from_value(wire.clone()).unwrap();
    assert_eq!(attachment.reference_type, AttachmentGitHubReferenceType::Pr);
    assert_eq!(attachment.r#type, AttachmentGitHubReferenceType::Unknown);
    assert_eq!(serde_json::to_value(&attachment).unwrap(), wire);
    assert_eq!(
        serde_json::to_value(AttachmentGitHubReference::default()).unwrap()["type"],
        "github_reference"
    );

    let push: PushAttachmentGitHubReference = serde_json::from_value(wire.clone()).unwrap();
    assert_eq!(serde_json::to_value(&push).unwrap(), wire);
    assert_eq!(
        serde_json::to_value(PushAttachmentGitHubReference::default()).unwrap()["type"],
        "github_reference"
    );
}

#[test]
fn listed_mcp_server_literals_with_defaults_survive_the_owned_marker() {
    let manual = McpServer {
        name: "manual".to_string(),
        status: McpServerStatus::Stopped,
        ..Default::default()
    };
    assert!(manual.owned.is_none());
    assert!(
        serde_json::to_value(&manual)
            .unwrap()
            .get("owned")
            .is_none()
    );

    let owned: McpServer = serde_json::from_value(serde_json::json!({
        "name": "owned",
        "status": "stopped",
        "owned": {"installationId": "installation"},
    }))
    .unwrap();
    assert_eq!(owned.owned.unwrap().installation_id, "installation");
}
