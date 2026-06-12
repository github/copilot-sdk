/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.TelemetryConfig;

/**
 * Unit tests for {@link TelemetryConfig} getters, setters, and fluent chaining.
 */
class TelemetryConfigTest {

    @Test
    void defaultValuesAreNull() {
        var config = new TelemetryConfig();
        assertNull(config.getOtlpEndpoint());
        assertNull(config.getOtlpProtocol());
        assertNull(config.getOtlpTracesProtocol());
        assertNull(config.getOtlpMetricsProtocol());
        assertNull(config.getFilePath());
        assertNull(config.getExporterType());
        assertNull(config.getSourceName());
        assertTrue(config.getCaptureContent().isEmpty());
    }

    @Test
    void otlpEndpointGetterSetter() {
        var config = new TelemetryConfig();
        config.setOtlpEndpoint("http://localhost:4318");
        assertEquals("http://localhost:4318", config.getOtlpEndpoint());
    }

    @Test
    void otlpProtocolGetterSetter() {
        var config = new TelemetryConfig();
        config.setOtlpProtocol("http/protobuf");
        assertEquals("http/protobuf", config.getOtlpProtocol());
    }

    @Test
    void otlpTracesProtocolGetterSetter() {
        var config = new TelemetryConfig();
        config.setOtlpTracesProtocol("http/json");
        assertEquals("http/json", config.getOtlpTracesProtocol());
    }

    @Test
    void otlpMetricsProtocolGetterSetter() {
        var config = new TelemetryConfig();
        config.setOtlpMetricsProtocol("http/protobuf");
        assertEquals("http/protobuf", config.getOtlpMetricsProtocol());
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
        assertTrue(config.getCaptureContent().get());

        config.setCaptureContent(false);
        assertFalse(config.getCaptureContent().get());
    }

    @Test
    void fluentChainingReturnsThis() {
        var config = new TelemetryConfig().setOtlpEndpoint("http://localhost:4318").setOtlpProtocol("http/protobuf")
                .setOtlpTracesProtocol("http/json").setOtlpMetricsProtocol("http/protobuf")
                .setFilePath("/tmp/spans.json").setExporterType("file").setSourceName("sdk-test")
                .setCaptureContent(true);

        assertEquals("http://localhost:4318", config.getOtlpEndpoint());
        assertEquals("http/protobuf", config.getOtlpProtocol());
        assertEquals("http/json", config.getOtlpTracesProtocol());
        assertEquals("http/protobuf", config.getOtlpMetricsProtocol());
        assertEquals("/tmp/spans.json", config.getFilePath());
        assertEquals("file", config.getExporterType());
        assertEquals("sdk-test", config.getSourceName());
        assertTrue(config.getCaptureContent().get());
    }
}
