/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * Structured metadata identifying what triggered this notification
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SystemNotificationAgentCompleted.class, name = "agent_completed"),
    @JsonSubTypes.Type(value = SystemNotificationAgentIdle.class, name = "agent_idle"),
    @JsonSubTypes.Type(value = SystemNotificationNewInboxMessage.class, name = "new_inbox_message"),
    @JsonSubTypes.Type(value = SystemNotificationShellCompleted.class, name = "shell_completed"),
    @JsonSubTypes.Type(value = SystemNotificationShellDetachedCompleted.class, name = "shell_detached_completed"),
    @JsonSubTypes.Type(value = SystemNotificationInstructionDiscovered.class, name = "instruction_discovered")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class SystemNotification {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the type discriminator
     */
    public abstract String getType();
}
