/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
using System.Text.Json;

namespace GitHub.Copilot.Test.E2E;

internal static class RpcSurfaceTestCli
{
    public static async Task<(string CliPath, string CapturePath)> CreateAsync(E2ETestContext context)
    {
        var cliPath = Path.Join(context.WorkDir, $"rpc-surface-cli-{Guid.NewGuid():N}.js");
        var capturePath = Path.Join(context.WorkDir, $"rpc-surface-cli-{Guid.NewGuid():N}.json");
        await File.WriteAllTextAsync(cliPath, Script);
        return (cliPath, capturePath);
    }

    public static async Task<JsonElement[]> ReadRequestsAsync(string capturePath)
    {
        await TestHelper.WaitForConditionAsync(
            () => Task.FromResult(File.Exists(capturePath)),
            timeout: TimeSpan.FromSeconds(10),
            timeoutMessage: "Timed out waiting for the RPC surface request capture.");

        using var capture = JsonDocument.Parse(await File.ReadAllTextAsync(capturePath));
        return capture.RootElement.GetProperty("requests").EnumerateArray().Select(request => request.Clone()).ToArray();
    }

    private const string Script = """
        const fs = require("fs");

        const captureIndex = process.argv.indexOf("--capture-file");
        const captureFile = process.argv[captureIndex + 1];
        const requests = [];
        let buffer = Buffer.alloc(0);

        function saveCapture() {
          fs.writeFileSync(captureFile, JSON.stringify({ requests }));
        }

        function writeResponse(id, result) {
          const body = JSON.stringify({ jsonrpc: "2.0", id, result });
          process.stdout.write(`Content-Length: ${Buffer.byteLength(body, "utf8")}\r\n\r\n${body}`);
        }

        function params(message) {
          return Array.isArray(message.params) ? (message.params[0] ?? {}) : (message.params ?? {});
        }

        function workspace(name = "RPC workspace") {
          return {
            path: "/tmp/rpc-workspace",
            workspace: {
              id: "workspace-1",
              cwd: "/tmp/rpc-workspace",
              name,
              branch: "rpc-branch",
              client_name: "rpc-client",
              created_at: "2026-09-18T11:00:00.000Z",
              git_root: "/tmp/rpc-workspace",
              remote_steerable: true
            }
          };
        }

        function task(sequence, status) {
          return {
            id: "task-1",
            type: "client",
            clientTaskId: "client-task-1",
            description: "RPC task",
            displayName: "RPC Task",
            activeStartedAt: "2026-09-18T12:00:00.500Z",
            activeTimeMs: 500,
            canCancel: true,
            executionMode: "background",
            owner: {
              displayName: "RPC owner",
              joinId: "join-1",
              kind: "sdk",
              participantId: "participant-1",
              presence: "connected",
              source: "rpc-test"
            },
            sequence,
            status,
            startedAt: "2026-09-18T12:00:00.000Z",
            updatedAt: "2026-09-18T12:00:01.000Z"
          };
        }

        function handle(message) {
          if (!Object.prototype.hasOwnProperty.call(message, "id")) {
            return;
          }

          requests.push({ method: message.method, params: message.params });
          saveCapture();

          switch (message.method) {
            case "connect":
              writeResponse(message.id, { ok: true, protocolVersion: 3, version: "rpc-surface-test" });
              return;
            case "session.create":
              writeResponse(message.id, {
                sessionId: params(message).sessionId ?? "rpc-surface-session",
                workspacePath: "/tmp/rpc-workspace",
                capabilities: null
              });
              return;
            case "commands.list":
              writeResponse(message.id, {
                commands: [{
                  name: "rpc-command",
                  description: "RPC command",
                  aliases: ["rpc"],
                  allowDuringAgentExecution: true,
                  experimental: false,
                  input: {
                    hint: "<value>",
                    preserveMultilineInput: false,
                    required: true
                  },
                  kind: "builtin",
                  schedulable: true
                }]
              });
              return;
            case "hooks.discover":
              writeResponse(message.id, { hooks: [], warnings: ["rpc-warning"], errors: [] });
              return;
            case "llmInference.setProvider":
              writeResponse(message.id, { success: true });
              return;
            case "managedSettings.read":
              writeResponse(message.id, { settingsJson: { policy: "strict" }, errorMessage: null });
              return;
            case "mcp.planInstall":
              writeResponse(message.id, {
                kind: "unavailable",
                message: "The host does not provide installation.",
                reason: "host-not-available"
              });
              return;
            case "models.getBuiltInCatalog":
              writeResponse(message.id, {
                models: [{
                  id: "built-in-model",
                  name: "Built-in Model",
                  family: "test-family"
                }]
              });
              return;
            case "sessions.getClientMetadata":
              writeResponse(message.id, [{
                status: "ok",
                sessionId: "persisted-session",
                metadata: { "rpc/key": "rpc-value" }
              }]);
              return;
            case "session.contentExclusion.checkPaths":
              writeResponse(message.id, {
                available: true,
                checks: [{ path: "/tmp/rpc-workspace/file.txt", excluded: false }]
              });
              return;
            case "session.debug.collectLogs":
              writeResponse(message.id, {
                kind: "directory",
                path: "/tmp/rpc-debug",
                entries: [{
                  bundlePath: "host/diagnostic.txt",
                  sizeBytes: 123,
                  source: "additional"
                }],
                skippedEntries: [{
                  bundlePath: "host/missing.txt",
                  path: "/tmp/missing.txt",
                  reason: "not found"
                }]
              });
              return;
            case "session.factory.run":
            case "session.factory.getRun":
              writeResponse(message.id, {
                runId: "factory-run-1",
                status: "running",
                attempt: 1,
                result: { value: "running" },
                snapshot: { step: 1 }
              });
              return;
            case "session.factory.pause":
              writeResponse(message.id, {
                runId: "factory-run-1",
                status: "paused",
                attempt: 1,
                reason: "caller requested pause",
                snapshot: { step: 2 }
              });
              return;
            case "session.factory.resume":
              writeResponse(message.id, {
                factoryName: "rpc-factory",
                run: {
                  runId: "factory-run-1",
                  status: "running",
                  attempt: 2,
                  snapshot: { step: 3 }
                }
              });
              return;
            case "session.factory.log":
            case "session.factory.journal.put":
            case "session.tools.set":
              writeResponse(message.id, {});
              return;
            case "session.factory.agent":
              writeResponse(message.id, { result: { answer: "agent-result" } });
              return;
            case "session.factory.journal.get":
              writeResponse(message.id, { hit: true, resultJson: { checkpoint: 7 } });
              return;
            case "session.history.clearContext":
              writeResponse(message.id, { messagesCleared: 4 });
              return;
            case "session.limitPrediction.predict":
              writeResponse(message.id, { kind: "unavailable", reason: "insufficient-data" });
              return;
            case "session.mcp.moveLoadingToBackground":
              writeResponse(message.id, { movedToBackground: true });
              return;
            case "session.mcp.oauth.respond":
              writeResponse(message.id, { success: true });
              return;
            case "session.mcp.resources.list":
              writeResponse(message.id, {
                nextCursor: "resource-next",
                resources: [{
                  uri: "file://rpc/resource.txt",
                  name: "RPC resource",
                  description: "Resource description",
                  mimeType: "text/plain",
                  size: 16,
                  title: "RPC Resource"
                }]
              });
              return;
            case "session.mcp.resources.listTemplates":
              writeResponse(message.id, {
                nextCursor: "template-next",
                resourceTemplates: [{
                  uriTemplate: "file://rpc/{name}",
                  name: "RPC template",
                  description: "Template description",
                  mimeType: "text/plain",
                  title: "RPC Template"
                }]
              });
              return;
            case "session.mcp.resources.read":
              writeResponse(message.id, {
                contents: [{
                  uri: "file://rpc/resource.txt",
                  mimeType: "text/plain",
                  text: "resource-content",
                  _meta: { audience: "assistant" }
                }]
              });
              return;
            case "session.metadata.getClientMetadata":
              writeResponse(message.id, { "rpc/key": "rpc-value", "rpc/other": "other-value" });
              return;
            case "session.model.setAllowedModels":
              writeResponse(message.id, {
                allowedModels: ["model-a", "model-b"],
                effectiveAllowedModels: ["model-a"],
                fallbackModel: "model-a",
                modelId: "model-a"
              });
              return;
            case "session.model.switchAutoTier":
              writeResponse(message.id, {
                status: "applied",
                activatingAutoTier: "intelligence",
                effectiveAutoTier: "intelligence",
                pendingAutoTier: null,
                supersededAutoTier: "balance"
              });
              return;
            case "session.sandbox.getEnforcementStatus":
              writeResponse(message.id, { required: true, blocked: false, reason: "managed-policy" });
              return;
            case "session.sandbox.disableForSession":
              writeResponse(message.id, { success: true, enabled: false });
              return;
            case "session.abort":
              writeResponse(message.id, { success: true, error: null });
              return;
            case "session.interruptMainTurn":
              writeResponse(message.id, { interrupted: true });
              return;
            case "session.cancelAllBackgroundAgents":
              writeResponse(message.id, 3);
              return;
            case "session.log":
              writeResponse(message.id, { eventId: "11111111-2222-3333-4444-555555555555" });
              return;
            case "session.tasks.register":
              writeResponse(message.id, { created: true, reclaimed: false, task: task(0, "running") });
              return;
            case "session.tasks.update":
              writeResponse(message.id, { applied: true, duplicate: false, task: task(1, "running") });
              return;
            case "session.tools.execute":
              writeResponse(message.id, { resultType: "success", textResultForLlm: "executed" });
              return;
            case "session.tools.getBuiltinDescriptors":
              writeResponse(message.id, {
                tools: [{
                  name: "rpc_builtin",
                  description: "RPC built-in tool",
                  hasSummariseIntention: true,
                  inputSchema: { type: "object" },
                  instructions: "Use the RPC built-in.",
                  isTerminal: false,
                  safeForTelemetry: true,
                  title: "RPC Built-in",
                  type: "test"
                }]
              });
              return;
            case "session.tools.taskCompleteEventData":
              writeResponse(message.id, {
                objectiveId: 17,
                outcome: "completed",
                reason: "completed",
                success: true,
                summary: "RPC task complete"
              });
              return;
            case "session.workspaces.updateMetadata":
              writeResponse(message.id, workspace("Updated RPC workspace"));
              return;
            case "session.workspaces.ensure":
              writeResponse(message.id, workspace());
              return;
            case "session.workspaces.statFile":
              writeResponse(message.id, {
                birthtimeMs: 1000,
                isDirectory: false,
                isFile: true,
                mtimeMs: 2000,
                size: 42
              });
              return;
            case "session.workspaces.addSummary":
              writeResponse(message.id, {
                summary: { number: 3, title: "RPC summary", content: "Summary content" },
                workspace: { id: "workspace-1", cwd: "/tmp/rpc-workspace", name: "RPC workspace" }
              });
              return;
            case "session.workspaces.truncateSummaries":
              writeResponse(message.id, workspace("Truncated RPC workspace"));
              return;
            default:
              writeResponse(message.id, {});
          }
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
            handle(JSON.parse(body));
          }
        });

        process.stdin.resume();
        saveCapture();
        """;
}
