/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using GitHub.Copilot.Rpc;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Fact]
    public void Workspace_Diff_Preserves_Existing_Signature_And_Defaults()
    {
        var method = typeof(WorkspacesApi).GetMethod("DiffAsync",
            [typeof(WorkspaceDiffMode), typeof(bool?), typeof(CancellationToken)]);
        Assert.NotNull(method);
        Assert.Equal(
            new (string? Name, bool IsOptional)[]
            {
                ("mode", false),
                ("ignoreWhitespace", true),
                ("cancellationToken", true),
            },
            method.GetParameters().Select(parameter => (parameter.Name, parameter.IsOptional)));
        Assert.All(method.GetParameters().Skip(1), parameter => Assert.Null(parameter.DefaultValue));
    }

    [Fact]
    public async Task Workspace_Diff_Preserves_Positional_Calls_And_Offers_Contents_Through_Request()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = request => request.Method == "session.workspaces.diff"
            ? new Dictionary<string, object?>
            {
                ["mode"] = "session",
                ["requestedMode"] = "session",
                ["isFallback"] = false,
                ["changes"] = Array.Empty<object>(),
            }
            : throw new InvalidOperationException($"Unexpected workspace diff request '{request.Method}'.");
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        server.ClearRequests();
        var cancellation = CancellationToken.None;

        await session.Rpc.Workspaces.DiffAsync(WorkspaceDiffMode.Session, false, cancellation);
        await session.Rpc.Workspaces.DiffAsync(WorkspaceDiffMode.Session);
        await session.Rpc.Workspaces.DiffAsync(WorkspaceDiffMode.Session, cancellationToken: cancellation);
        await session.Rpc.Workspaces.DiffAsync(new WorkspacesDiffRequest
        {
            Mode = WorkspaceDiffMode.Session,
            IgnoreWhitespace = false,
            IncludeContents = true,
        }, cancellation);
        await session.Rpc.Workspaces.DiffAsync(new WorkspacesDiffRequest
        {
            Mode = WorkspaceDiffMode.Session,
            IncludeContents = false,
        }, cancellation);

        Assert.Null(typeof(WorkspacesDiffRequest).GetProperty("SessionId"));
        var requests = server.Requests.ToArray();
        Assert.All(requests, request => Assert.Equal(
            ("session.workspaces.diff", session.SessionId, "session"),
            (request.Method, request.Params.GetProperty("sessionId").GetString(),
                request.Params.GetProperty("mode").GetString())));
        Assert.Equal(
            new (bool? IgnoreWhitespace, bool? IncludeContents)[]
            {
                (false, null),
                (null, null),
                (null, null),
                (false, true),
                (null, false),
            },
            requests.Select(request => (
                request.Params.TryGetProperty("ignoreWhitespace", out var ignoreWhitespace)
                    ? ignoreWhitespace.GetBoolean() : (bool?)null,
                request.Params.TryGetProperty("includeContents", out var includeContents)
                    ? includeContents.GetBoolean() : (bool?)null)));
    }
}
#endif
