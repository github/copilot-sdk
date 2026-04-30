/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Microsoft.Extensions.AI;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class HooksExtendedTests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "hooks_extended", output)
{
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
        var session = await CreateSessionAsync(new SessionConfig
        {
            Hooks = new SessionHooks
            {
                OnSessionEnd = (input, invocation) =>
                {
                    inputs.Add(input);
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
        await Task.Delay(200);

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

        // ErrorOccurred is dispatched only for runtime errors. A normal run verifies the hook
        // is accepted by the session and any unexpected invocation is validated above.
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

        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Call echo_value with value 'original', then reply with the result.",
        });

        Assert.NotEmpty(inputs);
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
        Assert.NotNull(response?.Data.Content);
    }
}
