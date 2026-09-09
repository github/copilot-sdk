//! App-level executable-extension badge protocol.
//!
//! The app owns hidden-session lifecycle, eligible-session filtering, and
//! native GitHub badge precedence. This module only exposes typed v1 transport
//! wrappers and provider-attributed event decoding.

use std::fmt;
use std::path::PathBuf;

use serde::{Deserialize, Serialize};

use crate::session::Session;
use crate::subscription::{EventSubscription, RecvError};
use crate::types::{SessionEvent, SessionId};
use crate::{Client, Error, ErrorKind};

const PROTOCOL_VERSION: u8 = 1;
const UPDATE_SNAPSHOT_METHOD: &str = "extensions.appSessionBadges.updateSnapshot";
const BADGE_CHANGED_EVENT: &str = "session.extensions.app_session_badge_changed";
const PRESENTATION_CHANGED_EVENT: &str = "session.extensions.app_session_presentation_changed";

/// Constrained visual state for an extension-provided workspace badge.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum AppSessionBadgeState {
    /// Draft pull request semantics.
    Draft,
    /// Open pull request semantics.
    Open,
    /// Merged pull request semantics.
    Merged,
    /// Closed pull request semantics.
    Closed,
}

/// Constrained badge presentation supplied by an executable extension.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AppSessionBadge {
    /// Host-defined icon semantics.
    pub state: AppSessionBadgeState,
    /// Optional text label displayed with the constrained state.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub label: Option<String>,
}

/// Availability state for a contributed Create Pull Request action.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub enum AppSessionPullRequestActionState {
    /// The action can be selected.
    Available,
    /// The extension is currently handling the action.
    InProgress,
}

/// Constrained Create Pull Request action presentation.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AppSessionPullRequestAction {
    kind: AppSessionPullRequestActionKind,
    /// Current action availability.
    pub state: AppSessionPullRequestActionState,
    /// Whether the extension supports a draft choice.
    pub supports_draft: bool,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
enum AppSessionPullRequestActionKind {
    CreatePullRequest,
}

impl AppSessionPullRequestAction {
    /// Create a Create Pull Request action presentation.
    pub fn new(state: AppSessionPullRequestActionState, supports_draft: bool) -> Self {
        Self {
            kind: AppSessionPullRequestActionKind::CreatePullRequest,
            state,
            supports_draft,
        }
    }
}

/// Atomic badge and Create Pull Request action presentation.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AppSessionPresentation {
    /// Constrained badge, or `None` to clear only the badge.
    pub badge: Option<AppSessionBadge>,
    /// Create Pull Request action, or `None` to clear only the action.
    pub action: Option<AppSessionPullRequestAction>,
}

impl AppSessionBadge {
    /// Create a badge with no text label.
    pub fn new(state: AppSessionBadgeState) -> Self {
        Self { state, label: None }
    }

    /// Set the optional badge label.
    pub fn with_label(mut self, label: impl Into<String>) -> Self {
        self.label = Some(label.into());
        self
    }
}

/// Stable identity and paths for an app-visible badge target.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AppSessionBadgeTarget {
    /// Stable app workspace/sidebar identity.
    pub workspace_id: String,
    /// Active app session linked to the workspace.
    pub session_id: String,
    /// Repository root path known to the app.
    pub repository_path: PathBuf,
    /// Worktree path for the active workspace.
    pub worktree_path: PathBuf,
    /// Current branch name, when known.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
}

impl AppSessionBadgeTarget {
    /// Create an eligible app-visible badge target.
    pub fn new(
        workspace_id: impl Into<String>,
        session_id: impl Into<String>,
        repository_path: impl Into<PathBuf>,
        worktree_path: impl Into<PathBuf>,
    ) -> Self {
        Self {
            workspace_id: workspace_id.into(),
            session_id: session_id.into(),
            repository_path: repository_path.into(),
            worktree_path: worktree_path.into(),
            branch: None,
        }
    }

    /// Set the current branch name.
    pub fn with_branch(mut self, branch: impl Into<String>) -> Self {
        self.branch = Some(branch.into());
        self
    }
}

