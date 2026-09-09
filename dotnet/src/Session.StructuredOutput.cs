/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Microsoft.Extensions.AI;
using System.Diagnostics.CodeAnalysis;
using System.Text.Json;
using System.Text.Json.Serialization.Metadata;

namespace GitHub.Copilot;

public sealed partial class CopilotSession
{
    /// <summary>
    /// Sends a prompt with a JSON Schema inferred from <typeparamref name="TResult"/> and
    /// deserializes the final response into that type.
    /// </summary>
    /// <typeparam name="TResult">The expected response type.</typeparam>
    /// <param name="prompt">The user message text.</param>
    /// <param name="serializerOptions">Options used both for schema inference and deserialization.
    /// Defaults to <see cref="AIJsonUtilities.DefaultOptions"/>, as for custom tools.
    /// For Native AOT, supply options with a source-generated type resolver.</param>
    /// <param name="timeout">Timeout duration (default: 60 seconds). Does not abort agent work.</param>
    /// <param name="cancellationToken">Cancellation token for sending and waiting.</param>
    /// <returns>The non-null deserialized response.</returns>
    [Experimental(Diagnostics.Experimental)]
    public Task<TResult> SendAndWaitAsync<TResult>(
        string prompt,
        JsonSerializerOptions? serializerOptions = null,
        TimeSpan? timeout = null,
        CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(prompt);
        return SendAndWaitAsync<TResult>(new MessageOptions { Prompt = prompt }, serializerOptions, timeout, cancellationToken);
    }

    /// <summary>
    /// Sends a message with a JSON Schema inferred from <typeparamref name="TResult"/> and
    /// deserializes the final response into that type.
    /// </summary>
    /// <typeparam name="TResult">The expected response type.</typeparam>
    /// <param name="options">The message to send. Must not specify a response schema or immediate delivery.</param>
    /// <param name="serializerOptions">Options used both for schema inference and deserialization.
    /// Defaults to <see cref="AIJsonUtilities.DefaultOptions"/>, as for custom tools.
    /// For Native AOT, supply options with a source-generated type resolver.</param>
    /// <param name="timeout">Timeout duration (default: 60 seconds). Does not abort agent work.</param>
    /// <param name="cancellationToken">Cancellation token for sending and waiting.</param>
    /// <returns>The non-null deserialized response.</returns>
    /// <exception cref="ArgumentException">The message specifies a response schema or immediate delivery.</exception>
    /// <exception cref="InvalidOperationException">No final response was received, or the session reported an error.</exception>
    /// <exception cref="JsonException">The response is not valid JSON for the requested type, or is null.</exception>
    /// <exception cref="TimeoutException">The response did not arrive within the timeout.</exception>
    /// <remarks>
    /// Uses the same Microsoft.Extensions.AI schema inference as custom tools. Property naming,
    /// converters, required members and nullable annotations follow the supplied serialization
    /// contracts. The inferred schema requests strict output with all properties required and
    /// additional properties disallowed. Provider schema restrictions still apply.
    /// Deserialization is not full JSON Schema validation; apply application-specific validation
    /// to the returned value where needed. The supplied message options are not modified.
    /// </remarks>
    [Experimental(Diagnostics.Experimental)]
    public async Task<TResult> SendAndWaitAsync<TResult>(
        MessageOptions options,
        JsonSerializerOptions? serializerOptions = null,
        TimeSpan? timeout = null,
        CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(options);
        ThrowIfDisposed();
        if (options.ResponseSchema is not null)
        {
            throw new ArgumentException("The typed overload infers its response schema. Use the untyped overload for an explicit response schema.", nameof(options));
        }
        if (options.Mode == "immediate")
        {
            throw new ArgumentException("Structured output cannot be requested on an immediate steering message.", nameof(options));
        }

        serializerOptions ??= AIJsonUtilities.DefaultOptions;
        var typeInfo = (JsonTypeInfo<TResult>)serializerOptions.GetTypeInfo(typeof(TResult));
        var schema = AIJsonUtilities.CreateJsonSchema(
            typeof(TResult),
            serializerOptions: serializerOptions,
            inferenceOptions: new AIJsonSchemaCreateOptions
            {
                TransformOptions = new AIJsonSchemaTransformOptions
                {
                    RequireAllProperties = true,
                    DisallowAdditionalProperties = true,
                    MoveDefaultKeywordToDescription = true,
                },
            });
        var message = options.Clone();
        message.ResponseSchema = schema;

        var response = await SendAndWaitForStructuredMessageAsync(message, timeout, cancellationToken);
        return JsonSerializer.Deserialize(response.Data.Content, typeInfo)
            ?? throw new JsonException("The structured response was JSON null, not a result.");
    }

