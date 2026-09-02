//! Windows crash-safe ownership of an SDK-spawned CLI process.
//!
//! A Job Object with `JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE` lets Windows
//! terminate the CLI when the SDK-hosting process exits abruptly, even when
//! Rust cleanup code never runs. Other platforms retain Tokio's direct-child
//! ownership because no equivalent product failure has been demonstrated.

use std::io;

use tokio::process::{Child, Command};
#[cfg(unix)]
use tracing::warn;

pub(crate) fn configure(command: &mut Command) {
    platform::configure(command);
}

pub(crate) fn spawn(command: &mut Command) -> io::Result<(Child, Option<ProcessTree>)> {
    #[cfg(windows)]
    {
        platform::spawn(command).map(|(child, tree)| (child, Some(ProcessTree(Some(tree)))))
    }
    #[cfg(unix)]
    {
        let child = command.spawn()?;
        let tree = attach(&child);
        Ok((child, tree))
    }
    #[cfg(not(any(unix, windows)))]
    {
        command.spawn().map(|child| (child, None))
    }
}

#[cfg(unix)]
pub(crate) fn attach(child: &Child) -> Option<ProcessTree> {
    match platform::attach(child) {
        Ok(tree) => Some(ProcessTree(Some(tree))),
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

#[cfg(all(test, unix))]
pub(crate) fn process_alive(pid: u32) -> bool {
    platform::process_alive(pid)
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

#[cfg(unix)]
mod platform {
    use std::io;

    use tokio::process::{Child, Command};

    pub(super) struct Tree {
        pgid: i32,
    }

    pub(super) fn configure(command: &mut Command) {
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
            // SAFETY: `killpg` only signals processes sharing `self.pgid`.
            if unsafe { libc::killpg(self.pgid, libc::SIGKILL) } == 0 {
                return Ok(());
            }
            match io::Error::last_os_error() {
                error if error.raw_os_error() == Some(libc::ESRCH) => Ok(()),
                error => Err(error),
            }
        }
    }

    #[cfg(test)]
    pub(super) fn process_alive(pid: u32) -> bool {
        #[cfg(target_os = "linux")]
        if let Ok(stat) = std::fs::read_to_string(format!("/proc/{pid}/stat"))
            && stat
                .rsplit_once(") ")
                .and_then(|(_, fields)| fields.chars().next())
                .is_some_and(|state| matches!(state, 'Z' | 'X'))
        {
            return false;
        }
        // SAFETY: signal 0 only probes process existence.
        (unsafe { libc::kill(pid as i32, 0) }) == 0
    }
}

#[cfg(not(any(unix, windows)))]
mod platform {
    use tokio::process::Command;

    pub(super) struct Tree;

    pub(super) fn configure(_command: &mut Command) {}

    impl Tree {
        pub(super) fn terminate(&self) -> std::io::Result<()> {
            unreachable!("process-tree ownership is unsupported on this platform")
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

    pub(super) fn configure(_command: &mut Command) {}

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
