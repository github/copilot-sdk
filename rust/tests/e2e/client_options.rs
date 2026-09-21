use std::collections::HashMap;
use std::path::PathBuf;
use std::time::Duration;

use github_copilot_sdk::canvas::CanvasDeclaration;
use github_copilot_sdk::rpc::{ConnectRemoteSessionParams, OpenCanvasInstance, RemoteSessionMode};
use github_copilot_sdk::session_events::{
    ReasoningSummary, SessionEventType, SessionLimitsConfig, SessionStartData,
};
use github_copilot_sdk::{
    AgentMode, Attachment, AttachmentLineRange, AttachmentSelectionPosition,
    AttachmentSelectionRange, CliProgram, Client, ClientOptions, CloudSessionOptions,
    CloudSessionRepository, CopilotExpAssignmentResponse, DeliveryMode, ExtensionInfo,
    GitHubReferenceType, MessageOptions, MessageSource, ProviderConfig, ResumeSessionConfig,
    SessionConfig, SessionId, Transport,
};
use serde::Deserialize;
use serde_json::{Value, json};
use tempfile::TempDir;

#[tokio::test]
async fn should_forward_advanced_session_creation_options_to_the_cli() {
    let fake = FakeCli::new();
    let client = Client::start(fake.client_options("advanced-create-client-token"))
        .await
        .expect("start fake CLI client");

    let config_dir = fake.path("config");
    let working_dir = fake.path("workspace");
    let extension_sdk_path = fake.path("extension-sdk");
    let session = client
        .create_session(
            SessionConfig::default()
                .with_session_id("advanced-session-id")
                .with_client_name("rust-sdk-e2e-client")
                .with_model("claude-sonnet-5")
                .with_reasoning_effort("low")
                .with_reasoning_summary(ReasoningSummary::None)
                .with_context_tier("long_context")
                .with_config_directory(config_dir.clone())
                .with_enable_config_discovery(true)
                .with_skip_embedding_retrieval(true)
                .with_embedding_cache_storage("in-memory")
                .with_organization_custom_instructions("organization guidance")
                .with_enable_on_demand_instruction_discovery(true)
                .with_enable_file_hooks(false)
                .with_enable_host_git_operations(false)
                .with_enable_session_store(false)
                .with_enable_skills(false)
                .with_working_directory(working_dir.clone())
                .with_streaming(true)
                .with_include_sub_agent_streaming_events(false)
                .with_available_tools(["read_file"])
                .with_excluded_tools(["bash"])
                .with_excluded_builtin_agents(["legacy-agent"])
                .with_enable_session_telemetry(false)
                .with_enable_citations(true)
                .with_session_limits(SessionLimitsConfig {
                    max_ai_credits: Some(42.0),
                })
                .with_skip_custom_instructions(true)
                .with_custom_agents_local_only(true)
                .with_coauthor_enabled(false)
                .with_manage_schedule_enabled(false)
                .with_github_token("advanced-create-session-token")
                .with_remote_session(RemoteSessionMode::Export)
                .with_skill_directories([PathBuf::from("skills")])
                .with_plugin_directories([PathBuf::from("plugins")])
                .with_instruction_directories([PathBuf::from("instructions")])
                .with_disabled_skills(["disabled-skill"])
                .with_enable_mcp_apps(true)
                .with_canvases([CanvasDeclaration::new(
                    "canvas",
                    "Canvas",
                    "Canvas description",
                )])
                .with_request_canvas_renderer(true)
                .with_request_extensions(true)
                .with_extension_sdk_path(path_string(&extension_sdk_path))
                .with_extension_info(ExtensionInfo::new("github-app", "rust-e2e-extension"))
                .with_exp_assignments(CopilotExpAssignmentResponse {
                    flights: HashMap::from([("feature".to_string(), "enabled".to_string())]),
                    assignment_context: "ctx".to_string(),
                    ..Default::default()
                }),
        )
        .await
        .expect("create session");
    session.disconnect().await.expect("disconnect session");
    client.stop().await.expect("stop client");

    let create = fake.captured_request("session.create");
    let params = create.params.as_object().expect("session.create params");
    assert_json_values(
        params,
        [
            ("sessionId", json!("advanced-session-id")),
            ("clientName", json!("rust-sdk-e2e-client")),
            ("model", json!("claude-sonnet-5")),
            ("reasoningEffort", json!("low")),
            ("reasoningSummary", json!("none")),
            ("contextTier", json!("long_context")),
            ("configDir", json!(path_string(&config_dir))),
            ("enableConfigDiscovery", json!(true)),
            ("skipEmbeddingRetrieval", json!(true)),
            ("embeddingCacheStorage", json!("in-memory")),
            (
                "organizationCustomInstructions",
                json!("organization guidance"),
            ),
            ("enableOnDemandInstructionDiscovery", json!(true)),
            ("enableFileHooks", json!(false)),
            ("enableHostGitOperations", json!(false)),
            ("enableSessionStore", json!(false)),
            ("enableSkills", json!(false)),
            ("workingDirectory", json!(path_string(&working_dir))),
            ("streaming", json!(true)),
            ("includeSubAgentStreamingEvents", json!(false)),
            ("enableSessionTelemetry", json!(false)),
            ("enableCitations", json!(true)),
            ("gitHubToken", json!("advanced-create-session-token")),
            ("remoteSession", json!("export")),
            ("requestMcpApps", json!(true)),
            ("requestCanvasRenderer", json!(true)),
            ("requestExtensions", json!(true)),
            ("extensionSdkPath", json!(path_string(&extension_sdk_path))),
            ("envValueMode", json!("direct")),
        ],
    );
    assert_eq!(params["availableTools"], json!(["read_file"]));
    assert_eq!(params["excludedTools"], json!(["bash"]));
    assert_eq!(params["excludedBuiltinAgents"], json!(["legacy-agent"]));
    assert_eq!(params["skillDirectories"], json!(["skills"]));
    assert_eq!(params["pluginDirectories"], json!(["plugins"]));
    assert_eq!(params["instructionDirectories"], json!(["instructions"]));
    assert_eq!(params["disabledSkills"], json!(["disabled-skill"]));
    assert_eq!(params["sessionLimits"]["maxAiCredits"], json!(42));
    assert_eq!(
        params["extensionInfo"],
        json!({ "source": "github-app", "name": "rust-e2e-extension" })
    );
    assert_eq!(params["canvases"][0]["id"], json!("canvas"));
    assert_eq!(params["canvases"][0]["displayName"], json!("Canvas"));
    assert_eq!(
        params["canvases"][0]["description"],
        json!("Canvas description")
    );
    assert_eq!(
        params["expAssignments"]["Flights"]["feature"],
        json!("enabled")
    );

    let update = fake.captured_request("session.options.update");
    let update_params = update.params.as_object().expect("options update params");
    assert_json_values(
        update_params,
        [
            ("sessionId", json!("advanced-session-id")),
            ("skipCustomInstructions", json!(true)),
            ("customAgentsLocalOnly", json!(true)),
            ("coauthorEnabled", json!(false)),
            ("manageScheduleEnabled", json!(false)),
        ],
    );
}

