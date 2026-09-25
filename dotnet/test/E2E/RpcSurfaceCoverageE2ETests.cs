/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using System.Text.Json;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// Direct coverage for public generated RPC methods that are not exercised by another test.
/// The deterministic stdio runtime verifies request serialization and returns property-rich
/// responses so the generated result projections are validated without network or timing inputs.
/// </summary>
[Trait(E2ETestTraits.Backend, E2ETestTraits.SelfConfiguredBackend)]
public class RpcSurfaceCoverageE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_surface_coverage", output)
{
    [Fact]
    public async Task Server_Rpcs_Serialize_Requests_And_Project_Results()
    {
        var (client, capturePath) = await CreateClientAsync();
        await using (client)
        {
            await client.StartAsync();

            await client.Rpc.RegisterExtensionLaunchProviderAsync();

            var commands = await client.Rpc.Commands.ListAsync();
            var command = Assert.Single(commands.Commands);
            Assert.Equal("rpc-command", command.Name);
            Assert.Equal("RPC command", command.Description);
            Assert.Equal(["rpc"], command.Aliases);
            Assert.True(command.AllowDuringAgentExecution);
            Assert.False(command.Experimental);
            Assert.Equal("<value>", command.Input!.Hint);
            Assert.False(command.Input.PreserveMultilineInput);
            Assert.True(command.Input.Required);
            Assert.Equal(SlashCommandKind.Builtin, command.Kind);
            Assert.True(command.Schedulable);

            var hooks = await client.Rpc.Hooks.DiscoverAsync(["Q:\\rpc-project"], excludeHostHooks: true);
            Assert.Empty(hooks.Hooks);
            Assert.Equal(["rpc-warning"], hooks.Warnings);
            Assert.Empty(hooks.Errors);

            Assert.True((await client.Rpc.LlmInference.SetProviderAsync()).Success);

            var managedSettings = await client.Rpc.ManagedSettings.ReadAsync();
            Assert.Null(managedSettings.ErrorMessage);
            Assert.Equal("strict", managedSettings.SettingsJson!.Value.GetProperty("policy").GetString());

            var install = await client.Rpc.Mcp.PlanInstallAsync(
                new CatalogClientContract
                {
                    ProtocolVersion = 3,
                    RequiredCapabilities = ["mcp-install-planning"],
                },
                new McpPlanInstallSourceCandidate
                {
                    CandidateHandle = "candidate-1",
                    SearchId = "search-1",
                },
                McpPlanScope.User);
            var unavailable = Assert.IsType<McpPlanInstallResultUnavailable>(install);
            Assert.Equal("The host does not provide installation.", unavailable.Message);
            Assert.Equal("host-not-available", unavailable.Reason.Value);

            var model = Assert.Single((await client.Rpc.Models.GetBuiltInCatalogAsync()).Models);
            Assert.Equal("built-in-model", model.Id);

            await client.Rpc.Plugins.Builtin.SetAsync(["Q:\\rpc-plugins"]);

            var metadataEntries = await client.Rpc.Sessions.GetClientMetadataAsync(
                ["persisted-session"],
                ["rpc/key"]);
            var metadata = Assert.IsType<SessionsClientMetadataEntryOk>(Assert.Single(metadataEntries));
            Assert.Equal("persisted-session", metadata.SessionId);
            Assert.Equal("rpc-value", metadata.Metadata["rpc/key"]);

            await client.Rpc.Skills.Config.SetSkillDisabledAsync("skill-one", disabled: true);
        }

        var requests = await RpcSurfaceTestCli.ReadRequestsAsync(capturePath);
        AssertCalledExactlyOnce(
            requests,
            "registerExtensionLaunchProvider",
            "commands.list",
            "hooks.discover",
            "llmInference.setProvider",
            "managedSettings.read",
            "mcp.planInstall",
            "models.getBuiltInCatalog",
            "plugins.builtin.set",
            "sessions.getClientMetadata",
            "skills.config.setSkillDisabled");

        var hooksRequest = GetParams(FindRequest(requests, "hooks.discover"));
        Assert.True(hooksRequest.GetProperty("excludeHostHooks").GetBoolean());
        Assert.Equal("Q:\\rpc-project", hooksRequest.GetProperty("projectPaths")[0].GetString());

        var installRequest = GetParams(FindRequest(requests, "mcp.planInstall"));
        Assert.Equal(3, installRequest.GetProperty("contract").GetProperty("protocolVersion").GetInt64());
        Assert.Equal("candidate", installRequest.GetProperty("source").GetProperty("kind").GetString());
        Assert.Equal("user", installRequest.GetProperty("scope").GetString());

        var plugins = GetParams(FindRequest(requests, "plugins.builtin.set"));
        Assert.Equal("Q:\\rpc-plugins", plugins.GetProperty("paths")[0].GetString());
        var skill = GetParams(FindRequest(requests, "skills.config.setSkillDisabled"));
        Assert.Equal("skill-one", skill.GetProperty("name").GetString());
        Assert.True(skill.GetProperty("disabled").GetBoolean());
    }

