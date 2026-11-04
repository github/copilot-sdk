// Copyright (c) Microsoft Corporation. All rights reserved.

#![cfg(test)]

use std::path::PathBuf;

use serde_json::{Value, json};

use super::{ResumeSessionConfig, SessionConfig, SessionId};

fn assert_roots(wire: &Value, roots: Option<&[PathBuf]>) {
    for (key, kind) in [
        ("skillDirectories", "skills"),
        ("agentDirectories", "agents"),
        ("instructionDirectories", "instructions"),
        ("hookDirectories", "hooks"),
    ] {
        match roots {
            Some(paths) => assert_eq!(
                wire[key],
                serde_json::to_value(paths.iter().map(|path| path.join(kind)).collect::<Vec<_>>())
                    .unwrap()
            ),
            None => assert!(wire.get(key).is_none(), "unexpected {key}: {wire}"),
        }
    }
    assert!(wire.get("additionalDirectories").is_none());
    assert!(wire.get("customAgents").is_none());
    assert_eq!(wire["hooks"], false);
    assert_eq!(wire["enableFileHooks"], false);
    assert_eq!(wire["enableConfigDiscovery"], false);
}

#[test]
fn create_and_resume_preserve_discovery_roots_without_client_rebasing() {
    for roots in [
        None,
        Some(Vec::new()),
        Some(vec![
            PathBuf::from("relative-root"),
            PathBuf::from("other-root"),
        ]),
    ] {
        let mut create = SessionConfig::default()
            .with_working_directory("request-cwd")
            .with_enable_config_discovery(false)
            .with_enable_file_hooks(false);
        let mut resume = ResumeSessionConfig::new(SessionId::from("resumed"))
            .with_working_directory("resume-cwd")
            .with_enable_config_discovery(false)
            .with_enable_file_hooks(false);
        if let Some(paths) = &roots {
            create = create
                .with_skill_directories(paths.iter().map(|path| path.join("skills")))
                .with_agent_directories(paths.iter().map(|path| path.join("agents")))
                .with_instruction_directories(paths.iter().map(|path| path.join("instructions")))
                .with_hook_directories(paths.iter().map(|path| path.join("hooks")));
            resume = resume
                .with_skill_directories(paths.iter().map(|path| path.join("skills")))
                .with_agent_directories(paths.iter().map(|path| path.join("agents")))
                .with_instruction_directories(paths.iter().map(|path| path.join("instructions")))
                .with_hook_directories(paths.iter().map(|path| path.join("hooks")));
        }
        let (wire, _) = create.into_wire(None).unwrap();
        assert_roots(&serde_json::to_value(wire).unwrap(), roots.as_deref());
        let (wire, _) = resume.into_wire().unwrap();
        assert_roots(&serde_json::to_value(wire).unwrap(), roots.as_deref());
    }
}

#[test]
fn host_factory_forwards_agent_and_hook_roots() {
    let create = SessionConfig::default()
        .with_agent_directories(["relative-agents"])
        .with_hook_directories(Vec::<PathBuf>::new());
    let resume = ResumeSessionConfig::new(SessionId::from("resumed"))
        .with_agent_directories(["relative-agents"])
        .with_hook_directories(Vec::<PathBuf>::new());
    for wire in [
        crate::ahp_host::config_for_host(&create).unwrap(),
        crate::ahp_host::resume_config_for_host(&resume).unwrap(),
    ] {
        assert_eq!(wire["agentDirectories"], json!(["relative-agents"]));
        assert_eq!(wire["hookDirectories"], json!([]));
        assert!(wire.get("customAgents").is_none());
        assert!(wire.get("hooks").is_none());
    }
}
