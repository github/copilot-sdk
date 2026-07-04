/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics;
using System.Globalization;
using System.Net;
using System.Net.Sockets;
using System.Text.Json;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class ClientOptionsE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "client_options", output)
{
    [Fact]
    public async Task Should_Listen_On_Configured_Tcp_Port()
    {
        var port = GetAvailableTcpPort();
        await using var client = Ctx.CreateClient(
            options: new CopilotClientOptions { Connection = RuntimeConnection.ForTcp(port: port) });

        await client.StartAsync();
        Assert.Equal(port, client.RuntimePort);

        var response = await client.PingAsync("fixed-port");
        Assert.Equal("pong: fixed-port", response.Message);
    }

    [Fact]
    public async Task Should_Use_Client_Cwd_For_Default_WorkingDirectory()
    {
        var clientCwd = Path.Join(Ctx.WorkDir, "client-cwd");
        Directory.CreateDirectory(clientCwd);
        await File.WriteAllTextAsync(Path.Join(clientCwd, "marker.txt"), "I am in the client cwd");

        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            WorkingDirectory = clientCwd,
        });

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var message = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Read the file marker.txt and tell me what it says",
        });

        Assert.Contains("client cwd", message?.Data.Content ?? string.Empty);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Propagate_Process_Options_To_Spawned_Cli()
    {
        var (cliPath, capturePath) = await CreateFakeCliCaptureAsync();
        var telemetryPath = Path.Join(Ctx.WorkDir, "telemetry.jsonl");
        var copilotHomeFromEnv = Path.Join(Ctx.WorkDir, "copilot-home-from-env");
        var copilotHomeFromOption = Path.Join(Ctx.WorkDir, "copilot-home-from-option");
        var clientEnv = Ctx.GetEnvironment().ToDictionary(pair => pair.Key, pair => pair.Value);
        clientEnv["COPILOT_HOME"] = copilotHomeFromEnv;
        await File.WriteAllTextAsync(cliPath, FakeStdioCliScript);

        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(path: cliPath, args: ["--capture-file", capturePath]),
            BaseDirectory = copilotHomeFromOption,
            Environment = clientEnv,
            GitHubToken = "process-option-token",
            LogLevel = CopilotLogLevel.Debug,
            SessionIdleTimeoutSeconds = 17,
            Telemetry = new TelemetryConfig
            {
                OtlpEndpoint = "http://127.0.0.1:4318",
                OtlpProtocol = "http/protobuf",
                FilePath = telemetryPath,
                ExporterType = "file",
                SourceName = "dotnet-sdk-e2e",
                CaptureContent = true,
            },
            UseLoggedInUser = false,
        });

        await client.StartAsync();

        using var capture = JsonDocument.Parse(await File.ReadAllTextAsync(capturePath));
        var root = capture.RootElement;
        var args = root.GetProperty("args").EnumerateArray().Select(e => e.GetString()).ToArray();
        var capturedEnv = root.GetProperty("env");

        AssertArgumentValue(args, "--log-level", "debug");
        Assert.Contains("--stdio", args);
        AssertArgumentValue(args, "--auth-token-env", "COPILOT_SDK_AUTH_TOKEN");
        Assert.Contains("--no-auto-login", args);
        AssertArgumentValue(args, "--session-idle-timeout", "17");
        Assert.Equal(Path.GetFullPath(Ctx.WorkDir), root.GetProperty("cwd").GetString());

        Assert.Equal(copilotHomeFromOption, capturedEnv.GetProperty("COPILOT_HOME").GetString());
        Assert.Equal("process-option-token", capturedEnv.GetProperty("COPILOT_SDK_AUTH_TOKEN").GetString());
        Assert.Equal("true", capturedEnv.GetProperty("COPILOT_OTEL_ENABLED").GetString());
        Assert.Equal("http://127.0.0.1:4318", capturedEnv.GetProperty("OTEL_EXPORTER_OTLP_ENDPOINT").GetString());
        Assert.Equal("http/protobuf", capturedEnv.GetProperty("OTEL_EXPORTER_OTLP_PROTOCOL").GetString());
        Assert.Equal(telemetryPath, capturedEnv.GetProperty("COPILOT_OTEL_FILE_EXPORTER_PATH").GetString());
        Assert.Equal("file", capturedEnv.GetProperty("COPILOT_OTEL_EXPORTER_TYPE").GetString());
        Assert.Equal("dotnet-sdk-e2e", capturedEnv.GetProperty("COPILOT_OTEL_SOURCE_NAME").GetString());
        Assert.Equal("true", capturedEnv.GetProperty("OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT").GetString());

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            EnableConfigDiscovery = true,
            IncludeSubAgentStreamingEvents = false,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        using var updatedCapture = JsonDocument.Parse(await File.ReadAllTextAsync(capturePath));
        var createRequest = GetCapturedRequestParams(updatedCapture.RootElement, "session.create");
        Assert.True(createRequest.GetProperty("enableConfigDiscovery").GetBoolean());
        Assert.False(createRequest.GetProperty("includeSubAgentStreamingEvents").GetBoolean());

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Forward_EnableSessionTelemetry_In_Wire_Request()
    {
        var (cliPath, capturePath) = await CreateFakeCliCaptureAsync();

        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(path: cliPath, args: ["--capture-file", capturePath]),
            UseLoggedInUser = false,
        });

        await client.StartAsync();

        // When explicitly set to false, it should appear in the wire request
        var session = await client.CreateSessionAsync(new SessionConfig
        {
            EnableSessionTelemetry = false,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        using var capture = JsonDocument.Parse(await File.ReadAllTextAsync(capturePath));
        var createRequest = GetCapturedRequestParams(capture.RootElement, "session.create");
        Assert.False(createRequest.GetProperty("enableSessionTelemetry").GetBoolean());

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Omit_EnableSessionTelemetry_When_Not_Set()
    {
        var (cliPath, capturePath) = await CreateFakeCliCaptureAsync();

        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(path: cliPath, args: ["--capture-file", capturePath]),
            UseLoggedInUser = false,
        });

        await client.StartAsync();

        // When omitted (null/default), the field should not be present in the wire request
        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        using var capture = JsonDocument.Parse(await File.ReadAllTextAsync(capturePath));
        var createRequest = GetCapturedRequestParams(capture.RootElement, "session.create");
        Assert.False(createRequest.TryGetProperty("enableSessionTelemetry", out _));

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Forward_Granular_Multitenancy_Fields_In_Create_Wire_Request()
    {
        var (cliPath, capturePath) = await CreateFakeCliCaptureAsync();

        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(path: cliPath, args: ["--capture-file", capturePath]),
            UseLoggedInUser = false,
        });

        await client.StartAsync();

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            SkipEmbeddingRetrieval = false,
            OrganizationCustomInstructions = "Follow org policy.",
            EnableOnDemandInstructionDiscovery = true,
            EmbeddingCacheStorage = EmbeddingCacheStorageMode.Persistent,
            EnableFileHooks = true,
            EnableHostGitOperations = false,
            EnableSessionStore = true,
            EnableSkills = false,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        using var capture = JsonDocument.Parse(await File.ReadAllTextAsync(capturePath));
        var createRequest = GetCapturedRequestParams(capture.RootElement, "session.create");
        Assert.False(createRequest.GetProperty("skipEmbeddingRetrieval").GetBoolean());
        Assert.Equal("Follow org policy.", createRequest.GetProperty("organizationCustomInstructions").GetString());
        Assert.True(createRequest.GetProperty("enableOnDemandInstructionDiscovery").GetBoolean());
        Assert.Equal("persistent", createRequest.GetProperty("embeddingCacheStorage").GetString());
        Assert.True(createRequest.GetProperty("enableFileHooks").GetBoolean());
        Assert.False(createRequest.GetProperty("enableHostGitOperations").GetBoolean());
        Assert.True(createRequest.GetProperty("enableSessionStore").GetBoolean());
        Assert.False(createRequest.GetProperty("enableSkills").GetBoolean());

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Forward_Advanced_Session_Options_In_Create_Wire_Request()
    {
        var (cliPath, capturePath) = await CreateFakeCliCaptureAsync();
        var outputDirectory = Path.Join(Ctx.WorkDir, "large-output-create");

        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(path: cliPath, args: ["--capture-file", capturePath]),
            UseLoggedInUser = false,
        });

        await client.StartAsync();

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            ClientName = "advanced-create-client",
            Model = "claude-sonnet-4.5",
            ReasoningEffort = "medium",
            ReasoningSummary = ReasoningSummary.Detailed,
            ContextTier = ContextTier.LongContext,
            EnableCitations = true,
            Capi = new CapiSessionOptions { EnableWebSocketResponses = false },
            McpOAuthTokenStorage = McpOAuthTokenStorageMode.Persistent,
            CustomAgents =
            [
                new CustomAgentConfig
                {
                    Name = "agent-one",
                    DisplayName = "Agent One",
                    Description = "Handles agent-one tasks.",
                    Prompt = "Be agent one.",
                    Tools = ["view"],
                    Infer = true,
                    Skills = ["create-skill"],
                    Model = "claude-haiku-4.5",
                },
            ],
            DefaultAgent = new DefaultAgentConfig { ExcludedTools = ["edit"] },
            Agent = "agent-one",
            SkillDirectories = ["skills-create"],
            DisabledSkills = ["disabled-create-skill"],
            PluginDirectories = ["plugins-create"],
            InfiniteSessions = new InfiniteSessionConfig
            {
                Enabled = false,
                BackgroundCompactionThreshold = 0.5,
                BufferExhaustionThreshold = 0.9,
            },
            LargeOutput = new LargeToolOutputConfig
            {
                Enabled = true,
                MaxSizeBytes = 4096,
                OutputDirectory = outputDirectory,
            },
            Memory = new MemoryConfiguration { Enabled = true },
            GitHubToken = "session-create-token",
            RemoteSession = GitHub.Copilot.Rpc.RemoteSessionMode.Export,
            Cloud = new CloudSessionOptions
            {
                Repository = new CloudSessionRepository
                {
                    Owner = "github",
                    Name = "copilot-sdk",
                    Branch = "main",
                },
            },
            EnableMcpApps = true,
            RequestCanvasRenderer = true,
            RequestExtensions = true,
            ExtensionSdkPath = "custom-extension-sdk",
            ExtensionInfo = new ExtensionInfo { Source = "dotnet-sdk-tests", Name = "advanced-create-extension" },
            Canvases =
            [
                new CanvasDeclaration
                {
                    Id = "advanced-create-canvas",
                    DisplayName = "Advanced Create Canvas",
                    Description = "Covers create-time canvas options.",
                },
            ],
            Providers =
            [
                new NamedProviderConfig
                {
                    Name = "create-provider",
                    Type = "openai",
                    WireApi = "responses",
                    BaseUrl = "https://create-provider.example.test/v1",
                    ApiKey = "create-provider-key",
                    Headers = new Dictionary<string, string> { ["X-Create-Provider"] = "yes" },
                },
            ],
            Models =
            [
                new ProviderModelConfig
                {
                    Provider = "create-provider",
                    Id = "create-model",
                    Name = "Create Model",
                    ModelId = "claude-sonnet-4.5",
                    WireModel = "create-wire-model",
                    MaxContextWindowTokens = 12_000,
                    MaxPromptTokens = 10_000,
                    MaxOutputTokens = 2_000,
                },
            ],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        using var capture = JsonDocument.Parse(await File.ReadAllTextAsync(capturePath));
        var createRequest = GetCapturedRequestParams(capture.RootElement, "session.create");
        Assert.Equal("advanced-create-client", createRequest.GetProperty("clientName").GetString());
        Assert.Equal("claude-sonnet-4.5", createRequest.GetProperty("model").GetString());
        Assert.Equal("medium", createRequest.GetProperty("reasoningEffort").GetString());
        Assert.Equal("detailed", createRequest.GetProperty("reasoningSummary").GetString());
        Assert.Equal("long_context", createRequest.GetProperty("contextTier").GetString());
        Assert.True(createRequest.GetProperty("enableCitations").GetBoolean());
        Assert.False(createRequest.GetProperty("capi").GetProperty("enableWebSocketResponses").GetBoolean());
        Assert.Equal("persistent", createRequest.GetProperty("mcpOAuthTokenStorage").GetString());
        Assert.Equal("agent-one", createRequest.GetProperty("agent").GetString());
        Assert.Equal("edit", createRequest.GetProperty("defaultAgent").GetProperty("excludedTools")[0].GetString());
        Assert.Equal("agent-one", createRequest.GetProperty("customAgents")[0].GetProperty("name").GetString());
        Assert.Equal("plugins-create", createRequest.GetProperty("pluginDirectories")[0].GetString());
        Assert.Equal("disabled-create-skill", createRequest.GetProperty("disabledSkills")[0].GetString());
        Assert.False(createRequest.GetProperty("infiniteSessions").GetProperty("enabled").GetBoolean());
        Assert.True(createRequest.GetProperty("largeOutput").GetProperty("enabled").GetBoolean());
        Assert.Equal(4096, createRequest.GetProperty("largeOutput").GetProperty("maxSizeBytes").GetInt64());
        Assert.Equal(outputDirectory, createRequest.GetProperty("largeOutput").GetProperty("outputDir").GetString());
        Assert.True(createRequest.GetProperty("memory").GetProperty("enabled").GetBoolean());
        Assert.Equal("session-create-token", createRequest.GetProperty("gitHubToken").GetString());
        Assert.Equal("export", createRequest.GetProperty("remoteSession").GetString());
        Assert.Equal("github", createRequest.GetProperty("cloud").GetProperty("repository").GetProperty("owner").GetString());
        Assert.True(createRequest.GetProperty("requestMcpApps").GetBoolean());
        Assert.True(createRequest.GetProperty("requestCanvasRenderer").GetBoolean());
        Assert.True(createRequest.GetProperty("requestExtensions").GetBoolean());
        Assert.Equal("custom-extension-sdk", createRequest.GetProperty("extensionSdkPath").GetString());
        Assert.Equal("advanced-create-extension", createRequest.GetProperty("extensionInfo").GetProperty("name").GetString());
        Assert.Equal("advanced-create-canvas", createRequest.GetProperty("canvases")[0].GetProperty("id").GetString());
        Assert.Equal("create-provider", createRequest.GetProperty("providers")[0].GetProperty("name").GetString());
        Assert.Equal("responses", createRequest.GetProperty("providers")[0].GetProperty("wireApi").GetString());
        Assert.Equal("create-model", createRequest.GetProperty("models")[0].GetProperty("id").GetString());
        Assert.Equal(12000, createRequest.GetProperty("models")[0].GetProperty("maxContextWindowTokens").GetInt32());

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Forward_Singular_Provider_Options_In_Create_Wire_Request()
    {
        var (cliPath, capturePath) = await CreateFakeCliCaptureAsync();

        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(path: cliPath, args: ["--capture-file", capturePath]),
            UseLoggedInUser = false,
        });

        await client.StartAsync();

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            Model = "claude-sonnet-4.5",
            Provider = new ProviderConfig
            {
                Type = "azure",
                WireApi = "responses",
                Transport = "http",
                BaseUrl = "https://azure-provider.example.test/openai",
                ApiKey = "provider-api-key",
                BearerToken = "provider-bearer-token",
                Azure = new AzureOptions { ApiVersion = "2024-02-15-preview" },
                Headers = new Dictionary<string, string> { ["X-Provider-Wire"] = "yes" },
                ModelId = "claude-sonnet-4.5",
                WireModel = "azure-deployment",
                MaxPromptTokens = 8192,
                MaxOutputTokens = 1024,
            },
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        using var capture = JsonDocument.Parse(await File.ReadAllTextAsync(capturePath));
        var provider = GetCapturedRequestParams(capture.RootElement, "session.create").GetProperty("provider");
        Assert.Equal("azure", provider.GetProperty("type").GetString());
        Assert.Equal("responses", provider.GetProperty("wireApi").GetString());
        Assert.Equal("http", provider.GetProperty("transport").GetString());
        Assert.Equal("https://azure-provider.example.test/openai", provider.GetProperty("baseUrl").GetString());
        Assert.Equal("provider-api-key", provider.GetProperty("apiKey").GetString());
        Assert.Equal("provider-bearer-token", provider.GetProperty("bearerToken").GetString());
        Assert.Equal("2024-02-15-preview", provider.GetProperty("azure").GetProperty("apiVersion").GetString());
        Assert.Equal("yes", provider.GetProperty("headers").GetProperty("X-Provider-Wire").GetString());
        Assert.Equal("claude-sonnet-4.5", provider.GetProperty("modelId").GetString());
        Assert.Equal("azure-deployment", provider.GetProperty("wireModel").GetString());
        Assert.Equal(8192, provider.GetProperty("maxPromptTokens").GetInt32());
        Assert.Equal(1024, provider.GetProperty("maxOutputTokens").GetInt32());

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Apply_Empty_Mode_Defaults_To_CreateSession_Wire_Request()
    {
        var (cliPath, capturePath) = await CreateFakeCliCaptureAsync();

        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(path: cliPath, args: ["--capture-file", capturePath]),
            Mode = CopilotClientMode.Empty,
            BaseDirectory = Ctx.WorkDir,
            UseLoggedInUser = false,
        });

        await client.StartAsync();

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            AvailableTools = new ToolSet().AddBuiltIn(BuiltInTools.Isolated),
        });

        using var capture = JsonDocument.Parse(await File.ReadAllTextAsync(capturePath));
        var createRequest = GetCapturedRequestParams(capture.RootElement, "session.create");
        Assert.False(createRequest.GetProperty("enableSessionTelemetry").GetBoolean());
        Assert.True(createRequest.GetProperty("skipEmbeddingRetrieval").GetBoolean());
        Assert.False(createRequest.GetProperty("enableOnDemandInstructionDiscovery").GetBoolean());
        Assert.Equal("in-memory", createRequest.GetProperty("embeddingCacheStorage").GetString());
        Assert.False(createRequest.GetProperty("enableFileHooks").GetBoolean());
        Assert.False(createRequest.GetProperty("enableHostGitOperations").GetBoolean());
        Assert.False(createRequest.GetProperty("enableSessionStore").GetBoolean());
        Assert.False(createRequest.GetProperty("enableSkills").GetBoolean());
        Assert.False(createRequest.TryGetProperty("organizationCustomInstructions", out _));

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Propagate_Activity_TraceContext_To_Session_Create_And_Send()
    {
        var (cliPath, capturePath) = await CreateFakeCliCaptureAsync();

        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(path: cliPath, args: ["--capture-file", capturePath]),
            UseLoggedInUser = false,
        });

        await client.StartAsync();

        using var activity = new Activity("dotnet-sdk-trace-create-send");
        activity.SetIdFormat(ActivityIdFormat.W3C);
        activity.TraceStateString = "vendor=create-send";
        activity.Start();

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var messageId = await session.SendAsync(new MessageOptions
        {
            Prompt = "Trace this message.",
        });

        Assert.Equal("fake-message", messageId);

        using var capture = JsonDocument.Parse(await File.ReadAllTextAsync(capturePath));
        var createRequest = GetCapturedRequestParams(capture.RootElement, "session.create");
        var sendRequest = GetCapturedRequestParams(capture.RootElement, "session.send");

        Assert.Equal(activity.Id, createRequest.GetProperty("traceparent").GetString());
        Assert.Equal("vendor=create-send", createRequest.GetProperty("tracestate").GetString());
        Assert.Equal(activity.Id, sendRequest.GetProperty("traceparent").GetString());
        Assert.Equal("vendor=create-send", sendRequest.GetProperty("tracestate").GetString());

        await session.DisposeAsync();
    }

    [Fact]
    public async Task ForceStop_Does_Not_Rethrow_When_Tcp_Cli_Drops_During_Startup()
    {
        var cliPath = Path.Join(Ctx.WorkDir, $"fake-tcp-drop-cli-{Guid.NewGuid():N}.js");
        await File.WriteAllTextAsync(cliPath, FakeTcpDropDuringStartupCliScript);

        await using var client = Ctx.CreateClient(
            options: new CopilotClientOptions
            {
                Connection = RuntimeConnection.ForTcp(path: cliPath),
                UseLoggedInUser = false,
            });

        var ex = await Assert.ThrowsAsync<IOException>(() => client.StartAsync());
        Assert.Contains("Communication error", ex.Message, StringComparison.Ordinal);

        await client.ForceStopAsync();
    }

    [Fact]
    public async Task StartAsync_Cleans_Up_Tcp_Cli_Process_When_Connect_Fails()
    {
        var cliPath = Path.Join(Ctx.WorkDir, $"fake-tcp-unavailable-port-cli-{Guid.NewGuid():N}.js");
        var pidPath = Path.Join(Ctx.WorkDir, $"fake-tcp-unavailable-port-cli-{Guid.NewGuid():N}.pid");
        var unavailablePort = GetAvailableTcpPort();
        await File.WriteAllTextAsync(cliPath, FakeTcpUnavailablePortCliScript);

        await using var client = Ctx.CreateClient(
            options: new CopilotClientOptions
            {
                Connection = RuntimeConnection.ForTcp(path: cliPath, args: ["--pid-file", pidPath, "--announce-port", unavailablePort.ToString(CultureInfo.InvariantCulture)]),
                UseLoggedInUser = false,
            });

        await Assert.ThrowsAnyAsync<Exception>(() => client.StartAsync());

        var pid = int.Parse(await File.ReadAllTextAsync(pidPath), CultureInfo.InvariantCulture);
        await AssertProcessExitedAsync(pid);

        await client.ForceStopAsync();
    }

    [Fact]
    public async Task Should_Propagate_Activity_TraceContext_To_Session_Resume()
    {
        var (cliPath, capturePath) = await CreateFakeCliCaptureAsync();

        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(path: cliPath, args: ["--capture-file", capturePath]),
            UseLoggedInUser = false,
        });

        await client.StartAsync();

        using var activity = new Activity("dotnet-sdk-trace-resume");
        activity.SetIdFormat(ActivityIdFormat.W3C);
        activity.TraceStateString = "vendor=resume";
        activity.Start();

        var session = await client.ResumeSessionAsync("trace-resume-session", new ResumeSessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        using var capture = JsonDocument.Parse(await File.ReadAllTextAsync(capturePath));
        var resumeRequest = GetCapturedRequestParams(capture.RootElement, "session.resume");

        Assert.Equal(activity.Id, resumeRequest.GetProperty("traceparent").GetString());
        Assert.Equal("vendor=resume", resumeRequest.GetProperty("tracestate").GetString());

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Forward_Granular_Multitenancy_Fields_In_Resume_Wire_Request()
    {
        var (cliPath, capturePath) = await CreateFakeCliCaptureAsync();

        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(path: cliPath, args: ["--capture-file", capturePath]),
            UseLoggedInUser = false,
        });

        await client.StartAsync();

        var session = await client.ResumeSessionAsync("resume-session", new ResumeSessionConfig
        {
            SkipEmbeddingRetrieval = false,
            OrganizationCustomInstructions = "Resume org policy.",
            EnableOnDemandInstructionDiscovery = true,
            EmbeddingCacheStorage = EmbeddingCacheStorageMode.Persistent,
            EnableFileHooks = true,
            EnableHostGitOperations = false,
            EnableSessionStore = true,
            EnableSkills = false,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        using var capture = JsonDocument.Parse(await File.ReadAllTextAsync(capturePath));
        var resumeRequest = GetCapturedRequestParams(capture.RootElement, "session.resume");
        Assert.False(resumeRequest.GetProperty("skipEmbeddingRetrieval").GetBoolean());
        Assert.Equal("Resume org policy.", resumeRequest.GetProperty("organizationCustomInstructions").GetString());
        Assert.True(resumeRequest.GetProperty("enableOnDemandInstructionDiscovery").GetBoolean());
        Assert.Equal("persistent", resumeRequest.GetProperty("embeddingCacheStorage").GetString());
        Assert.True(resumeRequest.GetProperty("enableFileHooks").GetBoolean());
        Assert.False(resumeRequest.GetProperty("enableHostGitOperations").GetBoolean());
        Assert.True(resumeRequest.GetProperty("enableSessionStore").GetBoolean());
        Assert.False(resumeRequest.GetProperty("enableSkills").GetBoolean());

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Forward_Advanced_Session_Options_In_Resume_Wire_Request()
    {
        var (cliPath, capturePath) = await CreateFakeCliCaptureAsync();
        var outputDirectory = Path.Join(Ctx.WorkDir, "large-output-resume");
        using var canvasInput = JsonDocument.Parse("{\"start\":41}");

        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(path: cliPath, args: ["--capture-file", capturePath]),
            UseLoggedInUser = false,
        });

        await client.StartAsync();

        var session = await client.ResumeSessionAsync("advanced-resume-session", new ResumeSessionConfig
        {
            ClientName = "advanced-resume-client",
            Model = "claude-haiku-4.5",
            ReasoningEffort = "low",
            ReasoningSummary = ReasoningSummary.None,
            ContextTier = ContextTier.Default,
            SuppressResumeEvent = true,
            ContinuePendingWork = true,
            McpOAuthTokenStorage = McpOAuthTokenStorageMode.Persistent,
            PluginDirectories = ["plugins-resume"],
            LargeOutput = new LargeToolOutputConfig
            {
                Enabled = false,
                MaxSizeBytes = 2048,
                OutputDirectory = outputDirectory,
            },
            Memory = new MemoryConfiguration { Enabled = false },
            RemoteSession = GitHub.Copilot.Rpc.RemoteSessionMode.On,
            OpenCanvases =
            [
                new GitHub.Copilot.Rpc.OpenCanvasInstance
                {
                    CanvasId = "resume-canvas",
                    ExtensionId = "dotnet-sdk-tests/resume-extension",
                    ExtensionName = "Resume Extension",
                    InstanceId = "resume-canvas-1",
                    Input = canvasInput.RootElement.Clone(),
                    Status = "ready",
                    Title = "Resume Canvas",
                    Url = "https://example.com/resume-canvas",
                },
            ],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        using var capture = JsonDocument.Parse(await File.ReadAllTextAsync(capturePath));
        var resumeRequest = GetCapturedRequestParams(capture.RootElement, "session.resume");
        Assert.Equal("advanced-resume-session", resumeRequest.GetProperty("sessionId").GetString());
        Assert.Equal("advanced-resume-client", resumeRequest.GetProperty("clientName").GetString());
        Assert.Equal("claude-haiku-4.5", resumeRequest.GetProperty("model").GetString());
        Assert.Equal("low", resumeRequest.GetProperty("reasoningEffort").GetString());
        Assert.Equal("none", resumeRequest.GetProperty("reasoningSummary").GetString());
        Assert.Equal("default", resumeRequest.GetProperty("contextTier").GetString());
        Assert.True(resumeRequest.GetProperty("disableResume").GetBoolean());
        Assert.True(resumeRequest.GetProperty("continuePendingWork").GetBoolean());
        Assert.Equal("persistent", resumeRequest.GetProperty("mcpOAuthTokenStorage").GetString());
        Assert.Equal("plugins-resume", resumeRequest.GetProperty("pluginDirectories")[0].GetString());
        Assert.False(resumeRequest.GetProperty("largeOutput").GetProperty("enabled").GetBoolean());
        Assert.Equal(2048, resumeRequest.GetProperty("largeOutput").GetProperty("maxSizeBytes").GetInt64());
        Assert.Equal(outputDirectory, resumeRequest.GetProperty("largeOutput").GetProperty("outputDir").GetString());
        Assert.False(resumeRequest.GetProperty("memory").GetProperty("enabled").GetBoolean());
        Assert.Equal("on", resumeRequest.GetProperty("remoteSession").GetString());

        var openCanvas = resumeRequest.GetProperty("openCanvases")[0];
        Assert.Equal("resume-canvas", openCanvas.GetProperty("canvasId").GetString());
        Assert.Equal("dotnet-sdk-tests/resume-extension", openCanvas.GetProperty("extensionId").GetString());
        Assert.Equal("Resume Extension", openCanvas.GetProperty("extensionName").GetString());
        Assert.Equal("resume-canvas-1", openCanvas.GetProperty("instanceId").GetString());
        Assert.Equal(41, openCanvas.GetProperty("input").GetProperty("start").GetInt32());
        Assert.Equal("ready", openCanvas.GetProperty("status").GetString());
        Assert.Equal("Resume Canvas", openCanvas.GetProperty("title").GetString());
        Assert.Equal("https://example.com/resume-canvas", openCanvas.GetProperty("url").GetString());

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Apply_Empty_Mode_Defaults_To_ResumeSession_Wire_Request()
    {
        var (cliPath, capturePath) = await CreateFakeCliCaptureAsync();

        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(path: cliPath, args: ["--capture-file", capturePath]),
            Mode = CopilotClientMode.Empty,
            BaseDirectory = Ctx.WorkDir,
            UseLoggedInUser = false,
        });

        await client.StartAsync();

        var session = await client.ResumeSessionAsync("resume-empty-session", new ResumeSessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            AvailableTools = new ToolSet().AddBuiltIn(BuiltInTools.Isolated),
        });

        using var capture = JsonDocument.Parse(await File.ReadAllTextAsync(capturePath));
        var resumeRequest = GetCapturedRequestParams(capture.RootElement, "session.resume");
        Assert.False(resumeRequest.GetProperty("enableSessionTelemetry").GetBoolean());
        Assert.True(resumeRequest.GetProperty("skipEmbeddingRetrieval").GetBoolean());
        Assert.False(resumeRequest.GetProperty("enableOnDemandInstructionDiscovery").GetBoolean());
        Assert.Equal("in-memory", resumeRequest.GetProperty("embeddingCacheStorage").GetString());
        Assert.False(resumeRequest.GetProperty("enableFileHooks").GetBoolean());
        Assert.False(resumeRequest.GetProperty("enableHostGitOperations").GetBoolean());
        Assert.False(resumeRequest.GetProperty("enableSessionStore").GetBoolean());
        Assert.False(resumeRequest.GetProperty("enableSkills").GetBoolean());
        Assert.False(resumeRequest.TryGetProperty("organizationCustomInstructions", out _));

        await session.DisposeAsync();
    }

    [Fact]
    public void Should_Accept_GitHubToken_Option()
    {
        var options = new CopilotClientOptions
        {
            GitHubToken = "gho_test_token"
        };

        Assert.Equal("gho_test_token", options.GitHubToken);
    }

    [Fact]
    public void Should_Default_UseLoggedInUser_To_Null()
    {
        var options = new CopilotClientOptions();

        Assert.Null(options.UseLoggedInUser);
    }

    [Fact]
    public void Should_Allow_Explicit_UseLoggedInUser_False()
    {
        var options = new CopilotClientOptions
        {
            UseLoggedInUser = false
        };

        Assert.False(options.UseLoggedInUser);
    }

    [Fact]
    public void Should_Allow_Explicit_UseLoggedInUser_True_With_GitHubToken()
    {
        var options = new CopilotClientOptions
        {
            GitHubToken = "gho_test_token",
            UseLoggedInUser = true
        };

        Assert.True(options.UseLoggedInUser);
    }

    [Fact]
    public void Should_Throw_When_GitHubToken_Used_With_UriConnection()
    {
        Assert.Throws<ArgumentException>(() =>
        {
            _ = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri("localhost:8080"), GitHubToken = "gho_test_token" });
        });
    }

    [Fact]
    public void Should_Throw_When_UseLoggedInUser_Used_With_UriConnection()
    {
        Assert.Throws<ArgumentException>(() =>
        {
            _ = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri("localhost:8080"), UseLoggedInUser = false });
        });
    }

    [Fact]
    public void Should_Default_SessionIdleTimeoutSeconds_To_Null()
    {
        var options = new CopilotClientOptions();

        Assert.Null(options.SessionIdleTimeoutSeconds);
    }

    [Fact]
    public void Should_Accept_SessionIdleTimeoutSeconds_Option()
    {
        var options = new CopilotClientOptions
        {
            SessionIdleTimeoutSeconds = 600
        };

        Assert.Equal(600, options.SessionIdleTimeoutSeconds);
    }

    private static int GetAvailableTcpPort()
    {
        var listener = new TcpListener(IPAddress.Loopback, 0);
        listener.Start();
        try
        {
            return ((IPEndPoint)listener.LocalEndpoint).Port;
        }
        finally
        {
            listener.Stop();
        }
    }

    private static void AssertArgumentValue(string?[] args, string name, string expectedValue)
    {
        var index = Array.IndexOf(args, name);
        Assert.True(index >= 0, $"Expected argument '{name}' was not present. Args: {string.Join(" ", args)}");
        Assert.True(index + 1 < args.Length, $"Expected argument '{name}' to have a value.");
        Assert.Equal(expectedValue, args[index + 1]);
    }

    private async Task<(string CliPath, string CapturePath)> CreateFakeCliCaptureAsync()
    {
        var cliPath = Path.Join(Ctx.WorkDir, $"fake-cli-{Guid.NewGuid():N}.js");
        var capturePath = Path.Join(Ctx.WorkDir, $"fake-cli-capture-{Guid.NewGuid():N}.json");
        await File.WriteAllTextAsync(cliPath, FakeStdioCliScript);
        return (cliPath, capturePath);
    }

    private static JsonElement GetCapturedRequestParams(JsonElement captureRoot, string method)
    {
        return captureRoot
            .GetProperty("requests")
            .EnumerateArray()
            .Single(request => request.GetProperty("method").GetString() == method)
            .GetProperty("params");
    }

    private static async Task AssertProcessExitedAsync(int pid)
    {
        for (var i = 0; i < 50; i++)
        {
            if (!IsProcessRunning(pid))
            {
                return;
            }

            await Task.Delay(100);
        }

        Assert.False(IsProcessRunning(pid), $"Expected process {pid} to have exited.");
    }

    private static bool IsProcessRunning(int pid)
    {
        try
        {
            using var process = Process.GetProcessById(pid);
            return !process.HasExited;
        }
        catch (Exception ex) when (ex is ArgumentException or InvalidOperationException)
        {
            return false;
        }
    }

    private const string FakeTcpUnavailablePortCliScript = """
        const fs = require("fs");

        const pidFileIndex = process.argv.indexOf("--pid-file");
        const portIndex = process.argv.indexOf("--announce-port");

        fs.writeFileSync(process.argv[pidFileIndex + 1], String(process.pid));
        console.log(`listening on port ${process.argv[portIndex + 1]}`);

        setInterval(() => {}, 1000);
        """;

    private const string FakeTcpDropDuringStartupCliScript = """
        const net = require("net");

        const server = net.createServer(socket => {
          socket.on("data", () => {
            socket.destroy();
            server.close(() => process.exit(0));
          });
        });

        server.listen(0, "localhost", () => {
          const address = server.address();
          console.log(`listening on port ${address.port}`);
        });

        setTimeout(() => process.exit(2), 30000).unref();
        """;

    private const string FakeStdioCliScript = """
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
              OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT: process.env.OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT
            }
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
            if (headerEnd < 0) {
              return;
            }

            const header = buffer.subarray(0, headerEnd).toString("utf8");
            const match = /Content-Length:\s*(\d+)/i.exec(header);
            if (!match) {
              throw new Error("Missing Content-Length header");
            }

            const length = Number(match[1]);
            const bodyStart = headerEnd + 4;
            const bodyEnd = bodyStart + length;
            if (buffer.length < bodyEnd) {
              return;
            }

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
            writeResponse(message.id, { message: "pong", protocolVersion: 3 });
            return;
          }

          if (message.method === "session.create") {
            const sessionId = message.params?.sessionId ?? message.params?.[0]?.sessionId ?? "fake-session";
            writeResponse(message.id, { sessionId, workspacePath: null, capabilities: null });
            return;
          }

          if (message.method === "session.resume") {
            const sessionId = message.params?.sessionId ?? message.params?.[0]?.sessionId ?? "fake-session";
            writeResponse(message.id, { sessionId, workspacePath: null, capabilities: null });
            return;
          }

          if (message.method === "session.send") {
            writeResponse(message.id, { messageId: "fake-message" });
            return;
          }

          writeResponse(message.id, {});
        }

        function writeResponse(id, result) {
          const body = JSON.stringify({ jsonrpc: "2.0", id, result });
          process.stdout.write(`Content-Length: ${Buffer.byteLength(body, "utf8")}\r\n\r\n${body}`);
        }
        """;
}
