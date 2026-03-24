/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

/**
 * OpenTelemetry configuration for the Copilot CLI server.
 * <p>
 * When set on {@link CopilotClientOptions#setTelemetry(TelemetryConfig)}, the
 * CLI server is started with OpenTelemetry instrumentation enabled using the
 * provided settings.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var options = new CopilotClientOptions()
 * 		.setTelemetry(new TelemetryConfig().setOtlpEndpoint("http://localhost:4318").setSourceName("my-app"));
 * }</pre>
 *
 * @see CopilotClientOptions#setTelemetry(TelemetryConfig)
 * @since 1.2.0
 */
public class TelemetryConfig {

    private String otlpEndpoint;
    private String filePath;
    private String exporterType;
    private String sourceName;
    private Boolean captureContent;

    /**
     * Gets the OTLP exporter endpoint URL.
     * <p>
     * Maps to the {@code OTEL_EXPORTER_OTLP_ENDPOINT} environment variable.
     *
     * @return the OTLP endpoint URL, or {@code null}
     */
    public String getOtlpEndpoint() {
        return otlpEndpoint;
    }

    /**
     * Sets the OTLP exporter endpoint URL.
     *
     * @param otlpEndpoint
     *            the endpoint URL (e.g., {@code "http://localhost:4318"})
     * @return this config for method chaining
     */
    public TelemetryConfig setOtlpEndpoint(String otlpEndpoint) {
        this.otlpEndpoint = otlpEndpoint;
        return this;
    }

    /**
     * Gets the file path for the file exporter.
     * <p>
     * Maps to the {@code COPILOT_OTEL_FILE_EXPORTER_PATH} environment variable.
     *
     * @return the file path, or {@code null}
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Sets the file path for the file exporter.
     *
     * @param filePath
     *            the path where telemetry spans are written
     * @return this config for method chaining
     */
    public TelemetryConfig setFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    /**
     * Gets the exporter type.
     * <p>
     * Maps to the {@code COPILOT_OTEL_EXPORTER_TYPE} environment variable.
     *
     * @return the exporter type (e.g., {@code "otlp-http"} or {@code "file"}), or
     *         {@code null}
     */
    public String getExporterType() {
        return exporterType;
    }

    /**
     * Sets the exporter type.
     *
     * @param exporterType
     *            the exporter type ({@code "otlp-http"} or {@code "file"})
     * @return this config for method chaining
     */
    public TelemetryConfig setExporterType(String exporterType) {
        this.exporterType = exporterType;
        return this;
    }

    /**
     * Gets the source name for telemetry spans.
     * <p>
     * Maps to the {@code COPILOT_OTEL_SOURCE_NAME} environment variable.
     *
     * @return the source name, or {@code null}
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Sets the source name for telemetry spans.
     *
     * @param sourceName
     *            a name identifying the application producing the spans
     * @return this config for method chaining
     */
    public TelemetryConfig setSourceName(String sourceName) {
        this.sourceName = sourceName;
        return this;
    }

    /**
     * Gets whether to capture message content as part of telemetry.
     * <p>
     * Maps to the {@code OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT}
     * environment variable.
     *
     * @return {@code true} to capture content, {@code false} to suppress it, or
     *         {@code null} to use the default
     */
    public Boolean getCaptureContent() {
        return captureContent;
    }

    /**
     * Sets whether to capture message content as part of telemetry.
     *
     * @param captureContent
     *            {@code true} to capture content, {@code false} to suppress it
     * @return this config for method chaining
     */
    public TelemetryConfig setCaptureContent(Boolean captureContent) {
        this.captureContent = captureContent;
        return this;
    }
}
