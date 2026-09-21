// Copyright (c) Microsoft Corporation. All rights reserved.

package e2e

import (
	"context"
	"encoding/json"
	"net"
	"reflect"
	"runtime"
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

type generatedRPCGapCase struct {
	name      string
	wire      string
	signature any
	receiver  func(*generatedRPCFixture) any
	rpcError  bool
}

func TestGeneratedRPCSurfaceGapsE2E(t *testing.T) {
	ctx, cancel := testContextWithTimeout(t, 20*time.Second)
	defer cancel()
	f := newGeneratedRPCFixture(t, ctx)

	server := func(get func(*rpc.ServerRPC) any) func(*generatedRPCFixture) any {
		return func(f *generatedRPCFixture) any { return get(f.client.RPC) }
	}
	session := func(get func(*rpc.SessionRPC) any) func(*generatedRPCFixture) any {
		return func(f *generatedRPCFixture) any { return get(f.session.RPC) }
	}

	cases := []generatedRPCGapCase{
		{"server catalog search", "catalog.search", (*rpc.ServerCatalogAPI).Search, server(func(r *rpc.ServerRPC) any { return r.Catalog }), true},
		{"server extensions discover", "extensions.discover", (*rpc.ServerExtensionsAPI).Discover, server(func(r *rpc.ServerRPC) any { return r.Extensions }), false},
		{"server hooks discover", "hooks.discover", (*rpc.ServerHooksAPI).Discover, server(func(r *rpc.ServerRPC) any { return r.Hooks }), false},
		{"server llm inference set provider", "llmInference.setProvider", (*rpc.ServerLlmInferenceAPI).SetProvider, server(func(r *rpc.ServerRPC) any { return r.LlmInference }), false},
		{"server managed settings read", "managedSettings.read", (*rpc.ServerManagedSettingsAPI).Read, server(func(r *rpc.ServerRPC) any { return r.ManagedSettings }), false},
		{"server mcp plan install", "mcp.planInstall", (*rpc.ServerMCPAPI).PlanInstall, server(func(r *rpc.ServerRPC) any { return r.MCP }), false},
		{"server models built in catalog", "models.getBuiltInCatalog", (*rpc.ServerModelsAPI).GetBuiltInCatalog, server(func(r *rpc.ServerRPC) any { return r.Models }), false},
		{"server plugins builtin set", "plugins.builtin.set", (*rpc.ServerPluginsBuiltinAPI).Set, server(func(r *rpc.ServerRPC) any { return r.Plugins.Builtin() }), false},
		{"server sessions client metadata", "sessions.getClientMetadata", (*rpc.ServerSessionsAPI).GetClientMetadata, server(func(r *rpc.ServerRPC) any { return r.Sessions }), false},
		{"server sessions persisted events", "sessions.readPersistedEvents", (*rpc.ServerSessionsAPI).ReadPersistedEvents, server(func(r *rpc.ServerRPC) any { return r.Sessions }), false},
		{"server skills disabled", "skills.config.setSkillDisabled", (*rpc.ServerSkillsConfigAPI).SetSkillDisabled, server(func(r *rpc.ServerRPC) any { return r.Skills.Config() }), false},
		{"server extension launch provider", "registerExtensionLaunchProvider", (*rpc.ServerRPC).RegisterExtensionLaunchProvider, server(func(r *rpc.ServerRPC) any { return r }), false},
		{"session agent prompt", "session.agent.setPrompt", (*rpc.AgentAPI).SetPrompt, session(func(r *rpc.SessionRPC) any { return r.Agent }), false},
		{"session autopilot objective state", "session.autopilotObjective.getState", (*rpc.AutopilotObjectiveAPI).GetState, session(func(r *rpc.SessionRPC) any { return r.AutopilotObjective }), false},
		{"session canvas list open", "session.canvas.listOpen", (*rpc.CanvasAPI).ListOpen, session(func(r *rpc.SessionRPC) any { return r.Canvas }), false},
		{"session completion triggers", "session.completions.getTriggerCharacters", (*rpc.CompletionsAPI).GetTriggerCharacters, session(func(r *rpc.SessionRPC) any { return r.Completions }), false},
		{"session content exclusion paths", "session.contentExclusion.checkPaths", (*rpc.ContentExclusionAPI).CheckPaths, session(func(r *rpc.SessionRPC) any { return r.ContentExclusion }), false},
		{"session debug logs", "session.debug.collectLogs", (*rpc.DebugAPI).CollectLogs, session(func(r *rpc.SessionRPC) any { return r.Debug }), false},
		{"session factory agent", "session.factory.agent", (*rpc.FactoryAPI).Agent, session(func(r *rpc.SessionRPC) any { return r.Factory }), false},
		{"session factory cancel", "session.factory.cancel", (*rpc.FactoryAPI).Cancel, session(func(r *rpc.SessionRPC) any { return r.Factory }), false},
		{"session factory get run", "session.factory.getRun", (*rpc.FactoryAPI).GetRun, session(func(r *rpc.SessionRPC) any { return r.Factory }), false},
		{"session factory detail", "session.factory.getRunDetail", (*rpc.FactoryAPI).GetRunDetail, session(func(r *rpc.SessionRPC) any { return r.Factory }), false},
		{"session factory progress", "session.factory.getRunProgress", (*rpc.FactoryAPI).GetRunProgress, session(func(r *rpc.SessionRPC) any { return r.Factory }), false},
		{"session factory list runs", "session.factory.listRuns", (*rpc.FactoryAPI).ListRuns, session(func(r *rpc.SessionRPC) any { return r.Factory }), false},
		{"session factory log", "session.factory.log", (*rpc.FactoryAPI).Log, session(func(r *rpc.SessionRPC) any { return r.Factory }), false},
		{"session factory pause", "session.factory.pause", (*rpc.FactoryAPI).Pause, session(func(r *rpc.SessionRPC) any { return r.Factory }), false},
		{"session factory resume", "session.factory.resume", (*rpc.FactoryAPI).Resume, session(func(r *rpc.SessionRPC) any { return r.Factory }), false},
		{"session factory run", "session.factory.run", (*rpc.FactoryAPI).Run, session(func(r *rpc.SessionRPC) any { return r.Factory }), false},
		{"session factory journal get", "session.factory.journal.get", (*rpc.FactoryJournalAPI).Get, session(func(r *rpc.SessionRPC) any { return r.Factory.Journal() }), false},
		{"session factory journal put", "session.factory.journal.put", (*rpc.FactoryJournalAPI).Put, session(func(r *rpc.SessionRPC) any { return r.Factory.Journal() }), false},
		{"session history clear context", "session.history.clearContext", (*rpc.HistoryAPI).ClearContext, session(func(r *rpc.SessionRPC) any { return r.History }), false},
		{"session limit prediction", "session.limitPrediction.predict", (*rpc.LimitPredictionAPI).Predict, session(func(r *rpc.SessionRPC) any { return r.LimitPrediction }), false},
		{"session mcp loading background", "session.mcp.moveLoadingToBackground", (*rpc.MCPAPI).MoveLoadingToBackground, session(func(r *rpc.SessionRPC) any { return r.MCP }), false},
		{"session mcp restart", "session.mcp.restartServer", (*rpc.MCPAPI).RestartServer, session(func(r *rpc.SessionRPC) any { return r.MCP }), false},
		{"session mcp start", "session.mcp.startServer", (*rpc.MCPAPI).StartServer, session(func(r *rpc.SessionRPC) any { return r.MCP }), false},
		{"session mcp oauth state changed", "session.mcp.oauth.authenticationStateChanged", (*rpc.MCPOauthAPI).AuthenticationStateChanged, session(func(r *rpc.SessionRPC) any { return r.MCP.Oauth() }), false},
		{"session mcp oauth probe", "session.mcp.oauth.probe", (*rpc.MCPOauthAPI).Probe, session(func(r *rpc.SessionRPC) any { return r.MCP.Oauth() }), true},
		{"session mcp oauth respond", "session.mcp.oauth.respond", (*rpc.MCPOauthAPI).Respond, session(func(r *rpc.SessionRPC) any { return r.MCP.Oauth() }), false},
		{"session mcp resources list", "session.mcp.resources.list", (*rpc.MCPResourcesAPI).List, session(func(r *rpc.SessionRPC) any { return r.MCP.Resources() }), false},
		{"session mcp resource templates", "session.mcp.resources.listTemplates", (*rpc.MCPResourcesAPI).ListTemplates, session(func(r *rpc.SessionRPC) any { return r.MCP.Resources() }), false},
		{"session mcp resource read", "session.mcp.resources.read", (*rpc.MCPResourcesAPI).Read, session(func(r *rpc.SessionRPC) any { return r.MCP.Resources() }), false},
		{"session metadata get", "session.metadata.getClientMetadata", (*rpc.MetadataAPI).GetClientMetadata, session(func(r *rpc.SessionRPC) any { return r.Metadata }), false},
		{"session metadata update", "session.metadata.updateClientMetadata", (*rpc.MetadataAPI).UpdateClientMetadata, session(func(r *rpc.SessionRPC) any { return r.Metadata }), false},
		{"session allowed models", "session.model.setAllowedModels", (*rpc.ModelAPI).SetAllowedModels, session(func(r *rpc.SessionRPC) any { return r.Model }), false},
		{"session auto tier", "session.model.switchAutoTier", (*rpc.ModelAPI).SwitchAutoTier, session(func(r *rpc.SessionRPC) any { return r.Model }), false},
		{"session queue duplicate", "session.queue.duplicateAt", (*rpc.QueueAPI).DuplicateAt, session(func(r *rpc.SessionRPC) any { return r.Queue }), false},
		{"session queue insert", "session.queue.insertAt", (*rpc.QueueAPI).InsertAt, session(func(r *rpc.SessionRPC) any { return r.Queue }), false},
		{"session queue move", "session.queue.moveItem", (*rpc.QueueAPI).MoveItem, session(func(r *rpc.SessionRPC) any { return r.Queue }), false},
		{"session queue remove", "session.queue.removeAt", (*rpc.QueueAPI).RemoveAt, session(func(r *rpc.SessionRPC) any { return r.Queue }), false},
		{"session queue send now", "session.queue.sendNow", (*rpc.QueueAPI).SendNow, session(func(r *rpc.SessionRPC) any { return r.Queue }), false},
		{"session queue drain pause", "session.queue.setDrainPaused", (*rpc.QueueAPI).SetDrainPaused, session(func(r *rpc.SessionRPC) any { return r.Queue }), false},
		{"session queue update", "session.queue.updateText", (*rpc.QueueAPI).UpdateText, session(func(r *rpc.SessionRPC) any { return r.Queue }), false},
		{"session sandbox disable", "session.sandbox.disableForSession", (*rpc.SandboxAPI).DisableForSession, session(func(r *rpc.SessionRPC) any { return r.Sandbox }), false},
		{"session sandbox status", "session.sandbox.getEnforcementStatus", (*rpc.SandboxAPI).GetEnforcementStatus, session(func(r *rpc.SessionRPC) any { return r.Sandbox }), false},
		{"session tasks register", "session.tasks.register", (*rpc.TasksAPI).Register, session(func(r *rpc.SessionRPC) any { return r.Tasks }), false},
		{"session tasks update", "session.tasks.update", (*rpc.TasksAPI).Update, session(func(r *rpc.SessionRPC) any { return r.Tasks }), false},
		{"session tools execute", "session.tools.execute", (*rpc.ToolsAPI).Execute, session(func(r *rpc.SessionRPC) any { return r.Tools }), false},
		{"session builtin tool descriptors", "session.tools.getBuiltinDescriptors", (*rpc.ToolsAPI).GetBuiltinDescriptors, session(func(r *rpc.SessionRPC) any { return r.Tools }), false},
		{"session tools set", "session.tools.set", (*rpc.ToolsAPI).Set, session(func(r *rpc.SessionRPC) any { return r.Tools }), false},
		{"session task complete event data", "session.tools.taskCompleteEventData", (*rpc.ToolsAPI).TaskCompleteEventData, session(func(r *rpc.SessionRPC) any { return r.Tools }), false},
		{"session workspace summary", "session.workspaces.addSummary", (*rpc.WorkspacesAPI).AddSummary, session(func(r *rpc.SessionRPC) any { return r.Workspaces }), false},
		{"session workspace objective exists", "session.workspaces.autopilotObjectiveExists", (*rpc.WorkspacesAPI).AutopilotObjectiveExists, session(func(r *rpc.SessionRPC) any { return r.Workspaces }), false},
		{"session workspace directory", "session.workspaces.createDirectory", (*rpc.WorkspacesAPI).CreateDirectory, session(func(r *rpc.SessionRPC) any { return r.Workspaces }), false},
		{"session workspace objective delete", "session.workspaces.deleteAutopilotObjective", (*rpc.WorkspacesAPI).DeleteAutopilotObjective, session(func(r *rpc.SessionRPC) any { return r.Workspaces }), false},
		{"session workspace ensure", "session.workspaces.ensure", (*rpc.WorkspacesAPI).Ensure, session(func(r *rpc.SessionRPC) any { return r.Workspaces }), false},
		{"session workspace objective read", "session.workspaces.readAutopilotObjective", (*rpc.WorkspacesAPI).ReadAutopilotObjective, session(func(r *rpc.SessionRPC) any { return r.Workspaces }), false},
		{"session workspace remove", "session.workspaces.removePath", (*rpc.WorkspacesAPI).RemovePath, session(func(r *rpc.SessionRPC) any { return r.Workspaces }), false},
		{"session workspace rename", "session.workspaces.renamePath", (*rpc.WorkspacesAPI).RenamePath, session(func(r *rpc.SessionRPC) any { return r.Workspaces }), false},
		{"session workspace stat", "session.workspaces.statFile", (*rpc.WorkspacesAPI).StatFile, session(func(r *rpc.SessionRPC) any { return r.Workspaces }), false},
		{"session workspace truncate summaries", "session.workspaces.truncateSummaries", (*rpc.WorkspacesAPI).TruncateSummaries, session(func(r *rpc.SessionRPC) any { return r.Workspaces }), false},
		{"session workspace metadata", "session.workspaces.updateMetadata", (*rpc.WorkspacesAPI).UpdateMetadata, session(func(r *rpc.SessionRPC) any { return r.Workspaces }), false},
		{"session workspace objective write", "session.workspaces.writeAutopilotObjective", (*rpc.WorkspacesAPI).WriteAutopilotObjective, session(func(r *rpc.SessionRPC) any { return r.Workspaces }), false},
		{"session abort", "session.abort", (*rpc.SessionRPC).Abort, session(func(r *rpc.SessionRPC) any { return r }), false},
		{"session cancel background agents", "session.cancelAllBackgroundAgents", (*rpc.SessionRPC).CancelAllBackgroundAgents, session(func(r *rpc.SessionRPC) any { return r }), false},
		{"session interrupt main turn", "session.interruptMainTurn", (*rpc.SessionRPC).InterruptMainTurn, session(func(r *rpc.SessionRPC) any { return r }), false},
		{"session log", "session.log", (*rpc.SessionRPC).Log, session(func(r *rpc.SessionRPC) any { return r }), false},
		{"session send", "session.send", (*rpc.SessionRPC).Send, session(func(r *rpc.SessionRPC) any { return r }), false},
		{"session send messages", "session.sendMessages", (*rpc.SessionRPC).SendMessages, session(func(r *rpc.SessionRPC) any { return r }), false},
	}

	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			var captured json.RawMessage
			f.server.SetRequestHandler(tc.wire, func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				captured = append(captured[:0], params...)
				if tc.rpcError {
					return nil, &jsonrpc2.Error{Code: -32042, Message: "synthetic " + tc.wire}
				}
				return json.RawMessage(generatedRPCResponse(tc.wire)), nil
			})

			method := reflect.ValueOf(tc.receiver(f)).MethodByName(methodName(tc.signature))
			if !method.IsValid() {
				t.Fatalf("Method expression %T did not resolve on receiver %T", tc.signature, tc.receiver(f))
			}
			args := []reflect.Value{reflect.ValueOf(ctx)}
			requestJSON := generatedRPCRequest(tc.wire)
			if method.Type().NumIn() == 2 {
				if requestJSON == "" {
					if !method.Type().IsVariadic() {
						args = append(args, reflect.Zero(method.Type().In(1)))
					}
				} else {
					requestType := method.Type().In(1)
					if method.Type().IsVariadic() {
						requestType = requestType.Elem()
					}
					request := reflect.New(requestType.Elem())
					if err := json.Unmarshal([]byte(requestJSON), request.Interface()); err != nil {
						t.Fatalf("Decode %s typed request: %v", tc.wire, err)
					}
					args = append(args, request)
				}
			}
			results := method.Call(args)
			if len(results) != 2 {
				t.Fatalf("Expected two return values, got %d", len(results))
			}
			err, _ := results[1].Interface().(error)
			if tc.rpcError {
				if err == nil || !strings.Contains(err.Error(), "synthetic "+tc.wire) {
					t.Fatalf("Expected synthetic RPC error, got %v", err)
				}
			} else {
				if err != nil {
					t.Fatalf("%s returned error: %v", tc.wire, err)
				}
				if results[0].Kind() == reflect.Pointer && results[0].IsNil() {
					t.Fatalf("%s returned a nil typed result", tc.wire)
				}
				assertGeneratedRPCResult(t, tc.wire, results[0].Interface())
			}

			var request map[string]any
			if len(captured) != 0 && string(captured) != "null" {
				if err := json.Unmarshal(captured, &request); err != nil {
					t.Fatalf("Decode %s request %s: %v", tc.wire, captured, err)
				}
			}
			if strings.HasPrefix(tc.wire, "session.") {
				if request["sessionId"] != f.session.SessionID {
					t.Fatalf("%s sessionId = %#v, want %q; request=%s", tc.wire, request["sessionId"], f.session.SessionID, captured)
				}
			} else if _, exists := request["sessionId"]; exists {
				t.Fatalf("%s unexpectedly serialized sessionId; request=%s", tc.wire, captured)
			}
			if requestJSON != "" {
				var expected map[string]any
				if err := json.Unmarshal([]byte(requestJSON), &expected); err != nil {
					t.Fatal(err)
				}
				assertJSONSubset(t, tc.wire+" request", expected, request)
			}
		})
	}
}

