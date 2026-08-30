//! Cross-platform ownership of the CLI process tree.
//!
//! [`Client`](crate::Client) spawns the CLI as a direct child, and the CLI
//! may itself spawn descendants (MCP servers, shell tools, subagents).
//! Without containment, [`Client::stop`](crate::Client::stop),
//! [`Client::force_stop`](crate::Client::force_stop), and `Drop` can only
//! reach the root: killing it leaves any descendant it spawned running
//! and holding whatever it held (files, sockets, locks).
//!
//! [`ProcessTree`] gives those three teardown paths a single primitive that
//! reaches the whole tree instead of only the root: a dedicated process
//! group on Unix, a Job Object carrying `JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE`
//! on Windows.
//!
//! # What this does and does not guarantee
//!
//! Both primitives terminate the tree once [`ProcessTree::terminate`]
//! actually runs. They differ in what happens if it never runs — the SDK's
//! own process crashes, is killed, or loses power before any teardown code
//! executes:
//!
//! - **Windows**: `JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE` is enforced by the
//!   kernel when the last handle to the Job Object closes, including on an
//!   unclean exit of this process. The tree dies even if no Rust code runs.
//! - **Unix**: a process group has no equivalent kernel-enforced cleanup.
//!   [`ProcessTree::terminate`] must actually execute for the group to die;
//!   an unclean exit of this process leaves the group running, and a
//!   descendant that calls `setsid()` escapes the group entirely. This
//!   asymmetry is inherent to the two platforms' process models, not an
//!   implementation gap here.
//!
//! Attaching the Windows Job Object needs a small window after `spawn()` to
//! call `AssignProcessToJobObject` on the live process handle; a child that
//! spawns its own descendants faster than that call can still escape
//! containment. Closing that window requires creating the process suspended
//! and resuming its primary thread only after assignment, which trades this
//! narrow race for a more invasive spawn path. This module accepts the race
//! and assigns immediately after spawn instead.

use std::io;

use tokio::process::{Child, Command};
use tracing::warn;

/// Configure `command` so its eventual child is contained in a dedicated
/// process tree from the moment it is spawned.
///
/// Call before `command.spawn()`. Cheap and infallible on both platforms —
/// it only sets flags on the `Command`, it never touches a live process.
/// [`attach`] performs the (Windows-only) work that needs a live process
/// handle.
pub(crate) fn configure(command: &mut Command) {
    platform::configure(command);
}

/// Attach the platform containment primitive to a freshly spawned `child`.
///
/// Call immediately after `spawn()`, before the child has had a chance to
/// create descendants (see the module-level Windows caveat). Returns `None`
/// (after logging a warning) rather than an error when attachment fails on a
/// platform where failure is recoverable — a `Client` whose containment
/// setup failed should still start normally and fall back to root-only
/// teardown rather than turning a containment failure into a hard start
/// failure. Unix attachment cannot practically fail once `spawn()` has
/// already succeeded (the process group was established at fork, before
/// `exec`), so it never needs this fallback.
pub(crate) fn attach(child: &Child) -> Option<ProcessTree> {
    match platform::attach(child) {
        Ok(tree) => Some(ProcessTree(tree)),
        Err(error) => {
            warn!(
                pid = ?child.id(),
                %error,
                "failed to attach CLI process to a containment tree; \
                 falling back to root-process-only teardown"
            );
            None
        }
    }
}

/// Owns the platform primitive that contains one spawned root process and
/// its descendants.
pub(crate) struct ProcessTree(platform::Tree);

impl ProcessTree {
    /// Signal every process still alive in the tree to exit immediately.
    ///
    /// Idempotent: safe to call after the tree has already exited.
    pub(crate) fn terminate(&self) -> io::Result<()> {
        self.0.terminate()
    }

    /// `true` once no process remains in the tree.
    #[cfg(test)]
    pub(crate) fn is_empty(&self) -> io::Result<bool> {
        self.0.is_empty()
    }
}

/// `true` if a process with `pid` is still alive. Test-only introspection —
/// shared by this module's own tests and by the process-tree tests in
/// `lib.rs` so both check liveness the same way.
#[cfg(test)]
pub(crate) fn process_alive(pid: u32) -> bool {
    platform::process_alive(pid)
}

