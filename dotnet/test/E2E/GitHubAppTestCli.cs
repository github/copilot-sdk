/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
using System.Text.Json;

namespace GitHub.Copilot.Test.E2E;

internal static class GitHubAppTestCli
{
    public static async Task<(string CliPath, string CapturePath)> CreateAsync(E2ETestContext context)
    {
        var cliPath = Path.Join(context.WorkDir, $"github-app-test-cli-{Guid.NewGuid():N}.js");
        var capturePath = Path.Join(context.WorkDir, $"github-app-test-cli-{Guid.NewGuid():N}.json");
        await File.WriteAllTextAsync(cliPath, Script);
        return (cliPath, capturePath);
    }

    public static async Task<JsonElement[]> ReadRequestsAsync(string capturePath)
    {
        await TestHelper.WaitForConditionAsync(
            () => Task.FromResult(File.Exists(capturePath)),
            timeout: TimeSpan.FromSeconds(10),
            timeoutMessage: "Timed out waiting for the fake CLI request capture.");

        using var capture = JsonDocument.Parse(await File.ReadAllTextAsync(capturePath));
        return capture.RootElement.GetProperty("requests").EnumerateArray().Select(request => request.Clone()).ToArray();
    }

    private const string Script = """
        const fs = require("fs");

        const captureIndex = process.argv.indexOf("--capture-file");
        const behaviorIndex = process.argv.indexOf("--behavior");
        const captureFile = process.argv[captureIndex + 1];
        const behavior = process.argv[behaviorIndex + 1];
        const requests = [];
        let resumeAttempts = 0;
        let buffer = Buffer.alloc(0);

        function saveCapture() {
          fs.writeFileSync(captureFile, JSON.stringify({ requests }));
        }

        function writeResponse(id, result) {
          const body = JSON.stringify({ jsonrpc: "2.0", id, result });
          process.stdout.write(`Content-Length: ${Buffer.byteLength(body, "utf8")}\r\n\r\n${body}`);
        }

        function writeError(id, code, message) {
          const body = JSON.stringify({ jsonrpc: "2.0", id, error: { code, message } });
          process.stdout.write(`Content-Length: ${Buffer.byteLength(body, "utf8")}\r\n\r\n${body}`);
        }

        function writeSessionEvent(sessionId, type, data) {
          const body = JSON.stringify({
            jsonrpc: "2.0",
            method: "session.event",
            params: {
              sessionId,
              event: {
                id: "00000000-0000-0000-0000-" + String(requests.length).padStart(12, "0"),
                timestamp: "2026-09-17T20:00:00.000Z",
                parentId: null,
                type,
                data
              }
            }
          });
          process.stdout.write(`Content-Length: ${Buffer.byteLength(body, "utf8")}\r\n\r\n${body}`);
        }

        function handleMessage(message) {
          if (!Object.prototype.hasOwnProperty.call(message, "id")) {
            return;
          }

          requests.push({ method: message.method, params: message.params });
          saveCapture();

          if (message.method === "connect") {
            writeResponse(message.id, { ok: true, protocolVersion: 3, version: "github-app-test" });
            return;
          }

          if (message.method === "session.create") {
            const sessionId = message.params?.sessionId ?? message.params?.[0]?.sessionId ?? "github-app-session";
            writeResponse(message.id, { sessionId, workspacePath: null, capabilities: null });
            if (behavior === "emit-ui-events") {
              setTimeout(() => {
                writeSessionEvent(sessionId, "user_input.requested", {
                  requestId: "app-user-input",
                  question: "Choose an app action",
                  choices: ["Approve", "Decline"],
                  allowFreeform: true,
                  toolCallId: "tool-user-input"
                });
                writeSessionEvent(sessionId, "elicitation.requested", {
                  requestId: "app-form-accept",
                  message: "Provide app settings",
                  mode: "form",
                  requestedSchema: {
                    type: "object",
                    properties: { name: { type: "string" } },
                    required: ["name"]
                  },
                  toolCallId: "tool-form"
                });
                writeSessionEvent(sessionId, "elicitation.requested", {
                  requestId: "app-url-decline",
                  message: "Authorize the app",
                  mode: "url",
                  url: "https://example.test/authorize",
                  toolCallId: "tool-url"
                });
                writeSessionEvent(sessionId, "elicitation.requested", {
                  requestId: "app-form-cancel",
                  message: "Optional app settings",
                  mode: "form",
                  requestedSchema: {
                    type: "object",
                    properties: {},
                    required: []
                  },
                  toolCallId: "tool-cancel"
                });
              }, 10);
            }
            return;
          }

          if (message.method === "session.resume") {
            resumeAttempts++;
            if (behavior === "resume-not-found-once" && resumeAttempts === 1) {
              writeError(message.id, -32001, "Session not found");
              return;
            }

            const sessionId = message.params?.sessionId ?? message.params?.[0]?.sessionId ?? "github-app-session";
            writeResponse(message.id, { sessionId, workspacePath: null, capabilities: null });
            return;
          }

          if (message.method === "session.send" && behavior === "drop-after-send") {
            process.stdout.end();
            return;
          }

          if (message.method === "session.send") {
            writeResponse(message.id, { messageId: "github-app-message" });
            return;
          }

          if (message.method === "session.delete" && behavior === "delete-not-found") {
            writeResponse(message.id, { success: false, error: "Session file not found" });
            return;
          }

          if (message.method === "session.ui.handlePendingElicitation" ||
              message.method === "session.ui.handlePendingUserInput") {
            const requestId = message.params?.requestId ?? message.params?.[0]?.requestId;
            writeResponse(message.id, { success: requestId !== "stale-app-request" });
            return;
          }

          writeResponse(message.id, { success: true });
        }

        process.stdin.on("data", chunk => {
          buffer = Buffer.concat([buffer, chunk]);
          while (true) {
            const headerEnd = buffer.indexOf("\r\n\r\n");
            if (headerEnd < 0) {
              return;
            }

            const header = buffer.subarray(0, headerEnd).toString("utf8");
            const match = /Content-Length:\s*(\d+)/i.exec(header);
            if (!match) {
              throw new Error("Missing Content-Length header");
            }

            const bodyStart = headerEnd + 4;
            const bodyEnd = bodyStart + Number(match[1]);
            if (buffer.length < bodyEnd) {
              return;
            }

            const body = buffer.subarray(bodyStart, bodyEnd).toString("utf8");
            buffer = buffer.subarray(bodyEnd);
            handleMessage(JSON.parse(body));
          }
        });

        process.stdin.resume();
        saveCapture();
        """;
}
