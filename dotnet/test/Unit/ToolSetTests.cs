/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;

namespace GitHub.Copilot.Test.Unit;

public class ToolSetTests
{
    private static readonly string[] BashAndView = ["bash", "view"];
    private static readonly string[] ExpectedBashAndView = ["builtin:bash", "builtin:view"];
    private static readonly string[] AllWildcards = ["builtin:*", "custom:*", "mcp:*"];
    private static readonly string[] BannedTools = ["bash", "powershell", "edit", "grep", "web_fetch"];
    private static readonly string[] ExpectedIsolatedTools = ["ask_user", "task_complete"];

    [Fact]
    public void ToolSet_Emits_Source_Qualified_Strings()
    {
        var items = new ToolSet()
            .AddBuiltIn("bash")
            .AddBuiltIn("*")
            .AddCustom("my_tool")
            .AddCustom("*")
            .AddMcp("github-list_issues")
            .AddMcp("*")
            .ToList();

        Assert.Equal(
        [
            "builtin:bash",
            "builtin:*",
            "custom:my_tool",
            "custom:*",
            "mcp:github-list_issues",
            "mcp:*",
        ], items);
    }

    [Fact]
    public void ToolSet_AddBuiltIn_Accepts_Enumerable()
    {
        var items = new ToolSet().AddBuiltIn(BashAndView).ToList();
        Assert.Equal(ExpectedBashAndView, items);
    }

    [Theory]
    [InlineData("has:colon")]
    [InlineData("has space")]
    [InlineData("")]
    public void ToolSet_Rejects_Invalid_Names(string bad)
    {
        Assert.Throws<ArgumentException>(() => new ToolSet().AddBuiltIn(bad));
        Assert.Throws<ArgumentException>(() => new ToolSet().AddCustom(bad));
        Assert.Throws<ArgumentException>(() => new ToolSet().AddMcp(bad));
    }

    [Fact]
    public void ToolSet_Accepts_Wildcard()
    {
        var items = new ToolSet().AddBuiltIn("*").AddCustom("*").AddMcp("*").ToList();
        Assert.Equal(AllWildcards, items);
    }

    [Fact]
    public void BuiltInTools_Isolated_Does_Not_Contain_Banned_Tools()
    {
        foreach (var banned in BannedTools)
        {
            Assert.DoesNotContain(banned, BuiltInTools.Isolated);
        }
    }

    [Fact]
    public void BuiltInTools_Isolated_Contains_Expected_Tools()
    {
        foreach (var expected in ExpectedIsolatedTools)
        {
            Assert.Contains(expected, BuiltInTools.Isolated);
        }
    }

    [Fact]
    public void CopilotClient_Mode_Empty_Throws_Without_Base_Directory()
    {
        var ex = Assert.Throws<ArgumentException>(() => new CopilotClient(new CopilotClientOptions
        {
            Mode = CopilotClientMode.Empty,
        }));
        Assert.Contains("Empty", ex.Message, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public void CopilotClient_Mode_Empty_Accepts_Base_Directory()
    {
        var dir = Path.Combine(Path.GetTempPath(), "copilot-empty-mode-test-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(dir);
        try
        {
            using var client = new CopilotClient(new CopilotClientOptions
            {
                Mode = CopilotClientMode.Empty,
                BaseDirectory = dir,
            });
            Assert.NotNull(client);
        }
        finally
        {
            Directory.Delete(dir, recursive: true);
        }
    }

    [Fact]
    public void CopilotClient_Default_Mode_Is_CopilotCli()
    {
        using var client = new CopilotClient(new CopilotClientOptions());
        Assert.NotNull(client);
    }
}
