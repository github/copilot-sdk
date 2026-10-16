/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
using Microsoft.Extensions.AI;
using System.Text.Json;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class TelemetryExportE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "telemetry", output)
{
    [Fact]
    public async Task Should_Export_File_Telemetry_For_Sdk_Interactions()
    {
        var telemetryPath = Path.Join(Ctx.WorkDir, $"telemetry-{Guid.NewGuid():N}.jsonl");
        const string marker = "copilot-sdk-telemetry-e2e";
        const string sourceName = "dotnet-sdk-telemetry-e2e";
        const string toolName = "echo_telemetry_marker";
        const string prompt = $"Use the {toolName} tool with value '{marker}', then respond with TELEMETRY_E2E_DONE.";

        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(),
            Telemetry = new TelemetryConfig
            {
                FilePath = telemetryPath,
                ExporterType = "file",
                SourceName = sourceName,
                CaptureContent = true,
            },
        });

        var session = await Ctx.CreateSessionAsync(client, new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(EchoTelemetryMarker, toolName, "Echoes a marker string for telemetry validation.")],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var assistantMessage = await TestHelper.SendAndGetFinalAssistantMessageAsync(session, new MessageOptions { Prompt = prompt });
        Assert.NotNull(assistantMessage);
        Assert.Contains("TELEMETRY_E2E_DONE", assistantMessage!.Data.Content ?? string.Empty, StringComparison.Ordinal);

        await session.DisposeAsync();
        await client.StopAsync();

        var entries = await ReadTelemetryEntriesAsync(telemetryPath);
        var spans = entries.Where(entry => GetTypeName(entry) == "span").ToList();

        Assert.NotEmpty(spans);
        Assert.All(spans, span => Assert.Equal(sourceName, GetInstrumentationScopeName(span)));

        Assert.All(spans, span => Assert.NotEqual(2, GetStatusCode(span)));

        var invokeAgentSpan = AssertSpanWithOperation(spans, "invoke_agent");
        Assert.Equal(session.SessionId, GetStringAttribute(invokeAgentSpan, "gen_ai.conversation.id"));
        Assert.True(IsRootSpan(invokeAgentSpan),
            "invoke_agent should be the root of the SDK turn trace.");
        var invokeAgentSpanId = GetSpanId(invokeAgentSpan);
        Assert.False(string.IsNullOrEmpty(invokeAgentSpanId));
        var invokeAgentTraceId = GetTraceId(invokeAgentSpan);
        Assert.False(string.IsNullOrEmpty(invokeAgentTraceId));

        var chatSpans = spans.Where(span => IsSpanWithOperation(span, "chat")).ToList();
        Assert.NotEmpty(chatSpans);
        Assert.All(chatSpans, chat => Assert.Equal(invokeAgentSpanId, GetParentSpanId(chat)));
        Assert.All(chatSpans, chat => Assert.Equal(invokeAgentTraceId, GetTraceId(chat)));
        Assert.Contains(
            chatSpans,
            span => (GetStringAttribute(span, "gen_ai.input.messages") ?? string.Empty).Contains(prompt, StringComparison.Ordinal));
        Assert.Contains(
            chatSpans,
            span => (GetStringAttribute(span, "gen_ai.output.messages") ?? string.Empty).Contains("TELEMETRY_E2E_DONE", StringComparison.Ordinal));

        var toolSpan = AssertSpanWithOperation(spans, "execute_tool");
        Assert.Equal(invokeAgentSpanId, GetParentSpanId(toolSpan));
        Assert.Equal(invokeAgentTraceId, GetTraceId(toolSpan));
        Assert.Equal(toolName, GetStringAttribute(toolSpan, "gen_ai.tool.name"));
        Assert.False(string.IsNullOrWhiteSpace(GetStringAttribute(toolSpan, "gen_ai.tool.call.id")),
            "execute_tool span should carry gen_ai.tool.call.id.");
        Assert.Equal($"{{\"value\":\"{marker}\"}}", GetStringAttribute(toolSpan, "gen_ai.tool.call.arguments"));
        Assert.Equal(marker, GetStringAttribute(toolSpan, "gen_ai.tool.call.result"));

        static string EchoTelemetryMarker(string value) => value;
    }

    [Fact]
    public async Task Should_Export_Per_Request_Subagent_Chat_Spans()
    {
        var telemetryPath = Path.Join(Ctx.WorkDir, $"telemetry-{Guid.NewGuid():N}.jsonl");
        const string sourceName = "dotnet-sdk-subagent-telemetry-e2e";
        const string prompt = "Use the task tool in sync mode to ask a task agent to read subagent-otel.txt with the view tool. Then reply with SUBAGENT_OTEL_DONE.";

        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(),
            Telemetry = new TelemetryConfig
            {
                FilePath = telemetryPath,
                ExporterType = "file",
                SourceName = sourceName,
                CaptureContent = true,
            },
        });

        await File.WriteAllTextAsync(Path.Join(Ctx.WorkDir, "subagent-otel.txt"), "SUBAGENT_OTEL_FILE_CONTENT");
        var session = await Ctx.CreateSessionAsync(client, new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var response = await TestHelper.SendAndGetFinalAssistantMessageAsync(session, new MessageOptions { Prompt = prompt });
        Assert.Contains("SUBAGENT_OTEL_DONE", response?.Data.Content ?? string.Empty, StringComparison.Ordinal);

        await session.DisposeAsync();
        await client.StopAsync();

        var spans = (await ReadTelemetryEntriesAsync(telemetryPath))
            .Where(entry => GetTypeName(entry) == "span")
            .ToList();
        Assert.All(spans, span => Assert.Equal(sourceName, GetInstrumentationScopeName(span)));
        Assert.All(spans, span => Assert.NotEqual(2, GetStatusCode(span)));

        var root = Assert.Single(spans, span => IsSpanWithOperation(span, "invoke_agent") && IsRootSpan(span));
        Assert.Equal(2, spans.Count(span => IsSpanWithOperation(span, "invoke_agent")));
        Assert.Equal(session.SessionId, GetStringAttribute(root, "gen_ai.conversation.id"));
        var rootId = GetSpanId(root);
        var traceId = GetTraceId(root);
        Assert.False(string.IsNullOrEmpty(traceId));
        var taskTool = Assert.Single(spans, span =>
            IsSpanWithOperation(span, "execute_tool") && GetStringAttribute(span, "gen_ai.tool.name") == "task");
        Assert.Equal(rootId, GetParentSpanId(taskTool));
        var subagent = Assert.Single(spans, span =>
            IsSpanWithOperation(span, "invoke_agent") && GetParentSpanId(span) == GetSpanId(taskTool));
        Assert.Equal(traceId, GetTraceId(taskTool));
        Assert.Equal(traceId, GetTraceId(subagent));

        Assert.Equal(4, spans.Count(span => IsSpanWithOperation(span, "chat")));
        var rootChats = spans.Where(span =>
            IsSpanWithOperation(span, "chat") && GetParentSpanId(span) == rootId).ToList();
        Assert.Equal(2, rootChats.Count);
        Assert.All(rootChats, chat => Assert.Equal(traceId, GetTraceId(chat)));
        var subagentChats = spans.Where(span =>
            IsSpanWithOperation(span, "chat") && GetParentSpanId(span) == GetSpanId(subagent)).ToList();
        Assert.True(subagentChats.Count == 2,
            $"Expected two subagent chats; got {subagentChats.Count}. Span hierarchy: {string.Join("; ", spans.Where(span => GetStringAttribute(span, "gen_ai.operation.name") is "chat" or "invoke_agent" or "execute_tool").Select(span => $"{GetStringAttribute(span, "gen_ai.operation.name")}:{GetSpanId(span)} parent={GetParentSpanId(span)}"))}");
        Assert.All(subagentChats, chat =>
        {
            Assert.Equal(traceId, GetTraceId(chat));
            Assert.Equal("sub-agent", GetStringAttribute(chat, "github.copilot.initiator"));
        });
        var requestingChat = Assert.Single(subagentChats, chat =>
            (GetStringAttribute(chat, "gen_ai.output.messages") ?? string.Empty).Contains("\"view\"", StringComparison.Ordinal));
        Assert.DoesNotContain("SUBAGENT_OTEL_FILE_CONTENT",
            GetStringAttribute(requestingChat, "gen_ai.input.messages") ?? string.Empty, StringComparison.Ordinal);
        var finalChat = Assert.Single(subagentChats, chat =>
            (GetStringAttribute(chat, "gen_ai.output.messages") ?? string.Empty).Contains("SUBAGENT_OTEL_CHILD_DONE", StringComparison.Ordinal));
        Assert.Contains("SUBAGENT_OTEL_FILE_CONTENT",
            GetStringAttribute(finalChat, "gen_ai.input.messages") ?? string.Empty, StringComparison.Ordinal);
    }

    private static async Task<IReadOnlyList<JsonElement>> ReadTelemetryEntriesAsync(string path)
    {
        var entries = new List<JsonElement>();
        using var stream = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.Read);
        using var reader = new StreamReader(stream);
        while (await reader.ReadLineAsync() is { } line)
        {
            if (string.IsNullOrWhiteSpace(line))
            {
                continue;
            }

            using var document = JsonDocument.Parse(line);
            entries.Add(document.RootElement.Clone());
        }

        return entries;
    }

    private static string? GetTraceId(JsonElement entry) => GetStringProperty(entry, "traceId");

    private static string? GetSpanId(JsonElement entry) => GetStringProperty(entry, "spanId");

    private static string? GetParentSpanId(JsonElement entry) => GetStringProperty(entry, "parentSpanId");

    private static bool IsRootSpan(JsonElement entry)
    {
        // OTel exporters represent "no parent" inconsistently: the property may be missing,
        // an empty string, or an all-zeros span id. Accept any of the three.
        var parent = GetParentSpanId(entry);
        return string.IsNullOrEmpty(parent) || parent == "0000000000000000";
    }

    private static int GetStatusCode(JsonElement entry)
    {
        return entry.TryGetProperty("status", out var status) && status.TryGetProperty("code", out var code) && code.ValueKind == JsonValueKind.Number
            ? code.GetInt32()
            : 0;
    }

    private static JsonElement AssertSpanWithOperation(IEnumerable<JsonElement> spans, string operationName)
    {
        var matchingSpan = spans.FirstOrDefault(span => GetStringAttribute(span, "gen_ai.operation.name") == operationName);
        Assert.True(matchingSpan.ValueKind != JsonValueKind.Undefined,
            $"Missing {operationName} span. Exported operations: {string.Join(", ", spans.Select(span => GetStringAttribute(span, "gen_ai.operation.name") ?? "<unnamed>"))}");
        return matchingSpan;
    }

    private static bool IsSpanWithOperation(JsonElement span, string operationName)
    {
        return GetStringAttribute(span, "gen_ai.operation.name") == operationName;
    }

    private static string? GetTypeName(JsonElement entry) => GetStringProperty(entry, "type");

    private static string? GetInstrumentationScopeName(JsonElement entry)
    {
        return entry.TryGetProperty("instrumentationScope", out var scope)
            ? GetStringProperty(scope, "name")
            : null;
    }

    private static string? GetStringAttribute(JsonElement entry, string name)
    {
        if (!entry.TryGetProperty("attributes", out var attributes) ||
            !attributes.TryGetProperty(name, out var value))
        {
            return null;
        }

        return GetStringValue(value);
    }

    private static string? GetStringProperty(JsonElement entry, string name)
    {
        return entry.TryGetProperty(name, out var value) ? GetStringValue(value) : null;
    }

    private static string? GetStringValue(JsonElement value)
    {
        return value.ValueKind switch
        {
            JsonValueKind.String => value.GetString(),
            JsonValueKind.Number or JsonValueKind.True or JsonValueKind.False or JsonValueKind.Array or JsonValueKind.Object => value.GetRawText(),
            _ => null,
        };
    }
}