    private async Task<AssistantMessageEvent> SendAndWaitForStructuredMessageAsync(
        MessageOptions options, TimeSpan? timeout, CancellationToken cancellationToken)
    {
        var effectiveTimeout = timeout ?? TimeSpan.FromSeconds(60);
        using var cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        cts.CancelAfter(effectiveTimeout);
        var completion = new TaskCompletionSource<AssistantMessageEvent>(TaskCreationOptions.RunContinuationsAsynchronously);
        using var registration = cts.Token.Register(() => completion.TrySetCanceled(cts.Token));
        var gate = new object();
        var pendingEvents = new List<SessionEvent>();
        string? messageId = null;
        var started = false;
        AssistantMessageEvent? finalMessage = null;

        void ProcessEvent(SessionEvent evt)
        {
            switch (evt)
            {
                case UserMessageEvent user when string.IsNullOrEmpty(user.AgentId) && user.Data.MessageId == messageId:
                    started = true;
                    break;
                case AssistantMessageEvent assistant when string.IsNullOrEmpty(assistant.AgentId) && assistant.Data.OriginatingMessageId == messageId:
                    started = true;
                    finalMessage = assistant.Data.ToolRequests is { Length: > 0 } ? null : assistant;
                    break;
                case SessionIdleEvent idle when started && string.IsNullOrEmpty(idle.AgentId) && idle.Data.Mode != SessionMode.Autopilot:
                    if (idle.Data.Aborted == true)
                    {
                        completion.TrySetException(new InvalidOperationException("The session was aborted before a final structured response was received."));
                    }
                    else if (finalMessage is null || string.IsNullOrWhiteSpace(finalMessage.Data.Content))
                    {
                        completion.TrySetException(new InvalidOperationException("The turn completed without a final structured response."));
                    }
                    else
                    {
                        completion.TrySetResult(finalMessage);
                    }
                    break;
                case SessionErrorEvent error when started && string.IsNullOrEmpty(error.AgentId):
                    completion.TrySetException(new InvalidOperationException($"Session error: {error.Data.Message}"));
                    break;
            }
        }

        using var subscription = On<SessionEvent>(evt =>
        {
            if (evt is not (UserMessageEvent or AssistantMessageEvent or SessionIdleEvent or SessionErrorEvent))
            {
                return;
            }
            lock (gate)
            {
                if (messageId is null)
                {
                    // Events can arrive before the send RPC response supplies the logical message ID.
                    pendingEvents.Add(evt);
                }
                else
                {
                    ProcessEvent(evt);
                }
            }
        });
        try
        {
            var sentMessageId = await SendAsync(options, cts.Token);
            lock (gate)
            {
                messageId = sentMessageId;
                foreach (var evt in pendingEvents)
                {
                    ProcessEvent(evt);
                }
                pendingEvents.Clear();
            }
            await Task.WhenAny(completion.Task, JsonRpc.Completion, _eventChannel.Reader.Completion);
            if (!completion.Task.IsCompleted)
            {
                throw new IOException("The session closed before a final structured response was received.");
            }
            return await completion.Task;
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
        {
            throw new TimeoutException($"SendAndWaitAsync timed out after {effectiveTimeout}");
        }
    }
}
