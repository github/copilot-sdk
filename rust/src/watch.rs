use std::pin::Pin;
use std::sync::Arc;
use std::task::{Context, Poll};

use parking_lot::Mutex as ParkingLotMutex;
use tokio::sync::mpsc;
use tokio_stream::Stream;

use crate::generated::api_types::{
    ConnectedRemoteSessionMetadata, SessionsCloseRequest, WatchSharedSessionParams,
    WatchSharedSessionResult, rpc_methods,
};
use crate::router::SessionChannels;
use crate::types::{SessionEvent, SessionEventNotification, SessionId};
use crate::{Client, Error, ErrorKind};

/// Passive, read-only attachment to a session shared with the authenticated user.
///
/// History and live updates are available through [`events`](Self::events).
/// Interactive session operations are intentionally absent from this type.
/// Call [`close`](Self::close) before dropping the handle; client shutdown also
/// closes any watch that remains registered.
pub struct SharedSessionWatch {
    session_id: SessionId,
    metadata: ConnectedRemoteSessionMetadata,
    client: Client,
    events: SharedSessionWatchEvents,
    closed: tokio::sync::Mutex<bool>,
}

impl SharedSessionWatch {
    pub(crate) fn new(
        client: Client,
        session_id: SessionId,
        metadata: ConnectedRemoteSessionMetadata,
        channels: SessionChannels,
    ) -> Self {
        let SessionChannels {
            notifications,
            requests: _,
        } = channels;
        Self {
            session_id,
            metadata,
            client,
            events: SharedSessionWatchEvents { notifications },
            closed: tokio::sync::Mutex::new(false),
        }
    }

    /// SDK session ID assigned to this watch.
    pub fn session_id(&self) -> &SessionId {
        &self.session_id
    }

    /// Metadata for the watched shared session.
    pub fn metadata(&self) -> &ConnectedRemoteSessionMetadata {
        &self.metadata
    }

    /// Whether this attachment is read-only.
    pub const fn is_read_only(&self) -> bool {
        true
    }

    /// Ordered canonical history and live events for the watched session.
    ///
    /// The receiver is registered before the watch response is delivered, so
    /// replay events remain buffered until the caller begins consuming them.
    pub fn events(&mut self) -> &mut SharedSessionWatchEvents {
        &mut self.events
    }

    /// Close the watch and release local event routing.
    ///
    /// Repeated calls are idempotent.
    pub async fn close(&self) -> Result<(), Error> {
        let mut closed = self.closed.lock().await;
        if *closed {
            return Ok(());
        }

        let result = self
            .client
            .rpc()
            .sessions()
            .close(SessionsCloseRequest {
                session_id: self.session_id.clone(),
            })
            .await
            .map(|_| ());
        self.client.unregister_watch_session(&self.session_id);
        *closed = true;
        result
    }
}

/// Event stream retained by a [`SharedSessionWatch`].
pub struct SharedSessionWatchEvents {
    notifications: mpsc::UnboundedReceiver<SessionEventNotification>,
}

impl SharedSessionWatchEvents {
    /// Receive the next canonical session event.
    ///
    /// Returns `None` after the watch is closed or the client disconnects.
    pub async fn recv(&mut self) -> Option<SessionEvent> {
        self.notifications
            .recv()
            .await
            .map(|notification| notification.event)
    }
}

impl Stream for SharedSessionWatchEvents {
    type Item = SessionEvent;

    fn poll_next(mut self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Option<Self::Item>> {
        Pin::new(&mut self.notifications)
            .poll_recv(cx)
            .map(|notification| notification.map(|notification| notification.event))
    }
}

impl Client {
    /// Watch a session shared with the authenticated user.
    ///
    /// The runtime derives viewer identity, authorization and lane routing from
    /// the existing authenticated connection. The returned handle exposes no
    /// send, steer, prompt, permission, configuration or cancellation methods.
    /// Subscribe with [`Client::subscribe_lifecycle`] before calling this method
    /// if terminal connection loss must not be missed.
    pub async fn watch_shared_session(
        &self,
        session_id: impl Into<SessionId>,
    ) -> Result<SharedSessionWatch, Error> {
        let params = WatchSharedSessionParams {
            session_id: session_id.into(),
        };
        let wire_params = serde_json::to_value(params)?;
        let registration = Arc::new(ParkingLotMutex::new(None));
        let registration_for_callback = registration.clone();
        let client = self.clone();

        let value = self
            .call_with_inline_callback(
                rpc_methods::SESSIONS_WATCH,
                Some(wire_params),
                Some(Box::new(move |response| {
                    let value = response.result.as_ref().ok_or_else(|| {
                        Error::with_message(
                            ErrorKind::Rpc { code: -32603 },
                            "sessions.watch response did not include a result",
                        )
                    })?;
                    let result: WatchSharedSessionResult = serde_json::from_value(value.clone())?;
                    let channels = client.register_watch_session(&result.session_id);
                    *registration_for_callback.lock() = Some(channels);
                    Ok(())
                })),
            )
            .await?;
        let result: WatchSharedSessionResult = serde_json::from_value(value)?;

        if !result.read_only {
            let _ = self
                .rpc()
                .sessions()
                .close(SessionsCloseRequest {
                    session_id: result.session_id.clone(),
                })
                .await;
            self.unregister_watch_session(&result.session_id);
            return Err(Error::with_message(
                ErrorKind::Rpc { code: -32603 },
                "runtime returned an interactive shared-session watch",
            ));
        }

        let channels = registration.lock().take().ok_or_else(|| {
            Error::with_message(
                ErrorKind::Rpc { code: -32603 },
                "sessions.watch response was not registered for event routing",
            )
        })?;
        Ok(SharedSessionWatch::new(
            self.clone(),
            result.session_id,
            result.metadata,
            channels,
        ))
    }
}
