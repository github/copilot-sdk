"""
E2E coverage for ``CopilotClient`` configuration options exposed via
``CopilotClientOptions`` and ``RuntimeConnection``.

Mirrors ``dotnet/test/ClientOptionsTests.cs``. The two CliUrl-conflict tests
(``Should_Throw_When_GitHubToken_Used_With_CliUrl`` and
``Should_Throw_When_UseLoggedInUser_Used_With_CliUrl``) have no Python
equivalent because Python's ``RuntimeConnection.for_uri(...)`` does not accept
``github_token`` / ``use_logged_in_user`` fields at all (those live on
``CopilotClientOptions``, but a Uri-connected runtime ignores them), so the
conflict cannot be expressed in code and the configurations are therefore
intentionally omitted.
"""

from __future__ import annotations

import json
import os
import socket

import pytest

from copilot import (
    CanvasDeclaration,
    CloudSessionOptions,
    CloudSessionRepository,
    CopilotClient,
    ExtensionInfo,
    OpenCanvasInstance,
    RemoteSessionMode,
    RuntimeConnection,
)
from copilot.rpc import PingRequest
from copilot.session import PermissionHandler

from .testharness import DEFAULT_GITHUB_TOKEN, E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


def _make_options(
    ctx: E2ETestContext,
    *,
    use_tcp: bool = False,
    port: int = 0,
    connection_token: str | None = None,
    cli_path: str | None = None,
    cli_args: list[str] | None = None,
    **overrides,
) -> dict[str, object]:
    """Build CopilotClient kwargs pre-populated for the test harness."""
    if use_tcp:
        connection: RuntimeConnection = RuntimeConnection.for_tcp(
            port=port,
            connection_token=connection_token,
            path=cli_path if cli_path is not None else ctx.cli_path,
            args=tuple(cli_args or []),
        )
    else:
        connection = RuntimeConnection.for_stdio(
            path=cli_path if cli_path is not None else ctx.cli_path,
            args=tuple(cli_args or []),
        )
    base: dict[str, object] = {
        "connection": connection,
        "working_directory": ctx.work_dir,
        "env": ctx.get_env(),
        "github_token": DEFAULT_GITHUB_TOKEN,
    }
    base.update(overrides)
    return base


def _get_available_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


# ------------------- A scriptable fake CLI to capture process options -------------------

FAKE_STDIO_CLI_SCRIPT = r"""
const fs = require("fs");

const captureIndex = process.argv.indexOf("--capture-file");
const captureFile = captureIndex >= 0 ? process.argv[captureIndex + 1] : undefined;
const requests = [];

function saveCapture() {
  if (!captureFile) {
    return;
  }
    fs.writeFileSync(captureFile, JSON.stringify({
    args: process.argv.slice(2),
    cwd: process.cwd(),
    requests,
    env: {
      COPILOT_HOME: process.env.COPILOT_HOME,
      COPILOT_SDK_AUTH_TOKEN: process.env.COPILOT_SDK_AUTH_TOKEN,
      COPILOT_OTEL_ENABLED: process.env.COPILOT_OTEL_ENABLED,
      OTEL_EXPORTER_OTLP_ENDPOINT: process.env.OTEL_EXPORTER_OTLP_ENDPOINT,
      OTEL_EXPORTER_OTLP_PROTOCOL: process.env.OTEL_EXPORTER_OTLP_PROTOCOL,
      COPILOT_OTEL_FILE_EXPORTER_PATH: process.env.COPILOT_OTEL_FILE_EXPORTER_PATH,
      COPILOT_OTEL_EXPORTER_TYPE: process.env.COPILOT_OTEL_EXPORTER_TYPE,
      COPILOT_OTEL_SOURCE_NAME: process.env.COPILOT_OTEL_SOURCE_NAME,
      OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT:
        process.env.OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT,
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
    const sessionId = message.params?.sessionId ?? message.params?.session_id ?? "fake-session";
    writeResponse(message.id, { sessionId, workspacePath: null, capabilities: null });
    return;
  }
  if (message.method === "session.resume") {
    const sessionId = message.params?.sessionId ?? message.params?.session_id ?? "fake-session";
    writeResponse(message.id, {
      sessionId,
      workspacePath: null,
      capabilities: null,
      openCanvases: message.params?.openCanvases ?? [],
    });
    return;
  }
  writeResponse(message.id, {});
}

function writeResponse(id, result) {
  const body = JSON.stringify({ jsonrpc: "2.0", id, result });
  process.stdout.write(`Content-Length: ${Buffer.byteLength(body, "utf8")}\r\n\r\n${body}`);
}
"""


