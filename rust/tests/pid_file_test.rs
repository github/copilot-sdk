use std::io::{self, Write};

#[path = "fixtures/pid_file.rs"]
mod pid_file;

#[test]
fn publishes_only_the_complete_pid() {
    let dir = tempfile::tempdir().expect("create test directory");
    let path = dir.path().join("cli.pid");

    pid_file::publish_pid_file(&path, |file| {
        assert!(!path.exists(), "an empty PID must not be visible");
        file.write_all(b"12")?;
        assert!(!path.exists(), "a partial PID must not be visible");
        file.write_all(b"345")
    })
    .expect("publish complete PID");

    assert_eq!(
        std::fs::read_to_string(path).expect("read published PID"),
        "12345"
    );
}

#[test]
fn does_not_publish_a_failed_write() {
    let dir = tempfile::tempdir().expect("create test directory");
    let path = dir.path().join("cli.pid");

    let error = pid_file::publish_pid_file(&path, |file| {
        file.write_all(b"12")?;
        Err(io::Error::other("incomplete PID"))
    })
    .expect_err("reject incomplete PID write");

    assert_eq!(error.kind(), io::ErrorKind::Other);
    assert!(!path.exists(), "a failed PID write must not become ready");
}
