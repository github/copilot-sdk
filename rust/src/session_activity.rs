//! Versioned session activity snapshots and ordering.

use serde::{Deserialize, Serialize};
use serde_json::Value;

use crate::generated::session_events::{SessionMainAgentState, SessionMainAgentWaitReason};
use crate::types::SessionEvent;
use crate::{Error, ErrorKind, ProtocolErrorKind};

const CONTRACT_VERSION: u8 = 1;
const MAX_JSON_SAFE_INTEGER: u64 = 9_007_199_254_740_991;

/// A complete version 1 session activity snapshot.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionActivitySnapshot {
    /// Activity contract version. Always `1` for this type.
    pub contract_version: u8,
    /// Opaque identifier for the current runtime incarnation of the session.
    pub activity_epoch: String,
    /// Monotonically increasing revision within `activity_epoch`.
    pub revision: u64,
    /// Legacy broad abortability flag.
    pub abortable: bool,
    /// Compatibility aggregate for executing agent work.
    pub has_active_work: bool,
    /// Current main-agent activity.
    pub main_agent: SessionActivityMainAgent,
    /// Current background-agent counts.
    pub background_agents: SessionActivityBackgroundAgents,
    /// Current live-process counts.
    pub processes: SessionActivityProcesses,
}

/// Main-agent activity in a complete version 1 snapshot.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionActivityMainAgent {
    /// Whether the main agent is working, waiting, or idle.
    pub state: SessionMainAgentState,
    /// Interactive condition blocking a waiting main agent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wait_reason: Option<SessionMainAgentWaitReason>,
    /// Whether the current main-agent turn can be interrupted.
    pub abortable: bool,
}

/// Background-agent activity in a complete version 1 snapshot.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionActivityBackgroundAgents {
    /// Agents currently executing.
    pub running: u64,
    /// Live multi-turn agents parked for another message.
    pub idle: u64,
    /// Agents accepted by scoped background-agent cancellation.
    pub cancelable: u64,
}

/// Process activity in a complete version 1 snapshot.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionActivityProcesses {
    /// Live shell processes visible to the session.
    pub running: u64,
    /// Live processes accepted by scoped process termination.
    pub terminable: u64,
}

/// Result of capability detection for session activity version 1.
#[derive(Debug, Clone, PartialEq, Eq)]
#[non_exhaustive]
pub enum SessionActivitySupport {
    /// The runtime returned a complete, valid version 1 snapshot.
    Supported(SessionActivitySnapshot),
    /// The runtime does not expose the version 1 contract.
    Unsupported(SessionActivityUnsupportedReason),
}

/// Why session activity version 1 is unavailable.
#[derive(Debug, Clone, PartialEq, Eq)]
#[non_exhaustive]
pub enum SessionActivityUnsupportedReason {
    /// The runtime does not implement `session.metadata.activity`.
    MethodUnavailable,
    /// The runtime returned the legacy two-boolean response.
    LegacyResponse,
    /// The runtime returned a contract version this SDK does not understand.
    UnknownContractVersion(Value),
}

/// Token identifying the reducer's current runtime connection.
///
/// Capture this token before starting the activity subscription and snapshot
/// query. A token becomes stale after
/// [`SessionActivityReducer::begin_connection`] is called.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct SessionActivityConnectionToken(u64);

/// Outcome of applying a snapshot to a [`SessionActivityReducer`].
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[non_exhaustive]
pub enum SessionActivityReduction {
    /// The first snapshot for the current connection was accepted.
    Accepted,
    /// A greater revision in the current epoch was accepted.
    Advanced,
    /// A snapshot from a new epoch on the current connection replaced state.
    ReplacedEpoch,
    /// The snapshot matched the current epoch and revision.
    Idempotent,
    /// A lower revision in the current epoch was ignored.
    StaleRevision,
    /// A result from a closed or superseded connection was ignored.
    StaleConnection,
}

/// Connection-scoped reducer for activity query results and change events.
#[derive(Debug)]
pub struct SessionActivityReducer {
    connection_generation: u64,
    current: Option<SessionActivitySnapshot>,
}

impl Default for SessionActivityReducer {
    fn default() -> Self {
        Self::new()
    }
}

impl SessionActivityReducer {
    /// Create a reducer for a new connection.
    pub fn new() -> Self {
        Self {
            connection_generation: 1,
            current: None,
        }
    }

    /// Return the token for the current connection.
    pub fn connection_token(&self) -> SessionActivityConnectionToken {
        SessionActivityConnectionToken(self.connection_generation)
    }

