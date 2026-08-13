//! Lifecycle of the CLI child process: termination that is guaranteed to
//! reach the OS reaper.
//!
//! Terminating a child is two steps — deliver the signal, then wait for the
//! kernel to release the process entry — and the second step is the one
//! that is easy to lose. A caller that owns the [`Child`] across an
//! `.await` loses it if its future is cancelled: the handle drops, no one
//! waits, and on Unix the process stays a zombie for as long as the parent
//! lives. That is fatal for an embedded host, which is long-lived by
//! definition and shuts sessions down under a timeout.
//!
//! [`ChildLifecycle`] closes that hole by making the *lifecycle* own the
//! reap rather than the caller. The kill is delivered synchronously, so
//! signalling never depends on a task being polled; the claimed child is
//! then reaped on a dedicated thread with its own runtime, so neither a
//! cancelled future nor a caller's runtime being torn down can strand it.
//! Callers observe the outcome through a [`watch`] channel, which means
//! concurrent and repeat callers all await the same terminal result
//! instead of racing for the handle.

use std::process::ExitStatus;
use std::sync::Arc;

use parking_lot::Mutex;
use tokio::process::Child;
use tokio::sync::watch;
use tracing::{info, warn};

use crate::{Error, ErrorKind};

/// Terminal state of the CLI child process.
///
/// Every state other than `Pending` is definitive, so a waiter always has
/// something to return and never blocks forever.
#[derive(Clone, Debug)]
enum ReapState {
    /// No reaper has finished yet.
    Pending,
    /// The child was waited on and the OS released it. `None` means this
    /// client never owned a child (stream-backed or in-process transport).
    Reaped(Option<ExitStatus>),
    /// Termination could not be confirmed. The process may still exist.
    Failed {
        kind: std::io::ErrorKind,
        message: String,
    },
}

impl ReapState {
    fn from_wait(result: std::io::Result<ExitStatus>) -> Self {
        match result {
            Ok(status) => Self::Reaped(Some(status)),
            Err(error) => Self::Failed {
                kind: error.kind(),
                message: error.to_string(),
            },
        }
    }

    /// Resolve a definitive state into the public result, or `None` while
    /// the outcome is still pending.
    fn resolve(&self) -> Option<Result<Option<ExitStatus>, Error>> {
        match self {
            Self::Pending => None,
            Self::Reaped(status) => Some(Ok(*status)),
            Self::Failed { kind, message } => Some(Err(Error::new(
                ErrorKind::Io,
                std::io::Error::new(*kind, message.clone()),
            ))),
        }
    }
}

/// Owns the CLI child process and guarantees it is reaped exactly once.
pub(crate) struct ChildLifecycle {
    /// Holds the child until a caller claims it for termination. Empty
    /// from that moment on: the detached reaper owns it instead.
    child: Mutex<Option<Child>>,
    /// Last known process ID. Fixed at construction, so diagnostics can
    /// still name the process after the child has been claimed.
    pid: Option<u32>,
    /// Terminal outcome, shared by every waiter.
    state: Arc<watch::Sender<ReapState>>,
}

impl ChildLifecycle {
    pub(crate) fn new(child: Option<Child>) -> Self {
        let pid = child.as_ref().and_then(Child::id);
        // A client with no child has nothing to reap, so its outcome is
        // already final and waiters resolve immediately.
        let initial = match child {
            Some(_) => ReapState::Pending,
            None => ReapState::Reaped(None),
        };
        let (state, _) = watch::channel(initial);
        Self {
            child: Mutex::new(child),
            pid,
            state: Arc::new(state),
        }
    }

    /// Process ID of the live child, or `None` once it has been claimed
    /// for termination.
    pub(crate) fn pid(&self) -> Option<u32> {
        self.child.lock().as_ref().and_then(Child::id)
    }

    /// Whether a live child is still held. False once termination starts.
    pub(crate) fn has_child(&self) -> bool {
        self.child.lock().is_some()
    }

