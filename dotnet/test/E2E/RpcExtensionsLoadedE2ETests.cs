/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Rpc;
using GitHub.Copilot.SDK.Test.Harness;
using System.Diagnostics;
using Xunit;
using Xunit.Abstractions;
using RpcExtension = GitHub.Copilot.SDK.Rpc.Extension;

namespace GitHub.Copilot.SDK.Test.E2E;

/// <summary>
/// E2E coverage for the loaded-extensions code path in the runtime: when the
/// experimental EXTENSIONS feature flag is enabled and a session is created
/// with EnableConfigDiscovery=true, the runtime discovers user/project
/// extensions from disk, forks each one as a subprocess, and exposes
/// session.Rpc.Extensions.{List,Enable,Disable,Reload}.
///
/// The "controller absent" path is already covered by
/// <c>RpcMcpAndSkillsE2ETests.Should_Report_Error_When_Extensions_Are_Not_Available</c>;
/// these tests cover the controller-present path.
/// </summary>
public class RpcExtensionsLoadedE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_extensions_loaded", output)
{
    /// <summary>
    /// Extension subprocess startup involves Node fork + SDK resolver + JSON-RPC
    /// handshake. Empirically this completes in well under a second on Windows,
    /// but the runtime's READY_TIMEOUT_MS is 30s, so we use the same upper bound
    /// to keep the test bulletproof on cold starts.
    /// </summary>
    private static readonly TimeSpan ExtensionStartupTimeout = TimeSpan.FromSeconds(45);

    /// <summary>
    /// Builds an environment dict that opts the runtime into the experimental
    /// EXTENSIONS feature flag while preserving every other harness-managed
    /// var (COPILOT_API_URL, COPILOT_HOME, NODE_V8_COVERAGE, etc).
    /// </summary>
    private Dictionary<string, string> ExtensionsEnabledEnvironment()
    {
        var env = new Dictionary<string, string>(Ctx.GetEnvironment())
        {
            ["COPILOT_CLI_ENABLED_FEATURE_FLAGS"] = "EXTENSIONS",
        };
        return env;
    }

    /// <summary>
    /// Creates a client with the EXTENSIONS feature flag and --yolo CLI arg.
    /// --yolo auto-approves extension permission gates at the CLI level,
    /// preventing tests from breaking when new permission gates are added
    /// (e.g., extension-permission-access from copilot-agent-runtime#6024).
    /// </summary>
    private CopilotClient CreateExtensionsClient()
    {
        return Ctx.CreateClient(options: new CopilotClientOptions
        {
            CliArgs = ["--yolo"],
            Environment = ExtensionsEnabledEnvironment(),
        });
    }

    /// <summary>
    /// Writes a minimal user extension into <c>{HomeDir}/extensions/{name}/extension.mjs</c>.
    /// The body imports <c>@github/copilot-sdk/extension</c>, calls <c>joinSession</c>
    /// to establish the JSON-RPC handshake (so the extension transitions from
    /// "starting" → "running" quickly), and then keeps the process alive.
    /// Returns the unique extension name.
    /// </summary>
    private string CreateUserExtension(string? prefix = null)
    {
        var extName = Path.GetFileName($"{prefix ?? "test-ext"}-{Guid.NewGuid():N}");
        var extDir = Path.Join(Ctx.HomeDir, "extensions", extName);
        WriteRunningExtension(extDir);
        return extName;
    }

    private async Task<(string Name, string Id, string WorkingDirectory)> CreateProjectExtensionAsync(string? prefix = null)
    {
        var extName = Path.GetFileName($"{prefix ?? "project-ext"}-{Guid.NewGuid():N}");
        var projectDirName = Path.GetFileName($"extension-project-{Guid.NewGuid():N}");
        var projectDir = Path.Join(Ctx.WorkDir, projectDirName);
        Directory.CreateDirectory(projectDir);
        await InitializeGitRepositoryAsync(projectDir);

        var extDir = Path.Join(projectDir, ".github", "extensions", extName);
        WriteRunningExtension(extDir);
        return (extName, $"project:{extName}", projectDir);
    }

