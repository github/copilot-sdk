//! Windows crash-safe ownership of an SDK-spawned CLI process.
//!
//! A Job Object with `JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE` lets Windows
//! terminate the CLI when the SDK-hosting process exits abruptly, even when
//! Rust cleanup code never runs. Other platforms retain Tokio's direct-child
//! ownership because no equivalent product failure has been demonstrated.

use std::io;

#[cfg(feature = "runtime")]
use tokio::process::{Child, Command};

#[cfg(feature = "runtime")]
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

#[cfg(not(all(windows, feature = "runtime")))]
mod platform {
    pub(super) struct Tree;

    impl Tree {
        pub(super) fn terminate(&self) -> std::io::Result<()> {
            unreachable!("process-tree ownership is Windows-only")
        }
    }
}

#[cfg(all(windows, feature = "runtime"))]
mod platform {
    use std::mem::size_of;
    use std::os::windows::process::CommandExt;
    use std::{io, ptr};

    use tokio::process::{Child, Command};
    use windows_sys::Win32::Foundation::{CloseHandle, ERROR_NO_MORE_ITEMS, HANDLE};
    use windows_sys::Win32::System::Diagnostics::ProcessSnapshotting::{
        HPSS, HPSSWALK, PSS_CAPTURE_THREADS, PSS_THREAD_ENTRY, PSS_WALK_THREADS,
        PssCaptureSnapshot, PssFreeSnapshot, PssWalkMarkerCreate, PssWalkMarkerFree,
        PssWalkSnapshot,
    };
    use windows_sys::Win32::System::JobObjects::{
        AssignProcessToJobObject, CreateJobObjectW, JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE,
        JOBOBJECT_EXTENDED_LIMIT_INFORMATION, JobObjectExtendedLimitInformation,
        SetInformationJobObject, TerminateJobObject,
    };
    use windows_sys::Win32::System::Threading::{
        CREATE_NO_WINDOW, CREATE_SUSPENDED, GetCurrentProcess, GetProcessIdOfThread, OpenThread,
        ResumeThread, THREAD_QUERY_LIMITED_INFORMATION, THREAD_SUSPEND_RESUME,
    };

    #[cfg(test)]
    use super::tests::{
        BEFORE_ASSIGNMENT, BEFORE_RESUME, FailurePoint, MARKERS, RESUME_ELAPSED, SNAPSHOTS,
        THREADS_EXAMINED, fail_at,
    };

    struct ThreadSnapshot(HPSS);

    impl Drop for ThreadSnapshot {
        fn drop(&mut self) {
            // SAFETY: this process uniquely owns the local snapshot descriptor.
            let status = unsafe { PssFreeSnapshot(GetCurrentProcess(), self.0) };
            if let Err(error) = check_status(status) {
                tracing::warn!(%error, "failed to free CLI thread snapshot");
            }
            #[cfg(test)]
            if status == 0 {
                SNAPSHOTS.set((SNAPSHOTS.get().0, SNAPSHOTS.get().1 + 1));
            }
        }
    }

    struct WalkMarker(HPSSWALK);

