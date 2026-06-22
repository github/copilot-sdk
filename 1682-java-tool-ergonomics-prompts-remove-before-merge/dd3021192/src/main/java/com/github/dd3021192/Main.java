package com.github.dd3021192;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Named-module JPMS test for issue #1682, Phase 3.7.
 *
 * Proves that ToolDefinition.fromObject() pattern works in a named module:
 * - Class.forName() can locate the generated $$CopilotToolMeta companion class
 * - The companion class is accessible (same package, same module)
 * - Method invocation on the companion works without extra JPMS exports
 */
public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("=== JPMS Named-Module Test for §3.7 ===");
        System.out.println("Module: " + Main.class.getModule().getName());
        System.out.println();

        MyTools instance = new MyTools();
        Class<?> toolsClass = instance.getClass();

        // This is exactly what ToolDefinition.fromObject() will do:
        String metaClassName = toolsClass.getName() + "$$CopilotToolMeta";
        System.out.println("Looking up generated meta class: " + metaClassName);

        // Step 1: Class.forName() — the critical JPMS question
        Class<?> metaClass = Class.forName(metaClassName);
        System.out.println("[PASS] Class.forName() found: " + metaClass.getName());

        // Step 2: Get the 'definitions' method
        Method defsMethod = metaClass.getMethod("definitions", toolsClass);
        System.out.println("[PASS] Found method: " + defsMethod);

        // Step 3: Invoke it
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> definitions =
            (List<Map<String, Object>>) defsMethod.invoke(null, instance);
        System.out.println("[PASS] Invoked definitions(), got " + definitions.size() + " tool(s)");

        // Step 4: Verify content
        Map<String, Object> tool = definitions.get(0);
        assert "set_current_phase".equals(tool.get("name"))
            : "Expected tool name 'set_current_phase', got: " + tool.get("name");
        assert "Sets the current phase".equals(tool.get("description"))
            : "Expected description mismatch";
        System.out.println("[PASS] Tool definition correct: " + tool.get("name")
            + " — \"" + tool.get("description") + "\"");

        System.out.println();
        System.out.println("=== ALL CHECKS PASSED ===");
        System.out.println("Conclusion: Class.forName() works within a named JPMS module");
        System.out.println("for locating $$CopilotToolMeta in the same package. No extra");
        System.out.println("exports or opens directives are needed.");
    }
}
