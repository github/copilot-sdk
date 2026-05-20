#:project ../src/GitHub.Copilot.SDK.csproj

using System.ComponentModel;
using GitHub.Copilot.SDK;
using GitHub.Copilot.SDK.Rpc;
using Microsoft.Extensions.AI;

var tool = ManualToolDeclaration();

// 1. Create a session with a declaration-only tool, then stop after the permission prompt.
await using CopilotClient client1 = new();
await using var session1 = await client1.CreateSessionAsync(new() { Tools = [tool] });

// Subscribe before sending so the permission event cannot be missed.
var permissionRequested = WaitForEventAsync<PermissionRequestedEvent>(session1);
await session1.SendAsync(new MessageOptions
{
    Prompt = "Use the manual_resume_status tool with id 'alpha', then tell me the status.",
});

var permissionEvent = await permissionRequested;
await client1.ForceStopAsync();

await PauseAsync();

// 2. Resume pending work and grant permission to invoke the tool.
await using CopilotClient client2 = new();
await using var session2 = await client2.ResumeSessionAsync(session1.SessionId, new()
{
    Tools = [tool],
    ContinuePendingWork = true,
});

// Subscribe before approving so the external tool request cannot be missed.
var toolRequested = WaitForEventAsync<ExternalToolRequestedEvent>(
    session2,
    evt => evt.Data.ToolName == "manual_resume_status");

await session2.Rpc.Permissions.HandlePendingPermissionRequestAsync(
    permissionEvent.Data.RequestId,
    new PermissionDecisionApproveOnce());

var toolEvent = await toolRequested;
await client2.ForceStopAsync();

await PauseAsync();

// 3. Resume again and manually provide the pending tool result.
await using var client3 = new CopilotClient();
await using var session3 = await client3.ResumeSessionAsync(session1.SessionId, new ResumeSessionConfig
{
    Tools = [tool],
    ContinuePendingWork = true,
});

var assistantMessage = WaitForEventAsync<AssistantMessageEvent>(session3);
await session3.Rpc.Tools.HandlePendingToolCallAsync(
    toolEvent.Data.RequestId,
    result: "MANUAL_STATUS_READY");

var answer = await assistantMessage;
Console.WriteLine(answer.Data.Content);

static Task PauseAsync()
{
    Console.WriteLine("Simulating time passing...\n");
    return Task.Delay(TimeSpan.FromSeconds(1));
}

static AIFunctionDeclaration ManualToolDeclaration() =>
    AIFunctionFactory.Create(
        ([Description("Identifier to look up")] string id) => $"not used: {id}",
        "manual_resume_status",
        "Looks up a status value. The SDK consumer supplies the result manually.")
    // Remove the invocable callback so the SDK leaves tool execution pending.
    .AsDeclarationOnly();

static async Task<T> WaitForEventAsync<T>(CopilotSession session, Func<T, bool>? predicate = null)
    where T : SessionEvent
{
    var tcs = new TaskCompletionSource<T>(TaskCreationOptions.RunContinuationsAsynchronously);
    IDisposable? subscription = null;
    subscription = session.On<SessionEvent>(evt =>
    {
        if (evt is T typed && (predicate?.Invoke(typed) ?? true))
        {
            subscription?.Dispose();
            tcs.TrySetResult(typed);
        }
    });
    return await tcs.Task.WaitAsync(TimeSpan.FromMinutes(2));
}