    /// Begin terminating the child and return a handle to its completion.
    ///
    /// Synchronous and idempotent. The first caller signals the child and
    /// hands it to a dedicated reaper thread that waits to completion;
    /// later callers — and callers racing on another thread — get a handle
    /// to that same reaper's outcome. Dropping the returned handle never
    /// cancels the termination, which is the whole point: the reap
    /// outlives whatever future asked for it.
    pub(crate) fn begin_termination(&self) -> ForcedShutdown {
        // Claim, signal, and reset under one lock.
        //
        // The kill is delivered here rather than inside the reaper because
        // `start_kill` needs no reactor: signalling must not depend on a
        // task ever being polled, or a caller tearing its runtime down
        // would leave the CLI process alive. Holding one guard across the
        // take and the reset also stops a concurrent caller subscribing in
        // between and binding to a previous attempt's outcome.
        let (claimed, handle) = {
            let mut slot = self.child.lock();
            match slot.take() {
                Some(mut child) => {
                    let pid = child.id();
                    if let Err(error) = child.start_kill() {
                        // Usually just an already-exited child; the wait
                        // below still reports its real status.
                        warn!(pid = ?pid, error = %error, "kill signal not delivered to CLI process");
                    }
                    self.state.send_replace(ReapState::Pending);
                    (Some(child), self.handle())
                }
                // Either termination already started (the running reaper
                // will publish) or there was never a child (already
                // `Reaped(None)`).
                None => (None, self.handle()),
            }
        };
        let Some(child) = claimed else {
            return handle;
        };

        let pid = child.id();
        info!(pid = ?pid, "terminating CLI process");
        // Reap on a dedicated thread with its own runtime rather than on a
        // caller's. A `tokio::spawn`ed task is cancelled when its runtime
        // drops, which for an embedded host is precisely the moment
        // termination matters — the host tears its runtime down as it
        // shuts down. Owning a thread makes the reap independent of every
        // caller runtime, so a handle really can be awaited anywhere.
        // Termination happens once per client, so the thread is not a hot
        // path.
        let guard = ReapGuard {
            state: Arc::clone(&self.state),
            completed: false,
        };
        if let Err(error) = std::thread::Builder::new()
            .name("copilot-cli-reaper".to_string())
            .spawn(move || {
                // Moved in, so it drops with the thread even if the body
                // panics before publishing.
                let mut guard = guard;
                // `enable_all` is load-bearing on Unix, not boilerplate:
                // once `try_wait` misses, readiness for `Child::wait`
                // comes from the signal driver. Without it, a child that
                // outlives the reaper's first poll — a process wedged in
                // uninterruptible I/O, exactly what `force_stop` exists
                // for — would never be reaped.
                match tokio::runtime::Builder::new_current_thread()
                    .enable_all()
                    .build()
                {
                    Ok(runtime) => {
                        let outcome = runtime.block_on(reap(child));
                        guard.completed = true;
                        guard.state.send_replace(outcome);
                    }
                    Err(error) => {
                        warn!(error = %error, "could not build a runtime to reap the CLI process");
                    }
                }
            })
        {
            // The guard was moved into the closure and dropped with it, so
            // waiters have already been resolved with a definitive error.
            warn!(pid = ?pid, error = %error, "could not start the CLI reaper thread");
        }
        handle
    }

    fn handle(&self) -> ForcedShutdown {
        ForcedShutdown {
            pid: self.pid,
            state: self.state.subscribe(),
        }
    }
}

/// Terminating on drop is what keeps the reaping guarantee honest for a
/// client that is simply dropped: signalling alone would leave the same
/// zombie this type exists to prevent, so hand the child to the reaper.
impl Drop for ChildLifecycle {
    fn drop(&mut self) {
        if self.has_child() {
            info!("client dropped with a live CLI process; terminating it");
            drop(self.begin_termination());
        }
    }
}

/// Publishes a terminal state if the reaper never publishes one of its
/// own — its thread fails to start, its runtime cannot be built, or it
/// panics — so no waiter is left pending.
struct ReapGuard {
    state: Arc<watch::Sender<ReapState>>,
    completed: bool,
}

