//! Subscription handles for observing session and lifecycle events.
//!
//! Returned by [`Session::subscribe`](crate::session::Session::subscribe) and
//! [`Client::subscribe_lifecycle`](crate::Client::subscribe_lifecycle).
//!
//! Each subscription is an opt-in **observer** of events that are also
//! delivered to the per-event handlers installed on the session config
//! (see [`crate::handler`]). Subscribers receive a clone of every event but
//! cannot influence permission decisions, tool results, or any other event
//! whose handler return value affects the runtime.
//!
//! # Async iteration
//!
//! The subscription types implement [`tokio_stream::Stream`], so consumers
//! can use adapter combinators from [`tokio_stream::StreamExt`] or
//! `futures::StreamExt` (filtering, mapping, batching, racing with
//! `tokio::select!`, etc.) without learning the SDK's internal channel
//! choice. A simple `while let Ok(event) = sub.recv().await { ... }` loop
//! also works for callers who don't need the [`Stream`](tokio_stream::Stream)
//! surface.
//!
//! # Resume bootstrap and lag policy
//!
//! The first subscription on a resumed session may begin with a lossless,
//! ordered bootstrap prefix. Once its owner catches up, delivery switches
//! atomically to the bounded live stream. See
//! [`Session::subscribe`](crate::session::Session::subscribe) for the
//! unbounded retention, eager ownership, cleanup, and router limits.
//!
//! Each live subscriber maintains its own finite queue. If a consumer cannot
//! keep up, the oldest live events are dropped and the next call yields
//! [`Lagged`](crate::subscription::Lagged) reporting how many events were skipped.
//! Slow subscribers do not block the producer.

use std::collections::VecDeque;
use std::fmt;
use std::pin::Pin;
use std::sync::Arc;
use std::task::{Context, Poll};

use parking_lot::Mutex;
use tokio::sync::broadcast::{Receiver, Sender, WeakSender};
use tokio_stream::wrappers::BroadcastStream;
use tokio_stream::wrappers::errors::BroadcastStreamRecvError;
use tokio_stream::{Stream, StreamExt as _};

use crate::types::{SessionEvent, SessionLifecycleEvent};
use crate::{Custom, Repr};

/// The subscription fell behind the producer.
///
/// Reports the number of events that were dropped from this subscriber's
/// queue because the consumer didn't keep up. The subscription continues
/// after this error, starting from the next live event — callers who care
/// about lag should match on it and decide whether to resync, re-fetch, or
/// log and continue.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Lagged(pub(crate) u64);

impl Lagged {
    /// Number of events skipped before this consumer could read them.
    pub fn skipped(&self) -> u64 {
        self.0
    }
}

impl fmt::Display for Lagged {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "subscription lagged behind by {} events", self.0)
    }
}

impl std::error::Error for Lagged {}

/// Error kind for subscription receive operations.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
#[non_exhaustive]
pub enum RecvErrorKind {
    /// The producer is gone — the session has shut down or the client has
    /// stopped. No further events will be delivered.
    Closed,

    /// The subscriber fell behind. See [`Lagged`].
    Lagged(Lagged),
}

impl fmt::Display for RecvErrorKind {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            RecvErrorKind::Closed => write!(f, "subscription closed"),
            RecvErrorKind::Lagged(l) => write!(f, "{l}"),
        }
    }
}

/// Error returned by [`crate::subscription::EventSubscription::recv`] and
/// [`crate::subscription::LifecycleSubscription::recv`].
#[derive(Debug)]
pub struct RecvError {
    repr: Repr<RecvErrorKind>,
}

impl RecvError {
    /// The [`RecvErrorKind`] of this error.
    pub fn kind(&self) -> &RecvErrorKind {
        match &self.repr {
            Repr::Simple(k) | Repr::SimpleMessage(k, ..) | Repr::Custom(Custom { kind: k, .. }) => {
                k
            }
        }
    }
}

impl fmt::Display for RecvError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match &self.repr {
            Repr::Simple(k) => write!(f, "{k}"),
            Repr::SimpleMessage(_, m) => write!(f, "{m}"),
            Repr::Custom(Custom { error, .. }) => write!(f, "{error}"),
        }
    }
}

impl std::error::Error for RecvError {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match &self.repr {
            Repr::Custom(Custom { error, .. }) => Some(&**error),
            _ => None,
        }
    }
}

impl From<RecvErrorKind> for RecvError {
    fn from(kind: RecvErrorKind) -> Self {
        Self {
            repr: Repr::Simple(kind),
        }
    }
}