    [Fact]
    public async Task Session_Control_And_State_Rpcs_Project_All_Result_Properties()
    {
        var (client, capturePath) = await CreateClientAsync();
        await using (client)
        await using (var session = await Ctx.CreateSessionAsync(client, new SessionConfig()))
        {
            await session.Rpc.Agent.SetPromptAsync("agent-1", "Use the RPC prompt.");

            var exclusion = await session.Rpc.ContentExclusion.CheckPathsAsync(
                ["/tmp/rpc-workspace/file.txt"]);
            Assert.True(exclusion.Available);
            var pathCheck = Assert.Single(exclusion.Checks);
            Assert.Equal("/tmp/rpc-workspace/file.txt", pathCheck.Path);
            Assert.False(pathCheck.Excluded);

            var logs = await session.Rpc.Debug.CollectLogsAsync(
                new DebugCollectLogsDestinationDirectory { OutputDirectory = "/tmp/rpc-debug" },
                new DebugCollectLogsInclude
                {
                    Events = true,
                    ProcessLogs = false,
                    ShellLogs = true,
                },
                [
                    new DebugCollectLogsEntry
                    {
                        BundlePath = "host/diagnostic.txt",
                        Kind = new DebugCollectLogsEntryKind("file"),
                        Path = "/tmp/diagnostic.txt",
                        Required = true,
                    },
                ]);
            Assert.Equal("directory", logs.Kind.Value);
            Assert.Equal("/tmp/rpc-debug", logs.Path);
            var includedLog = Assert.Single(logs.Entries);
            Assert.Equal("host/diagnostic.txt", includedLog.BundlePath);
            Assert.Equal(123, includedLog.SizeBytes);
            Assert.Equal(DebugCollectLogsSource.Additional, includedLog.Source);
            var skippedLog = Assert.Single(logs.SkippedEntries!);
            Assert.Equal("host/missing.txt", skippedLog.BundlePath);
            Assert.Equal("/tmp/missing.txt", skippedLog.Path);
            Assert.Equal("not found", skippedLog.Reason);

            Assert.Equal(4, (await session.Rpc.History.ClearContextAsync("Reset context.")).MessagesCleared);

            var prediction = await session.Rpc.LimitPrediction.PredictAsync(
                new SessionLimitPredictionPredictRequest
                {
                    ClientType = new SessionLimitPredictionClientType("sdk"),
                    ModelId = "model-a",
                });
            var unavailable = Assert.IsType<SessionLimitPredictionResultUnavailable>(prediction);
            Assert.Equal("insufficient-data", unavailable.Reason.Value);

            var clientMetadata = await session.Rpc.Metadata.GetClientMetadataAsync();
            Assert.Equal("rpc-value", clientMetadata["rpc/key"]);
            Assert.Equal("other-value", clientMetadata["rpc/other"]);

            var allowed = await session.Rpc.Model.SetAllowedModelsAsync(["model-a", "model-b"]);
            Assert.Equal(["model-a", "model-b"], allowed.AllowedModels);
            Assert.Equal(["model-a"], allowed.EffectiveAllowedModels);
            Assert.Equal("model-a", allowed.FallbackModel);
            Assert.Equal("model-a", allowed.ModelId);

            var tier = await session.Rpc.Model.SwitchAutoTierAsync(AutoTier.Intelligence);
            Assert.Equal("applied", tier.Status.Value);
            Assert.Equal(AutoTier.Intelligence, tier.ActivatingAutoTier);
            Assert.Equal(AutoTier.Intelligence, tier.EffectiveAutoTier);
            Assert.Null(tier.PendingAutoTier);
            Assert.Equal(AutoTier.Balance, tier.SupersededAutoTier);

            var enforcement = await session.Rpc.Sandbox.GetEnforcementStatusAsync();
            Assert.True(enforcement.Required);
            Assert.False(enforcement.Blocked);
            Assert.Equal("managed-policy", enforcement.Reason);

            var disabled = await session.Rpc.Sandbox.DisableForSessionAsync("sandbox-request-1");
            Assert.True(disabled.Success);
            Assert.False(disabled.Enabled);

            var abort = await session.Rpc.AbortAsync(new AbortReason("user"));
            Assert.True(abort.Success);
            Assert.Null(abort.Error);

            Assert.True((await session.Rpc.InterruptMainTurnAsync(flushQueued: true)).Interrupted);
            Assert.Equal(3, await session.Rpc.CancelAllBackgroundAgentsAsync());

            var log = await session.Rpc.LogAsync(
                "RPC log",
                level: SessionLogLevel.Warning,
                type: "rpc",
                ephemeral: true,
                url: "https://example.test/rpc",
                tip: "Inspect the RPC.");
            Assert.Equal(Guid.Parse("11111111-2222-3333-4444-555555555555"), log.EventId);
        }

        var requests = await RpcSurfaceTestCli.ReadRequestsAsync(capturePath);
        AssertCalledExactlyOnce(
            requests,
            "session.agent.setPrompt",
            "session.contentExclusion.checkPaths",
            "session.debug.collectLogs",
            "session.history.clearContext",
            "session.limitPrediction.predict",
            "session.metadata.getClientMetadata",
            "session.model.setAllowedModels",
            "session.model.switchAutoTier",
            "session.sandbox.getEnforcementStatus",
            "session.sandbox.disableForSession",
            "session.abort",
            "session.interruptMainTurn",
            "session.cancelAllBackgroundAgents",
            "session.log");

        var logRequest = GetParams(FindRequest(requests, "session.log"));
        Assert.Equal("warning", logRequest.GetProperty("level").GetString());
        Assert.True(logRequest.GetProperty("ephemeral").GetBoolean());
        Assert.Equal("https://example.test/rpc", logRequest.GetProperty("url").GetString());

        var prompt = GetParams(FindRequest(requests, "session.agent.setPrompt"));
        Assert.Equal("agent-1", prompt.GetProperty("id").GetString());
        Assert.Equal("Use the RPC prompt.", prompt.GetProperty("prompt").GetString());
        var disable = GetParams(FindRequest(requests, "session.sandbox.disableForSession"));
        Assert.Equal("sandbox-request-1", disable.GetProperty("requestId").GetString());
        var interrupt = GetParams(FindRequest(requests, "session.interruptMainTurn"));
        Assert.True(interrupt.GetProperty("flushQueued").GetBoolean());
    }