def _assert_arg_value(args: list[str], name: str, expected_value: str) -> None:
    assert name in args, f"Expected argument '{name}' was not present. Args: {args}"
    index = args.index(name)
    assert index + 1 < len(args), f"Expected argument '{name}' to have a value."
    assert args[index + 1] == expected_value


def _get_captured_request(capture_path: str, method: str) -> dict:
    with open(capture_path) as f:
        capture = json.load(f)
    request = next((r for r in capture["requests"] if r["method"] == method), None)
    assert request is not None, f"Expected {method} request in capture"
    return request["params"]


class TestClientOptions:
    async def test_should_listen_on_configured_tcp_port(self, ctx: E2ETestContext):
        port = _get_available_port()
        client = CopilotClient(**_make_options(ctx, use_tcp=True, port=port))
        try:
            await client.start()
            assert client.runtime_port == port

            response = await client.rpc.ping(PingRequest(message="fixed-port"))
            assert "pong" in response.message
        finally:
            await client.stop()

    async def test_should_use_client_cwd_for_default_workingdirectory(self, ctx: E2ETestContext):
        client_cwd = os.path.join(ctx.work_dir, "client-cwd")
        os.makedirs(client_cwd, exist_ok=True)
        with open(os.path.join(client_cwd, "marker.txt"), "w") as f:
            f.write("I am in the client cwd")

        client = CopilotClient(**_make_options(ctx, working_directory=client_cwd))
        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                message = await session.send_and_wait(
                    "Read the file marker.txt and tell me what it says"
                )
                assert "client cwd" in (message.data.content or "")
            finally:
                await session.disconnect()
        finally:
            await client.stop()

    async def test_should_propagate_process_options_to_spawned_cli(self, ctx: E2ETestContext):
        cli_path = os.path.join(ctx.work_dir, "fake-cli.js")
        capture_path = os.path.join(ctx.work_dir, "fake-cli-capture.json")
        telemetry_path = os.path.join(ctx.work_dir, "telemetry.jsonl")
        copilot_home_from_env = os.path.join(ctx.work_dir, "copilot-home-from-env")
        copilot_home_from_option = os.path.join(ctx.work_dir, "copilot-home-from-option")
        with open(cli_path, "w") as f:
            f.write(FAKE_STDIO_CLI_SCRIPT)

        client = CopilotClient(
            **_make_options(
                ctx,
                cli_path=cli_path,
                base_directory=copilot_home_from_option,
                cli_args=["--capture-file", capture_path],
                env={**ctx.get_env(), "COPILOT_HOME": copilot_home_from_env},
                github_token="process-option-token",
                log_level="debug",
                session_idle_timeout_seconds=17,
                telemetry={
                    "otlp_endpoint": "http://127.0.0.1:4318",
                    "otlp_protocol": "http/protobuf",
                    "file_path": telemetry_path,
                    "exporter_type": "file",
                    "source_name": "python-sdk-e2e",
                    "capture_content": True,
                },
                use_logged_in_user=False,
            ),
        )
        try:
            await client.start()

            with open(capture_path) as f:
                capture = json.load(f)

            args = capture["args"]
            env = capture["env"]

            _assert_arg_value(args, "--log-level", "debug")
            assert "--stdio" in args
            _assert_arg_value(args, "--auth-token-env", "COPILOT_SDK_AUTH_TOKEN")
            assert "--no-auto-login" in args
            _assert_arg_value(args, "--session-idle-timeout", "17")
            assert os.path.realpath(capture["cwd"]) == os.path.realpath(ctx.work_dir)

            assert env["COPILOT_HOME"] == copilot_home_from_option
            assert env["COPILOT_SDK_AUTH_TOKEN"] == "process-option-token"
            assert env["COPILOT_OTEL_ENABLED"] == "true"
            assert env["OTEL_EXPORTER_OTLP_ENDPOINT"] == "http://127.0.0.1:4318"
            assert env["OTEL_EXPORTER_OTLP_PROTOCOL"] == "http/protobuf"
            assert env["COPILOT_OTEL_FILE_EXPORTER_PATH"] == telemetry_path
            assert env["COPILOT_OTEL_EXPORTER_TYPE"] == "file"
            assert env["COPILOT_OTEL_SOURCE_NAME"] == "python-sdk-e2e"
            assert env["OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT"] == "true"

            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                enable_config_discovery=True,
                enable_on_demand_instruction_discovery=True,
                include_sub_agent_streaming_events=False,
            )
            try:
                with open(capture_path) as f:
                    capture = json.load(f)
                create_request = next(
                    r for r in capture["requests"] if r["method"] == "session.create"
                )
                params = create_request["params"]
                assert params["enableConfigDiscovery"] is True
                assert params["enableOnDemandInstructionDiscovery"] is True
                assert params["includeSubAgentStreamingEvents"] is False
            finally:
                await session.disconnect()
        finally:
            try:
                await client.stop()
            except Exception:
                await client.force_stop()

    async def test_should_forward_advanced_session_options_in_create_wire_request(
        self, ctx: E2ETestContext
    ):
        cli_path = os.path.join(ctx.work_dir, f"fake-cli-advanced-create-{os.getpid()}.js")
        capture_path = os.path.join(
            ctx.work_dir, f"fake-cli-advanced-create-capture-{os.getpid()}.json"
        )
        output_directory = os.path.join(ctx.work_dir, "large-output-create")
        with open(cli_path, "w") as f:
            f.write(FAKE_STDIO_CLI_SCRIPT)

        client = CopilotClient(
            **_make_options(
                ctx,
                cli_path=cli_path,
                cli_args=["--capture-file", capture_path],
                use_logged_in_user=False,
            )
        )
        try:
            await client.start()
            session = await client.create_session(
                client_name="advanced-create-client",
                model="claude-sonnet-4.5",
                reasoning_effort="medium",
                reasoning_summary="detailed",
                context_tier="long_context",
                enable_citations=True,
                capi={"enable_web_socket_responses": False},
                mcp_oauth_token_storage="persistent",
                custom_agents=[
                    {
                        "name": "agent-one",
                        "display_name": "Agent One",
                        "description": "Handles agent-one tasks.",
                        "prompt": "Be agent one.",
                        "tools": ["view"],
                        "infer": True,
                        "skills": ["create-skill"],
                        "model": "claude-haiku-4.5",
                    }
                ],
                default_agent={"excluded_tools": ["edit"]},
                agent="agent-one",
                skill_directories=["skills-create"],
                disabled_skills=["disabled-create-skill"],
                plugin_directories=["plugins-create"],
                infinite_sessions={
                    "enabled": False,
                    "background_compaction_threshold": 0.5,
                    "buffer_exhaustion_threshold": 0.9,
                },
                large_output={
                    "enabled": True,
                    "max_size_bytes": 4096,
                    "output_directory": output_directory,
                },
                memory={"enabled": True},
                github_token="session-create-token",
                remote_session=RemoteSessionMode.EXPORT,
                cloud=CloudSessionOptions(
                    repository=CloudSessionRepository(
                        owner="github",
                        name="copilot-sdk",
                        branch="main",
                    )
                ),
                enable_mcp_apps=True,
                request_canvas_renderer=True,
                request_extensions=True,
                extension_sdk_path="custom-extension-sdk",
                extension_info=ExtensionInfo(
                    source="python-sdk-tests",
                    name="advanced-create-extension",
                ),
                canvases=[
                    CanvasDeclaration(
                        id="advanced-create-canvas",
                        display_name="Advanced Create Canvas",
                        description="Covers create-time canvas options.",
                    )
                ],
                providers=[
                    {
                        "name": "create-provider",
                        "type": "openai",
                        "wire_api": "responses",
                        "base_url": "https://create-provider.example.test/v1",
                        "api_key": "create-provider-key",
                        "headers": {"X-Create-Provider": "yes"},
                    }
                ],
                models=[
                    {
                        "provider": "create-provider",
                        "id": "create-model",
                        "name": "Create Model",
                        "model_id": "claude-sonnet-4.5",
                        "wire_model": "create-wire-model",
                        "max_context_window_tokens": 12_000,
                        "max_prompt_tokens": 10_000,
                        "max_output_tokens": 2_000,
                    }
                ],
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                params = _get_captured_request(capture_path, "session.create")
                assert params["clientName"] == "advanced-create-client"
                assert params["model"] == "claude-sonnet-4.5"
                assert params["reasoningEffort"] == "medium"
                assert params["reasoningSummary"] == "detailed"
                assert params["contextTier"] == "long_context"
                assert params["enableCitations"] is True
                assert params["capi"]["enableWebSocketResponses"] is False
                assert params["mcpOAuthTokenStorage"] == "persistent"
                assert params["agent"] == "agent-one"
                assert params["defaultAgent"]["excludedTools"][0] == "edit"
                assert params["customAgents"][0]["name"] == "agent-one"
                assert params["pluginDirectories"][0] == "plugins-create"
                assert params["disabledSkills"][0] == "disabled-create-skill"
                assert params["infiniteSessions"]["enabled"] is False
                assert params["largeOutput"]["enabled"] is True
                assert params["largeOutput"]["maxSizeBytes"] == 4096
                assert params["largeOutput"]["outputDir"] == output_directory
                assert params["memory"]["enabled"] is True
                assert params["gitHubToken"] == "session-create-token"
                assert params["remoteSession"] == "export"
                assert params["cloud"]["repository"]["owner"] == "github"
                assert params["requestMcpApps"] is True
                assert params["requestCanvasRenderer"] is True
                assert params["requestExtensions"] is True
                assert params["extensionSdkPath"] == "custom-extension-sdk"
                assert params["extensionInfo"]["name"] == "advanced-create-extension"
                assert params["canvases"][0]["id"] == "advanced-create-canvas"
                assert params["providers"][0]["name"] == "create-provider"
                assert params["providers"][0]["wireApi"] == "responses"
                assert params["models"][0]["id"] == "create-model"
                assert params["models"][0]["maxContextWindowTokens"] == 12_000
            finally:
                await session.disconnect()
        finally:
            await client.stop()

    async def test_should_forward_singular_provider_options_in_create_wire_request(
        self, ctx: E2ETestContext
    ):
        cli_path = os.path.join(ctx.work_dir, f"fake-cli-provider-create-{os.getpid()}.js")
        capture_path = os.path.join(
            ctx.work_dir, f"fake-cli-provider-create-capture-{os.getpid()}.json"
        )
        with open(cli_path, "w") as f:
            f.write(FAKE_STDIO_CLI_SCRIPT)

        client = CopilotClient(
            **_make_options(
                ctx,
                cli_path=cli_path,
                cli_args=["--capture-file", capture_path],
                use_logged_in_user=False,
            )
        )
        try:
            await client.start()
            session = await client.create_session(
                model="claude-sonnet-4.5",
                provider={
                    "type": "azure",
                    "wire_api": "responses",
                    "transport": "http",
                    "base_url": "https://azure-provider.example.test/openai",
                    "api_key": "provider-api-key",
                    "bearer_token": "provider-bearer-token",
                    "azure": {"api_version": "2024-02-15-preview"},
                    "headers": {"X-Provider-Wire": "yes"},
                    "model_id": "claude-sonnet-4.5",
                    "wire_model": "azure-deployment",
                    "max_prompt_tokens": 8192,
                    "max_output_tokens": 1024,
                },
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                provider = _get_captured_request(capture_path, "session.create")["provider"]
                assert provider["type"] == "azure"
                assert provider["wireApi"] == "responses"
                assert provider["transport"] == "http"
                assert provider["baseUrl"] == "https://azure-provider.example.test/openai"
                assert provider["apiKey"] == "provider-api-key"
                assert provider["bearerToken"] == "provider-bearer-token"
                assert provider["azure"]["apiVersion"] == "2024-02-15-preview"
                assert provider["headers"]["X-Provider-Wire"] == "yes"
                assert provider["modelId"] == "claude-sonnet-4.5"
                assert provider["wireModel"] == "azure-deployment"
                assert provider["maxPromptTokens"] == 8192
                assert provider["maxOutputTokens"] == 1024
            finally:
                await session.disconnect()
        finally:
            await client.stop()

    async def test_should_forward_advanced_session_options_in_resume_wire_request(
        self, ctx: E2ETestContext
    ):
        cli_path = os.path.join(ctx.work_dir, f"fake-cli-advanced-resume-{os.getpid()}.js")
        capture_path = os.path.join(
            ctx.work_dir, f"fake-cli-advanced-resume-capture-{os.getpid()}.json"
        )
        output_directory = os.path.join(ctx.work_dir, "large-output-resume")
        with open(cli_path, "w") as f:
            f.write(FAKE_STDIO_CLI_SCRIPT)

        client = CopilotClient(
            **_make_options(
                ctx,
                cli_path=cli_path,
                cli_args=["--capture-file", capture_path],
                use_logged_in_user=False,
            )
        )
        try:
            await client.start()
            session = await client.resume_session(
                "advanced-resume-session",
                client_name="advanced-resume-client",
                model="claude-haiku-4.5",
                reasoning_effort="low",
                reasoning_summary="none",
                context_tier="default",
                continue_pending_work=True,
                mcp_oauth_token_storage="persistent",
                plugin_directories=["plugins-resume"],
                large_output={
                    "enabled": False,
                    "max_size_bytes": 2048,
                    "output_directory": output_directory,
                },
                memory={"enabled": False},
                remote_session=RemoteSessionMode.ON,
                open_canvases=[
                    OpenCanvasInstance(
                        canvas_id="resume-canvas",
                        extension_id="python-sdk-tests/resume-extension",
                        extension_name="Resume Extension",
                        instance_id="resume-canvas-1",
                        input={"start": 41},
                        status="ready",
                        title="Resume Canvas",
                        url="https://example.com/resume-canvas",
                    )
                ],
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                params = _get_captured_request(capture_path, "session.resume")
                assert params["sessionId"] == "advanced-resume-session"
                assert params["clientName"] == "advanced-resume-client"
                assert params["model"] == "claude-haiku-4.5"
                assert params["reasoningEffort"] == "low"
                assert params["reasoningSummary"] == "none"
                assert params["contextTier"] == "default"
                assert params["continuePendingWork"] is True
                assert params["mcpOAuthTokenStorage"] == "persistent"
                assert params["pluginDirectories"][0] == "plugins-resume"
                assert params["largeOutput"]["enabled"] is False
                assert params["largeOutput"]["maxSizeBytes"] == 2048
                assert params["largeOutput"]["outputDir"] == output_directory
                assert params["memory"]["enabled"] is False
                assert params["remoteSession"] == "on"

                open_canvas = params["openCanvases"][0]
                assert open_canvas["canvasId"] == "resume-canvas"
                assert open_canvas["extensionId"] == "python-sdk-tests/resume-extension"
                assert open_canvas["extensionName"] == "Resume Extension"
                assert open_canvas["instanceId"] == "resume-canvas-1"
                assert open_canvas["input"]["start"] == 41
                assert open_canvas["status"] == "ready"
                assert open_canvas["title"] == "Resume Canvas"
                assert open_canvas["url"] == "https://example.com/resume-canvas"
            finally:
                await session.disconnect()
        finally:
            await client.stop()
