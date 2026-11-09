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

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public void InProcess_Telemetry_Configures_Only_The_Owned_Runtime(bool captureContent)
    {
        var before = Environment.GetEnvironmentVariables();
        var options = new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForInProcess(),
            Telemetry = new TelemetryConfig
            {
                OtlpEndpoint = "http://collector:4318",
                OtlpProtocol = "http/protobuf",
                FilePath = "/telemetry.jsonl",
                ExporterType = "file",
                SourceName = "inprocess-sdk",
                CaptureContent = captureContent,
            },
        };
        using var client = new CopilotClient(options);
        var method = typeof(CopilotClient).GetMethod(
            "BuildInProcessRuntimeEnvironment", BindingFlags.Static | BindingFlags.NonPublic);
        Assert.NotNull(method);
        var environment = Assert.IsType<Dictionary<string, string>>(method!.Invoke(null, [options]));
        var expected = new Dictionary<string, string>
        {
            ["COPILOT_OTEL_ENABLED"] = "true",
            ["OTEL_EXPORTER_OTLP_ENDPOINT"] = "http://collector:4318",
            ["OTEL_EXPORTER_OTLP_PROTOCOL"] = "http/protobuf",
            ["COPILOT_OTEL_FILE_EXPORTER_PATH"] = "/telemetry.jsonl",
            ["COPILOT_OTEL_EXPORTER_TYPE"] = "file",
            ["COPILOT_OTEL_SOURCE_NAME"] = "inprocess-sdk",
            ["OTEL_INSTRUMENTATION_GENAI_CAPTURE_CONTENT"] = captureContent ? "SPAN_ONLY" : "NO_CONTENT",
            ["OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT"] = captureContent ? "true" : "false",
        };
        foreach (var (key, value) in expected) Assert.Equal(value, environment[key]);
        // The single-variable getter can report native empty values as missing on .NET Framework.
        var after = Environment.GetEnvironmentVariables();
        Assert.Equal(before.Count, after.Count);
        foreach (System.Collections.DictionaryEntry entry in before)
        {
            Assert.True(after.Contains(entry.Key), $"Environment variable '{entry.Key}' was removed.");
            Assert.Equal(entry.Value, after[entry.Key]);
        }
    }
}
