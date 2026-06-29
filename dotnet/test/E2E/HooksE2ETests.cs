/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class HooksE2ETests(E2ETestFixture fixture, ITestOutputHelper output) : E2ETestBase(fixture, "hooks", output)
{
    private const string UnsupportedSdkHooksMessage = "SDK hook callbacks are no longer supported";

    private async Task AssertUnsupportedHooksAsync(SessionHooks hooks)
    {
        var ex = await Assert.ThrowsAnyAsync<Exception>(() => CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            Hooks = hooks,
        }));
        Assert.Contains(UnsupportedSdkHooksMessage, ex.ToString(), StringComparison.Ordinal);
    }

    public static IEnumerable<object[]> HookCases =>
    [
        [new SessionHooks
        {
            OnPreToolUse = (_, _) => Task.FromResult<PreToolUseHookOutput?>(new PreToolUseHookOutput { PermissionDecision = "allow" }),
        }],
        [new SessionHooks
        {
            OnPostToolUse = (_, _) => Task.FromResult<PostToolUseHookOutput?>(null),
        }],
        [new SessionHooks
        {
            OnPreToolUse = (_, _) => Task.FromResult<PreToolUseHookOutput?>(new PreToolUseHookOutput { PermissionDecision = "deny" }),
        }],
        [new SessionHooks
        {
            OnPreToolUse = (_, _) => Task.FromResult<PreToolUseHookOutput?>(new PreToolUseHookOutput { PermissionDecision = "allow" }),
            OnPostToolUse = (_, _) => Task.FromResult<PostToolUseHookOutput?>(null),
        }],
    ];

    [Theory]
    [MemberData(nameof(HookCases))]
    public async Task Rejects_SDK_Callback_Hooks(SessionHooks hooks)
    {
        await AssertUnsupportedHooksAsync(hooks);
    }
}
