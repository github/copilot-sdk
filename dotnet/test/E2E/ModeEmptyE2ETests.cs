/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class ModeEmptyE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "mode_empty", output)
{
    private static CopilotClientOptions EmptyModeOptions(E2ETestContext ctx) => new()
    {
        Mode = CopilotClientMode.Empty,
        BaseDirectory = ctx.HomeDir,
    };

    [Fact]
    public async Task Empty_Mode_Isolated_Set_Shell_Tool_Is_Not_Exposed()
    {
        await using var client = Ctx.CreateClient(options: EmptyModeOptions(Ctx));
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            AvailableTools = new ToolSet().AddBuiltIn(BuiltInTools.Isolated),
        });

        await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say hi." });

        var exchanges = await Ctx.GetExchangesAsync();
        var toolNames = GetToolNames(exchanges[^1]);

        Assert.DoesNotContain("bash", toolNames);
        Assert.DoesNotContain("powershell", toolNames);
        Assert.DoesNotContain("edit", toolNames);
        Assert.DoesNotContain("grep", toolNames);
        Assert.DoesNotContain("web_fetch", toolNames);

        Assert.Contains(toolNames, name => BuiltInTools.Isolated.Contains(name));
    }

    [Fact]
    public async Task Empty_Mode_Builtin_Star_Exposes_All_Built_In_Tools()
    {
        await using var client = Ctx.CreateClient(options: EmptyModeOptions(Ctx));
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            AvailableTools = new ToolSet().AddBuiltIn("*"),
        });

        await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say hi." });

        var exchanges = await Ctx.GetExchangesAsync();
        var toolNames = GetToolNames(exchanges[^1]);

        var shellToolName = OperatingSystem.IsWindows() ? "powershell" : "bash";
        Assert.Contains(shellToolName, toolNames);
    }

    [Fact]
    public async Task Empty_Mode_Excluded_Tools_Subtracts_From_Available_Tools()
    {
        var shellToolName = OperatingSystem.IsWindows() ? "powershell" : "bash";
        await using var client = Ctx.CreateClient(options: EmptyModeOptions(Ctx));
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            AvailableTools = new ToolSet().AddBuiltIn("*"),
            ExcludedTools = [$"builtin:{shellToolName}"],
        });

        await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say hi." });

        var exchanges = await Ctx.GetExchangesAsync();
        var toolNames = GetToolNames(exchanges[^1]);

        Assert.DoesNotContain(shellToolName, toolNames);
        Assert.NotEmpty(toolNames);
    }

    [Fact]
    public async Task Empty_Mode_Strips_Environment_Context_From_The_System_Message_By_Default()
    {
        await using var client = Ctx.CreateClient(options: EmptyModeOptions(Ctx));
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

        var exchanges = await Ctx.GetExchangesAsync();
        var systemMessage = GetSystemMessage(exchanges[^1]);
        Assert.DoesNotMatch(@"(?i)Current working directory:", systemMessage);
        Assert.DoesNotMatch(@"(?i)Operating System:", systemMessage);
    }

    [Fact]
    public async Task Empty_Mode_System_Message_Replace_Llm_Follows_Caller_Content_Verbatim()
    {
        await using var client = Ctx.CreateClient(options: EmptyModeOptions(Ctx));
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
        await using var client = Ctx.CreateClient(options: EmptyModeOptions(Ctx));
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

        var exchanges = await Ctx.GetExchangesAsync();
        var systemMessage = GetSystemMessage(exchanges[^1]);
        Assert.DoesNotMatch(@"(?i)Current working directory:", systemMessage);
        Assert.DoesNotMatch(@"(?i)Operating System:", systemMessage);
    }
}