    /// Start a new connection, clearing state and invalidating earlier tokens.
    pub fn begin_connection(&mut self) -> SessionActivityConnectionToken {
        self.connection_generation = self
            .connection_generation
            .checked_add(1)
            .expect("session activity connection generation overflow");
        self.current = None;
        self.connection_token()
    }

    /// Return the last accepted snapshot for the current connection.
    pub fn current(&self) -> Option<&SessionActivitySnapshot> {
        self.current.as_ref()
    }

    /// Apply a query result or activity-changed event.
    pub fn apply(
        &mut self,
        connection: SessionActivityConnectionToken,
        snapshot: SessionActivitySnapshot,
    ) -> SessionActivityReduction {
        if connection != self.connection_token() {
            return SessionActivityReduction::StaleConnection;
        }

        let Some(current) = &self.current else {
            self.current = Some(snapshot);
            return SessionActivityReduction::Accepted;
        };

        if current.activity_epoch != snapshot.activity_epoch {
            self.current = Some(snapshot);
            return SessionActivityReduction::ReplacedEpoch;
        }

        match snapshot.revision.cmp(&current.revision) {
            std::cmp::Ordering::Greater => {
                self.current = Some(snapshot);
                SessionActivityReduction::Advanced
            }
            std::cmp::Ordering::Equal => SessionActivityReduction::Idempotent,
            std::cmp::Ordering::Less => SessionActivityReduction::StaleRevision,
        }
    }
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct VersionOneWire {
    contract_version: u8,
    activity_epoch: String,
    revision: u64,
    abortable: bool,
    has_active_work: bool,
    main_agent: SessionActivityMainAgent,
    background_agents: SessionActivityBackgroundAgents,
    processes: SessionActivityProcesses,
}

impl From<VersionOneWire> for SessionActivitySnapshot {
    fn from(wire: VersionOneWire) -> Self {
        Self {
            contract_version: wire.contract_version,
            activity_epoch: wire.activity_epoch,
            revision: wire.revision,
            abortable: wire.abortable,
            has_active_work: wire.has_active_work,
            main_agent: wire.main_agent,
            background_agents: wire.background_agents,
            processes: wire.processes,
        }
    }
}

pub(crate) fn classify_query(activity: Value) -> Result<SessionActivitySupport, Error> {
    classify_value(activity)
}

fn classify_event(activity: Value) -> Result<SessionActivitySupport, Error> {
    classify_value(activity)
}

fn classify_value(value: Value) -> Result<SessionActivitySupport, Error> {
    let Some(contract_version) = value.get("contractVersion") else {
        return Ok(SessionActivitySupport::Unsupported(
            SessionActivityUnsupportedReason::LegacyResponse,
        ));
    };

    if contract_version != CONTRACT_VERSION {
        return Ok(SessionActivitySupport::Unsupported(
            SessionActivityUnsupportedReason::UnknownContractVersion(contract_version.clone()),
        ));
    }

    let wire: VersionOneWire = serde_json::from_value(value).map_err(invalid_activity)?;
    validate_version_one(&wire)?;
    Ok(SessionActivitySupport::Supported(wire.into()))
}

fn validate_version_one(wire: &VersionOneWire) -> Result<(), Error> {
    if wire.contract_version != CONTRACT_VERSION {
        return Err(invalid_activity("contractVersion must be 1"));
    }
    if uuid::Uuid::parse_str(&wire.activity_epoch).is_err() {
        return Err(invalid_activity("activityEpoch must be a UUID string"));
    }
    if wire.revision == 0 || wire.revision > MAX_JSON_SAFE_INTEGER {
        return Err(invalid_activity(
            "revision must be a JSON safe integer greater than zero",
        ));
    }
    if wire.background_agents.running > MAX_JSON_SAFE_INTEGER
        || wire.background_agents.idle > MAX_JSON_SAFE_INTEGER
        || wire.background_agents.cancelable > MAX_JSON_SAFE_INTEGER
        || wire.processes.running > MAX_JSON_SAFE_INTEGER
        || wire.processes.terminable > MAX_JSON_SAFE_INTEGER
    {
        return Err(invalid_activity(
            "activity counts must be JSON safe integers",
        ));
    }
    if matches!(wire.main_agent.state, SessionMainAgentState::Unknown) {
        return Err(invalid_activity("unknown mainAgent.state in version 1"));
    }
    if matches!(
        wire.main_agent.wait_reason,
        Some(SessionMainAgentWaitReason::Unknown)
    ) {
        return Err(invalid_activity(
            "unknown mainAgent.waitReason in version 1",
        ));
    }
    if wire.main_agent.state != SessionMainAgentState::Waiting
        && wire.main_agent.wait_reason.is_some()
    {
        return Err(invalid_activity(
            "mainAgent.waitReason is valid only while waiting",
        ));
    }
    let expected_active_work = wire.main_agent.state == SessionMainAgentState::Working
        || wire.background_agents.running > 0;
    if wire.has_active_work != expected_active_work {
        return Err(invalid_activity(
            "hasActiveWork does not match agent execution state",
        ));
    }
    Ok(())
}

fn invalid_activity(message: impl std::fmt::Display) -> Error {
    Error::with_message(
        ErrorKind::Protocol(ProtocolErrorKind::InvalidSessionActivity),
        format!("invalid session activity version 1: {message}"),
    )
}

impl SessionEvent {
    /// Decode a `session.activity_changed` event using the versioned activity
    /// capability contract.
    ///
    /// Returns `Ok(None)` for events of any other type.
    pub fn session_activity(&self) -> Result<Option<SessionActivitySupport>, Error> {
        if self.event_type != "session.activity_changed" {
            return Ok(None);
        }
        classify_event(self.data.clone()).map(Some)
    }
}

#[cfg(test)]
mod tests {
    use serde_json::json;
    use tokio::sync::broadcast;

