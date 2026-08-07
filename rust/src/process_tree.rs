//! Ownership and teardown for SDK-spawned process trees.

use std::io;
use std::process::ExitStatus;
use std::time::{Duration, Instant};

use tokio::process::{Child, Command};
use tracing::{error, warn};

const TREE_EXIT_POLL_INTERVAL: Duration = Duration::from_millis(10);
const SYNC_REAP_GRACE: Duration = Duration::from_millis(250);

/// Owns a direct child and the platform primitive that contains its descendants.
pub(crate) struct ManagedChild {
    child: Option<Child>,
    tree: Option<platform::ProcessTree>,
    tree_terminated: bool,
}

impl ManagedChild {
    /// Spawn a child into a process tree before it can create descendants.
    pub(crate) fn spawn(mut command: Command) -> io::Result<Self> {
        command.kill_on_drop(true);
        platform::configure_command(&mut command);

        let mut child = command.spawn()?;
        match platform::ProcessTree::attach_and_start(&mut child) {
            Ok(tree) => Ok(Self {
                child: Some(child),
                tree: Some(tree),
                tree_terminated: false,
            }),
            Err(error) => {
                reap_failed_spawn(&mut child);
                Err(error)
            }
        }
    }

    pub(crate) fn child_mut(&mut self) -> &mut Child {
        self.child.as_mut().expect("managed child is present")
    }

    pub(crate) fn id(&self) -> Option<u32> {
        self.child.as_ref().and_then(Child::id)
    }

    /// Terminate the complete tree. If the tree primitive fails, still signal
    /// the direct child so teardown never regresses to doing nothing.
    pub(crate) fn terminate(&mut self) -> io::Result<()> {
        if self.tree_terminated {
            return Ok(());
        }
        let result = self
            .tree
            .as_ref()
            .expect("managed process tree is present")
            .terminate();
        if result.is_ok() {
            self.tree_terminated = true;
        }
        if result.is_err()
            && let Some(child) = self.child.as_mut()
        {
            let _ = child.start_kill();
        }
        result
    }

    /// Wait for and reap the direct child through Tokio's sole child owner.
    pub(crate) async fn wait(&mut self) -> io::Result<ExitStatus> {
        self.child_mut().wait().await
    }

    /// Verify that no process remains in the platform tree.
    pub(crate) async fn wait_for_tree_exit(&mut self, timeout: Duration) -> io::Result<()> {
        let started = Instant::now();
        loop {
            let tree = self.tree.as_ref().expect("managed process tree is present");
            tree.reap_adopted()?;
            if tree.is_empty()? {
                self.tree.take();
                return Ok(());
            }
            if started.elapsed() >= timeout {
                return Err(io::Error::new(
                    io::ErrorKind::TimedOut,
                    "timed out waiting for CLI process tree to exit",
                ));
            }
            tokio::time::sleep(TREE_EXIT_POLL_INTERVAL).await;
        }
    }
}

impl Drop for ManagedChild {
    fn drop(&mut self) {
        let Some(mut child) = self.child.take() else {
            return;
        };
        let tree = self.tree.take();
        let pid = child.id();

        if !self.tree_terminated
            && let Some(tree) = tree.as_ref()
            && let Err(error) = tree.terminate()
        {
            warn!(pid = ?pid, %error, "failed to terminate CLI process tree on drop");
        }
        if let Err(error) = child.start_kill()
            && child.try_wait().ok().flatten().is_none()
        {
            warn!(pid = ?pid, %error, "failed to terminate direct CLI child on drop");
        }

        if reap_for(&mut child, SYNC_REAP_GRACE) {
            return;
        }

        let result = std::thread::Builder::new()
            .name("copilot-cli-reaper".to_string())
            .spawn(move || {
                loop {
                    match child.try_wait() {
                        Ok(Some(_)) => break,
                        Ok(None) => std::thread::sleep(TREE_EXIT_POLL_INTERVAL),
                        Err(error) => {
                            warn!(pid = ?pid, %error, "failed to reap CLI child");
                            break;
                        }
                    }
                }
                drop(tree);
            });
        if let Err(error) = result {
            error!(pid = ?pid, %error, "failed to start CLI child reaper thread");
        }
    }
}