impl Drop for ReapGuard {
    fn drop(&mut self) {
        if !self.completed {
            self.state.send_replace(ReapState::Failed {
                kind: std::io::ErrorKind::Interrupted,
                message: "the CLI child was signalled but its reaper could not run to completion \
                          (the reaper thread or its runtime failed to start, or it panicked); the \
                          OS may not have released the process"
                    .to_string(),
            });
        }
    }
}

/// Wait for the OS to release a child that has already been signalled.
async fn reap(mut child: Child) -> ReapState {
    let pid = child.id();
    let state = ReapState::from_wait(child.wait().await);
    info!(pid = ?pid, outcome = ?state, "CLI process reaped");
    state
}
/// Owned handle to a CLI process being terminated.
///
/// Returned by [`Client::start_force_stop`](crate::Client::start_force_stop).
/// Await [`wait`](Self::wait) to observe termination completing — that is,
/// the child killed *and* reaped, with no zombie left behind.
///
/// The handle borrows nothing from the client and reaping is owned by a
/// dedicated thread, so it can be moved to another task — or another
/// runtime — and awaited there, including after the runtime that started
/// termination has been torn down. Dropping it without awaiting does not
/// cancel the termination; it only gives up watching. Any number of
/// handles may exist for one child, and they all resolve to the same
/// outcome.
#[derive(Debug)]
#[must_use = "termination continues either way; await this handle to observe it completing"]
pub struct ForcedShutdown {
    pid: Option<u32>,
    state: watch::Receiver<ReapState>,
}

impl ForcedShutdown {
    /// Process ID of the terminating child, if this client spawned one.
    ///
    /// Reports which process is being terminated; it carries no outcome.
    pub fn pid(&self) -> Option<u32> {
        self.pid
    }

