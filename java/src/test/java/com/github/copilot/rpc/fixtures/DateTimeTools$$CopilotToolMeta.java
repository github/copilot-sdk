// Hand-written test fixture mimicking CopilotToolProcessor output.
package com.github.copilot.rpc.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.tool.CopilotToolMetadataProvider;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class DateTimeTools$$CopilotToolMeta implements CopilotToolMetadataProvider<DateTimeTools> {

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
    public List<ToolDefinition> definitions(DateTimeTools instance, ObjectMapper mapper) {
        return List.of(new ToolDefinition("schedule_event", "Schedule an event at a given time",
                Map.of("type", "object", "properties",
                        Map.ofEntries(Map.entry("when",
                                (Map<String, Object>) (Map) withMeta(Map.of("type", "string", "format", "date-time"),
                                        "When to schedule", null))),
                        "required", List.of("when")),
                invocation -> {
                    Map<String, Object> args = invocation.getArguments();
                    LocalDateTime when = mapper.convertValue(args.get("when"), LocalDateTime.class);
                    return CompletableFuture.completedFuture(instance.scheduleEvent(when));
                }, null, null, null));
    }
}
