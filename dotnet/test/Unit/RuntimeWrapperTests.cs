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
#if !NETFRAMEWORK
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
#endif

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

#if !NETFRAMEWORK
    [Fact]
    public async Task Marked_Bundled_Explicit_Cli_Does_Not_Require_Runtime_Pair()
    {
        var originalBaseDirectory = AppContext.GetData("APP_CONTEXT_BASE_DIRECTORY");
        var baseDirectory = Path.Combine(
            Path.GetTempPath(),
            $"explicit-bundled-copilot-{Guid.NewGuid():N}");
        var rid = GetPortableRid();
        var nativeDirectory = Path.Combine(baseDirectory, "runtimes", rid, "native");
        Directory.CreateDirectory(nativeDirectory);
        var cliPath = Path.Combine(nativeDirectory, OperatingSystem.IsWindows() ? "copilot.exe" : "copilot");
        await File.WriteAllTextAsync(cliPath, "not an executable");
        await File.WriteAllTextAsync(Path.Combine(nativeDirectory, ".copilot-explicit-cli"), "explicit");

        try
        {
            AppContext.SetData("APP_CONTEXT_BASE_DIRECTORY", baseDirectory);
            await using var client = new CopilotClient(new CopilotClientOptions
            {
                Connection = RuntimeConnection.ForStdio(),
                Environment = new Dictionary<string, string>(),
            });

            var exception = await Assert.ThrowsAnyAsync<Exception>(() => client.StartAsync());

            Assert.DoesNotContain("runtime wrapper", exception.ToString(), StringComparison.OrdinalIgnoreCase);
            Assert.Contains(cliPath, exception.ToString(), StringComparison.OrdinalIgnoreCase);
        }
        finally
        {
            AppContext.SetData("APP_CONTEXT_BASE_DIRECTORY", originalBaseDirectory);
            Directory.Delete(baseDirectory, recursive: true);
        }
    }
#endif

    private static string GetPortableRid()
    {
        var os = OperatingSystem.IsWindows() ? "win"
            : OperatingSystem.IsMacOS() ? "osx"
            : "linux";
        var architecture = System.Runtime.InteropServices.RuntimeInformation.OSArchitecture switch
        {
            System.Runtime.InteropServices.Architecture.X64 => "x64",
            System.Runtime.InteropServices.Architecture.Arm64 => "arm64",
            _ => throw new PlatformNotSupportedException(),
        };
        return $"{os}-{architecture}";
    }
}
