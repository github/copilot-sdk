/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using Microsoft.Extensions.AI;
using System.ComponentModel;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class ScenarioTestingPermissionsE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : ScenarioTestingE2ETestBase(fixture, "scenario_testing_permissions", output)
{
    [Theory]
    [InlineData("assisted", "gpt-5.5")]
    [InlineData("allow-all", null)]
    public async Task Should_Set_Reset_And_Read_Authoritative_Scenario_Permission_Mode(
        string modeValue,
        string? assistedApprovalModel)
    {
        await using var session = await CreateSessionAsync();
        var mode = new PermissionMode(modeValue);

        Assert.Equal(PermissionMode.Manual, (await session.Rpc.Permissions.GetModeAsync()).Mode);

        var set = await session.Rpc.Permissions.SetModeAsync(
            mode,
            assistedApprovalModel: assistedApprovalModel,
            source: PermissionModeSource.Rpc);
        Assert.True(set.Success);
        Assert.Equal(mode, set.Mode);
        Assert.Equal(mode, (await session.Rpc.Permissions.GetModeAsync()).Mode);

        var reset = await session.Rpc.Permissions.SetModeAsync(
            PermissionMode.Manual,
            source: PermissionModeSource.Rpc);
        Assert.True(reset.Success);
        Assert.Equal(PermissionMode.Manual, reset.Mode);
        Assert.Equal(PermissionMode.Manual, (await session.Rpc.Permissions.GetModeAsync()).Mode);
    }

    [Fact]
    public async Task Should_Report_Managed_Effective_Mode_When_Scenario_Escalation_Fails()
    {
        var resolved = new TaskCompletionSource<SessionManagedSettingsResolvedEvent>(
            TaskCreationOptions.RunContinuationsAsynchronously);
        var enforced = new TaskCompletionSource<SessionManagedSettingsEnforcedEvent>(
            TaskCreationOptions.RunContinuationsAsynchronously);

        await using var session = await CreateSessionAsync(new SessionConfig
        {
            EnableManagedSettings = true,
            ManagedSettings = new ManagedSettings
            {
                Permissions = new ManagedSettingsPermissions
                {
                    DisableBypassPermissionsMode = DisableBypassPermissionsModes.Disable,
                },
            },
            OnEvent = evt =>
            {
                if (evt is SessionManagedSettingsResolvedEvent resolvedEvent)
                {
                    resolved.TrySetResult(resolvedEvent);
                }
                else if (evt is SessionManagedSettingsEnforcedEvent enforcedEvent)
                {
                    enforced.TrySetResult(enforcedEvent);
                }
            },
        });

        var resolvedEvent = await resolved.Task.WaitAsync(TimeSpan.FromSeconds(30));
        Assert.True(resolvedEvent.Data.ClientManaged);
        Assert.True(resolvedEvent.Data.BypassPermissionsDisabled);
        Assert.Contains("permissions", resolvedEvent.Data.ManagedKeys);

        var set = await session.Rpc.Permissions.SetModeAsync(
            PermissionMode.AllowAll,
            source: PermissionModeSource.Rpc);
        Assert.False(set.Success);
        Assert.NotEqual(PermissionMode.AllowAll, set.Mode);

        var authoritative = await session.Rpc.Permissions.GetModeAsync();
        Assert.Equal(set.Mode, authoritative.Mode);

        var enforcedEvent = await enforced.Task.WaitAsync(TimeSpan.FromSeconds(30));
        Assert.Equal(ManagedSettingsEnforcedAction.BypassPermissionsBlocked, enforcedEvent.Data.Action);
        Assert.Equal(ManagedSettingsEnforcedEscalation.AllowAll, enforcedEvent.Data.Escalation);
        Assert.Equal("permissions.disableBypassPermissionsMode", enforcedEvent.Data.Setting);
    }

    [Fact]
    public async Task Should_Forward_Exact_Scenario_Permission_Callback_Payload()
    {
        var callback = new TaskCompletionSource<(PermissionRequestCustomTool Request, PermissionInvocation Invocation)>(
            TaskCreationOptions.RunContinuationsAsynchronously);
        CopilotSession? session = null;
        session = await CreateSessionAsync(new SessionConfig
        {
            Tools =
            [
                AIFunctionFactory.Create(
                    ScenarioPermissionTool,
                    "scenario_permission_tool",
                    "Reads a scenario-owned value after user approval")
            ],
            OnPermissionRequest = (request, invocation) =>
            {
                callback.TrySetResult((Assert.IsType<PermissionRequestCustomTool>(request), invocation));
                return Task.FromResult<PermissionDecision>(PermissionDecision.ApproveOnce());
            },
        });

        var response = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Call scenario_permission_tool with key 'payload', then reply with exactly its result.",
            DisplayPrompt = "Run permission-gated scenario action",
            Source = MessageSource.Agent("scenario-client"),
        });

        var (request, invocation) = await callback.Task.WaitAsync(TimeSpan.FromSeconds(30));
        Assert.Equal(session.SessionId, invocation.SessionId);
        Assert.False(invocation.ManagedSettingsEnabled);
        Assert.Equal("scenario_permission_tool", request.ToolName);
        Assert.Equal("Reads a scenario-owned value after user approval", request.ToolDescription);
        Assert.Equal("payload", request.Args!.Value.GetProperty("key").GetString());
        Assert.False(string.IsNullOrWhiteSpace(request.ToolCallId));
        Assert.Contains("SCENARIO_PERMISSION_PAYLOAD", response?.Data.Content ?? string.Empty, StringComparison.Ordinal);

        [Description("Reads a scenario-owned value after user approval")]
        static string ScenarioPermissionTool([Description("Scenario lookup key")] string key) =>
            $"SCENARIO_PERMISSION_{key.ToUpperInvariant()}";
    }

    [Fact]
    public async Task Should_Use_Scenario_Location_And_Folder_Trust_Rpcs()
    {
        await using var session = await CreateSessionAsync();
        var location = Path.Join(Ctx.WorkDir, $"scenario-location-{Guid.NewGuid():N}");
        var trusted = Path.Join(Ctx.WorkDir, $"scenario-trusted-{Guid.NewGuid():N}");
        Directory.CreateDirectory(location);
        Directory.CreateDirectory(trusted);

        var resolved = await session.Rpc.Permissions.Locations.ResolveAsync(location);
        Assert.Equal(PermissionLocationType.Dir, resolved.LocationType);
        Assert.True(PathsEqual(location, resolved.LocationKey));

        var identifier = $"scenario-client-command-{Guid.NewGuid():N}";
        var add = await session.Rpc.Permissions.Locations.AddToolApprovalAsync(
            resolved.LocationKey,
            new PermissionsLocationsAddToolApprovalDetailsCommands
            {
                CommandIdentifiers = [identifier],
            });
        Assert.True(add.Success);

        var applied = await session.Rpc.Permissions.Locations.ApplyAsync(location);
        Assert.True(applied.AppliedRuleCount >= 1);
        Assert.Contains(applied.AppliedRules, rule => rule.Kind == "shell" && rule.Argument == identifier);

        Assert.False((await session.Rpc.Permissions.FolderTrust.IsTrustedAsync(trusted)).Trusted);
        Assert.True((await session.Rpc.Permissions.FolderTrust.AddTrustedAsync(trusted)).Success);
        Assert.True((await session.Rpc.Permissions.FolderTrust.IsTrustedAsync(trusted)).Trusted);
    }

    private static bool PathsEqual(string left, string right) =>
        string.Equals(
            Path.GetFullPath(left).TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar),
            Path.GetFullPath(right).TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar),
            OperatingSystem.IsWindows() ? StringComparison.OrdinalIgnoreCase : StringComparison.Ordinal);
}
