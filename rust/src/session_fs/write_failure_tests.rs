#![cfg(test)]

use super::{FsError, FsErrorKind, SessionFsErrorCode};

#[test]
fn failed_write_reports_only_known_changes() {
    let ordinary = FsError::with_message(FsErrorKind::Other, "rejected").into_write_wire();
    assert_eq!(ordinary.code, SessionFsErrorCode::UNKNOWN);
    assert_eq!(ordinary.write_changed, None);

    let changed = FsError::with_message(FsErrorKind::Other, "disk full")
        .with_write_changed()
        .into_write_wire();
    assert_eq!(changed.message.as_deref(), Some("disk full"));
    assert_eq!(changed.write_changed, Some(true));
    let json = serde_json::to_value(changed).unwrap();
    assert_eq!(json["writeChanged"], true);
}

#[test]
fn write_marker_does_not_leak_into_other_methods() {
    let read_error = FsError::with_message(FsErrorKind::Other, "read failed")
        .with_write_changed()
        .into_wire();
    assert_eq!(read_error.write_changed, None);
}
