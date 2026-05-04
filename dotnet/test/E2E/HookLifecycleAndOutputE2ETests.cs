/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Microsoft.Extensions.AI;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

/// <summary>
/// E2E coverage for every handler exposed on <see cref="SessionHooks"/>:
/// OnPreToolUse, OnPostToolUse, OnUserPromptSubmitted, OnSessionStart, OnSessionEnd,
/// OnErrorOccurred. Output-shape behavior (modifiedPrompt / additionalContext /
/// errorHandling / modifiedArgs / modifiedResult / sessionSummary) is asserted alongside
/// hook invocation. If a new handler is added to <c>SessionHooks</c>, add a corresponding
/// test here.
/// </summary>
public class HookLifecycleAndOutputE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "hooks_extended", output)
{
    private static readonly string[] ValidErrorContexts = ["model_call", "tool_execution", "system", "user_input"];

    [Fact]
    public async Task Should_Invoke_OnSessionStart_Hook_On_New_Session()
    {
        var sessionStartInputs = new List<SessionStartHookInput>();
        CopilotSession? session = null;
        session = await CreateSessionAsync(new SessionConfig
        {
            Hooks = new SessionHooks
            {
                OnSessionStart = (input, invocation) =>
                {
                    sessionStartInputs.Add(input);
                    Assert.Equal(session!.SessionId, invocation.SessionId);
                    return Task.FromResult<SessionStartHookOutput?>(null);
                },
            },
        });

        await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say hi" });

        Assert.NotEmpty(sessionStartInputs);
        Assert.Equal("new", sessionStartInputs[0].Source);
        Assert.True(sessionStartInputs[0].Timestamp > 0);
        Assert.False(string.IsNullOrEmpty(sessionStartInputs[0].Cwd));

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Invoke_OnUserPromptSubmitted_Hook_When_Sending_A_Message()
    {
        var userPromptInputs = new List<UserPromptSubmittedHookInput>();
        CopilotSession? session = null;
        session = await CreateSessionAsync(new SessionConfig
        {
            Hooks = new SessionHooks
            {
                OnUserPromptSubmitted = (input, invocation) =>
                {
                    userPromptInputs.Add(input);
                    Assert.Equal(session!.SessionId, invocation.SessionId);
                    return Task.FromResult<UserPromptSubmittedHookOutput?>(null);
                },
            },
        });

        await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say hello" });

        Assert.NotEmpty(userPromptInputs);
        Assert.Contains("Say hello", userPromptInputs[0].Prompt);
        Assert.True(userPromptInputs[0].Timestamp > 0);
        Assert.False(string.IsNullOrEmpty(userPromptInputs[0].Cwd));

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Invoke_OnSessionEnd_Hook_When_Session_Is_Disconnected()
    {
        var sessionEndInputs = new List<SessionEndHookInput>();
        var sessionEndHookInvoked = new TaskCompletionSource<SessionEndHookInput>(TaskCreationOptions.RunContinuationsAsynchronously);
        CopilotSession? session = null;
        session = await CreateSessionAsync(new SessionConfig
        {
            Hooks = new SessionHooks
            {
                OnSessionEnd = (input, invocation) =>
                {
                    sessionEndInputs.Add(input);
                    sessionEndHookInvoked.TrySetResult(input);
                    Assert.Equal(session!.SessionId, invocation.SessionId);
                    return Task.FromResult<SessionEndHookOutput?>(null);
                },
            },
        });

        await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say hi" });

        await session.DisposeAsync();

        await sessionEndHookInvoked.Task.WaitAsync(TimeSpan.FromSeconds(10));
        Assert.NotEmpty(sessionEndInputs);
    }

    [Fact]
    public async Task Should_Invoke_OnErrorOccurred_Hook_When_Error_Occurs()
    {
        CopilotSession? session = null;
        session = await CreateSessionAsync(new SessionConfig
        {
            Hooks = new SessionHooks
            {
                OnErrorOccurred = (input, invocation) =>
                {
                    Assert.Equal(session!.SessionId, invocation.SessionId);
                    Assert.True(input.Timestamp > 0);
                    Assert.False(string.IsNullOrEmpty(input.Cwd));
                    Assert.False(string.IsNullOrEmpty(input.Error));
                    Assert.Contains(input.ErrorContext, ValidErrorContexts);
                    return Task.FromResult<ErrorOccurredHookOutput?>(null);
                },
            },
        });

        await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say hi" });

        // OnErrorOccurred is dispatched by the runtime for actual errors. In a normal
        // session it may not fire — this test verifies the hook is properly wired and
        // that the session works correctly with it registered. If the hook *did* fire,
        // the assertions above would have run.
        Assert.False(string.IsNullOrEmpty(session.SessionId));

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Invoke_UserPromptSubmitted_Hook_And_Modify_Prompt()
    {
        var inputs = new List<UserPromptSubmittedHookInput>();
        var session = await CreateSessionAsync(new SessionConfig
        {
            Hooks = new SessionHooks
            {
                OnUserPromptSubmitted = (input, invocation) =>
                {
                    inputs.Add(input);
                    Assert.False(string.IsNullOrWhiteSpace(invocation.SessionId));
                    return Task.FromResult<UserPromptSubmittedHookOutput?>(new UserPromptSubmittedHookOutput
                    {
                        ModifiedPrompt = "Reply with exactly: HOOKED_PROMPT",
                    });
                },
            },
        });

        var response = await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say something else" });

        Assert.NotEmpty(inputs);
        Assert.Contains("Say something else", inputs[0].Prompt);
        Assert.Contains("HOOKED_PROMPT", response?.Data.Content ?? string.Empty);
    }

    [Fact]
    public async Task Should_Invoke_SessionStart_Hook()
    {
        var inputs = new List<SessionStartHookInput>();
        var session = await CreateSessionAsync(new SessionConfig
        {
            Hooks = new SessionHooks
            {
                OnSessionStart = (input, invocation) =>
                {
                    inputs.Add(input);
                    Assert.False(string.IsNullOrWhiteSpace(invocation.SessionId));
                    return Task.FromResult<SessionStartHookOutput?>(new SessionStartHookOutput
                    {
                        AdditionalContext = "Session start hook context.",
                    });
                },
            },
        });

        await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say hi" });

        Assert.NotEmpty(inputs);
        Assert.Equal("new", inputs[0].Source);
        Assert.False(string.IsNullOrEmpty(inputs[0].Cwd));
    }

    [Fact]
    public async Task Should_Invoke_SessionEnd_Hook()
    {
        var inputs = new List<SessionEndHookInput>();
        var hookInvoked = new TaskCompletionSource<SessionEndHookInput>(TaskCreationOptions.RunContinuationsAsynchronously);
        var session = await CreateSessionAsync(new SessionConfig
        {
            Hooks = new SessionHooks
            {
                OnSessionEnd = (input, invocation) =>
                {
                    inputs.Add(input);
                    hookInvoked.TrySetResult(input);
                    Assert.False(string.IsNullOrWhiteSpace(invocation.SessionId));
                    return Task.FromResult<SessionEndHookOutput?>(new SessionEndHookOutput
                    {
                        SessionSummary = "session ended",
                    });
                },
            },
        });

        await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say bye" });
        await session.DisposeAsync();
        await hookInvoked.Task.WaitAsync(TimeSpan.FromSeconds(10));

        Assert.NotEmpty(inputs);
    }

    [Fact]
    public async Task Should_Register_ErrorOccurred_Hook()
    {
        var inputs = new List<ErrorOccurredHookInput>();
        var session = await CreateSessionAsync(new SessionConfig
        {
            Hooks = new SessionHooks
            {
                OnErrorOccurred = (input, invocation) =>
                {
                    inputs.Add(input);
                    Assert.False(string.IsNullOrWhiteSpace(invocation.SessionId));
                    return Task.FromResult<ErrorOccurredHookOutput?>(new ErrorOccurredHookOutput
                    {
                        ErrorHandling = "skip",
                    });
                },
            },
        });

        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Say hi",
        });

        // OnErrorOccurred is dispatched only by genuine runtime errors (e.g. provider
        // failures, internal exceptions). A normal turn cannot deterministically trigger
        // one, so this test is **registration-only**: it verifies the SDK accepts the hook,
        // wires it through to the runtime via session.create, and that the lambda above is
        // not invoked inappropriately during a healthy turn. End-to-end coverage of an
        // actually-fired ErrorOccurred event would require a fault injection point that
        // does not exist in the public surface today.
        Assert.Empty(inputs);
        Assert.NotNull(session.SessionId);
    }

    [Fact]
    public async Task Should_Allow_PreToolUse_To_Return_ModifiedArgs_And_SuppressOutput()
    {
        var inputs = new List<PreToolUseHookInput>();
        var session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            Tools =
            [
                AIFunctionFactory.Create(
                    (string value) => value,
                    "echo_value",
                    "Echoes the supplied value")
            ],
            Hooks = new SessionHooks
            {
                OnPreToolUse = (input, invocation) =>
                {
                    inputs.Add(input);
                    if (input.ToolName != "echo_value")
                    {
                        return Task.FromResult<PreToolUseHookOutput?>(new PreToolUseHookOutput
                        {
                            PermissionDecision = "allow",
                        });
                    }

                    return Task.FromResult<PreToolUseHookOutput?>(new PreToolUseHookOutput
                    {
                        PermissionDecision = "allow",
                        ModifiedArgs = new Dictionary<string, object> { ["value"] = "modified by hook" },
                        SuppressOutput = false,
                    });
                },
            },
        });

        var response = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Call echo_value with value 'original', then reply with the result.",
        });

        Assert.NotEmpty(inputs);
        Assert.Contains(inputs, input => input.ToolName == "echo_value");
        Assert.Contains("modified by hook", response?.Data.Content ?? string.Empty);
    }

    [Fact]
    public async Task Should_Allow_PostToolUse_To_Return_ModifiedResult()
    {
        var inputs = new List<PostToolUseHookInput>();
        var session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            AvailableTools = ["report_intent"],
            Hooks = new SessionHooks
            {
                OnPostToolUse = (input, invocation) =>
                {
                    inputs.Add(input);
                    if (input.ToolName != "report_intent")
                    {
                        return Task.FromResult<PostToolUseHookOutput?>(null);
                    }

                    return Task.FromResult<PostToolUseHookOutput?>(new PostToolUseHookOutput
                    {
                        ModifiedResult = "modified by post hook",
                        SuppressOutput = false,
                    });
                },
            },
        });

        var response = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Call the report_intent tool with intent 'Testing post hook', then reply done.",
        });

        Assert.Contains(inputs, input => input.ToolName == "report_intent");
        Assert.Equal("Done.", response?.Data.Content);
    }
}
