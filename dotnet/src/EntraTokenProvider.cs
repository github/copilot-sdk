/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics.CodeAnalysis;
using GitHub.Copilot.Rpc;

namespace GitHub.Copilot;

/// <summary>Entra application and optional host-owned identity and token acquisition.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class EntraAuthOptions
{
    /// <summary>Gets or sets the host application's Entra client ID.</summary>
    public string ClientId { get; set; } = string.Empty;

    /// <summary>Gets or sets the host's MATS name. Omit to disable OneAuth MATS telemetry.</summary>
    public string? MatsAppName { get; set; }

    /// <summary>
    /// Gets or sets the host callback. When absent, the runtime uses built-in sign-in.
    /// Unavailability or cancellation never falls back to runtime sign-in.
    /// </summary>
    public EntraTokenProvider? TokenProvider { get; set; }

    internal EntraAuthConfig ToWire() => new() { ClientId = ClientId, MatsAppName = MatsAppName };
}

/// <summary>
/// Establishes an Entra identity or acquires its resource token. Renewal must be silent
/// and account-pinned. Cancellation fires when the request or original connection ends.
/// </summary>
[Experimental(Diagnostics.Experimental)]
public delegate ValueTask<EntraTokenAcquireResult> EntraTokenProvider(
    EntraTokenAcquireRequest request,
    CancellationToken cancellationToken);

[Experimental(Diagnostics.Experimental)]
internal sealed class EntraTokenAdapter(EntraTokenProvider provider) : IEntraTokenHandler
{
    private CancellationToken _connectionClosed;

    internal void Attach(JsonRpc rpc) => _connectionClosed = rpc.ConnectionClosedToken;

    public async Task<EntraTokenAcquireResult> GetTokenAsync(
        EntraTokenGetTokenRequest request,
        CancellationToken cancellationToken = default)
    {
        var combined = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken, _connectionClosed);
        Task<EntraTokenAcquireResult>? task = null;
        try
        {
            combined.Token.ThrowIfCancellationRequested();
            task = provider(request.Request, combined.Token).AsTask();
            var result = await task.WaitAsync(Timeout.InfiniteTimeSpan, combined.Token).ConfigureAwait(false);
            combined.Token.ThrowIfCancellationRequested();
            return result ?? throw new InvalidOperationException("Entra token provider returned no result.");
        }
        catch (OperationCanceledException) when (combined.IsCancellationRequested)
        {
            throw new LocalRpcInvocationException(-32800, "Entra token acquisition cancelled.");
        }
        catch (Exception)
        {
            throw new LocalRpcInvocationException(-32603, "Entra token provider failed.");
        }
        finally
        {
            if (task is { IsCompleted: false })
            {
                _ = task.ContinueWith(
                    static (completed, state) =>
                    {
                        _ = completed.Exception;
                        ((CancellationTokenSource)state!).Dispose();
                    },
                    combined,
                    CancellationToken.None,
                    TaskContinuationOptions.ExecuteSynchronously,
                    TaskScheduler.Default);
            }
            else
            {
                combined.Dispose();
            }
        }
    }
}
