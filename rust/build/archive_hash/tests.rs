// Copyright (c) Microsoft Corporation. All rights reserved.
#![cfg(test)]

use super::*;

// Independently computed with Node's crypto.createHash("sha256").
const MULTIBLOCK_DIGEST: &str = "310eafc6f5d7a12015734d3a92a8fd03ab7d3c7c7512ab1e4bcc1662ef0389d9";

fn digest_hex(digest: [u8; 32]) -> String {
    digest.iter().map(|b| format!("{b:02x}")).collect()
}

#[test]
fn archive_digest_matches_independent_empty_and_nonempty_known_answers() {
    for (data, expected) in [
        (
            b"".as_slice(),
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        ),
        (
            b"abc".as_slice(),
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
        ),
    ] {
        let actual = sha256(data);
        assert_eq!(digest_hex(actual), expected);
    }
}

#[test]
fn archive_digest_matches_independent_multiblock_digest() {
    let data: Vec<u8> = (0..131_073).map(|index| (index * 53) as u8).collect();
    assert_eq!(digest_hex(sha256(&data)), MULTIBLOCK_DIGEST);
}

#[test]
fn archive_digest_preserves_every_binary_byte() {
    let bytes: Vec<u8> = (0..=255).collect();
    assert_eq!(
        digest_hex(sha256(&bytes)),
        "40aff2e9d2d8922e47afd4648e6967497158785fbd1da870e7110266bf944880"
    );
}

#[cfg(windows)]
#[test]
fn cng_streaming_matches_independent_digest_across_chunk_boundaries() {
    let data: Vec<u8> = (0..131_073).map(|index| (index * 53) as u8).collect();
    assert_eq!(digest_hex(sha256(&data)), MULTIBLOCK_DIGEST);
    for chunk_size in [1, 63, 64, 65, 4096, 65_537] {
        assert_eq!(
            digest_hex(windows::hash_chunks(data.chunks(chunk_size)).unwrap()),
            MULTIBLOCK_DIGEST,
            "CNG chunk size {chunk_size}"
        );
    }
    assert_eq!(
        windows::hash_chunks(std::iter::empty()).unwrap(),
        sha256(b"")
    );
}

#[cfg(windows)]
#[test]
fn cng_status_errors_preserve_operation_and_native_status() {
    let error = windows::check_status("BCryptHashData", 0xc000000d_u32 as i32).unwrap_err();
    assert_eq!(error.kind(), std::io::ErrorKind::Other);
    assert_eq!(
        error.to_string(),
        "BCryptHashData failed: NTSTATUS 0xc000000d"
    );
    windows::check_status("BCryptHashData", 0).unwrap();
}
