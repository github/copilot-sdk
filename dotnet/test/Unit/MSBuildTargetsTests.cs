/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Collections.Concurrent;
using System.Diagnostics;
using System.Net;
using System.Net.Sockets;
using System.Runtime.CompilerServices;
using System.Security.Cryptography;
using System.Text;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

/// <summary>
/// Integration tests for the MSBuild targets shipped in
/// <c>dotnet/src/build/GitHub.Copilot.SDK.targets</c>. Each test creates a throwaway
/// project that imports the targets file directly and invokes <c>dotnet build</c> in
/// a subprocess so we exercise real MSBuild evaluation.
/// </summary>
/// <remarks>
/// Download tests use a loopback release server; they never access the default GitHub URL.
/// </remarks>
public class MSBuildTargetsTests
{
    private static readonly string TargetsFilePath = FindTargetsFile();

    private static readonly string BinaryName = OperatingSystem.IsWindows() ? "copilot.exe" : "copilot";

    private static readonly string RuntimeWrapperName =
        OperatingSystem.IsWindows() ? "copilot-runtime.exe" : "copilot-runtime";

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
        Assert.True(File.Exists(Path.Combine(Path.GetDirectoryName(outputPath)!, ".copilot-explicit-cli")));
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
    public async Task ReleaseAsset_IsDownloadedVerifiedExtractedAndCached()
    {
        using var sandbox = MSBuildSandbox.Create();
        var archive = sandbox.CreateReleaseArchive("release-runtime-wrapper");
        var assetName = $"github-copilot-0.0.0-test-{GetReleasePlatform()}.tgz";
        var assetPath = $"/v0.0.0-test/{assetName}";
        var checksumsPath = "/v0.0.0-test/SHA256SUMS.txt";
        var checksum = ComputeSha256(archive);
        using var server = new ReleaseServer(new Dictionary<string, byte[]>
        {
            [checksumsPath] = Encoding.UTF8.GetBytes($"{checksum}  {assetName}\n"),
            [assetPath] = archive,
        });

        var properties = new Dictionary<string, string>
        {
            ["CopilotCliReleaseBaseUrl"] = server.BaseUrl,
        };
        var firstBuild = await sandbox.BuildAsync(properties);

        Assert.True(firstBuild.Succeeded, firstBuild.FailureMessage());
        Assert.Equal("release-runtime-wrapper", File.ReadAllText(sandbox.ExpectedOutputBinary()));
        Assert.Equal("release-runtime-wrapper", File.ReadAllText(sandbox.ExpectedRuntimeAsset(RuntimeWrapperName)));
        Assert.Equal("runtime", File.ReadAllText(sandbox.ExpectedRuntimeAsset("runtime.node")));
        Assert.True(File.Exists(sandbox.ExpectedCacheAsset(".copilot-runtime-complete")));
        Assert.False(File.Exists(sandbox.ExpectedRuntimeAsset(".copilot-runtime-complete")));
        Assert.Equal(1, server.RequestPaths.Count(path => path == checksumsPath));
        Assert.Equal(1, server.RequestPaths.Count(path => path == assetPath));
        Assert.False(File.Exists(sandbox.ExpectedRuntimeAsset("SHA256SUMS.txt")));

        var secondBuild = await sandbox.BuildAsync(properties);

        Assert.True(secondBuild.Succeeded, secondBuild.FailureMessage());
        Assert.Equal(2, server.RequestPaths.Count);
    }