#[cfg(unix)]
mod platform {
    use std::io;

    use tokio::process::{Child, Command};

    pub(super) struct Tree {
        /// The dedicated process group id, equal to the root child's pid.
        pgid: i32,
    }

    pub(super) fn configure(command: &mut Command) {
        // Put the eventual child in its own new process group (pgid ==
        // its own pid), established by the kernel at fork, before `exec`
        // runs. Anything the child spawns inherits this group unless it
        // explicitly changes its own, so containment exists before we ever
        // get a chance to call `attach`.
        command.process_group(0);
    }

    pub(super) fn attach(child: &Child) -> io::Result<Tree> {
        let pid = child.id().ok_or_else(|| {
            io::Error::new(
                io::ErrorKind::NotFound,
                "CLI process exited before its process group could be recorded",
            )
        })?;
        Ok(Tree { pgid: pid as i32 })
    }

    impl Tree {
        pub(super) fn terminate(&self) -> io::Result<()> {
            // SAFETY: `killpg` only ever signals processes sharing
            // `self.pgid`; no pointers are involved.
            if unsafe { libc::killpg(self.pgid, libc::SIGKILL) } == 0 {
                return Ok(());
            }
            match io::Error::last_os_error() {
                // No process left in the group — already terminated.
                error if error.raw_os_error() == Some(libc::ESRCH) => Ok(()),
                error => Err(error),
            }
        }

        #[cfg(test)]
        pub(super) fn is_empty(&self) -> io::Result<bool> {
            // Signal 0 only probes for existence. Delivering a signal to a
            // process group requires no parent/child relationship, only
            // that at least one process in the group is still alive, so
            // this reads liveness without trying to reap anything — the
            // root child stays owned solely by Tokio's `Child`.
            // SAFETY: as above.
            if unsafe { libc::killpg(self.pgid, 0) } == 0 {
                return Ok(false);
            }
            match io::Error::last_os_error() {
                error if error.raw_os_error() == Some(libc::ESRCH) => Ok(true),
                error => Err(error),
            }
        }
    }

    #[cfg(test)]
    pub(super) fn process_alive(pid: u32) -> bool {
        // SAFETY: signal 0 only probes process existence.
        (unsafe { libc::kill(pid as i32, 0) }) == 0
    }
}

#[cfg(windows)]
mod platform {
    use std::{io, ptr};

    use tokio::process::{Child, Command};
    use windows_sys::Win32::Foundation::{CloseHandle, HANDLE};
    use windows_sys::Win32::System::JobObjects::{
        AssignProcessToJobObject, CreateJobObjectW, JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE,
        JOBOBJECT_EXTENDED_LIMIT_INFORMATION, JobObjectExtendedLimitInformation,
        SetInformationJobObject, TerminateJobObject,
    };
    #[cfg(test)]
    use windows_sys::Win32::System::JobObjects::{
        JOBOBJECT_BASIC_ACCOUNTING_INFORMATION, JobObjectBasicAccountingInformation,
        QueryInformationJobObject,
    };

    pub(super) fn configure(_command: &mut Command) {
        // Nothing to set on the `Command` itself; the Job Object is
        // created and assigned to the live process in `attach`, after
        // `spawn()` returns a process handle.
    }

    struct OwnedHandle(HANDLE);

    // SAFETY: Win32 handles may be used and closed from any thread.
    unsafe impl Send for OwnedHandle {}
    unsafe impl Sync for OwnedHandle {}

    impl Drop for OwnedHandle {
        fn drop(&mut self) {
            // SAFETY: `self.0` is a valid handle owned uniquely by this value.
            unsafe {
                CloseHandle(self.0);
            }
        }
    }

    pub(super) struct Tree {
        job: OwnedHandle,
    }

