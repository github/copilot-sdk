//! Windows crash-safe ownership of an SDK-spawned CLI process.
//!
//! A Job Object with `JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE` lets Windows
//! terminate the CLI when the SDK-hosting process exits abruptly, even when
//! Rust cleanup code never runs. Other platforms retain Tokio's direct-child
//! ownership because no equivalent product failure has been demonstrated.

use std::io;

use tokio::process::{Child, Command};

pub(crate) fn spawn(command: &mut Command) -> io::Result<(Child, Option<ProcessTree>)> {
    #[cfg(windows)]
    {
        platform::spawn(command).map(|(child, tree)| (child, Some(ProcessTree(Some(tree)))))
    }
    #[cfg(not(windows))]
    {
        command.spawn().map(|child| (child, None))
    }
}

pub(crate) struct ProcessTree(Option<platform::Tree>);

impl ProcessTree {
    pub(crate) fn terminate(mut self) -> io::Result<()> {
        self.0.take().expect("process tree is armed").terminate()
    }
}

impl Drop for ProcessTree {
    fn drop(&mut self) {
        if let Some(tree) = self.0.take() {
            let _ = tree.terminate();
        }
    }
}

#[cfg(not(windows))]
mod platform {
    pub(super) struct Tree;

    impl Tree {
        pub(super) fn terminate(&self) -> std::io::Result<()> {
            unreachable!("process-tree ownership is Windows-only")
        }
    }
}

#[cfg(windows)]
mod platform {
    use std::mem::size_of;
    use std::os::windows::process::CommandExt;
    use std::{io, ptr};

    use tokio::process::{Child, Command};
    use windows_sys::Win32::Foundation::{CloseHandle, HANDLE, INVALID_HANDLE_VALUE};
    use windows_sys::Win32::System::Diagnostics::ToolHelp::{
        CreateToolhelp32Snapshot, TH32CS_SNAPTHREAD, THREADENTRY32, Thread32First, Thread32Next,
    };
    use windows_sys::Win32::System::JobObjects::{
        AssignProcessToJobObject, CreateJobObjectW, JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE,
        JOBOBJECT_EXTENDED_LIMIT_INFORMATION, JobObjectExtendedLimitInformation,
        SetInformationJobObject, TerminateJobObject,
    };
    use windows_sys::Win32::System::Threading::{
        CREATE_NO_WINDOW, CREATE_SUSPENDED, OpenThread, ResumeThread, THREAD_SUSPEND_RESUME,
    };

    struct OwnedHandle(HANDLE);

    // SAFETY: Win32 handles may be used and closed from any thread.
    unsafe impl Send for OwnedHandle {}
    unsafe impl Sync for OwnedHandle {}

    impl Drop for OwnedHandle {
        fn drop(&mut self) {
            // SAFETY: this value uniquely owns a valid handle.
            unsafe {
                CloseHandle(self.0);
            }
        }
    }

    pub(super) struct Tree {
        job: OwnedHandle,
    }

    pub(super) fn spawn(command: &mut Command) -> io::Result<(Child, Tree)> {
        // The root cannot run or create descendants before Job assignment.
        command
            .as_std_mut()
            .creation_flags(CREATE_NO_WINDOW | CREATE_SUSPENDED);
        let mut child = command.spawn()?;
        match attach_and_resume(&child) {
            Ok(tree) => Ok((child, tree)),
            Err(error) => {
                let _ = child.start_kill();
                Err(error)
            }
        }
    }

    fn attach_and_resume(child: &Child) -> io::Result<Tree> {
        // SAFETY: null security attributes and name create a private,
        // non-inheritable Job Object.
        let raw_job = unsafe { CreateJobObjectW(ptr::null(), ptr::null()) };
        if raw_job.is_null() {
            return Err(io::Error::last_os_error());
        }
        let job = OwnedHandle(raw_job);

        let mut limits = JOBOBJECT_EXTENDED_LIMIT_INFORMATION::default();
        limits.BasicLimitInformation.LimitFlags = JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE;
        // SAFETY: `limits` has the layout required by the selected info class.
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

        let process = child.raw_handle().ok_or_else(|| {
            io::Error::new(
                io::ErrorKind::NotFound,
                "CLI exited before Job Object assignment",
            )
        })?;
        // SAFETY: both handles are valid and the child is still suspended.
        if unsafe { AssignProcessToJobObject(job.0, process.cast()) } == 0 {
            return Err(io::Error::last_os_error());
        }

        resume_initial_thread(child.id().ok_or_else(|| {
            io::Error::new(io::ErrorKind::NotFound, "CLI exited before thread resume")
        })?)?;
        Ok(Tree { job })
    }