    [Fact]
    public async Task Workflow_Rpcs_Project_Run_Journal_And_Agent_State()
    {
        var (client, capturePath) = await CreateClientAsync();
        await using (client)
        await using (var session = await Ctx.CreateSessionAsync(client, new SessionConfig()))
        {
            var run = await session.Rpc.Workflow.RunAsync(
                "rpc-workflow",
                ParseJson("""{ "input": 42 }"""),
                new WorkflowRunOptions
                {
                    Limits = new WorkflowRunLimits
                    {
                        MaxAiCredits = 2.5,
                        MaxConcurrentSubagents = 2,
                        MaxTotalSubagents = 4,
                        TimeoutSeconds = 30,
                    },
                    LogPhaseNames = true,
                    NotifyOnComplete = false,
                });
            Assert.Equal("workflow-run-1", run.RunId);
            Assert.Equal(WorkflowRunStatus.Running, run.Status);
            Assert.Equal(1, run.Attempt);
            Assert.Equal("running", run.Result!.Value.GetProperty("value").GetString());
            Assert.Equal(1, run.Snapshot!.Value.GetProperty("step").GetInt32());

            var resumed = await session.Rpc.Workflow.ResumeAsync(
                "workflow-run-1",
                new WorkflowRunLimits { MaxTotalSubagents = 8 },
                notifyOnComplete: true,
                logPhaseNames: false);
            Assert.Equal("rpc-workflow", resumed.WorkflowName);
            Assert.Equal(WorkflowRunStatus.Running, resumed.Run.Status);
            Assert.Equal(2, resumed.Run.Attempt);

            var current = await session.Rpc.Workflow.GetRunAsync("workflow-run-1");
            Assert.Equal("workflow-run-1", current.RunId);
            Assert.Equal(WorkflowRunStatus.Running, current.Status);

            var paused = await session.Rpc.Workflow.PauseAsync("workflow-run-1");
            Assert.Equal(WorkflowRunStatus.Paused, paused.Status);
            Assert.Equal("caller requested pause", paused.Reason);
            Assert.Equal(2, paused.Snapshot!.Value.GetProperty("step").GetInt32());

            await session.Rpc.Workflow.LogAsync(
                "workflow-run-1",
                "execution-token-1",
                [
                    new WorkflowLogLine
                    {
                        Kind = WorkflowLogLineKind.Log,
                        Seq = 7,
                        Text = "Workflow progress",
                    },
                ]);

            var agent = await session.Rpc.Workflow.AgentAsync(
                "workflow-run-1",
                "execution-token-1",
                "Complete the RPC task.",
                new WorkflowAgentOptions
                {
                    Agent = "explore",
                    Label = "rpc-agent",
                    Model = "model-a",
                    ReasoningEffort = "high",
                });
            Assert.Equal("agent-result", agent.Result!.Value.GetProperty("answer").GetString());

            var journal = await session.Rpc.Workflow.Journal.GetAsync(
                "workflow-run-1",
                "execution-token-1",
                "checkpoint");
            Assert.True(journal.Hit);
            Assert.Equal(7, journal.ResultJson!.Value.GetProperty("checkpoint").GetInt32());

            await session.Rpc.Workflow.Journal.PutAsync(
                "workflow-run-1",
                "execution-token-1",
                "checkpoint",
                ParseJson("""{ "checkpoint": 8 }"""));
        }

        var requests = await RpcSurfaceTestCli.ReadRequestsAsync(capturePath);
        AssertCalledExactlyOnce(
            requests,
            "session.workflow.run",
            "session.workflow.resume",
            "session.workflow.getRun",
            "session.workflow.pause",
            "session.workflow.log",
            "session.workflow.agent",
            "session.workflow.journal.get",
            "session.workflow.journal.put");

        var runRequest = GetParams(FindRequest(requests, "session.workflow.run"));
        Assert.Equal(42, runRequest.GetProperty("args").GetProperty("input").GetInt32());
        Assert.Equal(2.5, runRequest.GetProperty("options").GetProperty("limits").GetProperty("maxAiCredits").GetDouble());
        Assert.True(runRequest.GetProperty("options").GetProperty("logPhaseNames").GetBoolean());

        var factoryLog = GetParams(FindRequest(requests, "session.workflow.log"));
        Assert.Equal("execution-token-1", factoryLog.GetProperty("executionToken").GetString());
        var line = Assert.Single(factoryLog.GetProperty("lines").EnumerateArray());
        Assert.Equal("log", line.GetProperty("kind").GetString());
        Assert.Equal(7, line.GetProperty("seq").GetInt64());
        Assert.Equal("Workflow progress", line.GetProperty("text").GetString());

        var journalPut = GetParams(FindRequest(requests, "session.workflow.journal.put"));
        Assert.Equal("checkpoint", journalPut.GetProperty("key").GetString());
        Assert.Equal(8, journalPut.GetProperty("resultJson").GetProperty("checkpoint").GetInt32());
    }

