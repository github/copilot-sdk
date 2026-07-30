// Hand-written test fixture mimicking CopilotToolProcessor output for ToolInvocation injection.
package com.github.copilot.rpc.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.rpc.RecordInvocationArgs;
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.tool.CopilotToolMetadataProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class InvocationAwareTools$$CopilotToolMeta implements CopilotToolMetadataProvider<InvocationAwareTools> {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<ToolDefinition> definitions(InvocationAwareTools instance, ObjectMapper mapper) {
        return List.of(new ToolDefinition("report_progress", "Reports progress with invocation context",
                Map.of("type", "object", "properties",
                        Map.ofEntries(Map.entry("phase", Map.of("type", "string", "description", "Current phase"))),
                        "required", List.of("phase")),
                invocation -> {
                    Map<String, Object> args = invocation.getArguments();
                    String phase = (String) args.get("phase");
                    return CompletableFuture.completedFuture(instance.reportProgress(phase, invocation));
                }, null, null, null, null),
                new ToolDefinition("report_progress_async", "Reports progress asynchronously with invocation context",
                        Map.of("type", "object", "properties",
                                Map.ofEntries(
                                        Map.entry("phase", Map.of("type", "string", "description", "Current phase"))),
                                "required", List.of("phase")),
                        invocation -> {
                            Map<String, Object> args = invocation.getArguments();
                            String phase = (String) args.get("phase");
                            return instance.reportProgressAsync(phase, invocation).thenApply(r -> (Object) r);
                        }, null, null, null, null),
                new ToolDefinition("report_progress_first", "Reports progress with invocation first",
                        Map.of("type", "object", "properties",
                                Map.ofEntries(
                                        Map.entry("phase", Map.of("type", "string", "description", "Current phase"))),
                                "required", List.of("phase")),
                        invocation -> {
                            Map<String, Object> args = invocation.getArguments();
                            String phase = (String) args.get("phase");
                            return CompletableFuture.completedFuture(instance.reportProgressFirst(invocation, phase));
                        }, null, null, null, null),
                new ToolDefinition("only_context", "Reports context with invocation only",
                        Map.of("type", "object", "properties", Map.of(), "required", List.of()),
                        invocation -> CompletableFuture.completedFuture(instance.onlyContext(invocation)), null, null,
                        null, null),
                new ToolDefinition("report_progress_middle", "Reports progress with invocation in the middle", Map.of(
                        "type", "object", "properties",
                        Map.ofEntries(Map.entry("phase", Map.of("type", "string", "description", "Current phase")),
                                Map.entry("limit", Map.of("type", "integer", "description", "Maximum items"))),
                        "required", List.of("phase", "limit")), invocation -> {
                            Map<String, Object> args = invocation.getArguments();
                            String phase = (String) args.get("phase");
                            int limit = ((Number) args.get("limit")).intValue();
                            return CompletableFuture
                                    .completedFuture(instance.reportProgressMiddle(phase, invocation, limit));
                        }, null, null, null, null),
                new ToolDefinition("report_progress_with_record", "Reports progress with record args and invocation",
                        Map.of("type", "object", "properties",
                                Map.ofEntries(Map.entry("query", Map.of("type", "string")),
                                        Map.entry("limit", Map.of("type", "integer"))),
                                "required", List.of("query", "limit")),
                        invocation -> {
                            RecordInvocationArgs args = mapper.convertValue(invocation.getArguments(),
                                    RecordInvocationArgs.class);
                            return CompletableFuture
                                    .completedFuture(instance.reportProgressWithRecord(args, invocation));
                        }, null, null, null, null));
    }
}
