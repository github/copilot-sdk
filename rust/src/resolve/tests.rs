/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use std::fs;

use tempfile::tempdir;

use super::validate_runtime_pair;

#[test]
fn runtime_pair_requires_adjacent_nonempty_runtime_node() {
    let dir = tempdir().expect("temp dir");
    let wrapper = dir.path().join(if cfg!(windows) {
        "copilot-runtime.exe"
    } else {
        "copilot-runtime"
    });
    fs::write(&wrapper, b"wrapper").expect("write wrapper");

    let error = validate_runtime_pair(&wrapper).expect_err("runtime.node is required");
    assert!(error.to_string().contains("runtime.node"));

    fs::write(dir.path().join("runtime.node"), b"runtime").expect("write runtime.node");
    validate_runtime_pair(&wrapper).expect("complete pair is valid");
}