    pub(super) fn attach(child: &Child) -> io::Result<Tree> {
        // SAFETY: null attributes and name create a private,
        // non-inheritable Job Object owned solely by this process.
        let raw_job = unsafe { CreateJobObjectW(ptr::null(), ptr::null()) };
        if raw_job.is_null() {
            return Err(io::Error::last_os_error());
        }
        let job = OwnedHandle(raw_job);

        let mut limits = JOBOBJECT_EXTENDED_LIMIT_INFORMATION::default();
        limits.BasicLimitInformation.LimitFlags = JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE;
        // SAFETY: `job.0` is a live Job Object handle, and `limits` has the
        // exact layout `JobObjectExtendedLimitInformation` requires.
        if unsafe {
            SetInformationJobObject(
                job.0,
                JobObjectExtendedLimitInformation,
                ptr::from_ref(&limits).cast(),
                size_of::<JOBOBJECT_EXTENDED_LIMIT_INFORMATION>() as u32,
            )
        } == 0
        {
            return Err(io::Error::last_os_error());
        }

        // Assign as soon as we have a live process handle — see the
        // module-level doc for the residual assign-race this accepts.
        let process = child.raw_handle().ok_or_else(|| {
            io::Error::new(
                io::ErrorKind::NotFound,
                "CLI process exited before it could be assigned to a Job Object",
            )
        })?;
        // SAFETY: `job.0` is live, and `process` is the handle Tokio owns
        // for this still-running child.
        if unsafe { AssignProcessToJobObject(job.0, process.cast()) } == 0 {
            return Err(io::Error::last_os_error());
        }

        Ok(Tree { job })
    }

    impl Tree {
        pub(super) fn terminate(&self) -> io::Result<()> {
            // SAFETY: `self.job` is a live Job Object handle owned by this value.
            if unsafe { TerminateJobObject(self.job.0, 1) } != 0 {
                Ok(())
            } else {
                Err(io::Error::last_os_error())
            }
        }

        #[cfg(test)]
        pub(super) fn is_empty(&self) -> io::Result<bool> {
            let mut accounting = JOBOBJECT_BASIC_ACCOUNTING_INFORMATION::default();
            // SAFETY: `accounting` has the exact layout the query requires.
            if unsafe {
                QueryInformationJobObject(
                    self.job.0,
                    JobObjectBasicAccountingInformation,
                    ptr::from_mut(&mut accounting).cast(),
                    size_of::<JOBOBJECT_BASIC_ACCOUNTING_INFORMATION>() as u32,
                    ptr::null_mut(),
                )
            } == 0
            {
                return Err(io::Error::last_os_error());
            }
            Ok(accounting.ActiveProcesses == 0)
        }
    }

    #[cfg(test)]
    pub(super) fn process_alive(pid: u32) -> bool {
        use windows_sys::Win32::Foundation::WAIT_TIMEOUT;
        use windows_sys::Win32::System::Threading::{
            OpenProcess, PROCESS_SYNCHRONIZE, WaitForSingleObject,
        };

        // SAFETY: the process handle is closed before returning.
        unsafe {
            let process = OpenProcess(PROCESS_SYNCHRONIZE, 0, pid);
            if process.is_null() {
                return false;
            }
            let exists = WaitForSingleObject(process, 0) == WAIT_TIMEOUT;
            CloseHandle(process);
            exists
        }
    }
}

#[cfg(test)]
mod tests {
    use std::path::Path;
    use std::time::Duration;

    use tempfile::tempdir;

    use super::*;

    const TEST_TIMEOUT: Duration = Duration::from_secs(15);

    /// Not a real test — a re-exec entry point. The containment tests spawn
    /// this same compiled test binary as a subprocess, filtered by exact
    /// name to run only this "test", so its own OS process becomes the
    /// descendant the tests kill. It reads its lock/ready paths from
    /// environment variables (set on the child before spawn) rather than
    /// CLI args, since the harness controls what CLI args a filtered run
    /// receives. Run normally (no env vars set), it is a harmless no-op.
    #[test]
    fn lock_holder_helper_entrypoint() {
        let Ok(lock_path) = std::env::var("PROCESS_TREE_TEST_LOCK_PATH") else {
            return;
        };
        let ready_path = std::env::var("PROCESS_TREE_TEST_READY_PATH").expect("ready path env var");

        let _lock = acquire_exclusive_lock(Path::new(&lock_path)).expect("acquire exclusive lock");
        std::fs::write(&ready_path, std::process::id().to_string()).expect("write ready file");

        // The containment tests kill this process (root-only, or the whole
        // tree); there is no voluntary shutdown path that reaches past here.
        std::thread::sleep(Duration::from_secs(120));
    }

