// Hand-written test fixture mimicking CopilotToolProcessor output for ToolInvocation injection.
package com.github.copilot.rpc.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
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
                }, null, null, null),
                new ToolDefinition("report_progress_async", "Reports progress asynchronously with invocation context",
                        Map.of("type", "object", "properties",
                                Map.ofEntries(
                                        Map.entry("phase", Map.of("type", "string", "description", "Current phase"))),
                                "required", List.of("phase")),
                        invocation -> {
                            Map<String, Object> args = invocation.getArguments();
                            String phase = (String) args.get("phase");
                            return instance.reportProgressAsync(phase, invocation).thenApply(r -> (Object) r);
                        }, null, null, null));
    }
}
