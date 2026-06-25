/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Microsoft.Extensions.AI;
using System.ComponentModel;
using System.Text.Json;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public class CopilotToolTests
{
    [Fact]
    public void DefineTool_Sets_Name_Description_And_Copilot_Metadata()
    {
        var function = CopilotTool.DefineTool(
            ReturnsOk,
            new CopilotToolOptions
            {
                OverridesBuiltInTool = true,
                SkipPermission = true,
                Defer = CopilotToolDefer.Auto
            });

        Assert.Equal("test_tool", function.Name);
        Assert.Equal("Test tool", function.Description);
        Assert.True(function.AdditionalProperties.TryGetValue("is_override", out var isOverride));
        Assert.True((bool)isOverride!);
        Assert.True(function.AdditionalProperties.TryGetValue("skip_permission", out var skipPermission));
        Assert.True((bool)skipPermission!);
        Assert.True(function.AdditionalProperties.TryGetValue("defer", out var defer));
        Assert.Equal(CopilotToolDefer.Auto, defer);
    }

    [Fact]
    public void DefineTool_Omits_Copilot_Metadata_When_Flags_Are_False()
    {
        var function = CopilotTool.DefineTool(ReturnsOk);

        Assert.False(function.AdditionalProperties.ContainsKey("is_override"));
        Assert.False(function.AdditionalProperties.ContainsKey("skip_permission"));
        Assert.False(function.AdditionalProperties.ContainsKey("defer"));
    }

    [Fact]
    public void DefineTool_Accepts_Lambda_Handlers_Without_Casts()
    {
        var function = CopilotTool.DefineTool((string value) => value, factoryOptions: new() { Name = "echo", Description = "Echo a value" });

        Assert.Equal("echo", function.Name);
    }

    [Fact]
    public async Task DefineTool_Binds_ToolInvocation_And_Excludes_It_From_Schema()
    {
        var function = CopilotTool.DefineTool(
            (string value, ToolInvocation invocation) => $"{value}:{invocation.ToolName}",
            factoryOptions: new() { Name = "echo", Description = "Echo a value" });

        var schema = function.JsonSchema.GetRawText();
        Assert.Contains("\"value\"", schema);
        Assert.DoesNotContain("\"invocation\"", schema);

        using var document = JsonDocument.Parse("\"hello\"");
        var result = await function.InvokeAsync(new AIFunctionArguments
        {
            ["value"] = document.RootElement.Clone(),
            Context = new Dictionary<object, object?>
            {
                [typeof(ToolInvocation)] = new ToolInvocation { ToolName = "echo" }
            }
        });

        Assert.Equal("hello:echo", Assert.IsType<JsonElement>(result).GetString());
    }

    [Fact]
    public async Task DefineTool_Preserves_Custom_Parameter_Binding()
    {
        var function = CopilotTool.DefineTool(
            (string value, string suffix, ToolInvocation invocation) => $"{value}:{suffix}:{invocation.ToolName}",
            factoryOptions: new()
            {
                Name = "echo",
                Description = "Echo a value",
                ConfigureParameterBinding = pi =>
                    pi.Name == "suffix"
                        ? new AIFunctionFactoryOptions.ParameterBindingOptions
                        {
                            ExcludeFromSchema = true,
                            BindParameter = static (_, _) => "bound"
                        }
                        : default
            });

        var schema = function.JsonSchema.GetRawText();
        Assert.Contains("\"value\"", schema);
        Assert.DoesNotContain("\"suffix\"", schema);
        Assert.DoesNotContain("\"invocation\"", schema);

        using var document = JsonDocument.Parse("\"hello\"");
        var result = await function.InvokeAsync(new AIFunctionArguments
        {
            ["value"] = document.RootElement.Clone(),
            Context = new Dictionary<object, object?>
            {
                [typeof(ToolInvocation)] = new ToolInvocation { ToolName = "echo" }
            }
        });

        Assert.Equal("hello:bound:echo", Assert.IsType<JsonElement>(result).GetString());
    }

    [Fact]
    public void DefineTool_Preserves_Additional_Properties_And_ToolOptions_Take_Precedence()
    {
        var function = CopilotTool.DefineTool(
            ReturnsOk,
            new CopilotToolOptions
            {
                SkipPermission = true
            },
            new AIFunctionFactoryOptions
            {
                Name = "test_tool",
                AdditionalProperties = new Dictionary<string, object?>
                {
                    ["custom"] = 42,
                    ["skip_permission"] = false,
                }
            });

        Assert.Equal(42, function.AdditionalProperties["custom"]);
        Assert.True(function.AdditionalProperties.TryGetValue("skip_permission", out var skipPermission));
        Assert.True((bool)skipPermission!);
    }

    [DisplayName("test_tool")]
    [Description("Test tool")]
    private static string ReturnsOk() => "ok";
}
