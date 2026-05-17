/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics;
using System.Runtime.CompilerServices;
using System.Text;
using Xunit;

namespace GitHub.Copilot.SDK.Test.Unit;

/// <summary>
/// Integration tests for the MSBuild targets shipped in
/// <c>dotnet/src/build/GitHub.Copilot.SDK.targets</c>. Each test creates a throwaway
/// project that imports the targets file directly and invokes <c>dotnet build</c> in
/// a subprocess so we exercise real MSBuild evaluation.
/// </summary>
/// <remarks>
/// These tests deliberately do not exercise the network-bound default download path; they
/// pin a fake <c>CopilotCliVersion</c> and supply a fake CLI binary via
/// <c>CopilotCliBinaryPath</c>. That is sufficient to cover the regression in issue
/// #921 ("preinstalled CLI is ignored and copy/register are skipped when
/// CopilotSkipCliDownload=true").
/// </remarks>
public class MSBuildTargetsTests
{
    private static readonly string TargetsFilePath = FindTargetsFile();

    private static readonly string BinaryName = OperatingSystem.IsWindows() ? "copilot.exe" : "copilot";

    [Fact]
    public async Task PreinstalledCliBinaryPath_IsHonored_DownloadSkipped_AndCopiedToOutput()
    {
        using var sandbox = MSBuildSandbox.Create();
        var preinstalled = sandbox.WritePreinstalledBinary("fake-cli-contents");

        var result = await sandbox.BuildAsync(new Dictionary<string, string>
        {
            ["CopilotCliBinaryPath"] = preinstalled,
        });

        Assert.True(result.Succeeded, result.FailureMessage());

        // Download message must be absent because the download target was skipped.
        Assert.DoesNotContain("Downloading Copilot CLI", result.StandardOutput, StringComparison.Ordinal);

        // Binary must be placed at the canonical runtimes path so Client.cs can locate it.
        var outputPath = sandbox.ExpectedOutputBinary();
        Assert.True(File.Exists(outputPath), $"Expected CLI to be copied to '{outputPath}'.\n{result.FailureMessage()}");
        Assert.Equal(File.ReadAllText(preinstalled), File.ReadAllText(outputPath));
    }

    [Fact]
    public async Task PreinstalledCliBinaryPath_NormalizesNonStandardFileNameToCanonical()
    {
        using var sandbox = MSBuildSandbox.Create();
        // Use an off-spec source filename to confirm the copy task renames it to copilot[.exe].
        var preinstalled = sandbox.WritePreinstalledBinary("custom-named", fileName: "my-copilot-binary-v1.bin");

        var result = await sandbox.BuildAsync(new Dictionary<string, string>
        {
            ["CopilotCliBinaryPath"] = preinstalled,
        });

        Assert.True(result.Succeeded, result.FailureMessage());

        var outputPath = sandbox.ExpectedOutputBinary();
        Assert.True(File.Exists(outputPath), $"Expected canonical binary at '{outputPath}'.\n{result.FailureMessage()}");
    }

    [Fact]
    public async Task SkipCliDownload_WithoutBinaryPath_ProducesNoBinaryAndSucceeds()
    {
        using var sandbox = MSBuildSandbox.Create();

        var result = await sandbox.BuildAsync(new Dictionary<string, string>
        {
            ["CopilotSkipCliDownload"] = "true",
        });

        Assert.True(result.Succeeded, result.FailureMessage());

        // The runtimes folder may or may not be created by something else, but the binary
        // itself must not exist.
        Assert.False(File.Exists(sandbox.ExpectedOutputBinary()),
            $"Expected no CLI binary in output when CopilotSkipCliDownload=true and no path supplied.\n{result.FailureMessage()}");

        // Download must also have been skipped.
        Assert.DoesNotContain("Downloading Copilot CLI", result.StandardOutput, StringComparison.Ordinal);
    }

    [Fact]
    public async Task PreinstalledCliBinaryPath_WithSkipCliDownload_StillCopiesToOutput()
    {
        using var sandbox = MSBuildSandbox.Create();
        var preinstalled = sandbox.WritePreinstalledBinary("fake-cli-contents");

        var result = await sandbox.BuildAsync(new Dictionary<string, string>
        {
            ["CopilotCliBinaryPath"] = preinstalled,
            ["CopilotSkipCliDownload"] = "true",
        });

        Assert.True(result.Succeeded, result.FailureMessage());
        Assert.True(File.Exists(sandbox.ExpectedOutputBinary()), result.FailureMessage());
    }

    [Fact]
    public async Task PreinstalledCliBinaryPath_NonExistentFile_FailsWithActionableError()
    {
        using var sandbox = MSBuildSandbox.Create();
        var nonexistent = Path.Combine(sandbox.ProjectDir, "does-not-exist", BinaryName);

        var result = await sandbox.BuildAsync(new Dictionary<string, string>
        {
            ["CopilotCliBinaryPath"] = nonexistent,
        });

        Assert.False(result.Succeeded, "Build should have failed when CopilotCliBinaryPath points at a missing file.");
        Assert.Contains("Copilot CLI binary not found", result.StandardOutput, StringComparison.Ordinal);
        Assert.Contains(nonexistent, result.StandardOutput, StringComparison.Ordinal);
    }

