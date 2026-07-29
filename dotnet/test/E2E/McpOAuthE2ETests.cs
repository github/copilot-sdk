/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using System.Diagnostics;
using System.Net.Http;
using System.Text.Json;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class McpOAuthE2ETests(E2ETestFixture fixture, ITestOutputHelper output) : E2ETestBase(fixture, "mcp_oauth", output)
{
    private const string ExpectedToken = "sdk-host-token";
    private const string RefreshToken = ExpectedToken + "-refresh";
    private const string UpscopeToken = ExpectedToken + "-upscope";
    private const string ReauthToken = ExpectedToken + "-reauth";

    [Fact]
    public async Task Should_Satisfy_MCP_OAuth_Using_Host_Provided_Token()
    {
        await using var oauthServer = await OAuthMcpServer.StartAsync(ExpectedToken);
        var serverName = "oauth-protected-mcp";
        McpAuthContext? observedRequest = null;

        await using var session = await CreateSessionAsync(new SessionConfig
        {
            OnMcpAuthRequest = request =>
            {
                observedRequest = request;
                return Task.FromResult<McpAuthResult?>(McpAuthResult.FromToken(new McpAuthToken
                {
                    AccessToken = ExpectedToken,
                    TokenType = "Bearer",
                    ExpiresIn = 3600
                }));
            },
            McpServers = new Dictionary<string, McpServerConfig>
            {
                [serverName] = new McpHttpServerConfig
                {
                    Url = $"{oauthServer.Url}/mcp",
                    Tools = ["*"]
                }
            }
        });

        await WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.Connected);
        var tools = await session.Rpc.Mcp.ListToolsAsync(serverName);
        Assert.Contains(tools.Tools, tool => tool.Name == "whoami");

        Assert.NotNull(observedRequest);
        Assert.NotEmpty(observedRequest!.RequestId);
        Assert.Equal(serverName, observedRequest!.ServerName);
        Assert.Equal($"{oauthServer.Url}/mcp", observedRequest.ServerUrl);
        Assert.Equal(McpOauthRequestReason.Initial, observedRequest.Reason);
        Assert.NotNull(observedRequest.WwwAuthenticateParams);
        Assert.Equal($"{oauthServer.Url}/.well-known/oauth-protected-resource", observedRequest.WwwAuthenticateParams!.ResourceMetadataUrl);
        Assert.Equal("mcp.read", observedRequest.WwwAuthenticateParams.Scope);
        Assert.Equal("invalid_token", observedRequest.WwwAuthenticateParams.Error);

        using var metadata = JsonDocument.Parse(observedRequest.ResourceMetadata!);
        Assert.Equal($"{oauthServer.Url}/mcp", metadata.RootElement.GetProperty("resource").GetString());

        var requests = await oauthServer.GetRequestsAsync();
        Assert.Contains(requests, request => request.Authorization is null);
        Assert.Contains(requests, request => request.Authorization == $"Bearer {ExpectedToken}");
    }

    [Fact]
    public async Task Should_Resolve_Pending_MCP_OAuth_Request_With_Direct_Rpc()
    {
        await using var oauthServer = await OAuthMcpServer.StartAsync(ExpectedToken);
        var serverName = "oauth-direct-rpc-mcp";
        var authRequest = new TaskCompletionSource<McpAuthContext>(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseHandler = new TaskCompletionSource<McpAuthResult?>(TaskCreationOptions.RunContinuationsAsynchronously);

        await using var session = await CreateSessionAsync(new SessionConfig
        {
            OnMcpAuthRequest = request =>
            {
                authRequest.TrySetResult(request);
                return releaseHandler.Task;
            },
            McpServers = new Dictionary<string, McpServerConfig>
            {
                [serverName] = new McpHttpServerConfig
                {
                    Url = $"{oauthServer.Url}/mcp",
                    Tools = ["*"],
                },
            },
        });

        var connected = WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.Connected);
        var request = await authRequest.Task.WaitAsync(TimeSpan.FromSeconds(30));
        Assert.NotEmpty(request.RequestId);
        Assert.Equal(serverName, request.ServerName);
        Assert.Equal($"{oauthServer.Url}/mcp", request.ServerUrl);
        Assert.Equal(McpOauthRequestReason.Initial, request.Reason);
        Assert.NotNull(request.WwwAuthenticateParams);
        Assert.Equal("mcp.read", request.WwwAuthenticateParams!.Scope);

        var handled = await session.Rpc.Mcp.Oauth.HandlePendingRequestAsync(
            request.RequestId,
            new McpOauthPendingRequestResponseToken
            {
                AccessToken = ExpectedToken,
                TokenType = "Bearer",
                ExpiresIn = 3600,
            });
        Assert.True(handled.Success);

        await connected;
        var tools = await session.Rpc.Mcp.ListToolsAsync(serverName);
        Assert.Contains(tools.Tools, tool => tool.Name == "whoami");

        releaseHandler.SetResult(McpAuthResult.FromToken(new McpAuthToken { AccessToken = ExpectedToken }));
    }

    [Fact]
    public async Task Should_Request_Replacement_Tokens_Across_MCP_OAuth_Lifecycle()
    {
        await using var oauthServer = await OAuthMcpServer.StartAsync(ExpectedToken);
        var serverName = "oauth-lifecycle-mcp";
        List<McpOauthRequestReason> observedReasons = [];
        var refreshCount = 0;

        await using var session = await CreateSessionAsync(new SessionConfig
        {
            EnableMcpApps = true,
            OnMcpAuthRequest = request =>
            {
                observedReasons.Add(request.Reason);
                if (request.Reason == McpOauthRequestReason.Refresh)
                {
                    refreshCount++;
                    Assert.NotNull(request.WwwAuthenticateParams);
                    Assert.Null(request.WwwAuthenticateParams!.ResourceMetadataUrl);
                    Assert.Equal("invalid_token", request.WwwAuthenticateParams.Error);
                    if (refreshCount > 1)
                    {
                        return Task.FromResult<McpAuthResult?>(McpAuthResult.Cancel());
                    }
                }

                if (request.Reason == McpOauthRequestReason.Upscope)
                {
                    Assert.NotNull(request.WwwAuthenticateParams);
                    Assert.Equal($"{oauthServer.Url}/.well-known/oauth-protected-resource", request.WwwAuthenticateParams!.ResourceMetadataUrl);
                    Assert.Equal("mcp.write", request.WwwAuthenticateParams.Scope);
                    Assert.Equal("insufficient_scope", request.WwwAuthenticateParams.Error);
                }

                var token = request.Reason == McpOauthRequestReason.Refresh
                    ? RefreshToken
                    : request.Reason == McpOauthRequestReason.Upscope
                        ? UpscopeToken
                        : request.Reason == McpOauthRequestReason.Reauth
                            ? ReauthToken
                            : ExpectedToken;

                return Task.FromResult<McpAuthResult?>(McpAuthResult.FromToken(new McpAuthToken
                {
                    AccessToken = token
                }));
            },
            McpServers = new Dictionary<string, McpServerConfig>
            {
                [serverName] = new McpHttpServerConfig
                {
                    Url = $"{oauthServer.Url}/mcp",
                    Tools = ["*"]
                }
            }
        });

        await WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.Connected);
        await CallWhoamiAsync(session, serverName, "refresh");
        await CallWhoamiAsync(session, serverName, "upscope");
        await CallWhoamiAsync(session, serverName, "reauth");

        Assert.Equal(
            [
                McpOauthRequestReason.Initial,
                McpOauthRequestReason.Refresh,
                McpOauthRequestReason.Upscope,
                McpOauthRequestReason.Refresh,
                McpOauthRequestReason.Reauth
            ],
            observedReasons);

        var requests = await oauthServer.GetRequestsAsync();
        Assert.Contains(requests, request => request.Authorization == $"Bearer {RefreshToken}");
        Assert.Contains(requests, request => request.Authorization == $"Bearer {UpscopeToken}");
        Assert.Contains(requests, request => request.Authorization == $"Bearer {ReauthToken}");
    }

    [Fact]
    public async Task Should_Cancel_Pending_MCP_OAuth_Request()
    {
        await using var oauthServer = await OAuthMcpServer.StartAsync(ExpectedToken);
        var serverName = "oauth-cancelled-mcp";
        var authRequest = new TaskCompletionSource<McpAuthContext>(TaskCreationOptions.RunContinuationsAsynchronously);

        await using var session = await CreateSessionAsync(new SessionConfig
        {
            OnMcpAuthRequest = request =>
            {
                authRequest.TrySetResult(request);
                return Task.FromResult<McpAuthResult?>(McpAuthResult.Cancel());
            },
            McpServers = new Dictionary<string, McpServerConfig>
            {
                [serverName] = new McpHttpServerConfig
                {
                    Url = $"{oauthServer.Url}/mcp",
                    Tools = ["*"]
                }
            }
        });

        await WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.NeedsAuth);

        // The MCP connection is kicked off by session.create, but the SDK only registers its
        // `mcp.oauth_required` event interest once create returns. If the server's initial 401
        // wins that race, the runtime records `needs-auth` WITHOUT invoking the host callback,
        // so the callback fires only on a later auth retry (now that interest is registered),
        // with the same `Initial` reason. Await the callback rather than sampling it the instant
        // `needs-auth` first appears, which is what made this test flaky.
        var observedRequest = await authRequest.Task.WaitAsync(TimeSpan.FromSeconds(60));

        Assert.NotEmpty(observedRequest.RequestId);
        Assert.Equal(serverName, observedRequest.ServerName);
        Assert.Equal(McpOauthRequestReason.Initial, observedRequest.Reason);
    }

    private static async Task CallWhoamiAsync(CopilotSession session, string serverName, string scenario)
    {
        using var argumentDocument = JsonDocument.Parse($"{{\"scenario\":\"{scenario}\"}}");
        var result = await session.Rpc.Mcp.Apps.CallToolAsync(
            serverName,
            "whoami",
            serverName,
            new Dictionary<string, JsonElement>
            {
                ["scenario"] = argumentDocument.RootElement.GetProperty("scenario").Clone()
            });

        var content = result["content"].EnumerateArray().ToList();
        Assert.Single(content);
        Assert.Equal("oauth-test-user", content[0].GetProperty("text").GetString());
    }

    private sealed class OAuthMcpServer : IAsyncDisposable
    {
        private readonly Process _process;
        private readonly HttpClient _http = new();

        private OAuthMcpServer(Process process, string url)
        {
            _process = process;
            Url = url;
        }

        public string Url { get; }

        public static async Task<OAuthMcpServer> StartAsync(string expectedToken)
        {
            var repoRoot = FindRepoRoot();
            var script = GetRepoRelativePath(repoRoot, "test", "harness", "test-mcp-oauth-server.mjs");
            var startInfo = new ProcessStartInfo
            {
                FileName = "node",
                Arguments = QuoteProcessArgument(script),
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                UseShellExecute = false
            };
            startInfo.Environment["EXPECTED_TOKEN"] = expectedToken;

            var process = Process.Start(startInfo)
                ?? throw new InvalidOperationException("Failed to start OAuth MCP server.");
            var stderrTask = process.StandardError.ReadToEndAsync();

            using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
            while (!cts.IsCancellationRequested)
            {
                var line = await process.StandardOutput.ReadLineAsync(cts.Token);
                if (line is null)
                {
                    throw new InvalidOperationException($"OAuth MCP server exited before listening: {await stderrTask}");
                }
                if (line.StartsWith("Listening: ", StringComparison.Ordinal))
                {
                    return new OAuthMcpServer(process, line["Listening: ".Length..]);
                }
            }

            throw new TimeoutException($"Timed out waiting for OAuth MCP server: {await stderrTask}");
        }

        public async Task<List<OAuthMcpRequest>> GetRequestsAsync()
        {
            var json = await _http.GetStringAsync($"{Url}/__requests");
            using var document = JsonDocument.Parse(json);
            return document.RootElement.EnumerateArray()
                .Select(element => new OAuthMcpRequest(
                    element.TryGetProperty("authorization", out var authorization)
                        && authorization.ValueKind is JsonValueKind.String
                            ? authorization.GetString()
                            : null))
                .ToList();
        }

        public async ValueTask DisposeAsync()
        {
            _http.Dispose();
            if (!_process.HasExited)
            {
                _process.Kill(entireProcessTree: true);
                await _process.WaitForExitAsync();
            }
            _process.Dispose();
        }

        private static string FindRepoRoot()
        {
            var dir = new DirectoryInfo(AppContext.BaseDirectory);
            while (dir != null)
            {
                var candidate = GetRepoRelativePath(dir.FullName, "test", "harness", "test-mcp-oauth-server.mjs");
                if (File.Exists(candidate))
                    return dir.FullName;
                dir = dir.Parent;
            }
            throw new InvalidOperationException("Could not find repository root.");
        }

        private static string GetRepoRelativePath(string repoRoot, params string[] relativeSegments)
        {
            var path = repoRoot;
            foreach (var segment in relativeSegments)
            {
                if (Path.IsPathRooted(segment))
                    throw new ArgumentException("Repository-relative path segments must not be rooted.", nameof(relativeSegments));
                path = Path.Join(path, segment);
            }
            return Path.GetFullPath(path);
        }

        private static string QuoteProcessArgument(string argument)
            => "\"" + argument.Replace("\"", "\\\"") + "\"";
    }

    private sealed record OAuthMcpRequest(string? Authorization);
}
