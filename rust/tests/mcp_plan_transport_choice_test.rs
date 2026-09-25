// Copyright (c) Microsoft Corporation. All rights reserved.

#![allow(clippy::unwrap_used)]

use github_copilot_sdk::rpc::{
    McpInstallPlan, McpPlanPackageInstallMethod, McpPlanRemoteInstallMethod,
    McpPlanRemoteTransport, McpPlanTransportChoice, McpPlanTransportChoicePackage,
    McpPlanTransportChoiceRemote,
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
fn plan_keeps_released_raw_choices_and_decodes_them_explicitly() {
    let wire = json!([package_choice(), remote_choice()]);
    // The released `Vec<serde_json::Value>` field shape is unchanged.
    let plan = McpInstallPlan {
        transport_choices: serde_json::from_value(wire.clone()).unwrap(),
        ..Default::default()
    };
    let choices: Vec<McpPlanTransportChoice> =
        serde_json::from_value(Value::Array(plan.transport_choices.clone())).unwrap();

    let McpPlanTransportChoice::Package(package) = &choices[0] else {
        panic!("expected package choice");
    };
    assert_eq!(package.choice_id, "package-choice");
    assert_eq!(package.package_identifier, "@example/mcp-server");
    assert_eq!(package.install_method, McpPlanPackageInstallMethod::Package);

    let McpPlanTransportChoice::Remote(remote) = &choices[1] else {
        panic!("expected remote choice");
    };
    assert_eq!(remote.choice_id, "remote-choice");
    assert_eq!(remote.endpoint, "https://example.test/mcp");
    assert_eq!(remote.install_method, McpPlanRemoteInstallMethod::Remote);
    assert_eq!(remote.transport, McpPlanRemoteTransport::StreamableHttp);
    assert!(remote.required_values.is_empty());
    assert!(remote.secret_placeholders.is_empty());
    assert_eq!(serde_json::to_value(&choices).unwrap(), wire);
    assert_eq!(serde_json::to_value(&plan.transport_choices).unwrap(), wire);
}

#[test]
fn choices_preserve_declared_values_and_secret_placeholders() {
    let mut wire = remote_choice();
    wire["requiredValues"] = json!([{
        "kind": "enum",
        "key": "region",
        "category": "url-variable",
        "valueType": "enum",
        "enumValues": ["eu", "us"],
        "required": true,
        "isRepeated": false,
        "defaultValue": "eu",
        "title": "Region"
    }]);
    wire["secretPlaceholders"] = json!([{
        "key": "token",
        "placeholder": "${secret:opaque-id}",
        "title": "Access token"
    }]);
    let choice: McpPlanTransportChoice = serde_json::from_value(wire.clone()).unwrap();
    assert_eq!(serde_json::to_value(choice).unwrap(), wire);
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

#[test]
fn unconstrained_enums_retain_forward_compatibility() {
    assert_eq!(
        serde_json::from_value::<McpPlanPackageInstallMethod>(json!("future-method")).unwrap(),
        McpPlanPackageInstallMethod::Unknown
    );
    assert_eq!(
        serde_json::from_value::<McpPlanRemoteInstallMethod>(json!("future-method")).unwrap(),
        McpPlanRemoteInstallMethod::Unknown
    );

    let mut wire = remote_choice();
    wire["transport"] = json!("future-transport");
    let McpPlanTransportChoice::Remote(remote) =
        serde_json::from_value::<McpPlanTransportChoice>(wire).unwrap()
    else {
        panic!("expected remote choice");
    };
    assert_eq!(remote.transport, McpPlanRemoteTransport::Unknown);
}

#[test]
fn extra_fields_retain_existing_struct_compatibility() {
    let original = remote_choice();
    let mut wire = original.clone();
    wire["futureMetadata"] = json!({"version": 2});
    let choice: McpPlanTransportChoice = serde_json::from_value(wire).unwrap();
    assert_eq!(serde_json::to_value(choice).unwrap(), original);
}
