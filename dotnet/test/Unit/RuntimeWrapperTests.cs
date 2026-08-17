/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using System.Reflection;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed class RuntimeWrapperTests
{
    [Fact]
    public async Task Runtime_Override_Requires_Adjacent_Runtime_Node()
    {
        var directory = Directory.CreateTempSubdirectory("copilot-runtime-pair-");
        var originalCliPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH");
        try
        {
            Environment.SetEnvironmentVariable(
                "COPILOT_CLI_PATH",
                Path.Combine(directory.FullName, OperatingSystem.IsWindows() ? "copilot.exe" : "copilot"));
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
                    ["COPILOT_SDK_USE_LEGACY_CLI"] = "true",
                },
            });

            var exception = await Assert.ThrowsAsync<InvalidOperationException>(() => client.StartAsync());

            Assert.Contains("adjacent runtime.node", exception.Message);
        }
        finally
        {
            Environment.SetEnvironmentVariable("COPILOT_CLI_PATH", originalCliPath);
            directory.Delete(recursive: true);
        }
    }

    [Theory]
    [InlineData(null, false)]
    [InlineData("", false)]
    [InlineData("0", false)]
    [InlineData("false", false)]
    [InlineData("1", true)]
    [InlineData("true", true)]
    [InlineData("TRUE", true)]
    public void Legacy_Cli_Environment_Value_Uses_Standard_Truthy_Parsing(string? value, bool expected)
    {
        var method = typeof(CopilotClient).GetMethod(
            "IsTruthyEnvironmentValue",
            BindingFlags.NonPublic | BindingFlags.Static);

        Assert.NotNull(method);
        Assert.Equal(expected, method!.Invoke(null, [value]));
    }

    [Fact]
    public void Bundled_Launch_Defaults_To_Wrapper_And_Legacy_Selects_Root_Cli()
    {
        var directory = Directory.CreateTempSubdirectory("copilot-bundled-runtime-");
        var cliPath = Path.Combine(
            directory.FullName,
            OperatingSystem.IsWindows() ? "copilot.exe" : "copilot");
        var wrapperPath = Path.Combine(
            directory.FullName,
            OperatingSystem.IsWindows() ? "copilot-runtime.exe" : "copilot-runtime");
        File.WriteAllText(cliPath, "cli");
        File.WriteAllText(wrapperPath, "wrapper");
        File.WriteAllText(Path.Combine(directory.FullName, "runtime.node"), "runtime");
        var method = typeof(CopilotClient).GetMethod(
            "CreateBundledRuntimeLaunch",
            BindingFlags.NonPublic | BindingFlags.Static);

        Assert.NotNull(method);
        var defaultLaunch = method!.Invoke(null, [cliPath, false]);
        var legacyLaunch = method.Invoke(null, [cliPath, true]);

        Assert.NotNull(defaultLaunch);
        Assert.NotNull(legacyLaunch);
        var executable = defaultLaunch!.GetType().GetProperty("Executable");
        var residualCli = defaultLaunch.GetType().GetProperty("ResidualCli");
        Assert.NotNull(executable);
        Assert.NotNull(residualCli);

        Assert.EndsWith(
            OperatingSystem.IsWindows() ? "copilot-runtime.exe" : "copilot-runtime",
            Assert.IsType<string>(executable!.GetValue(defaultLaunch)));
        Assert.EndsWith(
            OperatingSystem.IsWindows() ? "copilot.exe" : "copilot",
            Assert.IsType<string>(residualCli!.GetValue(defaultLaunch)));
        Assert.EndsWith(
            OperatingSystem.IsWindows() ? "copilot.exe" : "copilot",
            Assert.IsType<string>(executable.GetValue(legacyLaunch)));
        Assert.Null(residualCli.GetValue(legacyLaunch));
        directory.Delete(recursive: true);
    }

    [Fact]
    public async Task Explicit_Path_Precedes_Legacy_Selection()
    {
        var explicitPath = Path.Combine(
            Path.GetTempPath(),
            $"missing-explicit-copilot-{Guid.NewGuid():N}");
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(path: explicitPath),
            Environment = new Dictionary<string, string>
            {
                ["COPILOT_SDK_USE_LEGACY_CLI"] = "true",
            },
        });

        var exception = await Assert.ThrowsAnyAsync<Exception>(() => client.StartAsync());

        Assert.Contains(explicitPath, exception.ToString());
    }
}