func generatedRPCRequest(wire string) string {
	switch wire {
	case "hooks.discover":
		return `{"projectPaths":["Q:\\rpc-project"],"excludeHostHooks":true}`
	case "mcp.planInstall":
		return `{"contract":{"protocolVersion":3,"requiredCapabilities":["mcp-install-planning"]},"source":{"kind":"candidate","candidateHandle":"candidate-1","searchId":"search-1"},"scope":"user"}`
	case "plugins.builtin.set":
		return `{"paths":["Q:\\rpc-plugins"]}`
	case "sessions.getClientMetadata":
		return `{"sessionIds":["persisted-session"],"keys":["rpc/key"]}`
	case "skills.config.setSkillDisabled":
		return `{"name":"skill-one","disabled":true}`
	case "session.agent.setPrompt":
		return `{"id":"agent-1","prompt":"Use the RPC prompt."}`
	case "session.contentExclusion.checkPaths":
		return `{"paths":["/tmp/rpc-workspace/file.txt"]}`
	case "session.debug.collectLogs":
		return `{"destination":{"kind":"directory","outputDirectory":"/tmp/rpc-debug"},"include":{"events":true,"processLogs":false,"shellLogs":true},"additionalEntries":[{"bundlePath":"host/diagnostic.txt","kind":"file","path":"/tmp/diagnostic.txt","required":true}]}`
	case "session.factory.run":
		return `{"name":"rpc-factory","args":{"input":42},"options":{"limits":{"maxAiCredits":2.5,"maxConcurrentSubagents":2,"maxTotalSubagents":4,"timeoutSeconds":30},"logPhaseNames":true,"notifyOnComplete":false}}`
	case "session.factory.resume":
		return `{"runId":"factory-run-1","limits":{"maxTotalSubagents":8},"notifyOnComplete":true,"logPhaseNames":false}`
	case "session.factory.getRun", "session.factory.pause":
		return `{"runId":"factory-run-1"}`
	case "session.factory.log":
		return `{"runId":"factory-run-1","executionToken":"execution-token-1","lines":[{"kind":"log","seq":7,"text":"Factory progress"}]}`
	case "session.factory.agent":
		return `{"factoryRunId":"factory-run-1","executionToken":"execution-token-1","prompt":"Complete the RPC task.","opts":{"agent":"explore","label":"rpc-agent","model":"model-a","reasoningEffort":"high"}}`
	case "session.factory.journal.get":
		return `{"runId":"factory-run-1","executionToken":"execution-token-1","key":"checkpoint"}`
	case "session.factory.journal.put":
		return `{"runId":"factory-run-1","executionToken":"execution-token-1","key":"checkpoint","resultJson":{"checkpoint":8}}`
	case "session.history.clearContext":
		return `{"prompt":"Reset context."}`
	case "session.limitPrediction.predict":
		return `{"clientType":"sdk","modelId":"model-a"}`
	case "session.mcp.startServer":
		return `{"serverName":"rpc-server","config":{"type":"stdio","command":"node","args":["server.js"]}}`
	case "session.mcp.oauth.authenticationStateChanged":
		return `{"serverName":"rpc-server","refreshSessionToken":true}`
	case "session.mcp.oauth.respond":
		return `{"requestId":"oauth-request-1"}`
	case "session.mcp.resources.list":
		return `{"serverName":"rpc-server","cursor":"resource-cursor"}`
	case "session.mcp.resources.listTemplates":
		return `{"serverName":"rpc-server","cursor":"template-cursor"}`
	case "session.mcp.resources.read":
		return `{"serverName":"rpc-server","uri":"file://rpc/resource.txt"}`
	case "session.model.setAllowedModels":
		return `{"allowedModels":["model-a","model-b"]}`
	case "session.model.switchAutoTier":
		return `{"autoTier":"intelligence"}`
	case "session.sandbox.disableForSession":
		return `{"requestId":"sandbox-request-1"}`
	case "session.abort":
		return `{"reason":"user"}`
	case "session.interruptMainTurn":
		return `{"flushQueued":true}`
	case "session.log":
		return `{"message":"RPC log","level":"warning","type":"rpc","ephemeral":true,"url":"https://example.test/rpc","tip":"Inspect the RPC."}`
	case "session.tasks.register":
		return `{"type":"client","clientTaskId":"client-task-1","description":"RPC task","cancellable":true,"displayName":"RPC Task"}`
	case "session.tasks.update":
		return `{"id":"task-1","sequence":1,"update":{"kind":"progress","message":"Halfway","percentage":50,"phase":"work","status":"running"}}`
	case "session.tools.execute":
		return `{"name":"rpc_tool","arguments":{"value":"input"},"toolCallId":"tool-call-1"}`
	case "session.tools.getBuiltinDescriptors":
		return `{"reduceUserIntervention":true,"includeAuthor":true,"skillEmbeddingEnabled":false,"shellConfig":{"displayName":"PowerShell","shellType":"powershell","shellToolName":"shell","listShellsToolName":"list_shells","readShellToolName":"read_shell","stopShellToolName":"stop_shell","descriptionLines":["Runs shell commands."]},"shellSupportsPowerShell7Syntax":true,"shellTimeoutMs":1234,"backgroundTaskNotificationsEnabled":true}`
	case "session.tools.set":
		return `{"tools":[{"name":"rpc_external","title":"RPC External","description":"External RPC tool","parameters":{"type":"object"},"isTerminal":false,"overridesBuiltInTool":false,"skipPermission":true}]}`
	case "session.tools.taskCompleteEventData":
		return `{"toolArgs":{"objectiveId":17},"finalResult":{"resultType":"success","textResultForLlm":"RPC task complete","sessionLog":"Completion logged."}}`
	case "session.workspaces.updateMetadata":
		return `{"context":{"owner":"rpc-test"},"name":"Updated RPC workspace"}`
	case "session.workspaces.ensure":
		return `{"context":{"owner":"rpc-test"}}`
	case "session.workspaces.statFile":
		return `{"path":"folder/file.txt"}`
	case "session.workspaces.createDirectory":
		return `{"path":"folder/nested","recursive":true}`
	case "session.workspaces.renamePath":
		return `{"source":"folder/file.txt","destination":"folder/renamed.txt"}`
	case "session.workspaces.removePath":
		return `{"path":"folder","recursive":true,"force":true}`
	case "session.workspaces.addSummary":
		return `{"title":"RPC summary","content":"Summary content"}`
	case "session.workspaces.truncateSummaries":
		return `{"keepCount":2}`
	default:
		return ""
	}
}

