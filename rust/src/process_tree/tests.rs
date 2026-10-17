#![cfg(test)]

use std::marker::PhantomData;
use std::os::windows::io::{AsRawHandle, FromRawHandle, OwnedHandle};
use std::os::windows::process::CommandExt;
use std::path::{Path, PathBuf};
use std::rc::Rc;
use std::sync::Arc;
use std::time::{Duration, Instant};
use std::{io, ptr};

use tokio::process::{Child, Command};
use windows_sys::Win32::Foundation::{
    CompareObjectHandles, DUPLICATE_SAME_ACCESS, DuplicateHandle, ERROR_ACCESS_DENIED,
    ERROR_INVALID_PARAMETER, ERROR_NO_TOKEN, HANDLE,
};
use windows_sys::Win32::Security::{
    ACL, ACL_REVISION, CreateRestrictedToken, DACL_SECURITY_INFORMATION, DISABLE_MAX_PRIVILEGE,
    ImpersonateLoggedOnUser, InitializeAcl, InitializeSecurityDescriptor, SECURITY_DESCRIPTOR,
    SetKernelObjectSecurity, SetSecurityDescriptorDacl, TOKEN_DUPLICATE, TOKEN_IMPERSONATE,
    TOKEN_QUERY,
};
use windows_sys::Win32::System::Diagnostics::ToolHelp::{
    CreateToolhelp32Snapshot, TH32CS_SNAPTHREAD, THREADENTRY32, Thread32First, Thread32Next,
};
use windows_sys::Win32::System::JobObjects::{
    IsProcessInJob, JOB_OBJECT_LIMIT_BREAKAWAY_OK, JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE,
    JOB_OBJECT_LIMIT_SILENT_BREAKAWAY_OK, JOBOBJECT_EXTENDED_LIMIT_INFORMATION,
    JobObjectExtendedLimitInformation, QueryInformationJobObject,
};
use windows_sys::Win32::System::Threading::{
    GetCurrentProcess, GetCurrentProcessId, GetCurrentThread, GetCurrentThreadId, OpenProcess,
    OpenProcessToken, OpenThread, OpenThreadToken, PROCESS_SYNCHRONIZE, SetThreadToken,
    THREAD_ALL_ACCESS, THREAD_QUERY_LIMITED_INFORMATION, THREAD_SUSPEND_RESUME, TerminateProcess,
    WaitForSingleObject,
};

use super::platform::{check_status, resume_initial_thread, resume_owned_thread};

#[derive(Clone, Copy, Debug, PartialEq)]
pub(super) enum FailurePoint {
    Capture,
    Marker,
    Walk,
    Toolhelp,
    OpenThread,
    QueryThread,
    Resume,
    Kill,
    CompatibilityAssignment,
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
    static SECOND_FAILURE: std::cell::Cell<Option<FailurePoint>> =
        const { std::cell::Cell::new(None) };
    static FAILURE_CODE: std::cell::Cell<u32> =
        const { std::cell::Cell::new(ERROR_ACCESS_DENIED) };
    pub(super) static SNAPSHOTS: std::cell::Cell<(u32, u32)> =
        const { std::cell::Cell::new((0, 0)) };
    pub(super) static MARKERS: std::cell::Cell<(u32, u32)> =
        const { std::cell::Cell::new((0, 0)) };
}

pub(super) fn fail_at(point: FailurePoint) -> io::Result<()> {
    if FAILURE.get() == Some(point) || SECOND_FAILURE.get() == Some(point) {
        Err(io::Error::from_raw_os_error(FAILURE_CODE.get() as i32))
    } else {
        Ok(())
    }
}

struct ResetHooks;

