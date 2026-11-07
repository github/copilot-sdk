/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.InstalledPluginInfo;
import com.github.copilot.generated.rpc.MarketplacePluginInfo;
import com.github.copilot.generated.rpc.Plugin;

class PluginLegacyConstructorCompatibilityTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void releasedConstructorsOmitTheAssignment() {
        var plugin = new Plugin("plugin", "market", "1.0.0", true, null, null, null, false, null, true);
        var installed = new InstalledPluginInfo("plugin", "market", null, "1.0.0", true, null, null, false, null, true,
                "description", null, null);
        var marketplace = new MarketplacePluginInfo("plugin", "description");

        assertEquals("plugin", plugin.name());
        assertEquals("plugin", installed.name());
        assertEquals("plugin", marketplace.name());
        assertNull(plugin.assignment());
        assertNull(installed.assignment());
        assertNull(marketplace.assignment());
        assertFalse(MAPPER.valueToTree(plugin).has("assignment"));
        assertFalse(MAPPER.valueToTree(installed).has("assignment"));
        assertFalse(MAPPER.valueToTree(marketplace).has("assignment"));
    }

    @Test
    void assignmentDeserializesThroughTheCanonicalRecords() throws Exception {
        var json = """
                {"name":"plugin","marketplace":"market","enabled":true,
                 "assignment":{"state":"defaultEnabled","reason":"Company setup"}}
                """;
        var plugin = MAPPER.readValue(json, Plugin.class);
        var installed = MAPPER.readValue(json, InstalledPluginInfo.class);
        var marketplace = MAPPER.readValue(json, MarketplacePluginInfo.class);

        assertEquals("defaultEnabled", plugin.assignment().state());
        assertEquals("Company setup", installed.assignment().reason());
        assertEquals("defaultEnabled", marketplace.assignment().state());
    }
}