#[tokio::test]
async fn should_forward_singular_provider_configuration_on_session_creation() {
    let fake = FakeCli::new();
    let client = Client::start(fake.client_options("provider-client-token"))
        .await
        .expect("start fake CLI client");

    let session = client
        .create_session(
            SessionConfig::default().with_provider(
                ProviderConfig::new("https://models.example.test/v1")
                    .with_provider_type("openai")
                    .with_wire_api("responses")
                    .with_transport("websockets")
                    .with_api_key("provider-key")
                    .with_model_id("base-model")
                    .with_wire_model("wire-model")
                    .with_max_prompt_tokens(1000)
                    .with_max_output_tokens(2000)
                    .with_headers(HashMap::from([(
                        "x-provider".to_string(),
                        "rust".to_string(),
                    )])),
            ),
        )
        .await
        .expect("create session");
    session.disconnect().await.expect("disconnect session");
    client.stop().await.expect("stop client");

    let create = fake.captured_request("session.create");
    let provider = create.params["provider"]
        .as_object()
        .expect("provider params");
    assert_json_values(
        provider,
        [
            ("type", json!("openai")),
            ("wireApi", json!("responses")),
            ("transport", json!("websockets")),
            ("baseUrl", json!("https://models.example.test/v1")),
            ("apiKey", json!("provider-key")),
            ("modelId", json!("base-model")),
            ("wireModel", json!("wire-model")),
            ("maxPromptTokens", json!(1000)),
            ("maxOutputTokens", json!(2000)),
        ],
    );
    assert_eq!(provider["headers"]["x-provider"], json!("rust"));
}

