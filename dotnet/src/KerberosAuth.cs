/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics.CodeAnalysis;
using GitHub.Copilot.Rpc;

namespace GitHub.Copilot;

/// <summary>
/// Supplies Kerberos credentials from the SDK host for a requested service principal.
/// </summary>
[Experimental(Diagnostics.Experimental)]
public sealed class KerberosAuthConfig
{
    /// <summary>
    /// Gets when the runtime uses <see cref="Callback"/>: <see cref="KerberosAuthMode.Fallback"/> uses it only
    /// when native credential lookup fails; <see cref="KerberosAuthMode.Always"/> never attempts native lookup.
    /// </summary>
    public required KerberosAuthMode Mode { get; init; }

    /// <summary>
    /// Gets which traffic <see cref="Callback"/> authenticates. <see langword="null"/> (the default) or
    /// <see cref="KerberosAuthScope.Connection"/> authenticates only requests made for sessions this client creates or resumes;
    /// <see cref="KerberosAuthScope.Runtime"/> also authenticates the runtime's shared background traffic.
    /// </summary>
    /// <remarks>
    /// Only one connection at a time can hold <see cref="KerberosAuthScope.Runtime"/>, so startup fails if another client
    /// holds it. The registration ends when the connection closes.
    /// </remarks>
    public KerberosAuthScope? Scope { get; init; }

    /// <summary>
    /// Gets the callback that returns a fresh, single-use base64 SPNEGO token without the <c>Negotiate </c> prefix,
    /// or <see langword="null"/> when credentials are unavailable.
    /// </summary>
    /// <remarks>
    /// <see cref="KerberosAuthAcquireRequest.Spn"/> uses the platform-independent <c>service/host[@REALM]</c> form;
    /// validate it before issuing credentials. Tokens must not be logged or persisted. Exceptions, including cancellation
    /// not requested through the supplied <see cref="CancellationToken"/>, fail the lookup.
    /// </remarks>
    public required Func<KerberosAuthAcquireRequest, CancellationToken, Task<string?>> Callback { get; init; }
}

[Experimental(Diagnostics.Experimental)]
internal sealed class KerberosAuthAdapter : IKerberosAuthHandler
{
    private readonly Func<KerberosAuthAcquireRequest, CancellationToken, Task<string?>> _callback;

    internal KerberosAuthAdapter(KerberosAuthConfig config)
    {
        Mode = config.Mode;
        Scope = config.Scope;
        _callback = config.Callback ?? throw new ArgumentException(
            $"{nameof(KerberosAuthConfig)}.{nameof(KerberosAuthConfig.Callback)} must not be null.", nameof(config));
    }

    internal KerberosAuthMode Mode { get; }

    internal KerberosAuthScope? Scope { get; }

    public async Task<KerberosAuthAcquireResult> AcquireAsync(
        KerberosAuthAcquireRequest request,
        CancellationToken cancellationToken = default)
    {
        try
        {
            return new KerberosAuthAcquireResult { Token = await _callback(request, cancellationToken).ConfigureAwait(false) };
        }
        catch (OperationCanceledException ex) when (!cancellationToken.IsCancellationRequested)
        {
            // The dispatcher only replies to cancellation of the request itself; surface
            // callback-local cancellation as an ordinary failure so the runtime is not left waiting.
            throw new InvalidOperationException("Kerberos credential callback was cancelled.", ex);
        }
    }
}
