// Copyright (c) Microsoft Corporation. All rights reserved.

#![cfg(test)]

use super::*;

struct RangeOnlyProvider;

#[async_trait::async_trait]
impl crate::session_fs::SessionFsProvider for RangeOnlyProvider {
    fn byte_ranges(&self) -> Option<&dyn crate::session_fs::SessionFsReadFileRangeProvider> {
        Some(self)
    }
}

#[async_trait::async_trait]
impl crate::session_fs::SessionFsReadFileRangeProvider for RangeOnlyProvider {
    async fn read_file_range(
        &self,
        path: &str,
        offset: i64,
        length: Option<i64>,
    ) -> Result<crate::session_fs::FileByteRange, FsError> {
        assert_eq!(path, "/file");
        let data = "a☃🚀z".as_bytes();
        let start = (offset as usize).min(data.len());
        let end = start + (length.unwrap_or(data.len() as i64) as usize).min(data.len() - start);
        Ok(crate::session_fs::FileByteRange::new(
            data[start..end].to_vec(),
            data.len() as i64,
        ))
    }
}

#[tokio::test]
async fn range_only_provider_preserves_byte_positions_and_eof() {
    assert!(RangeOnlyProvider.binary().is_none());
    for (offset, length, expected) in [
        (2, Some(2), &"a☃🚀z".as_bytes()[2..4]),
        (4, None, &"a☃🚀z".as_bytes()[4..]),
        (3, Some(0), &b""[..]),
        (7, Some(20), &b"\x80z"[..]),
        (9, None, &b""[..]),
        (100, Some(2), &b""[..]),
    ] {
        let response = read_file_range_response(
            &RangeOnlyProvider,
            &SessionFsReadFileRangeRequest {
                session_id: "session".into(),
                path: "/file".to_owned(),
                offset,
                length,
            },
        )
        .await;
        assert!(response.error.is_none());
        assert_eq!(response.byte_length, 9);
        assert_eq!(
            base64::engine::general_purpose::STANDARD
                .decode(response.content)
                .unwrap(),
            expected
        );
    }
}

#[tokio::test]
async fn invalid_ranges_are_rejected_before_invoking_provider() {
    for (offset, length) in [
        (-1, None),
        (9_007_199_254_740_992, None),
        (0, Some(-1)),
        (0, Some(9_007_199_254_740_992)),
    ] {
        let response = read_file_range_response(
            &RangeOnlyProvider,
            &SessionFsReadFileRangeRequest {
                session_id: "session".into(),
                path: "must-not-be-read".to_owned(),
                offset,
                length,
            },
        )
        .await;
        assert_eq!(response.error.unwrap().code, SessionFsErrorCode::UNKNOWN);
    }
}

struct InvalidRangeProvider;

#[async_trait::async_trait]
impl crate::session_fs::SessionFsProvider for InvalidRangeProvider {
    fn byte_ranges(&self) -> Option<&dyn crate::session_fs::SessionFsReadFileRangeProvider> {
        Some(self)
    }
}

#[async_trait::async_trait]
impl crate::session_fs::SessionFsReadFileRangeProvider for InvalidRangeProvider {
    async fn read_file_range(
        &self,
        _path: &str,
        _offset: i64,
        _length: Option<i64>,
    ) -> Result<crate::session_fs::FileByteRange, FsError> {
        Ok(crate::session_fs::FileByteRange::new(vec![1], 20))
    }
}

#[tokio::test]
async fn malformed_range_results_are_filesystem_errors() {
    let response = read_file_range_response(
        &InvalidRangeProvider,
        &SessionFsReadFileRangeRequest {
            session_id: "session".into(),
            path: "/file".to_owned(),
            offset: 1,
            length: Some(2),
        },
    )
    .await;
    assert!(
        response
            .error
            .unwrap()
            .message
            .unwrap()
            .contains("incorrect byte range length")
    );
}

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
