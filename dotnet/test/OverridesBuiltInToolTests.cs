/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Microsoft.Extensions.AI;
using System.ComponentModel;
using System.Text.Json;
using Xunit;

namespace GitHub.Copilot.SDK.Test;

public class OverridesBuiltInToolTests
{
    [Fact]
    public void ToolDefinition_FromAIFunction_Sets_OverridesBuiltInTool()
    {
        var fn = AIFunctionFactory.Create(Noop, "grep");
        var def = CopilotClient.ToolDefinition.FromAIFunction(fn, overridesBuiltInTool: true);

        Assert.Equal("grep", def.Name);
        Assert.True(def.OverridesBuiltInTool);
    }

    [Fact]
    public void ToolDefinition_FromAIFunction_Omits_OverridesBuiltInTool_When_False()
    {
        var fn = AIFunctionFactory.Create(Noop, "custom_tool");
        var def = CopilotClient.ToolDefinition.FromAIFunction(fn, overridesBuiltInTool: false);

        Assert.Equal("custom_tool", def.Name);
        Assert.Null(def.OverridesBuiltInTool);
    }

    [Fact]
    public void SessionConfig_BuiltInToolOverrides_Is_Used()
    {
        var config = new SessionConfig
        {
            Tools = new List<AIFunction> { AIFunctionFactory.Create(Noop, "grep") },
            BuiltInToolOverrides = new HashSet<string> { "grep" },
        };

        Assert.Contains("grep", config.BuiltInToolOverrides);
    }

    [Fact]
    public void ResumeSessionConfig_BuiltInToolOverrides_Is_Used()
    {
        var config = new ResumeSessionConfig
        {
            Tools = new List<AIFunction> { AIFunctionFactory.Create(Noop, "grep") },
            BuiltInToolOverrides = new HashSet<string> { "grep" },
        };

        Assert.NotNull(config.BuiltInToolOverrides);
        Assert.Contains("grep", config.BuiltInToolOverrides!);
    }

    [Description("No-op")]
    static string Noop() => "";
}
