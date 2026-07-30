/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * Open a session by creating, resuming, attaching, connecting to a remote, or handing off.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SessionsOpenCreate.class, name = "create"),
    @JsonSubTypes.Type(value = SessionsOpenResume.class, name = "resume"),
    @JsonSubTypes.Type(value = SessionsOpenResumeLast.class, name = "resumeLast"),
    @JsonSubTypes.Type(value = SessionsOpenAttach.class, name = "attach"),
    @JsonSubTypes.Type(value = SessionsOpenRemote.class, name = "remote"),
    @JsonSubTypes.Type(value = SessionsOpenCloud.class, name = "cloud"),
    @JsonSubTypes.Type(value = SessionsOpenHandoff.class, name = "handoff")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class SessionsOpenParams {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    public abstract String getKind();
}
