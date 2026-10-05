/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Collections.Concurrent;
using System.Net.Http;
using System.Text.Json;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

#pragma warning disable GHCP001 // The LLM inference surface is intentionally experimental.

public class SubagentHooksE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "subagent_hooks", output)
{
    private const string ChildContext = "Subagent start hook verified: read the requested file.";
    private const string StopResponsePrefix = "Subagent stop hook verified: ";

    [Fact]
    public async Task Should_Apply_Subagent_Lifecycle_Hook_Outputs()
    {
        var hookLog = new ConcurrentBag<(string Kind, string ToolName, string SessionId)>();
        var lifecycle = new ConcurrentQueue<string>();
        var starts = new ConcurrentQueue<(SubagentStartHookInput Input, string InvocationSessionId)>();
        var stops = new ConcurrentQueue<(SubagentStopHookInput Input, string InvocationSessionId)>();
        var requestHandler = new RecordingForwardingRequestHandler();
        const string waitingText = "I've launched an explore agent to read subagent-test.txt. Waiting for it to complete...";
        const string finalText = "The explore agent successfully read the file. The contents of **subagent-test.txt** are:\n\n```\nHello from subagent test!\n```";
        var parentSessionId = Guid.NewGuid().ToString();
        var parentWaiting = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);

        // Create a client with the session-based subagents feature flag
        var env = new Dictionary<string, string>(Ctx.GetEnvironment());
        env["COPILOT_EXP_COPILOT_CLI_SESSION_BASED_SUBAGENTS"] = "true";
        var client = Ctx.CreateClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(),
            RequestHandler = requestHandler
        }, environment: env);

        var session = await Ctx.CreateSessionAsync(client, new SessionConfig
        {
            SessionId = parentSessionId,
            OnPermissionRequest = PermissionHandler.ApproveAll,
            Hooks = new SessionHooks
            {
                OnPreToolUse = (input, invocation) =>
                {
                    hookLog.Add(("pre", input.ToolName, input.SessionId));
                    return Task.FromResult<PreToolUseHookOutput?>(new PreToolUseHookOutput
                    {
                        PermissionDecision = "allow"
                    });
                },
                OnPostToolUse = async (input, invocation) =>
                {
                    hookLog.Add(("post", input.ToolName, input.SessionId));
                    // A fast child can inject its result before the fixture's waiting reply is requested.
                    if (input.ToolName == "view" && input.SessionId != parentSessionId)
                    {
                        await parentWaiting.Task;
                    }
                    return null;
                },
                OnSubagentStart = (input, invocation) =>
                {
                    starts.Enqueue((input, invocation.SessionId));
                    lifecycle.Enqueue("start");
                    return Task.FromResult<SubagentStartHookOutput?>(new SubagentStartHookOutput
                    {
                        AdditionalContext = ChildContext
                    });
                },
                OnSubagentStop = (input, invocation) =>
                {
                    stops.Enqueue((input, invocation.SessionId));
                    lifecycle.Enqueue("stop");
                    return Task.FromResult<SubagentStopHookOutput?>(new SubagentStopHookOutput
                    {
                        ModifiedResponse = StopResponsePrefix + input.Response
                    });
                },
            },
        });

        // Create a file for the sub-agent to read
        await File.WriteAllTextAsync(Path.Join(Ctx.WorkDir, "subagent-test.txt"), "Hello from subagent test!");

        using var subscription = session.On<AssistantMessageEvent>(message =>
        {
            if (string.IsNullOrEmpty(message.AgentId) && message.Data.Content == waitingText)
            {
                parentWaiting.TrySetResult(true);
            }
        });
        try
        {
            var response = await session.SendAndWaitAsync(
                new MessageOptions
                {
                    Prompt = "Use the task tool to spawn an explore agent that reads the file "
                        + "subagent-test.txt in the current directory and reports its contents. "
                        + "You must use the task tool."
                },
                timeout: TimeSpan.FromSeconds(120));
            Assert.NotNull(response);
            Assert.True(string.IsNullOrEmpty(response.AgentId));
            Assert.Equal(finalText, response.Data.Content);
            var replies = (await session.GetEventsAsync()).OfType<AssistantMessageEvent>()
                .Where(message => string.IsNullOrEmpty(message.AgentId))
                .Select(message => message.Data.Content)
                .Where(content => content == waitingText || content == finalText);
            Assert.Equal([waitingText, finalText], replies);
        }
        finally
        {
            parentWaiting.TrySetResult(true);
        }

        Assert.Collection(lifecycle,
            kind => Assert.Equal("start", kind),
            kind => Assert.Equal("stop", kind));

        var start = Assert.Single(starts);
        Assert.Equal(session.SessionId, start.InvocationSessionId);
        Assert.Equal(session.SessionId, start.Input.SessionId);
        Assert.True(start.Input.Timestamp > DateTimeOffset.UnixEpoch);
        Assert.Equal(Path.GetFullPath(Ctx.WorkDir), Path.GetFullPath(start.Input.WorkingDirectory));
        Assert.Equal("explore", start.Input.AgentName);
        Assert.Null(start.Input.AgentDisplayName);
        Assert.Null(start.Input.AgentDescription);

        var stop = Assert.Single(stops);
        Assert.Equal(session.SessionId, stop.InvocationSessionId);
        Assert.Equal(session.SessionId, stop.Input.SessionId);
        Assert.True(stop.Input.Timestamp > DateTimeOffset.UnixEpoch);
        Assert.True(stop.Input.Timestamp >= start.Input.Timestamp);
        Assert.Equal(Path.GetFullPath(Ctx.WorkDir), Path.GetFullPath(stop.Input.WorkingDirectory));
        Assert.Equal(start.Input.TranscriptPath, stop.Input.TranscriptPath);
        Assert.Equal(start.Input.AgentName, stop.Input.AgentName);
        Assert.Equal(start.Input.AgentDisplayName, stop.Input.AgentDisplayName);
        Assert.Equal(start.Input.AgentDescription, stop.Input.AgentDescription);
        Assert.False(string.IsNullOrEmpty(stop.Input.AgentId));
        Assert.Equal("explore", stop.Input.AgentType);
        Assert.Equal("end_turn", stop.Input.StopReason);
        Assert.Contains("Hello from subagent test!", stop.Input.Response);

        var log = hookLog.ToArray();

        // Parent tool hooks fire for "task"
        var taskPre = log.Where(h => h.Kind == "pre" && h.ToolName == "task").ToArray();
        Assert.True(taskPre.Length >= 1, "preToolUse should fire for the parent's 'task' tool call");

        // Sub-agent tool hooks fire for "view"
        var viewPre = log.Where(h => h.Kind == "pre" && h.ToolName == "view").ToArray();
        var viewPost = log.Where(h => h.Kind == "post" && h.ToolName == "view").ToArray();
        Assert.True(viewPre.Length > 0, "preToolUse should fire for the sub-agent's 'view' tool call");
        Assert.True(viewPost.Length > 0, "postToolUse should fire for the sub-agent's 'view' tool call");

        // input.SessionId distinguishes parent from sub-agent
        Assert.NotEqual(viewPre[0].SessionId, taskPre[0].SessionId);
        AssertSubagentRequestMetadata(requestHandler.InferenceRequests);
        Assert.Contains(requestHandler.InferenceRequests, r =>
        {
            if (string.IsNullOrEmpty(r.ParentAgentId)) return false;
            using var body = JsonDocument.Parse(r.Body);
            if (!body.RootElement.TryGetProperty("messages", out var messages)
                && !body.RootElement.TryGetProperty("input", out messages))
                return false;
            return messages.ValueKind == JsonValueKind.Array && messages.EnumerateArray().Any(message =>
                message.ValueKind == JsonValueKind.Object
                && message.TryGetProperty("role", out var role)
                && role.ValueKind == JsonValueKind.String
                && role.GetString() == "user"
                && message.TryGetProperty("content", out var content)
                && (content.ValueKind == JsonValueKind.String
                    ? content.GetString()!.Contains(ChildContext + "\n\nRead the file \"subagent-test.txt\"", StringComparison.Ordinal)
                    : content.ValueKind == JsonValueKind.Array && content.EnumerateArray().Any(part =>
                        part.ValueKind == JsonValueKind.Object
                        && part.TryGetProperty("text", out var text)
                        && text.ValueKind == JsonValueKind.String
                        && text.GetString()!.Contains(ChildContext + "\n\nRead the file \"subagent-test.txt\"", StringComparison.Ordinal))));
        });
        Assert.Contains(requestHandler.InferenceRequests, r =>
            string.IsNullOrEmpty(r.ParentAgentId)
            && r.Body.Contains(StopResponsePrefix, StringComparison.Ordinal));
    }

    private static void AssertSubagentRequestMetadata(IReadOnlyCollection<RequestRecord> records)
    {
        Assert.NotEmpty(records);
        var subagentRequest = records.FirstOrDefault(r => !string.IsNullOrEmpty(r.ParentAgentId));
        Assert.NotNull(subagentRequest);
        Assert.False(string.IsNullOrEmpty(subagentRequest.AgentId),
            "Sub-agent inference request should carry an agent id");
        Assert.False(string.IsNullOrEmpty(subagentRequest.InteractionType),
            "Sub-agent inference request should carry an interaction type");
        Assert.NotEqual(subagentRequest.ParentAgentId, subagentRequest.AgentId);
    }

    private sealed class RecordingForwardingRequestHandler : CopilotRequestHandler
    {
        private readonly ConcurrentBag<RequestRecord> _records = [];

        public IReadOnlyCollection<RequestRecord> InferenceRequests =>
            [.. _records.Where(r => RecordingRequestHandler.IsInferenceUrl(r.Url))];

        protected override async Task<HttpResponseMessage> SendRequestAsync(HttpRequestMessage request, CopilotRequestContext ctx)
        {
            var body = RecordingRequestHandler.IsInferenceUrl(request.RequestUri!.ToString())
                ? await request.Content!.ReadAsStringAsync()
                : string.Empty;
            _records.Add(new RequestRecord(
                request.RequestUri!.ToString(),
                ctx.AgentId,
                ctx.ParentAgentId,
                ctx.InteractionType,
                body));
            return await base.SendRequestAsync(request, ctx);
        }
    }

    private sealed record RequestRecord(
        string Url,
        string? AgentId,
        string? ParentAgentId,
        string? InteractionType,
        string Body);
}
