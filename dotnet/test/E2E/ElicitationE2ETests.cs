/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Rpc;
using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

public class ElicitationE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "elicitation", output)
{
    [Fact]
    public async Task Defaults_Capabilities_When_Not_Provided()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        // Default capabilities should exist (even if empty)
        Assert.NotNull(session.Capabilities);
        await session.DisposeAsync();
    }

    [Fact]
    public async Task Elicitation_Throws_When_Capability_Is_Missing()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        // Capabilities.Ui?.Elicitation should not be true by default (headless mode)
        Assert.True(session.Capabilities.Ui?.Elicitation != true);

        // Calling any UI method should throw
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(async () =>
        {
            await session.Ui.ConfirmAsync("test");
        });
        Assert.Contains("not supported", ex.Message, StringComparison.OrdinalIgnoreCase);

        ex = await Assert.ThrowsAsync<InvalidOperationException>(async () =>
        {
            await session.Ui.SelectAsync("test", ["a", "b"]);
        });
        Assert.Contains("not supported", ex.Message, StringComparison.OrdinalIgnoreCase);

        ex = await Assert.ThrowsAsync<InvalidOperationException>(async () =>
        {
            await session.Ui.InputAsync("test");
        });
        Assert.Contains("not supported", ex.Message, StringComparison.OrdinalIgnoreCase);

        ex = await Assert.ThrowsAsync<InvalidOperationException>(async () =>
        {
            await session.Ui.ElicitationAsync(new ElicitationParams
            {
                Message = "Enter name",
                RequestedSchema = new ElicitationSchema
                {
                    Properties = new Dictionary<string, object>() { ["name"] = new Dictionary<string, object> { ["type"] = "string" } },
                    Required = ["name"],
                },
            });
        });
        Assert.Contains("not supported", ex.Message, StringComparison.OrdinalIgnoreCase);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Sends_RequestElicitation_When_Handler_Provided()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            OnElicitationRequest = _ => Task.FromResult(new ElicitationResult
            {
                Action = UIElicitationResponseAction.Accept,
                Content = new Dictionary<string, object>(),
            }),
        });

        // Session should be created successfully with requestElicitation=true
        Assert.NotNull(session);
        Assert.NotNull(session.SessionId);
        await session.DisposeAsync();
    }

    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public async Task Should_Report_Elicitation_Capability_Based_On_Handler_Presence(bool hasHandler)
    {
        var config = new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        };

        if (hasHandler)
        {
            config.OnElicitationRequest = _ => Task.FromResult(new ElicitationResult
            {
                Action = UIElicitationResponseAction.Accept,
                Content = new Dictionary<string, object>(),
            });
        }

        var session = await CreateSessionAsync(config);
        Assert.Equal(hasHandler, session.Capabilities.Ui?.Elicitation == true);
        await session.DisposeAsync();
    }

    [Fact]
    public async Task Session_Without_ElicitationHandler_Creates_Successfully()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        // requestElicitation was false (no handler)
        Assert.NotNull(session);
        await session.DisposeAsync();
    }

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

    [Fact]
    public void SessionCapabilities_Types_Are_Properly_Structured()
    {
        var capabilities = new SessionCapabilities
        {
            Ui = new SessionUiCapabilities { Elicitation = true }
        };

        Assert.NotNull(capabilities.Ui);
        Assert.True(capabilities.Ui.Elicitation);

        // Test with null UI
        var emptyCapabilities = new SessionCapabilities();
        Assert.Null(emptyCapabilities.Ui);
    }

    [Fact]
    public void ElicitationSchema_Types_Are_Properly_Structured()
    {
        var schema = new ElicitationSchema
        {
            Type = "object",
            Properties = new Dictionary<string, object>
            {
                ["name"] = new Dictionary<string, object> { ["type"] = "string", ["minLength"] = 1 },
                ["confirmed"] = new Dictionary<string, object> { ["type"] = "boolean", ["default"] = true },
            },
            Required = ["name"],
        };

        Assert.Equal("object", schema.Type);
        Assert.Equal(2, schema.Properties.Count);
        Assert.Single(schema.Required!);
    }

    [Fact]
    public void ElicitationParams_Types_Are_Properly_Structured()
    {
        var ep = new ElicitationParams
        {
            Message = "Enter your name",
            RequestedSchema = new ElicitationSchema
            {
                Properties = new Dictionary<string, object>
                {
                    ["name"] = new Dictionary<string, object> { ["type"] = "string" },
                },
            },
        };

        Assert.Equal("Enter your name", ep.Message);
        Assert.NotNull(ep.RequestedSchema);
    }

    [Fact]
    public void ElicitationResult_Types_Are_Properly_Structured()
    {
        var result = new ElicitationResult
        {
            Action = UIElicitationResponseAction.Accept,
            Content = new Dictionary<string, object> { ["name"] = "Alice" },
        };

        Assert.Equal(UIElicitationResponseAction.Accept, result.Action);
        Assert.NotNull(result.Content);
        Assert.Equal("Alice", result.Content!["name"]);

        var declined = new ElicitationResult
        {
            Action = UIElicitationResponseAction.Decline,
        };
        Assert.Null(declined.Content);
    }

    [Fact]
    public void InputOptions_Has_All_Properties()
    {
        var options = new InputOptions
        {
            Title = "Email Address",
            Description = "Enter your email",
            MinLength = 5,
            MaxLength = 100,
            Format = "email",
            Default = "user@example.com",
        };

        Assert.Equal("Email Address", options.Title);
        Assert.Equal("Enter your email", options.Description);
        Assert.Equal(5, options.MinLength);
        Assert.Equal(100, options.MaxLength);
        Assert.Equal("email", options.Format);
        Assert.Equal("user@example.com", options.Default);
    }

    [Fact]
    public void ElicitationContext_Has_All_Properties()
    {
        var context = new ElicitationContext
        {
            SessionId = "session-42",
            Message = "Pick a color",
            RequestedSchema = new ElicitationSchema
            {
                Properties = new Dictionary<string, object>
                {
                    ["color"] = new Dictionary<string, object> { ["type"] = "string", ["enum"] = new[] { "red", "blue" } },
                },
            },
            Mode = ElicitationRequestedMode.Form,
            ElicitationSource = "mcp-server",
            Url = null,
        };

        Assert.Equal("session-42", context.SessionId);
        Assert.Equal("Pick a color", context.Message);
        Assert.NotNull(context.RequestedSchema);
        Assert.Equal(ElicitationRequestedMode.Form, context.Mode);
        Assert.Equal("mcp-server", context.ElicitationSource);
        Assert.Null(context.Url);
    }

    [Fact]
    public async Task Session_Config_OnElicitationRequest_Is_Cloned()
    {
        ElicitationHandler handler = _ => Task.FromResult(new ElicitationResult
        {
            Action = UIElicitationResponseAction.Cancel,
        });

        var config = new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            OnElicitationRequest = handler,
        };

        var clone = config.Clone();

        Assert.Same(handler, clone.OnElicitationRequest);
    }

    [Fact]
    public void Resume_Config_OnElicitationRequest_Is_Cloned()
    {
        ElicitationHandler handler = _ => Task.FromResult(new ElicitationResult
        {
            Action = UIElicitationResponseAction.Cancel,
        });

        var config = new ResumeSessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            OnElicitationRequest = handler,
        };

        var clone = config.Clone();

        Assert.Same(handler, clone.OnElicitationRequest);
    }
}

