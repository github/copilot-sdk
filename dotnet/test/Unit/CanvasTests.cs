/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System;
using System.IO;
using System.Reflection;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using GitHub.Copilot;
using GitHub.Copilot.Rpc;
using Microsoft.Extensions.Logging;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public class CanvasTests
{
    private static JsonSerializerOptions GetSerializerOptions()
    {
        var prop = typeof(CopilotClient).GetProperty(
            "SerializerOptionsForMessageFormatter",
            BindingFlags.NonPublic | BindingFlags.Static);
        var options = (JsonSerializerOptions?)prop?.GetValue(null);
        Assert.NotNull(options);
        return options!;
    }

    private static CopilotSession CreateSession()
    {
        var options = GetSerializerOptions();
        var rpcType = typeof(CopilotClient).Assembly.GetType("GitHub.Copilot.JsonRpc");
        Assert.NotNull(rpcType);

        var inputStream = new MemoryStream();
        var outputStream = new MemoryStream();
        object? rpc;
        try
        {
            rpc = Activator.CreateInstance(
                rpcType!,
                BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic,
                binder: null,
                args: [inputStream, outputStream, options, null],
                culture: null);
            Assert.NotNull(rpc);
        }
        catch
        {
            inputStream.Dispose();
            outputStream.Dispose();
            throw;
        }

        var logger = new TestLogger();
        var ctor = typeof(CopilotSession).GetConstructor(
            BindingFlags.Instance | BindingFlags.NonPublic,
            binder: null,
            types: [typeof(string), rpcType!, typeof(ILogger), typeof(CopilotClient), typeof(string)],
            modifiers: null);
        Assert.NotNull(ctor);
        try
        {
            return (CopilotSession)ctor!.Invoke(["session-1", rpc, logger, new CopilotClient(), null]);
        }
        catch
        {
            inputStream.Dispose();
            outputStream.Dispose();
            throw;
        }
    }

    private sealed class TestLogger : ILogger
    {
        public IDisposable? BeginScope<TState>(TState state) where TState : notnull => null;

        public bool IsEnabled(LogLevel logLevel) => false;

        public void Log<TState>(
            LogLevel logLevel,
            EventId eventId,
            TState state,
            Exception? exception,
            Func<TState, Exception?, string> formatter)
        {
        }
    }

    private static void DispatchEvent(CopilotSession session, SessionEvent evt)
    {
        var method = typeof(CopilotSession).GetMethod(
            "DispatchEvent",
            BindingFlags.Instance | BindingFlags.NonPublic);
        Assert.NotNull(method);
        method!.Invoke(session, [evt]);
    }

    [Fact]
    public void CanvasDeclaration_Serializes_CamelCase_SkippingNulls()
    {
        var options = GetSerializerOptions();
        var decl = new CanvasDeclaration
        {
            Id = "report",
            DisplayName = "Quarterly Report",
            Description = "Renders the latest report",
        };

        var json = JsonSerializer.Serialize(decl, options);
        using var doc = JsonDocument.Parse(json);
        var root = doc.RootElement;

        Assert.Equal("report", root.GetProperty("id").GetString());
        Assert.Equal("Quarterly Report", root.GetProperty("displayName").GetString());
        Assert.Equal("Renders the latest report", root.GetProperty("description").GetString());
        Assert.False(root.TryGetProperty("inputSchema", out _));
        Assert.False(root.TryGetProperty("actions", out _));
    }

    [Fact]
    public void CanvasProviderOpenResult_Roundtrips_WithCamelCaseFields()
    {
        var options = GetSerializerOptions();
        var response = new CanvasProviderOpenResult
        {
            Url = "https://example.com/c/1",
            Title = "Demo",
            Status = "ready"
        };

        var json = JsonSerializer.Serialize(response, options);
        var parsed = JsonSerializer.Deserialize<CanvasProviderOpenResult>(json, options);

        Assert.NotNull(parsed);
        Assert.Equal("https://example.com/c/1", parsed!.Url);
        Assert.Equal("Demo", parsed.Title);
        Assert.Equal("ready", parsed.Status);
    }

    [Fact]
    public void SessionCanvasOpenedEvent_UpdatesOpenCanvasSnapshots()
    {
        var session = CreateSession();

        DispatchEvent(session, new SessionCanvasOpenedEvent
        {
            Id = Guid.NewGuid(),
            Timestamp = DateTimeOffset.UtcNow,
            Data = new SessionCanvasOpenedData
            {
                Availability = CanvasOpenedAvailability.Ready,
                CanvasId = "",
                ExtensionId = "project:counter",
                InstanceId = "missing-canvas-id",
                Reopen = false,
            }
        });
        DispatchEvent(session, new SessionCanvasOpenedEvent
        {
            Id = Guid.NewGuid(),
            Timestamp = DateTimeOffset.UtcNow,
            Data = new SessionCanvasOpenedData
            {
                Availability = CanvasOpenedAvailability.Ready,
                CanvasId = "counter",
                ExtensionId = "project:counter",
                ExtensionName = "Counter Provider",
                InstanceId = "counter-1",
                Title = "Counter",
                Status = "ready",
                Url = "https://example.test/counter",
                Input = JsonDocument.Parse("""{"seed":1}""").RootElement.Clone(),
                Reopen = false,
            }
        });
        DispatchEvent(session, new SessionCanvasOpenedEvent
        {
            Id = Guid.NewGuid(),
            Timestamp = DateTimeOffset.UtcNow,
            Data = new SessionCanvasOpenedData
            {
                Availability = CanvasOpenedAvailability.Stale,
                CanvasId = "logs",
                ExtensionId = "project:logs",
                InstanceId = "logs-1",
                Title = "Logs",
                Reopen = false,
            }
        });

        Assert.Collection(
            session.OpenCanvases,
            canvas => Assert.Equal("counter-1", canvas.InstanceId),
            canvas => Assert.Equal("logs-1", canvas.InstanceId));

        DispatchEvent(session, new SessionCanvasOpenedEvent
        {
            Id = Guid.NewGuid(),
            Timestamp = DateTimeOffset.UtcNow,
            Data = new SessionCanvasOpenedData
            {
                Availability = CanvasOpenedAvailability.Stale,
                CanvasId = "counter",
                ExtensionId = "project:counter",
                ExtensionName = "Counter Provider",
                InstanceId = "counter-1",
                Title = "Counter Updated",
                Status = "reconnected",
                Url = "https://example.test/counter-updated",
                Input = JsonDocument.Parse("""{"seed":2}""").RootElement.Clone(),
                Reopen = true,
            }
        });

        Assert.Collection(
            session.OpenCanvases,
            canvas =>
            {
                Assert.Equal("counter-1", canvas.InstanceId);
                Assert.Equal("Counter Updated", canvas.Title);
                Assert.Equal("reconnected", canvas.Status);
                Assert.Equal("https://example.test/counter-updated", canvas.Url);
                Assert.True(canvas.Reopen);
                Assert.Equal(CanvasInstanceAvailability.Stale, canvas.Availability);
                Assert.Equal(2, canvas.Input!.Value.GetProperty("seed").GetInt32());
            },
            canvas => Assert.Equal("logs-1", canvas.InstanceId));
    }

    [Fact]
    public void ExtensionInfo_Serializes_SourceAndName()
    {
        var options = GetSerializerOptions();
        var info = new ExtensionInfo { Source = "github-app", Name = "demo" };
        var json = JsonSerializer.Serialize(info, options);
        using var doc = JsonDocument.Parse(json);
        Assert.Equal("github-app", doc.RootElement.GetProperty("source").GetString());
        Assert.Equal("demo", doc.RootElement.GetProperty("name").GetString());
    }

    [Fact]
    public async Task CanvasHandlerBase_DefaultOnClose_Completes()
    {
        var handler = new TestHandler();
        await handler.OnCloseAsync(new CanvasProviderCloseRequest(), CancellationToken.None);
    }

    [Fact]
    public async Task CanvasHandlerBase_DefaultOnAction_ThrowsNoHandlerCanvasException()
    {
        var handler = new TestHandler();
        var ex = await Assert.ThrowsAsync<CanvasException>(
            () => handler.OnActionAsync(new CanvasProviderInvokeActionRequest(), CancellationToken.None));
        Assert.Equal("canvas_action_no_handler", ex.Code);
    }

    [Fact]
    public void CanvasException_NoHandler_HasExpectedCode()
    {
        var err = CanvasException.NoHandler();
        Assert.Equal("canvas_action_no_handler", err.Code);
        Assert.False(string.IsNullOrEmpty(err.Message));
    }

    [Fact]
    public void SessionConfig_Clone_CopiesCanvasFields()
    {
        var handler = new TestHandler();
        var declaration = new CanvasDeclaration { Id = "c1", DisplayName = "C", Description = "d" };
        var config = new SessionConfig
        {
            Canvases = new[] { declaration },
            RequestCanvasRenderer = true,
            RequestExtensions = true,
            ExtensionInfo = new ExtensionInfo { Source = "github-app", Name = "demo" },
            CanvasHandler = handler
        };

        var clone = config.Clone();

        Assert.NotNull(clone.Canvases);
        Assert.Single(clone.Canvases!);
        Assert.Equal("c1", clone.Canvases![0].Id);
        Assert.True(clone.RequestCanvasRenderer);
        Assert.True(clone.RequestExtensions);
        Assert.NotNull(clone.ExtensionInfo);
        Assert.Equal("github-app", clone.ExtensionInfo!.Source);
        Assert.Same(handler, clone.CanvasHandler);

        // Mutating the clone's list does not affect the original.
        clone.Canvases!.Add(new CanvasDeclaration { Id = "c2", DisplayName = "C2", Description = "d2" });
        Assert.Single(config.Canvases!);
    }

    [Fact]
    public void ResumeSessionConfig_Clone_CopiesCanvasFields()
    {
        var handler = new TestHandler();
        var config = new ResumeSessionConfig
        {
            Canvases = new[] { new CanvasDeclaration { Id = "c1", DisplayName = "C", Description = "d" } },
            RequestCanvasRenderer = true,
            ExtensionInfo = new ExtensionInfo { Source = "s", Name = "n" },
            CanvasHandler = handler
        };

        var clone = config.Clone();

        Assert.NotNull(clone.Canvases);
        Assert.Single(clone.Canvases!);
        Assert.True(clone.RequestCanvasRenderer);
        Assert.NotNull(clone.ExtensionInfo);
        Assert.Same(handler, clone.CanvasHandler);
    }

    private sealed class TestHandler : CanvasHandlerBase
    {
        public override Task<CanvasProviderOpenResult> OnOpenAsync(CanvasProviderOpenRequest context, CancellationToken cancellationToken)
            => Task.FromResult(new CanvasProviderOpenResult { Url = "https://example.com" });
    }
}
