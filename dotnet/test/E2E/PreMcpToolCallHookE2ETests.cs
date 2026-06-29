/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class PreMcpToolCallHookE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "pre_mcp_tool_call_hook", output)
{
    private const string UnsupportedSdkHooksMessage = "SDK hook callbacks are no longer supported";

    [Fact]
    public async Task Rejects_SDK_PreMcpToolCall_Callback_Hooks()
    {
        var ex = await Assert.ThrowsAnyAsync<Exception>(() => CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            Hooks = new SessionHooks
            {
                OnPreMcpToolCall = (_, _) => Task.FromResult<PreMcpToolCallHookOutput?>(new PreMcpToolCallHookOutput()),
            },
        }));
        Assert.Contains(UnsupportedSdkHooksMessage, ex.ToString(), StringComparison.Ordinal);
    }
}