    #[cfg(unix)]
    fn acquire_exclusive_lock(path: &Path) -> std::io::Result<std::fs::File> {
        use std::os::fd::AsRawFd;

        let file = std::fs::OpenOptions::new()
            .create(true)
            .truncate(false)
            .write(true)
            .open(path)?;
        // SAFETY: `file` owns a valid fd for the duration of this call.
        if unsafe { libc::flock(file.as_raw_fd(), libc::LOCK_EX) } != 0 {
            return Err(std::io::Error::last_os_error());
        }
        Ok(file)
    }

    #[cfg(windows)]
    fn acquire_exclusive_lock(path: &Path) -> std::io::Result<std::fs::File> {
        use std::os::windows::fs::OpenOptionsExt;

        // `share_mode(0)` denies every other open on this path — read,
        // write, and delete — until this handle closes. Windows closes it
        // automatically, and only then, when the process exits by any
        // means, so the lock is always released exactly when the process
        // is actually gone.
        std::fs::OpenOptions::new()
            .create(true)
            .truncate(false)
            .write(true)
            .share_mode(0)
            .open(path)
    }

    /// Root command that waits for `start_path` to appear, then re-execs
    /// this test binary's [`lock_holder_helper_entrypoint`] as a descendant
    /// and blocks so the root itself stays alive until killed.
    ///
    /// The start-file wait exists only so tests can call [`attach`] before
    /// the descendant is created — deterministically avoiding the
    /// assign-race the module docs describe, which real callers accept but
    /// a test must not be flaky about.
    fn root_with_lock_holding_descendant(
        temp: &Path,
        lock_path: &Path,
        descendant_ready: &Path,
        start_path: &Path,
    ) -> Command {
        let this_test_binary = std::env::current_exe().expect("locate current test binary");
        // libtest's `--exact` filter matches the fully qualified test path,
        // not the bare function name.
        const HELPER_FILTER: &str = "process_tree::tests::lock_holder_helper_entrypoint";
        #[cfg(unix)]
        let mut command = {
            let mut command = Command::new("sh");
            command.args([
                "-c",
                "while [ ! -f \"$START\" ]; do sleep 0.05; done; \
                 \"$HELPER_BIN\" \"$HELPER_FILTER\" --exact --nocapture & wait",
            ]);
            command
        };
        #[cfg(windows)]
        let mut command = {
            let mut command = Command::new("powershell.exe");
            command.args([
                "-NoLogo",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                "while (-not (Test-Path $env:START)) { Start-Sleep -Milliseconds 50 }; \
                 Start-Process -FilePath $env:HELPER_BIN -ArgumentList @( \
                 $env:HELPER_FILTER,'--exact','--nocapture') -NoNewWindow -Wait",
            ]);
            command
        };
        configure(&mut command);
        command
            .env("HELPER_FILTER", HELPER_FILTER)
            .current_dir(temp)
            .env("HELPER_BIN", &this_test_binary)
            .env("PROCESS_TREE_TEST_LOCK_PATH", lock_path)
            .env("PROCESS_TREE_TEST_READY_PATH", descendant_ready)
            .env("START", start_path)
            // The re-exec'd descendant inherits stdio by default; nulling it
            // here keeps this test's own output pipe from staying open for
            // as long as that descendant (and, transitively, its own nested
            // test harness output) is alive.
            .stdout(std::process::Stdio::null())
            .stderr(std::process::Stdio::null());
        command
    }

    async fn wait_for_file(path: &Path) {
        let deadline = tokio::time::Instant::now() + TEST_TIMEOUT;
        while !path.exists() {
            assert!(
                tokio::time::Instant::now() < deadline,
                "expected file was never created: {}",
                path.display()
            );
            tokio::time::sleep(Duration::from_millis(10)).await;
        }
    }

    async fn wait_until(mut predicate: impl FnMut() -> bool, message: &str) {
        let deadline = tokio::time::Instant::now() + TEST_TIMEOUT;
        while !predicate() {
            assert!(tokio::time::Instant::now() < deadline, "{message}");
            tokio::time::sleep(Duration::from_millis(10)).await;
        }
    }

