#![cfg(test)]

use std::os::windows::io::{AsRawHandle, FromRawHandle, OwnedHandle};
use std::os::windows::process::CommandExt;
use std::path::{Path, PathBuf};
use std::sync::Arc;
use std::time::{Duration, Instant};
use std::{io, ptr};

use tokio::process::{Child, Command};
use windows_sys::Win32::Foundation::{
    DUPLICATE_SAME_ACCESS, DuplicateHandle, ERROR_ACCESS_DENIED, HANDLE,
};
use windows_sys::Win32::System::JobObjects::IsProcessInJob;
use windows_sys::Win32::System::Threading::{
    GetCurrentProcess, GetCurrentProcessId, GetCurrentThreadId, OpenThread,
    THREAD_QUERY_LIMITED_INFORMATION, THREAD_SUSPEND_RESUME, WaitForSingleObject,
};

use super::platform::{check_status, resume_initial_thread, resume_owned_thread};

#[derive(Clone, Copy, PartialEq)]
pub(super) enum FailurePoint {
    Capture,
    Marker,
    Walk,
    OpenThread,
    QueryThread,
    Resume,
}

type BeforeAssignment = Box<dyn FnOnce(&Child) -> io::Result<()>>;
type BeforeResume = Box<dyn FnOnce(&Child, HANDLE) -> io::Result<()>>;

thread_local! {
    pub(super) static THREADS_EXAMINED: std::cell::Cell<u32> = const { std::cell::Cell::new(0) };
    pub(super) static RESUME_ELAPSED: std::cell::Cell<Duration> =
        const { std::cell::Cell::new(Duration::ZERO) };
    pub(super) static BEFORE_ASSIGNMENT: std::cell::RefCell<Option<BeforeAssignment>> =
        const { std::cell::RefCell::new(None) };
    pub(super) static BEFORE_RESUME: std::cell::RefCell<Option<BeforeResume>> =
        const { std::cell::RefCell::new(None) };
    static FAILURE: std::cell::Cell<Option<FailurePoint>> =
        const { std::cell::Cell::new(None) };
    pub(super) static SNAPSHOTS: std::cell::Cell<(u32, u32)> =
        const { std::cell::Cell::new((0, 0)) };
    pub(super) static MARKERS: std::cell::Cell<(u32, u32)> =
        const { std::cell::Cell::new((0, 0)) };
}

pub(super) fn fail_at(point: FailurePoint) -> io::Result<()> {
    if FAILURE.get() == Some(point) {
        Err(io::Error::from_raw_os_error(ERROR_ACCESS_DENIED as i32))
    } else {
        Ok(())
    }
}

struct ResetHooks;

impl Drop for ResetHooks {
    fn drop(&mut self) {
        FAILURE.set(None);
        BEFORE_ASSIGNMENT.with_borrow_mut(|hook| *hook = None);
        BEFORE_RESUME.with_borrow_mut(|hook| *hook = None);
    }
}

fn command() -> Command {
    let mut command = Command::new(
        PathBuf::from(std::env::var_os("SystemRoot").expect("SystemRoot"))
            .join("System32")
            .join("cmd.exe"),
    );
    command.args(["/d", "/c"]).kill_on_drop(true);
    command
}

fn marker_command(marker: &Path) -> Command {
    let mut command = command();
    command
        .as_std_mut()
        .raw_arg(format!("echo ready>\"{}\"", marker.display()));
    command
}

fn retain_process(child: &Child) -> OwnedHandle {
    let mut handle = ptr::null_mut();
    // SAFETY: the child owns its live process handle; the duplicate is independently owned.
    let result = unsafe {
        DuplicateHandle(
            GetCurrentProcess(),
            child.raw_handle().expect("live child").cast(),
            GetCurrentProcess(),
            &mut handle,
            0,
            0,
            DUPLICATE_SAME_ACCESS,
        )
    };
    assert_ne!(result, 0, "{}", io::Error::last_os_error());
    // SAFETY: DuplicateHandle returned a new uniquely owned process handle.
    unsafe { OwnedHandle::from_raw_handle(handle) }
}

