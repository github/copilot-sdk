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
        session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = (request, invocation) =>
            {
                permissionRequests.Add(request);
                Assert.Equal(session!.SessionId, invocation.SessionId);
                return Task.FromResult(new PermissionRequestResult { Kind = PermissionRequestResultKind.Approved });
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
        var session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = (request, invocation) =>
            {
                return Task.FromResult(new PermissionRequestResult
                {
                    Kind = PermissionRequestResultKind.DeniedInteractivelyByUser
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
    public async Task Should_Deny_Tool_Operations_When_Handler_Explicitly_Denies()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = (_, _) =>
                Task.FromResult(new PermissionRequestResult { Kind = PermissionRequestResultKind.DeniedCouldNotRequestFromUser })
        });
        var permissionDenied = false;

        session.On(evt =>
        {
            if (evt is ToolExecutionCompleteEvent toolEvt &&
                !toolEvt.Data.Success &&
                toolEvt.Data.Error?.Message.Contains("Permission denied") == true)
            {
                permissionDenied = true;
            }
        });

        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Run 'node --version'"
        });

        Assert.True(permissionDenied, "Expected a tool.execution_complete event with Permission denied result");
    }

    [Fact]
    public async Task Should_Work_With_Approve_All_Permission_Handler()
    {
        var session = await CreateSessionAsync(new SessionConfig());

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
        var session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = async (request, invocation) =>
            {
                permissionRequestReceived = true;
                // Simulate async permission check
                await Task.Delay(10);
                return new PermissionRequestResult { Kind = PermissionRequestResultKind.Approved };
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
        var session1 = await CreateSessionAsync();
        var sessionId = session1.SessionId;
        await session1.SendAndWaitAsync(new MessageOptions { Prompt = "What is 1+1?" });

        // Resume with permission handler
        var session2 = await ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            OnPermissionRequest = (request, invocation) =>
            {
                permissionRequestReceived = true;
                return Task.FromResult(new PermissionRequestResult { Kind = PermissionRequestResultKind.Approved });
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
        var session = await CreateSessionAsync(new SessionConfig
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
    public async Task Should_Deny_Tool_Operations_When_Handler_Explicitly_Denies_After_Resume()
    {
        var session1 = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        });
        var sessionId = session1.SessionId;
        await session1.SendAndWaitAsync(new MessageOptions { Prompt = "What is 1+1?" });

        var session2 = await ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            OnPermissionRequest = (_, _) =>
                Task.FromResult(new PermissionRequestResult { Kind = PermissionRequestResultKind.DeniedCouldNotRequestFromUser })
        });
        var permissionDenied = false;

        session2.On(evt =>
        {
            if (evt is ToolExecutionCompleteEvent toolEvt &&
                !toolEvt.Data.Success &&
                toolEvt.Data.Error?.Message.Contains("Permission denied") == true)
            {
                permissionDenied = true;
            }
        });

        await session2.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Run 'node --version'"
        });

        Assert.True(permissionDenied, "Expected a tool.execution_complete event with Permission denied result");
    }

    [Fact]
    public async Task Should_Receive_ToolCallId_In_Permission_Requests()
    {
        var receivedToolCallId = false;
        var session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = (request, invocation) =>
            {
                if (request is PermissionRequestShell shell && !string.IsNullOrEmpty(shell.ToolCallId))
                {
                    receivedToolCallId = true;
                }
                return Task.FromResult(new PermissionRequestResult { Kind = PermissionRequestResultKind.Approved });
            }
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Run 'echo test'"
        });

        await TestHelper.GetFinalAssistantMessageAsync(session);

        Assert.True(receivedToolCallId, "Should have received toolCallId in permission request");
    }

    /// <summary>
    /// Regression test for issue #300: permission callback lost after session disposal.
    /// When session A is disposed but remains in the client's session map, a broadcast
    /// permission.requested event gets routed to the disposed session (which has a null
    /// handler) instead of the active session B. This causes the CLI to hang forever
    /// waiting for a permission decision that never arrives.
    ///
    /// The fix ensures DisposeAsync removes the session from the client map via OnDisposed,
    /// and adds a disposed guard in DispatchEvent to prevent stale sessions from receiving events.
    /// </summary>
    [Fact]
    public async Task Should_Handle_Permission_After_Prior_Session_Disposed()
    {
        var session1PermissionReceived = false;
        var session2PermissionReceived = false;

        // Create session A with a permission handler
        var session1 = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = (request, invocation) =>
            {
                session1PermissionReceived = true;
                return Task.FromResult(new PermissionRequestResult { Kind = PermissionRequestResultKind.Approved });
            }
        });

        // Send a simple non-tool prompt so session A is established
        await session1.SendAndWaitAsync(new MessageOptions { Prompt = "What is 1+1?" });

        // Dispose session A — this is the key step that triggers the bug.
        // Before the fix: session A stays in client._sessions with a null permission handler.
        // After the fix: OnDisposed removes it from the map, and DispatchEvent has a disposed guard.
        await session1.DisposeAsync();

        // Create session B with its own permission handler
        var session2 = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = (request, invocation) =>
            {
                session2PermissionReceived = true;
                return Task.FromResult(new PermissionRequestResult { Kind = PermissionRequestResultKind.Approved });
            }
        });

        // Send a prompt that requires a tool call (triggers permission.requested broadcast)
        // Before the fix: the broadcast hits disposed session A → null handler → silent drop → hang
        // After the fix: session A is not in the map; session B handles it correctly
        // Use a 15s timeout so the regression (infinite hang) fails fast instead of waiting 60s.
        await session2.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Run 'echo hello' for me"
        }, timeout: TimeSpan.FromSeconds(15));

        // Session B's handler should have been invoked
        Assert.True(session2PermissionReceived,
            "Session B's permission handler should fire after session A was disposed. " +
            "If this fails, the disposed session A is still in the client map and swallowing the broadcast.");

        // Session A's handler should NOT have been invoked (it was disposed)
        Assert.False(session1PermissionReceived,
            "Disposed session A should not receive permission events.");
    }
}
