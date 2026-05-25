/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Text.Json.Serialization;
using System.Threading;
using System.Threading.Tasks;
using GitHub.Copilot.Rpc;

namespace GitHub.Copilot;

/// <summary>
/// Declarative metadata for a single canvas, sent over the wire on
/// <c>session.create</c> / <c>session.resume</c>.
/// </summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CanvasDeclaration
{
    /// <summary>Canvas identifier, unique within the declaring connection.</summary>
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    /// <summary>Human-readable name shown in host UI and canvas pickers.</summary>
    [JsonPropertyName("displayName")]
    public string DisplayName { get; set; } = string.Empty;

    /// <summary>Short, single-sentence description shown to the agent in canvas catalogs.</summary>
    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;

    /// <summary>JSON Schema for the <c>input</c> payload accepted by <c>canvas.open</c>.</summary>
    [JsonPropertyName("inputSchema")]
    public JsonElement? InputSchema { get; set; }

    /// <summary>Agent-callable actions this canvas exposes.</summary>
    [JsonPropertyName("actions")]
    public IList<CanvasAction>? Actions { get; set; }
}

/// <summary>
/// Stable extension identity for session participants that provide canvases.
/// </summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ExtensionInfo
{
    /// <summary>Extension namespace/source, e.g. <c>"github-app"</c>.</summary>
    [JsonPropertyName("source")]
    public string Source { get; set; } = string.Empty;

    /// <summary>Stable provider name within the source namespace.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;
}

/// <summary>Response returned from <see cref="ICanvasHandler.OnOpenAsync"/>.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CanvasOpenResponse
{
    /// <summary>URL the host should render. Optional for canvases with no visual surface.</summary>
    [JsonPropertyName("url")]
    public string? Url { get; set; }

    /// <summary>Provider-supplied title shown in host chrome.</summary>
    [JsonPropertyName("title")]
    public string? Title { get; set; }

    /// <summary>Provider-supplied status text shown in host chrome.</summary>
    [JsonPropertyName("status")]
    public string? Status { get; set; }
}

/// <summary>Host capabilities passed to canvas provider callbacks.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CanvasHostContext
{
    /// <summary>Host capability details.</summary>
    [JsonPropertyName("capabilities")]
    public CanvasHostCapabilities Capabilities { get; set; } = new();
}

/// <summary>Host capability details passed to canvas provider callbacks.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CanvasHostCapabilities
{
    /// <summary>Whether the host supports canvas rendering.</summary>
    [JsonPropertyName("canvases")]
    public bool Canvases { get; set; }
}

/// <summary>Context handed to <see cref="ICanvasHandler.OnOpenAsync"/>.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CanvasOpenContext
{
    /// <summary>Session that requested the canvas.</summary>
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Owning provider identifier.</summary>
    public string ExtensionId { get; set; } = string.Empty;

    /// <summary>Canvas id from the declaring <see cref="CanvasDeclaration"/>.</summary>
    public string CanvasId { get; set; } = string.Empty;

    /// <summary>Stable instance id supplied by the runtime.</summary>
    public string InstanceId { get; set; } = string.Empty;

    /// <summary>Validated input payload.</summary>
    public JsonElement Input { get; set; }

    /// <summary>Host capabilities supplied by the runtime.</summary>
    public CanvasHostContext? Host { get; set; }
}

/// <summary>Context handed to <see cref="ICanvasHandler.OnActionAsync"/>.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CanvasActionContext
{
    /// <summary>Session that invoked the action.</summary>
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Owning provider identifier.</summary>
    public string ExtensionId { get; set; } = string.Empty;

    /// <summary>Canvas id targeted by the action.</summary>
    public string CanvasId { get; set; } = string.Empty;

    /// <summary>Instance id targeted by the action.</summary>
    public string InstanceId { get; set; } = string.Empty;

    /// <summary>Action name from <see cref="CanvasAction.Name"/>.</summary>
    public string ActionName { get; set; } = string.Empty;

    /// <summary>Validated input payload.</summary>
    public JsonElement Input { get; set; }

    /// <summary>Host capabilities supplied by the runtime.</summary>
    public CanvasHostContext? Host { get; set; }
}

/// <summary>Context handed to a canvas's close lifecycle hook.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CanvasLifecycleContext
{
    /// <summary>Session owning the canvas instance.</summary>
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Owning provider identifier.</summary>
    public string ExtensionId { get; set; } = string.Empty;

    /// <summary>Canvas id from the declaring <see cref="CanvasDeclaration"/>.</summary>
    public string CanvasId { get; set; } = string.Empty;

    /// <summary>Instance id this lifecycle event applies to.</summary>
    public string InstanceId { get; set; } = string.Empty;

    /// <summary>Host capabilities supplied by the runtime.</summary>
    public CanvasHostContext? Host { get; set; }
}

