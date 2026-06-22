package com.github.dd3021192;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Simulates the generated companion class that the annotation processor would produce.
 * In real usage, this class is auto-generated at compile time.
 *
 * The key point: it lives in the SAME package as MyTools, so it can call
 * package-private methods and is discoverable via Class.forName() from the same module.
 */
public final class MyTools$$CopilotToolMeta {

    private MyTools$$CopilotToolMeta() {}

    /**
     * Returns tool definitions for the given MyTools instance.
     * This mirrors what the real generated code will produce.
     */
    public static List<Map<String, Object>> definitions(MyTools instance) {
        return List.of(
            Map.of(
                "name", "set_current_phase",
                "description", "Sets the current phase",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "phase", Map.of(
                            "type", "string",
                            "description", "The phase to transition to"
                        )
                    ),
                    "required", List.of("phase")
                )
            )
        );
    }
}
