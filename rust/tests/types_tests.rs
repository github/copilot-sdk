//! Tests for core types and serialization

use github_copilot_sdk::{ClientOptions, SessionConfig, SystemMessage};

#[test]
fn test_client_options_default() {
    let options = ClientOptions::default();

    assert_eq!(options.cli_path, "copilot");
    assert!(options.use_stdio);
    assert!(options.auto_start);
    assert!(options.auto_restart);
    assert_eq!(options.log_level, "info");
}

#[test]
fn test_session_config_serialization() {
    let config = SessionConfig {
        model: Some("gpt-4o".to_string()),
        system_message: Some(SystemMessage::Append {
            content: Some("Test".to_string()),
        }),
        ..Default::default()
    };

    let json = serde_json::to_string(&config).unwrap();
    let deserialized: SessionConfig = serde_json::from_str(&json).unwrap();

    assert_eq!(deserialized.model, Some("gpt-4o".to_string()));
}

#[test]
fn test_system_message_append() {
    let msg = SystemMessage::Append {
        content: Some("Additional instructions".to_string()),
    };

    let json = serde_json::to_value(&msg).unwrap();
    assert_eq!(json["mode"], "append");
    assert_eq!(json["content"], "Additional instructions");
}

#[test]
fn test_system_message_replace() {
    let msg = SystemMessage::Replace {
        content: "Complete replacement".to_string(),
    };

    let json = serde_json::to_value(&msg).unwrap();
    assert_eq!(json["mode"], "replace");
    assert_eq!(json["content"], "Complete replacement");
}
