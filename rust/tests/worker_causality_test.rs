// Copyright (c) Microsoft Corporation. All rights reserved.

#![allow(clippy::unwrap_used)]

use github_copilot_sdk::rpc::TasksSendMessageResult;
use github_copilot_sdk::session_events::TypedSessionEvent;
use serde_json::Value;

fn corpus() -> Value {
    serde_json::from_str(include_str!("../../test/worker-causality.json")).unwrap()
}

fn decode(value: &Value, event: bool) -> Value {
    if event {
        serde_json::to_value(serde_json::from_value::<TypedSessionEvent>(value.clone()).unwrap())
            .unwrap()
    } else {
        serde_json::to_value(
            serde_json::from_value::<TasksSendMessageResult>(value.clone()).unwrap(),
        )
        .unwrap()
    }
}

fn payload(value: &Value, event: bool) -> &Value {
    if event { &value["data"] } else { value }
}

fn payload_mut(value: &mut Value, event: bool) -> &mut Value {
    if event { &mut value["data"] } else { value }
}

#[test]
fn worker_causality_public_readers_preserve_source_and_product() {
    let corpus = corpus();
    for case in corpus["valid"].as_array().unwrap() {
        let event = case.get("event").is_some();
        let mut wire = case[if event { "event" } else { "result" }].clone();
        assert_eq!(
            payload(&decode(&wire, event), event)["workerCausality"],
            payload(&wire, event)["workerCausality"],
            "{}",
            case["name"]
        );
        payload_mut(&mut wire, event)
            .as_object_mut()
            .unwrap()
            .remove("workerCausality");
        let baseline = serde_json::to_vec(&decode(&wire, event)).unwrap();
        for invalid in corpus["invalid"].as_array().unwrap() {
            payload_mut(&mut wire, event)["workerCausality"] = invalid["value"].clone();
            assert_eq!(
                serde_json::to_vec(&decode(&wire, event)).unwrap(),
                baseline,
                "{}: {}",
                case["name"],
                invalid["name"]
            );
        }
    }
}

#[test]
fn worker_causality_exact_utf8_budget_and_unknown_fields() {
    let corpus = corpus();
    for case in corpus["boundaries"].as_array().unwrap() {
        let mut wire = corpus["valid"][0]["event"].clone();
        wire["data"]["workerCausality"] = case["value"].clone();
        let decoded = decode(&wire, true);
        assert_eq!(
            decoded["data"].get("workerCausality").is_some(),
            case["accepted"].as_bool().unwrap(),
            "{}",
            case["name"]
        );
        assert_eq!(decoded["data"]["content"], wire["data"]["content"]);
    }
}
