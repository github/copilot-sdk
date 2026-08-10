// Hand-written test fixture mimicking CopilotToolProcessor output for static ToolInvocation injection.
package com.github.copilot.rpc.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.tool.CopilotToolMetadataProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class StaticInvocationTools$$CopilotToolMeta
        implements
            CopilotToolMetadataProvider<StaticInvocationTools> {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<ToolDefinition> definitions(StaticInvocationTools instance, ObjectMapper mapper) {
        return List.of(new ToolDefinition("report_static", "Returns invocation context from a static tool",
                Map.of("type", "object", "properties",
                        Map.ofEntries(Map.entry("phase", Map.of("type", "string", "description", "Current phase"))),
                        "required", List.of("phase")),
                invocation -> {
                    Map<String, Object> args = invocation.getArguments();
                    String phase = (String) args.get("phase");
                    return CompletableFuture.completedFuture(StaticInvocationTools.reportStatic(phase, invocation));
                }, null, null, null, null));
    }
}
