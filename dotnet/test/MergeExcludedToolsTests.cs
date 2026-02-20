/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Microsoft.Extensions.AI;
using System.ComponentModel;
using Xunit;

namespace GitHub.Copilot.SDK.Test;

public class MergeExcludedToolsTests
{
    [Fact]
    public void Tool_Names_Are_Added_To_ExcludedTools()
    {
        var tools = new List<AIFunction>
        {
            AIFunctionFactory.Create(Noop, "my_tool"),
        };

        var result = CopilotClient.MergeExcludedTools(null, tools);

        Assert.NotNull(result);
        Assert.Contains("my_tool", result!);
    }

    [Fact]
    public void Merges_With_Existing_ExcludedTools_And_Deduplicates()
    {
        var existing = new List<string> { "view", "my_tool" };
        var tools = new List<AIFunction>
        {
            AIFunctionFactory.Create(Noop, "my_tool"),
            AIFunctionFactory.Create(Noop, "another_tool"),
        };

        var result = CopilotClient.MergeExcludedTools(existing, tools);

        Assert.NotNull(result);
        Assert.Equal(3, result!.Count);
        Assert.Contains("view", result);
        Assert.Contains("my_tool", result);
        Assert.Contains("another_tool", result);
    }

    [Fact]
    public void Returns_Null_When_No_Tools_Provided()
    {
        var result = CopilotClient.MergeExcludedTools(null, null);
        Assert.Null(result);
    }

    [Fact]
    public void Returns_ExcludedTools_Unchanged_When_Tools_Empty()
    {
        var existing = new List<string> { "view" };
        var result = CopilotClient.MergeExcludedTools(existing, new List<AIFunction>());

        Assert.Same(existing, result);
    }

    [Fact]
    public void Returns_Tool_Names_When_ExcludedTools_Null()
    {
        var tools = new List<AIFunction>
        {
            AIFunctionFactory.Create(Noop, "my_tool"),
        };

        var result = CopilotClient.MergeExcludedTools(null, tools);

        Assert.NotNull(result);
        Assert.Single(result!);
        Assert.Equal("my_tool", result[0]);
    }

    [Description("No-op")]
    static string Noop() => "";
}
