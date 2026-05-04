"""
E2E coverage for ``CopilotClient`` configuration options exposed via
``SubprocessConfig`` and ``CopilotClient(..., auto_start=...)``.

Mirrors ``dotnet/test/ClientOptionsTests.cs``. The two CliUrl-conflict tests
(``Should_Throw_When_GitHubToken_Used_With_CliUrl`` and
``Should_Throw_When_UseLoggedInUser_Used_With_CliUrl``) have no Python
equivalent because Python's ``ExternalServerConfig`` does not accept
``github_token`` / ``use_logged_in_user`` fields at all (so the conflict cannot
be expressed in code), and the configurations are therefore intentionally
omitted.
"""

from __future__ import annotations

import json
import os
import socket

import pytest

from copilot import CopilotClient
from copilot.client import SubprocessConfig
from copilot.generated.rpc import PingRequest
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


def _make_subprocess_config(ctx: E2ETestContext, **overrides) -> SubprocessConfig:
    base = {
        "cli_path": ctx.cli_path,
        "cwd": ctx.work_dir,
        "env": ctx.get_env(),
        "github_token": (
            "fake-token-for-e2e-tests" if os.environ.get("GITHUB_ACTIONS") == "true" else None
        ),
    }
    base.update(overrides)
    return SubprocessConfig(**base)


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
    const sessionId = message.params?.sessionId ?? "fake-session";
    writeResponse(message.id, { sessionId, workspacePath: null, capabilities: null });
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


class TestClientOptions:
    async def test_autostart_false_requires_explicit_start(self, ctx: E2ETestContext):
        client = CopilotClient(_make_subprocess_config(ctx), auto_start=False)
        try:
            assert client.get_state() == "disconnected"

            with pytest.raises(RuntimeError) as exc_info:
                await client.create_session(
                    on_permission_request=PermissionHandler.approve_all,
                )
            # Python raises "Client not connected" — equivalent intent to C#'s "StartAsync".
            assert (
                "not connected" in str(exc_info.value).lower()
                or "start" in str(exc_info.value).lower()
            )

            await client.start()
            assert client.get_state() == "connected"

            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            assert session.session_id
            await session.disconnect()
        finally:
            await client.stop()

    async def test_should_listen_on_configured_tcp_port(self, ctx: E2ETestContext):
        port = _get_available_port()
        client = CopilotClient(_make_subprocess_config(ctx, use_stdio=False, port=port))
        try:
            await client.start()
            assert client.get_state() == "connected"
            assert client.actual_port == port

            response = await client.rpc.ping(PingRequest(message="fixed-port"))
            assert "pong" in response.message
        finally:
            await client.stop()

    async def test_should_use_client_cwd_for_default_workingdirectory(self, ctx: E2ETestContext):
        client_cwd = os.path.join(ctx.work_dir, "client-cwd")
        os.makedirs(client_cwd, exist_ok=True)
        with open(os.path.join(client_cwd, "marker.txt"), "w") as f:
            f.write("I am in the client cwd")

        client = CopilotClient(_make_subprocess_config(ctx, cwd=client_cwd))
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
            _make_subprocess_config(
                ctx,
                cli_path=cli_path,
                copilot_home=copilot_home_from_option,
                cli_args=["--capture-file", capture_path],
                env={**ctx.get_env(), "COPILOT_HOME": copilot_home_from_env},
                github_token="process-option-token",
                log_level="debug",
                session_idle_timeout_seconds=17,
                telemetry={
                    "otlp_endpoint": "http://127.0.0.1:4318",
                    "file_path": telemetry_path,
                    "exporter_type": "file",
                    "source_name": "python-sdk-e2e",
                    "capture_content": True,
                },
                use_logged_in_user=False,
            ),
            auto_start=False,
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
            assert env["COPILOT_OTEL_FILE_EXPORTER_PATH"] == telemetry_path
            assert env["COPILOT_OTEL_EXPORTER_TYPE"] == "file"
            assert env["COPILOT_OTEL_SOURCE_NAME"] == "python-sdk-e2e"
            assert env["OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT"] == "true"

            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                enable_config_discovery=True,
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
                assert params["includeSubAgentStreamingEvents"] is False
            finally:
                await session.disconnect()
        finally:
            try:
                await client.stop()
            except Exception:
                await client.force_stop()


# ---------------------------------------------------------------------------
# Unit-style tests mirroring the property-only tests in
# dotnet/test/ClientOptionsTests.cs. These exercise the SubprocessConfig
# dataclass shape only — no client / proxy required.
# ---------------------------------------------------------------------------


class TestSubprocessConfigOptions:
    """Mirrors the unit-style ClientOptions tests in the C# baseline."""

    async def test_should_accept_github_token_option(self):
        # Mirrors: Should_Accept_GitHubToken_Option
        config = SubprocessConfig(github_token="gho_test_token")
        assert config.github_token == "gho_test_token"

    async def test_should_default_use_logged_in_user_to_none(self):
        # Mirrors: Should_Default_UseLoggedInUser_To_Null
        config = SubprocessConfig()
        assert config.use_logged_in_user is None

    async def test_should_allow_explicit_use_logged_in_user_false(self):
        # Mirrors: Should_Allow_Explicit_UseLoggedInUser_False
        config = SubprocessConfig(use_logged_in_user=False)
        assert config.use_logged_in_user is False

    async def test_should_allow_explicit_use_logged_in_user_true_with_github_token(self):
        # Mirrors: Should_Allow_Explicit_UseLoggedInUser_True_With_GitHubToken
        config = SubprocessConfig(github_token="gho_test_token", use_logged_in_user=True)
        assert config.use_logged_in_user is True
        assert config.github_token == "gho_test_token"

    # NOTE: Should_Throw_When_GitHubToken_Used_With_CliUrl and
    # Should_Throw_When_UseLoggedInUser_Used_With_CliUrl from the C# baseline
    # do not apply to Python: ExternalServerConfig has no github_token /
    # use_logged_in_user fields at all (they live only on SubprocessConfig),
    # so the conflicting configuration is impossible to express.

    async def test_should_default_session_idle_timeout_seconds_to_none(self):
        # Mirrors: Should_Default_SessionIdleTimeoutSeconds_To_Null
        config = SubprocessConfig()
        assert config.session_idle_timeout_seconds is None

    async def test_should_accept_session_idle_timeout_seconds_option(self):
        # Mirrors: Should_Accept_SessionIdleTimeoutSeconds_Option
        config = SubprocessConfig(session_idle_timeout_seconds=600)
        assert config.session_idle_timeout_seconds == 600