    fn resume_initial_thread(pid: u32) -> io::Result<()> {
        // SAFETY: the returned snapshot handle is owned and closed below.
        let snapshot = unsafe { CreateToolhelp32Snapshot(TH32CS_SNAPTHREAD, 0) };
        if snapshot == INVALID_HANDLE_VALUE {
            return Err(io::Error::last_os_error());
        }
        let snapshot = OwnedHandle(snapshot);
        let mut entry = THREADENTRY32 {
            dwSize: size_of::<THREADENTRY32>() as u32,
            ..Default::default()
        };

        // SAFETY: `entry` has the documented size and remains live throughout
        // enumeration.
        let mut found = unsafe { Thread32First(snapshot.0, &mut entry) } != 0;
        while found {
            if entry.th32OwnerProcessID == pid {
                // SAFETY: the thread id came from the live system snapshot.
                let raw_thread =
                    unsafe { OpenThread(THREAD_SUSPEND_RESUME, 0, entry.th32ThreadID) };
                if raw_thread.is_null() {
                    return Err(io::Error::last_os_error());
                }
                let thread = OwnedHandle(raw_thread);
                // SAFETY: this is the root's suspended initial thread.
                if unsafe { ResumeThread(thread.0) } == u32::MAX {
                    return Err(io::Error::last_os_error());
                }
                return Ok(());
            }
            // SAFETY: same valid snapshot and initialized entry as above.
            found = unsafe { Thread32Next(snapshot.0, &mut entry) } != 0;
        }

        Err(io::Error::new(
            io::ErrorKind::NotFound,
            "CLI initial thread was not found",
        ))
    }

    impl Tree {
        pub(super) fn terminate(&self) -> io::Result<()> {
            // SAFETY: the handle is a live Job Object owned by this value.
            if unsafe { TerminateJobObject(self.job.0, 1) } != 0 {
                Ok(())
            } else {
                Err(io::Error::last_os_error())
            }
        }
    }
}

#[cfg(all(test, windows))]
mod tests {
    use std::path::Path;
    use std::time::Duration;

    use tokio::process::Command;
    use windows_sys::Win32::Foundation::{CloseHandle, WAIT_TIMEOUT};
    use windows_sys::Win32::System::Threading::{
        OpenProcess, PROCESS_SYNCHRONIZE, WaitForSingleObject,
    };

    use super::spawn;

    const HELPER_FILTER: &str = "process_tree::tests::sdk_host_helper_entrypoint";

    #[tokio::test]
    async fn sdk_host_helper_entrypoint() {
        let Ok(cli_pid_path) = std::env::var("PROCESS_TREE_CLI_PID_PATH") else {
            return;
        };
        let mut command = Command::new("powershell.exe");
        command.args([
            "-NoLogo",
            "-NoProfile",
            "-NonInteractive",
            "-Command",
            "Start-Sleep -Seconds 120",
        ]);
        let (child, _tree) = spawn(&mut command).expect("spawn owned CLI process");
        std::fs::write(cli_pid_path, child.id().expect("CLI pid").to_string())
            .expect("record CLI pid");
        std::future::pending::<()>().await;
    }

    #[tokio::test]
    async fn job_kills_cli_when_sdk_host_is_terminated() {
        let temp = tempfile::tempdir().expect("create temp directory");
        let cli_pid_path = temp.path().join("cli.pid");
        let mut host = Command::new(std::env::current_exe().expect("current test binary"));
        host.args([HELPER_FILTER, "--exact", "--nocapture"])
            .env("PROCESS_TREE_CLI_PID_PATH", &cli_pid_path);
        let mut host = host.spawn().expect("spawn SDK host");
        let cli_pid = wait_for_pid(&cli_pid_path).await;
        assert!(process_alive(cli_pid), "CLI must be alive before host exit");

        host.kill().await.expect("terminate SDK host abruptly");

        wait_for_process_exit(cli_pid).await;
    }

    async fn wait_for_pid(path: &Path) -> u32 {
        let deadline = tokio::time::Instant::now() + Duration::from_secs(15);
        loop {
            if let Ok(value) = std::fs::read_to_string(path) {
                return value.trim().parse().expect("parse CLI pid");
            }
            assert!(
                tokio::time::Instant::now() < deadline,
                "SDK host did not record its CLI pid"
            );
            tokio::time::sleep(Duration::from_millis(10)).await;
        }
    }

    async fn wait_for_process_exit(pid: u32) {
        let deadline = tokio::time::Instant::now() + Duration::from_secs(15);
        while process_alive(pid) {
            assert!(
                tokio::time::Instant::now() < deadline,
                "CLI survived abrupt SDK host termination"
            );
            tokio::time::sleep(Duration::from_millis(10)).await;
        }
    }

    fn process_alive(pid: u32) -> bool {
        // SAFETY: the process handle is closed before returning.
        unsafe {
            let process = OpenProcess(PROCESS_SYNCHRONIZE, 0, pid);
            if process.is_null() {
                return false;
            }
            let alive = WaitForSingleObject(process, 0) == WAIT_TIMEOUT;
            CloseHandle(process);
            alive
        }
    }
}
