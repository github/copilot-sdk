/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// E2E coverage for the <c>Mode = CopilotClientMode.Empty</c> SDK surface and
/// source-qualified tool filter patterns. The runtime is mode-agnostic; these
/// tests verify that the SDK's translation reaches the runtime correctly by
/// inspecting the resulting CapiProxy chat-completion request (the LLM only
/// sees tools the runtime exposed for the session) and end-to-end behavior
/// (asking the agent to use a tool that should or shouldn't be enabled).
///
/// Mirrors <c>nodejs/test/e2e/mode_empty.e2e.test.ts</c> and shares the same
/// recorded cassettes under <c>test/snapshots/mode_empty/</c>.
///
/// Test method names are intentionally lowercase + underscore so that
/// <see cref="E2ETestContext.ConfigureForTestAsync"/> sanitizes them to the
/// same filenames the Node tests produce.
/// </summary>
public class ModeEmptyE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "mode_empty", output)
{
    private CopilotClient CreateEmptyModeClient()
    {
        return Ctx.CreateClient(options: new CopilotClientOptions
        {
            Mode = CopilotClientMode.Empty,
            BaseDirectory = Ctx.HomeDir,
        });
    }

    [Fact]
    public async Task Empty_Mode_Isolated_Set_Shell_Tool_Is_Not_Exposed()
    {
        var client = CreateEmptyModeClient();
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            AvailableTools = new ToolSet().AddBuiltIn(BuiltInTools.Isolated),
        });

        try
        {
            await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say hi." });
        }
        catch
        {
            // Some runs end the turn without producing a final assistant message;
            // we only care about the tool surface the LLM was shown.
        }

        var exchanges = await WaitForExchangesAsync();
        var toolNames = GetToolNames(exchanges[^1]);

        Assert.DoesNotContain("bash", toolNames);
        Assert.DoesNotContain("edit", toolNames);
        Assert.DoesNotContain("grep", toolNames);
        Assert.DoesNotContain("web_fetch", toolNames);

        // Sanity: at least one of the isolated tools is registered.
        Assert.Contains(toolNames, name => BuiltInTools.Isolated.Contains(name));
    }

    [Fact]
    public async Task Empty_Mode_Builtin_Star_Exposes_All_Built_In_Tools()
    {
        var client = CreateEmptyModeClient();
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            AvailableTools = new ToolSet().AddBuiltIn("*"),
        });

        try
        {
            await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say hi." });
        }
        catch
        {
        }

        var exchanges = await WaitForExchangesAsync();
        var toolNames = GetToolNames(exchanges[^1]);

        // The shell tool name differs by platform (bash vs powershell); either
        // way it's a canonical built-in excluded from Isolated, and builtin:*
        // should bring it back.
        var shellToolName = OperatingSystem.IsWindows() ? "powershell" : "bash";
        Assert.Contains(shellToolName, toolNames);
    }

    [Fact]
    public async Task Empty_Mode_Excluded_Tools_Subtracts_From_Available_Tools()
    {
        var shellToolName = OperatingSystem.IsWindows() ? "powershell" : "bash";
        var client = CreateEmptyModeClient();
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            AvailableTools = new ToolSet().AddBuiltIn("*"),
            ExcludedTools = [$"builtin:{shellToolName}"],
        });

        try
        {
            await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say hi." });
        }
        catch
        {
        }

        var exchanges = await WaitForExchangesAsync();
        var toolNames = GetToolNames(exchanges[^1]);

        // The platform shell is in builtin:* but explicitly excluded → must not be exposed.
        Assert.DoesNotContain(shellToolName, toolNames);
        // Other built-ins are still there (proves the subtraction is targeted).
        Assert.NotEmpty(toolNames);
    }

    [Fact]
    public async Task Empty_Mode_Strips_Environment_Context_From_The_System_Message_By_Default()
    {
        // We can't directly observe section presence, but we can detect it
        // indirectly: in default empty mode the SDK injects the customize-mode
        // override environment_context: { action: "remove" }. We also append a
        // deterministic instruction. If the env_context strip didn't fire, the
        // runtime would still inject OS/cwd lines into the system message.
        var client = CreateEmptyModeClient();
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            AvailableTools = new ToolSet().AddBuiltIn(BuiltInTools.Isolated),
            SystemMessage = new SystemMessageConfig
            {
                Mode = SystemMessageMode.Customize,
                Content = "If the user asks you to name an element, reply with exactly the single word ARGON in all caps and nothing else.",
            },
        });

        var reply = await session.SendAndWaitAsync(new MessageOptions { Prompt = "Name an element." });
        Assert.Contains("ARGON", reply?.Data.Content ?? string.Empty);

        var exchanges = await WaitForExchangesAsync();
        var systemMessage = GetSystemMessage(exchanges[^1]);
        Assert.DoesNotMatch(@"(?i)Current working directory:", systemMessage);
        Assert.DoesNotMatch(@"(?i)Operating System:", systemMessage);
    }

    [Fact]
    public async Task Empty_Mode_System_Message_Replace_Llm_Follows_Caller_Content_Verbatim()
    {
        var client = CreateEmptyModeClient();
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            AvailableTools = new ToolSet().AddBuiltIn(BuiltInTools.Isolated),
            SystemMessage = new SystemMessageConfig
            {
                Mode = SystemMessageMode.Replace,
                Content = "You are a test fixture. Whenever the user asks anything, reply with exactly the single word KRYPTON in all caps and nothing else.",
            },
        });

        var reply = await session.SendAndWaitAsync(new MessageOptions { Prompt = "Hello." });
        Assert.Contains("KRYPTON", reply?.Data.Content ?? string.Empty);
    }

    [Fact]
    public async Task Empty_Mode_Append_Caller_Instruction_Takes_Effect_And_Env_Context_Stripped()
    {
        var client = CreateEmptyModeClient();
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            AvailableTools = new ToolSet().AddBuiltIn(BuiltInTools.Isolated),
            SystemMessage = new SystemMessageConfig
            {
                Mode = SystemMessageMode.Append,
                Content = "If the user asks you to name a noble gas, reply with exactly the single word XENON in all caps and nothing else.",
            },
        });

        var reply = await session.SendAndWaitAsync(new MessageOptions { Prompt = "Name a noble gas." });
        Assert.Contains("XENON", reply?.Data.Content ?? string.Empty);

        var exchanges = await WaitForExchangesAsync();
        var systemMessage = GetSystemMessage(exchanges[^1]);
        Assert.DoesNotMatch(@"(?i)Current working directory:", systemMessage);
        Assert.DoesNotMatch(@"(?i)Operating System:", systemMessage);
    }
}
