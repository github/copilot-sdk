/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Rpc;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class UiHappyPathTests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "ui_happy_path", output)
{
    [Fact]
    public async Task ConfirmAsync_Returns_True_When_Handler_Accepts()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            OnElicitationRequest = context =>
            {
                Assert.Equal("Confirm?", context.Message);
                Assert.Contains("confirmed", context.RequestedSchema!.Properties.Keys);
                return Task.FromResult(new ElicitationResult
                {
                    Action = UIElicitationResponseAction.Accept,
                    Content = new Dictionary<string, object> { ["confirmed"] = true },
                });
            },
        });

        Assert.True(session.Capabilities.Ui?.Elicitation);
        Assert.True(await session.Ui.ConfirmAsync("Confirm?"));
    }

    [Fact]
    public async Task ConfirmAsync_Returns_False_When_Handler_Declines()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            OnElicitationRequest = _ => Task.FromResult(new ElicitationResult
            {
                Action = UIElicitationResponseAction.Decline,
            }),
        });

        Assert.False(await session.Ui.ConfirmAsync("Confirm?"));
    }

    [Fact]
    public async Task SelectAsync_Returns_Selected_Option()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            OnElicitationRequest = context =>
            {
                Assert.Equal("Choose", context.Message);
                Assert.Contains("selection", context.RequestedSchema!.Properties.Keys);
                return Task.FromResult(new ElicitationResult
                {
                    Action = UIElicitationResponseAction.Accept,
                    Content = new Dictionary<string, object> { ["selection"] = "beta" },
                });
            },
        });

        Assert.Equal("beta", await session.Ui.SelectAsync("Choose", ["alpha", "beta"]));
    }

    [Fact]
    public async Task InputAsync_Returns_Freeform_Value()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            OnElicitationRequest = context =>
            {
                Assert.Equal("Enter value", context.Message);
                Assert.Contains("value", context.RequestedSchema!.Properties.Keys);
                return Task.FromResult(new ElicitationResult
                {
                    Action = UIElicitationResponseAction.Accept,
                    Content = new Dictionary<string, object> { ["value"] = "typed value" },
                });
            },
        });

        var result = await session.Ui.InputAsync("Enter value", new InputOptions
        {
            Title = "Value",
            Description = "A value to test",
            MinLength = 1,
            MaxLength = 20,
            Default = "default",
        });

        Assert.Equal("typed value", result);
    }

    [Fact]
    public async Task ElicitationAsync_Returns_All_Action_Shapes()
    {
        var responses = new Queue<ElicitationResult>([
            new ElicitationResult
            {
                Action = UIElicitationResponseAction.Accept,
                Content = new Dictionary<string, object> { ["name"] = "Mona" },
            },
            new ElicitationResult { Action = UIElicitationResponseAction.Decline },
            new ElicitationResult { Action = UIElicitationResponseAction.Cancel },
        ]);

        var session = await CreateSessionAsync(new SessionConfig
        {
            OnElicitationRequest = context =>
            {
                Assert.Equal("Name?", context.Message);
                return Task.FromResult(responses.Dequeue());
            },
        });

        var parameters = new ElicitationParams
        {
            Message = "Name?",
            RequestedSchema = new ElicitationSchema
            {
                Properties = new Dictionary<string, object>
                {
                    ["name"] = new Dictionary<string, object> { ["type"] = "string" },
                },
                Required = ["name"],
            },
        };

        var accept = await session.Ui.ElicitationAsync(parameters);
        var decline = await session.Ui.ElicitationAsync(parameters);
        var cancel = await session.Ui.ElicitationAsync(parameters);

        Assert.Equal(UIElicitationResponseAction.Accept, accept.Action);
        Assert.Equal("Mona", accept.Content!["name"].ToString());
        Assert.Equal(UIElicitationResponseAction.Decline, decline.Action);
        Assert.Equal(UIElicitationResponseAction.Cancel, cancel.Action);
    }
}
