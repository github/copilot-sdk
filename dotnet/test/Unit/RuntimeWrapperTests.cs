/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed class RuntimeWrapperTests
{
    [Fact]
    public async Task Runtime_Override_Requires_Adjacent_Runtime_Node()
    {
        var directory = Directory.CreateTempSubdirectory("copilot-runtime-pair-");
        try
        {
            var wrapper = Path.Combine(
                directory.FullName,
                OperatingSystem.IsWindows() ? "copilot-runtime.exe" : "copilot-runtime");
            await File.WriteAllTextAsync(wrapper, "wrapper");
            await using var client = new CopilotClient(new CopilotClientOptions
            {
                Connection = RuntimeConnection.ForStdio(),
                Environment = new Dictionary<string, string>
                {
                    ["COPILOT_RUNTIME_PATH"] = wrapper,
                },
            });

            var exception = await Assert.ThrowsAsync<InvalidOperationException>(() => client.StartAsync());

            Assert.Contains("adjacent runtime.node", exception.Message);
        }
        finally
        {
            directory.Delete(recursive: true);
        }
    }
}