func generatedRPCResponse(wire string) string {
	switch wire {
	case "hooks.discover":
		return `{"hooks":[],"warnings":["rpc-warning"],"errors":[]}`
	case "llmInference.setProvider":
		return `{"success":true}`
	case "managedSettings.read":
		return `{"settingsJson":{"policy":"strict"}}`
	case "mcp.planInstall":
		return `{"kind":"unavailable","message":"The host does not provide installation.","reason":"host-not-available"}`
	case "models.getBuiltInCatalog":
		return `{"models":[{"id":"built-in-model"}]}`
	case "sessions.getClientMetadata":
		return `[{"status":"ok","sessionId":"persisted-session","metadata":{"rpc/key":"rpc-value"}}]`
	case "session.contentExclusion.checkPaths":
		return `{"available":true,"checks":[{"path":"/tmp/rpc-workspace/file.txt","excluded":false}]}`
	case "session.debug.collectLogs":
		return `{"kind":"directory","path":"/tmp/rpc-debug","entries":[{"bundlePath":"host/diagnostic.txt","sizeBytes":123,"source":"additional"}],"skippedEntries":[{"bundlePath":"host/missing.txt","path":"/tmp/missing.txt","reason":"not found"}]}`
	case "session.factory.run", "session.factory.getRun":
		return `{"runId":"factory-run-1","status":"running","attempt":1,"result":{"value":"running"},"snapshot":{"step":1}}`
	case "session.factory.pause":
		return `{"runId":"factory-run-1","status":"paused","attempt":1,"reason":"caller requested pause","snapshot":{"step":2}}`
	case "session.factory.resume":
		return `{"factoryName":"rpc-factory","run":{"runId":"factory-run-1","status":"running","attempt":2,"snapshot":{"step":3}}}`
	case "session.factory.agent":
		return `{"result":{"answer":"agent-result"}}`
	case "session.factory.journal.get":
		return `{"hit":true,"resultJson":{"checkpoint":7}}`
	case "session.history.clearContext":
		return `{"messagesCleared":4}`
	case "session.limitPrediction.predict":
		return `{"kind":"unavailable","reason":"insufficient-data"}`
	case "session.mcp.moveLoadingToBackground":
		return `{"movedToBackground":true}`
	case "session.mcp.oauth.respond":
		return `{"success":true}`
	case "session.mcp.resources.list":
		return `{"nextCursor":"resource-next","resources":[{"uri":"file://rpc/resource.txt","name":"RPC resource","description":"Resource description","mimeType":"text/plain","size":16,"title":"RPC Resource"}]}`
	case "session.mcp.resources.listTemplates":
		return `{"nextCursor":"template-next","resourceTemplates":[{"uriTemplate":"file://rpc/{name}","name":"RPC template","description":"Template description","mimeType":"text/plain","title":"RPC Template"}]}`
	case "session.mcp.resources.read":
		return `{"contents":[{"uri":"file://rpc/resource.txt","mimeType":"text/plain","text":"resource-content","_meta":{"audience":"assistant"}}]}`
	case "session.metadata.getClientMetadata":
		return `{"rpc/key":"rpc-value","rpc/other":"other-value"}`
	case "session.model.setAllowedModels":
		return `{"allowedModels":["model-a","model-b"],"effectiveAllowedModels":["model-a"],"fallbackModel":"model-a","modelId":"model-a"}`
	case "session.model.switchAutoTier":
		return `{"status":"applied","activatingAutoTier":"intelligence","effectiveAutoTier":"intelligence","supersededAutoTier":"balance"}`
	case "session.sandbox.getEnforcementStatus":
		return `{"required":true,"blocked":false,"reason":"managed-policy"}`
	case "session.sandbox.disableForSession":
		return `{"success":true,"enabled":false}`
	case "session.abort":
		return `{"success":true}`
	case "session.interruptMainTurn":
		return `{"interrupted":true}`
	case "session.cancelAllBackgroundAgents":
		return `3`
	case "session.log":
		return `{"eventId":"11111111-2222-3333-4444-555555555555"}`
	case "session.tasks.register":
		return `{"created":true,"reclaimed":false,"task":{"id":"task-1","type":"client","clientTaskId":"client-task-1","description":"RPC task","displayName":"RPC Task","activeTimeMs":500,"canCancel":true,"executionMode":"background","owner":{"displayName":"RPC owner","joinId":"join-1","kind":"sdk","participantId":"participant-1","presence":"connected","source":"rpc-test"},"sequence":0,"status":"running"}}`
	case "session.tasks.update":
		return `{"applied":true,"duplicate":false,"task":{"id":"task-1","type":"client","clientTaskId":"client-task-1","description":"RPC task","displayName":"RPC Task","activeTimeMs":500,"canCancel":true,"executionMode":"background","owner":{"displayName":"RPC owner","joinId":"join-1","kind":"sdk","participantId":"participant-1","presence":"connected","source":"rpc-test"},"sequence":1,"status":"running"}}`
	case "session.tools.execute":
		return `{"resultType":"success","textResultForLlm":"executed"}`
	case "session.tools.getBuiltinDescriptors":
		return `{"tools":[{"name":"rpc_builtin","description":"RPC built-in tool","hasSummariseIntention":true,"inputSchema":{"type":"object"},"instructions":"Use the RPC built-in.","isTerminal":false,"safeForTelemetry":true,"title":"RPC Built-in","type":"test"}]}`
	case "session.tools.taskCompleteEventData":
		return `{"objectiveId":17,"outcome":"completed","reason":"completed","success":true,"summary":"RPC task complete"}`
	case "session.workspaces.updateMetadata":
		return `{"path":"/tmp/rpc-workspace","workspace":{"id":"workspace-1","cwd":"/tmp/rpc-workspace","name":"Updated RPC workspace","branch":"rpc-branch","client_name":"rpc-client","created_at":"2026-09-18T11:00:00Z","git_root":"/tmp/rpc-workspace","remote_steerable":true}}`
	case "session.workspaces.ensure":
		return `{"path":"/tmp/rpc-workspace","workspace":{"id":"workspace-1","cwd":"/tmp/rpc-workspace","name":"RPC workspace"}}`
	case "session.workspaces.statFile":
		return `{"birthtimeMs":1000,"isDirectory":false,"isFile":true,"mtimeMs":2000,"size":42}`
	case "session.workspaces.addSummary":
		return `{"summary":{"number":3,"title":"RPC summary","content":"Summary content"},"workspace":{"id":"workspace-1","cwd":"/tmp/rpc-workspace","name":"RPC workspace"}}`
	case "session.workspaces.truncateSummaries":
		return `{"path":"/tmp/rpc-workspace","workspace":{"id":"workspace-1","cwd":"/tmp/rpc-workspace","name":"Truncated RPC workspace"}}`
	default:
		return `{}`
	}
}

