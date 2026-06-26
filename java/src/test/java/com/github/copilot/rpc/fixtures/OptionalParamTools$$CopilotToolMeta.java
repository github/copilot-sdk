// Hand-written test fixture mimicking CopilotToolProcessor output for Optional parameters.
package com.github.copilot.rpc.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.tool.CopilotToolMetadataProvider;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class OptionalParamTools$$CopilotToolMeta implements CopilotToolMetadataProvider<OptionalParamTools> {

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
    public List<ToolDefinition> definitions(OptionalParamTools instance, ObjectMapper mapper) {
        return List.of(new ToolDefinition(
                "greet_with_title", "Greet with optional title", Map
                        .of("type", "object", "properties",
                                Map.ofEntries(
                                        Map.entry("name",
                                                (Map<String, Object>) (Map) withMeta(Map.of("type", "string"), "Name",
                                                        null)),
                                        Map.entry("title",
                                                (Map<String, Object>) (Map) withMeta(Map.of("type", "string"),
                                                        "Optional title", null))),
                                "required", List.of("name")),
                invocation -> {
                    Map<String, Object> args = invocation.getArguments();
                    String name = (String) args.get("name");
                    Object titleRaw = args.get("title");
                    Optional<String> title = titleRaw != null ? Optional.of((String) titleRaw) : Optional.empty();
                    return CompletableFuture.completedFuture(instance.greetWithTitle(name, title));
                }, null, null, null),
                new ToolDefinition("multiply", "Multiply with optional factor",
                        Map.of("type", "object", "properties",
                                Map.ofEntries(
                                        Map.entry("base",
                                                (Map<String, Object>) (Map) withMeta(Map.of("type", "integer"),
                                                        "Base value", null)),
                                        Map.entry("factor",
                                                (Map<String, Object>) (Map) withMeta(Map.of("type", "integer"),
                                                        "Optional factor", null))),
                                "required", List.of("base")),
                        invocation -> {
                            Map<String, Object> args = invocation.getArguments();
                            int base = ((Number) args.get("base")).intValue();
                            Object factorRaw = args.get("factor");
                            OptionalInt factor = factorRaw != null
                                    ? OptionalInt.of(((Number) factorRaw).intValue())
                                    : OptionalInt.empty();
                            return CompletableFuture.completedFuture(instance.multiply(base, factor));
                        }, null, null, null),
                new ToolDefinition("scale", "Scale with optional ratio",
                        Map.of("type", "object", "properties",
                                Map.ofEntries(
                                        Map.entry("value",
                                                (Map<String, Object>) (Map) withMeta(Map.of("type", "number"), "Value",
                                                        null)),
                                        Map.entry("ratio",
                                                (Map<String, Object>) (Map) withMeta(Map.of("type", "number"),
                                                        "Optional ratio", null))),
                                "required", List.of("value")),
                        invocation -> {
                            Map<String, Object> args = invocation.getArguments();
                            double value = ((Number) args.get("value")).doubleValue();
                            Object ratioRaw = args.get("ratio");
                            OptionalDouble ratio = ratioRaw != null
                                    ? OptionalDouble.of(((Number) ratioRaw).doubleValue())
                                    : OptionalDouble.empty();
                            return CompletableFuture.completedFuture(instance.scale(value, ratio));
                        }, null, null, null),
                new ToolDefinition("offset", "Offset with optional delta",
                        Map.of("type", "object", "properties",
                                Map.ofEntries(
                                        Map.entry("base",
                                                (Map<String, Object>) (Map) withMeta(Map.of("type", "integer"), "Base",
                                                        null)),
                                        Map.entry("delta",
                                                (Map<String, Object>) (Map) withMeta(Map.of("type", "integer"),
                                                        "Optional delta", null))),
                                "required", List.of("base")),
                        invocation -> {
                            Map<String, Object> args = invocation.getArguments();
                            long base = ((Number) args.get("base")).longValue();
                            Object deltaRaw = args.get("delta");
                            OptionalLong delta = deltaRaw != null
                                    ? OptionalLong.of(((Number) deltaRaw).longValue())
                                    : OptionalLong.empty();
                            return CompletableFuture.completedFuture(instance.offset(base, delta));
                        }, null, null, null));
    }
}
