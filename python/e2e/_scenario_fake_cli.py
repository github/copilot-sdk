"""Deterministic bidirectional JSON-RPC CLI used by scenario-parity E2Es."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from copilot import CopilotClient, RuntimeConnection

from .testharness import DEFAULT_GITHUB_TOKEN, E2ETestContext

SCENARIO_FAKE_CLI_SCRIPT = r"""
const fs = require("fs");

const scenarioIndex = process.argv.indexOf("--scenario");
const captureIndex = process.argv.indexOf("--capture-file");
const scenario = scenarioIndex >= 0 ? process.argv[scenarioIndex + 1] : "";
const captureFile = captureIndex >= 0 ? process.argv[captureIndex + 1] : undefined;
const capture = { scenario, requests: [], callbackResponses: [] };

let pendingCreateId;
let pendingResumeId;
let resumeCallbackStep = 0;
let resumeAttempts = 0;
let buffer = Buffer.alloc(0);

function saveCapture() {
  if (captureFile) {
    fs.writeFileSync(captureFile, JSON.stringify(capture));
  }
}

function writeMessage(message) {
  const body = JSON.stringify(message);
  process.stdout.write(
    `Content-Length: ${Buffer.byteLength(body, "utf8")}\r\n\r\n${body}`
  );
}

function writeResponse(id, result) {
  writeMessage({ jsonrpc: "2.0", id, result });
}

function writeError(id, code, message, data = null) {
  writeMessage({ jsonrpc: "2.0", id, error: { code, message, data } });
}

function writeNotification(method, params) {
  writeMessage({ jsonrpc: "2.0", method, params });
}

function writeRequest(id, method, params) {
  writeMessage({ jsonrpc: "2.0", id, method, params });
}

function canvasParams(operation) {
  const params = {
    sessionId: "scenario-session",
    canvasId: "counter",
    extensionId: "python-scenario-tests",
    instanceId: `scenario-${operation}`,
    host: { capabilities: { canvases: true } },
    session: {
      workingDirectory: "Q:\\scenario-workspace",
    },
  };
  if (operation === "open") {
    params.input = { startValue: 7 };
  } else if (operation === "action") {
    params.actionName = "increment";
    params.input = { amount: 5 };
  }
  return params;
}

function sendCanvasCallback(operation, id = "canvas-callback") {
  const method =
    operation === "action" ? "canvas.action.invoke" : `canvas.${operation}`;
  writeRequest(id, method, canvasParams(operation));
}

function sessionStartEvent(sessionId) {
  return {
    id: "11111111-1111-4111-8111-111111111111",
    parentId: null,
    timestamp: "2026-01-02T03:04:05Z",
    type: "session.start",
    data: {
      copilotVersion: "fake",
      producer: "scenario-fake-cli",
      sessionId,
      startTime: "2026-01-02T03:04:05Z",
      version: 1,
      remoteSteerable: false,
    },
  };
}

function assistantEvent() {
  return {
    id: "22222222-2222-4222-8222-222222222222",
    parentId: null,
    timestamp: "2026-01-02T03:04:06Z",
    type: "assistant.message",
    data: {
      content: "scenario response",
      messageId: "assistant-message",
    },
  };
}

function idleEvent() {
  return {
    id: "33333333-3333-4333-8333-333333333333",
    parentId: null,
    timestamp: "2026-01-02T03:04:07Z",
    type: "session.idle",
    data: { mode: "interactive" },
  };
}

function remoteSteerableEvent() {
  return {
    id: "44444444-4444-4444-8444-444444444444",
    parentId: null,
    timestamp: "2026-01-02T03:04:05Z",
    type: "session.remote_steerable_changed",
    data: { remoteSteerable: true },
  };
}

function connectedMetadata() {
  return {
    kind: "coding-agent",
    modifiedTime: "2026-01-02T03:04:05Z",
    repository: {
      owner: "github",
      name: "copilot-sdk",
      branch: "scenario-branch",
    },
    sessionId: "remote-resource-id",
    startTime: "2026-01-01T00:00:00Z",
    name: "Scenario cloud session",
    pullRequestNumber: 42,
    resourceId: "remote-resource-id",
    state: "running",
    summary: "Remote task summary",
  };
}

