/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use super::*;

#[test]
fn fs_error_maps_io_not_found_to_enoent() {
    let io_err = std::io::Error::new(std::io::ErrorKind::NotFound, "missing.txt");
    let fs_err: FsError = io_err.into();
    assert!(matches!(fs_err.kind(), FsErrorKind::NotFound(message) if message == "missing.txt"));
    let wire = fs_err.into_wire();
    assert_eq!(wire.code, SessionFsErrorCode::ENOENT);
}

#[test]
fn fs_error_maps_other_io_to_unknown() {
    let io_err = std::io::Error::other("disk full");
    let fs_err: FsError = io_err.into();
    assert!(matches!(fs_err.kind(), FsErrorKind::Other));
    let wire = fs_err.into_wire();
    assert_eq!(wire.code, SessionFsErrorCode::UNKNOWN);
    assert!(wire.message.unwrap().contains("disk full"));
}
