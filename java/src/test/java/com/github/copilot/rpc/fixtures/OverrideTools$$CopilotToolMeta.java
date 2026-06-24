// Hand-written test fixture mimicking CopilotToolProcessor output.
package com.github.copilot.rpc.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.tool.CopilotToolMetadataProvider;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class OverrideTools$$CopilotToolMeta implements CopilotToolMetadataProvider<OverrideTools> {

    private static Map<String, Object> withMeta(Map<String, Object> base, String description, Object defaultValue) {
        var result = new LinkedHashMap<String, Object>(base);
        if (description != null)
            result.put("description", description);
        if (defaultValue != null)
            result.put("default", defaultValue);
        return Collections.unmodifiableMap(result);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<ToolDefinition> definitions(OverrideTools instance, ObjectMapper mapper) {
        return List
                .of(new ToolDefinition(
                        "grep", "Custom grep implementation", Map
                                .of("type", "object", "properties",
                                        Map.ofEntries(Map.entry("pattern",
                                                (Map<String, Object>) (Map) withMeta(Map.of("type", "string"),
                                                        "Search pattern", null))),
                                        "required", List.of("pattern")),
                        invocation -> {
                            Map<String, Object> args = invocation.getArguments();
                            String pattern = (String) args.get("pattern");
                            return CompletableFuture.completedFuture(instance.customGrep(pattern));
                        }, Boolean.TRUE, null, null));
    }
}
