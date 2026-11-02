/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An ID-bound reference to an eligible live session.
 * <p>
 * Supplies identity context only, without sending a message, reading history,
 * or granting permission.
 *
 * @see MessageOptions#setAttachments(java.util.List)
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SessionReferenceAttachment implements MessageAttachment {

    @JsonProperty("type")
    private final String type = "session_reference";

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("mentionIndex")
    private Long mentionIndex;

    /**
     * Returns the attachment type, always {@code "session_reference"}.
     *
     * @return {@code "session_reference"}
     */
    @Override
    public String getType() {
        return type;
    }

    /**
     * Gets the stable identity of the selected session.
     *
     * @return the session identifier
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the stable identity of the selected session, not its title.
     *
     * @param sessionId
     *            the session identifier
     * @return this attachment for method chaining
     */
    public SessionReferenceAttachment setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    /**
     * Gets the mention label captured when the reference was selected.
     *
     * @return the display name, or {@code null}
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the mention label captured when the reference was selected.
     *
     * @param displayName
     *            the user-visible label
     * @return this attachment for method chaining
     */
    public SessionReferenceAttachment setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * Gets the character offset of the mention in the displayed prompt.
     *
     * @return the mention offset, or {@code null}
     */
    public Long getMentionIndex() {
        return mentionIndex;
    }

    /**
     * Sets the character offset of the mention in the displayed prompt.
     *
     * @param mentionIndex
     *            the mention offset, or {@code null} to omit it
     * @return this attachment for method chaining
     */
    public SessionReferenceAttachment setMentionIndex(Long mentionIndex) {
        this.mentionIndex = mentionIndex;
        return this;
    }
}
