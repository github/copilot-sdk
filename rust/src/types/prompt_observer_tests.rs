// Copyright (c) Microsoft Corporation. All rights reserved.

#![cfg(test)]

use super::*;

#[test]
fn omitted_prompt_observation_preserves_create_and_resume_wire_shapes() {
    let (create, _) = SessionConfig::default().into_wire(None).unwrap();
    let (resume, _) = ResumeSessionConfig::new(SessionId::from("observer"))
        .into_wire()
        .unwrap();
    for wire in [
        serde_json::to_value(create).unwrap(),
        serde_json::to_value(resume).unwrap(),
    ] {
        assert!(wire.get("observePromptEvents").is_none());
        assert_eq!(wire["requestUserInput"], false);
        assert_eq!(wire["requestPermission"], false);
        assert_eq!(wire["requestElicitation"], false);
    }
}

#[test]
fn prompt_observation_is_serialized_independently_of_callback_handlers() {
    for enabled in [true, false] {
        let (create, _) = SessionConfig::default()
            .with_observe_prompt_events(enabled)
            .into_wire(None)
            .unwrap();
        let (resume, _) = ResumeSessionConfig::new(SessionId::from("observer"))
            .with_observe_prompt_events(enabled)
            .into_wire()
            .unwrap();
        for wire in [
            serde_json::to_value(create).unwrap(),
            serde_json::to_value(resume).unwrap(),
        ] {
            assert_eq!(wire["observePromptEvents"], enabled);
            assert_eq!(wire["requestUserInput"], false);
            assert_eq!(wire["requestPermission"], false);
            assert_eq!(wire["requestElicitation"], false);
        }
    }
}
