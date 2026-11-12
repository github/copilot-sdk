/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics.CodeAnalysis;

namespace GitHub.Copilot;

/// <summary>Why the runtime is requesting a GitHub token.</summary>
[Experimental(Diagnostics.Experimental)]
public enum GitHubTokenRequestReason
{
    /// <summary>The session needs its initial token.</summary>
    Initial,

    /// <summary>The session needs a refreshed token.</summary>
    Refresh,
}

/// <summary>Arguments passed to a session-scoped GitHub token provider.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class GitHubTokenProviderArgs
{
    /// <summary>Gets the effective GitHub host for which a token is needed.</summary>
    public required string Host { get; init; }

    /// <summary>
    /// Gets the session receiving the token, or <see langword="null"/> before a
    /// cloud session has been assigned an identifier.
    /// </summary>
    public string? SessionId { get; init; }

    /// <summary>Gets whether the runtime needs an initial or refreshed token.</summary>
    public required GitHubTokenRequestReason Reason { get; init; }
}

/// <summary>A GitHub access token returned by a session-scoped provider.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class GitHubToken
{
    /// <summary>Gets or sets the GitHub access token.</summary>
    public required string AccessToken { get; set; }

    /// <summary>Gets or sets the OAuth token type. The runtime defaults it to <c>bearer</c>.</summary>
    public string? TokenType { get; set; }

    /// <summary>
    /// Gets or sets the required positive number of seconds remaining when the
    /// callback completes. Production GitHub tokens typically last eight hours.
    /// </summary>
    public required long ExpiresIn { get; set; }

    /// <inheritdoc />
    public override string ToString()
        => $"{nameof(GitHubToken)} {{ {nameof(TokenType)} = {TokenType}, {nameof(ExpiresIn)} = {ExpiresIn}, {nameof(AccessToken)} = <redacted> }}";
}

/// <summary>The result returned by a session-scoped GitHub token provider.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class GitHubTokenProviderResult
{
    /// <summary>Gets whether token acquisition was cancelled.</summary>
    public bool Cancelled { get; private init; }

    /// <summary>Gets the acquired token, if acquisition was not cancelled.</summary>
    public GitHubToken? Token { get; private init; }

    /// <summary>Creates a successful token result.</summary>
    public static GitHubTokenProviderResult FromToken(GitHubToken token)
    {
        ArgumentNullException.ThrowIfNull(token);
        return new() { Token = token };
    }

    /// <summary>Creates a cancelled result.</summary>
    public static GitHubTokenProviderResult Cancel() => new() { Cancelled = true };
}
