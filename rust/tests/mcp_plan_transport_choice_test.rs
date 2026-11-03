// Copyright (c) Microsoft Corporation. All rights reserved.

#![allow(clippy::unwrap_used)]

use github_copilot_sdk::rpc::{
    McpPlanTransportChoice, McpPlanTransportChoicePackage, McpPlanTransportChoiceRemote,
};
use serde_json::{Value, json};

fn package_choice() -> Value {
    json!({
        "choiceId": "package-choice",
        "installMethod": "package",
        "packageIdentifier": "@example/mcp-server",
        "packageType": "npm",
        "transport": "stdio",
        "requiredValues": [],
        "secretPlaceholders": []
    })
}

fn remote_choice() -> Value {
    json!({
        "choiceId": "remote-choice",
        "installMethod": "remote",
        "endpoint": "https://example.test/mcp",
        "transport": "streamable-http",
        "requiredValues": [],
        "secretPlaceholders": []
    })
}

#[test]
fn unknown_missing_null_and_wrongly_typed_discriminators_are_rejected() {
    for original in [package_choice(), remote_choice()] {
        for discriminator in [
            None,
            Some(Value::Null),
            Some(json!("future-install-method")),
            Some(json!(false)),
            Some(json!(1)),
            Some(json!({})),
        ] {
            let mut wire = original.clone();
            if let Some(discriminator) = discriminator {
                wire["installMethod"] = discriminator;
            } else {
                wire.as_object_mut().unwrap().remove("installMethod");
            }
            assert!(
                serde_json::from_value::<McpPlanTransportChoice>(wire.clone()).is_err(),
                "unexpectedly accepted {wire}"
            );
        }
    }
}

#[test]
fn mismatched_discriminators_and_incomplete_payloads_are_rejected() {
    let mut package = package_choice();
    package["installMethod"] = json!("remote");
    assert!(serde_json::from_value::<McpPlanTransportChoice>(package.clone()).is_err());
    assert!(serde_json::from_value::<McpPlanTransportChoicePackage>(package).is_err());

    let mut remote = remote_choice();
    remote["installMethod"] = json!("package");
    assert!(serde_json::from_value::<McpPlanTransportChoice>(remote.clone()).is_err());
    assert!(serde_json::from_value::<McpPlanTransportChoiceRemote>(remote).is_err());

    for (original, fields) in [
        (package_choice(), vec!["packageType", "packageIdentifier"]),
        (remote_choice(), vec!["endpoint"]),
    ] {
        for field in fields.into_iter().chain([
            "choiceId",
            "transport",
            "requiredValues",
            "secretPlaceholders",
        ]) {
            for replacement in [None, Some(Value::Null), Some(json!(42))] {
                let mut wire = original.clone();
                if let Some(replacement) = replacement {
                    wire[field] = replacement;
                } else {
                    wire.as_object_mut().unwrap().remove(field);
                }
                assert!(
                    serde_json::from_value::<McpPlanTransportChoice>(wire.clone()).is_err(),
                    "unexpectedly accepted {wire}"
                );
            }
        }
    }
}
