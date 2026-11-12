// Unit tests for custom API serialization and released Rust source compatibility.

#![allow(clippy::unwrap_used)]

use github_copilot_sdk::AutoTier;
use github_copilot_sdk::rpc::{
    AcceptedEnqueueCommandResult, ConnectorConnectResult, EnqueueCommandResult,
    ModelSwitchAutoTierRequest, UnsupportedEnqueueCommandResult,
};
use github_copilot_sdk::session_events::RecommendedAutoTier;

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