/// Full replacement snapshot of sessions eligible for extension-provided badges.
#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AppSessionBadgesSnapshot {
    protocol_version: u8,
    /// Monotonically increasing app-owned snapshot revision.
    pub revision: u64,
    /// Complete eligible-session replacement set.
    pub sessions: Vec<AppSessionBadgeTarget>,
}

impl AppSessionBadgesSnapshot {
    /// Create a v1 full replacement snapshot.
    pub fn new(revision: u64, sessions: Vec<AppSessionBadgeTarget>) -> Self {
        Self {
            protocol_version: PROTOCOL_VERSION,
            revision,
            sessions,
        }
    }
}

/// Provider-attributed badge change emitted on the retained hidden session.
#[derive(Debug, Clone, PartialEq, Eq, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AppSessionBadgeChanged {
    protocol_version: u8,
    /// Stable runtime extension identity that published the change.
    pub extension_id: String,
    /// Stable app workspace/sidebar identity.
    pub workspace_id: String,
    /// Active app session linked to the workspace.
    pub session_id: String,
    /// New badge, or `None` when the provider state was cleared.
    pub badge: Option<AppSessionBadge>,
}

/// Authenticated provider presentation update emitted on the retained hidden session.
#[derive(Debug, Clone, PartialEq, Eq, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AppSessionPresentationChanged {
    protocol_version: u8,
    /// Legacy runtime extension identity retained for compatibility.
    pub extension_id: String,
    /// Runtime-authenticated package identity.
    pub package_id: String,
    /// Runtime-authenticated activation identity.
    pub activation_id: String,
    /// Runtime-authenticated contribution identity.
    pub contribution_id: String,
    /// Stable app workspace/sidebar identity.
    pub workspace_id: String,
    /// Active app session linked to the workspace.
    pub session_id: String,
    /// New presentation, or `None` when provider lifecycle state was reset.
    pub presentation: Option<AppSessionPresentation>,
}

impl AppSessionPresentationChanged {
    /// Protocol version carried by the event.
    pub fn protocol_version(&self) -> u8 {
        self.protocol_version
    }
}

impl AppSessionBadgeChanged {
    /// Protocol version carried by the event.
    pub fn protocol_version(&self) -> u8 {
        self.protocol_version
    }
}

/// Error returned while decoding an app-session badge event.
#[derive(Debug)]
pub struct AppSessionBadgeDecodeError {
    message: String,
}

impl fmt::Display for AppSessionBadgeDecodeError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.message)
    }
}

impl std::error::Error for AppSessionBadgeDecodeError {}

/// Decode a generic hidden-session event when it is an app-session badge change.
pub fn decode_app_session_badge_changed(
    event: &SessionEvent,
) -> Result<Option<AppSessionBadgeChanged>, AppSessionBadgeDecodeError> {
    if event.event_type != BADGE_CHANGED_EVENT {
        return Ok(None);
    }
    let changed: AppSessionBadgeChanged =
        serde_json::from_value(event.data.clone()).map_err(|error| AppSessionBadgeDecodeError {
            message: format!("invalid {BADGE_CHANGED_EVENT} payload: {error}"),
        })?;
    if changed.protocol_version != PROTOCOL_VERSION {
        return Err(AppSessionBadgeDecodeError {
            message: format!(
                "unsupported app session badges protocol version: {}",
                changed.protocol_version
            ),
        });
    }
    validate_non_empty(&changed.extension_id, "extensionId")
        .and_then(|_| validate_non_empty(&changed.workspace_id, "workspaceId"))
        .and_then(|_| validate_non_empty(&changed.session_id, "sessionId"))
        .map_err(|error| AppSessionBadgeDecodeError {
            message: error.to_string(),
        })?;
    Ok(Some(changed))
}

