/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.BuiltInTools;
import com.github.copilot.rpc.ToolSet;

/**
 * Tests for {@link ToolSet} and {@link BuiltInTools}.
 */
public class ToolSetTest {

    @Test
    void testAddBuiltIn() {
        var ts = new ToolSet().addBuiltIn("bash");
        assertEquals(1, ts.size());
        assertEquals("builtin:bash", ts.get(0));
    }

    @Test
    void testAddBuiltInWildcard() {
        var ts = new ToolSet().addBuiltIn("*");
        assertEquals("builtin:*", ts.get(0));
    }

    @Test
    void testAddCustom() {
        var ts = new ToolSet().addCustom("my_tool");
        assertEquals("custom:my_tool", ts.get(0));
    }

    @Test
    void testAddMcp() {
        var ts = new ToolSet().addMcp("github-list_issues");
        assertEquals("mcp:github-list_issues", ts.get(0));
    }

    @Test
    void testAddMcpWildcard() {
        var ts = new ToolSet().addMcp("*");
        assertEquals("mcp:*", ts.get(0));
    }

    @Test
    void testChaining() {
        var ts = new ToolSet().addBuiltIn("bash").addMcp("*").addCustom("my_tool");
        assertEquals(3, ts.size());
        assertEquals("builtin:bash", ts.get(0));
        assertEquals("mcp:*", ts.get(1));
        assertEquals("custom:my_tool", ts.get(2));
    }

    @Test
    void testAddBuiltInCollection() {
        var ts = new ToolSet();
        ts.addBuiltIn((Collection<String>) BuiltInTools.ISOLATED);
        assertEquals(BuiltInTools.ISOLATED.size(), ts.size());
        assertTrue(ts.contains("builtin:ask_user"));
        assertTrue(ts.contains("builtin:task_complete"));
    }

    @Test
    void testInvalidNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> new ToolSet().addBuiltIn(""));
        assertThrows(IllegalArgumentException.class, () -> new ToolSet().addBuiltIn((String) null));
        assertThrows(IllegalArgumentException.class, () -> new ToolSet().addBuiltIn("bad/name"));
        assertThrows(IllegalArgumentException.class, () -> new ToolSet().addBuiltIn("bad name"));
        assertThrows(IllegalArgumentException.class, () -> new ToolSet().addMcp("bad.name"));
        assertThrows(IllegalArgumentException.class, () -> new ToolSet().addCustom("bad:name"));
    }

    @Test
    void testValidNamePatterns() {
        // Names with letters, digits, hyphens, underscores are valid
        assertDoesNotThrow(() -> new ToolSet().addBuiltIn("my-tool_123"));
        assertDoesNotThrow(() -> new ToolSet().addMcp("github-list_issues"));
    }

    @Test
    void testBuiltInToolsIsolatedIsUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () -> BuiltInTools.ISOLATED.add("new_tool"));
    }

    @Test
    void testToolSetIsListOfStrings() {
        var ts = new ToolSet().addBuiltIn("bash").addMcp("*");
        // ToolSet extends ArrayList<String>, so it is a List<String>
        List<String> list = ts;
        assertEquals(2, list.size());
    }
}