    use super::*;
    use crate::subscription::EventSubscription;

    const EPOCH: &str = "2a7d81bf-c012-48a7-b186-31b1374799ef";

    fn activity(revision: u64) -> Value {
        json!({
            "contractVersion": 1,
            "activityEpoch": EPOCH,
            "revision": revision,
            "abortable": false,
            "hasActiveWork": false,
            "mainAgent": {
                "state": "idle",
                "abortable": false
            },
            "backgroundAgents": {
                "running": 0,
                "idle": 1,
                "cancelable": 0
            },
            "processes": {
                "running": 1,
                "terminable": 1
            }
        })
    }

    fn snapshot(revision: u64) -> SessionActivitySnapshot {
        match classify_value(activity(revision)).unwrap() {
            SessionActivitySupport::Supported(snapshot) => snapshot,
            unsupported => panic!("expected supported activity, got {unsupported:?}"),
        }
    }

    #[test]
    fn decodes_and_serializes_exact_version_one_shape() {
        let snapshot = snapshot(7);
        assert_eq!(snapshot.revision, 7);
        assert_eq!(snapshot.main_agent.state, SessionMainAgentState::Idle);
        assert_eq!(snapshot.background_agents.idle, 1);
        assert_eq!(snapshot.processes.running, 1);
        assert_eq!(serde_json::to_value(snapshot).unwrap(), activity(7));
    }

    #[test]
    fn classifies_legacy_and_unknown_versions_as_unsupported() {
        assert_eq!(
            classify_value(json!({"abortable": false, "hasActiveWork": false})).unwrap(),
            SessionActivitySupport::Unsupported(SessionActivityUnsupportedReason::LegacyResponse)
        );
        assert_eq!(
            classify_value(json!({
                "contractVersion": 2,
                "abortable": false,
                "hasActiveWork": false
            }))
            .unwrap(),
            SessionActivitySupport::Unsupported(
                SessionActivityUnsupportedReason::UnknownContractVersion(json!(2))
            )
        );
    }

    #[test]
    fn rejects_incomplete_or_semantically_invalid_version_one() {
        let mut incomplete = activity(1);
        incomplete.as_object_mut().unwrap().remove("mainAgent");
        assert!(classify_value(incomplete).is_err());

        let mut inconsistent = activity(1);
        inconsistent["hasActiveWork"] = json!(true);
        assert!(classify_value(inconsistent).is_err());

        let mut out_of_range = activity(1);
        out_of_range["revision"] = json!(MAX_JSON_SAFE_INTEGER + 1);
        assert!(classify_value(out_of_range).is_err());
    }