/// Decode a generic hidden-session event when it is an app-session presentation change.
pub fn decode_app_session_presentation_changed(
    event: &SessionEvent,
) -> Result<Option<AppSessionPresentationChanged>, AppSessionBadgeDecodeError> {
    if event.event_type != PRESENTATION_CHANGED_EVENT {
        return Ok(None);
    }
    let changed: AppSessionPresentationChanged = serde_json::from_value(event.data.clone())
        .map_err(|error| AppSessionBadgeDecodeError {
            message: format!("invalid {PRESENTATION_CHANGED_EVENT} payload: {error}"),
        })?;
    if changed.protocol_version != PROTOCOL_VERSION {
        return Err(AppSessionBadgeDecodeError {
            message: format!(
                "unsupported app session badges protocol version: {}",
                changed.protocol_version
            ),
        });
    }
    for (value, name) in [
        (&changed.extension_id, "extensionId"),
        (&changed.package_id, "packageId"),
        (&changed.activation_id, "activationId"),
        (&changed.contribution_id, "contributionId"),
        (&changed.workspace_id, "workspaceId"),
        (&changed.session_id, "sessionId"),
    ] {
        validate_non_empty(value, name).map_err(|error| AppSessionBadgeDecodeError {
            message: error.to_string(),
        })?;
    }
    if changed
        .presentation
        .as_ref()
        .and_then(|presentation| presentation.badge.as_ref())
        .and_then(|badge| badge.label.as_ref())
        .is_some_and(|label| label.len() > 512)
    {
        return Err(AppSessionBadgeDecodeError {
            message: "badge.label must be at most 512 bytes".to_string(),
        });
    }
    Ok(Some(changed))
}

/// Receive error for a typed app-session badge event subscription.
#[derive(Debug)]
pub enum AppSessionBadgeSubscriptionError {
    /// The underlying hidden-session subscription closed or lagged.
    Receive(RecvError),
    /// A matching event carried an invalid v1 payload.
    Decode(AppSessionBadgeDecodeError),
}

impl fmt::Display for AppSessionBadgeSubscriptionError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Receive(error) => error.fmt(f),
            Self::Decode(error) => error.fmt(f),
        }
    }
}

impl std::error::Error for AppSessionBadgeSubscriptionError {}

/// Typed subscription that skips unrelated hidden-session events.
pub struct AppSessionBadgeSubscription {
    inner: EventSubscription,
}

/// Typed subscription for authenticated app-session presentation updates.
pub struct AppSessionPresentationSubscription {
    inner: EventSubscription,
}

impl AppSessionPresentationSubscription {
    /// Receive the next authenticated presentation update.
    pub async fn recv(
        &mut self,
    ) -> Result<AppSessionPresentationChanged, AppSessionBadgeSubscriptionError> {
        loop {
            let event = self
                .inner
                .recv()
                .await
                .map_err(AppSessionBadgeSubscriptionError::Receive)?;
            match decode_app_session_presentation_changed(&event)
                .map_err(AppSessionBadgeSubscriptionError::Decode)?
            {
                Some(changed) => return Ok(changed),
                None => continue,
            }
        }
    }
}

impl AppSessionBadgeSubscription {
    /// Receive the next provider-attributed badge change.
    pub async fn recv(
        &mut self,
    ) -> Result<AppSessionBadgeChanged, AppSessionBadgeSubscriptionError> {
        loop {
            let event = self
                .inner
                .recv()
                .await
                .map_err(AppSessionBadgeSubscriptionError::Receive)?;
            match decode_app_session_badge_changed(&event)
                .map_err(AppSessionBadgeSubscriptionError::Decode)?
            {
                Some(changed) => return Ok(changed),
                None => continue,
            }
        }
    }
}

/// Host-side controller for publishing app-curated eligible-session snapshots.
#[derive(Clone)]
pub struct AppSessionBadgesHost {
    client: Client,
    app_session_id: SessionId,
}

impl AppSessionBadgesHost {
    /// Replace the runtime's complete eligible-session snapshot.
    pub async fn update_snapshot(&self, snapshot: AppSessionBadgesSnapshot) -> Result<(), Error> {
        validate_snapshot(&snapshot)?;
        let params = snapshot_params(&self.app_session_id, snapshot)?;
        self.client
            .call(UPDATE_SNAPSHOT_METHOD, Some(params))
            .await?;
        Ok(())
    }
}

impl Session {
    /// Create a host controller for snapshots associated with this retained session.
    pub fn app_session_badges(&self) -> AppSessionBadgesHost {
        AppSessionBadgesHost {
            client: self.client().clone(),
            app_session_id: self.id().clone(),
        }
    }

    /// Subscribe to provider-attributed badge changes on this hidden session.
    pub fn subscribe_app_session_badges(&self) -> AppSessionBadgeSubscription {
        AppSessionBadgeSubscription {
            inner: self.subscribe(),
        }
    }