#[tokio::test]
async fn should_forward_advanced_session_resume_options_to_the_cli() {
    let fake = FakeCli::new();
    let client = Client::start(fake.client_options("advanced-resume-client-token"))
        .await
        .expect("start fake CLI client");

    let config_dir = fake.path("resume-config");
    let working_dir = fake.path("resume-workspace");
    let extension_sdk_path = fake.path("resume-extension-sdk");
    let session = client
        .resume_session(
            ResumeSessionConfig::new(SessionId::from("resume-session-id"))
                .with_model("gpt-5-mini")
                .with_reasoning_effort("low")
                .with_reasoning_summary(ReasoningSummary::None)
                .with_context_tier("long_context")
                .with_working_directory(working_dir.clone())
                .with_config_directory(config_dir.clone())
                .with_enable_config_discovery(false)
                .with_suppress_resume_event(true)
                .with_continue_pending_work(false)
                .with_streaming(true)
                .with_include_sub_agent_streaming_events(false)
                .with_github_token("advanced-resume-session-token")
                .with_canvases([CanvasDeclaration::new(
                    "resume-canvas",
                    "Resume Canvas",
                    "Resume canvas description",
                )])
                .with_open_canvases([OpenCanvasInstance {
                    canvas_id: "resume-canvas".to_string(),
                    extension_id: "github-app/rust-e2e-extension".to_string(),
                    extension_name: None,
                    icon: None,
                    input: Some(json!({ "value": "from-resume" })),
                    instance_id: "resume-instance".to_string(),
                    status: None,
                    title: None,
                    url: None,
                }])
                .with_request_canvas_renderer(true)
                .with_request_extensions(true)
                .with_extension_sdk_path(path_string(&extension_sdk_path))
                .with_extension_info(ExtensionInfo::new("github-app", "rust-e2e-extension"))
                .with_skip_custom_instructions(true)
                .with_custom_agents_local_only(true)
                .with_coauthor_enabled(false)
                .with_manage_schedule_enabled(false)
                .with_exp_assignments(CopilotExpAssignmentResponse {
                    flights: HashMap::from([("resumeFeature".to_string(), "enabled".to_string())]),
                    assignment_context: "ctx".to_string(),
                    ..Default::default()
                }),
        )
        .await
        .expect("resume session");
    session.disconnect().await.expect("disconnect session");
    client.stop().await.expect("stop client");

    let resume = fake.captured_request("session.resume");
    let params = resume.params.as_object().expect("session.resume params");
    assert_json_values(
        params,
        [
            ("sessionId", json!("resume-session-id")),
            ("model", json!("gpt-5-mini")),
            ("reasoningEffort", json!("low")),
            ("reasoningSummary", json!("none")),
            ("contextTier", json!("long_context")),
            ("workingDirectory", json!(path_string(&working_dir))),
            ("configDir", json!(path_string(&config_dir))),
            ("enableConfigDiscovery", json!(false)),
            ("disableResume", json!(true)),
            ("continuePendingWork", json!(false)),
            ("streaming", json!(true)),
            ("includeSubAgentStreamingEvents", json!(false)),
            ("gitHubToken", json!("advanced-resume-session-token")),
            ("requestCanvasRenderer", json!(true)),
            ("requestExtensions", json!(true)),
            ("extensionSdkPath", json!(path_string(&extension_sdk_path))),
            ("envValueMode", json!("direct")),
        ],
    );
    assert_eq!(
        params["openCanvases"][0]["canvasId"],
        json!("resume-canvas")
    );
    assert_eq!(
        params["openCanvases"][0]["extensionId"],
        json!("github-app/rust-e2e-extension")
    );
    assert_eq!(
        params["openCanvases"][0]["instanceId"],
        json!("resume-instance")
    );
    assert_eq!(
        params["extensionInfo"],
        json!({ "source": "github-app", "name": "rust-e2e-extension" })
    );
    assert_eq!(
        params["expAssignments"]["Flights"]["resumeFeature"],
        json!("enabled")
    );

    let update = fake.captured_request("session.options.update");
    let update_params = update.params.as_object().expect("options update params");
    assert_json_values(
        update_params,
        [
            ("sessionId", json!("resume-session-id")),
            ("skipCustomInstructions", json!(true)),
            ("customAgentsLocalOnly", json!(true)),
            ("coauthorEnabled", json!(false)),
            ("manageScheduleEnabled", json!(false)),
        ],
    );
}