    [Fact]
    public async Task Mcp_Rpcs_Project_Resource_And_Oauth_State()
    {
        var (client, capturePath) = await CreateClientAsync();
        await using (client)
        await using (var session = await Ctx.CreateSessionAsync(client, new SessionConfig()))
        {
            Assert.True((await session.Rpc.Mcp.MoveLoadingToBackgroundAsync()).MovedToBackground);
            await session.Rpc.Mcp.StartServerAsync(
                "rpc-server",
                ParseJson("""{ "command": "node", "args": ["server.js"] }"""));
            await session.Rpc.Mcp.Oauth.AuthenticationStateChangedAsync(
                "rpc-server",
                refreshSessionToken: true);
            Assert.True((await session.Rpc.Mcp.Oauth.RespondAsync("oauth-request-1")).Success);

            var resources = await session.Rpc.Mcp.Resources.ListAsync("rpc-server", "resource-cursor");
            Assert.Equal("resource-next", resources.NextCursor);
            var resource = Assert.Single(resources.Resources);
            Assert.Equal("file://rpc/resource.txt", resource.Uri);
            Assert.Equal("RPC resource", resource.Name);
            Assert.Equal("Resource description", resource.Description);
            Assert.Equal("text/plain", resource.MimeType);
            Assert.Equal(16, resource.Size);
            Assert.Equal("RPC Resource", resource.Title);

            var templates = await session.Rpc.Mcp.Resources.ListTemplatesAsync("rpc-server", "template-cursor");
            Assert.Equal("template-next", templates.NextCursor);
            var template = Assert.Single(templates.ResourceTemplates);
            Assert.Equal("file://rpc/{name}", template.UriTemplate);
            Assert.Equal("RPC template", template.Name);
            Assert.Equal("Template description", template.Description);
            Assert.Equal("text/plain", template.MimeType);
            Assert.Equal("RPC Template", template.Title);

            var read = await session.Rpc.Mcp.Resources.ReadAsync("rpc-server", "file://rpc/resource.txt");
            var content = Assert.Single(read.Contents);
            Assert.Equal("file://rpc/resource.txt", content.Uri);
            Assert.Equal("text/plain", content.MimeType);
            Assert.Equal("resource-content", content.Text);
            Assert.Null(content.Blob);
            Assert.Equal("assistant", content.Meta!["audience"].GetString());
        }

        var requests = await RpcSurfaceTestCli.ReadRequestsAsync(capturePath);
        AssertCalledExactlyOnce(
            requests,
            "session.mcp.moveLoadingToBackground",
            "session.mcp.startServer",
            "session.mcp.oauth.authenticationStateChanged",
            "session.mcp.oauth.respond",
            "session.mcp.resources.list",
            "session.mcp.resources.listTemplates",
            "session.mcp.resources.read");

        var start = GetParams(FindRequest(requests, "session.mcp.startServer"));
        Assert.Equal("rpc-server", start.GetProperty("serverName").GetString());
        Assert.Equal("node", start.GetProperty("config").GetProperty("command").GetString());
        var auth = GetParams(FindRequest(requests, "session.mcp.oauth.authenticationStateChanged"));
        Assert.True(auth.GetProperty("refreshSessionToken").GetBoolean());
        var list = GetParams(FindRequest(requests, "session.mcp.resources.list"));
        Assert.Equal("resource-cursor", list.GetProperty("cursor").GetString());
        var listTemplates = GetParams(FindRequest(requests, "session.mcp.resources.listTemplates"));
        Assert.Equal("template-cursor", listTemplates.GetProperty("cursor").GetString());
        var readRequest = GetParams(FindRequest(requests, "session.mcp.resources.read"));
        Assert.Equal("file://rpc/resource.txt", readRequest.GetProperty("uri").GetString());
    }