    private static string FindTargetsFile([CallerFilePath] string? thisFile = null)
    {
        // thisFile == <repo>/dotnet/test/Unit/MSBuildTargetsTests.cs
        if (thisFile is not null && File.Exists(thisFile))
        {
            var candidate = Path.GetFullPath(Path.Combine(
                Path.GetDirectoryName(thisFile)!, "..", "..", "src", "build", "GitHub.Copilot.SDK.targets"));
            if (File.Exists(candidate))
            {
                return candidate;
            }
        }

        // Fall back to walking up from the test assembly location.
        var dir = AppContext.BaseDirectory;
        for (var i = 0; i < 8 && dir is not null; i++)
        {
            var candidate = Path.Combine(dir, "src", "build", "GitHub.Copilot.SDK.targets");
            if (File.Exists(candidate))
            {
                return candidate;
            }
            dir = Path.GetDirectoryName(dir);
        }

        throw new InvalidOperationException(
            "Could not locate GitHub.Copilot.SDK.targets relative to test assembly or source file.");
    }

    /// <summary>
    /// A throwaway directory containing a minimal csproj that imports the SDK targets
    /// file. Disposing removes the directory tree.
    /// </summary>
    private sealed class MSBuildSandbox : IDisposable
    {
        public string ProjectDir { get; }

        private MSBuildSandbox(string projectDir)
        {
            ProjectDir = projectDir;
        }

        public static MSBuildSandbox Create()
        {
            var dir = Path.Combine(Path.GetTempPath(), "copilot-sdk-targets-test-" + Guid.NewGuid().ToString("N"));
            Directory.CreateDirectory(dir);

            // Minimal class library that imports the SDK targets with a pinned fake
            // CopilotCliVersion so the targets do not need the generated props file.
            var csproj = $"""
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                    <CopilotCliVersion>0.0.0-test</CopilotCliVersion>
                    <EnableDefaultCompileItems>true</EnableDefaultCompileItems>
                  </PropertyGroup>
                  <Import Project="{TargetsFilePath}" />
                </Project>
                """;
            File.WriteAllText(Path.Combine(dir, "App.csproj"), csproj);
            File.WriteAllText(Path.Combine(dir, "Stub.cs"), "namespace CopilotSdkTargetsTest { internal static class Stub { } }\n");

            return new MSBuildSandbox(dir);
        }

        public string WritePreinstalledBinary(string contents, string? fileName = null)
        {
            var preinstallDir = Path.Combine(ProjectDir, "preinstall");
            Directory.CreateDirectory(preinstallDir);
            var path = Path.Combine(preinstallDir, fileName ?? BinaryName);
            File.WriteAllText(path, contents);
            return path;
        }

        public string ExpectedOutputBinary()
        {
            var rid = GetPortableRid();
            return Path.Combine(ProjectDir, "bin", "Debug", "net8.0", "runtimes", rid, "native", BinaryName);
        }

        public async Task<BuildResult> BuildAsync(IDictionary<string, string> properties)
        {
            var args = new StringBuilder("build --nologo -clp:NoSummary");
            foreach (var (key, value) in properties)
            {
                // Quote the value so paths with spaces are preserved.
                args.Append(" /p:").Append(key).Append('=').Append('"').Append(value).Append('"');
            }

            var psi = new ProcessStartInfo("dotnet", args.ToString())
            {
                WorkingDirectory = ProjectDir,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                UseShellExecute = false,
                CreateNoWindow = true,
            };
            // Avoid inheriting the parent's MSBuildSDKsPath/RuntimeIdentifier from the
            // running test host; the subprocess should resolve its own SDK.
            psi.Environment.Remove("MSBuildSDKsPath");

            using var process = Process.Start(psi) ?? throw new InvalidOperationException("Failed to start dotnet build subprocess.");

            // Drain both streams concurrently to avoid deadlocks on full pipe buffers.
            var stdoutTask = process.StandardOutput.ReadToEndAsync();
            var stderrTask = process.StandardError.ReadToEndAsync();

            // Generous timeout: dotnet restore + build of an empty project on a slow CI
            // worker can take ~60s the first time. We keep individual tests short by
            // using minimal projects.
            using var cts = new CancellationTokenSource(TimeSpan.FromMinutes(5));
            try
            {
                await process.WaitForExitAsync(cts.Token);
            }
            catch (OperationCanceledException)
            {
                try { process.Kill(entireProcessTree: true); } catch { /* best effort */ }
                throw new TimeoutException($"dotnet build did not complete within the timeout for args: {args}");
            }

            return new BuildResult(
                ExitCode: process.ExitCode,
                StandardOutput: await stdoutTask,
                StandardError: await stderrTask,
                CommandLine: $"dotnet {args}");
        }

        public void Dispose()
        {
            try { Directory.Delete(ProjectDir, recursive: true); } catch { /* best effort */ }
        }

        private static string GetPortableRid()
        {
            if (OperatingSystem.IsWindows())
            {
                return System.Runtime.InteropServices.RuntimeInformation.OSArchitecture switch
                {
                    System.Runtime.InteropServices.Architecture.Arm64 => "win-arm64",
                    _ => "win-x64",
                };
            }
            if (OperatingSystem.IsMacOS())
            {
                return System.Runtime.InteropServices.RuntimeInformation.OSArchitecture switch
                {
                    System.Runtime.InteropServices.Architecture.Arm64 => "osx-arm64",
                    _ => "osx-x64",
                };
            }
            return System.Runtime.InteropServices.RuntimeInformation.OSArchitecture switch
            {
                System.Runtime.InteropServices.Architecture.Arm64 => "linux-arm64",
                _ => "linux-x64",
            };
        }
    }

    private sealed record BuildResult(int ExitCode, string StandardOutput, string StandardError, string CommandLine)
    {
        public bool Succeeded => ExitCode == 0;

        public string FailureMessage() =>
            $"{CommandLine}\nExitCode: {ExitCode}\n--- STDOUT ---\n{StandardOutput}\n--- STDERR ---\n{StandardError}";
    }
}
