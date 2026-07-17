//! GitHub Copilot App v1 first-response performance adapter.
//!
//! The executable is intentionally driven only through `describe` and `run`
//! so an orchestrator can use it without interactive input.

use std::collections::{BTreeMap, HashSet, VecDeque};
use std::env;
use std::fs;
use std::io::{self, Read as _, Write as _};
use std::path::{Component, Path, PathBuf};
use std::sync::Arc;
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};

use async_trait::async_trait;
use github_copilot_sdk::session::Session;
use github_copilot_sdk::session_events::{
    AssistantMessageData, AssistantMessageDeltaData, SessionEventType,
};
use github_copilot_sdk::types::{MessageOptions, ProviderConfig, SessionConfig, SessionEvent};
use github_copilot_sdk::{
    CliProgram, Client, ClientMode, ClientOptions, OtelExporterType, TelemetryConfig, TraceContext,
    TraceContextProvider,
};
use parking_lot::{Mutex, RwLock};
use serde::{Deserialize, Serialize};
use serde_json::{Map, Value, json};
use sha2::{Digest, Sha256};
use tempfile::TempDir;
use time::OffsetDateTime;
use time::format_description::well_known::Rfc3339;
use tracing::field::{Field, Visit};
use tracing::{Event, Subscriber};
use tracing_subscriber::layer::{Context, SubscriberExt as _};
use tracing_subscriber::{Layer, Registry};

const CONTRACT_VERSION: &str = "1.0";
const ADAPTER_ID: &str = "github-copilot-sdk-rust";
const ADAPTER_VERSION: &str = env!("CARGO_PKG_VERSION");
const COLD_SCENARIO: &str = "cold-process-new-session-first-turn";
const WARM_SCENARIO: &str = "warm-process-new-session-first-turn";
const SECOND_TURN_SCENARIO: &str = "second-turn-same-session";
const MAX_ATTACHMENT_BYTES: usize = 1_000_000;
const CLEANUP_TIMEOUT: Duration = Duration::from_secs(5);

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(untagged)]
enum Scalar {
    Null,
    Bool(bool),
    Number(serde_json::Number),
    String(String),
}

#[derive(Clone, Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct ArtifactLocation {
    kind: String,
    value: String,
}

#[derive(Clone, Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct ArtifactIdentity {
    role: String,
    location: ArtifactLocation,
    local_path_id: String,
    discovery_source: String,
    version: String,
    git_sha: Option<String>,
    git_dirty: Option<bool>,
    sha256: String,
    size_bytes: u64,
}

#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct LocalArtifactBinding {
    role: String,
    path: PathBuf,
    identity: ArtifactIdentity,
}

#[derive(Clone, Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct SanitizedConfiguration {
    model: Option<String>,
    reasoning_effort: Option<String>,
    context_tier: Option<String>,
    session_mode: Option<String>,
    prompt: PromptIdentity,
    parameters: BTreeMap<String, Scalar>,
}

#[derive(Clone, Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct PromptIdentity {
    fixture_id: String,
    sha256: String,
    byte_length: u64,
}

#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct ProcessBatch {
    id: String,
    reuse: String,
}

#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct TrialInput {
    prompt: String,
}

#[derive(Clone, Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct Scenario {
    id: String,
    version: String,
    temperature: String,
}

