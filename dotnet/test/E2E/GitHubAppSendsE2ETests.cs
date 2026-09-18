/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Microsoft.Extensions.AI;
using System.ComponentModel;
using System.Diagnostics;
using System.Text.Json;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class GitHubAppSendsE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "github_app_sends", output)
{
    private static readonly TimeSpan SendTimeout = TimeSpan.FromSeconds(60);

    [Fact]
    public async Task Should_Send_Complete_App_Message_Wire_Shape()
    {
        var (cliPath, capturePath) = await GitHubAppTestCli.CreateAsync(Ctx);
        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(
                path: cliPath,
                args: ["--capture-file", capturePath, "--behavior", "normal"]),
            UseLoggedInUser = false,
        });

        using var activity = new Activity("github-app-send");
        activity.SetIdFormat(ActivityIdFormat.W3C);
        activity.TraceStateString = "github-app=send";
        activity.Start();

        await using var session = await Ctx.CreateSessionAsync(client, new SessionConfig
        {
            Streaming = true,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var filePath = Path.Join(Ctx.WorkDir, "app-wire-file.txt");
        var directoryPath = Path.Join(Ctx.WorkDir, "app-wire-directory");
        var selectionPath = Path.Join(Ctx.WorkDir, "Program.cs");
        using var payload = JsonDocument.Parse("""{"selection":"APP_SELECTION","line":17}""");
        var messageId = await session.SendAsync(new MessageOptions
        {
            Prompt = "Use the hidden app context.",
            DisplayPrompt = "Review selected app context",
            Mode = "enqueue",
            AgentMode = AgentMode.Interactive,
            Source = MessageSource.Agent("github-app"),
            Attachments =
            [
                new AttachmentFile
                {
                    DisplayName = "app-wire-file.txt",
                    Path = filePath,
                    LineRange = new AttachmentFileLineRange { Start = 3, End = 9 },
                },
                new AttachmentDirectory
                {
                    DisplayName = "app-wire-directory",
                    Path = directoryPath,
                },
                new AttachmentSelection
                {
                    DisplayName = "Program.cs",
                    FilePath = selectionPath,
                    Text = "APP_SELECTION",
                    Selection = new AttachmentSelectionDetails
                    {
                        Start = new AttachmentSelectionDetailsStart { Line = 16, Character = 0 },
                        End = new AttachmentSelectionDetailsEnd { Line = 16, Character = 13 },
                    },
                },
                new AttachmentGitHubReference
                {
                    Number = 610,
                    ReferenceType = AttachmentGitHubReferenceType.Pr,
                    State = "open",
                    Title = "App-shaped E2E coverage",
                    Url = "https://github.com/github/copilot-sdk/pull/610",
                },
                new AttachmentBlob
                {
                    Data = "QVBQX0JMT0I=",
                    MimeType = "text/plain",
                    DisplayName = "app-wire-blob.txt",
                },
                new AttachmentExtensionContext
                {
                    CapturedAt = DateTimeOffset.Parse("2026-09-17T20:00:00Z"),
                    ExtensionId = "github-app:code-review",
                    CanvasId = "diff",
                    InstanceId = "diff-17",
                    Title = "Selected change",
                    Payload = payload.RootElement.Clone(),
                },
            ],
        });

        Assert.Equal("github-app-message", messageId);

        var requests = await GitHubAppTestCli.ReadRequestsAsync(capturePath);
        var send = Assert.Single(requests, request => request.GetProperty("method").GetString() == "session.send");
        var parameters = send.GetProperty("params");

        Assert.Equal("Use the hidden app context.", parameters.GetProperty("prompt").GetString());
        Assert.Equal("Review selected app context", parameters.GetProperty("displayPrompt").GetString());
        Assert.Equal("enqueue", parameters.GetProperty("mode").GetString());
        Assert.Equal("interactive", parameters.GetProperty("agentMode").GetString());
        Assert.Equal("agent-github-app", parameters.GetProperty("source").GetString());
        Assert.Equal(activity.Id, parameters.GetProperty("traceparent").GetString());
        Assert.Equal("github-app=send", parameters.GetProperty("tracestate").GetString());

        var attachments = parameters.GetProperty("attachments").EnumerateArray().ToArray();
        Assert.Equal(
            ["file", "directory", "selection", "github_reference", "blob", "extension_context"],
            attachments.Select(item => item.GetProperty("type").GetString()));
        Assert.Equal(filePath, attachments[0].GetProperty("path").GetString());
        Assert.Equal(3, attachments[0].GetProperty("lineRange").GetProperty("start").GetInt32());
        Assert.Equal(directoryPath, attachments[1].GetProperty("path").GetString());
        Assert.Equal("APP_SELECTION", attachments[2].GetProperty("text").GetString());
        Assert.Equal(selectionPath, attachments[2].GetProperty("filePath").GetString());
        Assert.Equal(610, attachments[3].GetProperty("number").GetInt32());
        Assert.Equal("pr", attachments[3].GetProperty("referenceType").GetString());
        Assert.Equal("QVBQX0JMT0I=", attachments[4].GetProperty("data").GetString());
        Assert.Equal("text/plain", attachments[4].GetProperty("mimeType").GetString());
        Assert.Equal("github-app:code-review", attachments[5].GetProperty("extensionId").GetString());
        Assert.Equal("APP_SELECTION", attachments[5].GetProperty("payload").GetProperty("selection").GetString());
    }

    [Fact]
    public async Task Should_Not_Invoke_Send_When_App_Cancels_Before_Dispatch()
    {
        var (cliPath, capturePath) = await GitHubAppTestCli.CreateAsync(Ctx);
        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(
                path: cliPath,
                args: ["--capture-file", capturePath, "--behavior", "normal"]),
            UseLoggedInUser = false,
        });
        await using var session = await Ctx.CreateSessionAsync(client, new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        using var cancellation = new CancellationTokenSource();
        cancellation.Cancel();

        await Assert.ThrowsAnyAsync<OperationCanceledException>(() =>
            session.SendAsync(
                new MessageOptions
                {
                    Prompt = "This message must never be invoked.",
                    DisplayPrompt = "Cancelled app message",
                    Source = MessageSource.Agent("github-app"),
                },
                cancellation.Token));

        var requests = await GitHubAppTestCli.ReadRequestsAsync(capturePath);
        Assert.DoesNotContain(requests, request => request.GetProperty("method").GetString() == "session.send");
    }

    [Fact]
    public async Task Should_Not_Replay_App_Send_After_Ambiguous_Transport_Loss()
    {
        var (cliPath, capturePath) = await GitHubAppTestCli.CreateAsync(Ctx);
        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(
                path: cliPath,
                args: ["--capture-file", capturePath, "--behavior", "drop-after-send"]),
            UseLoggedInUser = false,
        });
        await using var session = await Ctx.CreateSessionAsync(client, new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        using var cancellation = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        await Assert.ThrowsAnyAsync<Exception>(() =>
            session.SendAsync(new MessageOptions
            {
                Prompt = "AMBIGUOUS_APP_SEND",
                DisplayPrompt = "Ambiguous app send",
                Source = MessageSource.Agent("github-app"),
            }, cancellation.Token));

        var requests = await GitHubAppTestCli.ReadRequestsAsync(capturePath);
        Assert.Single(requests, request => request.GetProperty("method").GetString() == "session.send");
    }

    [Fact]
    public async Task Should_Order_Idle_Queued_And_Immediate_App_Delivery()
    {
        var firstToolStarted = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var secondToolStarted = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseFirstTool = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseSecondTool = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var toolInvocationCount = 0;
        var messages = new List<UserMessageEvent>();

        await using var session = await CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(BlockingTurn, "app_send_blocker")],
        });
        using var subscription = session.On<UserMessageEvent>(message =>
        {
            lock (messages)
            {
                messages.Add(message);
            }
        });

        var idleEnqueue = TestHelper.GetNextEventOfTypeAsync<SessionIdleEvent>(session, SendTimeout);
        var idleEnqueueId = await session.SendAsync(new MessageOptions
        {
            Prompt = "Reply with exactly IDLE_ENQUEUE.",
            Mode = "enqueue",
            Source = MessageSource.Agent("github-app"),
        });
        await idleEnqueue;

        var idleImmediate = TestHelper.GetNextEventOfTypeAsync<SessionIdleEvent>(session, SendTimeout);
        var idleImmediateId = await session.SendAsync(new MessageOptions
        {
            Prompt = "Reply with exactly IDLE_IMMEDIATE.",
            Mode = "immediate",
            Source = MessageSource.Agent("github-app"),
        });
        await idleImmediate;

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Call app_send_blocker, then reply with its result.",
            Source = MessageSource.Agent("github-app"),
        });
        await firstToolStarted.Task.WaitAsync(SendTimeout);

        var steeringId = await session.SendAsync(new MessageOptions
        {
            Prompt = "Call app_send_blocker again, then reply with exactly FIRST_STEERING.",
            Mode = "immediate",
            Source = MessageSource.Agent("github-app"),
        });
        releaseFirstTool.TrySetResult("APP_SEND_BLOCKER_RELEASED");
        await secondToolStarted.Task.WaitAsync(SendTimeout);

        var immediateBehindSteeringId = await session.SendAsync(new MessageOptions
        {
            Prompt = "Reply with exactly SECOND_IMMEDIATE.",
            Mode = "immediate",
            Source = MessageSource.Agent("github-app"),
        });
        var queuedId = await session.SendAsync(new MessageOptions
        {
            Prompt = "Reply with exactly FINAL_QUEUED.",
            Mode = "enqueue",
            Source = MessageSource.Agent("github-app"),
        });

        releaseSecondTool.TrySetResult("APP_SEND_BLOCKER_RELEASED_AGAIN");

        await TestHelper.WaitForConditionAsync(
            () =>
            {
                lock (messages)
                {
                    return Task.FromResult(
                        messages.Any(message => message.Data.MessageId == steeringId) &&
                        messages.Any(message => message.Data.MessageId == immediateBehindSteeringId) &&
                        messages.Any(message => message.Data.MessageId == queuedId));
                }
            },
            timeout: SendTimeout,
            timeoutMessage: "Timed out waiting for all app delivery classifications.");

        List<UserMessageEvent> observed;
        lock (messages)
        {
            observed = [.. messages];
        }

        Assert.Equal(UserMessageDelivery.Idle, Find(idleEnqueueId).Data.Delivery);
        Assert.Equal(UserMessageDelivery.Idle, Find(idleImmediateId).Data.Delivery);
        Assert.Equal(UserMessageDelivery.Steering, Find(steeringId).Data.Delivery);
        Assert.Equal(UserMessageDelivery.Steering, Find(immediateBehindSteeringId).Data.Delivery);
        Assert.Equal(UserMessageDelivery.Queued, Find(queuedId).Data.Delivery);

        var steeringIndex = observed.FindIndex(message => message.Data.MessageId == steeringId);
        var behindIndex = observed.FindIndex(message => message.Data.MessageId == immediateBehindSteeringId);
        var queuedIndex = observed.FindIndex(message => message.Data.MessageId == queuedId);
        Assert.True(steeringIndex < behindIndex, "The second immediate update must remain ordered behind the first steering update.");
        Assert.True(behindIndex < queuedIndex, "The second immediate message must retain its position ahead of the later enqueue.");

        UserMessageEvent Find(string id) =>
            Assert.Single(observed, message => string.Equals(message.Data.MessageId, id, StringComparison.Ordinal));

        [Description("Blocks an active app turn until delivery ordering is staged")]
        async Task<string> BlockingTurn(CancellationToken cancellationToken)
        {
            if (Interlocked.Increment(ref toolInvocationCount) == 1)
            {
                firstToolStarted.TrySetResult();
                return await releaseFirstTool.Task.WaitAsync(Timeout.InfiniteTimeSpan, cancellationToken);
            }

            secondToolStarted.TrySetResult();
            return await releaseSecondTool.Task.WaitAsync(Timeout.InfiniteTimeSpan, cancellationToken);
        }
    }
}
