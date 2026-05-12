/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Rpc;
using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

/// <summary>
/// Targeted gap-filler tests for assorted RPC surface area where the previous suite covered
/// the happy path but missed boundary semantics: idempotent state transitions, empty-content
/// IO, no-op operations, and unicode round-trips. None of these tests depend on LLM replay.
/// </summary>
public class RpcAdditionalEdgeCasesE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_additional_edge_cases", output)
{
    [Fact]
    public async Task Shell_Exec_With_Zero_Timeout_Does_Not_Kill_Long_Running_Command()
    {
        // The runtime treats timeout > 0 as "schedule SIGTERM at deadline" (shellApi.ts).
        // timeout = 0 must mean "no timer at all" — the command should be allowed to
        // keep running long enough to write a marker, after which we kill it explicitly.
        var session = await CreateSessionAsync();
        var markerPath = Path.Join(Ctx.WorkDir, $"shell-zero-timeout-{Guid.NewGuid():N}.txt");
        var command = OperatingSystem.IsWindows()
            ? $"powershell -NoLogo -NoProfile -Command \"Start-Sleep -Milliseconds 500; Set-Content -LiteralPath '{markerPath}' -Value 'alive'; Start-Sleep -Seconds 60\""
            : $"sh -c \"sleep 0.5; printf alive > '{markerPath}'; sleep 60\"";

        var execResult = await session.Rpc.Shell.ExecAsync(command, cwd: Path.GetTempPath(), timeout: TimeSpan.Zero);
        Assert.False(string.IsNullOrWhiteSpace(execResult.ProcessId));

        await TestHelper.WaitForConditionAsync(
            () => File.Exists(markerPath),
            timeout: TimeSpan.FromSeconds(30),
            timeoutMessage: $"Timed out waiting for zero-timeout shell command to write marker to '{markerPath}'.");

        var killResult = await session.Rpc.Shell.KillAsync(execResult.ProcessId);
        Assert.True(killResult.Killed);
    }

    [Fact]
    public async Task Workspaces_CreateFile_With_Empty_Content_Round_Trips()
    {
        var session = await CreateSessionAsync();
        var path = $"empty-{Guid.NewGuid():N}.txt";

        await session.Rpc.Workspaces.CreateFileAsync(path, string.Empty);

        var read = await session.Rpc.Workspaces.ReadFileAsync(path);
        Assert.Equal(string.Empty, read.Content);

        var listed = await session.Rpc.Workspaces.ListFilesAsync();
        Assert.Contains(path, listed.Files);
    }

    [Fact]
    public async Task Workspaces_CreateFile_With_Unicode_Content_Round_Trips()
    {
        var session = await CreateSessionAsync();
        var path = $"unicode-{Guid.NewGuid():N}.txt";
        // Mix of BMP, supplementary plane (emoji), CJK, Cyrillic, and a NUL byte to stress the
        // string-only persistence path (workspace files are persisted as UTF-8 strings).
        var payload = "Hello, 世界! 🚀✨ Привет\u0000end";

        await session.Rpc.Workspaces.CreateFileAsync(path, payload);

        var read = await session.Rpc.Workspaces.ReadFileAsync(path);
        Assert.Equal(payload, read.Content);
    }

    [Fact]
    public async Task Workspaces_CreateFile_With_Large_Content_Round_Trips()
    {
        var session = await CreateSessionAsync();
        var path = $"large-{Guid.NewGuid():N}.txt";

        // 256KB of varied content stresses both the runtime's UTF-8 encoding path and the
        // JSON-RPC line-buffer path; small enough not to risk RPC size limits.
        var payload = string.Create(256 * 1024, (object?)null, static (span, _) =>
        {
            for (int i = 0; i < span.Length; i++)
            {
                span[i] = (char)('a' + (i % 26));
            }
        });

        await session.Rpc.Workspaces.CreateFileAsync(path, payload);

        var read = await session.Rpc.Workspaces.ReadFileAsync(path);
        Assert.Equal(payload.Length, read.Content.Length);
        Assert.Equal(payload, read.Content);
    }

    [Fact]
    public async Task Plan_Update_With_Empty_Content_Then_Read_Returns_Empty()
    {
        var session = await CreateSessionAsync();

        await session.Rpc.Plan.UpdateAsync(string.Empty);

        var read = await session.Rpc.Plan.ReadAsync();
        Assert.Equal(string.Empty, read.Content);
    }

    [Fact]
    public async Task Plan_Delete_When_None_Exists_Is_Idempotent()
    {
        var session = await CreateSessionAsync();

        // No prior plan — delete should succeed (no-op) and a subsequent read should still
        // return null/empty content rather than throwing.
        await session.Rpc.Plan.DeleteAsync();
        await session.Rpc.Plan.DeleteAsync();

        var read = await session.Rpc.Plan.ReadAsync();
        Assert.True(string.IsNullOrEmpty(read.Content));
    }

