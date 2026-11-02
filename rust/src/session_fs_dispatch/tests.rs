// Copyright (c) Microsoft Corporation. All rights reserved.

#![cfg(test)]

use super::*;

#[test]
fn oversized_binary_provider_result_is_a_filesystem_error() {
    let result = binary_read_response(Ok(vec![0; MAX_BINARY_BYTES + 1]));
    assert!(result.content.is_empty());
    let error = result.error.expect("oversized result must carry an error");
    assert_eq!(error.code, SessionFsErrorCode::UNKNOWN);
    assert!(
        error
            .message
            .as_deref()
            .is_some_and(|message| message.contains("binary read limit"))
    );
}

#[test]
fn small_binary_provider_result_preserves_exact_bytes() {
    let result = binary_read_response(Ok(vec![0, 255, 254, 1]));
    assert_eq!(result.content, "AP/+AQ==");
    assert!(result.error.is_none());
}