fn assert_exited(handle: &OwnedHandle) {
    // SAFETY: the independently owned process handle remains valid.
    assert_eq!(
        unsafe { WaitForSingleObject(handle.as_raw_handle().cast(), 5000) },
        0,
        "owned suspended root must terminate"
    );
}

#[tokio::test]
async fn initial_thread_lookup_is_process_scoped() {
    let mut command = command();
    command.args(["exit", "/b", "0"]);
    let start = Instant::now();
    let (mut child, tree) = super::spawn(&mut command).expect("spawn owned suspended child");
    let spawn_elapsed = start.elapsed();
    let status = tokio::time::timeout(Duration::from_secs(5), child.wait())
        .await
        .expect("initial thread must execute")
        .expect("reap child");
    drop(tree);
    assert!(status.success());
    eprintln!(
        "owned lookup: spawn_ms={:.3} resume_ms={:.3} entries={}",
        spawn_elapsed.as_secs_f64() * 1000.0,
        RESUME_ELAPSED.get().as_secs_f64() * 1000.0,
        THREADS_EXAMINED.get()
    );
    assert_eq!(
        THREADS_EXAMINED.get(),
        1,
        "lookup must not walk unrelated machine-wide threads"
    );
}

#[tokio::test]
async fn initial_thread_executes_only_after_private_job_assignment() {
    let _reset = ResetHooks;
    let dir = tempfile::tempdir().expect("fixture directory");
    let marker = dir.path().join("executed.txt");
    BEFORE_ASSIGNMENT.with_borrow_mut(|hook| {
        let marker = marker.clone();
        *hook = Some(Box::new(move |_| {
            assert!(
                !marker.exists(),
                "suspended root cannot execute before assignment"
            );
            Ok(())
        }));
    });
    BEFORE_RESUME.with_borrow_mut(|hook| {
        let marker = marker.clone();
        *hook = Some(Box::new(move |child, job| {
            assert!(
                !marker.exists(),
                "suspended initial thread must not have executed"
            );
            let mut assigned = 0;
            // SAFETY: both handles remain owned throughout the hook.
            assert_ne!(
                unsafe {
                    IsProcessInJob(
                        child.raw_handle().expect("live child").cast(),
                        job,
                        &mut assigned,
                    )
                },
                0,
                "{}",
                io::Error::last_os_error()
            );
            assert_ne!(
                assigned, 0,
                "root must belong to its private Job before resume"
            );
            Ok(())
        }));
    });
    let (mut child, tree) = super::spawn(&mut marker_command(&marker)).expect("owned spawn");
    let status = tokio::time::timeout(Duration::from_secs(5), child.wait())
        .await
        .expect("fixture must execute its initial thread")
        .expect("reap fixture");
    assert!(status.success());
    assert_eq!(
        std::fs::read_to_string(marker)
            .expect("executed marker")
            .trim(),
        "ready"
    );
    drop(tree);
}

#[tokio::test]
async fn lookup_failures_reap_suspended_root_and_release_pss_resources() {
    let _reset = ResetHooks;
    for point in [
        FailurePoint::Capture,
        FailurePoint::Marker,
        FailurePoint::Walk,
        FailurePoint::OpenThread,
        FailurePoint::QueryThread,
        FailurePoint::Resume,
    ] {
        let dir = tempfile::tempdir().expect("fixture directory");
        let marker = dir.path().join("must-not-execute.txt");
        let retained = Arc::new(parking_lot::Mutex::new(None));
        BEFORE_ASSIGNMENT.with_borrow_mut(|hook| {
            let retained = retained.clone();
            *hook = Some(Box::new(move |child| {
                *retained.lock() = Some(retain_process(child));
                Ok(())
            }));
        });
        SNAPSHOTS.set((0, 0));
        MARKERS.set((0, 0));
        FAILURE.set(Some(point));
        let result = super::spawn(&mut marker_command(&marker));
        let error = result.err().expect("injected launch failure");
        assert_eq!(error.raw_os_error(), Some(ERROR_ACCESS_DENIED as i32));
        assert_exited(retained.lock().as_ref().expect("retained suspended root"));
        assert!(
            !marker.exists(),
            "failed lookup must never execute the root"
        );
        let snapshots = u32::from(point != FailurePoint::Capture);
        let markers = u32::from(point != FailurePoint::Capture && point != FailurePoint::Marker);
        assert_eq!(SNAPSHOTS.get(), (snapshots, snapshots));
        assert_eq!(MARKERS.get(), (markers, markers));
    }
}

