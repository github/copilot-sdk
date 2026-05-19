/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Microsoft.Extensions.AI;
using System.Text.Json;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

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
            Telemetry = new TelemetryConfig
            {
                FilePath = telemetryPath,
                ExporterType = "file",
                SourceName = sourceName,
                CaptureContent = true,
            },
        });

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(EchoTelemetryMarker, toolName, "Echoes a marker string for telemetry validation.")],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        await session.SendAsync(new MessageOptions { Prompt = prompt });
        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.NotNull(assistantMessage);
        Assert.Contains("TELEMETRY_E2E_DONE", assistantMessage!.Data.Content ?? string.Empty, StringComparison.Ordinal);

        await session.DisposeAsync();
        await client.StopAsync();

        var entries = await ReadTelemetryEntriesAsync(
            telemetryPath,
            entries => entries.Any(entry => GetTypeName(entry) == "span" &&
                GetStringAttribute(entry, "gen_ai.operation.name") == "invoke_agent"));
        var spans = entries.Where(entry => GetTypeName(entry) == "span").ToList();

        Assert.NotEmpty(spans);
        Assert.All(spans, span => Assert.Equal(sourceName, GetInstrumentationScopeName(span)));

        // All spans for one SDK turn must share the same trace id and must not be in error state.
        var traceIds = spans.Select(GetTraceId).Where(id => !string.IsNullOrEmpty(id)).Distinct().ToList();
        Assert.Single(traceIds);
        Assert.All(spans, span => Assert.NotEqual(2, GetStatusCode(span)));

        var invokeAgentSpan = AssertSpanWithOperation(spans, "invoke_agent");
        Assert.Equal(session.SessionId, GetStringAttribute(invokeAgentSpan, "gen_ai.conversation.id"));
        Assert.True(IsRootSpan(invokeAgentSpan),
            "invoke_agent should be the root of the SDK turn trace.");
        var invokeAgentSpanId = GetSpanId(invokeAgentSpan);
        Assert.False(string.IsNullOrEmpty(invokeAgentSpanId));

        var chatSpans = spans.Where(span => IsSpanWithOperation(span, "chat")).ToList();
        Assert.NotEmpty(chatSpans);
        Assert.All(chatSpans, chat => Assert.Equal(invokeAgentSpanId, GetParentSpanId(chat)));
        Assert.Contains(
            chatSpans,
            span => (GetStringAttribute(span, "gen_ai.input.messages") ?? string.Empty).Contains(prompt, StringComparison.Ordinal));
        Assert.Contains(
            chatSpans,
            span => (GetStringAttribute(span, "gen_ai.output.messages") ?? string.Empty).Contains("TELEMETRY_E2E_DONE", StringComparison.Ordinal));

        var toolSpan = AssertSpanWithOperation(spans, "execute_tool");
        Assert.Equal(invokeAgentSpanId, GetParentSpanId(toolSpan));
        Assert.Equal(toolName, GetStringAttribute(toolSpan, "gen_ai.tool.name"));
        Assert.False(string.IsNullOrWhiteSpace(GetStringAttribute(toolSpan, "gen_ai.tool.call.id")),
            "execute_tool span should carry gen_ai.tool.call.id.");
        Assert.Equal($"{{\"value\":\"{marker}\"}}", GetStringAttribute(toolSpan, "gen_ai.tool.call.arguments"));
        Assert.Equal(marker, GetStringAttribute(toolSpan, "gen_ai.tool.call.result"));

        static string EchoTelemetryMarker(string value) => value;
    }

    private static async Task<IReadOnlyList<JsonElement>> ReadTelemetryEntriesAsync(
        string path,
        Func<IReadOnlyList<JsonElement>, bool> isComplete)
    {
        IReadOnlyList<JsonElement> entries = [];
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                entries = await ReadTelemetryEntriesOnceAsync(path);
                return entries.Count > 0 && isComplete(entries);
            },
            timeout: TimeSpan.FromSeconds(30),
            timeoutMessage: $"Timed out waiting for telemetry records in '{path}'.",
            transientExceptionFilter: exception => TestHelper.IsTransientFileSystemException(exception) || exception is JsonException);

        return entries;

        static async Task<IReadOnlyList<JsonElement>> ReadTelemetryEntriesOnceAsync(string path)
        {
            if (!File.Exists(path) || new FileInfo(path).Length == 0)
            {
                return [];
            }

            var entries = new List<JsonElement>();
            using var stream = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.ReadWrite | FileShare.Delete);
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
        Assert.NotEqual(JsonValueKind.Undefined, matchingSpan.ValueKind);
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