#[tokio::test]
async fn should_send_complete_message_wire_shape() {
    let fake = FakeCli::new();
    let client = Client::start(fake.client_options("message-wire-client-token"))
        .await
        .expect("start fake CLI client");
    let session = client
        .create_session(SessionConfig::default())
        .await
        .expect("create session");
    let file_path = fake.path("message-file").join("scenario.txt");
    let directory_path = fake.path("message-directory");
    let selection_path = fake.path("selection").join("Program.rs");

    let message_id = session
        .send(
            MessageOptions::new("Use the hidden scenario context.")
                .with_display_prompt("Review selected scenario context")
                .with_mode(DeliveryMode::Enqueue)
                .with_agent_mode(AgentMode::Interactive)
                .with_source(MessageSource::Agent("scenario-client".to_string()))
                .with_traceparent("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01")
                .with_tracestate("scenario-client=send")
                .with_attachments(vec![
                    Attachment::File {
                        path: file_path.clone(),
                        display_name: Some("scenario.txt".to_string()),
                        line_range: Some(AttachmentLineRange { start: 3, end: 9 }),
                    },
                    Attachment::Directory {
                        path: directory_path.clone(),
                        display_name: Some("message-directory".to_string()),
                    },
                    Attachment::Selection {
                        file_path: selection_path.clone(),
                        text: "SCENARIO_SELECTION".to_string(),
                        display_name: Some("Program.rs".to_string()),
                        selection: AttachmentSelectionRange {
                            start: AttachmentSelectionPosition {
                                line: 17,
                                character: 0,
                            },
                            end: AttachmentSelectionPosition {
                                line: 17,
                                character: 18,
                            },
                        },
                    },
                    Attachment::GitHubReference {
                        number: 610,
                        reference_type: GitHubReferenceType::Pr,
                        state: "open".to_string(),
                        title: "Scenario-shaped E2E coverage".to_string(),
                        url: "https://github.com/github/copilot-sdk/pull/610".to_string(),
                    },
                    Attachment::Blob {
                        data: "QVBQX0JMT0I=".to_string(),
                        mime_type: "text/plain".to_string(),
                        display_name: Some("scenario-wire-blob.txt".to_string()),
                    },
                    Attachment::ExtensionContext {
                        captured_at: "2026-09-17T20:00:00Z".to_string(),
                        extension_id: "scenario-client:code-review".to_string(),
                        canvas_id: Some("diff".to_string()),
                        instance_id: Some("diff-17".to_string()),
                        title: "Selected change".to_string(),
                        payload: Some(json!({ "selection": "SCENARIO_SELECTION", "line": 17 })),
                    },
                ]),
        )
        .await
        .expect("send complete message");
    assert_eq!(message_id, "scenario-client-message");

    session.disconnect().await.expect("disconnect session");
    client.stop().await.expect("stop client");

    let send = fake.captured_request("session.send");
    let params = send.params.as_object().expect("session.send params");
    assert_json_values(
        params,
        [
            ("prompt", json!("Use the hidden scenario context.")),
            ("displayPrompt", json!("Review selected scenario context")),
            ("mode", json!("enqueue")),
            ("agentMode", json!("interactive")),
            ("source", json!("agent-scenario-client")),
            (
                "traceparent",
                json!("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"),
            ),
            ("tracestate", json!("scenario-client=send")),
        ],
    );
    let attachments = params["attachments"].as_array().expect("attachments");
    assert_eq!(
        attachments
            .iter()
            .map(|attachment| attachment["type"].as_str().expect("attachment type"))
            .collect::<Vec<_>>(),
        [
            "file",
            "directory",
            "selection",
            "github_reference",
            "blob",
            "extension_context"
        ]
    );
    assert_eq!(attachments[0]["path"], json!(path_string(&file_path)));
    assert_eq!(attachments[0]["lineRange"], json!({ "start": 3, "end": 9 }));
    assert_eq!(attachments[1]["path"], json!(path_string(&directory_path)));
    assert_eq!(
        attachments[2]["filePath"],
        json!(path_string(&selection_path))
    );
    assert_eq!(attachments[2]["text"], json!("SCENARIO_SELECTION"));
    assert_eq!(attachments[3]["number"], json!(610));
    assert_eq!(attachments[3]["referenceType"], json!("pr"));
    assert_eq!(attachments[4]["data"], json!("QVBQX0JMT0I="));
    assert_eq!(attachments[4]["mimeType"], json!("text/plain"));
    assert_eq!(
        attachments[5]["extensionId"],
        json!("scenario-client:code-review")
    );
    assert_eq!(
        attachments[5]["payload"]["selection"],
        json!("SCENARIO_SELECTION")
    );
}

