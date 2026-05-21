/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.json.ModelInfo;
import com.github.copilot.sdk.json.ModelSupports;
import com.github.copilot.sdk.json.SessionMetadata;

/**
 * Unit tests for {@link ModelInfo}, {@link ModelSupports}, and
 * {@link SessionMetadata} getters and setters.
 */
class ModelInfoTest {

    @Test
    void modelSupportsReasoningEffortGetterSetter() {
        var supports = new ModelSupports();
        assertFalse(supports.isReasoningEffort());

        supports.setReasoningEffort(true);
        assertTrue(supports.isReasoningEffort());
    }

    @Test
    void modelSupportsFluentChaining() {
        var supports = new ModelSupports().setVision(true).setReasoningEffort(true);
        assertTrue(supports.isVision());
        assertTrue(supports.isReasoningEffort());
    }

    @Test
    void modelInfoSupportedReasoningEffortsGetterSetter() {
        var model = new ModelInfo();
        assertNull(model.getSupportedReasoningEfforts());

        model.setSupportedReasoningEfforts(List.of("low", "medium", "high"));
        assertEquals(List.of("low", "medium", "high"), model.getSupportedReasoningEfforts());
    }

    @Test
    void modelInfoDefaultReasoningEffortGetterSetter() {
        var model = new ModelInfo();
        assertNull(model.getDefaultReasoningEffort());

        model.setDefaultReasoningEffort("medium");
        assertEquals("medium", model.getDefaultReasoningEffort());
    }

    @Test
    void sessionMetadataGettersAndSetters() {
        var meta = new SessionMetadata();
        assertNull(meta.getStartTime());
        assertNull(meta.getModifiedTime());
        assertNull(meta.getSummary());
        assertFalse(meta.isRemote());

        meta.setRemote(true);
        assertTrue(meta.isRemote());
    }
}
