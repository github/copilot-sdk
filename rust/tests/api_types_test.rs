// Unit tests for generated API types -- struct construction and field
// access. These do not require a client, session, or replay proxy.

#![allow(clippy::unwrap_used)]

use github_copilot_sdk::rpc::{
    AcceptedEnqueueCommandResult, AuthIdentityMetadata, AuthInfoType, ConnectorAccountRequest,
    ConnectorCapabilities, ConnectorCatalogEntry, ConnectorCatalogStatus, ConnectorConnectRequest,
    ConnectorConnectResult, ConnectorContinueRequest, ConnectorReconcileOptions,
    ConnectorReconcileRequest, ConnectorSessionAccount, ConnectorSessionAccountResult,
    CustomizationReloadOutcome, CustomizationReloadStatus, CustomizationReloadSubsystem,
    EnqueueCommandResult, Extension, ExtensionList, ExtensionSource, ExtensionStatus,
    ExtensionsDisableRequest, ExtensionsEnableRequest, FleetStartRequest, FleetStartResult,
    McpDisableRequest, McpEnableOptions, McpEnableRequest, McpInstallationOperationStatus,
    McpOauthLoginOptions, McpOauthLoginRequest, McpServer, McpStopServerRequest,
    ModelSetAllowedModelsRequest, ModelSetAllowedModelsResult, ModelSwitchAutoTierRequest,
    ModelSwitchAutoTierResult, ModelSwitchAutoTierStatus, QueuePendingItems, QueuePendingItemsKind,
    SandboxConfig, SendAgentMode, TasksStartAgentRequest, UnsupportedEnqueueCommandResult,
};
use github_copilot_sdk::session_events::{
    McpServerStatus, PermissionRequest, PermissionRequestedData, SessionEventData,
    TypedSessionEvent,
};
use github_copilot_sdk::{AutoTier, AutoTierPreference, SetModelOptions};

#[test]
fn customization_reload_outcome_preserves_future_wire_values() {
    let wire = serde_json::json!({
        "status": "future-status",
        "subsystem": "future-subsystem",
        "detail": "new component",
    });
    let outcome: CustomizationReloadOutcome = serde_json::from_value(wire.clone()).unwrap();
    assert_eq!(serde_json::to_value(outcome).unwrap(), wire);

    assert_eq!(
        serde_json::from_value::<CustomizationReloadStatus>(serde_json::json!("reloaded")).unwrap(),
        CustomizationReloadStatus::Reloaded
    );
    assert_eq!(
        serde_json::from_value::<CustomizationReloadSubsystem>(serde_json::json!("skills"))
            .unwrap(),
        CustomizationReloadSubsystem::Skills
    );
    assert_eq!(
        serde_json::to_value(CustomizationReloadStatus::Reloaded).unwrap(),
        serde_json::json!("reloaded")
    );
    assert_eq!(
        serde_json::to_value(CustomizationReloadSubsystem::Skills).unwrap(),
        serde_json::json!("skills")
    );
}

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
fn host_start_round_trips_absent_and_legacy_child_pid() {
    for pid in [None, Some(1234)] {
        let mut wire = serde_json::json!({
            "hostId": "host",
            "url": "ws://127.0.0.1:4321"
        });
        if let Some(pid) = pid {
            wire["pid"] = serde_json::json!(pid);
        }
        let host: github_copilot_sdk::rpc::HostStartResult =
            serde_json::from_value(wire.clone()).unwrap();
        assert_eq!(host.pid, pid);
        assert_eq!(serde_json::to_value(host).unwrap(), wire);
    }
}

#[test]
fn host_start_round_trips_optional_transport_results() {
    for wire in [
        serde_json::json!({"hostId": "host"}),
        serde_json::json!({"hostId": "host", "environmentId": "environment"}),
        serde_json::json!({
            "hostId": "host", "environmentId": "environment",
            "url": "ws://127.0.0.1:4321", "token": "secret"
        }),
    ] {
        let host: github_copilot_sdk::rpc::HostStartResult =
            serde_json::from_value(wire.clone()).unwrap();
        assert_eq!(host.url.as_deref(), wire["url"].as_str());
        assert_eq!(
            host.environment_id.as_deref(),
            wire["environmentId"].as_str()
        );
        assert_eq!(host.token.as_deref(), wire["token"].as_str());
        assert_eq!(host.pid, None);
        assert_eq!(serde_json::to_value(host).unwrap(), wire);
    }
}