    #[test]
    fn reducer_orders_revisions_and_epochs() {
        let mut reducer = SessionActivityReducer::new();
        let token = reducer.connection_token();
        assert_eq!(
            reducer.apply(token, snapshot(7)),
            SessionActivityReduction::Accepted
        );
        assert_eq!(
            reducer.apply(token, snapshot(7)),
            SessionActivityReduction::Idempotent
        );
        assert_eq!(
            reducer.apply(token, snapshot(6)),
            SessionActivityReduction::StaleRevision
        );
        assert_eq!(
            reducer.apply(token, snapshot(8)),
            SessionActivityReduction::Advanced
        );

        let mut restarted = activity(1);
        restarted["activityEpoch"] = json!("e8ba82d0-812a-42d7-bcb1-b8ba7739f956");
        let SessionActivitySupport::Supported(restarted) = classify_value(restarted).unwrap()
        else {
            panic!("expected supported snapshot");
        };
        assert_eq!(
            reducer.apply(token, restarted),
            SessionActivityReduction::ReplacedEpoch
        );
        assert_eq!(reducer.current().unwrap().revision, 1);
    }

    #[test]
    fn reducer_rejects_delayed_results_from_superseded_connections() {
        let mut reducer = SessionActivityReducer::new();
        let old_connection = reducer.connection_token();
        let current_connection = reducer.begin_connection();
        assert_eq!(
            reducer.apply(current_connection, snapshot(9)),
            SessionActivityReduction::Accepted
        );
        assert_eq!(
            reducer.apply(old_connection, snapshot(10)),
            SessionActivityReduction::StaleConnection
        );
        assert_eq!(reducer.current().unwrap().revision, 9);
    }

    #[test]
    fn delayed_snapshot_does_not_overwrite_newer_event() {
        let mut reducer = SessionActivityReducer::new();
        let token = reducer.connection_token();
        assert_eq!(
            reducer.apply(token, snapshot(8)),
            SessionActivityReduction::Accepted
        );
        assert_eq!(
            reducer.apply(token, snapshot(7)),
            SessionActivityReduction::StaleRevision
        );
        assert_eq!(reducer.current().unwrap().revision, 8);
    }

    #[test]
    fn session_event_uses_the_same_snapshot_type() {
        let event = SessionEvent {
            id: "event-1".to_string(),
            timestamp: "2026-09-10T00:00:00Z".to_string(),
            parent_id: None,
            ephemeral: Some(true),
            agent_id: None,
            debug_cli_received_at_ms: None,
            debug_ws_forwarded_at_ms: None,
            event_type: "session.activity_changed".to_string(),
            data: activity(3),
        };

        let support = event.session_activity().unwrap().unwrap();
        let SessionActivitySupport::Supported(actual) = support else {
            panic!("expected supported snapshot");
        };
        assert_eq!(actual, snapshot(3));
    }

    #[tokio::test]
    async fn activity_event_is_delivered_through_event_subscription() {
        let (sender, receiver) = broadcast::channel(1);
        let mut subscription = EventSubscription::new(receiver);
        sender
            .send(SessionEvent {
                id: "event-1".to_string(),
                timestamp: "2026-09-10T00:00:00Z".to_string(),
                parent_id: None,
                ephemeral: Some(true),
                agent_id: None,
                debug_cli_received_at_ms: None,
                debug_ws_forwarded_at_ms: None,
                event_type: "session.activity_changed".to_string(),
                data: activity(4),
            })
            .unwrap();

        let support = subscription
            .recv()
            .await
            .unwrap()
            .session_activity()
            .unwrap()
            .unwrap();
        let SessionActivitySupport::Supported(actual) = support else {
            panic!("expected supported snapshot");
        };
        assert_eq!(actual, snapshot(4));
    }

    #[test]
    fn non_activity_event_is_ignored() {
        let event = SessionEvent {
            id: "event-1".to_string(),
            timestamp: "2026-09-10T00:00:00Z".to_string(),
            parent_id: None,
            ephemeral: None,
            agent_id: None,
            debug_cli_received_at_ms: None,
            debug_ws_forwarded_at_ms: None,
            event_type: "session.idle".to_string(),
            data: json!({}),
        };

        assert_eq!(event.session_activity().unwrap(), None);
    }

    #[test]
    fn unknown_event_contract_does_not_require_version_one_fields() {
        let event = SessionEvent {
            id: "event-1".to_string(),
            timestamp: "2026-09-10T00:00:00Z".to_string(),
            parent_id: None,
            ephemeral: Some(true),
            agent_id: None,
            debug_cli_received_at_ms: None,
            debug_ws_forwarded_at_ms: None,
            event_type: "session.activity_changed".to_string(),
            data: json!({
                "contractVersion": 2,
                "replacementShape": true
            }),
        };

        assert_eq!(
            event.session_activity().unwrap(),
            Some(SessionActivitySupport::Unsupported(
                SessionActivityUnsupportedReason::UnknownContractVersion(json!(2))
            ))
        );
    }
}
