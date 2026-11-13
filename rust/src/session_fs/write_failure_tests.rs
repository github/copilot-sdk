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
