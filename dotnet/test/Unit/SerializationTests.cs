/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using System.Text.Json;
#if !NET8_0_OR_GREATER
using System.Runtime.Serialization;
#endif
using GitHub.Copilot.Rpc;

namespace GitHub.Copilot.Test.Unit;

/// <summary>
/// Tests for JSON serialization compatibility with the SDK's configured options.
/// </summary>
public class SerializationTests
{
    [Fact]
    public void ProviderConfig_CanSerializeHeaders_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var original = new ProviderConfig
        {
            BaseUrl = "https://example.com/provider",
            Headers = new Dictionary<string, string> { ["Authorization"] = "Bearer provider-token" },
            ModelId = "gpt-4o",
            WireModel = "my-finetune-v3",
            MaxPromptTokens = 100_000,
            MaxOutputTokens = 4096
        };

        var json = JsonSerializer.Serialize(original, options);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        Assert.Equal("https://example.com/provider", root.GetProperty("baseUrl").GetString());
        Assert.Equal("Bearer provider-token", root.GetProperty("headers").GetProperty("Authorization").GetString());
        Assert.Equal("gpt-4o", root.GetProperty("modelId").GetString());
        Assert.Equal("my-finetune-v3", root.GetProperty("wireModel").GetString());
        Assert.Equal(100_000, root.GetProperty("maxPromptTokens").GetInt32());
        Assert.Equal(4096, root.GetProperty("maxOutputTokens").GetInt32());

