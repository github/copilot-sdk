/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Microsoft.Extensions.AI;
using System.Text.Json;
using System.Text.Json.Serialization;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

public partial class PermissionE2ETests(E2ETestFixture fixture, ITestOutputHelper output) : E2ETestBase(fixture, "permissions", output)
{
    [JsonSourceGenerationOptions(JsonSerializerDefaults.Web)]
    [JsonSerializable(typeof(ToolResultAIContent))]
    [JsonSerializable(typeof(ToolResultObject))]
    private partial class PermissionJsonContext : JsonSerializerContext;

    [Fact]
    public async Task Should_Invoke_Permission_Handler_For_Write_Operations()
    {
        var permissionRequests = new List<PermissionRequest>();
        var permissionRequestsLock = new object();
        var readPermissionRequestReceived = new TaskCompletionSource<PermissionRequestRead>(TaskCreationOptions.RunContinuationsAsynchronously);
        var writePermissionRequestReceived = new TaskCompletionSource<PermissionRequestWrite>(TaskCreationOptions.RunContinuationsAsynchronously);
        CopilotSession? session = null;
        session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = (request, invocation) =>
            {
                lock (permissionRequestsLock)
                {
                    permissionRequests.Add(request);
                }
                Assert.Equal(session!.SessionId, invocation.SessionId);
                if (request is PermissionRequestRead readRequest)
                {
                    readPermissionRequestReceived.TrySetResult(readRequest);
                }
                else if (request is PermissionRequestWrite writeRequest)
                {
                    writePermissionRequestReceived.TrySetResult(writeRequest);
                }
                return Task.FromResult(new PermissionRequestResult { Kind = PermissionRequestResultKind.Approved });
            }
        });

        await File.WriteAllTextAsync(Path.Combine(Ctx.WorkDir, "test.txt"), "original content");

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Edit test.txt and replace 'original' with 'modified'"
        });

        var readRequest = await readPermissionRequestReceived.Task.WaitAsync(TimeSpan.FromSeconds(30));
        var writeRequest = await writePermissionRequestReceived.Task.WaitAsync(TimeSpan.FromSeconds(30));
        await TestHelper.GetFinalAssistantMessageAsync(session);

        List<PermissionRequest> observedPermissionRequests;
        lock (permissionRequestsLock)
        {
            observedPermissionRequests = [.. permissionRequests];
        }

        Assert.NotEmpty(observedPermissionRequests);
        Assert.EndsWith("test.txt", readRequest.Path, StringComparison.Ordinal);
        Assert.Contains("test.txt", readRequest.Intention, StringComparison.OrdinalIgnoreCase);
        Assert.False(string.IsNullOrWhiteSpace(readRequest.ToolCallId));

        Assert.Contains(observedPermissionRequests, request => request is PermissionRequestWrite);
        Assert.EndsWith("test.txt", writeRequest.FileName, StringComparison.Ordinal);
        Assert.Contains("original content", writeRequest.Diff, StringComparison.Ordinal);
        Assert.Contains("modified content", writeRequest.Diff, StringComparison.Ordinal);
        Assert.False(string.IsNullOrWhiteSpace(writeRequest.ToolCallId));

        var updatedContent = await File.ReadAllTextAsync(Path.Join(Ctx.WorkDir, "test.txt"));
        Assert.Equal("modified content", updatedContent);
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
                    Kind = PermissionRequestResultKind.Rejected
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
                Task.FromResult(new PermissionRequestResult { Kind = PermissionRequestResultKind.UserNotAvailable })
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
                await Task.Yield();
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
                Task.FromResult(new PermissionRequestResult { Kind = PermissionRequestResultKind.UserNotAvailable })
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

    [Fact]
    public async Task Should_Wait_For_Slow_Permission_Handler()
    {
        var handlerEntered = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseHandler = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var targetToolCallId = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var lifecycle = new List<(string Phase, string? ToolCallId)>();
        var lifecycleLock = new object();

        void AddLifecycleEvent(string phase, string? toolCallId)
        {
            lock (lifecycleLock)
            {
                lifecycle.Add((phase, toolCallId));
            }
        }

        var session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = async (request, invocation) =>
            {
                var shellRequest = Assert.IsType<PermissionRequestShell>(request);
                Assert.False(string.IsNullOrWhiteSpace(shellRequest.ToolCallId));

                AddLifecycleEvent("permission-start", shellRequest.ToolCallId);
                targetToolCallId.TrySetResult(shellRequest.ToolCallId!);
                handlerEntered.TrySetResult();
                await releaseHandler.Task.WaitAsync(TimeSpan.FromSeconds(30));
                AddLifecycleEvent("permission-complete", shellRequest.ToolCallId);
                return new PermissionRequestResult { Kind = PermissionRequestResultKind.Approved };
            }
        });

        using var subscription = session.On(evt =>
        {
            switch (evt)
            {
                case ToolExecutionStartEvent started:
                    AddLifecycleEvent("tool-start", started.Data.ToolCallId);
                    break;
                case ToolExecutionCompleteEvent completed:
                    AddLifecycleEvent("tool-complete", completed.Data.ToolCallId);
                    break;
            }
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Run 'echo slow_handler_test'"
        });

        await handlerEntered.Task.WaitAsync(TimeSpan.FromSeconds(30));
        var targetToolId = await targetToolCallId.Task.WaitAsync(TimeSpan.FromSeconds(30));
        lock (lifecycleLock)
        {
            Assert.DoesNotContain(lifecycle, evt => evt.Phase == "tool-complete" && evt.ToolCallId == targetToolId);
        }

        releaseHandler.SetResult();

        var message = await TestHelper.GetFinalAssistantMessageAsync(session);

        List<(string Phase, string? ToolCallId)> orderedLifecycle;
        lock (lifecycleLock)
        {
            orderedLifecycle = [.. lifecycle];
        }

        var permissionStartIndex = orderedLifecycle.FindIndex(evt => evt.Phase == "permission-start" && evt.ToolCallId == targetToolId);
        var permissionCompleteIndex = orderedLifecycle.FindIndex(evt => evt.Phase == "permission-complete" && evt.ToolCallId == targetToolId);
        var toolStartIndex = orderedLifecycle.FindIndex(evt => evt.Phase == "tool-start" && evt.ToolCallId == targetToolId);
        var toolCompleteIndex = orderedLifecycle.FindIndex(evt => evt.Phase == "tool-complete" && evt.ToolCallId == targetToolId);
        var observedLifecycle = string.Join(", ", orderedLifecycle.Select(evt => $"{evt.Phase}:{evt.ToolCallId}"));

        Assert.InRange(permissionStartIndex, 0, orderedLifecycle.Count - 1);
        Assert.InRange(permissionCompleteIndex, 0, orderedLifecycle.Count - 1);
        Assert.InRange(toolStartIndex, 0, orderedLifecycle.Count - 1);
        Assert.InRange(toolCompleteIndex, 0, orderedLifecycle.Count - 1);
        Assert.True(
            permissionCompleteIndex < toolCompleteIndex,
            $"Expected permission completion before target tool completion. Observed: {observedLifecycle}");
        Assert.True(
            toolStartIndex < toolCompleteIndex,
            $"Expected target tool start before target tool completion. Observed: {observedLifecycle}");

        // The tool should have actually run after permission was granted
        Assert.Contains("slow_handler_test", message?.Data.Content ?? string.Empty);
    }

    [Fact]
    public async Task Should_Handle_Concurrent_Permission_Requests_From_Parallel_Tools()
    {
        var permissionRequestCount = 0;
        var permissionRequests = new List<PermissionRequest>();
        var permissionRequestsLock = new object();
        var bothPermissionRequestsStarted = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var firstToolCompleted = new TaskCompletionSource<ToolExecutionCompleteEvent>(TaskCreationOptions.RunContinuationsAsynchronously);
        var secondToolCompleted = new TaskCompletionSource<ToolExecutionCompleteEvent>(TaskCreationOptions.RunContinuationsAsynchronously);
        var firstToolCalled = false;
        var secondToolCalled = false;

        var session = await CreateSessionAsync(new SessionConfig
        {
            Tools =
            [
                AIFunctionFactory.Create(
                    FirstPermissionTool,
                    "first_permission_tool",
                    "First concurrent permission test tool",
                    serializerOptions: PermissionJsonContext.Default.Options),
                AIFunctionFactory.Create(
                    SecondPermissionTool,
                    "second_permission_tool",
                    "Second concurrent permission test tool",
                    serializerOptions: PermissionJsonContext.Default.Options),
            ],
            AvailableTools = ["first_permission_tool", "second_permission_tool"],
            OnPermissionRequest = async (request, invocation) =>
            {
                var count = Interlocked.Increment(ref permissionRequestCount);
                lock (permissionRequestsLock) { permissionRequests.Add(request); }
                if (count >= 2)
                {
                    bothPermissionRequestsStarted.TrySetResult();
                }

                await bothPermissionRequestsStarted.Task.WaitAsync(TimeSpan.FromSeconds(30));
                return new PermissionRequestResult { Kind = PermissionRequestResultKind.Approved };
            }
        });

        session.On(evt =>
        {
            if (evt is ToolExecutionCompleteEvent toolEvt)
            {
                var errorMessage = toolEvt.Data.Error?.Message ?? string.Empty;
                if (errorMessage.Contains("first_permission_tool completed", StringComparison.Ordinal))
                {
                    firstToolCompleted.TrySetResult(toolEvt);
                }
                else if (errorMessage.Contains("second_permission_tool completed", StringComparison.Ordinal))
                {
                    secondToolCompleted.TrySetResult(toolEvt);
                }
            }
        });
        var idle = TestHelper.GetNextEventOfTypeAsync<SessionIdleEvent>(session);

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Call both first_permission_tool and second_permission_tool in the same turn. Do not call any other tools."
        });

        await bothPermissionRequestsStarted.Task.WaitAsync(TimeSpan.FromSeconds(30));
        var completed = await Task.WhenAll(firstToolCompleted.Task, secondToolCompleted.Task).WaitAsync(TimeSpan.FromSeconds(60));
        await idle;

        // Should have received multiple permission requests (one per tool call)
        Assert.Equal(2, permissionRequestCount);

        List<PermissionRequest> requests;
        lock (permissionRequestsLock) { requests = [.. permissionRequests]; }
        Assert.Contains(requests, request => request is PermissionRequestCustomTool custom && custom.ToolName == "first_permission_tool");
        Assert.Contains(requests, request => request is PermissionRequestCustomTool custom && custom.ToolName == "second_permission_tool");

        Assert.True(firstToolCalled);
        Assert.True(secondToolCalled);
        Assert.All(completed, toolEvt =>
        {
            Assert.False(toolEvt.Data.Success);
            Assert.Equal("rejected", toolEvt.Data.Error?.Code);
        });

        ToolResultAIContent FirstPermissionTool()
        {
            firstToolCalled = true;
            return new(new ToolResultObject
            {
                ResultType = "rejected",
                TextResultForLlm = "first_permission_tool completed after permission approval",
            });
        }

        ToolResultAIContent SecondPermissionTool()
        {
            secondToolCalled = true;
            return new(new ToolResultObject
            {
                ResultType = "rejected",
                TextResultForLlm = "second_permission_tool completed after permission approval",
            });
        }
    }

    [Fact]
    public async Task Should_Deny_Permission_With_NoResult_Kind()
    {
        var permissionCalled = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);

        var session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = (_, _) =>
            {
                permissionCalled.TrySetResult(true);
                return Task.FromResult(new PermissionRequestResult { Kind = PermissionRequestResultKind.NoResult });
            }
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Run 'node --version'"
        });

        Assert.True(
            await permissionCalled.Task.WaitAsync(TimeSpan.FromSeconds(30)),
            "Expected the no-result permission handler to be called.");

        await session.AbortAsync();
    }

    [Fact]
    public async Task Should_Short_Circuit_Permission_Handler_When_Set_Approve_All_Enabled()
    {
        var handlerCallCount = 0;

        var session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = (_, _) =>
            {
                Interlocked.Increment(ref handlerCallCount);
                return Task.FromResult(new PermissionRequestResult { Kind = PermissionRequestResultKind.Approved });
            },
        });

        // Runtime contract: when approveAllToolPermissionRequests is true the runtime
        // short-circuits the permission flow with { kind: "approved" } *before*
        // invoking the SDK-supplied handler. This RPC sets that runtime flag.
        var setResult = await session.Rpc.Permissions.SetApproveAllAsync(true);
        Assert.True(setResult.Success);

        try
        {
            var toolCompleted = new TaskCompletionSource<ToolExecutionCompleteEvent>(TaskCreationOptions.RunContinuationsAsynchronously);
            using var subscription = session.On(evt =>
            {
                if (evt is ToolExecutionCompleteEvent done && done.Data.Success)
                {
                    toolCompleted.TrySetResult(done);
                }
            });

            await session.SendAndWaitAsync(new MessageOptions
            {
                Prompt = "Run 'echo test' and tell me what happens",
            });

            // A real shell tool must have completed successfully under the runtime-level approval.
            await toolCompleted.Task.WaitAsync(TimeSpan.FromSeconds(30));

            Assert.Equal(0, Volatile.Read(ref handlerCallCount));
        }
        finally
        {
            await session.Rpc.Permissions.SetApproveAllAsync(false);
        }
    }
}