impl Drop for ResetHooks {
    fn drop(&mut self) {
        FAILURE.set(None);
        SECOND_FAILURE.set(None);
        FAILURE_CODE.set(ERROR_ACCESS_DENIED);
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

fn current_thread_token() -> Option<OwnedHandle> {
    let mut token = ptr::null_mut();
    // SAFETY: only the calling thread's token is queried; the returned handle is owned.
    if unsafe {
        OpenThreadToken(
            GetCurrentThread(),
            TOKEN_DUPLICATE | TOKEN_IMPERSONATE | TOKEN_QUERY,
            1,
            &mut token,
        )
    } != 0
    {
        // SAFETY: OpenThreadToken returned a uniquely owned token handle.
        Some(unsafe { OwnedHandle::from_raw_handle(token) })
    } else {
        assert_eq!(
            io::Error::last_os_error().raw_os_error(),
            Some(ERROR_NO_TOKEN as i32)
        );
        None
    }
}

struct RestrictedThreadToken {
    previous: Option<OwnedHandle>,
    _thread_bound: PhantomData<Rc<()>>,
}

impl RestrictedThreadToken {
    fn new() -> Self {
        let previous = current_thread_token();
        let process_token = if previous.is_none() {
            let mut token = ptr::null_mut();
            // SAFETY: obtain the current process token without modifying it.
            assert_ne!(
                unsafe {
                    OpenProcessToken(
                        GetCurrentProcess(),
                        TOKEN_DUPLICATE | TOKEN_QUERY,
                        &mut token,
                    )
                },
                0,
                "{}",
                io::Error::last_os_error()
            );
            // SAFETY: OpenProcessToken returned an owned handle.
            Some(unsafe { OwnedHandle::from_raw_handle(token) })
        } else {
            None
        };
        let source = previous.as_ref().or(process_token.as_ref()).unwrap();
        let mut restricted = ptr::null_mut();
        // SAFETY: the source remains live; privileges are disabled only on a new token.
        assert_ne!(
            unsafe {
                CreateRestrictedToken(
                    source.as_raw_handle(),
                    DISABLE_MAX_PRIVILEGE,
                    0,
                    ptr::null(),
                    0,
                    ptr::null(),
                    0,
                    ptr::null(),
                    &mut restricted,
                )
            },
            0,
            "{}",
            io::Error::last_os_error()
        );
        // SAFETY: CreateRestrictedToken returned an owned handle.
        let restricted = unsafe { OwnedHandle::from_raw_handle(restricted) };
        // SeDebugPrivilege can bypass the fixture's empty DACL on elevated runners.
        // SAFETY: impersonation affects only this calling thread, restored by the guard.
        assert_ne!(
            unsafe { ImpersonateLoggedOnUser(restricted.as_raw_handle()) },
            0,
            "{}",
            io::Error::last_os_error()
        );
        Self {
            previous,
            _thread_bound: PhantomData,
        }
    }
}

impl Drop for RestrictedThreadToken {
    fn drop(&mut self) {
        let previous = self
            .previous
            .as_ref()
            .map_or(ptr::null_mut(), |token| token.as_raw_handle());
        // SAFETY: this !Send guard retains and restores the exact prior thread token.
        assert_ne!(
            unsafe { SetThreadToken(ptr::null(), previous) },
            0,
            "restore test thread token: {}",
            io::Error::last_os_error()
        );
    }
}

fn deny_thread_reopening(pid: u32) {
    // SAFETY: snapshot is read-only and the resulting handle is uniquely owned.
    let raw = unsafe { CreateToolhelp32Snapshot(TH32CS_SNAPTHREAD, 0) };
    assert_ne!(
        raw,
        windows_sys::Win32::Foundation::INVALID_HANDLE_VALUE,
        "{}",
        io::Error::last_os_error()
    );
    let snapshot = unsafe { OwnedHandle::from_raw_handle(raw) };
    let mut entry = THREADENTRY32 {
        dwSize: std::mem::size_of::<THREADENTRY32>() as u32,
        ..Default::default()
    };
    // SAFETY: entry has the required size; snapshot remains live.
    assert_ne!(
        unsafe { Thread32First(snapshot.as_raw_handle(), &mut entry) },
        0
    );
    while entry.th32OwnerProcessID != pid {
        assert_ne!(
            unsafe { Thread32Next(snapshot.as_raw_handle(), &mut entry) },
            0,
            "owned thread not found"
        );
    }
    // SAFETY: only the owned suspended fixture's thread is opened and modified.
    let raw = unsafe { OpenThread(THREAD_ALL_ACCESS, 0, entry.th32ThreadID) };
    assert!(!raw.is_null(), "{}", io::Error::last_os_error());
    let thread = unsafe { OwnedHandle::from_raw_handle(raw) };
    let mut descriptor = SECURITY_DESCRIPTOR::default();
    let descriptor = ptr::from_mut(&mut descriptor).cast();
    let mut acl = ACL::default();
    // SAFETY: aligned descriptor/ACL stay live for all calls. The empty DACL
    // denies reopening but does not revoke the owned process termination handle.
    unsafe {
        assert_ne!(InitializeSecurityDescriptor(descriptor, 1), 0);
        assert_ne!(
            InitializeAcl(&mut acl, std::mem::size_of::<ACL>() as u32, ACL_REVISION),
            0
        );
        assert_ne!(SetSecurityDescriptorDacl(descriptor, 1, &acl, 0), 0);
        assert_ne!(
            SetKernelObjectSecurity(
                thread.as_raw_handle(),
                DACL_SECURITY_INFORMATION,
                descriptor
            ),
            0
        );
    }
    // SAFETY: exercise the exact production access mask on the DACL-restricted thread.
    let reopened = unsafe {
        OpenThread(
            THREAD_QUERY_LIMITED_INFORMATION | THREAD_SUSPEND_RESUME,
            0,
            entry.th32ThreadID,
        )
    };
    if !reopened.is_null() {
        drop(unsafe { OwnedHandle::from_raw_handle(reopened) });
        panic!("native thread DACL did not deny reopening");
    }
    assert_eq!(
        io::Error::last_os_error().raw_os_error(),
        Some(ERROR_ACCESS_DENIED as i32)
    );
}

#[test]
fn restricted_token_restores_exact_previous_impersonation() {
    let original = current_thread_token();
    {
        let _outer = RestrictedThreadToken::new();
        let outer_token = current_thread_token().expect("outer impersonation");
        {
            let _inner = RestrictedThreadToken::new();
            assert!(current_thread_token().is_some());
        }
        let restored = current_thread_token().expect("restored outer token");
        // SAFETY: both independently retained token handles are live.
        assert_ne!(
            unsafe { CompareObjectHandles(outer_token.as_raw_handle(), restored.as_raw_handle()) },
            0
        );
    }
    match (original, current_thread_token()) {
        (None, None) => {}
        (Some(original), Some(restored)) => {
            assert_ne!(
                unsafe { CompareObjectHandles(original.as_raw_handle(), restored.as_raw_handle()) },
                0
            );
        }
        _ => panic!("changed whether the calling thread is impersonating"),
    }
}

#[tokio::test]
async fn pss_lookup_failures_recover_through_toolhelp() {
    let _reset = ResetHooks;
    for point in [
        FailurePoint::Capture,
        FailurePoint::Marker,
        FailurePoint::Walk,
    ] {
        FAILURE.set(Some(point));
        SNAPSHOTS.set((0, 0));
        MARKERS.set((0, 0));
        let mut command = command();
        command.args(["exit", "/b", "23"]);
        let (mut child, tree) =
            super::spawn(&mut command).expect("Toolhelp must recover PSS failure");
        let status = tokio::time::timeout(Duration::from_secs(5), child.wait())
            .await
            .expect("recovered child must execute")
            .expect("reap child");
        assert_eq!(status.code(), Some(23));
        assert!(tree.is_some());
        drop(tree);
        assert_eq!(SNAPSHOTS.get().0, SNAPSHOTS.get().1);
        assert_eq!(MARKERS.get().0, MARKERS.get().1);
    }
}

#[tokio::test]
async fn native_thread_access_denial_recovers_usable_child_without_double_execution() {
    use tokio::io::AsyncWriteExt;
    let _reset = ResetHooks;
    let dir = tempfile::tempdir().expect("fixture directory");
    let marker = dir.path().join("executions.txt");
    let original = Arc::new(parking_lot::Mutex::new(None));
    let private_job = Rc::new(std::cell::RefCell::new(None));
    BEFORE_ASSIGNMENT.with_borrow_mut(|hook| {
        let original = original.clone();
        *hook = Some(Box::new(move |child| {
            *original.lock() = Some(retain_process(child));
            deny_thread_reopening(child.id().unwrap());
            Ok(())
        }));
    });
    BEFORE_RESUME.with_borrow_mut(|hook| {
        let private_job = private_job.clone();
        *hook = Some(Box::new(move |_, job| {
            *private_job.borrow_mut() = Some(job);
            Ok(())
        }));
    });
    let mut command = Command::new(command().as_std().get_program());
    command.args(["/d", "/v:on", "/c"]);
    command.as_std_mut().raw_arg(format!(
        "echo executed>>\"{}\" & set /p input= & echo !input! & echo !SDK_RECOVERY_VALUE! & cd & echo recovery stderr 1>&2 & exit /b 23",
        marker.display()
    ));
    command
        .current_dir(dir.path())
        .env("SDK_RECOVERY_VALUE", "value with spaces")
        .stdin(std::process::Stdio::piped())
        .stdout(std::process::Stdio::piped())
        .stderr(std::process::Stdio::piped())
        .kill_on_drop(true);
    let (mut child, tree) = {
        let _restricted = RestrictedThreadToken::new();
        super::spawn(&mut command).expect("recover actual native OpenThread denial")
    };
    assert_exited(original.lock().as_ref().expect("retained original"));
    assert!(tree.is_some(), "replacement retains private Job ownership");
    let mut assigned = 0;
    // SAFETY: the returned tree retains this exact per-client Job, and stdin
    // keeps the replacement alive until after its ownership is checked.
    assert_ne!(
        unsafe {
            IsProcessInJob(
                child.raw_handle().unwrap().cast(),
                private_job.borrow().expect("original private Job"),
                &mut assigned,
            )
        },
        0,
        "{}",
        io::Error::last_os_error()
    );
    assert_ne!(assigned, 0, "replacement must join its own client's Job");
    let mut stdin = child.stdin.take().expect("replacement stdin");
    stdin
        .write_all(b"recovery input\r\n")
        .await
        .expect("write replacement stdin");
    drop(stdin);
    let output = tokio::time::timeout(Duration::from_secs(5), child.wait_with_output())
        .await
        .expect("recovery must execute")
        .expect("reap replacement");
    assert_eq!(output.status.code(), Some(23));
    let stdout = String::from_utf8(output.stdout).unwrap();
    assert_eq!(
        stdout.lines().map(str::trim).collect::<Vec<_>>(),
        [
            "recovery input",
            "value with spaces",
            dir.path().to_str().unwrap()
        ]
    );
    assert_eq!(
        String::from_utf8(output.stderr).unwrap().trim(),
        "recovery stderr"
    );
    assert_eq!(
        std::fs::read_to_string(marker)
            .unwrap()
            .lines()
            .map(str::trim)
            .collect::<Vec<_>>(),
        ["executed"]
    );
    drop(tree);
}

#[tokio::test]
async fn termination_failure_does_not_retry_denied_thread() {
    let _reset = ResetHooks;
    let dir = tempfile::tempdir().unwrap();
    let marker = dir.path().join("must-not-execute");
    let original = Arc::new(parking_lot::Mutex::new(None));
    BEFORE_ASSIGNMENT.with_borrow_mut(|hook| {
        let original = original.clone();
        *hook = Some(Box::new(move |child| {
            *original.lock() = Some(retain_process(child));
            deny_thread_reopening(child.id().unwrap());
            FAILURE.set(Some(FailurePoint::Kill));
            Ok(())
        }));
    });
    let error = {
        let _restricted = RestrictedThreadToken::new();
        super::spawn(&mut marker_command(&marker))
            .err()
            .expect("failed termination must block retry")
    };
    assert!(
        error.to_string().starts_with("OpenThread failed:"),
        "{error}"
    );
    // The retained Job remains armed on the error path and cleans up the root.
    assert_exited(original.lock().as_ref().unwrap());
    assert!(!marker.exists(), "neither original nor retry may execute");
}

#[tokio::test]
async fn compatibility_assignment_failure_retains_usable_direct_child() {
    use tokio::io::AsyncWriteExt;
    let _reset = ResetHooks;
    let original = Arc::new(parking_lot::Mutex::new(None));
    BEFORE_ASSIGNMENT.with_borrow_mut(|hook| {
        let original = original.clone();
        *hook = Some(Box::new(move |child| {
            *original.lock() = Some(retain_process(child));
            deny_thread_reopening(child.id().unwrap());
            FAILURE.set(Some(FailurePoint::CompatibilityAssignment));
            Ok(())
        }));
    });
    let mut command = command();
    command.as_std_mut().raw_arg("set /p input= & exit /b 23");
    command.stdin(std::process::Stdio::piped());
    let (mut child, tree) = {
        let _restricted = RestrictedThreadToken::new();
        super::spawn(&mut command).expect("best-effort assignment must not block recovery")
    };
    assert!(
        tree.is_none(),
        "failed assignment must not claim tree ownership"
    );
    assert_exited(original.lock().as_ref().unwrap());
    let mut stdin = child.stdin.take().unwrap();
    stdin.write_all(b"ready\r\n").await.unwrap();
    drop(stdin);
    let status = tokio::time::timeout(Duration::from_secs(5), child.wait())
        .await
        .expect("replacement runs")
        .expect("reap replacement");
    assert_eq!(status.code(), Some(23));
}

#[tokio::test]
async fn wrong_owner_at_spawn_is_fatal_without_retry() {
    let _reset = ResetHooks;
    let dir = tempfile::tempdir().unwrap();
    let marker = dir.path().join("must-not-execute");
    let original = Arc::new(parking_lot::Mutex::new(None));
    BEFORE_RESUME.with_borrow_mut(|hook| {
        let original = original.clone();
        *hook = Some(Box::new(move |child, _| {
            *original.lock() = Some(retain_process(child));
            // SAFETY: this live test thread cannot belong to the owned child.
            let raw = unsafe {
                OpenThread(
                    THREAD_QUERY_LIMITED_INFORMATION | THREAD_SUSPEND_RESUME,
                    0,
                    GetCurrentThreadId(),
                )
            };
            assert!(!raw.is_null(), "{}", io::Error::last_os_error());
            let handle = unsafe { OwnedHandle::from_raw_handle(raw) };
            resume_owned_thread(handle.as_raw_handle(), child.id().unwrap())
        }));
    });
    let error = super::spawn(&mut marker_command(&marker))
        .err()
        .expect("wrong owner is fatal");
    assert_eq!(error.kind(), io::ErrorKind::PermissionDenied);
    assert!(error.to_string().contains("no longer belongs"));
    assert_exited(original.lock().as_ref().unwrap());
    assert!(!marker.exists());
}

#[tokio::test]
async fn failed_toolhelp_lookup_does_not_retry() {
    let _reset = ResetHooks;
    let dir = tempfile::tempdir().unwrap();
    let marker = dir.path().join("must-not-execute");
    let original = Arc::new(parking_lot::Mutex::new(None));
    BEFORE_ASSIGNMENT.with_borrow_mut(|hook| {
        let original = original.clone();
        *hook = Some(Box::new(move |child| {
            *original.lock() = Some(retain_process(child));
            Ok(())
        }));
    });
    FAILURE.set(Some(FailurePoint::Capture));
    SECOND_FAILURE.set(Some(FailurePoint::Toolhelp));
    let error = super::spawn(&mut marker_command(&marker))
        .err()
        .expect("failed lookup must be fatal");
    assert_eq!(error.raw_os_error(), Some(ERROR_ACCESS_DENIED as i32));
    assert_exited(original.lock().as_ref().unwrap());
    assert!(!marker.exists());
}

#[tokio::test]
async fn cleanup_accepts_access_denial_only_for_observed_process_exit() {
    let mut command = command();
    command.as_std_mut().creation_flags(
        windows_sys::Win32::System::Threading::CREATE_NO_WINDOW
            | windows_sys::Win32::System::Threading::CREATE_SUSPENDED,
    );
    command.args(["exit", "/b", "0"]);
    let mut child = command.spawn().unwrap();
    let retained = retain_process(&child);
    // SAFETY: request termination of only the owned, still-suspended root.
    assert_ne!(unsafe { TerminateProcess(retained.as_raw_handle(), 1) }, 0);
    assert_exited(&retained);
    super::platform::terminate_suspended(&mut child).expect("an exited root is safe to discard");
    child.wait().await.expect("reap original");
}

async fn recovered_client(dir: &Path, tcp: bool) -> crate::Client {
    use std::future::Future;
    let original = Arc::new(parking_lot::Mutex::new(None));
    BEFORE_ASSIGNMENT.with_borrow_mut(|hook| {
        let original = original.clone();
        *hook = Some(Box::new(move |child| {
            *original.lock() = Some(retain_process(child));
            deny_thread_reopening(child.id().unwrap());
            Ok(())
        }));
    });
    let script = PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .join("../test/harness/stdio-shutdown-runtime.cjs");
    let mut options = crate::ClientOptions::new()
        .with_program(crate::CliProgram::Path("node".into()))
        .with_prefix_args([
            script.into_os_string(),
            dir.join("cleanup.jsonl").into_os_string(),
            "force".into(),
            dir.join("child.pid").into_os_string(),
        ])
        .with_cwd(dir)
        .with_mode(crate::ClientMode::Empty)
        .with_base_directory(dir.join("state"))
        .with_use_logged_in_user(false)
        .with_transport(crate::Transport::Stdio);
    if tcp {
        options.prefix_args.push("--fixture-tcp".into());
        options.transport = crate::Transport::Tcp {
            port: 0,
            connection_token: None,
        };
    }
    let mut starting = std::pin::pin!(crate::Client::start(options));
    let client = tokio::time::timeout(
        Duration::from_secs(10),
        std::future::poll_fn(|cx| {
            // Restore on every Pending/Ready: never retain impersonation over an
            // async suspension, or change the process-wide token.
            let _restricted = RestrictedThreadToken::new();
            starting.as_mut().poll(cx)
        }),
    )
    .await
    .expect("bound fixture handshake")
    .expect("Client::start must recover");
    assert_exited(
        original
            .lock()
            .as_ref()
            .expect("original suspended fixture"),
    );
    client
}

fn retain_client_root(client: &crate::Client) -> OwnedHandle {
    // SAFETY: the connected fixture client still owns its live process.
    let raw = unsafe { OpenProcess(PROCESS_SYNCHRONIZE, 0, client.pid().unwrap()) };
    assert!(!raw.is_null(), "{}", io::Error::last_os_error());
    unsafe { OwnedHandle::from_raw_handle(raw) }
}

#[tokio::test]
async fn recovered_stdio_and_tcp_clients_preserve_handshake_and_private_job_isolation() {
    let _reset = ResetHooks;
    for tcp in [false, true] {
        let first_dir = tempfile::tempdir().unwrap();
        let first = recovered_client(first_dir.path(), tcp).await;
        let first_root = retain_client_root(&first);
        let retained = first.clone();
        let second_dir = tempfile::tempdir().unwrap();
        let second = recovered_client(second_dir.path(), tcp).await;
        let second_root = retain_client_root(&second);
        first
            .ping(Some("recovered-first"))
            .await
            .expect("first client is usable");
        drop(first);
        // SAFETY: both roots have independently retained handles.
        assert_eq!(
            unsafe { WaitForSingleObject(first_root.as_raw_handle(), 0) },
            258
        );
        drop(retained);
        assert_exited(&first_root);
        assert_eq!(
            unsafe { WaitForSingleObject(second_root.as_raw_handle(), 0) },
            258
        );
        second
            .ping(Some("recovered-second"))
            .await
            .expect("another client's Job is unaffected");
        second.force_stop();
        assert_exited(&second_root);
    }
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
async fn private_job_allows_only_explicit_breakaway() {
    let _reset = ResetHooks;
    BEFORE_RESUME.with_borrow_mut(|hook| {
        *hook = Some(Box::new(|_, job| {
            let mut limits = JOBOBJECT_EXTENDED_LIMIT_INFORMATION::default();
            // SAFETY: the Job remains owned and `limits` matches the requested information class.
            let queried = unsafe {
                QueryInformationJobObject(
                    job,
                    JobObjectExtendedLimitInformation,
                    ptr::from_mut(&mut limits).cast(),
                    std::mem::size_of::<JOBOBJECT_EXTENDED_LIMIT_INFORMATION>() as u32,
                    ptr::null_mut(),
                )
            };
            assert_ne!(queried, 0, "{}", io::Error::last_os_error());
            let flags = limits.BasicLimitInformation.LimitFlags;
            let required = JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE | JOB_OBJECT_LIMIT_BREAKAWAY_OK;
            assert_eq!(
                flags & required,
                required,
                "private Job must retain kill-on-close and permit explicit breakaway"
            );
            assert_eq!(
                flags & JOB_OBJECT_LIMIT_SILENT_BREAKAWAY_OK,
                0,
                "ordinary descendants must not silently escape the private Job"
            );
            Ok(())
        }));
    });
    let mut command = command();
    command.args(["exit", "/b", "0"]);
    let (mut child, tree) = super::spawn(&mut command).expect("spawn owned child");
    assert!(
        BEFORE_RESUME.with_borrow(Option::is_none),
        "job policy must be checked before resume"
    );
    let status = tokio::time::timeout(Duration::from_secs(5), child.wait())
        .await
        .expect("child must exit")
        .expect("reap child");
    drop(tree);
    assert!(status.success());
}

#[tokio::test]
async fn lookup_failures_reap_suspended_root_and_release_pss_resources() {
    let _reset = ResetHooks;
    for point in [
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
        FAILURE_CODE.set(if point == FailurePoint::OpenThread {
            ERROR_INVALID_PARAMETER
        } else {
            ERROR_ACCESS_DENIED
        });
        let result = super::spawn(&mut marker_command(&marker));
        let error = result.err().expect("injected launch failure");
        if point == FailurePoint::OpenThread {
            assert!(
                error.to_string().starts_with("OpenThread failed:"),
                "{error}"
            );
        } else {
            assert_eq!(error.raw_os_error(), Some(ERROR_ACCESS_DENIED as i32));
        }
        assert_exited(retained.lock().as_ref().expect("retained suspended root"));
        assert!(
            !marker.exists(),
            "failed lookup must never execute the root"
        );
        assert_eq!(SNAPSHOTS.get(), (1, 1));
        assert_eq!(MARKERS.get(), (1, 1));
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
