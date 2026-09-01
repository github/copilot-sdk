// Copyright (c) GitHub, Inc.
// Licensed under the MIT License.

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;

using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class RewindE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rewind", output)
{
    private const string FileName = "rewind-sdk.txt";
    private const string OriginalFileContent = "Original rewind content";
    private const string PreparedFileContent = "Prepared rewind content";
    private const string FileContent = "SDK rewind content";

    [Fact]
    public async Task Should_Restore_Tracked_File_And_Conversation()
    {
        var filePath = Path.Join(Ctx.WorkDir, FileName);
        await File.WriteAllTextAsync(filePath, OriginalFileContent);
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            Model = "claude-sonnet-4.5",
            EnableFileChangeTracking = true,
        });

        var ready = await session.SendAndWaitAsync(
            new MessageOptions
            {
                Prompt = $"Use the edit tool to replace the exact contents of {FileName} "
                    + $"from {OriginalFileContent} to {PreparedFileContent}. "
                    + "After the tool succeeds, reply with exactly SDK_REWIND_READY.",
            },
            TimeSpan.FromSeconds(30));
        Assert.Equal("SDK_REWIND_READY", ready?.Data.Content);
        Assert.Equal(PreparedFileContent, await File.ReadAllTextAsync(filePath));

        var response = await session.SendAndWaitAsync(
            new MessageOptions
            {
                Prompt = $"Use the edit tool to replace the exact contents of {FileName} "
                    + $"from {PreparedFileContent} to {FileContent}. "
                    + "After the tool succeeds, reply with exactly SDK_REWIND_DONE.",
            },
            TimeSpan.FromSeconds(30));

        Assert.Equal("SDK_REWIND_DONE", response?.Data.Content);
        Assert.True(File.Exists(filePath));
        Assert.Equal(FileContent, await File.ReadAllTextAsync(filePath));

        HistoryListRewindPointsResult? rewindPoints = null;
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                rewindPoints = await session.Rpc.History.ListRewindPointsAsync();
                return rewindPoints.UnavailableReason is null
                    && rewindPoints.Points.Count == 2
                    && rewindPoints.Points[1].TurnChangedFiles
                    && rewindPoints.Points[1].CanRestoreFiles
                    && rewindPoints.Points[1].FileCount == 1;
            },
            timeout: TimeSpan.FromSeconds(30),
            timeoutMessage: "Timed out waiting for a restorable file rewind point.",
            pollInterval: TimeSpan.FromMilliseconds(100));

        Assert.NotNull(rewindPoints);
        Assert.True(rewindPoints.FileChangeTrackingEnabled);
        Assert.Equal(2, rewindPoints.Points.Count);
        var rewindPoint = rewindPoints.Points[1];
        Assert.True(rewindPoint.TurnChangedFiles);
        Assert.True(rewindPoint.CanRestoreFiles);
        Assert.Equal(1, rewindPoint.FileCount);

        var preview = await session.Rpc.History.PreviewRewindAsync(rewindPoint.EventId);
        Assert.True(preview.Available);
        var previewFile = Assert.Single(preview.Files);
        Assert.Equal(
            Path.GetFullPath(filePath),
            Path.GetFullPath(previewFile.Path),
            OperatingSystem.IsWindows() ? StringComparer.OrdinalIgnoreCase : StringComparer.Ordinal);

        var rewind = await session.Rpc.History.RewindAsync(
            rewindPoint.EventId,
            HistoryRewindMode.ConversationAndFiles);

        Assert.Equal(HistoryRewindOutcome.Success, rewind.Outcome);
        Assert.True(rewind.EventsRemoved > 0);
        var restoredFile = Assert.Single(rewind.RestoredFiles);
        Assert.Equal(
            Path.GetFullPath(filePath),
            Path.GetFullPath(restoredFile),
            OperatingSystem.IsWindows() ? StringComparer.OrdinalIgnoreCase : StringComparer.Ordinal);
        Assert.Equal(PreparedFileContent, await File.ReadAllTextAsync(filePath));

        var events = await session.GetEventsAsync();
        Assert.DoesNotContain(events, sessionEvent => sessionEvent.Id.ToString() == rewindPoint.EventId);
    }
}
