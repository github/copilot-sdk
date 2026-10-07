/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use super::*;

fn session_id() -> SessionId {
    SessionId::new("router-ownership")
}

#[test]
fn each_registration_gets_a_distinct_token() {
    let router = SessionRouter::new();
    let first = router.register(&session_id());
    let second = router.register(&session_id());
    assert_ne!(first.token, second.token);
}

#[test]
fn unregister_owned_removes_only_the_matching_registration() {
    let router = SessionRouter::new();
    let stale = router.register(&session_id());
    let live = router.register(&session_id());

    // The stale owner must not evict the registration that replaced it.
    assert!(!router.unregister_owned(&session_id(), stale.token));
    assert_eq!(router.session_ids(), vec![session_id()]);

    assert!(router.unregister_owned(&session_id(), live.token));
    assert!(router.session_ids().is_empty());

    // Removing twice is a no-op rather than evicting a future tenant.
    assert!(!router.unregister_owned(&session_id(), live.token));
}
