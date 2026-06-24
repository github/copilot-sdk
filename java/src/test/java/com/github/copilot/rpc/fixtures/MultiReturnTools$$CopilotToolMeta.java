// Hand-written test fixture mimicking CopilotToolProcessor output.
package com.github.copilot.rpc.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.tool.CopilotToolMetadataProvider;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class MultiReturnTools$$CopilotToolMeta implements CopilotToolMetadataProvider<MultiReturnTools> {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<ToolDefinition> definitions(MultiReturnTools instance, ObjectMapper mapper) {
        return List.of(new ToolDefinition("string_method", "Returns a string",
                Map.of("type", "object", "properties", Map.of(), "required", List.of()), invocation -> {
                    return CompletableFuture.completedFuture(instance.stringMethod());
                }, null, null, null), new ToolDefinition("void_method", "Void method",
                        Map.of("type", "object", "properties", Map.of(), "required", List.of()), invocation -> {
                            instance.voidMethod();
                            return CompletableFuture.completedFuture("Success");
                        }, null, null, null),
                new ToolDefinition("async_method", "Async method",
                        Map.of("type", "object", "properties", Map.of(), "required", List.of()), invocation -> {
                            return instance.asyncMethod().thenApply(r -> (Object) r);
                        }, null, null, null));
    }
}
