/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics;
using Xunit;

namespace GitHub.Copilot.SDK.Test;

public class TelemetryTests
{
    [Fact]
    public void TelemetryConfig_DefaultValues_AreNull()
    {
        var config = new TelemetryConfig();

        Assert.Null(config.OtlpEndpoint);
        Assert.Null(config.FilePath);
        Assert.Null(config.ExporterType);
        Assert.Null(config.SourceName);
        Assert.Null(config.CaptureContent);
    }

    [Fact]
    public void TelemetryConfig_CanSetAllProperties()
    {
        var config = new TelemetryConfig
        {
            OtlpEndpoint = "http://localhost:4318",
            FilePath = "/tmp/traces.json",
            ExporterType = "otlp-http",
            SourceName = "my-app",
            CaptureContent = true
        };

        Assert.Equal("http://localhost:4318", config.OtlpEndpoint);
        Assert.Equal("/tmp/traces.json", config.FilePath);
        Assert.Equal("otlp-http", config.ExporterType);
        Assert.Equal("my-app", config.SourceName);
        Assert.True(config.CaptureContent);
    }

    [Fact]
    public void CopilotClientOptions_Telemetry_DefaultsToNull()
    {
        var options = new CopilotClientOptions();

        Assert.Null(options.Telemetry);
    }

    [Fact]
    public void CopilotClientOptions_Clone_CopiesTelemetry()
    {
        var telemetry = new TelemetryConfig
        {
            OtlpEndpoint = "http://localhost:4318",
            ExporterType = "otlp-http"
        };

        var options = new CopilotClientOptions { Telemetry = telemetry };
        var clone = options.Clone();

        Assert.Same(telemetry, clone.Telemetry);
    }
}
