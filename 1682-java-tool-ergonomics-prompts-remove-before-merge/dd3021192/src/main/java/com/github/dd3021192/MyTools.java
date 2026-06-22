package com.github.dd3021192;

/**
 * Simulates a user's tool class annotated with @CopilotTool methods.
 * In real usage, the annotation processor would generate MyTools$$CopilotToolMeta.
 */
public class MyTools {

    private String currentPhase = "init";

    // This would be annotated with @CopilotTool("Sets the current phase")
    public String setCurrentPhase(String phase) {
        this.currentPhase = phase;
        return "Phase set to " + phase;
    }

    public String getCurrentPhase() {
        return currentPhase;
    }
}
