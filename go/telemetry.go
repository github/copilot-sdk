package copilot

import (
	"context"
	"strconv"

	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/propagation"
)

func (config *TelemetryConfig) environment() map[string]string {
	if config == nil {
		return nil
	}
	env := map[string]string{"COPILOT_OTEL_ENABLED": "true"}
	for key, value := range map[string]string{
		"OTEL_EXPORTER_OTLP_ENDPOINT":     config.OTLPEndpoint,
		"OTEL_EXPORTER_OTLP_PROTOCOL":     config.OTLPProtocol,
		"COPILOT_OTEL_FILE_EXPORTER_PATH": config.FilePath,
		"COPILOT_OTEL_EXPORTER_TYPE":      config.ExporterType,
		"COPILOT_OTEL_SOURCE_NAME":        config.SourceName,
	} {
		if value != "" {
			env[key] = value
		}
	}
	if config.CaptureContent != nil {
		mode := "NO_CONTENT"
		if *config.CaptureContent {
			mode = "SPAN_ONLY"
		}
		env["OTEL_INSTRUMENTATION_GENAI_CAPTURE_CONTENT"] = mode
		env["OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT"] = strconv.FormatBool(*config.CaptureContent)
	}
	return env
}

// getTraceContext extracts the current W3C Trace Context (traceparent/tracestate)
// from the Go context using the global OTel propagator.
func getTraceContext(ctx context.Context) (traceparent, tracestate string) {
	carrier := propagation.MapCarrier{}
	otel.GetTextMapPropagator().Inject(ctx, carrier)
	return carrier.Get("traceparent"), carrier.Get("tracestate")
}

// contextWithTraceParent returns a new context with trace context extracted from
// the provided W3C traceparent and tracestate headers.
func contextWithTraceParent(ctx context.Context, traceparent, tracestate string) context.Context {
	if traceparent == "" {
		return ctx
	}
	carrier := propagation.MapCarrier{
		"traceparent": traceparent,
	}
	if tracestate != "" {
		carrier["tracestate"] = tracestate
	}
	return otel.GetTextMapPropagator().Extract(ctx, carrier)
}
