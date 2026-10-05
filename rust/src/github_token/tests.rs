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

#[test]
fn retiring_session_removes_its_provider() {
    let registry = GitHubTokenRegistry::new();
    let provider = Arc::new(|_args: GitHubTokenProviderArgs| async {
        Ok(GitHubTokenProviderResult::Cancelled)
    });
    let registration_id = registry.register(provider);
    let session_id = crate::SessionId::from("session-1");
    registry.claim(&registration_id, session_id.clone());

    registry.retire_session(&session_id);

    assert!(
        !registry
            .state
            .lock()
            .providers
            .contains_key(&registration_id)
    );
}
