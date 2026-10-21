/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Reflection;
using GitHub.Copilot.Test.Harness;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed class InProcessRuntimeArgumentsTests
{
    [Fact]
    public void Integration_Id_Is_Forwarded_To_The_Native_Host()
    {
        InProcessEnvIsolation.Apply("GITHUB_COPILOT_INTEGRATION_ID", "  microsoft/vscode  ");
        var method = typeof(CopilotClient).GetMethod(
            "BuildInProcessRuntimeArguments",
            BindingFlags.Static | BindingFlags.NonPublic);
        Assert.NotNull(method);

        var args = Assert.IsType<List<string>>(
            method!.Invoke(null, [new CopilotClientOptions()]));

        Assert.Contains("--cli-login-sync-integration-id=microsoft/vscode", args);
    }
}