    [Fact]
    public async Task Tasks_And_Tools_Rpcs_Project_And_Serialize_Complete_State()
    {
        var (client, capturePath) = await CreateClientAsync();
        await using (client)
        await using (var session = await Ctx.CreateSessionAsync(client, new SessionConfig()))
        {
            var registered = await session.Rpc.Tasks.RegisterAsync(
                TaskClientType.Client,
                "client-task-1",
                "RPC task",
                cancellable: true,
                displayName: "RPC Task");
            Assert.True(registered.Created);
            Assert.False(registered.Reclaimed);
            Assert.Equal("task-1", registered.Task.Id);
            Assert.Equal(TaskClientType.Client, registered.Task.Type);
            Assert.Equal("client-task-1", registered.Task.ClientTaskId);
            Assert.Equal("RPC task", registered.Task.Description);
            Assert.Equal("RPC Task", registered.Task.DisplayName);
            Assert.Equal(0, registered.Task.Sequence);
            Assert.Equal("running", registered.Task.Status.Value);
            Assert.Equal(500, registered.Task.ActiveTimeMs);
            Assert.True(registered.Task.CanCancel);
            Assert.Equal(TaskClientExecutionMode.Background, registered.Task.ExecutionMode);
            Assert.Equal("RPC owner", registered.Task.Owner.DisplayName);
            Assert.Equal("join-1", registered.Task.Owner.JoinId);
            Assert.Equal(TaskClientOwnerKind.Sdk, registered.Task.Owner.Kind);
            Assert.Equal("participant-1", registered.Task.Owner.ParticipantId);
            Assert.Equal(TaskClientOwnerPresence.Connected, registered.Task.Owner.Presence);
            Assert.Equal("rpc-test", registered.Task.Owner.Source);

            var updated = await session.Rpc.Tasks.UpdateAsync(
                "task-1",
                sequence: 1,
                new TaskClientUpdateProgress
                {
                    Message = "Halfway",
                    Percentage = 50,
                    Phase = "work",
                    Status = new TaskClientActiveStatus("running"),
                });
            Assert.True(updated.Applied);
            Assert.False(updated.Duplicate);
            Assert.Equal(1, updated.Task.Sequence);

            var executed = await session.Rpc.Tools.ExecuteAsync(
                "rpc_tool",
                ParseJson("""{ "value": "input" }"""),
                toolCallId: "tool-call-1");
            Assert.Equal("success", executed.GetProperty("resultType").GetString());
            Assert.Equal("executed", executed.GetProperty("textResultForLlm").GetString());

            var descriptors = await session.Rpc.Tools.GetBuiltinDescriptorsAsync(
                reduceUserIntervention: true,
                includeAuthor: true,
                skillEmbeddingEnabled: false,
                shellConfig: new ToolsShellDescriptorConfig
                {
                    DisplayName = "PowerShell",
                    ShellType = "powershell",
                    ShellToolName = "shell",
                    ListShellsToolName = "list_shells",
                    ReadShellToolName = "read_shell",
                    StopShellToolName = "stop_shell",
                    DescriptionLines = ["Runs shell commands."],
                },
                shellSupportsPowerShell7Syntax: true,
                shellTimeoutMs: 1234,
                backgroundTaskNotificationsEnabled: true);
            var descriptor = Assert.Single(descriptors.Tools);
            Assert.Equal("rpc_builtin", descriptor.Name);
            Assert.Equal("RPC built-in tool", descriptor.Description);
            Assert.True(descriptor.HasSummariseIntention);
            Assert.Equal(BuiltinToolInputSchemaType.Object, descriptor.InputSchema!.Type);
            Assert.Equal("Use the RPC built-in.", descriptor.Instructions);
            Assert.False(descriptor.IsTerminal);
            Assert.True(descriptor.SafeForTelemetry.GetBoolean());
            Assert.Equal("RPC Built-in", descriptor.Title);
            Assert.Equal("test", descriptor.Type);

            using var parameterType = JsonDocument.Parse("\"object\"");
            await session.Rpc.Tools.SetAsync(
                [
                    new ProtocolExternalToolDefinition
                    {
                        Name = "rpc_external",
                        Title = "RPC External",
                        Description = "External RPC tool",
                        Parameters = new Dictionary<string, JsonElement>
                        {
                            ["type"] = parameterType.RootElement.Clone(),
                        },
                        IsTerminal = false,
                        OverridesBuiltInTool = false,
                        SkipPermission = true,
                    },
                ]);

            var completion = await session.Rpc.Tools.TaskCompleteEventDataAsync(
                ParseJson("""{ "objectiveId": 17 }"""),
                new ToolResultExpanded
                {
                    ResultType = ToolResultType.Success,
                    TextResultForLlm = "RPC task complete",
                    SessionLog = "Completion logged.",
                });
            Assert.Equal(17, completion.ObjectiveId);
            Assert.Equal(TaskCompletionOutcome.Completed, completion.Outcome);
            Assert.Equal("completed", completion.Reason);
            Assert.True(completion.Success);
            Assert.Equal("RPC task complete", completion.Summary);
        }

        var requests = await RpcSurfaceTestCli.ReadRequestsAsync(capturePath);
        AssertCalledExactlyOnce(
            requests,
            "session.tasks.register",
            "session.tasks.update",
            "session.tools.execute",
            "session.tools.getBuiltinDescriptors",
            "session.tools.set",
            "session.tools.taskCompleteEventData");

        var execute = GetParams(FindRequest(requests, "session.tools.execute"));
        Assert.Equal("rpc_tool", execute.GetProperty("name").GetString());
        Assert.Equal("input", execute.GetProperty("arguments").GetProperty("value").GetString());
        Assert.Equal("tool-call-1", execute.GetProperty("toolCallId").GetString());

        var set = GetParams(FindRequest(requests, "session.tools.set"));
        var tool = Assert.Single(set.GetProperty("tools").EnumerateArray());
        Assert.Equal("rpc_external", tool.GetProperty("name").GetString());
        Assert.True(tool.GetProperty("skipPermission").GetBoolean());

        var register = GetParams(FindRequest(requests, "session.tasks.register"));
        Assert.Equal("client", register.GetProperty("type").GetString());
        Assert.Equal("client-task-1", register.GetProperty("clientTaskId").GetString());
        Assert.True(register.GetProperty("cancellable").GetBoolean());
        var update = GetParams(FindRequest(requests, "session.tasks.update"));
        Assert.Equal(1, update.GetProperty("sequence").GetInt64());
        Assert.Equal("progress", update.GetProperty("update").GetProperty("kind").GetString());

        var descriptorsRequest = GetParams(FindRequest(requests, "session.tools.getBuiltinDescriptors"));
        Assert.True(descriptorsRequest.GetProperty("reduceUserIntervention").GetBoolean());
        Assert.Equal(1234, descriptorsRequest.GetProperty("shellTimeoutMs").GetInt64());
        Assert.Equal("powershell", descriptorsRequest.GetProperty("shellConfig").GetProperty("shellType").GetString());
    }