#[test]
fn github_host_environment_requires_name_and_compute_id() {
    use github_copilot_sdk::rpc::HostGitHubEnvironmentOptions;

    for wire in [
        serde_json::json!({}),
        serde_json::json!({"name": "SDK host"}),
        serde_json::json!({"computeId": "compute"}),
    ] {
        assert!(serde_json::from_value::<HostGitHubEnvironmentOptions>(wire).is_err());
    }
    let wire = serde_json::json!({"name": "SDK host", "computeId": "compute"});
    let options: HostGitHubEnvironmentOptions = serde_json::from_value(wire.clone()).unwrap();
    assert_eq!(serde_json::to_value(options).unwrap(), wire);
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

    let targeted = ConnectorReconcileOptions::new("account-1").force_connector_name("github");
    assert_eq!(
        serde_json::to_value(targeted).unwrap(),
        serde_json::json!({
            "accountId": "account-1",
            "forceConnectorName": "github",
        })
    );
}

#[test]
fn connector_session_account_is_nullable_and_credential_free() {
    let account = ConnectorSessionAccount {
        account_id: "session-account-1".to_string(),
        auth_info: AuthIdentityMetadata {
            r#type: AuthInfoType::Token,
            host: "github.com".to_string(),
            login: "alice".to_string(),
        },
    };
    let expected = serde_json::json!({
        "accountId": "session-account-1",
        "authInfo": { "type": "token", "host": "github.com", "login": "alice" },
    });
    assert_eq!(serde_json::to_value(&account).unwrap(), expected);
    let decoded: ConnectorSessionAccountResult = serde_json::from_value(expected).unwrap();
    assert_eq!(decoded.unwrap().account_id, account.account_id);
    let unavailable: ConnectorSessionAccountResult =
        serde_json::from_value(serde_json::Value::Null).unwrap();
    assert!(unavailable.is_none());
    assert_eq!(
        serde_json::to_value(unavailable).unwrap(),
        serde_json::Value::Null
    );
}

#[test]
fn connector_optional_capabilities_are_not_enabled_by_older_runtimes() {
    for flag in [None, Some(false), Some(true)] {
        let mut value = serde_json::json!({
            "apiVersion": 1,
            "availability": "enabled",
            "consentContinuation": true,
            "opaqueAccountSelection": true,
            "maxPollAttempts": 10,
            "maxPollIntervalMs": 5_000,
            "maxDeadlineMs": 60_000,
        });
        if let Some(flag) = flag {
            value["sessionAccountSelection"] = serde_json::json!(flag);
            value["targetedReconcile"] = serde_json::json!(flag);
        }
        let capabilities: ConnectorCapabilities = serde_json::from_value(value).unwrap();
        assert_eq!(capabilities.session_account_selection, flag);
        assert_eq!(capabilities.targeted_reconcile, flag);
        assert_eq!(
            capabilities.session_account_selection == Some(true),
            flag == Some(true)
        );
        assert_eq!(
            capabilities.targeted_reconcile == Some(true),
            flag == Some(true)
        );
    }
}