    /// Attempts to acquire the same lock the descendant holds. Succeeds
    /// only once the descendant process (or whatever kernel action ended
    /// it) has released it — proving the resource, not merely the pid, is
    /// gone.
    fn lock_is_free(path: &Path) -> bool {
        #[cfg(unix)]
        {
            use std::os::fd::AsRawFd;

            let Ok(file) = std::fs::OpenOptions::new().write(true).open(path) else {
                return false;
            };
            // SAFETY: `file` owns a valid fd for the duration of this call.
            let acquired =
                unsafe { libc::flock(file.as_raw_fd(), libc::LOCK_EX | libc::LOCK_NB) } == 0;
            if acquired {
                // SAFETY: `file` still owns the fd we just locked.
                unsafe {
                    libc::flock(file.as_raw_fd(), libc::LOCK_UN);
                }
            }
            acquired
        }
        #[cfg(windows)]
        {
            use std::os::windows::fs::OpenOptionsExt;

            std::fs::OpenOptions::new()
                .write(true)
                .share_mode(0)
                .open(path)
                .is_ok()
        }
    }

    #[tokio::test]
    async fn terminate_kills_descendant_and_releases_its_lock() {
        let temp = tempdir().expect("create temp directory");
        let lock_path = temp.path().join("resource.lock");
        let descendant_ready = temp.path().join("descendant-ready");
        let start_path = temp.path().join("start");

        let mut command = root_with_lock_holding_descendant(
            temp.path(),
            &lock_path,
            &descendant_ready,
            &start_path,
        );
        let mut root = command.spawn().expect("spawn root process");
        let root_pid = root.id().expect("root pid");
        let tree = attach(&root).expect("attach process tree");
        std::fs::write(&start_path, b"go").expect("signal root to spawn its descendant");

        wait_for_file(&descendant_ready).await;
        let descendant_pid: u32 = std::fs::read_to_string(&descendant_ready)
            .expect("read descendant pid")
            .trim()
            .parse()
            .expect("parse descendant pid");

        assert!(process_alive(root_pid), "root must be alive before stop");
        assert!(
            process_alive(descendant_pid),
            "descendant must be alive before stop"
        );
        assert!(
            !lock_is_free(&lock_path),
            "descendant must be holding its lock before stop"
        );

        tree.terminate().expect("terminate process tree");
        tokio::time::timeout(TEST_TIMEOUT, root.wait())
            .await
            .expect("root did not exit within the timeout after process-tree termination")
            .expect("reap root process");

        wait_until(
            || !process_alive(descendant_pid),
            "descendant survived process-tree termination",
        )
        .await;
        assert!(
            !process_alive(root_pid),
            "root survived its own termination"
        );
        wait_until(
            || lock_is_free(&lock_path),
            "descendant's lock was never released after process-tree termination",
        )
        .await;
        assert!(
            tree.is_empty().expect("query tree emptiness"),
            "tree must report empty once every process in it has exited"
        );
    }

    #[tokio::test]
    async fn is_empty_is_false_while_descendant_holds_the_tree_open() {
        let temp = tempdir().expect("create temp directory");
        let lock_path = temp.path().join("resource.lock");
        let descendant_ready = temp.path().join("descendant-ready");
        let start_path = temp.path().join("start");

        let mut command = root_with_lock_holding_descendant(
            temp.path(),
            &lock_path,
            &descendant_ready,
            &start_path,
        );
        let mut root = command.spawn().expect("spawn root process");
        let tree = attach(&root).expect("attach process tree");
        std::fs::write(&start_path, b"go").expect("signal root to spawn its descendant");

        wait_for_file(&descendant_ready).await;
        assert!(
            !tree.is_empty().expect("query tree emptiness"),
            "tree must report non-empty while the descendant is still alive"
        );

        tree.terminate().expect("terminate process tree");
        tokio::time::timeout(TEST_TIMEOUT, root.wait())
            .await
            .expect("root did not exit within the timeout after process-tree termination")
            .expect("reap root process");
    }
}