#[tokio::test]
async fn dropping_unpolled_send_never_dispatches_for_any_delivery_mode() {
    let fake = FakeCli::new();
    let client = Client::start(fake.client_options("cancelled-send-client-token"))
        .await
        .expect("start fake CLI client");
    let session = client
        .create_session(SessionConfig::default())
        .await
        .expect("create session");

    for mode in [
        None,
        Some(DeliveryMode::Enqueue),
        Some(DeliveryMode::Immediate),
    ] {
        let mut message = MessageOptions::new("This message must never be invoked.")
            .with_display_prompt("Cancelled scenario message")
            .with_source(MessageSource::Agent("scenario-client".to_string()));
        message.mode = mode;
        drop(session.send(message));
    }

    session.disconnect().await.expect("disconnect session");
    client.stop().await.expect("stop client");
    assert!(
        fake.capture()
            .requests
            .iter()
            .all(|request| request.method != "session.send")
    );
}

#[tokio::test]
async fn transport_loss_never_replays_send_for_any_delivery_mode() {
    for (mode, expected_mode) in [
        (None, None),
        (Some(DeliveryMode::Enqueue), Some("enqueue")),
        (Some(DeliveryMode::Immediate), Some("immediate")),
    ] {
        let fake = FakeCli::new();
        let client = Client::start(
            fake.client_options_with_behavior("ambiguous-send-client-token", "drop-after-send"),
        )
        .await
        .expect("start fake CLI client");
        let session = client
            .create_session(SessionConfig::default())
            .await
            .expect("create session");
        let mut message = MessageOptions::new("AMBIGUOUS_SCENARIO_SEND")
            .with_display_prompt("Ambiguous scenario send")
            .with_source(MessageSource::Agent("scenario-client".to_string()));
        message.mode = mode;

        let error = session
            .send(message)
            .await
            .expect_err("transport loss should fail send");
        assert!(error.is_transport_failure());
        client.force_stop();

        let sends = fake
            .capture()
            .requests
            .into_iter()
            .filter(|request| request.method == "session.send")
            .collect::<Vec<_>>();
        assert_eq!(sends.len(), 1);
        assert_eq!(
            sends[0].params.get("mode").and_then(Value::as_str),
            expected_mode
        );
    }
}

