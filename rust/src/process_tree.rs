//! Cross-platform ownership of a spawned CLI process tree.
//!
//! Windows Job Objects retain every nested descendant. Unix process groups
//! cover descendants that inherit the CLI's group; a process that explicitly
//! creates a new session or process group is outside that platform primitive.

use std::io;

use tokio::process::{Child, Command};

/// Spawns `command` inside an OS primitive that contains the root process and
/// every descendant it creates.
pub(crate) fn spawn(command: &mut Command) -> io::Result<(Child, ProcessTree)> {
    platform::spawn(command).map(|(child, tree)| (child, ProcessTree(Some(tree))))
}

/// Owns the OS containment primitive for one SDK-spawned CLI.
pub(crate) struct ProcessTree(Option<platform::Tree>);

impl ProcessTree {
    /// Terminates every process still contained in the tree.
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

#[cfg(test)]
pub(crate) fn process_alive(pid: u32) -> bool {
    platform::process_alive(pid)
}

#[cfg(unix)]
mod platform {
    use std::io;

    use tokio::process::{Child, Command};

    pub(super) struct Tree {
        pgid: i32,
    }

    pub(super) fn spawn(command: &mut Command) -> io::Result<(Child, Tree)> {
        // The group is established between fork and exec, before the child
        // can create descendants.
        command.process_group(0);
        let child = command.spawn()?;
        let pid = child.id().ok_or_else(|| {
            io::Error::new(
                io::ErrorKind::NotFound,
                "CLI exited before its process group could be recorded",
            )
        })?;
        Ok((child, Tree { pgid: pid as i32 }))
    }

    impl Tree {
        pub(super) fn terminate(&self) -> io::Result<()> {
            // Callers must terminate before reaping the root. Its unreaped pid
            // keeps this process-group id from being reused for an unrelated
            // group between ownership lookup and signaling.
            // SAFETY: `killpg` takes an integer process-group identifier and
            // does not dereference memory.
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
        // SAFETY: signal 0 probes process existence without changing it.
        unsafe { libc::kill(pid as i32, 0) == 0 }
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
        // Suspension closes the post-spawn assignment race: the root cannot
        // create descendants until it belongs to the Job Object.
        command
            .as_std_mut()
            .creation_flags(CREATE_NO_WINDOW | CREATE_SUSPENDED);
        let mut child = command.spawn()?;
        let result = attach_and_resume(&child);
        match result {
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
            let alive = WaitForSingleObject(process, 0) == WAIT_TIMEOUT;
            CloseHandle(process);
            alive
        }
    }
}
