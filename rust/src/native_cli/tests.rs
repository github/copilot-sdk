#![cfg(test)]

use serde_json::json;

use super::*;
use crate::ffi::initiation_tests::Peer;

#[tokio::test]
async fn missing_private_export_closes_only_the_new_non_owning_connection() {
    let original = Peer::with_ownership(true);
    let borrowed = Peer::with_ownership(false);
    let handle = borrowed.client().clone();
    assert_eq!(
        handle.call("ping", None).await.unwrap(),
        json!({"method": "ping"}),
    );

    let error = finish_start(handle.clone(), |_| Ok::<_, Error>(())).unwrap_err();
    assert!(
        error
            .to_string()
            .contains("is missing an expected export (copilot_runtime_cli_host_connection_admit)"),
        "{error}",
    );
    assert_eq!(handle.in_process_connection_id(), None);
    assert_eq!(borrowed.shutdowns(), 0);
    assert_eq!(original.shutdowns(), 0);
    assert_eq!(
        original.client().call("ping", None).await.unwrap(),
        json!({"method": "ping"}),
    );
}

#[tokio::test]
async fn image_validation_rejects_before_private_symbol_access() {
    let borrowed = Peer::with_ownership(false);
    let handle = borrowed.client().clone();
    let error = finish_start(handle.clone(), |_| {
        Err(Error::with_message(
            ErrorKind::InvalidConfig,
            "image identity rejected",
        ))
    })
    .unwrap_err();
    assert_eq!(error.to_string(), "image identity rejected");
    assert_eq!(handle.in_process_connection_id(), None);
    assert_eq!(borrowed.shutdowns(), 0);
}
