/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.sdk.json.ToolInvocation;

/**
 * Unit tests for {@link ToolInvocation}.
 * <p>
 * Tests getter methods, type-safe deserialization, and null handling to improve
 * coverage beyond what E2E tests exercise.
 */
public class ToolInvocationTest {

    /**
     * Test all basic getters return values set via setters.
     */
    @Test
    void testGettersReturnSetValues() {
        ToolInvocation invocation = new ToolInvocation().setSessionId("test-session-123").setToolCallId("call_abc123")
                .setToolName("test_tool");

        assertEquals("test-session-123", invocation.getSessionId());
        assertEquals("call_abc123", invocation.getToolCallId());
        assertEquals("test_tool", invocation.getToolName());
    }

    /**
     * Test getArguments returns null when no arguments are set.
     */
    @Test
    void testGetArgumentsWhenNull() {
        ToolInvocation invocation = new ToolInvocation();
        assertNull(invocation.getArguments(), "getArguments should return null when argumentsNode is null");
    }

    /**
     * Test getArguments returns a Map when arguments are set.
     */
    @Test
    void testGetArgumentsReturnsMap() {
        ToolInvocation invocation = new ToolInvocation();

        // Create a JsonNode with some arguments
        ObjectNode argsNode = JsonNodeFactory.instance.objectNode();
        argsNode.put("location", "San Francisco");
        argsNode.put("units", "celsius");

        invocation.setArguments(argsNode);

        var args = invocation.getArguments();
        assertNotNull(args);
        assertEquals("San Francisco", args.get("location"));
        assertEquals("celsius", args.get("units"));
    }

    /**
     * Test getArgumentsAs deserializes to a record type.
     */
    @Test
    void testGetArgumentsAsWithRecord() {
        ToolInvocation invocation = new ToolInvocation();

        // Create a JsonNode with weather arguments
        ObjectNode argsNode = JsonNodeFactory.instance.objectNode();
        argsNode.put("city", "Paris");
        argsNode.put("units", "metric");

        invocation.setArguments(argsNode);

        // Deserialize to record
        WeatherArgs args = invocation.getArgumentsAs(WeatherArgs.class);
        assertNotNull(args);
        assertEquals("Paris", args.city());
        assertEquals("metric", args.units());
    }

    /**
     * Test getArgumentsAs deserializes to a POJO.
     */
    @Test
    void testGetArgumentsAsWithPojo() {
        ToolInvocation invocation = new ToolInvocation();

        // Create a JsonNode with user data
        ObjectNode argsNode = JsonNodeFactory.instance.objectNode();
        argsNode.put("username", "alice");
        argsNode.put("age", 30);

        invocation.setArguments(argsNode);

        // Deserialize to POJO
        UserData userData = invocation.getArgumentsAs(UserData.class);
        assertNotNull(userData);
        assertEquals("alice", userData.getUsername());
        assertEquals(30, userData.getAge());
    }

    /**
     * Test getArgumentsAs throws IllegalArgumentException on deserialization
     * failure.
     */
    @Test
    void testGetArgumentsAsThrowsOnInvalidType() {
        ToolInvocation invocation = new ToolInvocation();

        // Create invalid JSON for the target type (missing required field)
        ObjectNode argsNode = JsonNodeFactory.instance.objectNode();
        argsNode.put("invalid_field", "value");

        invocation.setArguments(argsNode);

        // Try to deserialize to a type that doesn't match
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> invocation.getArgumentsAs(StrictType.class),
                "Should throw IllegalArgumentException for invalid deserialization");

        assertTrue(exception.getMessage().contains("Failed to deserialize arguments"));
        assertTrue(exception.getMessage().contains("StrictType"));
    }

    /**
     * Record for testing type-safe argument deserialization.
     */
    record WeatherArgs(String city, String units) {
    }

    /**
     * POJO for testing type-safe argument deserialization.
     */
    public static class UserData {
        private String username;
        private int age;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    /**
     * Strict type with constructor that throws, for testing error handling.
     */
    public static class StrictType {
        private final String requiredField;

        public StrictType(String requiredField) {
            if (requiredField == null) {
                throw new IllegalArgumentException("requiredField cannot be null");
            }
            this.requiredField = requiredField;
        }

        public String getRequiredField() {
            return requiredField;
        }
    }
}
