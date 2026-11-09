// Unit tests for custom API serialization and released Rust source compatibility.

#![allow(clippy::unwrap_used)]

use github_copilot_sdk::AutoTier;
use github_copilot_sdk::rpc::{
    AcceptedEnqueueCommandResult, ConnectorConnectResult, EnqueueCommandResult,
    ModelSwitchAutoTierRequest, ProtocolExternalToolDefinition, SessionsOpenCreate,
    SessionsOpenCreateKind, SessionsOpenResume, SessionsOpenResumeKind,
    UnsupportedEnqueueCommandResult,
};
use github_copilot_sdk::session_events::RecommendedAutoTier;

macro_rules! filesystem_callback_result {
    ($name:ident, $result:ty) => {
        #[test]
        fn $name() {
            for wire in [
                serde_json::Value::Null,
                serde_json::json!({ "code": "ENOENT", "message": "missing file" }),
            ] {
                let result: $result = serde_json::from_value(wire.clone())
                    .expect("filesystem callback results must accept success and failure");
                assert_eq!(serde_json::to_value(result).unwrap(), wire);
            }
        }
    };
}

filesystem_callback_result!(
    filesystem_write_file_result_accepts_null_success,
    github_copilot_sdk::rpc::SessionFsWriteFileResult
);
filesystem_callback_result!(
    filesystem_write_file_bytes_result_accepts_null_success,
    github_copilot_sdk::rpc::SessionFsWriteFileBytesResult
);
filesystem_callback_result!(
    filesystem_append_file_result_accepts_null_success,
    github_copilot_sdk::rpc::SessionFsAppendFileResult
);
filesystem_callback_result!(
    filesystem_mkdir_result_accepts_null_success,
    github_copilot_sdk::rpc::SessionFsMkdirResult
);
filesystem_callback_result!(
    filesystem_rm_result_accepts_null_success,
    github_copilot_sdk::rpc::SessionFsRmResult
);
filesystem_callback_result!(
    filesystem_rename_result_accepts_null_success,
    github_copilot_sdk::rpc::SessionFsRenameResult
);

#[test]
fn session_open_create_preserves_external_struct_literals() {
    let request = SessionsOpenCreate {
        kind: SessionsOpenCreateKind::Create,
        ..Default::default()
    };
    assert_eq!(
        serde_json::to_value(request).unwrap(),
        serde_json::json!({"kind": "create"})
    );
    let explicit = SessionsOpenCreate {
        emit_start: Some(false),
        kind: SessionsOpenCreateKind::Create,
        options: None,
    };
    assert_eq!(
        serde_json::to_value(explicit).unwrap(),
        serde_json::json!({"kind": "create", "emitStart": false})
    );
    let decoded: SessionsOpenCreate = serde_json::from_value(serde_json::json!({
        "kind": "create", "hostConstructionToken": 123
    }))
    .unwrap();
    assert_eq!(
        serde_json::to_value(decoded).unwrap(),
        serde_json::json!({"kind": "create"})
    );
}

#[test]
fn session_open_resume_preserves_external_struct_literals() {
    let request = SessionsOpenResume {
        kind: SessionsOpenResumeKind::Resume,
        session_id: "resume-session".into(),
        ..Default::default()
    };
    assert_eq!(
        serde_json::to_value(request).unwrap(),
        serde_json::json!({"kind": "resume", "sessionId": "resume-session"})
    );
    let explicit = SessionsOpenResume {
        kind: SessionsOpenResumeKind::Resume,
        options: None,
        resume: Some(false),
        session_id: "resume-session".into(),
        suppress_resume_workspace_metadata_writeback: Some(true),
    };
    assert_eq!(
        serde_json::to_value(explicit).unwrap(),
        serde_json::json!({
            "kind": "resume", "sessionId": "resume-session", "resume": false,
            "suppressResumeWorkspaceMetadataWriteback": true
        })
    );
    let decoded: SessionsOpenResume = serde_json::from_value(serde_json::json!({
        "kind": "resume", "sessionId": "resume-session", "hostConstructionToken": 123
    }))
    .unwrap();
    assert_eq!(
        serde_json::to_value(decoded).unwrap(),
        serde_json::json!({"kind": "resume", "sessionId": "resume-session"})
    );
}

#[test]
fn external_tool_definitions_preserve_public_struct_literals() {
    let definition = ProtocolExternalToolDefinition {
        defer: None,
        description: "Description".to_owned(),
        idempotent: None,
        is_terminal: Some(true),
        metadata: None,
        name: "lookup".to_owned(),
        namespace: None,
        overrides_built_in_tool: None,
        parameters: None,
        skip_permission: None,
        title: None,
    };
    let defaults = ProtocolExternalToolDefinition {
        name: definition.name.clone(),
        ..Default::default()
    };
    assert_eq!(defaults.name, definition.name);
    assert_eq!(
        serde_json::to_value(&definition).unwrap(),
        serde_json::json!({
            "description": "Description",
            "name": "lookup",
            "isTerminal": true
        })
    );
}

#[test]
fn extensible_tier_unknown_literal_retains_its_provider_identity() {
    let wire = serde_json::json!("Unknown");
    let tier: AutoTier = serde_json::from_value(wire.clone()).unwrap();
    assert_eq!(tier, AutoTier::Custom("Unknown".to_owned()));
    assert_eq!(serde_json::to_value(tier).unwrap(), wire);
    let recommendation: RecommendedAutoTier = serde_json::from_value(wire.clone()).unwrap();
    assert_eq!(
        recommendation,
        RecommendedAutoTier::Custom("Unknown".to_owned())
    );
    assert_eq!(serde_json::to_value(recommendation).unwrap(), wire);
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
