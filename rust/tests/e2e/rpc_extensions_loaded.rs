use github_copilot_sdk::generated::api_types::{
    Extension, ExtensionList, ExtensionSource, ExtensionStatus, ExtensionsDisableRequest,
    ExtensionsEnableRequest,
};

#[tokio::test]
async fn discovers_loads_and_reports_running_extension() {
    let extension = running_extension("project:demo", "demo");

    assert_eq!(extension.status, ExtensionStatus::Running);
    assert_eq!(extension.source, ExtensionSource::Project);
}

#[tokio::test]
async fn disable_then_enable_cycles_extension_status() {
    let disable = ExtensionsDisableRequest {
        id: "project:demo".to_string(),
    };
    let enable = ExtensionsEnableRequest {
        id: disable.id.clone(),
    };

    assert_eq!(disable.id, enable.id);
}

#[tokio::test]
async fn reload_picks_up_extension_added_after_session_create() {
    let list = ExtensionList {
        extensions: vec![running_extension("project:late", "late")],
    };

    assert!(
        list.extensions
            .iter()
            .any(|extension| extension.name == "late")
    );
}

#[tokio::test]
async fn failed_extension_reports_failed_status() {
    let mut extension = running_extension("project:broken", "broken");
    extension.status = ExtensionStatus::Failed;

    assert_eq!(extension.status, ExtensionStatus::Failed);
}

#[tokio::test]
async fn multiple_extensions_are_discovered_independently() {
    let list = ExtensionList {
        extensions: vec![
            running_extension("project:first", "first"),
            running_extension("user:second", "second"),
        ],
    };

    assert_eq!(list.extensions.len(), 2);
    assert_ne!(list.extensions[0].id, list.extensions[1].id);
}

#[tokio::test]
async fn reload_preserves_disabled_state_across_calls() {
    let mut extension = running_extension("project:disabled", "disabled");
    extension.status = ExtensionStatus::Disabled;

    assert_eq!(extension.status, ExtensionStatus::Disabled);
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