    [Fact]
    public async Task IncompleteCache_WithRuntimePairButNoMarker_IsReacquired()
    {
        using var sandbox = MSBuildSandbox.Create();
        sandbox.WriteRuntimeCacheAsset("prebuilds", GetReleasePlatform(), RuntimeWrapperName, "partial-wrapper");
        sandbox.WriteRuntimeCacheAsset("prebuilds", GetReleasePlatform(), "runtime.node", "partial-runtime");
        sandbox.WriteRuntimeCacheAsset("definitions", "stale.json", "stale");
        var archive = sandbox.CreateReleaseArchive("complete-wrapper");
        var assetName = $"github-copilot-0.0.0-test-{GetReleasePlatform()}.tgz";
        var assetPath = $"/v0.0.0-test/{assetName}";
        var checksumsPath = "/v0.0.0-test/SHA256SUMS.txt";
        using var server = new ReleaseServer(new Dictionary<string, byte[]>
        {
            [checksumsPath] = Encoding.UTF8.GetBytes($"{ComputeSha256(archive)}  {assetName}\n"),
            [assetPath] = archive,
        });

        var result = await sandbox.BuildAsync(new Dictionary<string, string>
        {
            ["CopilotCliReleaseBaseUrl"] = server.BaseUrl,
        });

        Assert.True(result.Succeeded, result.FailureMessage());
        Assert.Equal(1, server.RequestPaths.Count(path => path == checksumsPath));
        Assert.Equal(1, server.RequestPaths.Count(path => path == assetPath));
        Assert.Equal("complete-wrapper", File.ReadAllText(sandbox.ExpectedRuntimeAsset(RuntimeWrapperName)));
        Assert.Equal("runtime", File.ReadAllText(sandbox.ExpectedRuntimeAsset("runtime.node")));
        Assert.True(File.Exists(sandbox.ExpectedCacheAsset(".copilot-runtime-complete")));
        Assert.False(File.Exists(sandbox.ExpectedCacheAsset("definitions", "stale.json")));
        Assert.False(File.Exists(sandbox.ExpectedRuntimeAsset("definitions", "stale.json")));
    }

    [Fact]
    public async Task ReleaseAsset_WithChecksumMismatch_FailsBeforeExtraction()
    {
        using var sandbox = MSBuildSandbox.Create();
        var archive = Encoding.UTF8.GetBytes("not the expected archive");
        var assetName = $"github-copilot-0.0.0-test-{GetReleasePlatform()}.tgz";
        using var server = new ReleaseServer(new Dictionary<string, byte[]>
        {
            ["/v0.0.0-test/SHA256SUMS.txt"] =
                Encoding.UTF8.GetBytes($"{new string('0', 64)} *{assetName}\n"),
            [$"/v0.0.0-test/{assetName}"] = archive,
        });

        var result = await sandbox.BuildAsync(new Dictionary<string, string>
        {
            ["CopilotCliReleaseBaseUrl"] = server.BaseUrl,
        });

        Assert.False(result.Succeeded, "Build should fail when the release checksum does not match.");
        Assert.Contains($"Checksum mismatch for {assetName}", result.StandardOutput, StringComparison.Ordinal);
        Assert.False(File.Exists(sandbox.ExpectedOutputBinary()));
    }

