/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

[CollectionDefinition(Name, DisableParallelization = true)]
public sealed class RuntimeWrapperIsolationCollection
{
    public const string Name = "Runtime wrapper isolation";
}

[Collection(RuntimeWrapperIsolationCollection.Name)]
public sealed class RuntimeWrapperTests
{
    [Fact]
    public async Task Managed_Launch_Fails_When_Bundled_Runtime_Pair_Is_Missing()
    {
        var originalBaseDirectory = AppContext.GetData("APP_CONTEXT_BASE_DIRECTORY");
        var emptyBaseDirectory = Path.Combine(
            Path.GetTempPath(),
            $"missing-copilot-runtime-{Guid.NewGuid():N}");
        Directory.CreateDirectory(emptyBaseDirectory);

        try
        {
            AppContext.SetData("APP_CONTEXT_BASE_DIRECTORY", emptyBaseDirectory);
            await using var client = new CopilotClient(new CopilotClientOptions
            {
                Connection = RuntimeConnection.ForStdio(),
                Environment = new Dictionary<string, string>(),
            });

            var exception = await Assert.ThrowsAsync<InvalidOperationException>(() => client.StartAsync());

            Assert.Contains("runtime wrapper not found", exception.Message, StringComparison.OrdinalIgnoreCase);
        }
        finally
        {
            AppContext.SetData("APP_CONTEXT_BASE_DIRECTORY", originalBaseDirectory);
            Directory.Delete(emptyBaseDirectory);
        }
    }

    [Fact]
    public async Task Explicit_Path_Does_Not_Require_Adjacent_Runtime_Node()
    {
        var explicitPath = Path.Combine(
            Path.GetTempPath(),
            $"missing-explicit-copilot-{Guid.NewGuid():N}");
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(path: explicitPath),
            Environment = new Dictionary<string, string>(),
        });

        var exception = await Assert.ThrowsAnyAsync<Exception>(() => client.StartAsync());

        Assert.DoesNotContain("runtime.node", exception.ToString(), StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task Copilot_Cli_Path_Does_Not_Require_Adjacent_Runtime_Node()
    {
        var explicitPath = Path.Combine(
            Path.GetTempPath(),
            $"missing-environment-copilot-{Guid.NewGuid():N}");
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(),
            Environment = new Dictionary<string, string> { ["COPILOT_CLI_PATH"] = explicitPath },
        });

        var exception = await Assert.ThrowsAnyAsync<Exception>(() => client.StartAsync());

        Assert.DoesNotContain("runtime.node", exception.ToString(), StringComparison.OrdinalIgnoreCase);
    }
}
