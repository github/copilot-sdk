/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
using System.Text.Json;

namespace GitHub.Copilot.Test.E2E;

internal static class ScenarioTestingTestCli
{
    public static async Task<(string CliPath, string CapturePath)> CreateAsync(E2ETestContext context)
    {
        var cliPath = Path.Join(context.WorkDir, $"scenario-client-test-cli-{Guid.NewGuid():N}.js");
        var capturePath = Path.Join(context.WorkDir, $"scenario-client-test-cli-{Guid.NewGuid():N}.json");
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
        let nextQueueId = 1;
        let queueItems = [];
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

        function getParams(message) {
          return Array.isArray(message.params) ? (message.params[0] ?? {}) : (message.params ?? {});
        }

        function handleMessage(message) {
          if (!Object.prototype.hasOwnProperty.call(message, "id")) {
            return;
          }

          requests.push({ method: message.method, params: message.params });
          saveCapture();

          if (message.method === "connect") {
            writeResponse(message.id, { ok: true, protocolVersion: 3, version: "scenario-client-test" });
            return;
          }

          if (message.method === "session.create") {
            const sessionId = behavior === "cloud-assigned-event"
              ? "server-assigned-cloud-session"
              : getParams(message).sessionId ?? "scenario-client-session";
            writeResponse(message.id, { sessionId, workspacePath: null, capabilities: null });
            if (behavior === "cloud-assigned-event") {
              writeSessionEvent(sessionId, "session.start", {
                sessionId,
                copilotVersion: "scenario-runtime",
                producer: "scenario-test-cli",
                startTime: "2026-09-17T20:00:00.000Z",
                version: 1
              });
            }
            if (behavior === "emit-ui-events") {
              setTimeout(() => {
                writeSessionEvent(sessionId, "user_input.requested", {
                  requestId: "scenario-user-input",
                  question: "Choose a scenario action",
                  choices: ["Approve", "Decline"],
                  allowFreeform: true,
                  toolCallId: "tool-user-input"
                });
                writeSessionEvent(sessionId, "elicitation.requested", {
                  requestId: "scenario-form-accept",
                  message: "Provide scenario settings",
                  mode: "form",
                  requestedSchema: {
                    type: "object",
                    properties: { name: { type: "string" } },
                    required: ["name"]
                  },
                  toolCallId: "tool-form"
                });
                writeSessionEvent(sessionId, "elicitation.requested", {
                  requestId: "scenario-url-decline",
                  message: "Authorize the scenario",
                  mode: "url",
                  url: "https://example.test/authorize",
                  toolCallId: "tool-url"
                });
                writeSessionEvent(sessionId, "elicitation.requested", {
                  requestId: "scenario-form-cancel",
                  message: "Optional scenario settings",
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

            const sessionId = getParams(message).sessionId ?? "scenario-client-session";
            writeResponse(message.id, { sessionId, workspacePath: null, capabilities: null });
            return;
          }

          if (message.method === "catalog.search") {
            const params = getParams(message);
            writeResponse(message.id, {
              kind: "succeeded",
              candidates: [],
              negotiated: {
                runtimeProtocolVersion: params.contract.protocolVersion,
                grantedCapabilities: params.contract.requiredCapabilities
              },
              searchId: "scenario-search",
              truncated: false
            });
            return;
          }

          if (message.method === "session.autopilotObjective.getState") {
            writeResponse(message.id, {
              state: {
                id: 17,
                objective: "Ship the scenario.",
                status: "active",
                turnCount: 3,
                creditCountNanoAiu: "1250000000",
                creditLimit: {
                  credits: 5,
                  creditsUsed: 1.25,
                  creditsUsedNanoAiu: "1250000000"
                }
              }
            });
            return;
          }

          if (message.method === "session.remote.enable") {
            const params = getParams(message);
            writeResponse(message.id, {
              url: `https://example.test/sessions/${params.sessionId}`,
              remoteSteerable: params.mode === "on"
            });
            return;
          }

          if (message.method === "session.workflow.listRuns") {
            writeResponse(message.id, {
              runs: [{
                runId: "workflow-run-1",
                workflowName: "scenario-workflow",
                description: "Scenario workflow",
                status: "running",
                revision: 4,
                createdAt: 1000,
                updatedAt: 2000,
                observedAt: 2100,
                canResume: false,
                declaredLimits: {},
                consumed: {}
              }],
              oldestSeq: 7,
              newestSeq: 7,
              hasMoreNewer: false,
              omittedOlder: 0
            });
            return;
          }

          if (message.method === "session.workflow.getRunDetail") {
            writeResponse(message.id, {
              runId: "workflow-run-1",
              workflowName: "scenario-workflow",
              description: "Scenario workflow",
              status: "running",
              revision: 4,
              createdAt: 1000,
              updatedAt: 2000,
              observedAt: 2100,
              canResume: false,
              declaredLimits: {},
              consumed: {},
              progress: {
                records: [],
                revision: 4,
                hasMoreOlder: false,
                hasMoreNewer: false
              }
            });
            return;
          }

          if (message.method === "session.workflow.getRunProgress") {
            writeResponse(message.id, {
              records: [{
                seq: 12,
                attempt: 1,
                phaseId: "verify",
                kind: "log",
                text: "Validation complete",
                recordedAt: 2000
              }],
              oldestSeq: 12,
              newestSeq: 12,
              revision: 4,
              hasMoreOlder: false,
              hasMoreNewer: false
            });
            return;
          }

          if (message.method === "session.workflow.cancel") {
            writeResponse(message.id, {
              runId: "workflow-run-1",
              status: "cancelled",
              reason: "cancelled by user",
              attempt: 1
            });
            return;
          }

          if (message.method === "session.queue.setDrainPaused") {
            writeResponse(message.id, {});
            return;
          }

          if (message.method === "session.queue.insertAt") {
            const params = getParams(message);
            const id = `queue-${nextQueueId++}`;
            const item = {
              id,
              messageId: `message-${id}`,
              kind: "message",
              displayText: params.message.displayPrompt ?? params.message.prompt,
              prompt: params.message.prompt,
              agentMode: params.message.agentMode ?? "interactive"
            };
            const position = Math.max(0, Math.min(Number(params.position), queueItems.length));
            queueItems.splice(position, 0, item);
            writeResponse(message.id, { id });
            return;
          }

          if (message.method === "session.queue.pendingItems") {
            writeResponse(message.id, {
              items: queueItems.map(({ prompt, ...item }) => item),
              steeringMessages: [],
              inFlightSteeringCount: 0
            });
            return;
          }

          if (message.method === "session.queue.updateText") {
            const params = getParams(message);
            const item = queueItems.find(candidate => candidate.id === params.id);
            if (item) {
              item.prompt = params.prompt;
              item.displayText = params.displayPrompt ?? params.prompt;
            }
            writeResponse(message.id, { updated: Boolean(item) });
            return;
          }

          if (message.method === "session.queue.duplicateAt") {
            const params = getParams(message);
            const index = queueItems.findIndex(candidate => candidate.id === params.id);
            const id = `queue-${nextQueueId++}`;
            if (index >= 0) {
              queueItems.splice(index + 1, 0, {
                ...queueItems[index],
                id,
                messageId: `message-${id}`
              });
            }
            writeResponse(message.id, { id });
            return;
          }

          if (message.method === "session.queue.moveItem") {
            const params = getParams(message);
            const index = queueItems.findIndex(candidate => candidate.id === params.id);
            if (index < 0) {
              writeResponse(message.id, { changed: false });
              return;
            }
            const [item] = queueItems.splice(index, 1);
            const target = Math.max(0, Math.min(Number(params.toPosition), queueItems.length));
            queueItems.splice(target, 0, item);
            writeResponse(message.id, { changed: index !== target });
            return;
          }

          if (message.method === "session.queue.removeAt") {
            const params = getParams(message);
            const index = queueItems.findIndex(candidate => candidate.id === params.id);
            if (index >= 0) {
              queueItems.splice(index, 1);
            }
            writeResponse(message.id, { removed: index >= 0 });
            return;
          }

          if (message.method === "session.queue.sendNow") {
            const params = getParams(message);
            const index = queueItems.findIndex(candidate => candidate.id === params.id);
            if (index >= 0) {
              queueItems.splice(index, 1);
            }
            writeResponse(message.id, { steered: index >= 0 });
            return;
          }

          if (message.method === "session.send" && behavior === "drop-after-send") {
            process.stdout.end();
            return;
          }

          if (message.method === "session.send") {
            writeResponse(message.id, { messageId: "scenario-client-message" });
            return;
          }

          if (message.method === "session.delete" && behavior === "delete-not-found") {
            writeResponse(message.id, { success: false, error: "Session file not found" });
            return;
          }

          if (message.method === "session.ui.handlePendingElicitation" ||
              message.method === "session.ui.handlePendingUserInput") {
            const requestId = message.params?.requestId ?? message.params?.[0]?.requestId;
            writeResponse(message.id, { success: requestId !== "stale-scenario-request" });
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
