/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Collections.Concurrent;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class SubagentHooksE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "subagent_hooks", output)
{
    [Fact]
    public async Task Should_Invoke_PreToolUse_And_PostToolUse_Hooks_For_Sub_Agent_Tool_Calls()
    {
        var hookLog = new ConcurrentBag<(string Kind, string ToolName, string SessionId)>();

        // Create a client with the session-based subagents feature flag
        var env = new Dictionary<string, string>(Ctx.GetEnvironment());
        env["COPILOT_EXP_COPILOT_CLI_SESSION_BASED_SUBAGENTS"] = "true";
        var client = Ctx.CreateClient(options: new CopilotClientOptions { Environment = env });

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            Hooks = new SessionHooks
            {
                OnPreToolUse = (input, invocation) =>
                {
                    hookLog.Add(("pre", input.ToolName, input.SessionId));
                    return Task.FromResult<PreToolUseHookOutput?>(new PreToolUseHookOutput
                    {
                        PermissionDecision = "allow"
                    });
                },
                OnPostToolUse = (input, invocation) =>
                {
                    hookLog.Add(("post", input.ToolName, input.SessionId));
                    return Task.FromResult<PostToolUseHookOutput?>(null);
                },
            },
        });

        // Create a file for the sub-agent to read
        await File.WriteAllTextAsync(Path.Combine(Ctx.WorkDir, "subagent-test.txt"), "Hello from subagent test!");

        await session.SendAndWaitAsync(
            new MessageOptions
            {
                Prompt = "Use the task tool to spawn an explore agent that reads the file "
                    + "subagent-test.txt in the current directory and reports its contents. "
                    + "You must use the task tool."
            },
            timeout: TimeSpan.FromSeconds(120));

        var log = hookLog.ToArray();

        // Parent tool hooks fire for "task"
        var taskPre = log.Where(h => h.Kind == "pre" && h.ToolName == "task").ToArray();
        Assert.True(taskPre.Length >= 1, "preToolUse should fire for the parent's 'task' tool call");

        // Sub-agent tool hooks fire for "view"
        var viewPre = log.Where(h => h.Kind == "pre" && h.ToolName == "view").ToArray();
        var viewPost = log.Where(h => h.Kind == "post" && h.ToolName == "view").ToArray();
        Assert.True(viewPre.Length > 0, "preToolUse should fire for the sub-agent's 'view' tool call");
        Assert.True(viewPost.Length > 0, "postToolUse should fire for the sub-agent's 'view' tool call");

        // input.SessionId distinguishes parent from sub-agent
        Assert.NotEqual(viewPre[0].SessionId, taskPre[0].SessionId);
    }
}