#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct TrialPlan {
    trial_id: String,
    correlation_id: String,
    #[serde(default)]
    otel_trace_id: Option<String>,
    lane: String,
    scenario: Scenario,
    mode: String,
    repetition: u32,
    ordinal: u32,
    timeout_ms: u64,
    configuration: SanitizedConfiguration,
    input: TrialInput,
    attachment_dir: String,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct ProcessBatchRunRequest {
    contract_version: String,
    run_id: String,
    process_batch: ProcessBatch,
    output_root: PathBuf,
    artifacts: Vec<LocalArtifactBinding>,
    trials: Vec<TrialPlan>,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct ProcessBatchIdentity {
    id: String,
    reuse: String,
    ordinal: u32,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct TrialClock {
    id: String,
    origin: &'static str,
    unit: &'static str,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct ClockSourceNormalization {
    source: &'static str,
    clock_id: String,
    strategy: &'static str,
    quality: &'static str,
    anchor_offset_us: u64,
    #[serde(skip_serializing_if = "Option::is_none")]
    estimated_error_us: Option<u64>,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct ClockNormalization {
    trial_clock: TrialClock,
    sources: Vec<ClockSourceNormalization>,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct Milestone {
    name: String,
    offset_us: u64,
    sequence: u32,
    source: &'static str,
    #[serde(skip_serializing_if = "Option::is_none")]
    attributes: Option<BTreeMap<String, Scalar>>,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct TrialError {
    code: String,
    phase: String,
    message: String,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct AttachmentMetadata {
    name: String,
    path: String,
    media_type: String,
    sha256: String,
    size_bytes: u64,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct TrialRecord {
    schema_version: &'static str,
    run_id: String,
    trial_id: String,
    correlation_id: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    otel_trace_id: Option<String>,
    lane: String,
    scenario: Scenario,
    mode: String,
    repetition: u32,
    process_batch: ProcessBatchIdentity,
    status: &'static str,
    started_at: String,
    artifacts: Vec<ArtifactIdentity>,
    configuration: SanitizedConfiguration,
    clock_normalization: ClockNormalization,
    milestones: Vec<Milestone>,
    #[serde(skip_serializing_if = "Option::is_none")]
    error: Option<TrialError>,
    attachments: Vec<AttachmentMetadata>,
    #[serde(skip)]
    attachment_dir: String,
    #[serde(skip)]
    wall_time_unix_us: u64,
    #[serde(skip)]
    wall_anchor_error_us: u64,
}

struct Observation {
    origin: Instant,
    next_sequence: u32,
    milestones: Vec<Milestone>,
}

impl Observation {
    fn new() -> Self {
        let mut observation = Self {
            origin: Instant::now(),
            next_sequence: 0,
            milestones: Vec::new(),
        };
        observation.mark_at("trial.start", 0, "sdk", None);
        observation
    }

    fn offset_us(&self) -> u64 {
        elapsed_us(self.origin)
    }

    fn offset_at(&self, instant: Instant) -> u64 {
        instant
            .saturating_duration_since(self.origin)
            .as_micros()
            .min(u64::MAX as u128) as u64
    }

    fn mark(&mut self, name: impl Into<String>, source: &'static str) -> u64 {
        let offset = self.offset_us();
        self.mark_at(name, offset, source, None);
        offset
    }

    fn mark_with_attributes(
        &mut self,
        name: impl Into<String>,
        source: &'static str,
        attributes: BTreeMap<String, Scalar>,
    ) -> u64 {
        let offset = self.offset_us();
        self.mark_at(name, offset, source, Some(attributes));
        offset
    }

    fn mark_at(
        &mut self,
        name: impl Into<String>,
        offset_us: u64,
        source: &'static str,
        attributes: Option<BTreeMap<String, Scalar>>,
    ) {
        self.milestones.push(Milestone {
            name: name.into(),
            offset_us,
            sequence: self.next_sequence,
            source,
            attributes,
        });
        self.next_sequence += 1;
    }

    fn finish(mut self) -> Vec<Milestone> {
        self.mark("trial.end", "sdk");
        self.milestones
            .sort_by_key(|milestone| (milestone.offset_us, milestone.sequence));
        for (sequence, milestone) in self.milestones.iter_mut().enumerate() {
            milestone.sequence = sequence as u32;
        }
        self.milestones
    }
}

#[derive(Clone, Default)]
struct FixedTraceProvider {
    current: Arc<RwLock<TraceContext>>,
}

impl FixedTraceProvider {
    fn set(&self, trial: &TrialPlan) {
        *self.current.write() = trace_context(trial);
    }
}

#[async_trait]
impl TraceContextProvider for FixedTraceProvider {
    async fn get_trace_context(&self) -> TraceContext {
        self.current.read().clone()
    }
}

#[derive(Clone, Copy, Debug)]
struct CreateTiming {
    pre_rpc_us: u64,
    rpc_us: u64,
    post_rpc_us: u64,
}

#[derive(Clone, Copy, Debug)]
struct SessionSendTiming {
    prepare_us: u64,
    rpc_us: u64,
}

#[derive(Clone, Copy, Debug)]
struct RpcSendTiming {
    prepare_us: u64,
    write_us: u64,
    response_wait_us: u64,
    response_body_read_us: u64,
    response_parse_us: u64,
    response_dispatch_us: u64,
    response_delivery_us: u64,
}

#[derive(Clone, Debug)]
struct EventStageTiming {
    phase: String,
    event_id: String,
    observed_at: Instant,
    header_to_ready_us: Option<u64>,
    body_read_us: Option<u64>,
    parse_us: Option<u64>,
    route_us: Option<u64>,
    dispatch_us: Option<u64>,
}

#[derive(Clone, Default)]
struct CreateTimingLayer {
    timings: Arc<Mutex<VecDeque<CreateTiming>>>,
    session_send_timings: Arc<Mutex<VecDeque<SessionSendTiming>>>,
    rpc_send_timings: Arc<Mutex<VecDeque<RpcSendTiming>>>,
    assistant_delta_stages: Arc<Mutex<VecDeque<EventStageTiming>>>,
}

impl CreateTimingLayer {
    fn pop(&self) -> Option<CreateTiming> {
        self.timings.lock().pop_front()
    }

    fn pop_session_send(&self) -> Option<SessionSendTiming> {
        self.session_send_timings.lock().pop_front()
    }

    fn pop_rpc_send(&self) -> Option<RpcSendTiming> {
        self.rpc_send_timings.lock().pop_front()
    }

    fn pop_assistant_delta_stages(&self, event_id: &str) -> Vec<EventStageTiming> {
        let mut pending = self.assistant_delta_stages.lock();
        let mut matched = Vec::new();
        let mut index = 0;
        while index < pending.len() {
            if pending[index].event_id == event_id {
                if let Some(stage) = pending.remove(index) {
                    matched.push(stage);
                }
            } else {
                index += 1;
            }
        }
        matched
    }

    fn begin_turn(&self) {
        self.session_send_timings.lock().clear();
        self.rpc_send_timings.lock().clear();
        self.assistant_delta_stages.lock().clear();
    }
}

#[derive(Default)]
struct CreateTimingVisitor {
    perf_phase: Option<String>,
    method: Option<String>,
    event_type: Option<String>,
    event_id: Option<String>,
    pre_rpc_us: Option<u64>,
    rpc_us: Option<u64>,
    post_rpc_us: Option<u64>,
    prepare_us: Option<u64>,
    write_us: Option<u64>,
    response_wait_us: Option<u64>,
    response_body_read_us: Option<u64>,
    response_parse_us: Option<u64>,
    response_dispatch_us: Option<u64>,
    response_delivery_us: Option<u64>,
    header_to_ready_us: Option<u64>,
    body_read_us: Option<u64>,
    parse_us: Option<u64>,
    route_us: Option<u64>,
    dispatch_us: Option<u64>,
}

impl Visit for CreateTimingVisitor {
    fn record_u64(&mut self, field: &Field, value: u64) {
        match field.name() {
            "pre_rpc_us" => self.pre_rpc_us = Some(value),
            "rpc_us" => self.rpc_us = Some(value),
            "post_rpc_us" => self.post_rpc_us = Some(value),
            "prepare_us" => self.prepare_us = Some(value),
            "write_us" => self.write_us = Some(value),
            "response_wait_us" => self.response_wait_us = Some(value),
            "response_body_read_us" => self.response_body_read_us = Some(value),
            "response_parse_us" => self.response_parse_us = Some(value),
            "response_dispatch_us" => self.response_dispatch_us = Some(value),
            "response_delivery_us" => self.response_delivery_us = Some(value),
            "header_to_ready_us" => self.header_to_ready_us = Some(value),
            "body_read_us" => self.body_read_us = Some(value),
            "parse_us" => self.parse_us = Some(value),
            "route_us" => self.route_us = Some(value),
            "dispatch_us" => self.dispatch_us = Some(value),
            _ => {}
        }
    }

    fn record_str(&mut self, field: &Field, value: &str) {
        match field.name() {
            "perf_phase" => self.perf_phase = Some(value.to_string()),
            "method" => self.method = Some(value.to_string()),
            "event_type" => self.event_type = Some(value.to_string()),
            "event_id" => self.event_id = Some(value.to_string()),
            _ => {}
        }
    }

    fn record_debug(&mut self, _field: &Field, _value: &dyn std::fmt::Debug) {}
}

impl<S> Layer<S> for CreateTimingLayer
where
    S: Subscriber,
{
    fn on_event(&self, event: &Event<'_>, _ctx: Context<'_, S>) {
        let mut visitor = CreateTimingVisitor::default();
        event.record(&mut visitor);
        if let (Some(pre_rpc_us), Some(rpc_us), Some(post_rpc_us)) =
            (visitor.pre_rpc_us, visitor.rpc_us, visitor.post_rpc_us)
        {
            self.timings.lock().push_back(CreateTiming {
                pre_rpc_us,
                rpc_us,
                post_rpc_us,
            });
        }
        match visitor.perf_phase.as_deref() {
            Some("session.send.complete") => {
                if let (Some(prepare_us), Some(rpc_us)) = (visitor.prepare_us, visitor.rpc_us) {
                    self.session_send_timings
                        .lock()
                        .push_back(SessionSendTiming { prepare_us, rpc_us });
                }
            }
            Some("jsonrpc.request.complete")
                if visitor.method.as_deref() == Some("session.send") =>
            {
                if let (
                    Some(prepare_us),
                    Some(write_us),
                    Some(response_wait_us),
                    Some(response_body_read_us),
                    Some(response_parse_us),
                    Some(response_dispatch_us),
                    Some(response_delivery_us),
                ) = (
                    visitor.prepare_us,
                    visitor.write_us,
                    visitor.response_wait_us,
                    visitor.response_body_read_us,
                    visitor.response_parse_us,
                    visitor.response_dispatch_us,
                    visitor.response_delivery_us,
                ) {
                    self.rpc_send_timings.lock().push_back(RpcSendTiming {
                        prepare_us,
                        write_us,
                        response_wait_us,
                        response_body_read_us,
                        response_parse_us,
                        response_dispatch_us,
                        response_delivery_us,
                    });
                }
            }
            Some(phase)
                if visitor.event_type.as_deref() == Some("assistant.message_delta")
                    && matches!(
                        phase,
                        "jsonrpc.notification.ready"
                            | "router.notification.ready"
                            | "session.notification.ready"
                    ) =>
            {
                if let Some(event_id) = visitor.event_id {
                    self.assistant_delta_stages
                        .lock()
                        .push_back(EventStageTiming {
                            phase: phase.to_string(),
                            event_id,
                            observed_at: Instant::now(),
                            header_to_ready_us: visitor.header_to_ready_us,
                            body_read_us: visitor.body_read_us,
                            parse_us: visitor.parse_us,
                            route_us: visitor.route_us,
                            dispatch_us: visitor.dispatch_us,
                        });
                }
            }
            _ => {}
        }
    }
}

struct AdapterState {
    client: Option<Client>,
    session: Option<Session>,
    temp: TempDir,
    raw_otel_path: PathBuf,
    cli_path: PathBuf,
    trace_provider: FixedTraceProvider,
    timing_layer: CreateTimingLayer,
}

impl AdapterState {
    fn new(cli_path: PathBuf, timing_layer: CreateTimingLayer) -> Result<Self, String> {
        let temp = tempfile::tempdir().map_err(|_| "unable to create adapter state".to_string())?;
        let raw_otel_path = temp.path().join("runtime-otel.jsonl");
        Ok(Self {
            client: None,
            session: None,
            temp,
            raw_otel_path,
            cli_path,
            trace_provider: FixedTraceProvider::default(),
            timing_layer,
        })
    }

    async fn start_client(&mut self, observation: Option<&mut Observation>) -> Result<(), String> {
        if self.client.is_some() {
            return Ok(());
        }
        let work_dir = self.temp.path().join("work");
        let home_dir = self.temp.path().join("home");
        fs::create_dir_all(&work_dir).map_err(|_| "unable to create work directory".to_string())?;
        fs::create_dir_all(&home_dir).map_err(|_| "unable to create home directory".to_string())?;

        let mut observation = observation;
        if let Some(value) = observation.as_mut() {
            value.mark("sdk.client.start.begin", "sdk");
        }
        let options = ClientOptions::new()
            .with_program(CliProgram::Path(self.cli_path.clone()))
            .with_cwd(&work_dir)
            .with_base_directory(&home_dir)
            .with_mode(ClientMode::Empty)
            .with_use_logged_in_user(false)
            .with_trace_context_provider(self.trace_provider.clone())
            .with_telemetry(
                TelemetryConfig::new()
                    .with_exporter_type(OtelExporterType::File)
                    .with_file_path(&self.raw_otel_path)
                    .with_source_name(ADAPTER_ID)
                    .with_capture_content(false),
            );
        let client = Client::start(options)
            .await
            .map_err(|_| "client startup failed".to_string())?;
        if let Some(value) = observation.as_mut() {
            value.mark("sdk.client.start.end", "sdk");
            value.mark("process.ready", "sdk");
            value.mark("protocol.verified", "sdk");
        }
        self.client = Some(client);
        Ok(())
    }

    async fn create_session(
        &mut self,
        trial: &TrialPlan,
        observation: &mut Observation,
    ) -> Result<(), String> {
        self.timing_layer.begin_turn();
        self.trace_provider.set(trial);
        let config = session_config(trial, self.temp.path())?;
        let client = self
            .client
            .as_ref()
            .ok_or_else(|| "client is not running".to_string())?;
        let create_begin = observation.mark("session.create.begin", "sdk");
        let session = client
            .create_session(config)
            .await
            .map_err(|_| "session creation failed".to_string())?;
        let create_end = observation.mark("session.create.end", "sdk");
        if let Some(timing) = self.timing_layer.pop() {
            add_create_timing(observation, create_begin, create_end, timing);
        }
        self.session = Some(session);
        Ok(())
    }

    async fn run_turn(
        &mut self,
        trial: &TrialPlan,
        observation: &mut Observation,
    ) -> Result<(), String> {
        self.trace_provider.set(trial);
        let session = self
            .session
            .as_ref()
            .ok_or_else(|| "session is not available".to_string())?;
        let mut events = session.subscribe();
        observation.mark("prompt.submitted", "sdk");
        let message =
            MessageOptions::new(&trial.input.prompt).with_trace_context(trace_context(trial));
        let send_begin = observation.mark("sdk.session.send.begin", "sdk");
        session
            .send(message)
            .await
            .map_err(|_| "prompt submission failed".to_string())?;
        let send_end = observation.mark("prompt.accepted", "sdk");
        if let (Some(session_timing), Some(rpc_timing)) = (
            self.timing_layer.pop_session_send(),
            self.timing_layer.pop_rpc_send(),
        ) {
            add_send_timing(
                observation,
                send_begin,
                send_end,
                session_timing,
                rpc_timing,
            );
        }

        let mut observed_first_delta = false;
        loop {
            let event = events
                .recv()
                .await
                .map_err(|_| "session event stream closed".to_string())?;
            if event.agent_id.is_some() {
                continue;
            }
            if let Some(error) = root_session_error(&event) {
                return Err(error.to_string());
            }
            match event.parsed_type() {
                SessionEventType::AssistantMessageDelta => {
                    let event_stages = self
                        .timing_layer
                        .pop_assistant_delta_stages(event.id.as_str());
                    if observed_first_delta {
                        continue;
                    }
                    let Some(delta) = event.typed_data::<AssistantMessageDeltaData>() else {
                        continue;
                    };
                    if delta.delta_content.is_empty() {
                        continue;
                    }
                    add_assistant_delta_timing(observation, &event_stages);
                    let mut attributes = BTreeMap::new();
                    attributes.insert(
                        "byteLength".into(),
                        Scalar::Number(delta.delta_content.len().into()),
                    );
                    let offset = observation.mark_with_attributes(
                        "assistant.first_delta",
                        "sdk",
                        attributes.clone(),
                    );
                    observation.mark_at(
                        "sdk.assistant.first_delta",
                        offset,
                        "sdk",
                        Some(attributes),
                    );
                    observed_first_delta = true;
                }
                SessionEventType::AssistantMessage => {
                    let byte_length = event
                        .typed_data::<AssistantMessageData>()
                        .map_or(0, |message| message.content.len());
                    let mut attributes = BTreeMap::new();
                    attributes.insert("byteLength".into(), Scalar::Number(byte_length.into()));
                    observation.mark_with_attributes("assistant.message", "sdk", attributes);
                }
                SessionEventType::SessionIdle => {
                    observation.mark("session.idle", "sdk");
                    break;
                }
                _ => {}
            }
        }
        if !observed_first_delta {
            return Err("turn completed without a non-empty root assistant delta".to_string());
        }
        observation.mark("sdk.turn.complete", "sdk");
        Ok(())
    }

    async fn cleanup(&mut self) {
        let session = self.session.take();
        let Some(client) = self.client.take() else {
            return;
        };
        let graceful_cleanup = async {
            if let Some(session) = session
                && session.disconnect().await.is_err()
            {
                eprintln!("benchmark adapter session cleanup failed");
            }
            client.stop().await
        };

        match tokio::time::timeout(CLEANUP_TIMEOUT, graceful_cleanup).await {
            Ok(Ok(())) => {}
            Ok(Err(_)) => {
                eprintln!("benchmark adapter client cleanup failed");
                client.force_stop();
            }
            Err(_) => {
                eprintln!(
                    "benchmark adapter cleanup exceeded {} ms; forcing shutdown",
                    CLEANUP_TIMEOUT.as_millis()
                );
                client.force_stop();
            }
        }
    }
}

fn root_session_error(event: &SessionEvent) -> Option<&'static str> {
    if event.parsed_type() != SessionEventType::SessionError || event.is_transient_error() {
        return None;
    }
    Some("root session emitted session.error")
}

fn add_create_timing(
    observation: &mut Observation,
    create_begin: u64,
    create_end: u64,
    timing: CreateTiming,
) {
    let rpc_begin = create_begin
        .saturating_add(timing.pre_rpc_us)
        .min(create_end);
    let rpc_end = rpc_begin.saturating_add(timing.rpc_us).min(create_end);
    let local_post_end = rpc_end.saturating_add(timing.post_rpc_us).min(create_end);
    observation.mark_at(
        "sdk.session.create.local.pre.begin",
        create_begin,
        "sdk",
        None,
    );
    observation.mark_at("sdk.session.create.local.pre.end", rpc_begin, "sdk", None);
    observation.mark_at("session.create.rpc.begin", rpc_begin, "sdk", None);
    observation.mark_at("session.create.rpc.end", rpc_end, "sdk", None);
    observation.mark_at("sdk.session.create.local.post.begin", rpc_end, "sdk", None);
    observation.mark_at(
        "sdk.session.create.local.post.end",
        local_post_end,
        "sdk",
        None,
    );
    if local_post_end < create_end {
        observation.mark_at(
            "session.create.post-create.begin",
            local_post_end,
            "sdk",
            None,
        );
        observation.mark_at("session.create.post-create.end", create_end, "sdk", None);
    }
}

fn add_send_timing(
    observation: &mut Observation,
    send_begin: u64,
    send_end: u64,
    session: SessionSendTiming,
    rpc: RpcSendTiming,
) {
    let prepare_end = send_begin.saturating_add(session.prepare_us).min(send_end);
    observation.mark_at(
        "sdk.session.send.local.prepare.begin",
        send_begin,
        "sdk",
        None,
    );
    observation.mark_at(
        "sdk.session.send.local.prepare.end",
        prepare_end,
        "sdk",
        None,
    );
    let session_rpc_end = prepare_end.saturating_add(session.rpc_us).min(send_end);
    observation.mark_at("session.send.rpc.begin", prepare_end, "sdk", None);
    observation.mark_at("session.send.rpc.end", session_rpc_end, "sdk", None);

    let mut cursor = prepare_end;
    cursor = mark_interval(
        observation,
        "sdk.jsonrpc.request.prepare",
        cursor,
        rpc.prepare_us,
        send_end,
        "sdk",
    );
    cursor = mark_interval(
        observation,
        "sdk_transport.jsonrpc.request.write",
        cursor,
        rpc.write_us,
        send_end,
        "sdk",
    );
    cursor = mark_interval(
        observation,
        "runtime_transport.session.send.wait",
        cursor,
        rpc.response_wait_us,
        send_end,
        "sdk",
    );
    cursor = mark_interval(
        observation,
        "runtime_transport.jsonrpc.response.body_read",
        cursor,
        rpc.response_body_read_us,
        send_end,
        "sdk",
    );
    cursor = mark_interval(
        observation,
        "sdk.jsonrpc.response.parse",
        cursor,
        rpc.response_parse_us,
        send_end,
        "sdk",
    );
    cursor = mark_interval(
        observation,
        "sdk.jsonrpc.response.dispatch",
        cursor,
        rpc.response_dispatch_us,
        send_end,
        "sdk",
    );
    cursor = mark_interval(
        observation,
        "sdk.jsonrpc.response.delivery",
        cursor,
        rpc.response_delivery_us,
        send_end,
        "sdk",
    );
    if cursor < send_end {
        observation.mark_at("sdk.session.send.local.post.begin", cursor, "sdk", None);
        observation.mark_at("sdk.session.send.local.post.end", send_end, "sdk", None);
    }
}

fn mark_interval(
    observation: &mut Observation,
    name: &str,
    begin: u64,
    duration_us: u64,
    limit: u64,
    source: &'static str,
) -> u64 {
    let end = begin.saturating_add(duration_us).min(limit);
    observation.mark_at(format!("{name}.begin"), begin, source, None);
    observation.mark_at(format!("{name}.end"), end, source, None);
    end
}

fn add_assistant_delta_timing(observation: &mut Observation, stages: &[EventStageTiming]) {
    for stage in stages {
        let ready = observation.offset_at(stage.observed_at);
        match stage.phase.as_str() {
            "jsonrpc.notification.ready" => {
                let header = ready.saturating_sub(stage.header_to_ready_us.unwrap_or(0));
                observation.mark_at("sdk.assistant.delta.header.received", header, "sdk", None);
                let parse_begin = ready.saturating_sub(stage.parse_us.unwrap_or(0));
                let body_begin = parse_begin.saturating_sub(stage.body_read_us.unwrap_or(0));
                observation.mark_at(
                    "runtime_transport.assistant.delta.body_read.begin",
                    body_begin,
                    "sdk",
                    None,
                );
                observation.mark_at(
                    "runtime_transport.assistant.delta.body_read.end",
                    parse_begin,
                    "sdk",
                    None,
                );
                observation.mark_at("sdk.assistant.delta.parse.begin", parse_begin, "sdk", None);
                observation.mark_at("sdk.assistant.delta.parse.end", ready, "sdk", None);
                observation.mark_at("sdk.assistant.delta.jsonrpc.ready", ready, "sdk", None);
            }
            "router.notification.ready" => {
                let begin = ready.saturating_sub(stage.route_us.unwrap_or(0));
                observation.mark_at("sdk.assistant.delta.router.begin", begin, "sdk", None);
                observation.mark_at("sdk.assistant.delta.router.ready", ready, "sdk", None);
            }
            "session.notification.ready" => {
                let begin = ready.saturating_sub(stage.dispatch_us.unwrap_or(0));
                observation.mark_at("sdk.assistant.delta.session.begin", begin, "sdk", None);
                observation.mark_at("sdk.assistant.delta.session.ready", ready, "sdk", None);
            }
            _ => {}
        }
    }
}

fn session_config(trial: &TrialPlan, work_root: &Path) -> Result<SessionConfig, String> {
    let model = trial
        .configuration
        .model
        .as_deref()
        .ok_or_else(|| "configuration.model is required".to_string())?;
    let mut config = SessionConfig::default()
        .with_client_name(ADAPTER_ID)
        .with_model(model)
        .with_streaming(true)
        .with_available_tools(Vec::<String>::new())
        .with_working_directory(work_root.join("work"))
        .with_enable_config_discovery(false)
        .with_skip_custom_instructions(true)
        .with_include_sub_agent_streaming_events(false);

    if let Some(reasoning_effort) = &trial.configuration.reasoning_effort {
        config = config.with_reasoning_effort(reasoning_effort);
    }
    if let Some(context_tier) = &trial.configuration.context_tier {
        config = config.with_context_tier(context_tier);
    }

    match parameter_string(&trial.configuration, "providerPath").as_deref() {
        Some("custom-openai") => {
            let base_url = required_secret_env("COPILOT_PERF_PROVIDER_BASE_URL")?;
            let api_key = required_secret_env("COPILOT_PERF_PROVIDER_API_KEY")?;
            let provider = ProviderConfig::new(base_url)
                .with_provider_type("openai")
                .with_wire_api("completions")
                .with_api_key(api_key)
                .with_model_id(model)
                .with_wire_model(model);
            config = config.with_provider(provider);
        }
        Some("capi-auth-token-env") => {
            config = config.with_github_token(required_secret_env("COPILOT_PERF_GITHUB_TOKEN")?);
        }
        _ => return Err("unsupported providerPath".to_string()),
    }
    Ok(config)
}

fn parameter_string(configuration: &SanitizedConfiguration, key: &str) -> Option<String> {
    match configuration.parameters.get(key)? {
        Scalar::String(value) => Some(value.clone()),
        _ => None,
    }
}

fn required_secret_env(name: &str) -> Result<String, String> {
    env::var(name).map_err(|_| format!("required adapter environment variable {name} is missing"))
}

fn trace_context(trial: &TrialPlan) -> TraceContext {
    let Some(trace_id) = &trial.otel_trace_id else {
        return TraceContext::new();
    };
    let span_id = uuid::Uuid::new_v4().simple().to_string()[..16].to_string();
    TraceContext::from_traceparent(format!("00-{trace_id}-{span_id}-01"))
}

fn descriptor() -> Value {
    json!({
        "contractVersion": CONTRACT_VERSION,
        "adapter": {
            "id": ADAPTER_ID,
            "version": ADAPTER_VERSION
        },
        "lanes": ["sdk"],
        "scenarios": [
            {
                "id": COLD_SCENARIO,
                "version": "1",
                "lanes": ["sdk"],
                "modes": ["deterministic", "live"],
                "temperatures": ["cold"],
                "processReuse": ["isolated"]
            },
            {
                "id": WARM_SCENARIO,
                "version": "1",
                "lanes": ["sdk"],
                "modes": ["deterministic", "live"],
                "temperatures": ["warm"],
                "processReuse": ["shared"]
            },
            {
                "id": SECOND_TURN_SCENARIO,
                "version": "1",
                "lanes": ["sdk"],
                "modes": ["deterministic", "live"],
                "temperatures": ["warm"],
                "processReuse": ["shared"]
            }
        ]
    })
}

fn validate_request(request: &ProcessBatchRunRequest) -> Result<PathBuf, String> {
    if request.contract_version != CONTRACT_VERSION {
        return Err("unsupported contractVersion".to_string());
    }
    if !request.output_root.is_absolute() {
        return Err("outputRoot must be absolute".to_string());
    }
    if request.trials.is_empty() {
        return Err("trials must not be empty".to_string());
    }
    validate_trial_identities(&request.trials)?;
    for (index, trial) in request.trials.iter().enumerate() {
        if trial.ordinal as usize != index {
            return Err("trial ordinals must be contiguous and ordered".to_string());
        }
        if trial.lane != "sdk" || trial.scenario.version != "1" {
            return Err("unsupported lane or scenario version".to_string());
        }
        if !matches!(trial.mode.as_str(), "deterministic" | "live") {
            return Err("unsupported trial mode".to_string());
        }
        if trial.configuration.session_mode.as_deref() != Some("interactive") {
            return Err("only the interactive sessionMode is supported".to_string());
        }
        if trial.timeout_ms == 0 {
            return Err("timeoutMs must be positive".to_string());
        }
        if trial.repetition == 0 {
            return Err("repetition must be positive".to_string());
        }
        validate_prompt(&trial.input, &trial.configuration.prompt)?;
        safe_relative_path(&trial.attachment_dir)?;
    }

    match request.trials.as_slice() {
        [trial]
            if trial.scenario.id == COLD_SCENARIO
                && trial.scenario.temperature == "cold"
                && request.process_batch.reuse == "isolated" => {}
        [trial]
            if trial.scenario.id == WARM_SCENARIO
                && trial.scenario.temperature == "warm"
                && request.process_batch.reuse == "shared" => {}
        [first, second]
            if first.scenario.id == WARM_SCENARIO
                && first.scenario.temperature == "warm"
                && second.scenario.id == SECOND_TURN_SCENARIO
                && second.scenario.temperature == "warm"
                && request.process_batch.reuse == "shared" =>
        {
            validate_shared_session_configuration(first, second)?;
        }
        _ => return Err("unsupported process-batch scenario sequence".to_string()),
    }

    let cli = request
        .artifacts
        .iter()
        .find(|artifact| artifact.role == "cli")
        .ok_or_else(|| "a cli artifact binding is required".to_string())?;
    if request
        .artifacts
        .iter()
        .any(|artifact| artifact.role != artifact.identity.role)
    {
        return Err("artifact role does not match identity".to_string());
    }
    if !cli.path.is_absolute() || !cli.path.is_file() {
        return Err("cli artifact path must name an absolute file".to_string());
    }
    Ok(cli.path.clone())
}

fn validate_trial_identities(trials: &[TrialPlan]) -> Result<(), String> {
    let mut trial_ids = HashSet::new();
    let mut correlation_ids = HashSet::new();
    let mut trace_ids = HashSet::new();
    let mut attachment_dirs = HashSet::new();
    for trial in trials {
        if !trial_ids.insert(trial.trial_id.as_str()) {
            return Err("trialId values must be unique within a process batch".to_string());
        }
        if !correlation_ids.insert(trial.correlation_id.as_str()) {
            return Err("correlationId values must be unique within a process batch".to_string());
        }
        if let Some(trace_id) = trial.otel_trace_id.as_deref() {
            validate_otel_trace_id(trace_id)?;
            if !trace_ids.insert(trace_id) {
                return Err("otelTraceId values must be unique within a process batch".to_string());
            }
        }
        if !attachment_dirs.insert(trial.attachment_dir.as_str()) {
            return Err("attachmentDir values must be unique within a process batch".to_string());
        }
    }
    Ok(())
}

fn validate_otel_trace_id(value: &str) -> Result<(), String> {
    if value.len() != 32
        || value
            .bytes()
            .any(|byte| !byte.is_ascii_digit() && !(b'a'..=b'f').contains(&byte))
        || value.bytes().all(|byte| byte == b'0')
    {
        return Err("otelTraceId must be a lowercase nonzero 32-hex W3C trace ID".to_string());
    }
    Ok(())
}

fn validate_shared_session_configuration(
    first: &TrialPlan,
    second: &TrialPlan,
) -> Result<(), String> {
    let first_configuration = &first.configuration;
    let second_configuration = &second.configuration;
    if first.mode != second.mode
        || first_configuration.model != second_configuration.model
        || first_configuration.reasoning_effort != second_configuration.reasoning_effort
        || first_configuration.context_tier != second_configuration.context_tier
        || first_configuration.session_mode != second_configuration.session_mode
        || first_configuration.parameters.get("providerPath")
            != second_configuration.parameters.get("providerPath")
    {
        return Err(
            "warm and second-turn trials must use identical session configuration".to_string(),
        );
    }
    Ok(())
}

fn validate_prompt(input: &TrialInput, identity: &PromptIdentity) -> Result<(), String> {
    if input.prompt.len() as u64 != identity.byte_length {
        return Err("prompt byteLength does not match value".to_string());
    }
    let hash = format!("{:x}", Sha256::digest(input.prompt.as_bytes()));
    if hash != identity.sha256 {
        return Err("prompt sha256 does not match value".to_string());
    }
    Ok(())
}

fn safe_relative_path(value: &str) -> Result<PathBuf, String> {
    let path = Path::new(value);
    if path.as_os_str().is_empty() || path.is_absolute() || value.contains('\\') {
        return Err("attachmentDir must be a portable relative path".to_string());
    }
    if path
        .components()
        .any(|component| !matches!(component, Component::Normal(_)))
    {
        return Err("attachmentDir contains an unsafe path component".to_string());
    }
    Ok(path.to_path_buf())
}

fn artifact_identities(artifacts: &[LocalArtifactBinding]) -> Vec<ArtifactIdentity> {
    artifacts
        .iter()
        .map(|artifact| artifact.identity.clone())
        .collect()
}

fn system_time_us() -> Result<u64, String> {
    let micros = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map_err(|_| "system clock is before the Unix epoch".to_string())?
        .as_micros();
    Ok(micros.min(u64::MAX as u128) as u64)
}

fn started_at() -> Result<String, String> {
    OffsetDateTime::now_utc()
        .format(&Rfc3339)
        .map_err(|_| "unable to format UTC timestamp".to_string())
}

struct TrialAnchor {
    started_at: String,
    wall_time_unix_us: u64,
    wall_anchor_error_us: u64,
}

fn base_record(
    request: &ProcessBatchRunRequest,
    trial: &TrialPlan,
    status: &'static str,
    observation: Observation,
    error: Option<TrialError>,
    anchor: TrialAnchor,
) -> TrialRecord {
    TrialRecord {
        schema_version: CONTRACT_VERSION,
        run_id: request.run_id.clone(),
        trial_id: trial.trial_id.clone(),
        correlation_id: trial.correlation_id.clone(),
        otel_trace_id: trial.otel_trace_id.clone(),
        lane: trial.lane.clone(),
        scenario: trial.scenario.clone(),
        mode: trial.mode.clone(),
        repetition: trial.repetition,
        process_batch: ProcessBatchIdentity {
            id: request.process_batch.id.clone(),
            reuse: request.process_batch.reuse.clone(),
            ordinal: trial.ordinal,
        },
        status,
        started_at: anchor.started_at,
        artifacts: artifact_identities(&request.artifacts),
        configuration: trial.configuration.clone(),
        clock_normalization: ClockNormalization {
            trial_clock: TrialClock {
                id: format!("{}:clock", trial.trial_id),
                origin: "trial.start",
                unit: "us",
            },
            sources: vec![ClockSourceNormalization {
                source: "sdk",
                clock_id: format!("{}:clock", trial.trial_id),
                strategy: "native-monotonic",
                quality: "exact",
                anchor_offset_us: 0,
                estimated_error_us: None,
            }],
        },
        milestones: observation.finish(),
        error,
        attachments: Vec::new(),
        attachment_dir: trial.attachment_dir.clone(),
        wall_time_unix_us: anchor.wall_time_unix_us,
        wall_anchor_error_us: anchor.wall_anchor_error_us,
    }
}

async fn execute_trial(
    request: &ProcessBatchRunRequest,
    trial: &TrialPlan,
    state: &mut AdapterState,
) -> TrialRecord {
    let trial_started_at = started_at().unwrap_or_else(|_| "1970-01-01T00:00:00Z".to_string());
    let wall_before = system_time_us().unwrap_or(0);
    let mut observation = Observation::new();
    let wall_after = system_time_us().unwrap_or(wall_before);
    let wall_anchor_error_us = wall_after.saturating_sub(wall_before).max(1);
    let anchor = TrialAnchor {
        started_at: trial_started_at,
        wall_time_unix_us: wall_before,
        wall_anchor_error_us,
    };

    let future = async {
        if trial.scenario.id == COLD_SCENARIO {
            observation.mark_at("process.start", 0, "sdk", None);
            state.start_client(Some(&mut observation)).await?;
            state.create_session(trial, &mut observation).await?;
        } else if trial.scenario.id == WARM_SCENARIO {
            state.create_session(trial, &mut observation).await?;
        }
        state.run_turn(trial, &mut observation).await
    };

    let result = tokio::time::timeout(Duration::from_millis(trial.timeout_ms), future).await;
    match result {
        Ok(Ok(())) => base_record(request, trial, "ok", observation, None, anchor),
        Ok(Err(message)) => base_record(
            request,
            trial,
            "error",
            observation,
            Some(TrialError {
                code: "operation_failed".to_string(),
                phase: stage_for_message(&message).to_string(),
                message,
            }),
            anchor,
        ),
        Err(_) => base_record(
            request,
            trial,
            "error",
            observation,
            Some(TrialError {
                code: "timeout".to_string(),
                phase: "trial".to_string(),
                message: "trial exceeded timeoutMs".to_string(),
            }),
            anchor,
        ),
    }
}

fn stage_for_message(message: &str) -> &'static str {
    if message.contains("client") {
        "client.start"
    } else if message.contains("session creation") {
        "session.create"
    } else if message.contains("prompt") {
        "session.send"
    } else if message.contains("providerPath") || message.contains("environment variable") {
        "configuration"
    } else {
        "turn"
    }
}

async fn run_batch(
    request: ProcessBatchRunRequest,
    timing_layer: CreateTimingLayer,
) -> Result<Vec<TrialRecord>, String> {
    let cli_path = validate_request(&request)?;
    let mut state = AdapterState::new(cli_path, timing_layer)?;

    if request.trials[0].scenario.id == WARM_SCENARIO
        && let Err(message) = state.start_client(None).await
    {
        let mut records = Vec::with_capacity(request.trials.len());
        records.push(operation_error(
            &request,
            &request.trials[0],
            "client.start",
            "operation_failed",
            message,
        ));
        for trial in request.trials.iter().skip(1) {
            records.push(precondition_error(&request, trial));
        }
        state.cleanup().await;
        return Ok(records);
    }

    let mut records = Vec::with_capacity(request.trials.len());
    for trial in &request.trials {
        let record = execute_trial(&request, trial, &mut state).await;
        let failed = record.status != "ok";
        records.push(record);
        if failed {
            break;
        }
    }
    while records.len() < request.trials.len() {
        let trial = &request.trials[records.len()];
        records.push(precondition_error(&request, trial));
    }

    state.cleanup().await;
    if attach_runtime_otel(&request.output_root, &state.raw_otel_path, &mut records).is_err() {
        eprintln!("runtime telemetry attachment could not be produced");
    }
    Ok(records)
}

fn operation_error(
    request: &ProcessBatchRunRequest,
    trial: &TrialPlan,
    phase: &str,
    code: &str,
    message: String,
) -> TrialRecord {
    base_record(
        request,
        trial,
        "error",
        Observation::new(),
        Some(TrialError {
            code: code.to_string(),
            phase: phase.to_string(),
            message,
        }),
        TrialAnchor {
            started_at: started_at().unwrap_or_else(|_| "1970-01-01T00:00:00Z".to_string()),
            wall_time_unix_us: system_time_us().unwrap_or(0),
            wall_anchor_error_us: 1,
        },
    )
}

fn precondition_error(request: &ProcessBatchRunRequest, trial: &TrialPlan) -> TrialRecord {
    operation_error(
        request,
        trial,
        "processBatch",
        "invalid_state",
        "a preceding trial in the process batch failed".to_string(),
    )
}

fn attach_runtime_otel(
    output_root: &Path,
    raw_path: &Path,
    records: &mut [TrialRecord],
) -> Result<(), String> {
    let raw = match fs::read_to_string(raw_path) {
        Ok(value) => value,
        Err(error) if error.kind() == io::ErrorKind::NotFound => return Ok(()),
        Err(_) => return Err("unable to read runtime telemetry".to_string()),
    };
    fs::create_dir_all(output_root).map_err(|_| "unable to create outputRoot".to_string())?;
    let canonical_output_root =
        fs::canonicalize(output_root).map_err(|_| "unable to resolve outputRoot".to_string())?;
    for record in records {
        let Some(trace_id) = record.otel_trace_id.as_deref() else {
            continue;
        };
        let sanitized = sanitize_runtime_otel(
            &raw,
            trace_id,
            record.wall_time_unix_us,
            record.wall_anchor_error_us,
        )?;
        if sanitized.is_empty() {
            continue;
        }
        if sanitized.len() > MAX_ATTACHMENT_BYTES {
            eprintln!("runtime telemetry attachment exceeded the adapter size limit");
            continue;
        }
        let relative_dir = safe_relative_path(&record.attachment_dir)?;
        let relative_path = relative_dir.join("runtime-otel.sanitized.jsonl");
        let requested_parent = output_root.join(&relative_dir);
        fs::create_dir_all(&requested_parent)
            .map_err(|_| "unable to create attachment directory".to_string())?;
        let canonical_parent = fs::canonicalize(&requested_parent)
            .map_err(|_| "unable to resolve attachment directory".to_string())?;
        validate_attachment_parent(&canonical_output_root, &canonical_parent)?;
        let output_path = canonical_parent.join("runtime-otel.sanitized.jsonl");
        match fs::symlink_metadata(&output_path) {
            Ok(metadata) if metadata.file_type().is_symlink() => {
                return Err("attachment destination must not be a symbolic link".to_string());
            }
            Ok(metadata) if !metadata.is_file() => {
                return Err("attachment destination must be a regular file".to_string());
            }
            Ok(_) => {}
            Err(error) if error.kind() == io::ErrorKind::NotFound => {}
            Err(_) => return Err("unable to inspect attachment destination".to_string()),
        }
        fs::write(&output_path, sanitized.as_bytes())
            .map_err(|_| "unable to write runtime telemetry attachment".to_string())?;
        let metadata = AttachmentMetadata {
            name: "runtime-otel".to_string(),
            path: relative_path.to_string_lossy().replace('\\', "/"),
            media_type: "application/x-ndjson".to_string(),
            sha256: format!("{:x}", Sha256::digest(sanitized.as_bytes())),
            size_bytes: sanitized.len() as u64,
        };
        record.attachments.push(metadata);
        record
            .clock_normalization
            .sources
            .push(ClockSourceNormalization {
                source: "runtime",
                clock_id: format!("{}:runtime-otel-epoch", record.trial_id),
                strategy: "wall-anchor",
                quality: "estimated",
                anchor_offset_us: 0,
                estimated_error_us: Some(record.wall_anchor_error_us.max(2_000)),
            });
    }
    Ok(())
}

fn validate_attachment_parent(output_root: &Path, parent: &Path) -> Result<(), String> {
    if !parent.starts_with(output_root) {
        return Err("attachment path escaped outputRoot".to_string());
    }
    Ok(())
}

fn sanitize_runtime_otel(
    raw: &str,
    trace_id: &str,
    wall_time_unix_us: u64,
    estimated_error_us: u64,
) -> Result<String, String> {
    let mut lines = vec![json!({
        "type": "clock-anchor",
        "wallTimeUnixUs": wall_time_unix_us,
        "anchorOffsetUs": 0,
        "estimatedErrorUs": estimated_error_us.max(2_000)
    })];
    for line in raw.lines().filter(|line| !line.trim().is_empty()) {
        let value: Value =
            serde_json::from_str(line).map_err(|_| "runtime telemetry is malformed".to_string())?;
        if value.get("type").and_then(Value::as_str) != Some("span")
            || value.get("traceId").and_then(Value::as_str) != Some(trace_id)
        {
            continue;
        }
        if let Some(span) = sanitize_span(&value) {
            lines.push(span);
        }
    }
    if lines.len() == 1 {
        return Ok(String::new());
    }
    let mut result = String::new();
    for line in lines {
        result.push_str(
            &serde_json::to_string(&line)
                .map_err(|_| "unable to serialize runtime telemetry".to_string())?,
        );
        result.push('\n');
    }
    Ok(result)
}

fn sanitize_span(span: &Value) -> Option<Value> {
    const SPAN_NAMES: &[&str] = &[
        "invoke_agent",
        "session.create",
        "gen_ai.client.inference",
        "gen_ai.client.first_token",
        "assistant.message_delta",
    ];
    const ATTRIBUTE_NAMES: &[&str] = &[
        "gen_ai.operation.name",
        "gen_ai.request.model",
        "gen_ai.system",
        "server.address",
        "error.type",
    ];

    let name = span.get("name")?.as_str()?;
    if !SPAN_NAMES.contains(&name) {
        return None;
    }
    let mut result = Map::new();
    result.insert("type".to_string(), Value::String("span".to_string()));
    result.insert(
        "traceId".to_string(),
        Value::String(span.get("traceId")?.as_str()?.to_string()),
    );
    result.insert("name".to_string(), Value::String(name.to_string()));
    if let Some(span_id) = span.get("spanId").and_then(Value::as_str)
        && span_id.len() == 16
        && span_id.bytes().all(|byte| byte.is_ascii_hexdigit())
    {
        result.insert("spanId".to_string(), Value::String(span_id.to_string()));
    }
    for key in [
        "startTimeUnixNano",
        "startTimeUnixNanos",
        "startTime",
        "endTimeUnixNano",
        "endTimeUnixNanos",
        "endTime",
        "duration",
    ] {
        if let Some(value) = span.get(key)
            && safe_timestamp_or_identifier(value)
        {
            result.insert(key.to_string(), value.clone());
        }
    }
    if let Some(attributes) = span.get("attributes").and_then(Value::as_object) {
        let filtered: Map<String, Value> = attributes
            .iter()
            .filter(|(key, value)| {
                ATTRIBUTE_NAMES.contains(&key.as_str()) && value_is_safe_scalar(value)
            })
            .map(|(key, value)| (key.clone(), value.clone()))
            .collect();
        if !filtered.is_empty() {
            result.insert("attributes".to_string(), Value::Object(filtered));
        }
    }
    if let Some(events) = span.get("events").and_then(Value::as_array) {
        let filtered: Vec<Value> = events.iter().filter_map(sanitize_span_event).collect();
        if !filtered.is_empty() {
            result.insert("events".to_string(), Value::Array(filtered));
        }
    }
    Some(Value::Object(result))
}

fn sanitize_span_event(event: &Value) -> Option<Value> {
    let name = event.get("name")?.as_str()?;
    if !matches!(
        name,
        "gen_ai.client.first_token" | "assistant.message_delta"
    ) {
        return None;
    }
    let mut result = Map::new();
    result.insert("name".to_string(), Value::String(name.to_string()));
    for key in ["timeUnixNano", "timestampUnixNano", "time", "timestamp"] {
        if let Some(value) = event.get(key)
            && safe_timestamp_or_identifier(value)
        {
            result.insert(key.to_string(), value.clone());
        }
    }
    Some(Value::Object(result))
}

fn safe_timestamp_or_identifier(value: &Value) -> bool {
    match value {
        Value::Number(_) => true,
        Value::String(value) => {
            value.len() <= 40
                && value.bytes().all(|byte| {
                    byte.is_ascii_digit() || matches!(byte, b'T' | b'Z' | b'+' | b'-' | b':' | b'.')
                })
        }
        Value::Array(items) => items.len() == 2 && items.iter().all(Value::is_number),
        _ => false,
    }
}

fn value_is_safe_scalar(value: &Value) -> bool {
    matches!(
        value,
        Value::Null | Value::Bool(_) | Value::Number(_) | Value::String(_)
    )
}

fn elapsed_us(start: Instant) -> u64 {
    start.elapsed().as_micros().min(u64::MAX as u128) as u64
}

async fn run_command(timing_layer: CreateTimingLayer) -> Result<(), String> {
    let mut input = String::new();
    io::stdin()
        .read_to_string(&mut input)
        .map_err(|_| "unable to read batch request".to_string())?;
    let request: ProcessBatchRunRequest =
        serde_json::from_str(&input).map_err(|_| "invalid batch request JSON".to_string())?;
    let records = run_batch(request, timing_layer).await?;
    let stdout = io::stdout();
    let mut lock = stdout.lock();
    for record in records {
        serde_json::to_writer(&mut lock, &record)
            .map_err(|_| "unable to serialize trial record".to_string())?;
        lock.write_all(b"\n")
            .map_err(|_| "unable to write trial record".to_string())?;
    }
    lock.flush()
        .map_err(|_| "unable to flush trial records".to_string())
}

#[tokio::main]
async fn main() {
    let timing_layer = CreateTimingLayer::default();
    let subscriber = Registry::default().with(timing_layer.clone());
    if tracing::subscriber::set_global_default(subscriber).is_err() {
        eprintln!("unable to initialize SDK diagnostics");
        std::process::exit(3);
    }

    let args: Vec<_> = env::args_os().skip(1).collect();
    let result = match args.as_slice() {
        [command] if command == "describe" => {
            println!("{}", descriptor());
            Ok(())
        }
        [command] if command == "run" => run_command(timing_layer).await,
        _ => Err("expected exactly one command: describe or run".to_string()),
    };
    if let Err(error) = result {
        eprintln!("{error}");
        std::process::exit(2);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn trial(
        trial_id: &str,
        correlation_id: &str,
        scenario_id: &str,
        ordinal: u32,
        trace_id: &str,
        attachment_dir: &str,
    ) -> TrialPlan {
        let prompt = format!("prompt-{trial_id}");
        let mut parameters = BTreeMap::new();
        parameters.insert(
            "providerPath".to_string(),
            Scalar::String("custom-openai".to_string()),
        );
        TrialPlan {
            trial_id: trial_id.to_string(),
            correlation_id: correlation_id.to_string(),
            otel_trace_id: Some(trace_id.to_string()),
            lane: "sdk".to_string(),
            scenario: Scenario {
                id: scenario_id.to_string(),
                version: "1".to_string(),
                temperature: "warm".to_string(),
            },
            mode: "deterministic".to_string(),
            repetition: 1,
            ordinal,
            timeout_ms: 30_000,
            configuration: SanitizedConfiguration {
                model: Some("fixture-model".to_string()),
                reasoning_effort: Some("medium".to_string()),
                context_tier: Some("default".to_string()),
                session_mode: Some("interactive".to_string()),
                prompt: PromptIdentity {
                    fixture_id: format!("fixture-{trial_id}"),
                    sha256: format!("{:x}", Sha256::digest(prompt.as_bytes())),
                    byte_length: prompt.len() as u64,
                },
                parameters,
            },
            input: TrialInput { prompt },
            attachment_dir: attachment_dir.to_string(),
        }
    }

    fn session_error(error_type: &str, error_code: &str, message: &str) -> SessionEvent {
        SessionEvent {
            id: "event-id".to_string(),
            timestamp: "2026-01-01T00:00:00Z".to_string(),
            parent_id: None,
            ephemeral: None,
            agent_id: None,
            debug_cli_received_at_ms: None,
            debug_ws_forwarded_at_ms: None,
            event_type: "session.error".to_string(),
            data: json!({
                "errorType": error_type,
                "errorCode": error_code,
                "message": message
            }),
        }
    }

    #[test]
    fn descriptor_advertises_only_sdk_scenarios() {
        let descriptor = descriptor();
        assert_eq!(descriptor["contractVersion"], CONTRACT_VERSION);
        assert_eq!(descriptor["lanes"], json!(["sdk"]));
        assert_eq!(descriptor["scenarios"].as_array().map(Vec::len), Some(3));
    }

    #[test]
    fn prompt_validation_does_not_require_content_output() {
        let value = "deterministic prompt";
        let input = TrialInput {
            prompt: value.to_string(),
        };
        let identity = PromptIdentity {
            fixture_id: "deterministic".to_string(),
            sha256: format!("{:x}", Sha256::digest(value.as_bytes())),
            byte_length: value.len() as u64,
        };
        assert!(validate_prompt(&input, &identity).is_ok());
    }

    #[test]
    fn attachment_paths_are_portable_and_relative() {
        assert!(safe_relative_path("run/trial").is_ok());
        assert!(safe_relative_path("../trial").is_err());
        assert!(safe_relative_path("run\\trial").is_err());
    }

    #[test]
    fn shared_pair_requires_identical_session_configuration() {
        let first = trial(
            "warm",
            "correlation-warm",
            WARM_SCENARIO,
            0,
            "11111111111111111111111111111111",
            "run/warm",
        );
        let mut second = trial(
            "second",
            "correlation-second",
            SECOND_TURN_SCENARIO,
            1,
            "22222222222222222222222222222222",
            "run/second",
        );
        assert!(validate_shared_session_configuration(&first, &second).is_ok());

        second.configuration.model = Some("different-model".to_string());
        assert!(validate_shared_session_configuration(&first, &second).is_err());

        let mut second = second.clone();
        second.configuration.model = first.configuration.model.clone();
        second.mode = "live".to_string();
        assert!(validate_shared_session_configuration(&first, &second).is_err());

        second.mode = first.mode.clone();
        second.configuration.parameters.insert(
            "providerPath".to_string(),
            Scalar::String("capi-auth-token-env".to_string()),
        );
        assert!(validate_shared_session_configuration(&first, &second).is_err());
    }

    #[test]
    fn trial_identities_require_valid_unique_values() {
        let first = trial(
            "warm",
            "correlation-warm",
            WARM_SCENARIO,
            0,
            "11111111111111111111111111111111",
            "run/warm",
        );
        let second = trial(
            "second",
            "correlation-second",
            SECOND_TURN_SCENARIO,
            1,
            "22222222222222222222222222222222",
            "run/second",
        );
        assert!(validate_trial_identities(&[first.clone(), second.clone()]).is_ok());

        let mut duplicate = second.clone();
        duplicate.trial_id = first.trial_id.clone();
        assert!(validate_trial_identities(&[first.clone(), duplicate]).is_err());

        let mut duplicate = second.clone();
        duplicate.correlation_id = first.correlation_id.clone();
        assert!(validate_trial_identities(&[first.clone(), duplicate]).is_err());

        let mut duplicate = second.clone();
        duplicate.otel_trace_id = first.otel_trace_id.clone();
        assert!(validate_trial_identities(&[first.clone(), duplicate]).is_err());

        let mut duplicate = second.clone();
        duplicate.attachment_dir = first.attachment_dir.clone();
        assert!(validate_trial_identities(&[first.clone(), duplicate]).is_err());

        let mut invalid_trace = second;
        invalid_trace.otel_trace_id = Some("00000000000000000000000000000000".to_string());
        assert!(validate_trial_identities(&[first, invalid_trace]).is_err());
        assert!(validate_otel_trace_id("ABCDEF0123456789abcdef0123456789").is_err());
    }

    #[test]
    fn root_session_errors_are_sanitized_and_transient_errors_are_ignored() {
        let fatal = session_error("fatal", "runtime_failed", "secret response content");
        let error = root_session_error(&fatal).unwrap();
        assert_eq!(error, "root session emitted session.error");
        assert!(!error.contains("secret"));

        let transient = session_error("model_call", "retrying", "secret response content");
        assert!(root_session_error(&transient).is_none());
    }

    #[test]
    fn canonical_attachment_parent_must_remain_under_output_root() {
        let output_root = tempfile::tempdir().unwrap();
        let outside = tempfile::tempdir().unwrap();
        let inside = output_root.path().join("run").join("trial");
        fs::create_dir_all(&inside).unwrap();
        let canonical_root = fs::canonicalize(output_root.path()).unwrap();
        let canonical_inside = fs::canonicalize(inside).unwrap();
        let canonical_outside = fs::canonicalize(outside.path()).unwrap();

        assert!(validate_attachment_parent(&canonical_root, &canonical_inside).is_ok());
        assert!(validate_attachment_parent(&canonical_root, &canonical_outside).is_err());
    }

    #[cfg(any(unix, windows))]
    #[test]
    fn canonical_attachment_parent_rejects_symlink_escape() {
        let output_root = tempfile::tempdir().unwrap();
        let outside = tempfile::tempdir().unwrap();
        let link = output_root.path().join("escaped");
        #[cfg(unix)]
        std::os::unix::fs::symlink(outside.path(), &link).unwrap();
        #[cfg(windows)]
        if std::os::windows::fs::symlink_dir(outside.path(), &link).is_err() {
            return;
        }

        let canonical_root = fs::canonicalize(output_root.path()).unwrap();
        let canonical_parent = fs::canonicalize(link).unwrap();
        assert!(validate_attachment_parent(&canonical_root, &canonical_parent).is_err());
    }

    #[test]
    fn runtime_telemetry_projection_omits_content() {
        let trace_id = "1234567890abcdef1234567890abcdef";
        let raw = json!({
            "type": "span",
            "traceId": trace_id,
            "spanId": "1234567890abcdef",
            "name": "gen_ai.client.inference",
            "startTime": "2026-01-01T00:00:00Z",
            "attributes": {
                "gen_ai.request.model": "fixture-model",
                "gen_ai.input.messages": "secret"
            }
        })
        .to_string();
        let sanitized = sanitize_runtime_otel(&raw, trace_id, 1, 1).unwrap();
        assert!(sanitized.contains("fixture-model"));
        assert!(!sanitized.contains("secret"));
        assert!(!sanitized.contains("messages"));
    }

    #[test]
    fn create_rpc_boundaries_are_nested_in_total_create() {
        let mut observation = Observation::new();
        add_create_timing(
            &mut observation,
            10,
            110,
            CreateTiming {
                pre_rpc_us: 20,
                rpc_us: 50,
                post_rpc_us: 30,
            },
        );
        let begin = observation
            .milestones
            .iter()
            .find(|milestone| milestone.name == "session.create.rpc.begin")
            .unwrap();
        let end = observation
            .milestones
            .iter()
            .find(|milestone| milestone.name == "session.create.rpc.end")
            .unwrap();
        assert_eq!(begin.offset_us, 30);
        assert_eq!(end.offset_us, 80);
    }

    #[test]
    fn send_timing_splits_sdk_work_from_runtime_wait() {
        let mut observation = Observation::new();
        add_send_timing(
            &mut observation,
            10,
            110,
            SessionSendTiming {
                prepare_us: 10,
                rpc_us: 80,
            },
            RpcSendTiming {
                prepare_us: 5,
                write_us: 10,
                response_wait_us: 40,
                response_body_read_us: 5,
                response_parse_us: 5,
                response_dispatch_us: 5,
                response_delivery_us: 5,
            },
        );
        let offsets = observation
            .milestones
            .iter()
            .map(|milestone| (milestone.name.as_str(), milestone.offset_us))
            .collect::<BTreeMap<_, _>>();

        assert_eq!(offsets["sdk.session.send.local.prepare.end"], 20);
        assert_eq!(offsets["sdk_transport.jsonrpc.request.write.end"], 35);
        assert_eq!(offsets["runtime_transport.session.send.wait.begin"], 35);
        assert_eq!(offsets["runtime_transport.session.send.wait.end"], 75);
        assert_eq!(offsets["sdk.jsonrpc.response.delivery.end"], 95);
        assert_eq!(offsets["sdk.session.send.local.post.end"], 110);
    }

    #[test]
    fn assistant_delta_stages_use_the_trial_clock() {
        let mut observation = Observation::new();
        let origin = observation.origin;
        add_assistant_delta_timing(
            &mut observation,
            &[
                EventStageTiming {
                    phase: "jsonrpc.notification.ready".to_string(),
                    event_id: "delta-1".to_string(),
                    observed_at: origin + Duration::from_micros(100),
                    header_to_ready_us: Some(20),
                    body_read_us: Some(10),
                    parse_us: Some(5),
                    route_us: None,
                    dispatch_us: None,
                },
                EventStageTiming {
                    phase: "router.notification.ready".to_string(),
                    event_id: "delta-1".to_string(),
                    observed_at: origin + Duration::from_micros(120),
                    header_to_ready_us: None,
                    body_read_us: None,
                    parse_us: None,
                    route_us: Some(8),
                    dispatch_us: None,
                },
                EventStageTiming {
                    phase: "session.notification.ready".to_string(),
                    event_id: "delta-1".to_string(),
                    observed_at: origin + Duration::from_micros(140),
                    header_to_ready_us: None,
                    body_read_us: None,
                    parse_us: None,
                    route_us: None,
                    dispatch_us: Some(12),
                },
            ],
        );
        let offsets = observation
            .milestones
            .iter()
            .map(|milestone| (milestone.name.as_str(), milestone.offset_us))
            .collect::<BTreeMap<_, _>>();

        assert_eq!(offsets["sdk.assistant.delta.header.received"], 80);
        assert_eq!(
            offsets["runtime_transport.assistant.delta.body_read.begin"],
            85
        );
        assert_eq!(offsets["sdk.assistant.delta.parse.end"], 100);
        assert_eq!(offsets["sdk.assistant.delta.router.begin"], 112);
        assert_eq!(offsets["sdk.assistant.delta.session.begin"], 128);
        assert_eq!(offsets["sdk.assistant.delta.session.ready"], 140);
    }

    #[test]
    fn timing_layer_captures_content_free_send_and_event_fields() {
        let layer = CreateTimingLayer::default();
        let subscriber = Registry::default().with(layer.clone());
        tracing::subscriber::with_default(subscriber, || {
            tracing::debug!(
                perf_phase = "session.send.complete",
                prepare_us = 2_u64,
                rpc_us = 20_u64,
                "send timing"
            );
            tracing::debug!(
                perf_phase = "jsonrpc.request.complete",
                method = "session.send",
                prepare_us = 1_u64,
                write_us = 2_u64,
                response_wait_us = 10_u64,
                response_body_read_us = 1_u64,
                response_parse_us = 2_u64,
                response_dispatch_us = 1_u64,
                response_delivery_us = 1_u64,
                "RPC timing"
            );
            tracing::debug!(
                perf_phase = "session.notification.ready",
                event_type = "assistant.message_delta",
                event_id = "delta-1",
                dispatch_us = 3_u64,
                "event timing"
            );
        });

        assert_eq!(layer.pop_session_send().unwrap().prepare_us, 2);
        assert_eq!(layer.pop_rpc_send().unwrap().response_wait_us, 10);
        assert_eq!(layer.pop_assistant_delta_stages("delta-1").len(), 1);
    }

    #[test]
    fn event_stage_correlation_handles_interleaved_deltas() {
        let layer = CreateTimingLayer::default();
        let mut stages = layer.assistant_delta_stages.lock();
        for (event_id, phase) in [
            ("delta-1", "jsonrpc.notification.ready"),
            ("delta-2", "jsonrpc.notification.ready"),
            ("delta-1", "router.notification.ready"),
            ("delta-1", "session.notification.ready"),
        ] {
            stages.push_back(EventStageTiming {
                phase: phase.to_string(),
                event_id: event_id.to_string(),
                observed_at: Instant::now(),
                header_to_ready_us: None,
                body_read_us: None,
                parse_us: None,
                route_us: None,
                dispatch_us: None,
            });
        }
        drop(stages);

        let first = layer.pop_assistant_delta_stages("delta-1");
        assert_eq!(first.len(), 3);
        assert!(first.iter().all(|stage| stage.event_id == "delta-1"));
        let second = layer.pop_assistant_delta_stages("delta-2");
        assert_eq!(second.len(), 1);
    }
}