    [Fact]
    public async Task Mode_Set_To_Same_Value_Multiple_Times_Stays_Stable()
    {
        var session = await CreateSessionAsync();

        await session.Rpc.Mode.SetAsync(SessionMode.Plan);
        await session.Rpc.Mode.SetAsync(SessionMode.Plan);
        await session.Rpc.Mode.SetAsync(SessionMode.Plan);

        Assert.Equal(SessionMode.Plan, await session.Rpc.Mode.GetAsync());
    }

    [Fact]
    public async Task Name_Set_With_Unicode_Round_Trips()
    {
        var session = await CreateSessionAsync();
        const string name = "セッション 名前 ☕ – test";

        await session.Rpc.Name.SetAsync(name);

        var read = await session.Rpc.Name.GetAsync();
        Assert.Equal(name, read.Name);
    }

    [Fact]
    public async Task Usage_GetMetrics_On_Fresh_Session_Returns_Zero_Tokens()
    {
        var session = await CreateSessionAsync();

        var metrics = await session.Rpc.Usage.GetMetricsAsync();

        // Fresh session = no LLM calls yet. Last-call counters and the user-request count
        // must be zero, and SessionStartTime must be a positive epoch (set at create-time).
        Assert.Equal(0, metrics.LastCallInputTokens);
        Assert.Equal(0, metrics.LastCallOutputTokens);
        Assert.Equal(0, metrics.TotalUserRequests);
        Assert.True(metrics.SessionStartTime > 0, "SessionStartTime should be a positive epoch.");
    }

    [Fact]
    public async Task Permissions_ResetSessionApprovals_On_Fresh_Session_Is_Noop()
    {
        var session = await CreateSessionAsync();

        // No prior approvals to reset; should succeed without throwing.
        var result = await session.Rpc.Permissions.ResetSessionApprovalsAsync();

        Assert.True(result.Success);
    }

    [Fact]
    public async Task Permissions_SetApproveAll_Toggle_Round_Trips()
    {
        var session = await CreateSessionAsync();

        var first = await session.Rpc.Permissions.SetApproveAllAsync(true);
        Assert.True(first.Success);

        var second = await session.Rpc.Permissions.SetApproveAllAsync(true);
        Assert.True(second.Success);

        var third = await session.Rpc.Permissions.SetApproveAllAsync(false);
        Assert.True(third.Success);

        var fourth = await session.Rpc.Permissions.SetApproveAllAsync(false);
        Assert.True(fourth.Success);
    }

    [Fact]
    public async Task Workspaces_CreateFile_Then_ListFiles_Returns_All_Files()
    {
        var session = await CreateSessionAsync();
        var prefix = $"order-{Guid.NewGuid():N}-";

        var paths = Enumerable.Range(0, 5).Select(i => $"{prefix}{i:D2}.txt").ToList();
        foreach (var p in paths)
        {
            await session.Rpc.Workspaces.CreateFileAsync(p, $"content-{p}");
        }

        var listed = await session.Rpc.Workspaces.ListFilesAsync();
        var matchingFiles = listed.Files
            .Where(path => path.StartsWith(prefix, StringComparison.Ordinal))
            .ToList();

        // The files this test created should all be returned; the runtime does not guarantee
        // that workspace file enumeration is sorted.
        Assert.Equal(paths, matchingFiles.OrderBy(path => path, StringComparer.Ordinal));

        // A repeated list should still include the files regardless of returned order.
        var listed2 = await session.Rpc.Workspaces.ListFilesAsync();
        var matchingFiles2 = listed2.Files
            .Where(path => path.StartsWith(prefix, StringComparison.Ordinal))
            .ToList();
        Assert.Equal(paths, matchingFiles2.OrderBy(path => path, StringComparer.Ordinal));
    }

    [Fact]
    public async Task Workspaces_GetWorkspace_Returns_Stable_Result_Across_Calls()
    {
        var session = await CreateSessionAsync();

        var first = await session.Rpc.Workspaces.GetWorkspaceAsync();
        var second = await session.Rpc.Workspaces.GetWorkspaceAsync();

        // GetWorkspace is a pure getter. The two calls must return semantically equal results.
        // Even if the underlying implementation returns a fresh object each time, the JSON
        // shape should round-trip identically.
        Assert.Equal(first.Workspace?.Cwd, second.Workspace?.Cwd);
        Assert.Equal(first.Workspace?.Id, second.Workspace?.Id);
    }
}