fn reap_failed_spawn(child: &mut Child) {
    let pid = child.id();
    if let Err(error) = child.start_kill() {
        warn!(pid = ?pid, %error, "failed to terminate CLI after process-tree setup failure");
    }
    if !reap_for(child, SYNC_REAP_GRACE) {
        warn!(pid = ?pid, "CLI did not exit promptly after process-tree setup failure");
    }
}

fn reap_for(child: &mut Child, timeout: Duration) -> bool {
    let started = Instant::now();
    loop {
        match child.try_wait() {
            Ok(Some(_)) => return true,
            Ok(None) if started.elapsed() < timeout => {
                std::thread::sleep(TREE_EXIT_POLL_INTERVAL);
            }
            Ok(None) | Err(_) => return false,
        }
    }
}

#[cfg(test)]
pub(crate) fn active_tree_count() -> usize {
    platform::active_tree_count()
}

#[cfg(test)]
pub(crate) async fn wait_for_test_pid(path: &std::path::Path) -> u32 {
    let found = wait_for_test_condition(Duration::from_secs(10), || path.exists()).await;
    assert!(found, "grandchild pid file was not created");
    std::fs::read_to_string(path)
        .expect("read grandchild pid")
        .trim()
        .parse()
        .expect("parse grandchild pid")
}

#[cfg(test)]
pub(crate) async fn wait_for_test_condition(
    timeout: Duration,
    mut predicate: impl FnMut() -> bool,
) -> bool {
    let started = Instant::now();
    loop {
        if predicate() {
            return true;
        }
        if started.elapsed() >= timeout {
            return false;
        }
        tokio::time::sleep(TREE_EXIT_POLL_INTERVAL).await;
    }
}

#[cfg(all(test, unix))]
pub(crate) fn test_tree_command(pid_file: &std::path::Path) -> Command {
    let mut command = Command::new("sh");
    command
        .args(["-c", "sleep 60 & echo \"$!\" > \"$PID_FILE\"; wait"])
        .env("PID_FILE", pid_file)
        .stdin(std::process::Stdio::null())
        .stdout(std::process::Stdio::null())
        .stderr(std::process::Stdio::null());
    command
}

#[cfg(all(test, windows))]
pub(crate) fn test_tree_command(pid_file: &std::path::Path) -> Command {
    let script = concat!(
        "$child = Start-Process powershell.exe ",
        "-ArgumentList @('-NoLogo','-NoProfile','-NonInteractive','-Command',",
        "'Start-Sleep -Seconds 60') -PassThru; ",
        "Set-Content -LiteralPath $env:PID_FILE -Value $child.Id; ",
        "Wait-Process -Id $child.Id"
    );
    let mut command = Command::new("powershell.exe");
    command
        .args([
            "-NoLogo",
            "-NoProfile",
            "-NonInteractive",
            "-Command",
            script,
        ])
        .env("PID_FILE", pid_file)
        .stdin(std::process::Stdio::null())
        .stdout(std::process::Stdio::null())
        .stderr(std::process::Stdio::null());
    command
}

#[cfg(all(test, unix))]
pub(crate) fn test_process_exists(pid: u32) -> bool {
    // SAFETY: signal 0 only probes process existence.
    (unsafe { libc::kill(pid as i32, 0) }) == 0
}

