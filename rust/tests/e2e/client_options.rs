use std::collections::HashMap;
use std::path::PathBuf;

use github_copilot_sdk::canvas::CanvasDeclaration;
use github_copilot_sdk::rpc::{OpenCanvasInstance, RemoteSessionMode};
use github_copilot_sdk::session_events::{ReasoningSummary, SessionLimitsConfig};
use github_copilot_sdk::{
    CliProgram, Client, ClientOptions, ExtensionInfo, ProviderConfig, ResumeSessionConfig,
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
                .with_model("claude-sonnet-4.5")
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
                .with_exp_assignments(json!({ "feature": "enabled" })),
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
            ("model", json!("claude-sonnet-4.5")),
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
    assert_eq!(params["expAssignments"]["feature"], json!("enabled"));

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
                .with_exp_assignments(json!({ "resumeFeature": "enabled" })),
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
    assert_eq!(params["expAssignments"]["resumeFeature"], json!("enabled"));

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
        ClientOptions::new()
            .with_program(CliProgram::Path(PathBuf::from("node")))
            .with_prefix_args([self.script_path.as_os_str().to_owned()])
            .with_cwd(&self.work_dir)
            .with_extra_args([
                "--capture-file".to_string(),
                self.capture_path.to_string_lossy().into_owned(),
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
    writeResponse(message.id, { ok: true, protocolVersion: 3, version: "fake" });
    return;
  }
  if (message.method === "ping") {
    writeResponse(message.id, { message: "pong", protocolVersion: 3, timestamp: Date.now() });
    return;
  }
  if (message.method === "session.create") {
    const sessionId = (message.params && message.params.sessionId) || "fake-session";
    writeResponse(message.id, { sessionId, workspacePath: null, capabilities: null });
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
  writeResponse(message.id, {});
}

function writeResponse(id, result) {
  const body = JSON.stringify({ jsonrpc: "2.0", id, result });
  process.stdout.write("Content-Length: " + Buffer.byteLength(body, "utf8") + "\r\n\r\n" + body);
}
"#;