function handleRequest(message) {
  capture.requests.push({ method: message.method, params: message.params });
  saveCapture();

  switch (message.method) {
    case "connect":
      writeResponse(message.id, {
        ok: true,
        protocolVersion: 3,
        version: "scenario-fake",
      });
      return;
    case "ping":
      writeResponse(message.id, {
        message: message.params?.message ?? "pong",
        protocolVersion: 3,
        timestamp: "2026-01-02T03:04:05Z",
      });
      return;
    case "session.create": {
      const sessionId =
        message.params?.sessionId ??
        (scenario === "cloud" ? "cloud-runtime-session" : "scenario-session");
      if (scenario.startsWith("canvas-error-")) {
        pendingCreateId = message.id;
        sendCanvasCallback(scenario.slice("canvas-error-".length));
        return;
      }
      if (scenario === "preallocated-event") {
        writeNotification("session.event", {
          sessionId,
          event: sessionStartEvent(sessionId),
        });
      }
      writeResponse(message.id, {
        sessionId,
        workspacePath: null,
        capabilities: null,
      });
      if (scenario === "cloud") {
        writeNotification("session.event", {
          sessionId,
          event: sessionStartEvent(sessionId),
        });
      }
      return;
    }
    case "session.resume":
      if (scenario === "canvas-resume") {
        pendingResumeId = message.id;
        sendCanvasCallback("open", "resume-open");
        return;
      }
      if (scenario === "resume-retry") {
        resumeAttempts += 1;
        if (resumeAttempts === 1) {
          writeError(
            message.id,
            -32001,
            "Session not found before acceptance",
            { recoverable: true }
          );
          return;
        }
      }
      if (scenario === "resume-fail") {
        writeError(
          message.id,
          -32001,
          "Session not found before acceptance",
          { recoverable: true }
        );
        return;
      }
      writeResponse(message.id, {
        sessionId: message.params.sessionId,
        workspacePath: null,
        capabilities: null,
        openCanvases: message.params.openCanvases ?? [],
      });
      return;
    case "sessions.connect":
      writeResponse(message.id, {
        sessionId: "runtime-session-id",
        metadata: connectedMetadata(),
      });
      return;
    case "session.remote.notifySteerableChanged":
      writeResponse(message.id, {});
      writeNotification("session.event", {
        sessionId: message.params.sessionId,
        event: remoteSteerableEvent(),
      });
      return;
    case "session.send":
      if (scenario === "send-fail") {
        saveCapture();
        process.exit(23);
        return;
      }
      writeResponse(message.id, { messageId: "user-message" });
      writeNotification("session.event", {
        sessionId: message.params.sessionId,
        event: assistantEvent(),
      });
      writeNotification("session.event", {
        sessionId: message.params.sessionId,
        event: idleEvent(),
      });
      return;
    case "session.options.update":
    case "session.detach":
      writeResponse(message.id, { success: true });
      return;
    default:
      writeResponse(message.id, {});
  }
}

function handleResponse(message) {
  capture.callbackResponses.push(message);
  saveCapture();

  if (pendingCreateId) {
    const createId = pendingCreateId;
    pendingCreateId = undefined;
    writeResponse(createId, {
      sessionId: "scenario-session",
      workspacePath: null,
      capabilities: null,
    });
    return;
  }

  if (pendingResumeId) {
    resumeCallbackStep += 1;
    if (resumeCallbackStep === 1) {
      sendCanvasCallback("action", "resume-action");
    } else if (resumeCallbackStep === 2) {
      sendCanvasCallback("close", "resume-close");
    } else {
      const resumeId = pendingResumeId;
      pendingResumeId = undefined;
      writeResponse(resumeId, {
        sessionId: "scenario-session",
        workspacePath: null,
        capabilities: null,
        openCanvases: [],
      });
    }
  }
}

function handleMessage(message) {
  if (Object.prototype.hasOwnProperty.call(message, "method")) {
    handleRequest(message);
  } else if (Object.prototype.hasOwnProperty.call(message, "id")) {
    handleResponse(message);
  }
}

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

saveCapture();
process.stdin.on("data", chunk => {
  buffer = Buffer.concat([buffer, chunk]);
  processBuffer();
});
process.stdin.resume();
"""


def create_scenario_client(
    ctx: E2ETestContext,
    scenario: str,
) -> tuple[CopilotClient, Path]:
    """Create a client backed by the deterministic scenario fake CLI."""
    cli_path = Path(ctx.work_dir, f"scenario-fake-{scenario}.js")
    capture_path = Path(ctx.work_dir, f"scenario-fake-{scenario}.json")
    cli_path.write_text(SCENARIO_FAKE_CLI_SCRIPT, encoding="utf-8")
    client = CopilotClient(
        connection=RuntimeConnection.for_stdio(
            path=str(cli_path),
            args=("--scenario", scenario, "--capture-file", str(capture_path)),
        ),
        working_directory=ctx.work_dir,
        env=ctx.get_env(),
        github_token=DEFAULT_GITHUB_TOKEN,
        use_logged_in_user=False,
    )
    return client, capture_path


def read_scenario_capture(path: Path) -> dict[str, Any]:
    """Read the fake CLI's latest request and callback-response capture."""
    return json.loads(path.read_text(encoding="utf-8"))
