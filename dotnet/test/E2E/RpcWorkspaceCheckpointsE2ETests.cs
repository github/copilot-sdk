/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text;
using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class RpcWorkspaceCheckpointsE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_workspace_checkpoints", output)
{
    [Fact]
    public async Task Should_List_No_Checkpoints_For_Fresh_Session()
    {
        await using var session = await CreateSessionAsync();

        var result = await session.Rpc.Workspaces.ListCheckpointsAsync();

        Assert.NotNull(result.Checkpoints);
        Assert.Empty(result.Checkpoints);
    }

    [Fact]
    public async Task Should_Return_Null_Or_Empty_Content_For_Unknown_Checkpoint()
    {
        await using var session = await CreateSessionAsync();

        var result = await session.Rpc.Workspaces.ReadCheckpointAsync(long.MaxValue);

        Assert.True(string.IsNullOrEmpty(result.Content));
    }

    [Fact]
    public async Task Should_Return_Typed_Workspace_Diff_Result()
    {
        await using var session = await CreateSessionAsync();

        var result = await session.Rpc.Workspaces.DiffAsync(WorkspaceDiffMode.Unstaged);

        Assert.Equal(WorkspaceDiffMode.Unstaged, result.RequestedMode);
        Assert.Contains(result.Mode, new[] { WorkspaceDiffMode.Unstaged, WorkspaceDiffMode.Branch });
        Assert.NotNull(result.Changes);
        foreach (var change in result.Changes)
        {
            Assert.NotEmpty(change.Path);
            Assert.Contains(
                change.ChangeType,
                new[]
                {
                    WorkspaceDiffFileChangeType.Added,
                    WorkspaceDiffFileChangeType.Modified,
                    WorkspaceDiffFileChangeType.Deleted,
                    WorkspaceDiffFileChangeType.Renamed,
                });
            Assert.NotNull(change.Diff);
        }
    }

    [Fact]
    public async Task Should_Save_Large_Paste_And_Expose_Readable_Content()
    {
        await using var session = await CreateSessionAsync();
        var content = string.Concat(Enumerable.Repeat("Large paste payload 🚀\n", 512));

        var result = await session.Rpc.Workspaces.SaveLargePasteAsync(content);
        var saved = result.Saved;

        Assert.NotNull(saved);
        Assert.NotEmpty(saved.Filename);
        Assert.NotEmpty(saved.FilePath);
        Assert.Equal(Encoding.UTF8.GetByteCount(content), saved.SizeBytes);

        WorkspacesReadFileResult? read = null;
        Exception? readError = null;
        try
        {
            read = await session.Rpc.Workspaces.ReadFileAsync(saved.Filename);
        }
        catch (Exception ex)
        {
            readError = ex;
        }

        if (read is not null)
        {
            Assert.Equal(content, read.Content);
        }
        else
        {
            Assert.True(
                File.Exists(saved.FilePath),
                $"Saved paste file does not exist: {saved.FilePath}. ReadFile failed: {readError}");
            Assert.Equal(content, File.ReadAllText(saved.FilePath));
        }
    }
}
