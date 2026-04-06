/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: capabilities.changed
 * <p>
 * Broadcast when the host's session capabilities change. The SDK updates
 * {@link com.github.copilot.sdk.CopilotSession#getCapabilities()} accordingly.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CapabilitiesChangedEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private CapabilitiesChangedData data;

    @Override
    public String getType() {
        return "capabilities.changed";
    }

    public CapabilitiesChangedData getData() {
        return data;
    }

    public void setData(CapabilitiesChangedData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CapabilitiesChangedData(@JsonProperty("ui") CapabilitiesChangedUi ui) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CapabilitiesChangedUi(@JsonProperty("elicitation") Boolean elicitation) {
    }
}