#[tokio::test]
async fn pre_assignment_failure_terminates_suspended_root() {
    let _reset = ResetHooks;
    let dir = tempfile::tempdir().expect("fixture directory");
    let marker = dir.path().join("must-not-execute.txt");
    let retained = Arc::new(parking_lot::Mutex::new(None));
    BEFORE_ASSIGNMENT.with_borrow_mut(|hook| {
        let retained = retained.clone();
        *hook = Some(Box::new(move |child| {
            *retained.lock() = Some(retain_process(child));
            Err(io::Error::from_raw_os_error(ERROR_ACCESS_DENIED as i32))
        }));
    });
    let error = super::spawn(&mut marker_command(&marker))
        .err()
        .expect("assignment failure");
    assert_eq!(error.raw_os_error(), Some(ERROR_ACCESS_DENIED as i32));
    assert_exited(retained.lock().as_ref().expect("retained root"));
    assert!(!marker.exists());
}

#[test]
fn pss_status_uses_returned_win32_error() {
    assert!(check_status(0).is_ok());
    assert_eq!(
        check_status(ERROR_ACCESS_DENIED)
            .expect_err("direct status error")
            .raw_os_error(),
        Some(ERROR_ACCESS_DENIED as i32)
    );
}

#[test]
fn invalid_process_capture_returns_error_without_resources() {
    let _reset = ResetHooks;
    SNAPSHOTS.set((0, 0));
    MARKERS.set((0, 0));
    assert!(resume_initial_thread(ptr::null_mut(), 1).is_err());
    assert_eq!(SNAPSHOTS.get(), (0, 0));
    assert_eq!(MARKERS.get(), (0, 0));
}

#[test]
fn foreign_snapshot_entries_cannot_be_resumed() {
    let _reset = ResetHooks;
    SNAPSHOTS.set((0, 0));
    MARKERS.set((0, 0));
    // SAFETY: the pseudo handle references the current live process.
    let error = resume_initial_thread(unsafe { GetCurrentProcess() }, u32::MAX)
        .expect_err("no matching owned thread");
    assert_eq!(error.kind(), io::ErrorKind::NotFound);
    assert_eq!(SNAPSHOTS.get(), (1, 1));
    assert_eq!(MARKERS.get(), (1, 1));
}

#[test]
fn opened_foreign_thread_is_rejected_before_resume() {
    let _reset = ResetHooks;
    // SAFETY: the thread ID identifies this live test thread.
    let handle = unsafe {
        OpenThread(
            THREAD_QUERY_LIMITED_INFORMATION | THREAD_SUSPEND_RESUME,
            0,
            GetCurrentThreadId(),
        )
    };
    assert!(!handle.is_null(), "{}", io::Error::last_os_error());
    // SAFETY: OpenThread returned a new uniquely owned handle.
    let thread = unsafe { OwnedHandle::from_raw_handle(handle) };
    let error = resume_owned_thread(thread.as_raw_handle().cast(), unsafe {
        GetCurrentProcessId().wrapping_add(1)
    })
    .expect_err("opened handle belongs to a different process");
    assert_eq!(error.kind(), io::ErrorKind::PermissionDenied);
}

#[test]
fn invalid_thread_ownership_query_returns_error() {
    let _reset = ResetHooks;
    assert!(resume_owned_thread(ptr::null_mut(), 1).is_err());
}