impl From<Lagged> for RecvError {
    fn from(lagged: Lagged) -> Self {
        Self::from(RecvErrorKind::Lagged(lagged))
    }
}

enum ResumeBootstrapState {
    Unclaimed(VecDeque<SessionEvent>),
    Claimed(VecDeque<SessionEvent>),
    Disabled,
}

/// Publication, ownership, and the empty-queue handoff share one lock so
/// the bootstrap owner observes an exact prefix without gaps or duplicates.
pub(crate) struct ResumeBootstrap {
    state: Mutex<ResumeBootstrapState>,
    live: WeakSender<SessionEvent>,
}

/// Releases only unclaimed events when the publishing task exits or unwinds.
pub(crate) struct ResumeBootstrapCleanup(Arc<ResumeBootstrap>);

impl Drop for ResumeBootstrapCleanup {
    fn drop(&mut self) {
        self.0.release_unclaimed();
    }
}

impl ResumeBootstrap {
    pub(crate) fn new(event_tx: &Sender<SessionEvent>) -> Arc<Self> {
        Arc::new(Self {
            state: Mutex::new(ResumeBootstrapState::Unclaimed(VecDeque::new())),
            live: event_tx.downgrade(),
        })
    }

    pub(crate) fn cleanup_guard(self: &Arc<Self>) -> ResumeBootstrapCleanup {
        ResumeBootstrapCleanup(self.clone())
    }

    pub(crate) fn publish(&self, event_tx: &Sender<SessionEvent>, event: SessionEvent) {
        let mut state = self.state.lock();
        match &mut *state {
            ResumeBootstrapState::Unclaimed(events) | ResumeBootstrapState::Claimed(events) => {
                events.push_back(event.clone());
            }
            ResumeBootstrapState::Disabled => {}
        }
        // Other observers stay live while the bootstrap owner catches up.
        let _ = event_tx.send(event);
    }

    pub(crate) fn subscribe(
        self: &Arc<Self>,
        event_tx: &Sender<SessionEvent>,
    ) -> EventSubscription {
        let mut state = self.state.lock();
        match &mut *state {
            ResumeBootstrapState::Unclaimed(events) => {
                let events = std::mem::take(events);
                *state = ResumeBootstrapState::Claimed(events);
                EventSubscription {
                    inner: None,
                    bootstrap: Some(self.clone()),
                }
            }
            ResumeBootstrapState::Claimed(_) | ResumeBootstrapState::Disabled => {
                EventSubscription::new(event_tx.subscribe())
            }
        }
    }

    fn pop(&self, live: &mut Option<BroadcastStream<SessionEvent>>) -> Option<SessionEvent> {
        let mut state = self.state.lock();
        let ResumeBootstrapState::Claimed(events) = &mut *state else {
            return None;
        };
        if let Some(event) = events.pop_front() {
            return Some(event);
        }
        // Subscribe under the publication lock, never replaying the broadcast
        // copy of an event already delivered from the bootstrap queue.
        *live = self
            .live
            .upgrade()
            .map(|sender| BroadcastStream::new(sender.subscribe()));
        *state = ResumeBootstrapState::Disabled;
        None
    }

    pub(crate) fn release_unclaimed(&self) {
        let mut state = self.state.lock();
        if matches!(*state, ResumeBootstrapState::Unclaimed(_)) {
            *state = ResumeBootstrapState::Disabled;
        }
    }

    fn abandon(&self) {
        let mut state = self.state.lock();
        if matches!(*state, ResumeBootstrapState::Claimed(_)) {
            *state = ResumeBootstrapState::Disabled;
        }
    }
}

/// Subscription to runtime events for a single
/// [`Session`](crate::session::Session).
///
/// Created by [`Session::subscribe`](crate::session::Session::subscribe).
/// Implements [`Stream`] yielding `Result<SessionEvent, Lagged>`.
/// Drop the value to unsubscribe; there is no separate cancel handle.
/// A resume bootstrap is claimed at construction, not on the first poll.
/// Dropping its owner discards any unread bootstrap events.
#[must_use = "dropping the subscription unsubscribes and discards any owned resume bootstrap backlog"]
pub struct EventSubscription {
    inner: Option<BroadcastStream<SessionEvent>>,
    bootstrap: Option<Arc<ResumeBootstrap>>,
}

impl EventSubscription {
    pub(crate) fn new(rx: Receiver<SessionEvent>) -> Self {
        Self {
            inner: Some(BroadcastStream::new(rx)),
            bootstrap: None,
        }
    }

