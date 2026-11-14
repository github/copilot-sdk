/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * One step in an interactive login flow. The consumer acts on the step and calls advance to proceed. Browser-open is encoded as two distinct steps by design: `open-url` is CONSUMER-driven (the provider surfaces the authorize URL and the consumer opens it — github.com/GHEC web), while `needs-interaction` is PROVIDER-driven (the provider opens the browser or broker UI itself and does not surface a URL — Entra).
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = AuthLoginStepOpenUrl.class, name = "open-url"),
    @JsonSubTypes.Type(value = AuthLoginStepInputRequired.class, name = "input-required"),
    @JsonSubTypes.Type(value = AuthLoginStepAwaiting.class, name = "awaiting"),
    @JsonSubTypes.Type(value = AuthLoginStepNeedsInteraction.class, name = "needs-interaction"),
    @JsonSubTypes.Type(value = AuthLoginStepCompleted.class, name = "completed"),
    @JsonSubTypes.Type(value = AuthLoginStepError.class, name = "error")
})
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class AuthLoginStep {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    public abstract String getKind();
}