func assertGeneratedRPCResult(t *testing.T, wire string, result any) {
	t.Helper()
	if wire == "sessions.getClientMetadata" {
		entries, ok := result.(*rpc.SessionsGetClientMetadataResult)
		if !ok || len(*entries) != 1 {
			t.Fatalf("%s result = %#v, want one metadata entry", wire, result)
		}
		entry, ok := (*entries)[0].(*rpc.SessionsClientMetadataEntryOk)
		if !ok || entry.SessionID != "persisted-session" || entry.Metadata["rpc/key"] != "rpc-value" {
			t.Fatalf("%s result entry = %#v", wire, (*entries)[0])
		}
		return
	}
	expectedJSON := generatedRPCResponse(wire)
	if expectedJSON == "{}" {
		return
	}
	var expected any
	if err := json.Unmarshal([]byte(expectedJSON), &expected); err != nil {
		t.Fatal(err)
	}
	actualJSON, err := json.Marshal(result)
	if err != nil {
		t.Fatalf("Marshal %s typed result: %v", wire, err)
	}
	var actual any
	if err := json.Unmarshal(actualJSON, &actual); err != nil {
		t.Fatalf("Decode %s typed result %s: %v", wire, actualJSON, err)
	}
	assertJSONSubset(t, wire+" result", expected, actual)
}