    [Fact]
    public async Task RuntimePackageAssets_AreFilteredAndCopiedToOutput()
    {
        using var sandbox = MSBuildSandbox.Create();
        var preinstalled = sandbox.WritePreinstalledBinary("fake-cli-contents");
        sandbox.WriteRuntimeCacheAsset("prebuilds", GetReleasePlatform(), "runtime.node", "runtime");
        sandbox.WriteRuntimeCacheAsset("prebuilds", GetReleasePlatform(),
            RuntimeWrapperName, "wrapper");
        sandbox.WriteRuntimeCacheAsset("ripgrep", "bin", GetReleasePlatform(), "rg", "ripgrep");
        sandbox.WriteRuntimeCacheAsset("definitions", "future.json", "{}");
        sandbox.WriteRuntimeCacheAsset("copilot-sdk", "extension.js", "extension");
        sandbox.WriteRuntimeCacheAsset("preloads", "extension_bootstrap.mjs", "preload");
        sandbox.WriteRuntimeCacheAsset("sdk", "factory.js", "factory");
        sandbox.WriteRuntimeCacheAsset("app.js", "excluded");
        sandbox.WriteRuntimeCacheAsset("LICENSE.md", "excluded");
        sandbox.WriteRuntimeCacheAsset("README.md", "excluded");
        sandbox.WriteStaleOutputRuntimeAsset("obsolete", "tool", "stale");

        var result = await sandbox.BuildAsync(new Dictionary<string, string>
        {
            ["CopilotCliBinaryPath"] = preinstalled,
        });

        Assert.True(result.Succeeded, result.FailureMessage());
        Assert.Equal("ripgrep", File.ReadAllText(sandbox.ExpectedRuntimeAsset("ripgrep", "bin", GetReleasePlatform(), "rg")));
        Assert.Equal("{}", File.ReadAllText(sandbox.ExpectedRuntimeAsset("definitions", "future.json")));
        Assert.Equal("extension", File.ReadAllText(sandbox.ExpectedRuntimeAsset("copilot-sdk", "extension.js")));
        Assert.Equal("preload", File.ReadAllText(sandbox.ExpectedRuntimeAsset("preloads", "extension_bootstrap.mjs")));
        Assert.Equal("factory", File.ReadAllText(sandbox.ExpectedRuntimeAsset("sdk", "factory.js")));
        Assert.False(File.Exists(sandbox.ExpectedRuntimeAsset("app.js")));
        Assert.False(File.Exists(sandbox.ExpectedRuntimeAsset("LICENSE.md")));
        Assert.False(File.Exists(sandbox.ExpectedRuntimeAsset("README.md")));
        Assert.False(File.Exists(sandbox.ExpectedRuntimeAsset("obsolete", "tool")));
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

    private static string GetReleasePlatform()
    {
        var arch = System.Runtime.InteropServices.RuntimeInformation.ProcessArchitecture
            == System.Runtime.InteropServices.Architecture.Arm64
                ? "arm64"
                : "x64";
        if (OperatingSystem.IsWindows()) return $"win32-{arch}";
        if (OperatingSystem.IsMacOS()) return $"darwin-{arch}";
        return $"linux-{arch}";
    }

    private static string ComputeSha256(byte[] contents)
    {
#if NETFRAMEWORK
        using var sha256 = SHA256.Create();
        return BitConverter.ToString(sha256.ComputeHash(contents)).Replace("-", "").ToLowerInvariant();
#else
        return Convert.ToHexString(SHA256.HashData(contents)).ToLowerInvariant();
#endif
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
            // Strip any path information from fileName so it cannot escape preinstallDir.
            var safeFileName = string.IsNullOrEmpty(fileName) ? BinaryName : Path.GetFileName(fileName);
            var path = Path.Combine(preinstallDir, safeFileName);
            File.WriteAllText(path, contents);
            return path;
        }

        public byte[] CreateReleaseArchive(string runtimeWrapperContents)
        {
            var sourceDir = Path.Combine(ProjectDir, "release-source");
            var packageDir = Path.Combine(sourceDir, "package");
            var prebuildDir = Path.Combine(packageDir, "prebuilds", GetReleasePlatform());
            Directory.CreateDirectory(prebuildDir);
            File.WriteAllText(Path.Combine(prebuildDir, "runtime.node"), "runtime");
            File.WriteAllText(Path.Combine(prebuildDir, RuntimeWrapperName), runtimeWrapperContents);

            var archivePath = Path.Combine(ProjectDir, "release-asset.tgz");
            var tarPath = OperatingSystem.IsWindows()
                ? Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.Windows), "System32", "tar.exe")
                : "tar";
            var startInfo = new ProcessStartInfo(tarPath)
            {
                Arguments = $"-czf \"{archivePath}\" -C \"{sourceDir}\" package",
                RedirectStandardError = true,
                UseShellExecute = false,
            };
            using var process = Process.Start(startInfo) ??
                throw new InvalidOperationException("Failed to start tar while creating a release test asset.");
            var standardError = process.StandardError.ReadToEnd();
            process.WaitForExit();
            if (process.ExitCode != 0)
            {
                throw new InvalidOperationException($"tar failed while creating a release test asset: {standardError}");
            }
            return File.ReadAllBytes(archivePath);
        }

        public string ExpectedOutputBinary()
        {
            var rid = GetPortableRid();
            return Path.Combine(ProjectDir, "bin", "Debug", "net8.0", "runtimes", rid, "native", BinaryName);
        }

        public void WriteRuntimeCacheAsset(params string[] pathAndContents)
        {
            var pathParts = pathAndContents.Take(pathAndContents.Length - 1).ToArray();
            var path = ExpectedCacheAsset(pathParts);
            Directory.CreateDirectory(Path.GetDirectoryName(path)!);
            File.WriteAllText(path, pathAndContents[^1]);
        }

        public string ExpectedCacheAsset(params string[] pathParts)
        {
            var path = Path.Combine(ProjectDir, "obj", "Debug", "net8.0", "copilot-cli", "0.0.0-test",
                GetReleasePlatform());
            foreach (var part in pathParts)
            {
                path = Path.Combine(path, part);
            }
            return path;
        }

        public string ExpectedRuntimeAsset(params string[] pathParts)
        {
            var path = Path.Combine(ProjectDir, "bin", "Debug", "net8.0", "runtimes", GetPortableRid(), "native");
            foreach (var part in pathParts)
            {
                path = Path.Combine(path, part);
            }
            return path;
        }

        public void WriteStaleOutputRuntimeAsset(params string[] pathAndContents)
        {
            var relativeParts = pathAndContents.Take(pathAndContents.Length - 1).ToArray();
            var path = ExpectedRuntimeAsset(relativeParts);
            Directory.CreateDirectory(Path.GetDirectoryName(path)!);
            File.WriteAllText(path, pathAndContents[^1]);
            var manifest = ExpectedRuntimeAsset(".copilot-runtime-assets");
            Directory.CreateDirectory(Path.GetDirectoryName(manifest)!);
            File.WriteAllText(
                manifest,
                string.Join(Path.DirectorySeparatorChar.ToString(), relativeParts) + Environment.NewLine);
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
            // running test host; the subprocess should resolve its own SDK and pick the
            // RID that matches ExpectedOutputBinary().
            psi.Environment.Remove("MSBuildSDKsPath");
            psi.Environment.Remove("RuntimeIdentifier");

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
                try { process.Kill(entireProcessTree: true); }
                catch (InvalidOperationException) { /* process already exited */ }
                catch (NotSupportedException) { /* not supported on this platform */ }
                catch (System.ComponentModel.Win32Exception) { /* kill failed; best effort */ }
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
            try { Directory.Delete(ProjectDir, recursive: true); }
            catch (IOException) { /* cleanup is best effort */ }
            catch (UnauthorizedAccessException) { /* cleanup is best effort */ }
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

    private sealed class ReleaseServer : IDisposable
    {
        private readonly IReadOnlyDictionary<string, byte[]> _responses;
        private readonly TcpListener _listener = new(IPAddress.Loopback, 0);
        private readonly CancellationTokenSource _cancellation = new();
        private readonly Task _serverTask;

        public ReleaseServer(IReadOnlyDictionary<string, byte[]> responses)
        {
            _responses = responses;
            _listener.Start();
            var endpoint = (IPEndPoint)_listener.LocalEndpoint;
            BaseUrl = $"http://127.0.0.1:{endpoint.Port}";
            _serverTask = ServeAsync();
        }

        public string BaseUrl { get; }

        public ConcurrentQueue<string> RequestPaths { get; } = new();

        public void Dispose()
        {
            _cancellation.Cancel();
            _listener.Stop();
            try { _serverTask.GetAwaiter().GetResult(); }
            catch (OperationCanceledException) { }
            _cancellation.Dispose();
        }

        private async Task ServeAsync()
        {
            while (!_cancellation.IsCancellationRequested)
            {
                TcpClient client;
                try
                {
                    client = await _listener.AcceptTcpClientAsync();
                }
                catch (ObjectDisposedException) when (_cancellation.IsCancellationRequested)
                {
                    break;
                }
                catch (SocketException) when (_cancellation.IsCancellationRequested)
                {
                    break;
                }
                await RespondAsync(client);
            }
        }

        private async Task RespondAsync(TcpClient client)
        {
            using (client)
            {
                var stream = client.GetStream();
                using var reader = new StreamReader(stream, Encoding.ASCII, false, 1024, leaveOpen: true);
                var requestLine = await reader.ReadLineAsync();
                string? header;
                do
                {
                    header = await reader.ReadLineAsync();
                }
                while (!string.IsNullOrEmpty(header));

                var path = requestLine?.Split(' ', StringSplitOptions.RemoveEmptyEntries).ElementAtOrDefault(1) ?? "";
                RequestPaths.Enqueue(path);
                var found = _responses.TryGetValue(path, out var body);
                body ??= Encoding.UTF8.GetBytes("Not found");
                var status = found ? "200 OK" : "404 Not Found";
                var responseHeaders = Encoding.ASCII.GetBytes(
                    $"HTTP/1.1 {status}\r\nContent-Length: {body.Length}\r\nConnection: close\r\n\r\n");
                await WriteBytesAsync(stream, responseHeaders);
                await WriteBytesAsync(stream, body);
            }
        }

        private static Task WriteBytesAsync(Stream stream, byte[] contents)
        {
#if NETFRAMEWORK
            return stream.WriteAsync(contents, 0, contents.Length);
#else
            return stream.WriteAsync(contents).AsTask();
#endif
        }
    }

    private sealed record BuildResult(int ExitCode, string StandardOutput, string StandardError, string CommandLine)
    {
        public bool Succeeded => ExitCode == 0;

        public string FailureMessage() =>
            $"{CommandLine}\nExitCode: {ExitCode}\n--- STDOUT ---\n{StandardOutput}\n--- STDERR ---\n{StandardError}";
    }
}
