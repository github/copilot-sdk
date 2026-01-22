/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class PermissionTests(E2ETestFixture fixture, ITestOutputHelper output) : E2ETestBase(fixture, "permissions", output)
{
    [Fact]
    public async Task Should_Invoke_Permission_Handler_For_Write_Operations()
    {
        var permissionRequests = new List<PermissionRequest>();
        CopilotSession? session = null;
        session = await Client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = (request, invocation) =>
            {
                permissionRequests.Add(request);
                Assert.Equal(session!.SessionId, invocation.SessionId);
                return Task.FromResult(new PermissionRequestResult { Kind = "approved" });
            }
        });

        await File.WriteAllTextAsync(Path.Combine(Ctx.WorkDir, "test.txt"), "original content");

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Edit test.txt and replace 'original' with 'modified'"
        });

        await TestHelper.GetFinalAssistantMessageAsync(session);

        // Should have received at least one permission request
        Assert.NotEmpty(permissionRequests);

        // Should include write permission request
        Assert.Contains(permissionRequests, r => r.Kind == "write");
    }

    [Fact]
    public async Task Should_Deny_Permission_When_Handler_Returns_Denied()
    {
        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = (request, invocation) =>
            {
                return Task.FromResult(new PermissionRequestResult
                {
                    Kind = "denied-interactively-by-user"
                });
            }
        });

        var testFilePath = Path.Combine(Ctx.WorkDir, "protected.txt");
        await File.WriteAllTextAsync(testFilePath, "protected content");

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Edit protected.txt and replace 'protected' with 'hacked'."
        });

        await TestHelper.GetFinalAssistantMessageAsync(session);

        // Verify the file was NOT modified
        var content = await File.ReadAllTextAsync(testFilePath);
        Assert.Equal("protected content", content);
    }

    [Fact]
    public async Task Should_Work_Without_Permission_Handler__Default_Behavior_()
    {
        // Create session without permission handler
        var session = await Client.CreateSessionAsync(new SessionConfig());

        await session.SendAsync(new MessageOptions
        {
            Prompt = "What is 2+2?"
        });

        var message = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.Contains("4", message?.Data.Content ?? string.Empty);
    }

    [Fact]
    public async Task Should_Handle_Async_Permission_Handler()
    {
        var permissionRequestReceived = false;
        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = async (request, invocation) =>
            {
                permissionRequestReceived = true;
                // Simulate async permission check
                await Task.Delay(10);
                return new PermissionRequestResult { Kind = "approved" };
            }
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Run 'echo test' and tell me what happens"
        });

        await TestHelper.GetFinalAssistantMessageAsync(session);

        Assert.True(permissionRequestReceived, "Permission request should have been received");
    }

    [Fact]
    public async Task Should_Resume_Session_With_Permission_Handler()
    {
        var permissionRequestReceived = false;

        // Create session without permission handler
        var session1 = await Client.CreateSessionAsync();
        var sessionId = session1.SessionId;
        await session1.SendAndWaitAsync(new MessageOptions { Prompt = "What is 1+1?" });

        // Resume with permission handler
        var session2 = await Client.ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            OnPermissionRequest = (request, invocation) =>
            {
                permissionRequestReceived = true;
                return Task.FromResult(new PermissionRequestResult { Kind = "approved" });
            }
        });

        await session2.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Run 'echo resumed' for me"
        });

        Assert.True(permissionRequestReceived, "Permission request should have been received");
    }

    [Fact]
    public async Task Should_Handle_Permission_Handler_Errors_Gracefully()
    {
        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = (request, invocation) =>
            {
                // Simulate an error in the handler
                throw new InvalidOperationException("Handler error");
            }
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Run 'echo test'. If you can't, say 'failed'."
        });

        var message = await TestHelper.GetFinalAssistantMessageAsync(session);

        // Should handle the error and deny permission
        Assert.Matches("fail|cannot|unable|permission", message?.Data.Content?.ToLowerInvariant() ?? string.Empty);
    }

    [Fact]
    public async Task Should_Receive_ToolCallId_In_Permission_Requests()
    {
        var receivedToolCallId = false;
        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = (request, invocation) =>
            {
                if (!string.IsNullOrEmpty(request.ToolCallId))
                {
                    receivedToolCallId = true;
                }
                return Task.FromResult(new PermissionRequestResult { Kind = "approved" });
            }
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Run 'echo test'"
        });

        await TestHelper.GetFinalAssistantMessageAsync(session);

        Assert.True(receivedToolCallId, "Should have received toolCallId in permission request");
    }
}
