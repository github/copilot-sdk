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
    private const string FileContent = "SDK rewind content";

    [Fact]
    public async Task Should_Restore_Tracked_File_And_Conversation()
    {
        var filePath = Path.Join(Ctx.WorkDir, FileName);
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            Model = "claude-sonnet-4.5",
            EnableFileChangeTracking = true,
        });

        var response = await session.SendAndWaitAsync(
            new MessageOptions
            {
                Prompt = $"Use the create tool to create {FileName} containing exactly {FileContent}. "
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
                    && rewindPoints.Points.Count == 1
                    && rewindPoints.Points[0].CanRestoreFiles
                    && rewindPoints.Points[0].FileCount == 1;
            },
            timeout: TimeSpan.FromSeconds(10),
            timeoutMessage: "Timed out waiting for a restorable file rewind point.",
            pollInterval: TimeSpan.FromMilliseconds(100));

        Assert.NotNull(rewindPoints);
        Assert.True(rewindPoints.FileChangeTrackingEnabled);
        var rewindPoint = Assert.Single(rewindPoints.Points);
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
        Assert.False(File.Exists(filePath));

        var events = await session.GetEventsAsync();
        Assert.DoesNotContain(events, sessionEvent => sessionEvent.Id.ToString() == rewindPoint.EventId);
    }
}
