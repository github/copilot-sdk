// Hand-written test fixture mimicking CopilotToolProcessor output.
package com.github.copilot.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.tool.CopilotToolMetadataProvider;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class ErgonomicTestTools$$CopilotToolMeta implements CopilotToolMetadataProvider<ErgonomicTestTools> {

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
    public List<ToolDefinition> definitions(ErgonomicTestTools instance, ObjectMapper mapper) {
        return List.of(new ToolDefinition("set_current_phase", "Sets the current phase of the agent",
                Map.of("type", "object", "properties",
                        Map.ofEntries(Map.entry("phase",
                                (Map<String, Object>) (Map) withMeta(Map.of("type", "string"),
                                        "The phase to transition to", null))),
                        "required", List.of("phase")),
                invocation -> {
                    Map<String, Object> args = invocation.getArguments();
                    String phase = (String) args.get("phase");
                    return CompletableFuture.completedFuture(instance.setCurrentPhase(phase));
                }, null, null, null, null),
                new ToolDefinition(
                        "search_items", "Search for items by keyword", Map
                                .of("type", "object", "properties",
                                        Map.ofEntries(Map.entry("keyword",
                                                (Map<String, Object>) (Map) withMeta(Map.of("type", "string"),
                                                        "Search keyword", null))),
                                        "required", List.of("keyword")),
                        invocation -> {
                            Map<String, Object> args = invocation.getArguments();
                            String keyword = (String) args.get("keyword");
                            return CompletableFuture.completedFuture(instance.searchItems(keyword));
                        }, null, null, null, null),
                new ToolDefinition("get_status", "Returns the current status",
                        Map.of("type", "object", "properties", Map.of(), "required", List.of()), invocation -> {
                            return CompletableFuture.completedFuture(instance.getStatus());
                        }, null, null, null, null),
                new ToolDefinition("combine_values", "Combines two values into a single string", Map.of(
                        "type", "object", "properties", Map
                                .ofEntries(
                                        Map.entry("value1",
                                                (Map<String, Object>) (Map) withMeta(Map.of("type", "string"),
                                                        "First value", null)),
                                        Map.entry("value2",
                                                (Map<String, Object>) (Map) withMeta(Map.of("type", "string"),
                                                        "Second value", null))),
                        "required", List.of("value1", "value2")), invocation -> {
                            Map<String, Object> args = invocation.getArguments();
                            String value1 = (String) args.get("value1");
                            String value2 = (String) args.get("value2");
                            return CompletableFuture.completedFuture(instance.combineValues(value1, value2));
                        }, null, null, null, null));
    }
}
