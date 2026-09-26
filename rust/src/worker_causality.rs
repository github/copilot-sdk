/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

use std::io::{self, Write};

use serde::de::DeserializeOwned;
use serde::{Deserialize, Deserializer};
use serde_json::Value;

struct Budget(usize);

impl Write for Budget {
    fn write(&mut self, bytes: &[u8]) -> io::Result<usize> {
        self.0 = self.0.saturating_add(bytes.len()).min(4097);
        if self.0 > 4096 {
            Err(io::Error::other("optional worker metadata budget"))
        } else {
            Ok(bytes.len())
        }
    }

    fn flush(&mut self) -> io::Result<()> {
        Ok(())
    }
}

fn text(value: &Value) -> bool {
    value.as_str().is_some_and(|value| {
        !value.is_empty() && value.len() <= 256 && !value.chars().any(char::is_control)
    })
}

fn uuid(value: &Value) -> bool {
    value.as_str().is_some_and(|value| {
        value.len() == 36
            && value.bytes().enumerate().all(|(index, byte)| {
                if matches!(index, 8 | 13 | 18 | 23) {
                    byte == b'-'
                } else {
                    byte.is_ascii_hexdigit()
                }
            })
    })
}

fn fields(value: &Value, allowed: &[&str]) -> bool {
    value
        .as_object()
        .is_some_and(|object| object.keys().all(|key| allowed.contains(&key.as_str())))
}

fn reference(value: &Value, event_type: &str) -> bool {
    fields(
        value,
        &["sessionId", "eventId", "agentId", "eventType", "provenance"],
    ) && text(&value["sessionId"])
        && uuid(&value["eventId"])
        && value.get("agentId").is_none_or(text)
        && value["eventType"] == event_type
        && matches!(
            value["provenance"].as_str(),
            Some("native" | "ahp_coordinator")
        )
}

fn source(value: &Value) -> bool {
    let input = &value["input"];
    let Some(admissions) = value["admissions"].as_array() else {
        return false;
    };
    if !fields(
        value,
        &[
            "input",
            "admissions",
            "captureComplete",
            "completion",
            "notification",
            "admittedDuring",
        ],
    ) || !fields(
        input,
        &["queueItemId", "agentId", "sender", "senderBridges"],
    ) || !value["captureComplete"].is_boolean()
        || !uuid(&input["queueItemId"])
        || !text(&input["agentId"])
        || admissions.len() > 32
        || input
            .get("sender")
            .is_some_and(|sender| !reference(sender, "tool.execution_start"))
    {
        return false;
    }
    if let Some(edges) = input.get("senderBridges") {
        let Some(edges) = edges.as_array() else {
            return false;
        };
        if input.get("sender").is_none()
            || edges.len() > 32
            || edges.iter().any(|edge| {
                !fields(edge, &["source", "reported"])
                    || !reference(&edge["source"], "tool.execution_start")
                    || !reference(&edge["reported"], "tool.execution_start")
            })
        {
            return false;
        }
    }
    if admissions.iter().any(|admission| {
        !fields(admission, &["kind", "messageId", "event", "ahpTurnId"])
            || !matches!(
                admission["kind"].as_str(),
                Some("queued_input" | "system_continuation")
            )
            || !text(&admission["messageId"])
            || admission.get("ahpTurnId").is_some_and(|turn| !uuid(turn))
            || admission.get("event").is_some_and(|event| {
                !reference(event, "user.message") || event["agentId"] != input["agentId"]
            })
    }) {
        return false;
    }
    for (field, event_type) in [
        ("completion", "subagent.completed"),
        ("admittedDuring", "assistant.turn_start"),
    ] {
        if value
            .get(field)
            .is_some_and(|event| !reference(event, event_type))
        {
            return false;
        }
    }
    value.get("notification").is_none_or(|notification| {
        fields(notification, &["deliveryId", "event", "mode"])
            && uuid(&notification["deliveryId"])
            && matches!(notification["mode"].as_str(), Some("queued" | "immediate"))
            && notification
                .get("event")
                .is_none_or(|event| reference(event, "system.notification"))
    })
}

// Availability only: consumers must additionally validate placement and enclosing self identity.
fn supported(value: &Value) -> bool {
    fields(
        value,
        &[
            "version",
            "observationProvenance",
            "sources",
            "captureComplete",
        ],
    ) && value["version"].as_u64() == Some(1)
        && matches!(
            value["observationProvenance"].as_str(),
            Some("native" | "ahp_coordinator")
        )
        && value["captureComplete"].is_boolean()
        && value["sources"].as_array().is_some_and(|sources| {
            sources.len() <= 32
                && sources.iter().all(|item| {
                    source(item)
                        && (value["captureComplete"] != true || item["captureComplete"] == true)
                })
        })
}

pub(crate) fn deserialize_optional<'de, D: Deserializer<'de>, T: DeserializeOwned>(
    deserializer: D,
) -> Result<Option<T>, D::Error> {
    let value = Value::deserialize(deserializer)?;
    let field = std::collections::BTreeMap::from([("workerCausality", &value)]);
    if serde_json::to_writer(&mut Budget(0), &field).is_ok()
        && supported(&value)
        && let Ok(capture) = serde_json::from_value(value)
    {
        return Ok(Some(capture));
    }
    tracing::warn!("Ignoring invalid, unsupported or oversized workerCausality metadata");
    Ok(None)
}
