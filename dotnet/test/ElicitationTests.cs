/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Rpc;
using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class ElicitationTests(E2ETestFixture fixture, ITestOutputHelper output)
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

    [Fact]
    public async Task Session_With_ElicitationHandler_Reports_Elicitation_Capability()
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

        Assert.True(session.Capabilities.Ui?.Elicitation == true,
            "Session with onElicitationRequest should report elicitation capability");
        await session.DisposeAsync();
    }

    [Fact]
    public async Task Session_Without_ElicitationHandler_Reports_No_Capability()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        Assert.True(session.Capabilities.Ui?.Elicitation != true,
            "Session without onElicitationRequest should not report elicitation capability");
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