#[tokio::test]
async fn cloud_create_routes_first_event_for_server_assigned_session_id() {
    let fake = FakeCli::new();
    let client = Client::start(fake.client_options("cloud-create-client-token"))
        .await
        .expect("start fake CLI client");
    let prepared = client
        .prepare_session(
            SessionConfig::default().with_cloud(CloudSessionOptions::with_repository(
                CloudSessionRepository::new("github", "copilot-sdk").with_branch("main"),
            )),
        )
        .expect("prepare cloud session");
    let mut events = prepared.subscribe();
    let session = prepared.start().await.expect("start cloud session");

    assert_eq!(session.id().as_str(), "server-assigned-cloud-session");
    let event = tokio::time::timeout(Duration::from_secs(5), events.recv())
        .await
        .expect("first cloud event timed out")
        .expect("cloud event stream closed");
    assert_eq!(event.parsed_type(), SessionEventType::SessionStart);
    assert_eq!(
        event
            .typed_data::<SessionStartData>()
            .expect("session.start data")
            .session_id,
        session.id().clone()
    );

    session.disconnect().await.expect("disconnect session");
    client.stop().await.expect("stop client");
    let create = fake.captured_request("session.create");
    assert!(create.params.get("sessionId").is_none());
    assert_eq!(
        create.params["cloud"]["repository"]["owner"],
        json!("github")
    );
}

#[tokio::test]
async fn remote_connect_runtime_id_is_used_for_resume() {
    let fake = FakeCli::new();
    let client = Client::start(fake.client_options("cloud-connect-client-token"))
        .await
        .expect("start fake CLI client");

    let connection = client
        .rpc()
        .sessions()
        .connect(ConnectRemoteSessionParams {
            session_id: SessionId::from("cloud-control-session"),
        })
        .await
        .expect("connect remote session");
    assert_eq!(connection.session_id.as_str(), "runtime-session-id");
    assert_eq!(connection.metadata.session_id, connection.session_id);
    assert_eq!(
        connection.metadata.resource_id.as_deref(),
        Some("github/copilot-sdk#123")
    );
    let resumed = client
        .resume_session(ResumeSessionConfig::new(connection.session_id.clone()))
        .await
        .expect("resume connected runtime session");
    assert_eq!(resumed.id(), &connection.session_id);

    resumed.disconnect().await.expect("disconnect session");
    client.stop().await.expect("stop client");
    assert_eq!(
        fake.captured_request("sessions.connect").params["sessionId"],
        json!("cloud-control-session")
    );
    assert_eq!(
        fake.captured_request("session.resume").params["sessionId"],
        json!("runtime-session-id")
    );
}

#[tokio::test]
async fn remote_resource_mismatch_is_observable_before_resume() {
    let fake = FakeCli::new();
    let client = Client::start(
        fake.client_options_with_behavior("cloud-mismatch-client-token", "resource-mismatch"),
    )
    .await
    .expect("start fake CLI client");

    let connection = client
        .rpc()
        .sessions()
        .connect(ConnectRemoteSessionParams {
            session_id: SessionId::from("cloud-control-session"),
        })
        .await
        .expect("connect remote session");
    assert_ne!(
        connection.metadata.resource_id.as_deref(),
        Some("github/copilot-sdk#123")
    );

    client.stop().await.expect("stop client");
    assert!(
        fake.capture()
            .requests
            .iter()
            .all(|request| request.method != "session.resume")
    );
}