func assertJSONSubset(t *testing.T, label string, expected, actual any) {
	t.Helper()
	switch expected := expected.(type) {
	case map[string]any:
		actual, ok := actual.(map[string]any)
		if !ok {
			t.Fatalf("%s = %#v, want object", label, actual)
		}
		for key, value := range expected {
			actualValue, exists := actual[key]
			if !exists {
				t.Fatalf("%s missing %q in %#v", label, key, actual)
			}
			assertJSONSubset(t, label+"."+key, value, actualValue)
		}
	case []any:
		actual, ok := actual.([]any)
		if !ok || len(actual) != len(expected) {
			t.Fatalf("%s = %#v, want %d items", label, actual, len(expected))
		}
		for i := range expected {
			assertJSONSubset(t, label, expected[i], actual[i])
		}
	default:
		if !reflect.DeepEqual(expected, actual) {
			t.Fatalf("%s = %#v, want %#v", label, actual, expected)
		}
	}
}

func methodName(signature any) string {
	value := reflect.ValueOf(signature)
	function := runtime.FuncForPC(value.Pointer())
	if function == nil {
		panic("method expression has no runtime function")
	}
	name := function.Name()
	if dot := strings.LastIndexByte(name, '.'); dot >= 0 {
		return strings.TrimSuffix(name[dot+1:], "-fm")
	}
	panic("unexpected method expression name " + name)
}