    [Fact]
    public async Task Workspace_Rpcs_Serialize_Mutations_And_Project_Metadata()
    {
        var (client, capturePath) = await CreateClientAsync();
        await using (client)
        await using (var session = await Ctx.CreateSessionAsync(client, new SessionConfig()))
        {
            var updated = await session.Rpc.Workspaces.UpdateMetadataAsync(
                ParseJson("""{ "owner": "rpc-test" }"""),
                name: "Updated RPC workspace");
            Assert.Equal("/tmp/rpc-workspace", updated.Path);
            Assert.Equal("workspace-1", updated.Workspace!.Id);
            Assert.Equal("/tmp/rpc-workspace", updated.Workspace.Cwd);
            Assert.Equal("Updated RPC workspace", updated.Workspace.Name);
            Assert.Equal("rpc-branch", updated.Workspace.Branch);
            Assert.Equal("rpc-client", updated.Workspace.ClientName);
            Assert.Equal(DateTimeOffset.Parse("2026-09-18T11:00:00.000Z"), updated.Workspace.CreatedAt);
            Assert.Equal("/tmp/rpc-workspace", updated.Workspace.GitRoot);
            Assert.True(updated.Workspace.RemoteSteerable);

            var ensured = await session.Rpc.Workspaces.EnsureAsync(ParseJson("""{ "owner": "rpc-test" }"""));
            Assert.Equal("/tmp/rpc-workspace", ensured.Path);
            Assert.Equal("RPC workspace", ensured.Workspace!.Name);

            var stat = await session.Rpc.Workspaces.StatFileAsync("folder/file.txt");
            Assert.True(stat.IsFile);
            Assert.False(stat.IsDirectory);
            Assert.Equal(42, stat.Size);
            Assert.Equal(1000, stat.BirthtimeMs);
            Assert.Equal(2000, stat.MtimeMs);

            await session.Rpc.Workspaces.CreateDirectoryAsync("folder/nested", recursive: true);
            await session.Rpc.Workspaces.RenamePathAsync("folder/file.txt", "folder/renamed.txt");
            await session.Rpc.Workspaces.RemovePathAsync("folder", recursive: true, force: true);

            var summary = await session.Rpc.Workspaces.AddSummaryAsync("RPC summary", "Summary content");
            Assert.NotNull(summary.Summary);
            Assert.NotNull(summary.Workspace);

            var truncated = await session.Rpc.Workspaces.TruncateSummariesAsync(keepCount: 2);
            Assert.Equal("/tmp/rpc-workspace", truncated.Path);
            Assert.Equal("Truncated RPC workspace", truncated.Workspace!.Name);
        }

        var requests = await RpcSurfaceTestCli.ReadRequestsAsync(capturePath);
        AssertCalledExactlyOnce(
            requests,
            "session.workspaces.updateMetadata",
            "session.workspaces.ensure",
            "session.workspaces.statFile",
            "session.workspaces.createDirectory",
            "session.workspaces.renamePath",
            "session.workspaces.removePath",
            "session.workspaces.addSummary",
            "session.workspaces.truncateSummaries");

        var createDirectory = GetParams(FindRequest(requests, "session.workspaces.createDirectory"));
        Assert.Equal("folder/nested", createDirectory.GetProperty("path").GetString());
        Assert.True(createDirectory.GetProperty("recursive").GetBoolean());

        var rename = GetParams(FindRequest(requests, "session.workspaces.renamePath"));
        Assert.Equal("folder/file.txt", rename.GetProperty("source").GetString());
        Assert.Equal("folder/renamed.txt", rename.GetProperty("destination").GetString());

        var remove = GetParams(FindRequest(requests, "session.workspaces.removePath"));
        Assert.True(remove.GetProperty("recursive").GetBoolean());
        Assert.True(remove.GetProperty("force").GetBoolean());

        var updateMetadata = GetParams(FindRequest(requests, "session.workspaces.updateMetadata"));
        Assert.Equal("rpc-test", updateMetadata.GetProperty("context").GetProperty("owner").GetString());
        Assert.Equal("Updated RPC workspace", updateMetadata.GetProperty("name").GetString());
        var addSummary = GetParams(FindRequest(requests, "session.workspaces.addSummary"));
        Assert.Equal("RPC summary", addSummary.GetProperty("title").GetString());
        Assert.Equal("Summary content", addSummary.GetProperty("content").GetString());
        Assert.Equal(2, GetParams(FindRequest(requests, "session.workspaces.truncateSummaries"))
            .GetProperty("keepCount").GetInt64());
    }

