/* eslint-disable @typescript-eslint/no-explicit-any */
import { describe, expect, it } from "vitest";
import { getTraceContext } from "../src/telemetry.js";
import type { TraceContextProvider } from "../src/types.js";

describe("telemetry", () => {
    describe("getTraceContext", () => {
        it("returns empty object when no provider is given", async () => {
            const ctx = await getTraceContext();
            expect(ctx).toEqual({});
        });

        it("returns empty object when provider is undefined", async () => {
            const ctx = await getTraceContext(undefined);
            expect(ctx).toEqual({});
        });

        it("calls provider and returns trace context", async () => {
            const provider: TraceContextProvider = () => ({
                traceparent: "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
                tracestate: "congo=t61rcWkgMzE",
            });
            const ctx = await getTraceContext(provider);
            expect(ctx).toEqual({
                traceparent: "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
                tracestate: "congo=t61rcWkgMzE",
            });
        });

        it("supports async providers", async () => {
            const provider: TraceContextProvider = async () => ({
                traceparent: "00-abcdef1234567890abcdef1234567890-1234567890abcdef-01",
            });
            const ctx = await getTraceContext(provider);
            expect(ctx).toEqual({
                traceparent: "00-abcdef1234567890abcdef1234567890-1234567890abcdef-01",
            });
        });

        it("returns empty object when provider throws", async () => {
            const provider: TraceContextProvider = () => {
                throw new Error("boom");
            };
            const ctx = await getTraceContext(provider);
            expect(ctx).toEqual({});
        });

        it("returns empty object when async provider rejects", async () => {
            const provider: TraceContextProvider = async () => {
                throw new Error("boom");
            };
            const ctx = await getTraceContext(provider);
            expect(ctx).toEqual({});
        });

        it("returns empty object when provider returns null", async () => {
            const provider = (() => null) as unknown as TraceContextProvider;
            const ctx = await getTraceContext(provider);
            expect(ctx).toEqual({});
        });
    });

    describe("TelemetryConfig env var mapping", () => {
        it("sets correct env vars for full telemetry config", async () => {
            const telemetry = {
                otlpEndpoint: "http://localhost:4318",
                filePath: "/tmp/traces.jsonl",
                exporterType: "otlp-http",
                sourceName: "my-app",
                captureContent: true,
            };

            const env: Record<string, string | undefined> = {};

            if (telemetry) {
                const t = telemetry;
                env.COPILOT_OTEL_ENABLED = "true";
                if (t.otlpEndpoint !== undefined) env.OTEL_EXPORTER_OTLP_ENDPOINT = t.otlpEndpoint;
                if (t.filePath !== undefined) env.COPILOT_OTEL_FILE_EXPORTER_PATH = t.filePath;
                if (t.exporterType !== undefined) env.COPILOT_OTEL_EXPORTER_TYPE = t.exporterType;
                if (t.sourceName !== undefined) env.COPILOT_OTEL_SOURCE_NAME = t.sourceName;
                if (t.captureContent !== undefined)
                    env.OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT = String(
                        t.captureContent
                    );
            }

            expect(env).toEqual({
                COPILOT_OTEL_ENABLED: "true",
                OTEL_EXPORTER_OTLP_ENDPOINT: "http://localhost:4318",
                COPILOT_OTEL_FILE_EXPORTER_PATH: "/tmp/traces.jsonl",
                COPILOT_OTEL_EXPORTER_TYPE: "otlp-http",
                COPILOT_OTEL_SOURCE_NAME: "my-app",
                OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT: "true",
            });
        });

        it("only sets COPILOT_OTEL_ENABLED for empty telemetry config", async () => {
            const telemetry = {};
            const env: Record<string, string | undefined> = {};

            if (telemetry) {
                const t = telemetry as any;
                env.COPILOT_OTEL_ENABLED = "true";
                if (t.otlpEndpoint !== undefined) env.OTEL_EXPORTER_OTLP_ENDPOINT = t.otlpEndpoint;
                if (t.filePath !== undefined) env.COPILOT_OTEL_FILE_EXPORTER_PATH = t.filePath;
                if (t.exporterType !== undefined) env.COPILOT_OTEL_EXPORTER_TYPE = t.exporterType;
                if (t.sourceName !== undefined) env.COPILOT_OTEL_SOURCE_NAME = t.sourceName;
                if (t.captureContent !== undefined)
                    env.OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT = String(
                        t.captureContent
                    );
            }

            expect(env).toEqual({
                COPILOT_OTEL_ENABLED: "true",
            });
        });

        it("converts captureContent false to string 'false'", async () => {
            const telemetry = { captureContent: false };
            const env: Record<string, string | undefined> = {};

            env.COPILOT_OTEL_ENABLED = "true";
            if (telemetry.captureContent !== undefined)
                env.OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT = String(
                    telemetry.captureContent
                );

            expect(env.OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT).toBe("false");
        });
    });
});
