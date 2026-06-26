// Hand-written test fixture mimicking CopilotToolProcessor output for static methods.
package com.github.copilot.rpc.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.tool.CopilotToolMetadataProvider;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class StaticTools$$CopilotToolMeta implements CopilotToolMetadataProvider<StaticTools> {

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
    public List<ToolDefinition> definitions(StaticTools instance, ObjectMapper mapper) {
        return List.of(new ToolDefinition("greet", "Returns a greeting for the given name",
                Map.of("type", "object", "properties", Map.ofEntries(Map.entry("name",
                        (Map<String, Object>) (Map) withMeta(Map.of("type", "string"), "The name to greet", null))),
                        "required", List.of("name")),
                invocation -> {
                    Map<String, Object> args = invocation.getArguments();
                    String name = (String) args.get("name");
                    // Mimics what the processor now generates for static methods:
                    // QualifiedClassName.method(...) instead of instance.method(...)
                    return CompletableFuture.completedFuture(StaticTools.greet(name));
                }, null, null, null));
    }
}