    /// Subscribe to authenticated provider presentation updates on this hidden session.
    pub fn subscribe_app_session_presentations(&self) -> AppSessionPresentationSubscription {
        AppSessionPresentationSubscription {
            inner: self.subscribe(),
        }
    }
}

fn validate_snapshot(snapshot: &AppSessionBadgesSnapshot) -> Result<(), Error> {
    let mut target_ids = std::collections::HashSet::new();
    for target in &snapshot.sessions {
        validate_non_empty(&target.workspace_id, "workspaceId")?;
        validate_non_empty(&target.session_id, "sessionId")?;
        if target.repository_path.as_os_str().is_empty() {
            return Err(invalid_config(
                "repositoryPath must be a non-empty path".to_string(),
            ));
        }
        if target.worktree_path.as_os_str().is_empty() {
            return Err(invalid_config(
                "worktreePath must be a non-empty path".to_string(),
            ));
        }
        if !target_ids.insert((&target.workspace_id, &target.session_id)) {
            return Err(invalid_config(format!(
                "duplicate app session badge target: {}/{}",
                target.workspace_id, target.session_id
            )));
        }
    }
    Ok(())
}

fn snapshot_params(
    app_session_id: &SessionId,
    snapshot: AppSessionBadgesSnapshot,
) -> Result<serde_json::Value, Error> {
    let mut params = serde_json::to_value(snapshot)?;
    params["appSessionId"] = serde_json::to_value(app_session_id)?;
    Ok(params)
}

fn validate_non_empty(value: &str, name: &str) -> Result<(), Error> {
    if value.is_empty() {
        return Err(invalid_config(format!("{name} must be a non-empty string")));
    }
    Ok(())
}

fn invalid_config(message: String) -> Error {
    Error::with_message(ErrorKind::InvalidConfig, message)
}

#[cfg(test)]
mod tests {
    use std::path::PathBuf;

    use serde_json::{Value, json};
    use tokio::io::{AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt, duplex};

    use super::*;

    async fn read_framed(reader: &mut (impl AsyncRead + Unpin)) -> Value {
        let mut header = String::new();
        loop {
            let mut byte = [0u8; 1];
            reader.read_exact(&mut byte).await.unwrap();
            header.push(byte[0] as char);
            if header.ends_with("\r\n\r\n") {
                break;
            }
        }
        let length = header
            .trim()
            .strip_prefix("Content-Length: ")
            .unwrap()
            .parse()
            .unwrap();
        let mut body = vec![0; length];
        reader.read_exact(&mut body).await.unwrap();
        serde_json::from_slice(&body).unwrap()
    }

    async fn write_framed(writer: &mut (impl AsyncWrite + Unpin), value: &Value) {
        let body = serde_json::to_vec(value).unwrap();
        writer
            .write_all(format!("Content-Length: {}\r\n\r\n", body.len()).as_bytes())
            .await
            .unwrap();
        writer.write_all(&body).await.unwrap();
        writer.flush().await.unwrap();
    }

    #[test]
    fn snapshot_serializes_as_the_exact_v1_wire_shape() {
        let snapshot = AppSessionBadgesSnapshot::new(
            7,
            vec![
                AppSessionBadgeTarget::new(
                    "workspace-1",
                    "session-1",
                    r"C:\src\repo",
                    r"C:\src\worktree",
                )
                .with_branch("feature"),
            ],
        );

        assert_eq!(
            serde_json::to_value(snapshot).unwrap(),
            json!({
                "protocolVersion": 1,
                "revision": 7,
                "sessions": [{
                    "workspaceId": "workspace-1",
                    "sessionId": "session-1",
                    "repositoryPath": r"C:\src\repo",
                    "worktreePath": r"C:\src\worktree",
                    "branch": "feature"
                }]
            })
        );
    }