struct FakeCli {
    _dir: TempDir,
    script_path: PathBuf,
    capture_path: PathBuf,
    work_dir: PathBuf,
}

impl FakeCli {
    fn new() -> Self {
        let dir = tempfile::tempdir().expect("create fake CLI temp dir");
        let script_path = dir.path().join("fake-cli.js");
        let capture_path = dir.path().join("fake-cli-capture.json");
        let work_dir = dir.path().join("cwd");
        std::fs::create_dir(&work_dir).expect("create fake CLI cwd");
        std::fs::write(&script_path, FAKE_STDIO_CLI_SCRIPT).expect("write fake CLI script");
        Self {
            _dir: dir,
            script_path,
            capture_path,
            work_dir,
        }
    }

    fn client_options(&self, token: &str) -> ClientOptions {
        self.client_options_with_behavior(token, "normal")
    }

    fn client_options_with_behavior(&self, token: &str, behavior: &str) -> ClientOptions {
        ClientOptions::new()
            .with_program(CliProgram::Path(PathBuf::from("node")))
            .with_prefix_args([self.script_path.as_os_str().to_owned()])
            .with_cwd(&self.work_dir)
            .with_extra_args([
                "--capture-file".to_string(),
                self.capture_path.to_string_lossy().into_owned(),
                "--behavior".to_string(),
                behavior.to_string(),
            ])
            .with_github_token(token)
            .with_use_logged_in_user(false)
            .with_transport(Transport::Stdio)
    }

    fn path(&self, name: &str) -> PathBuf {
        let path = self.work_dir.join(name);
        std::fs::create_dir_all(&path).expect("create fake CLI test path");
        path
    }

    fn captured_request(&self, method: &str) -> CapturedRequest {
        let capture = self.capture();
        capture
            .requests
            .iter()
            .find(|request| request.method == method)
            .cloned()
            .unwrap_or_else(|| panic!("expected {method} request in {capture:?}"))
    }

    fn capture(&self) -> CapturedCli {
        let text = std::fs::read_to_string(&self.capture_path).expect("read fake CLI capture file");
        serde_json::from_str(&text).expect("parse fake CLI capture file")
    }
}

#[derive(Debug, Deserialize)]
struct CapturedCli {
    requests: Vec<CapturedRequest>,
}

#[derive(Debug, Clone, Deserialize)]
struct CapturedRequest {
    method: String,
    #[serde(default)]
    params: Value,
}

fn assert_json_values<'a>(
    object: &serde_json::Map<String, Value>,
    expected: impl IntoIterator<Item = (&'a str, Value)>,
) {
    for (key, expected_value) in expected {
        assert_eq!(
            object.get(key),
            Some(&expected_value),
            "unexpected value for key {key} in {object:?}"
        );
    }
}

fn path_string(path: &std::path::Path) -> String {
    path.to_string_lossy().into_owned()
}

const FAKE_STDIO_CLI_SCRIPT: &str = r#"
const fs = require("fs");

const captureIndex = process.argv.indexOf("--capture-file");
const captureFile = captureIndex >= 0 ? process.argv[captureIndex + 1] : undefined;
const behaviorIndex = process.argv.indexOf("--behavior");
const behavior = behaviorIndex >= 0 ? process.argv[behaviorIndex + 1] : "normal";
const requests = [];

function saveCapture() {
  if (!captureFile) {
    return;
  }
  fs.writeFileSync(captureFile, JSON.stringify({
    requests,
    args: process.argv.slice(2),
    cwd: process.cwd(),
    env: {
      COPILOT_SDK_AUTH_TOKEN: process.env.COPILOT_SDK_AUTH_TOKEN,
    },
  }));
}