    impl Drop for WalkMarker {
        fn drop(&mut self) {
            // SAFETY: this value uniquely owns a valid walk marker.
            let status = unsafe { PssWalkMarkerFree(self.0) };
            if let Err(error) = check_status(status) {
                tracing::warn!(%error, "failed to free CLI thread walk marker");
            }
            #[cfg(test)]
            if status == 0 {
                MARKERS.set((MARKERS.get().0, MARKERS.get().1 + 1));
            }
        }
    }

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
                if let Err(kill_error) = child.start_kill() {
                    tracing::warn!(%kill_error, "failed to terminate suspended CLI child");
                }
                Err(error)
            }
        }
    }

    fn attach_and_resume(child: &Child) -> io::Result<Tree> {
        #[cfg(test)]
        if let Some(hook) = BEFORE_ASSIGNMENT.with_borrow_mut(Option::take) {
            hook(child)?;
        }
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

        #[cfg(test)]
        if let Some(hook) = BEFORE_RESUME.with_borrow_mut(Option::take) {
            hook(child, job.0)?;
        }
        #[cfg(test)]
        let resume_start = std::time::Instant::now();
        let result = resume_initial_thread(
            process.cast(),
            child.id().ok_or_else(|| {
                io::Error::new(io::ErrorKind::NotFound, "CLI exited before thread resume")
            })?,
        );
        #[cfg(test)]
        RESUME_ELAPSED.set(resume_start.elapsed());
        result?;
        Ok(Tree { job })
    }

    pub(super) fn check_status(status: u32) -> io::Result<()> {
        if status == 0 {
            Ok(())
        } else {
            Err(io::Error::from_raw_os_error(status as i32))
        }
    }

    pub(super) fn resume_initial_thread(process: HANDLE, pid: u32) -> io::Result<()> {
        #[cfg(test)]
        THREADS_EXAMINED.set(0);
        #[cfg(test)]
        fail_at(FailurePoint::Capture)?;
        let mut snapshot = ptr::null_mut();
        // SAFETY: the suspended child owns the live process handle throughout
        // capture and resume. Only its thread information is captured.
        check_status(unsafe {
            PssCaptureSnapshot(process, PSS_CAPTURE_THREADS, 0, &mut snapshot)
        })?;
        let snapshot = ThreadSnapshot(snapshot);
        #[cfg(test)]
        SNAPSHOTS.set((SNAPSHOTS.get().0 + 1, SNAPSHOTS.get().1));

        #[cfg(test)]
        fail_at(FailurePoint::Marker)?;
        let mut marker = ptr::null_mut();
        // SAFETY: Windows initializes the marker with its default allocator.
        check_status(unsafe { PssWalkMarkerCreate(ptr::null(), &mut marker) })?;
        let marker = WalkMarker(marker);
        #[cfg(test)]
        MARKERS.set((MARKERS.get().0 + 1, MARKERS.get().1));

        loop {
            #[cfg(test)]
            fail_at(FailurePoint::Walk)?;
            let mut entry = PSS_THREAD_ENTRY::default();
            // SAFETY: the aligned entry has the layout and size required by
            // PSS_WALK_THREADS; both snapshot and marker remain owned.
            let status = unsafe {
                PssWalkSnapshot(
                    snapshot.0,
                    PSS_WALK_THREADS,
                    marker.0,
                    ptr::from_mut(&mut entry).cast(),
                    size_of::<PSS_THREAD_ENTRY>() as u32,
                )
            };
            if status == ERROR_NO_MORE_ITEMS {
                return Err(io::Error::new(
                    io::ErrorKind::NotFound,
                    "CLI initial thread was not found in its process snapshot",
                ));
            }
            check_status(status)?;
            #[cfg(test)]
            THREADS_EXAMINED.set(THREADS_EXAMINED.get() + 1);
            if entry.ProcessId == pid {
                #[cfg(test)]
                fail_at(FailurePoint::OpenThread)?;
                // SAFETY: the thread id came from the child's process snapshot.
                let raw_thread = unsafe {
                    OpenThread(
                        THREAD_SUSPEND_RESUME | THREAD_QUERY_LIMITED_INFORMATION,
                        0,
                        entry.ThreadId,
                    )
                };
                if raw_thread.is_null() {
                    return Err(io::Error::last_os_error());
                }
                let thread = OwnedHandle(raw_thread);
                return resume_owned_thread(thread.0, pid);
            }
        }
    }

    pub(super) fn resume_owned_thread(thread: HANDLE, pid: u32) -> io::Result<()> {
        #[cfg(test)]
        fail_at(FailurePoint::QueryThread)?;
        // SAFETY: the caller owns this thread handle with limited-query rights.
        let owner = unsafe { GetProcessIdOfThread(thread) };
        if owner == 0 {
            return Err(io::Error::last_os_error());
        }
        if owner != pid {
            return Err(io::Error::new(
                io::ErrorKind::PermissionDenied,
                "CLI initial thread no longer belongs to the spawned process",
            ));
        }
        #[cfg(test)]
        fail_at(FailurePoint::Resume)?;
        // SAFETY: the open handle's ownership was verified, and the root's
        // initial thread remains suspended until Job assignment completes.
        if unsafe { ResumeThread(thread) } == u32::MAX {
            return Err(io::Error::last_os_error());
        }
        Ok(())
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

#[cfg(all(test, windows, feature = "runtime"))]
mod tests;