#[test]
fn connector_catalog_presentation_metadata_is_optional() {
    let legacy = serde_json::json!({
        "name": "github",
        "displayName": "GitHub",
        "status": "connected",
        "runtimeServerIds": ["connector-github"],
    });
    let entry: ConnectorCatalogEntry = serde_json::from_value(legacy.clone()).unwrap();
    assert!(entry.logo.is_none());
    assert!(entry.tier.is_none());
    assert!(entry.release_tag.is_none());
    assert_eq!(serde_json::to_value(entry).unwrap(), legacy);

    let mut decorated = legacy;
    decorated["logo"] = serde_json::json!("https://example.com/github.svg");
    decorated["tier"] = serde_json::json!("standard");
    decorated["releaseTag"] = serde_json::json!("preview");
    let entry: ConnectorCatalogEntry = serde_json::from_value(decorated.clone()).unwrap();
    assert_eq!(
        entry.logo.as_deref(),
        Some("https://example.com/github.svg")
    );
    assert_eq!(entry.tier.as_deref(), Some("standard"));
    assert_eq!(entry.release_tag.as_deref(), Some("preview"));
    assert_eq!(serde_json::to_value(entry).unwrap(), decorated);
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
fn connector_connect_results_do_not_treat_unknown_outcomes_as_connected() {
    let future_result = serde_json::json!({
        "kind": "future_outcome",
        "continuationId": "continuation-1",
        "consentUrl": "https://example.com/consent",
        "status": {
            "apiVersion": 1,
            "availability": "enabled",
            "runtimeServers": [],
            "pendingConnections": 0
        }
    });

    assert!(serde_json::from_value::<ConnectorConnectResult>(future_result).is_err());
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

/// Released structs and enums that are unrelated to the MCP installation payloads keep
/// distinct types, so downstream trait impls, variant imports and literals still compile.
mod released_type_shapes {
    use github_copilot_sdk::rpc::{
        DiagnosticsReadResultEntriesItem, DiagnosticsReadResultEntriesItemSource,
        MetadataContextAttributionResultContextAttribution, MetadataContextInfoResultContextInfo,
        SendMessagesRequestResponseFormat, SendMessagesRequestResponseFormatType,
        SendRequestResponseFormat, SendRequestResponseFormatType,
        SessionDiagnosticsReadResultEntriesItem, SessionDiagnosticsReadResultEntriesItemSource,
        SessionMetadataContextInfoResultContextInfo,
        SessionMetadataGetContextAttributionResultContextAttribution,
        SessionMetadataSnapshotResultWorkspace, SessionMetadataSnapshotWorkspace,
        UpdateSubagentSettingsRequestSubagents,
    };

    trait Released {
        const NAME: &'static str;
    }
    macro_rules! released {
        ($($ty:ty),* $(,)?) => { $(impl Released for $ty { const NAME: &'static str = stringify!($ty); })* };
    }
    // Separate impls on each released name fail to compile if two names alias one type.
    released!(
        DiagnosticsReadResultEntriesItem,
        SessionDiagnosticsReadResultEntriesItem,
        DiagnosticsReadResultEntriesItemSource,
        SessionDiagnosticsReadResultEntriesItemSource,
        MetadataContextAttributionResultContextAttribution,
        SessionMetadataGetContextAttributionResultContextAttribution,
        MetadataContextInfoResultContextInfo,
        SessionMetadataContextInfoResultContextInfo,
        SendRequestResponseFormat,
        SendMessagesRequestResponseFormat,
        SendRequestResponseFormatType,
        SendMessagesRequestResponseFormatType,
        SessionMetadataSnapshotWorkspace,
        SessionMetadataSnapshotResultWorkspace,
        UpdateSubagentSettingsRequestSubagents,
    );

    #[test]
    fn released_names_are_distinct_types() {
        assert_ne!(
            <SendRequestResponseFormat as Released>::NAME,
            <SendMessagesRequestResponseFormat as Released>::NAME
        );
    }

    #[test]
    fn released_enum_variants_import_and_match() {
        use SendRequestResponseFormatType::*;
        let format = SendRequestResponseFormat {
            r#type: JsonSchema,
            ..Default::default()
        };
        assert!(matches!(format.r#type, JsonSchema));
        use DiagnosticsReadResultEntriesItemSource::Mcp;
        assert!(matches!(
            DiagnosticsReadResultEntriesItemSource::default(),
            Mcp
        ));
        assert!(matches!(
            SessionDiagnosticsReadResultEntriesItemSource::default(),
            SessionDiagnosticsReadResultEntriesItemSource::Mcp
        ));
        assert!(matches!(
            SendMessagesRequestResponseFormatType::default(),
            SendMessagesRequestResponseFormatType::JsonSchema
        ));
    }
}