    private async Task<(CopilotClient Client, string CapturePath)> CreateClientAsync()
    {
        var (cliPath, capturePath) = await RpcSurfaceTestCli.CreateAsync(Ctx);
        var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(
                path: cliPath,
                args: ["--capture-file", capturePath]),
            UseLoggedInUser = false,
        });
        return (client, capturePath);
    }

    private static JsonElement FindRequest(JsonElement[] requests, string method) =>
        Assert.Single(requests, request => request.GetProperty("method").GetString() == method);

    private static JsonElement GetParams(JsonElement request)
    {
        var parameters = request.GetProperty("params");
        return parameters.ValueKind == JsonValueKind.Array ? parameters[0] : parameters;
    }

    private static void AssertCalledExactlyOnce(JsonElement[] requests, params string[] methods)
    {
        Assert.All(
            methods,
            method =>
            {
                var request = Assert.Single(
                    requests,
                    request => request.GetProperty("method").GetString() == method);
                if (method.StartsWith("session.", StringComparison.Ordinal))
                {
                    Assert.False(string.IsNullOrWhiteSpace(GetParams(request).GetProperty("sessionId").GetString()));
                }
            });
    }

    private static JsonElement ParseJson(string json)
    {
        using var document = JsonDocument.Parse(json);
        return document.RootElement.Clone();
    }
}