    /// Wait until the child has been killed and reaped.
    ///
    /// Resolves to the child's [`ExitStatus`], or `None` when the client
    /// never owned a child (stream-backed and in-process transports).
    /// `None` therefore means "nothing to terminate", never "termination
    /// failed" — a failure is always an `Err`. The status itself is the
    /// child's, and a killed process reports an unsuccessful one; it says
    /// nothing about whether teardown worked.
    ///
    /// Repeat and concurrent waiters all observe the same outcome, and a
    /// handle may be awaited on any runtime — reaping is owned by a
    /// dedicated thread, not by the runtime that started it.
    ///
    /// # Cancel safety
    ///
    /// Cancel-safe, and cancelling gives nothing up: termination is owned
    /// by the SDK, not by this future, so dropping it leaves the reap
    /// running. Acquire another handle to await the same completion.
    ///
    /// # Errors
    ///
    /// Returns [`ErrorKind::Io`] if waiting on the child failed, or if the
    /// reaper could not run to completion — its thread failed to start,
    /// its runtime could not be built, or it panicked. The child has
    /// always been signalled by then; only confirmation is lost.
    ///
    /// No outcome is ever lost: every failure resolves to an `Err` rather
    /// than a pending wait. That is a guarantee about the state machine,
    /// not about elapsed time — the underlying wait is unbounded, so a
    /// process the kernel will not release (uninterruptible I/O) parks
    /// this future for as long as that lasts. Wrap it in
    /// [`tokio::time::timeout`] if the caller needs a bounded shutdown.
    pub async fn wait(mut self) -> Result<Option<ExitStatus>, Error> {
        loop {
            if let Some(outcome) = self.state.borrow_and_update().resolve() {
                return outcome;
            }
            if self.state.changed().await.is_err() {
                // The client and the reaper are both gone without
                // publishing. Report it rather than wait on a sender that
                // no longer exists.
                return Err(Error::new(
                    ErrorKind::Io,
                    std::io::Error::other(
                        "the client was dropped before CLI process termination completed",
                    ),
                ));
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use std::process::Stdio;
    use std::time::Duration;

    use tokio::process::Command;
    use tokio::time::timeout;

    use super::*;

    /// Failure backstop. Every wait in these tests must resolve well
    /// inside it; exceeding it means something hung.
    const TIMEOUT: Duration = Duration::from_secs(10);

    /// A child that stays alive until it is killed.
    fn spawn_sleeper() -> Child {
        let mut command = if cfg!(windows) {
            let mut command = Command::new("cmd");
            command.args(["/C", "ping -n 120 127.0.0.1 > nul"]);
            command
        } else {
            let mut command = Command::new("sleep");
            command.arg("120");
            command
        };
        command
            .stdin(Stdio::null())
            .stdout(Stdio::null())
            .stderr(Stdio::null())
            .spawn()
            .expect("spawn sleeper child")
    }

    /// A child that exits on its own, immediately.
    fn spawn_exiting() -> Child {
        let mut command = if cfg!(windows) {
            let mut command = Command::new("cmd");
            command.args(["/C", "exit 0"]);
            command
        } else {
            Command::new("true")
        };
        command
            .stdin(Stdio::null())
            .stdout(Stdio::null())
            .stderr(Stdio::null())
            .spawn()
            .expect("spawn exiting child")
    }

    /// The regression this type exists for. A caller that claims the child
    /// and then disappears — an outer timeout firing between the claim and
    /// the reap — must not strand the process. The recovery caller has to
    /// find the termination still owned and running, and observe it
    /// completing.
    ///
    /// Deterministic by construction: the first handle is dropped
    /// synchronously, before it is ever polled, which is the exact window
    /// that used to lose the child.
    #[tokio::test]
    async fn handle_dropped_before_polling_still_reaps_and_stays_observable() {
        let lifecycle = ChildLifecycle::new(Some(spawn_sleeper()));
        let pid = lifecycle.pid().expect("sleeper should report a pid");

        let abandoned = lifecycle.begin_termination();
        assert_eq!(abandoned.pid(), Some(pid));
        drop(abandoned);

        let recovery = lifecycle.begin_termination();
        assert_eq!(recovery.pid(), Some(pid), "the handle must not be lost");
        let status = timeout(TIMEOUT, recovery.wait())
            .await
            .expect("recovery waiter hung")
            .expect("recovery waiter failed")
            .expect("a spawned child must report an exit status");
        assert!(
            !status.success(),
            "a killed child must not report success: {status:?}"
        );
        assert!(lifecycle.pid().is_none());
        assert!(!lifecycle.has_child());
    }

    /// Every waiter — those that arrive before the reap finishes and those
    /// that arrive long after — resolves to the same terminal status.
    #[tokio::test]
    async fn concurrent_and_repeat_waiters_observe_one_outcome() {
        let lifecycle = ChildLifecycle::new(Some(spawn_sleeper()));

        let waiters: Vec<_> = (0..8)
            .map(|_| tokio::spawn(lifecycle.begin_termination().wait()))
            .collect();

        let mut statuses = Vec::new();
        for waiter in waiters {
            let status = timeout(TIMEOUT, waiter)
                .await
                .expect("waiter hung")
                .expect("waiter panicked")
                .expect("waiter failed");
            statuses.push(status);
        }
        let first = statuses[0];
        assert!(first.is_some(), "expected an exit status");
        assert!(
            statuses.iter().all(|status| *status == first),
            "waiters disagreed about the outcome: {statuses:?}"
        );

        // A waiter created after the fact resolves to the same value.
        let late = timeout(TIMEOUT, lifecycle.begin_termination().wait())
            .await
            .expect("late waiter hung")
            .expect("late waiter failed");
        assert_eq!(late, first);
    }

    /// A child that has already exited is reaped without hanging, and its
    /// real status survives the failed kill.
    #[tokio::test]
    async fn already_exited_child_is_reaped_without_hanging() {
        let mut child = spawn_exiting();
        let expected = child.wait().await.expect("child should exit");
        // Re-wrap a child that is already gone: `wait` is idempotent and
        // keeps reporting the same status.
        let lifecycle = ChildLifecycle::new(Some(child));

        let status = timeout(TIMEOUT, lifecycle.begin_termination().wait())
            .await
            .expect("waiting on an exited child hung")
            .expect("waiting on an exited child failed");
        assert_eq!(status, Some(expected));
    }

    /// Clients with no child of their own (stream-backed, in-process)
    /// resolve immediately instead of waiting for a process that does not
    /// exist.
    #[tokio::test]
    async fn childless_lifecycle_resolves_immediately() {
        let lifecycle = ChildLifecycle::new(None);
        assert!(!lifecycle.has_child());
        assert!(lifecycle.pid().is_none());

        for _ in 0..3 {
            let outcome = timeout(TIMEOUT, lifecycle.begin_termination().wait())
                .await
                .expect("childless wait hung")
                .expect("childless wait failed");
            assert_eq!(outcome, None);
        }
    }

    /// Gate: a handle really can be awaited on a *different* runtime than
    /// the one that started termination. Runtime A is dropped immediately
    /// after the claim; the reap is owned by a dedicated thread, so it
    /// completes regardless, and runtime B observes the real exit status
    /// rather than an error or a hang.
    #[test]
    fn termination_survives_its_originating_runtime() {
        let runtime_a = tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .expect("build runtime A");
        let lifecycle = runtime_a.block_on(async { ChildLifecycle::new(Some(spawn_sleeper())) });

        let handle = runtime_a.block_on(async { lifecycle.begin_termination() });
        drop(runtime_a);

        let runtime_b = tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .expect("build runtime B");
        let status = runtime_b
            .block_on(async { timeout(TIMEOUT, handle.wait()).await })
            .expect("waiter hung after its originating runtime was dropped")
            .expect("termination must not fail when its originating runtime goes away")
            .expect("a spawned child must report an exit status");
        assert!(!status.success(), "a killed child must not report success");
    }

    /// Termination needs no runtime in context at all: the kill is
    /// synchronous and the reap owns its own. This is what makes the
    /// synchronous `force_stop` usable while a caller is tearing its
    /// runtime down.
    #[test]
    fn termination_without_any_runtime_context_still_kills_and_reaps() {
        let setup = tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .expect("build runtime");
        let lifecycle = setup.block_on(async { ChildLifecycle::new(Some(spawn_sleeper())) });
        drop(setup);

        // No runtime context here whatsoever.
        assert!(tokio::runtime::Handle::try_current().is_err());
        let handle = lifecycle.begin_termination();
        assert!(!lifecycle.has_child(), "the child must have been claimed");

        let observer = tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .expect("build observing runtime");
        let status = observer
            .block_on(async { timeout(TIMEOUT, handle.wait()).await })
            .expect("wait hung")
            .expect("termination without a runtime must still succeed")
            .expect("a spawned child must report an exit status");
        assert!(!status.success());
    }

    /// Gate: dropping every observer — the lifecycle itself and every
    /// handle — must not cancel the kill or the reap. Nothing is left
    /// running, and nothing keeps the lifecycle alive.
    #[cfg(unix)]
    #[test]
    fn dropping_every_observer_does_not_cancel_the_reap() {
        let runtime = tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .expect("build runtime");
        let lifecycle = runtime.block_on(async { ChildLifecycle::new(Some(spawn_sleeper())) });
        let pid = lifecycle.pid().expect("sleeper should report a pid");

        let handle = lifecycle.begin_termination();
        drop(handle);
        drop(lifecycle);
        drop(runtime);

        // The reaper thread owns the child; give it a moment to finish.
        let deadline = std::time::Instant::now() + std::time::Duration::from_secs(5);
        loop {
            let output = std::process::Command::new("ps")
                .args(["-o", "stat=", "-p", &pid.to_string()])
                .output()
                .expect("run ps");
            let state = String::from_utf8_lossy(&output.stdout).trim().to_string();
            if state.is_empty() {
                return;
            }
            assert!(
                std::time::Instant::now() < deadline,
                "the CLI process was never reaped (ps state {state:?})"
            );
            std::thread::sleep(std::time::Duration::from_millis(50));
        }
    }

    /// The critical guarantee behind `force_stop`'s synchronous contract:
    /// the kill must not depend on a task ever being polled. A runtime
    /// torn down immediately after the claim discards the reaper, and the
    /// process must still have been signalled — a live orphaned CLI is a
    /// worse outcome than the zombie this work set out to remove.
    ///
    /// Unix-only because it reads the process state directly; the
    /// behaviour it guards is platform-independent.
    #[cfg(unix)]
    #[test]
    fn signal_is_delivered_even_when_the_reaper_never_runs() {
        let runtime = tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .expect("build runtime");
        let lifecycle = runtime.block_on(async { ChildLifecycle::new(Some(spawn_sleeper())) });
        let pid = lifecycle.pid().expect("sleeper should report a pid");

        // Claim inside the runtime, then discard the runtime before the
        // reaper can be polled.
        let _handle = runtime.block_on(async { lifecycle.begin_termination() });
        drop(runtime);
        std::thread::sleep(std::time::Duration::from_millis(300));

        let output = std::process::Command::new("ps")
            .args(["-o", "stat=", "-p", &pid.to_string()])
            .output()
            .expect("run ps");
        let state = String::from_utf8_lossy(&output.stdout).trim().to_string();
        assert!(
            state.is_empty() || state.starts_with('Z'),
            "the CLI process was left running instead of signalled (ps state {state:?})"
        );
    }

    /// A child that exits on its own after a beat, so a reaper polling it
    /// misses on the first `try_wait` and must fall through to the
    /// signal-driven path.
    fn spawn_slow_exiting() -> Child {
        let mut command = if cfg!(windows) {
            let mut command = Command::new("cmd");
            command.args(["/C", "ping -n 3 127.0.0.1 > nul"]);
            command
        } else {
            let mut command = Command::new("sleep");
            command.arg("1");
            command
        };
        command
            .stdin(Stdio::null())
            .stdout(Stdio::null())
            .stderr(Stdio::null())
            .spawn()
            .expect("spawn slow-exiting child")
    }

    /// Guards the `enable_all()` on the reaper's runtime, which is
    /// load-bearing rather than boilerplate.
    ///
    /// Every other test kills its child before the reaper runs, so the
    /// child is already a zombie and the first `try_wait` succeeds — the
    /// signal path is never touched, and the suite would pass even without
    /// a signal driver. This case waits on a child that is still running
    /// at the first poll, on a runtime other than the one that spawned it:
    /// the shape of the wedged process `force_stop` exists for.
    #[test]
    fn reap_completes_for_a_child_still_running_at_the_first_poll() {
        let spawner = tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .expect("build spawning runtime");
        let child = spawner.block_on(async { spawn_slow_exiting() });
        drop(spawner);

        let reaper = std::thread::spawn(move || {
            let runtime = tokio::runtime::Builder::new_current_thread()
                .enable_all()
                .build()
                .expect("build reaper runtime");
            runtime.block_on(reap(child))
        });

        match reaper.join().expect("reaper thread panicked") {
            ReapState::Reaped(Some(status)) => assert!(
                status.success(),
                "a child left to exit on its own should report success: {status:?}"
            ),
            other => panic!("expected a reaped child, got {other:?}"),
        }
    }

    /// Dropping a client that was never stopped must still reap its CLI
    /// process. Signalling alone would leave exactly the zombie this type
    /// exists to prevent.
    #[cfg(unix)]
    #[test]
    fn dropping_an_unstopped_lifecycle_reaps_the_child() {
        let runtime = tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .expect("build runtime");
        let lifecycle = runtime.block_on(async { ChildLifecycle::new(Some(spawn_sleeper())) });
        let pid = lifecycle.pid().expect("sleeper should report a pid");

        // No stop, no force stop, no handle — just drop it.
        drop(lifecycle);

        let deadline = std::time::Instant::now() + std::time::Duration::from_secs(5);
        loop {
            let output = std::process::Command::new("ps")
                .args(["-o", "stat=", "-p", &pid.to_string()])
                .output()
                .expect("run ps");
            let state = String::from_utf8_lossy(&output.stdout).trim().to_string();
            if state.is_empty() {
                return;
            }
            assert!(
                std::time::Instant::now() < deadline,
                "the CLI process was left unreaped after drop (ps state {state:?})"
            );
            std::thread::sleep(std::time::Duration::from_millis(50));
        }
    }

    #[test]
    fn forced_shutdown_handle_is_send_and_static() {
        fn assert_send_static<T: Send + 'static>() {}
        assert_send_static::<ForcedShutdown>();
    }
}