    #[tokio::test]
    async fn host_sends_the_exact_snapshot_rpc() {
        let (client_write, mut server_read) = duplex(8192);
        let (mut server_write, client_read) = duplex(8192);
        let client =
            Client::from_streams(client_read, client_write, PathBuf::from(r"C:\src")).unwrap();
        let server = tokio::spawn(async move {
            let request = read_framed(&mut server_read).await;
            assert_eq!(
                request,
                json!({
                    "jsonrpc": "2.0",
                    "id": 1,
                    "method": "extensions.appSessionBadges.updateSnapshot",
                    "params": {
                        "appSessionId": "hidden-session",
                        "protocolVersion": 1,
                        "revision": 2,
                        "sessions": []
                    }
                })
            );
            write_framed(
                &mut server_write,
                &json!({"jsonrpc": "2.0", "id": request["id"], "result": null}),
            )
            .await;
        });

        AppSessionBadgesHost {
            client,
            app_session_id: SessionId::new("hidden-session"),
        }
        .update_snapshot(AppSessionBadgesSnapshot::new(2, Vec::new()))
        .await
        .unwrap();
        server.await.unwrap();
    }

    #[test]
    fn decoder_accepts_badges_and_null_clears() {
        let event = session_event(json!({
            "protocolVersion": 1,
            "extensionId": "project:badges",
            "workspaceId": "workspace-1",
            "sessionId": "session-1",
            "badge": {
                "state": "merged",
                "label": "Merged"
            }
        }));
        let changed = decode_app_session_badge_changed(&event).unwrap().unwrap();
        assert_eq!(changed.protocol_version(), 1);
        assert_eq!(changed.extension_id, "project:badges");
        assert_eq!(
            changed.badge,
            Some(AppSessionBadge::new(AppSessionBadgeState::Merged).with_label("Merged"))
        );

        let clear = session_event(json!({
            "protocolVersion": 1,
            "extensionId": "project:badges",
            "workspaceId": "workspace-1",
            "sessionId": "session-1",
            "badge": null
        }));
        assert_eq!(
            decode_app_session_badge_changed(&clear)
                .unwrap()
                .unwrap()
                .badge,
            None
        );
    }

    #[test]
    fn presentation_decoder_preserves_authenticated_identity_and_reset() {
        let mut event = session_event(json!({
            "protocolVersion": 1,
            "extensionId": "project:badges",
            "packageId": "package",
            "activationId": "activation-7",
            "contributionId": "github-pr",
            "workspaceId": "workspace-1",
            "sessionId": "session-1",
            "presentation": {
                "badge": {"state": "draft", "label": "Draft"},
                "action": {
                    "kind": "createPullRequest",
                    "state": "available",
                    "supportsDraft": true
                }
            }
        }));
        event.event_type = PRESENTATION_CHANGED_EVENT.to_string();
        let changed = decode_app_session_presentation_changed(&event)
            .unwrap()
            .unwrap();
        assert_eq!(changed.protocol_version(), 1);
        assert_eq!(changed.package_id, "package");
        assert_eq!(changed.activation_id, "activation-7");
        assert_eq!(changed.contribution_id, "github-pr");
        assert_eq!(
            changed.presentation,
            Some(AppSessionPresentation {
                badge: Some(AppSessionBadge::new(AppSessionBadgeState::Draft).with_label("Draft")),
                action: Some(AppSessionPullRequestAction::new(
                    AppSessionPullRequestActionState::Available,
                    true,
                )),
            })
        );

        event.data["presentation"] = Value::Null;
        assert_eq!(
            decode_app_session_presentation_changed(&event)
                .unwrap()
                .unwrap()
                .presentation,
            None
        );
    }

    #[test]
    fn decoder_skips_other_events_and_rejects_other_versions() {
        let mut event = session_event(json!({}));
        event.event_type = "session.idle".to_string();
        assert!(decode_app_session_badge_changed(&event).unwrap().is_none());

        let invalid = session_event(json!({
            "protocolVersion": 2,
            "extensionId": "project:badges",
            "workspaceId": "workspace-1",
            "sessionId": "session-1",
            "badge": null
        }));
        assert!(
            decode_app_session_badge_changed(&invalid)
                .unwrap_err()
                .to_string()
                .contains("unsupported app session badges protocol version")
        );
    }

    fn session_event(data: Value) -> SessionEvent {
        SessionEvent {
            id: "event-1".to_string(),
            timestamp: "2026-09-04T00:00:00Z".to_string(),
            parent_id: None,
            ephemeral: Some(true),
            agent_id: None,
            debug_cli_received_at_ms: None,
            debug_ws_forwarded_at_ms: None,
            event_type: BADGE_CHANGED_EVENT.to_string(),
            data,
        }
    }
}