saveCapture();

let buffer = Buffer.alloc(0);
process.stdin.on("data", chunk => {
  buffer = Buffer.concat([buffer, chunk]);
  processBuffer();
});
process.stdin.resume();

function processBuffer() {
  while (true) {
    const headerEnd = buffer.indexOf("\r\n\r\n");
    if (headerEnd < 0) return;
    const header = buffer.subarray(0, headerEnd).toString("utf8");
    const match = /Content-Length:\s*(\d+)/i.exec(header);
    if (!match) throw new Error("Missing Content-Length header");
    const length = Number(match[1]);
    const bodyStart = headerEnd + 4;
    const bodyEnd = bodyStart + length;
    if (buffer.length < bodyEnd) return;
    const body = buffer.subarray(bodyStart, bodyEnd).toString("utf8");
    buffer = buffer.subarray(bodyEnd);
    handleMessage(JSON.parse(body));
  }
}

function handleMessage(message) {
  if (!Object.prototype.hasOwnProperty.call(message, "id")) {
    return;
  }
  requests.push({ method: message.method, params: message.params });
  saveCapture();
  if (message.method === "connect") {
    writeResponse(message.id, { ok: true, protocolVersion: 4, version: "fake" });
    return;
  }
  if (message.method === "ping") {
    writeResponse(message.id, { message: "pong", protocolVersion: 4, timestamp: Date.now() });
    return;
  }
  if (message.method === "session.create") {
    const isCloud = Boolean(message.params && message.params.cloud);
    const sessionId = (message.params && message.params.sessionId)
      || (isCloud ? "server-assigned-cloud-session" : "fake-session");
    writeResponse(message.id, { sessionId, workspacePath: null, capabilities: null });
    if (isCloud) {
      writeMessage({
        jsonrpc: "2.0",
        method: "session.event",
        params: {
          sessionId,
          event: {
            id: "cloud-start-event",
            timestamp: "2026-09-18T00:00:00Z",
            parentId: null,
            type: "session.start",
            data: {
              sessionId,
              version: 1,
              producer: "fake-cli",
              copilotVersion: "fake",
              startTime: "2026-09-18T00:00:00Z",
            }
          }
        }
      });
    }
    return;
  }
  if (message.method === "sessions.connect") {
    const resourceId = behavior === "resource-mismatch"
      ? "github/other-repository#456"
      : "github/copilot-sdk#123";
    writeResponse(message.id, {
      sessionId: "runtime-session-id",
      metadata: {
        kind: "coding_agent",
        modifiedTime: "2026-09-18T00:00:00Z",
        repository: { owner: "github", name: "copilot-sdk", branch: "main" },
        resourceId,
        sessionId: "runtime-session-id",
        startTime: "2026-09-18T00:00:00Z"
      }
    });
    return;
  }
  if (message.method === "session.send") {
    if (behavior === "drop-after-send") {
      process.exit(0);
      return;
    }
    writeResponse(message.id, { messageId: "scenario-client-message" });
    return;
  }
  if (message.method === "session.resume") {
    const sessionId = (message.params && message.params.sessionId) || "fake-session";
    writeResponse(message.id, { sessionId, workspacePath: null, capabilities: null, openCanvases: [] });
    return;
  }
  if (message.method === "session.options.update") {
    writeResponse(message.id, { success: true });
    return;
  }
  if (message.method === "session.detach") {
    writeResponse(message.id, { success: true });
    return;
  }
  writeResponse(message.id, {});
}

function writeResponse(id, result) {
  writeMessage({ jsonrpc: "2.0", id, result });
}

function writeMessage(message) {
  const body = JSON.stringify(message);
  process.stdout.write("Content-Length: " + Buffer.byteLength(body, "utf8") + "\r\n\r\n" + body);
}
"#;