    private static void WriteRunningExtension(string extDir)
    {
        Directory.CreateDirectory(extDir);

        var body = """
            import { joinSession } from "@github/copilot-sdk/extension";

            // Establish the JSON-RPC handshake so the runtime sees us as ready.
            await joinSession({});

            // Keep the process alive so the runtime doesn't reap us as exited.
            // The unref() ensures we still exit when the parent disconnects.
            setInterval(() => {}, 60_000).unref?.();
            """;

        File.WriteAllText(Path.Join(extDir, "extension.mjs"), body);
    }

    private static async Task InitializeGitRepositoryAsync(string projectDir)
    {
        using var process = new Process
        {
            StartInfo = new ProcessStartInfo("git")
            {
                WorkingDirectory = projectDir,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
            }
        };
        process.StartInfo.ArgumentList.Add("init");
        process.StartInfo.ArgumentList.Add("-q");

        if (!process.Start())
        {
            throw new InvalidOperationException("Failed to start git init.");
        }

        await process.WaitForExitAsync();
        if (process.ExitCode != 0)
        {
            var stderr = await process.StandardError.ReadToEndAsync();
            throw new InvalidOperationException($"git init failed with exit code {process.ExitCode}: {stderr}");
        }
    }

    /// <summary>
    /// Polls <c>session.Rpc.Extensions.ListAsync()</c> until the controller
    /// becomes available AND the named extension reaches a terminal status
    /// (running, failed, or disabled). The controller is set asynchronously
    /// after session create returns, and list calls can report an empty list
    /// until setup finishes.
    /// </summary>
    private static async Task<RpcExtension> WaitForExtensionAsync(
        CopilotSession session,
        string extensionId,
        ExtensionStatus expectedStatus,
        TimeSpan? timeout = null)
    {
        RpcExtension? lastSeen = null;
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                var list = await session.Rpc.Extensions.ListAsync();
                lastSeen = list.Extensions.FirstOrDefault(e => string.Equals(e.Id, extensionId, StringComparison.Ordinal));
                return lastSeen != null && lastSeen.Status == expectedStatus;
            },
            timeout: timeout ?? ExtensionStartupTimeout,
            timeoutMessage: $"Extension '{extensionId}' did not reach status '{expectedStatus}' (last seen: {lastSeen?.Status.ToString() ?? "<not present>"}).",
            transientExceptionFilter: ex => ex.ToString().Contains("Extensions not available", StringComparison.OrdinalIgnoreCase),
            pollInterval: TimeSpan.FromMilliseconds(100));

        return lastSeen!;
    }

    [Theory]
    [InlineData(ExtensionSource.User)]
    [InlineData(ExtensionSource.Project)]
    public async Task Discovers_Loads_And_Reports_Running_Extension(ExtensionSource source)
    {
        string extName;
        string extId;
        string? workingDirectory;
        if (source == ExtensionSource.User)
        {
            extName = CreateUserExtension();
            extId = $"user:{extName}";
            workingDirectory = null;
        }
        else if (source == ExtensionSource.Project)
        {
            (extName, extId, workingDirectory) = await CreateProjectExtensionAsync();
        }
        else
        {
            throw new ArgumentOutOfRangeException(nameof(source), source, null);
        }

        await using var client = CreateExtensionsClient();

        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            EnableConfigDiscovery = true,
            WorkingDirectory = workingDirectory,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var ext = await WaitForExtensionAsync(session, extId, ExtensionStatus.Running);

        Assert.Equal(extId, ext.Id);
        Assert.Equal(extName, ext.Name);
        Assert.Equal(source, ext.Source);
        Assert.Equal(ExtensionStatus.Running, ext.Status);
        Assert.NotNull(ext.Pid);
        Assert.True(ext.Pid > 0);
    }

    [Fact]
    public async Task Disable_Then_Enable_Cycles_Extension_Status()
    {
        var extName = CreateUserExtension();
        var extId = $"user:{extName}";

        await using var client = CreateExtensionsClient();

        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            EnableConfigDiscovery = true,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        // Wait until the initial running state is observed before mutating.
        await WaitForExtensionAsync(session, extId, ExtensionStatus.Running);

        // Disable: the extension should transition to "disabled" and have no pid.
        await session.Rpc.Extensions.DisableAsync(extId);
        var disabled = await WaitForExtensionAsync(session, extId, ExtensionStatus.Disabled);
        Assert.Null(disabled.Pid);

        // Re-enable: the extension is reloaded as a fresh subprocess.
        await session.Rpc.Extensions.EnableAsync(extId);
        var reEnabled = await WaitForExtensionAsync(session, extId, ExtensionStatus.Running);
        Assert.NotNull(reEnabled.Pid);
    }

    [Fact]
    public async Task Reload_Picks_Up_Extension_Added_After_Session_Create()
    {
        // Start the session BEFORE writing the extension so the initial discovery sees nothing.
        await using var client = CreateExtensionsClient();

        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            EnableConfigDiscovery = true,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        // setupExtensionsForSession runs asynchronously; until it completes the
        // controller isn't installed and ReloadAsync throws "Extensions not
        // available". (ListAsync returns {extensions: []} either way and is
        // therefore not a usable probe here.) Poll Reload directly.
        var extName = CreateUserExtension(prefix: "reloadable-ext");
        var extId = $"user:{extName}";

        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                await session.Rpc.Extensions.ReloadAsync();
                return true;
            },
            timeout: ExtensionStartupTimeout,
            timeoutMessage: "Extensions controller never became available for ReloadAsync.",
            transientExceptionFilter: ex => ex.ToString().Contains("Extensions not available", StringComparison.OrdinalIgnoreCase),
            pollInterval: TimeSpan.FromMilliseconds(100));

        var ext = await WaitForExtensionAsync(session, extId, ExtensionStatus.Running);
        Assert.Equal(ExtensionSource.User, ext.Source);
    }

    [Fact]
    public async Task Failed_Extension_Reports_Failed_Status()
    {
        // Write an extension whose body throws synchronously at import time.
        // The bootstrap will fork the child, the import will throw, the child
        // exits with code 1, and the runtime should mark it as "failed".
        var extName = $"crashing-ext-{Guid.NewGuid():N}";
        var extDir = Path.Join(Ctx.HomeDir, "extensions", extName);
        Directory.CreateDirectory(extDir);
        File.WriteAllText(
            Path.Join(extDir, "extension.mjs"),
            "throw new Error('intentional startup failure');");

        var extId = $"user:{extName}";

        await using var client = CreateExtensionsClient();

        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            EnableConfigDiscovery = true,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var ext = await WaitForExtensionAsync(session, extId, ExtensionStatus.Failed);
        Assert.Equal(extId, ext.Id);
        Assert.Equal(ExtensionSource.User, ext.Source);
    }

    [Fact]
    public async Task Multiple_Extensions_Are_Discovered_Independently()
    {
        var ext1Name = CreateUserExtension(prefix: "multi-a");
        var ext2Name = CreateUserExtension(prefix: "multi-b");
        var ext1Id = $"user:{ext1Name}";
        var ext2Id = $"user:{ext2Name}";

        await using var client = CreateExtensionsClient();

        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            EnableConfigDiscovery = true,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        await WaitForExtensionAsync(session, ext1Id, ExtensionStatus.Running);
        await WaitForExtensionAsync(session, ext2Id, ExtensionStatus.Running);

        var list = await session.Rpc.Extensions.ListAsync();
        var pids = list.Extensions.Select(e => e.Pid).Where(p => p.HasValue).ToList();
        Assert.Equal(pids.Count, pids.Distinct().Count());
    }

    [Fact]
    public async Task Reload_Preserves_Disabled_State_Across_Calls()
    {
        var extName = CreateUserExtension(prefix: "persistent-disable");
        var extId = $"user:{extName}";

        await using var client = CreateExtensionsClient();

        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            EnableConfigDiscovery = true,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        await WaitForExtensionAsync(session, extId, ExtensionStatus.Running);

        await session.Rpc.Extensions.DisableAsync(extId);
        await WaitForExtensionAsync(session, extId, ExtensionStatus.Disabled);

        // Reload re-runs discovery and respects the per-session disabled set,
        // so the extension stays disabled and is not re-launched.
        await session.Rpc.Extensions.ReloadAsync();

        var afterReload = await WaitForExtensionAsync(session, extId, ExtensionStatus.Disabled);
        Assert.Null(afterReload.Pid);
    }
}
