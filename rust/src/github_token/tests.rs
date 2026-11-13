/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use super::*;

#[test]
fn token_debug_is_redacted() {
    let token = GitHubToken::new("do-not-print", 28_800);
    assert!(!format!("{token:?}").contains("do-not-print"));
}