func testContextWithTimeout(t *testing.T, timeout time.Duration) (context.Context, context.CancelFunc) {
	t.Helper()
	return context.WithTimeout(t.Context(), timeout)
}

type generatedRPCFixture struct {
	client  *copilot.Client
	session *copilot.Session
	server  *jsonrpc2.Client
	conn    net.Conn
}

func newGeneratedRPCFixture(t *testing.T, ctx context.Context) *generatedRPCFixture {
	t.Helper()
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = listener.Close() })

	type serverConnection struct {
		server *jsonrpc2.Client
		conn   net.Conn
	}
	ready := make(chan serverConnection, 1)
	go func() {
		conn, err := listener.Accept()
		if err != nil {
			return
		}
		server := jsonrpc2.NewClient(conn, conn)
		t.Cleanup(server.Stop)
		for method, result := range map[string]string{
			"connect":                `{"ok":true,"protocolVersion":4,"version":"test"}`,
			"plugins.builtin.set":    `{}`,
			"session.create":         `{"sessionId":"generated-rpc-surface"}`,
			"session.options.update": `{"success":true}`,
			"session.detach":         `{"success":true}`,
		} {
			server.SetRequestHandler(method, func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				return json.RawMessage(result), nil
			})
		}
		server.Start()
		ready <- serverConnection{server: server, conn: conn}
	}()

	client := copilot.NewClient(&copilot.ClientOptions{
		Connection: copilot.URIConnection{URL: listener.Addr().String()},
	})
	t.Cleanup(client.ForceStop)
	session, err := client.CreateSession(ctx, &copilot.SessionConfig{
		SessionID:           "generated-rpc-surface",
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
	})
	if err != nil {
		t.Fatal(err)
	}

	select {
	case connection := <-ready:
		return &generatedRPCFixture{client: client, session: session, server: connection.server, conn: connection.conn}
	case <-ctx.Done():
		t.Fatal(ctx.Err())
		return nil
	}
}