/// <summary>Structured error returned from canvas handlers.</summary>
/// <remarks>
/// Throw this from <see cref="ICanvasHandler"/> implementations to surface a
/// machine-readable error code to the runtime. Any other exception is wrapped
/// in a generic <c>canvas_handler_error</c> envelope.
/// </remarks>
[Experimental(Diagnostics.Experimental)]
public sealed class CanvasException : Exception
{
    /// <summary>Initializes a new <see cref="CanvasException"/>.</summary>
    /// <param name="code">Machine-readable error code.</param>
    /// <param name="message">Human-readable message.</param>
    public CanvasException(string code, string message) : base(message)
    {
        Code = code;
    }

    /// <summary>Machine-readable error code.</summary>
    public string Code { get; }

    /// <summary>
    /// Default error returned when a custom action has no handler.
    /// </summary>
    public static CanvasException NoHandler() => new(
        "canvas_action_no_handler",
        "No handler implemented for this canvas action");
}

/// <summary>
/// Internal helpers used by the session runtime to translate <see cref="CanvasException"/>
/// (and other handler-thrown exceptions) into structured JSON-RPC error responses.
/// </summary>
internal static class CanvasErrorHelpers
{
    private const int InternalError = -32603;

    public static LocalRpcInvocationException HandlerUnset() => Build(
        "canvas_handler_unset",
        "No canvas handler is registered on this session");

    public static LocalRpcInvocationException HandlerError(string message) => Build(
        "canvas_handler_error",
        message);

    public static LocalRpcInvocationException ToRpcException(CanvasException error) => Build(error.Code, error.Message);

    private static LocalRpcInvocationException Build(string code, string message)
    {
        JsonElement payload = JsonSerializer.SerializeToElement(
            new JsonObject { ["code"] = code, ["message"] = message },
            TypesJsonContext.Default.JsonObject);
        return new LocalRpcInvocationException(InternalError, message, payload);
    }
}

/// <summary>
/// Provider-side canvas lifecycle handler.
/// </summary>
/// <remarks>
/// A session installs a single <see cref="ICanvasHandler"/> via
/// <c>SessionConfigBase.CanvasHandler</c>. The handler receives every
/// inbound <c>canvas.open</c> / <c>canvas.close</c> / <c>canvas.action.invoke</c>
/// JSON-RPC request the runtime issues for this session and decides — typically
/// by inspecting <see cref="CanvasOpenContext.CanvasId"/> — which
/// application-side canvas should handle the call.
/// <para>
/// The SDK does not maintain a per-canvas registry; multiplexing across
/// declared canvases is the implementor's responsibility.
/// </para>
/// <para>
/// Implementations targeting <c>netstandard2.0</c> cannot rely on default
/// interface methods; derive from <see cref="CanvasHandlerBase"/> to inherit
/// sensible defaults for <see cref="OnCloseAsync"/> and <see cref="OnActionAsync"/>.
/// </para>
/// </remarks>
[Experimental(Diagnostics.Experimental)]
public interface ICanvasHandler
{
    /// <summary>Open a new canvas instance.</summary>
    Task<CanvasOpenResponse> OnOpenAsync(CanvasOpenContext context, CancellationToken cancellationToken);

    /// <summary>Canvas was closed by the user or agent. Default: no-op.</summary>
    Task OnCloseAsync(CanvasLifecycleContext context, CancellationToken cancellationToken);

    /// <summary>
    /// Handle a non-lifecycle action declared by the canvas.
    /// Default: throws <see cref="CanvasException.NoHandler"/>.
    /// </summary>
    Task<object?> OnActionAsync(CanvasActionContext context, CancellationToken cancellationToken);
}

/// <summary>
/// Convenience base class for <see cref="ICanvasHandler"/> that supplies
/// default no-op / no-handler implementations of the optional callbacks.
/// </summary>
[Experimental(Diagnostics.Experimental)]
public abstract class CanvasHandlerBase : ICanvasHandler
{
    /// <inheritdoc />
    public abstract Task<CanvasOpenResponse> OnOpenAsync(CanvasOpenContext context, CancellationToken cancellationToken);

    /// <inheritdoc />
    public virtual Task OnCloseAsync(CanvasLifecycleContext context, CancellationToken cancellationToken)
#if NET8_0_OR_GREATER
        => Task.CompletedTask;
#else
        => Task.FromResult<object?>(null);
#endif

    /// <inheritdoc />
    public virtual Task<object?> OnActionAsync(CanvasActionContext context, CancellationToken cancellationToken)
        => Task.FromException<object?>(CanvasException.NoHandler());
}