    fn next_bootstrap_event(&mut self) -> Option<SessionEvent> {
        let event = self
            .bootstrap
            .as_ref()
            .and_then(|bootstrap| bootstrap.pop(&mut self.inner));
        if event.is_none() {
            self.bootstrap = None;
        }
        event
    }

    /// Receive the next event.
    ///
    /// Returns:
    ///
    /// - `Ok(event)` for the next delivered event.
    /// - [`RecvErrorKind::Lagged`] if live delivery fell behind; call again
    ///   to continue from the next available live event.
    /// - [`RecvErrorKind::Closed`] once the producer is gone and any retained
    ///   events have been drained.
    ///
    /// # Cancel safety
    ///
    /// **Cancel-safe.** Bootstrap events are removed before the future's
    /// first suspension point. Once live delivery begins, this wraps a
    /// cancel-safe `tokio::sync::broadcast::Receiver` via `BroadcastStream`.
    pub async fn recv(&mut self) -> Result<SessionEvent, RecvError> {
        match self.next().await {
            Some(Ok(event)) => Ok(event),
            Some(Err(lagged)) => Err(lagged.into()),
            None => Err(RecvErrorKind::Closed.into()),
        }
    }
}

impl Stream for EventSubscription {
    type Item = Result<SessionEvent, Lagged>;

    fn poll_next(mut self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Option<Self::Item>> {
        if let Some(event) = self.next_bootstrap_event() {
            return Poll::Ready(Some(Ok(event)));
        }
        let Some(inner) = self.inner.as_mut() else {
            return Poll::Ready(None);
        };
        match Pin::new(inner).poll_next(cx) {
            Poll::Ready(Some(Ok(event))) => Poll::Ready(Some(Ok(event))),
            Poll::Ready(Some(Err(BroadcastStreamRecvError::Lagged(n)))) => {
                Poll::Ready(Some(Err(Lagged(n))))
            }
            Poll::Ready(None) => Poll::Ready(None),
            Poll::Pending => Poll::Pending,
        }
    }
}

impl Drop for EventSubscription {
    fn drop(&mut self) {
        if let Some(bootstrap) = &self.bootstrap {
            bootstrap.abandon();
        }
    }
}

/// Subscription to lifecycle events on a [`Client`](crate::Client).
///
/// Created by [`Client::subscribe_lifecycle`](crate::Client::subscribe_lifecycle).
/// Implements [`Stream`] yielding `Result<SessionLifecycleEvent, Lagged>`.
/// Drop the value to unsubscribe; there is no separate cancel handle.
#[must_use = "dropping the subscription unsubscribes"]
pub struct LifecycleSubscription {
    inner: BroadcastStream<SessionLifecycleEvent>,
}

impl LifecycleSubscription {
    pub(crate) fn new(rx: Receiver<SessionLifecycleEvent>) -> Self {
        Self {
            inner: BroadcastStream::new(rx),
        }
    }

    /// Receive the next event.
    ///
    /// Returns:
    ///
    /// - `Ok(event)` for the next delivered event.
    /// - [`RecvErrorKind::Lagged`] if the subscriber fell behind; call again
    ///   to continue from the next available event.
    /// - [`RecvErrorKind::Closed`] once the producer is gone.
    ///
    /// # Cancel safety
    ///
    /// **Cancel-safe.** Wraps a `tokio::sync::broadcast::Receiver` via
    /// `BroadcastStream`; both are cancel-safe by design. Dropping the future
    /// before completion leaves buffered events available for the next call.
    pub async fn recv(&mut self) -> Result<SessionLifecycleEvent, RecvError> {
        match self.next().await {
            Some(Ok(event)) => Ok(event),
            Some(Err(lagged)) => Err(lagged.into()),
            None => Err(RecvErrorKind::Closed.into()),
        }
    }
}

impl Stream for LifecycleSubscription {
    type Item = Result<SessionLifecycleEvent, Lagged>;

    fn poll_next(mut self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Option<Self::Item>> {
        match Pin::new(&mut self.inner).poll_next(cx) {
            Poll::Ready(Some(Ok(event))) => Poll::Ready(Some(Ok(event))),
            Poll::Ready(Some(Err(BroadcastStreamRecvError::Lagged(n)))) => {
                Poll::Ready(Some(Err(Lagged(n))))
            }
            Poll::Ready(None) => Poll::Ready(None),
            Poll::Pending => Poll::Pending,
        }
    }
}

#[cfg(test)]
mod tests;