        var deserialized = JsonSerializer.Deserialize<ProviderConfig>(json, options);
        Assert.NotNull(deserialized);
        Assert.Equal("https://example.com/provider", deserialized.BaseUrl);
        Assert.Equal("Bearer provider-token", deserialized.Headers!["Authorization"]);
        Assert.Equal("gpt-4o", deserialized.ModelId);
        Assert.Equal("my-finetune-v3", deserialized.WireModel);
        Assert.Equal(100_000, deserialized.MaxPromptTokens);
        Assert.Equal(4096, deserialized.MaxOutputTokens);
    }

    [Fact]
    public void CapiSessionOptions_CanSerializeDisableWebSocketResponses_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var original = new CapiSessionOptions
        {
            DisableWebSocketResponses = true
        };

        var json = JsonSerializer.Serialize(original, options);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        Assert.True(root.GetProperty("disableWebSocketResponses").GetBoolean());

        var deserialized = JsonSerializer.Deserialize<CapiSessionOptions>(json, options);
        Assert.NotNull(deserialized);
        Assert.True(deserialized.DisableWebSocketResponses);
    }

    [Fact]
    public void ModelBilling_CanSerializeTokenPrices_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var original = new ModelBilling
        {
            Multiplier = 1.5,
            TokenPrices = new GitHub.Copilot.Rpc.ModelBillingTokenPrices
            {
                InputPrice = 2.0,
                OutputPrice = 8.0,
                CachePrice = 0.5,
                BatchSize = 1_000_000L,
                ContextMax = 128_000L,
                LongContext = new GitHub.Copilot.Rpc.ModelBillingTokenPricesLongContext
                {
                    InputPrice = 4.0,
                    OutputPrice = 16.0,
                    CachePrice = 1.0,
                    ContextMax = 1_000_000L
                }
            }
        };

        var json = JsonSerializer.Serialize(original, options);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        Assert.Equal(1.5, root.GetProperty("multiplier").GetDouble());
        var tokenPrices = root.GetProperty("tokenPrices");
        Assert.Equal(2.0, tokenPrices.GetProperty("inputPrice").GetDouble());
        Assert.Equal(8.0, tokenPrices.GetProperty("outputPrice").GetDouble());
        Assert.Equal(0.5, tokenPrices.GetProperty("cachePrice").GetDouble());
        Assert.Equal(1_000_000L, tokenPrices.GetProperty("batchSize").GetInt64());
        Assert.Equal(128_000L, tokenPrices.GetProperty("contextMax").GetInt64());
        var longContext = tokenPrices.GetProperty("longContext");
        Assert.Equal(4.0, longContext.GetProperty("inputPrice").GetDouble());
        Assert.Equal(1_000_000L, longContext.GetProperty("contextMax").GetInt64());

        var deserialized = JsonSerializer.Deserialize<ModelBilling>(json, options);
        Assert.NotNull(deserialized);
        Assert.Equal(1.5, deserialized.Multiplier);
        Assert.NotNull(deserialized.TokenPrices);
        Assert.Equal(2.0, deserialized.TokenPrices.InputPrice);
        Assert.Equal(1_000_000L, deserialized.TokenPrices.BatchSize);
        Assert.Equal(128_000L, deserialized.TokenPrices.ContextMax);
        Assert.NotNull(deserialized.TokenPrices.LongContext);
        Assert.Equal(16.0, deserialized.TokenPrices.LongContext.OutputPrice);
        Assert.Equal(1_000_000L, deserialized.TokenPrices.LongContext.ContextMax);
    }

    [Fact]
    public void MessageOptions_CanSerializeRequestHeaders_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var original = new MessageOptions
        {
            Prompt = "real prompt",
            Mode = "enqueue",
            RequestHeaders = new Dictionary<string, string> { ["X-Trace"] = "trace-value" }
        };

        var json = JsonSerializer.Serialize(original, options);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        Assert.Equal("real prompt", root.GetProperty("prompt").GetString());
        Assert.Equal("enqueue", root.GetProperty("mode").GetString());
        Assert.Equal("trace-value", root.GetProperty("requestHeaders").GetProperty("X-Trace").GetString());

        var deserialized = JsonSerializer.Deserialize<MessageOptions>(json, options);
        Assert.NotNull(deserialized);
        Assert.Equal("real prompt", deserialized.Prompt);
        Assert.Equal("enqueue", deserialized.Mode);
        Assert.Equal("trace-value", deserialized.RequestHeaders!["X-Trace"]);
    }

    [Fact]
    public void SendMessageRequest_CanSerializeRequestHeaders_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var requestType = GetNestedType(typeof(CopilotSession), "SendMessageRequest");
        var request = CreateInternalRequest(
            requestType,
            ("SessionId", "session-id"),
            ("Prompt", "real prompt"),
            ("Mode", "enqueue"),
            ("RequestHeaders", new Dictionary<string, string> { ["X-Trace"] = "trace-value" }));

        var json = JsonSerializer.Serialize(request, requestType, options);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        Assert.Equal("session-id", root.GetProperty("sessionId").GetString());
        Assert.Equal("real prompt", root.GetProperty("prompt").GetString());
        Assert.Equal("enqueue", root.GetProperty("mode").GetString());
        Assert.Equal("trace-value", root.GetProperty("requestHeaders").GetProperty("X-Trace").GetString());
    }

    [Fact]
    public void CreateSessionRequest_CanSerializeInstructionDirectories_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var requestType = GetNestedType(typeof(CopilotClient), "CreateSessionRequest");
        var request = CreateInternalRequest(
            requestType,
            ("SessionId", "session-id"),
            ("InstructionDirectories", new List<string> { "C:\\extra-instructions", "C:\\more-instructions" }));

        var json = JsonSerializer.Serialize(request, requestType, options);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        Assert.Equal("C:\\extra-instructions", root.GetProperty("instructionDirectories")[0].GetString());
        Assert.Equal("C:\\more-instructions", root.GetProperty("instructionDirectories")[1].GetString());
    }

    [Fact]
    public void CreateSessionRequest_CanSerializeCloudOptions_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var requestType = GetNestedType(typeof(CopilotClient), "CreateSessionRequest");
        var request = CreateInternalRequest(
            requestType,
            ("Cloud", new CloudSessionOptions
            {
                Repository = new CloudSessionRepository
                {
                    Owner = "github",
                    Name = "copilot-sdk",
                    Branch = "main"
                }
            }));

        var json = JsonSerializer.Serialize(request, requestType, options);
        using var document = JsonDocument.Parse(json);
        var repository = document.RootElement.GetProperty("cloud").GetProperty("repository");
        Assert.Equal("github", repository.GetProperty("owner").GetString());
        Assert.Equal("copilot-sdk", repository.GetProperty("name").GetString());
        Assert.Equal("main", repository.GetProperty("branch").GetString());
    }

    [Fact]
    public void CreateSessionRequest_CanSerializeModeRequestFlags_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var requestType = GetNestedType(typeof(CopilotClient), "CreateSessionRequest");
        var request = CreateInternalRequest(
            requestType,
            ("RequestExitPlanMode", true),
            ("RequestAutoModeSwitch", true));

        var json = JsonSerializer.Serialize(request, requestType, options);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        Assert.True(root.GetProperty("requestExitPlanMode").GetBoolean());
        Assert.True(root.GetProperty("requestAutoModeSwitch").GetBoolean());
    }

    [Fact]
    public void ResumeSessionRequest_CanSerializeInstructionDirectories_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var requestType = GetNestedType(typeof(CopilotClient), "ResumeSessionRequest");
        var request = CreateInternalRequest(
            requestType,
            ("SessionId", "session-id"),
            ("InstructionDirectories", new List<string> { "C:\\resume-instructions" }));

        var json = JsonSerializer.Serialize(request, requestType, options);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        Assert.Equal("C:\\resume-instructions", root.GetProperty("instructionDirectories")[0].GetString());
    }

    [Fact]
    public void SessionRequests_CanSerializeCapiOptions_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var capi = new CapiSessionOptions { DisableWebSocketResponses = true };

        var createRequestType = GetNestedType(typeof(CopilotClient), "CreateSessionRequest");
        var createRequest = CreateInternalRequest(
            createRequestType,
            ("SessionId", "session-id"),
            ("Capi", capi));

        var createJson = JsonSerializer.Serialize(createRequest, createRequestType, options);
        using var createDocument = JsonDocument.Parse(createJson);
        Assert.True(createDocument.RootElement.GetProperty("capi").GetProperty("disableWebSocketResponses").GetBoolean());

        var resumeRequestType = GetNestedType(typeof(CopilotClient), "ResumeSessionRequest");
        var resumeRequest = CreateInternalRequest(
            resumeRequestType,
            ("SessionId", "session-id"),
            ("Capi", capi));

        var resumeJson = JsonSerializer.Serialize(resumeRequest, resumeRequestType, options);
        using var resumeDocument = JsonDocument.Parse(resumeJson);
        Assert.True(resumeDocument.RootElement.GetProperty("capi").GetProperty("disableWebSocketResponses").GetBoolean());
    }

    [Fact]
    public void SessionRequests_OmitCapiOptions_WhenUnset()
    {
        var options = GetSerializerOptions();

        var createRequestType = GetNestedType(typeof(CopilotClient), "CreateSessionRequest");
        var createRequest = CreateInternalRequest(
            createRequestType,
            ("SessionId", "session-id"));

        var createJson = JsonSerializer.Serialize(createRequest, createRequestType, options);
        using var createDocument = JsonDocument.Parse(createJson);
        Assert.False(createDocument.RootElement.TryGetProperty("capi", out _));

        var resumeRequestType = GetNestedType(typeof(CopilotClient), "ResumeSessionRequest");
        var resumeRequest = CreateInternalRequest(
            resumeRequestType,
            ("SessionId", "session-id"));

        var resumeJson = JsonSerializer.Serialize(resumeRequest, resumeRequestType, options);
        using var resumeDocument = JsonDocument.Parse(resumeJson);
        Assert.False(resumeDocument.RootElement.TryGetProperty("capi", out _));
    }

    [Fact]
    public void SessionRequests_CanSerializeReasoningSummary_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var createRequestType = GetNestedType(typeof(CopilotClient), "CreateSessionRequest");
        var createRequest = CreateInternalRequest(
            createRequestType,
            ("SessionId", "session-id"),
            ("ReasoningSummary", ReasoningSummary.Detailed));

        var createJson = JsonSerializer.Serialize(createRequest, createRequestType, options);
        using var createDocument = JsonDocument.Parse(createJson);
        Assert.Equal("detailed", createDocument.RootElement.GetProperty("reasoningSummary").GetString());

        var resumeRequestType = GetNestedType(typeof(CopilotClient), "ResumeSessionRequest");
        var resumeRequest = CreateInternalRequest(
            resumeRequestType,
            ("SessionId", "session-id"),
            ("ReasoningSummary", ReasoningSummary.None));

        var resumeJson = JsonSerializer.Serialize(resumeRequest, resumeRequestType, options);
        using var resumeDocument = JsonDocument.Parse(resumeJson);
        Assert.Equal("none", resumeDocument.RootElement.GetProperty("reasoningSummary").GetString());
    }

    [Fact]
    public void SessionRequests_CanSerializeContextTier_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var createRequestType = GetNestedType(typeof(CopilotClient), "CreateSessionRequest");
        var createRequest = CreateInternalRequest(
            createRequestType,
            ("SessionId", "session-id"),
            ("ContextTier", ContextTier.LongContext));

        var createJson = JsonSerializer.Serialize(createRequest, createRequestType, options);
        using var createDocument = JsonDocument.Parse(createJson);
        Assert.Equal("long_context", createDocument.RootElement.GetProperty("contextTier").GetString());

        var resumeRequestType = GetNestedType(typeof(CopilotClient), "ResumeSessionRequest");
        var resumeRequest = CreateInternalRequest(
            resumeRequestType,
            ("SessionId", "session-id"),
            ("ContextTier", ContextTier.Default));

        var resumeJson = JsonSerializer.Serialize(resumeRequest, resumeRequestType, options);
        using var resumeDocument = JsonDocument.Parse(resumeJson);
        Assert.Equal("default", resumeDocument.RootElement.GetProperty("contextTier").GetString());
    }

    [Fact]
    public void SessionRequests_CanSerializePluginDirectoriesAndLargeOutput_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var pluginDirs = new List<string> { "/tmp/plugins/a", "/tmp/plugins/b" };
        var largeOutput = new LargeToolOutputConfig
        {
            Enabled = true,
            MaxSizeBytes = 1024,
            OutputDirectory = "/tmp/large-output",
        };

        var createRequestType = GetNestedType(typeof(CopilotClient), "CreateSessionRequest");
        var createRequest = CreateInternalRequest(
            createRequestType,
            ("SessionId", "session-id"),
            ("PluginDirectories", pluginDirs),
            ("LargeOutput", largeOutput));

        var createJson = JsonSerializer.Serialize(createRequest, createRequestType, options);
        using var createDocument = JsonDocument.Parse(createJson);
        var createRoot = createDocument.RootElement;
        Assert.Equal("/tmp/plugins/a", createRoot.GetProperty("pluginDirectories")[0].GetString());
        Assert.Equal("/tmp/plugins/b", createRoot.GetProperty("pluginDirectories")[1].GetString());
        var createLargeOutput = createRoot.GetProperty("largeOutput");
        Assert.True(createLargeOutput.GetProperty("enabled").GetBoolean());
        Assert.Equal(1024, createLargeOutput.GetProperty("maxSizeBytes").GetInt64());
        Assert.Equal("/tmp/large-output", createLargeOutput.GetProperty("outputDir").GetString());

        var resumeRequestType = GetNestedType(typeof(CopilotClient), "ResumeSessionRequest");
        var resumeRequest = CreateInternalRequest(
            resumeRequestType,
            ("SessionId", "session-id"),
            ("PluginDirectories", pluginDirs),
            ("LargeOutput", largeOutput));

        var resumeJson = JsonSerializer.Serialize(resumeRequest, resumeRequestType, options);
        using var resumeDocument = JsonDocument.Parse(resumeJson);
        var resumeRoot = resumeDocument.RootElement;
        Assert.Equal("/tmp/plugins/a", resumeRoot.GetProperty("pluginDirectories")[0].GetString());
        var resumeLargeOutput = resumeRoot.GetProperty("largeOutput");
        Assert.True(resumeLargeOutput.GetProperty("enabled").GetBoolean());
        Assert.Equal(1024, resumeLargeOutput.GetProperty("maxSizeBytes").GetInt64());
        Assert.Equal("/tmp/large-output", resumeLargeOutput.GetProperty("outputDir").GetString());
    }

    [Fact]
    public void SessionRequests_CanSerializeMemory_WithSdkOptions()
    {
        var options = GetSerializerOptions();

        var createRequestType = GetNestedType(typeof(CopilotClient), "CreateSessionRequest");
        var createRequest = CreateInternalRequest(
            createRequestType,
            ("SessionId", "session-id"),
            ("Memory", new MemoryConfiguration { Enabled = true }));

        var createJson = JsonSerializer.Serialize(createRequest, createRequestType, options);
        using var createDocument = JsonDocument.Parse(createJson);
        var createRoot = createDocument.RootElement;
        Assert.True(createRoot.GetProperty("memory").GetProperty("enabled").GetBoolean());

        var resumeRequestType = GetNestedType(typeof(CopilotClient), "ResumeSessionRequest");
        var resumeRequest = CreateInternalRequest(
            resumeRequestType,
            ("SessionId", "session-id"),
            ("Memory", new MemoryConfiguration { Enabled = false }));

        var resumeJson = JsonSerializer.Serialize(resumeRequest, resumeRequestType, options);
        using var resumeDocument = JsonDocument.Parse(resumeJson);
        var resumeRoot = resumeDocument.RootElement;
        Assert.False(resumeRoot.GetProperty("memory").GetProperty("enabled").GetBoolean());
    }

    [Fact]
    public void SessionRequests_OmitMemory_WhenUnset()
    {
        var options = GetSerializerOptions();

        var createRequestType = GetNestedType(typeof(CopilotClient), "CreateSessionRequest");
        var createRequest = CreateInternalRequest(
            createRequestType,
            ("SessionId", "session-id"));

        var createJson = JsonSerializer.Serialize(createRequest, createRequestType, options);
        using var createDocument = JsonDocument.Parse(createJson);
        Assert.False(createDocument.RootElement.TryGetProperty("memory", out _));

        var resumeRequestType = GetNestedType(typeof(CopilotClient), "ResumeSessionRequest");
        var resumeRequest = CreateInternalRequest(
            resumeRequestType,
            ("SessionId", "session-id"));

        var resumeJson = JsonSerializer.Serialize(resumeRequest, resumeRequestType, options);
        using var resumeDocument = JsonDocument.Parse(resumeJson);
        Assert.False(resumeDocument.RootElement.TryGetProperty("memory", out _));
    }

    [Fact]
    public void CreateSessionRequest_CanSerializeEnableSessionTelemetry_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var requestType = GetNestedType(typeof(CopilotClient), "CreateSessionRequest");
        var request = CreateInternalRequest(
            requestType,
            ("SessionId", "session-id"),
            ("EnableSessionTelemetry", false));

        var json = JsonSerializer.Serialize(request, requestType, options);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        Assert.False(root.GetProperty("enableSessionTelemetry").GetBoolean());
    }

    [Fact]
    public void ResumeSessionRequest_CanSerializeEnableSessionTelemetry_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var requestType = GetNestedType(typeof(CopilotClient), "ResumeSessionRequest");
        var request = CreateInternalRequest(
            requestType,
            ("SessionId", "session-id"),
            ("EnableSessionTelemetry", false));

        var json = JsonSerializer.Serialize(request, requestType, options);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        Assert.False(root.GetProperty("enableSessionTelemetry").GetBoolean());
    }

    [Fact]
    public void CreateSessionRequest_CanSerializeEnableOnDemandInstructionDiscovery_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var requestType = GetNestedType(typeof(CopilotClient), "CreateSessionRequest");

        var requestTrue = CreateInternalRequest(
            requestType,
            ("SessionId", "session-id"),
            ("EnableOnDemandInstructionDiscovery", true));
        var rootTrue = JsonDocument.Parse(JsonSerializer.Serialize(requestTrue, requestType, options)).RootElement;
        Assert.True(rootTrue.GetProperty("enableOnDemandInstructionDiscovery").GetBoolean());

        var requestFalse = CreateInternalRequest(
            requestType,
            ("SessionId", "session-id"),
            ("EnableOnDemandInstructionDiscovery", false));
        var rootFalse = JsonDocument.Parse(JsonSerializer.Serialize(requestFalse, requestType, options)).RootElement;
        Assert.False(rootFalse.GetProperty("enableOnDemandInstructionDiscovery").GetBoolean());

        var requestOmitted = CreateInternalRequest(
            requestType,
            ("SessionId", "session-id"));
        var rootOmitted = JsonDocument.Parse(JsonSerializer.Serialize(requestOmitted, requestType, options)).RootElement;
        Assert.False(rootOmitted.TryGetProperty("enableOnDemandInstructionDiscovery", out _));
    }

    [Fact]
    public void ResumeSessionRequest_CanSerializeEnableOnDemandInstructionDiscovery_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var requestType = GetNestedType(typeof(CopilotClient), "ResumeSessionRequest");

        var requestTrue = CreateInternalRequest(
            requestType,
            ("SessionId", "session-id"),
            ("EnableOnDemandInstructionDiscovery", true));
        var rootTrue = JsonDocument.Parse(JsonSerializer.Serialize(requestTrue, requestType, options)).RootElement;
        Assert.True(rootTrue.GetProperty("enableOnDemandInstructionDiscovery").GetBoolean());

        var requestFalse = CreateInternalRequest(
            requestType,
            ("SessionId", "session-id"),
            ("EnableOnDemandInstructionDiscovery", false));
        var rootFalse = JsonDocument.Parse(JsonSerializer.Serialize(requestFalse, requestType, options)).RootElement;
        Assert.False(rootFalse.GetProperty("enableOnDemandInstructionDiscovery").GetBoolean());

        var requestOmitted = CreateInternalRequest(
            requestType,
            ("SessionId", "session-id"));
        var rootOmitted = JsonDocument.Parse(JsonSerializer.Serialize(requestOmitted, requestType, options)).RootElement;
        Assert.False(rootOmitted.TryGetProperty("enableOnDemandInstructionDiscovery", out _));
    }

    [Fact]
    public void ResumeSessionRequest_CanSerializeOpenCanvases_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var requestType = GetNestedType(typeof(CopilotClient), "ResumeSessionRequest");
        var instances = new List<OpenCanvasInstance>
        {
            new()
            {
                CanvasId = "canvas-id",
                ExtensionId = "ext-id",
                InstanceId = "instance-1",
                Availability = CanvasInstanceAvailability.Ready,
            },
        };
        var request = CreateInternalRequest(
            requestType,
            ("SessionId", "session-id"),
            ("OpenCanvases", instances));

        var json = JsonSerializer.Serialize(request, requestType, options);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        var openCanvases = root.GetProperty("openCanvases");
        Assert.Equal(1, openCanvases.GetArrayLength());
        Assert.Equal("canvas-id", openCanvases[0].GetProperty("canvasId").GetString());
    }

    [Fact]
    public void ResumeSessionRequest_CanSerializeModeRequestFlags_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var requestType = GetNestedType(typeof(CopilotClient), "ResumeSessionRequest");
        var request = CreateInternalRequest(
            requestType,
            ("SessionId", "session-id"),
            ("RequestExitPlanMode", true),
            ("RequestAutoModeSwitch", true));

        var json = JsonSerializer.Serialize(request, requestType, options);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        Assert.True(root.GetProperty("requestExitPlanMode").GetBoolean());
        Assert.True(root.GetProperty("requestAutoModeSwitch").GetBoolean());
    }

    [Fact]
    public void AutoModeSwitchResponse_CanSerialize_WithSdkOptions()
    {
        var options = GetSerializerOptions();

        var json = JsonSerializer.Serialize(AutoModeSwitchResponse.YesAlways, options);

        Assert.Equal("\"yes_always\"", json);
    }

    [Fact]
    public void McpHttpServerConfig_CanSerializeOauthOptions_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        McpServerConfig original = new McpHttpServerConfig
        {
            Url = "https://example.com/mcp",
            Headers = new Dictionary<string, string> { ["Authorization"] = "Bearer token" },
            OauthClientId = "client-id",
            OauthPublicClient = false,
            OauthGrantType = McpHttpServerConfigOauthGrantType.ClientCredentials,
            Tools = ["*"],
            Timeout = 3000
        };

        var json = JsonSerializer.Serialize(original, options);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        Assert.Equal("http", root.GetProperty("type").GetString());
        Assert.Equal("https://example.com/mcp", root.GetProperty("url").GetString());
        Assert.Equal("Bearer token", root.GetProperty("headers").GetProperty("Authorization").GetString());
        Assert.Equal("client-id", root.GetProperty("oauthClientId").GetString());
        Assert.False(root.GetProperty("oauthPublicClient").GetBoolean());
        Assert.Equal("client_credentials", root.GetProperty("oauthGrantType").GetString());
        Assert.Equal("*", root.GetProperty("tools")[0].GetString());
        Assert.Equal(3000, root.GetProperty("timeout").GetInt32());

        var deserialized = JsonSerializer.Deserialize<McpServerConfig>(json, options);
        var httpConfig = Assert.IsType<McpHttpServerConfig>(deserialized);
        Assert.Equal("https://example.com/mcp", httpConfig.Url);
        Assert.Equal("Bearer token", httpConfig.Headers!["Authorization"]);
        Assert.Equal("client-id", httpConfig.OauthClientId);
        Assert.False(httpConfig.OauthPublicClient);
        Assert.Equal(McpHttpServerConfigOauthGrantType.ClientCredentials, httpConfig.OauthGrantType);
        Assert.Equal("*", Assert.Single(httpConfig.Tools!));
        Assert.Equal(3000, httpConfig.Timeout);
    }

    [Fact]
    public void QueuedCommandResult_SerializesHandledAsBoolean_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var original = new QueuedCommandResult
        {
            Handled = true,
            StopProcessingQueue = false
        };

        var json = JsonSerializer.Serialize(original, options);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        Assert.Equal(JsonValueKind.True, root.GetProperty("handled").ValueKind);
        Assert.Equal(JsonValueKind.False, root.GetProperty("stopProcessingQueue").ValueKind);

        var deserialized = JsonSerializer.Deserialize<QueuedCommandResult>("""{"handled":false}""", options);
        Assert.NotNull(deserialized);
        Assert.False(deserialized.Handled);
        Assert.Null(deserialized.StopProcessingQueue);
    }

    [Fact]
    public void PermissionDecision_SerializesBaseDiscriminator_WithSdkOptions()
    {
        var options = GetSerializerOptions();
        var original = PermissionDecision.ApproveOnce();

        var json = JsonSerializer.Serialize<PermissionDecision>(original, options);
        using var document = JsonDocument.Parse(json);

        Assert.Equal("approve-once", document.RootElement.GetProperty("kind").GetString());
    }

    [Fact]
    public void HooksInvokeResponse_SerializesPreMcpToolCallHookOutput_WithMetaToUse()
    {
        var options = GetSerializerOptions();

        // Create the PreMcpToolCallHookOutput with meta
        using var doc = JsonDocument.Parse("""{"injected":"by-hook","source":"test"}""");
        var meta = doc.RootElement.Clone();
        var hookOutput = new PreMcpToolCallHookOutput { MetaToUse = meta };

        // Create the HooksInvokeResponse using reflection (it's internal)
        var responseType = GetNestedType(typeof(CopilotClient), "HooksInvokeResponse");
        var response = CreateInternalRequest(responseType, ("Output", hookOutput));

        // Serialize using the exact same path as SendResultResponseAsync
        var typeInfo = options.GetTypeInfo(response.GetType());
        var json = JsonSerializer.SerializeToElement(response, typeInfo);

        // The JSON should be {"output":{"metaToUse":{"injected":"by-hook","source":"test"}}}
        Assert.True(json.TryGetProperty("output", out var outputProp), $"Expected 'output' property. Got: {json}");
        Assert.True(outputProp.TryGetProperty("metaToUse", out var metaToUseProp), $"Expected 'metaToUse' property. Got: {outputProp}");
        Assert.Equal("by-hook", metaToUseProp.GetProperty("injected").GetString());
        Assert.Equal("test", metaToUseProp.GetProperty("source").GetString());
    }

    [Fact]
    public void HooksInvokeResponse_SerializesPreMcpToolCallHookOutput_WithNullMetaToUse()
    {
        var options = GetSerializerOptions();

        // Create the PreMcpToolCallHookOutput with null meta (remove meta)
        var hookOutput = new PreMcpToolCallHookOutput { MetaToUse = null };

        // Create the HooksInvokeResponse using reflection (it's internal)
        var responseType = GetNestedType(typeof(CopilotClient), "HooksInvokeResponse");
        var response = CreateInternalRequest(responseType, ("Output", hookOutput));

        // Serialize
        var typeInfo = options.GetTypeInfo(response.GetType());
        var json = JsonSerializer.SerializeToElement(response, typeInfo);

        // Should be {"output":{"metaToUse":null}}
        Assert.True(json.TryGetProperty("output", out var outputProp), $"Expected 'output' property. Got: {json}");
        Assert.True(outputProp.TryGetProperty("metaToUse", out var metaToUseProp), $"Expected 'metaToUse' property. Got: {outputProp}");
        Assert.Equal(JsonValueKind.Null, metaToUseProp.ValueKind);
    }

    [Fact]
    public void HooksInvokeResponse_SerializesNullOutput_AsEmptyOrNoOutputProperty()
    {
        var options = GetSerializerOptions();

        // Create the HooksInvokeResponse with null Output (preserve meta)
        var responseType = GetNestedType(typeof(CopilotClient), "HooksInvokeResponse");
        var response = CreateInternalRequest(responseType, ("Output", (object?)null));

        // Serialize
        var typeInfo = options.GetTypeInfo(response.GetType());
        var json = JsonSerializer.SerializeToElement(response, typeInfo);

        // With WhenWritingNull, output property should be omitted when null
        // OR if present, should be null
        if (json.TryGetProperty("output", out var outputProp))
        {
            Assert.Equal(JsonValueKind.Null, outputProp.ValueKind);
        }
        // else: property omitted, which is fine (runtime treats undefined output as no-op)
    }

    private static JsonSerializerOptions GetSerializerOptions()
    {
        var prop = typeof(CopilotClient)
            .GetProperty("SerializerOptionsForMessageFormatter",
                System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Static);

        var options = (JsonSerializerOptions?)prop?.GetValue(null);
        Assert.NotNull(options);
        return options;
    }

    private static Type GetNestedType(Type containingType, string name)
    {
        var type = containingType.GetNestedType(name, System.Reflection.BindingFlags.NonPublic);
        Assert.NotNull(type);
        return type!;
    }

    [Fact]
    public void HooksInvokeResponse_SerializesBoxedJsonElement_AsOutput()
    {
        // This tests the EXACT path used by SerializeHookOutput:
        // PreMcpToolCallHookOutput -> serialize to JsonElement -> box as object? in HooksInvokeResponse.Output
        var options = GetSerializerOptions();

        using var metaDoc = JsonDocument.Parse("""{"injected":"by-hook","source":"test"}""");
        var hookOutput = new PreMcpToolCallHookOutput
        {
            MetaToUse = metaDoc.RootElement.Clone()
        };
        // SerializeHookOutput returns a JsonElement (value type)
        var hookTypeInfo = options.GetTypeInfo(typeof(PreMcpToolCallHookOutput));
        JsonElement serializedOutput = JsonSerializer.SerializeToElement(hookOutput, hookTypeInfo);

        // HooksInvokeResponse stores this as object? (boxed JsonElement)
        var responseType = GetNestedType(typeof(CopilotClient), "HooksInvokeResponse");
        var response = CreateInternalRequest(responseType, ("Output", (object)serializedOutput));

        // Serialize via GetTypeInfo(response.GetType()) — same as SendResultResponseAsync
        var typeInfo = options.GetTypeInfo(response.GetType());
        var json = JsonSerializer.SerializeToElement(response, typeInfo);

        // Expected: {"output":{"metaToUse":{"injected":"by-hook","source":"test"}}}
        Assert.True(json.TryGetProperty("output", out var outputProp), $"Expected 'output'. Got: {json}");
        Assert.True(outputProp.TryGetProperty("metaToUse", out var metaToUseProp), $"Expected 'metaToUse' in output. Got: {outputProp}");
        Assert.Equal("by-hook", metaToUseProp.GetProperty("injected").GetString());
        Assert.Equal("test", metaToUseProp.GetProperty("source").GetString());
    }

    private static object CreateInternalRequest(Type type, params (string Name, object? Value)[] properties)
    {
#if NET8_0_OR_GREATER
        var instance = System.Runtime.CompilerServices.RuntimeHelpers.GetUninitializedObject(type);
#else
        var instance = FormatterServices.GetUninitializedObject(type);
#endif

        foreach (var (name, value) in properties)
        {
            var property = type.GetProperty(name, System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.Public | System.Reflection.BindingFlags.NonPublic);
            Assert.NotNull(property);

            if (property!.SetMethod is not null)
            {
                property.SetValue(instance, value);
                continue;
            }

            var field = type.GetField($"<{name}>k__BackingField", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic);
            Assert.NotNull(field);
            field!.SetValue(instance, value);
        }

        return instance;
    }
}
