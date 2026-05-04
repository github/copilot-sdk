/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Rpc;
using GitHub.Copilot.SDK.Test.Harness;
using Microsoft.Extensions.AI;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

public class RpcShellAndFleetE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_shell_and_fleet", output)
{
    [Fact]
    public async Task Should_Execute_Shell_Command()
    {
        var session = await CreateSessionAsync();
        var markerPath = Path.Join(Ctx.WorkDir, $"shell-rpc-{Guid.NewGuid():N}.txt");
        const string marker = "copilot-sdk-shell-rpc";

        var result = await session.Rpc.Shell.ExecAsync(CreateWriteFileCommand(markerPath, marker), cwd: Ctx.WorkDir);

        Assert.False(string.IsNullOrWhiteSpace(result.ProcessId));
        await WaitForFileTextAsync(markerPath, marker);
    }

    [Fact]
    public async Task Should_Kill_Shell_Process()
    {
        var session = await CreateSessionAsync();
        var command = OperatingSystem.IsWindows()
            ? "powershell -NoLogo -NoProfile -Command \"Start-Sleep -Seconds 30\""
            : "sleep 30";

        var execResult = await session.Rpc.Shell.ExecAsync(command);
        Assert.False(string.IsNullOrWhiteSpace(execResult.ProcessId));

        var killResult = await session.Rpc.Shell.KillAsync(execResult.ProcessId);

        Assert.True(killResult.Killed);
    }

    [Fact]
    public async Task Should_Start_Fleet_And_Complete_Custom_Tool_Task()
    {
        var markerPath = Path.Join(Ctx.WorkDir, $"fleet-rpc-{Guid.NewGuid():N}.txt");
        const string marker = "copilot-sdk-fleet-rpc";
        const string toolName = "record_fleet_completion";
        var session = await CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(RecordFleetCompletion, toolName, "Records completion of the fleet validation task.")],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var prompt = $"Use the {toolName} tool with content '{marker}', then report that the fleet task is complete.";

        var result = await session.Rpc.Fleet.StartAsync(prompt);

        Assert.True(result.Started);
        await WaitForFileTextAsync(markerPath, marker);

        var messages = await WaitForMessagesAsync(
            session,
            messages => messages.OfType<AssistantMessageEvent>().Any(m =>
                (m.Data.Content ?? string.Empty).Contains("fleet task", StringComparison.OrdinalIgnoreCase)));

        Assert.Contains(messages.OfType<UserMessageEvent>(), message => message.Data.Content.Contains(prompt, StringComparison.Ordinal));
        Assert.Contains(messages.OfType<ToolExecutionStartEvent>(), message => message.Data.ToolName == toolName);
        Assert.Contains(
            messages.OfType<ToolExecutionCompleteEvent>(),
            message => message.Data.Success &&
                (message.Data.Result?.Content?.Contains(marker, StringComparison.Ordinal) ?? false));
        Assert.Contains(
            messages.OfType<AssistantMessageEvent>(),
            message => (message.Data.Content ?? string.Empty).Contains("fleet task", StringComparison.OrdinalIgnoreCase));

        string RecordFleetCompletion(string content)
        {
            File.WriteAllText(markerPath, content);
            return content;
        }
    }

    private static string CreateWriteFileCommand(string markerPath, string marker)
    {
        if (OperatingSystem.IsWindows())
        {
            return $"powershell -NoLogo -NoProfile -Command \"Set-Content -LiteralPath '{markerPath}' -Value '{marker}'\"";
        }

        return $"sh -c \"printf '%s' '{marker}' > '{markerPath}'\"";
    }

    private static async Task WaitForFileTextAsync(string path, string expected)
    {
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                return File.Exists(path) &&
                    (await File.ReadAllTextAsync(path)).Contains(expected, StringComparison.Ordinal);
            },
            timeout: TimeSpan.FromSeconds(30),
            timeoutMessage: $"Timed out waiting for shell command to write '{expected}' to '{path}'.",
            transientExceptionFilter: TestHelper.IsTransientFileSystemException);
    }

    private static async Task<IReadOnlyList<SessionEvent>> WaitForMessagesAsync(
        CopilotSession session,
        Func<IReadOnlyList<SessionEvent>, bool> predicate)
    {
        // Fleet-mode tasks do not emit SessionIdleEvent on completion, so polling the
        // session message list is the simplest way to wait for the assistant's final
        // reply text without depending on idle-event semantics.
        IReadOnlyList<SessionEvent> messages = [];
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                messages = (await session.GetMessagesAsync()).ToList();
                return predicate(messages);
            },
            timeout: TimeSpan.FromSeconds(120),
            timeoutMessage: "Timed out waiting for fleet-mode assistant reply to satisfy predicate.",
            pollInterval: TimeSpan.FromMilliseconds(250));
        return messages;
    }
}
