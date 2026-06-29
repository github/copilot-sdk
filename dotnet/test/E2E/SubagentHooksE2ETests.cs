/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class SubagentHooksE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "subagent_hooks", output)
{
    private const string UnsupportedSdkHooksMessage = "SDK hook callbacks are no longer supported";

    [Fact]
    public async Task Rejects_SDK_Callback_Hooks_For_Sub_Agent_Hook_Propagation()
    {
        var ex = await Assert.ThrowsAnyAsync<Exception>(() => CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            Hooks = new SessionHooks
            {
                OnPreToolUse = (_, _) => Task.FromResult<PreToolUseHookOutput?>(new PreToolUseHookOutput { PermissionDecision = "allow" }),
                OnPostToolUse = (_, _) => Task.FromResult<PostToolUseHookOutput?>(null),
            },
        }));
        Assert.Contains(UnsupportedSdkHooksMessage, ex.ToString(), StringComparison.Ordinal);
    }
}
