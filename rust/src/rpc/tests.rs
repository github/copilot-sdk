#![cfg(test)]

use serde_json::json;

use super::*;

#[test]
fn saved_provider_conversion_preserves_settings_without_credentials() {
    for kind in [
        Some("openai"),
        Some("azure"),
        Some("anthropic"),
        Some("winml"),
        None,
    ] {
        let mut settings = json!({
            "name": "discovered",
            "baseUrl": "http://127.0.0.1:1234",
            "wireApi": "responses",
            "transport": "http",
            "azure": { "apiVersion": "2025-01-01" },
            "localModel": { "device": "gpu", "future": [1, true] },
            "modelProvider": "azure_openai"
        });
        if let Some(kind) = kind {
            settings["type"] = json!(kind);
        }
        let mut input = settings.clone();
        input["apiKey"] = json!("fixture-key");
        input["bearerToken"] = json!("fixture-token");
        input["headers"] = json!({ "Authorization": "fixture-header" });
        input["hasBearerTokenProvider"] = json!(true);
        let provider: NamedProviderConfig = serde_json::from_value(input).unwrap();

        let saved = to_saved_provider_configuration(provider).unwrap();

        assert_eq!(
            serde_json::to_value(saved).unwrap(),
            settings,
            "type: {kind:?}"
        );
    }
}

#[test]
fn saved_provider_conversion_omits_absent_optional_settings() {
    let settings = json!({ "name": "minimal", "baseUrl": "http://127.0.0.1:1234" });
    let provider = serde_json::from_value(settings.clone()).unwrap();

    let saved = to_saved_provider_configuration(provider).unwrap();

    assert_eq!(serde_json::to_value(saved).unwrap(), settings);
}

#[test]
fn saved_provider_conversion_rejects_unknown_provider_type() {
    let provider = serde_json::from_value(json!({
        "name": "future", "baseUrl": "http://127.0.0.1:1234", "type": "future-provider"
    }))
    .unwrap();

    let error = to_saved_provider_configuration(provider).unwrap_err();

    assert_eq!(error.kind(), &crate::ErrorKind::InvalidConfig);
    assert!(
        error
            .to_string()
            .contains("unknown discovered provider type")
    );
}
