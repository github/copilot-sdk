//! Windows crash-safe ownership of an SDK-spawned CLI process.
//!
//! A Job Object with `JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE` lets Windows
//! terminate the CLI when the SDK-hosting process exits abruptly, even when
//! Rust cleanup code never runs. Other platforms retain Tokio's direct-child
//! ownership because no equivalent product failure has been demonstrated.
//! When Windows denies reopening the initial thread, a logged ordinary-spawn
//! retry uses best-effort Job assignment, without pre-assignment containment.

use std::io;

#[cfg(feature = "runtime")]
use tokio::process::{Child, Command};

#[cfg(feature = "runtime")]
pub(crate) fn spawn(command: &mut Command) -> io::Result<(Child, Option<ProcessTree>)> {
    #[cfg(windows)]
    {
        platform::spawn(command)
            .map(|(child, tree)| (child, tree.map(|tree| ProcessTree(Some(tree)))))
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
    use windows_sys::Win32::Foundation::{
        CloseHandle, ERROR_ACCESS_DENIED, ERROR_NO_MORE_FILES, ERROR_NO_MORE_ITEMS, HANDLE,
        INVALID_HANDLE_VALUE,
    };
    use windows_sys::Win32::System::Diagnostics::ProcessSnapshotting::{
        HPSS, HPSSWALK, PSS_CAPTURE_THREADS, PSS_THREAD_ENTRY, PSS_WALK_THREADS,
        PssCaptureSnapshot, PssFreeSnapshot, PssWalkMarkerCreate, PssWalkMarkerFree,
        PssWalkSnapshot,
    };
    use windows_sys::Win32::System::Diagnostics::ToolHelp::{
        CreateToolhelp32Snapshot, TH32CS_SNAPTHREAD, THREADENTRY32, Thread32First, Thread32Next,
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

    #[derive(Debug)]
    struct ThreadOpenError(io::Error);

    impl std::fmt::Display for ThreadOpenError {
        fn fmt(&self, formatter: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
            write!(formatter, "OpenThread failed: {}", self.0)
        }
    }

    impl std::error::Error for ThreadOpenError {
        fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
            Some(&self.0)
        }
    }

    pub(super) fn spawn(command: &mut Command) -> io::Result<(Child, Option<Tree>)> {
        // The root cannot run or create descendants before Job assignment.
        command
            .as_std_mut()
            .creation_flags(CREATE_NO_WINDOW | CREATE_SUSPENDED);
        let mut child = command.spawn()?;
        let tree = match attach(&child) {
            Ok(tree) => tree,
            Err(error) => {
                if let Err(kill_error) = terminate_suspended(&mut child) {
                    tracing::warn!(%kill_error, "failed to terminate suspended CLI child");
                }
                return Err(error);
            }
        };
        let Err(error) = resume(&child, &tree) else {
            return Ok((child, Some(tree)));
        };
        // Keep the Job alive until termination is requested. Dropping it earlier
        // races start_kill against kill-on-close. Never retry a root that can run.
        if let Err(kill_error) = terminate_suspended(&mut child) {
            tracing::warn!(%kill_error, "failed to terminate suspended CLI child");
            return Err(error);
        }
        let denied_thread_open = error
            .get_ref()
            .and_then(|source| source.downcast_ref::<ThreadOpenError>())
            .is_some_and(|source| source.0.raw_os_error() == Some(ERROR_ACCESS_DENIED as i32));
        if !denied_thread_open {
            return Err(error);
        }
        drop(child);
        tracing::warn!(
            %error,
            "CLI initial thread access denied; retrying without suspension and with \
             best-effort private Job assignment; descendants can escape before assignment"
        );
        command.as_std_mut().creation_flags(CREATE_NO_WINDOW);
        let child = command.spawn().map_err(|spawn_error| {
            io::Error::new(
                spawn_error.kind(),
                format!("ordinary CLI spawn failed after {error}: {spawn_error}"),
            )
        })?;
        #[cfg(test)]
        let assignment =
            fail_at(FailurePoint::CompatibilityAssignment).and_then(|()| tree.assign(&child));
        #[cfg(not(test))]
        let assignment = tree.assign(&child);
        match assignment {
            Ok(()) => Ok((child, Some(tree))),
            Err(assignment_error) => {
                tracing::warn!(
                    %assignment_error,
                    "ordinary CLI child could not join its private Job; descendants may survive \
                     client shutdown and processes may survive SDK host exit"
                );
                Ok((child, None))
            }
        }
    }

    pub(super) fn terminate_suspended(child: &mut Child) -> io::Result<()> {
        #[cfg(test)]
        let result = fail_at(FailurePoint::Kill).and_then(|()| child.start_kill());
        #[cfg(not(test))]
        let result = child.start_kill();
        match result {
            // TerminateProcess can deny an already-terminated process. Only an
            // observed exit on this Child's retained handle makes retry safe.
            Err(error) if error.raw_os_error() == Some(ERROR_ACCESS_DENIED as i32) => {
                match child.try_wait() {
                    Ok(Some(_)) => Ok(()),
                    Ok(None) => Err(error),
                    Err(wait_error) => {
                        tracing::warn!(%wait_error, "failed to confirm suspended CLI child exit");
                        Err(error)
                    }
                }
            }
            result => result,
        }
    }

    fn attach(child: &Child) -> io::Result<Tree> {
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

        let tree = Tree { job };
        tree.assign(child)?;
        Ok(tree)
    }

    fn resume(child: &Child, _tree: &Tree) -> io::Result<()> {
        #[cfg(test)]
        if let Some(hook) = BEFORE_RESUME.with_borrow_mut(Option::take) {
            hook(child, _tree.job.0)?;
        }
        let process = child.raw_handle().ok_or_else(|| {
            io::Error::new(io::ErrorKind::NotFound, "CLI exited before thread resume")
        })?;
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
        result
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
        let thread_id = match find_initial_thread_pss(process, pid) {
            Ok(thread_id) => thread_id,
            Err(error) => {
                tracing::warn!(
                    %error,
                    "CLI process-scoped thread lookup failed; falling back to Toolhelp"
                );
                find_initial_thread_toolhelp(pid)?
            }
        };
        let thread = open_initial_thread(thread_id)
            .map_err(|error| io::Error::new(error.kind(), ThreadOpenError(error)))?;
        resume_owned_thread(thread.0, pid)
    }

    fn open_initial_thread(thread_id: u32) -> io::Result<OwnedHandle> {
        #[cfg(test)]
        fail_at(FailurePoint::OpenThread)?;
        // SAFETY: the lookup selected a thread belonging to the suspended root.
        // The opened handle's ownership is independently checked before resume.
        let raw_thread = unsafe {
            OpenThread(
                THREAD_SUSPEND_RESUME | THREAD_QUERY_LIMITED_INFORMATION,
                0,
                thread_id,
            )
        };
        if raw_thread.is_null() {
            return Err(io::Error::last_os_error());
        }
        Ok(OwnedHandle(raw_thread))
    }

    pub(super) fn find_initial_thread_toolhelp(pid: u32) -> io::Result<u32> {
        #[cfg(test)]
        fail_at(FailurePoint::Toolhelp)?;
        // SAFETY: this read-only system snapshot does not execute any thread.
        let raw = unsafe { CreateToolhelp32Snapshot(TH32CS_SNAPTHREAD, 0) };
        if raw == INVALID_HANDLE_VALUE {
            return Err(io::Error::last_os_error());
        }
        let snapshot = OwnedHandle(raw);
        let mut entry = THREADENTRY32 {
            dwSize: size_of::<THREADENTRY32>() as u32,
            ..Default::default()
        };
        // SAFETY: entry has the required layout and the snapshot remains owned.
        let mut found = unsafe { Thread32First(snapshot.0, &mut entry) };
        while found != 0 {
            if entry.th32OwnerProcessID == pid {
                return Ok(entry.th32ThreadID);
            }
            entry.dwSize = size_of::<THREADENTRY32>() as u32;
            // SAFETY: entry and snapshot remain valid throughout the walk.
            found = unsafe { Thread32Next(snapshot.0, &mut entry) };
        }
        let error = io::Error::last_os_error();
        if error.raw_os_error() == Some(ERROR_NO_MORE_FILES as i32) {
            Err(io::Error::new(
                io::ErrorKind::NotFound,
                "CLI initial thread was not found in the Toolhelp snapshot",
            ))
        } else {
            Err(error)
        }
    }

    fn find_initial_thread_pss(process: HANDLE, pid: u32) -> io::Result<u32> {
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
                return Ok(entry.ThreadId);
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
        fn assign(&self, child: &Child) -> io::Result<()> {
            let process = child.raw_handle().ok_or_else(|| {
                io::Error::new(
                    io::ErrorKind::NotFound,
                    "CLI exited before Job Object assignment",
                )
            })?;
            // SAFETY: the Job and child handles remain valid. Normal startup
            // assigns before resume; compatibility startup is best-effort.
            if unsafe { AssignProcessToJobObject(self.job.0, process.cast()) } == 0 {
                Err(io::Error::last_os_error())
            } else {
                Ok(())
            }
        }

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