#[cfg(all(test, windows))]
pub(crate) fn test_process_exists(pid: u32) -> bool {
    use windows_sys::Win32::Foundation::{CloseHandle, WAIT_TIMEOUT};
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

#[cfg(unix)]
mod platform {
    use std::io;
    #[cfg(test)]
    use std::sync::atomic::{AtomicUsize, Ordering};

    use tokio::process::{Child, Command};

    #[cfg(test)]
    static ACTIVE_TREES: AtomicUsize = AtomicUsize::new(0);

    pub(super) struct ProcessTree {
        pgid: i32,
    }

    impl ProcessTree {
        pub(super) fn attach_and_start(child: &mut Child) -> io::Result<Self> {
            let pid = child.id().ok_or_else(|| {
                io::Error::new(
                    io::ErrorKind::NotFound,
                    "CLI exited before process-group ownership was established",
                )
            })?;
            #[cfg(test)]
            ACTIVE_TREES.fetch_add(1, Ordering::Relaxed);
            Ok(Self { pgid: pid as i32 })
        }

        pub(super) fn terminate(&self) -> io::Result<()> {
            // SAFETY: `pgid` is the dedicated group created for this child.
            if unsafe { libc::killpg(self.pgid, libc::SIGKILL) } == 0 {
                return Ok(());
            }
            let error = io::Error::last_os_error();
            if error.raw_os_error() == Some(libc::ESRCH) {
                Ok(())
            } else {
                Err(error)
            }
        }

        pub(super) fn is_empty(&self) -> io::Result<bool> {
            // SAFETY: signal 0 only probes the dedicated process group.
            if unsafe { libc::killpg(self.pgid, 0) } == 0 {
                return Ok(false);
            }
            let error = io::Error::last_os_error();
            if error.raw_os_error() == Some(libc::ESRCH) {
                Ok(true)
            } else {
                Err(error)
            }
        }

        pub(super) fn reap_adopted(&self) -> io::Result<()> {
            loop {
                let mut status = 0;
                // SAFETY: a negative pid selects children in this dedicated
                // process group. WNOHANG keeps the async caller non-blocking.
                let result = unsafe { libc::waitpid(-self.pgid, &mut status, libc::WNOHANG) };
                if result > 0 {
                    continue;
                }
                if result == 0 {
                    return Ok(());
                }
                let error = io::Error::last_os_error();
                match error.raw_os_error() {
                    Some(libc::ECHILD) => return Ok(()),
                    Some(libc::EINTR) => continue,
                    _ => return Err(error),
                }
            }
        }
    }

    impl Drop for ProcessTree {
        fn drop(&mut self) {
            #[cfg(test)]
            ACTIVE_TREES.fetch_sub(1, Ordering::Relaxed);
        }
    }

    pub(super) fn configure_command(command: &mut Command) {
        command.process_group(0);
    }

    #[cfg(test)]
    pub(super) fn active_tree_count() -> usize {
        ACTIVE_TREES.load(Ordering::Relaxed)
    }
}

#[cfg(windows)]
mod platform {
    use std::io;
    use std::ptr;
    #[cfg(test)]
    use std::sync::atomic::{AtomicUsize, Ordering};

    use tokio::process::{Child, Command};
    use windows_sys::Win32::Foundation::{CloseHandle, HANDLE, INVALID_HANDLE_VALUE};
    use windows_sys::Win32::System::Diagnostics::ToolHelp::{
        CreateToolhelp32Snapshot, TH32CS_SNAPTHREAD, THREADENTRY32, Thread32First, Thread32Next,
    };
    use windows_sys::Win32::System::JobObjects::{
        AssignProcessToJobObject, CreateJobObjectW, JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE,
        JOBOBJECT_BASIC_ACCOUNTING_INFORMATION, JOBOBJECT_EXTENDED_LIMIT_INFORMATION,
        JobObjectBasicAccountingInformation, JobObjectExtendedLimitInformation,
        QueryInformationJobObject, SetInformationJobObject, TerminateJobObject,
    };
    use windows_sys::Win32::System::Threading::{
        CREATE_NO_WINDOW, CREATE_SUSPENDED, OpenThread, ResumeThread, THREAD_SUSPEND_RESUME,
    };

    #[cfg(test)]
    static ACTIVE_TREES: AtomicUsize = AtomicUsize::new(0);

    struct OwnedHandle(HANDLE);

    // SAFETY: Windows kernel handles can be used and closed from any thread.
    unsafe impl Send for OwnedHandle {}

    impl Drop for OwnedHandle {
        fn drop(&mut self) {
            // SAFETY: this type owns the valid handle and closes it exactly once.
            unsafe {
                CloseHandle(self.0);
            }
        }
    }

    pub(super) struct ProcessTree {
        job: OwnedHandle,
    }

    impl ProcessTree {
        pub(super) fn attach_and_start(child: &mut Child) -> io::Result<Self> {
            let raw_process = child.raw_handle().ok_or_else(|| {
                io::Error::new(
                    io::ErrorKind::NotFound,
                    "CLI exited before Job Object ownership was established",
                )
            })?;

            // SAFETY: null attributes and name create a private, non-inheritable Job Object.
            let raw_job = unsafe { CreateJobObjectW(ptr::null(), ptr::null()) };
            if raw_job.is_null() {
                return Err(io::Error::last_os_error());
            }
            let job = OwnedHandle(raw_job);

            let mut limits = JOBOBJECT_EXTENDED_LIMIT_INFORMATION::default();
            limits.BasicLimitInformation.LimitFlags = JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE;
            // SAFETY: `job` and `raw_process` are live handles, and `limits`
            // has the exact layout required by JobObjectExtendedLimitInformation.
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
            if unsafe { AssignProcessToJobObject(job.0, raw_process.cast()) } == 0 {
                return Err(io::Error::last_os_error());
            }

            resume_primary_thread(child.id().ok_or_else(|| {
                io::Error::new(
                    io::ErrorKind::NotFound,
                    "CLI exited before its primary thread could be resumed",
                )
            })?)?;

            #[cfg(test)]
            ACTIVE_TREES.fetch_add(1, Ordering::Relaxed);
            Ok(Self { job })
        }

        pub(super) fn terminate(&self) -> io::Result<()> {
            // SAFETY: `self.job` is a live Job Object handle owned by this guard.
            if unsafe { TerminateJobObject(self.job.0, 1) } != 0 {
                Ok(())
            } else {
                Err(io::Error::last_os_error())
            }
        }

        pub(super) fn is_empty(&self) -> io::Result<bool> {
            let mut accounting = JOBOBJECT_BASIC_ACCOUNTING_INFORMATION::default();
            // SAFETY: `accounting` has the exact layout requested by the query.
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

        pub(super) fn reap_adopted(&self) -> io::Result<()> {
            Ok(())
        }
    }

    impl Drop for ProcessTree {
        fn drop(&mut self) {
            #[cfg(test)]
            ACTIVE_TREES.fetch_sub(1, Ordering::Relaxed);
        }
    }

    pub(super) fn configure_command(command: &mut Command) {
        use std::os::windows::process::CommandExt;

        command
            .as_std_mut()
            .creation_flags(CREATE_NO_WINDOW | CREATE_SUSPENDED);
    }

    fn resume_primary_thread(pid: u32) -> io::Result<()> {
        // The child was created suspended and therefore still has exactly one
        // thread. Enumerating by owner PID recovers the primary thread handle
        // that `std::process::Command` closes after CreateProcessW returns.
        // SAFETY: the snapshot and thread handles are wrapped immediately.
        let raw_snapshot = unsafe { CreateToolhelp32Snapshot(TH32CS_SNAPTHREAD, 0) };
        if raw_snapshot == INVALID_HANDLE_VALUE {
            return Err(io::Error::last_os_error());
        }
        let snapshot = OwnedHandle(raw_snapshot);
        let mut entry = THREADENTRY32 {
            dwSize: size_of::<THREADENTRY32>() as u32,
            ..Default::default()
        };

        // SAFETY: `entry` has the required size and remains live for iteration.
        let mut has_entry = unsafe { Thread32First(snapshot.0, &mut entry) } != 0;
        while has_entry {
            if entry.th32OwnerProcessID == pid {
                // SAFETY: the thread id came from the live system snapshot.
                let raw_thread =
                    unsafe { OpenThread(THREAD_SUSPEND_RESUME, 0, entry.th32ThreadID) };
                if raw_thread.is_null() {
                    return Err(io::Error::last_os_error());
                }
                let thread = OwnedHandle(raw_thread);
                // SAFETY: this is the suspended primary thread of our child.
                if unsafe { ResumeThread(thread.0) } == u32::MAX {
                    return Err(io::Error::last_os_error());
                }
                return Ok(());
            }
            // SAFETY: continue iterating the same valid snapshot and entry.
            has_entry = unsafe { Thread32Next(snapshot.0, &mut entry) } != 0;
        }

        Err(io::Error::new(
            io::ErrorKind::NotFound,
            "suspended CLI primary thread was not found",
        ))
    }

    #[cfg(test)]
    pub(super) fn active_tree_count() -> usize {
        ACTIVE_TREES.load(Ordering::Relaxed)
    }
}

#[cfg(test)]
mod tests {
    use std::time::Duration;

    use serial_test::serial;
    use tempfile::tempdir;

    use super::*;

    const TEST_TIMEOUT: Duration = Duration::from_secs(10);

    #[tokio::test]
    #[serial]
    async fn terminate_kills_grandchild_and_reaps_leader() {
        let baseline = active_tree_count();
        let temp = tempdir().expect("create temp directory");
        let pid_file = temp.path().join("grandchild.pid");
        let mut child =
            ManagedChild::spawn(test_tree_command(&pid_file)).expect("spawn managed process tree");
        let direct_pid = child.id().expect("direct child pid");
        let grandchild_pid = wait_for_test_pid(&pid_file).await;

        assert!(test_process_exists(direct_pid));
        assert!(test_process_exists(grandchild_pid));
        assert_eq!(active_tree_count(), baseline + 1);

        child.terminate().expect("terminate process tree");
        child.wait().await.expect("reap direct child");
        child
            .wait_for_tree_exit(TEST_TIMEOUT)
            .await
            .expect("wait for process tree exit");
        drop(child);

        assert!(!test_process_exists(direct_pid));
        assert!(!test_process_exists(grandchild_pid));
        assert_eq!(active_tree_count(), baseline);
    }

    #[tokio::test]
    #[serial]
    async fn drop_kills_grandchild_and_reaps_leader() {
        let baseline = active_tree_count();
        let temp = tempdir().expect("create temp directory");
        let pid_file = temp.path().join("grandchild.pid");
        let child =
            ManagedChild::spawn(test_tree_command(&pid_file)).expect("spawn managed process tree");
        let direct_pid = child.id().expect("direct child pid");
        let grandchild_pid = wait_for_test_pid(&pid_file).await;

        drop(child);

        assert!(
            wait_for_test_condition(TEST_TIMEOUT, || {
                !test_process_exists(direct_pid) && !test_process_exists(grandchild_pid)
            })
            .await,
            "process tree survived managed-child drop"
        );
        assert!(
            wait_for_test_condition(TEST_TIMEOUT, || active_tree_count() == baseline).await,
            "process-tree guard survived managed-child drop"
        );
    }

    #[cfg(windows)]
    #[tokio::test]
    #[serial]
    async fn job_handle_close_kills_grandchild() {
        let baseline = active_tree_count();
        let temp = tempdir().expect("create temp directory");
        let pid_file = temp.path().join("grandchild.pid");
        let mut child =
            ManagedChild::spawn(test_tree_command(&pid_file)).expect("spawn managed process tree");
        let direct_pid = child.id().expect("direct child pid");
        let grandchild_pid = wait_for_test_pid(&pid_file).await;

        drop(child.tree.take().expect("Windows Job Object"));
        child.wait().await.expect("reap direct child");
        drop(child);

        assert!(
            wait_for_test_condition(TEST_TIMEOUT, || {
                !test_process_exists(direct_pid)
                    && !test_process_exists(grandchild_pid)
                    && active_tree_count() == baseline
            })
            .await,
            "process tree survived KILL_ON_JOB_CLOSE"
        );
    }
}
