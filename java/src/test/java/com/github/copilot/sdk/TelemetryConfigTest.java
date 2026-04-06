/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.json.TelemetryConfig;

/**
 * Unit tests for {@link TelemetryConfig} getters, setters, and fluent chaining.
 */
class TelemetryConfigTest {

    @Test
    void defaultValuesAreNull() {
        var config = new TelemetryConfig();
        assertNull(config.getOtlpEndpoint());
        assertNull(config.getFilePath());
        assertNull(config.getExporterType());
        assertNull(config.getSourceName());
        assertNull(config.getCaptureContent());
    }

    @Test
    void otlpEndpointGetterSetter() {
        var config = new TelemetryConfig();
        config.setOtlpEndpoint("http://localhost:4318");
        assertEquals("http://localhost:4318", config.getOtlpEndpoint());
    }

    @Test
    void filePathGetterSetter() {
        var config = new TelemetryConfig();
        config.setFilePath("/tmp/telemetry.log");
        assertEquals("/tmp/telemetry.log", config.getFilePath());
    }

    @Test
    void exporterTypeGetterSetter() {
        var config = new TelemetryConfig();
        config.setExporterType("otlp-http");
        assertEquals("otlp-http", config.getExporterType());
    }

    @Test
    void sourceNameGetterSetter() {
        var config = new TelemetryConfig();
        config.setSourceName("my-app");
        assertEquals("my-app", config.getSourceName());
    }

    @Test
    void captureContentGetterSetter() {
        var config = new TelemetryConfig();
        config.setCaptureContent(true);
        assertTrue(config.getCaptureContent());

        config.setCaptureContent(false);
        assertFalse(config.getCaptureContent());
    }

    @Test
    void fluentChainingReturnsThis() {
        var config = new TelemetryConfig().setOtlpEndpoint("http://localhost:4318").setFilePath("/tmp/spans.json")
                .setExporterType("file").setSourceName("sdk-test").setCaptureContent(true);

        assertEquals("http://localhost:4318", config.getOtlpEndpoint());
        assertEquals("/tmp/spans.json", config.getFilePath());
        assertEquals("file", config.getExporterType());
        assertEquals("sdk-test", config.getSourceName());
        assertTrue(config.getCaptureContent());
    }
}
