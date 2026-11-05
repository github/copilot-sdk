/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Reflection;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed class InProcessRuntimeArgumentsTests
{
    [Fact]
    public void Runtime_Environment_Applies_Typed_Options_With_Platform_Casing()
    {
        var options = new CopilotClientOptions
        {
            InternalTestOptions = new InternalTestOptions
            {
                CopilotApiUrl = "http://replay",
                GitHubApiUrl = "http://github-replay",
                ProxyUrl = "http://interception",
                NoProxy = "",
                CaCertificatePath = "/test/ca.pem",
            },
            GitHubToken = "typed-token",
            BaseDirectory = "typed-home",
        };
        var method = typeof(CopilotClient).GetMethod(
            "BuildInProcessRuntimeEnvironment", BindingFlags.Static | BindingFlags.NonPublic);
        Assert.NotNull(method);
        var environment = Assert.IsType<Dictionary<string, string>>(method!.Invoke(null, [options.Clone()]));
        var expected = new Dictionary<string, string>
        {
            ["COPILOT_API_URL"] = "http://replay",
            ["COPILOT_DEBUG_GITHUB_API_URL"] = "http://github-replay",
            ["HTTP_PROXY"] = "http://interception",
            ["HTTPS_PROXY"] = "http://interception",
            ["http_proxy"] = "http://interception",
            ["https_proxy"] = "http://interception",
            ["NO_PROXY"] = "",
            ["NODE_EXTRA_CA_CERTS"] = "/test/ca.pem",
            ["SSL_CERT_FILE"] = "/test/ca.pem",
            ["REQUESTS_CA_BUNDLE"] = "/test/ca.pem",
            ["CURL_CA_BUNDLE"] = "/test/ca.pem",
            ["GIT_SSL_CAINFO"] = "/test/ca.pem",
            ["COPILOT_SDK_AUTH_TOKEN"] = "typed-token",
            ["COPILOT_HOME"] = "typed-home",
            ["COPILOT_CACHE_HOME"] = Path.Combine("typed-home", "cache"),
        };
        foreach (var (key, value) in expected) Assert.Equal(value, environment[key]);
        if (OperatingSystem.IsWindows())
        {
            Assert.Equal(environment.Count, environment.Keys.Distinct(StringComparer.OrdinalIgnoreCase).Count());
        }

        var exception = Assert.Throws<ArgumentException>(() => new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri("localhost:8080"),
            InternalTestOptions = new InternalTestOptions(),
        }));
        Assert.Contains("requires a runtime owned by this client", exception.Message);
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public void InProcess_Environment_Is_Rejected(bool empty)
    {
        var exception = Assert.Throws<ArgumentException>(() => new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForInProcess(),
            Environment = empty ? [] : new Dictionary<string, string> { ["FOO"] = "bar" },
        }));
        Assert.Contains("CopilotClientOptions.Environment is not supported", exception.Message);
    }

    [Fact]
    public void InProcess_Client_Working_Directory_Remains_Unsupported()
    {
        var exception = Assert.Throws<ArgumentException>(() => new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForInProcess(),
            WorkingDirectory = ".",
        }));
        Assert.Contains("SessionConfig.WorkingDirectory", exception.Message);
    }

    [Fact]
    public void InProcess_Telemetry_Remains_Unsupported()
    {
        var exception = Assert.Throws<ArgumentException>(() => new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForInProcess(),
            Telemetry = new TelemetryConfig(),
        }));
        Assert.Contains("CopilotClientOptions.Telemetry", exception.Message);
    }
}
