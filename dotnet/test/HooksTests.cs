/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class HooksTests(E2ETestFixture fixture, ITestOutputHelper output) : E2ETestBase(fixture, "hooks", output)
{
    [Fact]
    public async Task Should_Invoke_PreToolUse_Hook_When_Model_Runs_A_Tool()
    {
        var preToolUseInputs = new List<PreToolUseHookInput>();
        CopilotSession? session = null;
        session = await Client.CreateSessionAsync(new SessionConfig
        {
            Hooks = new SessionHooks
            {
                OnPreToolUse = (input, invocation) =>
                {
                    preToolUseInputs.Add(input);
                    Assert.Equal(session!.SessionId, invocation.SessionId);
                    return Task.FromResult<PreToolUseHookOutput?>(new PreToolUseHookOutput { PermissionDecision = "allow" });
                }
            }
        });

        // Create a file for the model to read
        await File.WriteAllTextAsync(Path.Combine(Ctx.WorkDir, "hello.txt"), "Hello from the test!");

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Read the contents of hello.txt and tell me what it says"
        });

        await TestHelper.GetFinalAssistantMessageAsync(session);

        // Should have received at least one preToolUse hook call
        Assert.NotEmpty(preToolUseInputs);

        // Should have received the tool name
        Assert.Contains(preToolUseInputs, i => !string.IsNullOrEmpty(i.ToolName));
    }

    [Fact]
    public async Task Should_Invoke_PostToolUse_Hook_After_Model_Runs_A_Tool()
    {
        var postToolUseInputs = new List<PostToolUseHookInput>();
        CopilotSession? session = null;
        session = await Client.CreateSessionAsync(new SessionConfig
        {
            Hooks = new SessionHooks
            {
                OnPostToolUse = (input, invocation) =>
                {
                    postToolUseInputs.Add(input);
                    Assert.Equal(session!.SessionId, invocation.SessionId);
                    return Task.FromResult<PostToolUseHookOutput?>(null);
                }
            }
        });

        // Create a file for the model to read
        await File.WriteAllTextAsync(Path.Combine(Ctx.WorkDir, "world.txt"), "World from the test!");

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Read the contents of world.txt and tell me what it says"
        });

        await TestHelper.GetFinalAssistantMessageAsync(session);

        // Should have received at least one postToolUse hook call
        Assert.NotEmpty(postToolUseInputs);

        // Should have received the tool name and result
        Assert.Contains(postToolUseInputs, i => !string.IsNullOrEmpty(i.ToolName));
        Assert.Contains(postToolUseInputs, i => i.ToolResult != null);
    }

    [Fact]
    public async Task Should_Invoke_Both_PreToolUse_And_PostToolUse_Hooks_For_Single_Tool_Call()
    {
        var preToolUseInputs = new List<PreToolUseHookInput>();
        var postToolUseInputs = new List<PostToolUseHookInput>();

        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            Hooks = new SessionHooks
            {
                OnPreToolUse = (input, invocation) =>
                {
                    preToolUseInputs.Add(input);
                    return Task.FromResult<PreToolUseHookOutput?>(new PreToolUseHookOutput { PermissionDecision = "allow" });
                },
                OnPostToolUse = (input, invocation) =>
                {
                    postToolUseInputs.Add(input);
                    return Task.FromResult<PostToolUseHookOutput?>(null);
                }
            }
        });

        await File.WriteAllTextAsync(Path.Combine(Ctx.WorkDir, "both.txt"), "Testing both hooks!");

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Read the contents of both.txt"
        });

        await TestHelper.GetFinalAssistantMessageAsync(session);

        // Both hooks should have been called
        Assert.NotEmpty(preToolUseInputs);
        Assert.NotEmpty(postToolUseInputs);

        // The same tool should appear in both
        var preToolNames = preToolUseInputs.Select(i => i.ToolName).Where(n => !string.IsNullOrEmpty(n)).ToHashSet();
        var postToolNames = postToolUseInputs.Select(i => i.ToolName).Where(n => !string.IsNullOrEmpty(n)).ToHashSet();
        Assert.True(preToolNames.Overlaps(postToolNames), "Expected the same tool to appear in both pre and post hooks");
    }

    [Fact]
    public async Task Should_Deny_Tool_Execution_When_PreToolUse_Returns_Deny()
    {
        var preToolUseInputs = new List<PreToolUseHookInput>();

        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            Hooks = new SessionHooks
            {
                OnPreToolUse = (input, invocation) =>
                {
                    preToolUseInputs.Add(input);
                    // Deny all tool calls
                    return Task.FromResult<PreToolUseHookOutput?>(new PreToolUseHookOutput { PermissionDecision = "deny" });
                }
            }
        });

        // Create a file
        var originalContent = "Original content that should not be modified";
        await File.WriteAllTextAsync(Path.Combine(Ctx.WorkDir, "protected.txt"), originalContent);

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Edit protected.txt and replace 'Original' with 'Modified'"
        });

        var response = await TestHelper.GetFinalAssistantMessageAsync(session);

        // The hook should have been called
        Assert.NotEmpty(preToolUseInputs);

        // The response should be defined
        Assert.NotNull(response);
    }
}
