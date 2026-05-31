/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Schema for the `SystemNotificationNewInboxMessage` type.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SystemNotificationNewInboxMessage extends SystemNotification {

    @JsonProperty("type")
    private final String type = "new_inbox_message";

    @Override
    public String getType() { return type; }

    /** Unique identifier of the inbox entry */
    @JsonProperty("entryId")
    private String entryId;

    /** Human-readable name of the sender */
    @JsonProperty("senderName")
    private String senderName;

    /** Category of the sender (e.g., sidekick-agent, plugin, hook) */
    @JsonProperty("senderType")
    private String senderType;

    /** Short summary shown before the agent decides whether to read the inbox */
    @JsonProperty("summary")
    private String summary;

    public String getEntryId() { return entryId; }
    public void setEntryId(String entryId) { this.entryId = entryId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderType() { return senderType; }
    public void setSenderType(String senderType) { this.senderType = senderType; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
